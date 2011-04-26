/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Version;

import com.google.common.base.Objects;
import com.google.common.io.NullOutputStream;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.Volume;

/**
 * {@link IndexStore} implementation using Apache Lucene.
 *
 * @author tim
 * @author ysasaki
 */
public final class LuceneIndex implements IndexStore {

    /**
     * We don't want to enable StopFilter preserving position increments, which is enabled on or after 2.9, because we
     * want phrases to match across removed stop words.
     * TODO: LUCENE_2* are oboslete as of Lucene 3.1.
     */
    @SuppressWarnings("deprecation")
    public static final Version VERSION = Version.LUCENE_24;

    private static final Semaphore READER_THROTTLE = new Semaphore(LC.zimbra_index_max_readers.intValue());
    private static final Semaphore WRITER_THROTTLE = new Semaphore(LC.zimbra_index_max_writers.intValue());

    private final Mailbox mailbox;
    private final LuceneDirectory luceneDirectory;
    private IndexWriterRef writerRef;
    private final IndexReadersCache readersCache;

    private LuceneIndex(Mailbox mbox, IndexReadersCache cache) throws ServiceException {
        mailbox = mbox;
        readersCache = cache;
        Volume vol = Volume.getById(mbox.getIndexVolume());
        String dir = vol.getMailboxDir(mailbox.getId(), Volume.TYPE_INDEX);

        // this must be different from the root dir (see the IMPORTANT comment below)
        File root = new File(dir + File.separatorChar + '0');

        // IMPORTANT!  Don't make the actual index directory (mIdxDirectory) yet!
        //
        // The runtime open-index code checks the existance of the actual index directory:
        // if it does exist but we cannot open the index, we do *NOT* create it under the
        // assumption that the index was somehow corrupted and shouldn't be messed-with....on the
        // other hand if the index dir does NOT exist, then we assume it has never existed (or
        // was deleted intentionally) and therefore we should just create an index.
        if (!root.exists()) {
            root.mkdirs();
        }

        if (!root.canRead()) {
            throw ServiceException.FAILURE("LuceneDirectory not readable mbox=" + mbox.getId() + ",dir=" + root, null);
        }
        if (!root.canWrite()) {
            throw ServiceException.FAILURE("LuceneDirectory not writable mbox=" + mbox.getId() + ",dir=" + root, null);
        }

        // the Lucene code does not atomically swap the "segments" and "segments.new" files...so it is possible that a
        // previous run of the server crashed exactly in such a way that we have a "segments.new" file but not a
        // "segments" file. We we will check here for the special situation that we have a segments.new/ file but not a
        // segments file...
        File segments = new File(root, "segments");
        if (!segments.exists()) {
            File newSegments = new File(root, "segments.new");
            if (newSegments.exists()) {
                newSegments.renameTo(segments);
            }
        }

        try {
            luceneDirectory = LuceneDirectory.open(root, new SingleInstanceLockFactory());
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to create LuceneDirectory: " + root, e);
        }
        ZimbraLog.index.info("Lucene index opened dir=%s", root);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("mbox", mailbox.getId()).add("dir", luceneDirectory).toString();
    }

    /**
     * Deletes this index completely.
     */
    @Override
    public synchronized void deleteIndex() throws IOException {
        assert(writerRef == null);
        ZimbraLog.index.debug("Deleting index %s", luceneDirectory);
        readersCache.remove(this);

        String[] files;
        try {
            files = luceneDirectory.listAll();
        } catch (NoSuchDirectoryException ignore) {
            return;
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to delete index: %s", luceneDirectory, e);
            return;
        }

        for (String file : files) {
            luceneDirectory.deleteFile(file);
        }
    }

    /**
     * Removes from cache.
     */
    @Override
    public void evict() {
        readersCache.remove(this);
    }

    /**
     * Caller is responsible for calling {@link Searcher#close()} before allowing it to go out of scope
     * (otherwise a RuntimeException will occur).
     *
     * @return A {@link Searcher} for this index.
     * @throws IOException if opening an {@link IndexReader} failed
     */
    @Override
    public Searcher openSearcher() throws IOException {
        return new SearcherImpl(getIndexReaderRef());
    }

    private IndexReader openIndexReader(boolean tryRepair) throws IOException {
        try {
            return IndexReader.open(luceneDirectory, null, true,
                    LC.zimbra_index_lucene_term_index_divisor.intValue());
        } catch (CorruptIndexException e) {
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexReader(false);
        } catch (AssertionError e) {
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexReader(false);
        }
    }

    private IndexWriter openIndexWriter(IndexWriterConfig.OpenMode mode, boolean tryRepair) throws IOException {
        try {
            IndexWriter writer = new IndexWriter(luceneDirectory, getWriterConfig().setOpenMode(mode)) {
                /**
                 * Redirect Lucene's logging to ZimbraLog.
                 */
                @Override
                public void message(String message) {
                    ZimbraLog.index.debug("IW: %s", message);
                }
            };
            if (ZimbraLog.index.isDebugEnabled()) {
                // Set a dummy PrintStream, otherwise Lucene suppresses logging.
                writer.setInfoStream(new PrintStream(new NullOutputStream()));
            }
            return writer;
        } catch (AssertionError e) {
            unlockIndexWriter();
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexWriter(mode, false);
        } catch (CorruptIndexException e) {
            unlockIndexWriter();
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexWriter(mode, false);
        }
    }

    private synchronized <T extends Throwable> void repair(T ex) throws T {
        ZimbraLog.index.error("Index corrupted", ex);
        LuceneIndexRepair repair = new LuceneIndexRepair(luceneDirectory);
        try {
            if (repair.repair() > 0) {
                ZimbraLog.index.info("Index repaired, re-indexing is recommended.");
            } else {
                ZimbraLog.index.warn("Unable to repair, re-indexing is required.");
                throw ex;
            }
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to repair, re-indexing is required.", e);
            throw ex;
        }
    }

    private void unlockIndexWriter() {
        try {
            IndexWriter.unlock(luceneDirectory);
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to unlock IndexWriter %s", this, e);
        }
    }

    /**
     * Caller is responsible for calling {@link IndexReaderRef#dec()} before
     * allowing it to go out of scope (otherwise a RuntimeException will occur).
     *
     * @return A {@link IndexReaderRef} for this index.
     * @throws IOException
     */
    private synchronized IndexReaderRef getIndexReaderRef() throws IOException {
        IndexReaderRef ref = readersCache.get(this);
        if (ref != null) {
            if (ref.isStale()) {
                IndexReader oldReader = ref.get();
                IndexReader newReader;
                try {
                    newReader = oldReader.reopen();
                    if (oldReader != newReader) { // reader changed, must close old one
                        ZimbraLog.search.debug("Reopened IndexReader");
                        readersCache.remove(this); // ref--
                        ref.dec();
                        READER_THROTTLE.acquireUninterruptibly();
                        ref = new IndexReaderRef(this, newReader); // ref = 1
                        readersCache.put(this, ref); // ref++
                    }
                } catch (IOException e) {
                    ZimbraLog.search.debug("Failed to reopen IndexReader", e);
                    readersCache.remove(this);
                    ref.dec();
                    ref = null;
                }
            }
            return ref;
        }

        READER_THROTTLE.acquireUninterruptibly();
        IndexReader reader = null;
        try {
            reader = openIndexReader(true);
        } catch (IOException e) {
            // Handle the special case of trying to open a not-yet-created index, by opening for write and immediately
            // closing. Index directory should get initialized as a result.
            if (isEmptyDirectory(luceneDirectory.getDirectory())) {
                // create an empty index
                IndexWriter writer = new IndexWriter(luceneDirectory,
                        getWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE));
                writer.close();
                reader = openIndexReader(false);
            } else {
                throw e;
            }
        } finally {
            if (reader == null) {
                READER_THROTTLE.release();
            }
        }

        ref = new IndexReaderRef(this, reader); // ref = 1
        readersCache.put(this, ref); // ref++
        return ref;
    }

    /**
     * Check to see if it is OK for us to create an index in the specified directory.
     *
     * @param dir index directory
     * @return TRUE if the index directory is empty or doesn't exist,
     *         FALSE if the index directory exists and has files in it or if we cannot list files in the directory
     */
    private boolean isEmptyDirectory(File dir) {
        if (!dir.exists()) { // dir doesn't even exist yet.  Create the parents and return true
            dir.mkdirs();
            return true;
        }

        // Empty directory is okay, but a directory with any files implies index corruption.
        File[] files = dir.listFiles();

        // if files is null here, we are likely running into file permission issue
        if (files == null) {
            ZimbraLog.index.warn("Could not list files in directory %s", dir.getAbsolutePath());
            return false;
        }

        int num = 0;
        for (File file : files) {
            String fname = file.getName();
            if (file.isDirectory() && (fname.equals(".") || fname.equals(".."))) {
                continue;
            }
            num++;
        }
        return (num <= 0);
    }

    @Override
    public synchronized Indexer openIndexer() throws IOException {
        if (writerRef != null) {
            writerRef.inc();
        } else {
            WRITER_THROTTLE.acquireUninterruptibly();
            try {
                writerRef = openWriter();
            } finally {
                if (writerRef == null) {
                    WRITER_THROTTLE.release();
                }
            }
        }
        return new IndexerImpl(writerRef);
    }

    private IndexWriterRef openWriter() throws IOException {
        assert(Thread.holdsLock(this));

        IndexWriter writer;
        try {
            writer = openIndexWriter(IndexWriterConfig.OpenMode.APPEND, true);
        } catch (IOException e) {
            // the index (the segments* file in particular) probably didn't exist
            // when new IndexWriter was called in the try block, we would get a
            // FileNotFoundException for that case. If the directory is empty,
            // this is the very first index write for this this mailbox (or the
            // index might be deleted), the FileNotFoundException is benign.
            if (isEmptyDirectory(luceneDirectory.getDirectory())) {
                writer = openIndexWriter(IndexWriterConfig.OpenMode.CREATE, false);
            } else {
                throw e;
            }
        }
        return new IndexWriterRef(this, writer);
    }

    private synchronized void commitWriter() throws IOException {
        assert(writerRef != null);

        ZimbraLog.index.debug("Commit IndexWriter");

        MergeTask task = new MergeTask(writerRef);
        if (MailboxIndex.isIndexThread()) { // batch index running in background
            task.exec();
        } else { // catch-up index running in foreground
            boolean success = false;
            try {
                try {
                    writerRef.get().commit();
                } catch (CorruptIndexException e) {
                    try {
                        writerRef.get().close(false);
                    } catch (Throwable ignore) {
                    }
                    repair(e);
                    throw e; // fail to commit regardless of the repair
                } catch (AssertionError e) {
                    try {
                        writerRef.get().close(false);
                    } catch (Throwable ignore) {
                    }
                    writerRef.get().close(false);
                    repair(e);
                    throw e; // fail to commit regardless of the repair
                }
                mailbox.index.submit(task); // merge must run in background
                success = true;
            } catch (RejectedExecutionException e) {
                ZimbraLog.index.warn("Skipping merge because all index threads are busy");
            } finally {
                if (!success) {
                    writerRef.dec();
                }
            }
        }
    }

    /**
     * Called by {@link IndexWriterRef#dec()}. Can be called by the thread that opened the writer or the merge thread.
     */
    private synchronized void closeWriter() {
        if (writerRef == null) {
            return;
        }

        ZimbraLog.index.debug("Close IndexWriter");

        try {
            writerRef.get().close(false); // ignore phantom pending merges
        } catch (CorruptIndexException e) {
            try {
                repair(e);
            } catch (CorruptIndexException ignore) {
            }
        } catch (AssertionError e) {
            repair(e);
        } catch (IOException e) {
            ZimbraLog.index.error("Failed to close IndexWriter", e);
        } finally {
            unlockIndexWriter();
            WRITER_THROTTLE.release();
            writerRef = null;
        }
    }

    /**
     * Called by {@link IndexReaderRef#dec()}. Can be called by the thread that opened the reader or the cache sweeper
     * thread.
     */
    void closeReader(IndexReader reader) {
        ZimbraLog.search.debug("Close IndexReader");

        try {
            reader.close();
        } catch (IOException e) {
            ZimbraLog.search.warn("Failed to close IndexReader", e);
        } finally {
            READER_THROTTLE.release();
        }
    }

    /**
     * Run a sanity check for the index. Callers are responsible to make sure the index is not opened by any writer.
     *
     * @param out info stream where messages should go. If null, no messages are printed.
     * @return true if no problems were found, otherwise false
     * @throws IOException failed to verify, but it doesn't necessarily mean the index is corrupted.
     */
    @Override
    public boolean verify(PrintStream out) throws IOException {
        CheckIndex check = new CheckIndex(luceneDirectory);
        if (out != null) {
            check.setInfoStream(out);
        }
        CheckIndex.Status status = check.checkIndex();
        return status.clean;
    }

    /**
     * Only one background thread that holds the lock may process a merge for the given writer. Other concurrent
     * attempts simply skip the merge.
     */
    private static final class MergeScheduler extends SerialMergeScheduler {
        private AtomicReference<Thread> lock = new AtomicReference<Thread>();

        /**
         * Try to hold the lock.
         *
         * @return true if the lock is held, false the lock is currently held by the other thread.
         */
        boolean tryLock() {
            return lock.compareAndSet(null, Thread.currentThread());
        }

        void release() {
            lock.compareAndSet(Thread.currentThread(), null);
        }

        /**
         * Skip the merge unless the lock is held.
         */
        @Override
        public void merge(IndexWriter writer) throws CorruptIndexException, IOException {
            if (lock.get() == Thread.currentThread()) {
                super.merge(writer);
            }
        }

        /**
         * Removes the Thread-to-IndexWriter reference.
         */
        @Override
        public void close() {
            super.close();
            lock.set(null);
        }
    }

    /**
     * In order to minimize delay caused by merges, merges are processed only in background threads. Writers triggered
     * by batch threshold or search commit the changes before processing merges, so that the changes are available to
     * readers without long delay that merges likely cause. Merge threads don't block other writer threads running in
     * foreground. Another indexing using the same writer may start even while the merge is in progress.
     */
    private final class MergeTask extends MailboxIndex.IndexTask {
        private final IndexWriterRef ref;

        MergeTask(IndexWriterRef ref) {
            super(ref.getIndex().mailbox);
            this.ref = ref;
        }

        @Override
        public void exec() throws IOException {
            IndexWriter writer = ref.get();
            try {
                if (((MergeScheduler) writer.getConfig().getMergeScheduler()).tryLock()) {
                    int dels = writer.maxDoc() - writer.numDocs();
                    if (dels >= LC.zimbra_index_max_pending_deletes.intValue()) {
                        ZimbraLog.index.debug("Expunge deletes %d", dels);
                        writer.expungeDeletes();
                    }
                    writer.maybeMerge();
                } else {
                    ZimbraLog.index.debug("Merge is in progress by other thread");
                }
            } catch (CorruptIndexException e) {
                try {
                    writer.close(false);
                } catch (Throwable ignore) {
                }
                repair(e);
            } catch (AssertionError e) {
                try {
                    writer.close(false);
                } catch (Throwable ignore) {
                }
                repair(e);
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to merge IndexWriter", e);
            } finally {
                ((MergeScheduler) ref.get().getConfig().getMergeScheduler()).release();
                ref.dec();
            }
        }
    }

    private IndexWriterConfig getWriterConfig() {
        IndexWriterConfig config = new IndexWriterConfig(VERSION, mailbox.index.getAnalyzer());
        config.setMergeScheduler(new MergeScheduler());
        config.setMaxBufferedDocs(LC.zimbra_index_lucene_max_buffered_docs.intValue());
        config.setRAMBufferSizeMB(LC.zimbra_index_lucene_ram_buffer_size_kb.intValue() / 1024.0);
        if (LC.zimbra_index_lucene_merge_policy.booleanValue()) {
            LogDocMergePolicy policy = new LogDocMergePolicy();
            config.setMergePolicy(policy);
            policy.setUseCompoundFile(LC.zimbra_index_lucene_use_compound_file.booleanValue());
            policy.setMergeFactor(LC.zimbra_index_lucene_merge_factor.intValue());
            policy.setMinMergeDocs(LC.zimbra_index_lucene_min_merge.intValue());
            if (LC.zimbra_index_lucene_max_merge.intValue() != Integer.MAX_VALUE) {
                policy.setMaxMergeDocs(LC.zimbra_index_lucene_max_merge.intValue());
            }
        } else {
            LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
            config.setMergePolicy(policy);
            policy.setUseCompoundFile(LC.zimbra_index_lucene_use_compound_file.booleanValue());
            policy.setMergeFactor(LC.zimbra_index_lucene_merge_factor.intValue());
            policy.setMinMergeMB(LC.zimbra_index_lucene_min_merge.intValue() / 1024.0);
            if (LC.zimbra_index_lucene_max_merge.intValue() != Integer.MAX_VALUE) {
                policy.setMaxMergeMB(LC.zimbra_index_lucene_max_merge.intValue() / 1024.0);
            }
        }
        return config;
    }

    public static ZimbraQueryResults search(ZimbraQuery zq) throws ServiceException {
        SearchParams params = zq.getParams();
        ZimbraLog.search.debug("query: %s", params.getQueryStr());

        // handle special-case Task-only sorts: convert them to a "normal sort"
        //     and then re-sort them at the end
        // FIXME - this hack (converting the sort) should be able to go away w/ the new SortBy
        //         implementation, if the lower-level code was modified to use the SortBy.Criterion
        //         and SortBy.Direction data (instead of switching on the SortBy itself)
        //         We still will need this switch so that we can wrap the
        //         results in the ReSortingQueryResults
        boolean isTaskSort = false;
        boolean isLocalizedSort = false;
        SortBy originalSort = params.getSortBy();
        switch (originalSort) {
            case TASK_DUE_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_DUE_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_STATUS_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_STATUS_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_PERCENT_COMPLETE_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_PERCENT_COMPLETE_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                isLocalizedSort = true;
                break;
        }

        ZimbraQueryResults results = zq.execute();
        if (isTaskSort) {
            results = new ReSortingQueryResults(results, originalSort, null);
        }
        if (isLocalizedSort) {
            results = new ReSortingQueryResults(results, originalSort, params);
        }
        return results;
    }

    public static final class Factory implements IndexStore.Factory {
        private final IndexReadersCache readersCache;

        public Factory() {
            BooleanQuery.setMaxClauseCount(LC.zimbra_index_lucene_max_terms_per_query.intValue());
            readersCache = new IndexReadersCache(
                    LC.zimbra_index_reader_cache_size.intValue(),
                    LC.zimbra_index_reader_cache_ttl.longValue() * 1000L,
                    LC.zimbra_index_reader_cache_sweep_frequency.longValue() * 1000L);
        }

        @Override
        public LuceneIndex getInstance(Mailbox mbox) throws ServiceException {
            return new LuceneIndex(mbox, readersCache);
        }

        @Override
        public void destroy() {
            readersCache.shutdown();
        }
    }

    private static final class IndexerImpl implements Indexer {
        private final IndexWriterRef writer;

        IndexerImpl(IndexWriterRef writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            writer.index.commitWriter();
            writer.getIndex().readersCache.stale(writer.index); // let the reader reopen
        }

        /**
         * Adds the list of documents to the index.
         * <p>
         * If the index status is stale, delete the stale documents first, then add new documents. If the index status
         * is deferred, we are sure that this item is not already in the index, and so we can skip the check-update step.
         */
        @Override
        public synchronized void addDocument(MailItem item, List<IndexDocument> docs) throws IOException {
            if (docs == null || docs.isEmpty()) {
                return;
            }

            for (IndexDocument doc : docs) {
                // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                synchronized (doc) {
                    doc.removeSortSubject();
                    doc.addSortSubject(item.getSortSubject());

                    doc.removeSortName();
                    doc.addSortName(item.getSortSender());

                    doc.removeMailboxBlobId();
                    doc.addMailboxBlobId(item.getId());

                    // If this doc is shared by multi threads, then the date might just be wrong,
                    // so remove and re-add the date here to make sure the right one gets written!
                    doc.removeSortDate();
                    doc.addSortDate(item.getDate());

                    doc.removeSortSize();
                    doc.addSortSize(item.getSize());

                    switch (item.getIndexStatus()) {
                        case STALE:
                        case DONE: // for partial re-index
                            Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(item.getId()));
                            writer.get().updateDocument(term, doc.toDocument());
                            break;
                        case DEFERRED:
                            writer.get().addDocument(doc.toDocument());
                            break;
                        default:
                            assert false : item.getIndexId();
                    }
                }
            }
        }

        /**
         * Deletes documents.
         * <p>
         * The document count may be more than you expect here, the document may already be deleted and just not be
         * optimized out yet -- some Lucene APIs (e.g. docFreq) will still return the old count until the indexes are
         * optimized.
         */
        @Override
        public void deleteDocument(List<Integer> ids) throws IOException {
            for (Integer id : ids) {
                Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, id.toString());
                writer.get().deleteDocuments(term);
                ZimbraLog.index.debug("Deleted documents id=%d", id);
            }
        }
    }

    private static final class SearcherImpl implements Searcher {
        private final IndexSearcher searcher;
        private final IndexReaderRef reader;

        SearcherImpl(IndexReaderRef reader) {
            this.reader = reader;
            searcher = new IndexSearcher(reader.get());
        }

        @Override
        public void close() throws IOException {
            try {
                searcher.close();
            } finally {
                reader.dec();
            }
        }

        @Override
        public List<Integer> search(Query query, Filter filter, Sort sort, int max) throws IOException {
            TopDocs docs;
            if (sort != null) {
                docs = searcher.search(query, filter, max, sort);
            } else {
                docs = searcher.search(query, filter, max);
            }
            List<Integer> result = new ArrayList<Integer>(docs.scoreDocs.length);
            for (ScoreDoc hit : docs.scoreDocs) {
                result.add(hit.doc);
            }
            return result;
        }

        /**
         * TODO: This may include terms associated with deleted documents.
         */
        @Override
        public TermEnum getTerms(Term term) throws IOException {
            return reader.get().terms(term);
        }

        @Override
        public Document getDocument(int id) throws IOException {
            return searcher.doc(id);
        }

        @Override
        public int getCount(Term term) throws IOException {
            return searcher.docFreq(term);
        }

        @Override
        public int getTotal() throws IOException {
            return searcher.maxDoc();
        }
    }

    /**
     * {@link IndexWriter} wrapper that supports a reference counter.
     */
    private final class IndexWriterRef {
        private final LuceneIndex index;
        private final IndexWriter writer;
        private final AtomicInteger count = new AtomicInteger(1); // ref counter

        IndexWriterRef(LuceneIndex index, IndexWriter writer) {
            this.index = index;
            this.writer = writer;
        }

        IndexWriter get() {
            return writer;
        }

        LuceneIndex getIndex() {
            return index;
        }

        void inc() {
            count.incrementAndGet();
        }

        void dec() {
            if (count.decrementAndGet() <= 0) {
                index.closeWriter();
            }
        }

    }

}

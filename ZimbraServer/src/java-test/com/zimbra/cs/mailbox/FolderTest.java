/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailAddress;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;

/**
 * Unit test for {@link Folder}.
 */
public class FolderTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning prov = new MockProvisioning();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        MailboxManager.setInstance(null);
        MailboxIndex.startup();

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
    }

    private int checkMODSEQ(String msg, Mailbox mbox, int folderId, int lastMODSEQ) throws Exception {
        int modseq = mbox.getFolderById(null, folderId).getImapMODSEQ();
        Assert.assertTrue("modseq change after " + msg, modseq != lastMODSEQ);
        return modseq;
    }

    @Test
    public void testImapMODSEQ() throws Exception {
        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // initial state: empty folder
        Folder f = mbox.createFolder(null, "foo", (byte) 0, MailItem.Type.MESSAGE);
        int folderId = f.getId(), modseq = f.getImapMODSEQ();

        // add a message to the folder
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setFlags(Flag.BITMASK_UNREAD);
        int msgId = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt).getId();
        modseq = checkMODSEQ("message add", mbox, folderId, modseq);

        // mark message read
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_UNREAD, false);
        modseq = checkMODSEQ("mark read", mbox, folderId, modseq);

        // move message out of folder
        mbox.move(null, msgId, MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);
        modseq = checkMODSEQ("move msg out", mbox, folderId, modseq);

        // move message back into folder
        mbox.move(null, msgId, MailItem.Type.MESSAGE, folderId);
        modseq = checkMODSEQ("move msg in", mbox, folderId, modseq);

        // mark message answered
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_REPLIED, true);
        modseq = checkMODSEQ("mark answered", mbox, folderId, modseq);

        // move virtual conversation out of folder
        mbox.move(null, -msgId, MailItem.Type.CONVERSATION, Mailbox.ID_FOLDER_INBOX);
        modseq = checkMODSEQ("move vconv out", mbox, folderId, modseq);

        // move virtual conversation back into folder
        mbox.move(null, -msgId, MailItem.Type.CONVERSATION, folderId);
        modseq = checkMODSEQ("move vconv in", mbox, folderId, modseq);

        // add a draft reply to the message (don't care about modseq change)
        ParsedMessage pm = new ParsedMessage(ThreaderTest.getSecondMessage(), false);
        mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, Integer.toString(msgId), MailSender.MSGTYPE_REPLY, null, null, 0L);
        modseq = mbox.getFolderById(null, folderId).getImapMODSEQ();

        // move conversation out of folder
        int convId = mbox.getMessageById(null, msgId).getConversationId();
        mbox.move(null, convId, MailItem.Type.CONVERSATION, Mailbox.ID_FOLDER_INBOX);
        modseq = checkMODSEQ("move conv out", mbox, folderId, modseq);

        // move conversation back into folder
        mbox.move(null, convId, MailItem.Type.CONVERSATION, folderId);
        modseq = checkMODSEQ("move conv in", mbox, folderId, modseq);

        // tag message
        int tagId = mbox.createTag(null, "taggity", (byte) 3).getId();
        modseq = mbox.getFolderById(null, folderId).getImapMODSEQ();
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tagId, true);
        modseq = checkMODSEQ("add tag", mbox, folderId, modseq);

        // rename tag
        mbox.rename(null, tagId, MailItem.Type.TAG, "blaggity", Mailbox.ID_AUTO_INCREMENT);
        modseq = checkMODSEQ("rename tag", mbox, folderId, modseq);

        // untag message
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tagId, false);
        modseq = checkMODSEQ("remove tag", mbox, folderId, modseq);

        // retag message
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tagId, true);
        modseq = checkMODSEQ("re-add tag", mbox, folderId, modseq);

        // delete tag
        mbox.delete(null, tagId, MailItem.Type.TAG);
        modseq = checkMODSEQ("tag delete", mbox, folderId, modseq);

        // hard delete message
        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        modseq = checkMODSEQ("hard delete", mbox, folderId, modseq);
    }

    @Test
    public void updateAddressCountOnSoftDelete() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.createFolder(null, "/Contacts/test", (byte) 0, MailItem.Type.CONTACT);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(3, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, folder.getId(), MailItem.Type.FOLDER, Mailbox.ID_FOLDER_TRASH); // soft-delete
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, folder.getId(), MailItem.Type.FOLDER, Mailbox.ID_FOLDER_CONTACTS); // soft-recover
        Assert.assertEquals(3, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        conn.closeQuietly();
    }

    @Test
    public void updateAddressCountOnHardDelete() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder1 = mbox.createFolder(null, "/Contacts/test1", (byte) 0, MailItem.Type.CONTACT);
        Folder folder2 = mbox.createFolder(null, "/Contacts/test2", (byte) 0, MailItem.Type.CONTACT);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), folder1.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder1.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder1.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder2.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder2.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder2.getId(), null);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(6, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.delete(null, folder1.getId(), MailItem.Type.FOLDER); // hard-delete from non Trash
        Assert.assertEquals(3, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, folder2.getId(), MailItem.Type.FOLDER, Mailbox.ID_FOLDER_TRASH); // soft-delete
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.delete(null, folder2.getId(), MailItem.Type.FOLDER); // hard-delete from Trash
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        conn.closeQuietly();
    }

    @Test
    public void updateAddressCountOnEmptyFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder1 = mbox.createFolder(null, "/Contacts/test1", (byte) 0, MailItem.Type.CONTACT);
        Folder folder2 = mbox.createFolder(null, "/Contacts/test2", (byte) 0, MailItem.Type.CONTACT);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), folder1.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder1.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder1.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder2.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder2.getId(), null);
        mbox.createContact(null, new ParsedContact(fields), folder2.getId(), null);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(6, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.emptyFolder(null, folder1.getId(), true); // empty the folder
        Assert.assertEquals(3, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, folder2.getId(), MailItem.Type.FOLDER, Mailbox.ID_FOLDER_TRASH); // soft-delete
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, true); // empty Trash
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        conn.closeQuietly();
    }

}

package com.zimbra.cs.db;

import java.sql.SQLException;

import com.zimbra.common.util.ZimbraLog;
/**
 * Abstract class used to wrap a SQL execute command with retry logic 
 */
public abstract class AbstractRetry<T> {
    
    /**
     * Child class must implement SQL execute in this method
     * @throws SQLException
     */
    public abstract ExecuteResult<T> execute() throws SQLException;

    protected boolean retryException(SQLException sqle)
    {
        return Db.errorMatches(sqle, Db.Error.BUSY) || Db.errorMatches(sqle, Db.Error.LOCKED);
    }

    private static final int RETRY_LIMIT = 10;
    private static final int RETRY_DELAY = 1000;
    
    private static int retryCount = 0;
    public static int getTotalRetries() {
        return retryCount;
    }
    
    private synchronized void incrementTotalRetries() {
        retryCount++;
    }

    /**
     * Invoke the execute method inside a retry loop
     * @throws SQLException
     */
    public ExecuteResult<T> doRetry() throws SQLException {
        int tries = 0;
        SQLException sqle = null;
        while (tries < RETRY_LIMIT) {
            if (Thread.interrupted()) {
                //necessary for SQLite since native code in driver does not respond to interrupts
                //this keeps us from starting to invoke a new command after thread has been interrupted
                throw new SQLException("Interrupted during I/O, likely due to connection down");
            }
            try {
                return execute();
            } catch (SQLException e) {
                if (retryException(e)) {
                    sqle = e;
                    tries++;
                    ZimbraLog.dbconn.warn("retrying connection attempt:"+tries+" due to possibly recoverable exception: ",e);
                    incrementTotalRetries();
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new SQLException("DB retry gave up after "+tries+" attempts.",sqle);
    }

}

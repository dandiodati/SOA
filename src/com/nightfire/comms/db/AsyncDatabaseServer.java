/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 * $Header: //comms/main/com/nightfire/comms/db/AsyncDatabaseServer.java#3 $
 */

package com.nightfire.comms.db;


import java.util.*;
import java.text.*;
import java.sql.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.db.portability.DBPortabilityLayer;
import com.nightfire.spi.common.communications.*;


/**
 * A communications server providing dequeueing functionality.
 * The queue database table must have the following columns:
 *
 *    id             NUMBER(10, 0)   NOT NULL,
 *    status         NUMBER(2, 0)    DEFAULT 0 NOT NULL,
 *    retryCount     NUMBER(2, 0),
 *    timestamp      DATE            NOT NULL,
 *    message        LONG            NOT NULL,
 *    CONSTRAINT queueId PRIMARY KEY (id)
 *
 * NOTE:  If this component is configured to not delete the processed rows, it is probably also
 *        a good idea to create an index on the 'status' and 'retryCount' columns to prevent
 *        inefficient full table scans.
 */
public class AsyncDatabaseServer extends PollComServerBase
{
    /**
     * Property naming the database table providing the persistent queue.
     */
    public static final String QUEUE_TABLE_NAME_PROP = "QUEUE_TABLE_NAME";

    /**
     * Property indicating the type of the column containing the queue value (One of: TEXT, BINARY).
     */
    public static final String VALUE_COLUMN_TYPE_PROP = "VALUE_COLUMN_TYPE";

    /**
     * Property indicating the number of times to retry processing of a queued message.
     */
    public static final String MAX_RETRY_COUNT_PROP = "MAX_RETRY_COUNT";

    /**
     * Property indicating whether successfully-processed queue items should
     * be deleted or not.
     */
    public static final String DELETE_PROCESSED_ITEMS_FLAG_PROP = "DELETE_PROCESSED_ITEMS_FLAG";


    // Status value that indicates that a queue item hasn't yet been processed.
    public static final int UNPROCESSED_STATUS_VALUE = 0;

    // Status value that indicates that a queue item has been successfully processed.
    public static final int PROCESSED_STATUS_VALUE = 1;

    // Status value that indicates that a queue item processing failed due to data errors.
    public static final int DATA_ERROR_STATUS_VALUE = -1;

    // Status value that indicates that a queue item processing failed due to system errors.
    public static final int SYSTEM_ERROR_STATUS_VALUE = -2;


    // VALUE_COLUMN_TYPE values.
    public static final String COLUMN_TYPE_TEXT   = "TEXT";
    public static final String COLUMN_TYPE_BINARY = "BINARY";

    // Required column names in database table supporting queue.
    public static final String ID_COLUMN_NAME          = "Id";
    public static final String STATUS_COLUMN_NAME      = "Status";
    public static final String RETRY_COUNT_COLUMN_NAME = "retryCount";
    public static final String TIMESTAMP_COLUMN_NAME   = "Timestamp";
    public static final String MESSAGE_COLUMN_NAME     = "Message";

    // Format of timestamp strings.
    public static final String TIMESTAMP_FORMAT = "yyyy MM dd HH:mm:ss";


    /**
     * Constructor
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails
     */
    public AsyncDatabaseServer ( String key, String type ) throws ProcessingException
    {
        super( key, type );
    }


    /**
     * Load the CID-specific configuration properties from the database.
     * (Implicit key is thread-local CID set on the current CustomerContext.)  The
     * object returned is a leaf class-specific configuration container.
     * This method should overridden in the leaf-class, and shouldn't be
     * called directly.  Instead, leaf-classes should call getConfiguration(),
     * which will call this method if it can't find a previously-loaded and
     * cached configuration object.
     *
     * @return  The customer-specific configuration, or null if unavailable.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    protected Object loadConfiguration ( ) throws FrameworkException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating async-database-server." );

        // Re-initialize properties so that we will load any per-customer ones as
        // specified by the CID on the thread-local customer context.
        initialize( key, type );

        PerCIDConfiguration pcc = new PerCIDConfiguration( );

        // Configure the server using the specific properties.
        StringBuffer errBuffer = new StringBuffer( );

        pcc.tableName = getRequiredPropertyValue( QUEUE_TABLE_NAME_PROP, errBuffer );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Name of database table providing queue [" + pcc.tableName + "]." );

        pcc.valueColumnType = getPropertyValue( VALUE_COLUMN_TYPE_PROP );

        if ( !StringUtils.hasValue(pcc.valueColumnType) )
            pcc.valueColumnType = COLUMN_TYPE_TEXT;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Queue message is of type [" + pcc.valueColumnType + "]." );

        // Default retry-count is none (0).
        pcc.maxRetryCount = getIntegerProperty( MAX_RETRY_COUNT_PROP, DEFAULT_MAX_RETRY_COUNT );

        if ( pcc.maxRetryCount < 0 )
            pcc.maxRetryCount = 0;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Maximum retry count [" + pcc.maxRetryCount + "]." );

        String temp = getPropertyValue( DELETE_PROCESSED_ITEMS_FLAG_PROP );

        try
        {
            if ( StringUtils.hasValue(temp) && (StringUtils.getBoolean(temp) == false) )
                pcc.deleteProcessedItems = false;
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Invalid value for property ["
                                           + DELETE_PROCESSED_ITEMS_FLAG_PROP + "]:\n" + e.toString()
                                           + "\n" + errBuffer.toString() );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Delete successfully-processed queue items? ["
                   + pcc.deleteProcessedItems + "]." );

        if ( errBuffer.length() > 0 )
        {
            throw new ProcessingException( errBuffer.toString() );
        }

        pcc.timestampFormatter = new SimpleDateFormat( TIMESTAMP_FORMAT );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Done initializing the async-database-server." );

        return pcc;
    }


    /**
     * Called by Timer object when its timer expires.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    protected void processRequests ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS,"Processing timer expiration event at [" + DateUtils.getCurrentTime() + "]." );

        // Create the SQL statements supporting the queue based on table name.
        createSQLStatements( );

        // Process any request messages.
        try
        {
            checkQueue( );
        }
        finally
        {
            // Reset customer context.
            try
            {
                CustomerContext.getInstance().cleanup( );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );

                throw new ProcessingException( e );
            }
        }
    }


    /**
     * Poll database table for messages to process and send them on for further processing.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private void checkQueue ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.IO_DATA))
            Debug.log(Debug.IO_DATA, "Checking the queue for messages to process." );

        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        String cid = null;

        try 
        {
            cid = CustomerContext.getInstance().getCustomerID( );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }

        // Process queued messages in retry-count based groups, from the largest retry-count
        // value (messages whose processing has failed the most times) to brand new messages
        // that haven't yet been processed.
        for ( int Ix = pcc.maxRetryCount;  Ix >= 0;  Ix -- )
        {
            processQueueEntriesWithStatus( cid, Ix );
        }

        // Clean up any database resources.
        closePreparedStatements( );

        releaseDBConnection( );
    }


    /**
     * Poll database table for messages to process and send them on for further processing.
     *
     * @param  cid  The current customer-id.
     * @param  retryCount  Retry count value to process.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private void processQueueEntriesWithStatus ( String cid, int retryCount ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Processing all queue entries at retry-count level [" + retryCount + "]." );

        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        QueueEntry qe = null;

        for ( int count = 0;  true;  count ++ )
        {
            try
            {
                // Get the next available queue item at the given retry-count level.
                qe = getQueueEntry( retryCount );

                // If none were found, we're done.
                if ( qe == null )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "Processed [" + count
                               + "] queue entries at retry-count level [" + retryCount + "]." );

                    break;
                }

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Processing queue entry [" + qe.describe() + "]." );

                // Put the column values in the header passed to the processing call.
                XMLMessageGenerator header = new XMLMessageGenerator( "QueueItem" );

                header.setValue( ID_COLUMN_NAME, String.valueOf(qe.id) );
                header.setValue( STATUS_COLUMN_NAME, String.valueOf(qe.status) );
                header.setValue( RETRY_COUNT_COLUMN_NAME, String.valueOf(qe.retryCount) );
                header.setValue( TIMESTAMP_COLUMN_NAME, pcc.timestampFormatter.format(qe.timestamp) );

                // Make appropriate call based on type of message.
                try
                {
                    if ( pcc.valueColumnType.equalsIgnoreCase(COLUMN_TYPE_BINARY) )
                    {
                        process( header.generate(), (byte[])(qe.message) );
                    }
                    else
                    {
                        process( header.generate(), (String)(qe.message) );
                    }
                }
                finally
                {
                    CustomerContext.getInstance().setCustomerID( cid );
                }


                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Queue entry [" + qe.describe() + "] has been successfully processed." );

                if ( pcc.deleteProcessedItems )
                {
                    deleteQueueEntry( qe );
                }
                else
                {
                    qe.status = PROCESSED_STATUS_VALUE;

                    updateQueueEntry( qe );
                }
            }
            catch ( Exception e )
            {
                Debug.log( Debug.ALL_ERRORS, "ERROR: Processing of queue item ["
                           + qe.describe() + "] failed:\n" + e.toString() );

                if ( e instanceof MessageException )
                    qe.status = DATA_ERROR_STATUS_VALUE;
                else
                    qe.status = SYSTEM_ERROR_STATUS_VALUE;

                // Increment the retry-count value to indicate that this queue item has been
                // unsuccessfully processed.
                qe.retryCount = qe.retryCount + 1;

                updateQueueEntry( qe );
            }
        }
    }


    /**
     * Get a single queue entry with the indicated status value.
     *
     * @param  retryCount  Retry-count level to get queued items for.
     *
     * @return  A queue entry, or null if none are available.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private QueueEntry getQueueEntry ( int retryCount ) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        QueueEntry queueItem = null;

        try
        {
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log( Debug.NORMAL_STATUS, "AsyncDatabaseServer: Executing SQL [" + pcc.selectSqlStatement
                       + "] using retry-count value [" + retryCount + "]." );

            PreparedStatement pstmt = getSelectStatement( );

            pstmt.setInt( 1, retryCount );

            ResultSet rs = pstmt.executeQuery( );
            
        	if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        		Debug.log(Debug.NORMAL_STATUS, "AsyncDatabaseServer: Finished Executing SQL..");


            if ( rs.next() )
            {
                int id = rs.getInt( ID_COLUMN_NAME );

                int status = rs.getInt( STATUS_COLUMN_NAME );

                retryCount = rs.getInt( RETRY_COUNT_COLUMN_NAME );

                java.util.Date timestamp = rs.getDate( TIMESTAMP_COLUMN_NAME );

                Object message = null;

                if ( pcc.valueColumnType.equalsIgnoreCase(COLUMN_TYPE_BINARY) )
                {
                    message = DBLOBUtils.getBLOB( rs, MESSAGE_COLUMN_NAME );
                }
                else
                {
                    message = DBLOBUtils.getCLOB( rs, MESSAGE_COLUMN_NAME );
                }

                queueItem = new QueueEntry( message, id, status, retryCount, timestamp );
            }

            rs.close( );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Could not select row from database table ["
                       + pcc.tableName + "]:\n" + e.toString() );
        }

        return queueItem;
    }


    /**
     * Update the status and retry-count values for the indicated queue entry.
     *
     * @param qe  Queue entry to update in the database table.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private void updateQueueEntry ( QueueEntry qe ) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Executing SQL [" + pcc.updateSqlStatement
                   + "] against queue entry [" + qe.describe() + "]." );

        try
        {
            PreparedStatement pstmt = getUpdateStatement( );

            pstmt.setInt( 1, qe.status );
            pstmt.setInt( 2, qe.retryCount );
            pstmt.setInt( 3, qe.id );

            int count = pstmt.executeUpdate( );

            DBConnectionPool.getInstance().commit( getDBConnection() );

            if(Debug.isLevelEnabled(Debug.DB_DATA))
                Debug.log( Debug.DB_DATA, "Number of queue rows updated [" + count + "]." );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Could not update row in database table ["
                       + pcc.tableName + "]:\n" + e.toString() );

            try
            {
                DBConnectionPool.getInstance().rollback( getDBConnection() );
            }
            catch ( Exception e2 )
            {
                Debug.log( Debug.ALL_ERRORS, e2.getMessage() );
            }
        }
    }


    /**
     * Delete the indicated queue entry.
     *
     * @param qe  Queue entry to delete from the database table.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private void deleteQueueEntry ( QueueEntry qe ) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Executing SQL [" + pcc.deleteSqlStatement
                   + "] against queue entry [" + qe.describe() + "]." );

        try
        {
            PreparedStatement pstmt = getDeleteStatement( );

            pstmt.setInt( 1, qe.id );

            int count = pstmt.executeUpdate( );

            DBConnectionPool.getInstance().commit( getDBConnection() );

            if(Debug.isLevelEnabled(Debug.DB_DATA))
                Debug.log( Debug.DB_DATA, "Number of rows deleted [" + count + "]." );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Could not delete row from database table ["
                       + pcc.tableName + "]:\n" + e.toString() );

            try
            {
                DBConnectionPool.getInstance().rollback( getDBConnection() );
            }
            catch ( Exception e2 )
            {
                Debug.log( Debug.ALL_ERRORS, e2.getMessage() );
            }
        }
    }


    /**
     * Get the prepared-statement associated with the SQL SELECT statement.
     *
     * @return  The prepared-statement.
     *
     *
     * @exception  Exception  Thrown if database connection can't be obtained,
     *                           or statement can't be created.
     */
    private PreparedStatement getSelectStatement ( ) throws Exception
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if ( pcc.selectPstmt == null )
            pcc.selectPstmt = getDBConnection().prepareStatement( pcc.selectSqlStatement );

        return pcc.selectPstmt;
    }


    /**
     * Get the prepared-statement associated with the SQL UPDATE statement.
     *
     * @return  The prepared-statement.
     *
     * @exception  Exception  Thrown if statement can't be created.
     */
    private PreparedStatement getUpdateStatement ( ) throws Exception
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if ( pcc.updatePstmt == null )
            pcc.updatePstmt = getDBConnection().prepareStatement( pcc.updateSqlStatement );

        return pcc.updatePstmt;
    }


    /**
     * Get the prepared-statement associated with the SQL DELETE statement.
     *
     * @return  The prepared-statement.
     *
     * @exception  Exception  Thrown if statement can't be created.
     */
    private PreparedStatement getDeleteStatement ( ) throws Exception
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if ( pcc.deletePstmt == null )
            pcc.deletePstmt = getDBConnection().prepareStatement( pcc.deleteSqlStatement );

        return pcc.deletePstmt;
    }


    /**
     * Close the given prepared statement object.
     */
    private void closePreparedStatements ( )
    {
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_STATUS, "Closing all open prepared-statements ..." );

        PerCIDConfiguration pcc = null;

        try
        {
            pcc = (PerCIDConfiguration)getConfiguration( true );
        }
        catch ( Exception e )
        {
            Debug.error( e.toString() );

            return;
        }

        if ( pcc.selectPstmt != null )
        {
            closePreparedStatement( pcc.selectPstmt );

           pcc.selectPstmt = null;
        }

        if ( pcc.updatePstmt != null )
        {
            closePreparedStatement( pcc.updatePstmt );

            pcc.updatePstmt = null;
        }

        if ( pcc.deletePstmt != null )
        {
            closePreparedStatement( pcc.deletePstmt );

            pcc.deletePstmt = null;
        }
    }


    /**
     * Close the given prepared statement object.
     *
     * @param  pstmt  The prepared statement object to close.
     */
    private void closePreparedStatement ( PreparedStatement pstmt )
    {
        if ( pstmt != null )
        {
            try
            {
                pstmt.close( );
            }
            catch ( SQLException sqle )
            {
                Debug.log( Debug.ALL_ERRORS, "ERROR: Couldn't close prepared-statement:\n"
                           + DBInterface.getSQLErrorMessage(sqle) );
            }
        }
    }


    /**
     * Release the database connection back to the pool.
     *
     * @return  A database connection.
     *
     * @exception  FrameworkException  Thrown if connection can't be obtained.
     */
    private Connection getDBConnection ( ) throws FrameworkException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if ( pcc.dbConn == null )
        {
            try
            {
                pcc.dbConn = DBConnectionPool.getInstance().acquireConnection( );
            }
            catch ( Exception e )
            {
                throw new FrameworkException( e.toString() );
            }
        }

        return pcc.dbConn;
    }


    /**
     * Release the database connection back to the pool.
     */
    private void releaseDBConnection ( )
    {
        try
        {
            PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

            if ( pcc.dbConn == null )
                return;

            DBConnectionPool.getInstance().releaseConnection( pcc.dbConn );

            pcc.dbConn = null;
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, e.toString() );
        }
    }


    /**
     * Create the SQL SELECT, UPDATE and DELETE statements used to process the queue.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private void createSQLStatements( ) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        // We've already done this if we have a DELETE statement.
        if ( pcc.deleteSqlStatement != null )
            return;

        StringBuffer sb = new StringBuffer( );

        /*
         * NOTE:  The "FOR UPDATE NOWAIT" clause at the end of the SELECT statement
         * is an Oracle-proprietary SQL construct.  It locks the row and prevents
         * another client from operating on the selected row until the transaction
         * containing the SELECT ends.
         */
        sb.append( "SELECT " );
        sb.append( ID_COLUMN_NAME );
        sb.append( ", " );
        sb.append( TIMESTAMP_COLUMN_NAME );
        sb.append( ", " );
        sb.append( STATUS_COLUMN_NAME );
        sb.append( ", " );
        sb.append( RETRY_COUNT_COLUMN_NAME );
        sb.append( ", " );
        sb.append( MESSAGE_COLUMN_NAME );
        sb.append( " FROM " );
        sb.append( pcc.tableName );
        sb.append( " WHERE " );
        sb.append( STATUS_COLUMN_NAME );
        sb.append( " IN ( " );
        sb.append( UNPROCESSED_STATUS_VALUE );
        sb.append( ", " );
        sb.append( SYSTEM_ERROR_STATUS_VALUE );
        sb.append( " ) AND ( " );
        sb.append( RETRY_COUNT_COLUMN_NAME );
        sb.append( " IS NULL OR " );
        sb.append( RETRY_COUNT_COLUMN_NAME );
        sb.append( " = ? ) ORDER BY " );
        sb.append( ID_COLUMN_NAME );
        sb.append( ", " );
        sb.append( STATUS_COLUMN_NAME );
        sb.append( " FOR UPDATE " );
        sb.append( DBPortabilityLayer.getNoRowLock() );

        pcc.selectSqlStatement = sb.toString( );

        sb = new StringBuffer( );

        sb.append( "UPDATE " );
        sb.append( pcc.tableName );
        sb.append( " SET " );
        sb.append( STATUS_COLUMN_NAME );
        sb.append( " = ?, " );
        sb.append( RETRY_COUNT_COLUMN_NAME );
        sb.append( " = ? WHERE " );
        sb.append( ID_COLUMN_NAME );
        sb.append( " = ?" );

        pcc.updateSqlStatement = sb.toString( );

        sb = new StringBuffer( );

        sb.append( "DELETE FROM " );
        sb.append( pcc.tableName );
        sb.append( " WHERE " );
        sb.append( ID_COLUMN_NAME );
        sb.append( " = ?" );

        pcc.deleteSqlStatement = sb.toString( );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        {
            Debug.log( Debug.SYSTEM_CONFIG, "Queue-item select SQL statement:\n" + pcc.selectSqlStatement
                                        + "\nQueue-item update SQL statement:\n" + pcc.updateSqlStatement
                                        + "\nQueue-item delete SQL statement:\n" + pcc.deleteSqlStatement );
        }
    }


    /**
     * Unit-test driver
     *
     * @param  args  String array containing the following items:
     *               [0] = db-name.
     *               [1] = db-user
     *               [2] = db-passwd.
     *               [3] = persistent property configuration key.
     *               [4] = persistent property configuration type.
     */
    public static void main ( String[] args )
    {
        if ( args.length < 5 )
        {
            System.err.println( "\n\nUSAGE: AsyncDatabaseServer <db-name> <db-user> <db-passwd> <key> <type>\n\n" );

            System.exit( -1 );
        }

        String dbName     = args[0];
        String dbUser     = args[1];
        String dbPassword = args[2];
        String key        = args[3];
        String type       = args[4];

        Debug.showLevels();
        Debug.enableAll();
        FrameworkException.showStackTrace( );

        try
        {
            DBInterface.initialize( dbName, dbUser, dbPassword );

            AsyncDatabaseServer server = new AsyncDatabaseServer( key, type );

            server.processRequests( );
        }
        catch ( Exception e )
        {
            System.err.println( e );

            e.printStackTrace( );
        }
    }


    /**
     * Class encapsulating one queue item.
     */
    private static class QueueEntry
    {
        public final Object message;
        public final int id;
        public int status;  // NOTE: Not final since we may want to modify it.
        public int retryCount;  // NOTE: Not final since we may want to modify it.
        public final java.util.Date timestamp;

        public QueueEntry ( Object message, int id, int status, int retryCount, java.util.Date timestamp )
        {
            this.message    = message;
            this.id         = id;
            this.status     = status;
            this.retryCount = retryCount;
            this.timestamp  = timestamp;
        }

        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Queue-item: ID [" );
            sb.append( id );
            sb.append( "], status [" );
            sb.append( status );
            sb.append( "], retry-count [" );
            sb.append( retryCount );
            sb.append( "], timestamp [" );
            sb.append( timestamp );
            sb.append( "]" );

            return( sb.toString() );
        }
    }


    // Used to contain configuration properties on a per-customer basis.
    private class PerCIDConfiguration
    {
        private com.nightfire.framework.timer.Timer timer;

        private String tableName;

        private String valueColumnType;

        private String selectSqlStatement;
        private String updateSqlStatement;
        private String deleteSqlStatement;

        private PreparedStatement selectPstmt;
        private PreparedStatement updatePstmt;
        private PreparedStatement deletePstmt;

        private Connection dbConn;

        private int maxRetryCount;

        private boolean deleteProcessedItems = true;

        private SimpleDateFormat timestampFormatter;
    }

    // Time (in seconds) to wait between database polls.
    private static final int DEFAULT_TIMER_WAIT_TIME = 60;

    // Default number of times to retry processing a queued item.
    private static final int DEFAULT_MAX_RETRY_COUNT = 0;
}

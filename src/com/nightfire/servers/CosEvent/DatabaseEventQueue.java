/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;
import java.io.*;
import java.sql.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.db.*;


/**
 * Event queue supporting persistent events via the database.
 * 
 * NOTE: The following usage is assumed for this EventQueue type:
 * 1.) Multiple supplier event-delivery threads may be concurrently 
 *     enqueueing events via the add() method.
 * 2.) A single thread, which is distinct from the supplier threads,
 *     will be dequeueing events via the hasNext(), next(), update() and
 *     noConsumersAvailable() methods.
 * 3.) The initialize() method is called before any other queuing operations occur.
 * 4.) The shutdown() method may be called at any point.
 */
public class DatabaseEventQueue implements EventQueue
{
    /**
     * Property giving the number of events to load from the database at a time.
     */
    public static final String LOAD_EVENT_BATCH_SIZE_PROP = "LOAD_EVENT_BATCH_SIZE";

    /**
     * The default value for the number of events to load from the database at a time.
     */
    public static final int DEFAULT_LOAD_EVENT_BATCH_SIZE = 10;

    /**
     * The maximum length of the error message than can be inserted into the 
     * PersistentEvent.lastErrorMessage column.
     */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 1024;
    

    /**
     * Constructor.
     */
    public DatabaseEventQueue ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "QUEUE OPERATION: Creating event queue of type [" 
                       + StringUtils.getClassName(this) + "] ..." );

        queue = Collections.synchronizedList( new LinkedList() );
    }


    /**
     * Initialize the event queue.
     *
     * @param  props  A container of configuration properties.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid.
     */
    public void initialize ( Map props ) throws FrameworkException
    {
        String temp = (String)props.get( LOAD_EVENT_BATCH_SIZE_PROP );
        
        if ( StringUtils.hasValue( temp ) )
        {
            maxDatabaseEventLoadSize = StringUtils.getInteger( temp );
            
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "QUEUE OPERATION: Initializing: Maximum-database-event-batch-load-size is [" 
                           + maxDatabaseEventLoadSize + "] rows." );
        }
    }
    
    
    /**
     * Add the event to the end of queue.
     * 
     * @param  event  The event to add to the queue.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public void add ( Event event ) throws FrameworkException
    {
        Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Adding event to database queue ..." );

        Connection dbConn = null;
        
        PreparedStatement ps = null;
        
        long startTime = -1;

        if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
            startTime = System.currentTimeMillis( );

        try 
        {
            event.id = PersistentSequence.getNextSequenceValue( SEQUENCE_NAME );
            
            dbConn = DBConnectionPool.getInstance().acquireConnection( );
            
            event.arrivalTime = new java.sql.Timestamp( System.currentTimeMillis() );

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "\n" + LINE + "\nExecuting SQL:\n" + INSERT_EVENT_SQL );
            
            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Event being inserted into database:\n" + event.describe() );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Event contents:\n" + event.message );

            ps = dbConn.prepareStatement( INSERT_EVENT_SQL );

            ps.setString( 1, event.channelName );
            ps.setInt( 2, event.id );

            DBLOBUtils.setCLOB( ps, 3, event.message );

            ps.setTimestamp( 4, event.arrivalTime );
            
            int numRows = ps.executeUpdate( );

            if ( numRows != 1 )
            {
                String errMsg = "Execution of SQL statement [" + INSERT_EVENT_SQL + "] affected [" 
                    + numRows + "] rows.";
                
                Debug.error( errMsg );
                
                throw new FrameworkException( errMsg );
            }

            DBConnectionPool.getInstance().commit( dbConn );

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Successfully committed SQL operation.\n" + LINE );

            // NOTE: We don't add the item just inserted into the database into the in-memory
            // queue, as we want it loaded by the separate dequeueing thread.
        }
        catch ( SQLException sqle )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         e.toString() );
        }
        finally 
        {
            releaseDatabaseResources( dbConn, ps );

            if ( Debug.isLevelEnabled( Debug.BENCHMARK ) && (startTime > 0) )
            {
                long stopTime = System.currentTimeMillis( );
                
                Debug.log( Debug.BENCHMARK, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  "
                           + "SQL: Time to insert event into PersistentEvent database table." );
            }
        }
    }
    
    
    /**
     * Update the given event as indicated.
     * 
     * @param  event  The event to update.
     * @param  eventStatus  The event delivery status.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public void update ( Event event, EventStatus eventStatus ) throws FrameworkException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Updating database queue using event status [" 
                       + eventStatus.name + "] ..." );

        // If no consumers were available, the event delivery wasn't attempted so leave
        // the queue in its current state.
        if ( eventStatus == EventStatus.NO_CONSUMERS_AVAILABLE )
        {
            Debug.log( Debug.MSG_STATUS, "Skipping queue update, as no consumers were available to process it." );
            
            return;
        }
        
        Connection dbConn = null;
        
        PreparedStatement ps = null;

        long startTime = -1;

        if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
            startTime = System.currentTimeMillis( );

        try 
        {
            dbConn = DBConnectionPool.getInstance().acquireConnection( );
            
            if ( eventStatus == EventStatus.DELIVERY_SUCCESSFUL )
            {
                // If the event was successfully delivered, update status in database to delivered.
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                    Debug.log( Debug.DB_DATA, "\n" + LINE + "\nExecuting SQL:\n" + UPDATE_EVENT_SUCCESS_SQL );
                
                ps = dbConn.prepareStatement( UPDATE_EVENT_SUCCESS_SQL );

                java.sql.Timestamp ts = new java.sql.Timestamp( System.currentTimeMillis() );
                
                ps.setTimestamp( 1, ts ); 
                ps.setString( 2, event.channelName );
                ps.setInt( 3, event.id );
            }
            else
            if ( eventStatus == EventStatus.DELIVERY_FAILED )
            {
                // If the event delivery failed, we mark it as failed in the database.
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                    Debug.log( Debug.DB_DATA, "\n" + LINE + "\nExecuting SQL:\n" + UPDATE_EVENT_ERROR_SQL );
                
                // Truncate error message if it's larger than database column.
                if ( (event.lastErrorMessage != null) && (event.lastErrorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) )
                    event.lastErrorMessage = event.lastErrorMessage.substring( 0, MAX_ERROR_MESSAGE_LENGTH );

                event.lastErrorTime = new java.sql.Timestamp( System.currentTimeMillis() );

                ps = dbConn.prepareStatement( UPDATE_EVENT_ERROR_SQL );
                
                ps.setTimestamp( 1, event.lastErrorTime ); 
                ps.setString( 2, event.lastErrorMessage );
                ps.setString( 3, event.channelName );
                ps.setInt( 4, event.id );
            }                
            else
            {
                throw new FrameworkException( "ERROR: Invalid event update type [" + eventStatus.name + "]." );
            }

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Event being operated on in database:\n" + event.describe() );

            int numRows = ps.executeUpdate( );
            
            if ( numRows > 1 )
            {
                String errMsg = "Execution of update SQL statement affected [" + numRows + "] rows.";
                
                Debug.error( errMsg );
                
                throw new FrameworkException( errMsg );
            }

            DBConnectionPool.getInstance().commit( dbConn );

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Successfully committed SQL operation.\n" + LINE );

            // At this point, the event should be removed from the in-memory buffer of events as well, 
            // irrespective of processing outcome.
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Removing event [" + event.describe() 
                           + "] from in-memory queue buffer." );
            
            boolean removed = queue.remove( event );
            
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Event removed? [" + removed 
                           + "].  In-memory queue buffer size [" + queue.size() + "]." );
        }
        catch ( SQLException sqle )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         e.toString() );
        }
        finally 
        {
            releaseDatabaseResources( dbConn, ps );

            if ( Debug.isLevelEnabled( Debug.BENCHMARK ) && (startTime > 0) )
            {
                long stopTime = System.currentTimeMillis( );
                
                Debug.log( Debug.BENCHMARK, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  "
                           + "SQL: Time to update event in PersistentEvent database table." );
            }
        }
    }


    /**
     * Returns 'true' if the queue has more items to process.
     * 
     * @param  criteria  An event containing the event-selection criteria.
     *
     * @return  'true' if queue has more items to process, otherwise 'false'.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public boolean hasNext ( Event criteria ) throws FrameworkException
    {
        boolean available = (queue.size() > 0);
        
        // If no events are available in memory, attempt to get more from the database.
        if ( !available )
        {
            loadFromDatabase( criteria );
            
            available = (queue.size() > 0);
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Events available in database queue? [" + available + "]." );
        
        return available;
    }


    /**
     * Get the next queued item.  Must be called after hasNext().
     * 
     * @return  The next item in the queue to process.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public Event next ( ) throws FrameworkException
    {
        if ( queue.size() == 0 )
        {
            throw new FrameworkException( "ERROR: Attempt was made to retrieve event from empty queue." );
        }

        Event event = (Event)queue.get( 0 );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Retrieving the following event from the database queue:\n" 
                       + event.describe() );
        
        return event;
    }


    /**
     * Load any available events from the database up to the configured maximum.
     * 
     * @param  criteria  An event containing the event-selection criteria.
     * 
     * @return  The next available event on the queue.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    private void loadFromDatabase ( Event criteria ) throws FrameworkException
    {
        Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Loading events from database into queue ..." );

        if ( !StringUtils.hasValue( criteria.channelName ) )
        {
            throw new FrameworkException( "ERROR: Event channel name is a required queue search criteria." );
        }

        Connection dbConn = null;

        PreparedStatement ps = null;

        long startTime = -1;

        if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
            startTime = System.currentTimeMillis( );

        try 
        {
            dbConn = DBConnectionPool.getInstance().acquireConnection( );
            
            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "\n" + LINE + "\nExecuting SQL:\n" + QUERY_EVENT_SQL );
            
            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Criteria used in query against database:\n" + criteria.describe() );

            ps = dbConn.prepareStatement( QUERY_EVENT_SQL );
            
            ps.setString( 1, criteria.channelName );
            
            ResultSet rs = ps.executeQuery( );
            
            for ( int counter = 0;  (counter < maxDatabaseEventLoadSize) && rs.next();  counter ++ ) 
            {
                Event event = new Event( );

                event.channelName = rs.getString( CHANNEL_NAME_COL );
                event.id = rs.getInt( ID_COL );
                
                event.message = DBLOBUtils.getCLOB( rs, MESSAGE_COL );
                
                if ( Debug.isLevelEnabled( Debug.MSG_LIFECYCLE ) )
                    Debug.log(Debug.MSG_LIFECYCLE, "Event contents:\n" + event.message );
                    
                event.arrivalTime = rs.getTimestamp( ARRIVAL_TIME_COL );
                event.errorStatus = rs.getString( ERROR_STATUS_COL );
                event.errorCount = rs.getInt( ERROR_COUNT_COL );
                event.lastErrorMessage = rs.getString( LAST_ERROR_MESSAGE_COL );
                event.lastErrorTime = rs.getTimestamp( LAST_ERROR_TIME_COL );

                // Add item to in-memory buffer.
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Adding event [" + event.describe() 
                               + "] to in-memory queue buffer." );
                
                queue.add( event );
                
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "In-memory queue buffer size [" + queue.size() + "]." );
            }

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "\n" + LINE );
        }
        catch ( SQLException sqle )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         e.toString() );
        }
        finally 
        {
            releaseDatabaseResources( dbConn, ps );

            if ( Debug.isLevelEnabled( Debug.BENCHMARK ) && (startTime > 0) )
            {
                long stopTime = System.currentTimeMillis( );
                
                Debug.log( Debug.BENCHMARK, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  "
                           + "SQL: Time to load event(s) from PersistentEvent database table." );
            }
        }
    }


    /**
     * Reset any events meeting the given criteria so that they can be retried.
     * 
     * @param  criteria  An event containing the event-selection criteria.
     * 
     * @return  The number of events reset.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    protected static int reset ( Event criteria ) throws FrameworkException
    {
        Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Resetting events in database for database queue ..." );

        if ( !StringUtils.hasValue( criteria.channelName ) )
        {
            throw new FrameworkException( "ERROR: Event channel name is a required queue search criteria." );
        }
        
        Connection dbConn = null;
        
        PreparedStatement ps = null;
        
        long startTime = -1;

        if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
            startTime = System.currentTimeMillis( );

        try 
        {
            dbConn = DBConnectionPool.getInstance().acquireConnection( );
            
            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Criteria used to reset events in database:\n" + criteria.describe() );

            // If no identifier was given that uniquely identifies a single event ...
            if ( criteria.id == 0 )
            {
                // Use last error time and error count, if available.
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                    Debug.log( Debug.DB_DATA, "\n" + LINE + "\nExecuting SQL:\n" + UPDATE_EVENT_RETRY_SQL );
            
                ps = dbConn.prepareStatement( UPDATE_EVENT_RETRY_SQL );
            
                ps.setString( 1, criteria.channelName );

                if ( criteria.lastErrorTime == null )
                    ps.setNull( 2, Types.DATE );
                else
                    ps.setTimestamp( 2, criteria.lastErrorTime );

                if ( criteria.errorCount < 1 )
                    ps.setNull( 3, Types.INTEGER );
                else
                    ps.setInt( 3, criteria.errorCount );
            }
            else 
            {
                // An Id was given which should uniquely identify a single event, so we should 
                // skip using any other qualifying criteria, if present.
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                    Debug.log( Debug.DB_DATA, "\n" + LINE + "\nExecuting SQL:\n" + UPDATE_EVENT_RETRY_BY_ID_SQL );

                ps = dbConn.prepareStatement( UPDATE_EVENT_RETRY_BY_ID_SQL );

                ps.setString( 1, criteria.channelName );

                ps.setInt( 2, criteria.id );
            }

            int numRows = ps.executeUpdate( );
            
            DBConnectionPool.getInstance().commit( dbConn );
            
            if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                Debug.log( Debug.DB_DATA, "Committed SQL execution affected [" + numRows + "] rows.\n" + LINE );
            
            return numRows;
        }
        catch ( SQLException sqle )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new DatabaseException( "ERROR: Could not execute SQL statement:\n" + 
                                         e.toString() );
        }
        finally 
        {
            releaseDatabaseResources( dbConn, ps );

            if ( Debug.isLevelEnabled( Debug.BENCHMARK ) && (startTime > 0) )
            {
                long stopTime = System.currentTimeMillis( );
                
                Debug.log( Debug.BENCHMARK, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  "
                           + "SQL: Time to reset event(s) in PersistentEvent database table." );
            }
        }
    }


    /**
     * Indicates to the queue that no event consumers are available.
     */
    public void noConsumersAvailable ( )
    {
        // Nothing to do here.
    }


    /**
     * Shut down the queue;
     */
    public void shutdown ( )
    {
        // Nothing to do here.
    }


    /**
     * Get a human-readable description of the event queue.
     *
     * @return  A description of the event queue.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Database event queue [" );
        sb.append( StringUtils.getClassName(this) );
        sb.append( "], in-memory-event-count [" );
        sb.append( queue.size() );
        sb.append( "], Event-load-batch-size [" );
        sb.append( maxDatabaseEventLoadSize );
        sb.append( "]" );

        return( sb.toString() );
    }


    // Release the given database resources, if non-null.
    private static void releaseDatabaseResources ( Connection dbConn, PreparedStatement ps )
    {
        if ( ps != null )
        {        
            try
            {
                ps.close( );
            }
            catch ( Exception e )
            {
                Debug.warning( "Failed to close the prepared statement:\n" 
                               + e.toString() );
            }
        }

        if ( dbConn != null )
        {        
            try
            {
                DBConnectionPool.getInstance().releaseConnection( dbConn );
            }
            catch ( Exception e )
            {
                Debug.warning( "Failed to release database connection back to the pool:\n" 
                               + e.toString() );
            }
        }
    }


    private List queue;

    // Buffer used to read event into from database.
    private byte[] readBuffer = new byte[ 4096 ];

    private int maxDatabaseEventLoadSize = DEFAULT_LOAD_EVENT_BATCH_SIZE;


    // Non-null status column values.
    private static final String ERROR_STATUS_AWAITING_RETRY = "AwaitingRetry";
    private static final String ERROR_STATUS_FAILED = "Failed";
    private static final String SUCCESS_STATUS_DELIVERED = "Delivered";

    // Database table name.
    private static final String TABLE_NAME = "PersistentEvent";

    // Database table column names.
    private static final String CHANNEL_NAME_COL       = "channelName";
    private static final String ID_COL                 = "id";
    private static final String MESSAGE_COL            = "message";
    private static final String ARRIVAL_TIME_COL       = "arrivalTime";
    private static final String ERROR_STATUS_COL       = "errorStatus";
    private static final String ERROR_COUNT_COL        = "errorCount";
    private static final String LAST_ERROR_MESSAGE_COL = "lastErrorMessage";
    private static final String LAST_ERROR_TIME_COL    = "lastErrorTime";
    
    // Event identifier sequence name.
    private static final String SEQUENCE_NAME = "PersistentEventIdSeq";
    
    // SQL statements used to manage events in the database.
    private static final String QUERY_EVENT_SQL = 
        "SELECT /*+ index(a) */ " + 
        CHANNEL_NAME_COL + ", " + 
        ID_COL + ", " + 
        MESSAGE_COL + ", " + 
        ARRIVAL_TIME_COL + ", " + 
        ERROR_STATUS_COL + ", " + 
        ERROR_COUNT_COL + ", " + 
        LAST_ERROR_MESSAGE_COL + ", " + 
        LAST_ERROR_TIME_COL + 
        " FROM " + TABLE_NAME + " a " +
        " WHERE " + CHANNEL_NAME_COL + " = ?" +
        " AND (" + ERROR_STATUS_COL + " = '" + ERROR_STATUS_AWAITING_RETRY + "' OR " + ERROR_STATUS_COL + " IS NULL)" +
        " ORDER BY " + ID_COL;
    
    private static final String INSERT_EVENT_SQL = 
        "INSERT INTO " + TABLE_NAME + "(" + 
        CHANNEL_NAME_COL + ", " + 
        ID_COL + ", " + 
        MESSAGE_COL + ", " + 
        ARRIVAL_TIME_COL + 
        ") VALUES (?, ?, ?, ?)";
    
    private static final String UPDATE_EVENT_SUCCESS_SQL = 
        "UPDATE " + TABLE_NAME +
        " SET " + ERROR_STATUS_COL + " = '" + SUCCESS_STATUS_DELIVERED + "', " +
        LAST_ERROR_TIME_COL + " = ?" +
        " WHERE " + CHANNEL_NAME_COL + " = ?" + 
        " AND " + ID_COL + " = ?";
    
    private static final String UPDATE_EVENT_ERROR_SQL = 
        "UPDATE " + TABLE_NAME + 
        " SET " + ERROR_STATUS_COL + " = '" + ERROR_STATUS_FAILED + "', " +
        ERROR_COUNT_COL + " = " + ERROR_COUNT_COL + " + 1, " +
        LAST_ERROR_TIME_COL + " = ?, " +
        LAST_ERROR_MESSAGE_COL + " = ? " +
        " WHERE " + CHANNEL_NAME_COL + " = ?" + 
        " AND " + ID_COL + " = ?";
    
    private static final String UPDATE_EVENT_RETRY_SQL = 
        "UPDATE " + TABLE_NAME + 
        " SET " + ERROR_STATUS_COL + " = '" + ERROR_STATUS_AWAITING_RETRY + "'" +
        " WHERE " + ERROR_STATUS_COL + " = '" + ERROR_STATUS_FAILED + "'" + 
        " AND " + CHANNEL_NAME_COL + " = ?" + 
        " AND " + LAST_ERROR_TIME_COL + " >= ?" +
        " AND " + ERROR_COUNT_COL + " <= ?";

    private static final String UPDATE_EVENT_RETRY_BY_ID_SQL = 
        "UPDATE " + TABLE_NAME + 
        " SET " + ERROR_STATUS_COL + " = '" + ERROR_STATUS_AWAITING_RETRY + "'" +
        " WHERE " + ERROR_STATUS_COL + " = '" + ERROR_STATUS_FAILED + "'" + 
        " AND " + CHANNEL_NAME_COL + " = ?" + 
        " AND " + ID_COL + " = ?";

    private static final String LINE 
        = "===============================================================================";
}

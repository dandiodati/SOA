/**
 * Copyright (c) 2004 Neustar, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/framework/util/MetricsAgent.java#1 $
 */

package com.nightfire.framework.util;


import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.SQLUtil;
import com.nightfire.framework.db.DatabaseException;

import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * The MetricsAgent class provides general-purpose performance metrics logging facilities.
 */
public final class MetricsAgent
{
    /**
     * Name of property indicating which metrics logging levels to enable (whitespace-separated category names).
     */
    public static final String METRICS_LOG_LEVELS_PROP = "METRICS_LOG_LEVELS";

    /**
     * Name of property indicating the name of the metrics log file.
     */
    public static final String METRICS_LOG_FILE_NAME_PROP = "METRICS_LOG_FILE";

    /**
     * Name of property indicating the maximum number of entries to write to a metrics log file.
     */
    public static final String MAX_METRICS_WRITES_PROP = "MAX_METRICS_WRITES";

    /**
     * Name of property indicating the maximum number of metrics log files to create 
     * before previously-created ones are overwritten in a circular buffer fashion.
     */
    public static final String MAX_METRICS_LOG_FILE_COUNT_PROP = "MAX_METRICS_LOG_FILE_COUNT";

    /**
     * Name of property indicating wether to write Metrics logs in database or not.
     */
    public static final String METRICS_DB_LOGGING_PROP = "METRICS_DB_LOGGING";

    /**
     * Name of property indicating the logging level of Metrics.
     * Action having this level would be logged in database.
     * Action other than this value would be skipped and would not be logged.
     */
    public static final String METRICS_DB_LOGGING_LEVEL_PROP = "METRICS_DB_LOGGING_LEVEL";

    /**
     * Name of property indicating the business object category of metrics.
     */
    public static final String BO_CATEGORY = "BO";

    /**
     * Name of property indicating the workflow category of metrics.
     */
    public static final String WF_CATEGORY = "WF";

    /**
     * Name of property indicating the synchronous API category of metrics (Ex: Interaction Manager).
     */
    public static final String SYNC_API_CATEGORY = "SYNC_API";

    /**
     * Name of property indicating the asynchronous API category of metrics (Ex: Event Manager).
     */
    public static final String ASYNC_API_CATEGORY = "ASYNC_API";

    /**
     * Name of property indicating the gateway category of metrics.
     */
    public static final String GATEWAY_CATEGORY = "GATEWAY";

    /**
     * Status value indicating successful outcome.
     */
    public static final String PASS_STATUS = "PASS";

    /**
     * Status value indicating failed outcome.
     */
    public static final String FAIL_STATUS = "FAIL";

    // Database table columns, that would log the user action
    // These column names would be used by Metrics Logging tables.
    /**
     * DATETIME column in database. This is of type Timestamp
     */
    public static final String DATETIME_COL = "DATETIME";

    /**
     * ELAPSEDTIME column in database.
     */
    public static final String ELAPSEDTIME_COL = "ELAPSEDTIME";

    /**
     * CUSTOMERID column. Stores CustomerID in customer context
     */
    public static final String CUSTOMERID_COL = "CUSTOMERID";

    /**
     * USERID column. Stores USERID in customer context
     */
    public static final String USERID_COL = "USERID";

    /**
     * Action column. Values could be "login", "singleSignOnLogin", "logout"
     */
    public static final String ACTION_COL = "ACTION";

    /**
     * CATEGORY column. Values of this column would be the Metrics logging category
     */
    public static final String CATEGORY_COL = "CATEGORY";

    /**
     * Session ID column.
     */
    public static final String SESSIONID_COL = "SESSIONID";

    /**
     * Inet Address of the machine where tomcat is running.
     */
    public static final String MACHINEINETADDRESS_COL = "MACHINEINETADDRESS";

    /**
     * METRICATTRIBUTE column. These columns would hold he various Metrics attributes
     */
    public static final String METRICATTRIBUTE_COL = "METRICATTRIBUTE";

    /**
     * Name of the database table to log the USER Action data in.
     */
    public static final String USER_ACTION_DB_TABLE = "USER_ACTION";

    /**
     * Name of the database table to log the Webapp Metrics data in.
     */
    public static final String WEBAPP_METRICS_DB_TABLE = "WEBAPP_METRICS";

    /**
     * Name of the database table to log the USER Action data in.
     */
    public static final String TIMESTAMP_COL = "TIMESTAMP";

     /**
     * Properties indicating the characters used in tokenizing strings etc
     */
    public static final String EQUALS_PROP = "=";

    public static final String COMMA_PROP = ",";

    public static final String SEMI_COLON_PROP = ";";

    public static final String DOT_PROP = ".";

    public static final String RIGHT_BRACKET_PROP = "[";

    /**
     * User Action starting with this suffix would be considered as METRICS_QUERY_ACTION
     */
    public static final String QUERY_PREFIX_PROP = "query";

    // Properties to indicate various login, logout actions
    public static final String LOGIN_ACTION = "login";

    public static final String LOGOUT_ACTION = "logout";

    public static final String SINGLE_SIGN_ON_LOGIN_ACTION = "singleSignOnLogin";


    /**
     * Log business object-relevant metrics.
     *
     * @param startTime  The time at which the measured item started.
     * @param metaDataName  Meta-data name identifying business object type.
     * @param BOID  Identifies business object instance within type.
     * @param currentState The current state that the business object is in.
     * @param operation  Operation being performed against the business object.
     */
    public static final void logBO ( long startTime, String metaDataName, String BOID, String currentState, String operation )
    {
        if ( !isOn( BO_CATEGORY ) )
            return;

        try
        {
            // Get the log file state object associated with the currrent thread.
            LogFileState lfs = getLogFile( );

            // Serialize access to a given log file, while allowing concurrent
            // access to different log files.
            synchronized( lfs )
            {
                StringBuffer sb = new StringBuffer( );

                logFixed( lfs, sb, startTime, BO_CATEGORY );

                sb.append( ',' );

                sb.append( metaDataName );

                sb.append( ',' );

                sb.append( BOID );

                sb.append( ',' );

                sb.append( currentState );

                sb.append( ',' );

                sb.append( operation );

                Writer fw = lfs.getLogFile( );

                write( fw, sb.toString() );
                write( fw, "\n" );

                flush( fw );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "MetricsAgent.logBO() failed.  Reason:\n" + e.toString() );
        }
    }


    /**
     * Log workflow-relevant metrics.
     *
     * @param startTime  The time at which the measured item started.
     * @param metaDataName  Meta-data name identifying business object type.
     * @param BOID  Identifies business object instance within type.
     * @param driverKey  Identifies the workflow segment.
     * @param driverType  Identifies the workflow segment.
     */
    public static final void logWF ( long startTime, String metaDataName, String BOID, String driverKey, String driverType )
    {
        if ( !isOn( WF_CATEGORY ) )
            return;

        try
        {
            // Get the log file state object associated with the currrent thread.
            LogFileState lfs = getLogFile( );

            // Serialize access to a given log file, while allowing concurrent
            // access to different log files.
            synchronized( lfs )
            {
                StringBuffer sb = new StringBuffer( );

                logFixed( lfs, sb, startTime, WF_CATEGORY );

                sb.append( ',' );

                sb.append( metaDataName );

                sb.append( ',' );

                sb.append( BOID );

                sb.append( ',' );

                sb.append( driverKey );

                sb.append( ',' );

                sb.append( driverType );

                Writer fw = lfs.getLogFile( );

                write( fw, sb.toString() );
                write( fw, "\n" );

                flush( fw );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "MetricsAgent.logWF() failed.  Reason:\n" + e.toString() );
        }
    }


    /**
     * Log synchronous API-relevant metrics.
     *
     * @param startTime  The time at which the measured item started.
     * @param action  Action being performed.
     */
    public static final void logSyncAPI ( long startTime, String action )
    {
        if ( !isOn( SYNC_API_CATEGORY ) )
            return;

        try
        {
            if(metricsDbLogging)
            {
                logIInDB(SYNC_API_CATEGORY, startTime, action);
                return;
            }

            // Get the log file state object associated with the currrent thread.
            LogFileState lfs = getLogFile( );

            // Serialize access to a given log file, while allowing concurrent
            // access to different log files.
            synchronized( lfs )
            {
                StringBuffer sb = new StringBuffer( );

                logFixed( lfs, sb, startTime, SYNC_API_CATEGORY );

                sb.append( ',' );

                sb.append( action );

                Writer fw = lfs.getLogFile( );

                write( fw, sb.toString() );
                write( fw, "\n" );

                flush( fw );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "MetricsAgent.logSyncAPI() failed.  Reason:\n" + e.toString() );
        }
    }


    /**
     * Log async API-relevant metrics.
     *
     * @param startTime  The time at which the measured item started.
     * @param metaDataName  Meta-data name identifying business object type.
     * @param BOID  Identifies business object instance within type.
     */
    public static final void logAsyncAPI ( long startTime, String metaDataName, String BOID )
    {
        if ( !isOn( ASYNC_API_CATEGORY ) )
            return;

        try
        {
            // Get the log file state object associated with the currrent thread.
            LogFileState lfs = getLogFile( );

            // Serialize access to a given log file, while allowing concurrent
            // access to different log files.
            synchronized( lfs )
            {
                StringBuffer sb = new StringBuffer( );

                logFixed( lfs, sb, startTime, ASYNC_API_CATEGORY );

                sb.append( ',' );

                sb.append( metaDataName );

                sb.append( ',' );

                sb.append( BOID );

                Writer fw = lfs.getLogFile( );

                write( fw, sb.toString() );
                write( fw, "\n" );

                flush( fw );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "MetricsAgent.logAsyncAPI() failed.  Reason:\n" + e.toString() );
        }
    }


    /**
     * Log gateway-relevant metrics.
     *
     * @param startTime  The time at which the measured item started.
     * @param driverKey  Identifies the gateway chain being executed.
     * @param driverType  Identifies the gateway chain being executed.
     */
    public static final void logGateway ( long startTime, String driverKey, String driverType )
    {
        if ( !isOn( GATEWAY_CATEGORY ) )
            return;

        try
        {
            // Get the log file state object associated with the currrent thread.
            LogFileState lfs = getLogFile( );

            // Serialize access to a given log file, while allowing concurrent
            // access to different log files.
            synchronized( lfs )
            {
                StringBuffer sb = new StringBuffer( );

                logFixed( lfs, sb, startTime, GATEWAY_CATEGORY );

                sb.append( ',' );

                sb.append( driverKey );

                sb.append( ',' );

                sb.append( driverType );

                Writer fw = lfs.getLogFile( );

                write( fw, sb.toString() );
                write( fw, "\n" );

                flush( fw );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "MetricsAgent.logGateway() failed.  Reason:\n" + e.toString() );
        }
    }


    /**
     * Log the fixed part of the metrics message (timestamps, customer, user, ..., etc).
     *
     * @param  lfs  Log file object.
     * @param  sb   String buffer to append data to.
     * @param  startTime  Time at which the metric being measured started.
     * @param  category  Type of metric.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    private static final void logFixed ( LogFileState lfs, StringBuffer sb, long startTime, String category ) 
        throws FrameworkException
    {
        sb.append( lfs.timestamp.format( new Date() ) );

        sb.append( ',' );

        sb.append( System.currentTimeMillis() - startTime );

        sb.append( ',' );

        CustomerContext cc = CustomerContext.getInstance( );

        sb.append( cc.getCustomerID() );

        sb.append( ',' );

        sb.append( cc.getUserID() );

        if (StringUtils.hasValue ( cc.getSubDomainId (), true ))
           sb.append ( "," ).append ( cc.getSubDomainId () );

        sb.append( ',' );

        sb.append( category );
    }

    /**
     * This is a utility method to log the login and logout action in database.
     * This method would take in action and session Id values.
     * @param action
     * @param sessionID
     */
    public static void logSyncAPIInDB( String action, String sessionID )
    {

        Connection conn = null;

        int sqlResult = 0;

        Hashtable columnValues = null;

        SimpleDateFormat timestamp = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );
        Date currDate = null;

        try{
            conn = DBInterface.acquireConnection();

            CustomerContext cc = CustomerContext.getInstance( );

            columnValues = new Hashtable ();

            currDate = new Date();

            columnValues.put (DATETIME_COL, timestamp.format( currDate ));

            columnValues.put (CUSTOMERID_COL, cc.getCustomerID());

            columnValues.put (USERID_COL, cc.getUserID());

            columnValues.put (ACTION_COL, action);

            columnValues.put (TIMESTAMP_COL, new java.sql.Timestamp(currDate.getTime()));

            if(null != sessionID)
                columnValues.put (SESSIONID_COL, sessionID);

            try{
                columnValues.put (MACHINEINETADDRESS_COL, InetAddress.getLocalHost().toString());
            }
            catch(UnknownHostException une)
            {
                une.printStackTrace();
            }

            sqlResult = SQLUtil.insertRow(conn, USER_ACTION_DB_TABLE, columnValues );

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "Updated [" + sqlResult + "] rows in database for user's action [" + action + "]");

        }
        catch(DatabaseException dbe)
        {
            dbe.printStackTrace();
        }
        catch(FrameworkException fex)
        {
            fex.printStackTrace();
        }
        finally
        {
            /** Here try is required to release the DB Connection Pool Instance */
            try {
                if (conn != null) {
                    conn.commit ();
                }

                DBInterface.releaseConnection(conn);

                columnValues = null;
            }
            catch (Exception e) {
                Debug.log(Debug.ALL_ERRORS, e.toString());
            }
        }
    }

    /**
     * This method logs the metrics logs in database
     * @param category
     * @param startTime
     * @param action
     */
    public static void logIInDB( String category, long startTime, String action )
    {

        Connection conn = null;

        int sqlResult = 0, metricsLoggingActionLevel;

        String token, tempStr, sessionID;

        Hashtable columnValues = null;

        HashMap actionMap = new HashMap();

        String actionValue;

        // Get the value of Action. If action is login and logout then the action woul dbe of form
        // login=[customerid=ACME_GW;user=example;sessionId=9D96808DB888F0CA16D7EA8C69B51F3A]

        if(action.startsWith(LOGIN_ACTION) || action.startsWith(LOGOUT_ACTION) || action.startsWith(SINGLE_SIGN_ON_LOGIN_ACTION))
        {
            actionValue = action.substring(0,action.indexOf(EQUALS_PROP));
            tempStr = action.substring(action.indexOf(RIGHT_BRACKET_PROP) + 1);
            StringTokenizer st = new StringTokenizer(tempStr, SEMI_COLON_PROP);
            while(st.hasMoreTokens())
            {
                token = st.nextToken();
                actionMap.put(token.substring(0,token.indexOf(EQUALS_PROP)).toUpperCase(), token.substring(token.indexOf(EQUALS_PROP) + 1));
            }
            // If Action value is login/logout/singleSignOnLogin then Action is of type METRICS_LOGIN_LOGOUT_ACTION
            // This will be used in checking the level assigned to action and the allowable logging level
            metricsLoggingActionLevel = Debug.LOGIN_LOGOUT_METRICS_LOGGING;
        }
        else
        {
            //If action is not login/logout then action string would be of form
            //query-lsr-preorders.PASS,outstanding-service-handlers=[0],DB-Pool[init-size=1; max-size=100; available=2; acquired=0; max-wait-time=60.0 sec]
            actionValue = action.substring(0, action.indexOf(DOT_PROP));

            //If Action value starts with "query" then Action is of type METRICS_QUERY_ACTION
            //else Action is of type METRICS_ORDER_ACTION
            if(actionValue.startsWith(QUERY_PREFIX_PROP))
                metricsLoggingActionLevel = Debug.QUERY_METRICS_LOGGING;
            else
                metricsLoggingActionLevel = Debug.ORDER_METRICS_LOGGING;
        }

        // If metrics DB Logging level is set to all then do not check levels of actions
        if(metricsDbLoggingLevel != Debug.ALL_METRICS_LOGGING)
        {
            //if metric logging level for actiopn has not been set to metrics DB Logging level then return.
            if(metricsLoggingActionLevel != metricsDbLoggingLevel)
                return;
        }

        SimpleDateFormat timestamp = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );

        try{

            // Get the instance of CustomerContext to get the CustomerID and UserID
            CustomerContext cc = CustomerContext.getInstance( );

            // Map to put the db table column values
            columnValues = new Hashtable ();

            columnValues.put (DATETIME_COL, timestamp.format(new Date()));

            StringBuffer sb = new StringBuffer();
            sb.append(System.currentTimeMillis() - startTime);
            columnValues.put (ELAPSEDTIME_COL, sb.toString());

            columnValues.put (CUSTOMERID_COL, cc.getCustomerID() );

            columnValues.put (USERID_COL, cc.getUserID() );

            columnValues.put (CATEGORY_COL, category );

            //Session ID would be available only in case of login and logout action
            sessionID = (String)actionMap.get(SESSIONID_COL);
            if(null != sessionID)
            {
                if(actionValue.equalsIgnoreCase(LOGIN_ACTION) || actionValue.equalsIgnoreCase(SINGLE_SIGN_ON_LOGIN_ACTION))
                    columnValues.put (SESSIONID_COL, sessionID.substring(0 , sessionID.length()-1));
                else
                    columnValues.put (SESSIONID_COL, sessionID);
            }

            // Get the InetAddress of the machine where tomcat is running
            try{
                sb = new StringBuffer();
                sb.append(InetAddress.getLocalHost());
                columnValues.put (MACHINEINETADDRESS_COL, sb.toString() );
            }
            catch(UnknownHostException uhe )
            {
                uhe.printStackTrace();
            }

            // Get the Action performed by the user
            columnValues.put (ACTION_COL, actionValue );

            // get the metrics attribute
            // Split the attributes by comma, and log it in database
            StringTokenizer st = new StringTokenizer(action , ",");
            int tokenCount = st.countTokens();
            // If more than five metrics attribute then ignore the attributes above five
            // This case is never expected.
            if(tokenCount > 5)
            {
                tokenCount = 5;
            }

            // Iterate through the tokens and put in columnvalues map.
            for(int ix = 1; ix <= tokenCount ; ix ++)
            {
                columnValues.put (METRICATTRIBUTE_COL + ix, st.nextToken());
            }

            // Get the connection from DBConnectionPool
            conn = DBInterface.acquireConnection();

            // Insert the record in database
            sqlResult = SQLUtil.insertRow(conn, WEBAPP_METRICS_DB_TABLE, columnValues );

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "Updated [" + sqlResult + "] rows in database table [" + WEBAPP_METRICS_DB_TABLE + "] for webapp metrics action [" + actionValue + "]");

            try{
                conn.commit ();
            }
            catch(SQLException sqle)
            {
                Debug.log(Debug.ALL_ERRORS, "Could not commit database [" + sqle.getMessage() + "]");
            }
        }
        catch(DatabaseException dbe)
        {
            Debug.log(Debug.ALL_ERRORS, dbe.getMessage());
        }
        catch(FrameworkException fex)
        {
            Debug.log(Debug.ALL_ERRORS, fex.getMessage());
        }
        finally
        {
            /** Here try is required to release the DB Connection Pool Instance */
            try {

                DBInterface.releaseConnection(conn);

                columnValues = null;

                actionMap = null;
            }
            catch (Exception e) {
                Debug.log(Debug.ALL_ERRORS, e.toString());
            }
        }
    }

    /**
     * Test to see if given logging-level is enabled.
     *
     * @param  metricName  Logging-level to test.
     *
     * @return  'true' if level is enabled, otherwise 'false'.
     */
    public static final boolean isOn ( String metricName )
    {
        return( enabledLevels.contains( metricName ) );
    }


    /**
     * Configure the metrics agent from properties.
     */
    public static final void configureFromProperties ( )
    {
        try
        {
            String temp = getProperty( null, METRICS_LOG_LEVELS_PROP );

            if ( temp == null )
                return;

			Debug.log( Debug.SYSTEM_CONFIG, "Setting metrics logging levels to [" + temp + "]");

            configureLevels( temp );

            temp = getProperty( null, MAX_METRICS_WRITES_PROP );

            if ( temp != null )
            {
                maxNumWrites = Long.parseLong( temp );

                Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of writes to a metrics log file is set to [" + maxNumWrites + "]" );
            }

            temp = getProperty( null, MAX_METRICS_LOG_FILE_COUNT_PROP );

            if ( temp != null )
            {
                maxLogFileCount = Integer.parseInt( temp );

                Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of metrics log files to create [" + maxLogFileCount + "]" );
            }

        	// Check debug log files.
            configuredLogFileName = getProperty( null, METRICS_LOG_FILE_NAME_PROP );
            
            // Set the configuredLogFileName pointing to correct log-base directory, if present. 
            // Else use the already set value.
            configuredLogFileName = FileUtils.prependBaseLogPath(System.getProperties(), Debug.BASE_LOG_DIRECTORY_PROP, configuredLogFileName);

            Debug.log( Debug.SYSTEM_CONFIG, "Metrics log files name [" + configuredLogFileName + "]" );

            // Check the Metrics Db Logging property
            temp = getProperty(null, METRICS_DB_LOGGING_PROP);

            metricsDbLogging = StringUtils.getBoolean(temp, false);

            Debug.log( Debug.SYSTEM_CONFIG, "Metrics DB Logging property is set to [" + metricsDbLogging + "]" );

            // Check the Metrics Db Logging level property
            temp = getProperty( null, METRICS_DB_LOGGING_LEVEL_PROP );

            if ( temp != null )
            {
                metricsDbLoggingLevel = Integer.parseInt( temp );

                Debug.log( Debug.SYSTEM_CONFIG, "Metrics DB Logging level is set to [" + metricsDbLoggingLevel + "]" );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "in MetricsAgent.configureFromProperties():\n" + e.toString() );
        }
    }


    /**
     * Configure the metrics agent from properties.
     */
    public static final void configureFromSystemsProperties ( )
    {
    	configureFromProperties( System.getProperties() );
    }


    /**
     * Configure the metrics agent from properties.
     */
    public static final void configureFromProperties ( Hashtable props )
    {
        try
        {
            // First reconfigure debug log levels.
            String temp = getProperty( props, METRICS_LOG_LEVELS_PROP );

            if ( temp == null )
                return;

			Debug.log( Debug.SYSTEM_CONFIG, "Setting metrics logging levels to [" + temp + "]");

            configureLevels( temp );

            temp = getProperty( props, MAX_METRICS_WRITES_PROP );

            if ( temp != null )
            {
                maxNumWrites = Long.parseLong( temp );

                Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of writes to a metrics log file is set to [" + maxNumWrites + "]" );
            }

            temp = getProperty( props, MAX_METRICS_LOG_FILE_COUNT_PROP );

            if ( temp != null )
            {
                maxLogFileCount = Integer.parseInt( temp );

                Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of metrics log files to create [" + maxLogFileCount + "]" );
            }

        	// Check debug log files.
            configuredLogFileName = getProperty( props, METRICS_LOG_FILE_NAME_PROP );
            
            // Set the configuredLogFileName pointing to correct log-base directory, if present. 
            // Else use the already set value.
            configuredLogFileName = FileUtils.prependBaseLogPath((Properties)props, Debug.BASE_LOG_DIRECTORY_PROP, configuredLogFileName);

            Debug.log( Debug.SYSTEM_CONFIG, "Metrics log files name [" + configuredLogFileName + "]" );

            // Check the Metrics Db Logging property
            temp = getProperty(props, METRICS_DB_LOGGING_PROP);

            metricsDbLogging = StringUtils.getBoolean(temp, false);

            Debug.log( Debug.SYSTEM_CONFIG, "Metrics DB Logging property is set to [" + metricsDbLogging + "]" );

            // Check the Metrics Db Logging level property
            temp = getProperty( props, METRICS_DB_LOGGING_LEVEL_PROP );

            if ( temp != null )
            {
                metricsDbLoggingLevel = Integer.parseInt( temp );

                Debug.log( Debug.SYSTEM_CONFIG, "Metrics DB Logging level is set to [" + metricsDbLoggingLevel + "]" );
            }

        }
        catch ( Exception e )
        {
            Debug.error( "in MetricsAgent.configureFromProperties():\n" + e.toString() );
        }
    }


    /**
     * Set the log-levels from the configuration string.
     *
     * @param  levels  Logging levels as they appear in
     *                 the configuration properties.
     */
    private static void configureLevels ( String levels )
    {
        if ( levels == null )
            return;

        StringTokenizer st = new StringTokenizer( levels, " " );

        while ( st.hasMoreTokens() )
            enabledLevels.add( st.nextToken() );

        Debug.log( Debug.SYSTEM_CONFIG, "Enabled metrics log levels [" + enabledLevels.toString() + "]" );
    }


    /**
     * Write any log messages, using either System.out or
     * a file-based writer.
     */
    private static final void write ( Writer logWriter, String msg ) throws IOException
    {
        if ( logWriter == null )
            System.out.print( msg );
        else
            logWriter.write( msg );
    }


    /**
     * Flush any buffered messages, using either System.out or
     * a file-based writer.
     */
    private static final void flush ( Writer logWriter ) throws IOException
    {
        if ( logWriter == null )
            System.out.flush( );
        else
            logWriter.flush( );
    }


    /**
     * Get the LogFileState object associated with this log.
     * NOTE: This method should only be called once per log entry.
     *
     * @return  The LogFileState object associated with the log file.
     */
    private static final synchronized LogFileState getLogFile ( )
    {
        // If the log file hasn't already been created, create it now
        // and add it to the cache for subsequent re-use.
        if ( logFile == null )
            logFile = new LogFileState( configuredLogFileName, maxLogFileCount );

        return logFile;
    }


    /**
     * Get named property from the given property hash table (if non-null),
     * and if no value is found, attempt to get it from the system properties.
     *
     * @param  props  Optional hash table to search first for property.
     * @param  name  Name of property.
     *
     * @return  Property value, or null if not found.
     */
    private static final String getProperty ( Hashtable props, String name )
    {
        String value = null;

        if ( props != null )
            value = (String)props.get( name );

        if ( value == null )
            value = System.getProperty( name );

        return value;
    }

    // Should never need to create MetricsAgent objects!
    private MetricsAgent ( )
    {
        // NOT USED!
    }


    /**
     * Unit testing ...
     */
    public static void main ( String[] args )
    {
            Debug.showLevels( );
            Debug.enableAll( );

        try
        {
            Properties props = new Properties();
            props.put( METRICS_LOG_LEVELS_PROP, "BO WF SYNC_API ASYNC_API GATEWAY" );
            props.put( METRICS_LOG_FILE_NAME_PROP, "console" );
            props.put( MAX_METRICS_WRITES_PROP, "10" );
            props.put( MAX_METRICS_LOG_FILE_COUNT_PROP, "3" );
            props.put( METRICS_LOG_FILE_NAME_PROP, "metrics.out" );
            // System.setProperties( props );
            MetricsAgent.configureFromProperties( props );

            long startTime = System.currentTimeMillis();

            for ( int Ix = 0;  Ix < 50;  Ix ++ )
            {
                Thread.currentThread().sleep( 1000 );

                MetricsAgent.logBO( startTime, "metaDataName", "boid", "currentState", "operation" );
                MetricsAgent.logWF( startTime, "metaDataName", "boid", "driverKey", "driverType" );
                MetricsAgent.logSyncAPI( startTime, "action" );
                MetricsAgent.logAsyncAPI( startTime, "metaDataName", "boid" );
                MetricsAgent.logGateway( startTime, "driverKey", "driverType" );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * Class encapsulating all state required by one log file.
     */
    private static class LogFileState
    {
        /**
         * Create an object encapsulating the log file's state.
         *
         * @param  fileName  Name of the log file.
         */
        public LogFileState ( String fileName, int maxLogFileCount )
        {
            logFileName = fileName;

            this.maxLogFileCount = maxLogFileCount;

            logWriter = null;

            curNumWrites = 0;

            curFileNum = 0;

            timestamp = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );
        }


        /**
         * Get the Writer object associated with this log.
         * NOTE: This method should only be called once per log entry,
         * and clients should lock against this object for the duration
         * of one write of a complete log message to the associated log file.
         *
         * @return  Writer associated with log file.
         */
        public final Writer getLogFile ( )
        {
            // If we have a file name, and it's not 'console' ...
            if ( (logFileName != null) && !"console".equalsIgnoreCase( logFileName ) )
            {
                // Check for file rotation need based on whether maximum-number-of-writes property was set or not.
                if ( maxNumWrites != NO_MAX_WRITES )
                {
                    // Bump the number-of-writes counter.
                    curNumWrites ++;

                    // If we've filled up the now active file ...
                    if ( curNumWrites >= maxNumWrites )
                    {
                        // Get the index of the current victim file to replace.
                        if ( curFileNum < maxLogFileCount )
                            curFileNum ++;
                        else
                            curFileNum = 1;

                        // Close active file before moving it to current file (if it exists).
                        if ( logWriter != null )
                        {
                            try
                            {
                                logWriter.close( );
                            }
                            catch ( IOException ioe )
                            {
                                Debug.error( "Could not close log file:\n" + ioe.toString() );
                            }

                            // Null out file object so that a new one will be opened.
                            logWriter = null;
                        }

                        // If it exists, rename last active file to current file.
                        File replacement = new File( logFileName );

                        if ( replacement.exists() )
                        {
                            String victimFileName = logFileName + curFileNum;

                            // Delete the pre-existing current file before renaming the last active file to have the same name.
                            File victim = new File( victimFileName );

                            if ( victim.exists() )
                            {
                                try
                                {
                                    victim.delete( );
                                }
                                catch ( Exception e )
                                {
                                    Debug.error( "Could not delete log file:\n" + e.toString() );
                                }
                            }

                            try
                            {
                                victim = new File( victimFileName );

                                replacement.renameTo( victim );
                            }
                            catch ( Exception e )
                            {
                                Debug.error( "Could not rename log file:\n" + e.toString() );
                            }
                        }
                    }
                }

                // If file isn't opened, open it now.
                if ( logWriter == null )
                {
                    try
                    {
                        // Reset the number-of-writes counter since we're opening a new file.
                        // (We set it to 1 because the timestamp written below is the first log entry.)
                        curNumWrites = 1;

                        // Currently-active log file doesn't have current-file-number suffix in name.
                        logWriter = new BufferedWriter( new FileWriter( logFileName ) );

                        // Timestamp the newly-created log.
                        Date d = new Date( );

                        write( logWriter, "******* Metrics log creation date [" + d.toString() + "]. *******\n" );
                    }
                    catch ( IOException ioe )
                    {
                        Debug.error( "Could not redirect metrics log messages:\n" + ioe.toString() );
                    }
                }
            }

            return logWriter;
        }

        // Used for timestamp formatting.
        public SimpleDateFormat timestamp;

        // Name of log file.
        private final String logFileName;

        // File to write data to.
        private Writer logWriter;

        // Current number of writes to this file.
        private long curNumWrites;

        // Current file number in circular buffer of files (i.e, 1->2->1 ...).
        private int curFileNum;

        // Default number of files is 2.
        private int maxLogFileCount = 2;
    }


    private static Set enabledLevels = new HashSet( );

    private static String configuredLogFileName;

    private static final int NO_MAX_WRITES = -1;
    private static long maxNumWrites = NO_MAX_WRITES;

    // The maximum number of files to create before wrapping and overwriting an existing file
    // in a circular buffer fashion. (Ex: A value of 3 means 1->2->3->1->...)
    private static int maxLogFileCount = 2;

    private static int metricsDbLoggingLevel;

    private static boolean metricsDbLogging;

    private static LogFileState logFile;
}

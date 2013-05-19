/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/DBSelectQuery.java#4 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.text.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.monitor.*;


/**
 * A generic message-processor for extracting row values from the database
 * via the execution of a configurable SQL SELECT statmement.
 */
public class DBSelectQuery extends DBMessageProcessorBase
{
    /**
     * Property indicating name of the database table to select row from.
     */
    public static final String TABLE_NAME_PROP = "TABLE_NAME";

    /**
     * Property giving the SQL SELECT statement to execute.
     */
    public static final String SQL_QUERY_STATEMENT_PROP = "SQL_QUERY_STATEMENT";

    /**
     * Property indicating the location into which the result should be placed.
     */
    public static final String RESULT_LOCATION_PROP = "RESULT_LOCATION";

    /**
     * Property indicating whether a single row or multiple rows should be returned.
     */
    public static final String SINGLE_RESULT_FLAG_PROP = "SINGLE_RESULT_FLAG";

    /**
     * Property giving date format for date type results.
     */
    public static final String OUTPUT_DATE_FORMAT_PROP = "OUTPUT_DATE_FORMAT";

    /**
     * Property indicating whether SQL operation should be part of overall driver transaction.
     */
    public static final String TRANSACTIONAL_LOGGING_PROP = "TRANSACTIONAL_LOGGING";

    /**
     * Property indicating separator token used to separate individual location alternatives.
     */
    public static final String LOCATION_SEPARATOR_PROP = "SEPARATOR";


    /**
     * Property prefix giving column data type.
     */
    public static final String COLUMN_TYPE_PREFIX_PROP = "COLUMN_TYPE";

    /**
     * Property prefix giving date format for date types used in query criteria.
     */
    public static final String INPUT_DATE_FORMAT_PREFIX_PROP = "INPUT_DATE_FORMAT";

    /**
     * Property prefix giving location of column value.
     */
    public static final String LOCATION_PREFIX_PROP = "LOCATION";

    /**
     * Property prefix giving default value for column.
     */
    public static final String DEFAULT_PREFIX_PROP = "DEFAULT";
    
    /**
     * Name of parent node for one row's-worth of data.
     */
    public static final String ROW_DELIMITER = "ROW";


    // Types of supported columns that require special processing.
    public static final String COLUMN_TYPE_DATE = "DATE";

    // Token value for LOCATION indicating that the entire contents of the message-
    // processor's input message-object should be used as the column value.
    public static final String PROCESSOR_INPUT = "PROCESSOR_INPUT";

    public static final String DEFAULT_LOCATION_SEPARATOR = "|";

    /**
     * Property indicating whether customer criteria should be added or not to the SQL statement.
     * If not set, default value is true.
     */
    public static final String USE_CUSTOMER_ID_PROP = "USE_CUSTOMER_ID";

    /**
     * Property indicating whether SubDomain criteria should be added or not to the SQL statement.
     * If not set, default value is false.
     */
    public static final String USE_SUBDOMAIN_ID_PROP = "USE_SUBDOMAIN_ID";

    /**
     * Property indicating the namespace associated with the CustomerID column if any.
     * If not set, this value is ignored.
     */
    public static final String CUSTOMER_ID_NAMESPACE_PROP = "CUSTOMER_ID_NAMESPACE";

    /**
     * Property indicating the namespace associated with the SubDomainID column if any.
     * If not set, this value is ignored.
     */
    public static final String SUBDOMAIN_ID_NAMESPACE_PROP = "SUBDOMAIN_ID_NAMESPACE";

    /**
     * Constructor.
     */
    public DBSelectQuery ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Creating database-select-query message-processor." );

        columns = new LinkedList( );
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );
        
        // Get configuration properties specific to this processor.
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "DBSelectQuery: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );
        
        tableName = getRequiredPropertyValue( TABLE_NAME_PROP, errorBuffer );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Database table to query against is [" + tableName + "]." );

        sqlStmt = getRequiredPropertyValue( SQL_QUERY_STATEMENT_PROP, errorBuffer );

        // keep a copy of the unadulterated (no CustomerId clause)
        // SQL statement
        originalSQLStmt = sqlStmt;
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "SQL query is [" + sqlStmt + "]." );

        resultLocation = getRequiredPropertyValue( RESULT_LOCATION_PROP, errorBuffer );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Location to store result into [" + resultLocation + "]." );

        String strTemp = getPropertyValue( SINGLE_RESULT_FLAG_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                singleResultFlag = getBoolean( strTemp );
            } catch ( FrameworkException e ) {
                errorBuffer.append ( "Property value for " + SINGLE_RESULT_FLAG_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }//if

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Single query result expected? [" 
                   + singleResultFlag + "]." );

        strTemp = getPropertyValue( TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                usingContextConnection = getBoolean( strTemp );
            } catch ( FrameworkException e ) {
                errorBuffer.append ( "Property value for " + TRANSACTIONAL_LOGGING_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Database-select-query will participate in overall driver transaction? [" 
                   + usingContextConnection + "]." );

        outputDateFormat = getPropertyValue( OUTPUT_DATE_FORMAT_PROP );

        separator = getPropertyValue( LOCATION_SEPARATOR_PROP );
        
        if ( !StringUtils.hasValue( separator ) )
            separator = DEFAULT_LOCATION_SEPARATOR;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Location separator token [" + separator + "]." );


        // Loop until all column configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String colType = getPropertyValue( PersistentProperty.getPropNameIteration( COLUMN_TYPE_PREFIX_PROP, Ix ) );

            String dateFormat = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_DATE_FORMAT_PREFIX_PROP, Ix ) );

            String location = getPropertyValue( PersistentProperty.getPropNameIteration( LOCATION_PREFIX_PROP, Ix ) );

            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_PREFIX_PROP, Ix ) );

            if ( !StringUtils.hasValue(colType) && !StringUtils.hasValue(dateFormat) 
                 && !StringUtils.hasValue(location) && !StringUtils.hasValue(defaultValue) )
                break;

            try
            {
                // Create a new column data object and add it to the list.
                ColumnData cd = new ColumnData( colType, dateFormat, location, defaultValue );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
                
                columns.add( cd );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create column data description:\n" 
                                               + e.toString() );
            }
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Number of columns participating in dynamic query criteria [" 
                   + columns.size() + "]." );

        strTemp = getPropertyValue( USE_CUSTOMER_ID_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                useCustomerId = getBoolean( strTemp );
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for " + USE_CUSTOMER_ID_PROP +
                  " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "To use customer id in SQL statement?[" + useCustomerId + "]." );

        customerIdNamespace = getPropertyValue( CUSTOMER_ID_NAMESPACE_PROP );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Customer ID namespace [" + customerIdNamespace + "]." );

        // SubDomain info

        strTemp = getPropertyValue( USE_SUBDOMAIN_ID_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                useSubDomainId = getBoolean( strTemp );
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for " + USE_SUBDOMAIN_ID_PROP +
                  " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "To use SubDomain id in SQL statement?[" + useSubDomainId + "]." );

        subDomainIdNamespace = getPropertyValue( SUBDOMAIN_ID_NAMESPACE_PROP );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "SubDomain ID namespace [" + subDomainIdNamespace + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "DBSelectQuery: Initialization done." );
    }


    /**
     * Extract data values from the context/input, and use them to
     * query for rows from a database table.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException 
    {
        if ( inputObject == null )
            return null;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "DBSelectQuery: processing ..." );
        
        Document result = null;

        // Reset the values in cases where we're querying again.
        resetColumnValues( );

        // Reset the query back to its original value in case we're querying
        // again.
        sqlStmt = originalSQLStmt;

        // Extract the column values from the arguments.
        extractQueryCriteriaValues( mpContext, inputObject );
        
        Connection dbConn = null;

        try
        {
            // Get a database connection from the appropriate location - based on transaction characteristics.
            if ( usingContextConnection )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Database logging is transactional, so getting connection from context." );

                dbConn = mpContext.getDBConnection( );
            }
            else
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Database logging is not transactional, so getting connection from pool." );

                dbConn = DBConnectionPool.getInstance().acquireConnection( );
            }

            // Get the data from the database.
            result = query( dbConn );
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: DBSelectQuery: Attempt to query database failed with error: " 
                            + e.getMessage();

            Debug.log( Debug.ALL_ERRORS, errMsg );

            // Re-throw the exception to the driver.
            throw new ProcessingException( errMsg );
        }
        finally
        {
            // If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, return the connection previously acquired back to the resource pool.
            if ( !usingContextConnection && (dbConn != null) )
            {
                try
                {
                    DBConnectionPool.getInstance().releaseConnection( dbConn );
                }
                catch ( Exception e )
                {
                    Debug.log( Debug.ALL_ERRORS, e.toString() );
                }
            }
        }

        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
        {
            Debug.log( Debug.MSG_DATA, "Result:\n" 
                       + XMLLibraryPortabilityLayer.convertDomToString( result ) );
        }

        // Place the result in the configured location.
        set( resultLocation, mpContext, inputObject, result );


        // Pass the input on to the output.
        return( formatNVPair( inputObject ) );
    }

    
    /**
     * Query for rows from the database table using the given connection.
     *
     * @param  dbConn  The database connection to perform the SQL SELECT operation against.
     *
     * @return  Document containing query results.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private Document query ( Connection dbConn ) throws ProcessingException
    {
        PreparedStatement pstmt = null;

        ThreadMonitor.ThreadInfo tmti = null;

        try
        {
            SimpleDateFormat sdf = null;

            if ( StringUtils.hasValue(outputDateFormat) )
                sdf = new SimpleDateFormat( outputDateFormat );

            if ( useCustomerId )
            {
                //Add customer information
                sqlStmt = SQLBuilder.addCustomerIDCondition( sqlStmt, customerIdNamespace );
            }

            /**
             * Here making subDomainId implicitly empty string i.e. ""
             * if there is no subDomainId associated to present user.
             *
             * If subDomainId need to be applied in query and
             * in some case it could be empty and
             * in some case it could be available
             * then following check need to be applied.
             *
             * As per above scenario user has to apply below condition.
             * </b>(subDomainId is null or subDomainId='${SDID}')</b>
             */
            String subDomainId = CustomerContext.getInstance().getSubDomainId ();
            if (!StringUtils.hasValue ( subDomainId , true ))
                subDomainId = "";

            if (useSubDomainId)
            {
                //Add SubDomain information
                sqlStmt = SQLBuilder.addSubDomainIDCondition ( sqlStmt, subDomainIdNamespace );
            }

            //If the query is a complex type query, then replace with CID_TOKEN if present,
            //with the customer ID.
            sqlStmt = StringUtils.replaceSubstrings(sqlStmt, CID_TOKEN,
                                               CustomerContext.getInstance().getCustomerID() );

            //If the query is a complex type query, then replace with SDID_TOKEN if present,
            //with the SubDomain ID.
            sqlStmt = StringUtils.replaceSubstrings(sqlStmt, SDID_TOKEN, subDomainId  );

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log( Debug.NORMAL_STATUS, "DBSelectQuery : Executing SQL:\n" + sqlStmt );

            tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] executing SQL: " + sqlStmt );

            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from the column data objects.
            populateSqlStatement( pstmt );

            // Execute the SQL SELECT operation.
            ResultSet rs = pstmt.executeQuery( );

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log( Debug.NORMAL_STATUS, "DBSelectQuery : Finished Executing SQL..");

            ResultSetMetaData rsmd = rs.getMetaData( );

            int numCols = rsmd.getColumnCount( );

            XMLMessageGenerator gen = new XMLMessageGenerator( tableName );

            int rowCount = 0;

            // Loop over rows returned by query ...
            for ( int Ix = 0;  rs.next();  Ix ++ )
            {
                rowCount ++;

                String iterationPrefix = ROW_DELIMITER + "(" + Ix + ").";

                // Loop over columns that are available for each returned row ...
                for ( int Jx = 1;  Jx <= numCols;  Jx ++ )
                {
                    String colValue = null;

                    String colName = rsmd.getColumnName( Jx );

                    int colType = rsmd.getColumnType( Jx );

                    if(Debug.isLevelEnabled(Debug.DB_DATA))
                        Debug.log( Debug.DB_DATA, "DBSelectQuery : Extracting result-set data for column [" 
                               + colName + "] of type [" + colType + "] ..." );

                    if ( (colType == Types.DATE) || (colType == Types.TIME) || (colType == Types.TIMESTAMP) )
                    {
                        if ( sdf == null )
                        {
                            throw new MessageException( "ERROR: Missing required date-format for extracting date query results from column [" 
                                                        + colName + "]." );
                        }

                        java.sql.Timestamp d = rs.getTimestamp( colName );

                        if ( rs.wasNull() )
                        {
                            if(Debug.isLevelEnabled(Debug.DB_DATA))
                                Debug.log( Debug.DB_DATA, "Skipping column [" + colName + "] with null value ..." );

                            continue;
                        }

                        colValue = sdf.format( d );
                    }
                    else  // It's not a date, so extract as string.
                    {
                        colValue = rs.getString( colName );

                        if ( rs.wasNull() )
                        {
                            if(Debug.isLevelEnabled(Debug.DB_DATA))
                                Debug.log( Debug.DB_DATA, "Skipping column [" + colName + "] with null value ..." );

                            continue;
                        }
                    }

                    if ( singleResultFlag )
                    {
                        if ( Ix > 0 )
                        {
                            throw new MessageException( "ERROR: Single-result flag was set to 'true', " 
                                                        + "but SQL query returned multiple rows." );
                        }

                        gen.setValue( colName, colValue );
                    }
                    else
                        gen.setValue( iterationPrefix + colName, colValue );
                }
            }

            rs.close( );

            // Check that the number of database rows selected was acceptable, based on configuration.
            checkSQLOperationResultCount( rowCount );

            return( gen.getDocument() );
        }
        catch ( SQLException sqle )
        {
            throw new ProcessingException( "ERROR: Could not select row from database table [" + tableName 
                                           + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Could not select row from database table [" + tableName 
                                           + "]:\n" + e.toString() );
        }
        finally
        {
            if ( pstmt != null )
            {
                try
                {
                    pstmt.close( );
                }
                catch ( SQLException sqle )
                {
                    Debug.log( Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle) );
                }
            }

            ThreadMonitor.stop( tmti );
        }
    }


    /**
     * Populate the SQL SELECT statement from the column data.
     *
     * @param  pstmt  The prepared statement object to populate.
     *
     * @exception Exception  thrown if population fails.
     */
    private void populateSqlStatement ( PreparedStatement pstmt ) throws Exception
    {
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Populating SQL SELECT statement ..." );

        Iterator iter = columns.iterator( );

        int Ix = 0;
        for ( Ix = 1;  iter.hasNext();  Ix ++ )
        {
            ColumnData cd = (ColumnData)iter.next( );
            
            if(Debug.isLevelEnabled(Debug.DB_DATA))
                Debug.log( Debug.DB_DATA, "Populating prepared-statement slot [" + Ix + "]." );

            // Default is no column type specified.
            if ( !StringUtils.hasValue(cd.columnType) )
            {
                if(Debug.isLevelEnabled(Debug.DB_DATA))
                    Debug.log( Debug.MSG_DATA, "Value for column is [" + cd.value.toString() + "]." );
                
                pstmt.setObject( Ix, cd.value );
            }
            else
            if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE) )
            {
                String val = (String)(cd.value);

                if(Debug.isLevelEnabled(Debug.DB_DATA))
                    Debug.log( Debug.MSG_DATA, "Value for date column is [" + val + "]." );

                if ( !StringUtils.hasValue( cd.dateFormat ) )
                {
                    throw new FrameworkException( "ERROR: Configuration for date column [" 
                                                  + cd.describe() + "] does not specify date format." );
                }

                SimpleDateFormat sdf = new SimpleDateFormat( cd.dateFormat );

                java.util.Date d = sdf.parse( val );

                pstmt.setTimestamp( Ix, new java.sql.Timestamp(d.getTime()) );
            }
            else
            {
                throw new FrameworkException( "ERROR: Invalid column-type property value given in configuration for [" 
                                              + cd.describe() + "] ." );
            }
        }

        if ( useCustomerId )
        {
            //Increment Ix slot number.
            SQLBuilder.populateCustomerID( pstmt, Ix++ );
        }

        if ( useSubDomainId )
        {
            //Increment Ix slot number.
            SQLBuilder.populateSubDomainID( pstmt, Ix );
        }

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Done populating SQL SELECT statement." );
    }


    /**
     * Extract data for each column from the input message/context.
     *
     * @param context The message context.
     * @param inputObject The input object.
     *
     * @exception MessageException thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractQueryCriteriaValues ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Extracting message data to use in query criteria ..." );
        
        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            // If location was given, try to get a value from it.
            if ( StringUtils.hasValue( cd.location ) )
            {
                // Location contains one or more alternative locations that could
                // contain the column's value.
                StringTokenizer st = new StringTokenizer( cd.location, separator );
                
                // While location alternatives are available ...
                while ( st.hasMoreTokens() )
                {
                    // Extract a location alternative.
                    String loc = st.nextToken().trim( );
                    
                    // If the value of location indicates that the input message-object's 
                    // entire value should be used as the column's value, extract it.
                    if ( loc.equalsIgnoreCase( PROCESSOR_INPUT ) )
                    {
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log( Debug.MSG_DATA, "Using message-object's contents as the column's value." );
                        
                        cd.value = inputObject.get( );
                        
                        break;
                    }
                    
                    // Attempt to get the value from the context or input object.
                    if ( exists( loc, context, inputObject ) )
                    {
                        cd.value = get( loc, context, inputObject );
                        
                        // If we found a value, we're done with this column.
                        if ( cd.value != null )
                        {
                            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                                Debug.log( Debug.MSG_DATA, "Found value for column at location [" + loc + "]." );
                            
                            break;
                        }
                    }
                }
            }
            
            // If no value was obtained from location in context/input, try to set it from default value (if available).
            if ( cd.value == null )
            {
                cd.value = cd.defaultValue;

                if ( cd.value != null)
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_DATA, "Using default value for column." );
                }
                else
                {
                    // Signal error to caller.
                    throw new ProcessingException( "ERROR: Could not locate required value for column [" + cd.describe() 
                                                   + "] used in query against database table [" + tableName + "]." );
                }
            }
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "Done extracting message data to use in query criteria." );
    }


    /**
     * Reset the column values in the list.
     */
    private void resetColumnValues ( )
    {
        Debug.log (Debug.MSG_STATUS, "Resetting column values ..." );
        
        Iterator iter = columns.iterator( );
        
        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );
            
            cd.value = null;
        }
    }
    
    
    private String tableName = null;
    private String sqlStmt = null;
    private String originalSQLStmt = null;
    private boolean singleResultFlag = true;
    private boolean usingContextConnection = true;
    private String outputDateFormat;
    private String separator = null;
    private String resultLocation;
    private boolean useCustomerId = true;
    private boolean useSubDomainId = false;
    private String customerIdNamespace = null;
    private String subDomainIdNamespace = null;
    
    private List columns;


    /**
     * Class ColumnData is used to encapsulate a description of a single column
     * and its associated value.
     */
    private static class ColumnData
    {
        public final String columnType;
        public final String dateFormat;
        public final String location;
        public final String defaultValue;

        public Object value = null;


        public ColumnData ( String columnType, String dateFormat, 
                            String location, String defaultValue ) throws FrameworkException
        {
            this.columnType   = columnType;
            this.dateFormat   = dateFormat;
            this.location     = location;
            this.defaultValue = defaultValue;
        }


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: " );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "type [" );
                sb.append( columnType );
            }

            if ( StringUtils.hasValue( dateFormat ) )
            {
                sb.append( "], date-format [" );
                sb.append( dateFormat );
            }

            if ( StringUtils.hasValue( location ) )
            {
                sb.append( "], location [" );
                sb.append( location );
            }

            if ( StringUtils.hasValue( defaultValue ) )
            {
                sb.append( "], default [" );
                sb.append( defaultValue );
            }

            if ( value != null )
            {
                sb.append( "], value [" );
                sb.append( value.toString() );
            }

            sb.append( "]." );
            
            return( sb.toString() );
        }
    }
}

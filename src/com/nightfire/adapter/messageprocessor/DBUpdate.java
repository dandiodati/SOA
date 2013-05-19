package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.sql.*;
import java.text.*;
import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.monitor.*;


/**
 * This is a generic message-processor for updating the database. All the
 * columns that are to be updated are specified in the SQL_SET_STATEMENT in persistent property
 * column to eb used in the where clause are specified in the SQL_WHERE_STATEMENT in persistent property
*/

public class DBUpdate extends DBMessageProcessorBase
{
    /**
     * Property indicating name of the database table to update row into.
     */
    public static final String TABLE_NAME_PROP = "TABLE_NAME";
    public static final String SQL_SET_STATEMENT_PROP = "SQL_SET_STATEMENT";
    public static final String SQL_WHERE_STATEMENT_PROP = "SQL_WHERE_STATEMENT";
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
    public static final String SET_COLUMN_TYPE_PREFIX_PROP = "SET_COLUMN_TYPE";
    /**
     * Property prefix giving date format for date types.
     */
    public static final String SET_DATE_FORMAT_PREFIX_PROP = "SET_DATE_FORMAT";
    /**
     * Property prefix giving location of column value.
     */
    public static final String SET_LOCATION_PREFIX_PROP = "SET_LOCATION";
    /**
     * Property prefix indicating whether column value is optional or required.
     */
    public static final String SET_OPTIONAL_PREFIX_PROP = "SET_OPTIONAL";
    /**
     * Property prefix giving default value for column.
     */
    public static final String SET_DEFAULT_PREFIX_PROP = "SET_DEFAULT";
    /**
     * Property prefix giving column data type.
     */
    public static final String WHERE_COLUMN_TYPE_PREFIX_PROP = "WHERE_COLUMN_TYPE";

    /**
     * Property prefix giving date format for date types.
     */
    public static final String WHERE_DATE_FORMAT_PREFIX_PROP = "WHERE_DATE_FORMAT";

    /**
     * Property prefix giving location of column value.
     */
    public static final String WHERE_LOCATION_PREFIX_PROP = "WHERE_LOCATION";

    /**
     * Property prefix indicating whether column value is optional or required.
     */
   // public static final String WHERE_OPTIONAL_PREFIX_PROP = "WHERE_OPTIONAL";

    /**
     * Property prefix giving default value for column.
     */
    public static final String WHERE_DEFAULT_PREFIX_PROP = "WHERE_DEFAULT";


    // Types of supported columns that require special processing.
    public static final String COLUMN_TYPE_DATE        = "DATE";
    public static final String COLUMN_TYPE_TEXT_BLOB   = "TEXT_BLOB";
    public static final String COLUMN_TYPE_BINARY_BLOB = "BINARY_BLOB";

    // Token indicating that current date/time should be used for date field values.
    public static final String SYSDATE = "SYSDATE";

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
     * Constructor.
     */
    public DBUpdate ( )
    {
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG,"Creating DBUpdate message-processor." );

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
            Debug.log( Debug.SYSTEM_CONFIG, "DBUpdate: Initializing..." );

        errorBuffer = new StringBuffer( );

        tableName = getRequiredPropertyValue( TABLE_NAME_PROP, errorBuffer );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Database table to update to is [" + tableName + "]." );


        String strTemp = getPropertyValue( TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                usingContextConnection = getBoolean( strTemp );
            } catch ( FrameworkException e ) {
                errorBuffer.append ( "Property value for " + TRANSACTIONAL_LOGGING_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
            }

        }//if

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Logger will participate in overall driver transaction? ["
                   + usingContextConnection + "]." );

        separator = getPropertyValue( LOCATION_SEPARATOR_PROP );

        if ( !StringUtils.hasValue( separator ) )
            separator = DEFAULT_LOCATION_SEPARATOR;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Location separator token [" + separator + "]." );

        sqlSetStmt = getRequiredPropertyValue( SQL_SET_STATEMENT_PROP, errorBuffer );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "'set part' of SQL query is [" + sqlSetStmt + "]." );

        sqlWhereStmt = getPropertyValue( SQL_WHERE_STATEMENT_PROP);
        // save copy of original SQL "where" stmt
        originalSQLWhereStmt = sqlWhereStmt;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "'where part of' SQL query is [" + sqlWhereStmt + "]." );
        
        populateColumnData();

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
        {
            Debug.log( Debug.SYSTEM_CONFIG, "To use SubDomain id in SQL statement?[" + useSubDomainId + "]." );
            Debug.log( Debug.SYSTEM_CONFIG, "DBUpdate: Initialization done." );
        }
    }

    /**
     * This method populates the list fo columndata
     *
     * @exception ProcessingException when initialization fails
    **/

    private void populateColumnData() throws ProcessingException
    {
        // Loop until all SetColumn configuration properties have been read ...
        int count = -1;
        int index = 0;
        // to find the number of Column values to be filled ...this is counted by number of ? in the string
        while(index != -1)
        {
            count++;
            index = sqlSetStmt.indexOf("?" , index+1);
        }
        if(count < 1)
            throw(new ProcessingException("ERROR:DBUpdate:SQL_SET_STATEMENT property not set properly in database."));

        for ( int Ix = 0;  true;  Ix ++ )
        {
             String optional = getPropertyValue( PersistentProperty.getPropNameIteration( SET_OPTIONAL_PREFIX_PROP, Ix ));
            // If we can't find next optional property we're done
            if ( !StringUtils.hasValue( optional ) )
                break;

            String colType = getPropertyValue( PersistentProperty.getPropNameIteration( SET_COLUMN_TYPE_PREFIX_PROP, Ix ) );
            String dateFormat = getPropertyValue( PersistentProperty.getPropNameIteration( SET_DATE_FORMAT_PREFIX_PROP, Ix ) );
            String location = getPropertyValue( PersistentProperty.getPropNameIteration( SET_LOCATION_PREFIX_PROP, Ix ) );
            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( SET_DEFAULT_PREFIX_PROP, Ix ) );

            try
            {
                // Create a new column data object and add it to the list.
                ColumnData cd = new ColumnData(  colType, dateFormat, location, defaultValue, optional );
                Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
                columns.add( cd );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create column data description:\n"
                                               + e.toString() );
            }
        }

        int setColumnsCount = columns.size();

        if(setColumnsCount!=count)
            throw new ProcessingException( "ERROR: Mismatch in the setStatement and setAttributes" );
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of columns to update [" + setColumnsCount + "]." );


        if(sqlWhereStmt==null) // no WHERE clause to be appended
        {
             // If any of the required properties are absent, indicate error to caller.
            if ( errorBuffer.length() > 0 )
            {
                String errMsg = errorBuffer.toString( );
                Debug.log( Debug.ALL_ERRORS, errMsg );
                throw new ProcessingException( errMsg );
            }
            return;
        }
        // Loop until all WhereColumn configuration properties have been read ...
        count = -1;
        index = 0;
        // to find the number of Column values to be filled in WHERE clause...this is counted by number of ? in the string
        while(index != -1)
        {
            count++;
            index = sqlWhereStmt.indexOf("?" , index+1);
        }

        for ( int Ix = 0;  true;  Ix ++ )
        {
            String location = getPropertyValue( PersistentProperty.getPropNameIteration( WHERE_LOCATION_PREFIX_PROP, Ix ) );
            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( WHERE_DEFAULT_PREFIX_PROP, Ix ) );

            //String optional = getPropertyValue( PersistentProperty.getPropNameIteration( WHERE_OPTIONAL_PREFIX_PROP, Ix ));
            // If we can't find default or location then , we're done.
            if ( !StringUtils.hasValue( location ) && ! StringUtils.hasValue( defaultValue ))
                break;

            String colType = getPropertyValue( PersistentProperty.getPropNameIteration( WHERE_COLUMN_TYPE_PREFIX_PROP, Ix ) );
            String dateFormat = getPropertyValue( PersistentProperty.getPropNameIteration( WHERE_DATE_FORMAT_PREFIX_PROP, Ix ) );
            try
            {
                // Create a new column data object and add it to the list.
                ColumnData cd = new ColumnData(  colType, dateFormat, location, defaultValue, "FALSE" );
                Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
                columns.add( cd );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create column data description:\n"
                                               + e.toString() );
            }
        }

        int whereColumnsCount = columns.size() - setColumnsCount;
        if(whereColumnsCount!=count)
            throw new ProcessingException( "ERROR: Unequal no. of fields in the whereStatement and whereAttributes" );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of columns in where clause [" + whereColumnsCount + "]." );


        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );
            Debug.log( Debug.ALL_ERRORS, errMsg );
            throw new ProcessingException( errMsg );
        }
    }


    /**
     * Extract data values from the context/input, and use them to
     * update a row into the configured database table.
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
            Debug.log( Debug.MSG_STATUS, "DBUpdate: processing ... " );

        // Reset the values in cases where we're logging again.
        resetColumnValues( );

        // Reset where clause in case we're logging again.
        sqlWhereStmt = originalSQLWhereStmt;        

        // Extract the column values from the arguments.
        extractMessageData( mpContext, inputObject );

        Connection dbConn = null;

        try
        {
            // Get a database connection from the appropriate location - based on transaction characteristics.
            if ( usingContextConnection )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Database update is transactional, so getting connection from context." );

                dbConn = mpContext.getDBConnection( );
            }
            else
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Database update is not transactional, so getting connection from pool." );

                dbConn = DBConnectionPool.getInstance().acquireConnection( );
            }

            // Update the data to the database.
            update( dbConn );

            // If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, commit the changes now.
            if ( !usingContextConnection )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Committing data updated to database." );

                DBConnectionPool.getInstance().commit( dbConn );
            }
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: DBUpdate: Attempt to update to database failed with error: "
                            + e.getMessage();

            Debug.log( Debug.ALL_ERRORS, errMsg );

            // If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, roll back any changes now.
            if ( !usingContextConnection )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Rolling-back any database changes due to database-update." );

                try
                {
                    DBConnectionPool.getInstance().rollback( dbConn );
                }
                catch ( ResourceException re )
                {
                    Debug.log( Debug.ALL_ERRORS, re.getMessage() );
                }
            }

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

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }


    /**
     * update into the database table using the given connection.
     *
     * @param  dbConn  The database connection to perform the SQL update operation against.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void update ( Connection dbConn ) throws ProcessingException
    {
        // Make sure that at least one column value will be updateed.
        validate( );

        PreparedStatement pstmt = null;

        ThreadMonitor.ThreadInfo tmti = null;

        try
        {
            // Create the SQL statement using the column data objects.
            String sqlStmt = "UPDATE " + tableName + " " + sqlSetStmt ;
            if(sqlWhereStmt !=null)
                sqlStmt= sqlStmt + " " + sqlWhereStmt;

            if ( useCustomerId )
            {
                //Add customer information
                sqlStmt = SQLBuilder.addCustomerIDCondition( sqlStmt );
            }

            if (useSubDomainId)
            {
                //Add SubDomain information
                sqlStmt = SQLBuilder.addSubDomainIDCondition ( sqlStmt );
            }

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log( Debug.NORMAL_STATUS, "DBUpdate : Executing SQL:\n" + sqlStmt );

            tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] executing SQL: " + sqlStmt );

            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from the column data objects.
            populateSqlStatement( pstmt );

            // Execute the SQL update operation.
            int count = pstmt.executeUpdate( );

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log( Debug.NORMAL_STATUS, "DBUpdate : Finished Executing SQL.. Successfully updated [" + count + "] row(s) into table ["
                       + tableName + "]" );

            // Check that the number of database rows updated was acceptable, based on configuration.
            checkSQLOperationResultCount( count );
        }
        catch ( SQLException sqle )
        {
            throw new ProcessingException( "ERROR: Could not update row into database table [" + tableName
                                           + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Could not update row into database table [" + tableName
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
     * Populate the SQL update statement from the column data.
     *
     * @param  pstmt  The prepared statement object to populate.
     *
     * @exception Exception  thrown if population fails.
     */
    private void populateSqlStatement ( PreparedStatement pstmt ) throws Exception
    {
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Populating SQL update statement ..." );

        int Ix = 1;  // First value slot in prepared statement.

        Iterator iter = columns.iterator( );

        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );
            
            if(Debug.isLevelEnabled(Debug.DB_DATA))
                Debug.log( Debug.DB_DATA, "Populating prepared-statement slot [" + Ix + "] with column data ." + cd.describe() );
            
            if ( cd.value == null )
            {
                  // will this work for all data types??
                 pstmt.setNull(Ix,java.sql.Types.VARCHAR);
                 Ix++;
                 continue;
            }

            // Default is no column type specified.
            if ( !StringUtils.hasValue(cd.columnType) )
            {
                pstmt.setObject( Ix, cd.value );
            }
            else if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE) )
            {
                String val = (String)(cd.value);
                if ( val.equalsIgnoreCase(SYSDATE) )
                {
                    //get current date to set a value for this column
                    pstmt.setTimestamp(Ix,new java.sql.Timestamp(DateUtils.getCurrentTimeValue()));
                    Ix++;
                    continue;
                }
                // If we're not updating the current system date, the caller must
                // have provided an actual date value, which we must now parse.
                if ( !StringUtils.hasValue( cd.dateFormat ) )
                {
                    throw new FrameworkException( "ERROR: Configuration for date column does not specify date format." );
                }

                SimpleDateFormat sdf = new SimpleDateFormat( cd.dateFormat );

                java.util.Date d = sdf.parse( val );

                pstmt.setTimestamp( Ix, new java.sql.Timestamp(d.getTime()) );
            }
            else
            if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_TEXT_BLOB) )
            {
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Updating column [" + cd.describe() + "]." );

                DBLOBUtils.setCLOB( pstmt, Ix, Converter.getString(cd.value) );
            }
            else
            if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_BINARY_BLOB) )
            {
                byte[] bytes = null;

                if ( cd.value instanceof byte[] )
                    bytes = (byte[])cd.value;
                else
                if ( cd.value instanceof String )
                    bytes = ((String)(cd.value)).getBytes( );
                else
                if ( cd.value instanceof Document )
                    bytes = Converter.getString(cd.value).getBytes( );
                else
                {
                    throw new FrameworkException( "ERROR: Value for database table [" + tableName + "], column ["
                                                  + "] of type [" + cd.value.getClass().getName()
                                                  + "] can't be converted to byte stream." );
                }

                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Updating column [" + cd.describe() + "]." );

                DBLOBUtils.setBLOB( pstmt, Ix, bytes );
            }
            else
            {
                throw new FrameworkException( "ERROR: Invalid column-type property value ["
                                              + cd.columnType + "] given in configuration." );
            }

            // Advance to next value slot in prepared statement.
            Ix ++;
        }

        if ( useCustomerId )
        {
            //Use Ix slot number as-is, because it was already incremented before exiting above loop.
            SQLBuilder.populateCustomerID( pstmt, Ix++ );
        }
                
        if (useSubDomainId)
        {
            //Use Ix slot number as-is, because it was already incremented before exiting above loop.
            SQLBuilder.populateSubDomainID ( pstmt, Ix );
        }
                
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Done populating SQL UPDATE statement." );

    }


    /**
     * Extract data for each column from the input message/context.
     *
     * @param context The message context.
     * @param inputObject The input object.
     *
     * @exception MessageException  thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractMessageData ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Extracting message data to update ..." );

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
                        if(Debug.isLevelEnabled(Debug.MSG_DATA))
                            Debug.log( Debug.MSG_DATA, "Using message-object's contents as the column's value." );

                        cd.value = inputObject.get( );

                        break;
                    }

                    // Attempt to get the value from the context or input object.
                    if ( exists( loc, context, inputObject ) )
                        cd.value = get( loc, context, inputObject );

                    // If we found a value, we're done with this column.
                    if ( cd.value != null )
                    {
                        if(Debug.isLevelEnabled(Debug.MSG_DATA))
                            Debug.log( Debug.MSG_DATA, "Found value for column [" + cd.describe() + "] at location [" + loc + "]." );

                        break;
                    }
                }
            }

            // If no value was obtained from location in context/input, try to set it from default value (if available).
            if ( cd.value == null )
                cd.value = cd.defaultValue;

            // If no value can be obtained ...
            if ( cd.value == null )
            {
                // If the value is required ...
                if ( cd.optional == false )
                {
                    // Signal error to caller.
                    throw new ProcessingException( "ERROR: Could not locate required value for column [" + cd.describe()
                                                   + "], database table [" + tableName + "]." );
                }
                else  // No optional value is available, so putting 'NULL'.
                    cd.value=null;
            }
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "Done extracting message data to update." );
    }


    /**
     * Reset the column values in the list.
     */
    private void resetColumnValues ( )
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "Resetting column values ..." );

        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            cd.value = null;
        }
    }

    /**
     * Check that columns were configured and at least one
     * field has a value to update.
     *
     * @exception ProcessingException  thrown if invalid
     */
    private void validate ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "Validating column data ..." );

        boolean valid = false;

        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );
             valid = true;
             break;
        }

        if ( !valid )
        {
            throw new ProcessingException( "ERROR: No database column values are available to write to ["
                                           + tableName + "]." );
        }

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Done validating column data." );
    }


    private String tableName = null;
    private String separator = null;
    private boolean usingContextConnection = true;
    private List columns;
    private String sqlSetStmt = null;
    private String sqlWhereStmt = null;
    private String originalSQLWhereStmt = null;    
    StringBuffer errorBuffer = null;
    private boolean useCustomerId = true;
    private boolean useSubDomainId = false;

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
        public final boolean optional;

        public Object value = null;


        public ColumnData ( String columnType, String dateFormat,
                            String location, String defaultValue, String optional ) throws FrameworkException
        {
            this.columnType   = columnType;
            this.dateFormat   = dateFormat;
            this.location     = location;
            this.defaultValue = defaultValue;
            this.optional     = StringUtils.getBoolean( optional );
        }


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: " );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "Type [" );
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

            sb.append( "], optional [" );
            sb.append( optional );

            if ( value != null )
            {
                sb.append( "], value [" );
                sb.append( value );
            }

            sb.append( "]." );

            return( sb.toString() );
        }
    }

    public static void main(String[] args)
    {
        Debug.enableAll();
        Debug.showLevels();
        if (args.length != 5)
        {
          Debug.log (Debug.ALL_ERRORS, "DBUpdate: USAGE:  "+
            " jdbc:oracle:thin:@192.168.164.238:1521:e911 e911 e911  E911BATCHER TRY_DBUPDATE  ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log(null, Debug.MAPPING_ERROR, "DBUpdate: " +
                      "Database initialization failure: " + e.getMessage());
        }


        DBUpdate bl = new DBUpdate();
        try
        {
            bl.initialize("E911BATCHER","TRY_DBUPDATE");

            HashMap m = new HashMap();
            m.put("recordtype","SOIFile");
            MessageObject msg = new MessageObject(m);
            MessageProcessorContext mpc = new MessageProcessorContext();
            mpc.set("FileName","NGTF0001");
            mpc.set("Status","Faxed");
            bl.process(mpc,msg);


        }
        catch (Exception pe)
        {
             System.out.println("FAILED IN MAIN OF DBUpdate:" + pe.getClass());
        }
}//end of main*/

//*************************************************



}

/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/DatabaseLogger.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.text.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.HeaderNodeNames;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.adapter.util.*;


/**
 * This is a generic message-processor for logging messages to the database. All the
 * columns that are to be inserted are specified in the persistent properties
 * configuration.
 */ 
public class  DatabaseLogger extends DBMessageProcessorBase
{
    /**
     * Property indicating name of the database table to insert row into.
     */
    public static final String TABLE_NAME_PROP = "TABLE_NAME";

    /**
     * Property indicating whether SQL operation should be part of overall driver transaction.
     */
    public static final String TRANSACTIONAL_LOGGING_PROP = "TRANSACTIONAL_LOGGING";

    /**
     * Property indicating whether stop execution on error or not.
    */
     public static final String FAIL_ON_ERROR_PROP = "FAIL_ON_ERROR";

    /**
     * Property indicating separator token used to separate individual location alternatives.
     */
    public static final String LOCATION_SEPARATOR_PROP = "SEPARATOR";


    /**
     * Property prefix giving name of column.
     */
    public static final String COLUMN_PREFIX_PROP = "COLUMN";

    /**
     * Property prefix giving column data type.
     */
    public static final String COLUMN_TYPE_PREFIX_PROP = "COLUMN_TYPE";

    /**
     * Property prefix giving date format for date types.
     */
    public static final String DATE_FORMAT_PREFIX_PROP = "DATE_FORMAT";
     /**
     * Property indicating whether sysdate would be logged in UTC time zone or not.
     */
    public static final String LOG_SYSDATE_IN_UTC_PROP = "LOG_SYSDATE_IN_UTC";
     /**
      * Property prefix giving location of column value.
     */
    public static final String LOCATION_PREFIX_PROP = "LOCATION";

    /**
     * Property prefix indicating whether column value is optional or required.
     */
    public static final String OPTIONAL_PREFIX_PROP = "OPTIONAL";

    /**
     * Property prefix giving default value for column.
     */
    public static final String DEFAULT_PREFIX_PROP = "DEFAULT";

    // Types of supported columns that require special processing.
    public static final String COLUMN_TYPE_DATE        = "DATE";
    public static final String COLUMN_TYPE_TEXT_BLOB   = "TEXT_BLOB";
    public static final String COLUMN_TYPE_BINARY_BLOB = "BINARY_BLOB";

    /**
     * Constant Oracle/SQL Date Format used internally to insert date into Oracle Database.
     */
    private static final String ORACLE_DATE_FORMAT_CONSTANT = "YYYY-MM-DD HH24:MI:SS TZR";
    
    /**
     * Constant Java Date format used internally to insert date into Oracle Database.
     */
    private static final String JDBC_DATE_FORMAT_CONSTANT = "yyyy-MM-dd HH:mm:ss";
	
    // Token indicating that current date/time should be used for date field values.
    public static final String SYSDATE = "SYSDATE";

    // Token indicating that current date/time millsecs should be used for date field values.
    public static final String SYSTIMESTAMP = "SYSTIMESTAMP";

    // Token indicating UTC time zone.
    public static final String UTC_TIMEZONE = "UTC";

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
     * Property indicating whether customer criteria should be added or not to the SQL statement.
     * If not set, default value is true.
     */
    public static final String USE_SUBDOMAIN_ID_PROP = "USE_SUBDOMAIN_ID";

	/**
	 * Property key holding the input Time-Zone value
	 */
    private static final String INPUT_TIMEZONE_PROP = "INPUT_TIMEZONE";

    /**
	 * Property key holding the output Time-Zone value
	 */
    private static final String OUTPUT_TIMEZONE_PROP = "OUTPUT_TIMEZONE";
    /**
     * Holds the current table meta data info about columns...
     */
    protected TableMetaData currTableInfo;

    private boolean failOnErrorFlag;

    /**
     * Constructor.
     */
    public DatabaseLogger ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating database-logger message-processor." );

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
            Debug.log( Debug.SYSTEM_CONFIG, "DatabaseLogger: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );

        tableName = getRequiredPropertyValue( TABLE_NAME_PROP, errorBuffer );
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Database table to log to is [" + tableName + "]." );
        

        String strTemp = getPropertyValue( TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                usingContextConnection = getBoolean( strTemp );
            }
            catch ( FrameworkException e )
            	{
                errorBuffer.append ( "Property value for " + TRANSACTIONAL_LOGGING_PROP +
                  " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Logger will participate in overall driver transaction? ["
                   + usingContextConnection + "]." );

        // Get boolean value of FAIL_ON_ERROR property if configured, by default it's 'true'.
        failOnErrorFlag = StringUtils.getBoolean( getPropertyValue( FAIL_ON_ERROR_PROP ), true);

        String logSYSDateInUTC = getPropertyValue( LOG_SYSDATE_IN_UTC_PROP );

        if ( StringUtils.hasValue( logSYSDateInUTC ) )
        {
            try {
                logSYSDateUTC = getBoolean( logSYSDateInUTC );
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for " + LOG_SYSDATE_IN_UTC_PROP +
                  " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        separator = getPropertyValue( LOCATION_SEPARATOR_PROP );

        if ( !StringUtils.hasValue( separator ) )
            separator = DEFAULT_LOCATION_SEPARATOR;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Location separator token [" + separator + "]." );


        // Loop until all column configuration properties have been read ...
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        	Debug.log( Debug.SYSTEM_CONFIG, "Reading all column configuration properties from persistent property." );
        
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String colName = getPropertyValue( PersistentProperty.getPropNameIteration( COLUMN_PREFIX_PROP, Ix ) );
            
            
            
            // If we can't find a column name, we're done.
            if ( !StringUtils.hasValue( colName ) ){
            	
            	if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            		Debug.log(Debug.SYSTEM_CONFIG, "no value found for a  column ");
                
            	break;
            }
            
            
            String colType = getPropertyValue( PersistentProperty.getPropNameIteration( COLUMN_TYPE_PREFIX_PROP, Ix ) );

            String dateFormat = getPropertyValue( PersistentProperty.getPropNameIteration( DATE_FORMAT_PREFIX_PROP, Ix ) );

            String location = getPropertyValue( PersistentProperty.getPropNameIteration( LOCATION_PREFIX_PROP, Ix ) );


            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_PREFIX_PROP, Ix ) );

            String optional = getRequiredPropertyValue( PersistentProperty.getPropNameIteration( OPTIONAL_PREFIX_PROP, Ix ),
                                                        errorBuffer );

            String inputTimezone = null;
            String outputTimezone = null;
            
            if(StringUtils.hasValue(colType)&& colType.equalsIgnoreCase(COLUMN_TYPE_DATE))
            {
            	inputTimezone = getPropertyValue(PersistentProperty.getPropNameIteration(INPUT_TIMEZONE_PROP , Ix));
            	
            	/* If output TimeZone value is not set then use system TimeZone*/
            	if(!StringUtils.hasValue(inputTimezone)){
            		inputTimezone = Calendar.getInstance().getTimeZone().toString();
            	}
                
            	outputTimezone = getPropertyValue(PersistentProperty.getPropNameIteration(OUTPUT_TIMEZONE_PROP , Ix));

            	/* If output TimeZone value is not set then use system TimeZone*/
                if(!StringUtils.hasValue(outputTimezone)){
                	outputTimezone = Calendar.getInstance().getTimeZone().toString();
            	}                
            }

            try
            {
                // Create a new column data object and add it to the list.
            	if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            		Debug.log(Debug.SYSTEM_CONFIG,"Creating a new column data object and adding it to the list." );
                
            	ColumnData cd = new ColumnData( colName, colType, dateFormat, location, defaultValue, optional, inputTimezone, outputTimezone );

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
            Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of columns to insert [" + columns.size() + "]." );

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
            Debug.log( Debug.SYSTEM_CONFIG, "To use SubDomain id in SQL statement? [" + useSubDomainId + "]." );


        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, "Error while initializing DatabaseLogger :"+errMsg );

            throw new ProcessingException( errMsg );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "DatabaseLogger: Initialization done." );
    }


    /**
     * Extract data values from the context/input, and use them to
     * insert a row into the configured database table.
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
            Debug.log( Debug.MSG_STATUS, "DatabaseLogger: processing ... " );


        // Reset the values in cases where we're logging again.
        resetColumnValues( );

        // Extract the column values from the arguments.
        extractMessageData( mpContext, inputObject );

        // Extract values from header.
        extractHeaderData(mpContext);

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
                    Debug.log( Debug.MSG_STATUS, "Database logging is not transactional, so getting connection from NightFire pool." );

                dbConn = DBConnectionPool.getInstance( true ).acquireConnection( );
            }

            // Write the data to the database.
            
            insert( dbConn);

            // If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, commit the changes now.
            if ( !usingContextConnection )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Committing data inserted by database-logger to database." );

                DBConnectionPool.getInstance( true ).commit( dbConn );
            }
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: DatabaseLogger: Attempt to log to database failed with error: "
                            + e.getMessage();

            Debug.log( Debug.ALL_ERRORS, errMsg );

            // If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, roll back any changes now.
            if ( !usingContextConnection )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Rolling-back any database changes due to database-logger." );

                try
                {
                    DBConnectionPool.getInstance( true ).rollback( dbConn );
                }
                catch( ResourceException re )
                {
                    Debug.log( Debug.ALL_ERRORS, re.getMessage() );
                }
            }

            // if FAIL_ON_ERROR property set as 'false', then eat exception to continue processing

            if(failOnErrorFlag){

                // Re-throw the exception to the driver.
                if ( e instanceof MessageException )
                    throw (MessageException)e;
                else
                    throw new ProcessingException( errMsg );
            }

            if(Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                Debug.log( Debug.MSG_STATUS, "Could not insert row into database table [" + tableName + "] "  );
                Debug.log( Debug.MSG_STATUS, "FAIL_ON_ERROR property value is [" + failOnErrorFlag + "]. Hence processing would be continued..." );
            }

        }
        finally
        {
            // If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, return the connection previously acquired back to the resource pool.
            if ( !usingContextConnection && (dbConn != null) )
            {
                try
                {
                    DBConnectionPool.getInstance(true).releaseConnection( dbConn );
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
     * Extract data values from the request header, and use them to
     * set the instance variables.
     *
     * @param  mpCtx The context
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if message is bad.
     */

    public void extractHeaderData(MessageProcessorContext mpCtx) throws ProcessingException, MessageException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"Extracting data from request header.");

        XMLPlainGenerator xpg = null;
        if ( mpCtx.exists( MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME ) )
        {
            try{

                 xpg = new XMLPlainGenerator(mpCtx.getDOM(MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME));

                // get InputSource node value from header.
                if (xpg.exists(HeaderNodeNames.INPUT_SOURCE_NODE))
                {
                    String attValueInSrc = xpg.getAttribute(HeaderNodeNames.INPUT_SOURCE_NODE, HeaderNodeNames.VALUE_ATTRIBUTE);
                    if (StringUtils.hasValue(attValueInSrc))
                        inputSource= attValueInSrc;
                }
                else
                    Debug.log( Debug.ALL_WARNINGS, "Could not find node " + HeaderNodeNames.INPUT_SOURCE_NODE + " in Header." );

                // get OrderOid node value from header.
                 if (xpg.exists(HeaderNodeNames.ORDER_OID_NODE))
                 {
                     String attValueOid =xpg.getAttribute(HeaderNodeNames.ORDER_OID_NODE,HeaderNodeNames.VALUE_ATTRIBUTE);
                     if (StringUtils.hasValue(attValueOid))
                         orderOId= attValueOid;
                 }
                 else
                     Debug.log( Debug.ALL_WARNINGS, "Could not find node " + HeaderNodeNames.ORDER_OID_NODE + " in Header." );

                 // get TransOid node value from header.
                 if (xpg.exists(HeaderNodeNames.TRANS_OID_NODE))
                 {
                    String attValueTid =xpg.getAttribute(HeaderNodeNames.TRANS_OID_NODE,HeaderNodeNames.VALUE_ATTRIBUTE);
                    if (StringUtils.hasValue(attValueTid))
                        transOID= attValueTid;
                 }
                 else
                     Debug.log( Debug.ALL_WARNINGS, "Could not find node " + HeaderNodeNames.TRANS_OID_NODE+ " in Header." );
                 
            }
            catch (MessageException e)
            {
                throw new ProcessingException( "ERROR: Could not read property from request header \n" + e.toString());

            }
        }
    }


    /**
     * Insert a single row into the database table using the given connection.
     *
     * @param  dbConn  The database connection to perform the SQL INSERT operation against.
     *
     * @exception  MessageException  Thrown on data errors.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected void insert ( Connection dbConn) throws MessageException, ProcessingException
    {

        // Make sure that at least one column value will be inserted.
        validate( );

        PreparedStatement pstmt = null;

        try
        {

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, CustomerContext.getInstance().describe() );

            //Check if userid, interface version need to be logged or not to the table.
            currTableInfo = DBMetaData.getInstance().getTableMetaData( dbConn, tableName );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, currTableInfo.describe() );

            //We log to the table only if the CustomerContext has the data and the
            //table has the column.
            //InterfaceVersion check
            if ( (currTableInfo.existsInterfaceVersion()) && (CustomerContext.getInstance().getInterfaceVersion() != null ) )
                useInterfaceVersion = true;

            //UserId check.
            if ( (currTableInfo.existsUserId()) && (CustomerContext.getInstance().getUserID() != null ) )
                useUserId = true;

            //OrderOid check.
            if ( (currTableInfo.existsOrderOId()) && (orderOId!=null) )
                useOrderOId = true;

            //TransOid check.
            if ( (currTableInfo.existsTransOId()) && (transOID!=null) )
                useTransOId = true;

            //Input source check.
            if ( (currTableInfo.existsInputSource()) && (inputSource!=null) )
                useInputSource = true;

            // Create the SQL statement using the column data objects.
            String sqlStmt = constructSqlStatement( );

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log( Debug.NORMAL_STATUS, "DatabaseLogger : Executing SQL:\n" + sqlStmt );

            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from the column data objects.
            populateSqlStatement( pstmt );

            // Execute the SQL INSERT operation.
            int count = pstmt.executeUpdate( );

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS, "DatabaseLogger : Finished Executing SQL.. Inserted [" + count + "] row into table ["
                       + tableName + "]" );
        }
        catch ( SQLException sqle )
        {
             throw new ProcessingException( "ERROR: Could not insert row into database table [" + tableName
                                           + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Could not insert row into database table [" + tableName
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
        }
    }

    /**
     * Construct the SQL INSERT statement from the column data.
     *
     * @return SQL INSERT statement.
     */
    protected String constructSqlStatement ( ) throws DatabaseException
    {
        Map currentTableColumnInfo = currTableInfo.getColumnMetaData();

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Constructing SQL INSERT statement ..." );

        StringBuffer sb = new StringBuffer( );

        sb.append( "INSERT INTO " );
        sb.append( tableName );
        sb.append( " ( " );

        boolean firstOne = true;

        Iterator iter = columns.iterator( );

        // Append the names of columns with non-null values.
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            // Skip columns with null values since null values aren't inserted.
            if ( isNull(cd.value) )
                continue;

            if ( firstOne )
                firstOne = false;
            else
                sb.append( ", " );

            sb.append( cd.columnName );
        }

        if ( useCustomerId )
        {
            firstOne = appendOptionalComma( firstOne, sb );
            SQLBuilder.insertCustomerID( sb );
        }

        if (useSubDomainId)
        {
            firstOne = appendOptionalComma(firstOne, sb);
            SQLBuilder.insertSubDomainId (sb);
        }

        if ( useInterfaceVersion )
        {
            firstOne = appendOptionalComma( firstOne, sb );
            SQLBuilder.insertInterfaceVersion( sb );
        }

        if ( useUserId )
        {
            firstOne = appendOptionalComma( firstOne, sb );
            SQLBuilder.insertUserID( sb );
        }
        if ( useOrderOId)
        {
            firstOne = appendOptionalComma( firstOne, sb );
            SQLBuilder.insertOrderOid(sb);
        }

        if ( useTransOId )
        {
            firstOne = appendOptionalComma( firstOne, sb );
            SQLBuilder.insertTransOid(sb);
        }

       if ( useInputSource)
       {
          firstOne = appendOptionalComma( firstOne, sb );
          SQLBuilder.insertInputSource(sb);
       }

       
        sb.append( " ) VALUES ( " );

        firstOne = true;

        iter = columns.iterator( );

        // Append the value placeholders for columns with non-null values.
        while ( iter.hasNext() )
        {
          ColumnData cd = (ColumnData) iter.next();

          // Skip columns with null values since null values aren't inserted.
          if (isNull(cd.value))
            continue;

          firstOne = appendOptionalComma(firstOne, sb);

          // If the current column is a date column, and the configuration indicates that the current system
          // date should be used for the value place it in the SQL statement now .
          // If UTC time-zone logging is true then SYSDATE would not be placed in SQL statement

          // Find and make changes so as to accommodate for the new time zone in which 
          // the date must be logged. If in case the TimeZone values are not found, then 
          // use the system's default TimeZone values for both incoming date and the output(required) date.

          // In case the date to be logged is different from the date of the incoming time
          // format, then the date must be converted to the desired(output) date format.
          
          
          if (StringUtils.hasValue(cd.columnType) &&
              cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE)  &&
              cd.value instanceof String  )
          {
            String cdvalue= (String)cd.value;
            
            if( (cdvalue.equalsIgnoreCase(SYSDATE)
                 || cdvalue.equalsIgnoreCase(SYSTIMESTAMP)))
            {
                      Object obj = currentTableColumnInfo.get(cd.columnName);
                      ColumnMetaData columnMetaDataInfo = (ColumnMetaData)currentTableColumnInfo.get(cd.columnName);
                      if (columnMetaDataInfo == null) {
                             if(logSYSDateUTC){
                            	 sb.append("CONVERT_TIME(SYSTIMESTAMP, SESSIONTIMEZONE ,'GMT')"); 
                             }
                             else
                                sb.append(SYSTIMESTAMP);
                      }
                      else {
                           if (columnMetaDataInfo.type == java.sql.Types.DATE)
                           {
                                  if(logSYSDateUTC)
                                 sb.append("CONVERT_TIME(SYSDATE, SESSIONTIMEZONE ,'GMT')");
                               else
                                 sb.append(SYSDATE);
                           }
                           else
                              { if(logSYSDateUTC)
                                 sb.append("CONVERT_TIME(SYSTIMESTAMP, SESSIONTIMEZONE ,'GMT')");
                               else
                                 sb.append(SYSTIMESTAMP);
                              }
                      }
           }
           else
               sb.append("TO_TIMESTAMP_TZ(?, ?)");
          }
          else
            sb.append("?");
        }

        if ( useCustomerId )
        {
            //Add customer information
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }

        if (useSubDomainId)
        {
            //Add SubDomain information
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }

        if ( useInterfaceVersion )
        {
            //Add interface version information
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }

        if ( useUserId )
        {
            //Add user id information
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }

        if ( useOrderOId )
        {
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }

        if ( useTransOId)
        {
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }

        if ( useInputSource )
        {
            firstOne = appendOptionalComma( firstOne, sb );
            sb.append( "?" );
        }
        sb.append( " )" );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Done constructing SQL INSERT statement." );

        return( sb.toString() );
    }

    /**
     * Utility method to construct the SQL query.
     */
    private boolean appendOptionalComma( boolean firstOne, StringBuffer sb )
    {
        if ( firstOne )
            firstOne = false;
        else
            sb.append( ", " );
        return firstOne;
    }

    /**
     * Populate the SQL INSERT statement from the column data.
     *
     * @param  pstmt  The prepared statement object to populate.
     *
     * @exception Exception  thrown if population fails.
     */
    protected void populateSqlStatement ( PreparedStatement pstmt ) throws Exception
    {
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "Populating SQL INSERT statement ..." );

        int Ix = 1;  // First value slot in prepared statement.

        Iterator iter = columns.iterator( );

        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log( Debug.MSG_DATA, "Populating SQL statement for:\n" + cd.describe() );

            // Skip columns with null values since null values aren't inserted.
            if ( isNull(cd.value) )
            {
                Debug.log( Debug.MSG_STATUS, "Skipping null column value." );

                continue;
            }

            if(Debug.isLevelEnabled(Debug.DB_DATA))
                Debug.log( Debug.DB_DATA, "Populating prepared-statement slot [" + Ix + "]." );

            // Default is no column type specified.
            if ( !StringUtils.hasValue(cd.columnType) )
            {
                Debug.log( Debug.MSG_DATA, "Value for column [" + cd.columnName
                           + "] is [" + cd.value.toString() + "]." );

                pstmt.setObject( Ix, cd.value );
            }
            else
            if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE) )
            {
                String val = (String)(cd.value);

                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                	Debug.log( Debug.MSG_DATA, "Value for date column [" + cd.columnName + "] is [" + val + "] ");

                if ( val.equalsIgnoreCase(SYSDATE)  || val.equalsIgnoreCase(SYSTIMESTAMP))
                {
                    Debug.log( Debug.MSG_STATUS, "Skipping date population since SYSDATE is already in SQL string and LOG_SYSDATE_IN_UTC property is FALSE." );

                    continue;
                }

                if ( !StringUtils.hasValue( cd.dateFormat ) )
                {
                    throw new FrameworkException( "ERROR: Configuration for date column ["
                                         + cd.columnName + "] does not specify date format." );
                }                
                
                // used for parsing/re-formatting with given input date format
                SimpleDateFormat dateFormatter = new SimpleDateFormat( cd.dateFormat ) ;
                
                /*
                 * if input/output time-zones are provided, then
                 *  - parse the input date value with given input time-zone
                 *  - format the input date into given output time-zone
                */
                if (StringUtils.hasValue(cd.inputTimezone)
						&& StringUtils.hasValue(cd.outputTimezone)) {

	                if (Debug.isLevelEnabled(Debug.MSG_DATA))
	                	Debug.log( Debug.MSG_DATA, "inputTimezone [" + cd.inputTimezone 
	                			+ "], outputTimezone [" + cd.outputTimezone + "] ");

					dateFormatter.getCalendar().setLenient(false);

					/* convert time-zone of DateFormat object into Input-Time-zone
					   object */
					TimeZone inputTimeZone = TimeZone.getTimeZone(cd.inputTimezone);
					dateFormatter.setTimeZone(inputTimeZone);

					/* Parse the input Time */
					java.util.Date inputDate = dateFormatter.parse(val);

					/* Convert time-zone to output time-zone. */
					TimeZone outputTimezone = TimeZone.getTimeZone(cd.outputTimezone);

					dateFormatter.getCalendar().setTimeZone(outputTimezone);

					/* set the value for logged date in the output time-zone. */
					val = dateFormatter.format(inputDate);
				} else {
					if (Debug.isLevelEnabled(Debug.MSG_WARNING))
						Debug.log(Debug.MSG_WARNING,"Input and Output Time-Zones are not configured.");
				}
                // use the date value which is formatted according to output time-zone
				java.util.Date d = dateFormatter.parse(val);
				
				// create a date formatter for internal JDBC date format: yyyy-MM-dd HH:mm:ss
				DateFormat newDateFormat = new SimpleDateFormat(JDBC_DATE_FORMAT_CONSTANT);
				newDateFormat.getCalendar().setLenient(false);
				newDateFormat.getCalendar().setTimeZone(TimeZone.getTimeZone(cd.outputTimezone));
								
				/* Setting offset values for the current time-zone. */
				int timeZoneOffset = dateFormatter.getTimeZone().getRawOffset();
				int offsetHours = timeZoneOffset / (60*60*1000);
				int offsetMinutes = Math.abs(timeZoneOffset / (60*1000)) % 60;

				// use internal JDBC date formatter to log date value 
				// into fix database date format: YYYY-MM-DD HH24:MI:SS TZR
				String outputDate = newDateFormat.format(d)+" "+offsetHours+":"+offsetMinutes;
				// set the date value
				pstmt.setString(Ix, outputDate);
				
				// increase the index for extra parameter used for date value
				Ix++;
				
				// set the database date format (internally used) 
				pstmt.setString(Ix, ORACLE_DATE_FORMAT_CONSTANT);				
            }
            else
            if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_TEXT_BLOB) )
            {
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Logging column [" + cd.describe() + "]." );

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
                                                  + cd.columnName + "] of type [" + cd.value.getClass().getName()
                                                  + "] can't be converted to byte stream." );
                }

                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Logging column [" + cd.describe() + "]." );

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

        //Populate customerid, interfaceversion, userid.
        if ( useCustomerId )
        {
            //Use Ix slot number as-is, because it was already incremented before exiting above loop.
            SQLBuilder.populateCustomerID( pstmt, Ix );
            ++Ix;
        }
        if (useSubDomainId)
        {
            //Use Ix slot number as-is, because it was already incremented before exiting above loop.
			SQLBuilder.populateSubDomainID (pstmt, Ix);
			Ix++;
        }
        if ( useInterfaceVersion )
        {
            SQLBuilder.populateInterfaceVersion( pstmt, Ix );
            ++Ix;
        }
        if ( useUserId )
        {
            SQLBuilder.populateUserID( pstmt, Ix );
            ++Ix;
        }
        if ( useOrderOId)
        {
            SQLBuilder.populateOrderOId( pstmt, Ix, orderOId );
            ++Ix;
        }
        if ( useTransOId)
        {
            SQLBuilder.populateTransOId( pstmt, Ix , transOID);
            ++Ix;
        }
        if ( useInputSource )
        {
            SQLBuilder.populateInputSource( pstmt, Ix , inputSource);
        }

        Debug.log( Debug.DB_DATA, "Done populating SQL INSERT statement." );
    }

   /**
     * Extract data for each column from the input message/context.
     *
     * @param context The message context.
     * @param inputObject The input object.
     *
     * @exception MessageException  thrown if required value can't be found.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected void extractMessageData ( MessageProcessorContext context, MessageObject inputObject )
        throws MessageException, ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "Extracting message data to log ..." );

        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log( Debug.MSG_DATA, "Extracting data for:\n" + cd.describe() );

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
                    if ( loc.equalsIgnoreCase( INPUT_MESSAGE ) || loc.equalsIgnoreCase( PROCESSOR_INPUT ) )
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
                            Debug.log( Debug.MSG_DATA, "Found value for column [" + cd.columnName + "] at location [" + loc + "]." );

                        break;
                    }
                }
            }

            // If no value was obtained from location in context/input, try to set it from default value (if available).
            if ( cd.value == null )
            {
                cd.value = cd.defaultValue;

                if ( cd.value != null )
                    Debug.log( Debug.MSG_DATA, "Using default value for column [" + cd.columnName + "]." );
            }

            // If no value can be obtained ...
            if ( cd.value == null )
            {
                // If the value is required ...
                if ( cd.optional == false )
                {
                    // Signal error to caller.
                    throw new MessageException( "ERROR: Could not locate required value for column [" + cd.columnName
                                                   + "], database table [" + tableName + "]." );
                }
                else  // No optional value is available, so just continue on.
                    Debug.log( Debug.MSG_DATA, "Skipping optional column [" + cd.columnName + "] since no data is available." );
            }

        }

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log (Debug.MSG_STATUS, "Done extracting message data to log." );
    }


    /**
     * Reset the column values in the list.
     */
    protected void resetColumnValues ( )
    {
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
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
     * mandatory field has a value to insert.
     *
     * @exception ProcessingException  thrown if invalid
     */
    protected void validate ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log (Debug.MSG_STATUS, "Validating column data ..." );

        boolean valid = false;

        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            // If we've found at least one value to insert, its valid.
            if ( cd.value != null )
            {
                valid = true;

                break;
            }
        }

        if ( !valid )
        {
            throw new ProcessingException( "ERROR: No database column values are available to write to ["
                                           + tableName + "]." );
        }

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log( Debug.DB_DATA, "Done validating column data." );
    }


    /**
     * Checks value argument to see if it is null, or is a
     * string with the text value "null" (case-invariant).
     *
     * @param value Value argument to check
     * @return  'true' if null, otherwise 'false'.
     */
    private boolean isNull ( Object value )
    {
        if ( value == null )
            return true;

        if ( (value instanceof String) && ((String)value).equalsIgnoreCase( "null" ) )
            return true;

        return false;
    }


    protected String tableName = null;
    protected String separator = null;
    protected boolean usingContextConnection = true;
    protected boolean useCustomerId = true;
    protected boolean useSubDomainId = false;
    protected boolean useInterfaceVersion = false;
    protected boolean useUserId = false;
    protected List columns;
    protected boolean useOrderOId = false;
    protected boolean useTransOId = false;
    protected boolean useInputSource = false;
    protected boolean logSYSDateUTC = false;
    
    private String inputSource = null;
    private String orderOId= null;
    private String transOID= null ;


    /**
     * Class ColumnData is used to encapsulate a description of a single column
     * and its associated value.
     */
    protected static class ColumnData
    {
        public final String columnName;
        public final String columnType;
        public final String dateFormat;
        public final String location;
        public final String defaultValue;
        public final String inputTimezone;
        public final String outputTimezone;
        
        public final boolean optional;

        public Object value = null;

        public ColumnData ( String columnName, String columnType, String dateFormat,
                            String location, String defaultValue, String optional, String inputTimezone, String outputTimezone ) throws FrameworkException
        {
            this.columnName   = columnName;
            this.columnType   = columnType;
            this.dateFormat   = dateFormat;
            this.location     = location;
            this.defaultValue = defaultValue;
            this.optional     = StringUtils.getBoolean( optional );
            this.inputTimezone = inputTimezone;
            this.outputTimezone = outputTimezone;
        }

       public ColumnData ( String columnName, String columnType, String dateFormat,
               String location, String defaultValue, String optional) throws FrameworkException
		{
		this.columnName   = columnName;
		this.columnType   = columnType;
		this.dateFormat   = dateFormat;
		this.location     = location;
		this.defaultValue = defaultValue;
		this.optional     = StringUtils.getBoolean( optional );
		this.inputTimezone = null;
		this.outputTimezone = null;
		}

       
       
        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: Name [" );
            sb.append( columnName );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "], type [" );
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

            
            if(StringUtils.hasValue(inputTimezone)){
                sb.append( "], inputTimezone [" );
                sb.append( inputTimezone );
            }

            if(StringUtils.hasValue(outputTimezone)){
                sb.append( "], outputTimezone [" );
                sb.append( outputTimezone );
            }

            sb.append( "]." );
            	
            return( sb.toString() );
        }
    }


    public static void main(String[] args) {
		System.out.println("NOTE:: MAIN STARTED");

		Properties props = new Properties();
		props.put("DEBUG_LOG_LEVELS", "ALL");
		props.put("LOG_FILE", "./logs/farji.log");
		props.put("MAX_DEBUG_WRITES", "10000");
		props.put("PROCESS_INFO_CLASS_NAME", "true");

		Debug.showLevels();
		Debug.configureFromProperties(props);

//		Debug.enableAll();
//		Debug.showLevels();
		/*
		 * if (args.length != 5) { Debug.log(Debug.ALL_ERRORS,
		 * "DBDelete: USAGE:  " +
		 * " jdbc:oracle:thin:@host:1521:sid user password key type"); return; }
		 */
		try {

			DBInterface.initialize(
					"jdbc:oracle:thin:@impetus-786:1521:ORCL786", "infra",
					"infra");
			// DBInterface.initialize(args[0], args[1], args[2]);
		} catch (DatabaseException e) {
			Debug.log(null, Debug.MAPPING_ERROR, "DBDelete: "
					+ "Database initialization failure: " + e.getMessage());
		}

		DatabaseLogger bl = new DatabaseLogger();

		/*
		 * MessageProcessorContext ctx = new MessageProcessorContext();
		 * DatabaseLogger validator = new DatabaseLogger();
		 * validator.initialize(""," ReqEDILogger ");
		 */
		System.out
				.println("NOTE:: DATABASELOGGER Object Created... Creating message Object");
		bl.tableName = "WASTED_LOG";
		try {

			bl.initialize("DATABASE_LOGGER_TZ_TESTING", "ReqEDILogger");
			System.out
					.println("NOTE:: INITIALIZATION COMPLETE... Creating message Object");

			System.out
					.println("NOTE:: MessageObject Created... Creating MessageProcessorContext object..");
			MessageObject input = new MessageObject("hello world");
			MessageProcessorContext mpc = new MessageProcessorContext();
			mpc.set("RequestKey", "131");
			mpc.set("SUPPLIER", "SBC");
			mpc.set("ILEC_NAME", "PIU");
			
			//03-11-2012-0735AM Problem time.
			
			mpc.set("InputDate", "11-03-2012 02:35:00.00");
			//mpc.set("InputDate", "2011-08-22 17:00:00.00 ");

			mpc.set("DATE_FORMAT", "yyyy-MM-dd HH:mm:ss");
			// mpc.set("PON", "ting123");
			mpc.set("TYPE", "NEW");
			mpc.set("INPUT_MESSAGE", "hello world !!");

			System.out
					.println("NOTE:: INITIALIZATION MessageProcessorContext COMPLETE... Processing MPC Object and Message");
			
			
			Debug.log(Debug.MSG_DATA, "Starting DatabaseLogger Messageprocessor...");
			/*Properties newProps = new Properties();
			newProps.put(Debug.DEBUG_LOG_LEVELS_PROP,"ALL" );
			
			Debug.configureFromProperties(newProps);
			Debug.log(Debug.MSG_DATA, "Starting DatabaseLogger Messageprocessor...");
			*
			*/
			
			bl.process(mpc, input);
			//props.put("DEBUG_LOG_LEVELS","ALL" );
			//Debug.configureFromProperties(props);
			Debug.log(Debug.MSG_DATA, "Finished processing DatabaseLogger Messageprocessor...");
			
			
			mpc.getDBConnection().commit();
			mpc.releaseDBConnection();

		} catch (Exception pe) {
			System.out.println("FAILED IN MAIN OF DBDelete:" + pe.getClass());
			pe.printStackTrace();
		}
  } //end of main*/


}

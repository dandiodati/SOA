/**
* @author   Priti Budhia
**/
package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;


/**
 * A generic message-processor for extracting row values from the database
 * via the execution of a configurable SQL SELECT statmement.
 */
public class DBQuery extends DBMessageProcessorBase
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
     * Property for checking whether to return XML or list
     */
    public static final String RETURN_XML_PROP = "RETURN_XML";

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
     * Constructor.
     */
    public DBQuery ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating database-query message-processor." );

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
        Debug.log( Debug.SYSTEM_CONFIG, "DBSelectQuery: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );
        
        tableName = getRequiredPropertyValue( TABLE_NAME_PROP, errorBuffer );

        Debug.log( Debug.SYSTEM_CONFIG, "Database table to query against is [" + tableName + "]." );

        //put in by infy
        try
        {
            returnXML = StringUtils.getBoolean(getPropertyValue( RETURN_XML_PROP));
        }
        catch(FrameworkException fex)
        {
            throw new ProcessingException( fex.getMessage() );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Return XML? [" + returnXML + "]." );

        sqlStmt = getRequiredPropertyValue( SQL_QUERY_STATEMENT_PROP, errorBuffer );

        // keep a copy of the unadulterated (no CustomerId clause) SQL statement
        originalSQLStmt = sqlStmt;
        
        Debug.log( Debug.SYSTEM_CONFIG, "SQL query is [" + sqlStmt + "]." );

        resultLocation = getPropertyValue( RESULT_LOCATION_PROP );

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

        }//if

        Debug.log( Debug.SYSTEM_CONFIG, "Database-query will participate in overall driver transaction? ["
                   + usingContextConnection + "]." );

        outputDateFormat = getPropertyValue( OUTPUT_DATE_FORMAT_PROP );

        separator = getPropertyValue( LOCATION_SEPARATOR_PROP );
        
        if ( !StringUtils.hasValue( separator ) )
            separator = DEFAULT_LOCATION_SEPARATOR;

        Debug.log( Debug.SYSTEM_CONFIG, "Location separator token [" + separator + "]." );


        // Loop until all column configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {


            String colType = getPropertyValue( PersistentProperty.getPropNameIteration( COLUMN_TYPE_PREFIX_PROP, Ix ) );

            String dateFormat = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_DATE_FORMAT_PREFIX_PROP, Ix ) );

            String location = getPropertyValue( PersistentProperty.getPropNameIteration( LOCATION_PREFIX_PROP, Ix ) );

            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_PREFIX_PROP, Ix ) );
            if ( !StringUtils.hasValue( colType ) && !StringUtils.hasValue( dateFormat )
               && !StringUtils.hasValue( location ) && !StringUtils.hasValue( defaultValue ))
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

        Debug.log( Debug.SYSTEM_CONFIG, "Number of columns participating in dynamic query criteria ["
                   + columns.size() + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

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

        Debug.log( Debug.MSG_STATUS, "DBSelectQuery: processing ... " );
        
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
            try
            {
                // Get a database connection from the appropriate location - based on transaction characteristics.
                if ( usingContextConnection )
                {
                    Debug.log( Debug.MSG_STATUS, "Database logging is transactional, so getting connection from context." );
                    
                    dbConn = mpContext.getDBConnection( );
                }
                else
                {
                    Debug.log( Debug.MSG_STATUS, "Database logging is not transactional, so getting connection from pool." );
                    
                    dbConn = DBConnectionPool.getInstance().acquireConnection( );
                }
            }
            catch ( Exception e )
            {
                String errMsg = "ERROR: DBSelectQuery: Attempt to query database failed with error: "
                    + e.getMessage();
                
                Debug.log( Debug.ALL_ERRORS, errMsg );
                
                // Re-throw the exception to the driver.
                throw new ProcessingException( errMsg );
            }

            if(returnXML)
            {
                // Get the data from the database.
                result = query( dbConn );
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                {
                    Debug.log( Debug.MSG_DATA, "Result:\n"
                               + XMLLibraryPortabilityLayer.convertDomToString( result ) );
                }
                
                // Place the result in the configured location.
                set( resultLocation, mpContext, inputObject, result );
                return( formatNVPair( inputObject ) );
            }
            else
            {
                MessageObject mo = fillList( dbConn );
                if (mo == null)
                    return null;
                else
                    return( formatNVPair(mo));
            }
            // Pass the input on to the output.
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

        try
        {
            SimpleDateFormat sdf = null;

            if ( StringUtils.hasValue(outputDateFormat) )
                sdf = new SimpleDateFormat( outputDateFormat );

            //Add customer information                
            sqlStmt = SQLBuilder.addCustomerIDCondition( sqlStmt );

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log( Debug.NORMAL_STATUS, "DBQuery: Executing SQL:\n" + sqlStmt );
            
            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from the column data objects.
            populateSqlStatement( pstmt );
            
            // Execute the SQL SELECT operation.
            ResultSet rs = pstmt.executeQuery( );
            
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log( Debug.NORMAL_STATUS, "DBQuery: Finished Executing SQL...");
            
            ResultSetMetaData rsmd = rs.getMetaData( );

            int numCols = rsmd.getColumnCount( );
            ///////////////////////////////////////////////////////////
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
                    	Debug.log( Debug.DB_DATA, "Extracting result-set data for column ["
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
        Debug.log( Debug.DB_DATA, "Populating SQL SELECT statement ..." );

        Iterator iter = columns.iterator( );

        int Ix = 0;
        for ( Ix = 1;  iter.hasNext();  Ix ++ )
        {
            ColumnData cd = (ColumnData)iter.next( );

            Debug.log( Debug.DB_DATA, "Populating prepared-statement slot [" + Ix + "]." );

            // Default is no column type specified.
            if ( !StringUtils.hasValue(cd.columnType) )
            {
                Debug.log( Debug.MSG_DATA, "Value for column is [" + cd.value.toString() + "]." );

                pstmt.setObject( Ix, cd.value );
            }
            else
            if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE) )
            {
                String val = (String)(cd.value);

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

        //Increment Ix slot number.
        SQLBuilder.populateCustomerID( pstmt, Ix++ );

        Debug.log( Debug.DB_DATA, "Done populating SQL SELECT statement." );
    }


    /**
     * Extract data for each column from the input message/context.
     *
     * @param mpcontext The message context.
     * @param intputObject The input object.
     *
     * @exception MessageException thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractQueryCriteriaValues ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        Debug.log (Debug.MSG_STATUS, "Extracting message data to use in query criteria ..." );
        
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

                if ( cd.value != null )
                    Debug.log( Debug.MSG_DATA, "Using default value for column." );
                else
                {
                    // Signal error to caller.
                    throw new ProcessingException( "ERROR: Could not locate required value for column [" + cd.describe() 
                                                   + "] used in query against database table [" + tableName + "]." );
                }
            }
        }

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
    
    
    private MessageObject fillList(Connection dbConn) throws ProcessingException
    {
        MessageObject retMsg = new MessageObject();

        boolean recFound=false;
        PreparedStatement pstmt = null;
        HashMap recMap ;
        List retList = new Vector();


        try
        {

            SimpleDateFormat sdf = null;

            if ( StringUtils.hasValue(outputDateFormat) )
                sdf = new SimpleDateFormat( outputDateFormat );

		        sqlStmt = SQLBuilder.addCustomerIDCondition( sqlStmt );

            Debug.log( Debug.DB_DATA, "Executing SQL:\n" + sqlStmt );

            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from the column data objects.
            populateSqlStatement( pstmt );

            // Execute the SQL SELECT operation.
            ResultSet rs = pstmt.executeQuery( );

            ResultSetMetaData rsmd = rs.getMetaData( );

            int numCols = rsmd.getColumnCount( );

            if ( numCols == 0 )
                return null;

            int rowCount = 0;

            // Loop over rows returned by query ...
            for ( int Ix = 0;  rs.next();  Ix ++ )
            {
                rowCount ++;

                recMap = new HashMap();

                // Loop over columns that are available for each returned row ...
                for ( int Jx = 1;  Jx <= numCols;  Jx ++ )
                {
                    String colValue = null;

                    String colName = rsmd.getColumnName( Jx );

                    int colType = rsmd.getColumnType( Jx );

                    Debug.log( Debug.DB_DATA, "Extracting result-set data for column ["
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
                            Debug.log( Debug.DB_DATA, "Skipping column [" + colName + "] with null value ..." );

                            continue;
                        }

                        colValue = sdf.format( d );
                    }
                    else  // It's not a date, so extract as string.
                    {
                        colValue = rs.getString( colName );
                    }
                    if ( rs.wasNull() )
                    {
                        Debug.log( Debug.DB_DATA, "Skipping column [" + colName + "] with null value ..." );

                        continue;
                    }


                    if ( singleResultFlag )
                    {
                        if ( Ix > 0 )
                        {
                            throw new MessageException( "ERROR: Single-result flag was set to 'true', "
                                                        + "but SQL query returned multiple rows." );
                        }

                        recMap.put( colName, colValue );
                    }
                    else
                    {
                        recMap.put( colName, colValue );
                    }
                }
                retList.add(recMap);
                recFound = true;
            }

            rs.close( );

            // Check that the number of database rows selected was acceptable, based on configuration.
            checkSQLOperationResultCount( rowCount );

            if(recFound)
              {
                  //set the message object to the list just generated
                  retMsg.set(retList);
                  Debug.log(this,Debug.UNIT_TEST,"querydb record-------->"+retMsg.describe());
                  return retMsg;
              }
              else
                  return null;
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
        }




    }//end of fillList method



    private String tableName = null;
    private String sqlStmt = null;
    private String originalSQLStmt = null;    
    private boolean singleResultFlag = true;
    private boolean usingContextConnection = true;
    private String outputDateFormat;
    private String separator = null;
    private String resultLocation;
    private Vector selCols = new Vector();
    private List columns;
    private boolean returnXML = true;

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
                            String location, String defaultValue  ) throws FrameworkException
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

   /////-------------------------------------------------------------------------------
    public static void main(String[] args)
    {
        Debug.enableAll();
        Debug.enable(Debug.DB_DATA);
        Debug.enable(Debug.BENCHMARK);
        Debug.showLevels();
        if (args.length != 3)
        {
          Debug.log (Debug.ALL_ERRORS, "DBQuery: USAGE:  "+
            " jdbc:oracle:thin:@192.168.164.238:1521:e911 e911 e911  E911BATCHER TRY_DBQUERY  ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log(null, Debug.MAPPING_ERROR, "DBQuery: " +
                      "Database initialization failure: " + e.getMessage());
        }


        DBQuery bl = new DBQuery();
        try
        {
            MessageProcessorContext mpx = new MessageProcessorContext();
            mpx.set("status","Faxed");
            bl.initialize("E911BATCHER","NENA_MESSAGE_DBPOLLER");
            /* following was done to see how MessageObject works!!!
            String nenaMessage = new String("NENAMessage");
            String xmlMessage = new String("XMLMessage");

            List l = new Vector();

            HashMap m1 = new HashMap();
            m1.put("NENAKEY",nenaMessage);
            l.add(m1);

            HashMap m2 = new HashMap();
            m2.put("XMLKEY",xmlMessage);
            l.add(m2);

            MessageObject msg1 = new MessageObject(m1);
            MessageObject msg2 = new MessageObject(m2);
            MessageObject msg3 = new MessageObject(l);
            System.out.println("from map nena =  " + msg1.get("NENAKEY"));

            System.out.println("from list 0th = " + ((HashMap)msg3.get("0")).get("NENAKEY"));
            System.out.println("from list 1st = " + ((HashMap)msg3.get("1")).get("XMLKEY"));
            ........end of how MessageObject works!!!*/

            HashMap m = new HashMap();
            m.put("recordtype","SOIFile");
            MessageObject msg = new MessageObject(m);
            bl.process(mpx,msg);
            System.out.println("retrieved added rows to the list");


        }
        catch (Exception pe)
        {
             System.out.println("FAILED IN MAIN OF DBQuery:" + pe.getClass());
        }
    }//end of main*/
}

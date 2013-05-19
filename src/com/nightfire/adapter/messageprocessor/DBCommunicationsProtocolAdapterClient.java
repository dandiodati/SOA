/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.io.*;
import java.sql.*;
import java.text.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.adapter.messageprocessor.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.generator.*;

import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * XML interface to database.
 * This class is a MessageProcessor which manages database queries.
 * It takes database queries formatted as XML, and performs all the
 * necessary jdbc manipulations to execute the query, and return
 * a result formatted as XML for the consumer.
 */
public class DBCommunicationsProtocolAdapterClient extends MessageProcessorBase
{
    /**
     * Keyword tokens use to parse database request.
     */
    public static final String OPERATION_TOKEN = "operation";

    public static final String TABLE_TOKEN = "table";

    public static final String COLUMNS_TOKEN = "columns";
    public static final String COLUMN_INPUTS = "inputcontainer";

    public static final String INPUT_TOKEN = "input";

    public static final String WHERE_TOKEN         = "where";
    public static final String WHERE_CLAUSE_NAME   = "where.clause";
    public static final String WHERE_CLAUSE_INPUTS = "where.inputcontainer";

    public static final String SELECT_OP_TYPE = "select";
    public static final String INSERT_OP_TYPE = "insert";
    public static final String UPDATE_OP_TYPE = "update";
    public static final String DELETE_OP_TYPE = "delete";

    public static final String RESULT_ROOT_NAME = "db_results";
    public static final String RESULT_NAME      = "itemcontainer";

  	private static final String SEPARATOR = "|";
  	private static final String PROCESSOR_NAME_TAG = "NAME";
  	private static final String TO_PROCESSOR_NAME_TAG = "NEXT_PROCESSOR_NAME";
  	private static final String DATE_FORMAT_TAG = "DATE_FORMAT";
  	private static final String MAX_RETURNED_ROWS_PROP = "MAX_RETURNED_ROWS";
  	private static final int    DEFAULT_MAX_RETURNED_ROWS = 100;

  	private String dbUser = "";
  	private String tableName = "";
  	private String dateFormat = "";
  	private String name = "";
   	private String[] toProcessorNames;
        private int maxReturnedRows = 0;

    /**
     * Create a communications-protocol adapter for database operations.
     */
    public DBCommunicationsProtocolAdapterClient ( )
    {
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS, "Creating db communications protocol adapter." );


        // Register to receive connection-close events so we can close all open prepared statements.
        DBInterface.registerDBEventListener( new DBCommunicationsProtocolAdapterClientDBEventListener(this) );
    }


    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException Thrown when Initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize(key, type);
      	dateFormat = (String) adapterProperties.get(DATE_FORMAT_TAG);
      	String maxReturnedRowsString  = (String) adapterProperties.get(MAX_RETURNED_ROWS_PROP);
        try
	{
            Integer maxReturnedRowsInteger = new Integer(maxReturnedRowsString);
            maxReturnedRows = maxReturnedRowsInteger.intValue();
	}
        catch(NumberFormatException nfe)
	{
            Debug.log(Debug.ALL_WARNINGS, "Failed to find MAX_RETURNED_ROWS property. " +
                      "Using default value of " + DEFAULT_MAX_RETURNED_ROWS + ".");
            maxReturnedRows = DEFAULT_MAX_RETURNED_ROWS;
        }
    }


    /**
     * Process the input DOM object-tree containing SQL operation.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Result of SQL operation in DOM object-tree form.
     *
     * @exception  MessageException  Thrown if bad message
     *
     * @exception  ProcessingException Thrown if processing fails.
     */
    public synchronized NVPair[] execute ( MessageProcessorContext mpcontext, Object input )
                                                                     throws MessageException,
                                                                           ProcessingException
    {
        if(input == null)
        {
            reset();
            return null;
        }
        if ( !(input instanceof Document) )
        {
            throw new ProcessingException( "ERROR: DBCommunicationsProtocolAdapterClient: Invalid input type to db communications protocol adapter [" +
                                        input.getClass().getName() + "]." );
        }

        // Construct a parser to access the DOM Document object-tree argument passed-in.
        request = new XMLMessageParser( );

        request.setDocument( (Document)input );

        request.log( );


        // Call appropriate method based on database operation type: SELECT, UPDATE, INSERT, DELETE.
        String reqType;
        try
        {
            reqType = request.getValue( OPERATION_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }
	      NVPair[] nvpair;

        if ( SELECT_OP_TYPE.equalsIgnoreCase( reqType ) )
        {
            nvpair = formatNVPair(processSelect( ));
            return(nvpair);
        }
        else
        if ( INSERT_OP_TYPE.equalsIgnoreCase( reqType ) )
        {
	          nvpair = formatNVPair(processInsert( ));
            return(nvpair);
        }
        else
        if ( UPDATE_OP_TYPE.equalsIgnoreCase( reqType ) )
        {
            nvpair = formatNVPair(processUpdate( ));
            return(nvpair );
        }
        else
        if ( DELETE_OP_TYPE.equalsIgnoreCase( reqType ) )
        {
            nvpair = formatNVPair(processDelete( ) );
            return(nvpair );
        }
        else
        {
            throw new ProcessingException( "ERROR: DBCommunicationsProtocoladapterClient: Unknown db operation type [" + reqType + "]." );
        }
    }

    /**
     * Parse and execute the DOM-formatted SQL SELECT operation.
     *
     * @return  Result of SQL operation in DOM object-tree form.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private Document processSelect ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_DATA, "Building SQL SELECT statement from XML message."  );

        // Construct the SQL statement from the XML values ...
        try
        {
           tableName = request.getValue( TABLE_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }
        String columns;
        try
        {
            columns = request.getValue( COLUMNS_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }

        StringBuffer sb = new StringBuffer( );

        sb.append( "SELECT " );
        sb.append( buildColumnList( columns, SELECT_INSERT_COL_LIST ) );
        sb.append( " FROM " );
        sb.append( tableName );

        if ( request.exists( WHERE_TOKEN ) )
        {
            sb.append( " WHERE " );

            try
            {
               sb.append( request.getValue(WHERE_CLAUSE_NAME) );
            }
            catch (MessageParserException mpe)
            {
                throw new ProcessingException (mpe.getMessage());
            }

        }

        String sqlStmt = sb.toString( );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "SQL statement given to db communications protocol adapter:\n" + sqlStmt );

        try
        {
            // Get a prepared statement object to execute the SQL statement.
            PreparedStatement ps = getPreparedStatement( sqlStmt );

            // Populate any inputs in where-clause.
            if ( request.exists( WHERE_CLAUSE_INPUTS ) )
            {
                Node n = request.getNode( WHERE_CLAUSE_INPUTS );

                populatePreparedStatement( ps, 0, n );
            }

            // Execute the SQL statement.
            java.sql.ResultSet rs = DBInterface.executeQuery( ps );

            // Populate a new DOM Document with any returned results.
            Document d = generateResult( rs );

            ps.close();

            return d;
        }
        catch ( Exception e )
        {
          throw new ProcessingException( "ERROR: DBCommunicationsProtocolAdapterClient: Could not execute SQL statement [" + sqlStmt +
                                        "] against database:\n" + e.getMessage() );
       }
    }


    /**
     * Parse and execute the DOM-formatted SQL INSERT operation.
     *
     * @return  null
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private Document processInsert ( ) throws ProcessingException
    {
        String columns;

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "Building SQL INSERT statement from XML message."  );

        // Construct the SQL statement from the XML values ...
        try {
           tableName = request.getValue( TABLE_TOKEN );
           columns = request.getValue( COLUMNS_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }

        StringBuffer sb = new StringBuffer( );

        sb.append( "INSERT INTO " );
        sb.append( tableName );
        sb.append( " ( " );
        sb.append( buildColumnList( columns, SELECT_INSERT_COL_LIST ) );
        sb.append( " ) VALUES ( " );
        sb.append( buildValueList( columns ) );
        sb.append( " )" );

        String sqlStmt = sb.toString( );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "SQL statement given to db communications protocol adapter:\n" + sqlStmt );

        try
        {
            // Get a prepared statement object to execute the SQL statement.
            PreparedStatement ps = getPreparedStatement( sqlStmt );

            // Populate inputs.
            if ( request.exists( COLUMN_INPUTS ) )
            {
                Node n = request.getNode( COLUMN_INPUTS );

                populatePreparedStatement( ps, 0, n );
            }

            // Execute the SQL statement.
            DBInterface.executeUpdate( ps, false );

            ps.close();


            // Nothing to return here.
            return null;
        }
        catch ( Exception e )
        {
             throw new ProcessingException( "ERROR: DBCommunicationsProtocolAdapterClient: Could not execute SQL statement [" + sqlStmt +
                                        "] against database:\n" + e.getMessage() );
        }
    }


    /**
     * Parse and execute the DOM-formatted SQL UPDATE operation.
     *
     * @return  null
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private Document processUpdate ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "Building SQL UPDATE statement from XML message."  );

        // Construct the SQL statement from the XML values ...
        try {
          tableName = request.getValue( TABLE_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }

        String columns;
        try {
            columns = request.getValue( COLUMNS_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }

        StringBuffer sb = new StringBuffer( );

        sb.append( "UPDATE " );
        sb.append( tableName );
        sb.append( " SET " );
        sb.append( buildColumnList( columns, UPDATE_COL_LIST ) );

        if ( request.exists( WHERE_TOKEN ) )
        {
            sb.append( " WHERE " );
            try {
                sb.append( request.getValue(WHERE_CLAUSE_NAME) );
            }
            catch (MessageParserException mpe)
            {
                throw new ProcessingException (mpe.getMessage());
            }

        }

        String sqlStmt = sb.toString( );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "SQL statement given to db communications protocol adapter:\n" + sqlStmt );

        try
        {
            // Get a prepared statement object to execute the SQL statement.
            PreparedStatement ps = getPreparedStatement( sqlStmt );

            int popCount = 0;

            // Populate inputs.
            if ( request.exists( COLUMN_INPUTS ) )
            {
                Node n = request.getNode( COLUMN_INPUTS );

                popCount = populatePreparedStatement( ps, popCount, n );
            }

            // Populate any inputs in where-clause.
            if ( request.exists( WHERE_CLAUSE_INPUTS ) )
            {
                Node n = request.getNode( WHERE_CLAUSE_INPUTS );

                populatePreparedStatement( ps, popCount, n );
            }

            // Execute the SQL statement.
            DBInterface.executeUpdate( ps, false );
            ps.close();


            // Nothing to return here.
            return null;
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: DBCommunicationsProtocolAdapterClient: Could not execute SQL statement [" + sqlStmt +
                                        "] against database:\n" + e.getMessage() );
        }
    }


    /**
     * Parse and execute the DOM-formatted SQL DELETE operation.
     *
     * @return  null
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private Document processDelete ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "Building SQL DELETE statement from XML message."  );

        // Construct the SQL statement from the XML values ...
        try {
          tableName = request.getValue( TABLE_TOKEN );
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException (mpe.getMessage());
        }

        StringBuffer sb = new StringBuffer( );

        sb.append( "DELETE FROM " );
        sb.append( tableName );

        if ( request.exists( WHERE_TOKEN ) )
        {
            sb.append( " WHERE " );
            try {
               sb.append( request.getValue(WHERE_CLAUSE_NAME) );
            }
            catch (MessageParserException mpe)
            {
                throw new ProcessingException (mpe.getMessage());
            }

        }

        String sqlStmt = sb.toString( );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "SQL statement given to db communications protocol adapter:\n" + sqlStmt );

        try
        {
            // Get a prepared statement object to execute the SQL statement.
            PreparedStatement ps = getPreparedStatement( sqlStmt );

            // Populate any inputs in where-clause.
            if ( request.exists( WHERE_CLAUSE_INPUTS ) )
            {
                Node n = request.getNode( WHERE_CLAUSE_INPUTS );

                populatePreparedStatement( ps, 0, n );
            }

            // Execute the SQL statement.
            DBInterface.executeUpdate( ps, false );
            ps.close();


            // Nothing to return here.
            return null;
        }
        catch ( Exception e )
        {
           throw new ProcessingException( "ERROR: DBCommunicationsProtocolAdapterClient: Could not execute SQL statement [" + sqlStmt +
                                        "] against database:\n" + e.getMessage() );
        }
    }

    /**
     * Return the SQL Data type for the column.
     *
     * @param  column   Name of the Column in question.
     *
     * @return  Type of the column as enumerated in java.sql.Types.
     */
    private int checkDataType(String column) throws ProcessingException
    {
        ResultSet rs = null;
        Connection dbCon = null;
        try
	{
            // get metadata
            dbCon = DBConnectionPool.getInstance().acquireConnection();
            DatabaseMetaData dmd = dbCon.getMetaData();
            dbUser = dmd.getUserName();
            rs = dmd.getColumns(null, dbUser.toUpperCase(), tableName.toUpperCase(), null);
            int colType = 0;
            String temp = null;

            // check to see if the type is date, if so, put in hastable
            while(rs.next())
	    {
                temp = rs.getString("COLUMN_NAME");
                
                if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                    Debug.log(Debug.UNIT_TEST, "Checking Column Type for Column :"  + column);
                
                if(temp.equalsIgnoreCase(column))
	           {
                    colType = rs.getInt("DATA_TYPE");
                    
                    if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                        Debug.log(Debug.UNIT_TEST, "Type :" + colType);
                }
                return colType;
            }
        }
        catch(ResourceException dbe)
        {
	   Debug.log(Debug.ALL_ERRORS, "ERROR: Failed getting the data type for the column " +
                      column + " . " + dbe.getMessage());
	   throw new ProcessingException("Failed getting the data type for the column " +
	                                 column + " . " + dbe.getMessage());
	}
        catch(SQLException sqle)
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: Failed getting the data type for the column " +
                      column + " . " + sqle.getMessage());
            throw new ProcessingException("Failed getting the data type for the column " +
                                          column + " . " + sqle.getMessage());
        }
        finally
	{
            try
	    {
                rs.close();
                DBConnectionPool.getInstance().releaseConnection(dbCon);
            }
            catch(ResourceException re)
            {
	       Debug.log(Debug.ALL_ERRORS, "ERROR: Failed getting the data type for the column " +
	                 column + " . " + re.getMessage());
	       throw new ProcessingException("Failed getting the data type for the column " +
	                                     column + " . " + re.getMessage());
            }
            catch(SQLException closeE)
            {
                Debug.log(Debug.ALL_ERRORS, "ERROR: Failed closing the ResultSet after error." +
                          closeE.getMessage());
                throw new ProcessingException("Failed closing the ResultSet after error." +
                                              closeE.getMessage());
            }
        }
        return java.sql.Types.OTHER;
    }


    /**
     * Build a list of column-name values for use in SQL statement construction.
     *
     * @param  columns  Whitespace-separated list of column-names.
     * @param  type     SELECT_INSERT_COL_LIST, UPDATE_COL_LIST
     *
     * @return  Constructed column-name list.
     */
    private String buildColumnList ( String columns, int type ) throws ProcessingException
    {
        StringBuffer sb = new StringBuffer( );

        StringTokenizer st = new StringTokenizer( columns );

        int colType = 0;
        int colCount = 0;
        String column = "";
        boolean firstTime = true;

        while( st.hasMoreTokens() )
        {
            colCount ++;
            column = st.nextToken();
            if ( firstTime )
                firstTime = false;
            else
                sb.append( ", " );

            sb.append( column );

            if ( type == UPDATE_COL_LIST )
                sb.append( " = ?" );

            // check to see if the type is date, if so, put in hastable
            
            if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                Debug.log(Debug.UNIT_TEST, "Checking Column Type for placeholder number :" + colCount);
            colType = checkDataType(column);
            if((colType == java.sql.Types.TIMESTAMP) ||
                (colType == java.sql.Types.LONGVARCHAR ))
	    {
                columnTypeTable.put(new Integer(colCount), new Integer(colType));
                
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "Entering Column number in column type table:" + colCount);
	    }
        }
        return( sb.toString() );
    }


    /**
     * Build a list of column-name value placeholders, one per column-name in input.
     *
     * @param  columns  Whitespace-separated list of column-names.
     *
     * @return  Constructed column-name value placeholders list.
     */
    private String buildValueList ( String columns )
    {
        StringBuffer sb = new StringBuffer( );

        StringTokenizer st = new StringTokenizer( columns );

        int numCols = st.countTokens( );

        for ( int Ix = 0;  Ix < numCols;  Ix ++ )
        {
            if ( Ix > 0 )
                sb.append( ", " );

            sb.append( '?' );
        }

        return( sb.toString() );
    }


    /**
     * Generate an XML DOM Document containing the returned database row
     * values in the result-set.
     *
     * @param  rs  Result-set object containing query results.
     *
     * @return  DOM Document containing results.
     *
     * @exception  MessageGeneratorException,SQLException  Thrown if processing fails.
     */
    private Document generateResult ( ResultSet rs ) throws MessageGeneratorException, SQLException
    {
        // Create a message generator object to populate DOM with results passed-in.
        XMLMessageGenerator generator = new XMLMessageGenerator( RESULT_ROOT_NAME );

        generator.create( RESULT_NAME );

        generator.setAttributeValue( RESULT_NAME, "type", "container" );

        // Get the column names for the returned values.
        ResultSetMetaData md = rs.getMetaData( );

        int colCount = md.getColumnCount( );

        int resultCount = 0;

        // While database rows are available ...
        while ( rs.next( ) && resultCount < maxReturnedRows)
        {
            if(Debug.isLevelEnabled(Debug.DB_DATA))
                Debug.log(Debug.DB_DATA, "Populating result [" + resultCount + "] value(s) ..." );

            // Construct name for current result iteration.
            String resultName = RESULT_NAME + "." + resultCount + ".";

            // Loop over available column names, extracting value from result-set and placing it in generator.
            for ( int Ix = 1;  Ix <= colCount;  Ix ++ )
            {
                // Get the column name from the meta-data.
                String colName = md.getColumnName( Ix );

                int colType = md.getColumnType( Ix );

                if(Debug.isLevelEnabled(Debug.DB_DATA))
                    Debug.log(Debug.DB_DATA, "\t Populating from column [" + colName +
                           "] of type [" + colType + "]." );

                String value = null;

                // Get the column value from the result-set.
                if ( (colType == Types.LONGVARBINARY) || (colType == Types.VARBINARY) ||
                     (colType == Types.BINARY) )
                {
                    try
                    {
                        value = new String( DBLOBUtils.getBLOB( rs, colName ) );
                    }
                    catch ( Exception e )
                    {
                        throw new MessageGeneratorException( e.toString() );
                    }
                }
                else
                {
                    value = rs.getString( colName );
                }

                // Set the name/value-pair in the generator.
                generator.setValue( resultName + colName, value );
            }

            resultCount ++;
        }

        rs.close( );

        // Return the generator's internal DOM Document that we just populated.
        return( generator.getDocument() );
    }


    /**
     * Populate the given prepared statement object with the given input values.
     *
     * @param  ps          Prepared-statement object to populate.
     * @param  startCount  Starting count for placeholder population (0-based).
     * @param  n           Inputcontainer node containing 'input' values to use in population.
     *
     * @return  Number of placeholders populated so far.
     *
     * @exception  ProcessingException,SQLException  Thrown if processing fails.
     */
    private int populatePreparedStatement ( PreparedStatement ps, int startCount, Node n ) throws ProcessingException, SQLException
    {
      try
      {
        // Get number of child nodes for node passed-in.
        int count = XMLMessageBase.getXMLNodeChildCount( n );

        if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log(Debug.DB_DATA, "Populating prepared statement with [" + count + "] value(s) ..." );

        // Loop over child nodes.
        for ( int Ix = 0;  Ix < count;  Ix ++ )
        {
            // Get current node.
            Node child = XMLMessageBase.getChildXMLNode( n, Ix );

            // If node isn't a value node, or doesn't have the name 'input', indicate error to caller.
            if ( !XMLMessageBase.isValueNode( child ) || !INPUT_TOKEN.equalsIgnoreCase( child.getNodeName() ) )
            {
                throw new ProcessingException( "ERROR: DBCommunicationsProtocolAdapterClient: Node [" + child.getNodeName() + "] is invalid for SQL input." );
            }

            // Extract the value from the node.
            String value = XMLMessageBase.getNodeValue( child );

            int psLoc = Ix + 1 + startCount;

            if(Debug.isLevelEnabled(Debug.MSG_STATUS)) 
            {
                Debug.log(Debug.MSG_STATUS, "Working on placeholder " + psLoc);
                Debug.log(Debug.MSG_STATUS, "Table lookup for  " + psLoc + " is " +
                                        columnTypeTable.get(new Integer(psLoc)));
            }
            
            if(Debug.isLevelEnabled(Debug.DB_DATA))
            Debug.log( Debug.DB_DATA, "\tPrepared statement placeholder [" +
                       psLoc + "] value = [" + value + "] ..." );

            // Place the value in the prepared-statement.
            Integer colType = (Integer)columnTypeTable.get(new Integer(psLoc));
            if ( null != colType )
	    {
                // set date with a properly formatted sql date
                Debug.log(Debug.UNIT_TEST, "Found special column Type");
                if( colType.intValue() == java.sql.Types.TIMESTAMP)
                {
                    java.sql.Timestamp timeStamp = new java.sql.Timestamp(
                                         new SimpleDateFormat(dateFormat).parse(value).getTime());
                    ps.setTimestamp(psLoc, timeStamp);
                }
                else if( colType.intValue() == java.sql.Types.LONGVARCHAR)
                {
                    try
                    {
                        DBLOBUtils.setCLOB( ps, psLoc, value );
                    }
                    catch ( Exception e )
                    {
                        throw new ProcessingException( e.toString() );
                    }
                }
            }
            else if ( value.length() < MAX_INSERT_LENGTH_AS_STRING )
            {
                ps.setString( psLoc, value );
            }
            else
            {
                try
                {
                    DBLOBUtils.setCLOB( ps, psLoc, value );
                }
                catch ( Exception e )
                {
                    throw new ProcessingException( e.toString() );
                }
            }
        }
        return count;
       }//try
       catch (MessageException me)
       {
           Debug.log(Debug.ALL_ERRORS, "ERROR: Failed populating the prepared statement " +
		     "from the Message: MessageException: " + me.getMessage());
           throw new ProcessingException ("Failed populating the prepared statement " +
		     "from the Message: MessageException: " + me.getMessage());
       }
       catch (ParseException pe)
       {
           Debug.log(Debug.ALL_ERRORS, "ERROR: Failed populating the prepared statement " +
		     "from the Message: Date Parsing Exception: " + pe.getMessage());
           throw new ProcessingException ("Failed populating the prepared statement " +
		     "from the Message: Date Parsing Exception: " + pe.getMessage());
       }
    }


    /**
     * Get the appropriate prepared-statement object from the cache, or create a new one if not found.
     *
     * @param  sqlStmt  SQL statement to get prepared-statement object for.
     *
     * @return  Prepared-statement object for SQL statement passed-in.
     *
     * @exception  DatabaseException  Thrown if prepared-statement can't be created.
     */
    private PreparedStatement getPreparedStatement ( String sqlStmt ) throws DatabaseException
    {
        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS, "Retrieving prepared statement for SQL string ..." );

            // Create a new prepared-statement object for the SQL statement passed-in.
            PreparedStatement ps = DBInterface.getPreparedStatement( sqlStmt );

        return ps;
    }


    /**
     * Empty the prepared-statement cache, closing any open objects.
     */
    public synchronized void reset ( )
    {

    }


    /**
     * Class DBCommunicationsProtocolAdapterClientDBEventListner acts as DBCommunicationsProtocolAdapterClient's proxy
     * to the DBInterface to listen for database events.
     */
    private class DBCommunicationsProtocolAdapterClientDBEventListener implements DBEventListener
    {
        public DBCommunicationsProtocolAdapterClientDBEventListener ( DBCommunicationsProtocolAdapterClient parent )
        {
            this.parent = parent;
        }

        // Tell parent to reset itself.
        public void reset ( )
        {
            parent.reset( );
        }


        DBCommunicationsProtocolAdapterClient parent;
    }


    //Following method is used only for testing purposes to generate context
    private MessageProcessorContext getContext ()
    {
        MessageProcessorContext context = null;
        try
	{
            context = new MPC();
        }
        catch(Exception e)
	{
            Debug.log(Debug.ALL_ERRORS, e.getMessage());
        }
        return context;
    }

    //Following class is used only for testing purposes
    public class MPC extends MessageProcessorContext
    {
      public MPC () throws ProcessingException
      {
        try
        {
          if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log( Debug.DB_STATUS, "DBCommunicationsProtocolAdapterClient: -----Initializng MessageProcessorContext-----");
          
          setDBConnection (DBConnectionPool.getInstance().acquireConnection( ));
          getDBConnection().setAutoCommit(true);
        }
        catch ( Exception dbe)
        {
          Debug.log(Debug.DB_ERROR, "ERROR: DBCommunicationsProtocolAdapterClient: database error, can not get connection " + dbe);
        }
      }
    }

    /**
     * Main method for testing purposes.
     * @param args The arguments to this class for testing purposes. The number of
     * arguments should be 6 and are <KEY> <TYPE> <Input xml document file name as
     * test.xml, should be located in current directory> <DBNAME> <DBUSER> <DBPASSWD>
     */
    public static void main (String[] args) {

    /* Following is a sample command to execute this program
    jre -nojit -classpath %CLASSPATH% com.nightfire.spi.common.adapter.XMLMessageLogger
    COVAD_ORDER OUT_LOGGER dsl.xml jdbc:oracle:thin:@192.168.10.158:1521:orcl
    b36nt b36nt
    */

        Debug.enableAll();
        Debug.showLevels();

        if (args.length != 6)
        {
          Debug.log (Debug.ALL_ERRORS, "DBCommunicationsProtocolAdapterClient: USAGE:  "+
          "jre -nojit -classpath %CLASSPATH%  "+
          "com.nightfire.adapter.messageprocessor.DBCommunicationsProtocolAdapterClient "+
          "COVAD_ORDER OUT_LOGGER dsl.xml jdbc:oracle:thin:@192.168.10.158:1521:orcl b36nt b36nt");
        }
        String ilecAndOSS = args[0];
        String propertyType = args[1];
        String MESSAGEFILE = args[2];


        // Read in message file and set up database:
        FileCache messageFile = new FileCache();
        String inputMessage = null;
        Document inputDom = null;
        try {

            inputMessage = messageFile.get(MESSAGEFILE);

        } catch (FrameworkException e) {
            Debug.log(null, Debug.MAPPING_ERROR, "DBCommunicationsProtocolAdapterClient.main: " +
                      "FileCache failure: " + e.getMessage());
        }

        try {
            XMLMessageParser parser = new XMLMessageParser(inputMessage);
        } catch (MessageException e) {
            Debug.log(null, Debug.MAPPING_ERROR, "DBCommunicationsProtocolAdapterClient.main: " +
                      "Failed getting Parser for input XML: " + e.getMessage());
        }


        try {

            DBInterface.initialize(args[3], args[4], args[5]);

        } catch (DatabaseException e) {
            Debug.log(null, Debug.MAPPING_ERROR, "DBCommunicationsProtocolAdapterClient.main: " +
                      "Database initialization failure: " + e.getMessage());
        }

        DBCommunicationsProtocolAdapterClient adapter = new DBCommunicationsProtocolAdapterClient();

        try {

            adapter.initialize(ilecAndOSS, propertyType);

        } catch (ProcessingException e) {
            Debug.log(null, Debug.MAPPING_ERROR, "DBCommunicationsProtocolAdapterClient.main: " +
                      "call to initialize failed with message: " + e.getMessage());
        }

        MessageProcessorContext context = adapter.getContext();

        try {
            adapter.execute(context,inputDom);

        }
        catch (MessageException e) {
            Debug.log(null, Debug.MAPPING_ERROR, "DBCommunicationsProtocolAdapterClient.main: " +
                      "call to process failed with message: " + e.getMessage());
        }
        catch (ProcessingException e) {
            Debug.log(null, Debug.MAPPING_ERROR, "DBCommunicationsProtocolAdapterClient: " +
                      "call to process failed with message: " + e.getMessage());
        }
    }

    // Maximum length criteria for insertion of value into prepared-statement as a string (above
    // this we use byte-array input stream).
    private static final int MAX_INSERT_LENGTH_AS_STRING = 2000;

    private static final int SELECT_INSERT_COL_LIST = 1;
    private static final int UPDATE_COL_LIST = 2;

    private Hashtable columnTypeTable = new Hashtable();

    XMLMessageParser request;
}

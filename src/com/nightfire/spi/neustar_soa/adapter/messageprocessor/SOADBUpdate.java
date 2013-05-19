/**
 * The purpose of this program to update the configured table .This component
 * will ignore null value's column
 * 
 * @author Ashok Kumar
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair
 * @see 		com.nightfire.framework.db.DBInterface
 * @see			com.nightfire.framework.util.StringUtils
 * @see			com.nightfire.spi.common.driver.MessageObject
 * @see			com.nightfire.spi.common.driver.MessageProcessorContext
 * @see			com.nightfire.spi.common.driver.MessageProcessorBase
 * @see			com.nightfire.framework.db.DBConnectionPool
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			07/02/2004			Created	
	2			Ashok			07/08/2004			Review comments incorporated
	3			Ashok			07/29/2004			Formal review comments 
													incorporated.

	
 */


package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.framework.db.DBConnectionPool;


public class SOADBUpdate extends MessageProcessorBase
{
	
	/**
	 * Variable contains value of Table name
	 */
   	private String tableName = null;
   	
	/**
	 * Variable contains value of Seperator
	 */
    private String separator = null;
    
	/**
	 * Variable contains value of transaction logging
	 */
    private boolean usingContextConnection = true;
    
	/**
	 * Variable contains list of ColumnDatas which contains 
	 * all configured column data(Column type , Column Name ...)
	 */
    private List columns = null;	
	
	/**
	 * Variable contains sql where clause
	 */
	private String sqlWhereStmt = null;
	
	/**
	 * Variable contains error buffer string
	 */    
	private StringBuffer errorBuffer = null;	
			
    /**
     * Constructor.
     */
    public SOADBUpdate ( )
    {
    	if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE, 
									"Creating SOADBUpdate message-processor." );
		}

		columns = new ArrayList( );		
		
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key  Property-key to use for locating initialization properties.
     * @param  type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) 
    												throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "SOADBUpdate: Initializing..." );
		}

        errorBuffer = new StringBuffer( );
		
		// Get configuration properties specific to this processor.
		
        tableName = getRequiredPropertyValue( SOAConstants.TABLE_NAME_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
							"Database table to update is [" + tableName + "]." );
		}

        String strTemp = getPropertyValue( SOAConstants.TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try 
            {
            	
                usingContextConnection = getBoolean( strTemp );
                
            }
            catch ( FrameworkException e )
            {
            	
                errorBuffer.append ( "Property value for " 
                					+ SOAConstants.TRANSACTIONAL_LOGGING_PROP +
                  					" is invalid. " + e.getMessage ( ) + "\n" );
                  
            }
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
						"SOADBUpdate will participate in overall" +
						" driver transaction? [" + usingContextConnection + "]." );
		}

        separator = getPropertyValue( SOAConstants.LOCATION_SEPARATOR_PROP );

        if ( !StringUtils.hasValue( separator ) )
        {
        
            separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
            
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){    
			Debug.log( Debug.SYSTEM_CONFIG, 
								"Location separator token [" + separator + "]." );
		}
            
		sqlWhereStmt = getRequiredPropertyValue( SOAConstants.SQL_WHERE_STATEMENT_PROP);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
						"'where part of' SQL query is [" + sqlWhereStmt + "]." );
		}
		try
		{
		
			populateColumnData(); 
			
			
		}catch( ProcessingException e )
		{
			
			Debug.log( Debug.ALL_ERRORS, e.toString() );
			
			throw new ProcessingException( 
						" Could not populate column data : "+e.toString());
			       
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "SOADBUpdate: Initialization done." );
		}
    }
    
	/**
	 * This method populates the list for columndata
	 *
	 * @exception ProcessingException when initialization fails
	 **/
	private void populateColumnData( ) throws ProcessingException
	{
		
		String optional = null;
		
		String columnName  = null;
		
		String colType = null;
		
		String dateFormat = null;
		
		String location = null;
		
		String defaultValue = null;
		
		ColumnData cd = null;
		
		for ( int Ix = 0;  true;  Ix ++ )
		{
			optional = getPropertyValue( PersistentProperty
			 				.getPropNameIteration( 
			 				SOAConstants.OPTIONAL_PREFIX_PROP, Ix ));
			 				
			// If we can't find next optional property we're done
			if ( !StringUtils.hasValue( optional ) )
			{
			
				break;
				
			}
			
			columnName = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.COLUMN_NAME_PREFIX_PROP, Ix ) );
						
			colType = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.COLUMN_TYPE_PREFIX_PROP, Ix ) );
						
			dateFormat = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.DATE_FORMAT_PREFIX_PROP, Ix ) );
						
			location = getPropertyValue( PersistentProperty
							.getPropNameIteration( 
							SOAConstants.LOCATION_PREFIX_PROP, Ix ) );
							
			defaultValue = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.DEFAULT_PREFIX_PROP, Ix ) );
			
			try
			{
				
				// Create a new column data object and add it to the list.
				cd = new ColumnData( columnName, colType,dateFormat, 
									 location, defaultValue , optional );
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){							
				  Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
				}
				
				columns.add( cd );
				
			}
			catch ( FrameworkException e )
			{
				
				throw new ProcessingException( "ERROR: Could not create column"+
										" data description:\n" + e.toString() );
										
			}
		}
		
		int columnsCount = columns.size();

		if(sqlWhereStmt==null) // no WHERE clause to be appended
		{
			 // If any of the required properties are absent, 
			 // indicate error to caller.
			if ( errorBuffer.length() > 0 )
			{
				String errMsg = errorBuffer.toString( );
				
				Debug.log( Debug.ALL_ERRORS, errMsg );
				
				throw new ProcessingException( errMsg );
				
			}
			
			return;
			
		}
		
		// Loop until all WhereColumn configuration properties have been read 
		int  count = -1;
		
		int index = 0;
		
		 // to find the number of Column values to be filled in WHERE clause...
		 // this is counted by number of ? in the string
		 while(index != -1)
		 {
			 count++;
			 
			 index = sqlWhereStmt.indexOf("?" , index+1);
			 
		 }
		
		for ( int Ix = 0;  true;  Ix ++ )
		{
			location = getPropertyValue( PersistentProperty
					.getPropNameIteration( 
					SOAConstants.WHERE_LOCATION_PREFIX_PROP, Ix ) );
			
			if ( !StringUtils.hasValue( location ) )
			{	
			
				break;
				
			}
			
			colType = getPropertyValue( PersistentProperty
				.getPropNameIteration( 
				SOAConstants.WHERE_COLUMN_TYPE_PREFIX_PROP, Ix ) );
				
			dateFormat = getPropertyValue( PersistentProperty
				.getPropNameIteration( 
				SOAConstants.WHERE_DATE_FORMAT_PREFIX_PROP, Ix ) );
				
			defaultValue = getPropertyValue( PersistentProperty
				.getPropNameIteration( 
				SOAConstants.WHERE_DEFAULT_PREFIX_PROP, Ix ) );
				
			try
			{
				// Create a new column data object and add it to the list.
				cd = new ColumnData( null, colType, dateFormat, 
									location, defaultValue , "FALSE" );
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){										
				  Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
				}
				
				columns.add( cd );
				
			}
			catch ( FrameworkException e )
			{
				
				throw new ProcessingException( "ERROR: Could not create " +
							"column data description:\n" + e.toString() );
							
			}
		}
		
		int whereColumnsCount = columns.size() - columnsCount;
		
		if(whereColumnsCount!=count)
		{
		
			throw new ProcessingException( "ERROR: Unequal no. of fields in " +
									"the whereStatement and whereAttributes" );
									
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of columns in where " +
											"clause [" + whereColumnsCount + "]." );
		}
		
		//	If any of the required properties are absent, 
		// indicate error to caller.
		 if ( errorBuffer.length() > 0 )
		 {
			 String errMsg = errorBuffer.toString( );
			 
			 Debug.log( Debug.ALL_ERRORS, errMsg );
			 
			 throw new ProcessingException( errMsg );
			 
		 }
		
	}


    /**
     * Extract data values from the context/input, and use them to
     * update row(s) in the configured database table.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, 
    						  MessageObject inputObject )
                        		throws MessageException, ProcessingException
    {
    	ThreadMonitor.ThreadInfo tmti = null;
		if ( inputObject == null )
		{
		
			return null;
            
		}
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
          Debug.log( Debug.MSG_STATUS, "SOADBUpdate: processing ... " );
		}
		
        Connection dbConn = null;
        
        try
		{
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        
			//	Reset the values in cases where we're logging again.
			resetColumnValues( );
	
			// Extract the column values from the arguments.
			extractMessageData( mpContext, inputObject );
		
		    // Get a database connection from the appropriate location - 
			// based on transaction characteristics.
			if ( usingContextConnection )
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database update is " +
							"transactional, so getting connection from context." );
				}

				dbConn = mpContext.getDBConnection( );
				
			}
			else
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database update is not " +
								"transactional, so getting connection from pool." );
				}
				try
				{
				
					dbConn 
						= DBConnectionPool.getInstance().acquireConnection( );
				}
				catch ( ResourceException re )
				{
					
					Debug.log( Debug.ALL_ERRORS, re.getMessage() );
					
				}
				
			}
			
			if( dbConn == null )
			{
	
				throw new ProcessingException("Connection is not acquired ," +
														"it is null ");
	
			}
			
			//	Update the data to the database.
			update( dbConn );			

			// If the configuration indicates that this SQL operation isn't
			// part of the overall driver transaction, commit the changes now.
			if ( !usingContextConnection )
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Committing data " +
														" updated to database." );
				}
				
				try
				{								
				
					DBConnectionPool.getInstance().commit( dbConn );
					
				}
				catch ( ResourceException re )
				{
					
					Debug.log( Debug.ALL_ERRORS, re.getMessage() );
				
				}
				
			}
		}
		catch ( ProcessingException e )
		{
			
			String errMsg = "ERROR: SOADBUpdate: Attempt to update " +
								"to database failed with error: " + 
								e.getMessage();

			Debug.log( Debug.ALL_ERRORS, errMsg );

			// If the configuration indicates that this SQL operation 
			// isn't part of the overall driver
			// transaction, roll back any changes now.
			if ( !usingContextConnection )
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Rolling-back any database " +
											"changes due to database-update." );
				}

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
			ThreadMonitor.stop(tmti);
			// If the configuration indicates that this SQL operation isn't 
			// part of the overall driver
			// transaction, return the connection previously acquired
			// back to the resource pool.
			if ( !usingContextConnection && (dbConn != null) )
			{
				try
				{
					
					DBConnectionPool.getInstance().releaseConnection( dbConn );
					
					dbConn = null;
					
				}
				catch ( ResourceException e )
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
	 * @param  dbConn  The database connection to perform the SQL 
	 * 				  update operation against.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
   	private void update ( Connection dbConn ) throws ProcessingException
   	{
	   
	   	PreparedStatement pstmt = null;
	   
	   	String 	sqlUpdateStmt = null;

	   	try
	   	{
			sqlUpdateStmt = constructUpdateSqlStatement();
	   		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
				Debug.log( Debug.NORMAL_STATUS, 
								 "Executing Update SQL:\n" + sqlUpdateStmt );
			}

		   	// Get a prepared statement for the SQL statement.
		   	pstmt = dbConn.prepareStatement( sqlUpdateStmt );

		   	// Populate the SQL statement using values obtained from 
		   	// the column data objects.
		   	populateUpdateSqlStatement( pstmt );

		   	// Execute the SQL update operation.
		   	pstmt.execute( );
            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
				Debug.log( Debug.NORMAL_STATUS, "Successfully updated  into table ["
						  + tableName + "]" );
			}

		   
	   	}
	   	catch ( SQLException sqle )
	   	{
	   	
		   	throw new ProcessingException( "ERROR: Could not update row " +
		   				"into database table [" + tableName
						+ "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
						
	   	}
	   	catch ( ProcessingException e )
	   {
	   	
		   	throw new ProcessingException( "ERROR: Could not update " +
		   					"row into database table [" + tableName
										  + "]:\n" + e.toString() );
										  
	   }
	   finally
	   {
		   	if ( pstmt != null )
		   	{
		   	
			   	try
			   	{
			   	
				   	pstmt.close( );
				   	
				   	pstmt = null;
				   
			   	}
			   	catch ( SQLException sqle )
			   	{
				   
				   	Debug.log( Debug.ALL_ERRORS, 
				   						DBInterface.getSQLErrorMessage(sqle) );
				   
			   	}
		   	}
	   	}
   	}
   	
	/**
	 * Construct the SQL update statement from the column data.If any 
	 * column doesn't have value or name
	 *
	 * @return SQL Update statement.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private String constructUpdateSqlStatement( )throws ProcessingException
	{
		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){	
		  Debug.log( Debug.DB_DATA, "Preparing SQL Update statement ..." );
		}

		Iterator iter = columns.iterator( );

		StringBuffer updateSql = new StringBuffer();
   	
		updateSql.append( "UPDATE " );

		updateSql.append( tableName );

		updateSql.append( " SET " );

		boolean flag = true;
   	
		ColumnData cd = null;
		
		while( iter.hasNext() )
		{
			cd = ( ColumnData )iter.next( );
		
			// if column name and/or value is null ,then no need to 
			// update that column		
			if(  SOAUtility.isNull( cd.value ) 
				||  SOAUtility.isNull( cd.columnName ) )
			{
		
				continue;
			
			} 
			
			if ( flag )
			{
		
				flag = false;
			
			} 
			else
			{
	   	
				updateSql.append( ", " );
	   		
			} 
		
			updateSql.append( cd.columnName );

			// If the current column is a date column, and the configuration
			// indicates that the current system date should be used for
			// the value, place it in the SQL statement now.
			if ( StringUtils.hasValue( cd.columnType )
				&& cd.columnType.equalsIgnoreCase( 
										SOAConstants.COLUMN_TYPE_DATE )
				&& (cd.value instanceof String)
				&& ((String)( cd.value ) ).equalsIgnoreCase( 
												SOAConstants.SYSDATE ) )
			{

				updateSql.append( " = " );
			
				updateSql.append( SOAConstants.SYSDATE );

			}
			else
			{

				updateSql.append( " = ? " );

			}
	
		}

		updateSql.append(" " + sqlWhereStmt );
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		 Debug.log( Debug.DB_DATA, "Done constructing SQL Update statement." );
		}

		return ( updateSql.toString() );
  	
	}

	/**
	* Populate the SQL update statement from the column data.
	*
	* @param  pstmt  The prepared statement object to populate.
	*
	* @exception ProcessingException  thrown if population fails.
	*/
    private void populateUpdateSqlStatement ( 	PreparedStatement pstmt ) 
   												throws ProcessingException
    {
		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
	     Debug.log( Debug.DB_DATA, "Populating SQL update statement ..." );
		}
	    
	    try
	    {
	    
		    int Ix = 1;  // First value slot in prepared statement.
	
		    Iterator iter = columns.iterator( );
		    
			ColumnData cd = null;
	
		    while ( iter.hasNext() )
		    {
	
			    cd = (ColumnData)iter.next( );
			    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA, "Populating prepared-statement slot " +
								"[" + Ix + "] with column data ." + cd.describe() );
				}
			   	
			   	// not populate null value
			   	if( !( SOAUtility.isNull( cd.value ) ) )
			   	{			
			   		    
				    // Default is no column type specified.
				   	if ( !StringUtils.hasValue( cd.columnType ) )
				   	{
				   	
					   	pstmt.setObject( Ix, cd.value );
					   
				   	}
				   	else if ( cd.columnType.equalsIgnoreCase(
				   								SOAConstants.COLUMN_TYPE_DATE) )
				   	{
					   	String val = (String)(cd.value);
					   
					   	if ( val.equalsIgnoreCase(SOAConstants.SYSDATE) )
					   	{
						   						   
						   	continue;
						   
					   	}
					   	// If we're not updating the current system date, 
					   	// the caller must have provided an actual date value,
					   	// which we must now parse.
					   	if ( !StringUtils.hasValue( cd.dateFormat ) )
					   	{
					   	
						   	throw new ProcessingException( 
								"ERROR: Configuration column does not " +
								"specify date format." );
						   				
					   	}
		
					   	SimpleDateFormat sdf 
					   					= new SimpleDateFormat( cd.dateFormat );
					   					
						java.util.Date d = null;
		
						try
						{
					
						 	d = sdf.parse( val );
	                
						}catch( ParseException parseException )
						{
						
							throw new ProcessingException( 
								"ERROR: could not parse date" +
								"\for [" + cd.describe() + "] ." 
								+ parseException.toString() );
						
						}
		
					   	pstmt.setTimestamp( Ix, 
					   					new java.sql.Timestamp(d.getTime()) );
					   
				   	} else
				   	{
				   		
					   	throw new ProcessingException( "ERROR: Invalid " +
					   		"column-type property value [" + cd.columnType + 
							"] given in configuration." );
				   	}
		
				   	// Advance to next value slot in prepared statement.
				   	Ix++;
			   	}
			   
		   	}
		   	
		}catch( SQLException sqlException )
		{
			throw new ProcessingException( 
							"ERROR:  could not populate sql statement ."
							+ sqlException.toString() );
	
		}
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){    
	    	Debug.log( Debug.DB_DATA, "Done populating SQL UPDATE statement." );
		}

   	}
   
   	/**
     * Reset the column values in the list.
     */
    private void resetColumnValues ( )
    {
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
          Debug.log (Debug.MSG_STATUS, "Resetting column values ..." );
		}

        Iterator iter = columns.iterator( );
        
		ColumnData cd = null;

        // While columns are available ...
        while ( iter.hasNext() )
        {
            cd = (ColumnData)iter.next( );

            cd.value = null;
        }
    }
    
	/**
	 * Extract data for each column from the input message/context.
	 *
	 * @param context The message context.
	 * @param inputObject The input object.
	 *
	 * @exception MessageException  thrown if required value can't be found.
	 * @exception ProcessingException thrown if any other processing 
	 * 									error occurs.
	 */
	private void extractMessageData ( MessageProcessorContext context, 
									  MessageObject inputObject )
									  throws ProcessingException, 
									  		 MessageException
	{
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log (Debug.MSG_STATUS, "Extracting message data ..." );
		}

		Iterator iter = columns.iterator( );
		
		ColumnData cd = null;

		// While columns are available ...
		while ( iter.hasNext() )
		{
			cd = (ColumnData)iter.next( );

			// If location was given, try to get a value from it.
			if ( StringUtils.hasValue( cd.location ) )
			{
				// Location contains one or more alternative locations that 
				// could contain the column's value.
				StringTokenizer st 
							= new StringTokenizer( cd.location, separator );
							
				String loc = null;
				
				// While location alternatives are available ...
				while ( st.hasMoreTokens() )
				{
					// Extract a location alternative.
					loc = st.nextToken().trim( );

					// If the value of location indicates that the input 
					// message-object's entire value should be used as 
					// the column's value, extract it.
					if ( loc.equalsIgnoreCase( SOAConstants.PROCESSOR_INPUT ) )
					{
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log( Debug.MSG_DATA, "Using message-object's" +
											" contents as the column's value." );
						}

						cd.value = inputObject.get( );

						break;
						
					}

					// Attempt to get the value from the context 
					// or input object.
					if ( exists( loc, context, inputObject ) )
					{
					
						cd.value = get( loc, context, inputObject );
						
					} 

					// If we found a value, we're done with this column.
					if ( !SOAUtility.isNull(cd.value ))
					{
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log( Debug.MSG_DATA, 
									"Found value for column [" + cd.describe() + 
									"] at location [" + loc + "]." );
						}

						break;
						
					}
				}
			}
			
			// If no value was obtained from location in context/input, 
			// try to set it from default value (if available).
			if ( cd.value == null )
			{
			
				cd.value = cd.defaultValue;
				
			}

			// If no value can be obtained ...
			if ( SOAUtility.isNull(cd.value ) )
			{
				// If the value is required ...
				if ( cd.optional == false )
				{
					// Signal error to caller.
					throw new ProcessingException( "ERROR: Could not " +
						"locate required value for column [" + cd.describe()
								+ "], database table [" + tableName + "]." );
								
				} else  // No optional value is available, so putting 'NULL'.
				{
				
					cd.value = null;
					
				} 
			}
		}
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
		  Debug.log (Debug.MSG_STATUS, "Done extracting message data ." );
		}
		
	}

    
    /**
     * Class ColumnData is used to encapsulate a description of a single column
     * and its associated value.
     */
    private static class ColumnData
    {
		//	Property used to store Column Name for a column
        public final String columnName;
        
		//	Property used to store Column Type for a column
        public final String columnType;
        
		//	Property used to store date format 
        public final String dateFormat;
        
		//	Property used to store location for a column's value
        public final String location;
        
        // Property used to store default value
		public final String defaultValue;
        
		//	Property used to store optional flag
        public final boolean optional;
		
		//	Property used to store value for a column
        public Object value = null;

        public ColumnData ( String columnName, String columnType, 
        					String dateFormat, String location,  
        					String defaultValue , String optional ) 
        					throws FrameworkException
        {
            this.columnName   		= columnName;
            this.columnType   		= columnType;
            this.dateFormat   		= dateFormat;
            this.location     		= location;
			this.defaultValue     	= defaultValue;
            this.optional     		= StringUtils.getBoolean( optional );
        }

		/**
		 * This method used to describe a column
		 */
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

            sb.append( "], optional [" );
            sb.append( optional );
            
			if ( StringUtils.hasValue( defaultValue ) )
			{
				sb.append( "], default [" );
				sb.append( defaultValue );
			}

			if ( value != null )
            {
                sb.append( "], value [" );
                sb.append( value );
            }

            sb.append( "]." );

            return( sb.toString() );
        }
    }
    
//	--------------------------For Testing---------------------------------//

  	public static void main(String[] args) {

	  	Properties props = new Properties();
	
	  	props.put( "DEBUG_LOG_LEVELS", "all" );
	
	  	props.put( "LOG_FILE", "e:\\SOAlog.txt" );
	
	  	Debug.showLevels( );
	
	  	Debug.configureFromProperties( props );
	
	  	if (args.length != 3)
	  	{
			Debug.log (Debug.ALL_ERRORS, "SOADBUpdate: USAGE:  "+
			" jdbc:oracle:thin:@192.168.1.240:1521:soa ashok ashok ");
			return;
			
	  	}
	  	try
	  	{

		  	DBInterface.initialize(args[0], args[1], args[2]);
		
	  	}
	  	catch (DatabaseException e)
	  	{
		   	Debug.log(null, Debug.MAPPING_ERROR, ": " +
					"Database initialization failure: " + e.getMessage());
	  	}


		SOADBUpdate soaDBUpdate = new SOADBUpdate();

	  	try
	  	{
			soaDBUpdate.initialize("FULL_NEUSTAR_SOA","SOADBUpdate");
		
		  	MessageProcessorContext mpx = new MessageProcessorContext();
		
		  	MessageObject mob = new MessageObject();			
		
		  	mob.set("LRN",	"2662610000");
		  	
			mob.set("LRNID",	"3979");
		
		  	mob.set("SPID","X039");
		
		  	mob.set("OCDate","11/14/2002 3:24:17 PM");
		
			soaDBUpdate.process(mpx,mob);
		
		  	Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

	  }
	  catch(ProcessingException pex)
	  {
		  System.out.println(pex.getMessage());
	  }
	  catch(MessageException mex)
	  {
		  System.out.println(mex.getMessage());
	  }
	
  } //end of main method
    				
}

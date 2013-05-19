/**
 * This is a generic message-processor for deleting the record(s). All the
 * column to be used in the where clause are specified in the
 * SQL_WHERE_STATEMENT in persistent property.
 * 
 * @author Jigar Talati
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.db.PersistentProperty;
 * @see com.nightfire.framework.db.DBConnectionPool;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.resource.ResourceException;
 * @see com.nightfire.framework.util.DateUtils;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.FrameworkException;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.framework.util.StringUtils;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorBase;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 *  
 */
  
/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Jigar			06/29/2004			Created
	2			Jigar			07/01/2004			Incorporated Review Commnets
	3			Jigar			07/29/2004			Formal review comments incorporated.

 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.DateUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class DBDelete extends MessageProcessorBase 
{
	
	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;
	
	/**
	 * Name of the oracle table requested
	 */
	private String tableName = null;
	
	/**
	 * Name of the oracle columns.
	 */
	private List columns;
	
	public DBDelete ( ) {

		Debug.log( Debug.OBJECT_LIFECYCLE,
							"Creating DBDelete message-processor." );

		columns = new ArrayList( );
	}
	
	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key   String Property-key to use for locating 
	 * 				 initialization properties.
	 * @param  type  String Property-type to use for locating 
	 * 				 initialization properties
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize ( String key, String type )throws ProcessingException
	{
		
		// Call base class method to load the properties.
		super.initialize( key, type );
    
		// Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "DBDelete: Initializing..." );
		}
		
		StringBuffer errorBuffer = new StringBuffer( );
		
		tableName = getRequiredPropertyValue( SOAConstants.TABLE_NAME_PROP, 
															errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Database table to delete to is [" 
															+ tableName + "]." );
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
								+ SOAConstants.TRANSACTIONAL_LOGGING_PROP 
								+ " is invalid. " + e.getMessage ( ) + "\n" );

			}
		}
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, 
					"Logger will participate in overall driver transaction? ["
				   + usingContextConnection + "]." );
		}
				
		sqlWhereStmt = getPropertyValue( SOAConstants.SQL_WHERE_STATEMENT_PROP);
		
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "'where part of' SQL query is [" + 
														sqlWhereStmt + "]." );
		}

		if(sqlWhereStmt!=null)
		{	
			
			populateColumnData();
			
		}
		
		separator = getPropertyValue( SOAConstants.LOCATION_SEPARATOR_PROP );

	    if ( !StringUtils.hasValue( separator ) )
	    {
	    
	       separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
	       
	    }  
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location separator token [" + 
																separator + "]." );
		}

		// If any of the required properties are absent,indicate error to caller
		if ( errorBuffer.length() > 0 )
		{
	
			String errMsg = errorBuffer.toString( );

			Debug.log( Debug.ALL_ERRORS, errMsg );

			throw new ProcessingException( errMsg );
    
		}
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
	  	 Debug.log( Debug.SYSTEM_CONFIG, "DBDelete: Initialization done." );
		}
		
	} // initialize end
	
	/**
	* This method will extract the data values from the input, 
	* perform DB lookups, apply business logic with the input values 
	* and delete the database.
	*
	* @param  context MessageProcessorContext the context
	* @param  object  MessageObject Input message to process.
	*
	* @return  NVPair[] the given input, or null.
	*
	* @exception  ProcessingException  thrown if processing fails.
	* @exception  MessageException  thrown if message is bad.
	*/
   public NVPair[] process ( MessageProcessorContext context, 
							 MessageObject object )
										   throws MessageException,
												  ProcessingException 
   {
	   
	    ThreadMonitor.ThreadInfo tmti = null;
		Connection dbConn = null;
					
		if ( object == null )
		{
		
			return null;
			
		}
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			// Reset the values in cases where we're logging again.
			resetColumnValues( );
			
			if(sqlWhereStmt!=null)
			{	
				// Extract the column values from the arguments.
				extractMessageData( context, object );
			}
			
			try
			{
			
				// Get a database connection from the appropriate location - based
				//  on transaction characteristics.
				if ( usingContextConnection )
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database delete is " +
							"transactional, so getting connection from context." );
					}
	
					dbConn = context.getDBConnection( );
					
				}
				else
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database delete is not " +
							"transactional, so getting connection from pool." );
					}
	
					dbConn = DBConnectionPool.getInstance().acquireConnection( );
					
				}
				
				if(dbConn == null)
				{
					
					throw new ProcessingException("DataBase Connection is failed");
					
				}
				
				// Delete the data from the database.
				deleteRecord( dbConn );	
	
				// If the configuration indicates that this SQL operation 
				// isn't part of the overall driver
				// transaction, commit the changes now.
				if ( !usingContextConnection )
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Committing data " +
														"deleted to database." );
					}
	
					DBConnectionPool.getInstance().commit( dbConn );
					
				}
			}
			catch ( FrameworkException ree )
			{
				
				String errMsg = "ERROR: DBdelete: Attempt to delete to database " +
											"failed with error: " + ree.getMessage();
	
				Debug.log( Debug.ALL_ERRORS, errMsg );
	
				// If the configuration indicates that this SQL operation isn't part
				// of the overall driver transaction, roll back any changes now.
				
				if ( !usingContextConnection )
				{
					
					Debug.log( Debug.MSG_STATUS, "Rolling-back any database " +
												"changes due to database-delete." );
	
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
				ThreadMonitor.stop( tmti );
				// If the configuration indicates that this SQL operation 
				// isn't part of the overall driver 
				//  transaction, return the connection previously acquired 
				// back to the resource pool.
				if ( !usingContextConnection && (dbConn != null) )
				{
					
					try
					{
						
						DBConnectionPool.getInstance().releaseConnection( dbConn );
						
						dbConn = null;
						
					}
					catch ( ResourceException re )
					{
						
						Debug.log( Debug.ALL_ERRORS, re.toString() );
						
					}
				}
			}
		}finally{
			ThreadMonitor.stop( tmti );
		}

	return( formatNVPair( object ) );
		 											
   } // NVPair[] end
   
   /**
	* Extract data for each column from the input message/context.
	*
	* @param context The message context.
	* @param inputObject The input object.
	*
	* @exception MessageException  thrown if required value can't be found.
	* @exception ProcessingException thrown if any other processing error 
	* occurs.
	*/
   private void extractMessageData ( MessageProcessorContext context,
   												 MessageObject inputObject )
   									throws ProcessingException, MessageException
   {
   	   if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	      Debug.log (Debug.MSG_STATUS, "Extracting message data to delete ..." );
	   }

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
			   StringTokenizer st = 
			   					new StringTokenizer( cd.location, separator );
			   					
			   String loc = null;

			   // While location alternatives are available ...
			   while ( st.hasMoreTokens() )
			   {
				
				   // Extract a location alternative.
				   loc = st.nextToken().trim( );

				   // If the value of location indicates that the input 
				   // message-object's entire value should be used as the
				   // column's value, extract it.
				   if ( loc.equalsIgnoreCase( SOAConstants.PROCESSOR_INPUT ) )
				   {
					  if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){ 
					   Debug.log( Debug.MSG_DATA, "Using message-object's " +
					   						"contents as the column's value." );
					  }

					   cd.value = inputObject.get( );

					   break;
					   
				   }

				   // Attempt to get the value from the context or input object.
				   if ( exists( loc, context, inputObject ) )
				   {
				   
					   cd.value = get( loc, context, inputObject );
					   
				   }  

				   // If we found a value, we're done with this column.
				   if ( cd.value != null )
				   {
					   if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						   Debug.log( Debug.MSG_DATA, "Found value for column ["
								 + cd.describe() + "] at location [" + loc + "]." );
					   }

					   break;
				   }
			   }
		   }

		   // If no value can be obtained ...
		   if ( cd.value == null )
		   {
			   
			   // If the value is required ...
			   if ( cd.optional == false )
			   {
				
				   // Signal error to caller.
				   throw new ProcessingException( "ERROR: Could not locate " +
				   	 	"required value for column [" + cd.describe() + "], " +
				   		"database table [" + tableName + "]." );
				   		
			   }
			   else  // No optional value is available, so putting 'NULL'.
			   {
			   
				   cd.value=null;
				   
			   }   
		   }
	   }
       if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
	     Debug.log (Debug.MSG_STATUS, "Done extracting message data to delete." );
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

	   // While columns are available ...
	   while ( iter.hasNext() )
	   {
	   	
		  ColumnData cd = (ColumnData)iter.next( );

		  cd.value = null;
		  
	   }
   }
   
   /**
	* This method populates the list fo columndata
	*
	* @exception ProcessingException when initialization fails
   **/
   private void populateColumnData( ) throws ProcessingException
   {
   	
	   // Loop until all WhereColumn configuration properties have been read ..
	   int count = -1;
	   int index = 0;
	   String location = null;
	   String colType = null;
	   String dateFormat = null;	   
	  
	   //  To find the number of Column values to be filled in WHERE clause.
	   //	this is counted by number of ? in the string
	   while(index != -1)
	   {
		
		   count++;
		   
		   index = sqlWhereStmt.indexOf("?" , index+1);
		   
	   }

	   for ( int Ix = 0;  true;  Ix ++ )
	   {
		   
		   location = getPropertyValue( PersistentProperty.
		   			getPropNameIteration( SOAConstants.WHERE_LOCATION_PREFIX_PROP, Ix ) );
		   
		   // If we can't find default or location then , we're done.
		   if ( !StringUtils.hasValue( location ))
		   {
		   
			   break;
			   
		   }  

		   colType = getPropertyValue( PersistentProperty.
		   			getPropNameIteration( SOAConstants.WHERE_COLUMN_TYPE_PREFIX_PROP, Ix ) );
		   
		   dateFormat = getPropertyValue( PersistentProperty.
		   			getPropNameIteration( SOAConstants.WHERE_DATE_FORMAT_PREFIX_PROP, Ix ) );
		   			
		   try
		   {
		   	
			   // Create a new column data object and add it to the list.
			   ColumnData cd = new ColumnData(  colType, dateFormat, location, 
																	"FALSE" );
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){													
			      Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
				}
			   
			   columns.add( cd );
			   
		   }
		   catch ( FrameworkException e )
		   {
		   	
			   throw new ProcessingException( "ERROR: Could not create column" +
			   							" data description:\n" + e.toString() );
			   							
		   }
	   }

	   int whereColumnsCount = columns.size();
	   
	   if(whereColumnsCount!=count)
	   {
	   
			throw new ProcessingException( "ERROR: Unequal no. of fields in " +
									 "the whereStatement and whereAttributes" );
		
	   }	   
	   if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		   Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of columns in where " +
											"clause [" + whereColumnsCount + "]." );
	   }
	  
   }

   /**
	* Delete the records from the database table using the given connection.
	*
	* @param  dbConn  Connection the database connection to perform 
	* 				  the SQL delete operation against.
	* 
	* @exception  ProcessingException  thrown if processing fails.
	*/
   private void deleteRecord ( Connection dbConn) throws ProcessingException 
   {
		
		PreparedStatement pstmt = null;

		try
		{	
			StringBuffer sb = new StringBuffer();
			
			sb.append( "DELETE FROM " );
			
			sb.append( tableName );
			
			
			if( sqlWhereStmt != null ){
				
				 sb.append(" "); 
				 
				 sb.append( sqlWhereStmt );
				
			}
						
			String sqlStmt = sb.toString();
			
			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){	
			Debug.log( Debug.NORMAL_STATUS, "Executing DELETE SQL:\n"
														 + sqlStmt.toString() );
			}

			// Get a prepared statement for the SQL statement.
			pstmt = dbConn.prepareStatement( sqlStmt.toString() );
			
			// Populate the SQL statement using values obtained from the column 
			// data objects.
			if(sqlWhereStmt !=null)
			{
			
				populateSqlStatement( pstmt );
		   	
			}
			
			boolean flag = pstmt.execute( );
			
			// If the configuration indicates that this SQL operation
			// isn't part of the overall driver
			// transaction, commit the changes now.
			if ( !usingContextConnection )
			{
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log( Debug.MSG_STATUS,
				"Committing data inserted by DBDelete to database." );
				}

				DBConnectionPool.getInstance( true ).commit( dbConn );

			}
            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
			Debug.log( Debug.NORMAL_STATUS, "Successfully deleted [" + flag 
							   		+ "] row into table [" + tableName + "]" );
			}
					   
		}
		catch ( SQLException sqle )
		{

			 // If the configuration indicates that this SQL operation isn't part
			 // of the overall driver transaction, roll back any changes now.
			 if ( !usingContextConnection )
			 {
                 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				 Debug.log( Debug.MSG_STATUS,
				 "Rolling-back any database changes due to DBDelete." );
				 }

				 try
				 {

					 DBConnectionPool.getInstance( true ).rollback( dbConn );

				 }
				 catch( ResourceException re )
				 {

					 Debug.log( Debug.ALL_ERRORS, re.getMessage() );

				 }
			 }

		}
		catch ( FrameworkException e )
		{
		
		   throw new ProcessingException( "ERROR: Could not delete row into " +
						"database table [" + tableName + "]:\n" + e.toString() );
											   
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
							   DBInterface.getSQLErrorMessage( sqle ) );

			   }
		   }
		}
   } // deleteRecord end
   
   /**
   * Populate the SQL DELETE statement from the column data.
   *
   * @param  pstmt  The prepared statement object to populate.
   *
   * @exception ProcessingException  thrown if population fails.
   */
  private void populateSqlStatement ( PreparedStatement pstmt ) 
  												throws ProcessingException
  {
      if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    	  Debug.log( Debug.DB_DATA, "Populating SQL delete statement ..." );
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
			  Debug.log( Debug.DB_DATA, "Populating prepared-statement slot ["
			  					 + Ix + "] with column data ." + cd.describe() );
			  }
			  					 
			  if ( cd.value == null )
			  {
				
				   pstmt.setNull(Ix,java.sql.Types.VARCHAR);
				   
				   Ix++;
				   
				   continue;
				   
			  }
	
			  // Default is no column type specified.
			  if ( !StringUtils.hasValue(cd.columnType) )
			  {
				  
				  pstmt.setObject( Ix, cd.value );
				  
			  }
			  else if ( cd.columnType.equalsIgnoreCase( 
			  								SOAConstants.COLUMN_TYPE_DATE ) )
			  {
			  	
				  String val = (String)(cd.value);
				
				  if ( val.equalsIgnoreCase( SOAConstants.SYSDATE ) )
				  {
				
					  //get current date to set a value for this column
					  pstmt.setTimestamp(Ix,new java.sql.Timestamp(DateUtils.
					  									getCurrentTimeValue()));
				
					  Ix++;
				
					  continue;
				
				  }
				
				  // If we're not updating the current system date, the caller must
				  // have provided an actual date value, which we must now parse.
				  if ( !StringUtils.hasValue( cd.dateFormat ) )
				  {
				  	
					  throw new ProcessingException( "ERROR: Configuration " +
					  			"for date column does not specify date format." );
					  			
				  }
	
				  SimpleDateFormat sdf = new SimpleDateFormat( cd.dateFormat );
	
				  java.util.Date d = sdf.parse( val );
	
				  pstmt.setTimestamp( Ix, new java.sql.Timestamp(d.getTime()) );
				  
			  }
			  else
			  {
				  
				  throw new ProcessingException( "ERROR: Invalid column-type " +
				  				"property value ["+ cd.columnType + "] given in " +
				  												"configuration." );
				  												
			  }
	
			  // Advance to next value slot in prepared statement.
			  Ix ++;
			  
		  }
		  
	  } catch( Exception exception )
	 {
		throw new ProcessingException( 
						"ERROR:  could not populate sql statement ."
						+ exception.toString() );

	 }
	 if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
	  Debug.log( Debug.DB_DATA, "Done populating SQL DELETE statement." );
	 }

  }

	  private String separator = null;
	  
 	  private String sqlWhereStmt = null;
 	  
	  private String originalSQLWhereStmt = null;
	      
	  StringBuffer errorBuffer = null;
	 
	  /**
	   * Class ColumnData is used to encapsulate a description of a single
	   * column and its associated value.
	   */
	  private static class ColumnData
	  {
		  public final String columnType;
		  
		  public final String dateFormat;
		  
		  public final String location;
		  
		  public final boolean optional;

		  public Object value = null;


		  public ColumnData ( String columnType, String dateFormat,
							  String location, String optional ) 
							  					throws FrameworkException
		  {
		  	
			  this.columnType   = columnType;
			  
			  this.dateFormat   = dateFormat;
			  
			  this.location     = location;
			  
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
// --------------------------For Testing---------------------------------//

	 public static void main(String[] args) {

		 Properties props = new Properties();

		 props.put( "DEBUG_LOG_LEVELS", "ALL" );

		 props.put( "LOG_FILE", "E:\\logmap.txt" );

		 Debug.showLevels( );

		 Debug.configureFromProperties( props );

		 if (args.length != 3)
		 {

			  Debug.log (Debug.ALL_ERRORS, "DBDelete: USAGE:  "+
			  " jdbc:oracle:thin:@192.168.1.246:1521:soa jigar talati");

			 return;

		 }
		 try
		 {

			 DBInterface.initialize( args[0], args[1], args[2] );

		 }
		 catch (DatabaseException e)
		 {

			  Debug.log( null, Debug.MAPPING_ERROR, ": " +
					   "Database initialization failure: " + e.getMessage() );

		 }

		 DBDelete dbDlt = new DBDelete();

		 try
		 {
			 dbDlt.initialize("FULL_NEUSTAR_SOA","DBDelete");

			 MessageProcessorContext mpx = new MessageProcessorContext();

			 MessageObject mob = new MessageObject();
			 
			 mob.set("TABLE_NAME","SOA_SUBSCRIPTION_VERSION");
			 
			 mob.set("ONSP","1111");
			 
			 mob.set("NNSP","1111");
			 
			 dbDlt.process(mpx,mob);

			 Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		 }
		 catch(ProcessingException pex)
		 {

			Debug.log(Debug.BENCHMARK, "Error : " + pex.getCause());

		 }
		 catch(MessageException mex)
		 {

			Debug.log(Debug.BENCHMARK, "Error : " + mex.getCause());

		 }
	 } //end of main method
}

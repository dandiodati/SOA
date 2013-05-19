/**
 * The purpose of this processor is to update record(s) into the 
 * specified SOA_SUBSCRIPTION_VERSION table by New NPA for given Old Npa and 
 * NXX. This processor will take Old NPA, New NPA, NXX and database table 
 * name and transaction  logging as input.
 * 
 * @author Jigar Talati
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.db.DBConnectionPool;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.resource.ResourceException;
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
	1			Jigar			06/24/2004			Created
	2			Jigar			06/26/2004			Review Comments 
  													incorporated
	3			Jigar			06/29/2004			Review Comments 
  													incorporated
	4			Jigar			07/29/2004			Formal review comments incorporated.
	5			Manoj k.		01/23/2007			Added the batch update functionality.
	6			Abhijit 		05/03/2007			Added comments.

 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NPAUpdate extends MessageProcessorBase 
{	
							
	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;
	
	/**
	 * Name of the DB table
	 */
	private String tableName = null;
	
	/**
	 * The value of oldnpa
	 */
	private String oldNpaValue = null;
	
	/**
	 * The value of newnpa
	 */
	private String newNpaValue = null;

	/**
	 * The value of nxx
	 */
	private String nxxValue = null;
	
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
		 Debug.log( Debug.SYSTEM_CONFIG, "NpaUpdate: Initializing..." );
		}
		
		StringBuffer errorBuffer = new StringBuffer( );
		
		tableName = getRequiredPropertyValue( SOAConstants.TABLE_NAME_PROP, 
															errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Database table to update to is [" 
															+ tableName + "]." );
		}

		String strTemp = getPropertyValue( SOAConstants.TRANSACTIONAL_LOGGING_PROP );

		// If the TRANSACTIONAL_LOGGING property is configured
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
				
		oldNpaValue = getRequiredPropertyValue( 
						SOAConstants.OLD_NPA_LOC_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of OLDNPA is [" 
															+ oldNpaValue + "]." );
		}
										
		newNpaValue = getRequiredPropertyValue( 
						SOAConstants.NEW_NPA_LOC_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of NEWNPA is [" 
															+ newNpaValue + "]." );
		}

		nxxValue = getRequiredPropertyValue( SOAConstants.NXX_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of NXX is [" 
																+ nxxValue + "]." );
		}
		
		
		// If any of the required properties are absent,indicate error to caller
		if ( errorBuffer.length() > 0 )
		{
	
			String errMsg = errorBuffer.toString( );

			Debug.log( Debug.ALL_ERRORS, errMsg );

			throw new ProcessingException( errMsg );
    
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "NpaUpdate: Initialization done." );
		}
		
	} // initialize end
	
	/**
	* This method will extract the data values from the input, 
	* perform DB lookups, apply business logic with the input values 
	* and update the database.
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
		// If MessageObject is null
		if ( object == null )
		{			
			return null;			
		}
				
		Connection dbConn = null;
			
		PreparedStatement dbStatement = null;
	
		ResultSet rs = null;
	
		String oldTn = null;
	
		String newTn = null;
	
		StringBuffer queryTN = new StringBuffer();
		try
		{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			oldNpaValue = getString( oldNpaValue, context, object );
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Value of Old Npa is [" 
																+ oldNpaValue + "]." );
			}
			
			newNpaValue = getString( newNpaValue, context, object );
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Value of New Npa is [" 
																+ newNpaValue + "]." );
			}
			
			nxxValue = getString( nxxValue, context, object );
	
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Value of Nxx is [" 
																+ nxxValue + "]." );
			}
			
			try
			{
				// Get a database connection from the appropriate location - based 
				// on transaction characteristics.
				if ( usingContextConnection )
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, "Database logging is "
							+ "transactional, so getting connection from context." );
					}
			
					dbConn = context.getDBConnection( );
				}
				else
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, "Database logging is not "
									+ "transactional, so getting connection "
									+ "from NightFire pool." );
					}
			
					dbConn = DBConnectionPool.getInstance( true )
								.acquireConnection( );
				}
				
				// If DB connection is null
				if(dbConn == null)
				{
					
					throw new ResourceException("DataBase Connection is failed");
					
				}
			
			}
			catch ( ResourceException e )
			{
				
				String errMsg = "ERROR: NPAUpdate: Attempt to get database "
								+ "connection failed with error: " 
								+ e.getMessage( );
			
				Debug.log( Debug.ALL_ERRORS, errMsg );   
				
			}
			PreparedStatement updateStmt = null;
	
			// Construct update statement to update the SOA_SUBSCRIPTION_VERSION
			// with New Npa for the Old Npa
			String updatesqlstmt = constructUpdate();
			
			// Construct the select query to get the record form 
			// SOA_SUBSCRIPTION_VERSION table with Old Npa and Nxx
			queryTN.append( "SELECT /*+ index( ");
			
			queryTN.append( tableName );
			
			queryTN.append(" SOA_SV_INDEX_2) */ ");
			
			queryTN.append( SOAConstants.PORTINGTN_COL );
			
			queryTN.append( " FROM " );
			
			queryTN.append( tableName );
			
			queryTN.append( " WHERE PORTINGTN LIKE '"+ oldNpaValue 
			 											+"-"+ nxxValue + "%'"  );
			
			try	
			{	
				// Create update statement object to Update PORTINGTN with New Npa
				updateStmt = dbConn.prepareStatement(updatesqlstmt);
				
				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS, "Executing SELECT SQL:\n"+queryTN.toString());
				// Create select statement object to get Record(s) for the Old Npa
				dbStatement = dbConn.prepareStatement( queryTN.toString( ) );
				
				// Execute the Select query
				rs = dbStatement.executeQuery();
				
				// Until record exists go throufh each record
				while(rs.next())
				{
					// Get the PORTINGTN value
					oldTn = rs.getString( SOAConstants.PORTINGTN_COL );
					
					// Make the New PORTINGTN replacing the Old Npa with New Npa
					newTn = oldTn.replaceAll(oldTn.substring(0, 3), newNpaValue);
				
					// Update SOA_SUBSCRIPTION_VERSION table with New Npa
					updateTn(updateStmt,oldTn, newTn );
					
				}
	
				// Exceute the batch to update records in SOA_SUBSCRIPTION_VERSION
				int count[] = updateStmt.executeBatch();
	            if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
					Debug.log(Debug.DB_STATUS,"Successfully updated ["+ count.length
								+ "] row into table ["+ tableName + "]");
				}
				
				// If the configuration indicates that this SQL operation 
				// isn't part of the overall driver
				// transaction, commit the changes now.
				if ( !usingContextConnection )
				{
	                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, 
						"Committing data updated by NPAUpdate to database." );
					}
					
					try
					{
						
						DBConnectionPool.getInstance( true ).commit( dbConn );
						
					}catch(ResourceException re)
					{
						
						Debug.log( Debug.ALL_ERRORS, re.getMessage() );
						
					}
					
	
				 }	
			}
			catch( SQLException sqlException )
			{
				
				throw new ProcessingException( " Could not select row from DB " 
												+ sqlException.toString() );
			}
			catch ( ProcessingException e )
			{
				String errMsg = "ERROR: NPAUpdate: Attempt to update database "
								+ "failed with error: " + e.getMessage();
	
				Debug.log( Debug.ALL_ERRORS, errMsg );
	
				// If the configuration indicates that this SQL operation isn't part 
				// of the overall driver transaction, roll back any changes now.
				if ( !usingContextConnection )
				{
	                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, 
						"Rolling-back any database changes due to database-logger." );
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
	
				// Re-throw the exception to the driver.			
				throw new ProcessingException( errMsg );								
	
			}
			finally
			{
	
				// Close ResultSet and Statement object
				try
				{
					// If ResultSet object rs is not null
					if (rs != null)
					{
						
						rs.close();
						rs = null;
						
					}
				}
				catch ( SQLException sqle )
				{
	
					Debug.log( Debug.ALL_ERRORS, 
						DBInterface.getSQLErrorMessage(sqle) );
	
				}			
	
				try
				{
					// If Statement object dbStatement is not null
					if (dbStatement != null)
					{
						
						dbStatement.close();
						dbStatement = null;
						
					}
				}
				catch ( SQLException sqle )
				{
	
					Debug.log( Debug.ALL_ERRORS, 
						DBInterface.getSQLErrorMessage(sqle) );
	
				}
	
				try
				{
					// If Statement object updateStmt is not null
					if (updateStmt != null)
					{
						
						updateStmt.close();
						updateStmt = null;
						
					}
				}
				catch ( SQLException sqle )
				{
	
					Debug.log( Debug.ALL_ERRORS, 
						DBInterface.getSQLErrorMessage(sqle) );
	
				}
	
				// If the configuration indicates that this SQL operation isn't 
				// part of the overall driver transaction, return the connection 
				// previously acquired back to the resource pool.
				if ( !usingContextConnection )
				{
					try
					{
	
						DBConnectionPool.getInstance(true)
										.releaseConnection( dbConn );
						dbConn = null;
	
					}
					catch ( ResourceException e )
					{
	
						Debug.log( Debug.ALL_ERRORS, e.toString() );
	
					}
				}
	
			}
		}finally
		{		
			ThreadMonitor.stop(tmti);
		}	
	return( formatNVPair( object ) );
		 											
   } // NVPair[] end
   

   /**
	* Construct the update sql statement which is used to update 
	* Old PORTINGTN with new PORTINGTN into SOA_SUBSCRIPTION_VERSION table.
	*
	*/
   private String constructUpdate () 
   {
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		  Debug.log( Debug.DB_DATA, "Constructing SQL UPDATE statement ..." );
		}

		StringBuffer sqlStmt = new StringBuffer( );

		sqlStmt.append( "UPDATE /*+ index( " );
		
		sqlStmt.append( tableName );
		
		sqlStmt.append(" SOA_SV_INDEX_2) */ ");

		sqlStmt.append( tableName );

		sqlStmt.append( " SET " );	
		
		sqlStmt.append( SOAConstants.PORTINGTN_COL );
		
		sqlStmt.append( " = ? WHERE " );
		
		sqlStmt.append( SOAConstants.PORTINGTN_COL );
		
		sqlStmt.append( " = ?" );
		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
		  Debug.log( Debug.NORMAL_STATUS, "Executing Update SQL:\n" + sqlStmt.toString() );
		}
		
		return sqlStmt.toString();

   }

   /**
	* Update Old PORTINGTN with new PORTINGTN into SOA_SUBSCRIPTION_VERSION 
	* table
	* @param  pstmt  PreparedStatement the prepared statment for batching the 
	*				 multiple update sql statement 
	* @param  oldTn  String the Old PORTINGTN which needs to update. 
	* @param  newTn  String the New PORTINGTN which will be used to replace 
	* Old PORTINGTN.	
	* 
	* @exception  ProcessingException  thrown if processing fails.
	*/
   private void updateTn ( PreparedStatement pstmt, String oldTn, String newTn )
							throws ProcessingException 
   {
	   try
	   {	if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){	
			   Debug.log( Debug.DB_DATA, "Set the New PORTINGTN value" );
	        }
			pstmt.setString( 1, newTn );
		    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			 Debug.log( Debug.DB_DATA, "Set the Old PORTINGTN value" );
			}
			pstmt.setString( 2, oldTn );
		    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			 Debug.log( Debug.DB_DATA, "Add into batch" );
			}
			pstmt.addBatch();
					
	   }
	   catch(SQLException ex){

		   throw new ProcessingException( " Could not able to populate and batch the sql statement. " 
											+ ex.toString() );		
	   
	   }
   } // updateTn end
   
      
// --------------------------For Testing---------------------------------//

	 public static void main(String[] args) {

		 Properties props = new Properties();

		 props.put( "DEBUG_LOG_LEVELS", "ALL" );

		 props.put( "LOG_FILE", "E:\\logmap.txt" );

		 Debug.showLevels( );

		 Debug.configureFromProperties( props );

		 if (args.length != 3)
		 {

			  Debug.log (Debug.ALL_ERRORS, "NPAUpdate: USAGE:  "+
			  " jdbc:oracle:thin:@192.168.1.246:1521:soa jigar jigar");

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

		 NPAUpdate npaUpdate = new NPAUpdate();

		 try
		 {
			npaUpdate.initialize("FULL_NEUSTAR_SOA","NPAUpdate");

			 MessageProcessorContext mpx = new MessageProcessorContext();

			 MessageObject mob = new MessageObject();
			
			 mob.set("OLD_NPA","111");
			 
			 mob.set("NEW_NPA","222");
			 
			 mob.set("NXX","456");
			 
			 mob.set("TABLE_NAME","SOA_SUBSCRIPTION_VERSION");
			 
			 npaUpdate.process(mpx,mob);

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

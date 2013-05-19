/**
 * The purpose of this program is to get the SV Key (s) from the 
 * input XML and insert record(s) per reference  Key in SOA_MESSAGE_MAP table 
 * along with the MessageKey.
 * 
 * @author Ashok Kumar
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.adapter.messageprocessor.DatabaseLogger
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair 
 */

/** 
 Revision History
 ---------------------
 Rev#		Modified By 	Date				Reason
 -----       -----------     ----------			--------------------------
 1			Ashok			05/13/2004			Created
 2			Ashok			05/17/2004			Review comments incorporated
 3			Jigar			06/16/2004			Tn tag has been removed.
 4			Jigar			06/17/2004			Review comments incorporated
 5			Jigar			07/29/2004			Formal review comments incorporated.
 6			Sreedhar	   	03/29/2005		   	Modifyed for Billing requirements. 
 7			Manoj K.		01/23/2007			Modified for batching the sql statements
 8			Manoj K.		01/23/2007			Review comments incorporated.
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.w3c.dom.Document;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
import com.nightfire.framework.util.StringUtils;


public class MapLogger extends DBMessageProcessorBase {

	/**
	 * This variable used to get value for input xml message
	 */
	private String referenceKeyMessage = null;

	/**
	 * This variable used to get value for message key
	 */
	private String messageKey = null;
	
	/**
	 * The value of Message Key value
	 */
	private String msgKeyValue = null;


	/**
	 *  Location of the isRageRequest output
	 */
	
	private String isRangeRequestLocation = null;

	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
    private boolean usingContextConnection = true;

		
	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key   Property-key to use for locating initialization properties.
	 * @param  type  Property-type to use for
	 * locating initialization properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type) throws ProcessingException {
		
		// Call base class method to load the properties.
		super.initialize(key, type);

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		   Debug.log(Debug.SYSTEM_CONFIG, "MapLogger: Initialization Started.");
		}
		
		StringBuffer errorBuffer = new StringBuffer();
		
		referenceKeyMessage = super.getRequiredPropertyValue(
				SOAConstants.INPUT_LOC_REFERENCE_KEY_MESSAGE_PROP, errorBuffer);

		// code to the value of messagekey from the properties file
		messageKey = getRequiredPropertyValue( 
									SOAConstants.MSG_KEY_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
        Debug.log( Debug.SYSTEM_CONFIG, "Value of MESSAGE_KEY is ["
										+ messageKey + "]." );
		}
		
		isRangeRequestLocation = getPropertyValue(
				SOAConstants.IS_RANGE_REQUEST_OUT_LOC_PROP);
		
		// If required property absent,
		// indicate error to caller.
		if (errorBuffer.length() > 0) {
			
			String errMsg = errorBuffer.toString();
			
			Debug.log(Debug.ALL_ERRORS, errMsg);
			
			throw new ProcessingException(errMsg);
		}

		String strTemp = getPropertyValue( 
        							SOAConstants.TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {

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
		 Debug.log(Debug.SYSTEM_CONFIG, "MapLogger: Initialization done.");
	   }
		
	}
	
	/**
	 * This method will parse the Input XML to extract ReferenceKey values 
	 * and insert record(s) in SOA_MESSAGE_MAP table 
	 * along with corresponding MessageKey. 
	 *
	 * @param  context The context
	 * @param  message  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(MessageProcessorContext context,
			MessageObject message) throws MessageException, ProcessingException
			{
		ThreadMonitor.ThreadInfo tmti = null;
		
		String isRangeRequest = "false";
		
		if (message == null) {
			
			return null;
			
		}
        try{
			msgKeyValue = (String) super.get(messageKey, context, message);
	
			if (super.exists(referenceKeyMessage, context, message)) {
				
				// Get the Document for input XML
				Document doc = super.getDOM(referenceKeyMessage, context, message);
				
				// get the length of child nodes i.e. count key nodes 
				int childNodesLen = doc.getElementsByTagName(SOAConstants.KEY_NODE)
				.getLength();
				
				if (childNodesLen > 1) {
					
					isRangeRequest = "true";
					
				}
				
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log(Debug.SYSTEM_CONFIG, "MapLogger: No of key nodes :"
							+ childNodesLen);
				}
				 
				//declared the connection variable
				Connection dbConn = null;
	
				PreparedStatement insertStatement = null;
	
				try {
					tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
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
	
					if (dbConn == null)
					{
						// Throw the exception to the driver.
						throw new ProcessingException( "DB connection is not available" );
					}
					
				} catch (FrameworkException e) {
					String errMsg = "ERROR: MappLogger:"
						+ " Attempt to get database connection"
						+ " failed with error: " + e.getMessage();
					
					Debug.log(Debug.ALL_ERRORS, errMsg);
					
					// Re-throw the exception to the driver.
					if (e instanceof MessageException) {
						
						throw (MessageException) e;
						
					} else {
						
						throw new ProcessingException(errMsg);
						
					}
				}
				
				try{
					//getting the preparedStatement  
					insertStatement = dbConn.prepareStatement(SOAQueryConstants.INSERT_SOA_MAP_LOG); 
	
					for (int i = 0; i < childNodesLen; i++) {
						
						// extract Reference Key from XML file
						String key = (String) super.get(referenceKeyMessage + "."
							+ SOAConstants.KEYCONTAINER_NODE + "."
								+ SOAConstants.KEY_NODE + "(" + i + ")", context,
								message);
						
						if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							
							Debug
							.log(Debug.MSG_STATUS, "Reference  Key: [" + key
									+ "]");
							
						}
						
						//populating the preparedStatement
						insertStatement.setLong(1,Long.parseLong(msgKeyValue));
	
						//setting the value for the referencekey
						insertStatement.setLong(2,Long.parseLong(key));
						
						
						//add it to the batch
						insertStatement.addBatch();					
											
					}
					//executing the batch statement to update the batch sql statements.
					insertStatement.executeBatch();
	
					if ( !usingContextConnection )
					{
	                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							Debug.log( Debug.MSG_STATUS,
							"Committing data inserted by MapLogger to database." );
						}
	
						try
						{
	
						   DBConnectionPool.getInstance( true ).commit( dbConn );
	
						}
						catch( ResourceException re )
						{
	
							Debug.log( Debug.ALL_ERRORS, re.getMessage() );
	
						}
	
					}
	
				}catch(Exception e){
						String errMsg = "ERROR: MappLogger: Attempt to insert the datat in database "
					+ "failed with error: " + e.getMessage();
				
					Debug.log(Debug.ALL_ERRORS, errMsg);
				
				}finally {
					ThreadMonitor.stop(tmti);
					try {
						if (insertStatement != null) {
							insertStatement.close();
							
							insertStatement = null;
						}
					} catch (SQLException sqle) {
						
						Debug.log(Debug.ALL_ERRORS, DBInterface
								.getSQLErrorMessage(sqle));
						
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
					
			}
			
			if (isRangeRequestLocation != null) {
				super.set(isRangeRequestLocation, context, message, isRangeRequest);
			}
        }finally{
        	ThreadMonitor.stop(tmti);
        }
		return (formatNVPair(message));
	}
	
	//--------------------------For Testing---------------------------------//
	
	public static void main(String[] args) {
		
		Properties props = new Properties();
		
		props.put("DEBUG_LOG_LEVELS", "all");
		
		props.put("LOG_FILE", "e:\\SOAlog.txt");
		
		Debug.showLevels();
		
		Debug.configureFromProperties(props);
		
		if (args.length != 3) {
			Debug.log(Debug.ALL_ERRORS, "MapLogger: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.246:1521:soa prasanthi " +
							"prasanthi ");
			return;
		}
		try {
			
			DBInterface.initialize(args[0], args[1], args[2]);
			
		} catch (DatabaseException e) {
			Debug.log(null, Debug.MAPPING_ERROR, ": "
					+ "Database initialization failure: " + e.getMessage());
		}
		
		MapLogger mapLogger = new MapLogger();
		
		try {
			
			mapLogger.initialize("FULL_NEUSTAR_SOA", "MapLogger");
			
			MessageProcessorContext mpx = new MessageProcessorContext();
			
			MessageObject mob = new MessageObject();
			
			mob.set("KEY", "<ReferenceKey><keycontainer><key value=\"123\" />"
					+ "<key value=\"345\" /> " + " </keycontainer> "
					+ " </ReferenceKey>");
			
			mob.set("KEY1", "<ReferenceKey><keycontainer><key value=\"123\" />"
					+
					//"<key value=\"345\" /> " +
					" </keycontainer> " + " </ReferenceKey>");
			
			mob.set("SPID", "1234");
			
			mob.set("MESSAGEKEY", "1000");
			mob.set("ISRANGEREQUEST", "abc");
			
			mapLogger.process(mpx, mob);
			
			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());
			
		} catch (ProcessingException pex) {
			System.out.println(pex.getMessage());
		} catch (MessageException mex) {
			System.out.println(mex.getMessage());
		}
		
	} //end of main method	
	
}
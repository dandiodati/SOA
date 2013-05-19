/**
 * The purpose of this program is to log Synchronous Response .
 * 
 * @author Abhijit Talukdar
 * @version 1.0
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
 1			Abhijit			03/07/2007			Created 
 2			Peeyush 		05/15/2007			Modified for Subdomain Requirement.
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.db.SQLBuilder;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.spi.common.driver.Converter;

public class SVSyncMsgLogger extends DBMessageProcessorBase {

	/**
	 * This variable used to get the value of request id xml message
	 */
	private String requestIdMessage = null;
	
	/**
	 * This variable used to get the value of request id 
	 */
	private String requestId = null;

	/**
	 * This variable used to get value of SPID
	 */
	private String spid = null;	
	
	/**
	 * This variable used to get value of Region Id
	 */
	private String regionId = null;

	/**
	 * This variable used to get value of Customer Id
	 */
	private String action = null;

	/**
	 * Get the  The  errorMessage
	 */
	private String errorMessage = null;	

	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;
	
	//changes made in 5.6.2 release
	//variables for initial message id generated in SOA INITIAL REQUEST LOGGER Message processor.
	private static final String INITIAL_MESSAGE_ID = "INITIAL_MESSAGE_ID";
	private String initMessageID_loc = null;
	private String initMessageID = null;
	
	//changes made in 5.6.4 release for TD#10281
	//location of PortingTn gets from context, in case of Normal NR.
	private static final String PORTING_TN = "PORTING_TN";
	private String porting_tn_loc = null;
	private String porting_tn = null;
	
	
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
		 Debug.log(Debug.SYSTEM_CONFIG, "SVSyncMsgLogger: Initialization Started.");
		}
		
		StringBuffer errorBuffer = new StringBuffer();
		
		requestIdMessage = super.getPropertyValue(
				SOAConstants.INPUT_LOC_REQUEST_ID_MESSAGE_PROP );

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log( Debug.MSG_STATUS, "Checking requestIdMessage for value..." + requestIdMessage );
		}
		
		requestId = super.getPropertyValue(
				SOAConstants.REQUEST_ID_PROP);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log( Debug.MSG_STATUS, "Checking requestId for value..." + requestId );
		}
		
		spid = super.getPropertyValue( 
				SOAConstants.SPID_PROP);

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log( Debug.MSG_STATUS, "Checking spid for value..." + spid );
		}
		
		regionId = super.getPropertyValue( SOAConstants.REGION_ID_PROP );

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log( Debug.MSG_STATUS, "Checking regionId for value..." + regionId );
		}
			
		action = super.getRequiredPropertyValue(
				SOAConstants.ACTION_LOC,errorBuffer );

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log( Debug.MSG_STATUS, "Checking action for value..." + action );
		}

		errorMessage = getRequiredPropertyValue( 
					SOAConstants.INPUT_LOC_ERROR_MESSAGE_PROP,errorBuffer );

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log( Debug.MSG_STATUS, "Checking errorMessage for value..." + errorMessage );
		}
		
		//changes made in 5.6.2 release
		//	get the location of initial messgaeId form configuration property.
		initMessageID_loc = getPropertyValue(INITIAL_MESSAGE_ID);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, "Value of INITIAL_MESSAGE_ID:["
					+ initMessageID_loc + "].");
		}
		
		// changes made in 5.6.4 release, TD#10281 
		// gets the location of PortingTn from context, in case of Normal NR.
		porting_tn_loc = getPropertyValue(PORTING_TN);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, "Value of porting_tn_loc:["
					+ porting_tn_loc + "].");
		}
		
		// If required property absent,
		// indicate error to caller.
		if (errorBuffer.length() > 0) {
			
			String errMsg = errorBuffer.toString();
			
			Debug.log(Debug.ALL_ERRORS, errMsg);
			
			throw new ProcessingException(errMsg);
		}
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log(Debug.SYSTEM_CONFIG, "SVSyncMsgLogger: Initialization done.");
		}
		
	}
	
	/**
	 * This method will insert record(s) in SOA_SV_MESSAGE table 
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
			if (message == null) {
			
				return null;
			
			}

			mpContext = context;
			inputObject = message;
			
			String requestIdValue = null;

			String spidValue = null;

			String regionIdValue =  null;

			String syncResStatusValue = null;

			String customerIdValue = null;

			String actionValue = null;

			String requestIdMessageValue = null;
			
			String apiFlag = null;
			
			if (requestId != null)
			{
				requestIdValue = getValue(requestId);
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log( Debug.MSG_STATUS, "requestId  value..." + requestIdValue );
			}
		
			String messageTypeValue = "Response";
		
			String messageSubTypeValue = "SynchronousResponse";
					

			if ( exists( requestIdMessage , context, message ) ){
			
			 requestIdMessageValue = getString( requestIdMessage, context, message );
				
			}
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log( Debug.MSG_STATUS, "Checking requestIdMessageValue for value..." + requestIdMessageValue );
			}
			
			if (spid != null)
			{
				spidValue = getValue(spid);
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log( Debug.MSG_STATUS, "Checking spidValue for value..." + spidValue );
			}
		
			if (regionId != null)
			{
				regionIdValue = getValue(regionId);
			}
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log( Debug.MSG_STATUS, "Checking regionIdValue for value..." + regionIdValue );
			}

			if (action != null)
			{
				actionValue = getValue(action);
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log( Debug.MSG_STATUS, "Checking actionValue for value..." + actionValue );
			}

			String errorValue = null;

			if ( exists( errorMessage, context, message ) ){
			
				errorValue = getString( errorMessage, context, message );
				
			}
			
			apiFlag = getValue(SOAConstants.API_FLAG);
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log( Debug.MSG_STATUS, "InputSource Value for Request..." + apiFlag );
			}
			
			//changes made in 5.6.2 release
			//get the initial Message ID value from context
			if ( exists( initMessageID_loc, mpContext, inputObject ) ){
				initMessageID = getString(initMessageID_loc, mpContext, inputObject);
			}else{
				initMessageID = null;
			}
			
			//changes made in 5.6.4 release, TD#10281
			//get the Porting Tn value from context
			if ( exists( porting_tn_loc, mpContext, inputObject ) ){
				porting_tn = getString(porting_tn_loc, mpContext, inputObject);
			}else{
				porting_tn = null;
			}
			
					
			//	declared the connection variable
			Connection dbConn = null;

			PreparedStatement insertStatement = null;

			try {
					// Get a database connection from the appropriate
					//location - based
					// on transaction characteristics.								
				dbConn = DBConnectionPool.getInstance( true )
				.acquireConnection( );
                                if (dbConn == null) {
						// Throw the exception to the driver.
						throw new ProcessingException("DB "
								+ "connection is not available");
					}
			
			} catch (FrameworkException e) {
				String errMsg = "ERROR: SVSyncMsgLogger:"
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
		
			try {
				tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
				
				XMLMessageGenerator successReply = null;

				XMLMessageGenerator failureReply = null;

				SimpleDateFormat format = 
				new SimpleDateFormat( SOAConstants.DATE_FORMAT );

				String dateTimeSentValue = format.format( new Date() );	

				String dateTimeSent = getString( dateTimeSentValue );

				//getting the preparedStatement  
				insertStatement = dbConn.prepareStatement(SOAQueryConstants.SOA_SV_MESSAGE_SYNC_INSERT);
				
				// Set Message Type value in prepared statement
				insertStatement.setString(2, SOAConstants.RESPONSE);
		
				// Set Message Sub Type value in prepared statement
				insertStatement.setString(3, SOAConstants.SYNCHRONOUS_RESPONSE);
				
				// Set Customer Id value in prepared statement	
				SQLBuilder.populateCustomerID( insertStatement, 4 );
				
				//Changes made for 5.6.4 TD#10186
				//Set SPID value in prepared statement
				if(spidValue!=null && spidValue.length()==4)
				{
					insertStatement.setString(5, spidValue);	
				}
				else
				{
					insertStatement.setNull(5, java.sql.Types.VARCHAR);
				}
				// End TD##10186
				
				// Set Status value in prepared statement
				insertStatement.setString(6, SOAConstants.SENT_STATUS );
				
				// Set Created By value in prepared statement
				SQLBuilder.populateUserID( insertStatement, 7 );
				
				// Set Date time value in prepared statement
				insertStatement.setTimestamp( 9,
						new java.sql.Timestamp( new java.util.Date().getTime() ) );
		
				// Set Interface Version value in prepared statement
				SQLBuilder.populateInterfaceVersion( insertStatement, 10 );
		
				// Set User Id value in prepared statement
				SQLBuilder.populateUserID( insertStatement, 11 );
				
				// Set Synchronus Response Message value in prepared statement
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				 Debug.log( Debug.MSG_STATUS, "Before  SOAUtility for value..."  );
				}
	
				if(!SOAUtility.isNull( requestIdValue ) )
				{
					//***Getting PortingTN node value from XML if present***					
					Document xpgDoc = null;					
					Element element = null;
					NodeList nodeList = null;
					String tnValue = null;
								
					XMLPlainGenerator xpgObj = new XMLPlainGenerator (getString(inputObject.getDOM()));
					
					xpgDoc = xpgObj.getDocument();
					
					element = xpgDoc.getDocumentElement();

					nodeList = element.getElementsByTagName("Tn");
					
					Element temp = (Element)nodeList.item(0);
					
					if(temp != null){
						
						tnValue = temp.getAttribute("value");
						Debug.log(Debug.MSG_STATUS, "The value of <Tn> node is +" + tnValue.toString());
					}					
					//***End***
					
					// changes made in 5.6.2 release
					// logging initialMessageId in MULTITNSEQUENCEID column.
					if(initMessageID!=null)
					{
						insertStatement.setString(17, initMessageID);
					}
					else
					{
						insertStatement.setNull(17, java.sql.Types.INTEGER );
					}
					
					// Set Message Key value in prepared statement
					insertStatement.setLong(1,Long.parseLong( requestIdValue ));
				
					if(errorValue == null )
					{
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log( Debug.MSG_STATUS, "after  SOAUtility for value...syncResStatusValue1"+syncResStatusValue  );
						}
						
						//Set Porting TN value as null in prepared statement
						insertStatement.setNull(8, java.sql.Types.VARCHAR );
						
						// Set Region Id value in prepared statement
						if (regionIdValue != null )
						{
							insertStatement.setString( 14, regionIdValue );
						}
						else {
							insertStatement.setNull( 14, java.sql.Types.VARCHAR);
						}							

						successReply = SOAUtility.successSyncResponse(requestIdValue, SOAConstants.SUCCESS, actionValue, regionIdValue, dateTimeSent, tnValue);

						insertStatement.setString( 12, successReply.generate());
					
					}else
					{
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log( Debug.MSG_STATUS, "after  SOAUtility for value...syncResStatusValue2"+syncResStatusValue  );
						}
						//release 5.6.4, changes nade for TD#10281.
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				    			Debug.log(Debug.MSG_STATUS, "SVSyncMsgLogger, portingTN : "
				    					+ porting_tn);
						}
						if(porting_tn!=null && porting_tn.length() == 12 )
						{
							porting_tn = porting_tn + "-" + porting_tn.substring(8, 12);
							insertStatement.setString(8, porting_tn);
						}
						else if(porting_tn!=null && porting_tn.length() == 17 )
						{
							insertStatement.setString(8, porting_tn);
						}
						else
						{
							insertStatement.setNull(8, java.sql.Types.VARCHAR );
						}
						// end Td#10281
						// Set region Id value as null in prepared statement
						insertStatement.setNull(14,java.sql.Types.VARCHAR);

						//generate the failure reply

						failureReply = SOAUtility.failPFSupported(errorValue, requestIdValue, SOAConstants.FAILURE, actionValue, regionIdValue, dateTimeSent, tnValue );
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log( Debug.MSG_STATUS, "Using DBLOBUtils.setCLOB" );
						}

						DBLOBUtils.setCLOB( insertStatement, 12, failureReply.generate());		
						
					}					
			
					// Set Invoke Id value in prepared statement
					insertStatement.setLong(13,Long.parseLong( requestIdValue ));

					//Set the Subdomain Value in prepared statement
					insertStatement.setString(15,populateSubDomainValue());
					
					//Set the InputSource Value in prepared statement
					insertStatement.setString(16, apiFlag);
					
					insertStatement.execute();
					dbConn.commit();
					
		
		
				}else if ( super.exists(requestIdMessage, context, message) ) {
		
					int successReqTNNodesLen= 0;
					int failureReqTNNodesLen= 0;						
											
					// Get the Document for input XML
					XMLMessageParser domMsg = new XMLMessageParser(requestIdMessageValue);				

					if (domMsg.exists("SuccessRequest"))
					{
						successReqTNNodesLen = domMsg.getChildCount("SuccessRequest");
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log( Debug.MSG_STATUS, "Child Node count SuccessRequest"+domMsg.getChildCount("SuccessRequest")  );
						}
					}
					if (domMsg.exists("FailureRequest"))
					{
						failureReqTNNodesLen = domMsg.getChildCount("FailureRequest");
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log( Debug.MSG_STATUS, "Child Node count FailureRequest"+domMsg.getChildCount("FailureRequest")  );
						}
					}						

					syncResStatusValue = SOAConstants.SUCCESS;
			
					boolean firstSuccess = true;
		
					for (int i = 0; i < successReqTNNodesLen; i++) {												

						// extract Request Id from XML file
						String reqId = (String) super.get(requestIdMessage + "."
								+ "SuccessRequest.RequestIdTN"
								+ "(" + i + ").RequestId", context,message);
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 Debug.log( Debug.MSG_STATUS, "reqId  "+reqId  );
						}
						//	extract Porting TN from XML file
						
						String portingTn = (String) super.get(requestIdMessage + "."
							+ "SuccessRequest.RequestIdTN"
							+ "(" + i + ").RequestTn", context,message);
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){	
						  Debug.log( Debug.MSG_STATUS, "portingTn  "+portingTn  );
						}
					
						// Set Message Key value in prepared statement
						insertStatement.setLong(1,Long.parseLong(reqId));
					
						//Chnages for 5.6.4 TD##10281
						 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				    			Debug.log(Debug.MSG_STATUS, "SVSyncMsgLogger, portingTN : "
				    					+ portingTn);
						 }
						if(portingTn!=null && portingTn.length() == 12 )
						{
							portingTn = portingTn + "-" + portingTn.substring(8, 12);
							insertStatement.setString(8, portingTn);
						}
						else if(portingTn!=null && portingTn.length() == 17 )
						{
							insertStatement.setString(8, portingTn);
						}
						else
						{
							insertStatement.setNull(8, java.sql.Types.VARCHAR );
						}
						// end  TD#10281
											
						// Set Invoke Id value in prepared statement
						insertStatement.setLong(13,Long.parseLong( reqId ));
					
						// Set Region Id value in prepared statement
						insertStatement.setString(14,regionIdValue);
						
						//Set the Subdomain Value in prepared statment
						insertStatement.setString(15,populateSubDomainValue());
						
						//Set the InputSource Value in prepared statement
						insertStatement.setString(16, apiFlag);
						
						// changes made in 5.6.2 release
						// logging initialMessageId in MULTITNSEQUENCEID column.
						if(initMessageID!=null)
						{
							insertStatement.setString(17, initMessageID);
						}
						else
						{
							insertStatement.setNull(17, java.sql.Types.INTEGER );
						}
						//generate the failure reply
						
						if (firstSuccess)
						{
							successReply = SOAUtility.successSyncResponse(reqId, syncResStatusValue, actionValue, regionIdValue, dateTimeSent, portingTn);
							firstSuccess = false;

						}
						else {
							//successReply.setValue( SOAConstants.REQUEST_ID_PATH, reqId );
							successReply.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.Tn", portingTn);
							successReply.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.RequestId", reqId);
						}
						

						insertStatement.setString( 12, successReply.generate());
						
						//Set the Subdomain Value in prepared statment
						insertStatement.setString(15,populateSubDomainValue());				
						
				
						//insert the record in DB
						insertStatement.execute();
						dbConn.commit();
									
					}
									
					boolean firstFailure = true;

					syncResStatusValue = SOAConstants.FAILURE;

					for (int i = 0; i < failureReqTNNodesLen; i++) {
				
						 //Node ruleIdNode = ruleIdList.item(msgCount);

						//ruleId = XMLMessageBase.getNodeValue(ruleIdNode);
						
						//	extract Request Id from XML file
						String reqId = (String) super.get(requestIdMessage + "."
								+ "FailureRequest.RequestIdTN"
								+ "(" + i + ").RequestId", context,message);
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 Debug.log( Debug.MSG_STATUS, "failure reqId  "+reqId  );
						}
						//	extract Porting TN from XML file
						String portingTn = (String) super.get(requestIdMessage + "."
								+ "FailureRequest.RequestIdTN"
								+ "(" + i + ").RequestTn", context,message);
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 Debug.log( Debug.MSG_STATUS, "failure portingTn  "+portingTn  );
						}
						// Set Message Key value in prepared statement
						insertStatement.setLong(1,Long.parseLong(reqId));
				
						// Set Porting TN value in prepared statement
						//	Changes for 5.6.4 TD#10281
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
						{
				    			Debug.log(Debug.MSG_STATUS, "SVSyncMsgLogger, portingTN : "
				    					+ portingTn);
				    	        }
						if(portingTn!=null && portingTn.length() == 12 )
						{
							portingTn = portingTn + "-" + portingTn.substring(8, 12);
							insertStatement.setString(8, portingTn);
						}
						else if(portingTn!=null && portingTn.length() == 17 )
						{
							insertStatement.setString(8, portingTn);
						}
						else
						{
							insertStatement.setNull(8, java.sql.Types.VARCHAR );
						}
						// end TD#10281
				
						// Set Invoke Id value in prepared statement
						insertStatement.setLong(13,Long.parseLong( reqId ));
					
						// Set Region Id value as null in prepared statement
						insertStatement.setNull(14,java.sql.Types.VARCHAR);

						if (firstFailure)
						{
							failureReply = SOAUtility.failPFSupported(errorValue, reqId, syncResStatusValue, actionValue, regionIdValue, dateTimeSent, portingTn);
							firstFailure = false;

						}
						else {
							//failureReply.setValue( SOAConstants.REQUEST_ID_PATH, reqId );
							failureReply.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.Tn", portingTn);
							failureReply.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.RequestId", reqId);
						}
				
						DBLOBUtils.setCLOB( insertStatement, 12, failureReply.generate());	
						
						//Set the Subdomain Value in prepared statment
						insertStatement.setString(15,populateSubDomainValue());
						
						//Set the InputSource Value in prepared statement
						insertStatement.setString(16, apiFlag);
						
						// changes made in 5.6.2 release
						// logging initialMessageId in MULTITNSEQUENCEID column.
						if(initMessageID!=null)
						{
							insertStatement.setString(17, initMessageID);
						}
						else
						{
							insertStatement.setNull(17, java.sql.Types.INTEGER );
						}
						
						//insert the record in DB
						insertStatement.execute();
						dbConn.commit();
									
					}
		
				}
			
			}catch(Exception e){
			
				String errMsg = "ERROR: SVSyncMsgLogger: Attempt to insert the datat in database "
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
					try
					{
	
					   if (dbConn != null)
					   {
							DBConnectionPool.getInstance(true)
									.releaseConnection( dbConn );
							dbConn = null;
							if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							 Debug.log( Debug.MSG_STATUS, "Releasing connection  ");
							}
					   }
					}
					catch( ResourceException re )
					{
	
						Debug.log( Debug.ALL_ERRORS, re.getMessage() );
	
					}
			}
	
			return (formatNVPair(message));
	}

	/**
     * This method tokenizes the input string and return an
     * object for exsisting value in context or messageobject.
     *
     * @param  locations as a string
     *
     * @return  object
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.

     */
  
	private String getValue ( String locations )
                                throws MessageException, ProcessingException
    {
        StringTokenizer st = new StringTokenizer( locations,
								DBMessageProcessorBase.SEPARATOR );

		String tok = null;

        while ( st.hasMoreTokens() )
        {
            tok = st.nextToken( );
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log( Debug.MSG_STATUS, "Checking location ["
									  + tok + "] for value..." );
			}

            if ( exists( tok, mpContext, inputObject ) )
			{
                return( (String) get( tok, mpContext, inputObject ) );
			}
        }

        return null;
	}
	
	/**
	 * This method return the SUBDOMAIN Value for synchronous reponse.
	 * @return
	 */
	public String populateSubDomainValue()
	{
		String subdomain = "";
		
		try
		{
			if( CustomerContext.getInstance().get("subdomain_in_svmsg") != null )
			{
				subdomain = (String)CustomerContext.getInstance().get("subdomain_in_svmsg");
			}			
		}
		catch(Exception e)
		{
			Debug.log(Debug.MSG_ERROR, " Error occured in populateSubDomainValue(). "+ e);
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS, " populateSubDomainValue().SUBDOMAIN VALUE ="+ subdomain);
		}
		
		return subdomain;
	}
	//--------------------------For Testing---------------------------------//
	
	public static void main(String[] args) {
		
		Properties props = new Properties();
		
		props.put("DEBUG_LOG_LEVELS", "all");
		
		props.put("LOG_FILE", "e:\\SOAlog.txt");
		
		Debug.showLevels();
		
		Debug.configureFromProperties(props);
		
		if (args.length != 3) {
			Debug.log(Debug.ALL_ERRORS, "SVSyncMsgLogger: USAGE:  "
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
			
			mapLogger.initialize("FULL_NEUSTAR_SOA", "SVSyncMsgLogger");
			
			MessageProcessorContext mpx = new MessageProcessorContext();
			
			MessageObject mob = new MessageObject();
			
			mob.set("REQUEST_ID_MESSAGE", "<ReferenceKey><keycontainer><key value=\"123\" />"
					+ "<key value=\"345\" /> " + " </keycontainer> "
					+ " </ReferenceKey>");
			
			mob.set("REQUEST_ID", "11111");
			
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
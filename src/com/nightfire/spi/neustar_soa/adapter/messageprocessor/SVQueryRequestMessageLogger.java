package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentSequence;
import com.nightfire.framework.db.SQLBuilder;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.NancPropException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class SVQueryRequestMessageLogger extends DBMessageProcessorBase {

	/**
	 * This variable contains MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;

	/**
	 * This variable contains MessageObject object
	 */
	private MessageObject inputObject = null;

	/**
	 * The value of SPID
	 */
	private String spidValue = null;

	/**
	 * Input message location
	 */
	private String inputMessage = null;

	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;		

	/**
	 * To indicate whether the request is coming from GUI or API.
	 */
	private String apiFlag = null;
	
	/**
	 * This variable used to get the value of Telephone Number
	 */
	private String tnNode = null;
	
	/**
	 * This variable used to get the value of RegionId
	 */
	private String regionIdNode = null;
	
	private String requestTypeProp = null;

	private String requestType = null;

	private String requestBodyNode = null;

	ArrayList<String> messageList = new ArrayList<String>();
	
	ArrayList<String> passTNList = new ArrayList<String>();
	
	ArrayList<String> failedTNList = new ArrayList<String>();

	PreparedStatement svMessageInsert = null;
	
	PreparedStatement insertMessageQueueStmt = null;
	
	PreparedStatement mapInsertStmt = null;

	private String tnEndNode;
	
	private String syncKeyOut;

	private String compFailFlag;

	private String completeFail = "false";

	XMLMessageGenerator message = null;
	
	String msgKey = null;
	
	String referenceKeyValue = null;
	
	/**
	 * This variable used to get the value of Reference Key
	 */
	private String referenceKeyNode = null;
	
	//changes made in 5.6.2 release
	//variables for initial message id generated in SOA INITIAL REQUEST LOGGER Message processor.
	private static final String INITIAL_MESSAGE_ID = "INITIAL_MESSAGE_ID";
	private String initMessageID_loc = null;
	private String initMessageID = null;

	/**
     * Initializes this object via its persistent properties.
     *
     * @param key Property-key to use for locating initialization properties.
     * @param type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
	public void initialize(String key, String type) throws ProcessingException {

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, " In SVQueryRequestMessageLogger initialize() method.");
		}
		
		super.initialize(key, type);

		// this is used to store error messages.
		StringBuffer errorBuffer = new StringBuffer();
		
		// get the transactional logging from configuration property.
		String strTemp = getPropertyValue(SOAConstants.TRANSACTIONAL_LOGGING_PROP);		

		if (StringUtils.hasValue(strTemp)) {
			try {
				usingContextConnection = getBoolean(strTemp);
			} catch (FrameworkException e) {
				errorBuffer.append("Property value for "
						+ SOAConstants.TRANSACTIONAL_LOGGING_PROP
						+ " is invalid. " + e.getMessage() + "\n");
			}
		}			
	
		// get the SPID form configuration property.
		spidValue = getRequiredPropertyValue(SOAConstants.SPID_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "Value of SPID:[" + spidValue + "].");
		}

		// get the Message from configuration property.
		inputMessage = getRequiredPropertyValue(
				SOAConstants.INPUT_XML_MESSAGE_LOC_PROP, errorBuffer);

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "Value of inputMessage:["+ inputMessage + "].");
		}

		// get the Request Type from configuration property.
		requestTypeProp = getRequiredPropertyValue(
				SOAConstants.REQUEST_TYPE_PROP, errorBuffer);
		
		//	get the Synchronous key from configuration property.
		syncKeyOut = getPropertyValue(SOAConstants.SYNC_KEY_LOC_PROP);

		// get the Complete Fail Flag from configuration property.
		compFailFlag = getPropertyValue(SOAConstants.COMPLETEFAIL_LOC_PROP);
		
		//changes made in 5.6.2 release
		//	get the location of initial messgaeId form configuration property.
		initMessageID_loc = getPropertyValue(INITIAL_MESSAGE_ID);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, "Value of INITIAL_MESSAGE_ID:["
					+ initMessageID_loc + "].");
		}
		
		// If any of required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG,"SVQueryRequestMessageLogger: Initialization done.");
		}

	}

	/**
	 * Extract data values from the context/input, and use them to
	 * insert/update a row(s) into the configured database table.
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	
	public NVPair[] process(MessageProcessorContext mpContext,
			MessageObject inputObject) throws MessageException,
			ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		
		if (inputObject == null) {
			return null;
		}

		this.mpContext = mpContext;

		this.inputObject = inputObject;

		Connection dbConn = null;
		
		try {
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS, " In SVQueryRequestMessageLogger process() method. ");
			}
			
			// Get a database connection from the appropriate location - based
			// on transaction characteristics.
			if (usingContextConnection) {

				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS, "Database logging is "
						+ "transactional, so getting connection from context.");
				}

				dbConn = mpContext.getDBConnection();
			} else {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, "Database logging is not "
							+ "transactional, so getting connection "
							+ "from NightFire pool.");
				}

				dbConn = DBConnectionPool.getInstance(true).acquireConnection();
			}
			if (dbConn == null) {
				// Throw the exception to the driver.
				throw new ProcessingException("DB connection is not available");
			}
		} 
		catch (FrameworkException e) 
		{
			String errMsg = "ERROR: SVQueryRequestMessageLogger: Attempt to get database "
					+ "connection failed with error: " + e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// Re-throw the exception to the driver.
			if (e instanceof MessageException) {
				throw (MessageException) e;
			}
		}

		// if requestType Property is not null get the value from context.
		if (requestTypeProp != null) {
			if (exists(requestTypeProp, mpContext, inputObject)) {
				requestType = ((String) get(requestTypeProp, mpContext,
						inputObject));
			}
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,	"requestTYpe ="+ requestType);
		}
		
		//changes made in 5.6.2 release
		//get the initial Message ID value from context
		if ( exists( initMessageID_loc, mpContext, inputObject ) ){
			
			initMessageID = getString(initMessageID_loc, mpContext, inputObject);
		}else{
			initMessageID = null;
		}
		
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, "Value of initial Message ID in Context : "+ initMessageID);
		}
		
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
		
		// Create an DOM for the request
		Document doc = getDOM(inputMessage, mpContext, inputObject);

		// get the SPID value from context.
		spidValue = getValue(spidValue);

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,	"Value of spid:"+ spidValue + "].");
		}

		//get the API Flag from context.
		apiFlag = getValue(SOAConstants.API_FLAG);
		
		String errorStatus = getValue(SOAConstants.BR_ERROR_STATUS);

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "Value of BR Error Status:[" + errorStatus + "].");
		}

		XMLMessageParser inputParser = new XMLMessageParser(doc);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		Debug.log(Debug.MSG_STATUS, "The Document request : "
				+ inputParser.getMessage());
		}

		String telephoneNumbers = null;		

		requestBodyNode = SOAConstants.REQUEST_BODY_PATH + "." + requestType
				+ ".QueryNPAC" + ".";	
				
		tnNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".Sv" + ".Tn";	
		
		referenceKeyNode = SOAConstants.REQUEST_BODY_PATH + "." + requestType + ".QueryNPAC" + ".ReferenceKey"; 
		
		inputParser.compressWhitespace(false);	
		
		String tnCoalescingFlagValue = null;

		try {

			tnCoalescingFlagValue = NANCSupportFlag.getInstance(spidValue.toString()).getTnCoalescingFlag();

		} catch (NancPropException exception) {

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				  Debug.log(Debug.SYSTEM_CONFIG, ": Could not retreive TN Coalescing Flag.");
				}
		}
		
		// If the request is for Single Tn or for Non-Contiguous TN
		if (inputParser.exists(tnNode)) {
			telephoneNumbers = inputParser.getValue(tnNode);
			if(telephoneNumbers.contains(SOAConstants.PORTINGTN_SEPARATOR))
			{
				StringTokenizer st = new StringTokenizer(telephoneNumbers, SOAConstants.PORTINGTN_SEPARATOR);
				while(st.hasMoreTokens())
				{
					String individualTN = st.nextToken();
					if(errorStatus != null && errorStatus.equals("Data Error"))
					{
						if( tnCoalescingFlagValue != null && !tnCoalescingFlagValue.equals("") && tnCoalescingFlagValue.equals("1"))
						{
							failedTNList = collapseConsecutiveTNSubRange(getTNlist(getIndividualTNList(telephoneNumbers)));
						}
						else
						{
							failedTNList.add(individualTN);
						}
					}
					else
					{
						if( tnCoalescingFlagValue != null && !tnCoalescingFlagValue.equals("") && tnCoalescingFlagValue.equals("1"))
						{
							passTNList = collapseConsecutiveTNSubRange(getTNlist(getIndividualTNList(telephoneNumbers)));
						}
						else
						{
							passTNList.add(individualTN);
						}
					}
				}
			}
			else
			{
				if(errorStatus != null && errorStatus.equals("Data Error"))
				{
					failedTNList.add(telephoneNumbers);
				}
				else
				{
					passTNList.add(telephoneNumbers);
				}
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS,	"Value of TN:"+ telephoneNumbers + "].");
			}
		}
		else {
			String tnStartNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".TnRange.Tn";
			tnEndNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".TnRange.EndStation";
			tnStartNode = inputParser.getValue(tnStartNode);
			tnEndNode = inputParser.getValue(tnEndNode);
			telephoneNumbers = tnStartNode + "-" + tnEndNode;
			
			if(errorStatus != null && errorStatus.equals("Data Error"))
			{
				failedTNList.add(telephoneNumbers);
			}
			else
			{
				passTNList.add(telephoneNumbers);
			}
		}

		
		try {	
			
			// INSERT into SOA_SV_MESSAGE table.
			getSvMessageInsert(passTNList, dbConn, inputParser,
						requestType);
			
			
			// INSERT into SOA_REQUEST_QUEUE table.
			insertMessageQueue(dbConn);

			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS,	"Value of ReferenceKey:"+ inputParser.exists(referenceKeyNode) + "].");
			}
			
			if (inputParser.exists(referenceKeyNode)) {
				referenceKeyValue = inputParser.getValue(referenceKeyNode);
				
				getSvMapInsert(dbConn);
			}

			//commit the transaction.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS,
						"Committing data inserted by SVQueryRequestMessageLogger to database.");
				}
				try {
					DBConnectionPool.getInstance(true).commit(dbConn);
				} catch (ResourceException re) {
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
				}
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS, "Output Msg : " + message.getDocument());
			}

			//	set the Synchronous key in context
			super.set(syncKeyOut, mpContext,inputObject, message.getDocument() );

			//set the Complete failure flag in context
			if(passTNList == null || passTNList.isEmpty() || passTNList.get(0) == null){
				completeFail = "true";
			}
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS, "CompleteFail Flag: " + completeFail);
			}
			super.set(compFailFlag, mpContext,inputObject, completeFail );

			// commit the transaction for API requests and complete failed requests.
			if ( !(apiFlag != null && apiFlag.equals("G") ))
			{
				if(failedTNList != null && !failedTNList.isEmpty() && failedTNList.get(0) != null)
				{
					dbConn.commit();

                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS, "Committed in SVQueryRequestMessageLogger");
					}
				}
			}
			else if (completeFail.equals("true"))
			{
				dbConn.commit();

                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				  Debug.log(Debug.MSG_STATUS, "Committed in SVQueryRequestMessageLogger for complete fail");
				}
			}

		} catch (SQLException sqlex) {
			sqlex.printStackTrace();
			String errMsg = "ERROR: SVQueryRequestMessageLogger: Attempt to log to database "
					+ "failed with error: " + sqlex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {

				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS,
						"Rolling-back any database changes due to SVQueryRequestMessageLogger.");
				}
				try {
					DBConnectionPool.getInstance(true).rollback(dbConn);
				} catch (ResourceException re) {
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
				}
			}
		} catch (FrameworkException fe) {

			// If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS,
							"Rolling-back any database changes due to SVQueryRequestMessageLogger.");
				}
				try {
					DBConnectionPool.getInstance(true).rollback(dbConn);
				} catch (ResourceException re) {
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
				}
			}
		} finally {
			try {
				// if svMessageInsert is not closed
				if (svMessageInsert != null) {
					// close svMessageInsert statement
					svMessageInsert.close();
					svMessageInsert = null;
				}		
				
				// if insertMessageQueueStmt is not closed
				if (insertMessageQueueStmt != null) {
					// close insertMessageQueueStmt statement
					insertMessageQueueStmt.close();
					insertMessageQueueStmt = null;		            
				
				}
				
				if(mapInsertStmt != null)
				{
					mapInsertStmt.close();
					mapInsertStmt = null;
				}
			} catch (SQLException sqle) {
				Debug.log(Debug.ALL_ERRORS, DBInterface
						.getSQLErrorMessage(sqle));
			}

			// If the configuration indicates that this SQL operation isn't
			// part of the overall driver transaction, return the connection
			// previously acquired back to the resource pool.
			if (!usingContextConnection && (dbConn != null)) {
				try {
					DBConnectionPool.getInstance(true)
							.releaseConnection(dbConn);
					dbConn = null;
				} catch (ResourceException e) {
					Debug.log(Debug.ALL_ERRORS, e.toString());
				}
			}
		}
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		return (formatNVPair(inputObject));
	}

	
	/**
	 * This method will insert record into the SOA_REQUEST_QUEUE table
	 * for the success TN
	 * 
	 * @param dbConn
	 * @throws SQLException
	 * @throws FrameworkException
	 */
	private void insertMessageQueue(Connection dbConn) throws SQLException,
			FrameworkException {
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,	" In SVQueryRequestMessageLogger.insertMessageQueue() method.");
		}
		
		insertMessageQueueStmt = dbConn
				.prepareStatement(SOAConstants.SOA_SV_REQUEST_QUEUE_INSERT);

		//Iterate through each input xml message or the successful TNs.
		for (int i = 0; i < messageList.size(); i++) {
			insertMessageQueueStmt.setString(1, Integer.toString(PersistentSequence
					.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_QUEUE_ID, dbConn )) );
			insertMessageQueueStmt.setString(2, SOAConstants.SOA_REQUEST_MESSAGE_TYPE);
			Date datetime = new Date();
			insertMessageQueueStmt.setTimestamp(3, new Timestamp(datetime
					.getTime()));
			insertMessageQueueStmt.setString(4, SOAConstants.SOA_ERROR_COUNT);
			insertMessageQueueStmt.setString(5, SOAConstants.SOA_ERROR_COUNT);
			insertMessageQueueStmt.setString(6, SOAConstants.NPAC_QUEUE_STATUS);
			insertMessageQueueStmt.setString(7, messageList.get(i).toString());
			insertMessageQueueStmt.setString(8, spidValue);
			insertMessageQueueStmt.addBatch();
		}
		insertMessageQueueStmt.executeBatch();
		
		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
			  Debug.log(Debug.NORMAL_STATUS,	"Insert Query for SOA_REQUEST_QUEUE:\n "+SOAConstants.SOA_SV_REQUEST_QUEUE_INSERT);
			}
		
		if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
			Debug.log(Debug.DB_STATUS, "The SV Message(s) has " +
											"been inserted into the SOA_REQUEST_QUEUE Table.");
		}
	}

	/**
	 * This method will insert record into SOA_SV_MESSAGE table
	 *	 
	 * @param passTNList
	 * @param dbConn
	 * @param inputParser
	 * @param requestType
	 * @param telephoneNumbers
	 * 
	 * @throws SQLException
	 * @throws FrameworkException
	 */
	private void getSvMessageInsert(ArrayList<String> passTNList, Connection dbConn,
			XMLMessageParser inputParser, String requestType) throws SQLException, FrameworkException {

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,	" In SVQueryRequestMessageLogger.getSvMessageInsert() method.");
		}
		
		String portingTN = "";		
		
		regionIdNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".Sv" + ".RegionId";			
		
		String rangeNode = requestBodyNode + "Subscription.TnRange";
		
		String startTnNode = requestBodyNode + "Subscription.TnRange.Tn";

		String endStationNode = requestBodyNode + "Subscription.TnRange.EndStation";
		
		String svNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".Sv";
		
		svMessageInsert = dbConn.prepareStatement(SOAConstants.SOA_SV_MESSAGE_DATA_INSERT_QUERY);		
		
		String subdomain = "";
		
		subdomain = CustomerContext.getInstance().getSubDomainId();
		  
		if( !(subdomain != null && !(subdomain.equals(""))) )
		{
			if(CustomerContext.getInstance().get("subdomain") != null)
				subdomain = (String)CustomerContext.getInstance().get("subdomain");
		}
		  
		CustomerContext.getInstance().set("subdomain_in_svmsg" , subdomain );

		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		  Debug.log(Debug.NORMAL_STATUS,	"Insert Query for SV Message: "+SOAConstants.SOA_SV_MESSAGE_DATA_INSERT_QUERY);
		}
		
		
		boolean passTnFlag = false;

		ArrayList<String> totTns = new ArrayList<String>();

		//If the failed TN list having values.
		if(failedTNList != null && !failedTNList.isEmpty())
		{			
			if (failedTNList.get(0) != null)
			{
				totTns.addAll(failedTNList);
			}
		}		

		//If the success TN list having values.
		if(passTNList != null && !passTNList.isEmpty())
		{			
			if (passTNList.get(0) != null)
			{
				totTns.addAll(passTNList);
			}
		}
		
		message = new XMLMessageGenerator("SuccessFailureRequestId");

		int passId = 0;
		int failedId = 0;
		
		//loop over all the success Telephone Numbers.
		for (int i = 0; i < totTns.size(); i++) {
			String startTn = null;
			String tn = null;
			String endStation = null;
			boolean isRange = false;			

			// Get the messagekey value from sequence
			msgKey = Integer.toString(PersistentSequence
				.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_MSG_KEY, dbConn ));			
			
			
			tn = (String)totTns.get(i);
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS,	"Value of tn:"+ tn + "].");
			}
			
			svMessageInsert.setString(1, msgKey.trim());
			svMessageInsert.setString(13, msgKey.trim());
			svMessageInsert.setString(2, SOAConstants.REQUEST);
			svMessageInsert.setString(3, requestType);
			SQLBuilder.populateCustomerID( svMessageInsert, 4 );
			svMessageInsert.setString(5, spidValue);
			SQLBuilder.populateUserID(svMessageInsert, 7 );
			svMessageInsert.setString(10, "1_0");
			SQLBuilder.populateUserID(svMessageInsert, 11 );
			svMessageInsert.setString(14, subdomain);
			// insert value for inputsource column in message table
			svMessageInsert.setString(16, apiFlag);
			
			if(inputParser.exists(regionIdNode))
			{
				svMessageInsert.setInt(15, Integer.parseInt(inputParser.getValue(regionIdNode)));
			}
			else
			{
				svMessageInsert.setNull(15, java.sql.Types.INTEGER );
			}
			
			//changes made in 5.6.2 release
			//insert the initial MessageID
			if(initMessageID!=null)
			{
				svMessageInsert.setString(17, initMessageID);
			}
			else
			{
				svMessageInsert.setNull(17, java.sql.Types.INTEGER );
			}
			
			//If input tn exist in success TN list.
			if (passTNList.contains(tn)) {
				message.setValue("SuccessRequest.RequestIdTN(" + passId +").RequestId",msgKey);
				message.setValue("SuccessRequest.RequestIdTN(" + passId +").RequestTn",tn);				
				svMessageInsert.setString(6, SOAConstants.SOA_QUEUED_STATUS);
				passTnFlag = true;
				passId++;
			}else if (failedTNList.contains(tn)) {
				message.setValue("FailureRequest.RequestIdTN(" + failedId +").RequestId",msgKey);
				message.setValue("FailureRequest.RequestIdTN(" + failedId +").RequestTn",tn);				
				svMessageInsert.setString(6, SOAConstants.SOA_DATAERROR_STATUS);
				passTnFlag = false;
				failedId++;
			}				 		
			
			// if it's a range Tn get the end TN values.
			if (tn.length() > 12) {
				isRange = true;
				startTn = tn.substring(0, 12);
				if (tn.length() > 13) {
					endStation = tn.substring(13);
				} else {
					endStation = "";
				}
			}			

			portingTN = tn;
			
			Node parentNode = inputParser.getNode("UpstreamToSOA");
			
			// remove TN node from input message.
			if (inputParser.exists(tnNode)) {
				inputParser.removeNode(tnNode);
			}	
			// remove StartTn node from input message.
			if (inputParser.exists(startTnNode)) {
				inputParser.removeNode(startTnNode);
			}

			// remove EndStation node from input message.
			if (inputParser.exists(endStationNode)) {
				inputParser.removeNode(endStationNode);
			}

			// remove Range node from input message.
			if (inputParser.exists(rangeNode)) {
				inputParser.removeNode(rangeNode);
			}	
			
			// remove Sv node from input message.
			if (inputParser.exists(svNode)) {
				inputParser.removeNode(svNode);
			}			
			// Generate the individual request for each subrange

			Node[] children = XMLMessageBase.getChildNodes(parentNode);

			XMLMessageGenerator generator = new XMLMessageGenerator(
					"SOAMessage");
			generator.setValue("UpstreamToSOA." + children[0].getNodeName(),
					children[0]);
			generator.setValue("UpstreamToSOA." + children[1].getNodeName(),
					children[1]);	
			// If range then add Tn and EndStation node
			if (isRange) {				
				generator.setValue(startTnNode, startTn);
				generator.setValue(endStationNode, endStation);				
			} 
			// Add Tn node
			else {
				
				generator.setValue(tnNode, tn);				
			}						
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, "The separated request : "
					+ generator.getMessage());
			}
			
			// If Single TN, make 17 digit TN
			if (!isRange) {
				if( portingTN.length() >= 12 )
				{
					portingTN = portingTN + "-" + portingTN.substring(8, 12);
				}
				}
			svMessageInsert.setString(8, portingTN);
			Date datetime = new Date();
			svMessageInsert.setTimestamp(9, new Timestamp(datetime.getTime()));
			
			svMessageInsert.setString(12, generator.getXMLDocumentAsString());
			generator.setValue("UpstreamToSOA.UpstreamToSOAHeader.InvokeID", msgKey.trim());

			// For success TN, add into messageList
			if (passTnFlag)
			{
				messageList.add(generator.getXMLDocumentAsString());
			}
			
			svMessageInsert.addBatch();
		}
		if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
		 Debug.log(Debug.DB_STATUS, "The Output message:"+  message.getMessage());
		}
		svMessageInsert.executeBatch();
		if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
			Debug.log(Debug.DB_STATUS, "The SV Message(s)  has been " +
					"inserted into the SOA_SV_MESSAGE Table.");
		}
	}	
	
	/**
	 * This method tokenizes the input string and return an object for existing
	 * value in context or messageobject.
	 * 
	 * @param locations as a string
	 * @return object
	 * @exception ProcessingException Thrown if processing fails.
	 * @exception MessageException Thrown if message is bad.
	 */
	private String getValue(String locations) throws MessageException,
			ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);

		String tok = null;

		while (st.hasMoreTokens()) {
			tok = st.nextToken();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, "Checking location [" + tok
					+ "] for value...");
			}

			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		return null;
	}	
	
	
	/**
	 * This method will INSERT record into SOA_MESSAGE_MAP
	 * 
	 * @param dbConn
	 * @throws SQLException is thrown when an operation is failed in SQL.
	 * @throws ProcessingException is thrown when any error is occurred while
	 *         processing the formateDate data.
	 * @throws FrameworkException.
	 */
	private void getSvMapInsert(Connection dbConn) throws SQLException,
			ProcessingException, FrameworkException {
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,	" In SVQueryRequestMessageLogger.getSvMapInsert() method.");
		}

		mapInsertStmt = dbConn.prepareStatement(SOAConstants.SOA_MSG_MAP);	
		
		String subdomain = CustomerContext.getInstance().getSubDomainId();		
		  
		if( !(subdomain != null && !(subdomain.equals(""))) )
		{
			if(CustomerContext.getInstance().get("subdomain") != null)
			 subdomain = (String)CustomerContext.getInstance().get("subdomain");
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, "Value of MessageKey:[" + msgKey + "].");
			Debug.log(Debug.MSG_STATUS, "Value of ReferenceKey:[" + referenceKeyValue + "].");
		}
		mapInsertStmt.setString( 1, msgKey );
		mapInsertStmt.setString( 2, referenceKeyValue );					
					
		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Insert query for SOA_MESSAGE_MAP:\n" + SOAConstants.SOA_MSG_MAP);
		//Execute mapInsertStmt statement to insert into Message Map table.
		mapInsertStmt.execute();
			
	

	}
	
	public ArrayList<String> getTNlist(ArrayList<String> submittedTnList)
	{	
		
		ArrayList<String> tnList = new ArrayList<String>();
		
		for (int i = 0; i < submittedTnList.size(); i++) {
			
			// if TN list contains TN Range
			if (submittedTnList.get(i).toString().length() > 12) {
				int startTn = Integer.parseInt(submittedTnList.get(i).toString()
						.substring(8, 12));
				int endTN = Integer.parseInt(submittedTnList.get(i).toString()
						.substring(13, 17));
				for (int l = startTn; l <= endTN; l++) {
					StringBuffer tn = new StringBuffer(submittedTnList.get(i)
							.toString().substring(0, 8));

					tn.append(StringUtils.padNumber(l,
							SOAConstants.TN_LINE, true, '0'));

					tnList.add(tn.toString());
					
				}
			} 
			// if TN list contains Single TN
			else {
				tnList.add(submittedTnList.get(i).toString());
			}
		}
		return tnList;
	}
	
	public ArrayList<String> getIndividualTNList(String telephoneNumbers)
	{
		
		ArrayList<String> portedTN = new ArrayList<String>();
		try
		{
			StringTokenizer st = new StringTokenizer(telephoneNumbers, SOAConstants.PORTINGTN_SEPARATOR);
			while(st.hasMoreTokens())
			{
				String individualTN = st.nextToken();
				portedTN.add(individualTN);
			}
		}
		catch( Exception err)
		{
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS) ){
				Debug.log(Debug.ALL_ERRORS, "Error in getting individual TN [" + err.getMessage()
						+ "]");

		}
		}
		return portedTN;
	}
	
	
	public ArrayList<String> collapseConsecutiveTNSubRange(ArrayList<String> tnList) {

		if (tnList == null) {
			return tnList;
		}

		Collections.sort(tnList);

		ArrayList<String> subRange = new ArrayList<String>();

		Iterator<String> iter = tnList.iterator();

		String previousNpaNxx = null;

		String currentNpaNxx = null;

		String endStation = null;

		String previousEndStation = null;

		String startTN = null;		

		int previousEndTN = -1;

		int i = 1;

		while (iter.hasNext()) {
			String tn = (String) iter.next();

			Debug.log(Debug.MSG_STATUS, i + "tn " + tn);

			currentNpaNxx = tn.substring(0, 7);

			if (previousNpaNxx != null) {

				endStation = tn.substring(8);

				Debug.log(Debug.MSG_STATUS, i + "endStation " + endStation);

				try {
					int endTN = Integer.parseInt(endStation);

					previousEndTN = Integer.parseInt(previousEndStation);

					if (currentNpaNxx.equals(previousNpaNxx)) {

						if (previousEndTN != -1 && endTN == (previousEndTN + 1)) {
							previousEndTN = endTN;
						} else {

							if (startTN.substring(8).equals(Integer.valueOf(previousEndTN).toString())) {
								subRange.add(startTN);
							} else {
								subRange.add(startTN + "-" + StringUtils.padNumber(previousEndTN, SOAConstants.TN_LINE, true, '0'));
							}
							
							previousEndTN = -1;
							previousNpaNxx = null;

							startTN = tn;

						}
					} else {

						if (startTN.substring(8).equals(Integer.valueOf(previousEndTN).toString())) {
							subRange.add(startTN);
						} else {
							subRange.add(startTN + "-" + StringUtils.padNumber(previousEndTN, SOAConstants.TN_LINE, true, '0'));
						}

						previousEndTN = -1;
						previousNpaNxx = null;

						startTN = tn;

					}

				} catch (NumberFormatException nfe) {
				}
			} else {
				startTN = tn;
			}

			previousNpaNxx = currentNpaNxx;

			previousEndStation = tn.substring(8);

			i++;

		}

		if (previousEndTN != -1) {
			subRange.add(startTN + "-" + StringUtils.padNumber(previousEndTN, SOAConstants.TN_LINE, true, '0'));
		} else {
			subRange.add(startTN);
		}

		return subRange;

	}
	

	
	//	--------------------------For Testing---------------------------------//
	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "D:\\logmap.txt");		

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug
					.log(
							Debug.ALL_ERRORS,
							"SVQueryRequestMessageLogger: USAGE:  "
									+ " jdbc:oracle:thin:@192.168.198.42:1521:vinsoa vinsoa vinsoa ");

			return;

		}
		try {

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {

			Debug.log(Debug.MAPPING_ERROR, ": "
					+ "Database initialization failure: " + e.getMessage());

		}

		SVQueryRequestMessageLogger svQueryRequestMessageLogger = new SVQueryRequestMessageLogger();

		try {
			svQueryRequestMessageLogger.initialize("SOA_VALIDATE_REQUEST","SVQueryRequestMessageLogger");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("spid", "1111");
			mob.set("useCustomerId", "PAT_TST");
			mob.set("userId", "example");
			mob.set("subrequest", "SvQueryRequest");
			mob.set("passTN","305-580-9945");
			mob.set("failedTN",	"");
			
			svQueryRequestMessageLogger.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		}

	}
}
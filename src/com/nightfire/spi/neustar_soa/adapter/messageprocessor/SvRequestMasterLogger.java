/**
 * This custom message processor will be part of the Request Driver chain and 
 * the responsibilities of this component are as follows.
 * 
 *  a)	Break the single Sv request of TN Range or list of non-contiguous TNs 
 * 		into individual requests. If there is a true range but due to data 
 * 		validation broken into the sub ranges then the request message will be 
 * 		prepared for each sub range. 
 *	b)	Construct the SQL batch for INSERT statement and execute against the 
 *		SOA_SV_MESSAGE table. 
 *  c)	Construct the SQL batch for INSERT/UPDATE statement based on the existence 
 * 		of the record in the SOA_SUBSCRIPTION_VERION table and execute the same 
 * 		against SOA_SUBSCRIPTOIN_VERSION. 
 *	d)	Construct the SQL batch for INSERT statement based on message key and
 *		reference key value retrieved from point # b & c and execute against 
 *		the SOA_MESSAGE_MAP table.
 *	e)	Construct the SQL batch for INSERT statement and execute against the 
 *		SOA_REQUEST_QUEUE table.
 *	f)	Construct the SQL batch for INSERT statement and execute against the 
 *		SOA_PENDING_REQUEST table.
 *	g)	Construct the SQL batch for INSERT statement and execute against the 
 *		SOA_SV_RANGE table.
 *	h)	Construct the SQL batch for INSERT statement and execute against the 
 *		SOA_SV_RANGE_MAP table.
 *
 * 
 * @Copyright (c) 2006-07 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is
 * considered to be confidential and proprietary to NeuStar.
 * 
 */

/**

	Revision History
	---------------
	
	Rev#	Modified By			Date			Reason
	----- ----------- ---------- --------------------------

	1		Peeyush M		05/22/2007			Modification for Subdomain Requirement Changess
	
	2		Peeyush M		06/25/2007			Modification for Store request in SOA_PENDING_REQUEST, in status='active'
	
	3		Peeyush M		06/27/2007			Modification to fix the TD #6369 and 6380.
	
	4		Peeyush M		06/28/2007			Modfication to fix the TD #6429.
	
	5		Peeyush M		07/06/2007			Modification to fix the TD #6357.

	6		Peeyush M		10/08/2007			Modification to fix the TD #6556.	
*/

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



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
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.RegexUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.rules.SOACustomDBFunctions.CreateTnSubdomainData;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class SvRequestMasterLogger extends DBMessageProcessorBase {

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
	 * To indicate whether the request is comming from GUI or API.
	 */
	private String apiFlag = null;

	/**
	 * This variable used to get the value of subrequest Type
	 */
	private String subRequestType = null;

	/**
	 * This variable used to get the value of Telephone Number
	 */
	private String tnNode = null;
	
	/**
	 * This variable used to get the value of Reference Key
	 */
	private String refkeyNode = null;
	
	/**
	 * This variable used to get the value of Reference Key
	 */
	private String refKeyValues = null;	
	
	/**
	 * This variable used to get the value of Rangeid
	 */
	private String rangeidNode = null;
	
	/**
	 * This variable used to get the value of Rangeid
	 */
	private String rangeIdValues = null;
	

	/*
	 * Private Static members
	 */
	private static int GATEWAY_BASE = Debug.USER_BASE;

	private static int GATEWAY_LIFECYCLE = GATEWAY_BASE;

	private String requestTypeProp = null;

	private String requestType = null;

	private String requestBodyNode = null;

	ArrayList requestDataList = new ArrayList();

	ArrayList requestNodeList = new ArrayList();

	ArrayList requestDtList = new ArrayList();

	ArrayList requestNdDtList = new ArrayList();

	ArrayList indivisualTNList = new ArrayList();

	ArrayList refKeyList = new ArrayList();

	ArrayList successKeyList = new ArrayList();
		
	ArrayList tnList = new ArrayList();

	ArrayList statusList = new ArrayList();
	
	ArrayList nnspList = new ArrayList();
	
	ArrayList onspList = new ArrayList();
	
	ArrayList nnspDueDateList = new ArrayList();
	
	ArrayList onspDueDateList = new ArrayList();

	ArrayList tnKeyList = new ArrayList();

	ArrayList pendingColumnList = new ArrayList();

	ArrayList pendingTnList = new ArrayList();

	ArrayList messageList = new ArrayList();
	
	ArrayList passTNList = new ArrayList();
	
	ArrayList failedTNList = new ArrayList();
	
	PreparedStatement svMessageInsert = null;

	Statement svObjectSelect = null;

	PreparedStatement svPendingInsertStmt = null;

	PreparedStatement svObjectUpdateStmt = null;

	PreparedStatement svObjectInsertStmt = null;

	PreparedStatement insertMessageQueueStmt = null;

	PreparedStatement mapInsertStmt = null;
	
	PreparedStatement svRangeStmt = null;
	
	PreparedStatement svRangeMapStmt = null;
	
	PreparedStatement svRangeIdMapStmt = null;
	
	PreparedStatement updateLastColumnStmt = null;

	HashMap tnMsgMap = new HashMap();
	
	HashMap tnRangeMap = new HashMap();

	//Added for inserting rangeid in object table
	HashMap tnRangeMap2 = new HashMap();
	
	HashMap tnRefMap = new HashMap();

	private String tnEndNode;
	
	private String syncKeyOut;

	private String compFailFlag;

	private String completeFail = "false";

	XMLMessageGenerator message = null;
	
	public static final String LRN = "LRN";
	
	public static final String CLASSDPC =  "CLASSDPC";
	
	public static final String CLASSSSN = "CLASSSSN";
	
	public static final String CNAMDPC = "CNAMDPC";
	
	public static final String CNAMSSN = "CNAMSSN";
	
	public static final String ISVMDPC = "ISVMDPC";
	
	public static final String ISVMSSN = "ISVMSSN";
	
	public static final String LIDBDPC = "LIDBDPC";
	
	public static final String LIDBSSN = "LIDBSSN";
	
	public static final String WSMSCDPC = "WSMSCDPC";
	
	public static final String WSMSCSSN = "WSMSCSSN";
	
	public static final String BILLINGID = "BILLINGID";
	
	public static final String ENDUSERLOCATIONTYPE = "ENDUSERLOCATIONTYPE";
	
	public static final String ENDUSERLOCATIONVALUE = "ENDUSERLOCATIONVALUE";
	
	public static final String ALTBILLINGID = "ALTBILLINGID";
	
	public static final String ALTENDUSERLOCATIONTYPE = "ALTENDUSERLOCATIONTYPE";
	
	public static final String ALTENDUSERLOCATIONVALUE = "ALTENDUSERLOCATIONVALUE";
	
	public static final String CAUSECODE = "CAUSECODE";
	
	public static final String CUSTOMERDISCONNECTDATE = "CUSTOMERDISCONNECTDATE";
	
	public static final String EFFECTIVERELEASEDATE = "EFFECTIVERELEASEDATE";
	
	public static final String SvType = "SvType";
	
	public static final String AlternativeSPID = "AlternativeSPID";
	
	public static final String LastAlternativeSPID = "LastAlternativeSPID";
	
	public static final String NNSPDUEDATE = "NNSPDUEDATE";
	
	public static final String VoiceURI = "VoiceURI";
	
	public static final String MMSURI = "MMSURI";
	
	public static final String PoCURI = "PoCURI";
	
	public static final String PRESURI = "PRESURI";
	
	public static final String SMSURI = "SMSURI";
	
	public static final String PORTINGTOORIGINAL = "PORTINGTOORIGINAL";
	
	// changes made in 5.6.2 release
	// variables for initial message id generated in SOA INITIAL REQUEST LOGGER Message processor.
	private static final String INITIAL_MESSAGE_ID = "INITIAL_MESSAGE_ID";
	private String initMessageID_loc = null;
	private String initMessageID = null;
	
        //changes made in 5.6.4 TD#8478
	public static final String LNPTYPE = "LNPTYPE";
	public static final String NNSP = "NNSP";
	public static final String ONSP = "ONSP";
	public static final String ONSPDUEDATE = "ONSPDUEDATE";
	public static final String AUTHORIZATIONFLAG = "AUTHORIZATIONFLAG";
	private ArrayList svUpdateColumnList = new ArrayList();

	//Changes are made for 5.6.5 release (NANAC441 req)
	public static final String NNSP_SIMPLEPORTINDICATOR = "NNSPSIMPLEPORTINDICATOR";
	public static final String ONSP_SIMPLEPORTINDICATOR = "ONSPSIMPLEPORTINDICATOR";
	// end 5.6.5 changes
	
	//SOA 5.6.8 changes
	public static final String SPCUSTOM1 = "SPCUSTOM1";
	public static final String SPCUSTOM2 = "SPCUSTOM2";
	public static final String SPCUSTOM3 = "SPCUSTOM3";
	
	public enum CancelType{
	    CANCEL,
	    CANCELOLD ,
	    CANCELNEW 
	};

	
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
		 Debug.log(Debug.MSG_STATUS, " In SvRequestMasterLogger initialize() method.");
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

		// get the header SubRequestType form configuration property.
		subRequestType = getPropertyValue(SOAConstants.REQUEST_HEADER_SUBREQUEST_PROP);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log(Debug.SYSTEM_CONFIG,
				"Value of REQUEST_HEADER_SUBREQUEST:[" + subRequestType	+ "].");
		}
		try 
		{ 			  
			// get success Telephone Number List from Context.
			passTNList = (ArrayList) CustomerContext.getInstance().get("successTNList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
			 Debug.log(Debug.MSG_DATA, "SvRequestMasterLogger.passTnList:- "+passTNList);
			}

			// get failed Telephone Number List from Context.		  
			failedTNList = (ArrayList) CustomerContext.getInstance().get("failedTNList");
			if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
			 Debug.log(Debug.MSG_DATA, "SvRequestMasterLogger.passTnList:- "+failedTNList);
			}
		}
		catch (FrameworkException e) {			
				e.printStackTrace(); 
		}

		// get the SPID form configuration property.
		spidValue = getRequiredPropertyValue(SOAConstants.SPID_PROP, errorBuffer);

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log(Debug.SYSTEM_CONFIG, "Value of SPID:[" + spidValue + "].");
		}

		// get the Message form configuration property.
		inputMessage = getRequiredPropertyValue(
				SOAConstants.INPUT_XML_MESSAGE_LOC_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log(Debug.SYSTEM_CONFIG, "Value of inputMessage:["+ inputMessage + "].");
		}

		// get the Request Type form configuration property.
		requestTypeProp = getRequiredPropertyValue(
				SOAConstants.REQUEST_TYPE_PROP, errorBuffer);
		
		//	get the Synchronous key from configuration property.
		syncKeyOut = getPropertyValue(SOAConstants.SYNC_KEY_LOC_PROP);

		// get the Complete Fail Flag form configuration property.
		compFailFlag = getPropertyValue(SOAConstants.COMPLETEFAIL_LOC_PROP);
		
		// changes made in 5.6.2 release
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
		 Debug.log(Debug.SYSTEM_CONFIG,"SvRequestMasterLogger: Initialization done.");
		}

	}

	/**
	 * Extract data values from the context/input, and use them to
	 * insert/upadte a row(s) into the configured database table.
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

		String successTn = null;

		String successRefKey = null;		
		
		
		try {
			
			super.set(SOAConstants.IS_SV_UNIQUE_EXCEPTION, mpContext, inputObject, "false");	 
			
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS, " In SvRequestMasterLogger process() method. ");
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
			String errMsg = "ERROR: SvRequestMasterLogger: Attempt to get database "
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
		 Debug.log(Debug.MSG_STATUS,"requestTYpe ="+ requestType);
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS,
				"The SOA request separator is processing the message. ");
		}

		// Set up the parser for retreiving data from the incoming Message.
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
		
		// Create an DOM for the request
		Document doc = getDOM(inputMessage, mpContext, inputObject);

		// get the SPID value from context.
		spidValue = getValue(spidValue);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS,"Value of spid:"+ spidValue + "].");
		}

		//get the API Flag from context.
		apiFlag = getValue(SOAConstants.API_FLAG);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Value of API FLAG:[" + apiFlag + "].");
		}
		
		// changes made in 5.6.2 release
		//get the initial Message ID value from context
		if ( exists( initMessageID_loc, mpContext, inputObject ) ){
			
			initMessageID = getString(initMessageID_loc, mpContext, inputObject);
		}else{
			initMessageID = null;
		}
		
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, "Value of initial Message ID in Context : "+ initMessageID);
		}
		
		XMLMessageParser inputParser = new XMLMessageParser(doc);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(null, Debug.MSG_STATUS, "The Document request : "+ inputParser.getMessage());
		}

		String telephoneNumbers = null;

		requestBodyNode = SOAConstants.REQUEST_BODY_PATH + "." + requestType
				+ ".";	
				
		tnNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".Tn";
		
		inputParser.compressWhitespace(false);
		
		// If the request is for Single Tn or for Non-Contiguous TN
		if (inputParser.exists(tnNode)) {
			telephoneNumbers = inputParser.getValue(tnNode);
		}
		// If it is a true range request
		else {
			String tnStartNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".TnRange.Tn";
			tnEndNode = requestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".TnRange.EndStation";
			tnStartNode = inputParser.getValue(tnStartNode);
			tnEndNode = inputParser.getValue(tnEndNode);
			telephoneNumbers = tnStartNode + "-" + tnEndNode;
		}
		
		refkeyNode = SOAConstants.REQUEST_BODY_PATH + "." + requestType + ".ReferenceKey";	
		
		ArrayList refKeyListInside = new ArrayList ();
		
		if (inputParser.exists(refkeyNode)) {
			String refKey = null;
			StringBuffer refKeyBuffer = new StringBuffer();
			refKey = inputParser.getValue(refkeyNode);
			if(refKey != null && !refKey.equals("")){
				StringTokenizer st = new StringTokenizer(refKey, SOAConstants.SVID_SEPARATOR);
				while(st.hasMoreTokens()) {
					String individualRefKey = st.nextToken();
					if(individualRefKey != null && !individualRefKey.equals("")){
		            refKeyBuffer.append(individualRefKey);
					refKeyBuffer.append(",");					
					refKeyListInside.add(individualRefKey);
					}
				}
				refKeyValues = refKeyBuffer.toString();
				if(refKeyValues != null && refKeyValues.length()>0)
				refKeyValues = refKeyValues.substring(0, refKeyValues.length()-1);     //remove the last comma
			}
		}
		rangeidNode = SOAConstants.REQUEST_BODY_PATH + "." + requestType + ".RangeId";	
		
		if (inputParser.exists(rangeidNode)) {
			String rangeid = null;
			StringBuffer rangeIdBuffer = new StringBuffer();
			rangeid = inputParser.getValue(rangeidNode);
			if(rangeid != null && !rangeid.equals("")){
	            StringTokenizer st = new StringTokenizer(rangeid, SOAConstants.SVID_SEPARATOR);
				while(st.hasMoreTokens()) {
					String individualRangeKey = st.nextToken();
					if(individualRangeKey != null && !individualRangeKey.equals("")){
					rangeIdBuffer.append(individualRangeKey);
					rangeIdBuffer.append(",");
					}
		         }
				rangeIdValues = rangeIdBuffer.toString();
				if(rangeIdValues != null && rangeIdValues.length()>0)
				rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma
		    }
		}

		ResultSet rsSelect = null;
		try {
			
			StringBuffer mainQuery = new StringBuffer();

			ArrayList totalTnList = new ArrayList();
			
			// If there there is success TN
			if(passTNList != null && !passTNList.isEmpty())
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				  Debug.log(Debug.MSG_STATUS," Breaking Success TN in individula TN.");
				}
				
			  if (passTNList.get(0) != null){

				// Go through the success TN and construct indivisualTNList
				for (int i = 0; i < passTNList.size(); i++) {
					
					// if success TN list contains TN Range
					if (passTNList.get(i).toString().length() > 12) {
						int startTn = Integer.parseInt(passTNList.get(i).toString()
								.substring(8, 12));
						int endTN = Integer.parseInt(passTNList.get(i).toString()
								.substring(13, 17));
						for (int l = startTn; l <= endTN; l++) {
							StringBuffer tn = new StringBuffer(passTNList.get(i)
									.toString().substring(0, 8));

							tn.append(StringUtils.padNumber(l,
									SOAConstants.TN_LINE, true, '0'));

							indivisualTNList.add(tn.toString());
							
						}
					} 
					// if success TN list contains Single TN
					else {
						indivisualTNList.add(passTNList.get(i).toString());
					}
				}

				// Add entire indivisualTNList in the totalTnList
				totalTnList.addAll(indivisualTNList);
			  }
				
			}

			// If there there is fail TN
			if(failedTNList != null && !failedTNList.isEmpty())
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				  Debug.log(Debug.MSG_STATUS,	" Breaking Failed TN in individula TN.");
				}
			
			   if (failedTNList.get(0) != null)
			   {
				   
				   // Go through the fail TN and construct indivisualTNList
				for (int i = 0; i < failedTNList.size(); i++) {
			
					// if fail TN list contains TN Range
					if (failedTNList.get(i).toString().length() > 12) {
						int startTn = Integer.parseInt(failedTNList.get(i)
								.toString().substring(8, 12));
						int endTN = Integer.parseInt(failedTNList.get(i).toString()
								.substring(13, 17));
						for (int l = startTn; l <= endTN; l++) {
							StringBuffer tn = new StringBuffer(failedTNList.get(i)
									.toString().substring(0, 8));
							tn.append(StringUtils.padNumber(l,
									SOAConstants.TN_LINE, true, '0'));
							totalTnList.add(tn.toString());
						}
					} 
					// if fail TN list contains Single TN
					else {
						totalTnList.add(failedTNList.get(i).toString());
					}
				}
			   }
			}			

			int tnCount =totalTnList.size();
			 
			int i = 0;

			// Construct the query that needs to execute to select the referenckey key. If the request/response contains
			// more than 1000 TNs, multiple query will be constructed for each 1000 TN and same will joined using UNION operator.
			// This has been done since ORACLE doesn't support IN operator more than 1000 items.
			while (i < tnCount)
			{			
				int j = 1;
				
				StringBuffer tnValue = new StringBuffer();
				StringBuffer refKeyValueBuffer = new StringBuffer();
				int refKeySize = refKeyListInside.size();
				boolean refKeyFlagInside = false;
				
				while (j <= 1000 && i <= tnCount-1 )
				{
					tnValue.append("'");
					tnValue.append(totalTnList.get(i));
					
					if(refKeySize > 0 ){
						refKeyValueBuffer.append("'");
						refKeyValueBuffer.append(refKeyListInside.get(i));
						refKeyFlagInside = true;
						refKeySize--;
						
					}					
					
					if ( j < 1000 && i != tnCount-1){				
						tnValue.append("',");
						if(refKeySize > 0){
							refKeyValueBuffer.append("',");
						}
					}
					else{
						tnValue.append("'");
						if(refKeySize >= 0 && refKeyFlagInside){
							refKeyValueBuffer.append("'");
							refKeyFlagInside = false;
						}
					}

					i++;
					j++;					
				}
			
			StringBuffer queryTN = new StringBuffer();
			queryTN.append("(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ");
			queryTN.append(SOAConstants.PORTINGTN_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.REFERENCEKEY_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.NNSP_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.ONSP_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.STATUS_COL);
			queryTN.append(", ");	
			queryTN.append(SOAConstants.NNSP_DUE_DATE_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.ONSP_DUE_DATE_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.PORTINGTOORIGINAL_COL);
			queryTN.append(" FROM SOA_SUBSCRIPTION_VERSION WHERE (PORTINGTN, SPID, CREATEDDATE) IN (");
			queryTN.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ");
			queryTN.append(SOAConstants.PORTINGTN_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.SPID_COL);
			queryTN.append(", MAX(CREATEDDATE) FROM SOA_SUBSCRIPTION_VERSION WHERE ");
			queryTN.append(SOAConstants.PORTINGTN_COL);
			queryTN.append(" IN ("+tnValue+" )");
			if((refKeyValues != null && !refKeyValues.equals("")) &&
			   (rangeIdValues == null || rangeIdValues.equals(""))){
				queryTN.append("AND REFERENCEKEY");
				queryTN.append(" IN ("+refKeyValueBuffer+" ) ");
			}
			if((rangeIdValues != null && !rangeIdValues.equals("")) && 
			   (refKeyValues == null || refKeyValues.equals(""))){
				queryTN.append("AND RANGEID");
				queryTN.append(" IN ("+rangeIdValues+" ) ");
			}
			if((rangeIdValues != null && !rangeIdValues.equals("")) && 
			   (refKeyValues != null && !refKeyValues.equals(""))){
				queryTN.append("AND ( RANGEID");
				queryTN.append(" IN ("+rangeIdValues+" ) ");
				queryTN.append("OR REFERENCEKEY");
				queryTN.append(" IN ("+refKeyValueBuffer+" )) ");
			}
			queryTN.append(" AND SPID = '"+spidValue+"' AND ");
			queryTN.append(SOAConstants.STATUS_COL);
			queryTN.append(" IN('");
			queryTN.append(SOAConstants.CONFLICT_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.PENDING_STATUS);
			queryTN.append("', '");

			if (!(requestType.equals(SOAConstants.SV_CREATE_REQUEST) || 
				  requestType.equals(SOAConstants.SV_RELEASE_REQUEST) || 
				  requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)))
			{
				queryTN.append(SOAConstants.ACTIVE_STATUS);
				queryTN.append("', '");

			}
			
			if ( requestType.equals(SOAConstants.SV_CREATE_REQUEST) || 
					  requestType.equals(SOAConstants.SV_RELEASE_REQUEST) || 
					  requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)|| 
					  requestType.equals(SOAConstants.SV_QUERY_REQUEST) )
			{
				
				queryTN.append(SOAConstants.CREATING_STATUS);
				queryTN.append("', '");

				queryTN.append(SOAConstants.NPAC_CREATE_FAILURE);
				queryTN.append("', '");				
			}
			
			queryTN.append(SOAConstants.SENDING_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.DOWNLOAD_FAILED_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.CANCEL_PENDING_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.DISCONNECT_PENDING_STATUS);
			queryTN.append("')");
			queryTN.append(" GROUP BY ");
			queryTN.append(SOAConstants.PORTINGTN_COL);
			queryTN.append(", ");
			queryTN.append(SOAConstants.SPID_COL);
			queryTN.append(") ");
			queryTN.append("AND ");
			queryTN.append(SOAConstants.STATUS_COL);
			queryTN.append(" IN('");
			queryTN.append(SOAConstants.CONFLICT_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.PENDING_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.ACTIVE_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.CREATING_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.NPAC_CREATE_FAILURE);
			queryTN.append("', '");
			queryTN.append(SOAConstants.SENDING_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.DOWNLOAD_FAILED_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.CANCEL_PENDING_STATUS);
			queryTN.append("', '");
			queryTN.append(SOAConstants.DISCONNECT_PENDING_STATUS);
			queryTN.append("'))");			

			mainQuery.append(queryTN);
			
			//Join the individual SQL queries with UNION operator.
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}
			
			
			
			} 
			
			mainQuery.append(" ORDER BY ");
			mainQuery.append(SOAConstants.PORTINGTOORIGINAL_COL);
			mainQuery.append(" DESC ");
			
			// end of construction of query
            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
			  Debug.log(null, Debug.NORMAL_STATUS,"MainQuery : "+ mainQuery.toString());
			}

			// Execute the query
			svObjectSelect = dbConn.createStatement();
			
			rsSelect = svObjectSelect.executeQuery(mainQuery.toString());

			// Process all the selected record
			while (rsSelect.next()) {

				successTn = rsSelect.getString(SOAConstants.PORTINGTN_COL);

				successRefKey = rsSelect.getString(SOAConstants.REFERENCEKEY_COL);

				tnList.add(successTn);

				// If TN is a success TN, add the reference key in successKeyList
				if (indivisualTNList.contains(successTn))
				{
					successKeyList.add(successRefKey);					
						
				}
								

				// Add the referenceKey in refKeyList
				refKeyList.add(successRefKey);

				// Add the status in statusList
				statusList.add(rsSelect.getString(SOAConstants.STATUS_COL));
				
				nnspList.add(rsSelect.getString(SOAConstants.NNSP_COL));
				
				onspList.add(rsSelect.getString(SOAConstants.ONSP_COL));
				
				nnspDueDateList.add(rsSelect.getTimestamp(SOAConstants.NNSPDUEDATE_COL));
				
				onspDueDateList.add(rsSelect.getTimestamp(SOAConstants.ONSPDUEDATE_COL));
			}

			// Create Map for TN and referenceKey
			for (int k = 0; k < tnList.size(); k++) {
				tnRefMap.put(tnList.get(k), refKeyList.get(k));
			}
						
			// INSERT/UPDATE into SOA_SUBSCRIPTION_VERSION table.
			if(!requestType.equals(SOAConstants.SV_ACTIVATE_REQUEST) &&
					!requestType.equals(SOAConstants.SV_CANCEL_REQUEST) &&
					!requestType.equals(SOAConstants.SV_CANCEL_AS_NEW_REQUEST) &&
					!requestType.equals(SOAConstants.SV_CANCEL_AS_OLD_REQUEST) &&
					!requestType.equals(SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST) &&
					!requestType.equals(SOAConstants.SV_QUERY_REQUEST)){
				
				getSvObjectInsert(dbConn, inputParser, requestType);
			}

			// Update SOA_SUBSCRIPTION_VERSION table.
			if(!successKeyList.isEmpty()){
				
				updateLastColumns(dbConn,inputParser);
			}			
			
			
			// INSERT into SOA_SV_MESSAGE table.
			if(requestType.equals(SOAConstants.SV_CANCEL_REQUEST))
				getSvCancelMessageInsert(failedTNList, passTNList, dbConn, inputParser,
						requestType, telephoneNumbers);			
			else				
				getSvMessageInsert(failedTNList, passTNList, dbConn, inputParser,
					requestType, telephoneNumbers);
			
			// INSERT into SOA_REQUEST_QUEUE table.
			insertMessageQueue(dbConn);
			
			// to insert into SOA_MESSAGE_MAP, SOA_SV_RANGE, SOA_SV_RANGE_MAP and SOA_PENDING_REQUEST tables.
			getSvMapInsert(dbConn);			

			//commit the transaction.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS,
							"Committing data inserted by SvRequestMasterLogger to database.");
				}
				try {
					DBConnectionPool.getInstance(true).commit(dbConn);
				} catch (ResourceException re) {
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
				}
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(null, Debug.MSG_STATUS, "Output Msg : "+message.getDocument());
			}

			//	set the Synchronous key in context
			super.set(syncKeyOut, mpContext,inputObject, message.getDocument() );

			//set the Complete failure flag in context
			if(passTNList == null || passTNList.isEmpty() || passTNList.get(0) == null){
				completeFail = "true";
			}
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(null, Debug.MSG_STATUS, "CompleteFail Flag: "+completeFail);
			}
			super.set(compFailFlag, mpContext,inputObject, completeFail );

			// commit the transaction for API requests and complete failed requests.
			if ( !(apiFlag != null && apiFlag.equals("G") ))
			{
				if(failedTNList != null && !failedTNList.isEmpty() && failedTNList.get(0) != null)
				{
					dbConn.commit();
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(null, Debug.MSG_STATUS, "Committed in MasterLogger");
					}
				}
			}
			else if (completeFail.equals("true"))
			{
				dbConn.commit();
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				 Debug.log(null, Debug.MSG_STATUS, "Committed in MasterLogger for complete fail");
				}
			}
			

		} catch (SQLException sqlex) {
			sqlex.printStackTrace();
			String errMsg = "ERROR: SvRequestMasterLogger: Attempt to log to database "
					+ "failed with error: " + sqlex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS,
					"Rolling-back any database changes due to SvRequestMasterLogger.");
				}
				try {
					DBConnectionPool.getInstance(true).rollback(dbConn);
				} catch (ResourceException re) {
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
				}
			}
			
			if (sqlex.getMessage() != null
						&& sqlex.getMessage().indexOf(
								"ORA-00001: unique constraint") != -1) {

					if (Debug.isLevelEnabled(Debug.MSG_ERROR)) {
						Debug.log(Debug.MSG_ERROR,
								"Caught 'ORA-00001: unique constraint' Exception in process() "
										+ sqlex.getMessage());
					}

					// set the SOA_SV_UNIQUE_INDEX exception in context
					super.set(SOAConstants.SV_DUPLICATE_ERROR_MSG, mpContext,
							inputObject, sqlex.getMessage());
					super.set(SOAConstants.IS_SV_UNIQUE_EXCEPTION, mpContext,
							inputObject, "true");

					String setInitialCount = "1";

					if (mpContext.exists(SOAConstants.SV_DUPLICATE_ERROR_COUNT)) {

						int errCountValue = Integer
								.parseInt(mpContext
										.getString(SOAConstants.SV_DUPLICATE_ERROR_COUNT));

						if (errCountValue > 0 && errCountValue < 3) {
							errCountValue = errCountValue + 1;
							super.set(SOAConstants.SV_DUPLICATE_ERROR_COUNT,
									mpContext, inputObject, Integer
											.toString(errCountValue));
							if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
								Debug.log(Debug.MSG_STATUS,
										"Set duplicate error count["
												+ errCountValue + "]");
							}
						}
						if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug.log(Debug.MSG_STATUS,
									"Next Resubmitting the request with next error count["
											+ errCountValue + "]");
						}

					} else {
						if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug.log(Debug.MSG_STATUS,
									"Initially re-submitting the request with initial error count["
											+ setInitialCount + "]");
						}
						super.set(SOAConstants.SV_DUPLICATE_ERROR_COUNT,
								mpContext, inputObject, setInitialCount);
					}

					removeEmptyNodes(inputParser);

					inputObject.set(doc);
				}			

		} catch (FrameworkException fe) {

			// If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS,
						"Rolling-back any database changes due to SvRequestMasterLogger.");
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
				// if svObjectSelect is not closed
				if (svObjectSelect != null) {
					// close svObjectSelect statement
					svObjectSelect.close();
					svObjectSelect = null;
				}
				// if svPendingInsertStmt is not closed
				if (svPendingInsertStmt != null) {
					// close svPendingInsertStmt statement
					svPendingInsertStmt.close();
					svPendingInsertStmt = null;
				}
				// if svObjectUpdateStmt is not closed
				if (svObjectUpdateStmt != null) {
					// close svObjectUpdateStmt statement
					svObjectUpdateStmt.close();
					svObjectUpdateStmt = null;					
				}
				// if svObjectInsertStmt is not closed
				if (svObjectInsertStmt != null) {
					// close svObjectInsertStmt statement
					svObjectInsertStmt.close();
					svObjectInsertStmt = null;
				}
				// if insertMessageQueueStmt is not closed
				if (insertMessageQueueStmt != null) {
					// close insertMessageQueueStmt statement
					insertMessageQueueStmt.close();
					insertMessageQueueStmt = null;
				}
				// if updateLastColumnStmt is not closed
				if(updateLastColumnStmt != null){
					// close updateLastColumnStmt statement
					updateLastColumnStmt.close();
					updateLastColumnStmt = null;
				}
				// if mapInsertStmt is not closed
				if(mapInsertStmt != null){
					// close mapInsertStmt statement
					mapInsertStmt.close();
					mapInsertStmt = null;
				}
				// if svRangeStmt is not closed
				if(svRangeStmt != null){
					// close svRangeStmt statement
					svRangeStmt.close();
					svRangeStmt = null;
				}
				// if svRangeMapStmt is not closed
				if(svRangeMapStmt != null){
					// close svRangeMapStmt statement
					svRangeMapStmt.close();
					svRangeMapStmt = null;
				}
               //if svRangeIdMapStmt is not closed
				if(svRangeIdMapStmt != null)
                {
               //close svRangeIdMapStmt statement
                    svRangeIdMapStmt.close();
                    svRangeIdMapStmt = null;
                }
				// if ResultSet is not closed
				if (rsSelect != null) {
					// close ResultSet statement
					rsSelect.close();
					rsSelect = null;
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
	
	private void removeEmptyNodes(XMLMessageParser inputParser) throws MessageParserException, MessageException
	{

		String reqBodyNode = "UpstreamToSOA.UpstreamToSOABody.SvCreateRequest.";
		boolean classDPC = false;
		boolean classSSN = false;
		boolean lidbDPC = false;
		boolean lidbSSN = false;
		boolean isvmDPC = false;
		boolean isvmSSN = false;
		boolean cnamDPC = false;
		boolean cnamSSN = false;
		boolean wsmscDPC = false;
		boolean wsmscSSN = false;

		if (inputParser.exists(reqBodyNode + "GTTData.ClassDPC")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.ClassDPC"))
					.equals("")) {
				classDPC = true;
				inputParser.removeNode(reqBodyNode + "GTTData.ClassDPC");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.ClassSSN")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.ClassSSN"))
					.equals("")) {
				classSSN = true;
				inputParser.removeNode(reqBodyNode + "GTTData.ClassSSN");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.LidbDPC")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.LidbDPC"))
					.equals("")) {
				lidbDPC = true;
				inputParser.removeNode(reqBodyNode + "GTTData.LidbDPC");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.LidbSSN")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.LidbSSN"))
					.equals("")) {
				lidbSSN = true;
				inputParser.removeNode(reqBodyNode + "GTTData.LidbSSN");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.IsvmDPC")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.IsvmDPC"))
					.equals("")) {
				isvmDPC = true;
				inputParser.removeNode(reqBodyNode + "GTTData.IsvmDPC");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.IsvmSSN")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.IsvmSSN"))
					.equals("")) {
				isvmSSN = true;
				inputParser.removeNode(reqBodyNode + "GTTData.IsvmSSN");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.CnamDPC")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.CnamDPC"))
					.equals("")) {
				cnamDPC = true;
				inputParser.removeNode(reqBodyNode + "GTTData.CnamDPC");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.CnamSSN")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.CnamSSN"))
					.equals("")) {
				cnamSSN = true;
				inputParser.removeNode(reqBodyNode + "GTTData.CnamSSN");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.WsmscDPC")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.WsmscDPC"))
					.equals("")) {
				wsmscDPC = true;
				inputParser.removeNode(reqBodyNode + "GTTData.WsmscDPC");
			}
		}

		if (inputParser.exists(reqBodyNode + "GTTData.WsmscSSN")) {
			if ((inputParser.getValue(reqBodyNode + "GTTData.WsmscSSN"))
					.equals("")) {
				wsmscSSN = true;
				inputParser.removeNode(reqBodyNode + "GTTData.WsmscSSN");
			}
		}

		if (wsmscSSN && wsmscDPC && cnamSSN && cnamDPC && isvmSSN && isvmDPC
				&& lidbSSN && lidbDPC && classSSN && classDPC) {
			inputParser.removeNode(reqBodyNode + "GTTData");
		}

		if (inputParser.exists(reqBodyNode + SvType)) {
			if ((inputParser.getValue(reqBodyNode + SvType)).equals("")) {
				inputParser.removeNode(reqBodyNode + SvType);
			}
		}

		if (inputParser.exists(reqBodyNode + AlternativeSPID)) {
			if ((inputParser.getValue(reqBodyNode + AlternativeSPID))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + AlternativeSPID);
			}
		}
		if (inputParser.exists(reqBodyNode + VoiceURI)) {
			if ((inputParser.getValue(reqBodyNode + VoiceURI)).equals("")) {
				inputParser.removeNode(reqBodyNode + VoiceURI);
			}
		}
		if (inputParser.exists(reqBodyNode + MMSURI)) {
			if ((inputParser.getValue(reqBodyNode + MMSURI)).equals("")) {
				inputParser.removeNode(requestBodyNode + MMSURI);
			}
		}
		if (inputParser.exists(reqBodyNode + PoCURI)) {
			if ((inputParser.getValue(reqBodyNode + PoCURI)).equals("")) {
				inputParser.removeNode(reqBodyNode + PoCURI);
			}
		}
		if (inputParser.exists(reqBodyNode + PRESURI)) {
			if ((inputParser.getValue(reqBodyNode + PRESURI)).equals("")) {
				inputParser.removeNode(reqBodyNode + PRESURI);
			}
		}

		if (inputParser.exists(reqBodyNode + SMSURI)) {
			if ((inputParser.getValue(reqBodyNode + SMSURI)).equals("")) {
				inputParser.removeNode(reqBodyNode + SMSURI);
			}
		}

		if (inputParser.exists(reqBodyNode + "EndUserLocationType")) {
			if ((inputParser.getValue(reqBodyNode + "EndUserLocationType"))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + "EndUserLocationType");
			}
		}

		if (inputParser.exists(reqBodyNode + "EndUserLocationValue")) {
			if ((inputParser.getValue(reqBodyNode + "EndUserLocationValue"))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + "EndUserLocationValue");
			}
		}

		if (inputParser.exists(reqBodyNode + "BillingId")) {
			if ((inputParser.getValue(reqBodyNode + "BillingId")).equals("")) {
				inputParser.removeNode(reqBodyNode + "BillingId");
			}
		}
		if (inputParser.exists(reqBodyNode + "AlternativeEndUserLocationType")) {
			if ((inputParser.getValue(reqBodyNode
					+ "AlternativeEndUserLocationType")).equals("")) {
				inputParser.removeNode(reqBodyNode
						+ "AlternativeEndUserLocationType");
			}
		}

		if (inputParser.exists(reqBodyNode + "AlternativeEndUserLocationValue")) {
			if ((inputParser.getValue(reqBodyNode
					+ "AlternativeEndUserLocationValue")).equals("")) {
				inputParser.removeNode(reqBodyNode
						+ "AlternativeEndUserLocationValue");
			}
		}

		if (inputParser.exists(reqBodyNode + "AlternativeBillingId")) {
			if ((inputParser.getValue(reqBodyNode + "AlternativeBillingId"))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + "AlternativeBillingId");
			}
		}
		// Changes are made in 5.6.5 release (NANC 441, Simple Port Req)
		if (inputParser.exists(reqBodyNode + "NNSPSimplePortIndicator")) {
			if ((inputParser.getValue(reqBodyNode + "NNSPSimplePortIndicator"))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + "NNSPSimplePortIndicator");
			}
		}
		if (inputParser.exists(reqBodyNode + "ONSPSimplePortIndicator")) {
			if ((inputParser.getValue(reqBodyNode + "ONSPSimplePortIndicator"))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + "ONSPSimplePortIndicator");
			}
		}
		if (inputParser.exists(reqBodyNode + LastAlternativeSPID)) {
			if ((inputParser.getValue(reqBodyNode + LastAlternativeSPID))
					.equals("")) {
				inputParser.removeNode(reqBodyNode + LastAlternativeSPID);
			}
		}
		if (inputParser.exists(reqBodyNode + SPCUSTOM1)) {
			if ((inputParser.getValue(reqBodyNode + SPCUSTOM1)).equals("")) {
				inputParser.removeNode(reqBodyNode + SPCUSTOM1);
			}
		}
		if (inputParser.exists(reqBodyNode + SPCUSTOM2)) {
			if ((inputParser.getValue(reqBodyNode + SPCUSTOM2)).equals("")) {
				inputParser.removeNode(reqBodyNode + SPCUSTOM2);
			}
		}
		if (inputParser.exists(reqBodyNode + SPCUSTOM3)) {
			if ((inputParser.getValue(reqBodyNode + SPCUSTOM3)).equals("")) {
				inputParser.removeNode(reqBodyNode + SPCUSTOM3);
			}
		}
		if (inputParser.exists(reqBodyNode + "Lrn")) {
			if ((inputParser.getValue(reqBodyNode + "Lrn")).equals("")) {
				inputParser.removeNode(reqBodyNode + "Lrn");
			}
		}

	}
	

	/**
	 * Update LASTREQUESTTYPE, LASTREQUESTDATE and LASTMESSAGE in
	 * SV object database table.
	 * Also update ACCOUNTID and ACCOUNTNAME if it is a request from Account
	 *
	 * @param dbConn
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  SQLException  Thrown if db fails.
	 * @exception  FrameworkException.
	 */
	private void updateLastColumns(Connection dbConn, XMLMessageParser inputParser)
								throws SQLException, FrameworkException {
		
		String bpelSpidSupport = null;	
		if (spidValue != null){
			bpelSpidSupport = String.valueOf(NANCSupportFlag.getInstance(spidValue).getBpelSpidFlag());
		}
		
		
		// Construct the update query for SOA_SUBSCRIPTION_VERSION table
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS," In SVRequestMasterLogger.updateLastColumns() method.");
		}
		
		String subdomain = "";
		boolean isSubDomainLevelUser = false;
		
		subdomain = CustomerContext.getInstance().getSubDomainId();
		if( subdomain != null && !(subdomain.equals("")) )
		{
			isSubDomainLevelUser = true;
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS," In SVRequestMasterLogger.updateLastColumns.subdomainid :"+subdomain);
		}
		
		StringBuffer lUpdateQuery = new StringBuffer();

		

		lUpdateQuery.append(SOAConstants.SOA_LAST_COLUMN_UPDATE);

		// Add the ACCOUNTID column in update statement
		if (inputParser.exists(requestBodyNode + "AccountId")) {
				lUpdateQuery.append(", ");
				lUpdateQuery.append(SOAConstants.ACCOUNTID_COL);
				lUpdateQuery.append("= ? ");
		}
		// Add the ACCOUNTNAME column in update statement
		if (inputParser.exists(requestBodyNode + "AccountName")) {
				lUpdateQuery.append(", ");
				lUpdateQuery.append(SOAConstants.ACCOUNTNAME_COL);
				lUpdateQuery.append("= ?");
		}

		// Add SUBDOMAINID column in update Statement
		 if( isSubDomainLevelUser )
		 {
			lUpdateQuery.append(", ");
			lUpdateQuery.append(SOAConstants.SUBDOMAIN_COL);
			lUpdateQuery.append("= ?");
		 }
		if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID)&&
				inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) !=null && 
				inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) !="") {
			 	lUpdateQuery.append(", ");
				lUpdateQuery.append("OID");
				lUpdateQuery.append("= ?");
			}
		
		//Added in SOA 5.6.4.1			
		if(apiFlag.equals(SOAConstants.GUI_REQUEST)){
			
			if(bpelSpidSupport !=null && "1".equals(bpelSpidSupport)){
				
				//SOA 5.6.4.1 patch if request is coming from GUI then always update TransOID with null
				lUpdateQuery.append(", ");
				lUpdateQuery.append("TransOID");
				lUpdateQuery.append("= ?");
			}				
		}
		else
		{
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) &&
				inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) !=null && 
				inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) !="") {
				lUpdateQuery.append(", ");
				lUpdateQuery.append("TransOID");
				lUpdateQuery.append("= ?");
			}
		}
		
		lUpdateQuery.append(" WHERE REFERENCEKEY = ?");
		
		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		 Debug.log(Debug.NORMAL_STATUS,	" In SVRequestMasterLogger.updateLastColumns.mainQuery :"+lUpdateQuery.toString());
		}
		
		updateLastColumnStmt = dbConn.prepareStatement(lUpdateQuery.toString());
		
		// Add all the success TN into batch for which already record exists
		for(int i=0;i<successKeyList.size();i++)
		{
			int queryIndex = 5;
			
			updateLastColumnStmt.setString(1,requestType);
			Date datetime = new Date();
			updateLastColumnStmt.setTimestamp(2, new Timestamp(datetime
					.getTime()));
			updateLastColumnStmt.setString(3, requestType );
			
			updateLastColumnStmt.setString(4, requestType );
			
			if (inputParser.exists(requestBodyNode + "AccountId")) {
				updateLastColumnStmt.setString(5, inputParser.getValue(requestBodyNode+ "AccountId"));
				queryIndex = 6;
			}
			if (inputParser.exists(requestBodyNode + "AccountName")) {
				updateLastColumnStmt.setString(6, inputParser.getValue(requestBodyNode+ "AccountName"));
				queryIndex = 7;
			}
			if( isSubDomainLevelUser )
			{
				updateLastColumnStmt.setString(queryIndex, subdomain );
					
				queryIndex+=1;
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) &&
					inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) !=null && 
					inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) !="") {
				updateLastColumnStmt.setString(queryIndex, inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID));
				queryIndex+=1;
			}
			//Added in SOA 5.6.4.1				
			if(apiFlag.equals(SOAConstants.GUI_REQUEST)){
				
				if(bpelSpidSupport != null && "1".equals(bpelSpidSupport)){
					
					//SOA 5.6.4.1 patch if request is coming from gui then always update TransOID with null
					updateLastColumnStmt.setNull(queryIndex, java.sql.Types.VARCHAR);
					queryIndex+=1;
				}
				
			}
			else{
				if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) &&
						inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) !=null && 
						inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) !="") {
					
					updateLastColumnStmt.setString(queryIndex, inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID));
					queryIndex+=1;
				}
			}
			
			updateLastColumnStmt.setString(queryIndex , successKeyList.get(i).toString());

			updateLastColumnStmt.addBatch();
		}
		updateLastColumnStmt.executeBatch();
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
		  Debug.log(Debug.MSG_STATUS," In SVRequestMasterLogger.insertMessageQueue() method.");
		}
		
        if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
			 Debug.log(Debug.NORMAL_STATUS,	" Insert query for SOA_REQUEST_QUEUE :\n"+SOAConstants.SOA_SV_REQUEST_QUEUE_INSERT);
			}
        
		insertMessageQueueStmt = dbConn
				.prepareStatement(SOAConstants.SOA_SV_REQUEST_QUEUE_INSERT);

		//Iterate through each input xml message or the successfull TNs.
		for (int i = 0; i < messageList.size(); i++) {
			insertMessageQueueStmt.setString(1, Integer.toString(PersistentSequence
					.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_QUEUE_ID, false,dbConn )) );
			insertMessageQueueStmt.setString(2, SOAConstants.SOA_REQUEST_MESSAGE_TYPE);
			Date datetime = new Date();
			insertMessageQueueStmt.setTimestamp(3, new Timestamp(datetime
					.getTime()));
			insertMessageQueueStmt.setString(4, SOAConstants.SOA_ERROR_COUNT);
			insertMessageQueueStmt.setString(5, SOAConstants.SOA_ERROR_COUNT);
			insertMessageQueueStmt.setString(6, SOAConstants.NPAC_QUEUE_STATUS);
			
			CharSequence message = (CharSequence)messageList.get(i);
			message = SOAUtility.removeWhitespace(message).toString();
			message = RegexUtils.replaceAll("> <", message.toString(), ">\n<");
			
			insertMessageQueueStmt.setString(7, message.toString());
			insertMessageQueueStmt.setString(8, spidValue);
			insertMessageQueueStmt.addBatch();
		}
		insertMessageQueueStmt.executeBatch();
				
		if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
		Debug.log(Debug.DB_STATUS, "The SV Message(s) has " +
										"been inserted into the SOA_REQUEST_QUEUE Table.");
		}
	}
	
	
	private boolean isDueDateAdjustable (ArrayList adjustDueDateListOfTNs, String requestType)
	{
		if (requestType.equals(SOAConstants.SV_CREATE_REQUEST)
				|| requestType.equals(SOAConstants.SV_RELEASE_REQUEST)
				|| requestType
						.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)) {
			if (adjustDueDateListOfTNs != null
					&& adjustDueDateListOfTNs.size() > 0) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	
	private boolean adjustDueDateCheck1 (ArrayList adjustDueDateListOfTNs, ArrayList passTNList)
	{
		Iterator itr = adjustDueDateListOfTNs.iterator();
		boolean chkTNRangeSplit = false;
		while (itr.hasNext()) {
			String adjustTN = (String) itr.next();
			if (!passTNList.contains(adjustTN)) {
				chkTNRangeSplit = true;
				break;
			}
		}
		return chkTNRangeSplit;
	}
	
	private int adjustDueDateCheck2 (ArrayList<String> splitTNList)
	{
		Date dueDate1;
		long time1 = 0;
		long time2 = 0;
		int count = 0;
		for (String tn2 : splitTNList) {
			int tnIndex = tnList.indexOf(tn2);
			if (requestType.equals(SOAConstants.SV_CREATE_REQUEST)) {
				dueDate1 = (Date) onspDueDateList.get(tnIndex);
			} else {
				dueDate1 = (Date) nnspDueDateList.get(tnIndex);
			}
			time1 = dueDate1.getTime();
			if (count == 0)
				count++;
			else if (time1 == time2)
				count++;
			else
				break;
			time2 = time1;
		}

		return count;
	}
	
	

	/**
	 * This method will insert record into SOA_SV_MESSAGE table
	 *
	 * @param failedTNList
	 * @param passTNList
	 * @param dbConn
	 * @param inputParser
	 * @param requestType
	 * @param telephoneNumbers
	 * 
	 * @throws SQLException
	 * @throws FrameworkException
	 */
	private void getSvMessageInsert(ArrayList failedTNList,
			ArrayList passTNList, Connection dbConn,
			XMLMessageParser inputParser, String requestType,
			String telephoneNumbers) throws SQLException, FrameworkException {
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvMessageInsert() method.");
		}
		
		ArrayList adjustDueDateListOfTNs = (ArrayList) CustomerContext.getInstance().get("adjustDueDateTNList");						
		
		String portingTN = "";		

		String rangeNode = requestBodyNode + "Subscription.TnRange";
		String startTnNode = requestBodyNode + "Subscription.TnRange.Tn";

		String endStationNode = requestBodyNode + "Subscription.TnRange.EndStation";
		svMessageInsert = dbConn.prepareStatement(SOAConstants.SOA_SV_MESSAGE_DATA_INSERT);						
		
		String subdomain = "";		
		
		subdomain = CustomerContext.getInstance().getSubDomainId();
		  
		if( !(subdomain != null && !(subdomain.equals(""))) )
		{
			if(CustomerContext.getInstance().get("subdomain") != null)
				subdomain = (String)CustomerContext.getInstance().get("subdomain");
		}
		  
		CustomerContext.getInstance().set("subdomain_in_svmsg" , subdomain );		
			

		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		 Debug.log(Debug.NORMAL_STATUS,	"Insert Query for SV Message: \n "+SOAConstants.SOA_SV_MESSAGE_DATA_INSERT);
		}
		
		int tnIdx = 0;

		boolean passTnFlag = false;

		ArrayList totTns = new ArrayList();

		//If the failed TN list having values.
		if(failedTNList != null && !failedTNList.isEmpty())
		{			
			if (failedTNList.get(0) != null)
			{
				totTns.addAll(failedTNList);
			}
		}
		ArrayList newpassTNList = new ArrayList();
		//If the success TN list having values.
		if(passTNList != null && !passTNList.isEmpty())
		{			
			if (passTNList.get(0) != null)
			{
				if (isDueDateAdjustable(adjustDueDateListOfTNs, requestType) && adjustDueDateCheck1(adjustDueDateListOfTNs,passTNList))
				{
					Iterator passTnItr = passTNList.iterator();
					while (passTnItr.hasNext()) {
						String tempTn = (String) passTnItr.next();
						if (tempTn.length() > 12) {
							boolean chkSplit = false;
							String start = tempTn.substring(0, 12);
							String end = tempTn.substring(13);
							ArrayList<String> splitTNList = SOAUtility.getSvRangeTnList(start, end);
							for (String tn1 : splitTNList) {
								if (!adjustDueDateListOfTNs.contains(tn1))
									chkSplit = true;
							}
							if (!chkSplit) {
								int count = adjustDueDateCheck2(splitTNList);
								if (count == splitTNList.size()) {
									for (String tn3 : splitTNList) {
										adjustDueDateListOfTNs.remove(tn3);
									}
									adjustDueDateListOfTNs.add(tempTn);
									newpassTNList.add(tempTn);
								} else
									chkSplit = true;
							}
							if (chkSplit) {
								newpassTNList.addAll(splitTNList);
							}
						} else
							newpassTNList.add(tempTn);

					}

					if (newpassTNList != null && newpassTNList.size() > 0)
						totTns.addAll(newpassTNList);

				} 
				else 
				{					
					totTns.addAll(passTNList);
					newpassTNList.addAll(passTNList);
				}
			}
		}
				
		message = new XMLMessageGenerator("SuccessFailureRequestId");

		int passId = 0;
		int failedId = 0;
		int tnStatusIdx = -1;
		String statusTn = null;			
		
		boolean isDateToModify = false;

		//loop over all the success Telephone Numbers.
		for (int i = 0; i < totTns.size(); i++) {
			String startTn = null;
			String tn = null;
			String endStation = null;
			boolean isRange = false;			

			// Get the messagekey value from sequence
			String msgKey = Integer.toString(PersistentSequence
				.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_MSG_KEY, false,dbConn ));			
			
			// Get the rangekey value from sequence
			String rangeKey = Integer.toString(PersistentSequence.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_RANGE_KEY, false, dbConn ));			


			tn = (String)totTns.get(i);		
								
			
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
			svMessageInsert.setString(15, apiFlag);
			
			//changes made in 5.6.2 release
			//insert the initial MessageID
			if(initMessageID!=null)
			{
				svMessageInsert.setString(16, initMessageID);
			}
			else
			{
				svMessageInsert.setNull(16, java.sql.Types.INTEGER );
			}
			
			//If input tn exist in success TN list.
			if (newpassTNList.contains(tn)) {
				message.setValue("SuccessRequest.RequestIdTN(" + passId +").RequestId",msgKey);
				message.setValue("SuccessRequest.RequestIdTN(" + passId +").RequestTn",tn);				
				svMessageInsert.setString(6, SOAConstants.SOA_QUEUED_STATUS);
				passTnFlag = true;
				passId++;				
			} else if (failedTNList.contains(tn)) {
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

			// get the index of the TN's from the list.
			if (tnList.contains(tn)) {
				tnIdx = tnList.indexOf(tn);				
			}

			portingTN = tn;
			
			Node parentNode = inputParser.getNode("UpstreamToSOA");
			
			String accName = requestBodyNode + "AccountName";
			String accId = requestBodyNode + "AccountId";
			String rangeID = requestBodyNode + "RangeId";
			String accRefKey = requestBodyNode + "ReferenceKey";
			String lata = null;
			if(requestType.equals(SOAConstants.SV_MODIFY_REQUEST)){
				lata = requestBodyNode +"DataToModify."+"Lata";
			}else
			{
				lata = requestBodyNode + "Lata";	
			}

			boolean classDPC = false;
			boolean	classSSN = false;
			boolean	lidbDPC = false;
			boolean	lidbSSN = false;
			boolean	isvmDPC = false;
			boolean	isvmSSN = false;
			boolean	cnamDPC = false;
			boolean	cnamSSN = false;
			boolean	wsmscDPC = false;
			boolean	wsmscSSN = false;					    
			
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

			// remove AccountName node from input message.
			if (inputParser.exists(accName)) {
				inputParser.removeNode(accName);
			}

			// remove AccountId node from input message.
			if (inputParser.exists(accId)) {
				inputParser.removeNode(accId);
			}
           // remove RangeId node from input message.
			if (inputParser.exists(rangeID)) {
				inputParser.removeNode(rangeID);
			}
           //remove lata node from input message.
			if (inputParser.exists(lata)) {
				inputParser.removeNode(lata);
			}

			// remove ReferenceKey node from input message.
			if (inputParser.exists(accRefKey)) {
				inputParser.removeNode(accRefKey);
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID)) {
				inputParser.removeNode(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID);
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID)) {
				inputParser.removeNode(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID);
			}
			
			// remove the GTTData and other optional Nodes from the SvCreateRequest if the node does not have values.
			if (requestBodyNode.equals("UpstreamToSOA.UpstreamToSOABody.SvCreateRequest.")) {
				
				if (inputParser.exists(requestBodyNode + "GTTData.ClassDPC")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.ClassDPC")).equals(""))
					{
						classDPC = true;						
						inputParser.removeNode(requestBodyNode + "GTTData.ClassDPC");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.ClassSSN")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.ClassSSN")).equals(""))
					{
						classSSN =true;
						inputParser.removeNode(requestBodyNode + "GTTData.ClassSSN");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.LidbDPC")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.LidbDPC")).equals(""))
					{
						lidbDPC =true;
						inputParser.removeNode(requestBodyNode + "GTTData.LidbDPC");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.LidbSSN")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.LidbSSN")).equals(""))
					{
						lidbSSN = true;
						inputParser.removeNode(requestBodyNode + "GTTData.LidbSSN");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.IsvmDPC")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.IsvmDPC")).equals(""))
					{
						isvmDPC = true;
						inputParser.removeNode(requestBodyNode + "GTTData.IsvmDPC");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.IsvmSSN")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.IsvmSSN")).equals(""))
					{
						isvmSSN = true;
						inputParser.removeNode(requestBodyNode + "GTTData.IsvmSSN");
					}				
				}
				
				if (inputParser.exists(requestBodyNode + "GTTData.CnamDPC")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.CnamDPC")).equals(""))
					{
						cnamDPC = true;
						inputParser.removeNode(requestBodyNode + "GTTData.CnamDPC");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.CnamSSN")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.CnamSSN")).equals(""))
					{
						cnamSSN = true;
						inputParser.removeNode(requestBodyNode + "GTTData.CnamSSN");
					}				
				}
			
				if (inputParser.exists(requestBodyNode + "GTTData.WsmscDPC")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.WsmscDPC")).equals(""))
					{
						wsmscDPC = true;
						inputParser.removeNode(requestBodyNode + "GTTData.WsmscDPC");
					}				
				}

				if (inputParser.exists(requestBodyNode + "GTTData.WsmscSSN")) {
					if ((inputParser.getValue(requestBodyNode+ "GTTData.WsmscSSN")).equals(""))
					{
						wsmscSSN = true;
						inputParser.removeNode(requestBodyNode + "GTTData.WsmscSSN");
					}				
				}

				if (wsmscSSN && wsmscDPC && cnamSSN && cnamDPC && isvmSSN && isvmDPC && lidbSSN && lidbDPC && classSSN && classDPC)
				{
					inputParser.removeNode(requestBodyNode + "GTTData");
				}

				if (inputParser.exists(requestBodyNode + SvType)) {
					if ((inputParser.getValue(requestBodyNode+ SvType)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + SvType);
					}				
				}
				
				if (inputParser.exists(requestBodyNode + AlternativeSPID)) {
					if ((inputParser.getValue(requestBodyNode+ AlternativeSPID)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + AlternativeSPID);
					}				
				}
				if (inputParser.exists(requestBodyNode + VoiceURI)) {
					if ((inputParser.getValue(requestBodyNode+ VoiceURI)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + VoiceURI);
					}				
				}
				if (inputParser.exists(requestBodyNode + MMSURI)) {
					if ((inputParser.getValue(requestBodyNode+ MMSURI)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + MMSURI);
					}				
				}
				if (inputParser.exists(requestBodyNode + PoCURI)) {
					if ((inputParser.getValue(requestBodyNode+ PoCURI)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + PoCURI);
					}				
				}
				if (inputParser.exists(requestBodyNode + PRESURI)) {
					if ((inputParser.getValue(requestBodyNode+ PRESURI)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + PRESURI);
					}				
				}
				
				if (inputParser.exists(requestBodyNode + SMSURI)) {
					if ((inputParser.getValue(requestBodyNode+ SMSURI)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + SMSURI);
					}				
				}

				if (inputParser.exists(requestBodyNode + "EndUserLocationType")) {
					if ((inputParser.getValue(requestBodyNode+ "EndUserLocationType")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "EndUserLocationType");
					}				
				}

				if (inputParser.exists(requestBodyNode + "EndUserLocationValue")) {
					if ((inputParser.getValue(requestBodyNode+ "EndUserLocationValue")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "EndUserLocationValue");
					}				
				}

				if (inputParser.exists(requestBodyNode + "BillingId")) {
					if ((inputParser.getValue(requestBodyNode+ "BillingId")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "BillingId");
					}				
				}
				if (inputParser.exists(requestBodyNode + "AlternativeEndUserLocationType")) {
					if ((inputParser.getValue(requestBodyNode+ "AlternativeEndUserLocationType")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "AlternativeEndUserLocationType");
					}				
				}

				if (inputParser.exists(requestBodyNode + "AlternativeEndUserLocationValue")) {
					if ((inputParser.getValue(requestBodyNode+ "AlternativeEndUserLocationValue")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "AlternativeEndUserLocationValue");
					}				
				}

				if (inputParser.exists(requestBodyNode + "AlternativeBillingId")) {
					if ((inputParser.getValue(requestBodyNode+ "AlternativeBillingId")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "AlternativeBillingId");
					}				
				}	
				//Changes are made in 5.6.5 release (NANC 441, Simple Port Req)
				if (inputParser.exists(requestBodyNode + "NNSPSimplePortIndicator"))
				{
					if ((inputParser.getValue(requestBodyNode+ "NNSPSimplePortIndicator")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "NNSPSimplePortIndicator");
					}				
				}
				if (inputParser.exists(requestBodyNode + "ONSPSimplePortIndicator"))
				{
					if ((inputParser.getValue(requestBodyNode+ "ONSPSimplePortIndicator")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "ONSPSimplePortIndicator");
					}				
				}
				if (inputParser.exists(requestBodyNode + LastAlternativeSPID)) {
					if ((inputParser.getValue(requestBodyNode+ LastAlternativeSPID)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + LastAlternativeSPID);
					}				
				}
				//End 5.6.5
				
				//SOA 5.6.8 Changes
				if (inputParser.exists(requestBodyNode + SPCUSTOM1)) {
					if ((inputParser.getValue(requestBodyNode+ SPCUSTOM1)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + SPCUSTOM1);
					}				
				}
				if (inputParser.exists(requestBodyNode + SPCUSTOM2)) {
					if ((inputParser.getValue(requestBodyNode+ SPCUSTOM2)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + SPCUSTOM2);
					}				
				}
				if (inputParser.exists(requestBodyNode + SPCUSTOM3)) {
					if ((inputParser.getValue(requestBodyNode+ SPCUSTOM3)).equals(""))
					{
						inputParser.removeNode(requestBodyNode + SPCUSTOM3);
					}				
				}
				//End SOA 5.6.8
				//Fixed TD #11198
				if (inputParser.exists(requestBodyNode + "Lrn")) {
					if ((inputParser.getValue(requestBodyNode+ "Lrn")).equals(""))
					{
						inputParser.removeNode(requestBodyNode + "Lrn");
					}				
				}
				
				
			}
			
			// If the request is SvModifyRequest add the SvStatus node
			if(requestType.equals(SOAConstants.SV_MODIFY_REQUEST)){
				
				tnStatusIdx = -1;
				statusTn = null;

				if (isRange)
					tnStatusIdx = tnList.indexOf(startTn);				
				else
					tnStatusIdx = tnList.indexOf(tn);

				if ( tnStatusIdx != -1)
				{
					statusTn = (String)statusList.get(tnStatusIdx);
					Node n = inputParser.getDocument().getDocumentElement();
					n = XMLMessageBase.getNode( n, SOAConstants.SV_MODIFY_STATUS_NODE, true );				

					XMLMessageBase.setNodeValue( n, statusTn );
				}
			}			

			// Generate the individual request for each subrange

			Node[] children = XMLMessageBase.getChildNodes(parentNode);

			XMLMessageGenerator generator = new XMLMessageGenerator(
					"SOAMessage");
			generator.setValue("UpstreamToSOA." + children[0].getNodeName(),
					children[0]);
			generator.setValue("UpstreamToSOA." + children[1].getNodeName(),
					children[1]);
			
			
			// if tn belongs to CN than remove the simple port indicator
			//get the boolean value that is TN belongs to Canadian region
			
			boolean isTnBelongsToCN = isPortingTnBelongsToCN(tn);
			
			if (isTnBelongsToCN
					&& (
						(failedTNList != null 
							&& !failedTNList.contains(tn) 
							
						) 
						|| 
						(newpassTNList != null
							&& newpassTNList.contains(tn) 
							
						)
					)
				) 
			{
				
				
				if(!isDateToModify){
					if(requestType.equals(SOAConstants.SV_MODIFY_REQUEST)){
						requestBodyNode = requestBodyNode + "DataToModify.";
						isDateToModify = true;
					}
				}
				
				String nnspSimplePortIndicatorNode = requestBodyNode + "NNSPSimplePortIndicator";
				if(inputParser.exists(nnspSimplePortIndicatorNode)){
					
					generator.removeNode(nnspSimplePortIndicatorNode);
				
					if(Debug.isLevelEnabled(Debug.MSG_STATUS))
							Debug.log(Debug.MSG_STATUS, "TN belongs to canada and nnsp simple portindicator present" +
								" so remove it");
				}
				
				String onspSimplePortIndicatorNode = requestBodyNode + "ONSPSimplePortIndicator";
				if(inputParser.exists(onspSimplePortIndicatorNode)){
					
					generator.removeNode(onspSimplePortIndicatorNode);
				
					if(Debug.isLevelEnabled(Debug.MSG_STATUS))
							Debug.log(Debug.MSG_STATUS, "TN belongs to canada and onsp simple portindicator present" +
								" so remove it");
				}
			}
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
				Debug.log(null, Debug.MSG_STATUS, "The separated request : "
						+ generator.getMessage());
			}
            
         // SOA 5.9 adjust DueDate change
			if (isDueDateAdjustable(adjustDueDateListOfTNs,requestType))
			{
				String dueDateNode;
				if (requestType.equals(SOAConstants.SV_CREATE_REQUEST))
					dueDateNode = requestBodyNode + "NewSPDueDate";
				else
					dueDateNode = requestBodyNode + "OldSPDueDate";

				if (adjustDueDateListOfTNs.contains(tn) && newpassTNList != null && newpassTNList.contains(tn)) {
					int tnNewIdx = -1;
					if (isRange)
						tnNewIdx = tnList.indexOf(startTn);
					else
						tnNewIdx = tnList.indexOf(tn);

					if (tnNewIdx != -1) {
						SimpleDateFormat dateFormat = new SimpleDateFormat(
								"MM-dd-yyyy-hhmmssa");
						Date dueDate;
						if (requestType.equals(SOAConstants.SV_CREATE_REQUEST)) {
							dueDate = (Date) onspDueDateList.get(tnNewIdx);
						} else {
							dueDate = (Date) nnspDueDateList.get(tnNewIdx);
						}

						String newDate = dateFormat.format(dueDate).toString();
						generator.setValue(dueDateNode, newDate);
					}
				} else {
					generator.setValue(dueDateNode, inputParser
							.getValue(dueDateNode));
				}
			}

			// store TN's and corrospanding reference keys in a Map.
			if (!isRange) {

				tnMsgMap.put(portingTN, msgKey);
					tnKeyList.add(portingTN);

			} else {
				int startTnVal = Integer.parseInt(startTn.substring(8, 12));				
				int endTnVal = Integer.parseInt(endStation);
				String rangeKey2="";
				if(startTnVal < endTnVal && 
						(requestType.equals(SOAConstants.SV_CREATE_REQUEST) || 
								requestType.equals(SOAConstants.SV_RELEASE_REQUEST) || 
								requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)) && 
								newpassTNList != null && !newpassTNList.isEmpty() && newpassTNList.contains(tn)){
					     //Get the rangekey value from equence
				         rangeKey2 = Integer.toString(PersistentSequence.
						getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_RANGEID_KEY, false,dbConn ));
				}
				
				for (int p = startTnVal; p <= endTnVal; p++) {
					StringBuffer tnBuff = new StringBuffer(startTn.substring(0,8));
					tnBuff.append(StringUtils.padNumber(p,SOAConstants.TN_LINE, true, '0'));

					tnMsgMap.put(tnBuff.toString(), msgKey);
					tnRangeMap.put(tnBuff.toString(), rangeKey);
					if(startTnVal < endTnVal && 
							(requestType.equals(SOAConstants.SV_CREATE_REQUEST) || 
									requestType.equals(SOAConstants.SV_RELEASE_REQUEST) || 
									requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)) && 
									newpassTNList != null && !newpassTNList.isEmpty() && newpassTNList.contains(tn)){
						    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 	 Debug.log(null, Debug.MSG_STATUS, "Checking the ranke key : "+rangeKey2);
							}
						    tnRangeMap2.put(tnBuff.toString(), rangeKey2);
					 }
					tnKeyList.add(tnBuff.toString());
						
				}
			}
			
			// If Single TN, make 17 digit TN
			if (!isRange) {
				portingTN = portingTN + "-" + portingTN.substring(8, 12);
			}
			svMessageInsert.setString(8, portingTN);
			Date datetime = new Date();
			svMessageInsert.setTimestamp(9, new Timestamp(datetime.getTime()));
			
			// SOA 5.8.3 :: If the request is SvReleaseInConflictRequest , log CauseCode column.
			if(requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST) && newpassTNList.contains(tn) && (inputParser.exists(SOAConstants.REQUEST_BODY_PATH + "." + SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST + "." + SOAConstants.CAUSECODE_NODE)))
			{
				String causeCodeVal = inputParser.getValue(SOAConstants.REQUEST_BODY_PATH + "." + SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST + "." + SOAConstants.CAUSECODE_NODE);								
				svMessageInsert.setString(17, causeCodeVal);
			}
			else
			{				
				svMessageInsert.setNull(17, java.sql.Types.VARCHAR );
			}
					
			
			CharSequence message = (CharSequence)generator.getXMLDocumentAsString();
			message = SOAUtility.removeWhitespace(message).toString();
			
			message = RegexUtils.replaceAll("> <", message.toString(), ">\n<");
			
			svMessageInsert.setString(12, message.toString() );
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
	
	private CancelType getCancelRequestType(String passTn)
	{
		int tnIndex=-1;
		
		 if (tnList.contains(passTn)) {
				tnIndex = tnList.indexOf(passTn);
				String nnsp = (String)nnspList.get(tnIndex);				
				String onsp = (String)onspList.get(tnIndex);				
				String status = (String)statusList.get(tnIndex);				
				if(status.equals("pending"))
				{
					return CancelType.CANCEL;
				}
				else if (status.equals("cancel-pending") && spidValue.equals(onsp))
				{
					return CancelType.CANCELOLD;
				}
				else if (status.equals("cancel-pending") && spidValue.equals(nnsp))
				{
					return CancelType.CANCELNEW;
				}
				
			}
		 
		 return null;		 
		
	}
	
	private void getSvCancelMessageInsert(ArrayList failedTNList,
			ArrayList passTNList, Connection dbConn,
			XMLMessageParser inputParser, String requestType,
			String telephoneNumbers) throws SQLException, FrameworkException {
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvCancelMessageInsert() method.");
		}
		
		String portingTN = "";		

		String rangeNode = requestBodyNode + "Subscription.TnRange";
		String startTnNode = requestBodyNode + "Subscription.TnRange.Tn";		

		String endStationNode = requestBodyNode + "Subscription.TnRange.EndStation";
		svMessageInsert = dbConn.prepareStatement(SOAConstants.SOA_SV_MESSAGE_DATA_INSERT);		
		String subdomain = "";		
		subdomain = CustomerContext.getInstance().getSubDomainId();		  
		if( !(subdomain != null && !(subdomain.equals(""))) )
		{
			if(CustomerContext.getInstance().get("subdomain") != null)
				subdomain = (String)CustomerContext.getInstance().get("subdomain");
		}		  
		CustomerContext.getInstance().set("subdomain_in_svmsg" , subdomain );
		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		 Debug.log(Debug.NORMAL_STATUS,	"Insert Query for SV Cancel Message: \n "+SOAConstants.SOA_SV_MESSAGE_DATA_INSERT);
		}				
		boolean passTnFlag = false;
		ArrayList totTns = new ArrayList();
		//If the failed TN list having values.
		if(failedTNList != null && !failedTNList.isEmpty())
		{			
			if (failedTNList.get(0) != null)
			{
				totTns.addAll(failedTNList);
			}
		}			
		ArrayList cancelTNList = new ArrayList();
		ArrayList cancelOldTNList = new ArrayList();
		ArrayList cancelNewTNList = new ArrayList();		
		//If the success TN list having values.
		if(passTNList != null && !passTNList.isEmpty())
		{
			if (passTNList.get(0) != null) {
				for (int i = 0; i < passTNList.size(); i++) {

					String passTn = (String) passTNList.get(i);					
					if (passTn.length() > 12) {
						String start = passTn.substring(0, 12);
						String end = passTn.substring(13);
						ArrayList splitTNList = SOAUtility.getSvRangeTnList(start,
								end);						
						ArrayList tempcancelTNList = new ArrayList();
						ArrayList tempcancelOldTNList = new ArrayList();
						ArrayList tempcancelNewTNList = new ArrayList();	
						
						for (int s = 0; s < splitTNList.size(); s++) {
							String newTn = (String) splitTNList.get(s);							
							CancelType type = getCancelRequestType(newTn);											
							switch (type) {
							case CANCEL:
								tempcancelTNList.add(newTn);
								break;
							case CANCELOLD:
								tempcancelOldTNList.add(newTn);
								break;
							case CANCELNEW:
								tempcancelNewTNList.add(newTn);
								break;
							}
						}	
						
						if(!tempcancelTNList.isEmpty() && (splitTNList.size() == tempcancelTNList.size()))
						{							
							cancelTNList.add(passTn);							
							tempcancelTNList.clear();
						}
						else if(!tempcancelOldTNList.isEmpty() && (splitTNList.size() == tempcancelOldTNList.size()))
						{							
							cancelOldTNList.add(passTn);
							tempcancelOldTNList.clear();
						}
						else if(!tempcancelNewTNList.isEmpty() && (splitTNList.size() == tempcancelNewTNList.size()))
						{							
							cancelNewTNList.add(passTn);
							tempcancelNewTNList.clear();
						}
						else
						{							
							if(!tempcancelTNList.isEmpty())
							{
								tempcancelTNList = collapseConsecutiveTNSubRange(tempcancelTNList, false);
								cancelTNList.addAll(tempcancelTNList);
								tempcancelTNList.clear();
							}
							if(!tempcancelOldTNList.isEmpty())
							{
								tempcancelOldTNList = collapseConsecutiveTNSubRange(tempcancelOldTNList, false);
								cancelOldTNList.addAll(tempcancelOldTNList);
								tempcancelOldTNList.clear();
							}
							if(!tempcancelNewTNList.isEmpty())
							{
								tempcancelNewTNList = collapseConsecutiveTNSubRange(tempcancelNewTNList, false);
								cancelNewTNList.addAll(tempcancelNewTNList);
								tempcancelNewTNList.clear();
							}
						}

					} else {
						CancelType type = getCancelRequestType(passTn);												
						switch (type) {
						case CANCEL:
							cancelTNList.add(passTn);
							break;
						case CANCELOLD:
							cancelOldTNList.add(passTn);
							break;
						case CANCELNEW:
							cancelNewTNList.add(passTn);
							break;
						}
					}

				}
							
				String tnCoalescingFlagValue = NANCSupportFlag.getInstance(spidValue).getTnCoalescingFlag();								
				if (StringUtils.hasValue(tnCoalescingFlagValue) && tnCoalescingFlagValue.equals("1")) {					
					if(!cancelTNList.isEmpty())						
						cancelTNList = collapseConsecutiveTNSubRange(cancelTNList, true);
					if(!cancelOldTNList.isEmpty())
						cancelOldTNList = collapseConsecutiveTNSubRange(cancelOldTNList, true);
					if(!cancelNewTNList.isEmpty())
						cancelNewTNList = collapseConsecutiveTNSubRange(cancelNewTNList, true);
				} 				
				if(!cancelTNList.isEmpty())
					totTns.addAll(cancelTNList);
				if(!cancelOldTNList.isEmpty())
					totTns.addAll(cancelOldTNList);
				if(!cancelNewTNList.isEmpty())
					totTns.addAll(cancelNewTNList);
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
			String newrequestType= null;
						
			// Get the messagekey value from sequence
			String msgKey = Integer.toString(PersistentSequence
				.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_MSG_KEY, false,dbConn ));			
			
			// Get the rangekey value from sequence
			String rangeKey = Integer.toString(PersistentSequence.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_RANGE_KEY, false, dbConn ));			

			tn = (String)totTns.get(i);								
			
			//If input tn exist in success TN list.
			if (passTNList.contains(tn) || cancelTNList.contains(tn) || cancelOldTNList.contains(tn) || cancelNewTNList.contains(tn)) {				
				message.setValue("SuccessRequest.RequestIdTN(" + passId +").RequestId",msgKey);
				message.setValue("SuccessRequest.RequestIdTN(" + passId +").RequestTn",tn);				
				svMessageInsert.setString(6, SOAConstants.SOA_QUEUED_STATUS);
				passTnFlag = true;
				passId++;					
				if(cancelTNList.contains(tn))
					newrequestType = "SvCancelRequest";
				else if (cancelOldTNList.contains(tn))
					newrequestType = "SvCancelAckAsOldRequest";
				else if (cancelNewTNList.contains(tn))
					newrequestType = "SvCancelAckAsNewRequest";
				
				
				
			} else if (failedTNList.contains(tn)) {				
				message.setValue("FailureRequest.RequestIdTN(" + failedId +").RequestId",msgKey);
				message.setValue("FailureRequest.RequestIdTN(" + failedId +").RequestTn",tn);				
				svMessageInsert.setString(6, SOAConstants.SOA_DATAERROR_STATUS);
				passTnFlag = false;
				failedId++;
				newrequestType = requestType;
			}					
			svMessageInsert.setString(1, msgKey.trim());
			svMessageInsert.setString(13, msgKey.trim());
			svMessageInsert.setString(2, SOAConstants.REQUEST);
			svMessageInsert.setString(3, newrequestType);
			SQLBuilder.populateCustomerID( svMessageInsert, 4 );
			svMessageInsert.setString(5, spidValue);
			SQLBuilder.populateUserID(svMessageInsert, 7 );
			svMessageInsert.setString(10, "1_0");
			SQLBuilder.populateUserID(svMessageInsert, 11 );
			svMessageInsert.setString(14, subdomain);			
			svMessageInsert.setString(15, apiFlag);
			
			if(initMessageID!=null)
				svMessageInsert.setString(16, initMessageID);			
			else
				svMessageInsert.setNull(16, java.sql.Types.INTEGER );
													
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
			String accName = requestBodyNode + "AccountName";
			String accId = requestBodyNode + "AccountId";
			String rangeID = requestBodyNode + "RangeId";
			String accRefKey = requestBodyNode + "ReferenceKey";						
			String newrequestBodyNode = SOAConstants.REQUEST_BODY_PATH + "." + newrequestType
			+ ".";				
			String tnNewNode = newrequestBodyNode + SOAConstants.SUBSCRIPTION_NODE + ".Tn";			
			String newrangeNode = newrequestBodyNode + "Subscription.TnRange";
			String newstartTnNode = newrequestBodyNode + "Subscription.TnRange.Tn";	
			String newendStationNode = newrequestBodyNode + "Subscription.TnRange.EndStation";			
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
			// remove AccountName node from input message.
			if (inputParser.exists(accName)) {
				inputParser.removeNode(accName);
			}
			// remove AccountId node from input message.
			if (inputParser.exists(accId)) {
				inputParser.removeNode(accId);
			}
           // remove RangeId node from input message.
			if (inputParser.exists(rangeID)) {
				inputParser.removeNode(rangeID);
			}           
			// remove ReferenceKey node from input message.
			if (inputParser.exists(accRefKey)) {
				inputParser.removeNode(accRefKey);
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID)) {
				inputParser.removeNode(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID);
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID)) {
				inputParser.removeNode(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID);
			}
			// Generate the individual request for each subrange
			Node[] children = XMLMessageBase.getChildNodes(parentNode);
			XMLMessageGenerator generator = new XMLMessageGenerator(
					"SOAMessage");
			generator.setValue("UpstreamToSOA." + children[0].getNodeName(),
					children[0]);
			generator.setValue("UpstreamToSOA." + children[1].getNodeName(),
					children[1]);			
			Document dom = generator.getDocument();
			NodeList nodes = dom.getElementsByTagName(requestType);				 		
			dom.renameNode(nodes.item(0), null, newrequestType);				 						 			 
			// If range then add Tn and EndStation node
			if (isRange) {				
				generator.setValue(newstartTnNode, startTn);
				generator.setValue(newendStationNode, endStation);				
			} 			
			// Add Tn node
			else {				
				generator.setValue(tnNewNode, tn);				
			}
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(null, Debug.MSG_STATUS, "The separated Cancel request : "
						+ generator.getMessage());
			}
			// store TN's and corrospanding reference keys in a Map.
			if (!isRange) {

				tnMsgMap.put(portingTN, msgKey);
					tnKeyList.add(portingTN);					

			} else {				
				int startTnVal = Integer.parseInt(startTn.substring(8, 12));
				int endTnVal = Integer.parseInt(endStation);								
				for (int p = startTnVal; p <= endTnVal; p++) {
					StringBuffer tnBuff = new StringBuffer(startTn.substring(0,8));
					tnBuff.append(StringUtils.padNumber(p,SOAConstants.TN_LINE, true, '0'));
					tnMsgMap.put(tnBuff.toString(), msgKey);
					tnRangeMap.put(tnBuff.toString(), rangeKey);										
					tnKeyList.add(tnBuff.toString());
						
				}
			}
			
			// If Single TN, make 17 digit TN
			if (!isRange) {
				portingTN = portingTN + "-" + portingTN.substring(8, 12);
			}
			svMessageInsert.setString(8, portingTN);
			Date datetime = new Date();
			svMessageInsert.setTimestamp(9, new Timestamp(datetime.getTime()));			
			svMessageInsert.setNull(17, java.sql.Types.VARCHAR );			
			CharSequence message = (CharSequence)generator.getXMLDocumentAsString();
			message = SOAUtility.removeWhitespace(message).toString();			
			message = RegexUtils.replaceAll("> <", message.toString(), ">\n<");			
			svMessageInsert.setString(12, message.toString() );
			generator.setValue("UpstreamToSOA.UpstreamToSOAHeader.InvokeID", msgKey.trim());
			// For success TN, add into messageList
			if (passTnFlag)
			{
				messageList.add(generator.getXMLDocumentAsString());
			}			
			svMessageInsert.addBatch();
		}
		if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
		 Debug.log(Debug.DB_STATUS, "The Output Cancel message:"+  message.getMessage());
		}
		svMessageInsert.executeBatch();
		if( Debug.isLevelEnabled(Debug.DB_STATUS) ){
		Debug.log(Debug.DB_STATUS, "The SV Cancel Message(s)  has been " +
				"inserted into the SOA_SV_MESSAGE Table.");
		}
	}

	/**
	 * This method will INSERT/UPDATE record in SOA_SUBSCRIPTION_VERSION request table
	 * 
	 * @param dbConn
	 * @param inputParser
	 * @param requestType
	 * 
	 * @throws SQLException
	 * @throws FrameworkException
	 */
	private void getSvObjectInsert(Connection dbConn,
			XMLMessageParser inputParser, String requestType) throws SQLException, FrameworkException {

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert() method.");
		}
		
		
		try
		{
			
			getRequestData(inputParser);
			
			
			
			boolean flag1 = false;
			boolean flag2 = false;
			boolean flag3 = false;
			String refKey = null;
			String tns = null;
			
			String subdomain = "";
			
			subdomain = CustomerContext.getInstance().getSubDomainId();
			  
			  if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			    Debug.log(Debug.MSG_STATUS,	" In getSvObjectInsert(): before getting subdomain value :");
			  }
			  
			  if( !(subdomain != null && !(subdomain.equals(""))) )
			  {
				  if(CustomerContext.getInstance().get("subdomain") != null)
					  subdomain = (String)CustomerContext.getInstance().get("subdomain");
			  }
		      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			    Debug.log(Debug.MSG_STATUS,	" In getSvObjectInsert() method: get subdomain value :"+subdomain);
			  }
			  
			// Iterate through all the input Telephone Numbers.
			for (int i = 0; i < indivisualTNList.size(); i++) 
			{
				
				tns = indivisualTNList.get(i).toString();		
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				 Debug.log(Debug.MSG_STATUS,	" In getSvObjectInsert() method: TN is ->"+tns);
				}
				
				if (tnList.contains(tns)) 
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert(): if Tn already exist in Object table .");
					}
					int tnIdx = tnList.indexOf(tns);
					String statusTn = (String)statusList.get(tnIdx);

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS,"SVRequestMasterLogger:StatusofTN"+statusTn);
					}
					
					// prepare the list to insert into SOA_PENDING_REQUEST table.
						if (((statusList.get(tnIdx).equals(SOAConstants.PENDING_STATUS) || statusList
								.get(tnIdx).equals(SOAConstants.CONFLICT_STATUS)  || 
								statusList.get(tnIdx).equals(SOAConstants.DISCONNECT_PENDING_STATUS) || 
								statusList.get(tnIdx).equals(SOAConstants.ACTIVE_STATUS)) && !(requestType
								.equals(SOAConstants.SV_RELEASE_REQUEST) || requestType
								.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)))
								|| requestType
										.equals(SOAConstants.SV_DISCONNECT_REQUEST)) 
						{
							if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							  Debug.log(Debug.MSG_STATUS,	"if insert into SOA_PENDING_REQUEST table ");
							}
							if (!flag1) 
							{
								svPendingInsertStmt = dbConn
										.prepareStatement(getInsertStmt(true, true, requestType, statusTn));
								flag1 = true;						
							}
																
							if (!requestType.equals(SOAConstants.SV_MODIFY_REQUEST)) 
							{
								pendingTnList.add(indivisualTNList.get(i).toString());
							}
							else if (!requestNodeList.isEmpty() || ! requestNdDtList.isEmpty() ) 
							{
								pendingTnList.add(indivisualTNList.get(i).toString());
							}
							
						//Setting Prepared statement Object to update SOA_SUBSCRIPTION_VERSION table 
						//if the status is in creating or NPACCreateFailure.
						} 
						else if (statusList.get(tnIdx).equals(SOAConstants.CREATING_STATUS) || statusList
								.get(tnIdx).equals(SOAConstants.NPAC_CREATE_FAILURE)) 
						{
							if (!flag2) 
							{
								svObjectUpdateStmt = dbConn.prepareStatement(getInsertStmt(false, false, requestType, statusTn));
								flag2 = true;						
							}
		
							int k = 8;
							svObjectUpdateStmt.setString(1, indivisualTNList.get(i).toString());
							svObjectUpdateStmt.setString(2, spidValue);
							Date datetime = new Date();
							svObjectUpdateStmt.setTimestamp(3, new Timestamp(datetime.getTime()));
							SQLBuilder.populateCustomerID( svObjectUpdateStmt, 4 );
							SQLBuilder.populateUserID(svObjectUpdateStmt, 5 );
							svObjectUpdateStmt.setString(6,	SOAConstants.CREATING_STATUS);
							
							svObjectUpdateStmt.setString(7,	subdomain);
							
							//Changes for 5.6.4 release TD#8478
							String strColumn = null;
							int columnIndx = 0;
							int dateFieldIndx = 0;
							// Iterate through svUpdateColumnList list
							for (int j = 0; j < svUpdateColumnList.size(); j++)
							{
								strColumn = svUpdateColumnList.get(j).toString();
								//checking column in updateColumnList present in requestNodeList
								columnIndx = requestNodeList.indexOf(strColumn);
								dateFieldIndx = requestNdDtList.indexOf(strColumn);
								//checking column in updateColumnList present in requestNdDtList (For Date field)
								if(columnIndx > -1)
								{
									if((isPortingTnBelongsToCN(tns)) && (requestNodeList.get(columnIndx).toString().
											equals(NNSP_SIMPLEPORTINDICATOR) 
											|| requestNodeList.get(columnIndx).toString().equals(ONSP_SIMPLEPORTINDICATOR))){
										svObjectUpdateStmt.setNull(j + 8, java.sql.Types.NULL);
									}else{
									svObjectUpdateStmt.setString(j + 8, requestDataList
											.get(columnIndx).toString());
									}
								}
								else if(dateFieldIndx > -1)
								{
									svObjectUpdateStmt.setTimestamp(j + 8, formatDate(requestDtList.get(dateFieldIndx).toString()));
								}
								else //If some column value does not come through Request set these value to Null.
								{
									if(strColumn.indexOf("DATE") > -1)
									{
										svObjectUpdateStmt.setNull(j + 8, java.sql.Types.TIMESTAMP);
									}
									else
									{
										svObjectUpdateStmt.setNull(j + 8, java.sql.Types.VARCHAR);
									}
								}
								k++;
							}
							//end TD#8478		
							svObjectUpdateStmt.setString(k, refKeyList.get(tnIdx)
									.toString());
							svObjectUpdateStmt.addBatch();
						}				
					
				} 
					
				// Setting Prepared statement Object to insert into SOA_SUBSCRIPTION_VERSION table.
				else 
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert(): if new entyr fof Tn required in Object table .");
					}
					refKey = Integer.toString(PersistentSequence
							.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_REF_KEY, false, dbConn ));			
					refKeyList.add(refKey);
	
					tnRefMap.put(indivisualTNList.get(i).toString(), refKey);
					if (!flag3) {
						svObjectInsertStmt = dbConn.prepareStatement(getInsertStmt(
								true, false, requestType, null));
						flag3 = true;
					}
					svObjectInsertStmt.setString(1, indivisualTNList.get(i)
							.toString());
					svObjectInsertStmt.setString(2, spidValue);
					svObjectInsertStmt.setString(3, refKey);
					Date datetime = new Date();
					svObjectInsertStmt.setTimestamp(4, new Timestamp(datetime.getTime()));
					SQLBuilder.populateCustomerID( svObjectInsertStmt, 5 );
					SQLBuilder.populateUserID( svObjectInsertStmt, 6 );
					svObjectInsertStmt.setString(7, SOAConstants.CREATING_STATUS);
					svObjectInsertStmt.setString(8, requestType);
					svObjectInsertStmt.setString(9, requestType);
					svObjectInsertStmt.setTimestamp(10, new Timestamp(datetime.getTime()));

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert() requestType :"+requestType);
						Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert() subdomain :"+subdomain);
					}
					
					if(requestType.equals(SOAConstants.SV_CREATE_REQUEST))
					{
						svObjectInsertStmt.setString(11, subdomain);
					}
					if( (requestType.equals(SOAConstants.SV_RELEASE_REQUEST)|| requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)) && subdomain !=null  && !subdomain.equals(""))
					{
						svObjectInsertStmt.setString(11, subdomain);
					}
					else if(requestType.equals(SOAConstants.SV_RELEASE_REQUEST) || requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST) )
					{
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert(): in else if block.");
						}
						String subdomainVal = "";
						
						if( CustomerContext.getInstance().get("releaseReqTNSubdomain") != null)
						{
							ArrayList releaseReqTNSubdomain = (ArrayList) CustomerContext.getInstance().get("releaseReqTNSubdomain");
							
							for(int k=0; k < releaseReqTNSubdomain.size();k++)
							{
								CreateTnSubdomainData ctsd = (CreateTnSubdomainData)releaseReqTNSubdomain.get(k);
		
								if(ctsd.portingTn.equals(tns))
									subdomainVal = ctsd.subdomain;
							}
						}
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvObjectInsert(): sbudomainVal:"+subdomainVal);
						}
						svObjectInsertStmt.setString(11, subdomainVal);
					}
					svObjectInsertStmt.setString(12, requestType);
					
					//Iterate through requestDataList list to get the non-Date columns.
					for (int j = 0; j < requestDataList.size(); j++) {
						
						if(isPortingTnBelongsToCN(tns) && (requestNodeList.get(j).toString().equals(NNSP_SIMPLEPORTINDICATOR)
								|| requestNodeList.get(j).toString().equals(ONSP_SIMPLEPORTINDICATOR))){
							svObjectInsertStmt.setNull(j + 13, java.sql.Types.NULL);
									
						}else{
							svObjectInsertStmt.setString(j + 13, requestDataList.get(j)
								.toString());
						}
					}
					//Iterate through requestDtList list to get the Date columns.
					for (int m = 0; m < requestDtList.size(); m++) {
						svObjectInsertStmt.setTimestamp(m + 13
								+ requestDataList.size(), formatDate(requestDtList
								.get(m).toString()));
					}
					svObjectInsertStmt.addBatch();
				}
			}
		
			//Execute svObjectInsertStmt statement to insert SOA_SUBSCRIPTION_VERSION table.
			if (svObjectInsertStmt != null) {
				svObjectInsertStmt.executeBatch();
			}
	
			//Execute svObjectUpdateStmt statement to update SOA_SUBSCRIPTION_VERSION table.
			if (svObjectUpdateStmt != null) {
				svObjectUpdateStmt.executeBatch();
			}
		}
		catch(Exception e)
		{
			if (Debug.isLevelEnabled(Debug.MSG_ERROR))
				Debug.log(Debug.MSG_ERROR,
						" Error Occured in getSvObjectInsert():" + e);

			if (e.getMessage() != null
					&& e.getMessage().indexOf("ORA-00001: unique constraint") != -1) {
				if (Debug.isLevelEnabled(Debug.MSG_ERROR)) {
					Debug.log(Debug.MSG_ERROR,
							" Throwing 'ORA-00001: unique constraint' SQLException with message ["
									+ e.getMessage() + "]");
				}
				throw new SQLException(e.getMessage());
			}
		}
	}


	
	/**
	 * This method will INSERT record into SOA_MESSAGE_MAP, SOA_SV_RANGE and SOA_SV_RANGE_MAP and SOA_PENDING_PENDING table
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
		 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSvMapInsert() method.");
		}

		mapInsertStmt = dbConn.prepareStatement(SOAConstants.SOA_MSG_MAP);
		
		svRangeStmt = dbConn.prepareStatement(SOAConstants.SOA_RANGE_MAP);
		
		svRangeMapStmt = dbConn.prepareStatement(SOAConstants.SOA_RANGE_MAP_INSERT);
		
		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Insert query for SOA_MESSAGE_MAP: \n"+ SOAConstants.SOA_MSG_MAP);
		
		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Insert query for SOA_SV_RANGE: \n"+ SOAConstants.SOA_RANGE_MAP);
		
		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Insert query for SOA_SV_RANGE_MAP: \n"+ SOAConstants.SOA_RANGE_MAP_INSERT);
		
		//Added for
		svRangeIdMapStmt = dbConn.prepareStatement(SOAConstants.SOA_SUBSCRIPTION_VERSION_UPDATE);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS, "Checking the ranke key ="+svRangeIdMapStmt.toString());
		}

		HashSet rangeKey = new HashSet();
		
		String rangeId = null;
				
		Date datetime = new Date();
		
		String subdomain = "";
		
		subdomain = CustomerContext.getInstance().getSubDomainId();
		
		  
		if( !(subdomain != null && !(subdomain.equals(""))) )
		{
			if(CustomerContext.getInstance().get("subdomain") != null)
			 subdomain = (String)CustomerContext.getInstance().get("subdomain");
		}		
		
		
		
		// Iterate though tnKey List to insert into message map table.
		if(tnKeyList != null){
			for (int i = 0; i < tnKeyList.size(); i++) 
				{
	
				//Check to insert into SOA_MESSAGE_MAP table
				if (tnMsgMap.containsKey(tnKeyList.get(i).toString())
						&& tnRefMap.containsKey(tnKeyList.get(i).toString())) {
					mapInsertStmt.setString(1, tnMsgMap.get(tnKeyList.get(i))
							.toString());
					mapInsertStmt.setString(2, tnRefMap.get(tnKeyList.get(i))
							.toString());
					mapInsertStmt.addBatch();
				}
				
				rangeId = (String)tnRangeMap.get(tnKeyList.get(i));
				
				//Check to insert into SOA_SV_RANGE table
				if (tnMsgMap.containsKey(tnKeyList.get(i).toString())
						&& tnRangeMap.containsKey(tnKeyList.get(i).toString()) && !rangeKey.contains(rangeId)) {
					rangeKey.add(rangeId);
					svRangeStmt.setString(1, tnRangeMap.get(tnKeyList.get(i))
							.toString());
					svRangeStmt.setString(2, requestType);
					svRangeStmt.setString(3, tnMsgMap.get(tnKeyList.get(i))
							.toString());
					svRangeStmt.setTimestamp(4, new Timestamp(datetime.getTime()));
					
					svRangeStmt.setString(5,subdomain);
					
					svRangeStmt.addBatch();
				}
				
				//Check to insert into SOA_SV_RANGE_MAP table.
				if (tnRangeMap.containsKey(tnKeyList.get(i).toString())
						&& tnRefMap.containsKey(tnKeyList.get(i).toString())) {
					svRangeMapStmt.setString(1, tnRangeMap.get(tnKeyList.get(i))
							.toString());
					svRangeMapStmt.setString(2, tnRefMap.get(tnKeyList.get(i))
							.toString());
					svRangeMapStmt.addBatch();
				}
	
				
				//Check to update into SOA_SUBSCRIPTION_VERSION table.
				if (tnRangeMap2.containsKey(tnKeyList.get(i).toString())
						&& tnRefMap.containsKey(tnKeyList.get(i).toString())) 
				{
					
					svRangeIdMapStmt.setString(1, tnRangeMap2.get(tnKeyList.get(i))
								.toString());
					svRangeIdMapStmt.setString(2, tnRefMap.get(tnKeyList.get(i))
							.toString());
					svRangeIdMapStmt.addBatch();
					
				}
				else if((requestType.equals(SOAConstants.SV_CREATE_REQUEST) || requestType.equals(SOAConstants.SV_RELEASE_REQUEST) || requestType.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)))
				{
					if ( tnKeyList.size() == 1  && tnRefMap !=null && tnRefMap.containsKey(tnKeyList.get(0).toString()) )
					{
					
						svRangeIdMapStmt.setString(1, "");
						svRangeIdMapStmt.setString(2, tnRefMap.get(tnKeyList.get(0)).toString());
						svRangeIdMapStmt.addBatch();
						
					}
				
				}
			}
		}
		
		//Execute mapInsertStmt statement to insert into Message Map table.
		if (mapInsertStmt != null) {
			int[] mapCount = mapInsertStmt.executeBatch();
		}

		//Execute svRangeStmt statement to insert into Sv range table.
		if (svRangeStmt != null) {
				int[] insertRangeCounts = svRangeStmt.executeBatch();
		}

		//Execute svRangeMapStmt statement to insert into Sv range map table.
		if (svRangeMapStmt != null) {
			int[] insertsvRangeMapCounts = svRangeMapStmt.executeBatch();
		}
		
		//Execute svRangeIdMapStmt to insert update into SOA_SUBSVRIPTION_VERSION table.
		if (svRangeIdMapStmt != null) {
			int[] updatesvRangeMapCounts = svRangeIdMapStmt.executeBatch();
		}
		// To insert record into SOA_PENDING_REQUEST table
		// if pendingTnList is not empty, get the pending column List.
		
		
		if (!pendingTnList.isEmpty()) 
		{
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS," In SVRequestMasterLogger.getSvMapInsert():[ prepare insert statment for SOA_PENDING_REQUEST table].");
			}
			pendingColumnList = getPendingColumn(pendingColumnList);			
			
			// Iterate through the pending TNList.
			for (int k = 0; k < pendingTnList.size(); k++) 
			{
				svPendingInsertStmt.setString(1, tnRefMap.get(
						pendingTnList.get(k)).toString());
				svPendingInsertStmt.setString(2, tnMsgMap.get(
						pendingTnList.get(k)).toString());
				svPendingInsertStmt.setString(3, spidValue);
				svPendingInsertStmt.setString(4, subdomain);
								
				int l =0;

				//Iterate though String and Integer nodes in the input XML message.
				for (int j = 0; j < requestNodeList.size(); j++) 
				{
					String rdata = (String)requestNodeList.get(j);
					//If input request nodes are part of pendingColumn List.
					if (pendingColumnList.contains(rdata)) 
					{		
						if(isPortingTnBelongsToCN(pendingTnList.get(k).toString()) && (requestNodeList.get(j).toString().equals(NNSP_SIMPLEPORTINDICATOR)
								|| requestNodeList.get(j).toString().equals(ONSP_SIMPLEPORTINDICATOR))){
							
							svPendingInsertStmt.setNull(l + 5, java.sql.Types.NULL);
						}else{
							int dataIdx = requestNodeList.indexOf(requestNodeList.get(j));						
							svPendingInsertStmt.setString(l + 5, requestDataList
								.get(dataIdx).toString());
						}
						l++;
					}
				}
								
				//Iterate though Date nodes like NNSPDUEDATE etc.
				for (int m = 0; m < requestNdDtList.size(); m++) 
				{
					String rdata = (String)requestNdDtList.get(m);
					
					// check whether to insert into pending request table.
					if (pendingColumnList.contains(rdata)) {						
						int dataIdx = requestNdDtList.indexOf(requestNdDtList
								.get(m));
						
						svPendingInsertStmt.setTimestamp(l +5,
								formatDate(requestDtList.get(dataIdx)
										.toString()));
						l++;
					}
				}
				svPendingInsertStmt.addBatch();
			}

			// execute the batch to insert into SOA_PENDING_REQUEST table.
			if (svPendingInsertStmt != null) {
				int[] insertPendingCounts = svPendingInsertStmt.executeBatch();
			}
			
		}

	}

	/**
	 * This method tokenizes the input string and return an object for exsisting
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
	 * This method get the Node values from inputXMLMessage parser and 
	 * store in a list.
	 * 
	 * @param inputParser
	 */
	private void getRequestData(XMLMessageParser inputParser) {

		try {
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getRequestData() method.");
			}
			
			if (requestBodyNode.equals("UpstreamToSOA.UpstreamToSOABody.SvModifyRequest.")) {
				requestBodyNode = requestBodyNode + "DataToModify.";
			}

			if (inputParser.exists(requestBodyNode + "LnpType")) {
				requestNodeList.add("LNPTYPE");
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "LnpType"));
			}
			if (inputParser.exists(requestBodyNode + "NewSP")) {
				requestNodeList.add("NNSP");
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "NewSP"));
			}
			if (inputParser.exists(requestBodyNode + "OldSP")) {
				requestNodeList.add("ONSP");
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "OldSP"));
			}
			if (inputParser.exists(requestBodyNode + "NewSPDueDate")) {
				requestNdDtList.add(NNSPDUEDATE);
				requestDtList.add(inputParser.getValue(requestBodyNode
						+ "NewSPDueDate"));
			}
			if (inputParser.exists(requestBodyNode + "OldSPDueDate")) {
				requestNdDtList.add("ONSPDUEDATE");
				requestDtList.add(inputParser.getValue(requestBodyNode
						+ "OldSPDueDate"));
			}
			if (inputParser.exists(requestBodyNode + "PortToOriginal")) {
				requestNodeList.add(PORTINGTOORIGINAL);
				if (inputParser.getValue(requestBodyNode+"PortToOriginal").equals("true") ||
					inputParser.getValue(requestBodyNode+"PortToOriginal").equals("1"))
				{					
					requestDataList.add("1");

				}
				else
				{
					requestDataList.add("0");
				}

			}
			if (inputParser.exists(requestBodyNode + "Lrn")) {
				requestNodeList.add(LRN);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "Lrn"));
			}
			if (inputParser.exists(requestBodyNode + "EndUserLocationValue")) {
				requestNodeList.add(ENDUSERLOCATIONVALUE);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "EndUserLocationValue"));
			}
			if (inputParser.exists(requestBodyNode + "EndUserLocationType")) {
				requestNodeList.add(ENDUSERLOCATIONTYPE);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "EndUserLocationType"));
			}
			if (inputParser.exists(requestBodyNode + "BillingId")) {
				requestNodeList.add(BILLINGID);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "BillingId"));
			}
			if (inputParser.exists(requestBodyNode + "AlternativeEndUserLocationValue")) {
				requestNodeList.add(ALTENDUSERLOCATIONVALUE);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "AlternativeEndUserLocationValue"));
			}
			if (inputParser.exists(requestBodyNode + "AlternativeEndUserLocationType")) {
				requestNodeList.add(ALTENDUSERLOCATIONTYPE);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "AlternativeEndUserLocationType"));
			}
			if (inputParser.exists(requestBodyNode + "AlternativeBillingId")) {
				requestNodeList.add(ALTBILLINGID);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "AlternativeBillingId"));
			}
			if (inputParser.exists(requestBodyNode + "OldSPAuthorization")) {
				requestNodeList.add("AUTHORIZATIONFLAG");
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "OldSPAuthorization"));
			}
			if (inputParser.exists(requestBodyNode + "CauseCode")) {
				requestNodeList.add(CAUSECODE);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "CauseCode"));
			}
			if (inputParser.exists(requestBodyNode + "CustomerDisconnectDate")) {
				requestNdDtList.add(CUSTOMERDISCONNECTDATE);
				requestDtList.add(inputParser.getValue(requestBodyNode
						+ "CustomerDisconnectDate"));
			}
			if (inputParser.exists(requestBodyNode + "EffectiveReleaseDate")) {
				requestNdDtList.add(EFFECTIVERELEASEDATE);
				requestDtList.add(inputParser.getValue(requestBodyNode
						+ "EffectiveReleaseDate"));
			}
			if (inputParser.exists(requestBodyNode + SvType)) {
				requestNodeList.add(SvType);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ SvType));
			}
			if (inputParser.exists(requestBodyNode + AlternativeSPID)) {
				requestNodeList.add(AlternativeSPID);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ AlternativeSPID));
			}
			if (inputParser.exists(requestBodyNode + VoiceURI)) {
				requestNodeList.add(VoiceURI);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ VoiceURI));
			}
			if (inputParser.exists(requestBodyNode + MMSURI)) {
				requestNodeList.add(MMSURI);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ MMSURI));
			}
			if (inputParser.exists(requestBodyNode + PoCURI)) {
				requestNodeList.add(PoCURI);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ PoCURI));
			}
			if (inputParser.exists(requestBodyNode + PRESURI)) {
				requestNodeList.add(PRESURI);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ PRESURI));
			}
			
			if (inputParser.exists(requestBodyNode + SMSURI)) {
				requestNodeList.add(SMSURI);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ SMSURI));
			}
			
			if (inputParser.exists(requestBodyNode + "AccountId")) {
				requestNodeList.add("AccountId");
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "AccountId"));
			}

			if (inputParser.exists(requestBodyNode + "AccountName")) {
				requestNodeList.add("AccountName");
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "AccountName"));
			}
			//Addedd in 5.6.5 release
			if (inputParser.exists(requestBodyNode + "NNSPSimplePortIndicator")) {
				requestNodeList.add(NNSP_SIMPLEPORTINDICATOR);
				requestDataList.add(inputParser.getValue(requestBodyNode+"NNSPSimplePortIndicator"));
			}
			if (inputParser.exists(requestBodyNode + "ONSPSimplePortIndicator")) {
				requestNodeList.add(ONSP_SIMPLEPORTINDICATOR);
				requestDataList.add(inputParser.getValue(requestBodyNode+"ONSPSimplePortIndicator"));
			}
			if (inputParser.exists(requestBodyNode + LastAlternativeSPID)) {
				requestNodeList.add(LastAlternativeSPID);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ LastAlternativeSPID));
			}
			
			//SOA 5.6.8 Changes
			if (inputParser.exists(requestBodyNode + SPCUSTOM1)) {
				requestNodeList.add(SPCUSTOM1);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ SPCUSTOM1));
			}
			if (inputParser.exists(requestBodyNode + SPCUSTOM2)) {
				requestNodeList.add(SPCUSTOM2);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ SPCUSTOM2));
			}
			if (inputParser.exists(requestBodyNode + SPCUSTOM3)) {
				requestNodeList.add(SPCUSTOM3);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ SPCUSTOM3));
			}
			//End SOA 5.6.8 
			
			//End 5.6.5 changes
			if (requestBodyNode.equals("UpstreamToSOA.UpstreamToSOABody.SvCreateRequest.")) {

				if (inputParser.exists(requestBodyNode + "GTTData.ClassDPC")) {
					requestNodeList.add(CLASSDPC);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.ClassDPC"));
				
				}
				if (inputParser.exists(requestBodyNode + "GTTData.ClassSSN")) {
					requestNodeList.add(CLASSSSN);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.ClassSSN"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.LidbDPC")) {
					requestNodeList.add(LIDBDPC);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.LidbDPC"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.LidbSSN")) {
					requestNodeList.add(LIDBSSN);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.LidbSSN"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.IsvmDPC")) {
					requestNodeList.add(ISVMDPC);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.IsvmDPC"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.IsvmSSN")) {
					requestNodeList.add(ISVMSSN);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.IsvmSSN"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.CnamDPC")) {
					requestNodeList.add(CNAMDPC);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.CnamDPC"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.CnamSSN")) {
					requestNodeList.add(CNAMSSN);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.CnamSSN"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.WsmscDPC")) {
					requestNodeList.add(WSMSCDPC);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.WsmscDPC"));
				}
				if (inputParser.exists(requestBodyNode + "GTTData.WsmscSSN")) {
					requestNodeList.add(WSMSCSSN);
					requestDataList.add(inputParser.getValue(requestBodyNode
							+ "GTTData.WsmscSSN"));
				}
			}

			if (inputParser.exists(requestBodyNode + "ClassDPC")) {
				requestNodeList.add(CLASSDPC);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "ClassDPC"));
				
			}
			if (inputParser.exists(requestBodyNode + "ClassSSN")) {
				requestNodeList.add(CLASSSSN);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "ClassSSN"));
			}
			if (inputParser.exists(requestBodyNode + "LidbDPC")) {
				requestNodeList.add(LIDBDPC);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "LidbDPC"));
			}
			if (inputParser.exists(requestBodyNode + "LidbSSN")) {
				requestNodeList.add(LIDBSSN);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "LidbSSN"));
			}
			if (inputParser.exists(requestBodyNode + "IsvmDPC")) {
				requestNodeList.add(ISVMDPC);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "IsvmDPC"));
			}
			if (inputParser.exists(requestBodyNode + "IsvmSSN")) {
				requestNodeList.add(ISVMSSN);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "IsvmSSN"));
			}
			if (inputParser.exists(requestBodyNode + "CnamDPC")) {
				requestNodeList.add(CNAMDPC);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "CnamDPC"));
			}
			if (inputParser.exists(requestBodyNode + "CnamSSN")) {
				requestNodeList.add(CNAMSSN);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "CnamSSN"));
			}
			if (inputParser.exists(requestBodyNode + "WsmscDPC")) {
				requestNodeList.add(WSMSCDPC);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "WsmscDPC"));
			}
			if (inputParser.exists(requestBodyNode + "WsmscSSN")) {
				requestNodeList.add(WSMSCSSN);
				requestDataList.add(inputParser.getValue(requestBodyNode
						+ "WsmscSSN"));
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID)&&
					inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) !=null && 
					inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID) !=""  ) {
				requestNodeList.add("OID" );
				requestDataList.add(inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.OID));
			}
			if (inputParser.exists(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) &&
					inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) !=null && 
					inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID) !="") {
				requestNodeList.add("TransOID");
				requestDataList.add(inputParser.getValue(SOAConstants.REQUEST_HEADER_PATH + "." + SOAConstants.TransOID));
			}
			if (requestBodyNode.equals("UpstreamToSOA.UpstreamToSOABody.SvModifyRequest.DataToModify.")) {
				requestBodyNode = "UpstreamToSOA.UpstreamToSOABody.SvModifyRequest.";
			}
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS,	" Returning from getRequestData() method.");
			}
		} 
		catch (MessageParserException mpe) 
		{
			if( Debug.isLevelEnabled(Debug.MSG_ERROR) ){
			 Debug.log(Debug.MSG_ERROR," Error Occured in getRequestData() method."+mpe);
			}
			mpe.printStackTrace();
		}
	}

	/**
	 * This method will return the INSERT/UPDATE statement for SOA_SUBSCRIPTION_VERSION
	 * and SOA_PENDING_REQUEST table.
	 * @param sqlType
	 * @param newTable
	 * @param requestType
	 * @param status
	 * @return String String of Insert Statement will be returned.
	 * 
	 */
	private String getInsertStmt(boolean sqlType, boolean newTable, String requestType, String status) {

		StringBuffer svInsertBuffer = new StringBuffer();
		StringBuffer svDataBuffer = new StringBuffer();
		ArrayList pendingColumnList = new ArrayList();
		String requestDt = null;
		String requestNd = null;
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS," In SVRequestMasterLogger.getInsertStmt() method.");
		}
		
		if (sqlType && !newTable) {

			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getInsertStmt() method, in if block.");
			}
			
			svInsertBuffer.append(SOAConstants.PORTINGTN_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.SPID_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.REFERENCEKEY_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.CREATEDDATE_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.CUSTOMERID_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.CREATEDBY_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.STATUS_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.LASTMESSAGE_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.LASTREQUESTTYPE_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.LASTREQUESTDATE_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.SUBDOMAIN_COL);			
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.LASTPROCESSING_COL);
			
			for (int i = 0; i < requestNodeList.size(); i++) {

				StringBuffer svInsBuffer = new StringBuffer();
				svInsBuffer.append(", ");
				svInsBuffer.append(requestNodeList.get(i).toString());
				svInsertBuffer.append(svInsBuffer.toString());
				svDataBuffer.append(", ?");
			}
			for (int k = 0; k < requestNdDtList.size(); k++) {
				StringBuffer svInsBuffer = new StringBuffer();
				svInsBuffer.append(", ");
				svInsBuffer.append(requestNdDtList.get(k).toString());
				svInsertBuffer.append(svInsBuffer.toString());
				svDataBuffer.append(", ?");
			}
		
			svInsertBuffer.append(") VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
			svDataBuffer.append(")");
		} 
		else if (newTable) {
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getInsertStmt() method, in else if block.");
			}
			svInsertBuffer.append(SOAConstants.REFERENCEKEY_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.MESSAGEKEY_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.SPID_COL);
			svInsertBuffer.append(", ");
			svInsertBuffer.append(SOAConstants.SUBDOMAIN_COL);
			
			pendingColumnList = getPendingColumn(pendingColumnList);
			
			for (int i = 0; i < requestNodeList.size(); i++) 
			{
				requestNd = (String)requestNodeList.get(i);
				if (pendingColumnList.contains(requestNd)) 
				{
					int nodeIdx = requestNodeList.indexOf(requestNodeList.get(i));
					StringBuffer svInsBuffer = new StringBuffer();
					svInsBuffer.append(", ");
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getInsertStmt() method: NodeListEle["+i+"]is ["+requestNodeList.get(nodeIdx).toString()+"");
					}
					svInsBuffer.append(requestNodeList.get(nodeIdx).toString());
					svInsertBuffer.append(svInsBuffer.toString());
					svDataBuffer.append(", ?");
					
				}
			}
			//
			for (int k = 0; k < requestNdDtList.size(); k++) 
			{
				requestDt = (String)requestNdDtList.get(k);
				
				if (pendingColumnList.contains(requestDt)) 
				{
					
					int nodeIdx = requestNdDtList.indexOf(requestDt);
					
					StringBuffer svInsBuffer = new StringBuffer();
					svInsBuffer.append(", ");
					svInsBuffer.append(requestNdDtList.get(nodeIdx).toString());
					svInsertBuffer.append(svInsBuffer.toString());
					svDataBuffer.append(", ?");
					
				}
			}
			
			svInsertBuffer.append(") VALUES ( ?, ?, ?, ?");
			svDataBuffer.append(")");
		} 
		else {
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getInsertStmt() method, in else if else block.");
			}
			
			svInsertBuffer.append(SOAConstants.PORTINGTN_COL);
			svInsertBuffer.append("= ?, ");
			svInsertBuffer.append(SOAConstants.SPID_COL);
			svInsertBuffer.append("= ?, ");
			svInsertBuffer.append(SOAConstants.CREATEDDATE_COL);
			svInsertBuffer.append("= ?, ");
			svInsertBuffer.append(SOAConstants.CUSTOMERID_COL);
			svInsertBuffer.append("= ?, ");
			svInsertBuffer.append(SOAConstants.CREATEDBY_COL);
			svInsertBuffer.append("= ?, ");
			svInsertBuffer.append(SOAConstants.STATUS_COL);
			svInsertBuffer.append("= ?, ");
			svInsertBuffer.append(SOAConstants.SUBDOMAIN_COL);
			
			// Changes are made for 5.6.4 release, TD#8478
		    ArrayList svUpdateColumnlist = getSVUpdateColumnList();
		    //  Iterate though String and Integer nodes.
		    for (int i = 0; i < svUpdateColumnlist.size(); i++) {
				StringBuffer svInsBuffer = new StringBuffer();
				svInsBuffer.append("= ?, ");
				svInsBuffer.append(svUpdateColumnlist.get(i).toString());
				svInsertBuffer.append(svInsBuffer.toString());
			}
			//TD#8478 end 
			svInsertBuffer.append(" = ? WHERE ");
			svInsertBuffer.append(SOAConstants.REFERENCEKEY_COL);
			svInsertBuffer.append("= ?");			
		}

		String insertUpdateSql = null;		

		// Insert into corrospanding tables based on the condition.
		if (newTable) {
			insertUpdateSql = "INSERT INTO SOA_PENDING_REQUEST ("
					+ svInsertBuffer.toString() + svDataBuffer.toString();
		} else if (sqlType) {
			insertUpdateSql = "INSERT INTO SOA_SUBSCRIPTION_VERSION ("
					+ svInsertBuffer.toString() + svDataBuffer.toString();
		} else {
			insertUpdateSql = "UPDATE SOA_SUBSCRIPTION_VERSION SET "
					+ svInsertBuffer.toString() + svDataBuffer.toString();
		}
		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		 Debug.log(Debug.NORMAL_STATUS,	"insertUpdateSQL:  "+insertUpdateSql);
		}

		
		return insertUpdateSql;				
	}

	/**
	 * This method Parses the date from the input string using the input date
	 * format and change the date format as required format and return the
	 * reformatted string.
	 * 
	 * @param inputTime
	 * @exception ProcessingException.
 	 * @return Timestamp of inputDate.
	 */
	private Timestamp formatDate(String inputDate) throws ProcessingException {

		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
			date = sdf.parse(inputDate);
		} catch (ParseException e) {
			throw new ProcessingException("Source date, [" + inputDate
					+ "], cannot be parsed.");
		}
		return new Timestamp(date.getTime());
	}

	/**
	 * This method prepares the columns for SOA_PENDING_REQUEST table.
	 * 
	 * @param pendingColumnList
 	 * @return ArrayList of columns.
	 */
	private ArrayList getPendingColumn(ArrayList pendingColumnList) {

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getPendingColumn() method.");
		}
		
		pendingColumnList.add(LRN);
		pendingColumnList.add(CLASSDPC);
		pendingColumnList.add(CLASSSSN);
		pendingColumnList.add(CNAMDPC);
		pendingColumnList.add(CNAMSSN);
		pendingColumnList.add(ISVMDPC);
		pendingColumnList.add(ISVMSSN);
		pendingColumnList.add(LIDBDPC);
		pendingColumnList.add(LIDBSSN);
		pendingColumnList.add(WSMSCDPC);
		pendingColumnList.add(WSMSCSSN);
		pendingColumnList.add(BILLINGID);
		pendingColumnList.add(ENDUSERLOCATIONTYPE);
		pendingColumnList.add(ENDUSERLOCATIONVALUE);
		pendingColumnList.add(ALTBILLINGID);
		pendingColumnList.add(ALTENDUSERLOCATIONTYPE);
		pendingColumnList.add(ALTENDUSERLOCATIONVALUE);
		pendingColumnList.add(CAUSECODE);
		pendingColumnList.add(CUSTOMERDISCONNECTDATE);
		pendingColumnList.add(EFFECTIVERELEASEDATE);
		pendingColumnList.add(SvType);		
		pendingColumnList.add(AlternativeSPID);
		pendingColumnList.add(NNSPDUEDATE);
		pendingColumnList.add(VoiceURI);
		pendingColumnList.add(MMSURI);
		pendingColumnList.add(PoCURI);
		pendingColumnList.add(PRESURI);
		pendingColumnList.add(SMSURI);
		pendingColumnList.add(PORTINGTOORIGINAL);
		//Added in 5.6.5 release
		pendingColumnList.add(NNSP_SIMPLEPORTINDICATOR);
		pendingColumnList.add(ONSP_SIMPLEPORTINDICATOR);
		pendingColumnList.add(LastAlternativeSPID);
		//end 5.6.5 changes
		//SOA 5.6.8 Changes
		pendingColumnList.add(SPCUSTOM1);
		pendingColumnList.add(SPCUSTOM2);
		pendingColumnList.add(SPCUSTOM3);
		//End SOA 5.6.8
		return pendingColumnList;
	}
	
	/**
	 * This method prepares the columns to update SOA_SUBSCRIPTION_VERSION table.
	 * 
	 * 
 	 * @return ArrayList of columns.
	 */
	private ArrayList getSVUpdateColumnList() {

	    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS,	" In SVRequestMasterLogger.getSVUpdateColumn() method.");
		}
		
		svUpdateColumnList.add(LRN);
		svUpdateColumnList.add(CLASSDPC);
		svUpdateColumnList.add(CLASSSSN);
		svUpdateColumnList.add(CNAMDPC);
		svUpdateColumnList.add(CNAMSSN);
		svUpdateColumnList.add(ISVMDPC);
		svUpdateColumnList.add(ISVMSSN);
		svUpdateColumnList.add(LIDBDPC);
		svUpdateColumnList.add(LIDBSSN);
		svUpdateColumnList.add(WSMSCDPC);
		svUpdateColumnList.add(WSMSCSSN);
		svUpdateColumnList.add(BILLINGID);
		svUpdateColumnList.add(ENDUSERLOCATIONTYPE);
		svUpdateColumnList.add(ENDUSERLOCATIONVALUE);
		svUpdateColumnList.add(ALTBILLINGID);
		svUpdateColumnList.add(ALTENDUSERLOCATIONTYPE);
		svUpdateColumnList.add(ALTENDUSERLOCATIONVALUE);
		svUpdateColumnList.add(CAUSECODE);
		svUpdateColumnList.add(SvType);		
		svUpdateColumnList.add(AlternativeSPID);
		svUpdateColumnList.add(NNSPDUEDATE);
		svUpdateColumnList.add(VoiceURI);
		svUpdateColumnList.add(MMSURI);
		svUpdateColumnList.add(PoCURI);
		svUpdateColumnList.add(PRESURI);
		svUpdateColumnList.add(SMSURI);
		svUpdateColumnList.add(PORTINGTOORIGINAL);
		svUpdateColumnList.add(LNPTYPE);
		svUpdateColumnList.add(ONSP);
		svUpdateColumnList.add(ONSPDUEDATE);
		svUpdateColumnList.add(NNSP);
		svUpdateColumnList.add(AUTHORIZATIONFLAG);
		//Added in 5.6.5 release
		svUpdateColumnList.add(NNSP_SIMPLEPORTINDICATOR);
		svUpdateColumnList.add(ONSP_SIMPLEPORTINDICATOR);
		svUpdateColumnList.add(LastAlternativeSPID);
		//end 5.6.5 changes
		//SOA 5.6.8 Changes
		svUpdateColumnList.add(SPCUSTOM1);
		svUpdateColumnList.add(SPCUSTOM2);
		svUpdateColumnList.add(SPCUSTOM3);
		//End SOa 5.6.8
		return svUpdateColumnList;
	}
	
	
	
	public ArrayList collapseConsecutiveTNSubRange(ArrayList tnList, boolean checkTNRange) {

		if (tnList == null) {
			return tnList;
		}		
		if(checkTNRange)
		{
			Iterator itr = tnList.iterator();	
			ArrayList splitTNList = new ArrayList();
			while (itr.hasNext())
			{
				String temptn = (String) itr.next();
				if (temptn.length() > 12) {
					String start = temptn.substring(0, 12);
					String end = temptn.substring(13);
					ArrayList tnlist = SOAUtility.getSvRangeTnList(start,
							end);
					itr.remove();
					splitTNList.addAll(tnlist);
					
				}
			}
			if(!splitTNList.isEmpty())
				tnList.addAll(splitTNList);			
				
		}				
		
		Collections.sort(tnList);

		ArrayList subRange = new ArrayList();

		Iterator iter = tnList.iterator();

		String previousNpaNxx = null;

		String currentNpaNxx = null;

		String endStation = null;

		String previousEndStation = null;

		String startTN = null;		

		int previousEndTN = -1;

		int i = 1;

		while (iter.hasNext()) {
			String tn = (String) iter.next();			

			currentNpaNxx = tn.substring(0, 7);

			if (previousNpaNxx != null) {

				endStation = tn.substring(8);				

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
	
	
	/*
	 * return boolean value indicates wether the given portingtn belongs to canadian region or not
	 */
	private boolean isPortingTnBelongsToCN(String tn) throws FrameworkException{
		
		boolean result = false;
		// get Telephone Number List from Context which belongs to CN.
		
		List canadianTnList = (ArrayList) CustomerContext.getInstance().get("canadianTnList");
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS,"TN list belongs to canada" + canadianTnList);
		
		if(canadianTnList != null && !canadianTnList.isEmpty()){
			if(tn.length() == 12){
				// single TN
				if(canadianTnList.contains(tn)){
					//tn belongs to canada
					result = true;
				}
			}else{
				// Range TN
				String startTn = tn.substring(0,12);
				if(canadianTnList.contains(startTn)){
					//tn belongs to canada
					result = true;
				}
			}
		}
		return result;
	}
	
	
	
	
	//	--------------------------For Testing---------------------------------//
	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "D:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug
					.log(
							Debug.ALL_ERRORS,
							"SvRequestMasterLogger: USAGE:  "
									+ " jdbc:oracle:thin:@192.168.198.42:1521:nsoa nsoa nsoa ");

			return;

		}
		try {

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {

			Debug.log(null, Debug.MAPPING_ERROR, ": "
					+ "Database initialization failure: " + e.getMessage());

		}

		SvRequestMasterLogger svRequestMasterLogger = new SvRequestMasterLogger();

		try {
			svRequestMasterLogger.initialize("FULL_NEUSTAR_SOA","SvRequestMasterLogger");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("spid", "1111");
			mob.set("useCustomerId", "ACME_GW");
			mob.set("userId", "example");
			mob.set("subrequest", "SvCreateRequest");
			mob.set("passTN","540-001-2367");
			mob.set("failedTN",	"");
			mob.set("inputMessage", "<?xml version=\"1.0\"?>"
					+ "<SOAMessage>"
					+ "<UpstreamToSOA>"
					+ "<UpstreamToSOAHeader>"
					+ "<InitSPID value=\"1111\" />"
					+ "<DateSent value=\"09-01-2006-035600AM\" />"
					+ "<Action value=\"submit\" />"
					+ "</UpstreamToSOAHeader>"
					+ "<UpstreamToSOABody>"
					+ "<SvCreateRequest>"
					
					+ "<Subscription> "
					
					+ "<Tn value=\"540-001-2367\" />"
										
					+ "</Subscription>"		
					
					+ "<LnpType value=\"lspp\" />"
					+ "<Lrn value=\"9874563210\" />"
					+ "<NewSP value=\"A111\" />"
					+ "<OldSP value=\"1111\" />"
					+ "<NewSPDueDate value=\"08-12-2006-073800PM\" />" 
					
					+ "<GTTData> "					
					+ "<ClassDPC value=\"001255255\" />"
					+ "<ClassSSN value=\"000\" />"
					+ "<LidbDPC value=\"001255255\" />"
					+ "<LidbSSN value=\"000\" />"
					+ "</GTTData>"										

					+ "</SvCreateRequest>" 
					+ "</UpstreamToSOABody>" 
					+ "</UpstreamToSOA>" 
					+ "</SOAMessage>");
			
			svRequestMasterLogger.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		}

	}
}
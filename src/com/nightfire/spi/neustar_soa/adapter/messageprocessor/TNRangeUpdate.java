/**
 * The purpose of this processor is to update SV record(s) into 
 * SOA_SUBSCRIPTION_VERSION table and generate the two XML outputs 
 * containing ReferenceKey values.
 *
 * @author Abhijit Talukdar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is
 * considered to be confidential and proprietary to NeuStar.
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.framework.util.StringUtils;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.framework.util.FrameworkException;
 * @see com.nightfire.framework.resource.ResourceException;
 * @see com.nightfire.framework.db.DBConnectionPool;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.db.PersistentProperty;
 * @see com.nightfire.framework.db.DBLOBUtils;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.spi.common.driver.Converter;
 * @see com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
 * @see	com.nightfire.spi.neustar_soa.utils.SOAConstants;
 * @see com.nightfire.framework.message.parser.xml.XMLMessageParser;
 * @see com.nightfire.spi.neustar_soa.utils.SOAUtility;
 * @see com.nightfire.framework.util.CustomerContext;
 */

/**
 
 Revision History
 ---------------
 
 Rev#	Modified By			Date			Reason
 ----- ----------- ---------- --------------------------        
 1		Abhijit			06/02/2004		Created
 2		Abhijit			06/07/2004		Review Comments incorporated.
 3		Jaganmohan		06/17/2004		Modifyed code to incorporate Database changes and Matt comments.
 4		Jaganmohan		06/18/2004		Review comments incorporated.
 5		Abhijit			07/29/2004		Formal review comments incorporated.
 6		Abhijit			09/21/2004		Modify to support uncontiguoes SV Id range.											
 7		Ashok			09/30/2004		RegionId added in DB Query ,If query's
 8 		Sreedhar   		03/28/2005		Modifyed to support Billing requirements 
 9		Manoj			01/12/2007		Modified for the batch updates.
10		Abhijit			01/17/2007		Modified for the setting date like
										activation date etc.
11		Shan			05/03/2007		Comments incorporated
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.Converter;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class TNRangeUpdate extends DBMessageProcessorBase {
	
	/**
	 * Name of the oracle table requested
	 */
	private String tableName = null;
	
	private String separator = null;
	
	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;
	
	/**
	 * The value of start SVID
	 */
	private String startSVIDValue = null;
	
	/**
	 * The value of end SVID
	 */
	private String endSVIDValue = null;
	
	/**
	 * The value of SPID
	 */
	private String spidValue = null;		
	
	private List columns = null;
	
	/**
	 * Context location of REFERENCE key XML
	 */
	private String keyXMLLocation = null;
	
	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;
	
	/**
	 * Input message location
	 */
	private String inputMessage = null;
	
	/**
	 * Context location of notification
	 */
	private String notification = null;

	/**
	 * Context location of NPAC notification
	 */
	private String npacNotification = null;
	
	/**
	 *  listDataFlag
	 */
	private boolean listDataFlag = false;
	
	/**
	 * 	This variable used to get location of Region ID.
	 */
	private String regionIdLoc = null;
	
	/**
	 * 	This variable used to get value of Region ID.
	 */
	private String regionId = null;
	
	/**
	 * 	This variable used to get location of NPA.
	 */
	private String npaLoc = null;
	
	/**
	 * 	This variable used to get value of NPA.
	 */
	private String npaValue = null;
	
	/**
	 * 	This variable used to get location of NXX.
	 */
	private String nxxLoc = null;
	
	/**
	 * 	This variable used to get value of NXX.
	 */
	private String nxxValue = null;
	
	/**
	 * 	This variable used to get location of startTN.
	 */
	private String startTNLoc = null;
		
	/**
	 * 	This variable used to get value of startTN.
	 */
	private String startTNValue = null;
	
	/**
	 * 	This variable used to get location of startTN.
	 */
	private String endTNLoc = null;
	
	/**
	 * 	This variable used to get value of startTN.
	 */
	private String endTNValue = null;
	
	/**
	 * 	This variable used to get value of OIDTransOIDPOPULATION_FLAG.
	 */
	private String oIdTransOID_Flag = null;
	

	
	
	/**
	 * Context location of Range REFERENCE key XML
	 */
	private String rangeKeyXMLLocation = null;

	/**
 	 * This variable used to get the value of portingTN.
 	 */
 	private String portingTN = null;
 	
 	private String oID = null;
 	
 	private String transOID = null;
 	
 	private String authFlag = null;
 	private String authFlagValue = null;
 	
 	private String authDateValue = null;
 	
 	private String useInvokeId = null;
 	
 	private Boolean useInvokeIdValue = false;
	/**
 	 * This variable used to get the value of last Attribute change InvokeId from db.
 	 */
 	private long lastAttributeChangeInvokeId = 0;
 	
 	/**
 	 * This variable used to get the value of last Status change InvokeId from db.
 	 */
 	private long lastStatusChangeInvokeId = 0;
 	
	/**
 	 * This variable used to get the value of InvokeId from the request XML.
 	 */
 	private long incomingInvokeId = 0;

 	/**
 	 * This variable used to add the value of Attribute Change Notification Reference Keys.
 	 */
 	ArrayList atbList = new ArrayList();;

	/**
	 *  Out of Sequence notification.
	 */
	private String outOfSequence = "false";

	private String outOfSequenceLocation = null;
	
	private HashSet<String> oidSet = new HashSet<String>();
	
	private HashSet<String> transOidSet = new HashSet<String>();
	
	private  List <TnOIDTransOID> oIDTransOidTnlst = new ArrayList<TnOIDTransOID>();	
	
	/**
 	 * This variable used to get the value of Attribute Change Notification Timestamp.
 	 */
 	private Timestamp attributeTimestamp = null;

	private long attributeLTimestamp = 0;

	/**
 	 * This variable used to get the value of Status Change Notification Timestamp.
 	 */
 	private Timestamp statusTimestamp = null;

	private long statusLTimestamp = 0;
	
	/**
	 * This variable used to verify whether incoming notification comes from BDD or not.
	 */
	private boolean is_bddNotification = false;
	
	/**
	 * This variable used to verify whether incoming notification came from Recovery or not.
	 */
	private boolean is_recoveryReplyNotification = false;

	//TD #13522 Changes
	
	private boolean is_subdomainId_present = false;
	
	private boolean is_altSpid_present = false;
	
	private String subdomainIdValue = null;
	
	private String customerIdValue = null;
	
	
	public static final String SvAttr_AltSPID_Node = SOAConstants.SOA_TO_UPSTREAM_BODY_PATH + 
					".SvAttributeChangeNotification.ModifiedData.AlternativeSPID";
	
	public static final String GET_SUBDOMAINID_QUERY = "SELECT SUBDOMAINID FROM CUSTOMER_LOOKUP WHERE " +
			"TPID = ? AND CUSTOMERID = ? AND SUBDOMAINID IN ( SELECT SUBDOMAINID FROM SOA_SUBDOMAIN_ALTSPID" +
			" WHERE CUSTOMERID = ? AND ALTERNATIVESPID = ? )";
	
	//TD #13522 Changes
	
	/**
	 * Constructor.
	 */
	public TNRangeUpdate() {
		
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ))
			Debug.log(Debug.OBJECT_LIFECYCLE, "Creating TNRangeUpdate message-processor.");
		
		columns = new ArrayList();
	}
	
	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param key Property-key to use for locating initialization properties.
	 * @param type Property-type to use for locating initialization properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type) throws ProcessingException {
		
		// Call base class method to load the properties.
		super.initialize(key, type);
		
		// Get configuration properties specific to this processor.
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "TNRangeUpdate: Initializing...");
		
		StringBuffer errorBuffer = new StringBuffer();
		
		tableName = getRequiredPropertyValue(SOAConstants.TABLE_NAME_PROP,
				errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Database table to update to is ["+ tableName + "].");
		
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
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG )){
			
			Debug.log(Debug.SYSTEM_CONFIG,"Logger will participate in overall driver transaction? ["
					+ usingContextConnection + "].");
		}
		separator = getPropertyValue(SOAConstants.LOCATION_SEPARATOR_PROP);
		
		if (!StringUtils.hasValue(separator)) {
			separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location separator token [" + separator	+ "].");
		
		startSVIDValue = getPropertyValue(SOAConstants.START_SVID_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of Start SVID is [" + startSVIDValue + "].");
		
		endSVIDValue = getPropertyValue(SOAConstants.END_SVID_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of End SVID is [" + endSVIDValue + "].");
		
		oIdTransOID_Flag = getPropertyValue(SOAConstants.OIDTransOIDPOPULATION_FLAG);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of OIDTransOIDPOPULATION_FLAG is [" + oIdTransOID_Flag	+ "].");
			
		spidValue = getRequiredPropertyValue(SOAConstants.SPID_PROP,
				errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of SPID is [" + spidValue + "].");
		
		keyXMLLocation = getRequiredPropertyValue(
				SOAConstants.REFERENCEKEY_OUT_LOC_PROP, errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of end TN is [" + keyXMLLocation + "].");
		
		rangeKeyXMLLocation = getPropertyValue(
				SOAConstants.RANGE_REFERENCEKEY_OUT_LOC_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of Range Key XML is ["+ rangeKeyXMLLocation + "].");
		regionIdLoc = getPropertyValue(SOAConstants.REGION_ID_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of region ID value ["
				+ regionIdLoc + "].");				
		
		inputMessage = getRequiredPropertyValue(
				SOAConstants.INPUT_XML_MESSAGE_LOC_PROP, errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of inputMessage is ["
				+ inputMessage + "].");
		
		notification = getPropertyValue(SOAConstants.NOTIFICATION_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of notification is ["
				+ notification + "].");

		npacNotification = getPropertyValue(SOAConstants.NPAC_NOTIFICATION_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of npac notification is ["
				+ npacNotification + "].");

		outOfSequenceLocation = getPropertyValue(SOAConstants.OUTOFSEQUENCE_LOC);

		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of outOfSequenceLocation is ["
				+ outOfSequenceLocation + "].");
		
		npaLoc = getPropertyValue(SOAConstants.NPA);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of NPA is ["
				+ npaLoc + "].");
		
		nxxLoc = getPropertyValue(SOAConstants.NXX);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of NXX is ["
				+ nxxLoc + "].");
		
		startTNLoc = getPropertyValue(SOAConstants.START_TN_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of START_TN is ["
				+ startTNLoc + "].");
		
		endTNLoc = getPropertyValue(SOAConstants.END_TN_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of END_TN is ["
				+ endTNLoc + "].");
		
		authFlag = getPropertyValue(SOAConstants.AUTHORIZATION_FLAG);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of AUTHORIZATION_FLAG is ["
				+ authFlag + "].");
		
		useInvokeId = getPropertyValue(SOAConstants.USE_INVOKEID_PROP);
		
		if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "Value of USE INVOKEID property is ["+useInvokeId+"]");
		
		String colName = null;
		
		String colType = null;
		
		String dateFormat = null;
		
		String defaultValue = null;
		
		String optional = null;
		
		String location = null;
		
		ColumnData cd = null;
		
		// Loop until all column configuration properties have been read ...
		for (int i = 0; true; i++) {
			colName = getPropertyValue(PersistentProperty.getPropNameIteration(
					SOAConstants.COLUMN_PREFIX_PROP, i));
			
			// If we can't find a column name, we're done.
			if (!StringUtils.hasValue(colName)) {
				break;
			}
			
			colType = getPropertyValue(PersistentProperty.getPropNameIteration(
					SOAConstants.COLUMN_TYPE_PREFIX_PROP, i));
			
			dateFormat = getPropertyValue(PersistentProperty
					.getPropNameIteration(SOAConstants.DATE_FORMAT_PREFIX_PROP,
							i));
			
			location = getPropertyValue(PersistentProperty
					.getPropNameIteration(SOAConstants.LOCATION_PREFIX_PROP, i));
			
			defaultValue = getPropertyValue(PersistentProperty
					.getPropNameIteration(SOAConstants.DEFAULT_PREFIX_PROP, i));
			
			optional = getRequiredPropertyValue(
					PersistentProperty.getPropNameIteration(
							SOAConstants.OPTIONAL_PREFIX_PROP, i), errorBuffer);
			
			try {
				// Create a new column data object and add it to the list.
				cd = new ColumnData(colName, colType, dateFormat, location,
						defaultValue, optional);
				
				if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
					Debug.log(Debug.SYSTEM_CONFIG, cd.describe());
				}
				
				columns.add(cd);
			} catch (FrameworkException e) {
				throw new ProcessingException(
						"ERROR: Could not create column data description:\n"
						+ e.toString());
			}
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Maximum number of columns to insert ["
				+ columns.size() + "].");
		
		// If any of required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();
			
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS, errMsg);
			
			throw new ProcessingException(errMsg);
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "TNRangeUpdate: Initialization done.");
	} //end of initialize method
	
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
	public NVPair[] process(MessageProcessorContext mpContext,
			MessageObject inputObject) throws MessageException,
			ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside process method");
		
		if (inputObject == null) {
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, this.getClass() + " obj is null");
			return null;
		}
		
		this.mpContext = mpContext;
		
		this.inputObject = inputObject;
		
		
		Connection dbConn = null;
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );		
		
		// if startSVIDValue is configured
		if (startSVIDValue != null) {
			
			// Get start SVID value from context
			startSVIDValue = getValue(startSVIDValue);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of Start SVID is ["
										+ startSVIDValue + "]." );
		}
		// if authFlage is configured
		
		if (StringUtils.hasValue(authFlag)) {

			// Get authFlag value from context
			authFlagValue = getValue(authFlag);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of authFlag is ["
										+ authFlagValue + "]." );
		}
			
		authDateValue = getValue(SOAConstants.AUTHORIZATION_Date);
		
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Value of authDate is ["
										+ authDateValue + "]." );
		
		// if endSVIDValue is configured
		if (endSVIDValue != null) {

			// Get End SVID value from context
			endSVIDValue = getValue(endSVIDValue);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of End SVID is ["
										+ endSVIDValue + "]." );
		}
		
		// if startSVIDValue is configured and endSVIDValue is not configured
		if (startSVIDValue != null && endSVIDValue == null) {

			// assign startSVID to endSVID value
			endSVIDValue = startSVIDValue;
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of End SVID is ["
										+ endSVIDValue + "]." );
		}	
		
		// Get the SPID value from context
		spidValue = getValue(spidValue);
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is ["
										+ spidValue + "]." );

		
		// if regionIdLoc is configured
		if (regionIdLoc != null) {
			
			// Get the REGIONID value from context
			regionId = getValue(regionIdLoc);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log(Debug.SYSTEM_CONFIG, "Region ID value :" + "[ " + regionId + " ].");			
		}
		
		if(npaLoc != null) {
//			 Get the NPA value from context
			npaValue = getValue(npaLoc);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log(Debug.SYSTEM_CONFIG, "NPA value :" + "[ " + npaValue + " ].");
			
		}
		
		if(nxxLoc != null) {
//			 Get the NXX value from context
			nxxValue = getValue(nxxLoc);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log(Debug.SYSTEM_CONFIG, "NXX value :" + "[ " + nxxValue
				+ " ].");
			
		}
		
		if(startTNLoc != null) {
//			 Get the START_TN value from context
			startTNValue = getValue(startTNLoc);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log(Debug.SYSTEM_CONFIG, "START_TN value :" + "[ " + startTNValue + " ].");
			
		}
		
		if(endTNLoc != null) {
//			 Get the END_TN value from context
			endTNValue = getValue(endTNLoc);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log(Debug.SYSTEM_CONFIG, "END_TN value :" + "[ " + endTNValue + " ].");
			
		}


		// if NOTIFICATION_TYPE is configured
		if (notification != null) {

			// Get the notification value from context
			notification = getValue(notification);
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of Notification is ["
										+ notification + "]." );			
		}

		// if NPAC_NOTIFICATION_TYPE is configured
		if (npacNotification != null) {

			// Get the NPAC Notification value from context
			npacNotification = getValue(npacNotification);
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of NPAC Notification is ["
										+ npacNotification + "]." );
		}
		
		if(useInvokeId != null && useInvokeId.equalsIgnoreCase("true")){
			useInvokeIdValue = true;
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log( Debug.SYSTEM_CONFIG, "Value of USE INVOKEID PROP is ["
										+ useInvokeIdValue + "]." );
			
		long startSVID = -1;
		
		long startLoop = -1;
	
		long endLoop = -1;
		
		
		try {
			if (startSVIDValue != null && endSVIDValue != null) {
				
				startSVID = startLoop = Long.parseLong(startSVIDValue);
				
				endLoop = Long.parseLong(endSVIDValue);
				
			}
						
			
		} catch (NumberFormatException nbrfex) {
			
			throw new MessageException("Invalid start TN/SVID"
					+ " & end TN/SVID: " + nbrfex);
			
		}
		
		//get the incoming invokeId from context.
		incomingInvokeId = Long.parseLong(getValue(SOAConstants.INVOKEID));
		
		is_bddNotification =  exists(SOAConstants.IS_BDDNOTIFICATION, mpContext, inputObject);
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				
			Debug.log(Debug.MSG_STATUS,"Value fetch for IS_BDDNOTIFICATION is ["+is_bddNotification+"]" );
				
		}
		
		is_recoveryReplyNotification =  exists(SOAConstants.IS_RECOVERYREPLYNOTIFICATION, mpContext, inputObject);
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				
			Debug.log(Debug.MSG_STATUS,"Value fetch for IS_RECOVERYREPLYNOTIFICATION is ["+is_recoveryReplyNotification+"]" );
				
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Value of Incoming InvokeId is ["
									+ incomingInvokeId + "]." );	
		
		try {
			// Get a database connection from the appropriate
			//location - based
			// on transaction characteristics.
			if (usingContextConnection) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					Debug.log(Debug.MSG_STATUS, "Database logging is"
							+ " transactional, so getting "
							+ "connection from context.");
				}
				
				dbConn = mpContext.getDBConnection();
			} else {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					Debug.log(Debug.MSG_STATUS, "Database logging is not "
							+ "transactional, so getting"
							+ " connection from NightFire" + " pool.");
				}

				dbConn = DBConnectionPool.getInstance(true).acquireConnection();
			}
			
			if (dbConn == null) {
				// Throw the exception to the driver.
				throw new ProcessingException("DB "
						+ "connection is not available");
			}
			
		} catch (FrameworkException e) {
			String errMsg = "ERROR: TNRangeLogger:"
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

		// Update the SV Object table and generate the output XML containing referenceKey
		genarateXMLDocuments(  dbConn , startSVID, 
				startLoop, endLoop);
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		return (formatNVPair(inputObject));
	}	
	
	/**
	 * This method is used to get Svid List value from the input message if
	 * input message comes with TnSvidList node or with the StartSVId and EndSVId
	 *
	 * @param  startSVID  Start Subscription Version Id
	 * @param  startLoop  Start loop
	 * @param  endLoop    End loop
	 * @param  parser  as XMLMessageParser
	 *
 	 * @return  SvIdList.
	 */

	private ArrayList getSvidList(long startSVID, long startLoop ,long endLoop, XMLMessageParser parser)
	{
		ArrayList svidList = new ArrayList();
		String node = null;
		int tnSvIdCount = 0;

		String rootNode = "SOAToUpstream.SOAToUpstreamBody." + notification
							+ ".Subscription.TnSvIdList";
		try
		{
			// if the notification contains TnSvIdList node
			if (parser.exists(rootNode)) {
						
				tnSvIdCount = parser.getChildCount(rootNode);
			}		
			// If the notification conatins more than one TnSvId node
			if (tnSvIdCount > 1)
			{
				 // Add all the SVId in SVID list
				 for (int i = 0; i < tnSvIdCount; i++)
				 {
					 node = "SOAToUpstream.SOAToUpstreamBody."
										+ notification 
										+ ".Subscription.TnSvIdList.TnSvId("
										+ i + ")" + ".SvId";

					if (parser.exists( node ))
					{
						// Get the value of SVID
						startSVID = Long.parseLong(parser.getValue( node ));
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: startSVID in listDataFlag :\n"+ startSVID);
						
						svidList.add(Long.valueOf(startSVID));
					}
				 }			
			}
			// If the notification contains StartSVId and EndSVId
			else {

				// Add all the SVId in SVID list
				for (long i = startLoop; i <= endLoop; i++) {
							
					svidList.add(Long.valueOf(startSVID));

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: startSVID :\n"+ startSVID);
						
					startSVID++;
				}
				
			}
		}
		catch( MessageException re )
		{		
				Debug.log( Debug.ALL_ERRORS, re.getMessage() );
		}

		return svidList;
	}
	
	/**
	 * Update the SV Objects table and generate the output XML conating referenceKey
	 *
	 * @param  dbConn  Connection Object
	 * @param  startSVID  Start Subscription Version Id
	 * @param  startLoop  Start loop
	 * @param  endLoop    End loop
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	
	
	private void genarateXMLDocuments(Connection dbConn,  
			long startSVID, long startLoop ,long endLoop) throws 
			MessageException, ProcessingException
	{			
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside genarateXMLDocuments method");
		// generate a REFERENCE Key XML
		XMLMessageGenerator referenceKeyXML = new XMLMessageGenerator(
				SOAConstants.KEY_ROOT);
		
		// generate a Range REFERENCE Key XML
		
		XMLMessageGenerator rangeReferenceKeyXML = new XMLMessageGenerator(
				SOAConstants.KEY_ROOT);
		
		boolean isRangeXMLGenarated= false;
		
		PreparedStatement dbStatement = null;

		PreparedStatement updateStatement = null;		

		Statement stmt = null;
		
		ResultSet rs = null;

		ArrayList svList = new ArrayList();
		
		long referenceKey = -1;
		
		String status = null;
		
		Date activationDate = null;
		
		String onsp = null;

		String nnsp = null;

		String lastRequestType = null;		

		try {						
			
			// get input XML from context		
			Document doc = (Document) super.getDOM(inputMessage, mpContext,
					inputObject);
			
			XMLMessageParser parser = new XMLMessageParser(doc);
			
			// if it is a notification
			if (notification != null) {								

				// if notification type is Activate or PortToOriginal notification
				if ( notification.equals(SOAConstants.SV_ACTIVATE_NOTIFICATION) 
					 || notification.equals(SOAConstants.SV_PTO_NOTIFICATION) )
				{
					// set the Activation date in context
					super.set(SOAConstants.ACTIVATION_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));
				}

				// if nitification type is Cancel notification
				if ( notification.equals(SOAConstants.SV_CANCEL_NOTIFICATION) )
				{
					// set the Canceled date in context
					super.set(SOAConstants.CANCELLATION_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));

					// set the DisconnectCompleteDate in context
					super.set(SOAConstants.DISCONNECT_COMPLETE_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));
				}

				// if nitification type is Disconnect notification
				if ( notification.equals(SOAConstants.SV_DISCONNECT_NOTIFICATION) )
				{
					// set the DisconnectCompleteDate in context
					super.set(SOAConstants.DISCONNECT_COMPLETE_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));

					// set the StatusOldDate in context
					super.set(SOAConstants.STATUS_OLD_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));
				}

				// if nitification type is AttributeChange notification
				if ( notification.equals(SOAConstants.SV_ATR_CHANGE_NOTIFICATION) )					 
				{
					
					// set the ObjectModifiedDate in context
					super.set(SOAConstants.OBJECT_MODIFIED_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));
					
						//FIX TD 13522(Fetch SubdomainId value if AltSPID present in SvAttributeChangeNotification)
						
					if(parser.exists(SvAttr_AltSPID_Node)){
						String altSpid = parser.getValue(SvAttr_AltSPID_Node);
					
						if(StringUtils.hasValue(altSpid)){
							
							is_altSpid_present = true;
							if(exists("@context.CustomerId.CUSTOMERID",mpContext,inputObject)) 
								customerIdValue = getValue("@context.CustomerId.CUSTOMERID");
								
							//get subdomainId value from DB
							subdomainIdValue = getSubDomainIdUsingAltSpid(dbConn, altSpid, customerIdValue);
							
							if(StringUtils.hasValue(subdomainIdValue)){
								is_subdomainId_present = true;
							}
						}
					}
				}

				// if nitification type is StatusChange notification
				if ( notification.equals(SOAConstants.SV_STS_CHANGE_NOTIFICATION) )					 
				{
					// if status value in context equals to canceled state
					if ( getValue(SOAConstants.STATUS_CONTEXT).equals(SOAConstants.CANCELED_STATUS) )
					{
						// set the CanceledDate in context
						super.set(SOAConstants.CANCELLATION_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));

						// set the DisconnectCompleteDate in context
						super.set(SOAConstants.DISCONNECT_COMPLETE_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));
					}
				}

				// if nitification type is SvDonor notification
				if ( notification.equals(SOAConstants.SOA_SV_DONOR_NOTIFICATION) )					 
				{
					// set the DisconnectCompleteDate in context
					super.set(SOAConstants.DISCONNECT_COMPLETE_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));

					// set the StatusOldDate in context
					super.set(SOAConstants.STATUS_OLD_DATE, mpContext, inputObject,
						getValue(SOAConstants.DATESENT));

					// set the Status in context
					super.set(SOAConstants.STATUS_CONTEXT, mpContext, inputObject,
						SOAConstants.OLD_STATUS);

				}

				// If it is a npacNotification
				if (npacNotification != null)
				{
					// if nitification type is VersionAttributeValueChange notification
					if ( npacNotification.equals(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION ) )
					{
						// set the AttributeCahngeTimestamp in context
						super.set(SOAConstants.ATTRIBUTE_TIMESTAMP, mpContext, inputObject,
							getValue(SOAConstants.DATESENT));
					}

					// if nitification type is VersionStatusAttributeValueChange notification
					if ( npacNotification.equals(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION ) )
					{
						// set the StatusChangeTimestamp in context
						super.set(SOAConstants.STATUS_TIMESTAMP, mpContext, inputObject,
							getValue(SOAConstants.DATESENT));
					}
				}

			}

			// Reset the values in cases where we're logging again.
			resetColumnValues();
			
			// Extract the column values from the arguments.
			extractMessageData(mpContext, inputObject);			
			
			int j = 0;						

			// Get the SVID list
			svList = getSvidList(startSVID, startLoop, endLoop, parser);

			int tnCount =svList.size();

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: tnCount :\n"+ tnCount);
			
			StringBuffer mainQuery = new StringBuffer();
		 
			int i = 0;

			// Construct the query that needs to execute to select the referenckey key. If the request/response contains
			// more than 1000 SVIDs, multiple query will be constructed for each 1000 SvId and same will joined using UNION operator.
			// This has been done since ORACLE doesn't support IN operator more than 1000 items.

			while (i < tnCount)
			{			
				int k = 1;

				StringBuffer  svidValue = new StringBuffer();

				// This loop can be used to append 1000 SvId's to the stringbuffer
				while (k <= 1000 && i <= tnCount-1 )
				{
					 svidValue.append("'");
					 svidValue.append(svList.get(i));
					
					// this condition used to add 1000 SvId's to the list
					if ( k < 1000 && i != tnCount-1)				
						 svidValue.append("',");
					else
						 svidValue.append("'");

					i++;
					k++;
					
				}
				
				// This SELECT query used for 1000 SvId's
				StringBuffer tnQuery = new StringBuffer("(select /*+ index(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ referencekey, portingTN, STATUS, nnsp, onsp, LASTREQUESTTYPE, LASTATTRIBUTECHANGEINVOKEID, LASTSTATUSCHANGEINVOKEID, ATTRIBUTECHANGETIMESTAMP, STATUSCHANGETIMESTAMP,ACTIVATIONDATE, OID,TransOID from soa_subscription_version where  SVID in ("+ svidValue+ " )  AND spid = '"+spidValue+"' ");

				// if regionIdLoc is configured, then append to where condition
				if (regionIdLoc != null)
				{
					tnQuery.append(" AND REGIONID = '"+regionId+"' )");
				}
				else
				{
					 tnQuery.append(" )");
				}
				
				// append the tnQuery statement to the main query for batch processing
				mainQuery.append(tnQuery);

				// UNION is added to the mainQuery for batch processing
				if (i <= tnCount-1)
				{
					mainQuery.append(" UNION ");
				}
										
			 } // end of construction of query

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: mainQuery :\n"+ mainQuery.toString());
				
			stmt = dbConn.createStatement();

			rs = stmt.executeQuery(mainQuery.toString());

			boolean firstSv = true;
			TnOIDTransOID tnoid;
			// Process all the record one by one
			while (rs.next()) {

				referenceKey = rs.getLong(SOAConstants.REFERENCEKEY_COL);
				portingTN = rs.getString(SOAConstants.PORTINGTN_COL);
				status = rs.getString(SOAConstants.STATUS_COL);
				onsp = rs.getString(SOAConstants.ONSP_COL);
				nnsp = rs.getString(SOAConstants.NNSP_COL);
				lastRequestType = rs.getString(SOAConstants.LASTREQUESTTYPE_COL);
				lastAttributeChangeInvokeId = rs.getLong(SOAConstants.ATTRIBUTECHANGEINVOKEID_COL);
				lastStatusChangeInvokeId = rs.getLong(SOAConstants.STATUSCHANGEINVOKEID_COL);	
				attributeTimestamp = rs.getTimestamp(SOAConstants.ATTRIBUTECHANGETIMESTAMP_COL);
				statusTimestamp = rs.getTimestamp(SOAConstants.STATUSCHANGETIMESTAMP_COL);				
				activationDate =  rs.getDate(SOAConstants.ACTIVATION_DATE_COL);				
								
				oID = rs.getString(SOAConstants.OID_COL);
				transOID=rs.getString(SOAConstants.TransOID_COL);
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, this.getClass() + "Populating OID and transOID:");
					Debug.log(Debug.MSG_STATUS, this.getClass() + "OID:["+oID+"]");
					Debug.log(Debug.MSG_STATUS, this.getClass() + "TransOID:["+transOID+"]");
				}					
				oidSet.add(oID);
				transOidSet.add(transOID);
				tnoid = new TnOIDTransOID(portingTN,oID,transOID);
				oIDTransOidTnlst.add(tnoid);
				
				if (attributeTimestamp != null)
				{
					attributeLTimestamp = attributeTimestamp.getTime();
				}

				if (statusTimestamp != null)
				{
					statusLTimestamp = statusTimestamp.getTime();
				}
				boolean flag = true;

				SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
				
				java.util.Date dsent = df.parse(getValue(SOAConstants.DATESENT));

				long dtime = dsent.getTime();

				if ( npacNotification != null )
				{
					// Notification type is VersionAttributeValueChangeNotification
					if ( npacNotification.equals(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION ) )
					{
						if(is_bddNotification || is_recoveryReplyNotification){
							
							// if dateSent time is lessthan attributeTimestamp, checking for OutOfSync notification
							if (attributeLTimestamp > dtime)
							{
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
									
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: attributeLTimestamp :\n"+ attributeLTimestamp);
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: dtime :\n"+ dtime);
								}

								// add the portingTN to attributeList
								atbList.add(portingTN);

								flag = false;
							}
							
						}else{
						
							// if incoming InvokeId is less than attributeChangeInvokeId, checking for OutOfSync notification
							if (lastAttributeChangeInvokeId > incomingInvokeId)
							{
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
								
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: LastAttributeChangeInvokeId :\n"+ lastAttributeChangeInvokeId);
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: Incoming InvokeID :\n"+ incomingInvokeId);
								}

								// add the portingTN to attributeList
								atbList.add(portingTN);

								flag = false;
							}
						}
					}
					
					// Notification type is VersionStatusAttributeValueChangeNotification
					if ( npacNotification.equals(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION ) )
					{
						
						if(is_bddNotification || is_recoveryReplyNotification){
							
							// if dateSent time is lessthan statusTimestamp, checking for OutOfSync notification
							if (statusLTimestamp > dtime)
							{
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
									
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: statusLTimestamp :\n"+ attributeLTimestamp);
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: dtime :\n"+ dtime);
								}
								flag = false;
							}	
							
						}else{
							// if incoming InvokeId  is less than statusChangeInvokeId, checking for OutOfSync notification
							if (lastStatusChangeInvokeId > incomingInvokeId)
							{
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
								
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: LastStatusChangeInvokeId :\n"+ lastStatusChangeInvokeId);
									Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: Incoming InvokeID :\n"+ incomingInvokeId);
								}
								flag = false;
							}
						}
					}
				}

				if ( flag) {
					// if notification type is SvStatusChangeNotification 
					if ( notification.equals(SOAConstants.SV_STS_CHANGE_NOTIFICATION ) )
					{							
						// if value of status in context equal to pending status 
						if ( getValue(SOAConstants.STATUS_CONTEXT).equals(SOAConstants.PENDING_STATUS) )
						{
							// If DB stattus value is conflict
							if ( status.equals(SOAConstants.CONFLICT_STATUS) )
							{
								if (lastRequestType != null && lastRequestType.equals(SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST))
								{	
									
									super.set(SOAConstants.CAUSE_CODE_VALUE, mpContext, inputObject,
									" ");
									
									// if SPID is New SP
									if (spidValue.equals(nnsp))
									{
										// set NewSPConflictResolutionDate in context
										super.set(SOAConstants.NEW_SP_RESOLUTION_DATE, mpContext, inputObject,
											getValue(SOAConstants.DATESENT));
										
									}
									// if SPID is Old SP
									else if (spidValue.equals(onsp))
									{
										// set OldSPConflictResolutionDate in context
										super.set(SOAConstants.OLD_SP_RESOLUTION_DATE, mpContext, inputObject,
											getValue(SOAConstants.DATESENT));
										
									}
								}
								// if last request type is otherthan SvRemoveFromConflict
								else {
									
									super.set(SOAConstants.CAUSE_CODE_VALUE, mpContext, inputObject,
									" ");
									
									// if SPID is New SP
									if (spidValue.equals(nnsp))
									{
										// set OldSPConflictResolutionDate in context
										super.set(SOAConstants.OLD_SP_RESOLUTION_DATE, mpContext, inputObject,
											getValue(SOAConstants.DATESENT));
									}
									// if SPID is Old SP
									else if (spidValue.equals(onsp))
									{
										// set NewSPConflictResolutionDate in context
										super.set(SOAConstants.NEW_SP_RESOLUTION_DATE, mpContext, inputObject,
											getValue(SOAConstants.DATESENT));
									}
								}

								// Reset the values in cases where we're logging again.
								resetColumnValues();
								
								// Extract the column values from the arguments.
								extractMessageData(mpContext, inputObject);
							}
						}
					}

					// if notification type is Disconnect nitification
					if ( notification.equals(SOAConstants.SV_DISCONNECT_NOTIFICATION ) )
					{
						super.set(SOAConstants.DISCONNECT_ONSP, mpContext, inputObject,
											onsp);
						
						if (activationDate == null)
						{							
							super.set(SOAConstants.ACTIVATION_DATE, mpContext, inputObject,
									getValue(SOAConstants.DATESENT));
						}

						// Reset the values in cases where we're logging again.
						resetColumnValues();
						
						// Extract the column values from the arguments.
						extractMessageData(mpContext, inputObject);
					}

					// If it is the first record which is selected from DB
					if (firstSv)
					{
						String sqlStmt = constructUpdateSqlStatement();
			
						// Getting a prepared statement for the SQL statement.
						updateStatement = dbConn.prepareStatement(sqlStmt);

						firstSv = false;
					}
					
					if (updateStatement != null && notification.equals(SOAConstants.SV_STS_CHANGE_NOTIFICATION)
							&&  status.equalsIgnoreCase("old") ) {
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
							
							Debug.log(Debug.MSG_STATUS,	"Notification is SvStatusChangeNotification and status is old");
						}
						
					} 
					else if(updateStatement != null ){
					//update SV Object table
						update(updateStatement,referenceKey);
					}
				}
				
				// add REFERENCE key in REFERENCE key XML
				referenceKeyXML.setValue(SOAConstants.KEYCONTAINER_NODE
						+ "." + SOAConstants.KEY_NODE + "(" + j + ")",
						String.valueOf(referenceKey));
				
				//Debug.log(Debug.MSG_STATUS,	"Before populateOIDTransOID");
				//populateOIDTransOID(oID , transOID, portingTN , inputObject ,mpContext);
				//Debug.log(Debug.MSG_STATUS,	"After populateOIDTransOID");
				j++;
				
			}
			boolean isRange = false;
			if (oidSet.size() == 1 && transOidSet.size() == 1 && oIDTransOidTnlst.size() > 1){
				isRange = true;
			}else{
				//individual handling
				isRange = false;
			}
			
			if(oIdTransOID_Flag != null && oIdTransOID_Flag.equalsIgnoreCase("true")){		
				populateOIDTransOID(oIDTransOidTnlst,isRange, inputObject, mpContext);
			}else{
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,	"No OID TransOID population will be done because oIdTransOID_Flag flag is set to false.");
			}
			
			// if it is a OutOfSync notification for any of the TN
			if (!atbList.isEmpty())
					{
						outOfSequence = "true";
						CustomerContext.getInstance().set(SOAConstants.OUTOFSYNCTNLIST, atbList);
						// set the OutOfSequence in context
						super.set(outOfSequenceLocation, mpContext, inputObject, outOfSequence);
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,	"TNRangeUpdate: OUTOFSEQUENCE :\n"+ outOfSequence);
					}
			
			if (updateStatement != null)
			{
				
				//executing the batch statement to update the batch sql statements.
				updateStatement.executeBatch();
				
			}
			

			// If the configuration indicates that this SQL operation
			// isn't part of the overall driver
			// transaction, commit the changes now.
			if (!usingContextConnection) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Committing data updated by TNRangeUpdate to database.");
				
				try {
									
					DBConnectionPool.getInstance(true).commit(dbConn);
					
				} catch (ResourceException re) {
					
					if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
						Debug.log(Debug.ALL_ERRORS, re.getMessage());
					
				}
				
			}
			
			// set the REFERENCE key XML in context
			super.set(keyXMLLocation, mpContext, inputObject, referenceKeyXML
					.getDocument());
			
			if (rangeKeyXMLLocation != null && isRangeXMLGenarated) {
				
				// set the Range REFERENCE key XML in context
				super.set(rangeKeyXMLLocation, mpContext, inputObject,
						rangeReferenceKeyXML.getDocument());
				
			}
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				
				Debug.log(Debug.MSG_STATUS,
						"TNRangeUpdate: Generated REFERENCEKey XML :\n"
						+ referenceKeyXML.generate() + "\n\n");
				Debug.log(Debug.MSG_STATUS,
						"TNRangeUpdate: Generated Range REFERENCEKey XML :\n"
						+ rangeReferenceKeyXML.generate());
				
			}
		} catch (Exception e) {
			String errMsg = "ERROR: TNRangeUpdate: Attempt to log to database "
				+ "failed with error: " + e.getMessage();
			
			Debug.log(Debug.ALL_ERRORS, errMsg);
			
			// If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Rolling-back any database changes due to database-logger.");
				
				try {
					
					DBConnectionPool.getInstance(true).rollback(dbConn);
					
				} catch (ResourceException re) {
					
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
					
				}
			}
			
			// Re-throw the exception to the driver.
			if (e instanceof MessageException) {
				throw (MessageException) e;
			} else {
				throw new ProcessingException(errMsg);
			}
		} finally {
			try {
				// if resultSet not closed
				if (rs != null) {
					// close the resultSet
					rs.close();
					
					rs = null;
				}
			} catch (SQLException sqle) {
				
				Debug.log(Debug.ALL_ERRORS, DBInterface
						.getSQLErrorMessage(sqle));
				
			}
			
			try {
				// if statement not closed
				if (stmt != null) {
					// close the statement
					stmt.close();
					
					stmt = null;
				}
				// if dbStatement not closed
				if (dbStatement != null) {
					// close the db statement
					dbStatement.close();
					
					dbStatement = null;
				}
				// if updateStatement not closed
				if (updateStatement != null)
				{
					// close the update statement
					updateStatement.close();
					
					updateStatement = null;
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
					
					// release the connection to resource pool
					DBConnectionPool.getInstance(true)
					.releaseConnection(dbConn);
					
					dbConn = null;
					
				} catch (ResourceException e) {
					
					Debug.log(Debug.ALL_ERRORS, e.toString());
					
				}
			}
			
		}
		
	}
	
	
	/**
	 * Reset the column values in the list.
	 */
	private void resetColumnValues() {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Resetting column values ...");
		
		Iterator iter = columns.iterator();
		
		ColumnData cd = null;
		
		// While columns are available ...
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();
			
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
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void extractMessageData(MessageProcessorContext context,
			MessageObject inputObject) throws MessageException,
			ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Extracting message data to log ...");
		
		Iterator iter = columns.iterator();
		
		ColumnData cd = null;
		
		// While columns are available ...
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();
			
			if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
				Debug.log(Debug.MSG_DATA, "Extracting data for:\n"
						+ cd.describe());
			}
			
			// If location was given, try to get a value from it.
			if (StringUtils.hasValue(cd.location)) {
				// Location contains one or more alternative locations
				// that could contain the column's value.
				StringTokenizer st = new StringTokenizer(cd.location, separator);
				
				String loc = null;
				
				// While location alternatives are available ...
				while (st.hasMoreTokens()) {
					// Extract a location alternative.
					loc = st.nextToken().trim();
					
					// If the value of location indicates that the input
					// message-object's entire value should be used as the
					// column's value, extract it.
					if (loc.equalsIgnoreCase(INPUT_MESSAGE)
							|| loc
							.equalsIgnoreCase(SOAConstants.PROCESSOR_INPUT)) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
							Debug.log(Debug.MSG_DATA, "Using message-object's " + "contents as the column's value.");
						
						cd.value = inputObject.get();
						
						break;
						
					}
					
					//Attempt to get the value from the context or input object.
					if (exists(loc, context, inputObject)) {
						cd.value = get(loc, context, inputObject);
					}
					
					// If we found a value, we're done with this column.
					if (cd.value != null) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
							Debug.log(Debug.MSG_DATA, "Found value for column ["
								+ cd.columnName + "] at location [" + loc + "].");
						
						break;
						
					}
				}
			}
			
			// If no value was obtained from location in context/input,
			// try to set it from default value (if available).
			if (cd.value == null) {
				
				cd.value = cd.defaultValue;
				
				if (cd.value != null) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
						Debug.log(Debug.MSG_DATA, "Using default value for column [" + cd.columnName + "].");
				}
			}
			
			// If no value can be obtained ...
			if (cd.value == null) {
				// If the value is required ...
				if (cd.optional == false) {
					
					// Signal error to caller.
					throw new MessageException(
							"ERROR: Could not locate required value for column ["
							+ cd.columnName + "], database table ["
							+ tableName + "].");
					
				} else // No optional value is available, so just continue on.
				{
					if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
						Debug.log(Debug.MSG_DATA, "Skipping optional column ["
							+ cd.columnName + "] since no data is available.");
				}
			}
		}
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Done extracting message data to log.");
	}
	
	/**
	 * Construct the SQL UPDATE statement from the column data.
	 *
	 * @param  referenceKey  The message key, to perform the SQL update.
	 * @return SQL UPDATE statement.
	 *
	 * @exception  DatabaseException  Thrown on data errors.
	 */
	private String constructUpdateSqlStatement()
	throws ProcessingException, MessageException{
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Constructing SQL UPDATE statement ...");
		
		// Construct the update statement
		StringBuffer sb = new StringBuffer();
		
		sb.append("UPDATE ");
		
		sb.append(tableName);
		
		sb.append(" SET ");
		
		boolean firstOne = true;
		
		Iterator iter = columns.iterator();
		
		ColumnData cd = null;
		
		// Append the names of columns with non-null values.
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();
			
			// Skip columns with null values since null values aren't updated.
			// and skip if update is false
			if (SOAUtility.isNull(cd.value)) {
				continue;
			}
			
			if (firstOne) {
				firstOne = false;
			} else {
				sb.append(", ");
			}
			
			sb.append(cd.columnName);
			
			// If the current column is a date column, and the configuration
			// indicates that the current system date should be used for
			// the value, place it in the SQL statement now.
			if (StringUtils.hasValue(cd.columnType)
					&& cd.columnType
					.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_DATE)
					&& (cd.value instanceof String)
					&& ((String) (cd.value ))
					.equalsIgnoreCase(SOAConstants.SYSDATE)) {
				
				sb.append(" = ");
				sb.append(SOAConstants.SYSDATE);
				
			} else {
				
				sb.append(" = ? ");
				
			}
		}
		
		if (notification != null && notification.equals(SOAConstants.SV_ATR_CHANGE_NOTIFICATION)){
			
				if(authFlagValue != null && authFlagValue.equals("0") && authDateValue != null){
										
					sb.append(" , "+SOAConstants.CONFLICT_DATE+" = ? ");
				}
				
				if(is_altSpid_present){
					sb.append(" , "+SOAConstants.SUBDOMAIN_COL+" = ? ");
				}
		}
		// If USE INVOKEID property is true then only populate
		// LASTATTRIBUTECHANGEINVOKEID and LASTSTATUSCHANGEINVOKEID columns		
		if(useInvokeIdValue){

			if(npacNotification !=null && npacNotification.equals(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION )){
				
				sb.append(" , "+SOAConstants.ATTRIBUTECHANGEINVOKEID_COL+" = ? ");
			}
			
			if(npacNotification !=null && npacNotification.equals(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION )){
				
				sb.append(" , "+SOAConstants.STATUSCHANGEINVOKEID_COL+" = ? ");
			}
		}

		sb.append(" WHERE ");
		sb.append(SOAConstants.REFERENCEKEY_COL);
		sb.append(" = ");
		sb.append(" ? ");
		
		if(useInvokeIdValue){
			
			if(is_bddNotification || is_recoveryReplyNotification){
				
				if (notification != null && notification.equals(SOAConstants.SV_STS_CHANGE_NOTIFICATION)) {
					
					sb.append(" AND ");
					sb.append(SOAConstants.STATUSCHANGETIMESTAMP_COL);
					sb.append(" < ");
					sb.append(" ? ");
				} 
			
			}else{
				if(npacNotification !=null && npacNotification.equals(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION )){
					
					sb.append(" AND ( ");
					sb.append(SOAConstants.STATUSCHANGEINVOKEID_COL);
					sb.append(" IS NULL OR ");
					sb.append(SOAConstants.STATUSCHANGEINVOKEID_COL);
					sb.append(" <= ");
					sb.append(" ?  )");
				}
			}			
		}
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Done constructing SQL UPDATE statement. "
				+ sb.toString());
		
		return (sb.toString());
	}
	
	/**
	 * update SV Object table using the given statement.
	 *
	 * @param  dbConn  The database connection to perform the SQL update
	 * operation against.
	 * @param  referenceKey  The message key, to perform the SQL update.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void update(PreparedStatement pstmt,long referenceKey)
	throws ProcessingException {
		
		// Make sure that at least one column value will be updated.
		validate();
						
		try {

			// Populate the SQL statement using values obtained from
			// the column data objects.
			populateSqlStatement(pstmt,referenceKey);
			
			// Add into batch
			pstmt.addBatch();
			
			
		} catch (SQLException sqle) {
			
			throw new ProcessingException(
					"ERROR: Could not update row into database table ["
					+ tableName + "]:\n"
					+ DBInterface.getSQLErrorMessage(sqle));
			
		} catch (Exception e) {
			
			throw new ProcessingException(
					"ERROR: Could not update row into database table ["
					+ tableName + "]:\n" + e.toString());
			
		}
	}
	
	/**
	 * Populate the SQL UPDATE statement from the column data.
	 *
	 * @param  pstmt  The prepared statement object to populate.
	 *
	 * @exception Exception  thrown if population fails.
	 */
	private void populateSqlStatement(PreparedStatement pstmt,long referenceKey)
	throws ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Populating SQL INSERT statement ...");
		
		try {
			
			int Ix = 1; // First value slot in prepared statement.
			
			Iterator iter = columns.iterator();
			Debug.log(Debug.MSG_STATUS,"ColumnName size["+columns.size()+"]");
			ColumnData cd = null;
			
			// Append the names of columns with non-null values.
			while (iter.hasNext()) {
				cd = (ColumnData) iter.next();
				
				if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
					Debug.log(Debug.MSG_DATA, "Populating SQL statement for:\n"
							+ cd.describe());
				}
				
				//Skip columns with null values since null 
				//values aren't inserted
				// or updated and also skip columns 
				//for update if update is false.
				if (SOAUtility.isNull(cd.value)) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS, "Skipping null column value.");
					
					continue;
					
				}
				
				if ( Debug.isLevelEnabled( Debug.DB_DATA ))
					Debug.log(Debug.DB_DATA, "Populating prepared-statement slot [" + Ix + "].");
				
				// Default is no column type specified.
				if (!StringUtils.hasValue(cd.columnType)) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						
						Debug.log(Debug.MSG_DATA, "Value for column ["
								+ cd.columnName + "] is [" + cd.value.toString()
								+ "].");
					}
						
					pstmt.setObject(Ix, cd.value);
					
				} 
				// if column type is date
				else if (cd.columnType
						.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_DATE)) {
					
					String val = (String) (cd.value);
					
					if ( Debug.isLevelEnabled( Debug.MSG_DATA )){
						Debug.log(Debug.MSG_DATA, "Value for date column ["
								+ cd.columnName + "] is [" + val + "].");
					}
						
					
					//SYSDATE is already in the text of the SQL statement 
					//used to create
					//the prepared statement, so there's nothing more to do here.
					if (val.equalsIgnoreCase(SOAConstants.SYSDATE)) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
							
							Debug.log(Debug.MSG_STATUS, "Skipping date population "
									+ "since SYSDATE is already in SQL string.");
						}						
						continue;
						
					}
					
					// If we're not inserting the current system date,
					//the caller must
					// have provided an actual date value,
					//which we must now parse.
					if (!StringUtils.hasValue(cd.dateFormat)) {
						
						throw new ProcessingException(
								"ERROR: Configuration for " + " date column ["
								+ cd.columnName
								+ "] does not specify date format.");
						
					}
					
					SimpleDateFormat sdf = new SimpleDateFormat(cd.dateFormat);
					
					java.util.Date d = sdf.parse(val);
					
					pstmt.setTimestamp(Ix, new java.sql.Timestamp(d.getTime()));
				}
				// if column type is Text BLOB
				else if (cd.columnType
						.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_TEXT_BLOB)) {
					
					if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
						Debug.log(Debug.MSG_DATA, "Querying column ["
								+ cd.describe() + "].");
					}
					
					DBLOBUtils
					.setCLOB(pstmt, Ix, Converter.getString(cd.value));
					
				} 
				// if column type is Binary BLOB
				else if (cd.columnType
						.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_BINARY_BLOB))
				{
					
					byte[] bytes = null;
					
					if (cd.value instanceof byte[]) {
						bytes = (byte[]) cd.value;
					}
					
					else if (cd.value instanceof String) {
						bytes = ((String) (cd.value)).getBytes();
					}
					
					else if (cd.value instanceof Document) {
						bytes = Converter.getString(cd.value).getBytes();
					} else {
						
						throw new ProcessingException(
								"ERROR: Value for database table ["
								+ tableName
								+ "], column ["
								+ cd.columnName
								+ "] of type ["
								+ cd.value.getClass().getName()
								+ "] can't be converted to byte stream.");
						
					}
					
					if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
						Debug.log(Debug.MSG_DATA, "Querying column ["
								+ cd.describe() + "].");
					}
					DBLOBUtils.setBLOB(pstmt, Ix, bytes);
					
				} else {
					
					throw new ProcessingException(
							"ERROR: Invalid column-type property value ["
							+ cd.columnType
							+ "] given in configuration.");
					
				}
				
				// Advance to next value slot in prepared statement.
				Ix++;
			}
					
			if (notification != null && notification.equals(SOAConstants.SV_ATR_CHANGE_NOTIFICATION)){
								
				if(authFlagValue != null && authFlagValue.equals("0") && authDateValue != null){
						
					SimpleDateFormat dfrm = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
					java.util.Date auth_date = dfrm.parse(authDateValue);
					java.sql.Timestamp auth_timeStamp = new Timestamp(auth_date.getTime());
					
					pstmt.setTimestamp(Ix, auth_timeStamp);
					Ix++;
				}
				
				if(is_altSpid_present){
					if(is_subdomainId_present){
					
						pstmt.setString(Ix, subdomainIdValue);
						Ix++;
					}else{
					
						pstmt.setNull(Ix , java.sql.Types.VARCHAR);
						Ix++;
					}
				}
			}
			if(useInvokeIdValue){
				
				if(npacNotification !=null && npacNotification.equals(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION )){
					pstmt.setLong(Ix, incomingInvokeId);
					Ix++;
				}
				if(npacNotification !=null && npacNotification.equals(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION )){
					pstmt.setLong(Ix, incomingInvokeId);
					Ix++;
				}
			}
			
			pstmt.setLong(Ix,referenceKey);
			if(useInvokeIdValue){				
				if(is_bddNotification || is_recoveryReplyNotification){
					
					if (notification != null && notification.equals(SOAConstants.SV_STS_CHANGE_NOTIFICATION)) {
						
						Ix++;
						SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
						java.util.Date dsent = df.parse(getValue(SOAConstants.DATESENT));
						java.sql.Timestamp timeStampDate = new Timestamp(dsent.getTime());					     
						pstmt.setTimestamp(Ix, timeStampDate);
					}
				}else{
					if(npacNotification !=null && npacNotification.equals(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION )){
						
						Ix++;
						pstmt.setLong(Ix, incomingInvokeId);
					}
				}
			}
			 
				
		} catch (Exception exception) {
			throw new ProcessingException(
					"ERROR:  could not populate sql statement ."
					+ exception.toString());
			
		}
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Done populating SQL UPDATE statement.");
	}
	
	/**
	 * Check that columns were configured and at least one
	 * mandatory field has a value to insert.
	 *
	 * @exception ProcessingException  thrown if invalid
	 */
	private void validate() throws ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Validating column data ...");
		
		boolean valid = false;
		
		Iterator iter = columns.iterator();
		
		ColumnData cd = null;
		
		// While columns are available ...
		while (iter.hasNext()) {
			
			cd = (ColumnData) iter.next();
			
			// If we've found at least one value to insert, its valid.
			if (cd.value != null) {
				valid = true;
				
				break;
			}
			
		}
		
		if (!valid) {
			
			throw new ProcessingException(
					"ERROR: No database column values are available to write to ["
					+ tableName + "].");
			
		}
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Done validating column data.");
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
	protected String getValue(String locations) throws MessageException,
	ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);
		
		String tok = null;
		
		// While tokens are available ...
		while (st.hasMoreTokens()) {
			tok = st.nextToken();
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "Checking location [" + tok	+ "] for value...");
			
			// if the value of token exists in context or messageobject.
			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		
		return null;
	}
	/**
	 * This method populated Tn , OID and TransOID in response XML.  
	 * @param transOidTnlst
	 * @param isRange
	 * @param mobj
	 * @param mpContext2
	 * @throws MessageException
	 * @throws ProcessingException
	 */
	
	private void populateOIDTransOID(List<TnOIDTransOID> transOidTnlst, boolean isRange, MessageObject mobj, 
			MessageProcessorContext mpContext2) throws MessageException, ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside populateOIDTransOID method");
		
		if(transOidTnlst.size()>0){
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, this.getClass() + "transOidTnlst is not empty ");
			
			Document dom = mobj.getDOM();
			Iterator iter = transOidTnlst.iterator();
			Element ele = null;
			if(isRange){
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside the range condition");
				
				StringBuffer tn = new StringBuffer( npaValue );
				tn.append( SOAConstants.DEFAULT_DELIMITER );
				tn.append( nxxValue );
				tn.append( SOAConstants.DEFAULT_DELIMITER );
				tn.append(startTNValue);
				tn.append( SOAConstants.DEFAULT_DELIMITER );
				tn.append(endTNValue);
				TnOIDTransOID tnOiDTransOid = (TnOIDTransOID)iter.next();
				if (tnOiDTransOid != null){
					if(tnOiDTransOid.oid != null || tnOiDTransOid.transOid != null){
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside oid and trans oid is not null ");
						
						Element xmlElement_Tn = dom.createElement(SOAConstants.TN_NODE);
						xmlElement_Tn.setAttribute("value", tn.toString());
						
						if( tnOiDTransOid.oid  != null){ 
						
							if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
								Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside the oid is not null");
						
							Element xmlElement_OID = dom.createElement(SOAConstants.OID);
							xmlElement_OID.setAttribute("value",tnOiDTransOid.oid.toString());
							xmlElement_Tn.appendChild(xmlElement_OID);
						}
						if ( tnOiDTransOid.transOid != null){
							
							if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
								Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside the transoid is not null");
							
							Element xmlElement_TransOID = dom.createElement(SOAConstants.TransOID);
							xmlElement_TransOID.setAttribute("value",tnOiDTransOid.transOid.toString());
							xmlElement_Tn.appendChild(xmlElement_TransOID);
						}
						ele = (Element) dom.getElementsByTagName("SOAToUpstreamHeader").item(0);
						ele.appendChild(xmlElement_Tn);
					}
				}else{
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS, this.getClass() + "OID and TransOID fields are not populated in request XML.");
					
				}
				inputObject.set(dom);
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,	"returning populateOIDTransOID");
			}else{
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside the not range condition");
				
				while(iter.hasNext()){
					TnOIDTransOID tnOiDTransOid = (TnOIDTransOID)iter.next();
					if (tnOiDTransOid != null){
						if(tnOiDTransOid.oid != null || tnOiDTransOid.transOid != null){
							
							if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
								Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside oid and trans oid is not null ");
							
							Element xmlElement_Tn = dom.createElement(SOAConstants.TN_NODE);
							xmlElement_Tn.setAttribute("value", tnOiDTransOid.tn);
							
							if( tnOiDTransOid.oid  != null){ 
							
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
									Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside the oid is not null");
								
								Element xmlElement_OID = dom.createElement(SOAConstants.OID);
								xmlElement_OID.setAttribute("value",tnOiDTransOid.oid.toString());
								xmlElement_Tn.appendChild(xmlElement_OID);
							}
							if ( tnOiDTransOid.transOid != null){
							
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
									Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside the transoid is not null");
								
								Element xmlElement_TransOID = dom.createElement(SOAConstants.TransOID);
								xmlElement_TransOID.setAttribute("value",tnOiDTransOid.transOid.toString());
								xmlElement_Tn.appendChild(xmlElement_TransOID);
							}
							ele = (Element) dom.getElementsByTagName("SOAToUpstreamHeader").item(0);
							ele.appendChild(xmlElement_Tn);
						}
					}else{
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, this.getClass() + "OID and TransOID fields are not populated in request XML.");
						
					}
					inputObject.set(dom);
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,	"returning populateOIDTransOID");
				}
			}
		}
	}

	/**
	 * Fetch SubdomainId value from SOA_SUBDOMAIN_ALTSPID table using spid , altspid and customerId Value.
	 * @param dbConn
	 * @param altSpid
	 * @param customerId
	 * @return
	 */
	
	private String getSubDomainIdUsingAltSpid(Connection dbConn, String altSpid, String customerId){
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String subdomainId = null;
		try{
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, " Spid Value["+spidValue+"] and customerID value["+customerId+"] " +
						"and altSpid value ["+altSpid+"] to fetch sudomainId value");
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "Query to get SubdomainId value from DB:\n"+GET_SUBDOMAINID_QUERY);
			
			
			pstmt = dbConn.prepareStatement(GET_SUBDOMAINID_QUERY);						
			pstmt.setString(1, spidValue);
			pstmt.setString(2, customerId);
			pstmt.setString(3, customerId);
			pstmt.setString(4, altSpid);
			
			rs = pstmt.executeQuery();
			
			if(rs.next()){
				subdomainId = rs.getString(SOAConstants.SUBDOMAIN_COL);
			}
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "SudomainId value fetched from DB is ["+subdomainId+"]");
			
		}catch (SQLException e) {

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log(Debug.SYSTEM_CONFIG, "Could not execute the sql statement:"+ e.getMessage());
			}
			
		} finally {
			try {
				if (pstmt != null)
					pstmt.close();
				if (rs != null)
					rs.close();
			} catch (SQLException sqle) {
				if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){
				 Debug.log(Debug.SYSTEM_CONFIG, "Could not close the resources: " +
				 		"Statement and Resultset:"+sqle.getMessage());
			   }
			
			}
		}
		
		return subdomainId;
	}
	
	/**
	 * Class ColumnData is used to encapsulate a description of a single column
	 * and its associated value.
	 */
	private static class ColumnData {
		
		public final String columnName;
		
		public final String columnType;
		
		public final String dateFormat;
		
		public final String location;
		
		public final String defaultValue;
		
		public final boolean optional;
		
		public Object value = null;
		
		public ColumnData(String columnName, String columnType,
				String dateFormat, String location, String defaultValue,
				String optional) throws FrameworkException {
			
			this.columnName = columnName;
			this.columnType = columnType;
			this.dateFormat = dateFormat;
			this.location = location;
			this.defaultValue = defaultValue;
			this.optional = StringUtils.getBoolean(optional);
		}
		
		/**
		 * To describe the column name, column type, data format, location
		 * and default value.
		 */
		public String describe() {
			
			StringBuffer sb = new StringBuffer();
			
			sb.append("Column description: Name [");
			sb.append(columnName);
			
			if (StringUtils.hasValue(columnType)) {
				sb.append("], type [");
				sb.append(columnType);
			}
			
			if (StringUtils.hasValue(dateFormat)) {
				sb.append("], date-format [");
				sb.append(dateFormat);
			}
			
			if (StringUtils.hasValue(location)) {
				sb.append("], location [");
				sb.append(location);
			}
			
			if (StringUtils.hasValue(defaultValue)) {
				sb.append("], default [");
				sb.append(defaultValue);
			}
			
			sb.append("], optional [");
			sb.append(optional);
			
			if (value != null) {
				sb.append("], value [");
				sb.append(value);
			}
			
			sb.append("].");
			
			return (sb.toString());
		}
	}
	/**
	 * TnOIDTransOID class is used to encapsulate the Tn , OID and TransOID info in objects.
	 * @author msgupta
	 *
	 */
	private static class TnOIDTransOID {

        public final String tn;
        public final String oid;
        public final String transOid;
        public TnOIDTransOID(String tn, String oid , String transOid) throws FrameworkException {

            this.tn = tn;
            this.oid = oid;
            this.transOid = transOid;

        }

    }
	
	//--------------------------For Testing---------------------------------//
	
	public static void main(String[] args) {
		
		Properties props = new Properties();
		
		props.put("DEBUG_LOG_LEVELS", "ALL");
		
		props.put("LOG_FILE", "E:\\logmap.txt");
		
		Debug.showLevels();
		
		Debug.configureFromProperties(props);
		
		if (args.length != 3) 
		{	
		Debug.log(Debug.ALL_ERRORS,"TNRangeUpdate: USAGE:  "
			+ " jdbc:oracle:thin:@192.168.1.246:1521:soa prasanthi prasanthi ");
			
			return;
		
		}
		try {
			
			DBInterface.initialize(args[0], args[1], args[2]);
			
		} catch (DatabaseException e) {
			
			Debug.log(null, Debug.MAPPING_ERROR, ": "
					+ "Database initialization failure: " + e.getMessage());
			
		}
		
		TNRangeUpdate rangeUpdate = new TNRangeUpdate();
		
		try {
			
			rangeUpdate.initialize("FULL_NEUSTAR_SOA", "TNRangeUpdate");
			
			MessageProcessorContext mpx = new MessageProcessorContext();
			
			MessageObject mob = new MessageObject();
				
			mob.set("StartTN", "8391");
			mob.set("EndTN", "8393");
			mob.set("REQUEST_HEADER_SUBREQUEST", "SvModifyRequest");
			//mob.set("STARTSVID","210841");
			//mob.set("RANGE_REFERENCEKEY_OUT_LOC","abcd");
			
			//mob.set("START","1");
			//mob.set("END","5");
			mob.set("NPA", "530");
			mob.set("NXX", "012");
			mob.set("SPID", "1111");
			mob.set("ONSP", "1235");
			mob.set("NNSP", "4567");
			mob.set("NOTIFICATION", "SvActivateNotification");
			//mob.set("SVID","2000");
			mob.set("LRN", "30000");
			mob.set("LNP", "lspp");
			mob.set("LASTREQUESTTYPE", "SvActivateRequest");
			//mob.set("REFERENCEKEY_OUT_LOC","ABC");
			//mob.set("TABLE_NAME","SOA_SUBSCRIPTION_VERSION");
			mob.set("KEY1", "<SOAMessage>" + "<SOAToUpstream>"
					+ "<SOAToUpstreamHeader>"
					+ "<DateSent value=\"10-20-2004-053236PM\" />"
					+ "<RegionId value=\"0003\" />" + "</SOAToUpstreamHeader>"
					+ "<SOAToUpstreamBody>" + "<SvActivateNotification>"
					+ "<Subscription>" + "<TnSvIdRange>" + "<TnRange>"
					+ "<Tn value=\"530-012-8391\" />"
					+ "<EndStation value=\"8393\" />" + "</TnRange>"
					+ "<SvIdRange>" + "<StartId value=\"210841\" />"
					+ "<EndId value=\"210843\" />" + "</SvIdRange>"
					+ "</TnSvIdRange>" + "</Subscription>"
					+ "<NewSP value=\"A111\" />" + "<OldSP value=\"1111\" />"
					+ "</SvActivateNotification>" + "</SOAToUpstreamBody>"
					+ "</SOAToUpstream>" + "</SOAMessage>");
			
			//mob.set("LASTMESSAGE","SvReleaseInConflictRequest");
			//mob.set("CREATEDBY","Jaganmohan");
			
			//mob.set("CREATEDDATE", "SYSDATE");
			
			rangeUpdate.process(mpx, mob);
			
			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());
			
		} catch (ProcessingException pex) {
			
			Debug.log(Debug.ALL_ERRORS, "Processing Exception: " + pex.getMessage());
			
		} catch (MessageException mex) {
			
			Debug.log(Debug.ALL_ERRORS, "Message Exception: " + mex.getMessage());
			
		}
	} //end of main method	
}
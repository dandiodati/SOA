/**
 * $Header: //spi/neustar_soa/adapter/messageprocessor/APIRequestPopulationNANCSupport.java#1 $
 */
package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.RegexUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

/**
 * This class is used to populate the request XML with GTT data along with
 * AlternativeSPID, SvType, BillingID, EndUserLocationType, EndUserLocationValue
 * nodes.
 * 
 * AlternativeSPID, SvTypebased node population is based on the NANC 399 support
 * flag for a given SPID.
 */
public class APIRequestPopulationNANCSupport extends MessageProcessorBase {

	/*
	 * Property indicating the location into which subRequestType node should be
	 * placed.
	 */
	public static final String SUBREQUESTTYPE_NODE = "SUBREQUESTTYPE_LOC";

	/*
	 * Property indicating the location into which SPID value should be placed.
	 */
	public static final String SPID_VAL = "SPID_LOC";

	/*
	 * Property indicating the location into which LRN value should be placed.
	 */
	public static final String LRN_VAL = "LRN_LOC";

	/*
	 * Property indicating the location into which Customer value should be
	 * placed.
	 */
	public static final String CUSTOMER_NODE = "CUSTOMER_LOC";

	/*
	 * Declaring private variables
	 */
	private String subRequestNode;
	private String subRequestValue;
	private String spidPropValue;
	private String spidValue;
	private String lrnPropValue;
	private String lrnValue;
	private String customerNode;
	private String customerValue;
	private String domainFlag;
	private String ptoValue;
		
	/**
	 * This variable contains MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;

	/**
	 * This variable contains MessageObject object
	 */
	private MessageObject inputObject = null;

	private static final String gttDataQuery = "SELECT "
			+ "CLASSDPC, CLASSSSN, CNAMDPC, CNAMSSN, ISVMDPC, ISVMSSN, LIDBDPC, LIDBSSN, WSMSCDPC, WSMSCSSN, "
			+ "SVTYPE, ALTERNATIVESPID, ENDUSERLOCATIONVALUE, ENDUSERLOCATIONTYPE, BILLINGID, LATA, VOICEURI, MMSURI, POCURI, PRESURI, SMSURI, " +
				"LASTALTERNATIVESPID, SPCUSTOM1, SPCUSTOM2, SPCUSTOM3 from SOA_GTT A, SOA_LRN B WHERE B.STATUS='ok' AND B.GTTID IS NOT NULL AND B.SPID=? AND B.LRN=? AND A.GTTID = B.GTTID";
	
	private static final String getDomainSetting = "SELECT GUIDISPLAYCONTROLKEY FROM DOMAIN WHERE CUSTOMERID=?";
	
	static String[] gttDataArray = { "ClassDPC", "ClassSSN", "CnamDPC",
			"CnamSSN", "IsvmDPC", "IsvmSSN", "LidbDPC", "LidbSSN", "WsmscDPC",
			"WsmscSSN" };

	static String[] gttDataArrayForSvModify = { "ClassDPC", "ClassSSN",
			"CnamDPC", "CnamSSN", "IsvmDPC", "IsvmSSN", "LidbDPC", "LidbSSN",
			"WsmscSSN", "WsmscDPC", "BillingId", "EndUserLocationType",
			"EndUserLocationValue" };

	static String[] gttDataArrayForNPBModify = { "ClassDPC", "ClassSSN",
			"CnamDPC", "CnamSSN", "IsvmDPC", "IsvmSSN", "LidbDPC", "LidbSSN",
			"WsmscSSN", "WsmscDPC" };

	public static final String ALTBIDFLAG = "ALTBIDNANC436FLAG";
	public static final String ALTEULVFLAG = "ALTEULVNANC436FLAG";
	public static final String ALTEULTFLAG = "ALTEULTNANC436FLAG";
	public static final String BILLINGID = "BILLINGID";
	public static final String ENDUSERLOCATIONTYPE = "ENDUSERLOCATIONTYPE";
	public static final String ENDUSERLOCATIONVALUE = "ENDUSERLOCATIONVALUE";
	public static final String AltBillingIDNode = "AlternativeBillingId";
	public static final String AltEndUserLocationTypeNode = "AlternativeEndUserLocationType";
	public static final String AltEndUserLocationValueNode = "AlternativeEndUserLocationValue";
	
	public static final String NNSPSimplePortIndicator = "NNSPSimplePortIndicator";
	public static final String ONSPSimplePortIndicator = "ONSPSimplePortIndicator";

	//NANC URI Fields
	public static final String VOICEURINODE = "VoiceURI";
	public static final String MMSURINODE = "MMSURI";
	public static final String POCURINODE = "PoCURI";
	public static final String PRESURINODE = "PRESURI";
	public static final String SMSURINODE = "SMSURI";
	
	
	public static final String NumberPoolBlockSvType = "NumberPoolBlockSvType";
	public static final String AlternativeSPID = "AlternativeSPID";
	public static final String SvType = "SvType";
	public static final String GTTData = "GTTData";
	public static final String ClassDPC = "ClassDPC";
	public static final String ClassSSN = "ClassSSN";
	public static final String CnamDPC = "CnamDPC";
	public static final String CnamSSN = "CnamSSN";
	public static final String IsvmDPC = "IsvmDPC";
	public static final String IsvmSSN = "IsvmSSN";
	public static final String LidbDPC = "LidbDPC";
	public static final String LidbSSN = "LidbSSN";
	public static final String WsmscDPC = "WsmscDPC";
	public static final String WsmscSSN = "WsmscSSN";
	public static final String DataToModify = "DataToModify";
	public static final String value = "value";
	
	public static final String NumberPoolBlockActivateRequest = "NumberPoolBlockActivateRequest";
	public static final String NumberPoolBlockModifyRequest = "NumberPoolBlockModifyRequest";
	public static final String SvCreateRequest = "SvCreateRequest";
	public static final String SvModifyRequest = "SvModifyRequest";
	public static final String SvReleaseRequest = "SvReleaseRequest";
	public static final String SvReleaseInConflictRequest = "SvReleaseInConflictRequest";
	
	public static final String LastAlternativeSPID = "LastAlternativeSPID";
	
	public static final String SPCUSTOM1NODE = "SPCUSTOM1";
	public static final String SPCUSTOM2NODE = "SPCUSTOM2";
	public static final String SPCUSTOM3NODE = "SPCUSTOM3";
	
	private Connection dbCon = null;

	/**
	 * Initializes this adapter with persistent properties
	 * 
	 * @param key
	 *            Property-key to use for locating initialization properties.
	 * 
	 * @param type
	 *            Property-type to use for locating initialization properties.
	 * 
	 * @exception ProcessingException
	 *                when initialization is not successful.
	 */
	public void initialize(String key, String type) throws ProcessingException {

		super.initialize(key, type);
        
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "APIRequestPopulationNANCSupport Initialing...");
		}

		StringBuffer sbuff = new StringBuffer();

		subRequestNode = getRequiredPropertyValue(SUBREQUESTTYPE_NODE, sbuff);
		spidPropValue = getRequiredPropertyValue(SPID_VAL, sbuff);
		lrnPropValue = getPropertyValue(LRN_VAL);
		customerNode = getRequiredPropertyValue(CUSTOMER_NODE, sbuff);
		
		// Throw error if any of the required properties are missing
		if (sbuff.length() > 0) {
			throw new ProcessingException(sbuff.toString());
		}

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "APIRequestPopulationNANCSupport Initialing done...");
		}
	}

	/**
	 * Extract data values from the context/input
	 * 
	 * @param context
	 *            The context
	 * @param msgObj
	 *            Input message to process.
	 * 
	 * @return input message
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 */
	public NVPair[] process(MessageProcessorContext context,
			MessageObject inputObj) throws MessageException,
			ProcessingException {	
		
		ThreadMonitor.ThreadInfo tmti = null;

		if (inputObj == null || inputObj.getDOM() == null) {
			return null;
		}
		this.mpContext = context;

		this.inputObject = inputObj;

		GttData gttData = null;

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "APIRequestPopulationNANCSupport Processing...");
		}		
		spidValue = getValue(spidPropValue, mpContext, inputObject);
		lrnValue = getValue(lrnPropValue, mpContext, inputObject);		
		subRequestValue = getValue(subRequestNode, mpContext, inputObject);
		customerValue = getValue(customerNode, mpContext, inputObject);
		
		try {
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] processing API request" );
			
			Document dom = inputObject.getDOM();			
			
			if ((lrnValue == null || lrnValue.equals("") || spidValue == null || spidValue.equals("") || subRequestValue == null
					|| subRequestValue.equals("") || customerValue == null || customerValue.equals(""))
					&& ((!subRequestValue.equals(SvReleaseRequest)) && (!subRequestValue.equals(SvReleaseInConflictRequest)))) {
	
				
				XMLMessageParser inputParser = new XMLMessageParser(dom);
				String ptoNode = SOAConstants.REQUEST_BODY_PATH + "." + subRequestValue + "." + "PortToOriginal";	
				
				if (inputParser.exists(ptoNode)) {
					ptoValue = inputParser.getValue(ptoNode);
					
					if( (ptoValue != null) && (ptoValue.equals("1") || ptoValue.equals("true"))){
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							  Debug.log(Debug.MSG_STATUS, "Request contains PortToOriginal field with value["+ptoValue+"]");
							}
						processSimplePortIndicator(dom, customerValue, subRequestValue, inputObj);
						
						/* SOA 5.8.1 requirement- In API request if NNSP & ONSP are same ,
						 *  then convert LNPTYPE value from "lspp" to "lisp" */
										
						checkLnpTypeNodeVal (dom, subRequestValue, inputObj);
					}
				}else{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log(Debug.MSG_STATUS, "Required nodes are not present in the request XML API request population ByPassed...");
						}
				}
				
	
			} 
			else {
				
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			        Debug.log(Debug.MSG_STATUS, "Required nodes are present in the request XML...");
			    }
				
				
				/*
				 * check for the API support flag for a SPID
				 */				
				String apiFlag = String.valueOf(NANCSupportFlag.getInstance(
						spidValue).getApiFlag());
				
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(Debug.MSG_STATUS, ": The API flag for spid ["	+ spidValue	+ "] is: [" + apiFlag + "]");
				}
				
				if (apiFlag != null && apiFlag.equals("1")) {
	
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					
					try {
						
						dbCon = DBConnectionPool.getInstance().acquireConnection();
						pstmt = dbCon.prepareStatement(gttDataQuery);						
						pstmt.setString(1, spidValue);
						pstmt.setString(2, lrnValue);
						
						rs = pstmt.executeQuery();

						if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
						  Debug.log(Debug.NORMAL_STATUS,": Executed query [" + gttDataQuery + "]");
						}

						if (rs.next()) {
							gttData = new GttData(rs.getString("CLASSDPC"), rs
									.getString("CLASSSSN"), rs
									.getString("CNAMDPC"), rs
									.getString("CNAMSSN"), rs
									.getString("ISVMDPC"), rs
									.getString("ISVMSSN"), rs
									.getString("LIDBDPC"), rs
									.getString("LIDBSSN"), rs
									.getString("WSMSCDPC"), rs
									.getString("WSMSCSSN"), rs
									.getString("SVTYPE"), rs
									.getString("ALTERNATIVESPID"), rs
									.getString("ENDUSERLOCATIONVALUE"), rs
									.getString("ENDUSERLOCATIONTYPE"), rs
									.getString("BILLINGID"), rs
									.getString("LATA"), rs
									.getString("VOICEURI"), rs
									.getString("MMSURI"), rs
									.getString("POCURI"), rs
									.getString("PRESURI"), rs
									.getString("SMSURI"), rs
									.getString("LASTALTERNATIVESPID"), rs
									.getString("SPCUSTOM1"), rs
									.getString("SPCUSTOM2"), rs
									.getString("SPCUSTOM3"));

						}
					} catch (SQLException e) {

						if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
							Debug.log(Debug.SYSTEM_CONFIG, "Could not execute the sql statement:"+ e.getMessage());
						}
						
					} catch (Exception e) {

						if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){
							Debug.log(Debug.SYSTEM_CONFIG, "Could not execute the sql statement:"+ e.getMessage());
						}
					}
					finally {
						try {
							if (pstmt != null)
								pstmt.close();
							if (rs != null)
								rs.close();
						} catch (SQLException sqle) {
							if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){
							 Debug.log(Debug.SYSTEM_CONFIG, "Could not close the resources: Statement and Resultset:"+sqle.getMessage());
						    }
						}
					}
					
					
					/*
					 * check for the NANC 399 support flag for a SPID
					 */
					String supportFlag = String.valueOf(NANCSupportFlag
							.getInstance(spidValue).getNanc399flag());

					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS, ": NANC 399 support flag for spid ["	+ spidValue	+ "] is: [" + supportFlag + "]");
					}

					if (supportFlag != null && supportFlag.length() == 1) {
						supportFlag = "0" + supportFlag;
					}
					
					if (supportFlag != null
							&& !(supportFlag.equals("0") || supportFlag
									.equals("00"))) {

						// Splitting support flag in to svType and altSpid
						String svTypeflag = supportFlag.toString().substring(0,
								1);
						String altSpidflag = supportFlag.toString()
								.substring(1);

						if (subRequestValue
								.equals(NumberPoolBlockActivateRequest)
								|| subRequestValue
										.equals(NumberPoolBlockModifyRequest)) {

							String npbType = NumberPoolBlockSvType;
							processNodePopulation(dom, npbType, svTypeflag,
									gttData, inputObject);

							String AltSPID = AlternativeSPID;
							processNodePopulation(dom, AltSPID, altSpidflag,
									gttData, inputObject);
						}

						if (subRequestValue.equals(SvCreateRequest)
								|| subRequestValue.equals(SvModifyRequest)) {
							String svType = SvType;
							processNodePopulation(dom, svType, svTypeflag,
									gttData, inputObject);

							String AltSPID = AlternativeSPID;
							processNodePopulation(dom, AltSPID, altSpidflag,
									gttData, inputObject);
						}
					} else {

						if (subRequestValue
								.equals(NumberPoolBlockActivateRequest)
								|| subRequestValue
										.equals(NumberPoolBlockModifyRequest)) {
							NodeList emptyNode = dom
									.getElementsByTagName(NumberPoolBlockSvType);
							removeEmptyNode(emptyNode);

						} else if (subRequestValue.equals(SvCreateRequest)
								|| subRequestValue.equals(SvModifyRequest)) {
							NodeList emptyNode = dom
									.getElementsByTagName(SvType);
							removeEmptyNode(emptyNode);
						}
						NodeList emptyNodeSecond = dom
								.getElementsByTagName(AlternativeSPID);
						removeEmptyNode(emptyNodeSecond);
					}

					if (subRequestValue.equals(SvCreateRequest)
							|| subRequestValue.equals(SvModifyRequest)) {
						ProcessEndUserData(dom, gttData, inputObject);
					}

					// Population of GTT data fields in the request XML based on
					// their presence.
					ProcessGTTData(dom, gttData, inputObject);

					// Specific to NANC436 population for NPB request
					if (subRequestValue
							.equals(NumberPoolBlockActivateRequest)
							|| subRequestValue
									.equals(NumberPoolBlockModifyRequest)) {

						NodeList emptyAltBiIdNode = dom
								.getElementsByTagName(AltBillingIDNode);
						// set the value attribute with empty node.
						removeEmptyNode(emptyAltBiIdNode);

						NodeList emptyAltEultNode = dom
								.getElementsByTagName(AltEndUserLocationTypeNode);
						// set the value attribute with empty node.
						removeEmptyNode(emptyAltEultNode);

						NodeList emptyAltEulvNode = dom
								.getElementsByTagName(AltEndUserLocationValueNode);
						// set the value attribute with empty node.
						removeEmptyNode(emptyAltEulvNode);

						String altBidflag = NANCSupportFlag.getInstance(
								spidValue).getAltBidNanc436Flag();
						String altEultFlag = NANCSupportFlag.getInstance(
								spidValue).getAltEultNanc436Flag();
						String altEulvFlag = NANCSupportFlag.getInstance(
								spidValue).getAltEulvNanc436Flag();
						if ((altBidflag != null && !altBidflag.equals("0"))
								|| (altEultFlag != null && !altEultFlag
										.equals("0")) || (altEulvFlag != null)
								&& !altEulvFlag.equals("0")) {

							if (gttData != null) {
								if (altBidflag != null
										&& altBidflag.equals("1")) {

									populateNanc436DataForNPB(dom,
											subRequestValue, AltBillingIDNode,
											gttData.billingId, inputObject);

								}
								if (altEulvFlag != null
										&& altEulvFlag.equals("1")) {

									populateNanc436DataForNPB(dom,
											subRequestValue,
											AltEndUserLocationValueNode,
											gttData.endUserLocationValue,
											inputObject);

								}
								if (altEultFlag != null
										&& altEultFlag.equals("1")) {

									populateNanc436DataForNPB(dom,
											subRequestValue,
											AltEndUserLocationTypeNode,
											gttData.endUserLocationType,
											inputObject);

								}
							} else {

								if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

								Debug.log(Debug.MSG_STATUS, ": LRN/GTT mapping doesnot exist for the LRN["
														+ lrnValue
														+ "] and spid ["
														+ spidValue + "]");
								}
								if(altBidflag != null
										&& altBidflag.equals("1")){
									
									populateNanc436DataForNPB(dom, subRequestValue, AltBillingIDNode ,null, inputObj);
									
								}
								if(altEulvFlag != null
										&& altEulvFlag.equals("1")){
									
									populateNanc436DataForNPB(dom, subRequestValue, AltEndUserLocationValueNode ,null, inputObj);
									
								}
								if(altEultFlag != null
										&& altEultFlag.equals("1")){
									
									populateNanc436DataForNPB(dom, subRequestValue, AltEndUserLocationTypeNode ,null, inputObj);
									
								}
							}
						} else {

							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							  Debug.log(Debug.MSG_STATUS,": All NANC436 flag are OFF for SPID["+ spidValue + "]");
							}
						}
					}
					
					String voiceUriFlag = NANCSupportFlag.getInstance(spidValue).getVoiceURI();
					String mmsUriFlag = NANCSupportFlag.getInstance(spidValue).getMmsURI();
					String pocUriFlag = NANCSupportFlag.getInstance(spidValue).getPocURI();
					String presUriFlag = NANCSupportFlag.getInstance(spidValue).getPresURI();
					String smsUriFlag = NANCSupportFlag.getInstance(spidValue).getSmsURI();
					
					NodeList emptyVoiceUriNode = dom.getElementsByTagName(VOICEURINODE);
					// set the value attribute with empty node.
					removeEmptyNode(emptyVoiceUriNode);
					
					NodeList emptyMMSUriNode = dom.getElementsByTagName(MMSURINODE);
					// set the value attribute with empty node.
					removeEmptyNode(emptyMMSUriNode);
					
					NodeList emptyPocUriNode = dom.getElementsByTagName(POCURINODE);
					// set the value attribute with empty node.
					removeEmptyNode(emptyPocUriNode);
					
					NodeList emptyPresUriNode = dom.getElementsByTagName(PRESURINODE);
					// set the value attribute with empty node.
					removeEmptyNode(emptyPresUriNode);
					
					NodeList emptySMSUriNode = dom.getElementsByTagName(SMSURINODE);
					// set the value attribute with empty node.
					removeEmptyNode(emptySMSUriNode);
					
					if(gttData != null ){
						
						if(voiceUriFlag != null && voiceUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, VOICEURINODE, gttData.voiceuri, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": VoiceURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(mmsUriFlag != null && mmsUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, MMSURINODE, gttData.mmsuri, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": MMSURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(pocUriFlag != null && pocUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, POCURINODE, gttData.pocuri, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": PoCURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(presUriFlag != null && presUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, PRESURINODE, gttData.presuri, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": PresURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
												
						if(smsUriFlag != null && smsUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, SMSURINODE, gttData.smsuri, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": SMSURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
					}else{
						
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

							Debug.log(Debug.MSG_STATUS, ": LRN/GTT mapping doesnot exist for the LRN["
													+ lrnValue
													+ "] and spid ["
													+ spidValue + "]");
						}
						if(voiceUriFlag != null && voiceUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, VOICEURINODE, null, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": VoiceURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(mmsUriFlag != null && mmsUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, MMSURINODE, null, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": MMSURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(pocUriFlag != null && pocUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, POCURINODE, null, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": PoCURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(presUriFlag != null && presUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, PRESURINODE, null, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": PresURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(smsUriFlag != null && smsUriFlag.equals("1")){
							populateNancUriField(dom, subRequestValue, SMSURINODE, null, inputObj);
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": SMSURI flag are OFF for SPID["+ spidValue + "]");
							}
						}
					}					
					//SOA 5.6.5 requirement
					//Removed Canada specific check in SOA 5.6.8 release.
					
					
					String lastAltSpidFlag = String.valueOf(NANCSupportFlag.getInstance(spidValue).getLastAltSpidFlag());
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS, ": LastAlternativeSPID support flag for spid ["	+ spidValue	+ "] " +
						"is: [" + lastAltSpidFlag + "]");
					}
					
					if (lastAltSpidFlag != null && "1".equals(lastAltSpidFlag)) {							
						
						PopulateLastAltSPID(dom, gttData, inputObj, subRequestValue, LastAlternativeSPID);						
					}	
					
					String domainFlagForCustomfield = getDomainSetting(customerValue);
					
					if((domainFlagForCustomfield != null) && (domainFlagForCustomfield.indexOf("CN") > -1)){
						
						String spCustom1Flag = NANCSupportFlag.getInstance(spidValue).getSpCustom1Flag();
						String spCustom2Flag = NANCSupportFlag.getInstance(spidValue).getSpCustom2Flag();
						String spCustom3Flag = NANCSupportFlag.getInstance(spidValue).getSpCustom3Flag();
						
						if(spCustom1Flag != null && "1".equals(spCustom1Flag)){
							if(gttData != null){
								populateCustomField(dom, subRequestValue, SPCUSTOM1NODE, gttData.spCustom1, inputObj);
							}else{
								populateCustomField(dom, subRequestValue, SPCUSTOM1NODE, null, inputObj);
							}
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": spCustom1Flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(spCustom2Flag != null && "1".equals(spCustom2Flag)){
							if(gttData != null){
								populateCustomField(dom, subRequestValue, SPCUSTOM2NODE, gttData.spCustom2, inputObj);
							}else{
								populateCustomField(dom, subRequestValue, SPCUSTOM2NODE, null, inputObj);
							}
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": spCustom2Flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
						if(spCustom3Flag != null && "1".equals(spCustom3Flag)){
							if(gttData != null){
								populateCustomField(dom, subRequestValue, SPCUSTOM3NODE, gttData.spCustom3, inputObj);
							}else{
								populateCustomField(dom, subRequestValue, SPCUSTOM3NODE, null, inputObj);
							}
						}else{
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
								  Debug.log(Debug.MSG_STATUS,": spCustom3Flag are OFF for SPID["+ spidValue + "]");
							}
						}
						
					}else{
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							Debug.log(Debug.MSG_STATUS, "The customer[" + customerValue + "] has " +
									"GUIDISPLAYCONTROLKEY value[" + domainFlagForCustomfield+ "] configured in DOMAIN table. " +
									"so it is not belongs to Canada region. Hence, No SPCustom fields population done.");
						
						}
					}
					
				} else 
					{
		                if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
		                	Debug.log(Debug.MSG_STATUS,
										": API support flag for spid ["
												+ spidValue
												+ "] is: ["
												+ apiFlag
												+ "] , So no changes are required in API request XML");
					}
					}
				// END IF, check for API support flag
				//SOA 5.6.5 requirement
				
				String domainFlagForSPI = getDomainSetting(customerValue);
				
				if (domainFlagForSPI == null || !(domainFlagForSPI.indexOf("CN") > -1) || domainFlagForSPI.indexOf("US") != -1)
				{		
					String spiFlag = String.valueOf(NANCSupportFlag.getInstance(spidValue).getSimplePortIndicatorFlag());					
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS, ": Simple Port Indicator support flag for spid ["	+ spidValue	+ "] " +
						"is: [" + spiFlag + "]");
					}
					if(spiFlag != null && ( spiFlag.equals("1") || spiFlag.equals("2")) ){
						
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							Debug.log(Debug.MSG_STATUS, "Populating SimplePort Indicator fields.");
						}
						populateSimplePortIndicatorField(dom, subRequestValue, inputObj, spiFlag);
					}
				}else{
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS, "The customer[" + customerValue + "] has " +
								"GUIDISPLAYCONTROLKEY value[" + domainFlagForSPI+ "] configured in DOMAIN table. " +
								"so it belongs to Canada region. Hence, No Simple Port Indicator population will be done.");
					}
				}
				
				/* SOA 5.8.1 requirement- In API request if NNSP & ONSP are same ,
				 *  then convert LNPTYPE value from "lspp" to "lisp" */
								
				checkLnpTypeNodeVal (dom, subRequestValue, inputObj);							 
							
									
									
			}			
			inputObject.set(dom);

			CharSequence message = (CharSequence)inputObject.getString();
			message = SOAUtility.removeWhitespace(message).toString();
			
			message = RegexUtils.replaceAll("> <", message.toString(), ">\n<");
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(Debug.MSG_STATUS, ": Message object after formating :["+message+"]");
			
			inputObject.set(message);

		}catch (ResourceException e) {
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				 Debug.log(Debug.SYSTEM_CONFIG,": Connection could not be stablished. "+ e.getMessage());
				}
			} 
		catch (Exception e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				 Debug.log(Debug.SYSTEM_CONFIG,": Connection could not be stablished. "+ e.getMessage());
				}
			}
		finally
			{
				ThreadMonitor.stop( tmti );
			}
		
		
		if (dbCon != null) {
			try {
				DBConnectionPool.getInstance().releaseConnection(dbCon);
			} catch (Exception e) {

				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				 Debug.log(Debug.SYSTEM_CONFIG,": Connection could not be stablished. "+ e.toString());
				}
				
			}
		}              

		
		return (formatNVPair(inputObj));
		
	}
	
	
	/**
	 * 
	 */
	
	public void checkLnpTypeNodeVal(Document dom, String subRequestValue, MessageObject mso) throws MessageException
	{
		
		if (subRequestValue.equals(SvCreateRequest))
		{
			String nnsp = null;
			String onsp = null;
			String lnpVal =null;					
			NodeList newSP = dom.getElementsByTagName(SOAConstants.NEW_SP_NODE);
			if (newSP.getLength()>0){
				nnsp = newSP.item(0).getAttributes().item(0).getTextContent();
			}
			NodeList oldSP = dom.getElementsByTagName(SOAConstants.OLD_SP_NODE);
			if (oldSP.getLength()>0){
				onsp = oldSP.item(0).getAttributes().item(0).getTextContent();
			}
			NodeList nodelnp = dom.getElementsByTagName(SOAConstants.LNP_TYPE_NODE);				
			if(nodelnp.getLength()>0){
				lnpVal = nodelnp.item(0).getAttributes().item(0).getTextContent();
			}
			if (nnsp != null && onsp != null && lnpVal!= null && nnsp.equals(onsp) && lnpVal.equals("lspp"))
			{							 						
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(Debug.MSG_STATUS, ": The SvCreate Port-In Request NNSP & ONSP values are same, update LnpType value to 'lisp'");
				}
				
				XMLMessageGenerator xmg = new XMLMessageGenerator(dom);
				xmg.setValue(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
						SOAConstants.LNP_TYPE_NODE,"lisp");	
				if (xmg.nodeExists(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
						SOAConstants.NNSP_SIMPLEPORTINDICATOR_NODE))
					xmg.removeNode(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
							SOAConstants.NNSP_SIMPLEPORTINDICATOR_NODE);
				
				mso.set(dom);
			}					
							
		}			
		
		
	}
	
	/**
	 * 
	 */
	private void processSimplePortIndicator(Document dom, String customer, String subRequestValue, MessageObject obj) 
	throws MessageException, ProcessingException {
		
		try{
			//get the domain flag settings
			String domainFlag = getDomainSetting(customer);
			
			if (domainFlag == null || !(domainFlag.indexOf("CN") > -1) || domainFlag.indexOf("US") != -1)
			{	
				String spiFlag = String.valueOf(NANCSupportFlag.getInstance(spidValue).getSimplePortIndicatorFlag());					
				
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(Debug.MSG_STATUS, ": Simple Port Indicator support flag for spid ["	+ spidValue	+ "] " +
					"is: [" + spiFlag + "]");
				}
				
				if(spiFlag != null && ( spiFlag.equals("1") || spiFlag.equals("2")) ){
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS))
						Debug.log(Debug.MSG_STATUS, "Populating SimplePort Indicator fields...");

					populateSimplePortIndicatorField(dom, subRequestValue, obj, spiFlag);
				}
			}else{
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(Debug.MSG_STATUS, "The customer[" + customerValue + "] has " +
							"GUIDISPLAYCONTROLKEY value[" + domainFlag + "] configured in DOMAIN table. " +
							"so it belongs to Canada region. Hence, No Simple Port Indicator population will be done.");
				}
			}
		}catch(Exception e){
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "Could not process the SimplePort Indicator field population.");
		}
				
	}
	
	/**
	 * 
	 */
	private String getDomainSetting(String customer){
		
		String domainFlagValue=null;
		PreparedStatement pstmtDomain = null;
		ResultSet rsDomain = null;
		Connection dbConForDomain = null;
		
		
		try{
			
			if(customer != null && !customer.equals("")){
				
				dbConForDomain = DBConnectionPool.getInstance().acquireConnection();
				
				if (dbConForDomain == null) {
					// Throw the exception to the driver.
					throw new ProcessingException(
							"DB connection is not available");
				}
				pstmtDomain = dbConForDomain.prepareStatement(getDomainSetting);
				
				pstmtDomain.setString(1, customer);
				rsDomain = pstmtDomain.executeQuery();
				
				if (rsDomain.next()){
					domainFlagValue = rsDomain.getString("GUIDISPLAYCONTROLKEY");
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS,
								"The domain flag setting for customer["
										+ customerValue + "] is ["
										+ domainFlagValue + "]");
					}
				}
			}
		}catch (ResourceException re) {
	
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log(Debug.SYSTEM_CONFIG, "Could not get the connection from the pool:"+ re.getMessage());
			}
			
		}
		catch (SQLException e) {
	
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log(Debug.SYSTEM_CONFIG, "Could not execute the domain sql statement:"+ e.getMessage());
			}
			
		} catch (Exception e) {
	
			if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){
				Debug.log(Debug.SYSTEM_CONFIG, "Could not get the domain settings for customer["+customer+"]\n"+ e.getMessage());
			}
		}finally{
			try {
				if (pstmtDomain != null)
					pstmtDomain.close();
				if (rsDomain != null)
					rsDomain.close();
			} catch (SQLException sqle) {
				if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){
				 Debug.log(Debug.SYSTEM_CONFIG, "Could not execute the domain sql statement:"+sqle.getMessage());
			    }
			}
			
			if (dbConForDomain != null) {
				try {
					DBConnectionPool.getInstance().releaseConnection(dbConForDomain);
				} catch (Exception e) {

					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					 Debug.log(Debug.SYSTEM_CONFIG,": Connection could not be stablished. "+ e.toString());
					}
					
				}
			} 
			
		}	
		return domainFlagValue; 
	}
	/**
	 * This method tokenizes the input string and return an object for exsisting
	 * value in mpContext or messageobject.
	 * 
	 * @param locations
	 *            as a string
	 * @param mpContext
	 *            The context
	 * @param inputObject
	 *            Input message to process.
	 * 
	 * @return object
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 * @exception MessageException
	 *                Thrown if message is bad.
	 */
	protected String getValue(String locations) throws MessageException,
			ProcessingException {
		StringTokenizer st = new StringTokenizer(locations, SEPARATOR);

		String tok = null;
		// While tokens are available ...
		while (st.hasMoreTokens()) {
			tok = st.nextToken();

			// if token exists in the context
			if (exists(tok, mpContext, inputObject)) {
				// get the token value from context and return the value
				return (String) (get(tok, mpContext, inputObject));
			}
		}

		return null;
	}

	/**
	 * Processing the AltSPID and SvType node population in request XML.
	 * @param dom
	 * @param nodeName
	 * @param flag
	 * @param gttData object
	 * @param  message object
	 * @throws MessageException
	 * @throws ProcessingException
	 */
	private void processNodePopulation(Document dom, String nodeName,
			String flag, GttData gttData, MessageObject mso)
			throws MessageException, ProcessingException {

		if (flag != null && flag.equals("1")) {

			NodeList node = dom.getElementsByTagName(nodeName);

			// set the value attribute with empty node.
			removeEmptyNode(node);

			if (node.getLength() > 0) {

				// Check for AlternativeSPID node
				if (nodeName.equals(AlternativeSPID)) {

					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					 Debug.log(Debug.MSG_STATUS,": AlternativeSPID node is exist in the request XML.");
					}

					Node xmlNode = node.item(0);
					String attrValue = xmlNode.getAttributes().item(0)
							.getTextContent();

					// Wipe out the AltSPID node, if present in request XML with
					// hyphen value.
					if (attrValue.equals("-")) {
						// xmlNode.getAttributes().removeNamedItem(value);
						// Setting the dom document to the message object
						mso.set(dom);
					} else if (attrValue.equals("--?--")) {
						xmlNode.getParentNode().removeChild(xmlNode);
						// Setting the dom document to the message object
						mso.set(dom);
					} else if (attrValue.equals("")) {
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						  Debug.log(Debug.MSG_STATUS,": SvType node is exist in the request XML.");
						}

						// Calling method for populating node.
						populateEmptyNode(gttData, dom, nodeName, mso);
					}
				}// END IF, check for AlternativeSPID node.
				else if (nodeName.equals(SvType)) { // Check for SvType node
					Node xmlNode = node.item(0);
					String attrValue = xmlNode.getAttributes().item(0)
							.getTextContent();
					if (attrValue.equals("")) {
						// Calling method for populating node.
						populateEmptyNode(gttData, dom, nodeName, mso);
					}
				}// END IF, check for SvType node.
				else if (nodeName.equals(NumberPoolBlockSvType)) { // Check
					// for
					// NumberPoolBlockSvType
					// node
					Node xmlNode = node.item(0);
					String attrValue = xmlNode.getAttributes().item(0)
							.getTextContent();
					if (attrValue.equals("")) {
						// Calling method for populating node.
						populateEmptyNode(gttData, dom, nodeName, mso);
					}
				}
			}// END IF, check for node existence
			else if (node.getLength() == 0) {

				// Calling method for populating node.
				populateNode(gttData, dom, nodeName, mso);
			}
		} else {
			NodeList node = dom.getElementsByTagName(nodeName);
			removeEmptyNode(node);
		}
	}

	/**
	 * Method is used to populate the AltSPID and SvType nodes in the request
	 * XML (SV request and NPB request both)
	 */
	private void populateEmptyNode(GttData gttData, Document dom,
			String nodeName, MessageObject mso) throws ProcessingException {

		String dbValue = null;
		boolean flag = false;

		try {
			if (gttData != null) {
				if (nodeName.equals(SvType))
					dbValue = gttData.svType;
				else if (nodeName.equals(NumberPoolBlockSvType))
					dbValue = gttData.svType;
				else if (nodeName.equals(AlternativeSPID))
					dbValue = gttData.alternativeSpid;

				if (dbValue != null)
					flag = true;
			}

			if (flag == true) {

				if (dbValue != null) {

					NodeList node = dom.getElementsByTagName(nodeName);
					Node xmlNode = node.item(0);

					if (subRequestValue.equals(SvCreateRequest)) {
						// Adding child node to the request XML in case of SV
						// create request.

						// populate node in the request XML with DB retrieved
						// value
						xmlNode.getAttributes().item(0).setTextContent(dbValue);

					} else if (subRequestValue.equals(SvModifyRequest)) {
						// Adding child node to the request XML in case of SV
						// modify request.

						// populate node in the request XML with DB retrieved
						// value
						xmlNode.getAttributes().item(0).setTextContent(dbValue);

					} else if (subRequestValue
							.equals(NumberPoolBlockActivateRequest)) {
						// Adding child node to the request XML in case of NPB
						// activate request.

						// populate node in the request XML with DB retrieved
						// value
						xmlNode.getAttributes().item(0).setTextContent(dbValue);

					} else if (subRequestValue
							.equals(NumberPoolBlockModifyRequest)) {
						// Adding child node to the request XML in case of NPB
						// modify request.

						// populate node in the request XML with DB retrieved
						// value
						xmlNode.getAttributes().item(0).setTextContent(dbValue);

					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// End IF, check DBValue
				else {
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					 Debug.log(Debug.MSG_STATUS,": LRN/GTT Mapping does not exist in the database with ["+ nodeName + "] node No change in the request XML.");
					}
				}
			} else if (flag == false) {
				// Setting the dom document to the message object
				mso.set(dom);
			}

		} catch (Exception e) {
			if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)){
			 Debug.log(Debug.SYSTEM_CONFIG, ": Could not populate the node in the request xml.");
			}
		}

	}

	/**
	 * Method is used to populate the AltSPID and SvType nodes in the request
	 * XML (SV request and NPB request both)
	 */
	private void populateNode(GttData gttData, Document dom, String nodeName,
			MessageObject mso) throws ProcessingException {

		String targetAttribute = value;
		String dbValue = null;

		boolean flag = false;

		Element xmlElement = null;
		try {

			if (gttData != null) {
				if (nodeName.equals(SvType))
					dbValue = gttData.svType;
				else if (nodeName.equals(NumberPoolBlockSvType))
					dbValue = gttData.svType;
				else if (nodeName.equals(AlternativeSPID))
					dbValue = gttData.alternativeSpid;

				if (dbValue != null)
					flag = true;
			}

			if (flag == true) {

				if (dbValue != null) {

					NodeList node = dom.getElementsByTagName(nodeName);

					if (node.getLength() == 0) {
						xmlElement = dom.createElement(nodeName);
						xmlElement.setAttribute(targetAttribute, dbValue);
					}

					if (subRequestValue.equals(SvCreateRequest)) {
						// Adding child node to the request XML in case of SV
						// create request.

						Element svCreateRequest = (Element) dom
								.getElementsByTagName(SvCreateRequest)
								.item(0);
						svCreateRequest.appendChild(xmlElement);

					} else if (subRequestValue.equals(SvModifyRequest)) {
						// Adding child node to the request XML in case of SV
						// modify request.
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(DataToModify).item(0);
						svModifyRequest.appendChild(xmlElement);

					} else if (subRequestValue
							.equals(NumberPoolBlockActivateRequest)) {
						// Adding child node to the request XML in case of NPB
						// activate request.

						Element npbActivaterequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockActivateRequest).item(
										0);
						npbActivaterequest.appendChild(xmlElement);
					} else if (subRequestValue
							.equals(NumberPoolBlockModifyRequest)) {
						// Adding child node to the request XML in case of NPB
						// modify request.

						Element npbModifyrequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						npbModifyrequest.appendChild(xmlElement);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// End IF, check DBValue
				else if (nodeName.equals(AlternativeSPID)) {
					// If db value is null and incoming node is AlternativeSPID.
					// Populate AlternativeSPID node with blank.

					if (subRequestValue.equals(SvModifyRequest)
							|| subRequestValue
									.equals(NumberPoolBlockModifyRequest)) {
						mso.set(dom);
					}
				}// END Else IF, check DB value and node name for
				// AlternativeSPID
			} else if (flag == false) {
				// Setting the dom document to the message object
				mso.set(dom);
			}

		} catch (Exception e) {
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG, ": Could not populate node in the request xml.");
			}

		}

	}

	/**
	 * ProcessEndUserData method is used to get the DB result value and call
	 * method to populate DB values in the request XML.
	 */
	private void ProcessEndUserData(Document dom, GttData gttData,
			MessageObject mso) {

		try {
			
			String dbbillingIdValue = null;
			
			String dbEndUserTypeValue = null;
			
			String dbEndUserLocValue = null;
			
			if( gttData != null )
			{				
				dbbillingIdValue = gttData.billingId;
				dbEndUserTypeValue = gttData.endUserLocationType;
				dbEndUserLocValue = gttData.endUserLocationValue;
			}				
			
			String billingId = "BillingId";			
			String endUserLocationType = "EndUserLocationType";
			String endUserLocationValue = "EndUserLocationValue";

			populateEndUserData(dbbillingIdValue, dom, subRequestValue,
					billingId, mso);
			populateEndUserData(dbEndUserTypeValue, dom, subRequestValue,
					endUserLocationType, mso);
			populateEndUserData(dbEndUserLocValue, dom, subRequestValue,
					endUserLocationValue, mso);

		} catch (Exception e) {

            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){            	
		    	Debug.log(Debug.SYSTEM_CONFIG,": Could not process to populate BillingId, EndUserLocationType and EndUserLocationvalue in the request XML.");
			}
		}
	}

	/**
	 * Method is used to populate the BillingId, EndUserLocationType and
	 * EndUserLocationValue nodes in the request XML (SV request)
	 */
	private void populateEndUserData(String dbData, Document dom,
			String subRequestValue, String nodeName, MessageObject mso) {		

		String targetAttribute = value;

		Element xmlElement = dom.createElement(nodeName);

		try {
			NodeList node = dom.getElementsByTagName(nodeName);
			// set the value attribute with empty node.
			removeEmptyNode(node);

			if (subRequestValue.equals(SvCreateRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {					

						if (dbData != null) {
							// populate node in the request XML with DB
							// retrieved value
							xmlElement.setAttribute(targetAttribute, dbData);
							Element svCreateRequest = (Element) dom
									.getElementsByTagName(SvCreateRequest)
									.item(0);
							svCreateRequest.appendChild(xmlElement);

						}// END IF, check DB value					
					else {
						// Setting the dom document to the message object
						mso.set(dom);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF, check for node existence
				else if (dom.getElementsByTagName(nodeName).getLength() > 0) {
					// Populating empty node with the DB mapped value in the
					// request XML
					node = dom.getElementsByTagName(nodeName);
					Node xmlNode = node.item(0);
					String attrValue = xmlNode.getAttributes().item(0)
							.getTextContent();					

						if (dbData != null) {
							if (attrValue.equals("")) {
								// populate node in the request XML with DB
								// retrieved value
								xmlNode.getAttributes().item(0).setTextContent(
										dbData);
							}
						}// END IF, check DB value					
					else {
						// Setting the dom document to the message object
						mso.set(dom);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// Else
			} else if (subRequestValue.equals(SvModifyRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {					
						if (dbData != null) {							
							// populate node in the request XML with DB
							// retrieved value
							xmlElement.setAttribute(targetAttribute, dbData);
							Element svModifyRequest = (Element) dom
									.getElementsByTagName(DataToModify).item(
											0);
							svModifyRequest.appendChild(xmlElement);
						} else {
							// populate node in the request XML with '-' value							
							xmlElement.setAttribute(targetAttribute, "-");
							Element svModifyRequest = (Element) dom
									.getElementsByTagName(DataToModify).item(
											0);
							svModifyRequest.appendChild(xmlElement);

						}// END IF, check for db value										
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF, check node existence
				else if (dom.getElementsByTagName(nodeName).getLength() > 0) {					
					node = dom.getElementsByTagName(nodeName);
					Node xmlNode = node.item(0);
					String attrValue = xmlNode.getAttributes().item(0)
							.getTextContent();
					
						if (attrValue.equals("")) {
							if (dbData != null) {								
								xmlNode.getAttributes().item(0).setTextContent(
										dbData);
							} else {								
								xmlNode.getAttributes().item(0).setTextContent(
										"-");
							}

						}
					}
				}			

		} catch (Exception e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG, ":Error Message:" + e.getMessage());
			}
		}
	}

	/**
	 * Method is used to get the GTT data values from database and call method
	 * to populate GTT data in the request XML.
	 */
	private void ProcessGTTData(Document dom, GttData gttData, MessageObject mso) {

		String targetAttribute = value;
		try {

			// Creating hash map to store the DB values with key as XML node.
			HashMap<String, String> gttMAP = new HashMap<String, String>();

			boolean flag = false;

			if (gttData != null) {
				String classDPC = gttData.classDpc;
				gttMAP.put(ClassDPC, classDPC);

				String classSSN = gttData.classSsn;
				gttMAP.put(ClassSSN, classSSN);

				String cnamDPC = gttData.cnamDpc;
				gttMAP.put(CnamDPC, cnamDPC);

				String cnamSSN = gttData.cnamSsn;
				gttMAP.put(CnamSSN, cnamSSN);

				String isvmDPC = gttData.isvmDpc;
				gttMAP.put(IsvmDPC, isvmDPC);

				String isvmSSN = gttData.isvmSsn;
				gttMAP.put(IsvmSSN, isvmSSN);

				String lidbDPC = gttData.lidbDpc;
				gttMAP.put(LidbDPC, lidbDPC);

				String lidbSSN = gttData.lidbSsn;
				gttMAP.put(LidbSSN, lidbSSN);

				String wsmscDPC = gttData.wsmscDpc;
				gttMAP.put(WsmscDPC, wsmscDPC);

				String wsmscSSN = gttData.wsmscSsn;
				gttMAP.put(WsmscSSN, wsmscSSN);

				flag = true;
			}

			if (subRequestValue.equals(SvCreateRequest)
					|| subRequestValue.equals(NumberPoolBlockActivateRequest)) {

				// Check If GTTData root node exist in the request XML
				NodeList gttDataNodes = dom.getElementsByTagName(GTTData);

				if (gttDataNodes.getLength() == 0) { // GTTData root node does
					// not exist in the
					// request XML

					// Creating GTTData root node.
					Element xmlGTTRootElement = dom.createElement(GTTData);

					if (flag == true) {
						for (int i = 0; i < gttDataArray.length; i++) {
							// Do not allow if null is returned.
							if ((gttMAP.get(gttDataArray[i])) != null) {


								// Creating each GTT element
								Element xmlGTTDataElement = dom
										.createElement(gttDataArray[i]);
								xmlGTTDataElement
										.setAttribute(targetAttribute, (gttMAP
												.get(gttDataArray[i])
												.toString()));

								// Appending each GTT element to the GTTData
								// root node
								xmlGTTRootElement
										.appendChild(xmlGTTDataElement);
							} // End IF
						} // END For Loop
						
						NodeList nl = xmlGTTRootElement.getChildNodes();
						//If any of the GTT Element node got populated in the <GTTData> root node, then add <GTTData> inside the request XML.
						if(nl.getLength() > 0){
							
						if (subRequestValue.equals(SvCreateRequest)) {
							// Adding GTTData node to the request XML in case of
							// SV create request.

							Element svCreateRequest = (Element) dom
									.getElementsByTagName(SvCreateRequest)
									.item(0);
							svCreateRequest.appendChild(xmlGTTRootElement);

						} else if (subRequestValue
								.equals(NumberPoolBlockActivateRequest)) {
							// Adding GTTData node to the request XML in case of
							// NPB activate request.

							Element npbActivaterequest = (Element) dom
									.getElementsByTagName(
											NumberPoolBlockActivateRequest)
									.item(0);
							npbActivaterequest.appendChild(xmlGTTRootElement);

						}
						}						

						// Setting the dom document to the message object
						mso.set(dom);
					} // End IF, check Resulset data
					else {
						// Setting the dom document to the message object
						mso.set(dom);
					}

				} else if (gttDataNodes.getLength() > 0) { // GTTData root node
					// exist in the
					// request XML.
					if (flag == true) {

						for (int i = 0; i < gttDataArray.length; i++) {

							// Check the existence of GTT Element.
							NodeList gttElement = dom
									.getElementsByTagName(gttDataArray[i]);

							// set the value attribute with empty node.
							removeEmptyNode(gttElement);

							if (gttElement.getLength() == 0) { // If GTT element
								// is missing


								// populate missing GTT element in the request
								// XML.
								// Do not allow if null is returned.
								if ((gttMAP.get(gttDataArray[i])) != null) {

									// Creating each GTT element
									Element xmlGTTDataElement = dom
											.createElement(gttDataArray[i]);
									xmlGTTDataElement.setAttribute(
											targetAttribute, (gttMAP
													.get(gttDataArray[i])
													.toString()));

									if (subRequestValue
											.equals(SvCreateRequest)
											|| subRequestValue
													.equals(NumberPoolBlockActivateRequest)) {
										// Adding GTTData node to the request
										// XML in case of SV create request.

										Element svCreateRequest = (Element) dom
												.getElementsByTagName(GTTData)
												.item(0);
										svCreateRequest
												.appendChild(xmlGTTDataElement);
									}
								} // End IF
							}// END IF, check GTT element presence
							else if (gttElement.getLength() > 0) {

								// populate the empty GTT element with DB value
								// in the request XML.
								NodeList node = dom
										.getElementsByTagName(gttDataArray[i]);
								Node xmlNode = node.item(0);
								String attrValue = xmlNode.getAttributes()
										.item(0).getTextContent();

								if ((gttMAP.get(gttDataArray[i])) != null) {

									if (attrValue.equals("")) {

										xmlNode
												.getAttributes()
												.item(0)
												.setTextContent(
														(gttMAP
																.get(gttDataArray[i])
																.toString()));
									}// END IF
								}// END IF
							}// END Else IF, check for GTT element existence
						}// END For Loop
						// Setting the dom document to the message object
						mso.set(dom);
					}// END IF
					else {
						// Setting the dom document to the message object
						mso.set(dom);
					}
				}// END Else IF, check GTTData node existence
			} else if (subRequestValue.equals(SvModifyRequest)
					|| subRequestValue.equals(NumberPoolBlockModifyRequest)) {

				if (flag == true) {
	
					for (int i = 0; i < gttDataArray.length; i++) {
						// Check the existence of GTT Element.
						NodeList gttElement = dom
								.getElementsByTagName(gttDataArray[i]);

						// set the value attribute with empty node.
						removeEmptyNode(gttElement);

						if (gttElement.getLength() == 0) {
							// If GTT element is missing
							if ((gttMAP.get(gttDataArray[i])) != null) {
								// If null is not returned then populate GTT
								// data with the retrieved database data
								// Creating each GTT element
								Element xmlGTTDataElement = dom
										.createElement(gttDataArray[i]);
								xmlGTTDataElement
										.setAttribute(targetAttribute, (gttMAP
												.get(gttDataArray[i])
												.toString()));

								if (subRequestValue.equals(SvModifyRequest)) {

									Element svModifyRequest = (Element) dom
											.getElementsByTagName(
													DataToModify).item(0);
									svModifyRequest
											.appendChild(xmlGTTDataElement);


								} else if (subRequestValue
										.equals(NumberPoolBlockModifyRequest)) {

									Element npbActivaterequest = (Element) dom
											.getElementsByTagName(
													NumberPoolBlockModifyRequest)
											.item(0);
									npbActivaterequest
											.appendChild(xmlGTTDataElement);
								}// END Else IF, check SubRequestValue

							} else if (((gttMAP.get(gttDataArray[i])) == null)) {

								if (gttDataArray[i].equals(WsmscDPC)
										|| gttDataArray[i].equals(WsmscSSN)) {
									
									if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
										  Debug.log(Debug.SYSTEM_CONFIG, ":Do not populate Hyphen (-) for "+gttDataArray[i]+
												  " GTTData node.");
										}
									
								} else {
									// If null is returned the populate GTT data
									// with the hyphen ('-')
									// Creating each GTT element
									Element xmlGTTDataElement = dom
											.createElement(gttDataArray[i]);
									xmlGTTDataElement.setAttribute(
											targetAttribute, "-");

									if (subRequestValue
											.equals(SvModifyRequest)) {

										Element svModifyRequest = (Element) dom
												.getElementsByTagName(
														DataToModify).item(0);
										svModifyRequest
												.appendChild(xmlGTTDataElement);

									} else if (subRequestValue
											.equals(NumberPoolBlockModifyRequest)) {

										Element npbActivaterequest = (Element) dom
												.getElementsByTagName(
														NumberPoolBlockModifyRequest)
												.item(0);
										npbActivaterequest
												.appendChild(xmlGTTDataElement);

									}// END Else IF, check SubRequestValue

								}

							}// END Else IF
						} else if (gttElement.getLength() > 0) {

							// populate empty GTT element with DB value in the
							// request XML.
							NodeList node = dom
									.getElementsByTagName(gttDataArray[i]);
							Node xmlNode = node.item(0);
							String attrValue = xmlNode.getAttributes().item(0)
									.getTextContent();
							if (attrValue.equals("")) {
								if ((gttMAP.get(gttDataArray[i])) != null) {
									if (attrValue.equals("")) {

										xmlNode
												.getAttributes()
												.item(0)
												.setTextContent(
														(gttMAP
																.get(gttDataArray[i])
																.toString()));

									}// END IF
								} else {
									if (!(xmlNode.getNodeName()
											.equalsIgnoreCase(WsmscDPC) || xmlNode
											.getNodeName().equalsIgnoreCase(
													WsmscSSN))) {
										// Do not populate WsmscDPC and WsmscSSN
										// in the request XML

										xmlNode.getAttributes().item(0)
												.setTextContent("-");

									} else {
										// Removing the existing node WsmscDPC
										// and WsmscSSN from the
										// request XML if attribute value is
										// empty
										xmlNode.getParentNode().removeChild(
												xmlNode);
									}
								}

							}

						}
					}// End For Loop
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF
				else if (!flag) {
					if (subRequestValue.equals(SvModifyRequest)) {
						// If mapping does not exist (no GTTID is present)
						String targetNode = DataToModify;
						for (int j = 0; j < gttDataArrayForSvModify.length; j++) {

							// Check the existence of empty GTT Element.
							NodeList gttElement = dom
									.getElementsByTagName(gttDataArrayForSvModify[j]);

							Node gttElementNode = gttElement.item(0);
							if (gttElementNode == null) {
								// Populating missing element
								// set empty nodes
								setEmptyNode(gttDataArrayForSvModify[j], dom,
										targetNode);
							} else if (gttElementNode.hasAttributes()) {
								if (gttElementNode.getAttributes().item(0)
										.getTextContent().equals("")) {
									// removing empty nodes
									removeEmptyNode(gttElement);
									// set empty nodes
									setEmptyNode(gttDataArrayForSvModify[j],
											dom, targetNode);
								} else {
									// ignoring pre-populated nodes
									if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
										  Debug.log(Debug.SYSTEM_CONFIG, ":Ignoring GTTdata elements if they " +
										  		"have proper attribute value");
										}
								}
							} else {
								// removing empty nodes
								removeEmptyNode(gttElement);
								// set empty nodes
								setEmptyNode(gttDataArrayForSvModify[j], dom,
										targetNode);
							}
						}
					} else if (subRequestValue
							.equals(NumberPoolBlockModifyRequest)) {
						// If mapping does not exist (no GTTID is present)
						for (int j = 0; j < gttDataArrayForNPBModify.length; j++) {

							// Check the existence of empty GTT Element.
							NodeList gttElement = dom
									.getElementsByTagName(gttDataArrayForNPBModify[j]);

							Node gttElementNode = gttElement.item(0);
							if (gttElementNode == null) {
								// Populating missing element
								// set empty nodes
								setEmptyNode(gttDataArrayForSvModify[j], dom,
										subRequestValue);
							} else if (gttElementNode.hasAttributes()) {
								if (gttElementNode.getAttributes().item(0)
										.getTextContent().equals("")) {
									// removing empty nodes
									removeEmptyNode(gttElement);
									// set empty nodes
									setEmptyNode(gttDataArrayForSvModify[j],
											dom, subRequestValue);

								} else {
									// ignoring pre-populated nodes
									if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
										  Debug.log(Debug.SYSTEM_CONFIG, ":Ignoring GTTdata elements if they " +
										  		"have proper attribute value");
										}
								}
							} else {
								// removing empty nodes
								removeEmptyNode(gttElement);
								// set empty nodes
								setEmptyNode(gttDataArrayForSvModify[j], dom,
										subRequestValue);
							}
						}
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}
			}// END Else IF

		} catch (Exception e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG, ": Could not populate the GTT data in the request XML.");
			}
		}
	}

	/**
	 * Method is used to populate the BillingId, EndUserLocationType and
	 * EndUserLocationValue nodes in the request XML (SV request)
	 */
	private void populateNanc436DataForNPB(Document dom,
			String subRequestValue, String nodeName, String nodeValue,
			MessageObject mso) {

		String targetAttribute = value;

		Element xmlElement = dom.createElement(nodeName);

		try {
			if (subRequestValue.equals(NumberPoolBlockActivateRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element npbActivate = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockActivateRequest).item(
										0);
						npbActivate.appendChild(xmlElement);
					}
					mso.set(dom);
				}
			} else if (subRequestValue.equals(NumberPoolBlockModifyRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						svModifyRequest.appendChild(xmlElement);
					} else {
						// populate node in the request XML with '-' value
						xmlElement.setAttribute(targetAttribute, "-");
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						svModifyRequest.appendChild(xmlElement);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF, check node existence
			}// END Else IF

		} catch (Exception e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG,":Error Message:" + e.getMessage());
			}
		}
	}
	
	/**
	 * This method is used to populate the NANC Uri fields.
	 * @param dom
	 * @param subRequestValue
	 * @param nodeName
	 * @param nodeValue
	 * @param mso
	 * @throws MessageException
	 * @throws ProcessingException
	 */
	private void populateNancUriField(Document dom,
			String subRequestValue, String nodeName, String nodeValue,
			MessageObject mso)
			throws MessageException, ProcessingException {
		String targetAttribute = value;

		Element xmlElement = dom.createElement(nodeName);

		try {
			if (subRequestValue.equals(SvCreateRequest)){
				
				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element svcreate = (Element) dom
								.getElementsByTagName(
										SvCreateRequest).item(
										0);
						svcreate.appendChild(xmlElement);
					}
					mso.set(dom);
				}
			}
			else if(subRequestValue.equals(SvModifyRequest)){
				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(
										DataToModify).item(0);
						svModifyRequest.appendChild(xmlElement);
					} else {
						// populate node in the request XML with '-' value
						xmlElement.setAttribute(targetAttribute, "-");
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(
										DataToModify).item(0);
						svModifyRequest.appendChild(xmlElement);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF, check node existence
			}
			else if (subRequestValue.equals(NumberPoolBlockActivateRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element npbActivate = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockActivateRequest).item(
										0);
						npbActivate.appendChild(xmlElement);
					}
					mso.set(dom);
				}
			} else if (subRequestValue.equals(NumberPoolBlockModifyRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element npbModifyRequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						npbModifyRequest.appendChild(xmlElement);
					} else {
						// populate node in the request XML with '-' value
						xmlElement.setAttribute(targetAttribute, "-");
						Element npbModifyRequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						npbModifyRequest.appendChild(xmlElement);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF, check node existence
			}// END Else IF

		} catch (Exception e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG,":Error Message:" + e.getMessage());
			}
		}
		
	}
	
	/**
	 * Populate LastAlternativeSPID field.
	 */
	private void PopulateLastAltSPID(Document dom, GttData gttData, MessageObject mso, String subRequestValue, String nodeName){
		
		String dbLastAlternativeSpid = null;
		String targetAttribute = value;
		
		try{
			
			if( gttData != null )
			{				
				dbLastAlternativeSpid = gttData.lastAlternativeSpid;
			}	
		
			Element xmlElement = dom.createElement(nodeName);
			
			NodeList node = dom.getElementsByTagName(nodeName);
			
			//remove empty attribute nodes from XML.
			removeEmptyNode(node);
			
			if(subRequestValue.equals(SvCreateRequest)){
				
				if(node.getLength() == 0){
					
					if(dbLastAlternativeSpid != null){
				
						xmlElement.setAttribute(targetAttribute, dbLastAlternativeSpid);
						Element elem = (Element) dom.getElementsByTagName(SvCreateRequest).item(0);
						elem.appendChild(xmlElement);
						mso.set(dom);
					}
				}
			}
			if(subRequestValue.equals(SvModifyRequest)){
				
				if(dbLastAlternativeSpid != null){
					
					xmlElement.setAttribute(targetAttribute, dbLastAlternativeSpid);
					Element elem = (Element) dom.getElementsByTagName(DataToModify).item(0);
					elem.appendChild(xmlElement);
					mso.set(dom);
				}
				else{
					
					xmlElement.setAttribute(targetAttribute, "-");
					Element elem = (Element) dom.getElementsByTagName(DataToModify).item(0);
					elem.appendChild(xmlElement);
					mso.set(dom);
				}				
			}
			if(subRequestValue.equals(NumberPoolBlockActivateRequest)){
				
				if(node.getLength() == 0){
					
					if(dbLastAlternativeSpid != null){
				
						xmlElement.setAttribute(targetAttribute, dbLastAlternativeSpid);
						Element elem = (Element) dom.getElementsByTagName(NumberPoolBlockActivateRequest).item(0);
						elem.appendChild(xmlElement);
						mso.set(dom);
					}					
				}
			}
			if(subRequestValue.equals(NumberPoolBlockModifyRequest)){
				if(dbLastAlternativeSpid != null){
					
					xmlElement.setAttribute(targetAttribute, dbLastAlternativeSpid);
					Element elem = (Element) dom.getElementsByTagName(NumberPoolBlockModifyRequest).item(0);
					elem.appendChild(xmlElement);
					mso.set(dom);
				}
				else{
					
					xmlElement.setAttribute(targetAttribute, "-");
					Element elem = (Element) dom.getElementsByTagName(NumberPoolBlockModifyRequest).item(0);
					elem.appendChild(xmlElement);
					mso.set(dom);
				}
				
			}
		}catch(Exception e){
			if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)){
				Debug.log(Debug.SYSTEM_CONFIG, "Error Message:" + e.getMessage());
			}
			
		}		
	}
	
	/**
	 * This method is used to populate the simple port indicator fields.
	 */
	private void populateSimplePortIndicatorField(Document dom, 
			String SubRequestValue, MessageObject mso, String spiflag)
			throws MessageException, ProcessingException {
		
		String targetAttribute = value;
		String nodeDefaultvalue = "0";
		String lnpTypeValue = null;
		
		Element nnspSpiElement = dom.createElement(NNSPSimplePortIndicator);
		Element onspSpiElement = dom.createElement(ONSPSimplePortIndicator);
		
		NodeList nnspSpiNode = dom.getElementsByTagName(NNSPSimplePortIndicator);
		NodeList onspSpiNode = dom.getElementsByTagName(ONSPSimplePortIndicator);
		
		XMLMessageParser xmp = new XMLMessageParser(dom);
		String lnpType = SOAConstants.REQUEST_BODY_PATH + "." + subRequestValue + "." + "LnpType";
		
		if(xmp.exists(lnpType)){
			lnpTypeValue = xmp.getValue(lnpType);			
		}
		// If Simple port indicator flag for a SPID is set to "2" then default
		// value "1" will be populated in the request
		if(spiflag.equals("2")){
			nodeDefaultvalue = "1";
		}
		try{
			if (subRequestValue.equals(SvCreateRequest)){
				
				if(lnpTypeValue != null && !lnpTypeValue.equals("lisp")){
					
					//Remove empty node if present.
					removeEmptyNode(onspSpiNode);
					removeEmptyNode(nnspSpiNode);
					
					// check if node exist in the request
					if(nnspSpiNode.getLength() == 0){
						//populate the node with default value 0.
						nnspSpiElement.setAttribute(targetAttribute, nodeDefaultvalue);
						Element svCreateRequestElement = (Element) dom.getElementsByTagName(SvCreateRequest).item(0);
						svCreateRequestElement.appendChild(nnspSpiElement);
						mso.set(dom);
					}
				}				
			}
			if (subRequestValue.equals(SvReleaseRequest) || subRequestValue.equals(SvReleaseInConflictRequest)){				
				//Remove empty node if present.
				removeEmptyNode(onspSpiNode);
				removeEmptyNode(nnspSpiNode);
				
				// check if node exist in the request
				if(onspSpiNode.getLength() == 0){
					//populate the node with default value 0.
					onspSpiElement.setAttribute(targetAttribute, nodeDefaultvalue);
					
					if (subRequestValue.equals(SvReleaseRequest)){
						Element svCreateRequestElement = (Element) dom.getElementsByTagName(SvReleaseRequest).item(0);
						svCreateRequestElement.appendChild(onspSpiElement);
					}
					if (subRequestValue.equals(SvReleaseInConflictRequest)){
						Element svCreateRequestElement = (Element) dom.getElementsByTagName(SvReleaseInConflictRequest).item(0);
						svCreateRequestElement.appendChild(onspSpiElement);
					}					
					mso.set(dom);
				}				
			}			
		}catch(Exception e){
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				  Debug.log(Debug.SYSTEM_CONFIG,":Could not populate the Simple Port Indicator node in the request XML." +
				  		"\nError Message:" + e.getMessage());
				}
		}
	}
	
	/**
	 * This method is used to populate the SPCUSTOM fields.
	 * @param dom
	 * @param subRequestValue
	 * @param nodeName
	 * @param nodeValue
	 * @param mso
	 * @throws MessageException
	 * @throws ProcessingException
	 */
	private void populateCustomField(Document dom,
			String subRequestValue, String nodeName, String nodeValue,
			MessageObject mso)
			throws MessageException, ProcessingException {
		String targetAttribute = value;

		Element xmlElement = dom.createElement(nodeName);
		NodeList node = dom.getElementsByTagName(nodeName);
		
		//remove empty attribute nodes from XML.
		removeEmptyNode(node);
		try {
			if (subRequestValue.equals(SvCreateRequest)){
				
				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element svcreate = (Element) dom
								.getElementsByTagName(
										SvCreateRequest).item(
										0);
						svcreate.appendChild(xmlElement);
					}
					mso.set(dom);
				}
			}
			else if(subRequestValue.equals(SvModifyRequest)){
				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(
										DataToModify).item(0);
						svModifyRequest.appendChild(xmlElement);
					} else {
						// populate node in the request XML with '-' value
						xmlElement.setAttribute(targetAttribute, "-");
						Element svModifyRequest = (Element) dom
								.getElementsByTagName(
										DataToModify).item(0);
						svModifyRequest.appendChild(xmlElement);
					}
					// Setting the dom document to the message object
					mso.set(dom);

				}// END IF, check node existence
			}
			else if (subRequestValue.equals(NumberPoolBlockActivateRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element npbActivate = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockActivateRequest).item(
										0);
						npbActivate.appendChild(xmlElement);
					}
					mso.set(dom);
				}
			} else if (subRequestValue.equals(NumberPoolBlockModifyRequest)) {

				// check if node exist in the request
				if (dom.getElementsByTagName(nodeName).getLength() == 0) {
					if (nodeValue != null) {
						// populate node in the request XML with DB retrieved
						// value
						xmlElement.setAttribute(targetAttribute, nodeValue);
						Element npbModifyRequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						npbModifyRequest.appendChild(xmlElement);
					} else {
						// populate node in the request XML with '-' value
						xmlElement.setAttribute(targetAttribute, "-");
						Element npbModifyRequest = (Element) dom
								.getElementsByTagName(
										NumberPoolBlockModifyRequest).item(0);
						npbModifyRequest.appendChild(xmlElement);
					}
					// Setting the dom document to the message object
					mso.set(dom);
				}// END IF, check node existence
			}// END Else IF

		} catch (Exception e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG,":Error Message:" + e.getMessage());
			}
		}
		
	}


	/**
	 * Remove the empty node from the request XML Empty node can be : <NODE/> or
	 * <NODE></NODE> or <NODE value=""/>
	 */
	private void removeEmptyNode(NodeList node) {
		if (node.item(0) != null) {
			// checks if this node has attributes or empty attribute value.
			if (!node.item(0).hasAttributes()) {
				node.item(0).getParentNode().removeChild(node.item(0));
			} else {
				if (node.item(0).getAttributes().item(0).getTextContent()
						.equals("")) {
					node.item(0).getParentNode().removeChild(node.item(0));
				}
			}
		}
	}

	/**
	 * Set node for empty nodes
	 */
	private void setEmptyNode(String item, Document dom, String targetNode) {

		if (!(item.equalsIgnoreCase(WsmscSSN) || item
				.equalsIgnoreCase(WsmscDPC))) {
			Element xmlGTTDataElement = dom.createElement(item);
			xmlGTTDataElement.setAttribute(value, "-");
			Element modifyRequest = (Element) dom.getElementsByTagName(
					targetNode).item(0);
			modifyRequest.appendChild(xmlGTTDataElement);
            
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				Debug.log(Debug.MSG_STATUS, ": Populating [" + item + "] GTT element inside the " + subRequestValue + " node with ('-').");
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
	private String getValue(String locations, MessageProcessorContext mpContext,
            MessageObject inputObject) throws MessageException,
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

    
	private static class GttData {

		private String classDpc;
		private String classSsn;
		private String cnamDpc;
		private String cnamSsn;
		private String isvmDpc;
		private String isvmSsn;
		private String lidbDpc;
		private String lidbSsn;
		private String wsmscDpc;
		private String wsmscSsn;
		private String svType;
		private String alternativeSpid;
		private String endUserLocationValue;
		private String endUserLocationType;
		private String billingId;
		private String lata;
		private String voiceuri;
		private String mmsuri;
		private String pocuri;
		private String presuri;
		private String smsuri;
		private String lastAlternativeSpid;
		private String spCustom1;
		private String spCustom2;
		private String spCustom3;
		
		public GttData(String classDpc, String classSsn, String cnamDpc,
				String cnamSsn, String isvmDpc, String isvmSsn, String lidbDpc,
				String lidbSsn, String wsmscDpc, String wsmscSsn,
				String svType, String alternativeSpid,
				String endUserLocationValue, String endUserLocationType,
				String billingId, String lata, String voiceuri, String mmsuri,
				String pocuri, String presuri, String smsuri, String lastAlternativeSpid, String spCustom1,
				String spCustom2, String spCustom3) throws FrameworkException {

			this.classDpc = classDpc;
			this.classSsn = classSsn;
			this.cnamDpc = cnamDpc;
			this.cnamSsn = cnamSsn;
			this.isvmDpc = isvmDpc;
			this.isvmSsn = isvmSsn;
			this.lidbDpc = lidbDpc;
			this.lidbSsn = lidbSsn;
			this.wsmscDpc = wsmscDpc;
			this.wsmscSsn = wsmscSsn;
			this.svType = svType;
			this.alternativeSpid = alternativeSpid;
			this.endUserLocationValue = endUserLocationValue;
			this.endUserLocationType = endUserLocationType;
			this.billingId = billingId;
			this.lata = lata;
			this.voiceuri = voiceuri;
			this.mmsuri = mmsuri;
			this.pocuri = pocuri;
			this.presuri = presuri;
			this.smsuri = smsuri;
			this.lastAlternativeSpid = lastAlternativeSpid;
			this.spCustom1 = spCustom1;
			this.spCustom2 = spCustom2;
			this.spCustom3 = spCustom3;
		}

	}

	/**
	 * This method main is used here for Unit Testing the Message Processor
	 * APIRequestPopulationNANCSupport
	 * 
	 * @param args
	 *            void
	 */
	public static void main(String[] args) {

		/* Initialize Debug logging for deguging purpose */
		Hashtable htLogTable = new Hashtable();
		htLogTable.put(Debug.DEBUG_LOG_LEVELS_PROP, "ALL");
		htLogTable.put(Debug.LOG_FILE_NAME_PROP,
				"c:/runtime/APIRequestPopulationNANCSupport.log");
		htLogTable.put(Debug.MAX_DEBUG_WRITES_PROP, "10000");

		/* Turn on maximum diagnostic logging. */
		Debug.showLevels();
		Debug.configureFromProperties(htLogTable);
		Debug.enable(Debug.ALL_ERRORS);
		Debug.enable(Debug.ALL_WARNINGS);
		Debug.enable(Debug.MSG_STATUS);
		Debug.enable(Debug.EXCEPTION_STACK_TRACE);

		try {

			/* Database properties */
			DBInterface.initialize(
					"jdbc:oracle:thin:@192.168.148.34:1521:NOIDADB",
					"devsoa_prav", "devsoa_prav");
			Debug
					.log(Debug.MSG_STATUS,
							"APIRequestPopulationNANCSupport Database Initalization done ");
		} catch (DatabaseException e) {
			Debug.log(Debug.MAPPING_ERROR, "Database initialization failure: "
					+ e.getMessage());
		}

		APIRequestPopulationNANCSupport reqObj = new APIRequestPopulationNANCSupport();

		try {

			MessageProcessorContext mpx = new MessageProcessorContext();
			mpx.set("SubRequest", "SvreleaseRequest"); //SubRequestType
			
			mpx.set("SPIDValue", "A111"); // SPIDValue
			mpx.set("LRNValue", ""); // LRNValue
			mpx.set("CustomerIdentifier", "PAT_TST");
			/*String InputMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAMessage>"+
			"<UpstreamToSOA><UpstreamToSOAHeader><InitSPID value=\"A111\"/><DateSent value=\"07-30-2009-065600PM\"/>"+
			"<Action value=\"submit\"/></UpstreamToSOAHeader><UpstreamToSOABody><SvCreateRequest><LnpType value=\"lspp\"/>"+
			"<Lrn value=\"3055919999\"/><SvType value=\"Wireline\"/><NewSPDueDate value=\"07-30-2009-070000PM\"/>"+
			"<PortToOriginal value=\"0\"/><AlternativeSPID value=\"1111\"/><OldSP value=\"1111\"/><NewSP value=\"A111\"/>"+
			"<NNSPSimplePortIndicator value=\"no\"/><Subscription><Tn value=\"3\"/></Subscription></SvCreateRequest></UpstreamToSOABody></UpstreamToSOA></SOAMessage> ";*/
			String InputMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAMessage><UpstreamToSOA><UpstreamToSOAHeader>" +
					"<InitSPID value=\"A111\"/><DateSent value=\"05-12-2010-1219PM\"/><Action value=\"submit\"/>" +
					"</UpstreamToSOAHeader><UpstreamToSOABody><SvCreateRequest><LnpType value=\"lspp\"/>" +
					"<Lata value=\"All\"/><NewSPDueDate value=\"05-13-2010-0800PM\"/>" +
					"<PortToOriginal value=\"1\"/><OldSP value=\"A111\"/><NewSP value=\"A111\"/><Subscription>" +
					"<Tn value=\"305-591-4010\"/></Subscription></SvCreateRequest></UpstreamToSOABody></UpstreamToSOA>" +
					"</SOAMessage>";
			
			MessageObject msob = new MessageObject(InputMessage);

			reqObj.initialize("SOA_VALIDATE_REQUEST",
					"APIRequestPopulationNANCSupport");
			reqObj.process(mpx, msob);

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

	}
}
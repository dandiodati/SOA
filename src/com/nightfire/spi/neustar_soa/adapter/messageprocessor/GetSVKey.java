/**
 * The purpose of this processor is to generate the XML output contains
 * Key values based on the TN or TN Range or SVID.
 *<root>	
 *   <keycontainer>
 *      <key value=""/>
 *   </keycontainer>	
 *   <keycontainer>
 *      <key value=""/> 
 *   </keycontainer>
 *</root>
 *
 * @author Ravi M Sharma
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see		com.nightfire.spi.common.driver.MessageProcessorBase;                  
 * @see		com.nightfire.common.ProcessingException;                              
 * @see		com.nightfire.framework.message.MessageException;                      
 * @see     com.nightfire.framework.util.Debug;                                    
 * @see		com.nightfire.framework.util.FrameworkException;                       
 * @see		com.nightfire.framework.util.NVPair;                                   
 * @see		com.nightfire.framework.util.StringUtils;                              
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants;                      
 * @see		com.nightfire.spi.common.driver.MessageObject;                         
 * @see		com.nightfire.spi.common.driver.MessageProcessorContext;                                                                                   
 * @see		com.nightfire.framework.resource.ResourceException;                    
 * @see		com.nightfire.framework.rules.Value;                                   
 * @see		com.nightfire.framework.db.DBConnectionPool;                           
 * @see		com.nightfire.framework.db.DatabaseException;                          
 * @see		com.nightfire.framework.db.DBInterface;                                
 * @see		com.nightfire.framework.message.generator.xml.XMLMessageGenerator;     

 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ravi.M			05/19/2004			Created
	2			Ravi.M			05/25/2004			Review Comments incorporated
	3			Ravi.M			07/29/2004			Formal review comments incorporated.
	4			Ashok			09/30/2004			RegionId added in DB Query ,If query's
													where clause contains ID.
	5			Ashok  			10/11/2004			Not required to check that StartTn should
													be less than or equal to end tn so related
													code removed

    6			Sunil  			25/01/2007			Modified the query in getTn method 
	7			Shanmugam		05/03/2007			Added Comments			
*/

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.Statement;

import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.rules.Value;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.monitor.ThreadMonitor;

public class GetSVKey extends MessageProcessorBase {	
	
	/**
	 * 	This variable used to get location of Region ID.
	 */
	private String regionIdLoc = null;
	
	/**
	 * 	This variable used to get value of Region ID.
	 */
	private String regionId = null;
		
	/**
	 * 	This variable used to get value for TN.
	 */
	private String inputLocTn = null;

	/**
	 * 	This variable used to get value for SVID.
	 */
	private String inputLocSvId = null;

	/**
	 * 	This variable used to get value for EndStation.
	 */
	private String inputLocEndStation = null;
	
	/**
	 * This variable used to get value for ReferneceKey.
	 */
	private String inputLocReferneceKey = null;
	/**
	 *This variable used to get value for starttn.
	 */
	private String startTn = null;

	/**
	 *This variable used to get value for starttn.
	 */
	private String endTn = null;

	/**
	 * This variable used to get value for Tn npa value.
	 */
	private String npaValue = null;

	/**
	 * This variable used to get value for Tn nxx value.
	 */
	private String nxxValue = null;

	/**
	 * The value of REQUEST_TYPE
	 */
	private String requestType = null;

	/**
	 * Name of the oracle table requested
	 */
	private String tableName = null;

	/**
	 * The value of SPID
	 */
	private String spidValue = null;

	/**
	 * Context location of SV key XML
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
	 * Constructor.
	 */
	public GetSVKey() {

		Debug.log(
			Debug.OBJECT_LIFECYCLE,
			"Creating GetSVKey message-processor.");

	}

	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param key Property-key to use for locating initialization properties.
	 * @param type Property-type to use for locating
	 * initialization properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type)
		throws ProcessingException {

		// Call base class method to load the properties.
		super.initialize(key, type);

		// Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		   Debug.log(Debug.SYSTEM_CONFIG, "GetSVKey: Initializing...");
		}

		StringBuffer errorBuffer = new StringBuffer();

		inputLocTn = getPropertyValue(SOAConstants.INPUT_LOC_TN_PROP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of TN is [" + inputLocTn + "].");
		}

		inputLocEndStation = getPropertyValue(SOAConstants.INPUT_LOC_END_STATION_PROP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of END_STATION is [" + inputLocEndStation + "].");
		}

		inputLocSvId = getPropertyValue(SOAConstants.INPUT_LOC_SVID_PROP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of SVID is [" + inputLocSvId + "].");
		}

		requestType = getPropertyValue(SOAConstants.REQUEST_TYPE_PROP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of requestType is [" + requestType + "].");
		}
		
		inputLocReferneceKey = getPropertyValue(SOAConstants.INPUT_LOC_REFERENCE_KEY_MESSAGE_PROP);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
					Debug.SYSTEM_CONFIG,
					"Location of ReferneceKey is [" + inputLocReferneceKey + "].");
		}

		tableName = getRequiredPropertyValue(
							SOAConstants.TABLE_NAME_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Value of Table_Name is [" + tableName + "].");
		}

		spidValue = getRequiredPropertyValue(SOAConstants.SPID_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of SPID is [" + spidValue + "].");
		}

		keyXMLLocation =
			getRequiredPropertyValue( 
							SOAConstants.REFERENCEKEY_OUT_LOC_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of KeyXMLLocation is [" + keyXMLLocation + "].");
		}
			
		regionIdLoc = getPropertyValue( SOAConstants.REGION_ID_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
						"Location of region ID value [" + regionIdLoc + "].");
		}

		// If any of required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "GetSVKey: Initialization done.");
		}
	} //end of initialize method

	/**
	 * This method queries the configured table to get the REFERENCEKEY values
	 * for the given TN or TN Range or SVID and generate XML output which contains Key
	 *
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(
		MessageProcessorContext mpContext,
		MessageObject inputObject)
		throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;

		if ( inputObject == null ) {
			
			return null;
		}
		
		this.mpContext = mpContext;
		
		this.inputObject = inputObject;
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
 		  Debug.log(Debug.MSG_STATUS, "GetSVKey: processing ... ");
		}

		// Get the value of the property from the context or inputObject.

		// Get the Input Location SvId value from context
		inputLocSvId =  getValue(inputLocSvId);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of Input Location SvId is ["
											+ inputLocSvId + "]." );
		}


		// Get the Input Location TN value from context
		inputLocTn =  getValue(inputLocTn);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of Input Location TN is ["
										+ inputLocTn + "]." );
		}

		// Get the Input Location End Station value from context
		inputLocEndStation = getValue(inputLocEndStation);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of Input Location End Station is ["
										+ inputLocEndStation + "]." );
		}

		// Get the SPID value from context
		spidValue =  getValue(spidValue);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is ["
										+ spidValue + "]." );
		}
		
		// Get the RegionId Location value from context
		regionId = getValue( regionIdLoc );
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of REGIONID is ["
										+ regionId + "]." );
		}
//		 Get the ReferenceKey value from context
		inputLocReferneceKey =  getValue(inputLocReferneceKey);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of ReferneceKey is ["
										+ inputLocReferneceKey + "]." );
		}
		

		// If the REQUEST_TYPE is configured
		if (requestType != null && exists(requestType, mpContext, inputObject))
		{
		
			// Get the Request type value from context
			requestType =
				(String) super.get(requestType, mpContext, inputObject);
		}
		// If REQUEST_TYPE is not configured
		else
		{
		
			requestType = null;
		}
		
		//generate a ReferenceKey Key XML
		XMLMessageGenerator refKeyXML =
			new XMLMessageGenerator(SOAConstants.KEY_ROOT);

		Connection dbConn = null;

		try {

			// Get DB connection from context
			dbConn = mpContext.getDBConnection( );
			
			// If DB Connection is null throw ProcessingException
			if(dbConn == null)
			{
				throw new ProcessingException("DB Connection not available");
			}

		} catch (Exception e) {

			String errMsg =
				"ERROR: GetSVKey: Attempt to get database "
					+ "connection failed with error: "
					+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// Re-throw the exception to the driver.
			throw new ProcessingException(errMsg);

		}

		try {
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] getting Subscription Version" );
			// If the Request/Response containes SVID element
			if (inputLocSvId != null) {
				
				// Get the ReferenceKey for the SVID
				getSv(dbConn, refKeyXML);			
				
			} 
			// If the Request/Response contains TN
			else if (inputLocTn != null) {

				boolean flag = true;

				//The component comes into picture due to Data Error, so the 
				// data error can be due to TN also, so we check for the format,
				// if Tn format is wrong we cannot get the Reference Key, so we
				// create a Empty XML. so we dont need to throw Message 
				// Exception in any case, instead always  create a an empty XML.
				if (!hasFormat("NNNN", inputLocEndStation)) {
					flag = false;
				}

				if (!hasFormat("NNN-NNN-NNNN", inputLocTn)) {
					flag = false;
				}

				// If the TN and EndStation format is correct, construct 
				// ParsedTN object
				if (flag) {

					ParsedTN pTN = makeTnRange(inputLocTn, inputLocEndStation);

					// Get the ReferenceKey for TN or TNRange
					getTn(dbConn, refKeyXML, pTN);

				} 
				// If TN or EndStation format is not coreect
				else {
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

					Debug.log(
						Debug.MSG_STATUS,
						"The format for Tn  ["
							+ inputLocTn
							+ "] or EndStation  ["
							+ inputLocEndStation
							+ "] is not valid");
					}

				}
			}
			
		}catch ( FrameworkException e ) {
			
			String errMsg = "ERROR: Unable to get Key and /or TN " + 
														e.getMessage();

			Debug.log( Debug.ALL_ERRORS, errMsg );

			if(e instanceof MessageException )
			{
	
				throw ( MessageException )e;
	
			}else
			{
	
				throw new ProcessingException( errMsg );
	
			}
			
			
		}
		finally
		{
			ThreadMonitor.stop( tmti );
		}

		// set the Referencekey XML in context
		super.set(
			keyXMLLocation,
			mpContext,
			inputObject,
			refKeyXML.getDocument());

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {

			Debug.log(
				Debug.MSG_STATUS,
				"GetSVKey: Generated ReferenceKey XML :\n"
					+ refKeyXML.generate());

		}

		return (formatNVPair(inputObject));

	}

	/**
	 * This method formats the TNVALUE,ENDSTATION and returns ParsedTN object
	 *
	 * @param  tnValue  as a string.
	 * @param endStationValue as a string.
	 *
	 * @return ParsedTN Object.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */

	private ParsedTN makeTnRange(String tnValue, String endStationValue)
		throws ProcessingException {

		Debug.log(Debug.UNIT_TEST, "GetSVKey: Entered the makeTnRange method.");

		try {

			StringTokenizer tokenizeRangeTn =
				new StringTokenizer(tnValue, SOAConstants.DEFAULT_DELIMITER);
			npaValue = tokenizeRangeTn.nextToken();
			nxxValue = tokenizeRangeTn.nextToken();
			startTn = tokenizeRangeTn.nextToken();

			// if it is a TN Range
			if (endStationValue != null && (!endStationValue.equals(""))) {
				// assign endStationValue to endTn
				endTn = endStationValue;

			} 
			// If it is a Single TN
			else {

				endTn = startTn;
			}

			if (Debug.isLevelEnabled(Debug.UNIT_TEST))
			{
			
				Debug.log(
					Debug.UNIT_TEST,
					"startTn-->" + startTn + "endTN-->" + endTn);
			}

			return new ParsedTN(npaValue, nxxValue, startTn, endTn);

		} catch (NullPointerException npex) {
			throw new ProcessingException("GetSVKey: Error: The given Tn was null.");
		} catch (NoSuchElementException nse) {
			//will never happen as this processor is after the rule processor.
			throw new ProcessingException("GetSVKey: Error: The Telephone Number is not correct ");
		}

	}

	/**
	 * This method queries SOA_SUBSCRIPTION_VERSION table to get the ReferenceKey values
	 * for the given SVID and generate XML output that contains Key.
	 *
	 * @return  The given input.
	 * @param dbConn
	 * @param SVKeyXML
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	private void getSv(Connection dbConn, XMLMessageGenerator refKeyXML)
		throws ProcessingException, MessageException {

		Debug.log(Debug.UNIT_TEST, "GetSVKey: Entered the getSv method.");

		// Construct query to get the ReferenceKey values for the given SVID
		StringBuffer query = new StringBuffer();

		query.append("SELECT /*+ index( ");
		
		query.append(tableName);
		
		query.append(" SOA_SV_INDEX_1) */ ");

		query.append(SOAConstants.REFERENCEKEY_COL);

		query.append(" FROM ");

		query.append(tableName);

		query.append(" WHERE SVID = ?");

		query.append(" AND SPID = ?");
		
		query.append(" AND ");
		
		query.append( SOAConstants.REGION_COL );
		
		query.append(" = ?");
        if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		  Debug.log(Debug.NORMAL_STATUS, "Value of query is [" + query + "].");
		}

		PreparedStatement tnStatement = null;
		
		ResultSet rs = null;

		try {

			tnStatement = dbConn.prepareStatement(query.toString());			

			tnStatement.setString(1, inputLocSvId );

			tnStatement.setString(2, spidValue);
			
			tnStatement.setString(3, regionId );

			rs = tnStatement.executeQuery();

			// If Record exists in DB
			if (rs.next()) {

				long refKey = rs.getLong(SOAConstants.REFERENCEKEY_COL);

				// Add key in ReferenceKey XML
				refKeyXML.setValue(
					SOAConstants.KEYCONTAINER_NODE
						+ "."
						+ SOAConstants.KEY_NODE,
					String.valueOf( refKey ));

			}
			

		} catch (SQLException e) {
			String errMsg =
				"ERROR: GetSVKey: Attempt to query to database "
					+ "failed with error: "
					+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);
			

		} finally {

			try {
				
				// if resultSet is not null
				if (rs != null) {

					// close the resultSet
					rs.close();
	
					rs = null;

				}
				
				// if statement is not null
				if (tnStatement != null) {

					// close the statement
					tnStatement.close();
					
					tnStatement = null;

				}
			} catch (SQLException sqle) {

				Debug.log(
					Debug.ALL_ERRORS,
					DBInterface.getSQLErrorMessage(sqle));

			}

		}

	}

	/**
	 * This method will get the referencekey from SOA_SUBSCRIPTION_VERSION table for 
	 * TN or TN Range and generate XML output that contains Key.
	 *
	 * @param dbConn Connection
	 * @param refKeyXML XMLMessageGenerator
	 * @param pTN ParsedTN
	 * @throws ProcessingException
	 * @throws MessageException
	 */
	private void getTn( Connection dbConn, XMLMessageGenerator refKeyXML, ParsedTN pTN) 	                                  
	                  throws ProcessingException, MessageException {

		int startTN = 0;
		int endTN = 0;

		Statement stmt = null;
		
		ResultSet rs = null;
		
		ArrayList tnList = new ArrayList();

		//startTn and endTn value is converted to Int.
		try {

			startTN = Integer.parseInt(pTN.startTn);
			endTN = Integer.parseInt(pTN.endTn);

		} catch (NumberFormatException nbrfex) {

		String error =
				"The start TN  ["
					+ pTN.startTn
					+ "] or end TN  ["
					+ pTN.endTn
					+ "] is not a valid integer: "
					+ nbrfex.getMessage();

			
			throw new MessageException(error);

		} 

		try{	
		
		 // this loop converts the range TN to individual single TN and add to the tnList
		 for (int i = startTN; i <= endTN; i++) {

		  String	tn =  pTN.npaValue
						+ SOAConstants.DEFAULT_DELIMITER
						+ pTN.nxxValue
						+ SOAConstants.DEFAULT_DELIMITER
						+ StringUtils.padNumber(
							i,
							SOAConstants.TN_LINE,
							true,
							'0');		
			
	     tnList.add(tn);

		}
	   
	   int tnCount =tnList.size();
       if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	     Debug.log(Debug.MSG_STATUS,"tnCount in GetSVKey: "+tnCount);
	   }

	   StringBuffer mainQuery = new StringBuffer();

	    int i = 0;

		// Construct the query that needs to execute to select the referenckey key. If the request/response contains
		// more than 1000 TNs, multiple query will be constructed for each 1000 TN and same will joined using UNION operator.
		// This has been done since ORACLE doesn't support IN operator more than 1000 items.
		while (i < tnCount)
		 {			
			int j = 1;

			StringBuffer  tnValue = new StringBuffer();

			// This loop can be used to append 1000 TN's to the stringbuffer
			while (j <= 1000 && i <= tnCount-1 )
			{
				 tnValue.append("'");
				 tnValue.append(tnList.get(i));
				
				// this condition used to add 1000 TN's to the list
				if ( j < 1000 && i != tnCount-1)				
					 tnValue.append("',");
				else
					 tnValue.append("'");

				i++;
				j++;
				
			}						
			
		 // this query is used to get 1000 TN values
		 StringBuffer tnQuery = new StringBuffer();
		 tnQuery.append("select /*+index( soa_subscription_version SOA_SV_INDEX_2 SOA_SV_INDEX_7) */ referencekey from soa_subscription_version where (portingtn, spid, createddate) in (select /*+index( soa_subscription_version SOA_SV_INDEX_2) */ portingtn, spid, max(createddate) from soa_subscription_version where portingtn in (" + tnValue + ") and spid = '" +spidValue+"'");
		 if(inputLocReferneceKey != null)
		 tnQuery.append(" and referencekey = '"+inputLocReferneceKey+"'");
		 tnQuery.append(" group by portingtn, spid)");

		 // append the tnQuery statement to the main query for batch processing
		 mainQuery.append(tnQuery);
         if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
	       Debug.log(Debug.NORMAL_STATUS,"Execute Tn Query: "+tnQuery);	
		 }

			// UNION is added to the mainQuery, since IN operator doesn't support more than 1000 TN
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}

		} //ends construction of query

		
			stmt = dbConn.createStatement();
		    if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
			   Debug.log(Debug.NORMAL_STATUS,"Executing GetSVKey query"+mainQuery.toString());
			}

			rs = stmt.executeQuery(mainQuery.toString());
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			 Debug.log(Debug.MSG_STATUS,"Executing GetSVKey after executeQuery result returned " + rs);
			}
     
            if( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
	    	Debug.log(
			  Debug.NORMAL_STATUS,
			     "Retrieving the ReferenceKeys using the following SQL: ["
				+ mainQuery
				+ "].");
			}
		

		long refKey = -1;
		

		try {		
			
			// For all the selected record
			while (rs.next()) {

					refKey = rs.getLong(SOAConstants.REFERENCEKEY_COL);

					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {

						Debug.log(
							Debug.MSG_STATUS,
							"The value of ReferenceKey: [" + refKey + "]");

					}
				
				//Add Key in ReferenceKey XML
				if (refKey != -1) {

					refKeyXML.setValue( SOAConstants.KEYCONTAINER_NODE + "."+ SOAConstants.KEY_NODE+ "("+ i + ")", String.valueOf( refKey ));

				}

				// nullifying the reference key value
				refKey = -1;	

			}				

						

		  }catch(Exception exception){

		Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }
    }catch(Exception exception){

		Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      } finally{
		  
		  // method to close resultSet and statement
		  close(rs, stmt);
	
      }     

   }

	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	protected String getValue( String locations)
 		throws MessageException, ProcessingException {
		StringTokenizer st = new StringTokenizer(locations, SEPARATOR);
		
		String tok = null;
		// While tokens are available ...
		while (st.hasMoreTokens()) {
			 tok = st.nextToken();
            if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
			Debug.log(
				Debug.MSG_STATUS,
				"Checking location [" + tok + "] for value...");
			}

			// if token exists in the context
			if (exists(tok, mpContext, inputObject))
			{
							// get the token value from context and return the value
							return (String)(get(tok, mpContext, inputObject));
			}
    	}

		return null;
	}

	/**
	 * Utility method for checking the format of a string.
	 *
	 * @param format String the string format.
	 * @param value String the string itself.
	 * @return boolean true if the value is null or has the given format,
	 *                      false otherwise.
	 */
	private static boolean hasFormat(String format, String value) {

		boolean result = true;

		if (value != null) {
			result = Value.hasFormat(format, value);
		}

		return result;

	}

	/**
	 * Class ParsedTN is used for capturing the values of
	 * npa,nxx,startTn and endTn.
	 *
	 */

	private static class ParsedTN {
		public final String npaValue;
		public final String nxxValue;
		public final String startTn;
		public final String endTn;

		public ParsedTN(
			String npaValue,
			String nxxValue,
			String startTn,
			String endTn) {
			this.npaValue = npaValue;
			this.nxxValue = nxxValue;
			this.startTn = startTn;
			this.endTn = endTn;
		}

	}

	 /**
    * Utility method for cleaning up a statement and returning a connection
    * to the pool. This method takes care of catching and logging any
    * exceptions.
    *
    * @param statement Statement this statement will be closed, if not
    *                                    null.
    * @param rs ResultSet this connection will be returned to the
    *                              connection pool, if not null.
    */
   protected static void close(ResultSet rs,
           					   Statement statement){
       
      // if resultSet is not closed
	  if (rs != null){
          
          try{
              
              // close the resultSet
			  rs.close();

		  }catch(SQLException sqlex){
              
              Debug.error("Could not close result set: "+sqlex);
          }
          
      }
      
      // if statement is not closed
	  if( statement != null ){

         try{
           
             // close the statement
			 statement.close();

		 }catch(SQLException sqlex){
            
             Debug.error("Could not close statement: "+sqlex);
         }

      }     

   }


	//	--------------------------For Testing---------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "d:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug.log(
				Debug.ALL_ERRORS,
				"GetSVKey: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.7:1521:cprod soa soa ");

			return;

		}
		try {

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {

			Debug.log(
				null,
				Debug.MAPPING_ERROR,
				": " + "Database initialization failure: " + e.getMessage());

		}

		GetSVKey getSvKey = new GetSVKey();

		try {
			getSvKey.initialize("NEUSTAR_SOA", "GetSVKey");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			//mob.set("TN", "381-201-2993");
			//mob.set("END_TN", "2995");
			mob.set("SPID", "1111");
			mob.set("SVID", "71994");
			mob.set("SvCreateRequest", "SvCreateRequest");
			
			getSvKey.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		}
	} //end of main method

	
}

/**
 * The purpose of this program is to get the SV Key (s) from the
 * input XML and update record(s) per reference  Key in configured table.
 *  *
 * @author Abhijit
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.adapter.messageprocessor.DatabaseLogger
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair
 */

/**
 * Revision History
 * ---------------------
 * Rev#		Modified By 	Date				Reason
 * -----       -----------     ----------			--------------------------
 * 1			Abhijit			07/31/2006			Created
 * 2			Abhijit			09/13/2006			Modified to add the
 * separator.
 * 3			Manoj k.		01/30/2007			Modified to batch update.
 * 4			Manoj K.		02/07/2007			Review Comments
 * incorporated
 * 5                      Rohit/Deepak            05/12/2007                      API & RangeID functionality for SV Update
 * 6                      Rohit                   07/16/2007                      Added SPID criterion for the RangeId Addition/Removal
 * 7                      Rohit                   08/31/2007                      SOA 4.0.4 functionality for sub-ranges addtion/removal
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;
import java.util.StringTokenizer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
import com.nightfire.framework.util.StringUtils;

public class SOAAccountSVUpdate extends DBMessageProcessorBase {
    
    /**
     *  Location of the reference key
     */
    private String referenceKeyProp = null;
    
    /**
     * This variable used to get value for reference key
     */
    private String referenceKey = null;
    
    /**
     *  Location of the rangeId
     */
    private String rangeIdProp = null;
    
    /**
     *  Location of the requestType key
     */
    //declaring the variable for holding the location of request type
    private String requestTypeKeyProp = null;
    
    /**
     * This variable used to get value for request type key
     */
    //declaring the variable to get the value of request type from the xml.
    private String requestTypeKey = null;
    
    /**
     * This variable is used to hold the accountId
     */
    private String accountIdLoc = null;
    
    /**
     * This variable is used to hold the accountName
     */
    private String accountNameLoc = null;
    
    /**
     * This variable contains  MessageProcessorContext object
     */
    private MessageProcessorContext mpContext = null;
    
    /**
     * This variable contains  MessageObject object
     */
    private MessageObject inputObject = null;
    
    /**
     * This variable contains  Secondary Service Provider
     */
    private String spidProp = null;
    
    /**
     * This variable contains  Telephone Number
     */
    private String telephoneNumberListProp = null;
    
    /**
     * This variable contains  Subscription Version ID
     */
    private String svidListProp = null;
    
    /**
     * This variable contains  Region ID
     */
    private String regionIdProp = null;
    
    /**
     * This variable contains  New Service Provider (NNSP)
     */
    private String nnspProp = null;
    
    /**
     * This variable contains  Old Service Provider (ONSP)
     */
    private String onspProp = null;
    
    /**
     * This variable contains  Old Service Provider (ONSP)
     */
    private String inputSourceProp = null;
    
    /**
     * This variable contains error msg for the GUI required fields if not present
     */
    private StringBuffer errorBuffer = null;
    
    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties
     * @param  type  Property-type to use for
     * locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException{
        
        // Call base class method to load the properties.
        super.initialize(key, type);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(Debug.SYSTEM_CONFIG, "SOAAccountSVUpdate: Initialization " +
					"Started.");
		}
        
        errorBuffer = new StringBuffer();
        
        referenceKeyProp = getRequiredPropertyValue(
                SOAConstants.REFERENCE_KEY_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of REFERENCE_KEY is ["
					+ referenceKeyProp + "]." );
		}
        
        rangeIdProp = getRequiredPropertyValue(
                SOAConstants.RANGE_ID_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of RANGE_ID is ["
					+ rangeIdProp + "]." );
		}
        
        //getting the location of request type
        requestTypeKeyProp = getRequiredPropertyValue(
                SOAConstants.REQUEST_KEY_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of REQUEST_TYPE is ["
					+ requestTypeKeyProp + "]." );
		}
        
        //getting the location of accountId
        accountIdLoc = getPropertyValue(SOAConstants.ACCOUNT_ID);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of ACCOUNT_ID is ["
					+ accountIdLoc + "]." );
		}
        
        accountNameLoc = getPropertyValue(SOAConstants.ACCOUNT_NAME);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of ACCOUNT_NAME is ["
					+ accountNameLoc + "]." );
		}
        
        //getting the location of SPID property
        spidProp = getPropertyValue(SOAConstants.SPID);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is [" + spidProp + "]." );
		}
        
        //getting the location of Tn List property
        telephoneNumberListProp = getPropertyValue(SOAConstants.TN_LIST);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "Value of TN_LIST is [" + telephoneNumberListProp + "]." );
		}
        
        //getting the location of SVID List property
        svidListProp = getPropertyValue(SOAConstants.SVID_LIST);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "Value of SVID_LIST is [" + svidListProp + "]." );
		}
        
        //getting the location of Region Id property
        regionIdProp = getPropertyValue(SOAConstants.REGION_ID);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "Value of REGION_ID is [" + regionIdProp + "]." );
		}
        
        //getting the location of NNSP property
        nnspProp = getPropertyValue(SOAConstants.NNSP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "Value of NNSP is [" + nnspProp + "]." );
		}
        
        //getting the location of ONSP property
        onspProp = getPropertyValue(SOAConstants.ONSP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "Value of ONSP is [" + onspProp + "]." );
		}
        
        //getting the location of inputSource property
        inputSourceProp = getPropertyValue(SOAConstants.INPUT_SOURCE);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "Value of INPUT_SOURCE is [" + inputSourceProp + "]." );
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(Debug.SYSTEM_CONFIG, "SOAAccountSVUpdate: " +
					"Initialization done.");
		}
        
    }
    
    /**
     * This method will parse the Input XML to extract ReferenceKey values
     * and update record(s) in the specified table.
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
            MessageObject message) throws MessageException, ProcessingException {
    	
    	ThreadMonitor.ThreadInfo tmti = null;
        String accountId = null;        //Account ID
        String accountName = null;      //Account Name
        String spid = null;             //SPID
        String tnList = null;           //List of Tn separated by semicolon
        String svidList = null;         //List of SVID separated by semicolon
        String regionId = null;         //Region Id
        String nnsp = null;             //New SP
        String onsp = null;             //Old SP
        String inputSource = null;      //To identify API or GUI request, G for GUI and A for API
        String rangeIdList = null;      //Range Id List separated by semicolon
        Connection dbConn = null;
        
        if (message == null) {
            return null;
        }
        
        this.mpContext = context;
        
        this.inputObject = message;
        
        try{
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        
	        //throw exception when input source property is null
	        if(!StringUtils.hasValue(inputSourceProp)) {
	            throw new ProcessingException("InputSource Property cannot be null. It should be either" +
	                    "A for API and G for GUI"  );
	        }
	        
	        if (rangeIdProp != null) {
	            rangeIdList = getValue(rangeIdProp);
	        }
	        
	        if (referenceKeyProp != null) {
	            referenceKey = getValue(referenceKeyProp);
	        }
	        
	        //get the input source value
	        inputSource = getValue(inputSourceProp);
	
	        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	          Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate: Request is coming thru " + inputSource);
			}
	        
	        //Throw exception when GUI and API specific required properties are missing
	        if (inputSource.equals("G")) {
	            if(errorBuffer.length() > 0) {
	                String errMsg = errorBuffer.toString();
	                
	                Debug.log(Debug.ALL_ERRORS, errMsg);
	                
	                throw new ProcessingException(errMsg);
	            }
	        } else if(inputSource.equals("A")) {
	            //Possible Combintaions
	            //AccountId, SPID, SvId, RegionId
	            //AccountId, SPID, NewSP, OldSP, Tn
	            //AccountId, SPID, RangeId
	            if(StringUtils.hasValue(accountIdLoc) && StringUtils.hasValue(spidProp)) {
	                if(!StringUtils.hasValue(rangeIdProp))
	                    if(!(StringUtils.hasValue(svidListProp) && StringUtils.hasValue(regionIdProp)))
	                        if(!(StringUtils.hasValue(nnspProp) && StringUtils.hasValue(onspProp) && StringUtils.hasValue(telephoneNumberListProp)))
	                            throw new ProcessingException("Properties Combination of anyone of the follwowing:\n " +
	                                    "[AccountId, SPID, and SvId, RegionId] or [AccountId, SPID, NewSP, OldSP, and Tn] or [AccountId, SPID, RangeId] " +
	                                    "must be present for API request");
	            } else {
	                throw new ProcessingException("[AccountId and SPID]" +
	                        "must be present for API request");
	            }
	        } else {
	            throw new ProcessingException("Request is coming neither thru GUI nor API");
	        }
	        
	        //getting the request type value from the xml
	        requestTypeKey = (String) super.get(requestTypeKeyProp, context, message);
	        
	        //Account Id and Account Name common to be both GUI and API
	        if (accountIdLoc != null) {
	            accountId = getValue(accountIdLoc);
	        }
	        
	        if (accountNameLoc != null && inputSource.equals("G")) {
	            accountName = getValue(accountNameLoc);
	        }
	        
	        try {
	            // Get a database connection from the appropriate
	            //location - based
	            // on transaction characteristics.
	            
	            dbConn = context.getDBConnection();
	            
	            if (dbConn == null) {
	                // Throw the exception to the driver.
	                throw new ProcessingException("DB "
	                        + "connection is not available");
	            }
	        } catch (FrameworkException e) {
	            String errMsg = "ERROR: SOAAccountSVUpdate:"
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
	        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	         Debug.log(Debug.MSG_STATUS, "RequestType is [" + requestTypeKey + "]");
			}
	        if (referenceKeyProp != null) {
	            referenceKey = getValue(referenceKeyProp);
	        }
	        
	        //get Account Name for API request otherwise throw exception if not able to fetch
	        if(inputSource.equals("A")) {
	            spid = spidProp != null ? getValue(spidProp):null;                                  //get the value of SPID
	            tnList = telephoneNumberListProp != null ? getValue(telephoneNumberListProp):null;  //get the value of TnList
	            svidList = svidListProp != null ? getValue(svidListProp):null;                      //get the value of SvId
	            regionId = regionIdProp != null ? getValue(regionIdProp):null;                      //get the value of RegionId
	            nnsp = nnspProp != null ? getValue(nnspProp):null;                                  //get the value of NewSP
	            onsp = onspProp != null ? getValue(onspProp):null;                                  //get the value of OldSP
	            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, "SPID is [" + spid + "]");
					Debug.log(Debug.MSG_STATUS, "Tn is [" + tnList + "]");
					Debug.log(Debug.MSG_STATUS, "SvId is [" + svidList + "]");
					Debug.log(Debug.MSG_STATUS, "RegionId is [" + regionId + "]");
					Debug.log(Debug.MSG_STATUS, "NewSP is [" + nnsp + "]");
					Debug.log(Debug.MSG_STATUS, "OldSP is [" + onsp + "]");
				}
	            
	            //throw exception when required value are not present
	            if(!StringUtils.hasValue(rangeIdList))
	                if(!(StringUtils.hasValue(svidList) && StringUtils.hasValue(regionId)))
	                    if(!(StringUtils.hasValue(nnsp) && StringUtils.hasValue(onsp) && StringUtils.hasValue(tnList)))
	                        throw new ProcessingException("Value Combination of anyone of the follwowing:\n " +
	                                "[AccountId, SPID, and SvId, RegionId] or [AccountId, SPID, NewSP, OldSP, and Tn] or [AccountId, SPID, RangeId] " +
	                                "must be present for API request");
	            
	            //getting API properties values
	            accountName = getAccountName(dbConn, accountId, spid);
	            if(accountName == null) {
	                throw new ProcessingException("Does not find an Account Name for the given AccountId [" +
	                        accountId + "] and SPID [" + spid + "]" );
	            }
	            
	            //setting null for AccountId and AccountName for ACCOUNT_REMOVE_SV_REQUEST
	            if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)) {
	                accountId = null;
	                accountName = null;
	            }
	        }
	        
	        if(inputSource.equals("G")) {
	            tnList = telephoneNumberListProp != null ? getValue(telephoneNumberListProp):null;  //get the value of TnList
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	             Debug.log(Debug.MSG_STATUS, "Tn is [" + tnList + "]");
				}
	            
	            //get the value of the GUI specific properties
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	              Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Updating SV for the request coming thru GUI");
				}
	            accountSVUpdateForGUI(dbConn,referenceKey, rangeIdList, tnList, accountId, accountName, inputSource);
	            
	        } else if(inputSource.equals("A")) {
	            if(rangeIdList != null) {
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Updating SV on the basis of " +
								"[AccountId, SPID, and RangeId]");
					}
	                accountSVRangeUpdateForAPI(dbConn, spid, rangeIdList, accountId, accountName);
	            } else {
	                if(StringUtils.hasValue(svidList) && StringUtils.hasValue(regionId)) {
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Updating SV on the basis of "
									+ " [AccountId, SPID, and SvId, RegionId]");
						}
	                    accountSVUpdateForAPI(dbConn, spid, svidList, regionId, accountId, accountName);
	                } else if(StringUtils.hasValue(nnsp) && StringUtils.hasValue(onsp) && StringUtils.hasValue(tnList)){
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Updating SV on the basis of "
									+ " [SPID, NewSP, OldSP, and Tn]");
						}
	                    accountSVUpdateForAPI(dbConn, spid, onsp, nnsp, tnList, accountId, accountName);
	                }
	            }
	        }
        }finally{
        	ThreadMonitor.stop(tmti);	
        }
        return (formatNVPair(message));
    }
    
    
    /**
     * This method update SOA_SUBSCRIPTION_VERSION table for ACCOUNTID and ACCOUNTNAME
     * for the request that is coming thru an GUI. It uses ReferenceKey
     * criteria for Adding Single TN List or Range List
     *
     * @param  dbConn as Connection
     * @param  referenceKey as String
     * @param  rangeId as String
     * @param  accountId as String
     * @param  accountName as String
     *
     * @return  void
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void accountSVUpdateForGUI(Connection dbConn, String referenceKey, String rangeId, String tnList, String accountId, String accountName, String inputSource) {
        PreparedStatement updateStmt = null;
        PreparedStatement updateRangeStmt = null;   //This is specfically used for only AddRemoveSVUpdate for Range
        boolean addRangeTnList = false;
        StringTokenizer st = null;
        
        try{
            //getting the preparedStatement
            if(requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) {
                //Check whether Range Addtion or Tn List Addition
                if(rangeId==null || rangeId.equals("")){
                    //Tn List updation query
                    updateStmt = dbConn.prepareStatement(
                            SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE);
                    addRangeTnList = false;       //true for Single Tn  List Addition
					if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
						Debug.log( Debug.NORMAL_STATUS, "Executing Query for TN List [" +
								SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE + "]");
					}
                } else {
                    //Range List updation query
                    updateStmt = dbConn.prepareStatement(SOAQueryConstants.SOA_ACCOUNT_RANGE_SV_UPDATE);
                    addRangeTnList = true;      //false for Range List Addition
					if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
						Debug.log( Debug.NORMAL_STATUS, "Executing Query for Range List [" +
								SOAQueryConstants.SOA_ACCOUNT_RANGE_SV_UPDATE + "]");
					}
                }
            } else if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)){
                updateStmt = dbConn.prepareStatement(
                        SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE);
            }
            
            if((rangeId != null &&  requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) || referenceKey != null ) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                  Debug.log( Debug.MSG_STATUS, "addRangeTnList = " + addRangeTnList);
				}
                st = new StringTokenizer((addRangeTnList==true?rangeId:referenceKey), SOAConstants.REFKEY_SEPARATOR);
            }
            
            while (st != null && st.hasMoreTokens()) {
                
                String refKey = null;
                
                refKey = st.nextToken();
                
                //if request type is AccountAddSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) {
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [1] is [" + accountId + "]");
					}
                    updateStmt.setString(1,accountId);  //accountId
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [2] is [" + accountName + "]");
					}
                    updateStmt.setString(2,accountName);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [3] is [" + refKey + "]");
					}
                    updateStmt.setLong(3,Long.parseLong(refKey));
                    
                    //add it to the batch
                    updateStmt.addBatch();
                    
                }
                
                //if request type is AccountRemoveSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)){
                    
                    
                    updateStmt.setNull(1,java.sql.Types.VARCHAR);
                    
                    
                    updateStmt.setNull(2,java.sql.Types.VARCHAR);
                    
                    
                    updateStmt.setLong(3,Long.parseLong(refKey));
                    
                    updateStmt.addBatch();
                    
                }
                
            }
            
            //executing the batch statement
            if(updateStmt != null){
            	int count[] = updateStmt.executeBatch();
            	if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            		Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Number of batch request submitted "
            				+ count.length);
            	}
            }
            
            //Updating only AddRemoveSVUpdate for Range
            if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST) && rangeId != null && !rangeId.equals("")) {
                String stRangeId[] = rangeId.split(SOAConstants.REFKEY_SEPARATOR);
                String stTn[] = tnList.split(SOAConstants.REFKEY_SEPARATOR);
                updateRangeStmt = dbConn.prepareStatement(SOAQueryConstants.SOA_ACCOUNT_RANGE_SV_REMOVE);
				if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
					Debug.log( Debug.NORMAL_STATUS, "Executing Query for sub-ranges removal [" +
								SOAQueryConstants.SOA_ACCOUNT_RANGE_SV_REMOVE + "]");
				}
                
                for (int rangeIndex = 0; rangeIndex < stRangeId.length; rangeIndex++) {
                    String tnRange = stTn[rangeIndex];
                    
                    if(!stRangeId[rangeIndex].trim().equals("")) {
                        
                        String startTn = getStartTn(tnRange);
                        String endTn = getEndTn(tnRange);
                        
                        
                        updateRangeStmt.setNull(1,java.sql.Types.VARCHAR);
                        
                        
                        updateRangeStmt.setNull(2,java.sql.Types.VARCHAR);
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                          Debug.log(Debug.MSG_STATUS, "Value for slot [3] is [" + stRangeId[rangeIndex] + "]");
						}
                        updateRangeStmt.setLong(3,Long.parseLong(stRangeId[rangeIndex]));
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                          Debug.log(Debug.MSG_STATUS, "Value for slot [4] is [" + startTn + "]");
						}
                        updateRangeStmt.setString(4, startTn);
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                          Debug.log(Debug.MSG_STATUS, "Value for slot [5] is [" + endTn + "]");
						}
                        updateRangeStmt.setString(5, endTn);
                        
                        updateRangeStmt.addBatch();
                    }
                }
                
                //executing the batch statement
                int rangeCount[] = updateRangeStmt.executeBatch();
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Number of batch request submitted "
							+ rangeCount.length);
				}
            }
            
        } catch(SQLException ex){
            String errMsg = "ERROR: SOAAccountSVUpdate: Attempt to " +
                    "update the data in database "
                    + "failed with error: " + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
        }finally{
            try {
                if (updateStmt != null) {
                    updateStmt.close();
                    
                    updateStmt = null;
                }
                if (updateRangeStmt != null) {
                    updateRangeStmt.close();
                    
                    updateRangeStmt = null;
                }
            } catch (SQLException sqle) {
                
                Debug.log(Debug.ALL_ERRORS, DBInterface
                        .getSQLErrorMessage(sqle));
                
            }
            
        }
    }
    
    /**
     * This method update SOA_SUBSCRIPTION_VERSION table for ACCOUNTID and ACCOUNTNAME
     * for the request that is coming thru an API. It uses SPID and RangeId criteria
     *
     * @param  dbConn as Connection
     * @param  spid as String
     * @param  rangeIdList as String
     * @param  accountId as String
     * @param  accountName as String
     * @return  void
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void accountSVRangeUpdateForAPI(Connection dbConn, String spid, String rangeIdList, String accountId, String accountName) throws ProcessingException {
        
        PreparedStatement updateStmt = null;
        
        try{
            //getting the preparedStatement
            updateStmt = dbConn.prepareStatement(SOAQueryConstants.SOA_ACCOUNT_RANGE_SV_UPDATE_FOR_API);
            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
              Debug.log(Debug.NORMAL_STATUS, "Executing Update Query [" + SOAQueryConstants.SOA_ACCOUNT_RANGE_SV_UPDATE_FOR_API + "]");
			}
            
            //Tokenize the RangeId
            StringTokenizer stRangeId = new StringTokenizer(rangeIdList, SOAConstants.REFKEY_SEPARATOR);
            
            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                Debug.log(Debug.MSG_STATUS, "Number of RangeId [" + stRangeId.countTokens() + "]");
            }
            
            while (stRangeId.hasMoreTokens()) {
                //set the rangeId
                String rangeId = stRangeId.nextToken();
                
                if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                    Debug.log(Debug.MSG_STATUS, "RangeId [" + rangeId + "]");
                }
                
                //if request type is AccountAddSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) {
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [1] is [" + accountId + "]");
					}
                    updateStmt.setString(1,accountId );    //Account Id
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [2] is [" + accountName + "]");
					}
                    updateStmt.setString(2,accountName);                //Account Name
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [3] is [" + spid + "]");
					}
                    updateStmt.setString(3, spid);                      //SPID
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [4] is [" + rangeId + "]");
					}
                    updateStmt.setLong(4, Long.parseLong(rangeId));     //rangeId
                    
                    //add it to the batch
                    updateStmt.addBatch();
                }
                
                //if request type is AccountRemoveSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)){
                    
                    
                    updateStmt.setNull(1,java.sql.Types.VARCHAR);       //Account Id
                    
                    
                    updateStmt.setNull(2,java.sql.Types.VARCHAR);       //Account Name
                    
                    
                    updateStmt.setString(3, spid);                      //SPID
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                      Debug.log(Debug.MSG_STATUS, "Value for slot [4] is [" + rangeId + "]");
					}
                    updateStmt.setLong(4, Long.parseLong(rangeId));     //rangeId
                    
                    //add it to the batch
                    updateStmt.addBatch();
                }
            }
            
            //executing the batch statement
            int count[] = updateStmt.executeBatch();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS, "SOAAccountSVUpdate :Number of batch request submitted "
						+ count.length);
			}
            
        }catch(SQLException ex){
            String errMsg = "ERROR: SOAAccountSVUpdate: Attempt to " +
                    "update the data in database "
                    + "failed with error: " + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
        }finally{
            try {
                if (updateStmt != null) {
                    updateStmt.close();
                    
                    updateStmt = null;
                }
            } catch (SQLException sqle) {
                
                Debug.log(Debug.ALL_ERRORS, DBInterface
                        .getSQLErrorMessage(sqle));
                
            }
            
        }
    }
    
    /**
     * This method update SOA_SUBSCRIPTION_VERSION table for ACCOUNTID and ACCOUNTNAME
     * for the request that is coming thru an API. It uses SPID, SVID, and REGIONID criteria
     *
     * @param  dbConn as Connection
     * @param  spid as String
     * @param  svidList as String
     * @param  regionId as String
     * @param  accountId as String
     * @param  accountName as String
     *
     * @return  void
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void accountSVUpdateForAPI(Connection dbConn, String spid, String svidList, String regionId, String accountId, String accountName) throws ProcessingException {
        
        PreparedStatement updateStmt = null;
        
        try{
            //getting the preparedStatement
            updateStmt = dbConn.prepareStatement(SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE_FOR_API_1);
            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
              Debug.log(Debug.NORMAL_STATUS, "Executing Update Query [" + SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE_FOR_API_1 + "]");
			}
            
            //Tokenize the SVID
            StringTokenizer stSVID = new StringTokenizer(svidList, SOAConstants.SVID_SEPARATOR);
            
            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                Debug.log(Debug.MSG_STATUS, "Number of SVID [" + stSVID.countTokens() + "]");
            }
            
            while (stSVID.hasMoreTokens()) {
                //set the SVID
                String svid = stSVID.nextToken();
                
                if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                    Debug.log(Debug.MSG_STATUS, "SVID [" + svid + "]");
                }
                
                //if request type is AccountAddSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) {
                    updateStmt.setString(1, accountId );    //Account Id
                    
                   
                    updateStmt.setString(2,accountName);                //Account Name
                    
                    
                    updateStmt.setString(3, spid);                      //SPID
                    
                    
                    updateStmt.setString(4, svid);                      //SvId
                    
                    
                    updateStmt.setString(5, regionId);                      //RegionId
                    
                    //add it to the batch
                    updateStmt.addBatch();
                }
                
                //if request type is AccountRemoveSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)){
                    
                    updateStmt.setNull(1,java.sql.Types.VARCHAR);       //Account Id
                    
                    updateStmt.setNull(2,java.sql.Types.VARCHAR);       //Account Name
                    
                    updateStmt.setString(3, spid);                      //SPID
                    
                    updateStmt.setString(4, svid);                      //SvId
                    
                    updateStmt.setString(5, regionId);                      //RegionId
                    
                    //add it to the batch
                    updateStmt.addBatch();
                }
            }
            
            //executing the batch statement
            updateStmt.executeBatch();
            
            
        }catch(SQLException ex){
            String errMsg = "ERROR: SOAAccountSVUpdate: Attempt to " +
                    "update the data in database "
                    + "failed with error: " + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
        }finally{
            try {
                if (updateStmt != null) {
                    updateStmt.close();
                    
                    updateStmt = null;
                }
            } catch (SQLException sqle) {
                
                Debug.log(Debug.ALL_ERRORS, DBInterface
                        .getSQLErrorMessage(sqle));
                
            }
            
        }
    }
    
    /**
     * This method update SOA_SUBSCRIPTION_VERSION table for ACCOUNTID and ACCOUNTNAME
     * for the request that is coming thru an API. It uses SPID, ONSP, NNSP, and TN criteria
     *
     * @param  dbConn as Connection
     * @param  spid as String
     * @param  onsp as String
     * @param  nnsp as String
     * @param  tnList as String
     * @param  accountId as String
     * @param  accountName as String
     *
     * @return  void
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void accountSVUpdateForAPI(Connection dbConn, String spid, String onsp, String nnsp, String tnList, String accountId, String accountName) throws ProcessingException {
        
        PreparedStatement updateStmt = null;
        
        try{
            //getting the preparedStatement
            updateStmt = dbConn.prepareStatement(SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE_FOR_API_2);

            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
              Debug.log(Debug.NORMAL_STATUS, "Executing Update Query [" + SOAQueryConstants.SOA_ACCOUNT_SV_UPDATE_FOR_API_2 + "]");
			}
            
            //Tokenize the PortingTn
            StringTokenizer stPortingTn = new StringTokenizer(tnList, SOAConstants.PORTINGTN_SEPARATOR);
            
            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                Debug.log(Debug.MSG_STATUS, "Number of Tn [" + stPortingTn.countTokens() + "]");
            }
            
            while (stPortingTn.hasMoreTokens()) {
                
                //set the Porting Tn
                String portingTn = stPortingTn.nextToken();
                
                if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                    Debug.log(Debug.MSG_STATUS, "Tn [" + portingTn + "]");
                }
                
                if(portingTn != null && portingTn.length()>12)
                {

    				int startTnVal = Integer.parseInt(portingTn.substring(8, 12));
    				int endTnVal = Integer.parseInt(portingTn.substring(13, 17));
    				   				    				
    				for (int p = startTnVal; p <= endTnVal; p++) {
    					StringBuffer tnBuff = new StringBuffer(portingTn.substring(0,8));
    					tnBuff.append(StringUtils.padNumber(p,SOAConstants.TN_LINE, true, '0'));

    					  //if request type is AccountAddSvRequest
    	                   if(requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) {
    	                    
    	                    
    	                    updateStmt.setString(1,accountId);          //Account Id
    	                    
    	                    
    	                    updateStmt.setString(2,accountName);                //Account Name
    	                    
    	                    
    	                    updateStmt.setString(3, spid);                      //SPID
    	                    
    	                    
    	                    updateStmt.setString(4, onsp);                      //ONSP
    	                    
    	                    
    	                    updateStmt.setString(5, nnsp);                      //NNSP
    	                    
    	                    
    	                    updateStmt.setString(6, tnBuff.toString());                 //Porting Tn
    	                    
    	                    //add it to the batch
    	                    updateStmt.addBatch();
    	                  }
    	                
    	                   //if request type is AccountRemoveSvRequest
    	                   if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)){
    	                    
    	                    
    	                    updateStmt.setNull(1,java.sql.Types.VARCHAR);       //Account Id
    	                    
    	                    
    	                    updateStmt.setNull(2,java.sql.Types.VARCHAR);       //Account Name
    	                    
    	                    
    	                    updateStmt.setString(3, spid);                      //SPID
    	                    
    	                    
    	                    updateStmt.setString(4, onsp);                      //ONSP
    	                    
    	                    
    	                    updateStmt.setString(5, nnsp);                      //NNSP
    	                    
    	                    
    	                    updateStmt.setString(6, tnBuff.toString());                 //Porting Tn
    	                    
    	                    //add it to the batch
    	                    updateStmt.addBatch();
    	                  }
    				   }
    		    }else{
                //if request type is AccountAddSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_ADD_SV_REQUEST)) {
                    
                    
                    updateStmt.setString(1,accountId);    //Account Id
                    
                    
                    updateStmt.setString(2,accountName);                //Account Name
                    
                    
                    updateStmt.setString(3, spid);                      //SPID
                    
                    
                    updateStmt.setString(4, onsp);                      //ONSP
                    
                    
                    updateStmt.setString(5, nnsp);                      //NNSP
                    
                    
                    updateStmt.setString(6, portingTn);                 //Porting Tn
                    
                    //add it to the batch
                    updateStmt.addBatch();
                }
                
                //if request type is AccountRemoveSvRequest
                if(requestTypeKey.equals(SOAConstants.ACCOUNT_REMOVE_SV_REQUEST)){
                    
                    
                    updateStmt.setNull(1,java.sql.Types.VARCHAR);       //Account Id
                    
                    
                    updateStmt.setNull(2,java.sql.Types.VARCHAR);       //Account Name
                    
                    
                    updateStmt.setString(3, spid);                      //SPID
                    
                    
                    updateStmt.setString(4, onsp);                      //ONSP
                    
                    
                    updateStmt.setString(5, nnsp);                      //NNSP
                    
                    
                    updateStmt.setString(6, portingTn);                 //Porting Tn
                    
                    //add it to the batch
                    updateStmt.addBatch();
                }
            }
        }
            //executing the batch statement
            updateStmt.executeBatch();
            
            
            
        }catch(SQLException ex){
            String errMsg = "ERROR: SOAAccountSVUpdate: Attempt to " +
                    "update the data in database "
                    + "failed with error: " + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
        }finally{
            try {
                if (updateStmt != null) {
                    updateStmt.close();
                    
                    updateStmt = null;
                }
            } catch (SQLException sqle) {
                
                Debug.log(Debug.ALL_ERRORS, DBInterface
                        .getSQLErrorMessage(sqle));
                
            }
            
        }
    }
    
    /**
     * This method is used to fetch Account Name
     * @param dbConn Connection
     * @param accountId String
     * @param spid String
     * @return AccountName as String if found otherwise null.
     */
    public String getAccountName(Connection dbConn, String accountId, String spid){
        
        PreparedStatement pstmt = null;
        
        ResultSet results = null;
        
        try{
			if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
              Debug.log(Debug.NORMAL_STATUS, "Executing Query to fetch Account Name [" + SOAQueryConstants.GET_ACCOUNT_NAME + "]");
			}
            
            pstmt = dbConn.prepareStatement(SOAQueryConstants.GET_ACCOUNT_NAME);
            
            
            pstmt.setString( 1, spid );
            
            
            pstmt.setString( 2, accountId );
            
            
            pstmt.setString( 3, spid );
            
            results = pstmt.executeQuery();
            
            if( results.next() ){
                return results.getString(1);   //return Account Name
            }
            
        }catch(Exception exception){
            Debug.log(Debug.ALL_ERRORS, SOAQueryConstants.GET_ACCOUNT_NAME + "\n" + exception);
        }finally{
            try {
                if( results != null )
                    results.close();
                results = null;
                
                if (pstmt != null)
                    pstmt.close();
                pstmt = null;
            } catch (SQLException sqle) {
                Debug.log(Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle));
                
            }
        }
        
        return null;     //return Account Name as null
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
     *
     */
    protected String getValue(String locations) throws MessageException,
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
     * This method returns the Starting Tn of the format NNN-NNN-NNNN
     *
     * @param  tnRange as String of 17 character     
     * @return Start Tn of 12 charcters     
     */
    public String getStartTn(String tnRange) {
        //when Tn is 12 cahracters
        if(tnRange.length() == 12)
            return tnRange;
        
        //Assuming Tn to be of 17 characters
        return tnRange.substring(0, 12);
    }
    
    /**
     * This method returns the Ending Tn of the format NNN-NNN-NNNN. It extract NPA-NXX and the concat enstation
     *
     * @param  tnRange as String of 17 character     
     * @return End Tn of 12 charcters     
     */
    public String getEndTn(String tnRange) {
        //when Tn is 12 cahracters
        if(tnRange.length() == 12)
            return tnRange;
        
        //Assuming Tn to be of 17 characters
        return tnRange.substring(0, 8) + tnRange.substring(13);
    }
    
    //--------------------------For Testing---------------------------------//
    
    public static void main(String[] args) {
        
        Properties props = new Properties();
        
        props.put("DEBUG_LOG_LEVELS", "all");
        
        props.put("LOG_FILE", "e:\\SOAlog.txt");
        
        Debug.showLevels();
        
        Debug.configureFromProperties(props);
        
        if (args.length != 3) {
            Debug.log(Debug.ALL_ERRORS, "SOAAccountSVUpdate: USAGE:  "
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
        
        SOAAccountSVUpdate svUpdate = new SOAAccountSVUpdate();
        
        try {
            
            svUpdate.initialize("FULL_NEUSTAR_SOA", "SOAAccountSVUpdate");
            
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
            
            svUpdate.process(mpx, mob);
            
            Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());
            
        } catch (ProcessingException pex) {
            System.out.println(pex.getMessage());
        } catch (MessageException mex) {
            System.out.println(mex.getMessage());
        }
        
    } //end of main method
    
}
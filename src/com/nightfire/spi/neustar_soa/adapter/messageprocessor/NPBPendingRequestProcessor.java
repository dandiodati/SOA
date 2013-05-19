/*
 * NPBPendingRequestProcessor.java
 *
 * Created on July 24, 2007, 5:09 PM
 *
 * The main objective of this Component is to Update the SOA_NBRPOOL_BLOCK
 * table based on the Messagekey received from NPAC as a successful responce.
 *
 * SOA receives successful Synchronous Response from NPAC for NumberPoolBlockModifyRequest
 * SOA system will fetch the corresponding request data from the SOA_PENDING_NBRPOOL_BLOCK
 * table based on the Messagekey and then appropriate object will be updated in the
 * SOA_NBRPOOL_BLOCK table based on the NPBID and RegionId, if there are any record exists.
 *
 *
 * @author Prasant Peter
 * @version 1.0
 * @Copyright (c) 2006-07 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 *
 * @see		com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see		com.nightfire.common.ProcessingException;
 * @see		com.nightfire.framework.message.MessageException;
 * @see         com.nightfire.framework.resource.ResourceException;
 * @see		com.nightfire.framework.util.Debug;
 * @see		com.nightfire.framework.util.NVPair;
 * @see		com.nightfire.framework.util.StringUtils;
 * @see		com.nightfire.spi.common.driver.MessageObject;
 * @see		com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see		com.nightfire.framework.resource.ResourceException;
 * @see		com.nightfire.framework.db.DBConnectionPool;
 * @see		com.nightfire.framework.db.DatabaseException;
 * @see		com.nightfire.framework.db.DBInterface;
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants;
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;




public class NPBPendingRequestProcessor extends DBMessageProcessorBase {
    
    /**
     * To indicate whether TRANSACTION_LOGGING true or false.
     */
    private boolean usingContextConnection = true;
    
    /**
     * Name of the oracle table to update values
     */
    private String updateTableName = null;
    
    /**
     * Name of the oracle table to search values
     */
    private String searchTableName = null;
    
    /**
     * The value of Message Key Reference
     */
    private String msgKeyRef = null;
    
    /**
     * The value of Message Key value
     */
    private String msgKeyValue = null;
    
    /**
     * The value of simple date format
     */
    private String strDateFormat = "MM-dd-yyyy-hhmmssa";
    
    /**
     * Class Constructor.
     */
    public NPBPendingRequestProcessor() {
        
        Debug.log(
                Debug.OBJECT_LIFECYCLE,
                "Creating NPBPendingRequestProcessor message-processor.");
        
    }
    
    /**
     * Initializes this object via its persistent properties.
     *
     * @param key Property-key to use for locating initialization properties.
     * @param type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize( String key, String type ) throws ProcessingException {
        
        // Call base class method to load the properties.
        super.initialize( key, type );
        
        // Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "NPBPendingRequestProcessor: Initializing..." );
		}
        
        // Variable contains error buffer string.
        StringBuffer errorBuffer = new StringBuffer( );
        
        // Update with appropriate values for specified TableName.
        updateTableName = getRequiredPropertyValue(
                SOAConstants.MSG_KEY_UPDATE_TABLE_NAME_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "UPDATE TABLE NAME is ["
					+ updateTableName + "]." );
		}
        
        // Search the values for the specified TableName.
        searchTableName = getRequiredPropertyValue(
                SOAConstants.MSG_KEY_SEARCH_TABLE_NAME_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "SEARCH TABLE NAME is ["
					+ searchTableName + "]." );
		}
        // Get the  msgKeyRef.
        msgKeyRef = getRequiredPropertyValue(
                SOAConstants.MSG_KEY_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of Message Key is ["
					+ msgKeyRef + "]." );
		}
        
        
        // If any of required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 ) {
            // Get the errMsg value.
            String errMsg = errorBuffer.toString( );
            
            Debug.log( Debug.ALL_ERRORS, errMsg );
            
            throw new ProcessingException( errMsg );
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
					"NPBPendingRequestProcessor: Initialization done." );
		}
    }  //end of initialize method
    
    
    /**
     * Extract data values from the context/input, and use them to
     * retrive data from SOA_PENDING_NBRPOOL_BLOCK table and UPDATES the
     * SOA_SUBSCRIPTION_VERSION table with the retrived data.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  An rarray of NVPair objects.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process(MessageProcessorContext mpContext,
            MessageObject inputObject)
            throws MessageException, ProcessingException {
        
    	ThreadMonitor.ThreadInfo tmti = null;
        // If the MessageObject value is not available.
        if ( inputObject == null ) {
            return null;
        }
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
          Debug.log(Debug.MSG_STATUS, "NPBPendingRequestProcessor: Processing ... ");
		}
        
        Connection dbConn = null;
        
	   try{
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        
	        msgKeyValue = (String) super.get(msgKeyRef, mpContext, inputObject);
	        
	        ArrayList refList = new ArrayList();
	        
	        ArrayList validColList = new ArrayList();
	        
	        HashMap colValMap = new HashMap();
	        
	        try {
	            
	            // Get the connection from context
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database logging is "
							+ "transactional, so getting connection from context." );
				}
	            
	            dbConn = mpContext.getDBConnection( );
	            
	            // If the Connection is not available throw ProcessingException
	            if (dbConn == null) {
	                
	                throw new ProcessingException("DB connection is not available");
	            }
	            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,"Connection Aquired...");
				}
	            
	            
	        } catch (FrameworkException e) {
	            
	            String errMsg =
	                    "ERROR: NPBPendingRequestProcessor: Attempt to get database "
	                    + "connection failed with error: "
	                    + e.getMessage();
	            
	            Debug.log(Debug.ALL_ERRORS, errMsg);
	            
	            if (e instanceof MessageException) {
	                
	                throw (MessageException) e;
	                
	            } else {
	                
	                throw new ProcessingException(errMsg);
	                
	            }
	        }
	        
	        
	        try {
	            
	            // Get corresponding data from  SOA_PENDING_NBRPOOL_BLOCK table for
	            // messagekey coming with Reply
	            if ( !retrieveMsgKeyData(dbConn, refList, validColList, colValMap) ) {
	                return( formatNVPair( inputObject ) );
	            }
	            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"NPBPendingRequestProcessor: Message Key Data Retrieved... ");
				}
	            
	            
	            // Update the SOA_SUBSCRIPTION_VERSION table for all the referenceKey, which are
	            // selected from  SOA_PENDING_NBRPOOL_BLOCK table for the messagekey coming with Reply
	            updateMsgKeyDataInBatch(dbConn,refList,validColList,colValMap);
	            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"NPBPendingRequestProcessor: Message Key Data Batch Updation Success... ");
				}
	            
	            
	        } catch (ProcessingException e) {
	            
	            String errMsg =
	                    "ERROR: NPBPendingRequestProcessor: Attempt to access database"
	                    + " failed with error: "
	                    + e.getMessage();
	            
	            Debug.log(Debug.ALL_ERRORS, errMsg);
	            
	            throw new ProcessingException(errMsg);
	            
	        } finally {
	            
	            // If the configuration indicates that this SQL operation
	            //isn't part of the overall driver transaction, return the
	            // connection previously acquired back to the resource pool.
	            
	            if ( !usingContextConnection ) {
	                
	                try {
	                    
	                    DBConnectionPool.getInstance().releaseConnection( dbConn );
	                    
	                    dbConn = null;
	                    
	                } catch ( ResourceException e ) {
	                    
	                    Debug.log( Debug.ALL_ERRORS, e.toString() );
	                    
	                }
	            }
	            
	        }
	      }finally {
        	ThreadMonitor.stop(tmti);
        }
        // Pass the input on to the output.
        return( formatNVPair( inputObject ) );
    }
    
    
    /**
     * The main objective of this method is to update data in
     * SOA_SUBSCRIPTION_VERSION table based on the Reference Keys
     * retried from "SOA_PENDING_RQUEST" table and passed as an input
     * parameter.
     *
     * The Transaction has been done throgh batch updates.
     *
     * Batch Update based on each retrieved Reference Key.
     *
     *
     * @param dbConn  	The database connection to perform the SQL
     * 					UPDATION operation against.
     * @param refList  	list of reference keys.
     * @param validColList  list of column names.
     * @param colValMap  map of column name and its corresponding value.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void updateMsgKeyDataInBatch(Connection dbConn,
            ArrayList refList,
            ArrayList validColList,
            HashMap colValMap)
            throws ProcessingException {
        
        // Initilization value for batchUpdStatement.
        PreparedStatement batchUpdStatement = null;
        
        // Initilization value for Date Format.
        SimpleDateFormat sdf = null;
        
        // Initilization value for jDate.
        java.util.Date jDate = null;
        
        // Initilzation value for batchUpdateFlg.
        int[] batchUpdateFlg = null;
        
        // Initilzation value for strBatchUpdStmt.
        String strBatchUpdStmt = null;
        
        // Initilization value for validColSize.
        int validColSize = 0;
        
        try {
            
            // If the Reference Key(s) are not available.
            if (refList == null || refList.size() <= 0) {
                
                String errMsg =
                        "ERROR: NPBPendingRequestProcessor: updateMsgKeyDataInBatch : " +
                        "NPBID and RegionId does not contains any Values.";
                
                Debug.log(Debug.ALL_ERRORS, errMsg);
                
                throw new ProcessingException(errMsg);
                
            } 
            
            // Get the list of column names those need to update.
            if (validColList != null && validColList.size() > 0) {
                
                validColSize = validColList.size();
                
            } else {
                
                String errMsg =
                        "ERROR: NPBPendingRequestProcessor: updateMsgKeyDataInBatch : " +
                        "Valid Column List does not contains any Column Names.";
                
                Debug.log(Debug.ALL_ERRORS, errMsg);
                
                throw new ProcessingException(errMsg);
            }
            
            sdf = new SimpleDateFormat(strDateFormat);
            
            strBatchUpdStmt = getBatchUpdateStmt(validColList);
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
						Debug.MSG_STATUS,"Batch Update Statement : " + strBatchUpdStmt);
			}
            
            batchUpdStatement = dbConn.prepareStatement(strBatchUpdStmt);
            
            // Iterate for the no of reference keys found
            /*for(int i=0;i<refListSize;i++){*/
            
            int valSetPos = 1;
            
            String strNPBId = (String) refList.get(0);      //get NPBID or Block Id
            
            String strRegionId = (String) refList.get(1);   //get Region Id
            
            // For all the column those need to update
            for (int k = 0; k < validColSize; k++) {
                
                String strColName = (String) validColList.get(k);
                
                String strColValue = null;
                
                strColValue = (String) colValMap.get(strColName);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Mapped Values ["
							+ k
							+ "] : "
							+ strColName
							+ " = "
							+ strColValue);
				}
                // If the column name is a LastRequestDate or Activation date then parse the Column Value.
                if (SOAConstants.LASTREQUESTDATE_COL.equalsIgnoreCase(strColName)
                || SOAConstants.ACTIVATION_DATE_COL.equalsIgnoreCase(strColName)) {
                    try {
                        
                        jDate = sdf.parse(strColValue);
                        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							Debug.log(
									Debug.MSG_STATUS,"Parsed Date value = " + jDate);
						}
                        
                    } catch (ParseException parseException) {
                        
                        throw new ProcessingException(
                                "ERROR: could not parse date"
                                + "\for ["
                                + strColName
                                + "] ."
                                + parseException.toString());
                        
                    }
                    
                    java.sql.Timestamp ts =
                            new java.sql.Timestamp(jDate.getTime());
                    
                    batchUpdStatement.setTimestamp(valSetPos, ts);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(
								Debug.MSG_STATUS,
								"batchUpdStatement.setTimestamp : refPos = "
								+ valSetPos
								+ " date = "
								+ ts.toString());
					}
                    
                } else {
                    
                    //Modify As Delete of Data, replacing - with null
                    batchUpdStatement.setObject(valSetPos, (strColValue.equals("-")?null:strColValue));
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(
								Debug.MSG_STATUS,
								"batchUpdStatement.setObject : refPos = "
								+ valSetPos
								+ " strColValue = "
								+ (strColValue.equals("-")?null:strColValue));
					}
                }
                
                
                valSetPos += 1;
                
            }// end of for (int k = 0; k < colSize; k++)
            
            batchUpdStatement.setLong(valSetPos, Long.parseLong(strNPBId));
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
						Debug.MSG_STATUS,
						"batchUpdStatement.setLong : refPos = "
						+ valSetPos
						+ " NPBID Value = "
						+ strNPBId);
			}
            valSetPos++;
            batchUpdStatement.setLong(valSetPos, Long.parseLong(strRegionId));
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
						Debug.MSG_STATUS,
						"batchUpdStatement.setLong : refPos = "
						+ valSetPos
						+ " REGIONID Value = "
						+ strRegionId);
			}
            
            batchUpdStatement.addBatch();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
						Debug.MSG_STATUS,
						"Update Statement added to Batch.");
			}
            
            /*}//end of for(int i=0;i<refListSize;i++)*/
            
            batchUpdateFlg = batchUpdStatement.executeBatch();
            
            //  Get total number of rows updated.
            if(batchUpdateFlg!=null){
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Batch Statement Execution Success with ("+batchUpdateFlg.length+" ) Rows Updated");
				}
                
                
            } else {
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Batch Statement Execution Failure.");
				}
            }
            
            
        } catch (BatchUpdateException bex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: Attempt to batch update database on sole reference key"
                    + " failed with error: "
                    + bex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            throw new ProcessingException(errMsg);
            
        } catch (Exception ex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: Attempt to batch update database on sole reference key"
                    + " failed with error: "
                    + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            throw new ProcessingException(errMsg);
            
        } finally {
            
            
            sdf = null;
            
            jDate =  null;
            
            if (batchUpdStatement != null) {
                
                try {
                    
                    batchUpdStatement.close();
                    
                    batchUpdStatement = null;
                    
                } catch (SQLException sqle) {
                    
                    Debug.log(
                            Debug.ALL_ERRORS,
                            DBInterface.getSQLErrorMessage(sqle));
                }
                
            }
            
        }
        
    }
    
    
    
    /**
     * The main objective of this method is to retrive data from
     * SOA_PENDING_RQUEST table based on the Messagekey, creates a
     * reference key list, creates a column name list if the retrived
     * column contains any value from the table, and creates a Map Table
     * with the column names and their corresponding values.
     *
     * @param dbConn  	The database connection to perform the SQL
     * 					SELECT operation against.
     * @param refList  	populated list of reference keys.
     * @param validColList  populated list of column names.
     * @param colValMap  populated map of column name and
     * 					 corresponding values.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private boolean retrieveMsgKeyData(Connection dbConn,
            ArrayList refList,
            ArrayList validColList,
            HashMap colValMap)
            throws ProcessingException {
        
        // Initilization strLrn value.
        String strSPID = null;
        // Initilization strLrn value.
        String strLrn = null;
        // Initilization strClassDpc value.
        String strClassDpc = null;
        // Initilization strClassSsn value.
        String strClassSsn = null;
        // Initilization strCnamDpc value.
        String strCnamDpc = null;
        // Initilization strCnamSsn value.
        String strCnamSsn = null;
        // Variable strIsvmDpc value of ISVMDPC.
        String strIsvmDpc = null;
        // Initilization strIsvmSsn value.
        String strIsvmSsn = null;
        // Initilization strLidbDpc value.
        String strLidbDpc = null;
        // Initilization strLidbSsn value.
        String strLidbSsn = null;
        // Initilization strWsmscDpc value.
        String strWsmscDpc = null;
        // Initilization strWsmscSsn value.
        String strWsmscSsn = null;
        // Initilization strLastRequestType value.
        String strLastRequestType = null;
        // Initilization strLastRequestDate value.
        String strLastRequestDate = null;
        // Initilization strWsmscSsn value.
        String strActivationDate = null;
        // Initilization strNPBType value.
        String strNPBType = null;
        // Initilization strAlternativeSpid value.
        String strAlternativeSpid =  null;
        //Initilization strAlternativeEndUserLocationValue value.
	String strAlternativeEndUserLocationValue = null;
	//Initilization strAlternativeEndUserLocationType value.
	String strAlternativeEndUserLocationType = null;
	//Initilization strAlternativeBillingId value.
	String strAlternativeBillingId = null;
        // Initilization strVoiceURI value.
        String strVoiceURI =  null;
        // Initilization strMMSURI value.
        String strMMSURI =  null;
        // Initilization strPoCURI value.
        String strPoCURI =  null;
        // Initilization strPRESURI value.
        String strPRESURI =  null;
     // Initilization strSMSURI value.
        String strSMSURI =  null;
        
        String strLastAlternativeSPID = null;
        
        String strSpcustom1 = null;
        String strSpcustom2 = null;
        String strSpcustom3 = null;
        
        // Initialization mkQryStatement.
        PreparedStatement mkQryStatement = null;
        // Initilization Result value.
        ResultSet rs = null;
        // Initilization Date Value.
        SimpleDateFormat sdf = null;
        // Initilization Timestamp value.
        java.sql.Timestamp d = null;
        
        // Initilization pendingRequest value.
        boolean pendingRequest = false;
        
        try {
            
            sdf = new SimpleDateFormat(strDateFormat);
            
            // Get the query which needs to execute for the message Key
            String strSearchQry = getSearchQryStmt();
            if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
             Debug.log(Debug.NORMAL_STATUS, "Query to fetch Pending Record from SOA_PENDING_NBRPOOL_BLOCK table : " + strSearchQry);
			}
            
            mkQryStatement = dbConn.prepareStatement(strSearchQry);
            
            //mkQryStatement.setString(1, msgKeyValue);
            mkQryStatement.setLong(1, Long.parseLong(msgKeyValue));
            
            rs = mkQryStatement.executeQuery();
            
            //if ResultSet contains more values.
            if(rs.next()) {
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Values retieved once for Message Key = " + msgKeyValue);
                }
                // DATA RETRIEVAL FOR SPID
                strSPID = rs.getString(SOAConstants.SPID_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of LRN = " + strSPID);
                }
                if (StringUtils.hasValue(strSPID)) {
                    
                    validColList.add(SOAConstants.SPID_COL);
                    
                    colValMap.put(SOAConstants.SPID_COL, strSPID);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR LRN
                strLrn = rs.getString(SOAConstants.LRN_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of LRN = " + strLrn);
                }
                if (StringUtils.hasValue(strLrn)) {
                    
                    validColList.add(SOAConstants.LRN_COL);
                    
                    colValMap.put(SOAConstants.LRN_COL, strLrn);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR CLASSDPC
                strClassDpc = rs.getString(SOAConstants.CLASSDPC_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of CLASSDPC = " + strClassDpc);
                }
                if (StringUtils.hasValue(strClassDpc)) {
                    
                    validColList.add(SOAConstants.CLASSDPC_COL);
                    
                    colValMap.put(SOAConstants.CLASSDPC_COL, strClassDpc);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR CLASSSSN
                strClassSsn = rs.getString(SOAConstants.CLASSSSN_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of CLASSSSN = " + strClassSsn);
                }
                if (StringUtils.hasValue(strClassSsn)) {
                    
                    validColList.add(SOAConstants.CLASSSSN_COL);
                    
                    colValMap.put(SOAConstants.CLASSSSN_COL, strClassSsn);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR CNAMDPC
                strCnamDpc = rs.getString(SOAConstants.CNAMDPC_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of CNAMDPC = " + strCnamDpc);
                }
                if (StringUtils.hasValue(strCnamDpc)) {
                    
                    validColList.add(SOAConstants.CNAMDPC_COL);
                    
                    colValMap.put(SOAConstants.CNAMDPC_COL, strCnamDpc);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR  CNAMSSN
                strCnamSsn = rs.getString(SOAConstants.CNAMSSN_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of CNAMSSN = " + strCnamSsn);
                }
                if (StringUtils.hasValue(strCnamSsn)) {
                    
                    validColList.add(SOAConstants.CNAMSSN_COL);
                    
                    colValMap.put(SOAConstants.CNAMSSN_COL, strCnamSsn);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR ISVMDPC
                strIsvmDpc = rs.getString(SOAConstants.ISVMDPC_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of ISVMDPC = " + strIsvmDpc);
                }
                if (StringUtils.hasValue(strIsvmDpc)) {
                    
                    validColList.add(SOAConstants.ISVMDPC_COL);
                    
                    colValMap.put(SOAConstants.ISVMDPC_COL, strIsvmDpc);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR ISVMSSN
                strIsvmSsn = rs.getString(SOAConstants.ISVMSSN_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of ISVMSSN = " + strIsvmSsn);
                }
                if (StringUtils.hasValue(strIsvmSsn)) {
                    
                    validColList.add(SOAConstants.ISVMSSN_COL);
                    
                    colValMap.put(SOAConstants.ISVMSSN_COL, strIsvmSsn);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR LIDBDPC
                strLidbDpc = rs.getString(SOAConstants.LIDBDPC_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of LIDBDPC = " + strLidbDpc);
                }
                if (StringUtils.hasValue(strLidbDpc)) {
                    
                    validColList.add(SOAConstants.LIDBDPC_COL);
                    
                    colValMap.put(SOAConstants.LIDBDPC_COL, strLidbDpc);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR LIDBSSN
                strLidbSsn = rs.getString(SOAConstants.LIDBSSN_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                  Debug.log(Debug.MSG_STATUS, "Value of LIDBSSN = " + strLidbSsn);
                }
                if (StringUtils.hasValue(strLidbSsn)) {
                    
                    validColList.add(SOAConstants.LIDBSSN_COL);
                    
                    colValMap.put(SOAConstants.LIDBSSN_COL, strLidbSsn);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR WSMSCDPC
                strWsmscDpc = rs.getString(SOAConstants.WSMSCDPC_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                  Debug.log(Debug.MSG_STATUS, "Value of WSMSCDPC = " + strWsmscDpc);
                }
                if (StringUtils.hasValue(strWsmscDpc)) {
                    
                    validColList.add(SOAConstants.WSMSCDPC_COL);
                    
                    colValMap.put(SOAConstants.WSMSCDPC_COL, strWsmscDpc);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR WSMSCSSN
                strWsmscSsn = rs.getString(SOAConstants.WSMSCSSN_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                  Debug.log(Debug.MSG_STATUS, "Value of WSMSCSSN = " + strWsmscSsn);
                }
                if (StringUtils.hasValue(strWsmscSsn)) {
                    
                    validColList.add(SOAConstants.WSMSCSSN_COL);
                    
                    colValMap.put(SOAConstants.WSMSCSSN_COL, strWsmscSsn);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR LASTREQUESTTYPE
                strLastRequestType = rs.getString(SOAConstants.LASTREQUESTTYPE_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of LASTREQUESTTYPE = " + strLastRequestType);
				}
                
                if (StringUtils.hasValue(strLastRequestType)) {
                    
                    validColList.add(SOAConstants.LASTREQUESTTYPE_COL);
                    
                    colValMap.put(
                            SOAConstants.LASTREQUESTTYPE_COL,
                            strLastRequestType);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR LASTREQUESTDATE
                d = rs.getTimestamp(SOAConstants.LASTREQUESTDATE_COL);
                
                if (d != null){
                    strLastRequestDate = sdf.format(d);
                    
                }
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of LASTREQUESTDATE = " + strLastRequestDate);
                }
                if (strLastRequestDate != null && !"".equals(strLastRequestDate)) {
                    
                    validColList.add(SOAConstants.LASTREQUESTDATE_COL);
                    
                    colValMap.put(
                            SOAConstants.LASTREQUESTDATE_COL,
                            strLastRequestDate);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR ACTIVATIONDATE
                d = rs.getTimestamp(SOAConstants.ACTIVATION_DATE_COL);
                
                if (d != null){
                    strLastRequestDate = sdf.format(d);
                }
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of LASTREQUESTDATE = " + strLastRequestDate);
				}
                
                if (strLastRequestDate != null && !"".equals(strLastRequestDate)) {
                    
                    validColList.add(SOAConstants.ACTIVATION_DATE_COL);
                    
                    colValMap.put(
                            SOAConstants.ACTIVATION_DATE_COL,
                            strLastRequestDate);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR NPBType
                strNPBType = rs.getString(SOAConstants.NPBTYPE_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of NPBTYPE = " + strNPBType);
				}
                
                if (StringUtils.hasValue(strNPBType)) {
                    
                    validColList.add(SOAConstants.NPBTYPE_COL);
                    
                    colValMap.put(
                            SOAConstants.NPBTYPE_COL,
                            strNPBType);
                    
                    pendingRequest = true;
                }
                
                // DATA RETRIEVAL FOR ALTERNATIVESPID
                strAlternativeSpid =
                        rs.getString(SOAConstants.ALTERNATIVESPID_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of ALTERNATIVESPID = " + strAlternativeSpid);
				}
                
                if (StringUtils.hasValue(strAlternativeSpid)) {
                    
                    validColList.add(SOAConstants.ALTERNATIVESPID_COL);
                    
                    colValMap.put(
                            SOAConstants.ALTERNATIVESPID_COL,
                            strAlternativeSpid);
                    
                    pendingRequest = true;
                }
                //DATA RETRIEVAL FOR ALTERNATIVEBILLINGID
				strAlternativeBillingId = rs.getString(SOAConstants.ALTERNATIVE_BILLINGID_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				 Debug.log(Debug.MSG_STATUS, "Value of ALTERNATIVEBILLINGID = " + strAlternativeBillingId);
				}

				if (StringUtils.hasValue(strAlternativeBillingId)) {

					validColList.add(SOAConstants.ALTERNATIVE_BILLINGID_COL);

					colValMap.put(SOAConstants.ALTERNATIVE_BILLINGID_COL, strAlternativeBillingId);

					pendingRequest = true;
				}

				// DATA RETRIEVAL FOR ALTERNATIVEENDUSERLOCATIONTYPE
				strAlternativeEndUserLocationType =
					rs.getString(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONTYPE_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of ALTERNATIVEENDUSERLOCATIONTYPE = " + strAlternativeEndUserLocationType);
				}

				if (StringUtils.hasValue(strAlternativeEndUserLocationType)) {

					validColList.add(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONTYPE_COL);

					colValMap.put(
						SOAConstants.ALTERNATIVE_ENDUSERLOCATIONTYPE_COL,
						strAlternativeEndUserLocationType);

					pendingRequest = true;
				}

				// DATA RETRIEVAL FOR ALTERNATIVEENDUSERLOCATIONVALUE
				strAlternativeEndUserLocationValue=
					rs.getString(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of ALTERNATIVEENDUSERLOCATIONVALUE = " + strAlternativeEndUserLocationValue);
				}

				if (StringUtils.hasValue(strAlternativeEndUserLocationValue)) {

					validColList.add(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL);

					colValMap.put(
						SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL,
						strAlternativeEndUserLocationValue);

					pendingRequest = true;
				}
                // DATA RETRIEVAL FOR VoiceURI
                strVoiceURI =
                        rs.getString(SOAConstants.VOICEURI_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of VoiceURI = " + strVoiceURI);
				}
                
                if (StringUtils.hasValue(strVoiceURI)) {
                    
                    validColList.add(SOAConstants.VOICEURI_COL);
                    
                    colValMap.put(
                            SOAConstants.VOICEURI_COL,
                            strVoiceURI);
                    
                    pendingRequest = true;
                }
                //DATA RETRIEVAL FOR MMSURI
                strMMSURI =
                        rs.getString(SOAConstants.MMSURI_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of MMSURI = " + strMMSURI);
				}
                
                if (StringUtils.hasValue(strMMSURI)) {
                    
                    validColList.add(SOAConstants.MMSURI_COL);
                    
                    colValMap.put(
                            SOAConstants.MMSURI_COL,
                            strMMSURI);
                    
                    pendingRequest = true;
                }
                //DATA RETRIEVAL FOR PoCURI
                strPoCURI =
                        rs.getString(SOAConstants.POCURI_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of PoCURI = " + strPoCURI);
				}
                
                if (StringUtils.hasValue(strPoCURI)) {
                    
                    validColList.add(SOAConstants.POCURI_COL);
                    
                    colValMap.put(
                            SOAConstants.POCURI_COL,
                            strPoCURI);
                    
                    pendingRequest = true;
                }
                //DATA RETRIEVAL FOR PRESURI
                strPRESURI =
                        rs.getString(SOAConstants.PRESURI_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of PRESURI = " + strPRESURI);
				}
                
                if (StringUtils.hasValue(strPRESURI)) {
                    
                    validColList.add(SOAConstants.PRESURI_COL);
                    
                    colValMap.put(
                            SOAConstants.PRESURI_COL,
                            strPRESURI);
                    
                    pendingRequest = true;
                }
                
                //DATA RETRIEVAL FOR SMSURI
                strSMSURI =
                        rs.getString(SOAConstants.SMSURI_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of SMSURI = " + strSMSURI);
				}
                
                if (StringUtils.hasValue(strSMSURI)) {
                    
                    validColList.add(SOAConstants.SMSURI_COL);
                    
                    colValMap.put(
                            SOAConstants.SMSURI_COL,
                            strSMSURI);
                    
                    pendingRequest = true;
                }
                
                //DATA RETRIEVAL FOR LASTALTERNATIVESPID
                strLastAlternativeSPID = rs.getString(SOAConstants.LASTALTERNATIVESPID_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS,
							"Value of LASTALTERNATIVESPID = " + strLastAlternativeSPID);
				}
                if(StringUtils.hasValue(strLastAlternativeSPID)){
                	
                	validColList.add(SOAConstants.LASTALTERNATIVESPID_COL);
                	
                	colValMap.put(SOAConstants.LASTALTERNATIVESPID_COL, strLastAlternativeSPID);
                	
                	pendingRequest = true;
                }
                
                //DATA RETRIEVAL FOR SPCUSTOM1
                strSpcustom1 = rs.getString(SOAConstants.SPCUSTOM1_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS, "Value of SPCUSTOM1 = " + strSpcustom1);
				}
                if(StringUtils.hasValue(strSpcustom1)){
                	
                	validColList.add(SOAConstants.SPCUSTOM1_COL);
                	
                	colValMap.put(SOAConstants.SPCUSTOM1_COL, strSpcustom1);
                	
                	pendingRequest = true;
                }
                
                //DATA RETRIEVAL FOR SPCUSTOM2
                strSpcustom2 = rs.getString(SOAConstants.SPCUSTOM2_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS, "Value of SPCUSTOM2 = " + strSpcustom2);
				}
                if(StringUtils.hasValue(strSpcustom2)){
                	
                	validColList.add(SOAConstants.SPCUSTOM2_COL);
                	
                	colValMap.put(SOAConstants.SPCUSTOM2_COL, strSpcustom2);
                	
                	pendingRequest = true;
                }
                
                //DATA RETRIEVAL FOR SPCUSTOM3
                strSpcustom3 = rs.getString(SOAConstants.SPCUSTOM3_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
							Debug.MSG_STATUS, "Value of SPCUSTOM3 = " + strSpcustom3);
				}
                if(StringUtils.hasValue(strSpcustom3)){
                	
                	validColList.add(SOAConstants.SPCUSTOM3_COL);
                	
                	colValMap.put(SOAConstants.SPCUSTOM3_COL, strSpcustom3);
                	
                	pendingRequest = true;
                }
                
                String strNPBId = rs.getString(SOAConstants.NPBID_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                 Debug.log(Debug.MSG_STATUS, "Value of NPBID = " + strNPBId);
				}
                
                refList.add(strNPBId);
                
                String strRegionId = rs.getString(SOAConstants.REGION_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                  Debug.log(Debug.MSG_STATUS, "Value of RegionId = " + strRegionId);
				}
                
                refList.add(strRegionId);
                
            } // end of if (rs.next())
            
        } catch (SQLException ex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: retrieveMsgKeyData() : " +
                    " Attempt to query database failed with error: "+ ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            throw new ProcessingException(errMsg);
            
        } catch (Exception e) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: retrieveMsgKeyData() : " +
                    " Attempt to query database failed with error: "+ e.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            throw new ProcessingException(errMsg);
            
        } finally {
            
            sdf = null;
            
            if (rs != null) {
                
                try {
                    rs.close();
                    rs = null;
                } catch (SQLException sqle) {
                    Debug.log(
                            Debug.ALL_ERRORS,
                            DBInterface.getSQLErrorMessage(sqle));
                }
            }
            if (mkQryStatement != null) {
                try {
                    mkQryStatement.close();
                    mkQryStatement = null;
                } catch (SQLException sqle) {
                    Debug.log(
                            Debug.ALL_ERRORS,
                            DBInterface.getSQLErrorMessage(sqle));
                }
            }
        }
        return pendingRequest;
    }
    
    
    
    /**
     * Returns the SQL update statement to be used for PreparedStatement for UPDATING
     * the SOA_SUBSCRIPTION_VERSION table. The statement is generated based on the
     * values retrieved from the column list contains valid column names.
     *
     * @param columns	List of valid column names
     * @return updQry 	Generated SQL Update Statement
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public String getBatchUpdateStmt(ArrayList columns)
    throws ProcessingException {
        
        String updQry = null;
        
        StringBuffer updMessage = null;
        
        try {
            
            updMessage = new StringBuffer();
            
            updMessage.append("UPDATE " + updateTableName + " SET ");
            
            updMessage.append(((String) columns.get(0)) + " = ? ");
            
            int size = columns.size();
            
            
            for (int j = 1; j < size; j++) {
                
                String strColName = (String) columns.get(j);
                
                updMessage.append(", " + strColName + " = ? ");
                
            }
            
            updMessage.append(
                    " WHERE " + SOAConstants.NPBID_COL + " = ? ");
            
            updMessage.append(" AND REGIONID = ? ");
            
            updQry = updMessage.toString();
            
            
        } catch (Exception ex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: getBatchUpdateStmt() : " +
                    "Attempt to generate sql update statement failed " +
                    "with error: " + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            // Re-throw the exception to the calling function.
            throw new ProcessingException(errMsg);
            
        } finally {
            
            updMessage = null;
        }
        
        return updQry;
    }
    
    /**
     * Returns the select statement to be used for PreparedStatement for quering
     * the SOA_PENDING_RQUEST table to retrive records for the given Message Key.
     *
     * @return searchQry 	select statement for query
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public String getSearchQryStmt() throws ProcessingException {
        
        String searchQry = null;
        
        StringBuffer queryMessage = null;
        
        try {
            
            queryMessage = new StringBuffer();
            
            queryMessage.append("SELECT ");
            
            queryMessage.append(SOAConstants.SPID_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.NPBID_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.REGION_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.LRN_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.CLASSDPC_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.CLASSSSN_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.CNAMDPC_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.CNAMSSN_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.ISVMDPC_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.ISVMSSN_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.LIDBDPC_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.LIDBSSN_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.WSMSCDPC_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.WSMSCSSN_COL);
            
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.LASTREQUESTTYPE_COL );
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.LASTREQUESTDATE_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.ACTIVATION_DATE_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.NPBTYPE_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.ALTERNATIVESPID_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.VOICEURI_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.MMSURI_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.POCURI_COL);
            
            queryMessage.append(", ");
            
            queryMessage.append(SOAConstants.PRESURI_COL);
            
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.ALTERNATIVE_BILLINGID_COL);
			
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONTYPE_COL);
			
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL);
            
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.SMSURI_COL);
            
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.LASTALTERNATIVESPID_COL); 
            
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.SPCUSTOM1_COL); 

            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.SPCUSTOM2_COL); 
            
            queryMessage.append(", ");
			
            queryMessage.append(SOAConstants.SPCUSTOM3_COL); 

            
            queryMessage.append(" FROM ");
            
            queryMessage.append(searchTableName);
            
            queryMessage.append(
                    " WHERE " + SOAConstants.MESSAGEKEY_COL + " = ? ");
            
            searchQry = queryMessage.toString();
            
            
        } catch (Exception ex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: Attempt to generate query statement"
                    + " failed with error: "
                    + ex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            // Re-throw the exception to the driver.
            throw new ProcessingException(errMsg);
            
        } finally {
            
            queryMessage = null;
        }
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(
					Debug.MSG_STATUS,
					"Generated Query : " + searchQry);
		}
        
        return searchQry;
    }
    
    
    
    //--------------------------For Testing---------------------------------//
    
    public static void main(String[] args) {
        
        Properties props = new Properties();
        
        props.put("DEBUG_LOG_LEVELS", "ALL");
        
        props.put("LOG_FILE", "C:\\SOA_LOG\\log_soa.txt");
        
        Debug.showLevels();
        
        Debug.configureFromProperties(props);
        
        if (args.length != 3) {
            
            Debug.log(Debug.ALL_ERRORS, "NPBPendingRequestProcessor: USAGE:  "+
                    " jdbc:oracle:thin:@192.168.198.6:1521:FULLSOA peter peter ");
            
            return;
        }
        
        try {
            
            
            DBInterface.initialize(args[0], args[1], args[2]);
            
            
        } catch (DatabaseException e) {
            
            Debug.log(
                    null,
                    Debug.MAPPING_ERROR,
                    ": " + "Database initialization failure: " + e.getMessage());
            
            System.out.println(
                    "Exception Caught in main : Database initialization failure: "
                    + e.getMessage());
            
        }
        
        NPBPendingRequestProcessor msgKeyUpdate = new NPBPendingRequestProcessor();
        
        
        try {
            
            msgKeyUpdate.initialize("FULL_NEUSTAR_SOA", "NPBPendingRequestProcessor");
            
            MessageProcessorContext mpx = new MessageProcessorContext();
            
            MessageObject mob = new MessageObject();
            
            msgKeyUpdate.process(mpx, mob);
            
            Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());
            
            Debug.log(Debug.BENCHMARK, "Process Executed...");
            
            
        } catch (ProcessingException pex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: ProcessingException Caught in main()	: "
                    + pex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            System.out.println(
                    "ERROR: NPBPendingRequestProcessor: ProcessingException Caught in main()	: "
                    + pex.getMessage());
            
        } catch (MessageException mex) {
            
            String errMsg =
                    "ERROR: NPBPendingRequestProcessor: MessageException Caught in main()	: "
                    + mex.getMessage();
            
            Debug.log(Debug.ALL_ERRORS, errMsg);
            
            System.out.println(
                    "ERROR: NPBPendingRequestProcessor: MessageException Caught in main()	: "
                    + mex.getMessage());
            
        }
        
    } //end of main method
    
}

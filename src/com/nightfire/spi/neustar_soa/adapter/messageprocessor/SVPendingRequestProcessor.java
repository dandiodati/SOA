/**
 * The main objective of this Component is to Update the SOA_SUBSCRIPTION_VERSION 
 * table based on the Messagekey received from NPAC as a successful responce.
 * 
 * SOA receives successful Synchronous Response from NPAC for SvCreateRequest, 
 * SvModifyRequest and SvDisconnectRequest SOA system will fetch the corresponding 
 * request data from the SOA_PENDING_RQUEST table based on the Messagekey and then 
 * appropriate Sv object will be updated in the SOA_SUBSCRIPTION_VERSION table based 
 * on the Referencekey, if there are any record exists. 
 * 
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
 * @see     com.nightfire.framework.resource.ResourceException;
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

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Peter			05/01/2007		Created
	2			VRameshChimata	02/05/2007		Revision comments incorporated.
	
	3			Peeyush M		05/22/2007		Changes for SubDomain requirement.

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




public class SVPendingRequestProcessor extends DBMessageProcessorBase
{

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
	public SVPendingRequestProcessor() {

		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log(
			Debug.OBJECT_LIFECYCLE,
			"Creating SVPendingRequestProcessor message-processor.");
		}

	}

		/**
		 * Initializes this object via its persistent properties.
		 *
		 * @param key Property-key to use for locating initialization properties.
		 * @param type Property-type to use for locating initialization properties.
		 *
		 * @exception ProcessingException when initialization fails
		 */
		public void initialize ( String key, String type ) throws ProcessingException
		{
	
			// Call base class method to load the properties.
			super.initialize( key, type );
	
			// Get configuration properties specific to this processor.
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "SVPendingRequestProcessor: Initializing..." );
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
			if ( errorBuffer.length() > 0 )
			{
				// Get the errMsg value.
				String errMsg = errorBuffer.toString( );
	
				Debug.log( Debug.ALL_ERRORS, errMsg );
	
				throw new ProcessingException( errMsg );
			}
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,
							"SVPendingRequestProcessor: Initialization done." );
			}
		 }  //end of initialize method


	/**
	 * Extract data values from the context/input, and use them to
	 * retrive data from SOA_PENDING_REQUEST table and UPDATES the
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
		if ( inputObject == null )
		{
			return null;
		}
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "SVPendingRequestProcessor: Processing ... ");
		}

		Connection dbConn = null;
		try
		{
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
				"ERROR: SVPendingRequestProcessor: Attempt to get database "
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

			// Get corresponding data from SOA_PENDING_REQUEST table for 
			// messagekey coming with Reply
			if ( !retrieveMsgKeyData(dbConn, refList, validColList, colValMap) )
			{
				return( formatNVPair( inputObject ) );
			}
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"SVPendingRequestProcessor: Message Key Data Retrieved... ");
			}

		
			// Update the SOA_SUBSCRIPTION_VERSION table for all the referenceKey, which are
			// selected from SOA_PENDING_REQUEST table for the messagekey coming with Reply
			updateMsgKeyDataInBatch(dbConn,refList,validColList,colValMap);
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"SVPendingRequestProcessor: Message Key Data Batch Updation Success... ");
			}
		

		} catch (ProcessingException e) {

			String errMsg =
				"ERROR: SVPendingRequestProcessor: Attempt to access database"
					+ " failed with error: "
					+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
			
		} finally {

			// If the configuration indicates that this SQL operation
			//isn't part of the overall driver transaction, return the
			// connection previously acquired back to the resource pool.
		
		  	if ( !usingContextConnection )
		  	{

			  	try
			  	{

				  	DBConnectionPool.getInstance().releaseConnection( dbConn );

				  	dbConn = null;

			  	}
			  	catch ( ResourceException e )
			  	{

				  Debug.log( Debug.ALL_ERRORS, e.toString() );

			  	}
		  	}

		}
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		// Pass the input on to the output.
		return( formatNVPair( inputObject ) );
	}


	/**
	 * The main objective of this method is to update data in 
	 * SOA_SUBSCRIPTION_VERSION table based on the Reference Keys 
	 * retried from SOA_PENDING_RQUEST table and passed as an input 
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
		
		// Initilzation value for strBatchUpdStmt.
		String strBatchUpdStmt = null;
		
		// Initilization value for validColSize.
		int validColSize = 0;
		
		// Initilization value for refListSize.
		int refListSize = 0;
		

		try {
			
			// If the Reference Key(s) are not available.
			if (refList == null || refList.size() <= 0) {
		
				String errMsg =
					"ERROR: SVPendingRequestProcessor: updateMsgKeyDataInBatch : " +
					"Reference Key List does not contains any Values.";

				Debug.log(Debug.ALL_ERRORS, errMsg);

				throw new ProcessingException(errMsg);
				
			} else {
				
				refListSize = refList.size();
			}
			
			// Get the list of column names those need to update.
			if (validColList != null && validColList.size() > 0) {
			
				validColSize = validColList.size();
			
			} else {
				
				String errMsg =
					"ERROR: SVPendingRequestProcessor: updateMsgKeyDataInBatch : " +
					"Valid Column List does not contains any Column Names.";

				Debug.log(Debug.ALL_ERRORS, errMsg);

				throw new ProcessingException(errMsg);
			}

			sdf = new SimpleDateFormat(strDateFormat);

			strBatchUpdStmt = getBatchUpdateStmt(validColList,1);
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,"Batch Update Statement : "+strBatchUpdStmt);
			}

			batchUpdStatement = dbConn.prepareStatement(strBatchUpdStmt);

			// Iterate for the no of reference keys found	
			for(int i=0;i<refListSize;i++){
				
				int valSetPos = 1;
				
				String strRefKeyVal = (String) refList.get(i);

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
					// If the column name is a CustomerDisconnectDate, 
					//EffectiveReleaseDate or nnspDueDate then parse the Column Value.
					if (SOAConstants.CUSTOMERDISCONNECTDATE_NODE.equalsIgnoreCase(strColName)
						|| SOAConstants.EFFECTIVERELEASEDATE_NODE.equalsIgnoreCase(strColName)
						|| SOAConstants.NNSPDUEDATE_COL.equalsIgnoreCase(strColName)) {

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

				batchUpdStatement.setObject(valSetPos, new Integer(strRefKeyVal) );
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"batchUpdStatement.setObject : refPos = "
							+ valSetPos
							+ " Reference Key Value = "
							+ strRefKeyVal);
				}
				
				batchUpdStatement.addBatch();
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Update Statement added to Batch.");
				}
				
			}//end of for(int i=0;i<refListSize;i++)				
				
			int[] batchUpdateFlg = batchUpdStatement.executeBatch();
				
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
				"ERROR: SVPendingRequestProcessor: Attempt to batch update database on sole reference key"
					+ " failed with error: "
					+ bex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);

		} catch (Exception ex) {
			
			String errMsg =
				"ERROR: SVPendingRequestProcessor: Attempt to batch update database on sole reference key"
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
		// Initilization strBillingId value.
		String strBillingId = null;
		// Initilization strEndUserLocationType value.
		String strEndUserLocationType = null;
		// Initilization strEndUserLocationValue value.
		String strEndUserLocationValue = null;
		//Initilization strAlternativeEndUserLocationValue value.
		String strAlternativeEndUserLocationValue = null;
		//Initilization strAlternativeEndUserLocationType value.
		String strAlternativeEndUserLocationType = null;
		//Initilization strAlternativeBillingId value.
		String strAlternativeBillingId = null;
		// Initilization strCauseCode value.
		String strCauseCode = null;
		// Initilization strCustomerDisconnectDate value.
		String strCustomerDisconnectDate = null;
		// Initilization strEffectiveReleaseDate value.
		String strEffectiveReleaseDate = null;
		// Initilization strSvType value.
		String strSvType =  null;
		// Initilization strAlternativeSpid value.
		String strAlternativeSpid =  null;
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
		
		// Initilization strNnspDueDate value.
		String strNnspDueDate =  null;
		
		// Initilization strVoiceURI value.
        String strSubDomainId =  null;

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
		
//		 Initilization strPortToOriginal value.
		String strPortToOriginal =  null;
		
		// Changes are made in 5.6.5 release (NANC 441, SimplePort Req.)
		String strNNSPSimplePortIndicator = null;
		String strONSPSimplePortIndicator = null;
		String strLastAlternativeSPID = null;
		// end 5.6.5 changes
		
		//5.7
		String strSpcustom1 = null;
		String strSpcustom2 = null;
		String strSpcustom3 = null;
		
		try {

			sdf = new SimpleDateFormat(strDateFormat);

			// Get the query which needs to execute for the message Key
			String strSearchQry = getSearchQryStmt();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS, "Search query : " + strSearchQry);
			}

			mkQryStatement = dbConn.prepareStatement(strSearchQry);

			mkQryStatement.setString(1, msgKeyValue);

			rs = mkQryStatement.executeQuery();

			// Flag to retrive data from resultset only once
			boolean loopFlg = true;			

			// While ResultSet contains more values.
			while (rs.next()) {

				if (loopFlg) {
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS, "Values retieved once for Message Key = " + msgKeyValue);
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

					// DATA RETRIEVAL FOR BILLINGID
					strBillingId = rs.getString(SOAConstants.BILLINGID_NODE);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS, "Value of BILLINGID = " + strBillingId);
                    }
					if (StringUtils.hasValue(strBillingId)) {

						validColList.add(SOAConstants.BILLINGID_NODE);

						colValMap.put(SOAConstants.BILLINGID_NODE, strBillingId);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR ENDUSERLOCATIONTYPE
					strEndUserLocationType =
						rs.getString(SOAConstants.ENDUSERLOCATIONTYPE_NODE);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
              						"Value of ENDUSERLOCATIONTYPE = " + strEndUserLocationType);
                    }
					if (StringUtils.hasValue(strEndUserLocationType)) {

						validColList.add(SOAConstants.ENDUSERLOCATIONTYPE_NODE);

						colValMap.put(
							SOAConstants.ENDUSERLOCATIONTYPE_NODE,
							strEndUserLocationType);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR ENDUSERLOCATIONVALUE
					strEndUserLocationValue =
						rs.getString(SOAConstants.ENDUSERLOCATIONVALUE_NODE);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of ENDUSERLOCATIONVALUE = " + strEndUserLocationValue);
					}

					if (StringUtils.hasValue(strEndUserLocationValue)) {

						validColList.add(SOAConstants.ENDUSERLOCATIONVALUE_NODE);

						colValMap.put(
							SOAConstants.ENDUSERLOCATIONVALUE_NODE,
							strEndUserLocationValue);

						pendingRequest = true;
					}
					
//					 DATA RETRIEVAL FOR ALTERNATIVEBILLINGID
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
                    
					if (StringUtils.hasValue(strAlternativeEndUserLocationValue)) {

						validColList.add(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL);

						colValMap.put(
							SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL,
							strAlternativeEndUserLocationValue);

						pendingRequest = true;
					}


					// DATA RETRIEVAL FOR CAUSECODE
					strCauseCode = rs.getString(SOAConstants.CAUSECODE_NODE);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS, "Value of CAUSECODE = " + strCauseCode);
					}

					if (StringUtils.hasValue(strCauseCode)) {

						validColList.add(SOAConstants.CAUSECODE_NODE);

						colValMap.put(SOAConstants.CAUSECODE_NODE, strCauseCode);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR CUSTOMERDISCONNECTDATE
					d = rs.getTimestamp(SOAConstants.CUSTOMERDISCONNECTDATE_NODE);

					if (d != null) {
						strCustomerDisconnectDate = sdf.format(d);

					}
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of CUSTOMERDISCONNECTDATE = "
							+ strCustomerDisconnectDate);
					}

					if (strCustomerDisconnectDate != null
						&& !"".equals(strCustomerDisconnectDate)) {

						validColList.add(SOAConstants.CUSTOMERDISCONNECTDATE_NODE);

						colValMap.put(
							SOAConstants.CUSTOMERDISCONNECTDATE_NODE,
							strCustomerDisconnectDate);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR EFFECTIVERELEASEDATE

					d = rs.getTimestamp(SOAConstants.EFFECTIVERELEASEDATE_NODE);

					if (d != null) {
						strEffectiveReleaseDate = sdf.format(d);

					}
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of EFFECTIVERELEASEDATE = " + strEffectiveReleaseDate);
					}

					if (strEffectiveReleaseDate != null
						&& !"".equals(strEffectiveReleaseDate)) {

						validColList.add(SOAConstants.EFFECTIVERELEASEDATE_NODE);

						colValMap.put(
							SOAConstants.EFFECTIVERELEASEDATE_NODE,
							strEffectiveReleaseDate);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR ENDUSERLOCATIONTYPE
					strSvType =
						rs.getString(SOAConstants.SVTYPE_COL);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of SVTYPE = " + strSvType);
					}

					if (StringUtils.hasValue(strSvType)) {

						validColList.add(SOAConstants.SVTYPE_COL);

						colValMap.put(
							SOAConstants.SVTYPE_COL,
							strSvType);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR ENDUSERLOCATIONTYPE
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
//					 DATA RETRIEVAL FOR MMSURI
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
//					 DATA RETRIEVAL FOR PoCURI
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
//					 DATA RETRIEVAL FOR PRESURI
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
					
//					 DATA RETRIEVAL FOR SMSURI
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
	
					// DATA RETRIEVAL FOR PortToOriginal
					strPortToOriginal =
						rs.getString(SOAConstants.PORTINGTOORIGINAL_COL);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of PortToOriginal = " + strPortToOriginal);
					}

					if (StringUtils.hasValue(strPortToOriginal)) {

						validColList.add(SOAConstants.PORTINGTOORIGINAL_COL);

						colValMap.put(
							SOAConstants.PORTINGTOORIGINAL_COL,
							strPortToOriginal);

						pendingRequest = true;
					}					

					// DATA RETRIEVAL FOR NNSPDUEDATE

					d = rs.getTimestamp(SOAConstants.NNSPDUEDATE_COL);
					
					if (d != null){
							strNnspDueDate = sdf.format(d);

						}
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, "Value of NNSPDUEDATE = " 
															+ strNnspDueDate);
					}

					if (strNnspDueDate != null
							&& !"".equals(strNnspDueDate)) 
						{

							validColList.add(SOAConstants.NNSPDUEDATE_COL);
							colValMap.put(
								SOAConstants.NNSPDUEDATE_COL, strNnspDueDate);

							pendingRequest = true;
						}

					// DATA RETRIEVAL FOR subDomain
					strSubDomainId =
						rs.getString(SOAConstants.SUBDOMAIN_COL);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of subDomain = " + strSubDomainId);
					}

					if (StringUtils.hasValue(strSubDomainId)) {

						validColList.add(SOAConstants.SUBDOMAIN_COL);

						colValMap.put(
							SOAConstants.SUBDOMAIN_COL,
							strSubDomainId);

						pendingRequest = true;
					}
				
					// DATA RETRIEVAL FOR NNSP SimplePortIndicator Field
					strNNSPSimplePortIndicator =
						rs.getString(SOAConstants.NNSP_SIMPLEPORTINDICATOR_COL);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of NNSPSimplePortIndicator = " + strNNSPSimplePortIndicator);
					}

					if (StringUtils.hasValue(strNNSPSimplePortIndicator)) {

						validColList.add(SOAConstants.NNSP_SIMPLEPORTINDICATOR_COL);

						colValMap.put(
							SOAConstants.NNSP_SIMPLEPORTINDICATOR_COL,
							strNNSPSimplePortIndicator);

						pendingRequest = true;
					}
					
					// DATA RETRIEVAL FOR ONSP SimplePortIndicator Field
					strONSPSimplePortIndicator =
						rs.getString(SOAConstants.ONSP_SIMPLEPORTINDICATOR_COL);
                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Value of ONSPSimplePortIndicator = " + strONSPSimplePortIndicator);
					}
                 
					if (StringUtils.hasValue(strONSPSimplePortIndicator)) {

						validColList.add(SOAConstants.ONSP_SIMPLEPORTINDICATOR_COL);

						colValMap.put(
							SOAConstants.ONSP_SIMPLEPORTINDICATOR_COL,
							strONSPSimplePortIndicator);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR LASTALTERNATIVESPID
					strLastAlternativeSPID = rs.getString(SOAConstants.LASTALTERNATIVESPID_COL);
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(
							Debug.MSG_STATUS,
							"Value of LastAlternativeSPID = " + strLastAlternativeSPID);
					}
					if (StringUtils.hasValue(strLastAlternativeSPID)) {

						validColList.add(SOAConstants.LASTALTERNATIVESPID_COL);

						colValMap.put(
							SOAConstants.LASTALTERNATIVESPID_COL,
							strLastAlternativeSPID);

						pendingRequest = true;
					}
					
					// DATA RETRIEVAL FOR SPCUSTOM1
					strSpcustom1 = rs.getString(SOAConstants.SPCUSTOM1_COL);
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS, "Value of SPCUSTOM1 = " + strSpcustom1);
					}
					if (StringUtils.hasValue(strSpcustom1)) {

						validColList.add(SOAConstants.SPCUSTOM1_COL);

						colValMap.put(SOAConstants.SPCUSTOM1_COL, strSpcustom1);

						pendingRequest = true;
					}

					// DATA RETRIEVAL FOR SPCUSTOM2
					strSpcustom2 = rs.getString(SOAConstants.SPCUSTOM2_COL);
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS, "Value of SPCUSTOM2 = " + strSpcustom2);
					}
					if (StringUtils.hasValue(strSpcustom2)) {

						validColList.add(SOAConstants.SPCUSTOM2_COL);

						colValMap.put(SOAConstants.SPCUSTOM2_COL, strSpcustom2);

						pendingRequest = true;
					}
					
					// DATA RETRIEVAL FOR SPCUSTOM3
					strSpcustom3 = rs.getString(SOAConstants.SPCUSTOM3_COL);
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS, "Value of SPCUSTOM3 = " + strSpcustom3);
					}
					if (StringUtils.hasValue(strSpcustom3)) {

						validColList.add(SOAConstants.SPCUSTOM3_COL);

						colValMap.put(SOAConstants.SPCUSTOM3_COL, strSpcustom3);

						pendingRequest = true;
					}
					
					
					//Change of flag value to stop retriving data 
					//from resultset for the next iteration.
					loopFlg = false;

				} //end of if(loopFlg)
				
				String strRef = rs.getString(SOAConstants.REFERENCEKEY_COL);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				 Debug.log(Debug.MSG_STATUS, "Value of REFERENCEKEY = " + strRef);
				}

				refList.add(strRef);

			} // end of while (rs.next())	

		} catch (SQLException ex) {
			
			String errMsg =
				"ERROR: SVPendingRequestProcessor: retrieveMsgKeyData() : " +
				" Attempt to query database failed with error: "+ ex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);

		} catch (Exception e) {

			String errMsg =
				"ERROR: SVPendingRequestProcessor: retrieveMsgKeyData() : " +
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
	public String getBatchUpdateStmt(ArrayList columns, int splitValue)
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
				" WHERE " + SOAConstants.REFERENCEKEY_COL + " IN ( ");
				
			for(int i=0;i<splitValue-1;i++){

				updMessage.append(" ? , ");				
				
			}
				
			updMessage.append(" ? ) ");

			updQry = updMessage.toString();
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, " Update Query:\n" + updQry);
			
		} catch (Exception ex) {
			
			String errMsg =
				"ERROR: SVPendingRequestProcessor: getBatchUpdateStmt() : " +
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

			queryMessage.append(SOAConstants.REFERENCEKEY_COL);

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

			queryMessage.append(SOAConstants.BILLINGID_NODE);

			queryMessage.append(", ");

			queryMessage.append(SOAConstants.ENDUSERLOCATIONTYPE_NODE);

			queryMessage.append(", ");

			queryMessage.append(SOAConstants.ENDUSERLOCATIONVALUE_NODE);

			queryMessage.append(", ");

			queryMessage.append(SOAConstants.CAUSECODE_NODE);

			queryMessage.append(", ");

			queryMessage.append(SOAConstants.CUSTOMERDISCONNECTDATE_NODE);

			queryMessage.append(", ");

			queryMessage.append(SOAConstants.EFFECTIVERELEASEDATE_NODE);

			queryMessage.append(", ");

			queryMessage.append(SOAConstants.SVTYPE_COL);

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
			
			queryMessage.append(SOAConstants.PORTINGTOORIGINAL_COL);

			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.NNSPDUEDATE_COL);	
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.SUBDOMAIN_COL);	
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.ALTERNATIVE_BILLINGID_COL);
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONTYPE_COL);
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.ALTERNATIVE_ENDUSERLOCATIONVALUE_COL);
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.SMSURI_COL);			
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.NNSP_SIMPLEPORTINDICATOR_COL);			
			
			queryMessage.append(", ");
			
			queryMessage.append(SOAConstants.ONSP_SIMPLEPORTINDICATOR_COL);
			
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
				"ERROR: SVPendingRequestProcessor: Attempt to generate query statement"
					+ " failed with error: "
					+ ex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// Re-throw the exception to the driver.
			throw new ProcessingException(errMsg);

		} finally {
			
			queryMessage = null;
		}
		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		Debug.log(
			Debug.NORMAL_STATUS,
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

		if (args.length != 3)
		{

			 Debug.log (Debug.ALL_ERRORS, "SVPendingRequestProcessor: USAGE:  "+
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

		SVPendingRequestProcessor msgKeyUpdate = new SVPendingRequestProcessor();


		try {
		
			msgKeyUpdate.initialize("FULL_NEUSTAR_SOA", "SVPendingRequestProcessor");
			
			MessageProcessorContext mpx = new MessageProcessorContext();
			
			MessageObject mob = new MessageObject();

			msgKeyUpdate.process(mpx, mob);
			
			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());
			
			Debug.log(Debug.BENCHMARK, "Process Executed...");


		} catch (ProcessingException pex) {

			String errMsg =
				"ERROR: SVPendingRequestProcessor: ProcessingException Caught in main()	: "
					+ pex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			System.out.println(
				"ERROR: SVPendingRequestProcessor: ProcessingException Caught in main()	: "
					+ pex.getMessage());

		} catch (MessageException mex) {

			String errMsg =
				"ERROR: SVPendingRequestProcessor: MessageException Caught in main()	: "
					+ mex.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			System.out.println(
				"ERROR: SVPendingRequestProcessor: MessageException Caught in main()	: "
					+ mex.getMessage());

		}
		
	} //end of main method
	
}


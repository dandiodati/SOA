package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

public class PopulateNTSvQueryRequest extends MessageProcessorBase {
	
	public static final String SPID_LOC = "SPID_LOC";
	public static final String INVOKEID_LOC = "INVOKEID_LOC";
	public static final String NTPOOL_KEY = "NTPOOL_KEY";
	public static final String MESSAGE_LOCATION = "MESSAGE_LOCATION";
	public static final String DAFAULT_NTUSER_VAL = "DAFAULT_NTUSER_VAL";
	
	private static final String GETNTFEATUREFLAG = "SELECT ALLOWSOANTFLAG FROM SOA_NANC_SUPPORT_FLAGS WHERE SPID=?";
	
	private static final String CHECKFORNTUSER_SQL = "SELECT /*+ index(SOA_SV_MESSAGE SOA_SV_MESSAGE_INDX3) */ CREATEDBY FROM SOA_SV_MESSAGE WHERE MESSAGETYPE  = 'Request' and MESSAGESUBTYPE  = 'SvQueryRequest'  and DATETIME >sysdate -1 and invokeid = ?";
	
	public static final String TARGET_NODE = "query_expression";	
	public static final String TARGET_NODE_VALUE = " AND (((subscription_version_status = 'active' OR subscription_version_status = 'download-failed-partial') OR subscription_version_status "
			+ "= 'download-failed') OR subscription_version_status = 'disconnect-pending')";		
	
	private String spidLoc;
	private String invokeidLoc;
	private String spid;
	private String invokeid;	
	private String inputLoc;	
	private String ntuser;

	/**
	 * Initializes this object via its persistent properties.
	 * 
	 * @param key
	 *            Property-key to use for locating initialization properties.
	 * @param type
	 *            Property-type to use for locating initialization properties.
	 * 
	 * @exception ProcessingException
	 *                when initialization fails
	 */
	public void initialize(String key, String type) throws ProcessingException {

		// Call base class method to load the properties.
		super.initialize(key, type);

		// Get configuration properties specific to this processor.
		if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS,
					"PopulateNTSvQueryRequest: Initializing...");

		StringBuffer errorBuffer = new StringBuffer();
		
		spidLoc = getRequiredPropertyValue(SPID_LOC, errorBuffer);
		ntuser = getRequiredPropertyValue(DAFAULT_NTUSER_VAL, errorBuffer);
		invokeidLoc = getPropertyValue(INVOKEID_LOC);	
		inputLoc = getPropertyValue(MESSAGE_LOCATION);		

		// If any of required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();			

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}

		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG,
					"PopulateNTSvQueryRequest: Initialization done.");
		}
	}

	/**
	 * Extract data values from the context/input, and use them to insert a row
	 * into the configured database table.
	 * 
	 * @param mpContext
	 *            The context
	 * @param inputObject
	 *            Input message to process.
	 * 
	 * @return The given input, or null.
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 * @exception MessageException
	 *                Thrown if message is bad.
	 */
	public NVPair[] process(MessageProcessorContext mpContext,
			MessageObject inputObject) throws MessageException,
			ProcessingException {

		ThreadMonitor.ThreadInfo tmti = null;
		Connection dbConn = null;		
		PreparedStatement pstmt = null;
		PreparedStatement pstmt1 = null;	
		ResultSet rset = null;
		ResultSet rset1 = null;		
		String spidFlag = null;						

		if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, this.getClass()
					+ " PopulateNTSvQueryRequest : Inside process method");

		if (inputObject == null) {

			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, this.getClass() + " obj is null");
			return null;
		}

		try {
			tmti = ThreadMonitor.start("Message-processor [" + getName()
					+ "] started processing the request");

			spid = getString(spidLoc, mpContext, inputObject);
			invokeid = getString(invokeidLoc, mpContext, inputObject);
									
			dbConn = DBConnectionPool.getInstance().acquireConnection();

			pstmt = dbConn.prepareStatement(GETNTFEATUREFLAG);
			pstmt.setString(1, spid);
			rset = pstmt.executeQuery();

			if (rset.next()) {

				spidFlag = rset.getString("ALLOWSOANTFLAG");
				if (spidFlag != null && "1".equals(spidFlag)) {

					pstmt1 = dbConn.prepareStatement(CHECKFORNTUSER_SQL);
					pstmt1.setString(1, invokeid);
					rset1 = pstmt1.executeQuery();

					if (rset1.next()) {
						
						String user = rset1.getString("CREATEDBY");
						if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
							Debug.log(Debug.NORMAL_STATUS,
									"NPAC Query reqeust User for this InvokeID is [ "
											+ user + "]");
						}

						if (user.equals(ntuser)) {

							if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
								Debug.log(Debug.NORMAL_STATUS,"This is NT NPAC query Request , so populating Default NT tn status criteria in NPAC XML");	
							
							Document npacXML = getDOM(inputLoc, mpContext,inputObject);
							NodeList nl = npacXML.getElementsByTagName(TARGET_NODE);
							
							if (nl.getLength() > 0) {
								StringBuffer sbuf = new StringBuffer();
								String queryExpVal = nl.item(0).getTextContent();		
													
								sbuf.append(queryExpVal.substring(0,
										queryExpVal.indexOf(")") - 1));
								sbuf.append(TARGET_NODE_VALUE);
								sbuf.append("))");
								
								nl.item(0).setTextContent(sbuf.toString());								

								set(inputLoc, mpContext, inputObject, npacXML);

							} else {
								if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
									Debug
									.log(Debug.NORMAL_STATUS,
											"Target node does not exist in NPAC XML");
								}
							}
						}

					} else {

						if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
							Debug
									.log(Debug.NORMAL_STATUS,
											"NPAC Query does not exist in SOA database, Skip processing");
						}
					}
				} else {
					// SPID does not support NT.
					// return from here.ALLOWSOANTFLAG
					if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
						Debug.log(Debug.NORMAL_STATUS,
								"Falg ALLOWSOANTFLAG is turned OFF for SPID["
										+ spid + "] , Skip Processing");
					}
				}
			} else {

				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
					Debug.log(
							Debug.NORMAL_STATUS,
							"SPID["
									+ spid
									+ "] entry does not exist in SOA_NANC_SUPPORT_FLAGS table, skip processing");
				}
			}
			

		} catch (Exception e) {
			if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
				Debug.log(Debug.ALL_ERRORS,
						" Could not populate active like status in sv query request." + e.getMessage());
			}
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
					pstmt = null;
				}
				if (pstmt1 != null) {
					pstmt1.close();
					pstmt1 = null;
				}
				if (rset != null) {
					rset.close();
					rset = null;
				}
				if (rset1 != null) {
					rset1.close();
					rset1 = null;
				}				

			} catch (SQLException e) {
				Debug.log(Debug.ALL_ERRORS, e.toString());
			}
			try {
				if (dbConn != null) {
					DBConnectionPool.getInstance().releaseConnection(dbConn);
				}				
			} catch (ResourceException e) {
				Debug.log(Debug.ALL_ERRORS, e.toString());
			} 

			ThreadMonitor.stop(tmti);
		}

		return (formatNVPair(inputObject));
	}

	public static void main(String args[]) {
		try {
			String inputstr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAMessage xmlns=\"urn:neustar:lnp:soa:1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><messageHeader><session_id/><invoke_id>1211597</invoke_id><customer_id>A111</customer_id><message_date_time>2012-04-10T14:45:00Z</message_date_time></messageHeader><messageContent><SOAtoNPAC><SubscriptionVersionQueryRequest><query_expression>((subscription_version_tn = '3055917019' ) AND ( subscription_version_status = 'active' ))</query_expression></SubscriptionVersionQueryRequest></SOAtoNPAC></messageContent></SOAMessage>";
			MessageProcessorContext mpc = new MessageProcessorContext();
			MessageObject input = new MessageObject(inputstr);

			PopulateNTSvQueryRequest ps = new PopulateNTSvQueryRequest();
			System.out.println("here1");
			ps.process(mpc, input);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
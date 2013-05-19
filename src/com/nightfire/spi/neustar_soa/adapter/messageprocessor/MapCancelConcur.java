package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.monitor.ThreadMonitor;

public class MapCancelConcur extends MessageProcessorBase {	
	
	
	private static final String REQUEST_TYPE_LOC = "REQUEST_TYPE_LOC";
	
	public static final String GETSVDATAQUERY = "SELECT /*+ index( SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ STATUS,NNSP,ONSP FROM SOA_SUBSCRIPTION_VERSION " +
			"WHERE  SPID = ? AND SVID = ? AND REGIONID = ?";
	/**
	 * 	This variable used to get location of Region ID.
	 */
	private String regionIdLoc = null;
	
	/**
	 * 	This variable used to get value of Region ID.
	 */
	private String regionId = null;
			

	/**
	 * 	This variable used to get value for SVID.
	 */
	private String inputLocSvId = null;
	
	/**
	 * The value of REQUEST_TYPE
	 */
	private String requestType = null;
	
	/**
	 * The value of SPID
	 */
	private String spidValue = null;
	
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
	public MapCancelConcur() {

		Debug.log(
			Debug.OBJECT_LIFECYCLE,
			"Creating MapCancelConcur message-processor.");

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
		   Debug.log(Debug.SYSTEM_CONFIG, "MapCancelConcur: Initializing...");
		}

		StringBuffer errorBuffer = new StringBuffer();
		        
		inputLocSvId = getPropertyValue(SOAConstants.INPUT_LOC_SVID_PROP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of SVID is [" + inputLocSvId + "].");
		}

		requestType = getPropertyValue(REQUEST_TYPE_LOC);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of requestType is [" + requestType + "].");
		}
				
		spidValue = getRequiredPropertyValue(SOAConstants.SPID_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location of SPID is [" + spidValue + "].");
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
		  Debug.log(Debug.SYSTEM_CONFIG, "MapCancelConcur: Initialization done.");
		}
	} //end of initialize method

	/**	
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
		
		Connection dbConn = null;
		PreparedStatement pstmt = null;		
		ResultSet rset = null;
		
		Document dom =null;			
		
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
 		  Debug.log(Debug.MSG_STATUS, "MapCancelConcur: processing ... ");
		}

		// Get the value of the property from the context or inputObject.

		// Get the Input Location SvId value from context
		inputLocSvId =  getValue(inputLocSvId);
		if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of Input Location SvId is ["
											+ inputLocSvId + "]." );
		}
		
		// Get the SPID value from context
		spidValue =  getValue(spidValue);
		if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is ["
										+ spidValue + "]." );
		}
		
		// Get the RegionId Location value from context
		regionId = getValue( regionIdLoc );
		if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of REGIONID is ["
										+ regionId + "]." );
		}		
						
		try {
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] getting Subscription Version" );
			dbConn = DBConnectionPool.getInstance(true).acquireConnection();
			pstmt = dbConn.prepareStatement(GETSVDATAQUERY);
			pstmt.setString(1, spidValue);
			pstmt.setString(2, inputLocSvId);
			pstmt.setString(3, regionId);
			rset = pstmt.executeQuery();
			String status = null;
			String nnsp =null;
			String onsp = null;
			if (rset.next()) {			
				
				status = rset.getString("STATUS");
				nnsp = rset.getString("NNSP");
				onsp = rset.getString("ONSP");				
			}
						
			 if (status!= null && status.equals("cancel-pending"))
			{				 
				 String reqTypeNode = null;				 
				 if(spidValue.equals(onsp))
					 reqTypeNode = "SvCancelAckAsOldRequest";
				 else if (spidValue.equals(nnsp))
					 reqTypeNode = "SvCancelAckAsNewRequest";				 
				 
				 if(reqTypeNode!=null)
				 {					 							 
					 if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
							Debug.log( Debug.SYSTEM_CONFIG, "Converting Cancel Request to ["
								+ reqTypeNode + "]." );								
						}
					 dom = inputObject.getDOM();
					 NodeList nodes = dom.getElementsByTagName(SOAConstants.SV_CANCEL_REQUEST);						 					
					 dom.renameNode(nodes.item(0), null, reqTypeNode); 					 
					 inputObject.set(dom); 
					 
					 super.set(requestType, mpContext,inputObject, reqTypeNode );
				 }				 
			}						 
			
		} catch (Exception e) {
			if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
				Debug.log(Debug.ALL_ERRORS,
						" Could not Handle Cancel Concur mapping scenario." + e.getMessage());
			}
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
					pstmt = null;
				}				
				if (rset != null) {
					rset.close();
					rset = null;
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

	
}

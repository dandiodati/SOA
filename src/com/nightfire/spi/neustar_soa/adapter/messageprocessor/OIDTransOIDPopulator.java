/**
 * $Header: //spi/neustar_soa/adapter/messageprocessor/OIDTransOIDPopulator.java#1 $
 */
package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * 
 * This class is used to populate the <Tn> , <OID> and <TransOID> nodes in Header part 
 * part of the response XML.
 * 
 * Added the support of sending SubRequest value node in Header Part in SOA 5.8.3 release
 *
 */
public class OIDTransOIDPopulator extends MessageProcessorBase{
	
	/*
	 * Property indicating the node name of InvokeId.
	 */
	public static final String INVOKEID = "INVOKEID";
	
	/*
	 * Property indicating the node name of RESPONSE.
	*/
	public static final String RESPONSE_TYPE = "RESPONSE";
	
	/*
	 * Property indicating the node name of SKIP_POPULATION.
	*/
	public static final String SKIP_POPULATION = "SKIP_POPULATION";
	
	/*
	 * Property indicating the node name of SPID
	 */
	public static final String SPID_LOCATION = "SPID_LOCATION";
	
	private String invokeIdProp = null;
	private String responseProp = null;
	private String skip_responseTypeValue = null;
	private String invokeId = null;
	private String response = null;
	private String spidProp = null;
	private String spid = null;
	
	private HashSet<String> oidSet = new HashSet<String>();
	private HashSet<String> transOidSet = new HashSet<String>();
	
	
	 public static final String GET_TN_OID_TransOID_LIST = "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION XPK_SOA_SUBSCRIPTION_VERSION) */" +
	 	" PORTINGTN, OID ,TransOID, LASTREQUESTTYPE FROM SOA_SUBSCRIPTION_VERSION WHERE REFERENCEKEY IN (" + 
		" SELECT /*+ INDEX(SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_1 ) */ REFERENCEKEY FROM SOA_MESSAGE_MAP WHERE MESSAGEKEY IN " + 
		" (SELECT /*+ INDEX(SOA_SV_MESSAGE SOA_SV_MESSAGE_INDX3 ) */ MESSAGEKEY FROM SOA_SV_MESSAGE WHERE INVOKEID = ?)) ORDER BY PORTINGTN ASC ";
	 
	 public static final String GET_BPELSPID_FLAG = "SELECT BPELSPIDFLAG from SOA_NANC_SUPPORT_FLAGS where SPID=?";
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
		 Debug.log(Debug.MSG_STATUS,": In OIDTransOIDPopulator initialize() method.");
		}
		
		super.initialize(key, type);

		//this is used to store error messages.
		StringBuffer errorBuffer = new StringBuffer();
		
		invokeIdProp = getRequiredPropertyValue(INVOKEID, errorBuffer);
		responseProp = getRequiredPropertyValue(RESPONSE_TYPE, errorBuffer);
		spidProp = getRequiredPropertyValue(SPID_LOCATION, errorBuffer);
		skip_responseTypeValue = getPropertyValue(SKIP_POPULATION);
		
		if(errorBuffer.length() > 0){
			String errMsg = errorBuffer.toString( );
            
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
            	Debug.log( Debug.ALL_ERRORS, " error occured in initialize method"+errMsg );
            }
            throw new ProcessingException( errMsg );
		}
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, ": Initialization done....");
		}
	}
	
	/**
	 * Extract data values of <Tn> , <OID> and <TransOID> from the SOA_SUBSCRIPTION_VERSION TABLE and 
	 * populate in the header part of the response XML.
	 *
	 * @param  ctx The context
	 * @param  obj Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	
	public NVPair[] process(MessageProcessorContext ctx,
			MessageObject obj) throws MessageException,
			ProcessingException {
		ThreadMonitor.ThreadInfo tmti = null;
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
		 Debug.log(Debug.MSG_STATUS, " Inside process method");
		}
		
		Connection dbConn = null;
		
		if (obj == null) {
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			  Debug.log(Debug.MSG_STATUS," obj is null");
			}
			return null;
		}
		
	   try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			invokeId = getString(invokeIdProp, ctx, obj);
			response = getString(responseProp, ctx, obj);
			spid = getString(spidProp, ctx, obj);
			
			String skip_resType [] ;
			ArrayList<String> resTypeList = new ArrayList<String>();
			
			if(skip_responseTypeValue != null){
				
				if(skip_responseTypeValue.indexOf("|") != -1)
				{	
					skip_responseTypeValue = skip_responseTypeValue.replace('|', ',');
					skip_resType = skip_responseTypeValue.split(",");
		
				}else{
					skip_resType = new String[1];
					skip_resType[0] = skip_responseTypeValue.toString();						
				}
				
				//Adding Array time in to list.
				for (int i = 0; i < skip_resType.length; i++) {
					
					resTypeList.add(skip_resType[i]);
					
				}
			}
			/*
			 * Populate the OID, TransOID and TN in the response xml based on the invokeId.
			 */
			try {
				if(!resTypeList.contains(response)){
					//Get the db connection.
					dbConn = DBConnectionPool.getInstance(true).acquireConnection();
					if (dbConn == null) {
						// Throw the exception to the driver.
						throw new ProcessingException(
								"DB connection is not available");
					}		
					ArrayList<TnOIDTransOID> tnOidToIDlst = getTNOIDTransOIDList(dbConn, invokeId);
					
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						 Debug.log(Debug.MSG_STATUS," Size of ArrayList: "+tnOidToIDlst.size());
					}
					boolean isRange = false;
					if (oidSet.size() == 1 && transOidSet.size() == 1 && tnOidToIDlst.size() > 1){
						isRange = true;
					}
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS," Size of ArrayList: "+tnOidToIDlst.size());
					}
					//Get the BPELSPIDFLAG value for response SPID.
					String supportFlag = getSupportFlag(dbConn, spid);
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(Debug.MSG_STATUS, "BPELSPID flag value for SPID["+spid+"] is ["+supportFlag+"]");
					}
					//populating TN, OId and transOID in response XML.
					//Also populate SubRequest in NPAC success/failure reply XML.
					populateOIDTransOID(tnOidToIDlst,isRange, obj, supportFlag );

				}		
			} catch (Exception e) {
				if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) ){
				  Debug.log(Debug.ALL_ERRORS, "e.printStackTrace()" + e.getMessage());
				}
			}finally{
				if(dbConn != null){
					try {
						DBConnectionPool.getInstance(true)
								.releaseConnection(dbConn);
						dbConn = null;
					} catch (ResourceException e) {
						Debug.log(Debug.ALL_ERRORS, e.toString());
					}
				}
			}
		}finally {
        	ThreadMonitor.stop(tmti);
        }
		return (formatNVPair(obj));
	}
	

	private String getSupportFlag(Connection dbConn, String spid2) throws ProcessingException{
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String bpelFlag = null;
		
		try{
			pstmt = dbConn.prepareStatement(GET_BPELSPID_FLAG);
			pstmt.setString(1, spid);
			rs = pstmt.executeQuery();
			
			while (rs.next()){
				bpelFlag = rs.getString("BPELSPIDFLAG");
			}
		}
		catch(SQLException e){
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
				Debug.log(Debug.ALL_ERRORS, "Error occured while fetching BPELSPIDFLAG value for SPID["+spid+"]");
				Debug.log(Debug.ALL_ERRORS, "Error:"+e.getStackTrace());
			}
		}
		finally{
			try{
				if( rs != null ){
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	  Debug.log(Debug.SYSTEM_CONFIG," Resultset was not null");
					}
					rs.close();
                }else{
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	   Debug.log(Debug.SYSTEM_CONFIG, " Resultset is null");
					}
                }
				if( pstmt != null ){
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	  Debug.log(Debug.SYSTEM_CONFIG," Prepared Statement was not null");
					}
					pstmt.close();
                }else{
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	   Debug.log(Debug.SYSTEM_CONFIG, " Prepared Statement is null");
					}
                }
			}catch(SQLException e){
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
	                   Debug.log(Debug.SYSTEM_CONFIG, e.getMessage());
					}
			}
		}
		return bpelFlag;
	}

	/**
	 * This method retrieves <Tn> , <OID> and <TransOID> from SOA_SUBSCRIPTION_VERSION TABLE on the basis of invokeId and 
	 * encapsulate these to TnOIDTransOID object and add in array list and return as a list.
	 * @param dbConn
	 * @param invokeId
	 * @return
	 * @throws MessageException
	 */
	private ArrayList<TnOIDTransOID> getTNOIDTransOIDList(Connection dbConn, String invokeId ) throws MessageException {
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
		  Debug.log(Debug.MSG_STATUS," Inside the getoIdtransoid method");
		}
		
		ArrayList<TnOIDTransOID> oidTransOIDList = new ArrayList<TnOIDTransOID>();
		ResultSet rSet = null;
		PreparedStatement svObjectSelect = null;
		TnOIDTransOID toid = null;
		
		try {
			svObjectSelect = dbConn.prepareStatement(GET_TN_OID_TransOID_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			svObjectSelect.setString(1, invokeId);
			
			if(Debug.isLevelEnabled(Debug.DB_DATA)){
				Debug.log(Debug.DB_DATA, "Query:"+ svObjectSelect.toString() + 
						", and Parameter InvokeId is:[" +invokeId+"]");
			}
			
			rSet = svObjectSelect.executeQuery();
			
			while(rSet.next()){
				
				toid = new TnOIDTransOID(rSet.getString(SOAConstants.PORTINGTN_COL), rSet.getString(SOAConstants.OID_COL) 
						, rSet.getString(SOAConstants.TransOID_COL), rSet.getString(SOAConstants.LASTREQUESTTYPE_COL));
				
				oidSet.add(rSet.getString(SOAConstants.OID_COL));
				transOidSet.add(rSet.getString(SOAConstants.TransOID_COL));
				
				oidTransOIDList.add(toid);
			}			
		}catch (SQLException sqlex) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG," SQL Exception:" + sqlex.getMessage());
			}
			
		} catch (FrameworkException e) {
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(Debug.SYSTEM_CONFIG, " Could not create TnOIDtrnasOID data, Exception:" + e.getMessage());
			}
		}finally{
			try{
                if( rSet != null ){
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	  Debug.log(Debug.SYSTEM_CONFIG," Resultset was not null");
					}
                	rSet.close();
                }else{
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	   Debug.log(Debug.SYSTEM_CONFIG, " Resultset is null");
					}
                }                
                if( svObjectSelect != null ){
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                	   Debug.log(Debug.SYSTEM_CONFIG," Prepared Statement was not null");
					}
                	svObjectSelect.close();
				}else{
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
	            	   Debug.log(Debug.SYSTEM_CONFIG, " Prepared statement is null");
					}
	            }
            }
            catch(SQLException e){
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                   Debug.log(Debug.SYSTEM_CONFIG, e.getMessage());
				}
            }
		}
		
		return oidTransOIDList;
	}
	
	/**
	 * This method populated Tn , OID and TransOID in response XML.  
	 * @param transOidTnlst
	 * @param isRange
	 * @param mobj
	 * @throws MessageException
	 * @throws ProcessingException
	 */
	private void populateOIDTransOID(List<TnOIDTransOID> transOidTnlst, boolean isRange, MessageObject mobj, String flagValue) throws MessageException, ProcessingException {
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
		  Debug.log(Debug.MSG_STATUS, " Inside the populateOIDTransOID method");
		}
		
		Document dom = mobj.getDOM();
		Iterator iter = transOidTnlst.iterator();
		Element ele = null;
		String startTN = null;
		String endTN = null;
		boolean lastRequestTypeFlag = false;
		
		if(isRange){
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			   Debug.log(Debug.MSG_STATUS," Inside Range codition");
			}
			
			String [] Tnlst = new String[transOidTnlst.size()];
			int i=0;
			Iterator iter_tmp = transOidTnlst.iterator();
			
			while(iter_tmp.hasNext()){
				TnOIDTransOID tnOiDTransOid1 = (TnOIDTransOID)iter_tmp.next();
				Tnlst[i] = tnOiDTransOid1.tn.toString();
				i++;
			}
			
			startTN = Tnlst[0];
			endTN = Tnlst[Tnlst.length-1];
			StringBuffer tn = new StringBuffer( startTN );
			
			tn.append( SOAConstants.DEFAULT_DELIMITER );
			tn.append( endTN.substring(8 , endTN.length()) );
			
			TnOIDTransOID tnOiDTransOid = (TnOIDTransOID)iter.next();
			
			Element xmlElement_Tn = dom.createElement(SOAConstants.TN_NODE);
			xmlElement_Tn.setAttribute("value", tn.toString());
			
			if( tnOiDTransOid.oid  != null){ 
				
				Element xmlElement_OID = dom.createElement(SOAConstants.OID);
				
				xmlElement_OID.setAttribute("value",tnOiDTransOid.oid.toString());
				xmlElement_Tn.appendChild(xmlElement_OID);
			}
			if ( tnOiDTransOid.transOid != null){
				
				Element xmlElement_TransOID = dom.createElement(SOAConstants.TransOID);
				
				xmlElement_TransOID.setAttribute("value",tnOiDTransOid.transOid.toString());
				xmlElement_Tn.appendChild(xmlElement_TransOID);
			}			
			if(flagValue != null && "1".equals(flagValue)){
				if(tnOiDTransOid.lastRequestType != null){				
					
					Element xmlElement_LastRequestType = dom.createElement(SOAConstants.LastRequest);
					xmlElement_LastRequestType.setAttribute("value",tnOiDTransOid.lastRequestType.toString());
					xmlElement_Tn.appendChild(xmlElement_LastRequestType);
					
					lastRequestTypeFlag = true;
				}
			}
			
			if(tnOiDTransOid.oid != null || tnOiDTransOid.transOid != null || lastRequestTypeFlag){
				ele = (Element) dom.getElementsByTagName("SOAToUpstreamHeader").item(0);
				ele.appendChild(xmlElement_Tn);
				
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){ 
					Debug.log(Debug.MSG_STATUS, "populated TN node in NPAC Reply XML");
				}
			}else{
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){ 
					Debug.log(Debug.MSG_STATUS, "OID, TranOID and LastRequestType fields are null. " +
							"Hence TN container with OID, TransOID & SubRequest nodes is not " +
							"populated in NPAC Reply XML");
				}
			}
		}else{
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				   Debug.log(Debug.MSG_STATUS," Inside single TN codition");
			}
			
			while(iter.hasNext()){
			
				TnOIDTransOID tnOiDTransOid = (TnOIDTransOID)iter.next();
				
				Element xmlElement_Tn = dom.createElement(SOAConstants.TN_NODE);
				xmlElement_Tn.setAttribute("value", tnOiDTransOid.tn.toString());
				
				if( tnOiDTransOid.oid  != null){ 
				    if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					   Debug.log(Debug.MSG_STATUS, " Inside the oid in not null");
					}
					Element xmlElement_OID = dom.createElement(SOAConstants.OID);
					
					xmlElement_OID.setAttribute("value",tnOiDTransOid.oid.toString());
					xmlElement_Tn.appendChild(xmlElement_OID);
				}
				if ( tnOiDTransOid.transOid != null){
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					  Debug.log(Debug.MSG_STATUS, " Inside the transoid in not null");
					}
					
					Element xmlElement_TransOID = dom.createElement(SOAConstants.TransOID);
					xmlElement_TransOID.setAttribute("value",tnOiDTransOid.transOid.toString());
					xmlElement_Tn.appendChild(xmlElement_TransOID);
				}
				if(flagValue != null && "1".equals(flagValue)){
					if(tnOiDTransOid.lastRequestType != null){
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							  Debug.log(Debug.MSG_STATUS, " Inside the lastRequestType in not null");
						}	
						Element xmlElement_LastRequestType = dom.createElement(SOAConstants.LastRequest);					
						xmlElement_LastRequestType.setAttribute("value",tnOiDTransOid.lastRequestType.toString());
						xmlElement_Tn.appendChild(xmlElement_LastRequestType);
						lastRequestTypeFlag = true;
					}
				}				
				if(tnOiDTransOid.oid != null || tnOiDTransOid.transOid != null || lastRequestTypeFlag){
					ele = (Element) dom.getElementsByTagName("SOAToUpstreamHeader").item(0);
					ele.appendChild(xmlElement_Tn);
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){ 
						Debug.log(Debug.MSG_STATUS, "Populated TN node in NPAC Reply XML");
					}
				}else{
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){ 
						Debug.log(Debug.MSG_STATUS, "OID, TranOID and LastRequestType fields are null. " +
								"Hence TN container with OID, TransOID & SubRequest nodes is not " +
								"populated in NPAC Reply XML");
					}
				}
			}
		}
		mobj.set(dom);
	}
	
	/**
	 * TnOIDTransOID class is used to encapsulate the <Tn> , <OID> and <TransOID> data 
	 * in to a object.
	 *
	 */
	private static class TnOIDTransOID {

        public final String tn;
        public final String oid;
        public final String transOid; 
        public final String lastRequestType;
        public TnOIDTransOID(String tn, String oid , String transOid, String lastRequestType) throws FrameworkException {

            this.tn = tn;
            this.oid = oid;
            this.transOid = transOid;
            this.lastRequestType = lastRequestType;
        }
    }
	
	public static void main(String[] args) {

		/* Initialize Debug logging. */
		Hashtable htLogTable = new Hashtable();
		htLogTable.put(Debug.DEBUG_LOG_LEVELS_PROP, "ALL");
		htLogTable.put(Debug.LOG_FILE_NAME_PROP,
				"c:/OIDTransOIDPopulator.log");
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
					"jdbc:oracle:thin:@192.168.148.34:1521:NOIDADB","MOHITSOA", "MOHITSOA");
			Debug.log(Debug.MSG_STATUS,
							"OIDTransOIDPopulator Database Initalization done ");
		} catch (DatabaseException e) {
			Debug.log(Debug.MAPPING_ERROR, "Database initialization failure: "
					+ e.getMessage());
		}

		OIDTransOIDPopulator reqObj = new OIDTransOIDPopulator();

		try {
			
			MessageProcessorContext mpx = new MessageProcessorContext();
			
			mpx.set("ResponseSPID", "A111");	//@context.LRNValue
			mpx.set("Response", "SvCreateReply");		//@context.SPIDValue
			mpx.set("Notification", "praveen");				//@context.SubRequestType
			mpx.set("InvokeId", "1151391");
			mpx.set("SKIP_POPULATION", "SvQueryReply");
			String InputMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAMessage><SOAToUpstream><SOAToUpstreamHeader>" +
					"<DateSent value=\"09-17-2008-081448PM\"/><RegionId value=\"0003\"/></SOAToUpstreamHeader><SOAToUpstreamBody>" +
							"<SvQueryReply><RequestId value=\"1151391\"/></SvQueryReply>" +
									"</SOAToUpstreamBody></SOAToUpstream></SOAMessage>"; 
		
			MessageObject msob = new MessageObject(InputMessage);
			reqObj.initialize("FULL_NEUSTAR_SOA_RESPONSE", "OIDTransOIDPopulator");
			Debug.log(Debug.MSG_STATUS,
			"before process ");
			
			reqObj.process(mpx, msob);
			

		} catch (Exception ex) {
			Debug.log(Debug.MSG_STATUS,
			"Exception : " +ex.getMessage());
		}

	}	
}
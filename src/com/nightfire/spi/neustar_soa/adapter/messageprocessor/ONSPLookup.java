package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NFConstants;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.portps.PortingRequestType;
import com.nightfire.spi.neustar_soa.portps.PortingResponseType;
import com.nightfire.spi.neustar_soa.portps.PortpsServiceBindingStub;
import com.nightfire.spi.neustar_soa.portps.PortpsServiceLocator;
import com.nightfire.spi.neustar_soa.portps.TNResponseType;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAConfiguredSPIDCache;

public class ONSPLookup extends MessageProcessorBase{
	
	/*
	 * Input Properties of this message-processor.
	 */
	public static final String LOOKUP_PROPFILE_PATH = "LOOKUP_PROPFILE_PATH";
	private static final String SPID_LOC = "SPID_LOC";
	public static final String SUBREQUESTTYPE_NODE = "SUBREQUEST_TYPE_LOC";
	public static final String LNP_TYPE = "LnpType";	
				
	//private variables
		
	private String ntsDBInfoPropPath;
	private String tnValue;
	private MessageProcessorContext mpContext = null;
	private String spidPropValue;
	private String spidValue;
	private String subRequestNode;
	private String subRequestValue;
	private MessageObject inputObject = null;
	private static int OMS_CUSTID;
	private static String ntsDB_pkg_name = null;
	private static int defLookupTimeout = 5;
	private static int defRetryCount = 1;
	private static int ntsLookupTimeout;
	private static int ntsLookupRetryCount;
	private static String portpsURL = null;
	private static String portpsUserName = null;
	private static String portpsPassword = null;
	private static int portpsLookupTimeout;
	private static int portpsLookupRetryCount;
	public static final String GET_HISTORY = "NO";

	private static boolean isDbPoolInt = false;

	
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
		  Debug.log(Debug.MSG_STATUS, ": ONSPLookup Initialing...");
		}
		StringBuffer sbuff = new StringBuffer();			
		spidPropValue = getRequiredPropertyValue(SPID_LOC, sbuff);
		subRequestNode = getRequiredPropertyValue(SUBREQUESTTYPE_NODE, sbuff);
		ntsDBInfoPropPath = getRequiredPropertyValue(LOOKUP_PROPFILE_PATH, sbuff);		
		// Throw error if any of the required properties are missing
		if (sbuff.length() > 0) {
			throw new ProcessingException(sbuff.toString());			
		}
		
		//Configure NTS DB Pool & portPS properties
		//configureONSPLookup(ntsDBInfoPropPath);
		
		if(!isDbPoolInt){
			synchronized (ONSPLookup.class) {
				if(!isDbPoolInt){
					try{
						//Configure NTS DB Pool
						configureONSPLookup(ntsDBInfoPropPath);						
						//Set the DB pool initialize variable to true.
						//So that other running threads could not initialize NTS DB pool again.
						isDbPoolInt = true;
					}
					catch(Exception ex){
						Debug.log(Debug.ALL_ERRORS, "Could not initialize NTS DB pool.");
						throw new ProcessingException(ex.getMessage());
					}									
				}
			}
		}
		
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, ": ONSPLookup Initialing done...");
		}
	}

	/**
	 * Extract data values from the context/input, and use them to
	 * fetch configured ONSP from NTS database.
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
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "LookupNTSONSP:" + " input obj is Null");
			
			return null;
		}		
		this.mpContext = mpContext;		
		this.inputObject = inputObject;				
		spidValue = getValue(spidPropValue);
		subRequestValue = getValue(subRequestNode);
		String onspValue = null; 
		String lnpVal =null;
		try{
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			if (!StringUtils.hasValue(spidValue)  || !StringUtils.hasValue(subRequestValue))
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS, "SubrequestType  or SPID value is not present , so skip processing");
					}
			}
			else
			{	
				NodeList nodelnp = inputObject.getDOM().getElementsByTagName(LNP_TYPE);				
				if(nodelnp.getLength()>0){
					lnpVal = nodelnp.item(0).getAttributes().item(0).getTextContent();
				}
				
				NodeList nodeTN = inputObject.getDOM().getElementsByTagName(SOAConstants.TN_NODE);
				if (nodeTN.getLength()>0){
					tnValue = nodeTN.item(0).getAttributes().item(0).getTextContent();
				}
				
				if (StringUtils.hasValue(lnpVal)&&  StringUtils.hasValue(tnValue))
				{
					if (subRequestValue.equals(SOAConstants.SV_CREATE_REQUEST) && lnpVal.equals("lspp"))
					{
						String startTN =null;						
						if (tnValue.length() == 12){
							startTN =tnValue;							
						}
						else if(tnValue.length() > 12){
							startTN = tnValue.substring(0,12);
						}
						else{
						
							if(Debug.isLevelEnabled(Debug.MSG_STATUS))
						    	Debug.log(Debug.MSG_STATUS, "The TN format is wrong, so skip the processing" +
						    			"["+tnValue+"]");
							
							return (formatNVPair(inputObject));
						}
						
						if(Debug.isLevelEnabled(Debug.MSG_STATUS))
					    	Debug.log(Debug.MSG_STATUS, "Start PORTINGTN value is ["+startTN+"]");
						
						/*
						 * check for the ONSP support flag for a SPID
						 */									
						String onspFlag = String.valueOf(NANCSupportFlag.getInstance(
							spidValue).getOnspFlag());
						
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							Debug.log(Debug.MSG_STATUS, ": ONSP flag for spid ["+ spidValue	+ "] is: [" + onspFlag + "]");
						}
						
						Document dom =null;	
						dom = inputObject.getDOM();	
						NodeList oldSPNode = dom.getElementsByTagName(SOAConstants.OLD_SP_NODE);
						XMLMessageGenerator xmg = new XMLMessageGenerator(dom);
						
						if (onspFlag!=null && "1".equals(onspFlag))
						{
							if ((oldSPNode.getLength() == 0) || (oldSPNode.getLength() > 0 && 
										"0000".equals(oldSPNode.item(0).getAttributes().item(0).getTextContent())))
							{	
								long start =System.currentTimeMillis();
								
								Debug.log(0, ": Fetching ONSP from PortPS Web-service for PortingTN [" + startTN + "] for SPID ["+ spidValue + "] for Create Port-In request");								
								
								onspValue = getPortPSONSP (startTN);
																
								Debug.log(0, ": Fetched ONSP value from PortPS Web-service in 1st attempt :[" + onspValue + "] : Elapsed Time [" +(System.currentTimeMillis() -start) +"]");
								
								/* ONSP lookup retry starts here */
								if (onspValue == null) // PortPS lookup fails 1st time, means onspValue is null
								{
									
									Debug.log(0, ": Retrying [" + portpsLookupRetryCount + "] time to fetch ONSP from PortPS Web-service :");
									
									for(int i = 0 ;i < portpsLookupRetryCount; i++)
									{
										onspValue = getPortPSONSP (startTN);
										if (onspValue!=null)
										{											
											Debug.log(0, ": Fetched ONSP from Port-PS web service in [" + (i +1) + "] attempt : Elapsed Time [" +(System.currentTimeMillis() -start) +"]");											
											break;
										}
										
									}	
									if (onspValue == null) // PortPS lookup failed , lookup from NTS DB
									{
										Connection dbCon = null;
										
										Debug.log(0, ": Port-PS Lookup Failed, so fetching ONSP from NTS DB :");										
										
										long startNTS =System.currentTimeMillis();
										
										/* NTS lookup starts here */
										try{
											
											DBConnectionPool.setThreadSpecificPoolKey(SOAConstants.NTS_POOL_ID);																					
											
											dbCon = DBConnectionPool.getInstance(SOAConstants.NTS_POOL_ID).acquireConnection();
											
											onspValue = getNTSData(startTN,dbCon);
																					
											Debug.log(0, ": Fetched ONSP value from NTS in 1st attempt :[" + onspValue + "] : Elapsed Time [" +(System.currentTimeMillis() -startNTS) +"]");										
																				
											if (onspValue == null)
											{
												for(int i = 0 ;i < ntsLookupRetryCount; i++)
												{
													onspValue = getNTSData (startTN,dbCon);
													if (onspValue!=null)
													{													
														Debug.log(0, ": Fetched ONSP from NTS in [" + (i +1) + "] attempt : Elapsed Time [" +(System.currentTimeMillis() -startNTS) +"]");													
														break;
													}													
													
												}
											}
																						
											
										}catch(Exception e){
											
												if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
													Debug.log(Debug.ALL_ERRORS, "Could not fetch the latest NTS transaction from database.");
												
										}
										finally{
											
											try {
												// if Db connection not closed
												if (dbCon != null) {
													// close the resultSet
													DBConnectionPool.getInstance(SOAConstants.NTS_POOL_ID).releaseConnection(dbCon);
												}
											} catch (ResourceException sqle) {				
												
												if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) )
													Debug.log(Debug.ALL_ERRORS, "Error Occured while releasing the NTS Database" +
														"db connection"+sqle.getMessage());
											}
											
											DBConnectionPool.setThreadSpecificPoolKey(NFConstants.NF_DEFAULT_RESOURCE);	
											
										} /* NTS lookup ends here */
										
									}																		
								}	/* ONSP lookup retry ends here */
								
								if(onspValue != null)
								{
									String nnspVal = null;
									try
									{
										CustomerContext.getInstance().set("lookupONSP", onspValue);
										if (SOAConfiguredSPIDCache.getInstance(onspValue).getSpid().equals("0"))
										{
											Debug.log(0,  "Require SPID synch up: SPID information is missing in SOA_SPID table for SPID [" +onspValue+ "] with 'ok' status and TN [" +startTN +"]");
										}											
										
									}catch(Exception exception)
									{										    
										if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
											   Debug.log(Debug.MSG_STATUS,  "Exception to check looked up ONSP value present in SOA_SPID table or not " + exception);
											}
										         
									}
									Debug.log(0, ": Populating looked up ONSP value in SV Create Port-In request [" + onspValue+ "]");									
									
									if (oldSPNode.getLength() == 0)
									{										
										Element svelement = (Element)dom.getElementsByTagName(SOAConstants.SV_CREATE_REQUEST).item(0);
										Element onspElement = dom.createElement(SOAConstants.OLD_SP_NODE);		    													
										onspElement.setAttribute("value",onspValue);					    	
										svelement.appendChild(onspElement);																																																	
									}
									else if (oldSPNode.getLength() > 0 && 
											"0000".equals(oldSPNode.item(0).getAttributes().item(0).getTextContent()))
									{
										xmg.setValue(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
												SOAConstants.OLD_SP_NODE,onspValue);																													
									}
									
									/* handle Intra port scenario :: if NNSP & Fetched ONSP are same, then convert LNPTYPE value from "lspp" to "lisp" */
				   
									NodeList newSPNode = dom.getElementsByTagName(SOAConstants.NEW_SP_NODE);
									if (newSPNode.getLength() > 0 )
									{
										nnspVal = newSPNode.item(0).getAttributes().item(0).getTextContent();
										if (nnspVal!= null && nnspVal.equals(onspValue))										
										{
											if(Debug.isLevelEnabled(Debug.NORMAL_STATUS)){
												Debug.log(Debug.NORMAL_STATUS, ": The NNSP & Fetched ONSP values are same, Converting SV Create -Portin to Intra-Port, update LnpType value to 'lisp'");
											}
											xmg.setValue(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
													SOAConstants.LNP_TYPE_NODE,"lisp");	
											if (xmg.nodeExists(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
													SOAConstants.NNSP_SIMPLEPORTINDICATOR_NODE))
												xmg.removeNode(SOAConstants.REQUEST_BODY_PATH + "."+SOAConstants.SV_CREATE_REQUEST+ "."+
														SOAConstants.NNSP_SIMPLEPORTINDICATOR_NODE);
																					
										}	
									}
									
									inputObject.set(dom);
									
								}
								else
									Debug.log(0,  "ONSP Lookup failed : SOA could not get the ONSP from PortPs service and NTS DB for Porting TN [" +startTN + "]");
							}							
							else
							{
								if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
										  Debug.log(Debug.MSG_STATUS, "OldSP Node already populated (other than '0000') by User,so skip the processing ");
								}
							}
						  }
						else
						{
							if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
									 Debug.log(Debug.MSG_STATUS, "ONSP Flag for SPID is turned - off , so skip processing");
								}
						}
														
					}
					else
					{
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							  Debug.log(Debug.MSG_STATUS, "Request is not Create Port-In request, so skip Processing");
						}
					}
				}
				else
				{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						  Debug.log(Debug.MSG_STATUS, "PortingTN and LNPTYPE are not present, so skip Processing");
					}
				}  
			}
		}catch (FrameworkException e) {
			String errMsg = "ERROR: ONSPLookup: processing failed with error: " + e.getMessage();
			
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, errMsg);
			
			// not throwing any Exception here
			
		}catch(Exception e){
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Error occure while processing ONSPLookup processor."+ e.getMessage());
			
			// not throwing any Exception here
			
		}finally
		{
			ThreadMonitor.stop(tmti);
		}	
		
		return (formatNVPair(inputObject));
		
	}
	
	/**
	 * This method call the PortPS Web service
	 * to fetch the ONSP configured for input TN.
	 */
	public String getPortPSONSP(String portingTN) {
		
		String onsp_portps =null;	
		PortingResponseType response = null;
		try
		{
			java.lang.String[] TNList = new String[1];
	        TNList[0] = portingTN;            
	        PortingRequestType ptype = new PortingRequestType(portpsUserName,portpsPassword,GET_HISTORY,TNList);                             	
	        response = portPSstub.getPortingInformation(ptype);           
			if (response  != null && response.getResponseMessage().equalsIgnoreCase("Processed"))
			{
				TNResponseType[] tnresponse = response.getTNResponseList();		
				onsp_portps = tnresponse[0].getPortingInfo().getSPID();
			}
		}catch (java.rmi.RemoteException e)
		{ 
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "SOAP service failed: " + e.getMessage());		    
		}
		catch (Exception e)
		{  
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Error: occured while fetching ONSP from PortPS Web-Service" + e.getMessage());			     
		}
		
          		    	
		return onsp_portps;
	}
	
	
	/**
	 * This method call the store procedure of NTS Database
	 * to fetch the ONSP configured for input SPID.
	 */
	public String getNTSData(String tn,Connection con)  {
		String onspVal =null;		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Inside the ONSPLookup.getNTSData method");
		
		CallableStatement cstmt = null;
		
		try{						
			
			StringBuffer ntsStoredProcedure = new StringBuffer(); 
			
			ntsStoredProcedure.append("{ call ");
			
			if(StringUtils.hasValue(ntsDB_pkg_name)){
				ntsStoredProcedure.append(ntsDB_pkg_name);
				ntsStoredProcedure.append(".");
			}
			
			ntsStoredProcedure.append("tn_lookup_oms(?,?,?,?,?,?,?,?,?) }");
						
			cstmt = con.prepareCall(ntsStoredProcedure.toString());
			//set query time-out if it takes too long.
			cstmt.setQueryTimeout(ntsLookupTimeout);
			String   tnWithoutHyphen = tn.replaceAll("-", "");
			//setting input parameters
			cstmt.setString(1, tnWithoutHyphen);			
			cstmt.setInt(2, OMS_CUSTID);						
			//registering output parameters
			cstmt.registerOutParameter(3, Types.VARCHAR);
			cstmt.registerOutParameter(4, Types.VARCHAR);
			cstmt.registerOutParameter(5, Types.VARCHAR);
			cstmt.registerOutParameter(6, Types.VARCHAR);			
			cstmt.registerOutParameter(7, Types.VARCHAR);
			cstmt.registerOutParameter(8, Types.VARCHAR);
			cstmt.registerOutParameter(9, Types.VARCHAR);			
			
			Debug.log(0, "executing  NTS stored procedure...");
			
			cstmt.execute();
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "Data retreiving from stored procedure...");
			
			onspVal = cstmt.getString(6); //ONSP		
						
			
			Debug.log(0, "Fetched following ONSP value: \n" +						
						"ONSP is ["+onspVal+"] \n");			
	
		}
		
		catch(SQLException sqlex){
			if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) ){
				  Debug.log(Debug.ALL_ERRORS," SQL Exception:" + sqlex.getMessage());
			}
			
		}
		
		catch(Exception e){
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Could not fetch the latest NTS transaction from  database.");
			
		}
		finally{
			try {
				// if statement not closed
				if (cstmt != null) {
					// close the statement
					cstmt.close();
					
					cstmt = null;
				}
			}catch (SQLException sqle) {
				
				if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) )
					Debug.log(Debug.ALL_ERRORS, "Error Occured while closing the CallableStatement. "); 
			}
			
		}
		
		return onspVal;
	}
	
	/**
	 * This method reads the configure property file and add the NTS DB pool in connection pools & portPS properties.
	 * @param propFileName
	 */
	public void configureONSPLookup( String propFileName ) 
	{
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Inside the ONSPLookup.configureONSPLookup method");
		
		Properties props = new Properties();
		
		try {			
			FileUtils.loadProperties( props, propFileName );
			
			DriverManager.registerDriver((Driver) Class.forName(props.getProperty(SOAConstants
					.DBDRIVER)).newInstance());
			
			String omsCustID = props.getProperty(SOAConstants.OMS_CUSTID);
			ntsDB_pkg_name = props.getProperty(SOAConstants.NTS_DB_PACKAGE_NAME);
			String ntsTimeoutVal = props.getProperty(SOAConstants.NTS_LOOKUP_TIMEOUT);
			if(StringUtils.hasValue(ntsTimeoutVal))
	        	ntsLookupTimeout = Integer.parseInt(ntsTimeoutVal);
			else
				ntsLookupTimeout = defLookupTimeout;
			
			String ntsRetryCountVal = props.getProperty(SOAConstants.NTS_RETRY_COUNT);
			if(StringUtils.hasValue(ntsRetryCountVal))
				ntsLookupRetryCount = Integer.parseInt(ntsRetryCountVal);
			else
				ntsLookupRetryCount = defRetryCount;
			// Configure port PS Web-Service properties
			portpsURL = props.getProperty(SOAConstants.PORTPS_WS_URL);
			portpsUserName = props.getProperty(SOAConstants.PORTPS_WS_USERNAME);
			portpsPassword  = props.getProperty(SOAConstants.PORTPS_WS_PASSWORD);
			String portpsTimeoutVal = props.getProperty(SOAConstants.PORTPS_LOOKUP_TIMEOUT);
			if(StringUtils.hasValue(portpsTimeoutVal))
	        	portpsLookupTimeout = Integer.parseInt(portpsTimeoutVal);
			else
				portpsLookupTimeout = defLookupTimeout;
			
			String portpsRetryCountVal = props.getProperty(SOAConstants.PORTPS_RETRY_COUNT);
			if(StringUtils.hasValue(portpsRetryCountVal))
				portpsLookupRetryCount = Integer.parseInt(portpsRetryCountVal);
			else
				portpsLookupRetryCount = defRetryCount;
			
			if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
				
				Debug.log(Debug.MSG_STATUS,"NTSDBNAME=["+props.getProperty(SOAConstants.NTS_DBNAME)+"]");
				Debug.log(Debug.MSG_STATUS,"NTSDBUSER=["+props.getProperty(SOAConstants.NTS_DBUSER)+"]");
				Debug.log(Debug.MSG_STATUS,"OMS_CUSTID=["+omsCustID+"]");
				Debug.log(Debug.MSG_STATUS,"PACKAGE_NAME=["+ntsDB_pkg_name+"]");
				Debug.log(Debug.MSG_STATUS,"NTS_LOOKUP_TIMEOUT=["+props.getProperty(SOAConstants.NTS_LOOKUP_TIMEOUT)+"]");
				Debug.log(Debug.MSG_STATUS,"NTS_RETRY_COUNT=["+props.getProperty(SOAConstants.NTS_RETRY_COUNT)+"]");
				Debug.log(Debug.MSG_STATUS,"PORTPS_WS_URL=["+portpsURL+"]");
				Debug.log(Debug.MSG_STATUS,"PORTPS_WS_USERNAME=["+portpsUserName+"]");
				Debug.log(Debug.MSG_STATUS,"PORTPS_LOOKUP_TIMEOUT=["+props.getProperty(SOAConstants.PORTPS_LOOKUP_TIMEOUT)+"]");
				Debug.log(Debug.MSG_STATUS,"PORTPS_RETRY_COUNT=["+props.getProperty(SOAConstants.PORTPS_RETRY_COUNT)+"]");
			}
			if(StringUtils.hasValue(omsCustID))
				OMS_CUSTID = Integer.parseInt(omsCustID);
			
			DBConnectionPool.addPoolConfiguration
				(SOAConstants.NTS_POOL_ID, props.getProperty(SOAConstants.NTS_DBNAME), props.getProperty(SOAConstants.NTS_DBUSER),
						props.getProperty(SOAConstants.NTS_DBPASSWORD));
			
			// initializing PortPS client
			
			portPSserviceLocator = new PortpsServiceLocator();

			portPSstub = (PortpsServiceBindingStub) portPSserviceLocator.getPortpsServiceBinding(new java.net.URL(portpsURL));  
			
			// added timeout if the request takes too long.
			portPSstub.setTimeout(portpsLookupTimeout*1000);
			
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, "Done initializing ONSPLookup.configureONSPLookup method");
                  
			
			
		} catch (FrameworkException fe) {
			
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Could not load properties from [" + propFileName + "] ");
			
		} catch (SQLException sqle) {			
			
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "ERROR:" + sqle.toString());
			
		} 
		catch(Exception ex){
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "ERROR:" + ex.toString());
			
		}
	}	
	
	/**
	 * This method tokenizes the input string and return an
	 * object for existing value in context or message object.
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
	protected String getValue(
		String locations)
		throws MessageException, ProcessingException {

		StringTokenizer st =
			new StringTokenizer(locations, MessageProcessorBase.SEPARATOR);

		String tok = null;
		while (st.hasMoreTokens()) {

			 tok = st.nextToken();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Checking location [" + tok + "] for value ...");
			}

			if (exists(tok, mpContext, inputObject))
			{
				return (String)(get(tok, mpContext, inputObject));
			}
		}

		return null;
	}
	
	private static PortpsServiceLocator portPSserviceLocator = null;
	private static PortpsServiceBindingStub portPSstub = null;
		
}
	
	

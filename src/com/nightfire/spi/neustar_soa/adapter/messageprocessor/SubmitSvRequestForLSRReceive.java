package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Date;
import java.util.StringTokenizer;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.comms.soap.SOAPRequestHandlerStubClient;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.generator.MessageGeneratorException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;

public class SubmitSvRequestForLSRReceive extends MessageProcessorBase{
	
	
	private String customerID_loc;
	private String spid_loc;
	private String nnsp_loc;
	private String onsp_loc;
	private String portingTN_loc;
	private String onspDueDate_loc;
	private String causeCode_loc;
	
	private String subrequest_type;
	private String customerID;
	private String spid;
	private String nnsp;
	private String onsp;
	private String portingTN;
	private String onspDueDate;
	
	private String oldSPAuthorization;
	private String SOA_SOAP_URL;
	private String request_template;
	private String causeCode;
	private String soaMessageBody;
	private String soaMessageHeader;
	
	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;
	
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
			Debug.log(Debug.SYSTEM_CONFIG, "SubmitSvRequest: Initializing...");
		
		StringBuffer errorBuffer = new StringBuffer();
		
		subrequest_type = getRequiredPropertyValue(
				SOAConstants.SUBREQUESTTYPE, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "subrequest_type location is [" 
														+ subrequest_type + "]." );
		}
		
       customerID_loc = getRequiredPropertyValue(SOAConstants.CUSTOMER_ID_PROP,
				errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of customerID location is [" + customerID_loc + "].");
		
		spid_loc = getRequiredPropertyValue(SOAConstants.SPID_PROP,
				errorBuffer);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "SPID location is [" 
												+ spid_loc + "]." );
		}
		
		portingTN_loc = getRequiredPropertyValue(
				SOAConstants.PORTINGTN_LOC, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Porting location is [" 
														+ portingTN_loc + "]." );
		}
        
        SOA_SOAP_URL = getRequiredPropertyValue(
				SOAConstants.SOA_SOAP_URL, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "SOA_SOAP_URL location is [" 
														+ SOA_SOAP_URL + "]." );
		}
        
        request_template = getRequiredPropertyValue(
				SOAConstants.SOA_REQUEST_TEMPLATE_PATH, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "request_template location is [" 
														+ request_template + "]." );
		}
		
        //Get the optional properties
		nnsp_loc = getPropertyValue(SOAConstants.NNSP_PROP);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "NNSP location is [" 
												+ nnsp_loc + "]." );
		}
		
		onsp_loc = getPropertyValue(SOAConstants.ONSP_PROP);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "ONSP location is [" 
												+ onsp_loc + "]." );
		}
		
		onspDueDate_loc = getPropertyValue(	SOAConstants.ONSPDUEDATE_COL );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "onspDueDate location is [" 
														+ onspDueDate_loc + "]." );
		}
        
        causeCode_loc = getPropertyValue(SOAConstants.CAUSECODE_COL );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "cause_code location is [" 
														+ causeCode_loc + "]." );
		}
        
        oldSPAuthorization = getPropertyValue(SOAConstants.OLDSPAUTHORIZATION_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "oldSPAuthorization location is [" 
														+ oldSPAuthorization + "]." );
		}
	}
	
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
			Debug.log(Debug.MSG_STATUS, this.getClass() + " SubmitSvRequest:Inside process method");
		
		if (inputObject == null) {
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, this.getClass() + " obj is null");
			return null;
		}

		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			this.mpContext = mpContext;
			
			this.inputObject = inputObject;
			
			if (customerID_loc != null) {
				
				// Get the customerID value from context
				customerID = getValue(customerID_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "CustomerID value :" + "[ " + customerID + " ].");			
			}
			
			if (spid_loc != null) {
				
				// Get the SPID value from context
				spid = getValue(spid_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "SPID value :" + "[ " + spid + " ].");			
			}
			
			if (portingTN_loc != null) {
				
				// Get the portingTN value from context
				portingTN = getValue(portingTN_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "SPID value :" + "[ " + portingTN + " ].");			
			}
			
			if (nnsp_loc != null) {
				
				// Get the NNSP value from context
				nnsp = getValue(nnsp_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "NNSP value :" + "[ " + nnsp + " ].");			
			}
			
			if (onsp_loc != null) {
				
				// Get the NNSP value from context
				onsp = getValue(onsp_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "ONSP value :" + "[ " + onsp + " ].");			
			}
			
			if (onspDueDate_loc != null) {
				
				// Get the onspDueDate value from context
				onspDueDate = getValue(onspDueDate_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "OnspDueDate value :" + "[ " + onspDueDate + " ].");			
			}
			if (causeCode_loc != null) {
				
				// Get the cause_code value from context
				causeCode = getValue(causeCode_loc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "cause_code value :" + "[ " + causeCode + " ].");			
			}
			
			
			//generate the SOA Message Header
			soaMessageHeader = generateSOAHeader();
			
			//generate the SOA Message Body
			if(subrequest_type.equals("SvModifyRequest")){
				
				//generate SvModifyRequest XML
				soaMessageBody = generateSVModifyRequestBody();
				
			}else if(subrequest_type.equals("SvReleaseInConflictRequest") ||
					subrequest_type.equals("SvReleaseRequest")){
				
				//generate SvReleaseInConflictRequest XML
				soaMessageBody = generateSVReleaseRequestBody();
			}else{
				
				if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
					Debug.log(Debug.ALL_ERRORS, subrequest_type +" is not supported for LSRReceiveIntegration process.");
				}
				throw new ProcessingException(subrequest_type +" is not supported for LSRReceiveIntegration process.");
			}
			
			//submit SOA API request
			
			String []response = submitRequest();
			
			if(response != null){
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(Debug.MSG_STATUS, "Synchronus Response Header:\n"+response[0]);
					Debug.log(Debug.MSG_STATUS, "Synchronus Response Body:\n"+response[1]);
				}
			}
			
		}catch (FrameworkException e) {
				String errMsg = "ERROR: SOALSRIntegrationProcess:"
					+ " failed with error: " + e.getMessage();
				
					Debug.log(Debug.ALL_ERRORS, errMsg);
				
				// Re-throw the exception to the driver.
				if (e instanceof MessageException) {
					
					throw (MessageException) e;
					
				} else {
					
					throw new ProcessingException(errMsg);
					
				}
		}catch(Exception e){
			
			throw new ProcessingException(e);
			
		}finally{
				ThreadMonitor.stop(tmti);
		}	
			
		return (formatNVPair(inputObject));
	}
	
	/**
	 * This method tokenized the input string and return an
	 * object for existing value in context or messageobject.
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
	 * Generate SvModifyRequest XML
	 * @return
	 * @throws ProcessingException
	 */
	private String generateSVModifyRequestBody() throws ProcessingException{
		
		XMLMessageGenerator body = null;
		String bodyFile = null;
		try {
			
			bodyFile = FileUtils.readFile(request_template);
			body = new XMLMessageGenerator(XMLLibraryPortabilityLayer.convertStringToDom(bodyFile));
			
			body.setValue(SOAConstants.REQUEST_HEADER_PATH +"." + "DateSent", getCurrentDate().toString());
			body.setValue(SOAConstants.REQUEST_HEADER_PATH +"." + "InitSPID", spid);
			
			body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
					"Subscription.Tn", portingTN);
			
			body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
					"DataToModify.OldSPAuthorization", oldSPAuthorization);
			
/*			if(causeCode != null && oldSPAuthorization.equalsIgnoreCase("false")){
				body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
						"DataToModify.CauseCode", causeCode);
			}else{
				body.removeNode(SOAConstants.REQUEST_BODY_PATH +"." + "SvModifyRequest" +"."+
				"DataToModify.CauseCode");
			}*/
			
			return body.generate();
			
		}catch (MessageGeneratorException fe) {
			throw new ProcessingException("Could not generate the SOA Message body XML.");
			
		}catch (FrameworkException fe) {
			throw new ProcessingException("Could not read template file to" +
					"to generate the SOA Message body XML.");
		}		
	}
	
	/**
	 * Generate SvReleaseInConflictRequest or SvReleaseRequest body xml based on the subrequest_type value. 
	 * @return
	 * @throws ProcessingException
	 */
	
	private String generateSVReleaseRequestBody()throws ProcessingException{
		
		XMLMessageGenerator body = null;
		String bodyFile = null;
		try {
			
			bodyFile = FileUtils.readFile(request_template);
			
			body = new XMLMessageGenerator(XMLLibraryPortabilityLayer.convertStringToDom(bodyFile));
			
			body.setValue(SOAConstants.REQUEST_HEADER_PATH +"." + "DateSent", getCurrentDate().toString());
			body.setValue(SOAConstants.REQUEST_HEADER_PATH +"." + "InitSPID", spid);
			
			body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
					"Subscription.Tn", portingTN);
			
			body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
					"NewSP", nnsp);
			
			body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
					"OldSP", onsp);
			
			body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
					"OldSPDueDate", onspDueDate);
			
			if(subrequest_type.equals("SvReleaseInConflictRequest")){
				body.setValue(SOAConstants.REQUEST_BODY_PATH +"." + subrequest_type +"."+
						"CauseCode", causeCode);
			}
			
			return body.generate();
			
		}catch (MessageGeneratorException fe) {
			throw new ProcessingException("Could not generate the SOA Message body XML.");
			
		}catch (FrameworkException fe) {
			throw new ProcessingException("Could not read template file to" +
					"to generate the SOA Message body XML.");
		}		
		
	}
	
	private String generateSOAHeader() throws ProcessingException {
		XMLMessageGenerator header = null;
		String headerFile = null;
		try {
			
			headerFile = FileUtils.readFile("soatemplates/SOAHeader.xml");
			
			header = new XMLMessageGenerator(XMLLibraryPortabilityLayer.convertStringToDom(headerFile));
			
			header.setValue("Subrequest", subrequest_type);
			header.setValue("CustomerIdentifier", customerID);
			
			return header.generate();
			
		}catch (MessageGeneratorException fe) {
			throw new ProcessingException("Could not generate the SOA Message Header XML.");
			
		}catch (FrameworkException fe) {
			throw new ProcessingException("Could not read template file to" +
					"to generate the SOA Message Header XML.");
		}		
	}
	
	/**
	 * Submit the SOA API request.
	 * @return
	 * @throws Exception
	 */
	private String[] submitRequest() throws ProcessingException{
		
		String response[] = null;
		
		SOAPRequestHandlerStubClient stub = new SOAPRequestHandlerStubClient(SOA_SOAP_URL);
		try{
			response = stub.processSync(soaMessageHeader, soaMessageBody);
			
			
		}catch(Exception e){
			//in case of business rule eat the thrown exception here just logs in log file  
			if(e.getMessage().contains("SOAP-ENV:Server.MessageException")){
				if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
					Debug.log(Debug.ALL_ERRORS, "Business Rule thrown from SOA Gateway. So, Rejecting the Request.\n" + e.getMessage());
				
			}else if(e.getMessage().contains("java.net.ConnectException")){
				
				if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
					Debug.log(Debug.ALL_ERRORS, "SOA Web service is not running:.\n" + e.getMessage());
				
				//throw new exception in this case
					throw new ProcessingException("SOA Web service is not running:.\n" + e.getMessage());
				
			}else if(e.getMessage().contains("SOAP-ENV:Server.SecurityException")){
				
				if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
					Debug.log(Debug.ALL_ERRORS, "Invalid username/password or domain:.\n" + e.getMessage());
				//throw new exception in this case
				throw new ProcessingException("Invalid username/password or domain:\n" + e.getMessage());
			
			}else{
				if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
					Debug.log(Debug.ALL_ERRORS, "Exception occured while calling SOA Web Service:\n" + e.getMessage());
				//throw new exception in this case
				throw new ProcessingException("Exception occured while calling SOA Web Service:\n" + e.getMessage());
				
			}
		}
		
		return response;
		
	}
	
	/**
	 * Get the current date time based on the local time zone in MM-dd-yyyy-hhmmssa format
	 * @return
	 */
	public String getCurrentDate(){
		
		String systemDate = null;
		
		systemDate = TimeZoneUtil.convert("LOCAL", "MM-dd-yyyy-hhmmssa", new Date() );
		
		return systemDate;
	}
}


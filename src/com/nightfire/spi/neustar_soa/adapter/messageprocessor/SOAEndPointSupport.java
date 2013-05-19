package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.EndPointSupportFlag;
import com.nightfire.spi.neustar_soa.utils.NancPropException;

public class SOAEndPointSupport extends MessageProcessorBase {
	
	public static final String CUSOTMERID_VAL = "CUSTID_LOC";
	public static final String MSGSUBTYPE_VAL = "MSGSUBTYPE_LOC";
	public static final String END_URL = "@context.URL";
	
	private MessageProcessorContext mpContext = null;
	
	private MessageObject inputObject = null;
	private String inputMessage = null;
	private static final String CLASSNAME = "SOAEndPointSupportLogger";
	private String customerIdLoc = null;
	private String customerIdValue = null;
	private String msgSubTypeLoc = null;
	private String msgSubTypeValue = null;
	
	private LinkedList<String> listURL = null;
	
	public void initialize(String key, String type) throws ProcessingException {

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, CLASSNAME + ", initialize() method.");
		}

		super.initialize(key, type);
		StringBuffer sbuff = new StringBuffer();
		customerIdLoc = getRequiredPropertyValue(CUSOTMERID_VAL, sbuff);
		msgSubTypeLoc = getRequiredPropertyValue(MSGSUBTYPE_VAL, sbuff);
		
		if (sbuff.length() > 0) {
			throw new ProcessingException(sbuff.toString());
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS, "SOAEndPointSupport Initialing done...");
			}
	
	}
	
	public NVPair[] process(MessageProcessorContext mpcontext,
			MessageObject inputobject) throws MessageException,
			ProcessingException {

		ThreadMonitor.ThreadInfo tmti = null;

		if (inputobject == null) {
			return null;
		}
		
		this.mpContext = mpcontext;
		this.inputObject = inputobject;
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS, "SOAEndPointSupportLogger Processing...");
			}
		
		customerIdValue = getValue(customerIdLoc, mpContext, inputObject);
		msgSubTypeValue = getValue(msgSubTypeLoc, mpContext, inputObject);
		
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log(Debug.MSG_STATUS, "Message type is "+msgSubTypeValue);
			}
		
		try{
			
			tmti = ThreadMonitor.start("Message-processor [" + getName()
					+ "] started processing the request");
			
			if(msgSubTypeValue.equals("SvCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvCreateAckNotification")){
			
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvCreateAckNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvReleaseNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvReleaseNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvReleaseAckNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvReleaseAckNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvReleaseInConflictNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvReleaseInConfNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvReleaseInConflictAckNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvReleaseInConAckfNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvActivateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvActivateNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvStatusChangeNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvStatusChangeNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvAttributeChangeNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvAttrChangeNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvDisconnectNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvDisconNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvPortToOriginalNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvPtoNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvReturnToDonorNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvRtdNotifFlag();
				
			}else if(msgSubTypeValue.equals("T1CreateRequestNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getT1CreateReqNotifFlag();				
				
			}else if(msgSubTypeValue.equals("T1ConcurrenceRequestNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getT1ConcReqNotifFlag();
				
			}else if(msgSubTypeValue.equals("T2FinalCreateWindowExpirationNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getT2FinalCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("T2FinalConcurrenceWindowExpirationNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getT2FinalConcNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvCancelNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvCancelNotifFlag();
				
			}else if(msgSubTypeValue.equals("SvCancelAckRequestNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvCancelAckNotifFlag();
				
			}else if(msgSubTypeValue.equals("NumberPoolBlockCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpbCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("NumberPoolBlockActivateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpbActivateNotifFlag();
				
			}else if(msgSubTypeValue.equals("NumberPoolBlockModifyNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpbModifyNotifFlag();
				
			}else if(msgSubTypeValue.equals("NumberPoolBlockStatusChangeNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpbStatusChangeNotifFlag();
				
			}else if(msgSubTypeValue.equals("ServiceProviderCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSpidCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("ServiceProviderModifyNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSpidModifyNotifFlag();
				
			}else if(msgSubTypeValue.equals("ServiceProviderDeleteNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSpidDeleteNotifFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxFirstUsageNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxFirstNotifFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxDeleteNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxDeleteNotifFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxXCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxXCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxXModifyNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxXModifyNotifFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxXDeleteNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxXDeleteNotifFlag();
				
			}else if(msgSubTypeValue.equals("LrnCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaLrnCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("LrnDeleteNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaLrnDeleteNotifFlag();
				
			}else if(msgSubTypeValue.equals("AuditCreateNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getAuditCreateNotifFlag();
				
			}else if(msgSubTypeValue.equals("AuditDeleteNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getAuditDeleteNotifFlag();
				
			}else if(msgSubTypeValue.equals("AuditDiscrepancyReportNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getAuditDiscRepNotifFlag();
				
			}else if(msgSubTypeValue.equals("AuditResultsNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getAuditResultsNotifFlag();
				
			}else if(msgSubTypeValue.equals("NPACRequestSuccessReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpacSuccessReplyFlag();
				
			}else if(msgSubTypeValue.equals("NPACRequestFailureReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpacFailureReplyFlag();
				
			}else if(msgSubTypeValue.equals("SvQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSvQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("NumberPoolBlockQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpbQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("ServiceProviderQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getSpidQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("NpaNxxXQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpaNxxXQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("LrnQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getLrnQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("AuditQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getAuditQueryReplyFlag();
				
			}else if(msgSubTypeValue.equals("NPACShutdownNotification")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getNpacDownNotifFlag();
				
			}else if(msgSubTypeValue.equals("OrderQueryReply")){
				
				listURL = EndPointSupportFlag.getInstance(customerIdValue).getOrderQueryReplyFlag();
				
			}
			
		StringBuffer url = new StringBuffer();
			Iterator itr = listURL.iterator();
			while(itr.hasNext())
		    {
				String strURL = (String) itr.next();
				url.append(strURL);
				if(itr.hasNext())
					url.append(",");
		    }
			
			super.set(END_URL, mpContext,inputObject, url.toString());
			
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				  Debug.log(Debug.MSG_STATUS, "URL is "+url.toString());
				}
		}catch(NancPropException nancPropException){
			 Debug.error("Could not get the Customer SOAP URL for Customer [" + customerIdValue +"] and  Notification [" + msgSubTypeValue +"]" + nancPropException.toString());
		}
		catch(Exception e){
			Debug.error("ERROR: SOAEndPointSupport:  " + e.toString());
			
		}finally{
			ThreadMonitor.stop( tmti );
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
	
}

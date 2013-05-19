package com.nightfire.spi.neustar_soa.adapter.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.queue.NPACQueueUtils;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.Session;

import com.nightfire.mgrcore.queue.QueueConstants;

/**
 * This handler is used to update the queue status of a request when we
 * receive a reply for that message. This class is also called on
 * when the resend timeout has expired for its request message.
 */
public class GenericReplyHandler extends NotificationHandler {

   /**
    * The value used to identify the message to be updated in the NPAC queue.
    */
   private String messageKey;

   /**
    * This is the SPID for whom the request was sent. In the case of
    * some GatewayError replies, the SPID is not returned in the error
    * message (for example, if the region mapping fails). This value
    * is used to populate the missing SPID.
    */
   private String spid;

   public GenericReplyHandler(NPACAdapter adapter,
                              Session session,
                              String messageKey,
                              String spid){

      super(adapter, session);

      this.messageKey = messageKey;
      this.spid = spid;

   }

   /**
    * This is called when a reply has been received.
    */
   public void run() {

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
         Debug.log(Debug.MSG_STATUS,
                   "Received successful reply for request with ID ["+
                   messageKey+"]");
      }
      
     // first check to see if the received message indicates that the session
      // or association was down
      if( ! requeue( super.notification ) ){

    	  // update the NPAC queue table so that the message is flagged as
         // a "success"
         NPACQueueUtils.deleteOnSuccess(messageKey);
         
         // process the message in the driver chain
         adapter.process( super.notification );

      }

   }

   protected void handleError(XMLMessageParser error) {

      if( ! requeue(error) ){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS,
                      "Received generic error reply for request with ID ["+
                      messageKey+"]. The request will get flagged as a "+
                      "success, and the error will get forwarded to the "+
                      "upstream system.");
         }

         // strangely enough, a gateway error is considered a successful reply
         // to our original request
         NPACQueueUtils.deleteOnSuccess(messageKey);

         try{

            XMLMessageGenerator generator =
               (XMLMessageGenerator) error.getGenerator();

            // check to see if the GatewayError was missing a customer SPID
            if (!error.textValueExists(NPACConstants.CUSTOMER_ID)) {

               if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                  Debug.log(Debug.MSG_STATUS,
                            "Gateway error reply for request with ID [" +
                            messageKey + "] does not contain a customer SPID. " +
                            "The SPID [" + spid + "] will be used.");
               }

               try {

                  generator.setTextValue(NPACConstants.CUSTOMER_ID, spid);

               }
               catch (MessageException mex) {
                  Debug.error("Could not set customer SPID value to [" +
                              spid + "]:" + mex);
               }

            }

            // pass the error message off to the driver chain
            adapter.process( generator.generate() );

         }
         catch(Exception ex){
            Debug.logStackTrace(ex);
         }

      }

   }

   private boolean requeue(XMLMessageParser message){

      boolean requeued = false;

      String status = null;
      
      if( adapter.isInvalidSession(message) ){
         status = "Session Invalid";
      }
      else if( adapter.isAssociationInRecovery(message) ){
         status = "Association in Recovery";
      }
      else if( adapter.isRegionNotAssociated(message) ){
         status = "Region Not Associated";
      }

      if(status != null){

         requeued = true;
         String regionValue = null;
         

         // messages that failed because the session or association were down
         // need to be requeued, and this needs to be done is such a way so as
         // not to flood the queue when one region is down

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS,
                      "Received ["+status+
                      "] message in response to request with ID [" +
                      messageKey + "]. This request will be flagged " +
                      " with a status of ["+QueueConstants.FAILURE_STATUS+
                      "] to be resent at a later time.");
         }

         // the retry queue poller will resend messages that have a
         // failed status in the queue
         NPACQueueUtils.updateStatus(messageKey,
                                     QueueConstants.FAILURE_STATUS);

         // update last error message to contain message failure reason
         NPACQueueUtils.updateLastErrorMessage(messageKey, status);
         
         try{
        	  regionValue =
	        	 message.getTextValue(NPACConstants.NPAC_REGION_ID);
        	  
        	  
	          if(regionValue != null)
	          {
	        	 int region = Integer.parseInt(regionValue);
	        	 
	        	 String connInstance="";
	        	 String connectivityKey = null;
	        	 
	        	 if(spid != null){
						connectivityKey = NPACQueueUtils.getConnectivityKey(spid);
						if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug.log(Debug.MSG_STATUS,
									"GenericReplyHandler Receive notification for spid :["
											+ spid
											+ "] and Connectivity : ["
											+ connectivityKey + "]");
						}
					} 
	          	 if(connectivityKey != null && connectivityKey.length()>16){
	          		   connInstance = connectivityKey.substring(17) ;
	          		 if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug.log(Debug.MSG_STATUS,
									"GenericReplyHandlerConnectivity instance :["+ connInstance + "]");
						}
	          	   }
	          	 
	          	 for (int i=0;i<=NPACConstants.NPACASSOCIATION_STATUS_ARR.length-1;i++)
		      	   {
		      		   if(connInstance.equals(""))
		      			   NPACConstants.NPACASSOCIATION_STATUS_ARR[0] = false;
		      		   else if((Integer.parseInt(connInstance))==i)
		      			   NPACConstants.NPACASSOCIATION_STATUS_ARR[i] = false;
		      	   }
	 	         	
	 			 Connection conn =null;	
	 			 PreparedStatement ps=null;
	 			 try
	 			 {
	 				 
 					conn = DBInterface.acquireConnection();
 						
 					ps=conn.prepareStatement(SOAQueryConstants.REGION_RECOVERY_UPDATE);
 	
 					ps.setString(1,SOAQueryConstants.NPACAWAITING_STATUS);
 	
 					ps.setInt(2,region);
 					
 					if(connectivityKey != null)
 						ps.setString(3,connectivityKey);
 					else
 						ps.setNull(3, java.sql.Types.VARCHAR);
 					// update SOA_REGION_RECOVERY table with awaiting status.
 					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug
								.log(
										Debug.MSG_STATUS,
										"Executing region recovery update query in GenericReplyHandler :["
												+ SOAQueryConstants.REGION_RECOVERY_UPDATE
												+ "]" + " for region ["
												+ region
												+ "] and connectivity :["
												+ connectivityKey + "]");
					}
 					ps.executeUpdate();
 	
 					conn.commit();
	 			 }
	 			 catch(Exception ex)
	 			 {
	 				try
	 				{	 	
	 					conn.rollback();
	 				}
	 				catch(Exception e)
	 				{
	 					Debug.log(Debug.SYSTEM_CONFIG,e.toString());
	 				}	 	
	 				Debug.log(Debug.SYSTEM_CONFIG,ex.toString());
	 			 }
	 			 finally
	 			 {
	 				 try
	 				 { 
	 					 if(ps!= null)
	 					 {
	 						 ps.close();							 
	 					 }
	 					 if( conn!= null)
	 					 {
	 						 DBInterface.releaseConnection( conn );
	 					 }
	 				 }
	 				 catch(Exception dbEx)
	 				 {
	 					Debug.log(Debug.SYSTEM_CONFIG,dbEx.toString());
	 				 } 
	 			 }
	          }	         
	        }catch(Exception ex){
	        	
	        	Debug.log(Debug.SYSTEM_CONFIG,ex.toString());
	        }
       }
      return requeued;
   }

   /**
    * The timout timer expired while waiting for a response. The queued
    * message will get flagged as a retry.
    */
   public void timeout() {

      // reset the message's status in the NPAC queue table so
      // that the queue agent will re-deliver the message
      NPACQueueUtils.updateStatus( messageKey, QueueConstants.RETRY_STATUS );

   }

}

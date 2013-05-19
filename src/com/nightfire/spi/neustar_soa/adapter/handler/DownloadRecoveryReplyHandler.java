///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import java.util.List;

/**
* This class is used to wait for the reply to a DownloadRecoveryRequest.
*/
public class DownloadRecoveryReplyHandler extends RecoveryHandlerBase
implements AssociationListener{
	


   public DownloadRecoveryReplyHandler(NPACAdapter adapter,
                                       Session session,
                                       int region,
                                       List spids,
                                       int spidIndex,
                                       Date recoveryCompleteTime,
                                       Date lastNotificationTime,
                                       LastNotificationTime lastNotificationTimeAccess,
                                       long interval,
                                       String requestType,
                                       boolean isTimeRangeRequest){

       super(adapter,
               session,
               region,
               spids,
               spidIndex,
               recoveryCompleteTime,
               lastNotificationTime,
               lastNotificationTimeAccess,
               interval,
               requestType,
               isTimeRangeRequest);
            
   }
   
   /**
    * Sends Swim request or download recover request depend upon download reply
    * 
    * @param downloadRecoveryReplyType recovery reply type in the response
    * @param swimActionID Swim action Id in the download recovery response.
    * 
    */
   protected void sendSwimRequest(String downloadRecoveryReplyType,
                                  String swimActionID ) {
       
       Debug.log(Debug.SYSTEM_CONFIG, "*** in side sendSwimRequest \n "+
                                            "DownloadRecoveryReplyType::"
                                             +downloadRecoveryReplyType+"\n"+
                                            "isTimeRange Request ::"
                                             +isTimeRangeRequest);
    
       String  spid = spids.get(spidIndex).toString();
      Debug.log(Debug.SYSTEM_CONFIG,"**** in sendng swim spid is::"+spid);
      
        if (downloadRecoveryReplyType != null
                && downloadRecoveryReplyType
                        .equals(NPACConstants.DOWNLOAD_DATA_NET_WORK_DATA)) {
            try
            {
            lastNotificationTimeAccess.setLastNetworkNotificationTime(spid,
                                                                      region,
                                                         lastNotificationTime);
            
          }
          catch(DatabaseException dbex){

            Debug.error("Could not log last netowork notification time for "+
                        "download recovery reply: "+dbex);
          }
        
            if(isTimeRangeRequest)
            {
                
                adapter.sendDownloadRecoveryRequestNetWorkData
                                                   (session,
                                                    region,
                                                    spids,
                                                    spidIndex,
                                                    swimActionID,
                                                    recoveryCompleteTime,
                                                    lastNotificationTime,
                                                    recoveryInterval,
                                                    isTimeRangeRequest);
            }else
            {
                adapter.sendNetworkDataSwimRequest
                                               (session,
                                                region,
                                                spids,
                                                spidIndex,
                                                recoveryCompleteTime,
                                                lastNotificationTime,
                                                swimActionID,
                                                spid,
                                                recoveryInterval );
            }
          
        }

        else if (downloadRecoveryReplyType != null
                && downloadRecoveryReplyType
                       .equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA)){
            
            
            try
            {
            lastNotificationTimeAccess.setLastServiceProvNotificationTime(spid,
                                                                      region,
                                                         lastNotificationTime);
            
          }
          catch(DatabaseException dbex){

            Debug.error("Could not log last netowork notification time for "+
                        "download recovery reply: "+dbex);
          }
            
            if(isTimeRangeRequest)
            {
                adapter.sendDownloadRecoveryRequestServiceProvidersData
                                                        (session,
                                                         region,
                                                         spids,
                                                         spidIndex,
                                                         swimActionID,
                                                         recoveryCompleteTime,
                                                         lastNotificationTime,
                                                         recoveryInterval,
                                                         isTimeRangeRequest);
                
            }else
            {
                adapter.sendServiceProvDataSwimRequest(session,
                        region,
                        spids,
                        spidIndex,
                        recoveryCompleteTime,
                        lastNotificationTime, 
                        swimActionID,
                        spid,
                        recoveryInterval);
                
                

                
            }

        }
        

    }
   


   /**
    * This is called in order to resend the recovery request with a smaller
    * interval when the previous time interval resulted in too many
    * results for the NPAC to deliver.
    *
    * @param smallerInterval long the new, smaller query interval in ms.
    */
   protected void sendSmallerInterval(long smallerInterval){
      boolean isTimeRangeRequest=true;
      // resend the recovery request immediately with the
      // smaller query interval
       if (downloadRecoveryReplyType != null
               && downloadRecoveryReplyType
                       .equals(NPACConstants.DOWNLOAD_DATA_NET_WORK_DATA)) {
                   
               adapter.sendDownloadRecoveryRequestNetWorkData
                                                 ( session,
                                                   region,
                                                   spids,
                                                   spidIndex,
                                                   swimActionID,
                                                   recoveryCompleteTime,
                                                   lastNotificationTime,
                                                   smallerInterval,
                                                   isTimeRangeRequest);
      
         
       }
       else if (downloadRecoveryReplyType != null
               && downloadRecoveryReplyType
                       .equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA)) {
           
                 
               adapter.sendDownloadRecoveryRequestServiceProvidersData
                                                       (session,
                                                        region,
                                                        spids,
                                                        spidIndex,
                                                        swimActionID,
                                                        recoveryCompleteTime,
                                                        lastNotificationTime,
                                                        recoveryInterval,
                                                        isTimeRangeRequest);
               
           }
   
       
     
   }

   protected void smallestIntervalFailed(){

      // The recovery failed for an interval of 1 second.
      // Hopefully, this will never ever happen, but just in case,
      // it's good to handle our boundary conditions. We'll
      // log a nasty error saying that the recovery failed for
      // this SPID, and then proceed with recovery for the next
      // SPID.
      Debug.error("A \""+status+"\" download recovery response was received "+
                  "for a recovery interval of 1 second!!! "+
                  "Download recovery will be aborted.");
      boolean isTimeRangeRequest = false;
      
      if (downloadRecoveryReplyType != null
              && downloadRecoveryReplyType
                      .equals(NPACConstants.DOWNLOAD_DATA_NET_WORK_DATA)) {
                  
              adapter.sendDownloadRecoveryRequestNetWorkData
                                                    ( session,
                                                      region,
                                                      spids,
                                                      ++spidIndex,
                                                      null,
                                                      recoveryCompleteTime,
                                                      null,
                                                      recoveryInterval,
                                                      isTimeRangeRequest);
                                     
        
      }else if (downloadRecoveryReplyType != null
              && downloadRecoveryReplyType
              .equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA)) {
  
        
      adapter.sendDownloadRecoveryRequestServiceProvidersData
                                              (session,
                                               region,
                                               spids,
                                               ++spidIndex,
                                               null,
                                               recoveryCompleteTime,
                                               null,
                                               recoveryInterval,
                                               isTimeRangeRequest);
      
      }

      
      

   }


   protected void handleError(){
       Debug.log(Debug.SYSTEM_CONFIG, "DRR in side handle error");

      if(! isAborted() ){

         Debug.error("Download recovery failed");

         // create a retry task to retry the recovery request
         adapter.retryDownloadRecoveryRequest(session,
                                              region,
                                              spids,
                                              spidIndex,
                                              recoveryCompleteTime,
                                              lastNotificationTime,
                                              recoveryInterval,
                                              downloadRecoveryReplyType,
                                              isTimeRangeRequest                                              
                                              );

      }

   }

   public static boolean isSuccess(XMLMessageParser parser){

      return isSuccess( getStatusValue(parser) );

   }

   private static boolean isSuccess(String statusString){

      return statusString != null &&
             statusString.equals(NPACConstants.SUCCESS_STATUS);

   }

   protected String getStatus(XMLMessageParser parser){
      return getStatusValue(parser);

   }
   /**
    * Gets the RecoveryReply type in the response xml
    * @param parser XMLMessageParser object which contains information 
    *               about response.
    * @return String 
    */
   protected String getDownloadRecoveryReplyType(XMLMessageParser parser) {

        String downloadRecoveryReplyType = null;
        try {
          if(  parser.exists
                  (NPACConstants.DOWNLOAD_RECOVERY_REPLY_NOTIFICATION_TYPE)){
            downloadRecoveryReplyType = parser.getNode(
                    NPACConstants.DOWNLOAD_RECOVERY_REPLY_NOTIFICATION_TYPE)
                    .getNodeName();
            }
        } catch (MessageException mex) {

            Debug.error("Could not retrieve download recovery reply type: "
                    + mex.toString());

        }

        return downloadRecoveryReplyType;
    }
   
   /**
    * gets Swim aciton Id in the response xml   
    * 
    * @param parser XMLMessageParser object which contains response information.
    * @return String 
    */
   
   
   protected String getSwimActionId(XMLMessageParser parser) {
        String actionID = null;

        try {
           
              if(parser.exists
                      (NPACConstants.DOWNLOAD_RECOVERY_REPLY_ACTION_ID))
              {
                
                actionID = parser
                    .getTextValue
                    (NPACConstants.DOWNLOAD_RECOVERY_REPLY_ACTION_ID);
              }

        } catch (MessageException mex) {

            Debug.error("Could not get download recovery reply actionId: "
                    + mex);

        }
      
        return actionID;

    }
   
   /**
    * gets status value  in the response xml   
    * 
    * @param parser XMLMessageParser object which contains response 
    *               information.
    * @return String 
    */

   private static String getStatusValue(XMLMessageParser parser){

      String result = null;

      try{
         // get the recovery status
         result =
           parser.getTextValue(NPACConstants.DOWNLOAD_RECOVERY_REPLY_STATUS);

      }
      catch(MessageException mex){

         Debug.error("Could not get download recovery reply status: "+mex);

      }

      return result;

   }

   /**
    * Used for loggging purposes.
    *
    * @return String
    */
   public String toString(){
      return "Download Recovery Reply Handler";
   }

   /**
    * This is called when the timer for this handler has expired before
    * a reply was received.
    */
   public void timeout(){

       // resend the message if the related association has not been
       // reinitialized
       if( ! isAborted() ){
           
           if ( downloadRecoveryReplyType != null
                   && downloadRecoveryReplyType
                       .equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA)){
               
               
               
               adapter.sendDownloadRecoveryRequestServiceProvidersData
                                                      (session,
                                                       region,
                                                       spids,
                                                       spidIndex,
                                                       swimActionID,
                                                       recoveryCompleteTime,
                                                       lastNotificationTime,
                                                       recoveryInterval,
                                                       isTimeRangeRequest);
           }else if(downloadRecoveryReplyType != null
                   && downloadRecoveryReplyType
                   .equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA))
           {
               adapter.sendDownloadRecoveryRequestNetWorkData
                                                  (session,
                                                   region,
                                                   spids,
                                                   spidIndex,
                                                   swimActionID,
                                                   recoveryCompleteTime,
                                                   lastNotificationTime,
                                                   recoveryInterval,
                                                   isTimeRangeRequest);
               
           }
         }
       

       }

   

}

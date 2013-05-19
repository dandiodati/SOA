///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

/**
* This class is used to wait for the reply to a RecoveryRequest.
*/
public class RecoveryReplyHandler extends RecoveryHandlerBase
                                  implements AssociationListener{

   public RecoveryReplyHandler(NPACAdapter adapter,
                               Session session,
                               int region,
                               List spids,
                               int spidIndex,
                               Date recoveryCompleteTime,
                               Date newLastNotificationTime,
                               LastNotificationTime lastNotificationAccess,
                               long currentRecoveryInterval,
                               String requestType,
                               boolean isTimeRangeRequest){

  
      super(adapter,
            session,
            region,
            spids,
            spidIndex,
            recoveryCompleteTime,
            newLastNotificationTime,
            lastNotificationAccess,
            currentRecoveryInterval,
            requestType,
            isTimeRangeRequest);

   }

   /**
    * For logging purposes.
    *
    * @return String
    */
   public String toString(){
      return "Recovery Reply Handler";
   }

   /**
    * gets Swim aciton Id in the response xml   
    * 
    * @param parser XMLMessageParser object which contains response 
    * information.
    * @return String 
    */
   
    protected  String getSwimActionId(XMLMessageParser parser)
    {
        String actionID= null;
           
           try{
               if(parser.exists(NPACConstants.RECOVERY_REPLY_ACTION_ID)){
                actionID = parser.getTextValue(NPACConstants.
                            RECOVERY_REPLY_ACTION_ID);
               }
           }catch(MessageException mex){
    
                 Debug.error("Could not get download recovery reply actionId: "
                         +mex);
    
              }
           
           Debug.log(Debug.SYSTEM_CONFIG,"**** Returned ActionId in " +
                "Recovery Reply:::"+actionID);
           return actionID;
    }
       
   protected  String  getDownloadRecoveryReplyType(XMLMessageParser parser)
   {
	   return null;
   }
   
   protected  void sendSwimRequest(String recoveryReplyType, 
                                   String swimActionID)
   {
	   sendSwimRequest(swimActionID);
   }
   
   private void sendSwimRequest(String swimActionID)
   {

       
       if(isTimeRangeRequest)
       {
           adapter.sendRecoveryRequest(session, 
                   region,
                   spids,
                   spidIndex,
                   null,
                   recoveryCompleteTime,
                   lastNotificationTime,
                   recoveryInterval,
                   isTimeRangeRequest);
       }else
       {
          
      String spid = spids.get(spidIndex).toString();
       adapter.sendRecoverySwimRequest(session,
                                       region,
                                       spids,
                                       spidIndex,
                                       recoveryCompleteTime,
                                       lastNotificationTime, 
                                       swimActionID,
                                       spid,recoveryInterval);
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
         adapter.sendRecoveryRequest(session, 
									 region,
									 spids,
									 spidIndex,
									 swimActionID,
									 recoveryCompleteTime,
									 lastNotificationTime,
									 smallerInterval,
									 isTimeRangeRequest);

   }

   protected void smallestIntervalFailed(){

      String spid = spids.get(spidIndex).toString();

      // The recovery failed for an interval of 1 second.
      // Hopefully, this will never ever happen, but just in case,
      // it's good to handle our boundary conditions. We'll
      // log a nasty error saying that the recovery failed for
      // this SPID, and then proceed with recovery for the next
      // SPID.
      Debug.error("A \""+status+"\" recovery response was received "+
                  "for a recovery interval of 1 second!!! "+
                  "Recovery will be aborted for SPID ["+
                  spid+"] in region ["+region+"]");
      boolean isTimeRangeRequest = false;

      adapter.sendRecoveryRequest(session,
                                  region,
                                  spids,
                                  ++spidIndex,
                                  recoveryCompleteTime,
                                  null,
                                  recoveryInterval,
                                  isTimeRangeRequest);

   }


   protected void handleError(){

      if(! isAborted() ){

         Debug.error("Recovery failed in region ["+region+"]");

         // create a retry task to retry the recovery request
         adapter.retryRecoveryRequest(session,
                                      region,
                                      spids,
                                      spidIndex,
                                      recoveryCompleteTime,
                                      lastNotificationTime,
                                      recoveryInterval,
                                      downloadRecoveryReplyType,
                                      isTimeRangeRequest);
      }

   }

   public static boolean isSuccess(XMLMessageParser parser){

      return isSuccess( getStatusValue(parser) );

   }

   private static boolean isSuccess(String statusString){

      return statusString != null &&
             statusString.equals(NPACConstants.SUCCESS_STATUS);

   }

   /**
    * gets status value  in the response xml   
    * 
    * @param parser XMLMessageParser object which contains 
    * response information.
    * @return String 
    */
   protected String getStatus(XMLMessageParser parser){
      return getStatusValue(parser);

   }

   /**
    * gets status value  in the response xml   
    * 
    * @param parser XMLMessageParser object which contains 
    * response information.
    * @return String 
    */
   private static String getStatusValue(XMLMessageParser parser){

      String result = null;

      try{
         // get the recovery status
         result =
           parser.getTextValue(NPACConstants.RECOVERY_REPLY_STATUS);

      }
      catch(MessageException mex){

         Debug.error("Could not get recovery reply status: "+mex);

      }

      return result;

   }


   /**
    * This is called when the timer for this handler has expired before
    * a reply was received.
    */
   public void timeout(){

       // resend the message if the related association has not been
       // reinitialized
       if( ! isAborted() ){

          adapter.sendRecoveryRequest
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

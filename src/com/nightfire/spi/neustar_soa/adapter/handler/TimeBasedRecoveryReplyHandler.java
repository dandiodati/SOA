///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

/**
* This class is used to wait for the reply to a RecoveryRequest.
*/
public class TimeBasedRecoveryReplyHandler extends TimeBasedRecoveryHandlerBase
                                  implements AssociationListener{

   public TimeBasedRecoveryReplyHandler(NPACAdapter adapter,
                               Session session,
                               int region,
                               List spids,
                               int currentSPIDIndex,
                               Date recoveryCompleteTime,
                               Date newLastNotificationTime,
							   Date startTime,
                               LastNotificationTime lastNotificationAccess,
                               long currentRecoveryInterval){

      super(adapter,
            session,
            region,
            spids,
            currentSPIDIndex,
            recoveryCompleteTime,
            newLastNotificationTime,
		    startTime,
            lastNotificationAccess,
            currentRecoveryInterval);

   }

   /**
    * For logging purposes.
    *
    * @return String
    */
   public String toString(){
      return "TimeBased Recovery Reply Handler";
   }

   protected void sendNextRequest(){

      String spid = spids.get(currentSPIDIndex).toString();

      try{

         // log the new last notification time to the DB
         lastNotificationTimeAccess.setLastNotificationTime(spid,
                                                            region,
                                                            lastNotificationTime);
      }
      catch(DatabaseException dbex){

        Debug.error("Could not log last notification time for the "+
                    "recovery reply: "+dbex);

      }

      // send next recovery request
      adapter.sendTimeBasedRecoveryRequest(session,
                                  region,
                                  spids,
                                  currentSPIDIndex,
                                  recoveryCompleteTime,
                                  lastNotificationTime );


   }

   /**
    * This is called in order to resend the recovery request with a smaller
    * interval when the previous time interval resulted in too many
    * results for the NPAC to deliver.
    *
    * @param smallerInterval long the new, smaller query interval in ms.
    */
   protected void sendSmallerInterval(long smallerInterval){

         // resend the recovery request immediately with the
         // smaller query interval
         adapter.sendTimeBasedRecoveryRequest(session,
                                     region,
                                     spids,
                                     currentSPIDIndex,
                                     recoveryCompleteTime,
                                     lastNotificationTime,
                                     smallerInterval);

   }

   protected void smallestIntervalFailed(){

      String spid = spids.get(currentSPIDIndex).toString();

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

      adapter.sendTimeBasedRecoveryRequest(session,
                                  region,
                                  spids,
                                  currentSPIDIndex+1,
                                  recoveryCompleteTime,
                                  null);

   }


   protected void handleError(){

      if(! isAborted() ){

         Debug.error("TimeBased Recovery failed in region ["+region+"]");

         // create a retry task to retry the recovery request
         adapter.retryTimeBasedRecoveryRequest(session,
                                      region,
                                      spids,
                                      currentSPIDIndex,
                                      recoveryCompleteTime,
                                      lastNotificationTime,
                                      recoveryInterval );
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

          adapter.sendTimeBasedRecoveryRequest(session,
                                      region,
                                      spids,
                                      currentSPIDIndex,
                                      recoveryCompleteTime,
                                      lastNotificationTime,
                                      recoveryInterval);

       }

   }

}

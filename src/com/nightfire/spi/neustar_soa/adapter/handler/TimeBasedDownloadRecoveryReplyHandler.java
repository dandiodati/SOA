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
* This class is used to wait for the reply to a DownloadRecoveryRequest.
*/
public class TimeBasedDownloadRecoveryReplyHandler extends TimeBasedRecoveryHandlerBase
                                          implements AssociationListener{


   public TimeBasedDownloadRecoveryReplyHandler(NPACAdapter adapter,
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

   protected void sendNextRequest(){

      String spid = spids.get(currentSPIDIndex).toString();

      try{

         lastNotificationTimeAccess.setLastNetworkNotificationTime(spid,
                                                                   region,
                                                     lastNotificationTime);

      }
      catch(DatabaseException dbex){

        Debug.error("Could not log last netowork notification time for "+
                    "download recovery reply: "+dbex);

      }

      // send next download recovery request
      adapter.sendTimeBasedDownloadRecoveryRequest(session,
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
      adapter.sendTimeBasedDownloadRecoveryRequest(session,
                                          region,
                                          spids,
                                          currentSPIDIndex,
                                          recoveryCompleteTime,
                                          lastNotificationTime,
                                          smallerInterval);

   }

   protected void smallestIntervalFailed(){

      // The recovery failed for an interval of 1 second.
      // Hopefully, this will never ever happen, but just in case,
      // it's good to handle our boundary conditions. We'll
      // log a nasty error saying that the recovery failed for
      // this SPID, and then proceed with recovery for the next
      // SPID.
      Debug.error("A \""+status+"\" TimeBased download recovery response was received "+
                  "for a recovery interval of 1 second!!! "+
                  "Download recovery will be aborted.");

      // skip ahead and send the first RecoveryRequest
      adapter.sendFirstRecoveryRequest(session,
                                       region,
                                       recoveryCompleteTime);

   }


   protected void handleError(){

      if(! isAborted() ){

         Debug.error("TimeBased Download recovery failed");

         // create a retry task to retry the recovery request
         adapter.retryTimeBasedDownloadRecoveryRequest(session,
                                              region,
                                              spids,
                                              currentSPIDIndex,
                                              recoveryCompleteTime,
                                              lastNotificationTime,
                                              recoveryInterval);

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
      return "TimeBased Download Recovery Reply Handler";
   }

   /**
    * This is called when the timer for this handler has expired before
    * a reply was received.
    */
   public void timeout(){

       // resend the message if the related association has not been
       // reinitialized
       if( ! isAborted() ){

          adapter.sendTimeBasedDownloadRecoveryRequest(session,
                                              region,
                                              spids,
                                              currentSPIDIndex,
                                              recoveryCompleteTime,
                                              lastNotificationTime,
                                              recoveryInterval);

       }

   }

}

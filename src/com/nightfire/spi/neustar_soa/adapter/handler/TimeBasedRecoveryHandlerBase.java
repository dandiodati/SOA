////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

public abstract class TimeBasedRecoveryHandlerBase extends NotificationHandler
                                          implements AssociationListener{

   protected int region;

   protected Date recoveryCompleteTime;

   protected Date lastNotificationTime;

   protected Date startTime;

   protected LastNotificationTime lastNotificationTimeAccess;

   /**
   * This is the length of the recovery query (in ms). This is
   * used when a "criteria-too-large" response is received to
   * make sure that the next recovery request is sent using
   * a smaller interval of time.
   */
   protected long recoveryInterval;

   /**
   * This is the value of the recovery status as retrieved from the
   * RecoveryResponse. This is extracted from the response by
   * receiveNotification() and used by the run() method in
   * determining how to handle the response.
   */
   protected String status;

   /**
   * This is the list of customer SPIDs that require recovery for this
   * region.
   */
   protected List spids;

   /**
   * This is the index of the customer SPID in the spids list for
   * which we are currently performing recovery.
   */
   protected int currentSPIDIndex;

   /**
   * This flag is set to true if the status of the related association changes
   * while we are waiting for the RecoveryReply. If this flag is true,
   * then the RecoveryReply will be ignored.
   */
   private boolean aborted = false;

   public TimeBasedRecoveryHandlerBase(NPACAdapter adapter,
                              Session session,
                              int region,
                              List spids,
                              int currentSPIDIndex,
                              Date recoveryCompleteTime,
                              Date newLastNotificationTime,
							  Date newStartTime,
                              LastNotificationTime lastNotificationTimeAccess,
                              long currentRecoveryInterval){

      super(adapter, session);

      this.region = region;
      this.recoveryCompleteTime = recoveryCompleteTime;
      this.lastNotificationTime = newLastNotificationTime;
	  this.startTime = newStartTime;
      this.lastNotificationTimeAccess = lastNotificationTimeAccess;
      this.recoveryInterval = currentRecoveryInterval;
      this.spids = spids;
      this.currentSPIDIndex = currentSPIDIndex;

      // listen for changes to the related association
      session.addAssociationListener(this);

   }

   public int receiveNotification(XMLMessageParser parsedNotification){

      int ack = super.receiveNotification(parsedNotification);

      status = getStatus(parsedNotification);

      if( Debug.isLevelEnabled( Debug.IO_STATUS ) ){

         Debug.log( Debug.IO_STATUS,
                    this+
                    " received recovery response status ["+status+"]" );

      }

      return ack;

   }

   public void run(){

      // stop listening for association changes
      session.removeAssociationListener(this);

      if(aborted){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

             Debug.log(Debug.MSG_STATUS,
                       "A TimeBased recovery response was received after the "+
                       "association was reinitialized. This response will "+
                       "be ignored.");

         }

         return;

      }

      // check that the reply had a status value
      if( status != null ){

		 // if the request was successful or if there was not data to be found,
         // then proceed happily...
         if( status.equals(NPACConstants.SUCCESS_STATUS) ){

            // pass the batched reply off to the driver chain for processing
            adapter.process(notification);

            // we've received a successful response, send the next
            // request
            sendNextRequest();

         }
         else if( status.equals(NPACConstants.NO_DATA_SELECTED_STATUS) ){

            // we've received a successful, empty response. send the next
            // request
            sendNextRequest();

         }
         else if( status.equals( NPACConstants.CRITERIA_TOO_LARGE ) ||
                  status.equals( NPACConstants.TIME_RANGE_INVALID )  ){

            // If we get a reply of criteria-too-large, we assume that
            // the number of notifications that would be recovered for
            // the current time interval was too large, and the NPAC
            // did not want to send it to us. We divide the recovery interval
            // by 2 and try again with the hopes that the number of
            // notifications found for that period will be small enough to
            // recover.
            if(recoveryInterval > 1000){

               // The last notification time is the "end" time
               // for our query interval, so we need to subtract
               // the previous recovery interval.
               long lastNotificationTimeMs = startTime.getTime();

               //lastNotificationTimeMs -= recoveryInterval;
               // then subtract one additional second that was added by the
               // sendRecoveryRequest() method to make sure we didn't
               // query the last notification time twice.
               lastNotificationTimeMs -= 1000;
               // reset the time
               lastNotificationTime.setTime(lastNotificationTimeMs);

               // divide the interval in half
               long smallerInterval = recoveryInterval/2;

               if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){

                  Debug.log( Debug.MSG_STATUS,
                             "Retrying recovery request with an interval of ["+
                             smallerInterval+"] ms" );

               }

               sendSmallerInterval( smallerInterval );

            }
            else{

               // recovery failed for an interval of only one second
               smallestIntervalFailed();

            }

         }
         else{

            // the reply status was "failed"
            handleError();

         }

      }
      else{

         // the status was null
         handleError();

      }

   }

   protected void handleError(XMLMessageParser error){

      // stop listening for association changes
      session.removeAssociationListener(this);

      handleError();

   }

   protected abstract void handleError();

   protected abstract void sendNextRequest();

   protected abstract void sendSmallerInterval(long smallerInterval);

   protected abstract void smallestIntervalFailed();

   /**
    * Utility method for getting the status value from a response.
    *
    * @param parser XMLMessageParser
    * @return String
    */
   protected abstract String getStatus(XMLMessageParser parser);

   /**
   * This method is called by the Session when the state of
   * an association changes (for example it goes down, completes
   * recovery, etc.).
   *
   * This reply handler will be aborted if the status of the association
   * changes while we are still waiting for a RecoveryReply. If the
   * association status has changed while we are waiting, this means that
   * the association was probably aborted. If this happens,
   * we need to disable/abort this handler, because a negative RecoveryReply
   * may still be received due to a timeout in the OSS Gateway.
   *
   * This defines the AssociationListener interface.
   *
   */
   public void associationStateChanged(AssociationEvent event){

      // check whether the change occured in this region
      if( region == event.getRegion() ){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

            Debug.log(Debug.MSG_STATUS,
                      "Association status for region ["+region+
                      "] has changed. Recovery reply handler ["+
                      this+"] will be disabled.");

         }

         aborted = true;

      }

   }


   /**
    * This is called when the resend timer for this handler has expired before
    * a reply was received.
    */
   public abstract void timeout();

   /**
    * Allows subclassed to check whether this handler has been aborted due
    * to a change in the association's status.
    *
    * @return boolean
    */
   protected boolean isAborted(){
      return aborted;
   }

}

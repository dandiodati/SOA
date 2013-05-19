package com.nightfire.spi.neustar_soa.adapter.handler;

import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.adapter.AssociationEvent;
import com.nightfire.spi.neustar_soa.adapter.AssociationListener;
import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.Session;

import java.util.*;

public class TimeBasedDownloadRecoveryRequestRetry extends Retry
                                          implements AssociationListener {

   /**
   * This reference to the adapter is used to make the actual
   * recovery call.
   */
   protected NPACAdapter adapter;

   /**
   * The region/association for which we are recovering.
   */
   protected int region;

   protected Date recoveryCompleteTime;

   protected Date lastNotificationTime;

   protected long recoveryInterval;

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
   * This flag gets set to true in the case that the association
   * we are recovering for has been brought down. If this flag
   * is set to true, then the retry attempt will be skipped.
   */
   private boolean aborted = false;

   public TimeBasedDownloadRecoveryRequestRetry(long waitPeriod,
                                       NPACAdapter adapter,
                                       Session session,
                                       int region,
                                       List spids,
                                       int currentSPID,
                                       Date recoveryComplete,
                                       Date lastNotificationTime,
                                       long recoveryInterval){

      super(waitPeriod, session);
      this.adapter = adapter;
      this.region = region;
      this.recoveryCompleteTime = recoveryComplete;
      this.lastNotificationTime = lastNotificationTime;
      this.recoveryInterval = recoveryInterval;
      this.spids = spids;
      this.currentSPIDIndex = currentSPID;

      // listen for changes to the association's status.
      session.addAssociationListener(this);

   }

   /**
   * This resends the request.
   */
   protected void retry(){

      try{

         if( aborted ){

            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
               Debug.log(Debug.MSG_STATUS,
                         "Aborting retry attempt ["+this+
                         "] because the related association has been "+
                         "reinitialized.");
            }

         }
         else{

            long lastNotificationTimeMs = lastNotificationTime.getTime();

            // The last notification time is the "end" time
            // for our query interval, so we need to subtract
            // the previous recovery interval so as not to
            // skip ahead to the next interval when doing the retry.
            lastNotificationTimeMs -= recoveryInterval;

            // One second was added to the recovery "start" time in
            // order to avoid querying the same second twice.
            // That one second also needs to be subtracted.
            lastNotificationTimeMs -= 1000;

            // set the new time
            lastNotificationTime.setTime(lastNotificationTimeMs);

            // send off the request
            send();

         }

      }
      finally{

         // stop listening for association changes
         session.removeAssociationListener(this);

      }


   }

   /**
    * This actually sends the request. This callout method allows this
    * method to be overriden while the rest of the functionality in this class
    * is reused.
    */
   protected void send(){

      adapter.sendTimeBasedDownloadRecoveryRequest(session,
                                          region,
                                          spids,
                                          currentSPIDIndex,
                                          recoveryCompleteTime,
                                          lastNotificationTime,
                                          recoveryInterval);

   }

   /**
   * Used for the logging.
   */
   public String toString(){

      return "TimeBased Download recovery request retry for session ["+
             originalSessionID+"], region ["+
             region+"], last notification time ["+
             lastNotificationTime.toGMTString()+"]";

   }

   /**
   * This gets called when this retry's session experiences an association
   * state change. This method checks to see if the association we are
   * recovering for has gone down. If so, we will abort this retry
   * attempt so that the retry will not continue forever.
   *
   */
   public void associationStateChanged(AssociationEvent event){

      // does this event involve the region/association for which we are
      // recovering?
      if( event.getRegion() == region ){

         // if the association status has changed in any way
         // (e.g. Retrying, Down, even Connected), then we will
         // abort this retry attempt
         aborted = true;

      }

   }
}

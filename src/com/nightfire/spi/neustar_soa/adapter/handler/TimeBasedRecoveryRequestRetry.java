///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.spi.neustar_soa.adapter.*;

/**
* This is used to retry a recovery attempt in the case of a failure.
*/
public class TimeBasedRecoveryRequestRetry extends TimeBasedDownloadRecoveryRequestRetry {


   public TimeBasedRecoveryRequestRetry(long waitPeriod,
                               NPACAdapter adapter,
                               Session session,
                               int region,
                               List spids,
                               int currentSPID,
                               Date recoveryComplete,
                               Date lastNotificationTime,
                               long recoveryInterval){

      super(waitPeriod,
            adapter,
            session,
            region,
            spids,
            currentSPID,
            recoveryComplete,
            lastNotificationTime,
            recoveryInterval);

   }

   /**
    * This overrides the parent method to send a RecoveryRequest.
    */
   protected void send(){

      adapter.sendTimeBasedRecoveryRequest(session,
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

      return "TimeBased RecoveryRequest retry for session ["+
             originalSessionID+"], region ["+
             region+"], last notification time ["+
             lastNotificationTime.toGMTString()+"]";

   }

}

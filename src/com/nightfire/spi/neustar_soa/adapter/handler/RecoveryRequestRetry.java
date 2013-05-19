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
public class RecoveryRequestRetry extends DownloadRecoveryRequestRetry {


   public RecoveryRequestRetry(long waitPeriod,
                               NPACAdapter adapter,
                               Session session,
                               int region,
                               List spids,
                               int spidIndex,
                               Date recoveryComplete,
                               Date lastNotificationTime,
                               long recoveryInterval, 
                               String requestType,
                               boolean isTimeRangeRequest)
   {
                               
         super(waitPeriod,
               adapter,
               session,
               region,
               spids,
               spidIndex,
               recoveryComplete,
               lastNotificationTime,
               recoveryInterval,
               requestType,
               isTimeRangeRequest);

   }

   /**
    * This overrides the parent method to send a RecoveryRequest.
    */
   protected void send(){

      adapter.sendRecoveryRequest(session,
                                  region,
                                  spids,
                                  spidIndex,
                                  null,
                                  recoveryCompleteTime,
                                  lastNotificationTime,
                                  recoveryInterval,
                                  isTimeRangeRequest);

   }

   /**
   * Used for the logging.
   */
   public String toString(){

      return "RecoveryRequest retry for session ["+
             originalSessionID+"], region ["+
             region+"], last notification time ["+
             lastNotificationTime.toGMTString()+"]";

   }

}

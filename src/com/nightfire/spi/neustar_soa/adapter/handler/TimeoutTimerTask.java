////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.TimerTask;

import com.nightfire.framework.util.Debug;
import com.nightfire.spi.neustar_soa.adapter.WorkQueue;

/**
 * This class is used to perform a timeout when waiting for a reply
 * from the NPAC gateway.
 */
public class TimeoutTimerTask extends TimerTask {

   /**
    * The invoke ID of the reply message for which this timer is waiting.
    * This is used to identify this timer task in log messages.
    */
   private String invokeID;

   /**
    * This object is waiting for a reply message. Should this
    * timer task expire, it will call timeout() on the handler to
    * signal that it should give up hope of receiving a reply.
    */
   private NotificationHandler handler;

   /**
    * This instance is used to execute units of work in the background
    * once this timer has expired.
    */
   private WorkQueue workerThreads;

   /**
    * This is a reference to the collection of waiting handlers.
    * This is used to callback and remove this timer's handler from the
    * collection should this timer task expire.
    */
   private WaitingHandlers waitingHandlers;

   public TimeoutTimerTask( String invokeID,
                            NotificationHandler handler,
                            WorkQueue workQueue,
                            WaitingHandlers handlers ){

      this.handler = handler;
      this.invokeID = invokeID;
      this.workerThreads = workQueue;
      this.waitingHandlers = handlers;

      handler.setTimeoutTimer(this);

   }

   /**
    * This is called when this timer task expires. This creates
    * a runnable to call timeout() on the handler instance. The runnable
    * will get executed in a worker thread.
    */
   public void run() {

      workerThreads.enqueue( new TimeoutRunnable() );

   }

   /**
    * This overrides the cancel() method to add a logging message.
    * @return boolean
    */
   public boolean cancel() {

      Debug.log(Debug.MSG_STATUS,
                "Cancelling timeout timer that was waiting for invoke ID ["+
                invokeID+"]");

       return super.cancel();

   }

   /**
    * An instance of this runnable is created and used to perform the
    * timeout action in a separate worker thread.
    */
   private class TimeoutRunnable implements Runnable{

      public void run(){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS,
                      "Timed-out while waiting for reply "+
                      "with invoke ID ["+invokeID+"]");
         }

         // remove the handler from the collection of waiting handlers,
         // so that it is no longer waiting for a reply
         waitingHandlers.remove(invokeID);

         // tell the handler to resend the message or do whatever it is
         // that it is supposed to do when it times out
         handler.timeout();

      }

      public String toString(){
         return "Timeout for invoke ID ["+invokeID+"]";
      }

   }

}

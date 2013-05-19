////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.spi.neustar_soa.adapter.WorkQueue;

public class WaitingHandlers {

   /**
    * This map contains all of the waiting notification/reply handlers.
    * They are keyed by their invoke ID.
    */
   private Map handlers;

   /**
    * The timer implementation.
    */
   private Timer timer;

   /**
    * A collection of worker threads that pulls work from a queue for
    * execution in the background.
    */
   private WorkQueue workerThreads;

   public WaitingHandlers(WorkQueue workQueue){

      handlers = new Hashtable();
      timer = new Timer();
      workerThreads = workQueue;

   }

   public void add(String invokeID,
                   NotificationHandler handler,
                   long timeout){

      TimerTask timeoutTask = new TimeoutTimerTask(invokeID,
                                                   handler,
                                                   workerThreads,
                                                   this );

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS,
                   "Creating timeout timer to wait for "+
                   "reply message with invoke ID ["+
                   invokeID+"]. Timer will expire in ["+
                   timeout+"] ms.");

      }

      timer.schedule( timeoutTask, timeout );

      add( invokeID, handler );

   }

   public void add(String invokeID, NotificationHandler handler){

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS,
                   "Adding notification handler to wait for a "+
                   "reply message with invoke ID ["+
                   invokeID+"]");

      }

      handlers.put(invokeID, handler);

   }

   /**
    * This checks whether a handler instance exists for the given invoke ID.
    *
    * @param invokeID String an invoke ID.
    * @return boolean true if there is a handler for the given invoke ID,
    *                      false otherwise.
    */
   public boolean exists(String invokeID){

      return handlers.containsKey(invokeID);

   }

   /**
    * This checks to see if there is an instance of GenericReplyHandler
    * handler waiting for the given invoke ID. This is used to determine
    * if the request was an automated request or not.
    *
    * @param invokeID String the invoke ID.
    * @return boolean
    */
   public boolean isGenericReplyHandler(String invokeID){

      return ( handlers.get(invokeID) instanceof GenericReplyHandler );

   }

   public NotificationHandler remove(String invokeID){

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS,
                   "Removing notification handler for invoke ID ["+
                   invokeID+"]");

      }

      NotificationHandler handler =
         (NotificationHandler) handlers.remove(invokeID);


      if(handler == null) {

         if(Debug.isLevelEnabled(Debug.ALL_WARNINGS)){

            Debug.warning("Could not find handler for invoke ID ["+
                          invokeID+"]");

         }

      }

      return handler;

   }

   public NotificationHandler cancel(String invokeID){

      NotificationHandler handler = remove( invokeID );

      if(handler != null){

         // cancel the timer that is waiting for this handler
         handler.cancelTimeoutTimer();

      }

      return handler;

   }

}

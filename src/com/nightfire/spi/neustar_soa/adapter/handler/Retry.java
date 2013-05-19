///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.adapter.Session;

/**
* This is the base class for tasks that need to be retried. Instances
* of this class will be added to the work queue where they
* will sleep in a worker thread for a given ammount of time before
* awakening to perform their retry attempt.  
*/
public abstract class Retry implements Runnable {

   /**
   * The period of time, in ms, that the retry task will sleep before
   * attempting a retry. 
   */
   private long waitPeriod;

   /**
   * The session object involved in this retry attempt. 
   */
   protected Session session;

   /**
   * The session ID of the session object at the time when this retry
   * task is created. This is used for comparison purposes. If the
   * session ID has changed when it comes time to perform the retry,
   * then the retry will be aborted, as the session ID changing
   * indicates that the session has been reinitialized. 
   */
   protected String originalSessionID;

   /**
   * Constructor.
   *
   * @param retryWait the ammount of time, in ms, that this task
   *                  should wait before performing the retry.
   * @param session the Session object involved in this retry. 
   */
   public Retry(long retryWait, Session session){

      waitPeriod = retryWait;
      this.session = session;

      originalSessionID = session.getSessionID();

   }

   /**
   * This defines the runnable interface. This method sleeps for the
   * configured wait time, and then calls retry() when the sleep
   * time has elapsed. If this task's session's ID has changed since
   * this task was created, this indicates that the session was
   * reinitialized, this retry task no longer applies, and will
   * be aborted.  
   */
   public void run(){

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

         Debug.log(Debug.MSG_STATUS,
                   "Waiting to perform retry ["+this+"] in ["+
                   waitPeriod+"] ms");

      }

      try{
         Thread.sleep( waitPeriod );
      }
      catch(InterruptedException iex){

         Debug.warning("Retry attempt ["+this+
                       "] was interrupted while waiting.");

      }

      // if the session ID has changed, that means that the session
      // has been reinitialized, and we should abort this retry attempt
      if(! originalSessionID.equals( session.getSessionID() ) ){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS,
                      "Retry attempt ["+this+
                      "] will be aborted, because the session invloved "+
                      "has been reinitialized.");
         }
         
      }
      else{

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS, "Performing retry attempt ["+this+"]");
         }

         // perform the retry
         retry();
      }

   }

   /**
   * This is implemented by subclasses to actually perform the retry work
   * after the wait period has expired. 
   */
   protected abstract void retry();

}
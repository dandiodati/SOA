///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import java.util.*;

import com.nightfire.framework.util.Debug;

/**
* This is a queue of Runnable implementations. Runnables added to the
* queue are dequeued by worker threads and executed.
*/
public class WorkQueue implements Runnable {

   /**
   * This is a static counter used in giving each thread a
   * unique ID number.
   */
   private static int workerThreadID = 0;

   /**
   * This list is used as the runnable queue implementation.
   */
   private List queue = new LinkedList( );

   // The current number of items in the queue list.
   private volatile int queueSize = 0;


   /**
   * This is the number of threads that are waiting for work.
   */
   private volatile int availableThreadCount = 0;

   /**
   * This is the number of threads that are currently executing a
   * Runnable instance.
   */
   private volatile int occupiedThreadCount = 0;

   /**
    * The total number of threads created.
    */
   private volatile int createdThreadCount = 0;

   /**
   * This is the maximum time that a thread should wait before
   * waking up and comtemplating offing itself.
   */
   private long maxWaitTime;

   /**
   * This is the maximum number of threads allowed to be waiting
   * around will nothing to do. If a worker thread's max wait time
   * expires, and it discovers that the number of waiting threads
   * exceeds the maxWaitingThreads, then the worker thread will
   * consider itself no longer of much use, and it will exit.
   */
   private int maxWaitingThreads;

   /**
    * The maximum number of threads to create.
    */
   private int maxThreads;

   /**
   * The run method will continue to loop while this flag is set to true.
   * kill() sets this to false.
   */
   private boolean alive = true;

   /**
   * The contructor. This uses a defalt max wait time of 10 minutes.
   *
   */
   public WorkQueue(){

      this(36000000, 10);

   }

   /**
   * The contructor.
   */
   public WorkQueue(long maxWaitTime, int maxWaitingThreads){

      this(maxWaitTime, maxWaitingThreads, maxWaitingThreads);

      if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){

         Debug.log(Debug.SYSTEM_CONFIG,
                   "Worker thread queue initialized:\n\tMaximum Wait Time ["+
                   maxWaitTime+"] ms\n\tMaximum Idle Threads ["+
                   maxWaitingThreads+"]\n");

      }

   }

   /**
   * The contructor.
   */
   public WorkQueue(long maxWaitTime,
                    int maxWaitingThreads,
                    int maxThreads){

      this.maxWaitTime = maxWaitTime;
      this.maxWaitingThreads = maxWaitingThreads;
      this.maxThreads = maxThreads;

      if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){

         Debug.log(Debug.SYSTEM_CONFIG,
                   "Worker thread queue initialized:\n\tMaximum Wait Time ["+
                   maxWaitTime+"] ms\n\tMaximum Number of Threads ["+
                   maxThreads+"]\n\tMaximum Idle Threads ["+
                   maxWaitingThreads+"]\n");

      }

   }

   /**
   * Adds the given Runnable to the queue.
   *
   * @param runMe a Runnable implementation to be executed.
   */
   public void enqueue(Runnable runMe){

      if( Debug.isLevelEnabled(Debug.THREAD_STATUS) ){
         Debug.log(Debug.THREAD_STATUS,
                   "Adding ["+runMe+"] to work queue: \n"+this);
      }

      boolean createThread = false;

      synchronized(this)
      {
         queue.add(runMe);

         queueSize = queue.size( );

         if(availableThreadCount > 0)
         {
            Debug.log(Debug.THREAD_STATUS, "Notifying worker threads of available work." );

            // notify the waiting threads
            notifyAll();
         }
         else if(createdThreadCount < maxThreads)
         {
            createThread = true;
         }
      }

      if(createThread)
      {
         Thread workerThread = new WorkerThread();

         if( Debug.isLevelEnabled(Debug.THREAD_STATUS) )
            Debug.log(Debug.THREAD_STATUS, "Starting thread: "+workerThread);

         workerThread.start();
      }
   }


   /**
    * While this queue is alive, this method loops, running any
    * Runnables in the queue and then waiting for more Runnables
    * to be added.
    */
   public void run()
   {
      // Indicate presence of new thread at start of execution.
      synchronized ( this )
      {
         createdThreadCount++;
      }

      try
      {
         while(alive)
         {
            Runnable runMe = null;

            synchronized ( this )
            {
               // Indicate that the thread is now available to perform new work.
               availableThreadCount++;

               // While this thread doesn't have a runnable to execute ...
               do
               {
                  // Exit loop if kill has been issued.
                  if ( !alive )
                     return;

                  queueSize = queue.size( );

                  // Check queue for runnables to work.
                  if ( queueSize > 0 )
                  {
                     runMe = (Runnable)queue.remove(0);

                     queueSize = queue.size( );

                     if(Debug.isLevelEnabled(Debug.THREAD_STATUS))
                        Debug.log(Debug.THREAD_STATUS, "Worker thread [" + Thread.currentThread() + "] got some work.  Queue size [" + queueSize + "]." );
                  }
                  else // No available work.
                  {
                     // There are too many threads and not enough work to do, so kill this thread.
                     if( availableThreadCount > maxWaitingThreads )
                     {
                        if(Debug.isLevelEnabled(Debug.THREAD_STATUS))
                           Debug.log(Debug.THREAD_STATUS, "Worker thread [" + Thread.currentThread() + "] exiting due to lack of work with excess waiting threads." );

                        return;  // Exit run loop here.
                     }

                     if(Debug.isLevelEnabled(Debug.THREAD_STATUS))
                        Debug.log(Debug.THREAD_STATUS, "Worker thread [" + Thread.currentThread() + "] waiting for work." );

                     // Wait until more runnables are added to the queue or until the maximum idle time has been reached.
                     // NOTE: Falling into this wait() free's up the lock on the work queue.
                     try
                     {
                        wait(maxWaitTime);

                        if(Debug.isLevelEnabled(Debug.THREAD_STATUS))
                           Debug.log(Debug.THREAD_STATUS, "Worker thread [" + Thread.currentThread() + "] just woke up from wait()." );
                     }
                     catch ( InterruptedException ie )
                     {
                        Debug.warning( "Worker thread wait was interrupted: " + ie.toString() );
                     }
                  } // End: No available work.
               } 
               while ( runMe == null );  // End: Loop waiting for runnable to execute.

               // Worker thread has become occupied.
               availableThreadCount--;
            } // End: synchronized block.


            occupiedThreadCount++;

           if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
                  Debug.log(Debug.THREAD_STATUS, "Executing runnable [" + runMe + "] in worker thread ...");

               try
               {
                  long startTime = System.currentTimeMillis( );

                  runMe.run();

                  if (Debug.isLevelEnabled(Debug.BENCHMARK))
                  {
                     Debug.log(Debug.BENCHMARK, "ELAPSED TIME [" + (System.currentTimeMillis() - startTime) + 
                               "] msec.  Worker thread execution of runnable [" + runMe + "]." );
                  }
               }
               catch(Throwable ex)
               {
                  // This should not happen, but just in case it does, we want to keep from breaking out of loop.
                  Debug.error( "Execution of [" + runMe + "] failed: " + ex.toString() );

                  // QUESTION: Shouldn't we put runMe back on the queue?  (May just continue to fail, so we'll skip for now.)
               }
            occupiedThreadCount--;
         } // End: while alive loop.
      }
      finally
      {
         synchronized( this )
         {
            availableThreadCount--;
            createdThreadCount--;
         }

         if(Debug.isLevelEnabled(Debug.THREAD_STATUS))
            Debug.log(Debug.THREAD_STATUS, "Thread " + Thread.currentThread() + " is exiting now.");
      }
   }


   /**
   * Returns whether or not this thread is alive and actively
   * executing enqueued runnables.
   */
   public boolean isAlive(){
      return alive;
   }

   /**
   * Sets the alive flag to false, causing run() to exit.
   */
   public synchronized void kill(){

      if(alive){

         Debug.log(Debug.NORMAL_STATUS, "Killing worker queue threads: "+this);

         alive = false;

         maxWaitingThreads = 0;

         // notify the worker threads to stop waiting so that run()
         // will then exit
         notifyAll();

      }

   }

   /**
   * This does some clean up by exiting the dequeue thread.
   */
   protected void finalize() throws Throwable{

      kill();

   }

   /**
   * This lists the number of threads available and number of runnables
   * queued.
   */
   public String toString(){

      StringBuffer buffer =
         new StringBuffer("Work Queue:\n\tRunnables Queued  [");
      buffer.append( queueSize );
      buffer.append("]\n\tThreads Available [");
      buffer.append(availableThreadCount);
      buffer.append("]\n\tThreads Occupied  [");
      buffer.append(occupiedThreadCount);
      buffer.append("]");

      return buffer.toString();

   }

   /**
   * This overrides thread to simply change the default label
   * for the thread. This makes it easier to identify these threads when
   * debugging.
   */
   protected class WorkerThread extends Thread{

      private int id;

      public WorkerThread(){

         // this thread will call WorkerQueue.run() when started
         super(WorkQueue.this);

         synchronized( WorkQueue.class )
         {
            id = workerThreadID++;
         }
      }

      /**
      * Returns a string that is the name of this class followed
      * by the ID for the thread being used to dequeue the runnables.
      *
      */
      public String toString(){

         return "Worker"+id+"-"+super.toString();

      }

   }

}

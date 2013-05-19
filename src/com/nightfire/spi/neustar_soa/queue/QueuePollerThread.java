////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.queue;

import java.util.*;

import com.nightfire.mgrcore.queue.*;
import com.nightfire.mgrcore.queue.agents.*;

import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;

/**
 * This is a thread that combines the functionality that was formerly
 * done by the a queue agent message processor and a timer. Instead
 * of needing to configured a separate timer and message processor
 * for each set of queue query criteria, several instances of this
 * class can be created programatically. Also, the sleep timer
 * in this class only sleeps if there are no more messages available in the
 * queue, as opposed to the timer com server which always sleeps between
 * DB queries regardless of whether there is more work to do.
 */
public class QueuePollerThread extends Thread{

   /**
    * The worker thread pool used to process messages found in the queue.
    */
   private QueueWorkerThreadPool threadPool;

   /**
    * The consumer instance used to retrieve messages from the queue.
    */
   private ConsumerBase consumer;

   /**
    * The SQL where clause tagged on to the end of the queue query.
    */
   private String whereCondition;

   /**
    * This policy tells the consumer what to do with a message
    * in the case of success, failure, etc.
    */
   private ConsumerPolicy policy;

   /**
    * These values are used
    */
   private Map values;

   /**
    * The message type found in the queue. Required by the queue infrastucture.
    */
   private String messageType;

   /**
    * This flag determines whether this poller should continue
    * querying the database or be shut down.
    */
   private boolean alive = true;

   /**
    * The ammount of time that this poller should sleep when it can
    * no longer find any more queued messages to process.
    */
   private long sleepInterval;

   /**
    * This is just a label used to identify this instance in the logs.
    */
   private String threadLabel;

   //add priority in orderby clause
   private final static String PRIORITY = "priority,";
   
   /**
    *
    *
    * @param consumer ConsumerBase The consumer instance used to retrieve
    *                              messages from the queue.
    * @param messageType String The message type.
    * @param policy ConsumerPolicy
    * @param whereCondition String The SQL where clause tagged on to the end
    *                              of the queue query.
    * @param values The message type values used to query the queue.
    * @param maxThreadCount int the maximum number of worker threads used
    *                           to process messages from the queue.
    * @param pollingInterval long the ammount of time, in ms, that this
    *                             thread will sleep when there are no messages
    *                             to be processed.
    * @throws Exception if the internal worker thread pool could not be
    *                   initialized.
    */
   public QueuePollerThread(ConsumerBase consumer,
                            String messageType,
                            String whereCondition,
                            Map values,
                            int maxThreadCount,
                            long pollingInterval )
                            throws Exception{

      this.consumer = consumer;
      this.messageType = messageType;
      this.policy = consumer.getConsumerPolicy(messageType);
      this.whereCondition = whereCondition;
      this.values = values;
      this.sleepInterval = pollingInterval;

      // this is just a label used to identify this instance in the logs
      threadLabel = "["+messageType+"] Poller Thread - ["+
                    values+"]-";

      try{

         threadPool =
            new QueueWorkerThreadPool("["+messageType+
                                      "] Worker Thread Pool - ["+
                                      values+"]", false);

         // set the maximum time to wait for a thread to become available
         // to 1 hour
         threadPool.setMaxWaitTime( 3600 );
         // set the maximum number of worker threads used
         threadPool.setMaxThreads(maxThreadCount);

      }
      catch(Exception ex){

         Debug.error("Could not initialize thread pool.");
         throw ex;

      }

   }

   /**
    * This polls the queue for new messages to process. If no messages
    * are found, this will sleep and then try again.
    */
   public void run(){

      if(Debug.isLevelEnabled(Debug.NORMAL_STATUS)){
         Debug.log(Debug.NORMAL_STATUS, "Thread started: "+this);
      }

      try{

         // the query that goes to the NPAC queue is not meant to be
         // specific to any customer, so the ID in the customer context
         // is cleared.
         CustomerContext.getInstance().setNoCustomer();

      }
      catch(Exception ex){
         Debug.error("Could not clear customer context: "+ex);
      }

      while( alive ){

         try{

            // the queueing infrastructure requires that the queue
            // instance in the consumer get reset each time we
            // want to query the DB
            resetConsumer();

            MessageQueue queue = consumer.getQueue();

            // if there are more messages in the DB queue
            if( queue.hasNext() ) {

               if( Debug.isLevelEnabled(Debug.IO_STATUS) ){
                  Debug.log(Debug.IO_STATUS,
                            this+
                            " continuing to process messages from queue.");
               }

               // hand consumer off to worker threads
               threadPool.process( queue, consumer, policy );

            }
            else {

               if( Debug.isLevelEnabled(Debug.IO_STATUS) ){
                  Debug.log(Debug.IO_STATUS,
                            this+
                            " sleeping.");
               }

               // There aren't any more messages at this time.
               // Take a little rest.
               Thread.sleep(sleepInterval);

            }

         }
         catch(InterruptedException iex){
            Debug.warning("The poller thread ["+this+"] was interrupted.");
         }
         catch(Throwable oops){
            Debug.error("An error occured in poller thread: "+this);
            Debug.logStackTrace(oops);
         }

      }


   }

   /**
    * This calls setDequeueCriteria() to reinitialize the Queue instance
    * in the consumer. This must be done each time before the consumer queries
    * the DB.
    */
   protected void resetConsumer() throws QueueException{

      // this map may be modified by the consumer, so create a working copy
      Map queryValues = new HashMap(values);

      //default orderby clause 
      String orderBy = QueueConstants.ID_COL;
      
      // set priority in orderby clause for SOA_QUEUE table or MessageType="soa"
      if(SOAMessageType.SOA_MESSAGE_TYPE.equals(messageType))
      {
    	  orderBy = PRIORITY+QueueConstants.ID_COL;
      }
      
      // poll only for our particular message type and order by queue ID column
      if (whereCondition == null) {
    	  // added priority in order by, for SOA_QUEUE table or MessageType="soa"
         consumer.setDequeueCriteria(messageType,
                                     queryValues,
                                     orderBy);

      }
      else {

         /*// include the optional where clause if we've got one
         consumer.setDequeueCriteria(messageType,
                                     queryValues,
                                     QueueConstants.ID_COL,
                                     whereCondition);
         */
    	  // added priority in order by, for SOA_QUEUE table or MessageType="soa"
    	  consumer.setDequeueCriteria(messageType,
                 queryValues,
                 orderBy,
                 whereCondition);


      }

   }

   /**
    * This method causes the run() method to exit and shutsdown the
    * worker thread pool being used to consumer queued messages.
    */
   public void shutdown(){

      alive = false;
      threadPool.shutdown();

      // interrupt this thread if it is sleeping
      interrupt();

   }


   /**
    * Returns a label for this thread.
    *
    * @return String
    */
   public String toString(){
      return threadLabel+super.toString();
   }

}

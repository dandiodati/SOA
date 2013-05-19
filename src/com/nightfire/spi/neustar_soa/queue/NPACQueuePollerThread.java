////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.queue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.HashMap;

import com.nightfire.mgrcore.queue.*;
import com.nightfire.mgrcore.queue.agents.*;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;


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
public class NPACQueuePollerThread extends Thread{

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
   
   /**
    * This represents the connectivity key
    */
    private String connectivityKey;
    
    /**
     * This represents the spid value
     */
     private String spid;
   

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
     
   public NPACQueuePollerThread(ConsumerBase consumer,
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
      this.spid = (String)values.get("SPID");
              
      // this is just a label used to identify this instance in the logs
      threadLabel = "["+messageType+"] Poller Thread - ["+
                    values+"]-";
      if(spid != null){
    	  this.connectivityKey = NPACQueueUtils.getConnectivityKey(spid);
    	  if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"intializing NPACQueuePollerThread for spid :["
								+ spid
								+ "] and Connectivity : ["
								+ connectivityKey + "]");
			}
		} 

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
    	  if(isAllRegionsConnected())
    	  {

         try{

            // the queueing infrastructure requires that the queue
            // instance in the consumer get reset each time we
            // want to query the DB
            resetConsumer();

            MessageQueue queue = consumer.getQueue();

            // if there are more messages in the DB queue
            if( queue.hasNext()) {

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
    	  else
    	  {
    		  try
    		  {
    		  Thread.sleep(sleepInterval);
    		  }
    		  catch(InterruptedException e)
    		  {
    			  e.printStackTrace();
    		  }
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

      // poll only for our particular message type and order by queue ID column
      if (whereCondition == null) {

         consumer.setDequeueCriteria(messageType,
                                     queryValues,
                                     QueueConstants.ID_COL);

      }
      else {

         // include the optional where clause if we've got one
         consumer.setDequeueCriteria(messageType,
                                     queryValues,
                                     QueueConstants.ID_COL,
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

	private boolean isAllRegionsConnected(){
		
	   Connection conn=null;

 	   PreparedStatement ps=null;

	   ResultSet rs=null;
	   
	   String connInstance="";
	   
	   boolean connCheck = false;
	   
	   String NPACASSOCIATION_STATUS;
	   
	   if(connectivityKey != null && connectivityKey.length()>16){
		   connInstance = connectivityKey.substring(17) ;
		   if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"isAllRegionsConnected Connectivity instance :[" + connInstance
								+ "]");
			}
		   
	   }
	  
	   for (int i=0;i<=NPACConstants.NPACASSOCIATION_STATUS_ARR.length-1;i++)
	   {
		   if(connInstance.equals(""))
			   connCheck = NPACConstants.NPACASSOCIATION_STATUS_ARR[0];
		   else if((Integer.parseInt(connInstance))==i)
			   connCheck = NPACConstants.NPACASSOCIATION_STATUS_ARR[i];
	   }	   	   	

 	   boolean recoveryPoll=false;

	   if(!connCheck)
	   {
			if (connectivityKey == null) {

				try {
					conn = DBInterface.acquireConnection();

					ps = conn
							.prepareStatement(SOAQueryConstants.REGION_RECOVERY_STATUS_NULL);

					// Record count of all the configured regions.
					rs = ps.executeQuery();

					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug
								.log(
										Debug.MSG_STATUS,
										"Executing region recovery status :["
												+ SOAQueryConstants.REGION_RECOVERY_STATUS_NULL
												+ "] for connectivity :["
												+ connectivityKey + "]");
					}
					
					if (rs.next()) {
						int recCount = rs.getInt(1);

						Debug.log(Debug.SYSTEM_CONFIG, "Record count is ="
								+ recCount);

						if (recCount == 0) {
							recoveryPoll = true;							
							for (int i=0;i<=NPACConstants.NPACASSOCIATION_STATUS_ARR.length-1;i++)
							   {
								   if(connInstance.equals(""))
									   NPACConstants.NPACASSOCIATION_STATUS_ARR[0] = true ;
								   else if((Integer.parseInt(connInstance))==i)
									   NPACConstants.NPACASSOCIATION_STATUS_ARR[i] = true;
							   }
						}
					}

				} catch (Exception ex) {
					Debug.log(Debug.SYSTEM_CONFIG, "Exception :"
							+ ex.toString());
				} finally {
					try {
						if (ps != null)
							ps.close();

						if (rs != null)
							rs.close();

						if (conn != null)
							DBInterface.releaseConnection(conn);
					} catch (Exception dbEx) {
						Debug.log(Debug.SYSTEM_CONFIG, "Exception :"
								+ dbEx.toString());
					}
				}

			} else {
				try {
					conn = DBInterface.acquireConnection();

					ps = conn
							.prepareStatement(SOAQueryConstants.REGION_RECOVERY_STATUS);

					
					ps.setString(1, connectivityKey);
					// Record count of all the configured regions.
					rs = ps.executeQuery();

					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug
								.log(
										Debug.MSG_STATUS,
										"Executing region recovery status :["
												+ SOAQueryConstants.REGION_RECOVERY_STATUS
												+ "] for connectivity :["
												+ connectivityKey + "]");
					}

					if (rs.next()) {
						int recCount = rs.getInt(1);

						Debug.log(Debug.SYSTEM_CONFIG, "Record count is ="
								+ recCount);

						if (recCount == 0) {
							recoveryPoll = true;							
							for (int i=0;i<=NPACConstants.NPACASSOCIATION_STATUS_ARR.length-1;i++)
							   {
								   if(connInstance.equals(""))
									   NPACConstants.NPACASSOCIATION_STATUS_ARR[0] = true ;
								   else if((Integer.parseInt(connInstance))==i)
									   NPACConstants.NPACASSOCIATION_STATUS_ARR[i] = true;
							   }
							
							
						}
					}					

				} catch (Exception ex) {
					Debug.log(Debug.SYSTEM_CONFIG, "Exception :"
							+ ex.toString());
				} finally {
					try {
						if (ps != null)
							ps.close();

						if (rs != null)
							rs.close();

						if (conn != null)
							DBInterface.releaseConnection(conn);
					} catch (Exception dbEx) {
						Debug.log(Debug.SYSTEM_CONFIG, "Exception :"
								+ dbEx.toString());
					}
				}
			}

		}
		else
		{
			recoveryPoll=true;
		}
	  
 	   return recoveryPoll;
 	   
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

///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.smartsockets;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.smartsockets.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class SmartSocketsReceiver implements Runnable {

   /**
    * This is the interface that this class will pass the incoming messages
    * off to for processing.
    */
   private Receiver receiver;

   /**
    * The subject to which this receiver is subscribed.
    */
   private String subject;

   /**
    * A reference to the SmartSockets RT Server connection.
    */
   private TipcSrv connection;

   /**
    * The work queue which is a list of threads that will receive messages.
    */
   private WorkQueue workQ;

   /**
    * While alive is true, the receiver will continue to try to read
    * the next message.
    */
   private boolean alive = true;


   /**
    * This Constructor creates a SmartSockets connection that doesn't use GMD.
    * It is called by sub-classes only.
    *
    */
   protected SmartSocketsReceiver(String project,
                                  String subject,
                                  String uniqueSubject,
                                  Receiver receiver,
                                  String username,
                                  String password,
                                  String server,
                                  int workerThreadCount)
                                  throws FrameworkException{

      this(project,
           subject,
           uniqueSubject,
           receiver,
           username,
           password,
           server,
           new WorkQueue( 36000000, workerThreadCount ) );

   }


   /**
    * This Constructor creates a SmartSockets connection that doesn't use GMD.
    * It is called by sub-classes only.
    *
    */
   protected SmartSocketsReceiver(String project,
                                  String subject,
                                  String uniqueSubject,
                                  Receiver receiver,
                                  String username,
                                  String password,
                                  String server,
                                  WorkQueue queue)
                                  throws FrameworkException{


      Debug.log(Debug.SYSTEM_CONFIG, "SmartSocketsReceiver: Initializing...");

      this.subject = subject;
      this.receiver = receiver;

      this.workQ = queue;

      // Create connection object
      connection = TipcSvc.createSrv();

      // Load options.
      try{

         if (username != null && password != null){
            connection.setUsernamePassword(username, password);
         }

         if (project != null){
            connection.setOption("ss.project", project);
         }

         if (server != null){
            connection.setOption("ss.server_names", server);
         }

         if (uniqueSubject != null){
            connection.setOption("ss.unique_subject", uniqueSubject);
         }

      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsClient: Could not set options: " + ssex);
         throw new FrameworkException("Could not set options.");

      }

      // run a thread in the background to receive incoming messages
      Thread receiverThread = new Thread(this){

         /**
          * This will identify this thread for logging purposes.
          */
         public String toString(){
            return "Receiver for Subject ["+SmartSocketsReceiver.this.subject+
                   "]-"+super.toString();
         }

      };
      receiverThread.start();

   }

   public void run(){

       Debug.log(Debug.MSG_STATUS,
                 "SmartSocketsReceiver: Starting background thread for subject: "+
                 subject);

      // subscribe to receive messages
      try{

         Debug.log(Debug.NORMAL_STATUS,
                   "Subscribing to subject ["+
                   subject+"]");
         connection.setSubjectSubscribe(subject, true);

      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsReceiver: Could not subscribe to the subject: " +
                     ssex);
         Debug.logStackTrace(ssex);

      }

      Debug.log(Debug.MSG_STATUS,
                "SmartSocketsReceiver: Begin reading messages");

      while(alive){

         TipcMsg msg;

         try {

            msg = connection.next(30);

            if (msg != null)
            {

              msg.setCurrent(0);

              // Clean this up as time allows - extract the parsing to message
              // type specific classes
              if (msg.getType().getName().equalsIgnoreCase(NPACSmartSocketsClient.NPAC_MESSAGE_TYPE))
              {
                  String spid = msg.nextStr();  // Do we need to pass this on? - RLP
              }

              String xml = msg.nextXml().getStr();

              // log the incoming message
              Debug.log(Debug.IO_STATUS,
                        "Received message for subject ["+subject+"]:\n" +
                        xml);

              CallbackRunnable runnable = new CallbackRunnable(receiver, xml);

              // This passes the runnable off to a separate worker thread so
              // that this loop can get back to listening for messages.
              workQ.enqueue(runnable);
            }
         }
         catch (TipcException ssex) {

           Debug.error("Error occured while getting next message: " + ssex);

         }
      }
   }

   /**
    * This is called when the background thread is being shutdown in order to
    * cleanup any resources.
    */
   public void cleanup(){

       Debug.log(Debug.MSG_STATUS, "SmartSocketsReceiver: Cleaning up background thread.");

      // kill the loop in the run() method
      alive = false;

      // clean out the work queue
      workQ.kill();

      // unsubscribe from the subject
      try{

         connection.setSubjectSubscribe(subject, false);

      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsReceiver: Could not unsubscribe from the subject: " + ssex);

      }

      try{

         connection.destroy();

      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsReceiver: Could not destroy Smart Sockets reference: " + ssex);

      }
   }

   /**
    * This class is passed off to the "RunnableEngine" so that its
    * run() method with get called in a separate worker thread, and this
    * receiver thread will not be blocked while the incoming message
    * gets processed.
    */
   private class CallbackRunnable implements Runnable{

      /**
       * A reference to the receiver whose process() method will get called.
       */
      private Receiver receiver;

      /**
       * The incoming XML message itself, received via SmartSockets.
       */
      private String message;

      public CallbackRunnable(Receiver receiver, String message){

         this.receiver = receiver;
         this.message = message;

      }

      public void run(){

         receiver.process( message );

      }
   }
}

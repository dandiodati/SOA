///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.adapter.smartsockets;

import com.smartsockets.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public abstract class SmartSocketsClient {

   /**
    * The Smart Sockets' subject to which messages should be sent.
    */
   private String subject;

   /**
    * The subject to which message responses should be published.
    */
   private String replyTo;

   /**
    * The message type to be created and used.
    */
   protected TipcMt messageType;

   /**
    * A reference to the SmartSockets RT Server connection.
    */
   private TipcSrv connection;


   /**
    * This Constructor creates a SmartSockets connection that doesn't use GMD.
    * It is called by sub-classes only.
    *
    * Constructor does the following:
    *
    *   1. Creates or retrieves proper message type
    *   2. Creates the connection object
    *   3. Loads the appropriate options
    *   4. Connects to RT Server
    *
    */
   protected SmartSocketsClient(String project,
                                String subject,
                                String username,
                                String password,
                                String server,
                                String messageTypeName,
                                int    messageTypeNumber,
                                String messageTypeParams,
                                int    loadBalancingMode,
                                String replyTo)
                                throws FrameworkException{

      Debug.log(Debug.SYSTEM_CONFIG,
                "SmartSocketsClient: Initializing...");

      this.subject = subject;
      this.replyTo = replyTo;

      messageType = createMsgType(messageTypeName,
                                  messageTypeNumber,
                                  messageTypeParams,
                                  loadBalancingMode);

      // Create connection object
      connection = TipcSvc.createSrv();

      // Load options.
      try{

         if (project != null){
            connection.setOption("ss.project", project);
         }

         if (server != null){
            connection.setOption("ss.server_names", server);
         }

         if (username != null && password != null){
            connection.setUsernamePassword(username, password);
         }

      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsClient: Could not set options: " + ssex);
         throw new FrameworkException("Could not set options.");

      }

      // Now connect to RTServer
      try{

         Debug.log(Debug.SYSTEM_CONFIG, "SmartSocketsClient: Connecting to RTServer");
         connection.create();

      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsClient: Could not connect to RTServer: " + ssex);
         throw new FrameworkException("Could not connect to RTServer.");

      }
   }

   /**
    * This sends the given XML string. This assumes that
    * This client's message type takes a single XML parameter.
    *
    * @param message String the XML message to be sent.
    */
   public void sendXML(String message) throws FrameworkException{

      // log the incoming message
      Debug.log(Debug.IO_STATUS,
                "Sending XML message to subject [" + subject +
                "]:\n" + message);

      TipcMsg msg = TipcSvc.createMsg( messageType );

      TutXml xml = new TutXml(message);
      msg.appendXml(xml);

      send(msg);

   }

   public void cleanup(){

      Debug.log(Debug.SYSTEM_CONFIG, "SmartSocketsClient: Cleaning Up...");

      try{

         connection.destroy();

      }
      catch(TipcException ssex){

         Debug.error("Could not destroy Smart Sockets reference: " + ssex);

      }
   }

   protected void send(TipcMsg msg) throws FrameworkException{

      // log the incoming message
      Debug.log(Debug.IO_STATUS,
               "Sending message to subject [" +
               subject + "]:\n" + msg);

      try{

         msg.setDest(subject);

         // set the subject where the message reply should be published
         if( replyTo != null ){

            msg.setReplyTo( replyTo );

         }

         connection.send(msg);
         connection.flush();
      }
      catch(TipcException ssex){

         Debug.error("SmartSocketsClient: Send failed for subject: " +
            subject + ": " + ssex);
         throw new FrameworkException("Send failed for subject: " + subject);

      }

   }

   protected TipcMt createMsgType(String messageTypeName,
                                  int messageTypeNumber,
                                  String messageTypeParams,
                                  int loadBalancingMode)
                                  throws FrameworkException{

      if( messageType != null ){

         return messageType;

      }

      // get the message type instance if it already exists
      messageType = TipcSvc.lookupMt(messageTypeName);

      // if the type does not exist, then create it
      if( messageType == null ){

         try {

            messageType = TipcSvc.createMt(messageTypeName,
                                           messageTypeNumber,
                                           messageTypeParams);

            // set load-balance mode
            messageType.setLbMode( loadBalancingMode );

         }
         catch (Exception ssex) {

            Debug.error("Could not create message type. Type [" +
                        messageTypeName + "], Number [" + messageTypeNumber +
                        "], Parameters [" + messageTypeParams + "]: " +
                        ssex);

            throw new FrameworkException("Could not create message type.");

         }

      }

      return messageType;

   }

}

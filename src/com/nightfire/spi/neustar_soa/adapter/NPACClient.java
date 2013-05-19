///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003-2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.FileUtils;

import com.nightfire.spi.neustar_soa.adapter.smartsockets.NPACSmartSocketsClient;

/**
* This is the client used to send messages to the NPAC's OSS Gateway
* via SmartSockets.
* The parent class also takes care of generating the outgoing XML messages in the
* correct format. Methods are provided that will generate the appropriate
* XML message based on given input parameters.
*/
public class NPACClient extends NPACClientBase{

   /**
    * Used to send NPAC requests via SmartSockets.
    */
   private NPACSmartSocketsClient client;

   /**
    *
    * @param project String the name of the SmartsSockets project.
    * @param subject String the name of the SmartsSockets subject.
    * @param user String the SmartsSockets user name used to log into
    *                    the NPAC OSS GW. This should not be confused
    *                    with clearinghouse user names used with the API.
    * @param password String the password for the given user used to log into
    *                    the NPAC OSS GW.
    * @param server String the host or hosts where SmartSockets will look for
    *                      its RTServer process.
    *
    * @throws FrameworkException thrown if the SmartSockets connection cannot
    *                            be initialized.
    */
   public NPACClient(String project,
                     String subject,
                     String user,
                     String password,
                     String server,
                     String replyTo)
                     throws FrameworkException{

      // create a SmartSockets client to which the requests will
      // be delegated for delivery
      client = new NPACSmartSocketsClient( project,
                                           subject,
                                           user,
                                           password,
                                           server,
                                           replyTo);

   }


    /**
    * Sends the XML message to the NPAC via SmartSockets.
    */
    public int send( String spid, String xml ){

       int result = NPACConstants.ACK_RESPONSE;

       try{
          client.send(spid, xml);
       }
       catch(FrameworkException fex){

          Debug.error("Could not send message ["+xml+
                      "] for SPID ["+spid+"]: "+fex );

          result = NPACConstants.NACK_RESPONSE;

       }

       return result;

    }

    /**
     * This cleans up any SmartSockets resources that are in use.
     * This is to be called only when the client is being shut down.
     */
    public void cleanup(){

      client.cleanup();

   }

   // For testing purposes only
   public static void main(String[] args){

      // e.g. "OSSGW_DEV1"
      String project = args[0];
      // e.g. "SOA_88_X001"
      String subject = args[1];

      String spid    = args[2];

      String xmlFile = args[3];

      String user = args[4];

      String password = args[5];

      String replyTo = null;

      if(args[6] != null){

         replyTo = args[6];

      }

      String server = null;

      Debug.setThreadLogFileName("clientLog.txt");
      Debug.log(Debug.MSG_STATUS, "Starting NPACClient main");
      Debug.enableAll();

      try{

         NPACClient npacClient = new NPACClient(project,
                                                subject,
                                                user,
                                                password,
                                                server,
                                                replyTo);

         String xml = FileUtils.readFile( xmlFile );

         Debug.log(Debug.IO_STATUS,
                   "Sending SPID ["+spid+
                   "] and XML:\n"+xml );

         npacClient.send( spid, xml );

         npacClient.cleanup();

      }
      catch(FrameworkException fwex){

         System.out.println("Error Sending Message");
         Debug.log(Debug.MSG_ERROR, "Error Sending Message: " + fwex);
         Debug.logStackTrace(fwex);

      }

   }

}


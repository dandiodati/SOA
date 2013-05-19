///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.smartsockets;

import com.smartsockets.TipcDefs;

import com.smartsockets.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class NPACSmartSocketsClient extends SmartSocketsClient{

   public static final String NPAC_MESSAGE_TYPE = "NPAC_OSSGW_MESSAGE";
   private static final int NPAC_MESSAGE_TYPE_NUMBER = 10000;
   private static final String NPAC_MESSAGE_TYPE_PARAMS = "str xml";
   private static final int NPAC_LOAD_BALANCE_MODE = TipcDefs.LB_NONE;

   public NPACSmartSocketsClient(String project,
                                 String subject,
                                 String username,
                                 String password,
                                 String server,
                                 String replyToSubject)
                                 throws FrameworkException{

      super( project,
             subject,
             username,
             password,
             server,
             NPAC_MESSAGE_TYPE,
             NPAC_MESSAGE_TYPE_NUMBER,
             NPAC_MESSAGE_TYPE_PARAMS,
             NPAC_LOAD_BALANCE_MODE,
             replyToSubject );

   }

   /**
    *
    * @param spid String the service provider ID to be used as the first
    *                    parameter of the message.
    * @param message String the NPAC XML message to be sent.
    */
   public void send(String spid, String message)
                     throws FrameworkException{

      // log the incoming message
      Debug.log(Debug.IO_STATUS,
                "Sending message to NPAC for spid [" +
                spid + "]:\n" + message);

      TipcMsg msg = TipcSvc.createMsg( messageType );

      msg.appendStr(spid);

      TutXml xml = new TutXml(message);
      msg.appendXml(xml);

      super.send( msg );

   }

   // For testing purposes only
   public static void main(String[] argv){

      System.out.println("Starting NPACClient main");

      NPACSmartSocketsClient client = null;

      String project = "OSSGW_DEV1";
      String subject = "OSSGW_0001";
      String username = "ossgwclient";
      String password = "unknown";
      String server = "queen";
      String replyTo = "SOA_88_X001_US";

      Debug.setThreadLogFileName("clientLog.txt");
      Debug.enableAll();

      try{

         client = new NPACSmartSocketsClient(project,
                                             subject,
                                             username,
                                             password,
                                             server,
                                             replyTo);

         for (int i=0; i<10; i++)
         {
            client.send("SPID001", "XML Message" + i);
         }

         client.cleanup();

      }
      catch(FrameworkException fwex){

         System.out.println("Error Sending Messages");
         Debug.log(Debug.MSG_ERROR, "Error Sending Messages: " + fwex);

      }
   }
}

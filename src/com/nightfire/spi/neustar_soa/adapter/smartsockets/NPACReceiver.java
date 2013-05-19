///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.smartsockets;

import com.nightfire.spi.neustar_soa.adapter.WorkQueue;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class NPACReceiver extends SmartSocketsReceiver {

   public NPACReceiver(String project,
                       String subject,
                       String uniqueSubject,
                       String username,
                       String password,
                       String server,
                       Receiver receiver,
                       int maxWorkerThreadCount)
                       throws FrameworkException{

      super (project,
             subject,
             uniqueSubject,
             receiver,
             username,
             password,
             server,
             maxWorkerThreadCount);

   }


   public NPACReceiver(String project,
                       String subject,
                       String uniqueSubject,
                       String username,
                       String password,
                       String server,
                       Receiver receiver,
                       WorkQueue queue)
                       throws FrameworkException{

      super (project,
             subject,
             uniqueSubject,
             receiver,
             username,
             password,
             server,
             queue);

   }

   // The following is for testing purposes only
   public static void main(String[] argv){

      System.out.println("Starting NPACReceiver main");

      NPACReceiver receiver = null;

      String project = "OSSGW_DEV1";
      String subject = "SOA_88_X001";
      String uniqueSubject = "SOA_88_X001_US";
      String username = "ossgwclient";
      String password = "unknown";
      String server = null;

      class TestProcessor implements Receiver
      {

        public int process(String message){

          Debug.log(Debug.MSG_STATUS, "Processor received: " + message);
          return 1;

        }
      }

      Debug.setThreadLogFileName("receiverLog.txt");
      Debug.log(Debug.MSG_STATUS, "Starting NPACReceiver main");
      Debug.enableAll();

      TestProcessor testProcessor = new TestProcessor();

      try{

          receiver = new NPACReceiver(project,
                                      subject,
                                      uniqueSubject,
                                      username,
                                      password,
                                      server,
                                      testProcessor,
                                      10);

      }
      catch(FrameworkException fwex){

         System.out.println("Error Creating Receiver");
         Debug.log(Debug.MSG_ERROR, "Error Creating Receiver: " + fwex);

      }

      try {
        Thread.sleep(60000);
      }
      catch (InterruptedException ie) {
        Debug.log(Debug.MSG_ERROR, "Sleep interrupted");
      }
      if(receiver != null)
    	  receiver.cleanup();

   }
}

///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.smartsockets;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

import java.util.GregorianCalendar;

public class SOAReceiver extends SmartSocketsReceiver {

   public SOAReceiver(String project,
                      String subject,
                      String server,
                      Receiver receiver,
                      int maxWorkerThreadCount)
                      throws FrameworkException{

      super (project,
             subject,
             null,
             receiver,
             null,
             null,
             server,
             maxWorkerThreadCount);

   }

   // The following is for testing purposes only
   public static void main(String[] argv){

      System.out.println("Starting SOAReceiver main");

      SOAReceiver receiver = null;

      String project = "SOA_DEV1";
      String subject = "SOA_0001";
      String server = null;

      class TestProcessor implements Receiver
      {

        public int process(String message){

          Debug.log(Debug.MSG_STATUS, "Processor received: " + message);
          return 1;

        }
      }

      GregorianCalendar cal = new GregorianCalendar();
      Debug.setThreadLogFileName(
        "soareceiverLog" + Long.toString(cal.getTimeInMillis()) + ".txt");
      Debug.log(Debug.MSG_STATUS, "Starting NPACReceiver main");
      Debug.enableAll();

      TestProcessor testProcessor = new TestProcessor();

      try{

          receiver = new SOAReceiver(project,
                                      subject,
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

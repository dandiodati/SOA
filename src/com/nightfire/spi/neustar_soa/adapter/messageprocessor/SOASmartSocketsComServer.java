////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.adapter.smartsockets.Receiver;
import com.nightfire.spi.neustar_soa.adapter.smartsockets.SOAReceiver;

import com.nightfire.spi.common.communications.*;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;

public class SOASmartSocketsComServer extends ComServerBase
                                      implements Receiver{

   public static final String SOA_PROJECT_PROP = "SOA_INTERNAL_PROJECT";

   public static final String SOA_SUBJECT_PROP = "SOA_INTERNAL_SUBJECT";

   public static final String SOA_RTSERVER_PROP = "SOA_RTSERVER";

   private SOAReceiver receiver;

   public SOASmartSocketsComServer(String key, String type)
      throws ProcessingException {

      super(key, type);

      String project = super.getRequiredPropertyValue( SOA_PROJECT_PROP );
      String subject = super.getRequiredPropertyValue( SOA_SUBJECT_PROP );
      String server  = super.getRequiredPropertyValue( SOA_RTSERVER_PROP );

      try{
         receiver = new SOAReceiver(project,
                                    subject,
                                    server,
                                    this,
                                    10);
      }
      catch(FrameworkException fex){

         throw new ProcessingException( fex.getMessage() );

      }

   }

   /**
    * This will wait for incoming messages via SmartSockets.
    */
   public void run() {
	   
	  ThreadMonitor.ThreadInfo tmti = null;
	   
	  try
	  {
		 tmti = ThreadMonitor.start( "Waiting for Incoming Messages" );

		  receiver.run();
	  }
	  finally
	  {
		  ThreadMonitor.stop(tmti);
	  }

   }

   /**
    * This will cleanup any communication resources in use.
    */
   public void shutdown() {

      receiver.cleanup();

   }

   /**
    * The SOAReceiver calls back to this method when a message is received.
    * This defines the Receiver interface.
    *
    * @param xml String the XML message received via SmartSockets.
    * @return int an ACK or NACK based on whether processing
    *             was successful.
    */
   public int process( String xml ){

      int ack = NPACConstants.NACK_RESPONSE;

      try{

         // pass message off to driver chain for processing
         super.process("", xml);

         ack = NPACConstants.ACK_RESPONSE;

      }
      catch(Exception ex){

         Debug.error("Errors occurred while processing:\n"+
                     xml+"\n"+
                     ex.toString());

      }

      return ack;

   }

}

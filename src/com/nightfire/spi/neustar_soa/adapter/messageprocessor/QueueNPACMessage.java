////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.Debug;

import com.nightfire.mgrcore.queue.QueueException;

import com.nightfire.spi.neustar_soa.queue.NPACMessageProducer;

/**
 * This message processor is used to queue an outbound NPAC request message.
 */
public class QueueNPACMessage extends MessageProcessorBase {

  public static final String INVOKE_ID_LOC_PROP = "INVOKE_ID_LOCATION";

  public static final String TABLE_NAME_LOC_PROP = "TABLE_NAME_LOCATION";

  public static final String MESSAGE_LOC_PROP = "MESSAGE_LOCATION";

  public static final String SPID_LOC_PROP = "SPID_LOCATION";

  private String invokeIDLoc;

  private String tableNameLoc;

  private String messageLoc;

  private String spidLoc;

  public void initialize(String key, String type)
                         throws ProcessingException {

     super.initialize(key, type);

     invokeIDLoc  = getRequiredPropertyValue(INVOKE_ID_LOC_PROP);
     tableNameLoc = getRequiredPropertyValue(TABLE_NAME_LOC_PROP);
     messageLoc   = getRequiredPropertyValue(MESSAGE_LOC_PROP);
     spidLoc      = getRequiredPropertyValue(SPID_LOC_PROP);

  }

  /**
   * Uses the configured location values to extract the message
   * contents and then queues this message.
   */
  public NVPair[] process(MessageProcessorContext context,
                          MessageObject message)
                          throws MessageException,
                                 ProcessingException
  {

	  ThreadMonitor.ThreadInfo tmti = null;
      // required response to a null input message
      if( message == null ){
         return null;
      }

      try{
    	  
    	 tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
    	  
         NPACMessageProducer producer = new NPACMessageProducer();

         String invokeID = getString(invokeIDLoc, context, message);
         String tableName = getString(tableNameLoc, context, message);
         String npacMessage = getString(messageLoc, context, message);
         String spid = getString(spidLoc, context, message);

         int invokeIDValue;

         try{
            invokeIDValue = Integer.parseInt(invokeID);
         }
         catch(NumberFormatException nfex){
            throw new MessageException("Invoke ID ["+invokeID+
                                       "] is not a valid integer.");
         }

         producer.enqueue(invokeIDValue,
                          tableName,
                          npacMessage,
                          spid);

      }
      catch(QueueException qex){

         Debug.logStackTrace(qex);
         throw new ProcessingException( qex.getMessage() );

      }
      
      finally
      {
    	ThreadMonitor.stop(tmti);  
      }

      // This returns the original message object unchanged.
      return formatNVPair(message);

  }

}

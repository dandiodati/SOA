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

import com.nightfire.spi.neustar_soa.queue.SOAMessageProducer;

/**
 * This message processor is used to queue an inbound NPAC reply or
 * notification message bound for the SOA.
 */
public class QueueSOAMessage extends MessageProcessorBase {

   /**
    * The name of the property indicating the location of the
    * message to be queued.
    */
   public static final String MESSAGE_LOC_PROP = "MESSAGE_LOCATION";

  /**
   * The value of the MESSAGE_LOCATION property, indicating where the
   * input message is.
   */
  private String messageLoc;

  /**
   * Gets the input message location from properties.
   *
   * @param key String the property key used to load properties from the DB.
   * @param type String the property type used to load properties from the DB.
   * @throws ProcessingException
   */
  public void initialize(String key, String type)
                         throws ProcessingException {

     super.initialize(key, type);

     // Get the message location from properties. Default to
     // use the input message itself.
     messageLoc = getPropertyValue( MESSAGE_LOC_PROP, INPUT_MESSAGE );

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
    	 
         SOAMessageProducer producer = new SOAMessageProducer();

         String npacMessage = getString(messageLoc, context, message);

         producer.enqueue( npacMessage );

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

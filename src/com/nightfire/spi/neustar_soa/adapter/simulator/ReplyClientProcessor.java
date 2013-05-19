package com.nightfire.spi.neustar_soa.adapter.simulator;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.Debug;

/**
 * This processor calls the NPACReplyComServer to send a simulated XML
 * back to the SOA.
 */
public class ReplyClientProcessor extends MessageProcessorBase {

   public static final String INPUT_LOC_PROP = "INPUT_LOC";

   /**
    * The value of the INPUT_LOC prop. The location where the input
    * XML should be found.
    */
   private String inputLocation;

   public void initialize(String key, String type)
      throws ProcessingException {

         super.initialize(key, type);

         // Get value of optional input location property.
         // If not specified, the input message itself will be
         // used.
         inputLocation = super.getPropertyValue(INPUT_LOC_PROP,
                                                INPUT_MESSAGE);

   }

   /**
    * This takes an input message from the configured location
    * and calls on the NPACReplyComServer to deliver that message
    * to the SOA Connectivity Server via SmartSockets.
    */
   public NVPair[] process(MessageProcessorContext context,
                           MessageObject message)
                           throws MessageException,
                                  ProcessingException {

      // This is the standard reply to a null message object.
      // The driver passes in a null input when it is done processing.
      if(message == null){

         return null;

      }

      String xml = getString(inputLocation, context, message);

      if( Debug.isLevelEnabled( Debug.IO_STATUS ) ){

         Debug.log( Debug.IO_STATUS, "Sending message:\n"+xml );

      }

      int ack = NPACReplyComServer.getInstance().send(xml);

      if( Debug.isLevelEnabled( Debug.IO_STATUS ) ){

         Debug.log( Debug.IO_STATUS, "Result: "+
                    ((ack == NPACConstants.ACK_RESPONSE) ? "ACK" : "NACK") );

      }

      return formatNVPair(message);

   }

}

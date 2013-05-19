
package com.nightfire.adapter.messageprocessor;

import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.MessageException;

import com.nightfire.spi.common.driver.*;

import com.nightfire.adapter.messageprocessor.*;

/**
 *
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 *
 *
 * This component attempts extract the message in MessageExceptions from the RuleProcessor MP.
 *
 * When the RuleProcessor MP encounters errors it wraps all the errors in XML and
 * a MessageException with this XML as the Exception message.
 *
 * This processor extracts the message from the MessageException and passes it along as the
 * input to the NextProcessor.  Note: it does no validation on the message extracted.
 *
 *   if ( input instance of MessageException )
 *       -extract the message from the MessageException object
 *       -if message is non-nul, forward extracted message as the input to the next processor.
 *       -else do nothing and pass the input unchanged.
 *   else
 *       -do nothing just pass it along
 *
 */
public class RuleExceptionMessageExtractor extends MessageProcessorBase {
    
    public RuleExceptionMessageExtractor() {
    }
    
    
   /**
    * A name used at the beginning of log messages.
    */
    private static final String className = StringUtils.getClassName(new RuleExceptionMessageExtractor());
    
    
  /**
   * This component handles MessageExceptions from the RuleProcessor MP.
   * Whenthe RuleProcessor MP encounters errors it wraps all the errors in XML and throws
   * a MessageException with this XML as the Exception message.
   *
   * All this processor does is extracts the message from the MessageException and passes it along as the
   * input to the NextProcessor.  Note: it does no validation on the message extracted.
   *
   *   if ( input instance of MessageException )
   *       -extract the message from the MessageException object
   *       -if message is non-nul, forward extracted message as the input to the next processor.
   *       -else do nothing and pass the input unchanged.
   *   else
   *       -do nothing just pass it along
   *
   * @return
   *
   */
    public NVPair[] process(MessageProcessorContext context,
    MessageObject input )
    throws MessageException, ProcessingException {
        
        if ( input == null ) return null;
        
        Object messageObj = input.get();
        
        if (messageObj instanceof MessageException) {
            
            MessageException messageException = (MessageException) messageObj;
            String msg = messageException.getMessage();
            
            //Note: Currently this is a moot check, since impossible to create a MessageException with null message,
            //but will leave in here just in case this changes in future.
            if ( msg == null ) {
                Debug.log(Debug.MSG_STATUS, className + ": The MessageException received has a message of null. "
                + "passing input along unchanged" );
                //since null triggers end of chain, i will pass the MessageException along as this component
                //may be in a deferred Exception chain and don't want to unintentionally short circuit the chain.
                return formatNVPair(input);
            }
            
            if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                Debug.log(Debug.MSG_STATUS, className + ": Extracted message: " + msg + " and forwarding as input to next processor." );
            
            return formatNVPair(msg);
            
        } else {
            
            if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                Debug.log(Debug.MSG_STATUS, className + ": Did not receive a MessageException as input, forwarding input unchanged");
            //don't know what it is but assume it is meaningful downstream so pass it along.
            return formatNVPair(input);
        }
    }
    
    public static void main (String args[] ) {
        try {
            
            //Tests
            
            
            Debug.enableAll();
            RuleExceptionMessageExtractor mp = new RuleExceptionMessageExtractor();
            
            //send MessageException with non-null message;
            
            System.out.println("*********************************************");
            System.out.println("Test: 1");
            System.out.println("Input Object: MessageException with non-null message");
            MessageException me = new MessageException("<root><somenode value=\"yada\"/></root>");
            MessageObject input = new MessageObject(me);
            MessageProcessorContext context = new MessageProcessorContext();
            NVPair[] result = mp.process(context, input);     
            System.out.println("End Test: 1");
            System.out.println("*********************************************");
            
            
            //send something other than a message exception
            System.out.println("\n\n*********************************************");
            System.out.println("Test: 2");
            System.out.println("Input Object: not a MessageException");
            input = new MessageObject("yada yada yada");
            result = mp.process(context, input);
            System.out.println("End Test: 2");
            System.out.println("*********************************************");
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}

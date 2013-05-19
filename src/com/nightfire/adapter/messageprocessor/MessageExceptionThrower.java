package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;

/**
* This processor extends the ExceptionThrower and overrides its
* functionality so that if the input MessageObject contains a String,
* that String will be used to create a MessageException which will then be
* handled normally by ExceptionThrower.
*/
public class MessageExceptionThrower extends ExceptionThrower{


   /**
   * A name used at the beginning of log messages.
   */
   private static final String className = StringUtils.getClassName(new MessageExceptionThrower());

   /**
   * This method loads this processors properties and initializes the input
   * message location from these properties.
   *
   * @param key The property key for loading this processor's properties.
   * @param type The property type for loading this processor's properties.
   * @exception ProcessingException thrown if an error occurs during
   *                                initialization.
   */
   public void initialize( String key, String type ) throws ProcessingException{

      Debug.log( Debug.MSG_STATUS, "Initializing "+className+"...");

      // Load properties
      super.initialize(key, type);

      Debug.log( Debug.MSG_STATUS, className+": Initialization done." );

   }

   /**
   * Creates a MessageException with the given message. If the input in the
   * MessageObject was a String, this method with be called by
   * ExceptionThrower.process() to create an Exception with that String.
   *
   * @param message The message used to populate the exception.
   * @return a new MessageException containing the given String.
   */
   protected Exception getException(String message){

      return new MessageException(message);

   }
   
} 
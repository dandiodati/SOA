/**
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.errorhandler;


import java.util.*;
import java.text.*;
import java.io.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.db.*;


/**
 * This message-processor  extracts batch errors/exceptions from the DriverMessageProcessor
 * context (ERROR_PROCESSOR_EXCEPTION_LOC)
 * This processor assumes that the Driver Chain is configured to store error(s)/exception(s)
 * in Exception LOcation (ERROR_PROCESSOR_EXCEPTION_LOC) and set for deferred exceptions
 * Determines the error (MessageException or Processing Exception).
 * Sets the error/exception and error type in context.
 * Returns input message to the next processor.
 */
public class DriverErrorExtractor extends MessageProcessorBase
{
    // Errors Location to read from.
    public static final String BATCH_ERRORS_CONTEXT_LOC_PROP = "BATCH_ERRORS_CONTEXT_LOC";

    // Result Location for exception type.
    public static final String EXCEPTION_TYPE_PROP = "EXCEPTION_TYPE";

    // Result Location for error messages.
    public static final String EXCEPTION_MSG_LOC_PROP = "EXCEPTION_MSG_LOC";

    // Flag indicating whether exception stack-trace should be appended into EXCEPTION_MSG.
    public static final String APPEND_STACKTRACE_PROP = "APPEND_STACKTRACE";

    // Constant for Message Exceptions.
    private static final String MESSAGE_EXCEPTION = "DATA ERROR";

    // Constant for Processing Exceptions.
    private static final String PROCESSING_EXCEPTION = "PROCESSING ERROR";

    // Abbreviated class name for use in logging.
    private static final String className ="DriverErrorExtractor" ;

    private boolean appendStackTrace = false;

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        StringBuffer errors = new StringBuffer();

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Initializing..." );

        batchErrorsLocation = getPropertyValue(BATCH_ERRORS_CONTEXT_LOC_PROP );
        errorType = getRequiredPropertyValue( EXCEPTION_TYPE_PROP, errors );
        errorMsg  = getRequiredPropertyValue( EXCEPTION_MSG_LOC_PROP, errors );
        String stackTraceProp = getPropertyValue(APPEND_STACKTRACE_PROP);

        if(StringUtils.hasValue(stackTraceProp))
        {
            try {
                appendStackTrace = StringUtils.getBoolean(stackTraceProp);
            } catch (FrameworkException e) {
                Debug.warning("Illegal boolean value of property :"+APPEND_STACKTRACE_PROP+" value  :"+stackTraceProp);
            }
        }


        // Use default if no configured value was given.
        if ( !StringUtils.hasValue( batchErrorsLocation ) )
            batchErrorsLocation = MessageProcessingDriver.ERROR_PROCESSOR_EXCEPTION_LOC;

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Error-context location in the message-processor context ["
                       + batchErrorsLocation + "]." );

	      if (errors.length() > 0){
    		  throw new ProcessingException(className+": " + errors.toString());
        }

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Initialization done." );
    }


    /**
     * Extracts error/exception from Exception Location sets in context.
     *
     * @param  context The context
     * @param  input  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException
    {
        if ( input == null )
            return null;

        // Get all errors.
        String allErrors = getExceptionMessage(context, input);

         if (StringUtils.hasValue(allErrors)){
            if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
              Debug.log(Debug.MSG_STATUS, className + ": Extracted error message is  [" + allErrors + "]" );
        // set the errors in context.
         set( errorMsg, context, input, allErrors);
        }
        else {
            
            if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                Debug.log(Debug.MSG_STATUS, className + ": Did not receive any errors as input, forwarding input unchanged");
            }

        // return the input to the next processor.
         return formatNVPair(input);
}

    /**
     * Get the exception message from the context.
     *
     * @param  context  The context.
     *
     * @return  The batch exceptions.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    private String getExceptionMessage ( MessageProcessorContext context, MessageObject input)
        throws MessageException, ProcessingException
    {
        StringBuilder batchErrors = new StringBuilder();
        String errorObjType = null;

        if ( !context.exists( batchErrorsLocation, false ) )
        {
                String err = "ERROR: Missing required exception at context location [" + batchErrorsLocation + "].";

                Debug.error( err );

                throw new MessageException( err );
        }

        // Get the error-messages from the message-processor context

        Exception errorObj = (Exception) context.get( batchErrorsLocation );

        if ( errorObj == null )
            throw new ProcessingException( "ERROR: Error-Messages are not available in message-processor context." );

       if (errorObj instanceof MessageException){
          errorObjType = MESSAGE_EXCEPTION;
       }
       if (errorObj instanceof ProcessingException){
          errorObjType = PROCESSING_EXCEPTION;
       }

       if (StringUtils.hasValue(errorObjType)){     // set the error type in context.
           if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
              Debug.log(Debug.MSG_STATUS, className + ": Extracted error is of type : [" + errorObjType + "]" );
              set( errorType, context, input, errorObjType);
       }

       batchErrors.append(errorObj.toString());
       if(appendStackTrace)
       {
           String stackTrace = Debug.getStackTrace(errorObj);
           batchErrors.append("\n")
                       .append(stackTrace);
       }

       return batchErrors.toString();
    }

    // The location in the context where the list of ErrorContext objects can be found.
      private String batchErrorsLocation ;
    // The location in the context where the error message can be set.
       private String errorMsg;
    // The location in the context where the result can be set.
       private String errorType;

}

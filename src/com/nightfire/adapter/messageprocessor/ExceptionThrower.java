/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;

import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * This message processor provides a mechanism for the delayed throwing
 * of exceptions. This allows processing to continue after an exceptional
 * condition occurs so that any essential operation (e.g., returning a
 * negative acknowledgement to a trading partner) can complete before the
 * exception is thrown.
 */
public class ExceptionThrower extends MessageProcessorBase {

    private MessageException messageException = null;
    private ProcessingException processingException = null;

    private boolean hasMessageException = false;
    private boolean hasProcessingException = false;

    /**
     * Initializes this object.
     * 
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException {

        // Call the abstract super class's initialize method. This initializes
        // the adapterProperties hashtable defined in the super class and
        // retrieves the name and toProcessorNames values from the properties.
        
        super.initialize(key, type);
    }

    /**
     * Throws the exception received as input. 
     *
     * @param  input  The exception to process.
     *
     * @return  Null or exception is thrown
     *
     * @exception  MessageException  Thrown if the message is bad
     *
     * @exception ProcessingException Thrown if processing fails
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject messageObject )
	throws MessageException, ProcessingException {

        if ( (messageObject == null) || (messageObject.get() == null) )
        {
            if ( hasMessageException ) {
                
                Debug.log( this, Debug.ALL_ERRORS,
                           "ExceptionThrower: throwing MessageException" );
                throw messageException;
                
            } else if ( hasProcessingException ) {
                
                Debug.log( this, Debug.ALL_ERRORS,
                           "ExceptionThrower: throwing ProcessingException" );
                throw processingException;
                
            } else {
                return null;
            }
        }

            //Assuming the the MessageObject only contains the exception if any.
            Object input = messageObject.get ( );

            // If the input is a String, create an appropriate Exception
            // object for it.
            if(input instanceof String){

               input = getException( (String) input );

            }

            if (input instanceof MessageException) {

                messageException = (MessageException) input;
                hasMessageException = true;

                Debug.log( this, Debug.ALL_ERRORS,
                           "ExceptionThrower received instance of MessageException" );
                Debug.log( this, Debug.ALL_ERRORS, "Error message: " + messageException.getMessage() );

            } else if (input instanceof ProcessingException) {

                processingException = (ProcessingException) input;
                hasProcessingException = true;

                Debug.log( this, Debug.ALL_ERRORS,
                           "ExceptionThrower received instance of ProcessingException" );
                Debug.log( this, Debug.ALL_ERRORS, "Error message: " + processingException.getMessage() );

            } else {

                Debug.log( this, Debug.ALL_ERRORS,
                           "ExceptionThrower received instance of " + input.getClass().getName() );

                if ( input instanceof Throwable) {

                    Debug.log( this, Debug.ALL_ERRORS, "with error message: " + ( (Throwable) input ).getMessage() );
                }

                throw new ProcessingException( "ERROR: ExceptionThrower: The input object is neither a MessageException " +
                                               "nor a ProcessingException");
            }

            return null;

        }


        /**
        * If the input in the MessageObject was a String, this method is
        * used to create an Exception containing that String message.
        *
        * @param message the input message.
        * @return a ProcessingException containing the <code>message</code>
        *         is created as a default.  
        */
        protected Exception getException(String message){

           return new ProcessingException(message);

        }
        
}

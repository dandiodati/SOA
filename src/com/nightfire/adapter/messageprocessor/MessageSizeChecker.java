/**
 * Copyright (c) 2006 Neustar Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * Evaluate the size of input message object (in KB) and test against the max allowable limit,
 * depending upon the result of test, the processor routes the input to the next processor if the
 * size of input messgae object is more then allowable limit, otherwise to the default next processor.
 */

public class MessageSizeChecker extends MessageProcessorBase
{

    /**
     * The allowable max size of input file in kb.
     */
    public static final String MAX_ALLOWABLE_MESSAGE_SIZE_PROP= "MAX_ALLOWABLE_MESSAGE_SIZE";

    /**
     * The name of the next processor to send the input message to if the size of input EDI is more then the allowable size.
     */
	public static final String NEXT_PROCESSOR_PROP = "NEXT_PROCESSOR";

    /**
     * The default processor to route the input message if size of input message is within maximum allowable limits.
     */
    public static final String DEFAULT_NEXT_PROCESSOR_PROP = "DEFAULT_NEXT_PROCESSOR";

    /**
     * The value of 1 KB. Used while determining the size of input message object.
     */
    public static final int KB = 1024;

    /**
     * Constructor.
     */
    public MessageSizeChecker ( )
    {
        loggingClassName = StringUtils.getClassName( this );
    }


    /**
     * Called to initialize this component.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  com.nightfire.common.ProcessingException  Thrown if processing fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, loggingClassName + ": Initializing ..." );

        super.initialize( key, type );

        try
        {
            // get the configured property value
            String maxSize= getRequiredPropertyValue(MAX_ALLOWABLE_MESSAGE_SIZE_PROP);
            if ( StringUtils.hasValue( maxSize ) )
				allowableMsgSize = StringUtils.getInteger(maxSize);
            else
                allowableMsgSize = 0;
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }

        // get the configured default processopr name
        defaultNextProcessorName = getRequiredPropertyValue( DEFAULT_NEXT_PROCESSOR_PROP );

        // get the configured next processopr name
        nextProcessorName = getRequiredPropertyValue( NEXT_PROCESSOR_PROP);

    }


    /**
     * Process the input message and (optionally) return
     * a value.
     *
     * @param  input  Input MessageObject that contains the message to be processed.
     * @param  mpcontext The context
     *
     * @return  Optional output NVPair array, or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  com.nightfire.framework.message.MessageException  Thrown if processing fails.
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input )
        throws MessageException, ProcessingException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": processing ..." );

        if ( input == null )
            return null;

        // Get the string from the input object.
        String inputObject = input.getString();

        //Get input object string length in bytes.
        long strSizeKb = inputObject.length();
        Debug.log (Debug.MSG_STATUS, "Length of the input message object string is = " + strSizeKb + " bytes");

        if (strSizeKb > allowableMsgSize * KB)
        {
            // Route to next processor as the size of input object string is more then allowable size.
            NVPair[ ] response = new NVPair[ 1 ];
            response[ 0 ] = new NVPair( nextProcessorName, input );
            Debug.log (Debug.MSG_STATUS, "Routing message to message-processor ["  + nextProcessorName + "]");
            return response;
        }
        else
        {
            // Route to default next processor as the size of input object string is within the allowable limits.
            toProcessorNames = new String[ 1 ];
            toProcessorNames[ 0 ] = defaultNextProcessorName;
            Debug.log (Debug.MSG_STATUS, "Routing message to default message-processor ["  +  defaultNextProcessorName + "]");
        }

        // Send the result on through the chain according to properties.
        return( formatNVPair( input ) );
    }

    private String defaultNextProcessorName;
    private int allowableMsgSize;
    private String nextProcessorName;

   // Abbreviated class name for use in logging.
    private String loggingClassName;
}

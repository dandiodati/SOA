/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/DatabaseLogger.java#2 $
 */

package com.nightfire.adapter.messageprocessor;


import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;


/**
 * This is a generic message-processor that does nothing and passes all
 * inputs to outputs under control of its configuration.
 */
public class NullProcessor extends MessageProcessorBase
{
    /**
     * Constructor.
     */
    public NullProcessor ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating null message-processor." );
    }


    /**
     * A no-op processor that just passes its input to output.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException 
    {
        if ( inputObject == null )
            return null;

        Debug.log( Debug.MSG_STATUS, "Executing null message-processor." );

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }
}

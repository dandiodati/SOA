/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.spi.common.driver;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;


/**
 * The factory creates and configures message processors(MP).
 */
public class MessageProcessorFactory
{
    /**
     * Creates and configures a message processor instance.
     * 
     * @param  className  Fully-qualified class name (including package path).
     * @param  propKey    Property key value.
     * @param  propType   Property type value.
     *
     * @return  Newly-created and configured message processor object.
     *
     * @exception  ProcessingException  Thrown if object can't be created or configured.
     */
    public static MessageProcessor create ( String className, String propKey, String propType ) throws ProcessingException
    {

        return create ( className, propKey, propType, true);
    }


    /**
     * Creates a message processor instance but does not configure it.
     * 
     * @param  className  Fully-qualified class name (including package path).
     * @param  propKey    Property key value.
     * @param  propType   Property type value.
     *
     * @return  Newly-created message processor object (not configured).
     *
     * @exception  ProcessingException  Thrown if object can't be created or configured.
     */
    public static MessageProcessor createOnly ( String className, String propKey, String propType ) throws ProcessingException
    {

        return create ( className, propKey, propType, false);
    }


    /**
     * Creates and configures a message-processor instance.
     * 
     * @param  className  Fully-qualified class name (including package path).
     * @param  propKey    Property key value.
     * @param  propType   Property type value.
     * @param  loadConfig	Flag that specifies whether to call the intialize() method of the message process object after creation.
     *
     * @return  Newly-created and optionally configured message processor object.
     *
     * @exception  ProcessingException  Thrown if object can't be created or configured.
     */
    public static MessageProcessor create ( String className, String propKey, String propType, boolean loadConfig ) throws ProcessingException
    {
        MessageProcessor mp = null;

        try
        {
             mp = (MessageProcessor)ObjectFactory.create( className, MessageProcessor.class );
        }
        catch ( FrameworkException fe )
        {
            throw new ProcessingException( "ERROR: Could not create message processor object [" +
                                        className + "]:\n" + fe.toString() );
        }

		if (loadConfig) {
        	mp.initialize( propKey, propType );
        }

        return mp;
    }
}

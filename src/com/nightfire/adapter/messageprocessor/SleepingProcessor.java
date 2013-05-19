/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/SleepingProcessor.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;


/**
 * This is a generic message-processor that does nothing other than sleep 
 * and passes all inputs to outputs under control of its configuration.
 */
public class SleepingProcessor extends MessageProcessorBase
{
    /**
     * Property indicating the time to sleep (in msecs).  If not set, the default is
     * to not sleep at all.
     */
    public static final String SLEEP_TIME_PROP = "SLEEP_TIME";


    /**
     * Constructor.
     */
    public SleepingProcessor ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating sleeping message-processor." );
    }


    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize( key, type );

        try
        {
            String sleepLength = getPropertyValue( SLEEP_TIME_PROP );

            if ( StringUtils.hasValue( sleepLength ) )
            {
                sleepTime = StringUtils.getInteger( sleepLength );

                if ( sleepTime < 0 )
                    sleepTime = 0;
            }
            else
                sleepTime = 0;

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Configured to sleep for [" + sleepTime + "] msecs." );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }
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

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Sleeping for [" + sleepTime + "] msecs ..." );

        if ( sleepTime > 0 )
        {
            try
            {
                Thread.currentThread().sleep( sleepTime );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );
            }
        }

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Just woke up from sleeping for [" + sleepTime + "] msecs." );

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }


    int sleepTime = 0;  // Default if property isn't set is to not sleep at all.
}

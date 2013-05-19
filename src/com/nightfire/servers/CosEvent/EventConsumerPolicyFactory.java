/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;

import com.nightfire.framework.util.*;


/**
 * A factory for Event Consumer objects.
 */
public class EventConsumerPolicyFactory
{
    /**
     * Constant indicating consumer providing an only-once delivery policy.
     */
    public static final int ONLY_ONCE_DELIVERY = 1;
   
    
    /**
     * Create an Event Consumer instance of the indicated type.
     *
     * @param  type  The type of consumer (one of: only-once-delivery).
     *
     * @return  The newly-created Event Consumer.
     *
     * @exception  FrameworkException  Thrown if the object can't be created.
     */
    public static EventConsumerPolicy create ( int type ) throws FrameworkException
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating an event consumer ..." );

        switch ( type )
        {
        case ONLY_ONCE_DELIVERY:
            return( (EventConsumerPolicy)ObjectFactory.create( OnlyOnceEventConsumerPolicy.class.getName(), 
                                                               OnlyOnceEventConsumerPolicy.class ) );

        default:
            throw new FrameworkException( "ERROR: Invalid event consumer type [" + type + "]." );
        }
    }


    /**
     * Get all of the names of configuration properties that
     * could be used to configure an event consumer policy.
     *
     * @return  An array of configuration property names.
     */
    public static String[] getConfigPropertyNames ( )
    {
        return additionalConfigPropNames;
    }

    
    // A list of additional property names
    // that can be used to configure an event consumer policy.
    private static String[] additionalConfigPropNames = {
        OnlyOnceEventConsumerPolicy.MAX_PUSH_WAIT_TIME_PROP
    };
}

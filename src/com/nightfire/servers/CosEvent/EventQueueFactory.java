/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;

import com.nightfire.framework.util.*;


/**
 * A factory for Event Queue objects.
 */
public class EventQueueFactory
{
    /**
     * Constant indicating a transient queue.  The queue contents
     * will be lost if the application containing the queue is shut down.
     */
    public static final int TRANSIENT = 1;
    
    /**
     * Constant indicating a persistent queue.  The queue contents
     * will persist across application invocations.
     */
    public static final int PERSISTENT = 2;
    
    
    /**
     * Create an Event Queue instance of the indicated type.
     *
     * @param  type  The type of queue (one of: transient, persistent).
     *
     * @return  The newly-created Event Queue.
     *
     * @exception  FrameworkException  Thrown if the object can't be created.
     */
    public static EventQueue create ( int type ) throws FrameworkException
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating an event queue ..." );

        switch ( type )
        {
        case TRANSIENT:
            return( (EventQueue)ObjectFactory.create( InMemoryEventQueue.class.getName(), 
                                                      InMemoryEventQueue.class ) );

        case PERSISTENT:
            return( (EventQueue)ObjectFactory.create( DatabaseEventQueue.class.getName(), 
                                                      DatabaseEventQueue.class ) );
            
        default:
            throw new FrameworkException( "ERROR: Invalid event queue type [" + type + "]." );
        }
    }


    /**
     * Get all of the names of configuration properties that
     * could be used to configure an event queue.
     *
     * @return  An array of configuration property names.
     */
    public static String[] getConfigPropertyNames ( )
    {
        return additionalConfigPropNames;
    }

    
    // A list of additional property names
    // that can be used to configure an event queue.
    private static String[] additionalConfigPropNames = {
        DatabaseEventQueue.LOAD_EVENT_BATCH_SIZE_PROP
    };
}

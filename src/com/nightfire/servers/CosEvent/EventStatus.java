/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


/**
 * Type-safe constants indicating event delivery disposition.
 */
public class EventStatus
{
    /**
     * Constant indicating that an event was successfully delivered to consumer(s).
     */
    public static final EventStatus DELIVERY_SUCCESSFUL = new EventStatus( "delivery-successful" );
    
    /**
     * Constant indicating that an event was not delivered to consumer(s) successfully.
     */
    public static final EventStatus DELIVERY_FAILED = new EventStatus( "delivery-failed" );
    
    /**
     * Constant indicating that no consumers were available to deliver event to.
     */
    public static final EventStatus NO_CONSUMERS_AVAILABLE = new EventStatus( "no-consumers-available" );
    
    
    /**
     * A human-readable representation of the constant.
     */
    public final String name;
    
    
    // No instances of this class should be created other than
    // the static constants given above.  The name is provided
    // as a means to facilitate useful logging.
    private EventStatus ( String name )
    {
        this.name = name;
    }

 
    /**
     * Get a human-readable representation of the object.
     *
     * @return  A textual representation of the event status object.
     */
    public String toString ( )
    {
        return( EventStatus.class.getName() + ":" + name );
    }
}

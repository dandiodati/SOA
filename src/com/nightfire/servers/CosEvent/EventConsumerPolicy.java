/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;

import org.omg.CORBA.*;

import com.nightfire.framework.util.*;


/**
 * Interface that all classes providing specific event
 * consumer policies must implement to satisfy required 
 * quality-of-service semantics.
 */
public interface EventConsumerPolicy
{
    /**
     * Initialize the consumer delivery policy object.
     *
     * @param  props  A container of configuration properties.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid.
     */
    public void initialize ( Map props ) throws FrameworkException;
    
    
    /**
     * Deliver the given event to the indicated consumers.  Classes implementing
     * this interface support the quality-of-service policy that determines
     * when an event is considered 'delivered'.
     * 
     * @param  consumers  An array of proxy push suppliers whose push consumers should receive
     *                    the event.
     * @param  event  The event to deliver.
     * 
     * @return  EventStatus.DELIVERY_SUCCESSFUL if event was successfully delivered.
     *          EventStatus.DELIVERY_FAILED if event delivery was not successful.
     *          EventStatus.NO_CONSUMERS_AVAILABLE if no consumers were available to push event to.
     * 
     * @exception  FrameworkException  Thrown on unexpected errors.
     */
    public EventStatus deliver ( ProxyPushSupplierImpl[] consumers, Event event ) 
        throws FrameworkException;
    
    
    /**
     * Get a human-readable description of the event consumer policy.
     *
     * @return  A description of the event consumer policy object.
     */
    public String describe ( );
}

/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;

import com.nightfire.framework.util.*;


/**
 * Interface that all classes supporting an ordered queue
 * of events must implement.
 * 
 * NOTE: The following usage is assumed for all EventQueue types:
 * 1.) Multiple supplier event-delivery threads may be concurrently 
 *     enqueueing events via the add() method.
 * 2.) A single thread, which is distinct from the supplier threads,
 *     will be dequeueing events via the hasNext(), next(), update() and
 *     noConsumersAvailable() methods.
 * 3.) The initialize() method is called before any other queuing operations occur.
 * 4.) The shutdown() method may be called at any point.
 */
public interface EventQueue
{
    /**
     * Initialize the event queue.
     *
     * @param  props  A container of configuration properties.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid.
     */
    public void initialize ( Map props ) throws FrameworkException;
    
    
    /**
     * Add the event to the end of queue.
     * 
     * @param  event  The event to add to the queue.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public void add ( Event event ) throws FrameworkException;
    
    
    /**
     * Returns 'true' if the queue has more items to process.
     * 
     * @param  criteria  An event containing the event-selection criteria.
     *
     * @return  'true' if queue has more items to process, otherwise 'false'.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public boolean hasNext ( Event criteria ) throws FrameworkException;


    /**
     * Get the next queued item.  Must be called after hasNext().
     * 
     * @return  The next item in the queue to process.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public Event next ( ) throws FrameworkException;


    /**
     * Update the given event to reflect the outcome of its processing.
     * 
     * @param  event  The event to update.
     * @param  eventStatus  The event delivery status.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public void update ( Event event, EventStatus eventStatus ) throws FrameworkException;


    /**
     * Indicates to the queue that no event consumers are available 
     * for event delivery.
     */
    public void noConsumersAvailable ( );


    /**
     * Shut down the queue;
     */
    public void shutdown ( );


    /**
     * Get a human-readable description of the event queue.
     *
     * @return  A description of the event queue.
     */
    public String describe ( );
}

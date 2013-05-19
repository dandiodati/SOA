/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;

import com.nightfire.framework.util.*;


/**
 * Class supporting a transient in-memory event queue.
 *
 * NOTE: The following usage is assumed for this EventQueue type:
 * 1.) Multiple supplier event-delivery threads may be concurrently 
 *     enqueueing events via the add() method.
 * 2.) A single thread, which is distinct from the supplier threads,
 *     will be dequeueing events via the hasNext(), next(), update() and
 *     noConsumersAvailable() methods.
 * 3.) The initialize() method is called before any other queuing operations occur.
 * 4.) The shutdown() method may be called at any point.
 */
public class InMemoryEventQueue implements EventQueue
{
    /**
     * Constructor.
     */
    public InMemoryEventQueue ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "QUEUE OPERATION: Creating event queue of type [" 
                       + StringUtils.getClassName(this) + "] ..." );
        
        queue = Collections.synchronizedList( new LinkedList() );
    }
    
    
    /**
     * Initialize the event queue.
     *
     * @param  props  A container of configuration properties.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid.
     */
    public void initialize ( Map props ) throws FrameworkException
    {
        // Nothing to do here currently.  Perhaps we should bound queue size?
    }
    
    
    /**
     * Add the event to the end of queue.
     * 
     * @param  event  The event to add to the queue.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public void add ( Event event ) throws FrameworkException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Adding the following event to the queue:\n" 
                       + event.describe() );
        
        queue.add( event );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Queue size [" + queue.size() + "]." );
    }
    
    
    /**
     * Update the given event as indicated.
     * 
     * @param  event  The event to update.
     * @param  eventStatus  The event delivery status.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public void update ( Event event, EventStatus eventStatus ) throws FrameworkException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Updating queue using event status [" 
                       + eventStatus.name + "]." );

        // This queue supports advisory notifications, so we will remove the event
        // from the queue regardless as to whether it was successfully delivered or not.
        if ( (eventStatus == EventStatus.DELIVERY_SUCCESSFUL) || 
             (eventStatus == EventStatus.DELIVERY_FAILED) ||
             (eventStatus == EventStatus.NO_CONSUMERS_AVAILABLE) )
        {
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Removing event [" + event.describe() + "] from queue." );
            
            boolean removed = queue.remove( event );
            
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Event removed? [" + removed 
                           + "].  Queue size [" + queue.size() + "]." );
        }
        else
        {
            throw new FrameworkException( "ERROR: Invalid event update type [" + eventStatus.name + "]." );
        }
    }


    /**
     * Returns 'true' if the queue has more items to process.
     * 
     * @param  criteria  An event containing the event-selection criteria.
     *
     * @return  'true' if queue has more items to process, otherwise 'false'.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public boolean hasNext ( Event criteria ) throws FrameworkException
    {
        // NOTE: The criteria argument is ignored in this case.

        boolean available = (queue.size() > 0);
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Events available in queue? [" + available + "]." );
        
        return available;
    }
    
    
    /**
     * Get the next queued item.  Must be called after hasNext().
     * 
     * @return  The next item in the queue to process.
     * 
     * @exception  FrameworkException  Thrown on errors.
     */
    public Event next ( ) throws FrameworkException
    {
        if ( queue.size() == 0 )
        {
            throw new FrameworkException( "ERROR: Attempt was made to retrieve event from empty queue." );
        }

        Event event = (Event)queue.get( 0 );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Retrieving the following event from the queue:\n" 
                       + event.describe() );
        
        return event;
    }
    
    
    /**
     * Indicates to the queue that no event consumers are available.
     */
    public void noConsumersAvailable ( )
    {
        // Empty out the queue.
        if ( queue.size() > 0 )
        {
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "QUEUE OPERATION: Removing all [" 
                           + queue.size() + "] entries from queue ..." );
            
            queue.clear( );
        }
    }
    
    
    /**
     * Shut down the queue;
     */
    public void shutdown ( )
    {
        // Nothing to do here.
    }


    /**
     * Get a human-readable description of the event queue.
     *
     * @return  A description of the event queue.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );
        
        sb.append( "In-memory event queue [" );
        sb.append( StringUtils.getClassName(this) );
        sb.append( "], number of events in queue [" );
        sb.append( queue.size() );
        sb.append( "]" );
        
        return( sb.toString() );
    }


    private List queue;
}

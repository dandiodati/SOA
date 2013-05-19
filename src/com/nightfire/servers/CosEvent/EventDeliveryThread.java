/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ReliableChannel.java#11 $
 */

package com.nightfire.servers.CosEvent;

import java.util.*;

import org.omg.CORBA.*;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.db.*;


/**
 * A thread used to deliver events.
 */
public class EventDeliveryThread extends Thread
{
    /**
     * Default time to wait between checks for available events on queue (30 minutes).
     */
    public static final int DEFAULT_WAIT_TIME = 30;


    /**
     * Constructs an event-delivery thread for given channel.
     *
     * @param  channel  Event channel that this delivery thread is associated with.
     */
    public EventDeliveryThread ( EventChannelImpl channel )
    {
        // Give the thread a meaningful name to help in logging.
        super( channel.getName() + ":event-delivery-thread" );

        this.channel = channel;
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating event-delivery thread for channel [" 
                       + channel.getName() + "] ..." );
        
        // Start the execution of the thread.
        start( );
    }
    
    
    /**
     * Shut down the event-delivery thread.
     */
    protected synchronized void shutdown ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Destroying event-delivery thread for channel [" 
                       + channel.getName() + "] ..." );
        
        // Set flag indicating that shut-down should occur and notify run method.
        shutDown = true;
        
        notifyAll( );
    }
    

    /**
     * Invoked when thread is started.  Executes an infinite loop that
     * waits until notified of events, at which time it pushes events
     * to connected consumers.
     */
    public void run ( ) 
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Starting event-delivery thread for channel [" 
                       + channel.getName() + "] ..." );
        
        while ( true )
        {
            // If no events are available to deliver, 
            // or no consumers are available to consume them,
            // sleep until things change.
            synchronized ( this )
            {
                // Wait until the conditions indicated above are met.
                while ( !channel.readyToDeliverEvents() )
                {
                    if ( shutDown )
                        break;
            
                    try
                    {
                        if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
                            Debug.log( Debug.THREAD_STATUS, "EventDeliveryThread.run(): Event delivery thread for channel [" 
                                       + channel.getName() + "] is now going to sleep for up to [" 
                                       + DEFAULT_WAIT_TIME + "] minutes." );
                    
                        // Periodically wake up and check for events that might have 
                        // slipped in between consumerPush() and wait() call.
                        wait( DEFAULT_WAIT_TIME * EventUtils.MSEC_PER_MIN );
                    
                        if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
                            Debug.log( Debug.THREAD_STATUS, "EventDeliveryThread.run(): Event delivery thread for channel [" 
                                       + channel.getName() + "] just woke up." );
                    }
                    catch ( Exception e )
                    {
                        Debug.warning( "Event delivery thread wait for channel [" 
                                       + channel.getName() + "] was interrupted:\n" 
                                       + e.toString() );
                    }
                }
            }
            
            if ( shutDown )
                break;
            
            // Tell the channel to push any available events
            // to any available consumers.
            try
            {
                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                {
                    Debug.log(Debug.MSG_DATA, "EventDeliveryThread.run(): Pushing available events to available consumers via channel [" + 
                                               channel.getName() + "] ...");    
                }
                
                channel.consumerPush( );
                
                Debug.log(Debug.MSG_DATA, "EventDeliveryThread.run(): The pushing of events has completed.");    
            }
            catch ( Exception e )
            {
                Debug.warning( "Event-delivery thread caught exception during push to consumers:\n" 
                               + e.toString() + "\n" + Debug.getStackTrace( e ) );
            }
        }

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event-delivery thread for channel [" 
                       + channel.getName() + "] is now exiting ..." );
    }


    /**
     * Alert the event-delivery thread that events may be available for consumption.
     */
    protected synchronized void alert ( )
    {
        if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
            Debug.log( Debug.THREAD_STATUS, "Notifying event-delivery thread for channel [" 
                       + channel.getName() + "]." );
        
        notifyAll( );
    }


    // Channel associated with event delivery thread.
    private EventChannelImpl channel;

    // Flag indicating whether this thread's execution should terminate or not.
    private boolean shutDown = false;
}

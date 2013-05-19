/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;


import java.util.*;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;
import org.omg.CORBA.*;

import com.nightfire.framework.util.*;


/**
 * Event consumer that provides guaranteed-only-once-delivery semantics.
 * An event is considered delivered if at least one consumer push succeeds.
 */
public class OnlyOnceEventConsumerPolicy implements EventConsumerPolicy
{
    /**
     * Maximum time to wait (in msec) for an event push to a consumer to complete 
     * before giving up. (Default is to wait 5 minutes.)
     */
    public static final String MAX_PUSH_WAIT_TIME_PROP = "MAX_PUSH_WAIT_TIME";


    /**
     * Constructor.
     */
    public OnlyOnceEventConsumerPolicy ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating event-delivery-policy object of type [" 
                       + StringUtils.getClassName(this) + "] ..." );
    }


    /**
     * Initialize the event consumer.
     *
     * @param  props  A container of configuration properties.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid.
     */
    public void initialize ( Map props ) throws FrameworkException
    {
        String temp = (String)props.get( MAX_PUSH_WAIT_TIME_PROP );

        if ( StringUtils.hasValue( temp ) )
        {
            try
            {
                maxPushWait = StringUtils.getInteger( temp );
            }
            catch ( Exception e )
            {
                Debug.warning( e.getMessage() + "  Using default value of [" 
                               + maxPushWait + "]." );
            }

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Maximum-push-wait-time is [" 
                           + maxPushWait + "] seconds." );
        }
    }


    /**
     * Deliver the given event to the indicated consumers.  Classes implementing
     * this interface support the quality-of-service policy that determines
     * when an event is considered 'delivered'.
     * 
     * @param  consumers  An array of proxy push suppliers whose push consumers should receive
     *                    the event.
     * @param  event  The event to deliver.
     * 
     * @return  EventStatus.DELIVERY_SUCCESSFUL if event was successfully delived.
     *          EventStatus.DELIVERY_FAILED if event deliver was not successful.
     *          EventStatus.NO_CONSUMERS_AVAILABLE if no consumers were available to push event to.
     * 
     * @exception  FrameworkException  Thrown on unexpected errors.
     */
    public EventStatus deliver ( ProxyPushSupplierImpl[] consumers, Event event ) 
        throws FrameworkException
    {
        if ( !StringUtils.hasValue( event.message ) )
        {
            throw new FrameworkException( "ERROR: Can't deliver null event:\n" 
                                          + event.describe() );
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
            Debug.log( Debug.MSG_STATUS, "OnlyOnceEventConsumerPolicy.deliver(): Attempting to deliver event to [" 
                       + consumers.length + "] remote consumers attached to channel ...\nEvent contents:\n" 
                       + event.message );
        }
        
        EventStatus status = EventStatus.NO_CONSUMERS_AVAILABLE;
        
        int successfulPushes = 0;
        int failedPushes = 0;
        
        // Loop over all consumers, attempting to deliver the event to each one.
        for ( int Ix = 0;  Ix < consumers.length;  Ix ++ )
        {
            ProxyPushSupplierImpl consumer = consumers[ Ix ];

            // If consumer isn't still attached to event service, skip it.
            if ( consumer.peerDisconnected() ) 
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Found a stale proxy push supplier which will be removed." );
                
                consumer.destroy( );
                
                continue;
            }


            // Execute the consumer using an observer/notifier construct, which
            // launches each push in its own thread.  This is done so that we
            // can interrupt pushes that take too long due to unresponsive consumers.
            TimedConsumer timedConsumer = new TimedConsumer( consumer, event.message );
            
            NFObserver observer = new NFObserver( timedConsumer, maxPushWait * EventUtils.MSEC_PER_SEC );
            
            observer.executeNotifier( );
            
            // If at least one consumer has successfully processed event, it
            // is considered delivered.
            if ( timedConsumer.eventDelivered() )
            {
                status = EventStatus.DELIVERY_SUCCESSFUL;
                
                successfulPushes ++;
            }
            else
            {
                // Only mark it failed if it hasn't been delivered successfully
                // at least once.
                if ( status != EventStatus.DELIVERY_SUCCESSFUL )
                {
                    status = EventStatus.DELIVERY_FAILED;
                    
                    // Remember exception that caused push to fail.
                    if ( consumer.getConsumerException() != null )
                        event.lastErrorMessage = consumer.getConsumerException().toString( );
                }
                
                failedPushes ++;
            }
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
            Debug.log( Debug.MSG_STATUS, "Successful-push-count [" + successfulPushes + "], failed-push-count [" 
                       + failedPushes + "].\nEvent delivery status [" + status.name + "]\n" );
        }
        
        return status;
    }


    /**
     * Get a human-readable description of the event consumer policy.
     *
     * @return  A description of the event consumer policy object.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );
        
        sb.append( "Only-once delivery policy [" );
        sb.append( StringUtils.getClassName(this) );
        sb.append( "], max-push-wait-time [" );
        sb.append( maxPushWait );
        sb.append( "] seconds" );
        
        return( sb.toString() );
    }


    /**
     * Class providing the ability to attempt an event push
     * in a separate thread, which allows for timeout management.
     */
    private class TimedConsumer extends NFNotifier
    {
        /**
         * timed-consumer object constructor.
         *
         * @param  consumer  Proxy push supplier containing consumer to push event to.
         * @param  eventData  The event data to push to consumer.
         */
        public TimedConsumer ( ProxyPushSupplierImpl consumer, String eventData )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating timed-event consumer ..." );
            
            this.consumer  = consumer;
            this.eventData = eventData;
        }
        
        
        /**
         * Test to see if event was successfully delivered.
         *
         * @return  'true' if event was delivered, otherwise 'false'.
         */
        public boolean eventDelivered ( )
        {
            // If the event delivery thread hasn't finished its push yet,
            // interrupt it, indicating failure of delivery to caller.
            if ( !observer.isNotified() )
            {
                try
                {
                    if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                        Debug.log( Debug.IO_STATUS, "Maximum-wait-time of [" + maxPushWait 
                                   + "] seconds has elapsed.  Interrupting consumer push thread that has taken too long ..." );
                    
                    observer.notifyObserver( );
                }
                catch ( Exception e )
                {
                    Debug.warning( e.toString() );
                }
            }
            
            return wasDelivered;
        }
        
        
        /**
         * Execute the notifier to push the event.
         *
         * @param NFObserver the observer monitoring the notifier's execution.
         */
        protected void executeNotifier ( NFObserver observer )
        {
            try
            {
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                {
                    Debug.log( Debug.IO_STATUS, "Event Service will wait for up to [" 
                               + maxPushWait + "] seconds for push to remote consumer to succeed." );
                 
                    Debug.log(Debug.IO_STATUS, "OnlyOnceEventConsumerPolicy.TimedConsumer.executeNotifier(): " + 
                                               "Pushing to the consumer event:\n" + eventData);
                }
                
                consumer.push( eventData );
                
                Debug.log(Debug.MSG_DATA, "OnlyOnceEventConsumerPolicy.TimedConsumer.executeNotifier(): Successfully pushed the event to the consumer.");
                
                // If we get here, the event has been delivered.
                wasDelivered = true;
            }
            catch ( Exception e )
            {
                Debug.error( "Failed to deliver event to remote consumer [" 
                             + consumer.toString() + "]:\n" + e.toString() );
            }
        }


        private ProxyPushSupplierImpl consumer = null;
        private String eventData = null;
        private boolean wasDelivered = false;
    }

    // The default wait time if unspecified in the configuration is 5 minutes (in seconds).
    private int maxPushWait = 300;
}

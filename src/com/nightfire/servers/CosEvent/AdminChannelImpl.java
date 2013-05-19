/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ReliableChannel.java#11 $
 */

package com.nightfire.servers.CosEvent;

import java.util.*;
import java.text.*;
import java.sql.*;

import org.omg.CORBA.*;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;
import org.omg.PortableServer.Servant;

import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.parser.xml.*;


/**
 * An event channel specialized for event channel administration.
 * Used to reset persistent events that should be re-processed.
 */
public class AdminChannelImpl extends EventChannelImpl
{
    /**
     * Name of channel providing channel administration facilities.
     */
    public static final String ADMIN_CHANNEL_NAME = "AdminChannel";
    
    /**
     * Name of node in XML event containing the name of the channel whose events should be reset.
     */
    public static final String CHANNEL_NAME_NODE = "ChannelName";
    
    /**
     * Name of node in XML event giving the oldest event to reset (optional).
     */
    public static final String DATE_FLOOR_NODE = "DateFloor";
    
    /**
     * Name of node in XML event giving the maximum error count to reset (optional).
     */
    public static final String RETRY_CEILING_NODE = "RetryCeiling";   
    
    /**
     * Name of node in XML event giving the unique identifier of the event to reset (optional).
     */
    public static final String EVENT_ID_NODE = "EventId";   
    
    /**
     * Format used to parse the date floor value, if present.
     */
    public static final String DATE_FORMAT = "M/d/yy:H";
    

    /**
     * Constructs the admin event channel.
     *
     * @param   orb  The CORBA orb to execute against.
     * @param   configProps  A container of additional channel configuration items.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid, or channel
     *                                 creation fails.
     */
    protected AdminChannelImpl ( org.omg.CORBA.ORB orb, Map configProps ) throws FrameworkException
    {
        super( orb, ADMIN_CHANNEL_NAME, configProps );
    }
    
    
    /**
     * Creates the event queue object.
     *
     * @param   configProps  A container of configuration items.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    protected EventQueue createQueue ( Map configProps ) throws FrameworkException
    {
        EventQueue queue = EventQueueFactory.create( EventQueueFactory.TRANSIENT );
        
        queue.initialize( configProps );
        
        return queue;
    }
    

    /**
     * Creates the event consumer policy object.
     *
     * @param   configProps  A container of configuration items.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    protected EventConsumerPolicy createEventConsumerPolicy ( Map configProps ) throws FrameworkException
    {
        return( new AdminEventConsumerPolicy() );
    }


    /**
     * Method invoked by supplier which decouples supplier push from consumer push.
     * This call enqueues the event and then notifies the event-delivery thread.
     * 
     * @param  any  The event to deliver.
     *
     * @exception  FrameworkException  Thrown on any errors.
     */
    protected void supplierPush ( Any any ) throws FrameworkException
    {
        /*
         * In order to decouple the supplier from the consumer, the following
         *  steps are performed:
         * 1. Deliver the event from a supplier to the channel's configured queue.
         * 2. Wake up a separate thread that will check the queue and deliver
         *    any messages to any connected consumers.
         */
        
        try
        {
            // The following call will throw a BAD_PARAM exception if the event is not a string.
            String eventValue = any.extract_string( );
            
            // Extract values from the XML event data and use them to populate
            // criteria object indicating which events should be reset.
            XMLMessageParser p = new XMLMessageParser( eventValue );
            
            Event criteria = new Event( );

            criteria.message = eventValue;
            
            // Channel name is the only required value.
            criteria.channelName = p.getValue( CHANNEL_NAME_NODE );
            
            if ( p.exists( DATE_FLOOR_NODE ) )
            {
                SimpleDateFormat sdf = new SimpleDateFormat( DATE_FORMAT );
                
                criteria.lastErrorTime = new java.sql.Timestamp( sdf.parse( p.getValue( DATE_FLOOR_NODE ) ).getTime() );
            }
            else
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Admin criteria item " + DATE_FLOOR_NODE 
                               + " was not specified, so using default date value of 0." );
                
                criteria.lastErrorTime = new java.sql.Timestamp( 0 );
            }

            if ( p.exists( RETRY_CEILING_NODE ) )
            {
                criteria.errorCount = Integer.parseInt( p.getValue( RETRY_CEILING_NODE ) );
            }
            else
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Admin criteria item " + RETRY_CEILING_NODE 
                               + " was not specified, so using default value of Integer.MAX_VALUE." );
                
                criteria.errorCount = Integer.MAX_VALUE;
            }
            
            if ( p.exists( EVENT_ID_NODE ) )
                criteria.id = Integer.parseInt( p.getValue( EVENT_ID_NODE ) );
            else
                criteria.id = 0;

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Event reset criteria:\n" + criteria.describe() );
            
            // Reset events in database.
            int numReset = DatabaseEventQueue.reset( criteria );
            
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Number of events reset in database [" + numReset + "]." );
            
            // Add the event to the queue.
            eventQueue.add( criteria );
            
            // If event was successfully enqueued, wake up thread to deliver any available events.
            alertDeliveryThread( );
        }
        catch ( Exception e )
        {
            throw new FrameworkException( "ERROR: Invalid admin event:\n" + e.getMessage() );
        }
    }


    /**
     * Method invoked by channel's event-delivery thread on consumers which 
     * decouples supplier push from consumer push.  This call is invoked
     * from the event-delivery thread.
     */
    protected synchronized void consumerPush ( )
    {
        try
        {
            // Populate event used as selection criteria for events to process.
            Event criteria = new Event( getName(), null );
            
            // Deliver event while:
            // 1. events are available, and 
            // 2. the channel is not being shut down.
            while ( eventQueue.hasNext( criteria ) )
            {
                // Don't deliver any events if channel is being shut down.
                if ( isChannelShuttingDown( ) )
                {
                    if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                        Debug.log( Debug.OBJECT_LIFECYCLE, "Interrupting event pushing since channel [" 
                                   + getName() + "] is being shut down." );
                    
                    break;
                }
                
                // Get the next available event off of the queue.
                Event event = eventQueue.next( );
                
                // Give it to policy object for deliver.
                EventStatus consumptionOutcome = eventConsumerPolicy.deliver( null, event );
                
                // Tell queue to deal with event based on outcome of delivery.
                eventQueue.update( event, consumptionOutcome );
            }
        }
        catch ( Exception e )
        {
            Debug.error( "Consumer push was interrupted due to error:\n" + e.toString() );
            
            Debug.error( Debug.getStackTrace( e ) );
        }
    }

    
    /**
     * Check to see if conditions exist indicating that an event should be delivered.
     *
     * @return  'true' if consumers are connected and events are available, otherwise 'false'.
     */
    public boolean readyToDeliverEvents ( )
    {
        // This implementation replaces that found in the class that this one extends.
        // It removes the requirement for connected consumers, as the channel itself
        // is the consumer.
        try
        {
            boolean eventsAvailable = eventQueue.hasNext( new Event( getName(), null ) );

            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "events-available? [" + eventsAvailable + "]." );

            if ( eventsAvailable )
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Ready to deliver events."  );

                return true;
            }
            else
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Not ready to deliver events."  );

                return false;
            }
        }
        catch ( Exception e )
        {
            Debug.error( e.toString() );
            Debug.error( Debug.getStackTrace( e ) );

            return false;
        }
    }


    // Internal class used to deliver administrative event to indicated channel.
    private class AdminEventConsumerPolicy implements EventConsumerPolicy
    {
        /**
         * Initialize the event consumer.
         *
         * @param  props  A container of configuration properties.
         *
         * @exception  FrameworkException  Thrown if configuration is invalid.
         */
        public void initialize ( Map props ) throws FrameworkException
        {
            // None required.
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
            // Ignore consumers, since we're delivering message directly to indicated channel.
            
            try
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Processing the following admin channel request:\n" + event.message );
                
                if ( !StringUtils.hasValue( event.channelName ) )
                {
                    throw new FrameworkException( "ERROR: Admin event does not specify a target channel name." );
                }
                                
                EventChannelImpl channel = (EventChannelImpl)availableChannels.get( event.channelName );
                
                if ( channel != null )
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                        Debug.log( Debug.MSG_STATUS, "Alerting channel [" + event.channelName 
                                   + "] that there may be events available for processing." );
                    
                    channel.alertDeliveryThread( );
                }
                else
                {
                    Debug.warning( " Channel named [" + event.channelName 
                                   + "] is not currently active." );
                }
                
                return( EventStatus.DELIVERY_SUCCESSFUL );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );
                
                return( EventStatus.DELIVERY_FAILED );
            }
        }

    
        /**
         * Get a human-readable description of the event consumer policy.
         *
         * @return  A description of the event consumer policy object.
         */
        public String describe ( )
        {
            return( StringUtils.getClassName( this ) );
        }
    }
}

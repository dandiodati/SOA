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
import org.omg.PortableServer.Servant;

import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.db.*;


/**
 * An implementation of the standard COS Event Service event channel. 
 */
public class EventChannelImpl extends EventChannelPOA
{
    /**
     * Iterated property giving event channel name.
     */
    public static final String CHANNEL_NAME_PROP = "EVENT_CHANNEL_NAME";

    /**
     * Iterated property flag indicating whether channel is persistent or not.
     */
    public static final String PERSISTENCE_PROP = "PERSISTENCE_FLAG";

    /**
     * Iterated property flag indicating whether _non_existent() check for object liveness should be skipped.
     */
    public static final String SKIP_NON_EXISTENT_CHECK_FLAG_PROP = "SKIP_NON_EXISTENT_CHECK_FLAG";


    /**
     * Name of the POA supporting event channels.
     */
    public static final String POA_NAME = "EVENT_CHANNEL_POA";


    /**
     * Constructs an event channel.
     *
     * @param   orb  The CORBA orb to execute against.
     * @param   channelName The name of the channel.
     * @param   configProps  A container of additional channel configuration items.
     *
     * @exception  FrameworkException  Thrown if configuration is invalid, or channel
     *                                 creation fails.
     */
    protected EventChannelImpl ( org.omg.CORBA.ORB orb, String channelName, Map configProps ) throws FrameworkException
    {
        this.channelName = channelName;

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating event channel with name [" + channelName + "] ..." );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) && (configProps != null) )
            Debug.log( Debug.SYSTEM_CONFIG, "Channel configuration properties:\n" 
                       + configProps.toString() );
        
        cpl = new CorbaPortabilityLayer( orb, POA_NAME );

        // Create the synchronized containers for the proxy objects.
        proxyPushConsumers = Collections.synchronizedList( new LinkedList() );
        proxyPushSuppliers = Collections.synchronizedList( new LinkedList() );

        // Create and initialize the desired queue type based on the value 
        // of the persistence flag configuration.
        eventQueue = createQueue( configProps );

        // Create and initialize the object implementing the event consumer policy.
        eventConsumerPolicy = createEventConsumerPolicy( configProps );

        // Start the event delivery thread.
        eventDeliveryThread = new EventDeliveryThread( this );
        
        if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) )
            Debug.log( Debug.NORMAL_STATUS, "Created event channel:\n" + describe() );


        cpl.activateObject( channelName, this );

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Exporting event channel [" 
                       + channelName + "] to COS Naming Service." );

        locator = new ObjectLocator( orb );
        
        locator.add(channelName, cpl.getObjectReference(this));

        // Add the newly-constructed channel to the map of available channels.
        availableChannels.put( channelName, this );
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
        // Create and initialize the desired queue type based on the value 
        // of the persistence flag configuration.
        String flagValue = getProperty( configProps, PERSISTENCE_PROP );

        if ( !StringUtils.hasValue( flagValue ) )
        {
            flagValue = "TRUE";

            Debug.warning( "Missing property named [" + PERSISTENCE_PROP 
                           + "], which will be defaulted to [" + flagValue + "]." );
        }
        
        boolean isPersistent = StringUtils.getBoolean( flagValue );
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Channel is persistent? [" + isPersistent + "]." );


        flagValue = getProperty( configProps, SKIP_NON_EXISTENT_CHECK_FLAG_PROP );

        if ( StringUtils.hasValue( flagValue ) )
            relaxNonExistentCheckFlag = StringUtils.getBoolean( flagValue );
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Relax non-existence CORBA object check? [" + relaxNonExistentCheckFlag + "]." );


        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating event queue ..." );

        EventQueue queue = null;

        if ( isPersistent )
            queue = EventQueueFactory.create( EventQueueFactory.PERSISTENT );
        else
            queue = EventQueueFactory.create( EventQueueFactory.TRANSIENT );

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
        // Create and initialize the object implementing the event consumer policy.
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating event consumer policy object ..." );
        
        EventConsumerPolicy ecp = EventConsumerPolicyFactory.create( EventConsumerPolicyFactory.ONLY_ONCE_DELIVERY );
        
        ecp.initialize( configProps );

        return ecp;
    }


    /**
     * Destroy the channel.
     */
    public void destroy ( )
    {
        if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) )
            Debug.log( Debug.NORMAL_STATUS, "Destroying event channel:\n" + describe() );

        // Mark the channel is being destroyed so that no new consumers or suppliers can connect to it.
        channelIsBeingDestroyed = true;

        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Removing event channel from COS Naming Service ..." );
            
            locator.remove( channelName );
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );

            Debug.warning( Debug.getStackTrace( e ) );
        }
        
        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Deactivating event channel object ..." );
            
            getCorbaPortabilityLayer().deactivateObject( this );
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );

            Debug.warning( Debug.getStackTrace( e ) );
        }
        
        if ( consumerAdmin != null ) 
        {
            try
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Disconnecting consumer admin object ..." );

                consumerAdmin.destroy( );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
                
                Debug.warning( Debug.getStackTrace( e ) );
            }
            
            consumerAdmin = null;
        }
        
        if (supplierAdmin != null) 
        {
            try
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Disconnecting supplier admin object ..." );

                supplierAdmin.destroy( );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
                
                Debug.warning( Debug.getStackTrace( e ) );
            }
            
            supplierAdmin = null;
        }
        
        synchronized( proxyPushConsumers ) 
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Disconnecting proxy push consumers ..." );
            
            ProxyPushConsumerImpl[] proxies = getProxyPushConsumersAsArray( );

            for ( int Ix = 0;  Ix < proxies.length;  Ix ++ )
            {
                try
                {
                    proxies[Ix].destroy( );
                }
                catch ( Exception e )
                {
                    Debug.warning( e.toString() );
                    
                    Debug.warning( Debug.getStackTrace( e ) );
                }
            }

            proxyPushConsumers.clear( );
        }
        
        synchronized( proxyPushSuppliers ) 
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Disconnecting proxy push suppliers ..." );

            ProxyPushSupplierImpl[] proxies = getProxyPushSuppliersAsArray( );

            for ( int Ix = 0;  Ix < proxies.length;  Ix ++ )
            {
                try
                {
                    proxies[Ix].destroy( );
                }
                catch ( Exception e )
                {
                    Debug.warning( e.toString() );
                    
                    Debug.warning( Debug.getStackTrace( e ) );
                }
            }

            proxyPushSuppliers.clear( );
        }
        
        eventDeliveryThread.shutdown( );

        eventQueue.shutdown( );

        // Remove the channel from the map of available channels.
        availableChannels.remove( channelName );

        Debug.log( Debug.NORMAL_STATUS, "Event channel [" + channelName + "] destroyed." );
    }


    /**
     * Get the CORBA portability layer object.
     *
     * @return  The Corba portability layer object.
     */
    protected CorbaPortabilityLayer getCorbaPortabilityLayer ( )
    {
        return cpl;
    }


    /**
     * Get the channel name.
     *
     * @return  The channel name.
     */
    protected String getName ( )
    {
        return channelName;
    }   


    /**
     * Returns the consumer admin that will be used to obtain proxy suppliers. 
     *
     * @return The consumer admin instance that will be used to obtain proxy suppliers.
     */
    public ConsumerAdmin for_consumers ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: for_consumers() called." );

        // Check that channel is not being shut down before continuing.
        if ( isChannelShuttingDown( ) )
        {
            EventUtils.handleWarning( "Consumer admin object is unavailable as event channel [" 
                                      + channelName + "] is shutting-down.", null );
        }
        
        // Create an instance on the first call.
        if ( consumerAdmin == null ) 
        {
            synchronized( this )
            {
                if ( consumerAdmin == null ) 
                {
                    try
                    {
                        consumerAdmin = new ConsumerAdminImpl( this );
                    
                        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                            Debug.log( Debug.OBJECT_LIFECYCLE, "Consumer admin object is now available:\n" 
                                       + consumerAdmin.toString() );
                    }
                    catch ( Exception e )
                    {
                        EventUtils.handleError( "Failed to create consumer admin object for channel [" 
                                                + channelName + "]", e );
                    }
                }
            }
        }
        
        return( ConsumerAdminHelper.narrow( cpl.getObjectReference( consumerAdmin ) ) );
    }


    /**
     * Returns the supplier admin that will be used to obtain proxy consumers.
     *
     * @return  The consumer admin instance that will be used to obtain proxy consumers.
     */
    public SupplierAdmin for_suppliers ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: for_suppliers() called." );

        // Check that channel is not being shut down before continuing.
        if ( isChannelShuttingDown( ) )
        {
            EventUtils.handleWarning( "Supplier admin object is unavailable as event channel [" 
                                      + channelName + "] is shutting-down.", null );
        }

        // Create an instance on the first call.
        if ( supplierAdmin == null ) 
        {
            synchronized( this )
            {
                if ( supplierAdmin == null ) 
                {
                    try
                    {
                        supplierAdmin = new SupplierAdminImpl( this );
                    
                        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                            Debug.log( Debug.OBJECT_LIFECYCLE, "Supplier admin object is now available:\n" 
                                       + supplierAdmin.toString() );
                    }
                    catch ( Exception e )
                    {
                        EventUtils.handleError( "Failed to create supplier admin object for channel [" 
                                                + channelName + "]", e );
                    }
                }
            }
        }

        return( SupplierAdminHelper.narrow( cpl.getObjectReference( supplierAdmin ) ) );
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
        
        String eventValue = null;
        
        try
        {
            // The following call will throw a BAD_PARAM exception if the event is not a string.
            eventValue = any.extract_string( );
            
            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Event contents from supplier:\n" + eventValue );
        }
        catch ( Exception e )
        {
            Debug.error( "Encountered invalid non-string Any contents from supplier:\n" 
                         + e.toString() );
            
            throw new FrameworkException( e.toString() );
        }
        
        Event e = new Event( channelName, eventValue );
        
        // Add the event to the queue.
        eventQueue.add( e );
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Current connected supplier/consumer counts: " 
                       + getSupplierConsumerCounts() );

        // If event was successfully enqueued, wake up thread to deliver any available events.
        alertDeliveryThread( );
    }


    /**
     * Method invoked by channel's event-delivery thread on consumers which 
     * decouples supplier push from consumer push.  This call is invoked
     * from the event-delivery thread.
     */
    protected synchronized void consumerPush ( )
    {
        if (Debug.isLevelEnabled(Debug.MSG_DATA))
        {
            Debug.log(Debug.MSG_DATA, "EventChannelImpl.consumerPush(): Trying to push event through channel [" + 
                                       channelName + "] ...");    
        }
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Current connected supplier/consumer counts: " 
                       + getSupplierConsumerCounts() );

        if ( proxyPushSuppliers.size() == 0 )
        {
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Skipping event pushing since channel [" 
                           + channelName + "] doesn't have any registered consumers." );
            
            eventQueue.noConsumersAvailable( );
            
            return;
        }
        
        /*
         * While consumers are connected, and events are available to be 
         * delivered from queue, push events to each consumer and dequeue
         * as appropriate.
         */
        try
        {
            // Populate event used as selection criteria for events to process.
            Event criteria = new Event( channelName, null );
            
            // Deliver event while:
            // 1. events are available, and 
            // 2. consumers are available, and
            // 3. the channel is not being shut down.
            while ( eventQueue.hasNext( criteria ) )
            {
                // Don't deliver any events if channel is being shut down.
                if ( isChannelShuttingDown( ) )
                {
                    if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                        Debug.log( Debug.OBJECT_LIFECYCLE, "Interrupting event pushing since channel [" 
                                   + channelName + "] is being shut down." );
                    
                    break;
                }
                
                if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Current connected supplier/consumer counts: " 
                               + getSupplierConsumerCounts() );

                // We re-get the list of consumers each time, so that any list updates
                // (removals, additions) will take effect.
                ProxyPushSupplierImpl[] consumers = getProxyPushSuppliersAsArray( );
                
                if ( consumers.length == 0 )
                {
                    if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                        Debug.log( Debug.OBJECT_LIFECYCLE, "Interrupting event pushing since channel [" 
                                   + channelName + "] doesn't have any registered consumers." );
                    
                    eventQueue.noConsumersAvailable( );

                    break;
                }
                
                // Get the next available event off of the queue.
                Event event = eventQueue.next( );
                
                // Give it to policy object for deliver.
                EventStatus consumptionOutcome = eventConsumerPolicy.deliver( consumers, event );
                
                // Tell queue to deal with event based on outcome of delivery.
                eventQueue.update( event, consumptionOutcome );
                
                // Use the last event processed as the new criteria for selecting the
                // next one to process.
                criteria = event;
            }
        }
        catch ( Exception e )
        {
            Debug.error( "Consumer push was interrupted due to error:\n" + e.toString() );

            Debug.error( Debug.getStackTrace( e ) );
        }
        
        if (Debug.isLevelEnabled(Debug.MSG_DATA))
        {
            Debug.log(Debug.MSG_DATA, "EventChannelImpl.consumerPush(): Successfully pushed event through channel [" + 
                                       channelName + "] ...");    
        }
    }


    /**
     * Check to see if conditions exist indicating that an event should be delivered.
     *
     * @return  'true' if consumers are connected and events are available, otherwise 'false'.
     */
    public boolean readyToDeliverEvents ( )
    {
        try
        {
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Current connected supplier/consumer counts: " 
                           + getSupplierConsumerCounts() );

            boolean consumersAvailable = proxyPushSuppliers.size() > 0;

            boolean eventsAvailable = eventQueue.hasNext( new Event( channelName, null ) );

            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "consumers-available? [" + consumersAvailable 
                           + "], events-available? [" + eventsAvailable + "]." );

            if ( eventsAvailable && !consumersAvailable )
                eventQueue.noConsumersAvailable( );

            if ( consumersAvailable && eventsAvailable )
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


    /**
     * Check to see if _non_existent() liveness check against CORBA objects should be skipped.
     *
     * @return  'true' if liveness check should be skipped.
     */
    public boolean relaxNonExistentCheck ( )
    {
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Relax non-existent check? [" + relaxNonExistentCheckFlag + "]." );

        return relaxNonExistentCheckFlag;
    }


    /**
     * Add a proxy push consumer to the event channel.  As a side effect, the method
     * checks for stale consumers in its internal container and removes any that are found.
     *
     * @param   consumer  The proxy push consumer to add.
     */
    protected void addProxyPushConsumer ( ProxyPushConsumerImpl consumer )
    {
        // First, check for any stale proxies and remove them.
        ProxyPushConsumerImpl[] proxyConsumers = getProxyPushConsumersAsArray( );
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channel [" + channelName 
                       + "]: Checking for stale proxy push consumers.  Current count is [" + 
                       proxyConsumers.length + "]." );
        
        for ( int Ix = 0;  Ix < proxyConsumers.length;  Ix ++ )
        {
            ProxyPushConsumerImpl proxy = proxyConsumers[ Ix ];
            
            try
            {
                if ( proxy.peerDisconnected() ) 
                {
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Found a stale proxy push consumer which will be removed." );
                    
                    proxy.destroy( );
                }
            }
            catch( Exception e )
            {
                Debug.warning( e.toString() );
                
                Debug.warning( Debug.getStackTrace( e ) );
            }
        }
        
        // Now, add the given consumer to the channel's container.
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channel [" + channelName 
                       + "]: Adding the following proxy push consumer to list:\n" + consumer.toString() );
        
        proxyPushConsumers.add( consumer );
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Successfully added proxy push consumer to event channel [" 
                       + channelName + "].  Current count is [" + proxyPushConsumers.size() + "]." );
    } 


    /**
     * Remove a proxy push consumer from the event channel.
     *
     * @param   consumer  The proxy push consumer to remove.
     */
    protected void removeProxyPushConsumer ( ProxyPushConsumerImpl consumer )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channel [" + channelName 
                       + "]: Removing the following proxy push consumer from list:\n" + consumer.toString() );
        
        boolean inList = proxyPushConsumers.remove( consumer );
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Removed proxy push consumer from event channel [" 
                       + channelName + "]? [" + inList + "].  Current count is [" + proxyPushConsumers.size() + "]." );
    }

    
    /**
     * Add a proxy push supplier to the event channel.  As a side effect, the method
     * checks for stale suppliers in its internal container and removes any that are found.
     *
     * @param   supplier  The proxy push supplier to add.
     */
    protected void addProxyPushSupplier ( ProxyPushSupplierImpl supplier )
    {
        ProxyPushSupplierImpl[] proxySuppliers = getProxyPushSuppliersAsArray( );
        
        // First, check for any stale proxies and remove them.
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channel [" + channelName 
                       + "]: Checking for stale proxy push suppliers. Current count is [" + 
                       proxySuppliers.length + "]." );
        
        for ( int Ix = 0;  Ix < proxySuppliers.length;  Ix ++ )
        {
            ProxyPushSupplierImpl proxy = proxySuppliers[ Ix ];
            
            try
            {
                if ( proxy.peerDisconnected() ) 
                {
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Found a stale proxy push supplier which will be removed." );
                    
                    proxy.destroy( );
                }
            }
            catch( Exception e )
            {
                Debug.warning( e.toString() );
                
                Debug.warning( Debug.getStackTrace( e ) );
            }
        }
        
        // Now, add the given supplier to the channel's container.
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channel [" + channelName 
                       + "]: Adding the following proxy push supplier to list:\n" + supplier.toString() );
        
        proxyPushSuppliers.add( supplier );
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Successfully added proxy push supplier to event channel [" 
                       + channelName + "]. Current count is [" + proxyPushSuppliers.size() + "]." );
        
        // Wake up thread to deliver any available events.
        alertDeliveryThread( );
    } 
    
    
    /**
     * Remove a proxy push supplier from the event channel.
     *
     * @param   supplier  The proxy push supplier to remove.
     */
    protected void removeProxyPushSupplier ( ProxyPushSupplierImpl supplier )
    {
        // Deactivate the proxy first before remove it.
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channel [" + channelName 
                       + "]: Removing the following proxy push supplier from list:\n" + supplier.toString() );
        
        boolean inList = proxyPushSuppliers.remove( supplier );
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Successfully removed proxy push supplier from event channel [" 
                       + channelName + "]? [" + inList + "].  Current count is [" + proxyPushSuppliers.size() + "]." );
    } 
    
    
    /**
     * Get a snapshot copy of the proxy push supplier list as an array.
     *
     * @return  An array of proxy push suppliers.
     */
    protected ProxyPushSupplierImpl[] getProxyPushSuppliersAsArray ( )
    {
        synchronized( proxyPushSuppliers )
        {
            return( (ProxyPushSupplierImpl[])proxyPushSuppliers.toArray( new ProxyPushSupplierImpl[ proxyPushSuppliers.size() ] ) );
        }
    }
    
    
    /**
     * Get a snapshot copy of the proxy push consumer list as an array.
     *
     * @return  An array of proxy push consumers.
     */
    protected ProxyPushConsumerImpl[] getProxyPushConsumersAsArray ( )
    {
        synchronized( proxyPushConsumers )
        {
            return( (ProxyPushConsumerImpl[])proxyPushConsumers.toArray( new ProxyPushConsumerImpl[ proxyPushConsumers.size() ] ) );
        }
    }
    
    
    /**
     * Test to see if channel is currently shutting-down.
     *
     * @return  'true' if channel is shutting-down, otherwise 'false'.
     */
    protected boolean isChannelShuttingDown ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Channel is shutting down? [" + channelIsBeingDestroyed + "]." );

        return channelIsBeingDestroyed;
    }


    /**
     * Alert the delivery thread, causing it to wake up if it is waiting
     * for something to happen.
     */
    protected void alertDeliveryThread ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Notifying event delivery thread ..." );

        eventDeliveryThread.alert( );

        Debug.log( Debug.OBJECT_LIFECYCLE, "Done notifying event delivery thread." );
    }


    /**
     * Get a human-readable description of the channel.
     *
     * @return  A description of the channel.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Event Channel [" );
        sb.append( channelName );
        sb.append( "]\n" );

        sb.append( "Event delivery policy:\n" );
        sb.append( eventConsumerPolicy.describe() );
        sb.append( "\n" );

        sb.append( "Event queue type:\n" );
        sb.append( eventQueue.describe() );
        sb.append( "\n" );

        sb.append( "Skip CORBA object existence check [" );
        sb.append( relaxNonExistentCheckFlag );
        sb.append( "]\n" );

        sb.append( "Push consumer count [" );
        sb.append( proxyPushConsumers.size() );
        sb.append( "], Push supplier count [" );
        sb.append( proxyPushSuppliers.size() );
        sb.append( "]" );

        return( sb.toString() );
    }


    /**
     * Get all of the names of configuration properties that
     * could be used to configure an event channel.
     *
     * @return  An array of configuration property names.
     */
    public static String[] getConfigPropertyNames ( )
    {
        // This method allows all of the sub-components that make up a channel
        // and require configuration to give the names of their configuation
        // properties to their containing parent for loading from the database.

        List names = new LinkedList( );

        // Combine all of the sub-component names into one array
        // to return to caller.
        addToList( names, EventQueueFactory.getConfigPropertyNames() );
        
        addToList( names, EventConsumerPolicyFactory.getConfigPropertyNames() );
        
        addToList( names, additionalConfigPropNames );
        
        return( (String[])names.toArray( new String[ names.size() ] ) );
    }
    
    
    /**
     * Add the items from the array to the list.
     *
     * @param  target  The container to populate.
     * @param  source  The array source.
     */
    protected static void addToList ( List target, String[] source )
    {
        for ( int Ix = 0;  Ix < source.length;  Ix ++ )
            target.add( source[Ix] );
    }


    /**
     * Get the named property.
     *
     * @param  props  A container of named property values.
     * @param  name  The name of the property.
     *
     * @return  The named property's value, or null if not found.
     */
    protected String getProperty ( Map props, String name )
    {
        return( (String)props.get( name ) );
    }


    /**
     * Get a human readable status of the connected consumers/suppliers (counts).
     *
     * @return  String containing connected consumer and supplier counts.
     */
    protected String getSupplierConsumerCounts ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "suppliers [" );
        sb.append( proxyPushConsumers.size() );
        sb.append( "], consumers [" );
        sb.append( proxyPushSuppliers.size() );
        sb.append( "]" );

        return( sb.toString() );
    }

    // A list of additional property names (other than the channel name)
    // that can be used to configure an event channel.
    protected static String[] additionalConfigPropNames = {
        PERSISTENCE_PROP,
        SKIP_NON_EXISTENT_CHECK_FLAG_PROP
    };


    private String channelName;

    // By default, we perform the _non_existent() check against CORBA objects.
    private boolean relaxNonExistentCheckFlag = false;

    protected CorbaPortabilityLayer cpl;

    private ObjectLocator locator;

    // The queue used to decouple event suppliers from consumers.
    protected EventQueue eventQueue;

    // The object implementing the event consumer policy.
    protected EventConsumerPolicy eventConsumerPolicy;

    private ConsumerAdminImpl consumerAdmin;
    private SupplierAdminImpl supplierAdmin;
    
    private List proxyPushConsumers;
    private List proxyPushSuppliers;

    protected EventDeliveryThread eventDeliveryThread;

    // Flag indicating whether channel is in the process of being destroyed.
    // The field is declared 'volatile' so that any thread must reconcile its working copy of the field with the 
    // master copy every time it accesses the variable. Moreover, operations on the master copies of one or more 
    // volatile variables on behalf of a thread are performed by the main memory in exactly the order that the 
    // thread requested.  (See Java Language Specification, section 8.3.1.4 "volatile Fields").
    protected volatile boolean channelIsBeingDestroyed = false;

    // Global map of event channels.
    protected static Map availableChannels = Collections.synchronizedMap( new HashMap() );
}

/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ProxyPushConsumerImpl.java#4 $
 */


package com.nightfire.servers.CosEvent;

import org.omg.CORBA.*;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.monitor.*;


/**
 * An implementation of the ProxyPushConsumer interface.
 */
public class ProxyPushConsumerImpl extends ProxyPushConsumerPOA
{
    /**
     * Construct a proxy push consumer against the given channel.
     *
     * @param  ec  The event channel that the proxy push consumer is associated with.
     *
     * @exception  FrameworkException  Thrown if channel is invalid.
     */
    protected ProxyPushConsumerImpl ( EventChannelImpl ec ) throws FrameworkException
    {
        if ( ec == null )
        {
            throw new FrameworkException( "ERROR: Attempt to create proxy push consumer object against null event channel." );
        }
        
        channel = ec;
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Activating proxy push consumer for channel [" 
                       + channel.getName() + "]." );
        
        // Activate the CORBA object.
        channel.getCorbaPortabilityLayer().activateObject( this );
    }


    /**
     * Destroy the proxy push consumer object.
     */
    protected void destroy ( )
    {
        // Remove this proxy from the channel's list of proxies to manage.
        channel.removeProxyPushConsumer( this );
        
        try 
        {
            if ( peerDisconnected() ) 
                Debug.log( Debug.OBJECT_LIFECYCLE, "Remote push supplier is already disconnected." );
            else
            {
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Disconnecting remote push supplier from channel [" 
                               + channel.getName() + "] ..." );
                
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                {
                    Debug.log( Debug.IO_STATUS, "Disconnecting push supplier:\n" + pushSupplier.toString() + "\nwith IOR:\n" 
                               + channel.getCorbaPortabilityLayer().getORB().object_to_string( pushSupplier ) );
                }

                Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: disconnect_push_supplier() called on remote push supplier." );
                
                pushSupplier.disconnect_push_supplier( );
            }
        }
        catch ( Exception e ) 
        {
            Debug.warning( "Failed to disconnect remote push supplier:\n" 
                           + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
        
        pushSupplier = null;
        
        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Deactivating proxy push consumer object ..." );
        
            channel.getCorbaPortabilityLayer().deactivateObject( this );
        }
        catch ( Exception e )
        {
            Debug.warning( "Failed to destroy proxy push consumer object:\n" 
                           + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
    }


    /**
     * Connects given remote supplier to this proxy, and therefore to the
     * event service.
     *
     * @param  supplier  The remote push supplier instance to connect.
     *
     * @exception  AlreadyConnected  Thrown if a remote supplier is already connected.
     */
    public void connect_push_supplier ( PushSupplier supplier )
        throws AlreadyConnected
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: connect_push_supplier() called." );

        // Check that channel is not being shut down before continuing.
        if ( channel.isChannelShuttingDown( ) )
        {
            EventUtils.handleWarning( "Event channel [" + channel.getName()
                                      + "] is shutting-down and is not accepting supplier connections.", null );
        }

        if ( supplier == null )
            Debug.warning( "Null remote supplier reference passed as argument to connect_push_supplier() call." );
        else
        {
            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Proxy push consumer connecting remote push supplier:\n" 
                           + supplier.toString() + " to channel [" + channel.getName() + "]." );
        }

        // We only allow one push supplier to connect to use.
        if ( pushSupplier != null ) 
        {
            Debug.warning( "Throwing AlreadyConnected exception back to remote client, since Push Supplier is already connected." );
            
            throw new AlreadyConnected( );
        }
        
        pushSupplier = supplier;
        
        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
        {
            Debug.log( Debug.IO_STATUS, "Connecting push supplier:\n" + pushSupplier.toString() + "\nwith IOR:\n" 
                       + channel.getCorbaPortabilityLayer().getORB().object_to_string( pushSupplier ) );
        }

        // After a push supplier is given, we add this proxy push consumer to the 
        // channel's list so that it can be managed by channel.
        channel.addProxyPushConsumer( this );
    }


    /**
     * Pushes the event to the event channel.
     *
     * @param  any  The event to be pushed.
     *
     * @exception  Disconnected  Thrown if channel is being destroyed or 
     *                           the supplier disconnected or push to consumer failed.
     */
    public void push ( Any any )
        throws Disconnected
    {
        Thread currentThread = Thread.currentThread( );

        String origThreadName = currentThread.getName( );

        currentThread.setName( channel.getName() + ":supplier-" + currentThread.hashCode() );
   
        long startTime = -1;

        ThreadMonitor.ThreadInfo tmti = null;

        try
        {
            tmti = ThreadMonitor.start( "Receiving event on event channel [" + channel.getName() + "]." );

            Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: push() called." );
        
            // Check that channel is not being shut down before continuing.
            if ( channel.isChannelShuttingDown( ) )
            {
                Debug.warning( "Event channel [" + channel.getName()
                               + "] is shutting-down and is not accepting push requests, so throwing Disconnected exception back to client." );
            
                throw new Disconnected( );
            }
        
            try 
            {
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Received the following Any from remote supplier push to channel:\n" + any );
            
                if ( pushSupplier == null ) 
                {
                    Debug.warning( "Skipping event push and throwing Disconnected exception back to remote supplier, " + 
                                   "since remote supplier reference is null and therefore unavailable." );
                
                    throw new Disconnected( );
                }
            
                if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
                    startTime = System.currentTimeMillis( );

                // Push to channel.
                channel.supplierPush( any );
            }
            catch ( Exception e ) 
            {
                EventUtils.handleError( "Failed to push event", e );
            }
        }
        finally
        {
            if ( Debug.isLevelEnabled( Debug.BENCHMARK ) && (startTime > 0) )
            {
                long stopTime = System.currentTimeMillis( );
                
                Debug.log( Debug.BENCHMARK, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  "
                           + "Time for remote supplier to push event to channel." );
            }

            currentThread.setName( origThreadName );

            ThreadMonitor.stop( tmti );
        }
    }


    /**
     * Disconnects the connected push supplier instance.
     */
    public void disconnect_push_consumer ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: disconnect_push_consumer() called." );

        destroy( );
    }

    
    /**
     * Check whether the consumer was and still is connected.
     */
    protected boolean peerDisconnected ( ) 
    {
        if ( channel.relaxNonExistentCheck() )
            return false;
        else
            return( EventUtils.isObjectDead( pushSupplier ) );
    }

    
    private EventChannelImpl channel;
    
    private PushSupplier pushSupplier;
}

/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ProxyPushSupplierImpl.java#4 $
 */


package com.nightfire.servers.CosEvent;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;
import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.monitor.*;


/**
 * An implementation of the ProxyPushSupplier interface.
 */
public class ProxyPushSupplierImpl extends ProxyPushSupplierPOA
{
    /**
     * Construct a proxy push supplier against the given channel.
     *
     * @param  ec  The event channel that the proxy push supplier is associated with.
     *
     * @exception  FrameworkException  Thrown if channel is invalid.
     */
    protected ProxyPushSupplierImpl ( EventChannelImpl ec ) throws FrameworkException
    {
        if ( ec == null )
        {
            throw new FrameworkException( "ERROR: Attempt to create proxy push supplier object against null event channel." );
        }
        
        channel = ec;
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Activating proxy push supplier for channel [" 
                       + channel.getName() + "]." );

        // Activate the CORBA object.
        channel.getCorbaPortabilityLayer().activateObject( this );
    }


    /**
     * Destroy the proxy push supplier object.
     */
    protected void destroy ( )
    {
        // Remove this proxy from the channel's list of proxies to manage.
        channel.removeProxyPushSupplier( this );
        
        try 
        {
            if ( peerDisconnected() )
                Debug.log( Debug.MSG_LIFECYCLE, "Remote push consumer has already been disconnected." );
            else
            {
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Disconnecting remote push consumer from channel [" 
                               + channel.getName() + "] ..." );
                
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                {
                    Debug.log( Debug.IO_STATUS, "Disconnecting push consumer:\n" + pushConsumer.toString() + "\nwith IOR:\n" 
                               + channel.getCorbaPortabilityLayer().getORB().object_to_string( pushConsumer ) );
                }

                Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: disconnect_push_consumer() called on remote push consumer." );

                pushConsumer.disconnect_push_consumer( );
            }
        }
        catch ( Exception e ) 
        {
            Debug.warning( "Failed to disconnect remote push consumer:\n" 
                           + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
        
        pushConsumer = null;
        
        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Deactivating proxy push supplier object ..." );
        
            channel.getCorbaPortabilityLayer().deactivateObject( this );
        }
        catch ( Exception e )
        {
            Debug.warning( "Failed to destroy proxy push supplier object:\n" 
                           + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
    }


    /**
     * Connects given remote consumer to this proxy, and therefore to the
     * event service.
     *
     * @param  consumer  The remote push consumer instance to connect.
     *
     * @exception  AlreadyConnected  Thrown if  a remote consumer is already connected.
     */
    public void connect_push_consumer ( PushConsumer consumer )
        throws AlreadyConnected
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: connect_push_consumer() called." );

        // Check that channel is not being shut down before continuing.
        if ( channel.isChannelShuttingDown( ) )
        {
            EventUtils.handleWarning( "Event channel [" + channel.getName()
                                      + "] is shutting-down and is not accepting consumer connections.", null );
        }

        if ( consumer == null )
            Debug.warning( "Null remote consumer reference passed as argument to connect_push_consumer() call." );
        else
        {
            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Proxy push supplier connecting remote push consumer:\n" 
                           + consumer.toString() );
        }

        // We only allow one push consumer to connect to use.
        if ( pushConsumer != null ) 
        {
            Debug.warning( "A remote push consumer is already connected to channel, " 
                           + "so throwing an AlreadyConnected exception back to remote client." );

            throw new AlreadyConnected( );
        }
        
        pushConsumer = consumer;
        
        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
        {
            Debug.log( Debug.IO_STATUS, "Connecting push consumer:\n" + pushConsumer.toString() + "\nwith IOR:\n" 
                       + channel.getCorbaPortabilityLayer().getORB().object_to_string( pushConsumer ) );
        }

        // After a push consumer is given, we add this proxy push supplier to the 
        // channel's list so that it can be managed by channel.
        channel.addProxyPushSupplier( this );
    }


    /**
     * Disconnects the current push consumer instance.
     */
    public void disconnect_push_supplier ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: disconnect_push_supplier() called." );

        destroy( );
    }


    /**
     * Push the event to the remote event consumer.
     *
     * @param  eventData  The event string to push.
     *
     * @exception  Disconnected  Thrown if consumer is not connected.
     * @exception  FrameworkException  Thrown if push fails.
     */
    protected void push ( String eventData ) throws Disconnected, FrameworkException
    {
        Thread currentThread = Thread.currentThread( );

        String origThreadName = currentThread.getName( );

        currentThread.setName( channel.getName() + ":consumer-" + currentThread.hashCode() );
   
        try
        {
            boolean delivered = false;

            long startTime = -1;
            
            ThreadMonitor.ThreadInfo tmti = null;

            try
            {
                tmti = ThreadMonitor.start( "Pushing event to consumer from event channel [" + channel.getName() + "]." );

                // Encapsulate the event string in a CORBA any.
                Any any = channel.getCorbaPortabilityLayer().getORB().create_any( );
            
                any.insert_string( eventData );
            
            
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                {
                    Debug.log( Debug.MSG_STATUS, "\n" + LINE + "\nDelivering event to consumer:\n" 
                               + pushConsumer.toString() + "\n\nwith IOR:\n" 
                               + channel.getCorbaPortabilityLayer().getORB().object_to_string( pushConsumer ) 
                               + "\n\nEvent contents:\n" + eventData );
                }
            
                Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: push() called on remote push consumer." );
            
                if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
                    startTime = System.currentTimeMillis( );
            
                // Push the event to the remote consumer connected to this proxy.
                pushConsumer.push( any );
                        
                delivered = true;
            }
            catch ( Exception e )
            {
                // Remember exception for clients of this class.
                pushException = e;

                String errMsg = "Remote event consumer threw exception during push() call:\n" 
                    + e.toString( );
            
                Debug.error( errMsg + "\n" + Debug.getStackTrace( e ) );
            
                if ( e instanceof Disconnected )
                    throw (Disconnected)e;
                else
                    throw new FrameworkException( errMsg );
            }
            finally
            {
                if ( Debug.isLevelEnabled( Debug.BENCHMARK ) && (startTime > 0) )
                {
                    long stopTime = System.currentTimeMillis( );
                
                    Debug.log( Debug.BENCHMARK, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  "
                               + "Time to push event to remote push consumer." );
                }
            
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Event successfully delivered to remote push consumer? [" 
                               + delivered + "].\n" + LINE );

                ThreadMonitor.stop( tmti );
            }    
        }
        finally
        {
            currentThread.setName( origThreadName );
        }
    }
    
    
    /**
     * Get any exception resulting from remote consumer push.
     *
     * @return  The exception thrown by the remote push consumer, 
     *          or null if none was thrown.
     */
    protected Exception getConsumerException ( )
    {
        return pushException;
    }


    /**
     * Check whether the consumer was and still is connected.
     */
    protected boolean peerDisconnected ( ) 
    {
        if ( channel.relaxNonExistentCheck() )
            return false;
        else
            return( EventUtils.isObjectDead( pushConsumer ) );
    }
    
    
    private EventChannelImpl channel;
    
    private PushConsumer pushConsumer;

    private Exception pushException;

    private static final String LINE 
        = "*******************************************************************************";
}


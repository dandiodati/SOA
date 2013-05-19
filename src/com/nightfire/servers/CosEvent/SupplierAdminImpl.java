/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/SupplierAdminImpl.java#3 $
 */


package com.nightfire.servers.CosEvent;

import org.omg.CORBA.*;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

import com.nightfire.framework.util.*;


/**
 * An implementation of the SupplierAdmin interface.
 */
public class SupplierAdminImpl extends SupplierAdminPOA
{
    /**
     * Constructs a supplier admin object against the given event channel.
     *
     * @param  ec  The event channel that the supplier admin is associated with.
     *
     * @exception  FrameworkException  Thrown if channel is invalid.
     */
    protected SupplierAdminImpl ( EventChannelImpl ec ) throws FrameworkException
    {
        if ( ec == null )
        {
            throw new FrameworkException( "ERROR: Attempt to create supplier admin object against null event channel." );
        }

        channel = ec;
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating supplier admin for channel [" 
                      + channel.getName() + "]." );

        channel.getCorbaPortabilityLayer().activateObject( this );
    }

    
    /**
     * Destroy the supplier admin object.
     */
    protected void destroy ( )
    {
        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Deactivating supplier admin object ..." );
            
            channel.getCorbaPortabilityLayer().deactivateObject( this );
        }
        catch ( Exception e )
        {
            Debug.warning( "Failed to destroy supplier admin object:\n" 
                           + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
    }
    

    /**
     * Get a proxy push consumer.
     *
     * @return A new ProxyPushConsumer instance.
     */
    public ProxyPushConsumer obtain_push_consumer ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: obtain_push_consumer() called." );

        // Check that channel is not being shut down before continuing.
        if ( channel.isChannelShuttingDown( ) )
        {
            EventUtils.handleWarning( "Supplier admin object is unavailable to provide proxy push consumer as event channel [" 
                                      + channel.getName() + "] is shutting-down.", null );
        }

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Getting proxy push consumer for channel [" 
                       + channel.getName() + "]." );
        
        try
        {
            ProxyPushConsumerImpl proxy = new ProxyPushConsumerImpl( channel );
            
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Proxy push consumer is:\n" 
                           + proxy.toString() );

            return( ProxyPushConsumerHelper.narrow( channel.getCorbaPortabilityLayer().getObjectReference( proxy ) ) );
        }
        catch ( Exception e )
        {
            EventUtils.handleError( "Could not obtain proxy push consumer for channel [" 
                                    + channel.getName() + "]", e );
        }

        return null;
    }
    

    /**
     * Get a proxy pull consumer.  This mode is currently not supported, so
     * the method always returns null.
     * (Actually, it throws an org.omg.CORBA.INTERNAL back to the caller.)
     *
     * @return  null.
     */
    public ProxyPullConsumer obtain_pull_consumer ( )
    {
        EventUtils.handleWarning( "SupplierAdminImpl.obtain_pull_consumer() is not implemented as pull event model is not supported.",
                                  null );

        return null;
    }


    private EventChannelImpl channel;
}

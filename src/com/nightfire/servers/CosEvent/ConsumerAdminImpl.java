/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ConsumerAdminImpl.java#3 $
 */


package com.nightfire.servers.CosEvent;

import org.omg.CORBA.*;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

import com.nightfire.framework.util.*;


/**
 * An implementation of the ConsumerAdmin interface.
 */
public class ConsumerAdminImpl extends ConsumerAdminPOA
{
    /**
     * Constructs a consumer admin object against the given event channel.
     *
     * @param  ec  The event channel that the consumer admin is associated with.
     *
     * @exception  FrameworkException  Thrown if channel is invalid.
     */
    protected ConsumerAdminImpl ( EventChannelImpl ec ) throws FrameworkException
    {
        if ( ec == null )
        {
            throw new FrameworkException( "ERROR: Attempt to create consumer admin object against null event channel." );
        }

        channel = ec;
        
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating consumer admin for channel [" 
                       + channel.getName() + "]." );
        
        channel.getCorbaPortabilityLayer().activateObject( this );
    }
    
    
    /**
     * Destroy the consumer admin object.
     */
    protected void destroy ( )
    {
        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Deactivating consumer admin object ..." );
            
            channel.getCorbaPortabilityLayer().deactivateObject( this );
        }
        catch ( Exception e )
        {
            Debug.warning( "Failed to destroy consumer admin object:\n" 
                           + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
    }


    /**
     * Get a proxy push supplier.
     *
     * @return A new ProxyPushSupplier instance.
     */
    public ProxyPushSupplier obtain_push_supplier ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA: obtain_push_supplier() called." );

        // Check that channel is not being shut down before continuing.
        if ( channel.isChannelShuttingDown( ) )
        {
            EventUtils.handleWarning( "Consumer admin object is unavailable to provide proxy push supplier as event channel [" 
                                      + channel.getName() + "] is shutting-down.", null );
        }

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Getting proxy push supplier for channel [" 
                       + channel.getName() + "]." );

        try
        {
            ProxyPushSupplierImpl proxy = new ProxyPushSupplierImpl( channel );
            
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Proxy push supplier is:\n" 
                           + proxy.toString() );

            return( ProxyPushSupplierHelper.narrow( channel.getCorbaPortabilityLayer().getObjectReference( proxy ) ) );
        }
        catch ( Exception e )
        {
            EventUtils.handleError( "Could not obtain proxy push supplier for channel [" 
                                    + channel.getName() + "]", e );
        }

        return null;
    }
    
    
    /**
     * Get a proxy pull supplier.  This mode is currently not supported, so
     * the method always returns null.  
     * (Actually, it throws an org.omg.CORBA.INTERNAL back to the caller.)
     *
     * @return  null.
     */
    public ProxyPullSupplier obtain_pull_supplier ( )
    {
        EventUtils.handleWarning( "ConsumerAdminImpl.obtain_pull_supplier() is not implemented as pull event model is not supported.", 
                                  null );

        return null;
    }
    
    
    private EventChannelImpl channel;
}

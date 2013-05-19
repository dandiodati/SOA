/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ProxyPushSupplierImpl.java#4 $
 */


package com.nightfire.servers.CosEvent;


import org.omg.CORBA.*;

import com.nightfire.framework.util.*;


/**
 * Assorted utilities for the Event Service.
 */
public class EventUtils
{
    /**
     * Useful conversion for time configuration values in seconds.
     */
    public static final int MSEC_PER_SEC = 1000;

    /**
     * Useful conversion for time configuration values in minutes.
     */
    public static final int MSEC_PER_MIN = 60 * MSEC_PER_SEC;


    /**
     * Test to see if given object is 'dead' with respect to CORBA access.
     * 
     * @param  candidate  Candidate CORBA object to check.
     *
     * @return  'true' if object is null, or CORBA non-existence
     *          tests indicate that it is dead.
     */
    public static boolean isObjectDead ( org.omg.CORBA.Object candidate )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Testing CORBA object for live-ness ..." );
        
        if ( candidate == null )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Null CORBA object is not live." );
            
            return true;
        }
        
        try
        {
            boolean dead = candidate._non_existent( );
            
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "CORBA object is dead? [" + dead + "]." );
            
            return dead;
        }
        catch ( Exception e )
        {
            Debug.warning( "CORBA: _non_existent() call against remote CORBA object failed with the following thrown exception:\n"
                           + e.toString() );
            
            Debug.warning( Debug.getStackTrace( e ) );

            // Make a best-guess effort as to the object's actual status.  
            // We can't rely on the minor codes to assist as they are vendor-specific.
            if ( (e instanceof COMM_FAILURE) || (e instanceof OBJECT_NOT_EXIST) || 
                 (e instanceof TRANSIENT) || (e instanceof INV_OBJREF) )
            {
                Debug.warning( "CORBA: Interpreting exceptions of this type as being due to a dead remote object." ); 
            
                return true;
            }
            else
            {
                Debug.warning( "CORBA: Interpeting exceptions of this type as being due to an object with indeterminate status, " 
                               + "so it's assumed to be still live." );

                return false;
            }
        }
    }


    /**
     * Handle an error exception by logging it and throwing a CORBA exception.
     * 
     * @param  msg  Message to log.
     * @param  e  Exception causing error, or null if none.
     *
     * @exception  An org.omg.CORBA.INTERNAL exception is always thrown.
     */
    public static void handleError ( String msg, Exception e ) 
        throws org.omg.CORBA.INTERNAL
    {
        if ( e != null )
            msg = msg + ":\n" + e.toString( );
        
        Debug.error( msg );
        
        if ( e != null )
            Debug.error( Debug.getStackTrace( e ) );
        
        throw new org.omg.CORBA.INTERNAL( msg );
    }
    

    /**
     * Handle an warning exception by logging it and throwing a CORBA exception.
     * 
     * @param  msg  Message to log.
     * @param  e  Exception causing warning, or null if none.
     *
     * @exception  An org.omg.CORBA.INTERNAL exception is always thrown.
     */
    public static void handleWarning ( String msg, Exception e ) 
        throws org.omg.CORBA.INTERNAL
    {
        if ( e != null )
            msg = msg + ":\n" + e.toString( );
        
        Debug.warning( msg );
        
        if ( e != null )
            Debug.warning( Debug.getStackTrace( e ) );
        
        throw new org.omg.CORBA.INTERNAL( msg );
    }


    private EventUtils ( )
    {
        // NOTE: Objects of this type should never be created.
    }
}

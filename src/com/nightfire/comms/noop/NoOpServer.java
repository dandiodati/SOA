/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.noop;


import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;


/**
 * Communications server that does nothing (i.e., will never
 * accept requests and initiate processing against them).
 * Useful to serve as an anchor for defining driver chains
 * in the gateway designer tool that will be used as sub-flows
 * in other driver chains.
 */
public class NoOpServer extends ComServerBase
{
    /**
     * Constructor.
     *
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public NoOpServer ( String key, String type ) throws ProcessingException 
    {
        super( key, type );

        loggingClassName = StringUtils.getClassName( this );
    }


    /**
     * Executes the server object in a separate supervisor thread.
     * The implementation in this class does nothing other than waiting.
     */
    public void run ( ) 
    {
        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, loggingClassName + ": Initializing ..." );

        try
        {
            synchronized ( this )
            {
                // Block here until shutdown() is called.
                wait( );
            }
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_WARNINGS, e.toString() );
        }

        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, loggingClassName + ": Server thread exiting ..." );
    }


    /**
     * Shuts-down the server object.
     */
    public void shutdown ( )
    {
        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, loggingClassName + ": Shutting-down ..." );

        synchronized ( this )
        {
            notifyAll( );
        }
    }


    // Abbreviated class name for use in logging.
    private String loggingClassName;
}



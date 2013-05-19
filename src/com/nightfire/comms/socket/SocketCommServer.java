/*
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.socket;

import com.nightfire.framework.util.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;


/*
 * A communications server class providing TCP/IP socket
 * connectivity.
 *
 * NOTE:  
 * TCP/IP socket communications protocol does not specify how data marshalling occurs.
 * Therefore, the request handler class should invoke the driver directly after it
 * extracts the data from the socket in an application-specific fashion instead of 
 * relying on the communications server infrastructure.  This also applies to returning
 * any data back to the client for synchronous interactions.
 */
public class SocketCommServer extends PushComServerBase
{
    /**
     * Fully-qualified (package + class) name of class to use in servicing requests.
     * Classes must implement the ClientSocketRequestHandler interface to be used by
     * this server object.
     */
    public static final String REQUEST_HANDLER_CLASS_NAME_PROP = "REQUEST_HANDLER_CLASS_NAME";

    /**
     * Property key indicating configuration to use in request-handler initialization (optional).
     */
    public static final String REQUEST_HANDLER_KEY_PROP = "REQUEST_HANDLER_KEY";

    /**
     * Property type indicating configuration to use in request-handler initialization (optional).
     */
    public static final String REQUEST_HANDLER_TYPE_PROP = "REQUEST_HANDLER_TYPE";


    /**
     * Constructor that creates the comm server object and loads its properties.
     *
     * @param  key  Key value used to access configuration properties.
     * @param  type  Type value used to access configuration properties.
     *
     * @exception  ProcessingException  Thrown on initialization errors.
     */
    public SocketCommServer ( String key, String type ) throws ProcessingException 
    {
        super( key, type );
        
        loggingClassName = StringUtils.getClassName( this );
        
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, loggingClassName + ": Initializing ..." );
        
        // NOTE:  In this case, no high-level IDL is involved and the data marshalling protocol is wide-open.
        // Therefore, the request handler class should invoke the driver directly instead of relying
        // on the comm. server infrastructure.
        String requestHandlerClassName = getRequiredPropertyValue( REQUEST_HANDLER_CLASS_NAME_PROP );
        
        try
        {
            int port = Integer.parseInt( getRequiredPropertyValue( PORT_NUMBER_PROP ) );
            
            String rhKey = getPropertyValue( REQUEST_HANDLER_KEY_PROP );

            String rhType = getPropertyValue( REQUEST_HANDLER_TYPE_PROP );

            listener = new SocketListener( port, requestHandlerClassName, rhKey, rhType );
        }
        catch ( NumberFormatException nfe )
        {
            throw new ProcessingException( "ERROR: Invalid port number configuration value [" 
                                           + getRequiredPropertyValue( PORT_NUMBER_PROP ) + "]." );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Could not initialize " 
                                           + loggingClassName + ":\n" + e.toString() );
        }
    }


    /**
     * Starts the server object, making it block waiting for requests.
     */
    public void run ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, loggingClassName + ": Running server ..." );
        
        listener.listen( );
    }
    
    
    /**
     * Shuts-down the server object.
     */
    public void shutdown ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, loggingClassName + ": Shutting-down server ..." );
        
        listener.shutdown( );
    }
    
    
    /**
     * Get type type of processing supported (synchronous/asynchronous).
     * 
     * @return True if asynchronous, otherwise false.
     */
    public boolean isAsync ( )
    {
        return true;
    }
    
    
    SocketListener listener;

    // Abbreviated class name for use in logging.
    private String loggingClassName;
}

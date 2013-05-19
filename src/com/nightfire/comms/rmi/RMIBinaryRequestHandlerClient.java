/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.comms.rmi;

import java.util.*;
import java.rmi.*;
import org.w3c.dom.*;

import com.nightfire.rmi.*;
import com.nightfire.common.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.spi.common.driver.*;


/**
 * Makes a synchronous or asynchronous RMI call with a binary payload.
 */
public class RMIBinaryRequestHandlerClient extends MessageProcessorBase
{
    /**
     * Name of property indicating host that RMI server object is running on.
     */
    public static final String RMI_REGISTRY_HOST_PROP = "RMI_REGISTRY_HOST" ;

    /**
     * Name of property indicating port that RMI server object is listening on.
     */
    public static final String RMI_REGISTRY_PORT_PROP = "RMI_REGISTRY_PORT" ;

    /**
     * Name of property indicating name of RMI server object.
     */
    public static final String SERVER_NAME_PROP = "SERVER_NAME" ;

    /**
     * Name of property indicating whether server is asynchronous (default) or synchronous.
     */
    public static final String IS_ASYNCHRONOUS_PROP = "IS_ASYNCHRONOUS";

    /**
     *  The key to use on the context to look up the NF style header
     */
    public static final String NF_HEADER_LOCATION_PROP = "HEADER_LOC";

    /**
     *  Properties for Default values that may be used if no values set in context
     */
    public static final String DEFAULT_HEADER_PROP = "DEFAULT_HEADER";


    /**
     * Loads the required property values into memory
     *
     * @param  key   Property Key to use for locating initialization properties.
     * @param  type  Property Type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize( key, type );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, StringUtils.getClassName(this) + ": Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );

        hostName = getRequiredPropertyValue( RMI_REGISTRY_HOST_PROP, errorBuffer );
        portNumber = getRequiredPropertyValue( RMI_REGISTRY_PORT_PROP, errorBuffer );
        serverName = getRequiredPropertyValue( SERVER_NAME_PROP, errorBuffer );

        String temp = getPropertyValue( IS_ASYNCHRONOUS_PROP );

        if ( StringUtils.hasValue( temp ) )
        {
            try
            {
                isAsyncFlag = StringUtils.getBoolean( temp );
            }
            catch ( Exception e )
            {
                errorBuffer.append( e.getMessage() );
            }
        }

        headerLocation = getPropertyValue( NF_HEADER_LOCATION_PROP );

        if ( !StringUtils.hasValue( headerLocation ) )
        {
            try
            {
                header = getRequiredPropertyValue( DEFAULT_HEADER_PROP, errorBuffer );
            }
            catch (Exception e)
            {
                errorBuffer.append( e.getMessage() );
            }
        }

        if ( errorBuffer.length() > 0 )
        {
            Debug.error( errorBuffer.toString() );

            throw new ProcessingException( errorBuffer.toString() );
        }

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Configuration: host [" + hostName + "], port [" + portNumber + "], server-name [" 
                       + serverName + "], is-async? [" + isAsyncFlag + "], header-location [" + headerLocation + "]." );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, StringUtils.getClassName(this) + ": Done initializing." );
    }


    /**
     * Execute the message-processor's functionality.
     *
     * @param context The MessageProcessorContext with required information
     * @param input  The MesageObject to be sent to the next processor
     *
     * @return NVPair[] Name-Value pair array of next processor name and the MessageObject passed in
     *
     * @exception ProcessingException Thrown if processing fails
     * @exception MessageException Thrown on data errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException
    {
        if ( input == null )
            return null;

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, StringUtils.getClassName(this) + ": Executing ..." );

        Object data = input.get( );

        if ( (data == null) || !(data instanceof byte[]) )
        {
            throw new MessageException( "Input data is of type [" + StringUtils.getClassName(data) 
                                        + "] instead of required byte[] type." );
        }

        if( StringUtils.hasValue( headerLocation ) )  
        {
            try 
            {
                header = getString( headerLocation, context, input );
            }
            catch ( MessageException me ) 
            {
                throw new ProcessingException( me.getMessage() );
            }
        }

        Object result = null;

        try
        {
            try
            {
                result = invokeServer( header, (byte[])data );
            }
            catch ( Exception e  )
            {
                // The following list of exceptions indicate that we might
                // have attempted a call against a stale server object reference,
                // so we'll get a new reference and retry once more in 
                // an attempt at recovery.
                if ( (e instanceof java.rmi.NoSuchObjectException) ||
                     (e instanceof java.rmi.ConnectException) ||
                     (e instanceof java.rmi.ConnectIOException) ||
                     (e instanceof java.rmi.NotBoundException) ||
                     (e instanceof java.rmi.UnknownHostException) )
                {
                    Debug.warning( "Got RMI exception indicating possible stale RMI server object reference, so retrying:\n" 
                                   + e.toString() );

                    // Null out cached reference so we'll look server up again.
                    server = null;

                    result = invokeServer( header, (byte[])data );
                }
                else if ( (e instanceof ClassCastException) ) 

                {
                    Debug.error( "ClassCastException in RMI invocation. " + 
                                 "Ensure that the server and client are " + 
                                 "same interface." );
                    throw e;
                }
                else

                {
                    Debug.error( "Got Exception in RMI communications:\n" 
                                   + e.toString() );
                    throw e;
                }
            }
        }
        catch ( RMIInvalidDataException ide )
        {
            throw new MessageException( ide.getMessage() );
        }
        catch ( Exception e )
        {
            if ( (e instanceof RemoteException) && (((RemoteException)e).detail != null) )
                throw new ProcessingException( ((RemoteException)e).detail.getMessage() );
            else
                throw new ProcessingException( e.getMessage() );
        }

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, StringUtils.getClassName(this) + ": Done executing." );

        //return the RMI call result - if non-null, or the input if null.
        if ( result == null )
            return( formatNVPair( input ) );
        else
            return( formatNVPair( result ) );
    }


    /**
     * Call the remote RMI server.
     *
     * @param header  The header XML to send.
     * @param request  The binary payload.
     *
     * @return Synchronous response, or null for asynchronous request.
     *
     * @exception Exception Thrown if processing fails
     */
    private Object invokeServer ( String header, byte[] request ) throws Exception
    {
        if ( Debug.isLevelEnabled( Debug.IO_DATA ) )
            Debug.log( Debug.IO_DATA, "Header value is:\n" + header + 
                       "\nRequest is byte array of length [" + request.length + "]." );

        header = CustomerContext.getInstance().propagate( header );

        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, "Header value:\n" + header );

        if ( isAsyncFlag )
        {
            Debug.log( Debug.IO_STATUS, "Invoking async request against RMI server ..." );

            getServer().processAsync( header, request );

            Debug.log( Debug.IO_STATUS, "Done invoking asynchronous request against RMI server." );

            return null;
        }
        else
        {
            Debug.log( Debug.IO_STATUS, "Invoking sync request against RMI server ..." );

            Object response = getServer().processSync( header, request );

            if ( response != null )
            {
                if ( Debug.isLevelEnabled( Debug.IO_DATA ) )
                {
                    if ( response instanceof byte[] )
                        Debug.log( Debug.IO_DATA, "Response is byte array of length [" + ((byte[])response).length + "]." );
                    else if ( response instanceof String )
                        Debug.log( Debug.IO_DATA, "Response is String of length [" + ((String)response).length() + "]." );
                    else
                        Debug.log( Debug.IO_DATA, "Response is object of type [" + StringUtils.getClassName( response ) + "]." );
                }
            }

            Debug.log( Debug.IO_STATUS, "Done invoking synchronous request against RMI server." );

            return response;
        }
    }


    /**
     * Get the remote RMI server reference.
     *
     * @return  A reference to a RMIBinaryRequestHandler object.
     *
     * @exception Exception Thrown if processing fails
     */
    private RMIBinaryRequestHandler getServer ( ) throws Exception
    {
        String serviceName = null;

        if ( server == null )
        {
            synchronized ( RMIBinaryRequestHandlerClient.class )
            {
                if ( server == null )
                {
                    serviceName = "//" + hostName + ":" + portNumber + "/" + serverName;

                    if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                        Debug.log( Debug.IO_STATUS, "Looking up RMI server [" + serviceName +"] ... " );

                    server = (RMIBinaryRequestHandler)Naming.lookup( serviceName );
                }
            }

            if ( server == null )
            {
                throw new ProcessingException( "Lookup of binary RMI server [" + serviceName + "] failed." );
            }

            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "RMI server is [" + server.toString() +"]." );
        }

        return server;
    }


    private String hostName;
    private String portNumber;
    private String serverName;
    private boolean isAsyncFlag = true;
    private String headerLocation = null;
    // The header to be sent to the Server 
    private String header = null;
    private static RMIBinaryRequestHandler server = null;
} 

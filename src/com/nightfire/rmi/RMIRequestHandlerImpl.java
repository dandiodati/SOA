/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.rmi;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;


/**
 * RMI version of NightFire RequestHandler remote interface.
 */
public class RMIRequestHandlerImpl 
    extends java.rmi.server.UnicastRemoteObject 
    implements RMIRequestHandler
{
    /**
     * Create the remote object and export it to make it available to 
     * receive incoming calls, using an anonymous port.
     *
     * @exception RemoteException  Thrown on error.
     */
    public RMIRequestHandlerImpl ( ) throws java.rmi.RemoteException
    {
        super( );
    }


    /**
     * Method used to get all interfaces supported by a given gateway.
     *
     * @return An array of UsageDescription objects.
     *
     * @exception RemoteException  Thrown on error.
     */
    public RMIRequestHandler.UsageDescription[] getUsageDescriptions ( ) throws java.rmi.RemoteException
    {
        return null;
    }
    
    
    /**
     * Test to see if a particular usage is supported.
     *
     * @param  usage  UsageDescription object containing usage in question.
     *
     * @return 'true' if usage is supported, otherwise false.
     *
     * @exception RemoteException  Thrown on communications errors.
     */
    public boolean supportsUsage ( RMIRequestHandler.UsageDescription usage ) throws java.rmi.RemoteException
    {
        return false;
    }
    
    
    /**
     * Method providing asynchronous processing.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     */
    public void processAsync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        throw new RMIServerException( "ERROR: processAsync() not supported by [" + getClass().getName() + "]." );
    }
    

    /**
     * Method providing synchronous processing.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @return The synchronous response.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public String processSync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        throw new RMIServerException( "ERROR: processSync() not supported by [" + getClass().getName() + "]." );
    }

    
    /**
     * Method providing synchronous processing, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public RMIRequestHandler.ResponsePair processSynchronous ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        throw new RMIServerException( "ERROR: processSynchronous() not supported by [" + getClass().getName() + "]." );
    }


    /**
     * Unit-test.
     */
    public static void main ( String args[] )
    {
        // NOTE: This test assumes that the rmiregistry is running!!!
        try
        {
            String serverName = "rmi://192.168.10.116:1099/testRequestHandler";

            // Create the server object, and make it available in the registry.
            RMIRequestHandlerImpl server = new RMIRequestHandlerImpl( );

            Naming.rebind( serverName, server );

            // Attach to the server and make a call.
            RMIRequestHandler client = (RMIRequestHandler)Naming.lookup( serverName );

            client.processSync( "header", "body" );
        }
        catch ( Exception e )
        {
            System.err.println( e );

            e.printStackTrace();
        }
    }
}; 


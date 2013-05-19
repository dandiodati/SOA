/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.rmi;

import java.io.*;
import java.rmi.*;


/**
 * RMI version of NightFire RequestHandler remote interface.
 */
public interface RMIRequestHandler extends java.rmi.Remote
{
    /**
     * Class encapsulating the description of one gateway usage.
     */
    public static class UsageDescription implements Serializable
    {
        public UsageDescription( String serviceProvider, String interfaceVersion, String operationType, boolean asynchronous )
        {
            this.serviceProvider = serviceProvider;
            this.interfaceVersion = interfaceVersion;
            this.operationType = operationType;
            this.asynchronous = asynchronous;
        }

        public String   serviceProvider;
        public String   interfaceVersion;
        public String   operationType;
        public boolean  asynchronous;
    };


    /**
     * Method used to get all interfaces supported by a given gateway.
     *
     * @return An array of UsageDescription objects.
     *
     * @exception 
     */
    public UsageDescription[] getUsageDescriptions ( ) throws java.rmi.RemoteException;
    
    
    /**
     * Test to see if a particular usage is supported.
     *
     * @param  usage  UsageDescription object containing usage in question.
     *
     * @return 'true' if usage is supported, otherwise false.
     *
     * @exception RemoteException  Thrown on communications errors.
     */
    public boolean supportsUsage ( UsageDescription usage ) throws java.rmi.RemoteException;
    
    
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
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException;
    

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
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException;

    
    /**
     * Class encapsulating the synchronous response header and message.
     */
    public static class ResponsePair implements Serializable
    {
        public String header;
        public String message;
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
    public ResponsePair processSynchronous ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException;
}; 

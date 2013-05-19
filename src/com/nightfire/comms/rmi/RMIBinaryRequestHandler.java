/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.comms.rmi;

import java.io.*;
import java.rmi.*;

import com.nightfire.rmi.*;



/**
 * RMI interface for sending and receiving binary data in an async/sync fashion.
 */
public interface RMIBinaryRequestHandler extends java.rmi.Remote
{
    /**
     * Method providing asynchronous processing of binary data.
     *
     * @param  header  Message header.
     * @param  request Message body containing binary data.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     */
    public void processAsync ( String header, byte[] request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException;
    

    /**
     * Method providing synchronous processing of binary data.
     *
     * @param  header  Message header.
     * @param  request Message body containing binary data.
     *
     * @return The synchronous response.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public Object processSync ( String header, byte[] request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException;

    
    /**
     * Class encapsulating the synchronous response header and message.
     */
    public static class BinaryResponsePair implements Serializable
    {
        public String header;
        public Object message;
    }


    /**
     * Method providing synchronous processing of binary data, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body containing binary data.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public BinaryResponsePair processSynchronous ( String header, byte[] request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException;
}; 

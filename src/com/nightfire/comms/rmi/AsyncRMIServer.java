/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.rmi;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;


/**
 * Asynchronous RMI Server waits
 * for the client objects to invoke process method on it
 * for String/DOM objects processing
 * <br> The instances of these are created by the Supervisor
 */
public class AsyncRMIServer extends RMIServer 
{
    /**
     * Constructor
     * 
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public AsyncRMIServer(String key, String type)
        throws ProcessingException
    { 
        super(key, type);
        Debug.log(Debug.NORMAL_STATUS,"AsyncRMIServer: Initializing ...");
    }
    
    /**
     * Sets server as asynchronous
     */
    public boolean isAsync()
    {
        return true;
    }
}

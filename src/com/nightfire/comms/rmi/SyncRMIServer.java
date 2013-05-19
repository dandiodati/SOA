/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.rmi;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;



/**
 * Synchronous RMI Server waits for the client objects to 
 * invoke processSync() method for String/DOM objects processing
 * The instances of these are created by the Supervisor
 */
public class SyncRMIServer extends RMIServer 
{
    /**
     * Constructor
     * 
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public SyncRMIServer(String key, String type)
        throws ProcessingException
    { 
        super(key, type);
        Debug.log(Debug.NORMAL_STATUS,"SyncRMIServer: Initializing ...");
    }
    

    /**
     * Sets server as synchronous
     */
    public boolean isAsync()
    {
        return false;
    }
}




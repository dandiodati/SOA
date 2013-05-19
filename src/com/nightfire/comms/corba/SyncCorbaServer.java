/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.corba;

import com.nightfire.common.*;
import com.nightfire.framework.corba.*;

 
/**
 * Synchronous Corba Server waits for the client objects to
 * invoke processSync() method on it for String/DOM objects processing
 * The instances of these are created by the Supervisor.
 */
public class SyncCorbaServer extends CorbaServer 
{
    /**
     * Constructor
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public SyncCorbaServer(String key, String type)
        throws ProcessingException
    {
        super(key, type);
    }


    /**
     * Constructor, that can be invoked by the Test Programs
     * 
     * @param   cpl - CorbaPortabilityLayer sent by the test programs
     * @param   key   Property-key to use for locating initialization properties.
     * @param   type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public SyncCorbaServer(CorbaPortabilityLayer cpl, String key , String type) 
        throws ProcessingException
    {
        super(key, type);
        this.cpl = cpl;
    }
    

    /**
     * Sets server as synchronous
     */
    public boolean isAsync()
    {
        return false;
    }
}




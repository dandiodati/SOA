/*
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.corba;

import com.nightfire.common.*;
import com.nightfire.framework.corba.*;


/**
 * Asynchronous Corba Server waits
 * for the client objects to invoke Process method on it
 * for String/DOM objects processing
 * <br> The instances of these are created by the
 * Supervisor
 */
public class AsyncCorbaServer extends CorbaServer 
{

    /**
     * Constructor used by Object FactoryS
     *
     * @param   key   Property-key to use for locating initialization properties.
     * @param   type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public AsyncCorbaServer(String key , String type) 
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
    public AsyncCorbaServer(CorbaPortabilityLayer cpl, String key , String type) 
        throws ProcessingException
    {
        super(key, type);
        this.cpl = cpl;
    }

    /**
     * Sets server as asynchronous
     */
    public boolean isAsync()
    {
        return true;
    }
}

/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.timer;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.message.MessageException;

import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;


/**
 *  Communications class that is just a timer that
 *  executes the actionPerformed() method after sleeping
 *  for a set time.  The SPI starts a thread for this class
 *  which in turn creates a Timer object that runs as
 *  seperate deamon thread in an infinite loop.
 */
public class AsyncTimerServer extends PollComServerBase
{
    /**
     * Constructor
     *
     */
    public AsyncTimerServer (String key, String type) throws ProcessingException 
    {
        super(key,type);
        
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,"Done initializing AsyncTimerServer .");
    }


    /**
     * Immitates request processing
     */
    public void processRequests() throws ProcessingException
    {
        try
        {
            process( null, request );
        }
        catch (MessageException e)
        {
            throw new ProcessingException(e);
        }
    }


    private static final String request = "<?xml version='1.0'?><Request></Request>";
}



/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/test/SimplePushConsumer.java#1 $
 */

package com.nightfire.servers.CosEvent.test;

//import org.omg.CORBA.SystemException;

import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;


/**
 * This simple push consumer registers to a channel whose name will be passed from command line and
 * prints out the events it received.
 *
 */

public class SimplePushConsumer implements PushConsumerCallBack {

    private EventPushConsumer helper;
    private int delay;
    private int eventNumber;
    
    private int eventCounter;

    SimplePushConsumer(org.omg.CORBA.ORB orb, String channelName, int vDelay, int vEventNumber) throws Exception {
        delay = vDelay;
        eventCounter = 0;
        eventNumber = vEventNumber;
        helper = new EventPushConsumer(orb, channelName, false);
        helper.register(this);

    }
  
    void disconnect() {
        helper.disconnect();

    }


	/**
	 * Prints out the event received from channel.
	 *
	 */
    public void processEvent(String message) {
        Debug.log(Debug.UNIT_TEST, "\tConsumer being pushed: " + message);
        addCounter();
        
        try {
          Thread.currentThread().sleep(1000 * delay);
        }
        catch (InterruptedException ie) {
	    }

        Debug.log(Debug.UNIT_TEST, "\tExiting process method.");
    }
    
    private synchronized void addCounter()
    {
    	eventCounter++;
    	if (eventCounter >= eventNumber)
    	{
        	Debug.log(Debug.UNIT_TEST, "[" + eventCounter + "] events received, disconnecting...");
    		helper.disconnect();
    	}
    }
}


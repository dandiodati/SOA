/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/test/SimplePushSupplier.java#1 $
 */

package com.nightfire.servers.CosEvent.test;

import org.omg.CORBA.SystemException;

import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;


/**
 * This test push consumer connects to a channel whose name will be passed from command line and
 * pushes events to the channel.
 *
 */
public class SimplePushSupplier implements Runnable {

    private EventPushSupplier helper;
    private Thread thread;

    private int delay;
    private int eventNumber;
    
    SimplePushSupplier(org.omg.CORBA.ORB orb, String channelName, int vDelay, int vEventNumber) throws Exception {
        delay = vDelay;
        eventNumber = vEventNumber;
        helper = new EventPushSupplier(orb, channelName, false);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();

    }
  
    void disconnect() {
        helper.disconnect();

    }



	/**
	 * Pushes events to the channel.
	 *
	 */
    public void run() {
        try {
	        int eventCounter = 0;
	        while(true) {
		    	if (eventCounter >= eventNumber)
		    	{
		        	Debug.log(Debug.UNIT_TEST, "[" + eventCounter + "] events pushed, disconnecting...");
		    		helper.disconnect();
		    		break;
		    	}
		    	eventCounter++;

	            Thread.currentThread().sleep(1000 * delay);
	            
	            try {
	                String message = "Push - Hello #" + eventCounter;
	                Debug.log(Debug.UNIT_TEST, "Supplier pushing: " + message);
	                helper.pushEvent(message);
	            }
	            catch (CorbaException ce) {
	                ce.printStackTrace();
	                break;
	            }
	            catch (SystemException se) {
	                se.printStackTrace();
	                break;
	            }
	        }
        }
        catch (InterruptedException ie) {
	    }
    }


}


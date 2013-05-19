/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/test/TestPushConsumer.java#1 $
 */

package com.nightfire.servers.CosEvent.test;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;
import java.io.DataInputStream;
import org.omg.CORBA.SystemException;
import com.nightfire.framework.corba.*;


/**
 * This test push consumer registers to a channel whose name will be passed from command line and
 * prints out the events it received.
 *
 */

public class TestPushConsumer implements PushConsumerCallBack {

    private EventPushConsumer helper;
    private int _delay;

    TestPushConsumer(org.omg.CORBA.ORB orb, String channelName) throws Exception {
        _delay = 10;
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
        System.out.println("Consumer being pushed: " + message);
        try {
          Thread.currentThread().sleep(1000 * _delay);
        }
        catch (InterruptedException ie) {
	    }
        System.out.println("Exiting process method.");
    }


	/**
	 * Entry point for starting the test process. Accept one command line argument which is the name of the channel.
	 *
	 */
	
    public static void main(String[] args) {

      CorbaPortabilityLayer cpl = null;

    	if (args.length != 1) {
    		System.out.println("Usage: TestPushConsumer <channelName>");
    		return;
    	}
        try {
            cpl = new CorbaPortabilityLayer(args, null, null);

            TestPushConsumer model = null;
            DataInputStream in = new DataInputStream(System.in);
            while(true) {
                try {
                    System.out.print("-> ");
                    System.out.flush();
                    if (System.getProperty("VM_THREAD_BUG") != null) {
                        while(in.available() == 0) {
                            try {
                                Thread.currentThread().sleep(100);
                            }
                            catch(InterruptedException e) {
                            }
                        }
                    }

                    String line = in.readLine();
         	        if(line.startsWith("m")) {
                        model = new TestPushConsumer(cpl.getORB(), args[0]);
                        System.out.println("Created model: " + model);
                        continue;
                    }

                    else if(line.startsWith("s")) {
                        if(model == null) {
                            System.out.println("Need to create a [m]odel");
                        }
                        else {
	                        try {
	                        	if ("s ".length() >= line.length()) {
		                            System.out.println("Current delay is " + model._delay);
		                            continue;
		                        }
	                        		
		                        int delay =
		                                new Integer(line.substring("s ".length(),
					                            line.length())).intValue();
		                        if (delay < 0) {
		                            System.out.println("[s]leep delay must be positive");
		                        }
                                else {
		                            model._delay = delay;
		                        }
	                        } catch (NumberFormatException e) {
		                        System.out.println("Invalid argument to [s]leep");
	                        }
	                    }
                        continue;
	                }

                    else if(line.startsWith("d")) {
                        if(model == null) {
                            System.out.println("Need to obtain a [m]odel");
                        }
                        else {
                            System.out.println("Disconnecting...");
                            model.disconnect();
                            continue;
                        }
                    }
                    else if(line.startsWith("q")) {
                        System.out.println("Quitting...");
                        break;
                    }
                    System.out.println("Commands: m             [m]odel\n" +
                                       "          s <# seconds>  set [s]leep delay\n" +
                                       "          d             [d]isconnect\n" +
                                       "          q             [q]uit\n");
                }
                catch(org.omg.CORBA.SystemException e) {
                    e.printStackTrace();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}


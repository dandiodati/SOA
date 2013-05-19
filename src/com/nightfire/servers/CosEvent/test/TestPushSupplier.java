/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/test/TestPushSupplier.java#1 $
 */


package com.nightfire.servers.CosEvent.test;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;
import java.io.DataInputStream;
import org.omg.CORBA.SystemException;
import com.nightfire.framework.corba.*;


/**
 * This test push consumer connects to a channel whose name will be passed from command line and
 * pushes events to the channel.
 *
 */
public class TestPushSupplier implements Runnable {

    private EventPushSupplier helper;
    private Thread _thread;
    private int _delay;

    TestPushSupplier(org.omg.CORBA.ORB orb, String channelName) throws Exception {
        helper = new EventPushSupplier(orb, channelName, false);
        _delay = 1;
        _thread = new Thread(this);
        _thread.setDaemon(true);
        _thread.start();
    }

    void disconnect() {
        _thread.interrupt();
        helper.disconnect();

    }


	/**
	 * Pushes events to the channel.
	 *
	 */
    public void run() {
        try {
	        int counter = 0;
	        while(true) {
	                _thread.sleep(1000 * _delay);
	            try {
	                String message = "Push - Hello #" + ++counter;
	                System.out.println("Supplier pushing: " + message);
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



	/**
	 * Entry point for starting the test process. Accept one command line argument which is the name of the channel.
	 *
	 */
    public static void main(String[] args) {
      CorbaPortabilityLayer cpl = null;

    	if (args.length != 1) {
    		System.out.println("Usage: TestPushSupplier <channelName>");
    		return;
    	}
        try {
            //org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(args, null);
            //org.omg.CORBA.BOA boa = CorbaPortabilityLayer.BOA_init(orb);

            cpl = new CorbaPortabilityLayer(args, null, null);

            TestPushSupplier model = null;
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
                        model = new TestPushSupplier(cpl.getORB(), args[0]);
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


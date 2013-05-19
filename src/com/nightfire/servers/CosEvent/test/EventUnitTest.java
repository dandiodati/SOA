/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/test/EventUnitTest.java#1 $
 */

package com.nightfire.servers.CosEvent.test;

import java.io.*;
import java.util.*;

import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.unittest.*;


/*
 * Example unit test driver.
 */
public class EventUnitTest extends UnitTestBase
{
	
	private static final int DELAY = 1;
	private static final int NUMBER_OF_EVENTS = 100;
	private static final String CHANNEL_NAME = "B";
	
    /**
     * Example main() that shows how to execute unit test cases.
     *
     * @param  args  Array of strings containing command-line arguments.
     */
    public static void main ( String[] args )
    {
        // Configure which debug log levels are displayed.
        // Debug.enableAll( );
        Debug.disableAll( );
        Debug.enable( Debug.UNIT_TEST );
        Debug.enable( Debug.ASSERT );
        // Show or hide logging-level as a part of each log message.
        // Debug.showLevels( );
        Debug.hideLevels( );
        // Hide the time-stamping of each log message.
        // Debug.disableTimeStamping( );

        // FrameworkException.showStackTrace( );

        // Write results to file with appropriate name.
        // redirectLogs( "example_unit_test.out" );

        String myTests[] = {
            "Test pushing events to channel.",
            "Test receiving events from channel.",
            "Test one push supplier and one consumer.",
            "Test two push supplier and two consumer.",
        };

        testNames = myTests;

        org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(args, null);

        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );

        testCounter = 0;

        try
        {
            // PUT YOUR UNIT TESTS HERE!!!
            logTestStart( );
            sendingEvents( orb );
            logTestEnd( );
            
            logTestStart( );
            receivingEvents( orb );
            logTestEnd( );

            logTestStart( );
            sendingAndReceiving(orb, 1 );
            logTestEnd( );

            logTestStart( );
            sendingAndReceiving(orb, 2 );
            logTestEnd( );
        }
        catch ( Exception e )
        {
        	log( "Unit test \"" + testNames[testCounter] + 
                 "\" failed with the following exception:\n" + e.toString() );
            
            log( getStackTrace( e ) );
        }
        
		log( "\t\t\tTEST SUMMARY");
    	log( "\tTotal test cases: " + testNames.length );
    	log( "\tTotal passed:     " + testCounter );
    	
    	if ( testCounter == testNames.length ) 
        {
    		log( "All tests finished successfully." );
    	}
        else
        {
    		log( "At least one test failed." );
    	}
    }
    

    /*************************************************************************/

    private static void sendingAndReceiving ( ORB orb, int numOfClients ) throws Exception
    {
    	for (int i=0; i<numOfClients; i++)
    	{
			new SimplePushConsumer(orb, CHANNEL_NAME, DELAY, NUMBER_OF_EVENTS*numOfClients);
			new SimplePushSupplier(orb, CHANNEL_NAME, DELAY, NUMBER_OF_EVENTS);
		}
		
		int sleepTime = DELAY * NUMBER_OF_EVENTS + 10;
		log("Sleeping for [" + sleepTime + "] seconds and waiting for sending and receiving events...");
		Thread.currentThread().sleep(sleepTime * 1000 * numOfClients);

    }


    /*************************************************************************/

    private static void sendingEvents ( ORB orb ) throws Exception
    {
		new SimplePushSupplier(orb, CHANNEL_NAME, DELAY, NUMBER_OF_EVENTS);
		int sleepTime = DELAY * NUMBER_OF_EVENTS + 3;
		log("Sleeping for [" + sleepTime + "] seconds and waiting for sending events....");
		Thread.currentThread().sleep(sleepTime * 1000);
    }


    /*************************************************************************/

    private static void receivingEvents ( ORB orb ) throws Exception
    {
		new SimplePushConsumer(orb, CHANNEL_NAME, DELAY, NUMBER_OF_EVENTS);
		int sleepTime = DELAY * NUMBER_OF_EVENTS + 3;
		log("Sleeping for [" + sleepTime + "] seconds and waiting for receiving events....");
		Thread.currentThread().sleep(sleepTime * 1000);
    }

}


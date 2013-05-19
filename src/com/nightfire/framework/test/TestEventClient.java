package com.nightfire.framework.test;

import org.omg.CORBA.*;

import com.nightfire.unittest.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;


/*
 * Class for testing Event Helper classes.
 * In case 1, the test supplies an event to the persistent channel.
 * In case 2, the test registers to the channel for an event before disconnects.
 * In case 3 and 4, it creats an anonymous event channel and remember it by its IOR.
 * Since the event channel is not persistent, the consumer is registered to channel before the
 * the supplier.
 */
class TestEventClient extends UnitTestBase
    implements PushConsumerCallBack
{
    private static final String CHANNEL_NAME = "A";

    private static org.omg.CORBA.ORB orb;

    private static String eventChannelIOR;
    
    private static EventPushConsumer holder = null;


    /**
     * Executes unit-tests for Event APIs functionality.
     *
     * @param  args  Array of strings containing command-line arguments.
     */
    public static void main ( String[] args )
    {
        // Configure which debug log levels are displayed.
        Debug.enableAll( );
        // Debug.disableAll( );
        // Debug.enable( Debug.UNIT_TEST );
        // Debug.enable( Debug.ASSERT );
        // Show or hide logging-level as a part of each log message.
        // Debug.showLevels( );
        Debug.hideLevels( );
        // Hide the time-stamping of each log message.
        // Debug.disableTimeStamping( );
        
        pause("Please make sure the event server is started...", 3000);

        FrameworkException.showStackTrace( );

        // Write results to file with appropriate name.
        // redirectLogs( "nameservice_test.out" );

        String myTests[] = {
            "Testing EventPushConsumer with IOR.",
            "Testing EventPushSupplier with IOR.",
            "Testing EventPushSupplier with persistent channel.",
            "Testing EventPushConsumer with persistent channel.",
        };

        testNames = myTests;

        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );

        testCounter = 0;

        try
        {
            orb = org.omg.CORBA.ORB.init( args, null );


			// Test using anonymous channel with IOR.
            logTestStart( );
            testPushConsumerIOR( orb );
            logTestEnd( );
            
            logTestStart( );
            testPushSupplierIOR( orb );
            logTestEnd( );

            // Disconnect now after the push.
            holder.disconnect();


			// Test using persistent channel.
            logTestStart( );
            testPushSupplier( orb );
            logTestEnd( );

            logTestStart( );
            testPushConsumer( orb );
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

    private static void testPushConsumerIOR ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Create a helper.
        EventPushConsumer helper = new EventPushConsumer( orb);
        eventChannelIOR = helper.getEventChannelIOR();
        log("IOR = " + eventChannelIOR);
	    	
        helper.register(new TestEventClient());

		// Do not disconnect here. Waiting to receive the event.
        // Same handle to disconnect in main after the supplier pushes an event.
        holder = helper;
    }

    /*************************************************************************/

    private static void testPushSupplierIOR ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Create a helper.
        EventPushSupplier helper = new EventPushSupplier( orb, eventChannelIOR, true);

	    String message = "PUSH IOR: Hello World!----->";
	    
	    log("Pushing message: " + message);
	

        helper.pushEvent(message);

        helper.disconnect();
    }


    /*************************************************************************/

    private static void testPushSupplier ( org.omg.CORBA.ORB orb ) throws Exception
    {
    	EventPushSupplier helper = new EventPushSupplier( orb, CHANNEL_NAME, false);
	    	
	    String message = "PUSH: Hello World!----->";
	    
	    log("Pushing message: " + message);
	

        helper.pushEvent(message);

        helper.disconnect();
    }

    /*************************************************************************/

    private static void testPushConsumer ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Create a helper.
        EventPushConsumer helper = new EventPushConsumer( orb, CHANNEL_NAME, false);

        helper.register(new TestEventClient());

        pause("Waiting a while to be pushed...", 3000);

        helper.disconnect();
    }





    /*************************************************************************/

    private static void pause ( String mesg, int sleepTime )
    {
        try {
        	log(mesg);
        	Thread.sleep(sleepTime);
        }
        catch (InterruptedException ie) {
        	log("Interrupted");
        }

	}



    // PushConsumerCallBack
    public void processEvent(String message) {
        log("Consumer being pushed: " + message);
    }

}


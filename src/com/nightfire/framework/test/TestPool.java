/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 * $Head$
 *
 */

package com.nightfire.framework.test;

import java.sql.*;

import com.nightfire.unittest.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.*;


/*
 * Class for testing DBConnectionPool.
 * In case 1, connections are obtained sequentially and returned.
 * In case 2, connections are obtained simutaneously and returned.
 * In case 3, connections are obtained with a specified timeout period.
 * In case 4, a pool is constructed and destroyed while some connections are not returned.
 */
class TestPool extends UnitTestBase
{


    /**
     * Executes unit-tests for db connection pooling functionality.
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


        FrameworkException.showStackTrace( );

        // Write results to file with appropriate name.
        // redirectLogs( "nameservice_test.out" );

        String myTests[] = {
            "Testing pool with sequential resource allocation.",
            "Testing pool with concurrent resource allocation.",
            "Testing pool resource aging and cleanup.",
            "Testing pool with timeout allocation.",
            "Testing pool destroy.",
        };

        testNames = myTests;

        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );

        testCounter = 0;

        try
        {
    		log( "Start initializing database connection ..." );

    		String dbName = args[0];
    		String dbUser = args[1];
    		String dbPassword = args[2];

            DBInterface.initialize( dbName, dbUser, dbPassword);

        	log( "Done database initialization.\n" );



			// Test resource allocation.
            logTestStart( );
            testSequential( );
            logTestEnd( );

            logTestStart( );
            testConcurrent( );
            logTestEnd( );

            logTestStart( );
            testAging( );
            logTestEnd( );


			// Test allocation with time out.
            logTestStart( );
            testTimeout( 6000 );
            logTestEnd( );

            logTestStart( );
            testDestroy( );
            logTestEnd( );

			DBInterface.closeConnection();

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

    private static void testSequential ( ) throws Exception
    {
        // Create a pool.
        DBConnectionPool pool = DBConnectionPool.getInstance();

        Connection[] conns = new Connection[30];
        
        int counter = 0;

        while (counter < conns.length && pool.isResourceAvailable())
        {
	        log("POOL_TEST: Getting DB conn [" + counter + "]...");

        	conns[counter] = pool.acquireConnection();

	        log("POOL_TEST: Got DB conn [" + counter + "]: " + conns[counter]);
	
        	counter ++;
        	
        }
        
        for (counter = counter-1; counter >=0; counter--)
        {
	        log("POOL_TEST: Returning DB conn [" + counter + "]: " + conns[counter]);
        	pool.releaseConnection(conns[counter]);
	        log("POOL_TEST: Returned DB conn [" + counter + "].");
        }

        log("POOL_TEST: Destroy the pool...");
        //pool.destroyAll();
        log("POOL_TEST: Destroyed the pool.");

    }

    /*************************************************************************/

    private static void testConcurrent ( ) throws Exception
    {
        // Create a pool.
        DBConnectionPool pool = DBConnectionPool.getInstance();

        for (int i=0; i<10; i++) {
        	Thread aThread = new Thread(new ConnectionConsumer(pool, (i+1)*3000/2));
        	aThread.setName("T-" + i);
        	aThread.start();
        }

        pause("POOL_TEST: Waiting all threads to die.", 13*3000);

        log("POOL_TEST: Destroy the pool...");
        //pool.destroyAll();
        log("POOL_TEST: Destroyed the pool.");

    }


    /*************************************************************************/

    private static void testAging ( ) throws Exception
    {
        // Create a pool.
        DBConnectionPool pool = DBConnectionPool.getInstance();

        // Set aging time to one minute.
        pool.setIdleCleanupTime( 1 );

        // Get several connections.
        Connection c1 = pool.acquireConnection( );
        Connection c2 = pool.acquireConnection( );
        Connection c3 = pool.acquireConnection( );

        // Return most of the connections.
        pool.releaseConnection( c2 );
        pool.releaseConnection( c3 );

        // Wait for aging time to expire.
        pause( "Wait with no activity ...", 2*60*1000 );

        log("POOL_TEST: Destroy the pool...");
        pool.shutdown();
        log("POOL_TEST: Destroyed the pool.");

    }


    /*************************************************************************/

    private static void testTimeout ( int timeout ) throws Exception
    {
        // Create a pool.
        DBConnectionPool pool = DBConnectionPool.getInstance();

		// Grab first and hold for timeout
        for (int i=0; i<5; i++) {
        	Thread aThread = new Thread(new ConnectionConsumer(pool, timeout));
        	aThread.setName("T-" + i);
        	aThread.start();
        }

		// Grab another 5 and hold for timeout/2
        for (int i=5; i<10; i++) {
        	Thread aThread = new Thread(new ConnectionConsumer(pool, timeout/2));
        	aThread.setName("T-" + i);
        	aThread.start();
        }

        Connection[] conns = new Connection[30];
        
        int counter = 0;

        while (counter < conns.length && pool.isResourceAvailable())
        {
        	try {
	        	log("POOL_TEST: Getting DB conn [" + counter + "]...");

	        	conns[counter] = pool.acquireConnection(timeout);
	
	        	log("POOL_TEST: Got DB conn [" + counter + "]: " + conns[counter]);
		
	        	counter ++;
        	}
        	catch (ResourceException re)
        	{
        		re.printStackTrace();
        	}
        }
        
	    log("POOL_TEST: Got DB conn total: [" + (counter-1) + "].");

        for (counter = counter-1; counter >=0; counter--)
        {
	        log("POOL_TEST: Returning DB conn [" + counter + "]: " + conns[counter]);
        	pool.releaseConnection(conns[counter]);
	        log("POOL_TEST: Returned DB conn [" + counter + "].");
        }

        log("POOL_TEST: Destroy the pool...");
        //pool.destroyAll();
        log("POOL_TEST: Destroyed the pool.");

    }

    /*************************************************************************/

    private static void testDestroy ( ) throws Exception
    {
        // Create a pool.
        DBConnectionPool pool = DBConnectionPool.getInstance();

        for (int i=0; i<15; i++) {
        	Thread aThread = new Thread( new ConnectionConsumer(pool, (i+1)*3000/2) );
        	aThread.setName("T-" + i);
        	aThread.start();
        }

        pause("POOL_TEST: Waiting for some threads to die...", 3000);

        log("POOL_TEST: Destroy the pool...");
        DBConnectionPool.shutdown();
        log("POOL_TEST: Destroyed the pool.");

    }





    /*************************************************************************/

    private static void pause ( String mesg, int sleepTime )
    {
        try {
        	log("POOL_TEST: " + mesg);
        	Thread.sleep(sleepTime);
        }
        catch (InterruptedException ie) {
        	log("POOL_TEST: Interrupted");
        }

   	}

	protected static void log(String msg)
	{
		UnitTestBase.log(msg);
	}


	static class ConnectionConsumer implements Runnable
	{
		private long howLong;
		private DBConnectionPool pool;

		// Constructor
		ConnectionConsumer(DBConnectionPool pool, long howLong)
		{
			log("POOL_TEST: Constructing runnable object " + this);
			this.pool = pool;
			this.howLong = howLong;
		}

		public void run()
		{
			log( "POOL_TEST: " + Thread.currentThread() + " begin to run...");
	        Connection conn = null;

			try
			{
				log( "POOL_TEST: " + Thread.currentThread() + " acquiring a connection...");
	        	conn = pool.acquireConnection(howLong);
	        	log("POOL_TEST: " + Thread.currentThread() + ": Connection of DB conn is " + conn);

	        	Thread.currentThread().sleep(howLong);

			}
			catch (ResourceException re)
			{
				re.printStackTrace();
			}
			catch (InterruptedException ie)
			{
				ie.printStackTrace();
			}
			finally
			{
				if (conn != null)
				{
					try
					{
						log( "POOL_TEST: " + Thread.currentThread() + " releasing the connection...");
						pool.releaseConnection(conn);
	        			log("POOL_TEST: " + Thread.currentThread() + ": released connection.");
					}
					catch (ResourceException re)
					{
						re.printStackTrace();
					}
				}
			}
			log( "POOL_TEST: " + Thread.currentThread() + " exiting.");
		}
	}

}

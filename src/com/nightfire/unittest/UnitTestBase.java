package com.nightfire.unittest;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;


/**
 * Abstract base class for all unit-testing applications.
 */
public abstract class UnitTestBase
{
    /**
     * Array of strings describing individual unit tests - NOTE: Must be given values by concrete child classes!
     */
    protected static String[] testNames;

    /**
     * Current unit test case number.
     */
    protected static int testCounter = 0;


    /**
     * Example main() that shows how to execute unit test cases.  Should be copied to
     * concrete child class that actually does tests.
     *
     * @param  args  Array of strings containing any command-line arguments.
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
        // redirectLogs( "unit_test.out" );

        String myTests[] = {
            "First unit test.",
            "Second unit test.",
            "Third unit test."
        };

        testNames = myTests;

        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );

        testCounter = 0;

        try
        {
            // PUT YOUR UNIT TESTS HERE!!!

            logTestStart( );
            // unit_test_one( );
            logTestEnd( );

            logTestStart( );
            // unit_test_two( );
            logTestEnd( );

            logTestStart( );
            // unit_test_three( );
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


    protected UnitTestBase ( )   { /*EMPTY*/ }



    /**
     * Write unit-test case initiation message to log.
     */
    protected static void logTestStart ( )
    {
        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );
        assertTrue( testCounter < testNames.length, "'testCounter' value is less than length of 'testNames'." );

    	log( "UNIT TEST CASE [" + (testCounter+1) + "]:" );
    	log( "Executing unit test \"" + testNames[testCounter] + "\" ..." );
    }


    /**
     * Write successful unit test case message to log.
     */
    protected static void logTestEnd ( )
    {
        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );
        assertTrue( testCounter < testNames.length, "'testCounter' value is less than length of 'testNames'." );

        log( "UNIT TEST: \"" + testNames[testCounter] + "\" : \t\t\t PASSED.\n");

        // Bump counter to next test case.
        testCounter ++;
    }


    /**
     * Log the given message to the unit-test log.
     *
     * @param  msg  Message to log.
     */
    protected static void log ( String msg )
    {
        Debug.log( Debug.UNIT_TEST, msg );
    }


    /**
     * Execute the given assertion, printing its description and a stack trace upon failure
     * (followed by the immediate termination of the application).
     *
     * @param  assertion             Boolean expression being asserted as true.
     * @param  assertionDescription  Text description of assertion.
     */
    protected static void assertTrue ( boolean assertion, String assertionDescription )
    {
        Debug.assertTrue( assertion, assertionDescription );
    }


    /**
     * Get a stack trace in string form showing the stack calls
     * from given exception
     *
     * @param  e  Exception to extract stack trace from.
     *
     * @return  String containing the stack trace.
     */
    protected static String getStackTrace ( Exception e )
    {
        StringWriter sw = new StringWriter( );

        e.printStackTrace( new PrintWriter( sw ) );

        return( sw.toString() );
    }


    /**
     * Redirect System.out and System.err messages to the given file.
     *
     * @param  fileName  Name of the log file.
     */
    protected static void redirectLogs ( String fileName )
    {
        try
        {
            PrintStream ps = new PrintStream( new FileOutputStream( fileName ) );

            System.setOut( ps );
            System.setErr( ps );
        }
        catch ( IOException ioe )
        {
            System.err.println( ioe );
        }
    }
}


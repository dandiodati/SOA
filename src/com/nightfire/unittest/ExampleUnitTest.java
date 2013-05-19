package com.nightfire.unittest;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;


/*
 * Example unit test driver.
 */
public class ExampleUnitTest extends UnitTestBase
{
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
            unit_test_one( );
            logTestEnd( );
            
            logTestStart( );
            unit_test_two( );
            logTestEnd( );

            logTestStart( );
            unit_test_three( );
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

    private static void unit_test_one ( )
    {

    }


    /*************************************************************************/

    private static void unit_test_two ( )
    {

    }


    /*************************************************************************/

    private static void unit_test_three ( )
    {

    }

}


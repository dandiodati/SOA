package com.nightfire.framework.test;

import org.omg.CORBA.*;

import com.nightfire.unittest.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;


/*
 * Class for testing COS Naming wrapper class.
 */
class TestNameService extends UnitTestBase
{
    private static final String OBJECT_NAME = "Nightfire.spi.ILEC";


    /**
     * Executes unit-tests for COS Naming functionality.
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
            "Adding named CORBA object to COS Naming Service.",
            "Locating named CORBA object in COS Naming Service.",
            "Removing named CORBA object from COS Naming Service.",
            "List CORBA objects in COS Naming Service."
        };

        testNames = myTests;

        // Concrete child class should provide names of unit tests.
        assertTrue( testNames != null, "'testNames' variable is not null." );

        testCounter = 0;

        try
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init( args, null );

            logTestStart( );
            testAdd( orb );
            logTestEnd( );
            
            logTestStart( );
            testFind( orb );
            logTestEnd( );

            logTestStart( );
            testRemove( orb );
            logTestEnd( );

            logTestStart( );
            testList( orb );
            logTestEnd( );
        }
        catch ( Exception e )
        {
            if ( e instanceof CorbaException )
                log( "ERROR: Message [" + ((CorbaException)e).getMessage() 
                     + "], status-code [" + ((CorbaException)e).getStatusCode() + "]." );

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

    private static void testAdd ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Get a handle to a CORBA object (any object will do).
        org.omg.CORBA.Object testAdd = orb.resolve_initial_references( "NameService" );
        
        
        // Create locator and use it to add CORBA object with given name.
        ObjectLocator locator = new ObjectLocator( orb );
        
        locator.add( OBJECT_NAME, testAdd );
    }


    /*************************************************************************/

    private static void testFind ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Create locator and use it to find CORBA object with given name.
        ObjectLocator locator = new ObjectLocator( orb );
        
        org.omg.CORBA.Object testLocate = locator.find( OBJECT_NAME );
    }


    /*************************************************************************/

    private static void testRemove ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Create locator and use it to remove CORBA object with given name.
        ObjectLocator locator = new ObjectLocator( orb );
        
        locator.remove( OBJECT_NAME );
    }


    /*************************************************************************/

    private static void testList ( org.omg.CORBA.ORB orb ) throws Exception
    {
        // Get a handle to a CORBA object (any object will do).
        org.omg.CORBA.Object testAdd = orb.resolve_initial_references( "NameService" );
        
        
        // Create locator and use it to add CORBA object with given name.
        ObjectLocator locator = new ObjectLocator( orb );
        
        locator.add( "Nightfire.stooges.larry", testAdd );
        locator.add( "Nightfire.stooges.curly", testAdd );
        locator.add( "Nightfire.stooges.moe", testAdd );


        ObjectLocator.NamingContextInfo[] items = locator.list( "Nightfire" );

        assertTrue( items != null, "Non-null items returned from locator.list() call." );

        for ( int Ix = 0;  Ix < items.length;  Ix ++ )
            Debug.log( Debug.UNIT_TEST, "\tITEM [" + Ix + "]  =  [" + items[Ix].name + "], Is object? [" + items[Ix].isObject + "]." );


        items = locator.list( "Nightfire.stooges" );

        assertTrue( (items != null) && (items.length == 3), "Three items returned from locator.list() call." );

        for ( int Ix = 0;  Ix < items.length;  Ix ++ )
            Debug.log( Debug.UNIT_TEST, "\tITEM [" + Ix + "]  =  [" + items[Ix].name + "], Is object? [" + items[Ix].isObject + "]." );
                  

        items = locator.list( "Nightfire.stooges.curly" );

        assertTrue( items != null, "Non-null items returned from locator.list() call." );

        for ( int Ix = 0;  Ix < items.length;  Ix ++ )
            Debug.log( Debug.UNIT_TEST, "\tITEM [" + Ix + "]  =  [" + items[Ix].name + "], Is object? [" + items[Ix].isObject + "]." );


        // Clean up.
        locator.remove( "Nightfire.stooges.larry" );
        locator.remove( "Nightfire.stooges.curly" );
        locator.remove( "Nightfire.stooges.moe" );
    }
}









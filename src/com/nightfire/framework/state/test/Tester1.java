/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.state.test;

import java.sql.*;
import java.util.Vector;

import com.nightfire.framework.db.*;
import com.nightfire.unittest.*;
import com.nightfire.framework.state.*;
import com.nightfire.framework.transaction.*;
import com.nightfire.framework.util.*;


/**
 * 
 */
public class Tester1 extends UnitTestBase
{
    public static void main(String[] args)
    {
	String myTests[] = {
	    "OddEven Test, init to state ZERO",
	    "OddEven Test, init to state ONE",
	    "OddEven Test, init to state TWO",
	    "OddEven Test, init to state THREE",
	    "OddEven Test, init to state FOUR",
	    "OddEven Test, init to state ZERO, do negative tests",

	};	
	testNames = myTests;	// static from UnitTestBase
		try
		{
		    SetEnvironment.setSystemProperties("db.properties",true);
		    DBInterface.initialize();
		}
		catch (FrameworkException de)
		{
			de.printStackTrace();
			return;
		}
		
	doTests(args);
    }


    public static void doTests(String[] args) {
        try {
	    // Configure which debug log levels are displayed.
	    Debug.enableAll( );
	    Debug.disableAll( );
	    Debug.enable( Debug.UNIT_TEST );
	    Debug.enable( Debug.ASSERT );
	    initializeTests();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	try {
		StateMachine [] sm = {
	     new DBStateMachine("OddEven", "Zero"),
	     new DBStateMachine("OddEven", "One"),
	     new DBStateMachine("OddEven", "Two"),
	     new DBStateMachine("OddEven", "Three"),
	     new DBStateMachine("OddEven", "Four"),

	     new DBStateMachine("OddEven", "Zero"),
	};	

	    logTestStart( );
	    dotest0(sm[0]);
	    logTestEnd( );

	    logTestStart( );
	    dotest1(sm[1]);
	    logTestEnd( );

	    logTestStart( );
	    dotest2(sm[2]);
	    logTestEnd( );

	    logTestStart( );
	    dotest3(sm[3]);
	    logTestEnd( );

	    logTestStart( );
	    dotest4(sm[4]);
	    logTestEnd( );

	    logTestStart( );
	    dotest5(sm[5]);
	    logTestEnd( );

	} catch ( Exception e )  {
	    log( "Unit test \"" + testNames[testCounter] + 
		 "\" failed with the following exception:\n" + e.toString() );
            
	    log( getStackTrace( e ) );
	} finally {
	    cleanup();
	}
    	
    }

    private static void initializeTests() 
	throws Exception
    {
    }

    private static void cleanup() 
    {
    }


    
    /**********************************************************/
    private static void doProcessEvent(StateMachine sm, StateContext context,
				       String event) 
			    throws Exception
    {
	sm.processEvent(event,context,null);

    }

    /**
     * The test will stimulate the state machine[0] with each of 
     * the eventNames (0-4).  The odd->odd and even->even state
     * transitions should be invalid. 
     */

    private static void dotest0(StateMachine current_sm)
	throws Exception
    {
	String[] eventNames = OddEvenStateMachine.eventNames;
	StateContext context = new TestStateContext();

	State current_state = null;
	try {
	    doProcessEvent(current_sm, context, eventNames[0]);
	} catch (Exception e) {	    
	    log("Event zero in state ZERO should be invalid: " + e.toString());
	}
	doProcessEvent(current_sm, context, eventNames[1]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "One")),
		"State Machine failed 1.");
	doProcessEvent(current_sm, context, eventNames[2]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Two")),
		"State Machine failed 2.");
	doProcessEvent(current_sm, context, eventNames[3]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Three")),
		"State Machine failed 3.");
	doProcessEvent(current_sm, context, eventNames[4]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Four")),
		"State Machine failed 4.");
	
    }

    /**
     * The test will stimulate the state machine[1] with each of 
     * the eventNames (0-4).  The odd->odd and even->even state
     * transitions should be invalid. 
     */


    private static void dotest1(StateMachine current_sm)
	throws Exception
    {
	String[] eventNames = OddEvenStateMachine.eventNames;
	StateContext context = new TestStateContext();

	State current_state = null;
	doProcessEvent(current_sm, context, eventNames[0]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Zero")),
		"State Machine failed 0.");

	doProcessEvent(current_sm, context, eventNames[1]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "One")),
		"State Machine failed 1.");

	doProcessEvent(current_sm, context, eventNames[2]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Two")),
		"State Machine failed 2.");
	doProcessEvent(current_sm, context, eventNames[3]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Three")),
		"State Machine failed 3.");
	doProcessEvent(current_sm, context, eventNames[4]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Four")),
		"State Machine failed 4.");
	

    }

    /**
     * The test will stimulate the state machine[2] with each of 
     * the eventNames (0-4).  The odd->odd and even->even state
     * transitions should be invalid. 
     */


    private static void dotest2(StateMachine current_sm)
	throws Exception
    {
	String[] eventNames = OddEvenStateMachine.eventNames;
	StateContext context = new TestStateContext();

	State current_state = null;

	try {
	    doProcessEvent(current_sm, context, eventNames[0]);
	    current_state = current_sm.getCurrentState();
	} catch (Exception e) {	    
	    log("Event zero in state TWO should be invalid: " + e.toString());
	}

	doProcessEvent(current_sm, context, eventNames[1]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "One")),
		"State Machine failed 1.");

	doProcessEvent(current_sm, context, eventNames[2]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Two")),
		"State Machine failed 2.");

	doProcessEvent(current_sm, context, eventNames[3]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Three")),
		"State Machine failed 3.");

	doProcessEvent(current_sm, context, eventNames[4]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Four")),
		"State Machine failed 4.");
	
	
    }


    /**
     * The test will stimulate the state machine[3] with each of 
     * the eventNames (0-4).  The odd->odd and even->even state
     * transitions should be invalid. 
     */

    private static void dotest3(StateMachine current_sm)
	throws Exception
    {
	String[] eventNames = OddEvenStateMachine.eventNames;
	StateContext context = new TestStateContext();

	State current_state = null;
	doProcessEvent(current_sm, context, eventNames[0]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Zero")),
		"State Machine failed 0.");

	doProcessEvent(current_sm, context, eventNames[1]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "One")),
		"State Machine failed 1.");

	doProcessEvent(current_sm, context, eventNames[2]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Two")),
		"State Machine failed 2.");
	doProcessEvent(current_sm, context, eventNames[3]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Three")),
		"State Machine failed 3.");
	doProcessEvent(current_sm, context, eventNames[4]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Four")),
		"State Machine failed 4.");
	
    }

    /**
     * The test will stimulate the state machine[4] with each of 
     * the eventNames (0-4).  The odd->odd and even->even state
     * transitions should be invalid. 
     */

    private static void dotest4(StateMachine current_sm)
	throws Exception
    {
	String[] eventNames = OddEvenStateMachine.eventNames;
	StateContext context = new TestStateContext();

	State current_state = null;
	try {
	    doProcessEvent(current_sm, context, eventNames[0]);
	} catch (Exception e) {	    
	    log("Event zero in state ZERO should be invalid: " + e.toString());
	}
	doProcessEvent(current_sm, context, eventNames[1]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "One")),
		"State Machine failed 1.");
	doProcessEvent(current_sm, context, eventNames[2]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Two")),
		"State Machine failed 2.");
	doProcessEvent(current_sm, context, eventNames[3]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Three")),
		"State Machine failed 3.");
	doProcessEvent(current_sm, context, eventNames[4]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "Four")),
		"State Machine failed 4.");
	
    }

	


    /**
     * The test will stimulate the state machine[5] with invalid
     * events.
     */
    private static void dotest5(StateMachine current_sm)
	throws Exception
    {
	String[] eventNames = OddEvenStateMachine.eventNames;
	StateContext context = new TestStateContext();

	State current_state = null;
	try {
	    doProcessEvent(current_sm, context, eventNames[0]);
	} catch (Exception e) {	    
	    log("Event zero in state ZERO should be invalid: " + e.toString());
	}

	try {
	    doProcessEvent(current_sm, context, eventNames[2]);
	} catch (Exception e) {	    
	    log("Event two in state ZERO should be invalid: " + e.toString());
	}

	doProcessEvent(current_sm, context, eventNames[1]);
	current_state = current_sm.getCurrentState();
        assertTrue( (checkStateName(current_state, "One")),
		"State Machine failed 1.");

	try {
	    doProcessEvent(current_sm, context, eventNames[1]);
	} catch (Exception e) {	    
	    log("Event one in state ONE should be invalid: " + e.toString());
	}

	try {
	    doProcessEvent(current_sm, context, eventNames[3]);
	} catch (Exception e) {	    
	    log("Event three in state ONE should be invalid: " + e.toString());
	}

    }


	static boolean checkStateName(State state, String name) throws StateException
	{
		return state.getStateName().equals(name);
	}
	
    static class TestStateContext 
	implements StateContext
    {
    }


}


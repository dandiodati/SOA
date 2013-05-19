/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.state.test;

import com.nightfire.framework.state.*;

/**
 * This state machine defines five state classes, StateOne,
 * StateTwo,etc., and allows transitions from odd to even states, and
 * even to odd states. It doesn't allow odd to odd states or even to
 * even states.
 *
 * The test will create five state machines, one in each of the
 * states, and stimulate each one with the events: one, two, three,
 * four.  The state machine should only allow odd->even or even->odd
 * transitions. Verify that every other kind of transition is
 * disallowed.
 *
 */
public class OddEvenStateMachine //extends StateMachine
{
	/**
	 * Define names for all required states
	 */
	public static final State ZERO		= new StateZero();
	public static final State ONE		= new StateOne();
	public static final State TWO		= new StateTwo();
	public static final State THREE	        = new StateThree();
	public static final State FOUR	        = new StateFour();

	
/*
	public OddEvenStateMachine(State initialState)
	throws StateException
	{
	    super(initialState.toString());
	}
*/	
	
	
	/**
	 * Load all state transition rules using super class function addRule
	 */

/*
	protected final void loadRules()
	{
		addRule(null, null, ZERO); // initial state

		addRule(ZERO, "one", ONE);
		// addRule(ZERO, "two", TWO);
		addRule(ZERO, "three", THREE);
		// addRule(ZERO, "four", FOUR);

		addRule(ONE, "zero", ZERO);
		//addRule(ONE, "one", ONE);
		addRule(ONE, "two", TWO);
		//addRule(ONE, "three", THREE);
		addRule(ONE, "four", FOUR);
 
		addRule(TWO, "one", ONE);
		// addRule(TWO, "two", TWO);
		addRule(TWO, "three", THREE);
		// addRule(TWO, "four", FOUR);

		addRule(THREE, "zero", ZERO);
		// addRule(THREE, "one", ONE);
		addRule(THREE, "two", TWO);
		// addRule(THREE, "three", THREE);
		addRule(THREE, "four", FOUR);

		addRule(FOUR, "one", ONE);
		// addRule(FOUR, "two", TWO);
		addRule(FOUR, "three", THREE);
		// addRule(FOUR, "four", FOUR);
	}
*/


    public static String[] eventNames = {
	"zero", "one", "two", "three", "four",
    };


}

/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/StateTransitionRuleManager.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import com.nightfire.framework.util.NVPair;


/**
 * The state transition rule manager manages a set of state transition rules for a state machine. 
 * 
 */
public interface StateTransitionRuleManager 
{
	/**
	 * Returns the destination state for a given state event from the given state.
	 * 
	 * @param	fromState 			-- Specifies the state when the state machine receives the event.
	 * @param	stateEvent 		 	-- The input state event.
	 * 
	 * @exception	StateException when error occurs.
	 */
	public State getDestinationState(State fromState, String stateEvent) 
	    throws StateException;
	
	
	/**
	 * Returns an state instance for the named state.
	 * 
	 * @param	stateName -- The name of the state.
	 * 
	 * @exception	StateException when error occurs.
	 */
	public State getStateForName(String stateName) 
	    throws StateException;


	/**
	 * Get all of the valid state transition rules associated with a given state.
	 * 
	 * @param	stateName  Name of the state to get the rules for.
	 * 
	 * @return  An array of name/value-pairs containing event/nextState values, or
     *          null if no rules were found for the state.  The next-state value
     *          is the state's name.
	 * 
	 * @exception	StateException  thrown on errors.
	 */
    public NVPair[] getTransitionRulesForState ( String stateName )
        throws StateException;


	/**
	 * Get a list of all states that accept the given event as valid, and
     * what the corresponding new state will be.
	 * 
	 * @param	eventName  Name of the event to get accepting states for.
	 * 
	 * @return  An array of name/value-pairs containing currentState/nextState values, or
     *          null if no acceptable transition rules were found.  The next-state value
     *          is the state's name.
	 * 
	 * @exception	StateException  thrown on errors.
	 */
    public NVPair[] getStatesThatAcceptEvent ( String eventName )
        throws StateException;
}

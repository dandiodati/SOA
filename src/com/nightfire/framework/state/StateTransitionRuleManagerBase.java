/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/StateTransitionRuleManagerBase.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import java.util.*;

import com.nightfire.framework.util.*;


/**
 * The abstract implementation of state transition rule manager which uses hashtable to store state to class mappings
 * and state transition rules. 
 * 
 */
public abstract class StateTransitionRuleManagerBase implements StateTransitionRuleManager
{
	private Hashtable stateClassMap;
	private Hashtable rules;
	
	/**
	 * Returns the name of destination state for a give state event from current state.
	 * 
	 * @param	currentStateName -- Specifies current state.
	 * @param	stateEvent 		 -- The input state event.
	 * 
	 * @exception	StateException when error occurs.
	 */
	public State getDestinationState(State fromState, String stateEvent) 
	    throws StateException
	{
		if (fromState == null)
		{
			throw new StateException("ERROR: Null reference passed as current state name.");
		}
		if (stateEvent == null)
		{
			throw new StateException("ERROR: Null reference passed as state event.");
		}
		Hashtable eventsForOneState = (Hashtable) rules.get(fromState.getStateName());
		if (eventsForOneState == null)
		{
			Debug.log(Debug.ALL_WARNINGS, "WARNING: No rules has been defined for state[" + fromState.getStateName() + "].");
			return null;
		}
		String toStateName = (String) eventsForOneState.get(stateEvent);
		if (toStateName == null)
		{
			Debug.log(Debug.ALL_WARNINGS, "WARNING: State event[" + stateEvent + "] is not accepted for state[" + fromState.getStateName() + "].");
			return null;
		}
		return getStateForName(toStateName);
	}
		
	
	
	/**
	 * Returns an state instance for the named state.
	 * 
	 * @param	stateName -- The name of the state.
	 * 
	 * @exception	StateException when error occurs.
	 */
	public State getStateForName(String stateName) 
	    throws StateException
	{
		if (stateName == null)
		{
			throw new StateException("ERROR: Null reference passed as state name.");
		}
		String stateClassName = (String)stateClassMap.get(stateName);
		if (stateClassName == null)
		{
			throw new StateException("ERROR: No class has been defined for state [" + stateName + "].");
		}
		try
		{
			State state = (State) ObjectFactory.create(stateClassName, State.class);
			state.setStateName(stateName);
			return state;
		}
		catch (FrameworkException fe)
		{
			throw new StateException("ERROR: Cannot create state [" + stateName + "] with class [" + stateClassName + "].\n" + fe.getMessage());
		}
	}


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
        throws StateException
    {
		if ( stateName == null)
		{
			throw new StateException("ERROR: Null value passed as current state name.");
		}

        // Get all of the transition rules associated with named state.
		Hashtable transitionsForOneState = (Hashtable)rules.get( stateName );

        if ( transitionsForOneState == null )
            return null;

        int count = transitionsForOneState.size();

        NVPair[] transitions = new NVPair[ count ];

        // While rules are found, populate name/value pair objects with stateEvent/nextState values.
        Enumeration iter = transitionsForOneState.keys( );

        for ( int Ix = 0;  iter.hasMoreElements();  Ix ++ )
        {
            String stateEvent = (String)iter.nextElement( );

            String nextState = (String)transitionsForOneState.get( stateEvent );

            transitions[Ix] = new NVPair( stateEvent, nextState );
        }

        return transitions;
    }


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
        throws StateException
    {
		if ( eventName == null)
		{
			throw new StateException("ERROR: Null value passed as event name.");
		}

        Vector results = new Vector( );

        // While states are available, populate name/value pair objects 
        // with currentState/nextState name values.
        Enumeration iter = rules.keys( );

        while ( iter.hasMoreElements() )
        {
            // Get the next candidate state.
            String curState = (String)iter.nextElement( );

            // Get all of the transition rules for the candidate state.
            Hashtable transitionsForOneState = (Hashtable)rules.get( curState );

            if ( transitionsForOneState == null )
                continue;

            // See if the event maps to a valid next-state (i.e., a transition
            // rule exists).
            String nextState = (String)transitionsForOneState.get( eventName );

            // If a transition rule exists for the candidate state,
            // Add the state name and the next state name to the results.
            if ( nextState != null )
                results.addElement( new NVPair( curState, nextState ) );
        }

        int count = results.size();
        
        NVPair[] pairs = new NVPair[ count ];

        results.copyInto( pairs );

        return pairs;
    }


	/**
	 * Sets the internal hashtable of state to class mapping. The key of the hashtable is the name of the state.
	 * And the corresponding value is the class name.
	 * 
	 * @param	vStateClassMap -- Specifies input hashtable that contains state to class mapping.
	 * 
	 * @exception	StateException when error occurs.
	 */
	protected void setStateClassMap(Hashtable vStateClassMap) throws StateException
	{
		if (vStateClassMap == null)
		{
			throw new StateException("ERROR: Null reference passed as state class map.");
		}
		stateClassMap = vStateClassMap;
	}


	/**
	 * Sets the internal hashtable of state transition rule. The key of the hashtable is the name of the state.
	 * And the corresponding value is a hashtable that contains events and the name of destination states.
	 * 
	 * @param	vRules -- Specifies input rules.
	 *                    Each element contains name of the origination state, event and destination state.
	 * 
	 * @exception	StateException when error occurs.
	 */
	protected void setStateTransitionRules(Vector vRules) throws StateException
	{
		if (vRules == null)
		{
			throw new StateException("ERROR: Null reference passed as state transition rules.");
		}
		rules = new Hashtable();
		for (Enumeration it=vRules.elements(); it.hasMoreElements();)
		{
			String[] oneRule = (String[]) it.nextElement();
			String fromState = oneRule[0];
			String stateEvent = oneRule[1];
			String toState = oneRule[2];
			
			Hashtable rulesForOneState = (Hashtable) rules.get(fromState);
			if (rulesForOneState == null)
			{
				rulesForOneState = new Hashtable();
				rules.put(fromState, rulesForOneState);
			}
			
			rulesForOneState.put(stateEvent, toState);
		}
	}
	
}

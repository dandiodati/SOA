/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/StateMachine.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import com.nightfire.framework.util.*;
import com.nightfire.framework.transaction.Transaction;
import java.util.Hashtable;


/**
 * This is the base (super) class for all client implemented State Machine classes.
 * 
 * It is responsible for state initialization when object is created, encapsulating
 * state change routine and maintaining references to the current and previous states.
 */
public abstract class StateMachine
{
    /**
     * Manages the state to class mapping and state transition rules.
     */
    private StateTransitionRuleManager   strm           = null;
        
    /**
     * Current state of the context.
     */
    private State       currentState    = null;
        
    /**
     * Constructs and initializes the StateTransitionRuleManager. Note the currentState is not initialized.
	 * 
	 * @param	vStrm -- Specifies the state transition rule manager.
	 * 
	 * @exception	StateException when error occurs.
     */
    protected StateMachine(StateTransitionRuleManager vStrm) throws StateException
    {
        strm = vStrm;
    }
        
        
    /**
     * Constructs and initializes the StateTransitionRuleManager and the currentState is set to initialState.
	 * 
	 * @param	vStrm -- Specifies the state transition rule manager.
     * @param   initialState -- Initial state that machine has to be set.
     * 
	 * @exception	StateException when error occurs.
     */
    protected StateMachine(StateTransitionRuleManager vStrm, String initialState) throws StateException
    {
        strm = vStrm;
                
        currentState = (State)strm.getStateForName(initialState);
    }
        
        
    /**
     * Returns the current state of the state machine.
     *
	 * @return	the current state of the state machine.
     */
    public final State getCurrentState()
    {
    	return currentState;
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
        return( strm.getTransitionRulesForState( stateName ) );
    }


	/**
	 * Get all of the valid state transition rules associated with the current state.
	 * 
	 * @return  An array of name/value-pairs containing event/nextState values, or
     *          null if no rules were found for the state.  The next-state value
     *          is the state's name.
	 * 
	 * @exception	StateException  thrown on errors.
	 */
    public NVPair[] getTransitionRulesForCurrentState ( )
        throws StateException
    {
        return( strm.getTransitionRulesForState( currentState.getStateName() ) );
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
        return( strm.getStatesThatAcceptEvent( eventName ) );
    }


    /**
     * Initialize the state of the context object when loaded from the database.
     *
     * @param initialState The state string as loaded from the database.
     * 
	 * @exception	StateException when error occurs.
     */
    public void initializeState(String initialState) throws StateException
    {
    	if (strm == null)
    	{
    		throw new StateException("ERROR: State transition rule manager has not been set.");
    	}
        currentState = (State)strm.getStateForName(initialState);
    }

    /**
     * Checks whether an event is valid for the current state.
     *
     * @param           event -- same as it defined in mTransitionRules
     *
     * @exception       StateException thrown if failed.
     * @exception       StateRejectedException if transition is invalid.
     */
    public boolean isValidEvent(String event)
        throws StateException, StateRejectedException
    {
        String current = currentState.getStateName( );

        Debug.log( Debug.STATE_STATUS, "Check validity of state transition for event [" 
                   + event + "] given current state [" + current + "]." );


        if ( Debug.isLevelEnabled( Debug.STATE_STATUS ) )
        {
            NVPair[] rules = getTransitionRulesForState( current );

            StringBuffer sb = new StringBuffer( );

            sb.append( "Existing state transition rules for current state [" + current + "]:\n" );

            if ( rules != null )
            {
                for ( int Ix = 0;  Ix < rules.length;  Ix ++ )
                {
                    sb.append( '\t' );
                    sb.append( rules[Ix].name );
                    sb.append( " --> " );
                    sb.append( (String)rules[Ix].value );
                    sb.append( '\n' );
                }
            }

            Debug.log( Debug.STATE_STATUS, sb.toString() );


            NVPair[] statePairs = getStatesThatAcceptEvent( event );

            sb = new StringBuffer( );

            sb.append( "States that accept event [" + event 
                       + "] as valid, with resulting new state:\n" );

            if ( statePairs != null )
            {
                for ( int Ix = 0;  Ix < statePairs.length;  Ix ++ )
                {
                    sb.append( '\t' );
                    sb.append( statePairs[Ix].name );
                    sb.append( " --> " );
                    sb.append( (String)statePairs[Ix].value );
                    sb.append( '\n' );
                }
            }

            Debug.log( Debug.STATE_STATUS, sb.toString() );
        }


        State newState = null;

        try
        {
        	newState = getDestination(event);
        }
        catch (StateException se)
        {
        	Debug.log( Debug.ALL_WARNINGS, "WARNING: Failed to get destination for event [" 
                       + event + "].");

        	return false;
        }

        if (newState != null)
        {
            Debug.log( Debug.STATE_STATUS, "Event is valid." );
            
            return true;
        }
        else
        {
            Debug.log( Debug.STATE_STATUS, "Event is not valid." );
            
            return false;
        }
    }


    /**
     * Encapsulates calls to the functions of the State class object
     * processEvent(event,context)and exit(context), where it passes event
     * and its context as an argument. This method declared as final
     * to prevent overwriting from sub-classes. The first action this
     * function does is checking currentState if it is equal to null.
     * It can be null only when object has just been instantiated, then
     * it will skip processEvent and exit and will call enter method in
     * its initialization state as it is defined by user.
     * 
     * @param           event -- same as it defined in mTransitionRules
     * @param           context -- reference to StateContext object
     * @param           tx -- the transaction to be used
     * 
     * @exception       StateException when error occures.
     * @exception       StateRejectedException if transition is invalid.
     */
    protected final void handleEvent(String event, StateContext context,
				     Transaction tx)
        throws StateException, StateRejectedException
    {
        Debug.log(Debug.STATE_BASE, "Received event [" + event + "] at state: " + currentState);
        State newState = getDestination(event);
        Debug.log(Debug.STATE_BASE, "Destination state determined to be " + newState);
                
        if (newState != null) {
            Debug.log(Debug.STATE_STATUS, "Starting event processing...");
            currentState.processEvent(event, context,tx);
                        
            Debug.log(Debug.STATE_STATUS, "Event [" + event + "] processed succsessfully. " +
                      "Ready to leave state " + currentState);
            currentState.exit(context,tx);
            Debug.log(Debug.STATE_STATUS, "State " + currentState + " exited...");
        } else {

            throw new StateException("Invalid event published to state machine.  Event: [" + 
                                 event + "] is not legal in state: [" + currentState + "]");
        }
                
        Debug.log(Debug.STATE_STATUS, "Entering " + newState + " ... " );
        newState.enter(context,tx);
        currentState = newState;
        Debug.log(Debug.STATE_STATUS, "State entered succsessfully. Current state now " + newState);
    }
        
        

    /**
     * This method is used to notify this state machine that a state change
     * related event has occurred. It simply calls the handleEvent() method.
     *
     * @param event The name of the event. See StateEvents class for a list of 
     *        event names.
     * @param           context -- reference to StateContext object
     * @param           tx -- the transaction to be used
     * 
     * @exception       StateException when error occures
     * @exception       StateRejectedException Thrown if the event is invalid in 
     *                         the current state.
     */ 
    public void processEvent(String event, StateContext context, 
			     Transaction tx) 
    throws StateRejectedException, StateException
    {
		this.handleEvent(event, context,tx);
    }


    /**
     * find out what is the destination state for this event given current state
     * of the object in accordance to defined mTransitionRules.
     * 
     * @param           String event -- same as it defined in mTransitionRules
     * 
     * @exception       StateException when error occures.
     * 
     */
    private State getDestination(String event) throws StateException
    {
    	if (strm == null)
    	{
    		throw new StateException("ERROR: State transition rule manager has not been set.");
    	}
        return strm.getDestinationState(currentState, event);
    }
        
}

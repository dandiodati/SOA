/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/State.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import com.nightfire.framework.transaction.Transaction;
import com.nightfire.framework.util.Debug;


/**
 * An object in a program frequently has an internal "state" and the behavior
 * of the object needs to change when its state changes. The State pattern takes
 * advantage of polymorphism to implement such state-dependent behavior in an
 * object-oriented program. 
 * 
 * The State class is an abstract class. It provides some basic behavior, but its
 * only real purpose is to be extended to produce "real" state classes.
 */
public abstract class State 
{
	private String stateName = null;
	
	/**
	 * Because the constructor is protected, it cannot be used by other classes
	 * to create new objects. Since the State class is abstract, only the subclasses
	 * have access to the constructor, which is equivalent to making the subclass
	 * constructors private.
	 */
	protected State() 
	{
	}
	
	
	/**
	 * Basic, no functionality implementation of the exit method.
	 * It shall be overwritten in subclasses. However, if subclass does not
	 * require specific implementation of the exit function this one will
	 * be called.
	 * 
	 * @param	context -- StateContext object
	 * 
	 * @exception	StateException when error occurs.
	 */
	public void exit(StateContext context, Transaction tx) 
	    throws StateException
	{
		Debug.log(Debug.STATE_BASE, "State exit is not defined. " +
									"Using basic state exit method.");
	}
	
	
	/**
	 * Basic, no functionality implementation of the enter method.
	 * It shall be overwritten in subclasses. However, if subclass does not
	 * require specific implementation of the enter function this one will
	 * be called.
	 * 
	 * @param	context -- StateContext object
	 * 
	 * @exception	StateException when error occurs.
	 */
	public void enter(StateContext context, Transaction tx) 
	    throws StateException
	{
		Debug.log(Debug.STATE_BASE, "State enter is not defined. " +
									"Using basic state enter method.");
	}
	
	
	/**
	 * Returns the name of the state.
	 * 
	 * @return	Name of state
	 * 
	 * @exception	StateException when error occurs.
	 */
	public String getStateName() 
	    throws StateException
	{
		return stateName;
	}
	
	
	/**
	 * Must be implemented by the subclasses that extend the State class.
	 * Implementation of this method supposed to perform all of the necessary
	 * activity before exiting the state and transition to another one.
	 * 
	 * @param		event	-- value determining activity performed
	 * @param		context	-- reference to the StateContext object
	 * 
	 * @exception	StateException when error occurs.
	 * @exception	StateRejectedException if transition is invalid
	 */
	protected void processEvent(String event, 
				    StateContext context, 
				    Transaction tx
				    )
	throws StateException, StateRejectedException
        {
        }

	/**
	 * Set the name of the state.
	 * 
	 * @param	vStateName -- Name of state
	 * 
	 * @exception	StateException when error occurs.
	 */
	protected void setStateName(String vStateName) 
	    throws StateException
	{
		stateName = vStateName;
	}
	
	
}

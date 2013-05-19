/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.state.test;

import com.nightfire.framework.transaction.Transaction;
import com.nightfire.framework.state.*;
import com.nightfire.framework.util.Debug;


/**
 *  implementation of the ACK state
 */
public class StateAck extends State
{
	/**
	 *  Implementation of the processEvent method.
	 * 
	 * @param	event -- State change event
	 * @param	context -- StateContext object
	 * 
	 * @exception	StateException when error occures
	 * @exception	StateRejectedException when state transition is invalid
	 */
	public void processEvent(String Event, StateContext context, 
				 Transaction tx)
		throws StateException, StateRejectedException
	{
		Order order = (Order) context;
		order.counter++;
	}
	
	
	/**
	 *  Implementation of the exit method.
	 * 
	 * @param	context -- StateContext object
	 * 
	 * @exception	StateException when error occures
	 */
	public void exit(StateContext context, 
				 Transaction tx) throws StateException
	{
		Order order = (Order) context;
		order.str = "Goodbye ACK state...";
		Debug.log(Debug.ALL_WARNINGS, "\nCounter = " + order.counter);
		Debug.log(Debug.ALL_WARNINGS, "We are saying: " + order.str);
		Debug.log(Debug.ALL_WARNINGS, "This is : " + order.toString());
	}

	/**
	 *  Implementation of the enter method.
	 * 
	 * @param	context -- StateContext object
	 * 
	 * @exception	StateException when error occures
	 */
	public void enter(StateContext context, 
				 Transaction tx) throws StateException
	{
		Order order = (Order) context;
		order.str = "Hello ACK state...";
		Debug.log(Debug.ALL_WARNINGS, "Counter = " + order.counter);
		Debug.log(Debug.ALL_WARNINGS, "We are saying: " + order.str);
		Debug.log(Debug.ALL_WARNINGS, "This is : " + order.toString());
	}
}

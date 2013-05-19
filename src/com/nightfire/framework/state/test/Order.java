/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.state.test;

import com.nightfire.framework.state.*;
import com.nightfire.framework.util.*;

/**
 * This is example of implementing Order class,
 * that is using State Machine
 */
public class Order implements StateContext
{
	
	/**
	 * Define object of the StateMachine
	 */
	private OrderStateMachine osm = null;
	
	
	/**
	 * Some local variable, that wil get changed while
	 * moving from state to state
	 */
	public int counter = 0;
	public String str = "Hello";
	
	
	/**
	 * Initialize StateMachine with own refernce
	 */
	public Order() throws StateException
	{
		osm = new OrderStateMachine(this);
	}
	
	/**
	 * Initialize StateMachine with own refernce
	 */
	public Order(String initialState) throws StateException
	{
		osm = new OrderStateMachine(this, initialState);
	}
	
	
	/**
	 * Sample implementation of the handleEvent
	 * 
	 * @param	event -- received event
	 * 
	 * @exception	StateException when error occures
	 * @exception	StateRejectedException if transition is invalid
	 */
	public void handleEvent(String event)
		throws StateException, StateRejectedException
	{
		osm.handleEvent(event);
	}
	
	/**
	 * Sample of use
	 */
	public static void main(String[] args)
	{
		Debug.enableAll();
		FrameworkException.showStackTrace();
		try
		{
			Order ord = new Order("Initial");
			ord.handleEvent("SENTEvent");
			ord.handleEvent("ACKEvent");
			ord.handleEvent("SOCEvent");
		}
		catch (StateException e)
		{
			System.out.println("Got EXC: " + e.getMessage());
		}
		catch (StateRejectedException e)
		{
			System.out.println("Got EXC: " + e.getMessage());
		}
	}
}

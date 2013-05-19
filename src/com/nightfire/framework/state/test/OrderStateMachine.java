/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.state.test;

import java.util.*;

import com.nightfire.framework.state.*;

/**
 * This is example of implementing specific StateMachine class
 */
public class OrderStateMachine extends StateMachine
{
	public static final String ORDER_STATE_MACHINE_NAME	= "Test.Order";

	/**
	 * Define owner object (StateContext) and initialize it
	 * in the constructor
	 */
	private Order order = null;
	
	
	public OrderStateMachine(Order order) throws StateException
	{
		super(new OrderRuleManager(ORDER_STATE_MACHINE_NAME), "Initial");
		this.order = order;
	}
	
	
	public OrderStateMachine(Order order, String initialState) throws StateException
	{
		super(new OrderRuleManager(ORDER_STATE_MACHINE_NAME), initialState);
		this.order = order;
	}
	
	
	/**
	 * Implement method handleEvent to hide the one from super class
	 * 
	 * @param	event - State change event
	 * @exception	StateException thrown if error occures
	 * @exception	StateRejectedException thrown if state transition fails
	 */
	public void handleEvent(String event)
		throws StateException, StateRejectedException
	{
		handleEvent(event, order,null);
	}
	
}



class OrderRuleManager extends StateTransitionRuleManagerBase
{
	OrderRuleManager(String stateMachineName) throws StateException
	{
		super();
		setStateClassMap(loadStateClassMap());
		setStateTransitionRules(loadStateTransitionRules());
	}
	
	/**
	 * Load all state transition rules using super class function addRule
	 */
	private Hashtable loadStateClassMap()
	{
		Hashtable map = new Hashtable();
		map.put("Initial", "com.nightfire.framework.state.NoProcessingState");
		map.put("Sent", "com.nightfire.framework.state.test.StateSent");
		map.put("Acknowleged", "com.nightfire.framework.state.test.StateAck");
		map.put("Completed", "com.nightfire.framework.state.test.StateCompleted");
		return map;
	}


	
	/**
	 * Load all state transition rules using super class function addRule
	 */
	private Vector loadStateTransitionRules()
	{
		Vector rules = new Vector();
		addRule(rules, "Initial", "SENTEvent", "Sent");
		addRule(rules, "Sent", "ACKEvent", "Acknowleged");
		addRule(rules, "Acknowleged", "SOCEvent", "Completed");
		return rules;
	}


	private void addRule(Vector toRules, String fromState, String stateEvent, String toState)
	{
		toRules.addElement(formatRule(fromState, stateEvent, toState));
	}
	

	public static String[] formatRule(String fromState, String stateEvent, String toState)
	{
		String[] rule = new String[3];
		rule[0] = fromState;
		rule[1] = stateEvent;
		rule[2] = toState;
		return rule;
	}
		
}

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
public class StateFour extends State
{
    public String toString() {
	return "Four";
    }

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
	Debug.log(Debug.UNIT_TEST, "processEvent called on " + toString());
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
	Debug.log(Debug.UNIT_TEST, "Goodbye " + toString() + " state...");
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
	Debug.log(Debug.UNIT_TEST, "Hello " + toString()  + " state...");
    }
}

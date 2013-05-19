/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/DBStateMachine.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.transaction.Transaction;
import java.util.Hashtable;


/**
 * This state machine class uses DBStateTransitionManager to manage 
 * its state to class mapping and state transition rules.
 * 
 */
public class DBStateMachine extends StateMachine
{
	/**
	 * Constructs the DBStateMachine using DBStateTransitionRuleManager created by the factory for the named state machine.
	 * The current state is not initialized.
	 *
	 * @param	stateMachineName -- Specifies the name of the state machine.
	 * 
	 * @exception	StateException when error occurs.
	 */
    public DBStateMachine(String stateMachineName) throws StateException
    {
        super(DBStateTransitionRuleManagerFactory.getInstance().getStateTransitionRuleManager(stateMachineName));
    }
        
        
	/**
	 * Constructs the DBStateMachine using DBStateTransitionRuleManager created by the factory for the named state machine.
	 * And the current state is initialized to the given state.
	 *
	 * @param	stateMachineName -- Specifies the name of the state machine.
     * @param   Initial state that machine has to be set.
	 * 
	 * @exception	StateException when error occurs.
	 */
    public DBStateMachine(String stateMachineName, String initialState) throws StateException
    {
        super(DBStateTransitionRuleManagerFactory.getInstance().getStateTransitionRuleManager(stateMachineName), initialState);
    }
        
        
}

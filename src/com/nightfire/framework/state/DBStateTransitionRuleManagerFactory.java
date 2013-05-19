/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/DBStateTransitionRuleManagerFactory.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import java.util.*;
import java.sql.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;


/**
 * The factory manages instances of state transition rule managers. 
 * 
 */
public class DBStateTransitionRuleManagerFactory 
{
	private static DBStateTransitionRuleManagerFactory instance = null;
	
	/**
	 * Internal cache of DBStateTransitionRuleManagers.
	 */
	private Hashtable managers = null;
	
	/**
	 * Constructs the factory instance. Singleton pattern.
	 */
	private DBStateTransitionRuleManagerFactory()
	{
		managers = new Hashtable();
	}

	/**
	 * Returns the singleton factory instance.
	 *
	 * @return The singleton factory instance.
	 */
	public static synchronized DBStateTransitionRuleManagerFactory getInstance()
	{
		if (instance == null)
		{
			instance = new DBStateTransitionRuleManagerFactory();
		}
		return instance;
	}

	/**
	 * Returns the DBStateTransitionRuleManager for the named state machine. If it is not
	 * already in the cache, an instance will be created and cached.
	 *
	 * @param	stateMachineName -- Specifies the name of the state machine.
	 * 
	 * @return The DBStateTransitionRuleManager associated with the named state machine.
	 * 
	 * @exception	StateException when error occurs.
	 */
	public StateTransitionRuleManager getStateTransitionRuleManager(String stateMachineName) throws StateException
	{
		StateTransitionRuleManager strm = (StateTransitionRuleManager)managers.get(stateMachineName);
		if (strm == null)
		{
			strm = new DBStateTransitionRuleManager(stateMachineName);
			managers.put(stateMachineName, strm);
		}
		return strm;
	}
	
}

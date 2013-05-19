/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/DBStateTransitionRuleManager.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import java.util.*;
import java.sql.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;


/**
 * The default implementation of state transition rule manager. 
 * 
 */
public class DBStateTransitionRuleManager extends StateTransitionRuleManagerBase 
{
	private static final String SELECT_STATE_CLASS_MAP = 
			"SELECT StateName, StateClassName FROM StateMachineMap WHERE StateMachineName = ?";
	
	private static final String SELECT_STATE_TRANSITION_RULE = 
			"SELECT FromStateName, StateEvent, ToStateName FROM StateTransitionRule WHERE StateMachineName = ?";
	

    /**
     * Constructs and initializes the DBStateTransitionRuleManager by load state machine information from DB.
     * Note the currentState is not initialized.
	 * 
	 * @param	stateMachineName -- Specifies the name of the state machine.
	 * 
	 * @exception	StateException when error occurs.
     */
	public DBStateTransitionRuleManager(String stateMachineName) throws StateException
	{
		super();
		Connection con = null;
		try
		{
			con = DBInterface.acquireConnection();
		}
		catch (DatabaseException de)
		{
			throw new StateException("ERROR: Failed to acqure a database connection.\n" + de.getMessage());
		}
		
		try
		{
			Hashtable stateClassMap = loadStateClassMap(con, stateMachineName);
			setStateClassMap(stateClassMap);
			Vector rules = loadStateTransitionRules(con, stateMachineName);
			setStateTransitionRules(rules);
		}
		catch (Exception e)
		{
			throw new StateException("ERROR: Failed to load state machine definitions for [" + stateMachineName + "] from database.\n" + e.getMessage());
		}
		finally
		{
			try
			{
				DBInterface.releaseConnection(con);
			}
			catch (DatabaseException de)
			{
				throw new StateException("ERROR: Failed to return the database connection.\n" + de.getMessage());
			}
		}
	}
	
	
	/**
	 * Query the StateMachineMap table and load all mappings for the named state machine.
	 *
	 * @param	con -- The database connection.
	 * @param	stateMachineName -- Specifies the name of the state machine.
	 * 
	 * @exception	Exception when error occurs.
	 */
	private static Hashtable loadStateClassMap(Connection con, String stateMachineName) throws Exception
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			ps = DBInterface.getPreparedStatement(con, SELECT_STATE_CLASS_MAP);
			ps.setString(1, stateMachineName);
			
			rs = ps.executeQuery();
			
			Hashtable result = new Hashtable();
			
			while (rs.next())
			{
				result.put(rs.getString(1), rs.getString(2));
			}
			
			return result;
		}
		finally
		{
			if (rs != null)
			{
				rs.close();
			}
			if (ps != null)
			{
				ps.close();
			}
		}
	}
			
	
	/**
	 * Query the StateTransitionRule table and load all rules for the named state machine.
	 *
	 * @param	con -- The database connection.
	 * @param	stateMachineName -- Specifies the name of the state machine.
	 * 
	 * @exception	Exception when error occurs.
	 */
	private static Vector loadStateTransitionRules(Connection con, String stateMachineName) throws Exception
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			ps = DBInterface.getPreparedStatement(con, SELECT_STATE_TRANSITION_RULE);
			ps.setString(1, stateMachineName);
			
			rs = ps.executeQuery();
			
			Vector result = new Vector();
			
			while (rs.next())
			{
				String[] oneRule = new String[3];
				oneRule[0] = rs.getString(1);
				oneRule[1] = rs.getString(2);
				oneRule[2] = rs.getString(3);
				
				result.addElement(oneRule);
			}
			
			return result;
		}
		finally
		{
			if (rs != null)
			{
				rs.close();
			}
			if (ps != null)
			{
				ps.close();
			}
		}
	}
			
	
}

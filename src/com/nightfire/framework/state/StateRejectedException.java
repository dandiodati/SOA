/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/StateRejectedException.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import com.nightfire.framework.util.*;


/**
 * This class will be used when a state transition is illegal.
 */
public class StateRejectedException extends FrameworkException
{
	/**
	 * Create a state exception object with the given message.
	 *
	 * @param  msg  Error message associated with exception.
	 */
	public StateRejectedException (String msg)
	{
		super(msg);
	}


	/**
	 * Create a state exception object with the given exception's message.
	 *
	 * @param  e  Exception object used in creation.
	 */
	public StateRejectedException (Exception e)
	{
		super(e);
	}
}

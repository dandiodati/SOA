/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 * $Header: //nfcommon/R4.4/com/nightfire/framework/state/StateException.java#1 $ 
 *
 */

package com.nightfire.framework.state;

import com.nightfire.framework.util.*;


/**
 * Base class for all state exceptions.
 */
public class StateException extends FrameworkException
{
	/**
	 * Create a state exception object with the given message.
	 *
	 * @param  msg  Error message associated with exception.
	 */
	public StateException (String msg)
	{
		super(msg);
	}


	/**
	 * Create a state exception object with the given exception's message.
	 *
	 * @param  e  Exception object used in creation.
	 */
	public StateException (Exception e)
	{
		super(e);
	}
}

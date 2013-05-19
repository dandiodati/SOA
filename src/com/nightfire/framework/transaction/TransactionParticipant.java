/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction;

import java.util.Vector;

import com.nightfire.framework.util.Debug;


/**
 * Interface that must be implemented by all classes participating in transactions.
 */
public interface TransactionParticipant
{
    /** 
     * Save all work done thus far using the given context.
     *
     * @param  context Context to save work against.
     */
    void save(SaveContext context) 
	throws SaveFailedException;

    /** 
     * Undo all work done thus far using the given context.
     *
     * @param  context  Context to undo work against.
     */
    void undo(Transaction tx)
	throws UndoFailedException;
}

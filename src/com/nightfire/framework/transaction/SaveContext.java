/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction;

import java.util.Vector;

import com.nightfire.framework.util.Debug;


/**
 * Interface supporting transactional contexts.
 */
public interface SaveContext
{

    /**
     * Commits the transaction
     *
     * @exception CommitFailedException  Thrown if commit fails.
     */
    void commit()
	throws CommitFailedException;

    /**
     * Rolls-back the transaction
     *
     * @exception RollbackFailedException  Thrown if rollback fails.
     */
    void rollback()
	throws RollbackFailedException;
}

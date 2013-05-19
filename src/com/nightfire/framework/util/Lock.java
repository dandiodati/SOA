/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.framework.util;


/**
 * Represents a lock that can by used to synchronize a section of code.
 *
 * @author Dan Diodati
 */
public interface Lock
{
    /**
     * obtains or starts the lock
     */
    public void acquire() throws InterruptedException;


     /**
     * obtains or starts the lock with a wait time of msecs milliseconds.
     * If it takes longer that msecs to obtain the lock an InterruptedException is thrown.
     */
    public void acquire(long msecs) throws InterruptedException;

    /**
     * releases the lock.
     */
    public void release();
}


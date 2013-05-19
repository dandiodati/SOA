/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.framework.util;


/**
 * Interface implemented by all classes providing 
 * information about the currently-executing process 
 * to the diagnostic logging API.
 */
public interface ProcessInfo
{
    /**
     * Describes the current JVM process from an
     * operating system standpoint. (Example: process-id).
     * (NOTE: The actual contents of this string are
     * OS-specific.)
     * 
     * @return  A string containing OS-specific information
     *          describing the current process.
     */
    public String describe ( );
}

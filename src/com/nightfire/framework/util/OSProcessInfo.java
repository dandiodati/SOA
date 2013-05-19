/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.framework.util;


/**
 * Provides OS-specific process information to the diagnostic logging API.
 */
public class OSProcessInfo implements ProcessInfo
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
    public native String describe ( );

    /*
     * This loads the native library which implements the appropriate system
     * call.
     */
    static
    {
        System.loadLibrary("procinfo");
    }
}

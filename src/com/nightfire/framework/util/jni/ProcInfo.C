/*
 * Copyright (c) 2001 Nightfire Software, Inc.
 * All rights reserved.
 */

#include <OSProcessInfo.h>

#include <stdio.h>

#ifndef WIN32
#include <unistd.h>
#else
#include <process.h>

// why, if it implements a standard interface it can't use a standard name
// and header file, I'll never understand
#define getpid _getpid
typedef int pid_t;
#endif

// max size for our buffer
static const int MAX_LOG_BUFFER = 256;

//****************************************************************************
// FUNCTION:    ...OSProcessInfo_describe
//
// PURPOSE:    Describes the current JVM process from an operating system
//             standpoint for the diagnostic logging API
//
// PARAMETERS:  env             - Java environment
//              thisObj         - The calling object (this in Java)
//
// RETURNS:     A string containing process information.  The returned
//              string looks like:
//              Process Id: <pid>
//              Where <pid> is the process id of the calling process.

JNIEXPORT jstring JNICALL OSProcessInfo_describe (JNIEnv * env,
                                                  jobject thisObj)
{
    // get the process id
    pid_t procId = getpid();

    // set up a string to print to
    char buff[MAX_LOG_BUFFER];

    // format the string to return
    sprintf(buff, "Process Id: %d", procId);

    // return a new Java string
    return env->NewStringUTF(buff);
}

/*
 * Copyright(c) 2000 Nightfire Software Inc.
 * All rights reserved.
 */

// multiple inclusion protection
#ifndef _NF_NFLOGGER_H_
#define _NF_NFLOGGER_H_

#include <jni.h>
#ifndef WIN32
#include <pthread.h>
#else
#include <windows.h>
#endif
#include <stdarg.h>

#include "NFLoggerLevel.h"

static const char* NF_LOG_FILE_PROP_NAME   = "LOG_FILE";
static const char* NF_LOG_LEVEL_PROP_NAME  = "DEBUG_LOG_LEVELS";
static const char* NF_MAX_WRITES_PROP_NAME = "MAX_DEBUG_WRITES";
static const char* NF_LOG_SUFFIX           = ".native";

static const int   NF_AVG_LINE_LENGTH      = 80;

// macros to pass to the log method
#define NFJavaLogInfo(env) env, __FILE__, __LINE__
#define NFLogInfo() NULL, __FILE__, __LINE__

// WIN32 compatibility macros
#ifdef WIN32
#define vsnprintf _vsnprintf
#define localtime_r(a,b) memcpy((b), localtime((a)), sizeof(struct tm))
#define strtok_r(a,b,c) strtok((a),(b))
#define strcasecmp _stricmp
#endif

class NFLogger
{
public:

    // makes a log entry
    static void log(JNIEnv* env, const char* file, int line,
                    const NFLogLevel& level, const char* msg, ...);

private:
    // indicates whether or not logging is enabled
    static inline int loggingEnabled(JNIEnv* env, const NFLogLevel& level);

    // formats a printf-style string into a new buffer
    static char* format(const char* format, va_list vargs);
    
    // formats a time stamp
    static void getTS(char* ts);

    // initializes state
    static void initialize(JNIEnv* env);

    // the equivalent of java.lang.System.getProperty
    static char* getJavaProperty(JNIEnv* env, const char* key, char* val,
                                 unsigned max);

    // prepares the global table that maps Java to C levels
    static void prepareLevelTable();

    // determines which file to use for logging
    static char* whichFile(char* fileName, unsigned max);

    enum
    {
        LOGGING_UNINITIALIZED = 0,
        LOGGING_INITIALIZED   = 1,

        BUFFER_SIZE           = 1024,
        TIME_SIZE             = 21, //YY-MM-DD HH:MM:SS.ss
        FILE_INFO_SIZE        = 32
    };

    // initialized flag
    static int initialized;

    // masks for determining logging levels that are enabled
    static unsigned classMask;
    static unsigned levelMask[NF_LOG_CLASS_COUNT];

    // Java to C log level mapping table
    static NFLogLevel allLogLevels[NF_MAX_LOG_LEVEL + 1];

    // log file information
    static char logFileBaseName[BUFFER_SIZE];
    static int  writesLimited;

    // locks
#ifndef WIN32
    static pthread_mutex_t initLock;
    static pthread_mutex_t formatLock;
#else
    static HANDLE initLock;
    static HANDLE formatLock;

public:
    static void initLocks();
#endif
};

//****************************************************************************
// METHOD:      loggingEnabled
//
// PURPOSE:     Indicates whether or not logging is turned on
//
// RETURNS:     non-zero if logging is enabled

inline int NFLogger::loggingEnabled(JNIEnv* env, const NFLogLevel& level)
{
    if (initialized == LOGGING_UNINITIALIZED)
        initialize(env);

    return ( (!level.logLevel) ||
             ((level.logClass & classMask) &&
              (level.logLevel & levelMask[level.logClass])) );
}

#endif // _NF_NFLOGGER_H_

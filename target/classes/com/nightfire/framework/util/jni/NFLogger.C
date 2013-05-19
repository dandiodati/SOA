/*
 * Copyright (c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

#include <NFLogger.h>

#include <sys/types.h>
#ifndef WIN32
#include <sys/time.h>
#include <unistd.h>
#else
#include <winsock.h>
#endif
#include <sys/stat.h>
#include <time.h>
#include <string.h>
#include <stdlib.h>

//****************************************************************************
// static members

int NFLogger::initialized = NFLogger::LOGGING_UNINITIALIZED;

unsigned NFLogger::classMask = 0;
unsigned NFLogger::levelMask[NF_LOG_CLASS_COUNT];

NFLogLevel NFLogger::allLogLevels[NF_MAX_LOG_LEVEL + 1];

char NFLogger::logFileBaseName[BUFFER_SIZE];
int  NFLogger::writesLimited = 0;

#ifndef WIN32
pthread_mutex_t NFLogger::initLock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t NFLogger::formatLock = PTHREAD_MUTEX_INITIALIZER;
#else
HANDLE NFLogger::initLock = 0;
HANDLE NFLogger::formatLock = 0;
#endif

//****************************************************************************
// METHOD:      log
//
// PURPOSE:     Generates a log entry if logging is enabled
//
// PARAMETERS:  env             - Java environment
//              file            - The file that called the log routine
//              line            - The line that called the log routine
//              level           - The logging level to use
//              msg             - printf-style formatted string to log
//              ...             - printf-style parameters

void NFLogger::log(JNIEnv* env, const char* file, int line,
                   const NFLogLevel& level, const char* msg, ...)
{
    // only proceed if logging is turned on
    if (!loggingEnabled(env, level))
        return;

    // open up our arg list
    va_list vargs;
    va_start(vargs, msg);

    // get a time stamp
    char ts[TIME_SIZE];
    getTS(ts);

    // create a file description
    char fileInfo[FILE_INFO_SIZE];
    sprintf(fileInfo, "%-.20s:%d", file, line);

    // format the user's string
    char* logMsg = format(msg, vargs);

    // determine which file to use
    char logFileName[BUFFER_SIZE];
    FILE* logFile = 0;

    if (whichFile(logFileName, BUFFER_SIZE))
    {
        // open our file for append
        logFile = fopen(logFileName, "a");
    }

    // only produce a log entry if the file was successfully opened
    if (logFile)
    {
        // write to the file
        fprintf(logFile, "[%20.20s] [%03d] [%-25.25s] %s\n", ts,
                level.javaLevel, fileInfo, logMsg);
        fflush(logFile);

        // close the file
        fclose(logFile);
    }

    // delete the allocated buffer
    delete [] logMsg;

    // close the list
    va_end(vargs);
}

//****************************************************************************
// METHOD:      format
//
// PURPOSE:     Formats a printf-style string into a newly allocated buffer.
//              The buffer must be de-allocated by the caller
//
// PARAMETERS:  msg             - printf-style formatted string to format
//              vargs           - printf-style parameters
//
// RETURNS:     A newly allocated buffer with the formatted string

char* NFLogger::format(const char* format, va_list vargs)
{
    // vsnprintf is not thread-safe
#ifndef WIN32
    pthread_mutex_lock(&formatLock);
#else
    WaitForSingleObject(formatLock, INFINITE);
#endif

    // allocate a new string
    char* buff = new char[BUFFER_SIZE];

    // format it
    int size = vsnprintf(buff, BUFFER_SIZE, format, vargs);

    // check to see if the whole thing was formatted
    if (size > BUFFER_SIZE)
    {
        delete [] buff;
        buff = new char[size];

        vsnprintf(buff, size, format, vargs);
    }

#ifndef WIN32
    pthread_mutex_unlock(&formatLock);
#else
    ReleaseMutex(formatLock);
#endif

    // return the formatted string
    return buff;
}

//****************************************************************************
// METHOD:      getTS
//
// PURPOSE:     Formats a time stamp for use in a log entry as:
//              YY-MM-DD HH:MM:SS.ss
//
// PARAMETERS:  ts              - output buffer for the time stamp, expected
//                                to be TIME_SIZE bytes

void NFLogger::getTS(char* ts)
{
    // get the current time
    struct timeval t;
#ifndef WIN32
    gettimeofday(&t, NULL);
#else
    t.tv_usec = 0;
    t.tv_sec = time(NULL);
#endif

    // make it local
    struct tm localt;
    localtime_r(&(t.tv_sec), &localt);

    char temp[TIME_SIZE];

    // format the time string
    strftime(temp, TIME_SIZE, "%y-%m-%d %H:%M:%S", &localt);

    // add the milliseconds
    sprintf(ts, "%s.%02.2d", temp, (t.tv_usec / 10000));
}

//****************************************************************************
// METHOD:      initialize
//
// PURPOSE:     Initializes logging state
//
// PARAMETERS:  env             - Java environment to retieve configuration
//                                information from

void NFLogger::initialize(JNIEnv* env)
{
    // acquire the lock
#ifndef WIN32
    pthread_mutex_lock(&initLock);
#else
    WaitForSingleObject(initLock, INFINITE);
#endif

    // only proceed if we've not be initialized
    if (initialized == LOGGING_UNINITIALIZED)
    {
        // mark this as initialized
        initialized = LOGGING_INITIALIZED;

        // default to no logging
        classMask = 0;
        memset(levelMask, 0, sizeof(unsigned) * NF_LOG_CLASS_COUNT);
        prepareLevelTable();
        logFileBaseName[0] = 0;
        writesLimited = 0;

        // get the log levels
        char levels[BUFFER_SIZE];
        if (getJavaProperty(env, NF_LOG_LEVEL_PROP_NAME, levels, BUFFER_SIZE))
        {
            // handle ALL separately
            if (!strcasecmp(levels, "ALL"))
            {
                classMask = 0xFFFFFFFF;
                memset(levelMask, 0xFF, sizeof(unsigned) * NF_LOG_CLASS_COUNT);
            }
            else
            {
                char* tok;
                char* lasts;

                // for each token
                tok = strtok_r(levels, " ", &lasts);
                while (tok)
                {
                    // get the integer of the token
                    int thisLevel = atoi(tok);

                    // set the flags
                    classMask |= allLogLevels[thisLevel].logClass;
                    levelMask[allLogLevels[thisLevel].logClass] |=
                        allLogLevels[thisLevel].logLevel;

                    // move to the next token
                    tok = strtok_r(0, " ", &lasts);
                }
            }
        }

        // get the file name to log to
        getJavaProperty(env, NF_LOG_FILE_PROP_NAME, logFileBaseName,
                        BUFFER_SIZE);

        // determine whether or not files are rolled
        char dummy[BUFFER_SIZE];
        if (getJavaProperty(env, NF_MAX_WRITES_PROP_NAME, dummy, BUFFER_SIZE))
            writesLimited = atoi(dummy);
        else
            writesLimited = 0;
    }

    // release the lock
#ifndef WIN32
    pthread_mutex_unlock(&initLock);
#else
    ReleaseMutex(initLock);
#endif
}

//****************************************************************************
// METHOD:      getJavaProperty
//
// PURPOSE:     The equivalent of java.lang.System.getProperty
//
// PARAMETERS:  env             - Java environment to use for retrieving the
//                                property
//              key             - The property to retieve
//              val             - Destination buffer for the property value
//              max             - The maximum number of characters (including
//                                the null terminator) to copy to val
//
// RETURNS:     val, if key was found, otherwise null

char* NFLogger::getJavaProperty(JNIEnv* env, const char* key, char* val,
                                unsigned max)
{
    // Java stuff
    static JNIEnv* lastEnv = NULL;
    static jclass sysClass = NULL;
    static jmethodID propMeth = NULL;

    // return null if we fail
    char* retVal = NULL;

    // reset for a new env
    if (lastEnv != env)
    {
        lastEnv = env;
        sysClass = NULL;
        propMeth = NULL;
    }

    // get the System class
    if ((env) && (!sysClass))
        sysClass = env->FindClass("java/lang/System");

    if ((sysClass) && (!propMeth))
        // get the method to call
        propMeth = env->GetStaticMethodID(sysClass, "getProperty",
            "(Ljava/lang/String;)Ljava/lang/String;");

    if (propMeth)
    {
        jobject jVal = NULL;
        const char* str = NULL;
        jsize len = 0;

        // call the method
        jVal = env->CallStaticObjectMethod(sysClass, propMeth,
                                           env->NewStringUTF(key));

        // check the return value
        if (jVal)
        {
            // get the string
            str = env->GetStringUTFChars((jstring)jVal, NULL);
            // get the length
            len = env->GetStringUTFLength((jstring)jVal);
        }

        if (str)
        {
            // copy the string
            len = (len + 1 > max) ? (max - 1) : len;
            memcpy(val, str, len);

            // terminate the string
            val[len] = 0;

            // return a copy of val
            retVal = val;

            // release the java version
            env->ReleaseStringUTFChars((jstring)jVal, str);
        }
    }

    return retVal;
}

//****************************************************************************
// METHOD:      whichFile
//
// PURPOSE:     Determines which file to log to
//
// PARAMETERS:  env             - Java environment to use for retrieving the
//                                properties
//              fileName        - Output parameter for the name of the file
//              max             - The maximum number of characters (including
//                                the null terminator) to copy to fileName
//
// RETURNS:     fileName, if a file was found, otherwise null, indicating
//              stdout should be used

char* NFLogger::whichFile(char* fileName, unsigned max)
{
    // start with our base name
    int len = strlen(logFileBaseName);
    if (len == 0)
        return 0;

    // allow room for the suffix
    int suffLen = strlen(NF_LOG_SUFFIX);

    // copy that to the destination
    len = (len >= (max - (2 + suffLen))) ? (max - (2 + suffLen)) : len;
    memcpy(fileName, logFileBaseName, len);
    fileName[len] = 0;

    // add the suffix
    strcat(fileName, NF_LOG_SUFFIX);

    // see if we are alternating files
    if (writesLimited)
    {
        // now get statistics for file 1 and file 2
        struct stat f1;
        struct stat f2;
        struct stat* fCheck;
        int r1, r2;
        int digitPos = strlen(fileName);

        // for file 1
        strcat(fileName, "1");
        r1 = stat(fileName, &f1);
       
        // for file 2
        fileName[digitPos] = '2';
        r2 = stat(fileName, &f2);
        fCheck = &f2;

        // if file 2 statistics were not obtained, check file 1
        if (r2 == -1)
        {
            fileName[digitPos] = '1';
            fCheck = &f1;
        }
        // if we have statistics for both, compare them
        else if (r1 != -1)
        {
            // if file 1 was more recently changed, switch to it
            if (f1.st_mtime > f2.st_mtime)
            {
                fileName[digitPos] = '1';
                fCheck = &f1;
            }
        }
        // anything else, leave it as file 2

        // now check that file to see if it has exceeded our maximum size
        if (fCheck->st_size > (writesLimited * NF_AVG_LINE_LENGTH))
        {
            // We don't track actual number of writes since a JNI library may
            // not be instantiated for the same period of time as the SPI

            // switch to the next file
            if (fileName[digitPos] == '1')
                fileName[digitPos] = '2';
            else
                fileName[digitPos] = '1';

            // truncate it
#ifdef WIN32
            remove(fileName);
#else
            truncate(fileName, 0);
#endif
        }
    }

    return fileName;
}

#ifdef WIN32
//****************************************************************************
// WIN32 Library Initialization

BOOL WINAPI DllMain(HINSTANCE hInstDll, DWORD fdwReason, LPVOID lpReserved)
{
    if (fdwReason == DLL_PROCESS_ATTACH)
        // initialize mutexes
        NFLogger::initLocks();

    return TRUE;
}

//****************************************************************************
// create mutexes for WIN32

void NFLogger::initLocks()
{
    initLock = CreateMutex(NULL, FALSE, NULL);
    formatLock = CreateMutex(NULL, FALSE, NULL);
}
#endif

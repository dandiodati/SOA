
// ----------------------------------------------------------------------------
//
//  This document contains material which is the proprietary property of and
//  confidential to bTrade.com Incorporated.
//  Disclosure outside bTrade.com is prohibited except by license agreement
//  or other confidentiality agreement.
//
//  Copyright (c) 1998 by bTrade.com Incorporated
//  All Rights Reserved
//
// ----------------------------------------------------------------------------
//  Description: Header file for mutex macros and function prototypes
//
// ----------------------------------------------------------------------------
//          MAINTENANCE HISTORY
//
// DATE     BY  BUG NO. DESCRIPTION
// -------- --- ------- -------------------------------------------------------
// 19980321 PAA         Initial version
// ----------------------------------------------------------------------------
#if !defined(__EAMUTEX_HPP__)
#define __EAMUTEX_HPP__

#include "eaconst.h"    // Defines EA_UNIX, bool, true, and other useful stuff

#if defined(EA_WIN32)
#include <windows.h>
#elif defined(EA_MVS) || ( defined(EA_UNIX) && !defined(EA_SCOUNIX) )
#include <pthread.h>    // Must be 1st include file for any source using threads
#endif

#include "cf.hpp"

// usleep is missing from unistd.h!
#if defined(EA_SOLARIS251) || defined(EA_DECUNIX)
extern "C" {
int         usleep(unsigned int useconds);
unsigned    sleep(unsigned useconds);
}
#endif

// ----------------------------------------------------------------------------
//  Define platform-independent mutex locking constants
// ----------------------------------------------------------------------------
#if defined(EA_WIN32) || defined(EA_WIN16)
#define EA_LOCKED                   0
#define EA_LOCK_ABANDONED           1
#define EA_LOCK_ERROR               2
#define MAX_MUTEX_LOCK_RETRY        5
#elif defined(EA_UNIX)
#define EA_LOCKED                   0
#define EA_LOCK_ABANDONED           EBUSY           // Not relevant to Solaris
#define EA_LOCK_ERROR               EBUSY
#define MAX_MUTEX_LOCK_RETRY        5
#elif defined(EA_MVS) || defined(EA_OS400)
#define EA_LOCKED                   0
#define EA_LOCK_ABANDONED           1
#define EA_LOCK_ERROR               2
#endif

// ----------------------------------------------------------------------------
//  Prototypes of functions used to manage mutexes in a platform-independent way
// ----------------------------------------------------------------------------
void*   createMutex(cf_t*, bool);
void*   deleteMutex(void* mutex, cf_t*, bool);
int     lockMutex(void* mutex, int timeoutVal);
void    unlockMutex(void* mutex, cf_t*, bool);

#endif  // if !defined(__EAMUTEX_HPP__)

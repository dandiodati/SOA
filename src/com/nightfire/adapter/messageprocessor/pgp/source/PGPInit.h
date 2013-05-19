/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

// protect against multiple inclusions
#ifndef _NF_PGPINIT_H_
#define _NF_PGPINIT_H_

#include <pthread.h>

#include "PGPException.h"

//****************************************************************************
// CLASS:       PGPInit
//
// PURPOSE:     Optimizes PGP intializiation and cleanup, as well as context
//              references

class PGPInit
{
public:
    // initialize the library, if necessary, and return a context to use
    static PGPContextRef getContext() throw(PGPException);

    // free a context, and clean up after the library, if appropriate
    static void freeContext(PGPContextRef ctx);

private:
    // this class may not be instantiated
    PGPInit();
    ~PGPInit();

    // privately called to get a context
    static int newContext(PGPContextRef& ctx);

    // privately called to return a context
    static void returnContext(PGPContextRef ctx);

    // lock for reference counting
    static pthread_mutex_t referenceLock;

    // reference count
    static unsigned references;

    // constants
    enum
    {
        CTX_BUFFER_SIZE = 10
    };

    // stack for buffered contexts
    static PGPContextRef ctxBuffer[CTX_BUFFER_SIZE];

    // stack pointer
    static int si;
};

#endif // __NF_PGPINIT_H_

/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

#include "PGPInit.h"

#include <pgpUtilities.h>

//*****************************************************************************
// static members

pthread_mutex_t PGPInit::referenceLock = PTHREAD_MUTEX_INITIALIZER;
unsigned PGPInit::references = 0;
PGPContextRef PGPInit::ctxBuffer[CTX_BUFFER_SIZE];
int PGPInit::si = 0;

//*****************************************************************************
// METHOD:      getContext
//
// PURPOSE:     Initialize the library, if necessary, and return a context to
//              use
//
// RETURNS:     A PGP context to use with this call

PGPContextRef PGPInit::getContext() throw(PGPException)
{
    PGPContextRef ctx;

    // lock out other perspective initializers
    if (pthread_mutex_lock(&referenceLock) == 0)
    {
        // see if we need to initialize
        if (!references)
            if ( IsPGPError(PGPsdkInit()) )
            {
                pthread_mutex_unlock(&referenceLock);
                throw PGPException("Could not initialize PGP library.");
            }

        // increment our reference count
        references++;

        // get a context
        if (!newContext(ctx))
        {
            pthread_mutex_unlock(&referenceLock);
            throw PGPException("Could not create a PGP context.");
        }

        // release our lock
        pthread_mutex_unlock(&referenceLock);
    }

    return ctx;
}

//*****************************************************************************
// METHOD:      freeContext
//
// PURPOSE:     Free a context, and clean up after the library, if appropriate
//
// PARAMETERS:  ctx             - The context to free

void PGPInit::freeContext(PGPContextRef ctx)
{
    // get a lock so we can check our reference count
    if (pthread_mutex_lock(&referenceLock) == 0)
    {
        // return our context
        returnContext(ctx);

        // decrease our reference count
        references--;

        // if we have no more references, shutdown the lib
        if (!references)
            PGPsdkCleanup();

        // release our lock
        pthread_mutex_unlock(&referenceLock);
    }
}

//*****************************************************************************
// METHOD:      newContext
//
// PURPOSE:     Handles acquiring new contexts
//
// PARAMETERS:  ctx             - Output parameter for the new context
//
// RETURNS:     A non-zero value if successful.

int PGPInit::newContext(PGPContextRef& ctx)
{
    // see if we have any pre-allocated contexts to hand out
    if (si)
    {
        // si point to the index just beyond our current position
        // so, return the one before it and decrease our pointer
        si--;
        ctx = ctxBuffer[si];
    }
    else // we're all out, so get a new one
    {
        // get a new PGP context
        if ( IsPGPError(PGPNewContext(kPGPsdkAPIVersion, &ctx)) )
            return 0;
    }

    return 1;
}

//*****************************************************************************
// METHOD:      returnContext
//
// PURPOSE:     Handles freeing acquired contexts
//
// PARAMETERS:  ctx             - The context to return

void PGPInit::returnContext(PGPContextRef ctx)
{
    // first make sure we're not about to go away
    if (references > 1)
    {
        // make sure we have a valid reference
        if (PGPContextRefIsValid(ctx))
        {
            // see if we have storage space left
            if (si < CTX_BUFFER_SIZE)
            {
                // store it and increment our count
                ctxBuffer[si] = ctx;
                si++;
            }
            else
                // we're out of storage space, so release it
                PGPFreeContext(ctx);
        }
    }
    else
    {
        // we're about to go away, so don't store anything

        // return our context
        if (PGPContextRefIsValid(ctx))
            PGPFreeContext(ctx);

        // return any others
        while (si)
        {
            si--;
            PGPFreeContext(ctxBuffer[si]);
        }
    }
}

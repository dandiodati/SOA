/*
 * Copyright(c) 2000 Nightfire Software Inc.
 * All rights reserved.
 */

// multiple inclusion protection
#ifndef _NF_PGPBASE_H_
#define _NF_PGPBASE_H_

#include "PGPBaseJava.h"
#include "PGPException.h"

#include "pgpOptionList.h"

#define NF_PUBLIC_KEY_NAME  "/pubring.pkr"
#define NF_PRIVATE_KEY_NAME "/secring.skr"
#define NF_ENCRYPT_PREFIX   "nfenc"
#define NF_DECRYPT_PREFIX   "nfdec"

void openRingPair(PGPContextRef ctx, const char* keyDir, PGPKeySetRef& keyRef)
    throw (PGPException);
PGPError decodeHandler(PGPContextRef context, PGPEvent* event,
                       PGPUserValue userValue);

#endif // _NF_PGPBASE_H_

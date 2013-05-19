/*
 * Copyright (c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

// multiple inclusion protection
#ifndef _NF_PGPEXCEPTION_H_
#define _NF_PGPEXCEPTION_H_

#include <string.h>
#include <pgpErrors.h>

class PGPException
{
public:
    // constructor
    PGPException(const char* msg);

    // constructor with a PGP error
    PGPException(PGPError err, const char* msg);

    // copy constructor
    PGPException(const PGPException& ex);

    // destructor
    ~PGPException();

    // assignment operator
    PGPException& operator==(const PGPException& ex);

    // accessor for the message
    // WARNING: The string is good only as long as this class persists
    const char* getMsg() const { return msg; }

protected:
    char* msg;

    // constants
    enum
    {
        NF_PGP_ERROR_BUFFER = 1024
    };
};

#endif // _NF_PGPEXCEPTION_H_

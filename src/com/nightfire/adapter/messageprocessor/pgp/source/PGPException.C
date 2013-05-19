/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

#include "PGPException.h"

//*****************************************************************************
// METHOD:      PGPException(const char*)
//
// PURPOSE:     Constructor
//
// PARAMETERS:  msg             - The message explaining the exception

PGPException::PGPException(const char* msg)
{
    // allocate a string
    this->msg = new char[strlen(msg) + 1];

    // copy the message
    if (this->msg)
        strcpy(this->msg, msg);
}

//*****************************************************************************
// METHOD:      PGPException(PGPError, const char*)
//
// PURPOSE:     Constructor for a PGP error
//
// PARAMETERS:  err             - The PGP error code
//              msg             - The message explaining the exception

PGPException::PGPException(PGPError err, const char* msg)
{
    // create a buffer to get the exception string
    char errBuf[NF_PGP_ERROR_BUFFER];

    // get the error string
    PGPGetErrorString(err, NF_PGP_ERROR_BUFFER, errBuf);

    // now allocate actual storage
    this->msg = new char[strlen(msg) + strlen(errBuf) + 2];
    
    // copy the message
    if (this->msg)
    {
        strcpy(this->msg, msg);
        strcat(this->msg, " ");
        strcat(this->msg, errBuf);
    }
}

//*****************************************************************************
// METHOD:      PGPException(const PGPException&)
//
// PURPOSE:     Copy constructor
//
// PARAMETERS:  ex              - The exception to copy

PGPException::PGPException(const PGPException& ex)
{
    // allocate a string
    msg = new char[strlen(ex.msg) + 1];
    
    // copy the message
    if (msg)
        strcpy(msg, ex.msg);
}

//*****************************************************************************
// METHOD:      ~PGPException
//
// PURPOSE:     Destructor

PGPException::~PGPException()
{
    // clean up any allocated memory
    if(msg)
        delete [] msg;
}

//*****************************************************************************
// METHOD:      operator==
//
// PURPOSE:     Assignment operator
//
// PARAMETERS:  ex              - The exception to assign to this one
//
// RETURNS:     A reference to self

PGPException& PGPException::operator==(const PGPException& ex)
{
    // free up our old message
    if (msg)
        delete [] msg;

    // assign the new one
    msg = new char[strlen(ex.msg) + 1];
    if (msg)
        strcpy(msg, ex.msg);
}

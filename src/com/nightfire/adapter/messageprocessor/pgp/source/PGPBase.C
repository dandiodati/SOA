/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

#include "PGPBase.h"

#include <limits.h>
#include <stdio.h>

#include <pgpOptionList.h>
#include <pgpEncode.h>
#include <pgpKeys.h>
#include <pgpUtilities.h>

#include "PGPInit.h"
#include "NFLogger.h"

//*****************************************************************************
// PACKAGE:     com.nightfire.spi.common.adapter.pgp

//*****************************************************************************
// FUNCTION:    PGPBase.decrypt
//
// PURPOSE:     Decrypts a PGP-encripted array of bytes
//
// PARAMETERS:  env             - The Java environment, this is our access to
//                                the JVM
//              jThis           - The object that called us (in a Java
//                                method, this is the equivalent of this)
//              message         - A byte array containing the bytes to
//                                decrypt
//              key             - The passphrase for our private key
//              ringDir         - Path to the "ring" directory, which is where
//                                the keys our stored

JNIEXPORT jbyteArray JNICALL Java_PGPBase_decrypt (
    JNIEnv* env, jobject jThis, jbyteArray message, jstring key,
    jstring ringDir)
{
    PGPContextRef ctx   = NULL;
    PGPKeySetRef keyRef = NULL;
    PGPSize outSize     = 0;
    PGPByte* outBuf     = 0;
    jbyteArray result   = NULL;
    const char* msg     = "Decoding failed.";

    // get our java data in some kind of format we can use
    PGPByte*    source = (PGPByte*) env->GetByteArrayElements(message, NULL);
    unsigned      size = env->GetArrayLength(message);
    const char*   pass = env->GetStringUTFChars(key, NULL);
    const char* keyDir = env->GetStringUTFChars(ringDir, NULL);

    NFLogger::log(NFJavaLogInfo(env), NF_SYSTEM_CONFIG,
                  "Encrypted buffer size: %u", size);
    NFLogger::log(NFJavaLogInfo(env), NF_SYSTEM_CONFIG,
                  "Passphrase length:     %d", strlen(pass));
    NFLogger::log(NFJavaLogInfo(env), NF_SYSTEM_CONFIG,
                  "Key ring directory:    %s", keyDir);

    try
    {
        // now were all set, so initialize the lib
        ctx = PGPInit::getContext();

        // open up the key rings
        openRingPair(ctx, keyDir, keyRef);

        NFLogger::log(NFJavaLogInfo(env), NF_MSG_STATUS,
                      "Beginning decryption.");

        // now decrypt it
        PGPError err = PGPDecode(ctx,
                                 PGPOInputBuffer(ctx, source, size),
                                 PGPOAllocatedOutputBuffer(ctx,
                                                           (void**)&outBuf,
                                                           ULONG_MAX,
                                                           &outSize),
                                 PGPOKeySetRef(ctx, keyRef),
                                 PGPOPassphrase(ctx, pass),
                                 PGPOEventHandler(ctx, decodeHandler, &msg),
                                 PGPOLastOption(ctx));

        NFLogger::log(NFJavaLogInfo(env), NF_MSG_STATUS,
                      "PGPDecode() returned a value of %d", err);

        if (IsPGPError(err))
            throw PGPException(err, msg);

        // create a byte array to return
        if ( (result = env->NewByteArray(outSize)) == NULL)
            throw PGPException(
                "Could not create an array to store encyrption results");
    

        // populate our results
        env->SetByteArrayRegion(result, 0, (int)outSize, (jbyte*)outBuf);
    }
    catch(PGPException ex)
    {
        NFLogger::log(NFJavaLogInfo(env), NF_ALL_ERRORS, "ERROR: %s",
                      ex.getMsg());

        // construct an exception for java
        jclass exClass = env->FindClass(
            "com/nightfire/common/ProcessingException");
        if (!exClass)
            env->FindClass("java/langException");

        // throw the exception
        env->ThrowNew(exClass, ex.getMsg());

        // set our result to null
        result = NULL;
    }

    // PGP cleanup
    if (PGPKeySetRefIsValid(keyRef))
        PGPFreeKeySet(keyRef);
    if (outBuf)
        delete [] outBuf;
    PGPInit::freeContext(ctx);

    // clean up our java references
    env->ReleaseByteArrayElements(message, (jbyte*)source, JNI_ABORT);
    env->ReleaseStringUTFChars(key, pass);
    env->ReleaseStringUTFChars(ringDir, keyDir);

    return result;
}

//*****************************************************************************
// FUNCTION:    PGPBase.encrypt
//
// PURPOSE:     PGP-encrypts an array of bytes
//
// PARAMETERS:  env             - The Java environment, this is our access to
//                                the JVM
//              jThis           - The object that called us (in a Java
//                                method, this is the equivalent of this)
//              message         - A byte array containing the bytes to
//                                encrypt
//              key             - Identifies the public key to encrypt with
//              ringDir         - Path to the "ring" directory, which is where
//                                the keys our stored

JNIEXPORT jbyteArray JNICALL Java_PGPBase_encrypt (
    JNIEnv* env, jobject jThis, jbyteArray message, jstring key,
    jstring ringDir)
{
    PGPContextRef ctx     = NULL;
    PGPKeySetRef keyRef   = NULL;
    PGPFilterRef filter   = NULL;
    PGPKeySetRef keySet   = NULL;
    PGPByte* outBuf       = 0;
    PGPSize  outSize      = 0;
    jbyteArray result     = NULL;

    // get our data in some kind of format we can use
    PGPByte*    source = (PGPByte*) env->GetByteArrayElements(message, NULL);
    unsigned      size = env->GetArrayLength(message);
    const char*  keyId = env->GetStringUTFChars(key, NULL);
    const char* keyDir = env->GetStringUTFChars(ringDir, NULL);

    NFLogger::log(NFJavaLogInfo(env), NF_SYSTEM_CONFIG,
                  "Unencrypted buffer size: %u", size);
    NFLogger::log(NFJavaLogInfo(env), NF_SYSTEM_CONFIG,
                  "Public key id:           %s", keyId);
    NFLogger::log(NFJavaLogInfo(env), NF_SYSTEM_CONFIG,
                  "Key ring directory:      %s", keyDir);

    try
    {
        // now were all set, so initialize the lib
        ctx = PGPInit::getContext();

        // open up the key rings
        openRingPair(ctx, keyDir, keyRef);

        // create a filter for the key
        NFLogger::log(NFJavaLogInfo(env), NF_MSG_STATUS,
                      "Creating a key filter.");
        if ( IsPGPError(PGPNewUserIDStringFilter(ctx, keyId,
                                                 kPGPMatchSubString,
                                                 &filter)) )
            throw PGPException("Could not create a key filter.");

        // look for the key
        NFLogger::log(NFJavaLogInfo(env), NF_MSG_STATUS, "Locating key %s",
                      keyId);
        if ( IsPGPError(PGPFilterKeySet(keyRef, filter, &keySet)) )
            throw PGPException("Could not find the specified key.");

        // now encrypt it
        NFLogger::log(NFJavaLogInfo(env), NF_MSG_STATUS,
                      "Beginning encryption.");
        PGPError err = PGPEncode(ctx,
                                 PGPOEncryptToKeySet(ctx, keySet),
                                 PGPOInputBuffer(ctx, source, size),
                                 PGPOAllocatedOutputBuffer(ctx,
                                                           (void**)&outBuf,
                                                           ULONG_MAX,
                                                           &outSize),
                                 PGPOLastOption(ctx));
        NFLogger::log(NFJavaLogInfo(env), NF_MSG_STATUS,
                      "PGPEncode() returned %d.", err);

        if (IsPGPError(err))
            throw PGPException(err, "Encoding failed.");

        // create a byte array to return
        if ( (result = env->NewByteArray(outSize)) == NULL)
            throw PGPException(
                "Could not create an array to store encyrption results");
    

        // populate our results
        env->SetByteArrayRegion(result, 0, (int)outSize, (jbyte*)outBuf);
    }
    catch(PGPException ex)
    {
        NFLogger::log(NFJavaLogInfo(env), NF_ALL_ERRORS, "ERROR: %s",
                      ex.getMsg());

        // construct an exception for java
        jclass exClass = env->FindClass(
            "com/nightfire/common/ProcessingException");
        if (!exClass)
            env->FindClass("java/langException");

        // throw the exception
        env->ThrowNew(exClass, ex.getMsg());

        // set our result to null
        result = NULL;
    }

    // clean up allocations
    if (PGPKeySetRefIsValid(keyRef))
        PGPFreeKeySet(keyRef);
    if (PGPFilterRefIsValid(filter))
        PGPFreeFilter(filter);
    if (PGPKeySetRefIsValid(keySet))
        PGPFreeKeySet(keySet);
    if (outBuf)
        delete [] outBuf;

    // we're done now, so clean up the lib
    PGPInit::freeContext(ctx);

    // release the reference counts java has on our data
    env->ReleaseByteArrayElements(message, (jbyte*)source, JNI_ABORT);
    env->ReleaseStringUTFChars(key, keyId);
    env->ReleaseStringUTFChars(ringDir, keyDir);

    return result;
}

//****************************************************************************
// FUNCTION:    openRingPair
//
// PURPOSE:     Opens the public and private key files
//
// 
void openRingPair(PGPContextRef ctx, const char* keyDir, PGPKeySetRef& keyRef)
    throw (PGPException)
{
    char* pubFile = 0;
    char* priFile = 0;
    PGPFileSpecRef pubRef = NULL;
    PGPFileSpecRef priRef = NULL;
    keyRef = NULL;
    PGPError err;

    try
    {
        // alloate some memory to hold the strings
        pubFile = new char[strlen(keyDir) + strlen(NF_PUBLIC_KEY_NAME) + 1];
        priFile = new char[strlen(keyDir) + strlen(NF_PRIVATE_KEY_NAME) + 1];

        // construct the file names
        strcpy(pubFile, keyDir);
        strcat(pubFile, NF_PUBLIC_KEY_NAME);
        strcpy(priFile, keyDir);
        strcat(priFile, NF_PRIVATE_KEY_NAME);

        // create the file specs required by PGP
        NFLogger::log(NFLogInfo(), NF_IO_STATUS,
                      "Creating public key file spec for %s", pubFile);
        if ( IsPGPError(PGPNewFileSpecFromFullPath(ctx, pubFile, &pubRef)) )
            throw PGPException("Error opening public key file.");

        NFLogger::log(NFLogInfo(), NF_IO_STATUS,
                      "Creating private key file spec for %s", priFile);
        if ( IsPGPError(PGPNewFileSpecFromFullPath(ctx, priFile, &priRef)) )
            throw PGPException("Error opening private key file.");

        // open the key pair
        NFLogger::log(NFLogInfo(), NF_IO_STATUS, "Opening key pair.");
        err = PGPOpenKeyRingPair(ctx, kPGPKeyRingOpenFlags_Mutable, pubRef,
                                 priRef, &keyRef);
        if ( IsPGPError(err) )
            throw PGPException(err, "Error opening key pair: ");
    }
    catch(PGPException ex)
    {
        // clean up allocations
        if (pubFile)
            delete [] pubFile;
        if (priFile)
            delete [] priFile;

        // clean up file specs
        if (PGPFileSpecRefIsValid(pubRef))
            PGPFreeFileSpec(pubRef);
        if (PGPFileSpecRefIsValid(priRef))
            PGPFreeFileSpec(priRef);

        // propagate
        throw ex;
    }

    // clean up
    delete [] pubFile;
    delete [] priFile;
    PGPFreeFileSpec(pubRef);
    PGPFreeFileSpec(priRef);
}

//*****************************************************************************
// FUNCTION:    decodeHandler
//
// PURPOSE:     Event handler for decode operations
//
// PARAMETERS:  context         - The corresponding PGP context
//              event           - The event to handle
//              msg             - Expected to be of type (char**), this
//                                may be populated with an output message
//                                when an error is returned.

PGPError decodeHandler(PGPContextRef context, PGPEvent* event,
                       PGPUserValue msg)
{
    // handle passphrase events only
    if (event->type == kPGPEvent_PassphraseEvent)
    {
        NFLogger::log(NFLogInfo(), NF_MSG_STATUS,
                      "Received a Passphrase Event, failing.");

        // return a message indicating a bad pass phrase
        *((const char**)msg) = "Decoding failed due to an invalid pass phrase "
            "or key.  Please verify that PRIVATE_KEY_PASSCODE and "
            "RING_DIR_PATH are correct.";

        // we cannot prompt the user, so fail
        return kPGPError_UserAbort;
    }
    
    return kPGPError_NoErr;
}

/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

#include <jni.h>
/* Header for class com_nightfire_adapter_messageprocessor_pgp_PGPBase */

#ifndef _Included_com_nightfire_adapter_messageprocessor_pgp_PGPBase
#define _Included_com_nightfire_adapter_messageprocessor_pgp_PGPBase

/* While Java may like long function names, I do not like typing them */
#define Java_PGPBase_decrypt Java_com_nightfire_adapter_messageprocessor_pgp_PGPBase_native_1decrypt
#define Java_PGPBase_encrypt Java_com_nightfire_adapter_messageprocessor_pgp_PGPBase_native_1encrypt

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_nightfire_adapter_messageprocessor_pgp_PGPBase
 * Method:    native_encrypt
 * Signature: ([BLjava/lang/String;Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_PGPBase_encrypt
  (JNIEnv *, jobject, jbyteArray, jstring, jstring);

/*
 * Class:     com_nightfire_adapter_messageprocessor_pgp_PGPBase
 * Method:    native_decrypt
 * Signature: ([BLjava/lang/String;Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_PGPBase_decrypt
  (JNIEnv *, jobject, jbyteArray, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif


#include "XferReference.h"

extern "C"
#include "FtpSslLayerInterconnectJni.h"

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <float.h>

#define FTP_LAYER_LOG eaFtp.log
#define XFER_LAYER_LOG  eaXfer.log

#define THREADED true  //indicates if the Easy Access api should be thread safe.

#define ARRAYSIZE 17 // used for random array size

#define LOG_LEVEL -6 // indicates log level for Easy Access API

#define DIR_LIST_MODE "w"      // overwrites(w) or appends(a) to the dir list file.

#define DIR_LIST_CMD ""  // if you change this then it will affect
                               // directory listing stucture and filtering.
			       // NOTE: if you change this you will have to
			       // also change the JAVA code accordingly.

// constants for message callback
static const int EA_MSG_HIGH_PRIORITY   = 101;
static const int EA_MSG_NORMAL_PRIORITY = 102;
static const int EA_MSG_PROGRESS        = 105;
static const int EA_MSG_PUT             = 107;
static const int EA_MSG_CANCEL          = 108;

  // converts jbooleans to bools
  bool convertJboolean(jboolean b);

  // copys jstrings into char arrays to be used in C functions
  char* copyJavaString(JNIEnv *env, jstring str);

  // callback for messages
  void eaMsgHandler(xferContext_t* pXferContext, int msgType, char msgLevel,
                    char* msgText, UINT4 bytesTransferred, UINT4 totalBytes);

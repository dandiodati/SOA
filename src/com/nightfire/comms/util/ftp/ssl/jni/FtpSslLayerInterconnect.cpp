#ifndef __FTPSSLLAYERINTERCONNECTCPP_HEADER__
#define __FTPSSLLAYERINTERCONNECTCPP_HEADER__

#include "FtpSslLayerInterconnect.h"

#ifndef UNIX
#include <windows.h>
#else
#include <pthread.h>
#endif

// unused global "random array" for SMTP/POP3 transfers
unsigned char  pseudoRandomBytes[ARRAYSIZE];

/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    createFtpObj
 * Signature: (Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_createFtpObj
  (JNIEnv * env, jobject javaObj, jstring ediName, jint networkStyle, jstring runtimeDir, jstring tempDir, jstring privateFileName, jstring dirSep, jstring ftpLayerLog, jstring xferLayerLog, jboolean ftpLayerDebug)
  {

      jint rtn = 0;
      try {

        //initialize XferReference obj	
  
	XferReference* xf = new XferReference();


// PAA - lines added to help Sonali get it to run
	xf->xParams->errorDir = copyJavaString(env, tempDir);
	xf->xParams->localHostIp = "";					// N/A here
	xf->xParams->certImportDir = copyJavaString(env, tempDir);
	xf->xParams->mdnDir = copyJavaString(env, tempDir);
	xf->xParams->mailboxUserId = "";
	xf->xParams->mailboxPasswd = "";
	xf->xParams->myEmailAddress = " ";
	xf->xParams->gatewayMailbox = " ";				// N/A for this FTP example
	xf->xParams->as2Name = copyJavaString(env, ediName);
// PAA - End


	xf->xParams->networkName =(char*)" ";

	xf->xParams->networkStyle = (int) networkStyle;

        	
	xf->xParams->clientSlash = copyJavaString(env, dirSep); 

	

	xf->xParams->tmpDir      = copyJavaString(env, tempDir);
	xf->xParams->storeUnique = 0; //no use actual filename
	xf->xParams->siteDelay = 0;
   

        // used only for SMTP/POP-3 based network styles. So not used
	// here.	
//	xf->xParams->emailAddress = " ";
	xf->xParams->smtpIp = (char*) " ";
	xf->xParams->popIp = (char*) " ";
	xf->xParams->popUserId = (char*)" ";
	xf->xParams->popPasswd = (char*)" ";
	xf->xParams->randomArray = pseudoRandomBytes;

	xf->xParams->autoDelete = false;
  
	xf->xParams->ediName = copyJavaString(env, ediName);
	xf->xParams->runtimeDir = copyJavaString(env, runtimeDir);
	xf->xParams->sslRuntimeDir = copyJavaString(env, runtimeDir);
	xf->xParams->securityDir = (char*)" ";
	
	xf->xParams->privateFileName = copyJavaString(env, privateFileName); 

	// N/A dummy values
	xf->xParams->aliasFileName = (char*)"alias.tbl"; 
	xf->xParams->exportFileName = (char*)"export.fil"; 
	xf->xParams->localPrivateFileName = (char*)"priv.fil"; 
	xf->xParams->secFileName = (char*)"header.def"; 
	xf->xParams->approvalCode = (char*)" "; 

	xf->xParams->compressLogDir = (char*)"."; 
        //
    
	xf->xParams->multiThreaded = true;

        xf->xParams->commLogFileSpec = copyJavaString(env, ftpLayerLog);
        xf->xParams->xferLogFileSpec = copyJavaString(env, xferLayerLog);


// PAA -- where Sonali controls logging -- via the ftpLayerDebug argument
        // if debug mode is set then set log levels, otherwise turn it off.
	if (ftpLayerDebug == JNI_TRUE) {
	   xf->xParams->commLogLevel = LOG_LEVEL;
	   xf->xParams->xferLogLevel = LOG_LEVEL;
           // low level socket trace not needed
// PAA -- if you have trouble with SSL, turn this back on (generates a HUGE
//	log file though...
	   //xf->xParams->socketLogLevel = 4; 
	}
// PAA -- this turns off logging.
	else {
	   xf->xParams->socketLogLevel = 0;
	   xf->xParams->commLogLevel = 0;
	   xf->xParams->xferLogLevel = 0;
	}

	xf->xParams->responseLogFileSpec = (char*) 0;
	//xf->xParams->responseLogFileSpec = "response.log";

        // set a callback for messages from the API
	//logs to oad console so only turn on for debugging. doesn't seem to add much info any way
        //xf->pXferContext->userCallback = eaMsgHandler;

    
    // done in other file.
    //xf->xParams->eaVersionData = eaVer;

    
	rtn = xferCreate(xf->xParams);
    

	// if successful return a pointer to XferReference object, otherwise return 0
        if (rtn != 0) {
	   rtn = (jint) xf;
	}

     } catch (...) {
        rtn = 0;
     }


        return(rtn);


  }


/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    connect
 * Signature: (ILjava/lang/String;Ljava/lang/String;SZZ)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_connect
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring hostIp, jstring hostIp2, jshort controlPort, jboolean ssl, jboolean passive)
  {
     int rtn = 0;
     char* thostip = NULL;
     char* thostip2 = NULL; 

     try {

     // used to reference original XferReference created by createFtpObj
     XferReference* xf = (XferReference * ) ftpObj;


     thostip = copyJavaString(env,hostIp);
     thostip2 = copyJavaString(env,hostIp2);

     // fix for illegal memory access in easy access

     char hostipTmp[257], hostip2Tmp[257];

     memset(hostip2Tmp, 0, 257);
     memset(hostipTmp, 0, 257);

     //int size1 = sizeof (*thostip);
     //int size2 = sizeof (*thostip2);

     int size1 = strlen(thostip) + 1;
     int size2 = strlen(thostip2) + 1;

     // debugging
     //FILE* fd = fopen("blah", "a");
     //fprintf(fd,"FTPNATIVE: host string length is %d", size1);
     //fprintf(fd, "\n\n");
     //fclose(fd);

     memcpy(hostipTmp, thostip, size1);
     memcpy(hostip2Tmp, thostip2, size2);
     // end of fix

     rtn = xferConnect( xf->xParams->pXferContext,
                            hostipTmp,
                            hostip2Tmp,
                            (unsigned short) controlPort,
                            convertJboolean(ssl),
                            false, // diffe-Hellman never
                            convertJboolean(passive),
                            //false, // dataOverCommand never used
                            0,  //specify any low port
                            0   // specify any high port
			  );


     delete [] thostip; thostip = NULL;
     delete [] thostip2; thostip2 = NULL;

     } catch (...) {
        rtn = 0;
     }
     return ((jint) rtn);



  }

/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    login
 * Signature: (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_login
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring userId, jstring passwd, jstring newPasswd, jboolean proxyLogin)
  {

     int rtn = 0;
     char* tuserId = NULL;
     char* tpasswd = NULL;
     char* tnewPasswd = NULL;
     try {

     XferReference* xf = (XferReference * ) ftpObj;

     tuserId = copyJavaString(env, userId);
     tpasswd = copyJavaString(env, passwd);
     tnewPasswd = copyJavaString(env, newPasswd);

     rtn = xferLogin( xf->xParams->pXferContext,
                          tuserId,
        (char*)"", //optional login account
			  tpasswd,
			  tnewPasswd,
			  convertJboolean(proxyLogin)

                        );

     delete [] tuserId; tuserId = NULL;
     delete [] tpasswd; tpasswd = NULL;
     delete  [] tnewPasswd; tnewPasswd = NULL;

     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);


  }


/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    dirList
 * Signature: (ILjava/lang/String;)I
 * 
 * @param dirFile the file to temporarily store the server dir listing.
 *        NOTE: this has to be unique between threads so that they
 *              don't overwrite each others files or the call to this method
 *              has to be synchronized.
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_dirList
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring dirFile)
  {
     int rtn = 0;
     char* tdirFile = NULL;
     try {

     XferReference* xf = (XferReference * ) ftpObj;



     tdirFile =   copyJavaString(env, dirFile);


     rtn = xferList( xf->xParams->pXferContext,
                         tdirFile,
			 (char*)DIR_LIST_MODE,
			 (char*)DIR_LIST_CMD,
			 0,
			 (char*)" ",
			 1,
			 (char*)" "
                         );


     delete [] tdirFile, tdirFile = NULL;

     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);

  }


/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    cdForGet
 * Signature: (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_cdForGet
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring userId, jstring userClass, jboolean edi)
  {
     int rtn = 0;
     XferReference* xf = NULL;
     try {

     xf = (XferReference * ) ftpObj;

     xf->transfer->receive.userClass = copyJavaString(env, userClass);
     xf->transfer->receiveParms.edi  = convertJboolean(edi);

      //if edi mode is false then don't send user id

     if (edi == JNI_FALSE) {
        xf->transfer->receive.userId = (char*)"";
     }
     else {
        xf->transfer->receive.userId = copyJavaString(env, userId);
     }


     rtn = xferCdForGet( xf->xParams->pXferContext,
                             xf->transfer
                         );


     delete [] xf->transfer->receive.userClass;
     xf->transfer->receive.userClass = NULL;
     if (edi != JNI_FALSE)
         delete [] xf->transfer->receive.userId;
     xf->transfer->receive.userId = NULL;

     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);

  }


/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    cdForPut
 * Signature: (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_cdForPut
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring userId, jstring userClass, jboolean edi)
  {

     int rtn = 0;
     XferReference* xf = NULL;
     try {
     
     xf = (XferReference * ) ftpObj;


     xf->transfer->send.userId = (char*)"";
     xf->transfer->send.userClass = copyJavaString(env, userClass);
     xf->transfer->sendParms.edi  = convertJboolean(edi);


     rtn = xferCdForPut( xf->xParams->pXferContext,
                             xf->transfer
                         );

     delete [] xf->transfer->send.userId;
     xf->transfer->send.userId = NULL;
     delete [] xf->transfer->send.userClass;
     xf->transfer->send.userClass = NULL;

     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);

  }

/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    delete
 * Signature: (ILjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_delete
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring deleteFileSpec, jstring userClass)
  {
     int rtn = 0;
     char* tdeleteFile = NULL;
     char* tuserClass  = NULL;
     try {

     XferReference* xf = (XferReference * ) ftpObj;

     tdeleteFile = copyJavaString(env, deleteFileSpec);
     tuserClass  = copyJavaString(env, userClass);

     rtn = xferDelete( xf->xParams->pXferContext,
                         tdeleteFile,
			 tuserClass
                         );

     delete [] tdeleteFile; tdeleteFile = NULL;
     delete [] tuserClass; tuserClass = NULL;

     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);


  }


/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    get
 * Signature: (ILjava/lang/String;Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_get
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring clientFileSpec, jstring serverFileSpec, jboolean ascii)
  {

     int rtn = 0;
     char* tclient = NULL;
     char* tserver = NULL;
     try {
     
     XferReference* xf = (XferReference * ) ftpObj;

     tclient = copyJavaString(env, clientFileSpec);
     tserver = copyJavaString(env, serverFileSpec);

        rtn = xferGet( xf->xParams->pXferContext,
                        tclient,
                        tserver,
                        convertJboolean(ascii),
                        false,
                        false,
                        0
                         );

     delete [] tclient; tclient = NULL;
     delete [] tserver; tserver = NULL;

     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);
  }

/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    getLastMessage
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_getLastMessage
  (JNIEnv * env, jobject javaObj, jint ftpObj)
  {

	 if (ftpObj == 0) 
		 return (env->NewStringUTF("") );

     XferReference* xf = (XferReference * ) ftpObj;

     //char* rtn = xferGetLastMsg( xf->xParams->pXferContext,
     //                                  0
     //                               );
       
               
     

     char* rtn = xf->xParams->pXferContext->lastMsg;

	 if (rtn == NULL) 
	    return(env->NewStringUTF("") );

     return ( env->NewStringUTF(rtn) );


  }


/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    logout
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_logout
  (JNIEnv * env, jobject javaObj, jint ftpObj)
  {

     int rtn = 0;
     try {

     XferReference* xf = (XferReference * ) ftpObj;

     rtn = xferEnd( xf->xParams->pXferContext );

     // clean up after dynamically allocated strings


     // debugging
     //FILE* fd = fopen("blah", "a");
     //fprintf(fd, "Cleaning up dynamic resources\n\n");
     //fprintf(fd, "\n\n");
     //fclose(fd); 

     delete [] xf->xParams->clientSlash;
     xf->xParams->clientSlash = NULL; 

     delete [] xf->xParams->tmpDir;
     xf->xParams->tmpDir = NULL;
        
     delete [] xf->xParams->ediName;
     xf->xParams->ediName = NULL;

     delete [] xf->xParams->runtimeDir;
     xf->xParams->runtimeDir = NULL;

     delete [] xf->xParams->sslRuntimeDir;
     xf->xParams->sslRuntimeDir = NULL;

     delete [] xf->xParams->privateFileName;
     xf->xParams->privateFileName = NULL;

     delete [] xf->xParams->commLogFileSpec;
     xf->xParams->commLogFileSpec = NULL;

     delete [] xf->xParams->xferLogFileSpec;
     xf->xParams->xferLogFileSpec = NULL;

     // debuggin
     //fd = fopen("blah", "a");
     //fprintf(fd, "DONE- Cleaning up dynamic resources\n\n");
     //fprintf(fd, "\n\n");
     //fclose(fd); 

     // delete the ftpObj itself
     delete xf;


     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);

  }



/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    put
 * Signature: (ILjava/lang/String;Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_put
  (JNIEnv * env, jobject javaObj, jint ftpObj, jstring clientFileSpec, jstring serverFileSpec, jboolean ascii)
  {

     int rtn = 0;
     char* clientFile = NULL;
     char* serverFile = NULL;
     try {

     XferReference* xf = (XferReference * ) ftpObj;

     clientFile = copyJavaString(env, clientFileSpec);
     serverFile = copyJavaString(env, serverFileSpec);

        rtn = xferPut( xf->xParams->pXferContext,
                         clientFile,
                         serverFile,
                         convertJboolean(ascii),
			 false,
                         false,
			                   (char*)"",
                         (transfer_t*)""
                         );
     delete [] clientFile; clientFile = NULL;
     delete [] serverFile; serverFile = NULL;

     } catch (...) {
        rtn = 0;
     }

      return ((jint) rtn);


  }

/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    setGetMode
 * Signature: (IZZ)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_setGetMode
  (JNIEnv * env, jobject javaObj, jint ftpObj, jboolean ascii, jboolean edi)
  {

     int rtn = 0;

     try {

     XferReference* xf = (XferReference * ) ftpObj;

     xf->transfer->receiveParms.ascii  = convertJboolean(ascii);
     xf->transfer->receiveParms.edi  = convertJboolean(edi);

     rtn = xferSetGetMode( xf->xParams->pXferContext,
                         xf->transfer,
			 0, // use pt->receiveParams.ascii value
			 0 // use pt->receiveParams.edi value
                         );


     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);


  }

/*
 * Class:     com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect
 * Method:    setPutMode
 * Signature: (IZZZ)I
 */
JNIEXPORT jint JNICALL Java_com_nightfire_comms_util_ftp_ssl_FtpSslLayerInterconnect_setPutMode
  (JNIEnv * env, jobject javaObj, jint ftpObj, jboolean secure, jboolean ascii)
  {

     int rtn = 0;
     try {

     XferReference* xf = (XferReference * ) ftpObj;

     xf->transfer->sendParms.compress  = false;
     xf->transfer->sendParms.secure  = convertJboolean(secure);
     xf->transfer->sendParms.ascii  = convertJboolean(ascii);
     xf->transfer->sendParms.filter  = false;

     rtn = xferSetPutMode( xf->xParams->pXferContext,
                               xf->transfer
                         );


     } catch (...) {
        rtn = 0;
     }

     return ((jint) rtn);


  }

  // convert a jboolean into a bool
  //
  bool convertJboolean(jboolean b)
  {

     bool temp;

     if (b == JNI_TRUE)
        temp = true;
     else
        temp = false;

     return (temp);
  }

  // copys a jstring into a char array
  //
  char* copyJavaString(JNIEnv *env, jstring str)
  {
	const char* tempStr;
	char* copyStr;

	jsize len;
	jboolean mybool = JNI_TRUE;

	len =  env->GetStringUTFLength(str);



	copyStr = new char[(int)len + 1];
        memset(copyStr, 0 , (int)len + 1);


	tempStr = env->GetStringUTFChars( str, &mybool);

	strncpy(copyStr, tempStr, (int)len + 1);
        // add the null byte just in case garbage ends up in the str
        copyStr[(int)len] = '\0';

	env->ReleaseStringUTFChars(str, tempStr);


	return (copyStr);
  }

// handle messages from API
//
void eaMsgHandler(xferContext_t* pXferContext, int msgType, char msgLevel,
                  char* msgText, UINT4 bytesTransferred, UINT4 totalBytes)
{
    // log it using the JVM, depending on the message type
    switch (msgType)
    {
    case EA_MSG_HIGH_PRIORITY:
    case EA_MSG_NORMAL_PRIORITY:
    case EA_MSG_PROGRESS:
    case EA_MSG_PUT:
        fprintf(stderr, "FtpSslLayerInterconnect.so: [%d] %s", msgType,
                msgText);
        break;

    case EA_MSG_CANCEL:
        fprintf(stderr, "FtpSslLayerInterconnect.so: [%d] %s", msgType,
                msgText);
#ifndef UNIX
        ExitThread(-1);
#else
        pthread_exit(0);
#endif
    }
}

#endif

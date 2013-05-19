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
//  Description: Prototypes for cf common utility functions
// ----------------------------------------------------------------------------
//              MAINTENANCE HISTORY
//
// DATE     BY  BUG NO. DESCRIPTION
// -------- --- ------- -------------------------------------------------------
// 19980321 PAA         Initial version
// ----------------------------------------------------------------------------
#if !defined(__CF_HPP__)
#define __CF_HPP__

#if defined(EA_MVS) || (defined(EA_OS400) && defined(IFS))
#include <dirent.h>
#endif /* defined(EA_MVS) || defined(EA_OS400) && defined(IFS) */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>                     // For va_start, va_end, etc.
#include <assert.h>
#include <string.h>
#include <time.h>
#include "eatypes.h"
#include "eaipc.h"
#include "eaconst.h"
#include "eapltfrm.h"
#include "mimeparse.h"
#include "eamsgtbl.h"
#include "eamsg.hpp"
#include "eamutex.hpp"                  // For createMutex(), etc.
#include "btfile.hpp"
#include "nettbl.h"

#if defined(EA_UNIX) || defined(EA_MVS)
//#include "xferdefs.h"
#include "limits.h"
#endif

#if defined(LOG_MEM)		// Turn on for cf_allocVec logging of mem usage
#define	CFMEM	pCf
#else
#define	CFMEM	((cf_t*)0)
#endif

// ----------------------------------------------------------------------------
//	Struct for parsing MIME files using MIMEParse() in mimeparse.c
// ----------------------------------------------------------------------------
#define EA_MAX_BODY_PARTS		32		// Max body parts at a recursion level
typedef struct eaFileList_s eaFileList_t;
struct eaFileList_s {
	int		bodyPartCount;
	BTFILE*	bodyBtFile[EA_MAX_BODY_PARTS];
	BTFILE*	hdrBtFile;
	MIMEHEADERCTX*	mimeCtx;

	//	Meta-fields derived from combinations of fields, values, etc.
	int		msgType;					// Value from list above
	int		mimeType;					// Value from list above
	int		micAlg;						// DA_SHA1/DA_MD5/-1 (multipart/signed)
	bool	base64;						// True if encoding is base64
	bool	quotedPrintable;			// True if encoding is quoted-printable
	bool	requestingSignedMdn;		// True if signed MDN requested
	int		requestingMicAlg;			// DA_SHA1/DA_MD5/-1 (MDN to be sent)
	char*	originalFileName;			// Name of original file from 'name='
										// on Content-Type or 'filename=' on
										// Content-Disposition header entries

	// Values pertaining to JPEG2000 payloads
	char*	xOriginalFileName;			// Name of original file from 'name='
	char*	xOriginalFileFormat;		// Name of original file from 'name='

	//	Convenience pointers into mimeCtx for specific fields
	//	Note: we perform processing on some of these fields, for example,
	//	removing leading '<' and trailing '>' on some, etc.
	//	So - keep these - they are not just useless pointers in mimeCtx!
	char*	returnPath;
	char*	to;
	char*	from;
	char*	as2To;
	char*	as2From;
	char*	subject;
	char*	mimeVersion;
	char*	messageId;
	char*	date;
	char*	dispNotificationTo;			// Reply address form MDN
	char*	dispNotificationOptions;
	char*	dispReceiptTo;				// Address for AS2 async MDN
	char*	asyncMdn;
	char*	boundary;
	char*	contentType;
	char*	partID;						// Unique ID shared by partial msgs
	int		partNumber;					// This partial message part number
	int		totalParts;					// Total parts for message/partial
	char*	encoding;
	char*	disposition;
	// Responses for HTTP messages
	char*	httpResponse;
	char*	httpServer;
	char*	httpKeepAlive;
	char*	httpConnection;
	char*	httpLength;
	char*	httpEncoding;
};

// ----------------------------------------------------------------------------
//  Constants used to file-open mode
// ----------------------------------------------------------------------------
#define CF_ASCII            "w"         // Open as 'text' file
#define CF_BINARY           "wb"        // Open as 'binary' file
#define CF_ASCII_APPEND     "a"         // Open as 'text' file
#define CF_BINARY_APPEND    "ab"        // Open as 'binary' file

// ----------------------------------------------------------------------------
//  Constants used to manage dynamic data on file-close.
// ----------------------------------------------------------------------------
#define CF_RETAIN_CONTEXT   0           // Do not destroy dynamic data in cf_t
#define CF_DESTROY_CONTEXT  1           // Destroy (free) dynamic data in cf_t

// ----------------------------------------------------------------------------
//  Data used to keep track of log-file, logging-level, etc.
// ----------------------------------------------------------------------------
struct cf_s
{
    FILE*       theLog;                 // File ptr of open log
    char*       logDir;                 // Ptr to dir logfile is in
    char*       logFileName;            // Base fileName of log-file
    char*       openMode;               // File open mode: CF_ASCII or CF_BINARY
    char*       tmpBuffer;              // Buffer to store formatted message
    logLevel_t  logLevel;               // Used to filter messages
    bool        timeStampFlag;          // If true then do time-stamping
    bool        batchMode;              // True if program in batch mode
    bool        flushByClose;           // If true, then close to flush
    bool        useMutex;               // If true, then lock mutex first
    void*       mutex;                  // Manages interprocess synchronization
};

// ----------------------------------------------------------------------------
//	Macros and struct for managing dynamic temp-file creation
// ----------------------------------------------------------------------------
typedef	struct eaTempFileInfo_s	eaTempFileInfo_t;
struct eaTempFileInfo_s
{
#if defined(EA_MVS) || defined(EA_OS400)
	char			hfsDir[BUF_512_SZ+1];		// HFS dir for temp files
	char			tmpFileName[BUF_512_SZ+1];	// Current temp file name
	int				recordLength;				// Record length for files.
#endif
	unsigned long	tmpFileCnt;					// Current file number
};

// ----------------------------------------------------------------------------
//  Macros used by functions dealing with wild-carded files
// ----------------------------------------------------------------------------
#define     FIND_FIRST_FILE     1
#define     FIND_NEXT_FILE      2
#define     NO_FILES_FOUND_STR  "No files found matching file name"
#define     NO_MORE_FILES_STR   "No more files found matching file name"

// ----------------------------------------------------------------------------
//  Macros used by copyFile
// ----------------------------------------------------------------------------
#define     EA_OVERWRITE_MODE   1
#define     EA_APPEND_MODE      2

// ----------------------------------------------------------------------------
//  Struct used by functions dealing with wild-carded files;
//  used to maintain context between calls
// ----------------------------------------------------------------------------
//typedef struct eaWCStruct_s   eaWCStruct_t;
struct eaWCStruct_s
{
    char            baseDir[BUF_256_SZ+1];
    char            filePattern[BUF_256_SZ+1];
#if defined(EA_WIN32) || defined(EA_WIN16)
    HANDLE          findHandle;
    WIN32_FIND_DATA findBuf;
#elif defined(EA_DOS)
    struct find_t   find;
#elif defined(EA_MVS) || defined(EA_UNIX) || (defined(EA_OS400) && defined(IFS))
#if defined(EA_SCOUNIX) || defined(EA_MVS)
    char            pEntry[sizeof(struct dirent) + _POSIX_PATH_MAX];
#else
    char            pEntry[sizeof(struct dirent) + PATH_MAX];   // readdir_r ctx
#endif
    struct dirent*  pDirent;                    // Struct to store file info
    DIR*            pDir;                       // Directory stream ptr
#endif
};

// ----------------------------------------------------------------------------
//  Structure used (with qsort) to sort a sortIndex_t array
// ----------------------------------------------------------------------------
//typedef struct sortIndex_s sortIndex_t;
struct sortIndex_s
{
    int     index;
    char*   string;
};

// ----------------------------------------------------------------------------
//  Prototypes of functions
// ----------------------------------------------------------------------------
#ifdef  __cplusplus
extern "C" {
#endif

void    buildDateAndRandomStrings(cf_t* pCf, unsigned char* randomArray,
            char* dateTime, int dateTimeSz, char* msgId, char* randomString);

void    cf_openLog(cf_t*, char*, char*, char*, int, bool, bool);
int		cf_reOpenLog(cf_t*, char*, char*, char*, int);
FILE*   cf_closeLog(cf_t*, int);
void    cf_log(cf_t*, int, const char*, ...);
FILE*   cf_openFile(char*, char*, char*, const char*);
int     cf_accessFile(char*, char*, char*, int);
bool    cf_isBlank(char*);
void    cf_allocVec(void**, int, int, cf_t*, char*, cf_t*);

void    cf_copyString(char** pOutput, char* inString1, char* text, cf_t* pCf);
void    cf_copyTwoStrings(char** pOutput, char* inString1, char* inString2,
            char* text, cf_t* pCf);

void	cf_freeMimeParts(cf_t* pCf, eaFileList_t* pMimeParts);
void	cf_getMsgType(cf_t* pCf, int style, eaFileList_t* pMimeParts,
    		int* retMultiPart, int* retMsgType, int* retMimeType,
			int* retBase64, int* retMicAlg, char* retDispNotifTo,
			char* retDispReceiptTo, char* retOriginalFileName,
			int* retRequestingSignedMdn, int* retRequestingMicAlg);
int     cf_parseMimeFile(cf_t* pCf, BTFILE* smimeBtFile,
            eaFileList_t* pMimeParts, int hdrOnly, SINT4* errCode,
            char* errMsg, char* tmpFileDir, void* tmpFileCnt);

int     addParmsToList(int numParms, char** args, char* otherParms, char* buf);
int     breakDate(char* dateString, int* year, int* month, int *day);
int     breakTime(char* timeString, int* hour, int* minute, int *second);

//#if defined(EA_MVS)
//extern "C"
//#endif
int     compareSortIndices(const void* arg1, const void* arg2);
void    convertDateTimeToLocalTimeZone(char* inBuf, char* outBuf,
            int outBufLen);
int     copyFile(char* inFileSpec, char* outFileSpec, int eaMode);

void    deleteWCContext(eaWCStruct_t* pWC);
int		deleteDirectory(cf_t* pCf, char* clientSlash, char* dir, char* errMsg);
int     deleteFiles(cf_t* pCf, char* clientSlash, char* dir, char* fileName,
            char* errMsg);

void    eaAscTime(struct tm* currTm, char* buf, int bufLen);
void    eaCtime(const time_t* currTime, char* buf, int bufLen);
void    eaCurrAscTime(char* buf, int bufLen);
void    eaCurrStrfGMTime(char* buf, int bufLen, char* format);
void    eaCurrStrfTime(char* buf, int bufLen, char* format);
char*   eaGetLine(char* readBuf, int bufSz, FILE* fp);
int     eaGetLineBinary(char* readBuf, int bufSz, FILE* fp, char* lastChar,
            int* fullBuf);
void    eaGMTime(time_t* currTime, struct tm* currTm);
void    eaLocalTime(time_t* currTime, struct tm* currTm);
int     eaMemicmp(char* source, char* target, int len);
int     eaRemove(char* fileSpec);
int     eaRename(char* oldFileSpec, char* newFileSpec);
void    eaSleep(cf_t* pCf, int milliSeconds, char* msg);
int     eaStat(char* fileSpec, struct CPSTAT* pStatBuf);
char*   eaStrUpr(char* a);
int     eaStricmp(const char* a, const char* b);
void	eaSubst(char* buf, int bufSz, char* substBuf, char* val);

bool    enumerateFileSpec(char* fileSpec, eaWCStruct_t* pWC, char* errBuf,
            int mode, char* outFileSpec, cf_t* pCf, char* clientSlash);

#if defined(EA_UNIX) || ( defined(EA_OS400) && defined(IFS) )
int     filePatternMatch(cf_t* pCf, const char* s, const char* p);
#endif
bool    fileSpecIsDir(const char* fileSpec, char* clientSlash);
void    makeAutoExtFileSpec(char* clientSlash, int autoExtLen, char* inFileSpec,
            char* outFileSpec, int useTime = 0);

void	makeTempFileSpec(cf_t* pCf, char** pFileName, char* dir, char* baseName,
			int empty, void* pVoidFI);

void    parseHttpAddress(char* addr, bool* ssl, char* url, int* port,
			char* uri);

void	splitEdsElitClass(char* userClass, char* edsFileName, char* edsRef,
			char* edsOptions);
void	splitEdsElitSendOptions(char* edsOptions, char* edsSenderId,
			char* edsFB, char* edsLRECL, bool* edsStripLF, bool* edsKeepLF);
void	splitEdsElitReceiveOptions(char* edsOptions, char* edsDataSetName,
			char* edsCtlNo, bool* edsFd);

char*   skipBackSlash(char* p);
char*   skipNonBackSlash(char* p);
char*   skipText(char* p);
char*   skipWhiteSpace(char* p);
void    stripFullPath(char* clientSlash, const char* fileSpec, char* dir,
            char* fileName);

void	DumpStorage(char* header, int len, char* buf);

#if defined(EA_OPENVMS71)
int     convertUnixFileNameToVMS(cf_t* pCf, char* inSpec, char* outSpec);
int     convertVMSFileNameToUnix(cf_t* pCf, char* inSpec, char* outSpec);
int     VMSCallback(char* name, int type);
#endif  // if defined(EA_OPENVMS71)

#ifdef  __cplusplus
}
#endif
#endif  // if !defined(__CF_HPP__)

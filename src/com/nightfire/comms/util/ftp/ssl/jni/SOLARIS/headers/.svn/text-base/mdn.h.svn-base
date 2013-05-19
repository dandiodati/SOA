/* ------------------------------------------------------------------------	*/
/* This document contains material which is the proprietary property of		*/
/* and confidential to Btrade.com Inc.										*/
/* Disclosure outside Btrade is prohibited except by license agreement		*/
/* or other confidentiality agreement.										*/
/*																			*/
/* Copyright (c) 2000 by Btrade.com Incorporated							*/
/* All Rights Reserved														*/
/*																			*/
/* Description: Accessor functions for MDN table							*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*						MAINTENANCE HISTORY									*/
/*																			*/
/* DATE		BY	BUG NO	DESCRIPTION											*/
/* -------- --- ------	--------------------------------------------------- */
/* 20000726	IDS			Initial version										*/
/* 20001228	PAA			Convert to ic V2 & V1b of db; made API modifications*/
/*						to insert row and icCtx into particCtx.				*/
/* ------------------------------------------------------------------------	*/
#if !defined(__MDN_H__)
#define __MDN_H__

#if defined(__cplusplus)
extern "C" {
#endif

#include <time.h>
/* #include "ic.h" */
#include "cpapi.h"

typedef void ICENV;         /* ICENV* points to BtDaoManager object */
typedef void ICCTX;         /* ICCTX* points to BtMdnDatabase object */
#if !defined(__ic_h) && !defined(__cpsql_h)
#if defined(EA_OS400) || defined(OS_400) || defined(EA_MVS) || defined(MVS)
typedef long BT_SQLHANDLE;
#else
typedef void* BT_SQLHANDLE;
#endif
#endif /* !defined(__ic_h) */

/* ------------------------------------------------------------------------	*/
/*	"C" function prototypes from ic.h										*/
/* ------------------------------------------------------------------------	*/


/*
 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

 DAO requires a new parameter for Connect functins: const char * apServerType

 Current version reads an environment variable CPSERVERTYPE

 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
*/

__EXPORT int    __CPCONV icAllocPooledEnv(ICENV**);
__EXPORT void   __CPCONV icFreePooledEnv (ICENV*);
__EXPORT int    __CPCONV icPooledConnect (ICCTX**, char*, char*, char*,
                                          ICENV*, char*);
__EXPORT void*  __CPCONV icGetConnectionHandle(ICCTX *ctx);
__EXPORT int    __CPCONV icConnect       (ICCTX**, char*, char*, char*, char*);
__EXPORT int    __CPCONV icConnectLite   (ICCTX **ctx, void* hDbc);
__EXPORT int    __CPCONV icDisconnect    (ICCTX*);
__EXPORT int    __CPCONV icDisconnectLite(ICCTX*);
__EXPORT short  __CPCONV icCommit        (ICCTX*);
__EXPORT short  __CPCONV icRollback      (ICCTX*);


/* ------------------------------------------------------------------------	*/
/*	Lengths of MDN character columns										*/
/* ------------------------------------------------------------------------	*/
#define	MDN_MESSAGEID_LEN			128
#define	MDN_DISP_LEN				128
#define	MDN_SENDERID_LEN			128
#define	MDN_RECEIVERID_LEN			128
#define	MDN_NETWORKNAME_LEN			64
#define	MDN_ORIGFILENAME_LEN		254
#define	MDN_MDNFILENAME_LEN			254
#define	MDN_MESSAGEDIGEST_LEN		36
#define	MDN_COMPRESSED_LEN			1
#define	MDN_ENCRYPTED_LEN			1
#define	MDN_SIGNED_LEN				1
#define	MDN_ERRORMESSAGE_LEN		254

#define	MDN_TABLENAME				"MDN"

/* ------------------------------------------------------------------------	*/
/*	Value of Disposition field for pending MDNs								*/
/* ------------------------------------------------------------------------	*/
#define	MDN_PENDING_STRING			(char*)"MDN Pending"

/* ------------------------------------------------------------------------	*/
/*	MDN column work-areas													*/
/* ------------------------------------------------------------------------	*/

/* typedef struct mdn_s MDN; */

typedef void MDN;  /* MDN* points to BtMdn object */

typedef struct mdn_cols_s 
{
	unsigned char	messageId[MDN_MESSAGEID_LEN+1];
	unsigned char	originalMessageId[MDN_MESSAGEID_LEN+1];
	unsigned char	disposition[MDN_DISP_LEN+1];
	unsigned char	senderId[MDN_SENDERID_LEN+1];
	unsigned char	receiverId[MDN_RECEIVERID_LEN+1];
	unsigned char	networkName[MDN_NETWORKNAME_LEN+1];
	unsigned char	origFileName[MDN_ORIGFILENAME_LEN+1];
	unsigned char	mdnFileName[MDN_MDNFILENAME_LEN+1];
	unsigned char	messageDigest[MDN_MESSAGEDIGEST_LEN+1];
	unsigned char	compressed[MDN_COMPRESSED_LEN+1];
	unsigned char	encrypted[MDN_ENCRYPTED_LEN+1];
	unsigned char	signedData[MDN_SIGNED_LEN+1];
	time_t			transTimeStamp;
	time_t			resendTimeStamp;
	time_t			mdnTimeStamp;
	unsigned char	errorMessage[MDN_ERRORMESSAGE_LEN+1];
} MDN_COLS;

/* ------------------------------------------------------------------------	*/
/*	"C" function prototypes													*/
/* ------------------------------------------------------------------------	*/
__EXPORT int __CPCONV AddMDN(MDN*, MDN_COLS*);
__EXPORT void __CPCONV CloseMDNCursor(MDN* mdn);
__EXPORT int __CPCONV DeleteMDN(MDN*);
__EXPORT int __CPCONV EndMDN(ICCTX* icCtx, MDN* mdn);
__EXPORT int __CPCONV FetchMDN(MDN*, MDN_COLS*);
__EXPORT int __CPCONV InitMDN(ICCTX*, MDN**, const char*);
__EXPORT int __CPCONV SelectMDN(MDN* mdn, MDN_COLS* row, time_t startDate,
						time_t endDate, int useMDN, int mdnMissing);
__EXPORT int __CPCONV SelectMDN_SandR(MDN* mdn, MDN_COLS* row, time_t startDate,
						time_t endDate, int useMDN, int mdnMissing);
__EXPORT int __CPCONV SelectMDN_NoSR(MDN* mdn, MDN_COLS* row, time_t startDate,
						time_t endDate, int useMDN, int mdnMissing);
__EXPORT int __CPCONV SelectMDNByMessageId(MDN*, MDN_COLS*, const char*);
__EXPORT int __CPCONV SelectMDNByOriginalMessageId(MDN*, MDN_COLS*, 
                                                   const char*);
__EXPORT int __CPCONV SelectMDNByMessageDigest(MDN*, MDN_COLS*, const char*);
__EXPORT int __CPCONV UpdateMDNByMessageId(MDN*, const char* messageId,
						const char* disposition, const char* mdnFileSpec,
						time_t mdnTimeStamp, char* errorMsg);
__EXPORT void __CPCONV buildSqlErrorMsg(ICCTX* icCtx,
						char* msg, const char* text, int rc);
__EXPORT int __CPCONV MDNMessageDigestExists(MDN*, const char*);
__EXPORT const char* __CPCONV GetMdnSqlErrorText(ICCTX* icCtx );

#if defined(__cplusplus)
}
#endif

#endif	/* if !defined(__MDN_H__) */

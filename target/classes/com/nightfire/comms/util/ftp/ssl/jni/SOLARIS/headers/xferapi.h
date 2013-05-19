/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary				*/
/*	property of	and confidential to bTrade, Incorporated.					*/
/*	Disclosure outside bTrade, Inc. is prohibited except by					*/
/*	license agreement or other confidentiality agreement.					*/
/*																			*/
/*	Copyright (c) 1999 by bTrade, Incorporated								*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description: Prototypes for xfer dll entry points						*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*					MAINTENANCE	HISTORY										*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19990106	PA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__XFERAPI_H__)
#define __XFERAPI_H__

#include "transfer.h"				/* Definition of transfer_t struct		*/
#include "xferdefs.h"				/* All other DLL structs and macros		*/
#include "btfile.hpp"

#ifdef  __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------	*/
/*	Prototypes of "public" DLL entry points 								*/
/* ------------------------------------------------------------------------	*/
CP_DLL int		xferBuildDestFileSpec(xferContext_t*, transfer_t* pT,
					char* sendFileSpec, char* sendDescrip, char* sendDescrip2,
					char* sendDescrip3, char* destFileName, char* destFileSpec);
CP_DLL int		xferCdForGet(xferContext_t*, transfer_t* pT);
CP_DLL int		xferCdForPut(xferContext_t*, transfer_t* pT);
CP_DLL int		xferCompressFile(xferContext_t*, transfer_t* pT,
					char* clientFileSpec, char* outputFileSpec,
					char* description, bool deEnvelopeOnly);
CP_DLL int		xferConnect(xferContext_t*, char* hostIp, char* hostIp2,
					unsigned short controlPort, bool ssl, bool dhOnly,
					bool passive, int lowClientPort, int highClientPort);
CP_DLL int		xferConnectionLost(xferContext_t*, bool* connectionLost);
CP_DLL int		xferCreate(xferParms_t* xferParms);
CP_DLL int		xferDecompFile(xferContext_t*, transfer_t* pT,
					char* clientFileSpec, char* outputFileSpec);
CP_DLL int		xferDelete(xferContext_t*, char* deleteFileSpec,
					char* userClass);
CP_DLL int		xferEnd(xferContext_t*);
CP_DLL int		xferGet(xferContext_t*, char* clientFileSpec,
					char* serverFileName, bool ascii, bool edi,
					bool restart, UINT4 size);
CP_DLL int		xferGetAuditLogs(xferContext_t*, auditParms_t* pAuditParms,
					char* auditFileSpec);
CP_DLL char*	xferGetLastMsg(xferContext_t*, long* lastCode);
CP_DLL int		xferList(xferContext_t*, char* listFileSpec, char* mode,
					char* pCmd, int type, char* pUserId, int userMode,
					char* pUserClass);
CP_DLL int		xferListData(xferContext_t*, void** pListData, int* listCount);
CP_DLL char*	xferListFilter(xferContext_t*, char* readBuffer, transfer_t* pT,
					int* transmitMode, char* originalFileName, int* dataFormat);
CP_DLL int		xferLogin(xferContext_t*, char* userId, char* account,
					char* passwd, char* newPasswd, bool proxyLogin);
CP_DLL int		xferPrepareImage(xferContext_t* pXferContext,
					char* clientFileSpec, char* outputFileSpec,
					char* mimeFileSpec, transfer_t* pT);
CP_DLL int		xferProcessExportFile(xferContext_t*, char* rtmFileSpec);
CP_DLL int		xferPut(xferContext_t*, char* clientFileSpec,
					char* serverFileName, bool ascii, bool edi,
					bool restart, char* uniqueServerFileName, transfer_t* pT);
CP_DLL int		xferReconnect(xferContext_t*);
CP_DLL int		xferRequestThreadCancel(xferContext_t*);

CP_DLL int		xferSetBtFile(xferContext_t* pXferContext, BTFILE* pBtFile);
CP_DLL int		xferSetCtxHandle(xferContext_t*, void* pCtxHandle,
					char* ediQual, char* ediName, char* as2Name);

CP_DLL int		xferSetGetMode(xferContext_t*, transfer_t* pT,
					int transmitMode, int dataFormat);
CP_DLL int		xferSetListMode(xferContext_t*);
CP_DLL int		xferSetPutMode(xferContext_t*, transfer_t* pT);
CP_DLL void		xferSetStatusFilter(xferContext_t* pXferContext, int val);
CP_DLL int		xferSite(xferContext_t*, char* command);
CP_DLL int		xferSiteCompress(xferContext_t*, int value);
CP_DLL int		xferSiteMsgSeqn(xferContext_t*, char* sequenceNumber);
CP_DLL int		xferTellBool(xferContext_t*, int keyword, bool* value);
CP_DLL int		xferTellInt(xferContext_t*, int keyword, int* value);
CP_DLL int		xferTellString(xferContext_t*, int keyword, char* buf,
					int bufLen);

CP_DLL int		xferSetRespParms_ExtendedStatus(xferContext_t*, char* statusMsg,
					long statusCode);
CP_DLL int		xferSetRespParms_LocalFileName(xferContext_t*, char* fileName);
CP_DLL int		xferSetRespParms_RemoteFileName(xferContext_t*, char* fileName);
CP_DLL int		xferSetRespParms_Sender(xferContext_t*, char* sender);
CP_DLL int		xferSetRespParms_Receiver(xferContext_t*, char* receiver);
CP_DLL int		xferSetRespParms_TransferName(xferContext_t*, char* transferNm);

CP_DLL int		xferWriteResponseFileEntry(xferContext_t*, char* errMsg);
#ifdef  __cplusplus
}
#endif

#endif	/* if !defined(__XFERAPI_H__) */

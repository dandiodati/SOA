/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary				*/
/*	property of	and confidential to bTrade.com Incorporated.				*/
/*	Disclosure outside bTrade.com is prohibited except by					*/
/*	license agreement or other confidentiality agreement.					*/
/*																			*/
/*	Copyright (c) 1999-2000 by bTrade.com Incorporated						*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description: Structs for messages propagated to component processes		*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*					MAINTENANCE	HISTORY										*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19980915	PAA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__EAIPC_H__)
#define	__EAIPC_H__

#include "eapltfrm.h"				/* For UINT4							*/
#include "eatypes.h"				/* For eaMsg_t typedef					*/

/* ------------------------------------------------------------------------	*/
/*	Constants for thread messages propagated to the main thread				*/
/* ------------------------------------------------------------------------	*/
#define	MSGBOX_MSG				101	/* Message for MessageBox				*/
#define	TEXT_MSG				102	/* Message for Text window				*/
#define	LOG_MSG					103	/* Message for EA log-file				*/
#define	LOGFILE_MSG				104	/* Message for EA log-file				*/
#define	PROGRESS_MSG			105	/* Update to progress bar 				*/
#define	DEBUG_MSG				106	/* Message for debug log				*/
#define	PUT_STATUS_MSG			107	/* Message with server's unique filename*/
#define	CANCEL_MSG				108	/* Message to cancel thread				*/
#define	GET_FILESPEC_MSG		109	/* Message to get a fileSpec from caller*/
#define	GET_USER_INPUT_MSG		110	/* Message to get user input text		*/
#define	PROCESSING_ERROR_MSG	111	/* Message to get user input text		*/
#define	VIEWFILE_MSG			112	/* Message to launch file-viewer 		*/

/* ------------------------------------------------------------------------ */
/*	Constants for detailLevel for textMsg and logFileMsg messages			*/
/* ------------------------------------------------------------------------ */
#define DETAIL_LEVEL		'D'
#define SUMMARY_LEVEL		'S'

/* ------------------------------------------------------------------------	*/
/*	MsgBox message: used for error messages and special info msgs			*/
/* ------------------------------------------------------------------------	*/
struct msgBoxMsg_s {
	int		msgBoxType;				/* EA_INFO, EA_WARN, EA_FATAL			*/
	int		infoCode;				/* Info code - EA_FILEOPEN_ERROR, etc.	*/
	char*	text;					/* Text to be show user					*/
	bool	needAnswer;				/* True if an answer to msg is needed	*/
	int		answer;					/* Response: 1/0=true/false=OK/Cancel	*/
};

/* ------------------------------------------------------------------------	*/
/*	Text message: used to convey SUMMARY- or DETAIL-level text				*/
/* ------------------------------------------------------------------------	*/
struct textMsg_s {
	char	detailLevel;			/* SUMMARY_LINE or DETAIL_LINE			*/
	char*	text;					/* Text to be displayed					*/
};

/* ------------------------------------------------------------------------	*/
/*	Logfile message: used to convey availability of logfile for an operation*/
/*	Always SUMMARY-level													*/
/* ------------------------------------------------------------------------	*/
struct logFileMsg_s {
	char	detailLevel;			/* SUMMARY_LINE or DETAIL_LINE			*/
	char*	fileSpec;				/* Filespec of file to be displayed		*/
};

/* ------------------------------------------------------------------------	*/
/*	Get-user-input message: ask user for input and pass down into EAAPI		*/
/* ------------------------------------------------------------------------	*/
struct viewFileMsg_s {
	char*	fileSpec;				/* Filespec of file to be displayed		*/
	char*	defaultExt;				/* Default file extension				*/
};

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
struct progressMsg_s {
	UINT4	bytesTransferred;		/* Bytes sent/received so far			*/
	UINT4	totalBytes;				/* Total bytes to be sent/received		*/
};

/* ------------------------------------------------------------------------	*/
/*	Debug message: used for logging purposes only							*/
/*	Always SUMMARY-level													*/
/* ------------------------------------------------------------------------	*/
struct debugMsg_s {
	int		debugLevel;				/* LogInfo, LogDebug, LogFatal, etc.	*/
	char*	text;					/* Text to be displayed					*/
};

/* ------------------------------------------------------------------------	*/
/*	Put-status message: used for passing server unique file ID up to caller*/
/* ------------------------------------------------------------------------	*/
struct putStatusMsg_s {
	char*	fileName;				/* Unique server file name				*/
};

/* ------------------------------------------------------------------------	*/
/*	Get-file message: ask user for file-spec and pass down into EAAPI		*/
/* ------------------------------------------------------------------------	*/
struct getFileSpecMsg_s {
	char*	fileSpec;				/* User selected full file specification*/
};

/* ------------------------------------------------------------------------	*/
/*	Get-user-input message: ask user for input and pass down into EAAPI		*/
/* ------------------------------------------------------------------------	*/
struct getUserInputMsg_s {
	char*	prompt;					/* Text to prompt user for input		*/
	char*	userInput;				/* User specified text					*/
};

/* ------------------------------------------------------------------------	*/
/*	Generic processing error message										*/
/* ------------------------------------------------------------------------	*/
struct processingErrorMsg_s {
	SINT4	errorCode;				/* Error code							*/
	char*	errorMsg;				/* Text of error message				*/
};

/* ------------------------------------------------------------------------	*/
/*	Struct defining fill EAAPI message content								*/
/* ------------------------------------------------------------------------	*/
struct eaMsg_s {
	eaMsg_t*	nextMsg;			/* User convenience for chaining msgs	*/
	int			msgType;			/* MSGBOX_MSG/TEXT_MSG/LOGFILE_MSG, etc.*/
	union msgFormat {
		msgBoxMsg_t				msgBoxMsg;
		textMsg_t				textMsg;
		logFileMsg_t			logFileMsg;
		viewFileMsg_t			viewFileMsg;
		progressMsg_t			progressMsg;
		debugMsg_t				debugMsg;
		putStatusMsg_t			putStatusMsg;
		getFileSpecMsg_t		getFileSpecMsg;
		getUserInputMsg_t		getUserInputMsg;
		processingErrorMsg_t	processingErrorMsg;
	} u;
};

#endif	/* if !defined(__EAIPC_H__) */

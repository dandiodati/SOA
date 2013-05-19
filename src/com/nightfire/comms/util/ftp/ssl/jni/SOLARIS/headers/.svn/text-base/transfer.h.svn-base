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
/*	Description: Header file which defines elements of a transfer			*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/* MAINTENANCE HISTORY														*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19990106	PA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__TRANSFER_H__)
#define __TRANSFER_H__

#include "eapltfrm.h"

#if defined(__cplusplus)
extern "C" {
#endif

/* ------------------------------------------------------------------------	*/
/*	Typedefs of all structs used for transfer processing					*/
/* ------------------------------------------------------------------------	*/
typedef struct sendParms_s			sendParms_t;
typedef struct receiveParms_s		receiveParms_t;
typedef struct send_s				send_t;
typedef struct receive_s			receive_t;
typedef struct xfer_s				xfer_t;
typedef struct xferProc_s			xferProc_t;
typedef struct transfer_s			transfer_t;

/* ------------------------------------------------------------------------	*/
/*	Macros used with transfer pre- and post-processing						*/
/* ------------------------------------------------------------------------	*/
#define	LOGIC_SUCCEEDS				0
#define	LOGIC_FAILS					1
#define	NUM_LOGIC_STATES			(LOGIC_FAILS + 1)

#define	LOGIC_EQ					0
#define	LOGIC_LT					1
#define	LOGIC_GT					2
#define	LOGIC_ALWAYS				3
#define	NUM_LOGIC_CODES				(LOGIC_ALWAYS + 1)

#define	LOGIC_STATE_DEFAULT			(LOGIC_SUCCEEDS)
#define	LOGIC_CODE_DEFAULT			(LOGIC_EQ)
#define	LOGIC_RETVAL_DEFAULT		(0)

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
struct sendParms_s
{
	bool			compress;			/* Enables compression				*/
	bool			secure;				/* Enables compress 4.0 certs		*/
	bool			filter;				/* Enables compress FILTER option	*/
	bool			ascii;				/* Send in binary or ascii mode		*/
	bool			crlf;				/* Enables compress CRLF option		*/
	bool			edi;				/* Send EDI file or normal file		*/
	bool			xml;				/* Data is XML file or not			*/
	char*			compressRate;		/* Compression rate (for JPEG2000)	*/
	bool			image;				/* Data is image file or not		*/
	bool			deleteAfterSend;	/* True=>delete src file after send	*/
	bool			perpetualSend;		/* True=> send files over and over	*/
	long			size;				/* Size (for EDI, from sendedi.log) */
	char*			sequenceNumber;		/* IGN-specific value (cmdline only)*/
	char*			otherParms;			/* Open-ended list of compress parms*/
};

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
struct receiveParms_s
{
	bool			append;				/* Enables decomp APPEND option		*/
	bool			crlf;				/* Enables decomp CRLF option		*/
	int				autoext;			/* Zero or autoext length			*/
	long			size;				/* Size (from Query or List)		*/
	bool			ascii;				/* Receive in binary or ascii mode	*/
	bool			edi;				/* Receive EDI file or normal file	*/
	bool			uncomp;				/* Option for non-compressed files	*/
	bool			perpetualReceive;	/* True=>receive files over and over*/
	bool			useServerName;		/* Use server's name for file		*/
	bool			useOriginalName;	/* Use original (client) file name	*/
	char*			otherParms;			/* Open-ended list of decomp parms	*/
};

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
struct xferProc_s
{
	char*			cmdLine;			/* Pre/post-processing prog and args*/
	int				state;				/* Index into logicStateTable		*/
	int				code;				/* Index into logicCodesTable		*/
	long			retVal;				/* Value to indicate success or fail*/
	bool			runAfterEachFile;	/* If true, do proc after each file	*/
};

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
struct xfer_s
{
	bool			transmit;			/* Y or N: transmit or not			*/
	bool			deleteServerFile;	/* Y or N: delete server file or not*/
	bool			checkFileExists;	/* Y or N: Check exists before exec	*/
	char*			clientFileSpec;		/* File on client side of transfer	*/
	char*			serverFileSpec;		/* File on server side of transfer	*/
	char*			originalFileName;	/* Orig filename as per server side	*/
	char*			userClass;			/* Class to send to or receive from	*/
	char*			userId;				/* User to send to or receive from	*/
	char*			toEdiName;			/* ediName (perhaps from tpAddrBook)*/
	char*			toEdiQual;			/* ediName (perhaps from tpAddrBook)*/
	char*			toAS2Name;			/* AS2name (perhaps from tpAddrBook)*/
	char*			fromEdiName;		/* Sender's EDINAME					*/
	char*			encryptEdiName;		/* EDINAME of encrypting cert		*/
	char*			originalMessageId;	/* Orig msgId (AS1/AS2 re-transmit) */
	xferProc_t		preProcessing;		/* Pre-processing info				*/
	xferProc_t		postProcessing;		/* Post-processing info				*/
};

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
struct transfer_s
{
	char*			name;				/* Transfer name (from namelist)	*/
	char*			userId;				/* User logon prior to send/receive	*/
	char*			passwd;				/* Password of logon for transfer	*/
	xfer_t			send;				/* Send portion						*/
	sendParms_t		sendParms;			/* Send/compress options			*/
	xfer_t			receive;			/* Receive portion					*/
	receiveParms_t	receiveParms;		/* Receive/decomp options			*/
};

#if defined(__cplusplus)
}
#endif

#endif	/* if !defined(__TRANSFER_H__) */

/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary				*/
/*	property of	and confidential to bTrade, Incorporated.					*/
/*	Disclosure outside bTrade, Inc. is prohibited except by					*/
/*	license agreement or other confidentiality agreement.					*/
/*																			*/
/*	Copyright (c) 1999 by bTrade, Inc. Incorporated							*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description: Structs and constants for xfer dll entry points			*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*					MAINTENANCE	HISTORY										*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19990106	PA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__XFERDEFS_H__)
#define __XFERDEFS_H__

#include "eapltfrm.h"
#include "trindex.h"
#include "mdn.h"

#ifdef  __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------	*/
/*	Platform-specific stuff													*/
/*	Macro CP_DLL handles the export and conversion to C API.				*/
/* ------------------------------------------------------------------------	*/
#if defined(WIN16) || defined(EA_WIN16) || defined(WIN32) || defined(EA_WIN32)
#	define	CP_DLL __declspec(dllexport)
#else
#	define	CP_DLL 
#endif

/* ------------------------------------------------------------------------ */
/*	Error message codes														*/
/* ------------------------------------------------------------------------	*/
#define	WILDCARD_ERROR				101	/* Attempted mget without compressio*/
#define	FTP_CREATE_ERROR			102	/* Failed at create step			*/
#define	FTP_INIT_ERROR				103	/* Failed at init step				*/
#define	FTP_CONNECT_ERROR			104	/* Failed to connect to host		*/
#define	FTP_LOGIN_ERROR				105	/* Failed to connect to host		*/
#define	SESSION_START_ERROR			106	/* Session already started			*/
#define	SESSION_EXISTS_ERROR		107	/* No session exists to be ended	*/
#define	LOGIN_EXISTS_ERROR			108	/* No session exists to be ended	*/
#define	XFER_ALLOC_ERROR			109	/* Failed to instantiate XFER object*/
#define	XFER_CREATE_ERROR			110	/* Failed to instantiate XFER object*/
#define	XFER_DATA_ERROR				111	/* Bad input found in constructor	*/
#define	FILENAME_ERROR				112	/* Generic error in filename (???)	*/
#define	FILENAME_LENGTH_ERROR		113	/* Filename too long - > BUF_256_SZ	*/
#define	PROCESS_EXPORT_FILE_ERROR	114	/* ProcessExportFile failed			*/
#define	XFER_INTERNAL_DATA_ERROR	115	/* Bad data caught in code firewall	*/
#define	UNSUPPORTED_FEATURE			116	/* Feature unsupported for platform	*/
#define	FTP_DELETE_ERROR			117	/* Failed to delete file on server	*/
#define	FTP_LS_ERROR				118	/* Failed to perform 'ls' on server	*/
#define	FTP_PUT_ERROR				119	/* Put failed or failed partially	*/

/* ------------------------------------------------------------------------	*/
/*	Typedefs used for the various structs									*/
/* ------------------------------------------------------------------------	*/
typedef struct xferContext_s xferContext_t;
typedef struct xferParms_s	xferParms_t;
typedef struct auditParms_s auditParms_t;
typedef struct eaVersionData_s eaVersionData_t;

/* ------------------------------------------------------------------------ */
/*	Define types of server directory listings								*/
/* ------------------------------------------------------------------------ */
#define SHORT_LS		0				/* Simple 'ls' or 'dir'				*/
#define LONG_LS			1				/* Simple 'ls -l' or 'dir -l'		*/
/* (IGN only) Merge simple 'ls -l' with 'ls -l' from "liststyle filename"	*/
#define LONG_MERGED_LS	2

/* ------------------------------------------------------------------------ */
/*	Define constants used by utility functions and EAFTP List command		*/
/* ------------------------------------------------------------------------ */
#define XFER_RECEIVE	1				/* User is the receiver				*/
#define XFER_SEND		2				/* User was the sender				*/
#define XFER_SEND_ONLY	3				/* User was sender (any receiver)	*/

/* ------------------------------------------------------------------------ */
/*	Macros to keep track of current FTP mode, so issue no redundant commands*/
/* ------------------------------------------------------------------------ */
#define	FTP_MODE_NOT_SET		0
#define	FTP_MODE_ASCII			1
#define	FTP_MODE_BINARY			2

#define	TR_FTP_MODE_ASCII		((char*)"A")
#define	TR_FTP_MODE_BINARY		((char*)"I")

#define	ASCII_STR				((char*)"ASCII")
#define	BINARY_STR				((char*)"Binary")

/* ------------------------------------------------------------------------ */
/*	Macros to keep track of whether current data is EDI data or not         */
/* ------------------------------------------------------------------------ */
#define	DATAFORMAT_NOT_SET		0
#define	DATAFORMAT_NONEDI		1
#define	DATAFORMAT_EDI			2

#define	TR_NONEDI_DATAFORMAT	((char*)"U")
#define	TR_EDI_DATAFORMAT		((char*)"E")

#define	EDI_STR					((char*)"EDI")
#define	NONEDI_STR				((char*)"Non-EDI")
#define	UNFORMATTED_STR			((char*)"Unformatted")

/* ------------------------------------------------------------------------	*/
/*	Define the types of MDNs supported										*/
/* ------------------------------------------------------------------------	*/
#define	MDN_ASYNC_SMTP			1		/* Asynchronous, using SMTP			*/
#define	MDN_ASYNC_HTTP			2		/* Asynchronous, using HTTP			*/
#define	MDN_SYNC				3		/* Synchronous						*/

/* ------------------------------------------------------------------------ */
/*	Macros used in conjunction with API xferTell*() to fetch XFER data		*/
/* ------------------------------------------------------------------------ */
/*	Macros to fetch member bool data from XFER using xferTellBool()			*/
#define	ERROR_FLAG					1001
#define	CDHLIST_FLAG				1002

/*	Macros to fetch member char array data from XFER using xferTellString()	*/
#define	LOGIN_USERID_STRING			1101
#define	CURR_NETWORK_STRING			1102
#define	FULL_LOGIN_USERID_STRING	1103

/* ------------------------------------------------------------------------	*/
/*	Structure used to represent program version info						*/
/* ------------------------------------------------------------------------	*/
#define	EA_VERSION			0		/* E.g. 62.01.01.04						*/
#define	EA_BUILD_DATE		1		/* Date executable was built			*/
#define	EA_CMD_BUILD_DATE	2		/* Date executable was built			*/
#define	EA_SYSTYPE			3		/* W95, AIX, SUN, OS400, MVS, etc.		*/
#define	CP_VERSION			4		/* Version of Compress linked into EA	*/
#define	EAFTP_VERSION		5		/* Version of EA-FTP linked into EA		*/
#define	EASMTP_VERSION		6		/* Version of EA-SMTP linked into EA	*/
#define	EAPOP3_VERSION		7		/* Version of EA-POP3 linked into EA	*/
#define	RIMPORT_VERSION		8		/* Version of CP rimport linked into EA	*/
#define	XFER_VERSION		9		/* Version of EA XFER linked into EA	*/

struct eaVersionData_s
{
	char*	descriptor;				/* Title of field						*/
	char*	data;					/* Field data							*/
};           

/* ------------------------------------------------------------------------	*/
/*	Structure and constants for writing entries into the response-file		*/
/* ------------------------------------------------------------------------	*/
#define	RESP_SEND_DIRECTION				'S'
#define	RESP_RECEIVE_DIRECTION			'R'

#define	RESP_NONEDI_DATAFORMAT			'N'
#define	RESP_EDI_DATAFORMAT				'E'

#define	RESP_ASCII_CHARFORMAT			'A'
#define	RESP_BINARY_CHARFORMAT			'B'

#define	RESP_OK_STATUS					'1'
#define	RESP_SENT_WITH_ERRORS_STATUS	'5'
#define	RESP_FAILED_STATUS				'9'

typedef struct responseParms_s responseParms_t;
struct responseParms_s
{
	char	direction;						/* 'S'=sent, 'R'=received		*/
	char	status;							/* 1=OK, 9=Failed				*/

	long	extendedStatusCode;				/* EA 'lastCode' or zero		*/
	char*	extendedStatusMsg;				/* EA lastMsg' or empty string	*/

	char*	networkName;					/* Name of EA network instance	*/
	char*	loginName;						/* Server login name of sender	*/
	char*	senderQual;						/* Edi qualifier of sender		*/
	char*	sender;							/* Edi name of sender			*/
	char*	receiverQual;					/* Edi qualifier of receiver	*/
	char*	receiver;						/* Edi name of receiver			*/
	char	dataFormat;						/* 'N'=nonEDI, 'E'=EDI			*/
	char	charFormat;						/* 'A'=ASCII, 'B'=binary		*/
	char*	transferName;					/* Name of transfer executing	*/
	char*	localFileName;					/* client-side filename			*/
	char*	remoteFileName;					/* Server-side filename			*/

	/* Used by AS2 network styles only:										*/
	char*	disposition;					/* Disposition reported in MDN	*/
	char*	messageDigest;					/* Digest from msg or MDN		*/
	char	mdnRequest;						/* (S)ync, (A)sync, or (N)one	*/
	char*	mdnFile;						/* FileSpec; '|' replaces slash	*/
	bool	compressed;						/* Y or N						*/
	bool	encrypted;						/* Y or N						*/
	bool	signedData;						/* Y or N						*/
};

/* ------------------------------------------------------------------------	*/
/*	Structure used to manage audit requests from FTP server					*/
/* ------------------------------------------------------------------------	*/
#define	AUDIT_DATE_SZ	9					/* yyyymmdd plus NULL terminator*/
#define	AUDIT_TP_SZ		128					/* Max length of TP name		*/
struct auditParms_s
{
	bool	sent;							/* List sent files				*/
	bool	received;						/* List received files			*/
	bool	undelivered;					/* List undelivered files		*/
	bool	delivered;						/* List delivered files			*/
	bool	purged;							/* List purged files			*/
	char	startDate[AUDIT_DATE_SZ];		/* List files after start-date	*/
	char	endDate[AUDIT_DATE_SZ];			/* List files before end-date	*/
	char	sender[AUDIT_TP_SZ];			/* Fetch for specific sender	*/
	char	receiver[AUDIT_TP_SZ];			/* Fetch for specific receiver	*/
//	char	tradingPartner[BUF_64_SZ];		/* List to/from specific tp		*/
};

/* ------------------------------------------------------------------------	*/
/*	Struct used to create XFER object										*/
/* ------------------------------------------------------------------------	*/
struct xferParms_s
{
	/* Entries required by all network styles								*/
	xferContext_t*		pXferContext;		/* Ptr to context structure		*/
	void*				pTempFileInfo;		/* Struct: info for tmp files	*/
	eaVersionData_t*	eaVersionData;		/* Version info for EA exe		*/
	char*				networkName;		/* Name of network being used	*/
	int					networkStyle;		/* Style of network				*/
	int					networkSubStyle;	/* subStyle of network			*/
	int					lowClientPort;		/* Low port on client side		*/
	int					highClientPort;		/* High port (or 0)				*/
	char*				clientSlash;		/* 'slash' delimiter for client	*/
	char*				tmpDir;				/* Dir for temp files			*/
	char*				errorDir;			/* Dir for unprocess-able AS1/2 */

	/*	Entries for those styles supporting transfer of image data			*/
	bool				autoViewImages;		/* Y or N (some styles only)	*/

	/*	Entries used only for FTP-based network styles						*/
	int					storeUnique;		/* 0=>No;1=>Yes;2=>Yes:RFC-style*/
	int					commTimeoutValue;	/* Valid values are positive	*/
	bool				dataOverCommand;	/* Y or N (EAFTP style only)	*/

	/*	Entries used only by the CPFTP network style						*/
	int					siteDelay;			/* Delay, in seconds, for server*/

	/*	Entries used only by the IGN network style							*/
	bool				enableEdiReplyBuf;	/* True/false (buffer responses)*/
	bool				enableEdiAliasProbe;/* True/false (use EDI probe)	*/

	/*	Entries used by OPENNET style										*/
	char*				mailboxUserId;		/* Initial Open*Net mailbox name*/
	char*				mailboxPasswd;		/* Initial Open*Net mailbox pass*/

	/*	Entries used by AS2 (HTTP) and FTP network styles					*/
	char*				localHostIp;		/* IP address of local machine	*/
	int					firewallType;		/* <None>/SOCKS/PROXY			*/
	char*				firewallHostIp;		/* IP address or Hostname (DNS)	*/
	char*				firewallUserId;		/* UserId to log into firewall	*/
	char*				firewallPasswd;		/* Password to log into firewall*/
	int					firewallPort;		/* Firewall's port				*/

	/*	Fields pertaining to database access								*/
	bool				mdnDatabase;		/* If true, store MDNs in db	*/
	bool				rtmDatabase;		/* If true, use runtime db		*/
	void*				pCtxHandle;			/* Handle to runtimes or databas*/
	void*				hDbc;				/* Handle to connection for MDN */
	char*				dbDSN;				/* DB Data Source Name			*/
	char*				dbSchema;			/* DB Schema name				*/
	char*				dbUserId;			/* DB logon userId				*/
	char*				dbPasswd;			/* DB logon password			*/
	char*				dbType;				/* DB type						*/

	/*	Fields pertaining to EDI-INT messages								*/
	char*				certImportDir;		/* Copy incoming certs here		*/
	char*				mdnDispTo;			/* Address to send MDNs			*/
	char*				mdnDir;				/* Dir to store MDNs			*/
	bool				divertDupData;		/* true=>move dup data to errDir*/
	int					securityLevel;		/* 1/2/3/4=None/Sign/Encrypt/Bot*/

	/*	Entries used only by EMAIL & AS1 (SMTP/POP3) network styles			*/
	char*				myEmailAddress;		/* EMail ID of client			*/
	char*				gatewayMailbox;		/* Name of GISB Server mailbox	*/
	char*				smtpIp;				/* Name/IP address of SMTP host	*/
	char*				popIp;				/* Name/IP address of POP3 host	*/
	char*				popUserId;			/* Username for POP3 login		*/
	char*				popPasswd;			/* Password for POP3 login		*/
	unsigned char*		randomArray;		/* Array of random chars		*/
	bool				autoDelete;			/* Delete EMail after receive	*/
	bool				autoCombine;		/* Auto combine partial msgs	*/
	bool				allPartsOnly;		/* Only get parts if all present*/
	bool				breakApart;			/* If true, split large msgs	*/
	UINT4				breakApartSize;		/* Threshhold for msg splitting	*/

	/*	Entries used only for certificate-based security/encryption			*/
	char*				ediName;			/* EDI name (passwd encryption)	*/
	char*				as2Name;			/* EDI name (passwd encryption)	*/
	char*				runtimeDir;			/* Dir with bTrade certs		*/
	char*				sslRuntimeDir;		/* Dir with SSL certs			*/
	char*				securityDir;		/* Dir with security files		*/
	char*				aliasFileName;		/* Name of export file			*/
	char*				exportFileName;		/* Name of export file			*/
	char*				privateFileName;	/* Name of private file			*/
	char*				localPrivateFileName;/* Name of local private file	*/
	char*				secFileName;		/* 'sec file' name (header.def)	*/
	char*				approvalCode;		/* Client's approval code		*/

	/*	Entries pertaining to log-file generation							*/
	char*				compressLogDir;		/* Dir for compress/decomp logs	*/
	char*				commLogFileSpec;	/* Communications session log	*/
	int					commLogLevel;		/* Log level for Comm log file	*/
	char*				responseLogFileSpec;/* File for Put passthru data	*/
	char*				xferLogFileSpec;	/* XFER log file				*/
	int					xferLogLevel;		/* Log level for XFER log file	*/
	int					socketLogLevel;		/* Log level for ssl/socketapi	*/
	bool				multiThreaded;		/* Use thread or run synchronous*/
};

/* ------------------------------------------------------------------------	*/
/*	Define 'handle' structure used to manage usage by concurrent processes	*/
/* ------------------------------------------------------------------------	*/
struct xferContext_s
{
	void*	lastErrorMsg;					/* Last error message (BTSTRING)*/
	long	lastErrorCode;					/* Last error code				*/
	char	lastMsg[BUF_2048_SZ + 1];		/* Last message (any type)		*/
	long	lastCode;						/* Last code (any type)			*/
	void*	xfer;							/* Base-class ptr to XFER object*/
	void*	pCf;							/* Manages XFER log file		*/
	void*	userObject;						/* Ptr to user object			*/
	void	(*userCallback)(xferContext_t*,	/* User callback: XFER context	*/
							int,			/* XFER msg type				*/
							char,			/* XFER msg level				*/
							char*,			/* XFER msg text				*/
							UINT4,			/* bytes transferred			*/
							UINT4);			/* total bytes					*/
};

/* ------------------------------------------------------------------------	*/
/*	Indices into eaPopMsgData.fields --- must start at 0 and be consecutive!*/
/*	Some fields have duplicate indices, since the eaPopMsgData_t struct is	*/
/*	used by several network styles.											*/
/* ------------------------------------------------------------------------	*/
#define	EA_POP_MSG_NO		0		/* String form of int: server msg count	*/
#define	EA_POP_SIZE			1		/* String form of UINT4: message size	*/
#define	EA_POP_MSG_ID		2		/* Unique message identifier			*/
#define	EA_POP_FROM			3		/* UserId of sender						*/
#define	EA_POP_TO			4		/* UserId of receiver					*/
#define	EA_POP_DATETIME		5		/* Time-stamp (unparsed - format is ???)*/
#define	EA_POP_SUBJECT		6		/* Subject of message					*/
#define	EA_POP_CONTENT_TYPE	7		/* Tells if have attachment?			*/
#define	EA_POP_BOUNDARY		8		/* String delimiting body and attachment*/
#define	EA_POP_STATUS		9		/* Whether has been read already or not	*/
#define	NUM_EA_POP_FIELDS	(EA_POP_STATUS+1)

/*	Duplicate index names for ILK and ENTERPRISE STYLES						*/
#define	EA_GXS_MSGID		0		/* ILK & ENTERPRISE: Message ID			*/
#define	EA_GXS_DATE			1		/* ILK & ENTERPRISE: Date				*/
#define	EA_GXS_SENDER		2		/* ILK & ENTERPRISE: Sender				*/
#define	EA_GXS_APRF			3		/* ILK & ENTERPRISE: APRF				*/
#define	EA_GXS_SNRF			4		/* ILK & ENTERPRISE: SNRF				*/

/*	Duplicate index names for EDS_ELIT style								*/
#define	EA_ELIT_CTL_NO		0		/* EDS_LIST: Control Number 			*/
#define	EA_ELIT_DATE		1		/* EDS_LIST: Date						*/
#define	EA_ELIT_SENDER		2		/* EDS_ELIT: Sender						*/
#define	EA_ELIT_SIZE		3		/* EDS_ELIT: Size						*/
#define	EA_ELIT_FILENAME	4		/* EDS_ELIT: ELIT Filename				*/
#define	EA_ELIT_DATASET		5		/* EDS_ELIT: Dataset name				*/

/*	Macro string used to represent 'already-received' status messages		*/
#define	ALREADY_RECEIVED_MSG		((char*)"RO")

/* ------------------------------------------------------------------------	*/
/*	Structure for managing the listing and receiving of POP messages		*/
/* ------------------------------------------------------------------------	*/
typedef struct eaPopMsgData_s eaPopMsgData_t;
struct eaPopMsgData_s {
	bool	partialMsg;				/* (EMail/AS1) True if msg is partial	*/
	int		partNumber;				/* Ordinal of partial message in set	*/
	int		totalParts;				/* Count of all partial messages in set	*/
	bool	missingParts;			/* True if partial msg set is incomplete*/
	char**	fields;					/* Array of strings EA_POP_MSG_NO, etc.	*/
};

#ifdef  __cplusplus
}
#endif

#endif	/* if !defined(__XFERDEFS_H__)	*/

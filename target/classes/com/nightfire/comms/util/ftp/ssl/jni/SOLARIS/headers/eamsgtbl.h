/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary property of	*/
/*	and confidential to bTrade.com Incorporated.							*/
/*	Disclosure outside bTrade.com is prohibited except by license agreement	*/
/*	or other confidentiality agreement.										*/
/*																			*/
/*	Copyright (c) 1998 by bTrade.com Incorporated							*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description: Macros used by TDClient Message Display sub-system			*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*			MAINTENANCE	HISTORY												*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19980321	PAA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__EA_MSGTBL_H__)
#define __EA_MSGTBL_H__

#ifdef  __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------	*/
/*	Macros used to define the severity level of a message					*/
/* ------------------------------------------------------------------------	*/
#define	EA_INFO					0	/* Inform user, then continue			*/
#define	EA_WARN					1	/* Warn user, cont on OK, exit on CANCEL*/
#define	EA_FATAL				2	/* Terminate program					*/

/* ------------------------------------------------------------------------	*/
/*	Define locations of messages in eamsgtbl.c message table.				*/
/* ------------------------------------------------------------------------	*/
#define	EA_OK						0	/* "OK"								*/
#define EA_ACTION_COMPLETED			1	/* "Action completed successfully"	*/
#define EA_ACTION_FAILED			2	/* "Action failed"					*/
#define	EA_INVALID_CMDLINE_ARG		3	/* "Invalid command-line argument	*/
#define	EA_USAGE_REQUEST			4	/* "Usage information"				*/
#define	EA_ALLOC_ERROR				5	/* "Memory allocation error"		*/
#define	EA_FILEOPEN_ERROR			6	/* "File open error"				*/
#define	EA_INVALID_FILENAME			7	/* "Invalid file name"				*/
#define	EA_BAD_NETWORK_STYLE		8	/* "No network style specified"		*/
#define	EA_NO_CURRENT_NETWORK		9	/* "No network currently selected"	*/
#define	EA_NO_NETWORKS				10	/* "No networks defined"			*/
#define	EA_NO_SECURITY_NETWORK		11	/* "No SECURITY network found"		*/
#define	EA_NO_MAINT_NETWORK			12	/* "No MAINT network found"			*/
#define	EA_PASSWORD_MISMATCH		13	/* "Passwords must match"			*/
#define EA_INVALID_NETWORK_NAME		14	/* "Invalid network name"			*/
#define	EA_INIFILE_ACCESS_ERROR		15	/* "Error accessing ini-file"		*/
#define	EA_INIFILE_CORRUPT			16	/* "Error processing ini-file"		*/
#define	EA_TRIAL_EXPIRED			17	/* "Software trial expired"			*/
#define	EA_AUDIT_NOT_AVAILABLE		18	/* "Audit Logs are not available"	*/
#define	EA_BAD_TRANSFER_DATA		19	/* "Invalid Stored Transfer data"	*/
#define	EA_DUPLICATE_TRANSFER		20	/* "Transfer already exists"		*/
#define	EA_NO_TRANSFER				21	/* "No Transfer has been specified"	*/
#define	EA_CERT_OUTSTANDING			22	/* "Outstanding certificate request"*/
#define	EA_PASSPHRASE_SELECT		23	/* "Default passphrase location"	*/
#define	EA_FTP_OBJECT_CREATE		24	/* "Creation of FTP object failed"	*/
#define	EA_FTP_CONNECT				25	/* "Failed to connect to FTP server"*/
#define	EA_FTP_LOGIN				26	/* "Logon to FTP server failed"		*/
#define	EA_FTP_SEND					27	/* "File Put to server failed"		*/
#define	EA_FTP_RECEIVE				28	/* "File Get from server failed"	*/
#define	EA_FTP_DELETE				29	/* "File Delete on server failed"	*/
#define	EA_FTP_LS					30	/* "File Listing on server failed"	*/
#define	EA_FTP_CD					31	/* "Change Dir on server failed"	*/
#define	EA_FTP_TELL					32	/* "Data query from XFER object fail*/
#define	EA_FTP_EDI_PUT				33	/* "EDI Data send with errors"		*/
#define	EA_DECOMP_FAILED			34	/* "Decompression of file failed"	*/
#define	EA_COMPRESS_FAILED			35	/* "Compression of file failed"		*/
#define	EA_DELAY_TIME				36	/* "Invalid delayed start time"		*/
#define	EA_INVALID_DATES			37	/* "Invalid start or end date"		*/
#define	EA_FILE_LIST_FAILED			38	/* "File List on Server failed"		*/
#define	EA_FILE_DOWNLOAD_FAILED		39	/* "File Download on Server failed"*/
#define	EA_FILE_DELETE_FAILED		40	/* "File Delete on Server failed"	*/
#define	EA_LOGON_PASSWD_ERROR		41	/* "Incorrect logon password"		*/
#define	EA_NO_FILES_FOUND			42	/* "No files found to be downloaded"*/
#define	EA_NO_FILES_SELECTED		43	/* "No files selected"				*/
#define	EA_RIMPORT_ERROR			44	/* "Install of runtimes failed"		*/
#define	EA_RAS_ERROR				45	/* "Windows RAS Error"				*/
#define	EA_RAS_DISABLED				46	/* "Windows RAS Error"				*/
#define	EA_ALREADY_CONNECTED		47	/* "Dial connection already exists"*/
#define	EA_ALREADY_DISCONNECTED		48	/* "No dial connection exists"		*/
#define	EA_PFX_INPUT_ERROR			49	/* "Import PFX input error"			*/
#define	EA_PFX_IMPORT_ERROR			50	/* "PFX Import failed"				*/
#define	EA_NO_HELP_AVAILABLE		51	/* "No On-line Help Available"		*/
#define	EA_ARE_YOU_SURE				52	/* "Are you sure you want to do this?"*/
#define	EA_DO_NOT_DO_THAT			53	/* "You really don't want to do that!"*/
#define	EA_BAD_BASEIN_MSG_FILE		54	/* "Error opening basein.msg file"	*/
#define	EA_INTERNAL_ERROR			55	/* "An internal error has occurred"	*/
#define EA_RESTART_FILE_FOUND		56	/* "TDClient Restart file found"	*/
#define EA_BAD_CONTROL_PORT			57	/* "Invalid Control Port			*/
#define	EA_TRANSFER_PREEMPTED		58	/* "Specified transfer pre-empted"	*/
#define	EA_GENKEYS_ERROR			59	/* "Error creating certificate reque*/
#define	EA_PRE_PROC_FAILED			60	/* "Transfer pre-processing failed"	*/
#define	EA_POST_PROC_FAILED			61	/* "Failed to load DLL"				*/
#define	EA_LOAD_DLL_ERROR			62	/* "Database Initialization Error"	*/
#define	EA_DB_INIT_ERROR			63	/* "Database Initialization Error"	*/
#define	EA_DB_OPEN_ERROR			64	/* "Database Open Error"			*/
#define	EA_DB_CREATE_DISP_ERROR		65	/* "Failed to create Disp record"	*/
#define	EA_MDN_NOT_RECEIVED			66	/* "File transferred, but MDN not re*/
#define	EA_DUP_MDN_RECEIVED			67	/* "Duplicate MDN received"			*/
#define	EA_DUP_MSG_RECEIVED			68	/* "Duplicate Message received"		*/
#define	EA_BAD_LOGLEVEL				69	/* "Invalid Log Level"				*/
#define	EA_UNKNOWN_ERROR			70	/* "An unknown error has occurred"	*/
#define	NUM_EA_MESSAGES				EA_UNKNOWN_ERROR + 1

/* ------------------------------------------------------------------------	*/
/*	Define structure of an entry in the eaMessages table					*/
/* ------------------------------------------------------------------------	*/
typedef struct eaMessage_s eaMessage_t;

struct eaMessage_s
{
	int			msgLevel;			/* EA_FATAL, EA_WARN, EA_INFO			*/
	const char*	msg;				/* Text of message						*/
};

/* ------------------------------------------------------------------------	*/
/*	Make static message table available globally							*/
/* ------------------------------------------------------------------------	*/
extern eaMessage_t eaMessageTbl[NUM_EA_MESSAGES+1];

#ifdef  __cplusplus
}
#endif

#endif	/* if !defined(__EA_MSGTBL_H__)	*/

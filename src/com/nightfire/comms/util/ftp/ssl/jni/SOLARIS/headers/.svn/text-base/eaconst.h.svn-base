/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary				*/
/*	property of	and confidential to bTrade.com Incorporated.				*/
/*	Disclosure outside bTrade.com is prohibited except by					*/
/*	license agreement or other confidentiality agreement.					*/
/*																			*/
/*	Copyright (c) 1999 by bTrade.com Incorporated							*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description: Miscellaneous constants and structures used by TDClient	*/
/* ------------------------------------------------------------------------	*/
/*				MAINTENANCE	HISTORY											*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19980321	PAA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__EACONST_H__)
#define __EACONST_H__

#if defined(__cplusplus)
extern "C" {
#endif

/* ------------------------------------------------------------------------	*/
/*	Must specify program type: EA_ZAF, EA_EXE, EA_LIB, or EA_DLL			*/
/* ------------------------------------------------------------------------	*/
#if !defined(EA_ZAF) && !defined(EA_EXE) && !defined(EA_LIB) && !defined(EA_DLL)
#error "Must specify program type (EA_ZAF, EA_EXE, EA_LIB, or EA_DLL)"
#endif

/* ------------------------------------------------------------------------	*/
/*	Must specify platform type: EA_DOS, EA_WIN16, EA_WIN32, EA_AIX42, etc.	*/
/* ------------------------------------------------------------------------	*/
#if !defined(EA_DOS) && !defined(EA_WIN16) && !defined(EA_WIN32)
#if !defined(EA_AIX42) && !defined(EA_HPUX10) && !defined(EA_HPUX11)
#if !defined(EA_SOLARIS251)
#if !defined(EA_OPENVMS71) && !defined(EA_DECUNIX) && !defined(EA_SCOUNIX)
#if !defined(EA_MVS) && !defined(EA_OS400) && !defined(EA_OPENVMS71)
#error "Must specify target platform (EA_DOS, EA_WIN32, EA_AIX42, EA_MVS, etc.)"
#endif
#endif
#endif
#endif
#endif

/* ------------------------------------------------------------------------	*/
/*	Convenience macro for Unix and OpenVMS platforms						*/
/* ------------------------------------------------------------------------	*/
#if !defined(EA_UNIX)
#if defined(EA_AIX42) || defined(EA_HPUX10) || defined(EA_HPUX11) 
#define	EA_UNIX	1
#elif defined(EA_SOLARIS251)
#define	EA_UNIX	1
#elif defined(EA_OPENVMS71) || defined(EA_DECUNIX) || defined(EA_SCOUNIX)
#define	EA_UNIX	1
#endif
#endif

/* ------------------------------------------------------------------------	*/
/*	Macros used for managing print-strings									*/
/* ------------------------------------------------------------------------	*/
#define	EAMAX(a, b)			(((long)(a)) > ((long)(b)) ? (a) : (b))
#define	EAMIN(a, b)			(((long)(a)) < ((long)(b)) ? (a) : (b))
#define	EAABS(a)			(((long)(a)) < 0 ? (-a) : (a))

#define BLANK_STRING (char*)"                                              "
#define	TRUE_STRING	 (char*)"True"
#define	FALSE_STRING (char*)"False"
#define	NULL_STRING	 (char*)"<Null>"
#define	NONE_STRING	 (char*)"<None>"
#define	HAS_VALUE(a)		((a)  && strlen(a))
#define	HAS_NON_BLANK_VALUE(a)	((a) && strlen(a) && \
								memcmp(a, BLANK_STRING, EAMIN(strlen(a), \
								strlen(BLANK_STRING))))
#define	IS_NULL(a)			((a) ? (a) : (NULL_STRING))
#define	IS_BLANK(a)			((a) ? (a) : ((char*)" "))
#define	IS_EMPTY(a)			((a) ? (a) : ((char*)""))
#define	IS_DOT(a)			(HAS_VALUE(a) ? (a) : ((char*)"."))
#define	IS_TRUE(a)			((a) ? (TRUE_STRING) : (FALSE_STRING))
#define	IS_Y_OR_N(a)		((a) ? ((char*)"Y") : ((char*)"N"))
#define	STRING_TO_BOOL(a) ((a) ? (toupper((a)[0])=='Y' ? true : false) : false)

/* ------------------------------------------------------------------------ */
/*	Define success and failure used return codes (used by all XFER methods)	*/
/* ------------------------------------------------------------------------ */
#define XFER_SUCCESS	1				/* if (xfer*()) evaluates to true	*/
#define XFER_FAILURE	0				/* if (xfer()) evaluates to false	*/

/* ------------------------------------------------------------------------	*/
/*	Generic stuff															*/
/* ------------------------------------------------------------------------	*/
#define	EXFER_INI					1
#define	TDCLIENT_INI				2
#define	TP_BOOK_INI					3

#define	SEND_TYPE					1
#define	RECEIVE_TYPE				2

#define	RANDOM_INIT					1
#define	RANDOM_UPDATE				2

/* ------------------------------------------------------------------------	*/
/*	Macros for remembering those cryptic access() mode values				*/
/* ------------------------------------------------------------------------	*/
#define	EA_FILE_EXISTS				00
#define	EA_FILE_IS_WRITABLE			02
#define	EA_FILE_IS_READABLE			04

/* ------------------------------------------------------------------------	*/
/* ------------------------------------------------------------------------	*/
#define	EXFER_TRANS					'Y'		/* For multifile flag in control*/
#define	BEXFER_TRANS				'B'		/* For multifile flag in control*/
#define	ADHOC_TRANS					'N'		/* For multifile flag in control*/

#define	ADHOC_XFER_NAME				"ADHOC TRANSFER"
#define	MAINT_XFER_NAME				"RECEIVE MAINTENANCE"
#define	QUERY_DOWNLOAD_XFER_NAME	"QUERY DOWNLOAD"
#define	QUERY_DELETE_XFER_NAME		"QUERY DELETE"
#define	SENDCERTREQ_XFER_NAME		"SEND CERT REQ"
#define	RECEIVECERT_XFER_NAME		"RECEIVE RUNTIMES"
#define	SEND_NOTIFY_XFER_NAME		"SEND TDCLIENT NOTIFICATION"

/* ------------------------------------------------------------------------	*/
/*	IGN-specific transfer constant (used for SendEdi)						*/
/* ------------------------------------------------------------------------	*/
#define USE_DEFAULT_ALIAS_TABLE_STRING	((char*)"Use default alias table")

/* ------------------------------------------------------------------------	*/
/*	Flags used to specify the type of return-code checking for transfer		*/
/*	pre- and post-processing												*/
/* ------------------------------------------------------------------------	*/
#define	TRANSFER_PROC_IGNORE		'I'
#define	TRANSFER_PROC_SUCCESS		'S'
#define	TRANSFER_PROC_FAILURE		'F'

/* ------------------------------------------------------------------------	*/
/*	Currently used in building and validating transfer data					*/
/* ------------------------------------------------------------------------	*/
#define	EDIT_FAILURE				-2
#define	EDIT_WARNING				-1
#define	EDIT_OK						0
#define	BAD_TRANSFER_NAME			201
#define	BAD_TRANSFER_LOGON			202
#define	BAD_TRANSFER_FILES			203
#define	BAD_TRANSFER_CLASS			204
#define	BAD_TRANSFER_USERID			205
#define	BAD_PERPETUAL_TRANSFER		206
#define	BAD_TRANSFER_PARMS			207

/* ------------------------------------------------------------------------	*/
/*	Macros used to manage dial connections (EA_WIN32 only)					*/
/* ------------------------------------------------------------------------	*/
#define	EA_DIAL_CONNECTING			1
#define	EA_DIAL_CONNECTED			2
#define	EA_DIAL_DISCONNECTING		3
#define	EA_DIAL_ERROR				4
#define	EA_DIAL_DISCONNECTED		5

/* ------------------------------------------------------------------------	*/
/*	File which tells thread to stop											*/
/* ------------------------------------------------------------------------	*/
#define	EA_CANCEL_FILE				"cancel.fil"

/* ------------------------------------------------------------------------	*/
/*	Sizing macros															*/
/* ------------------------------------------------------------------------	*/
#define	BUF_16_SZ					16
#define	BUF_32_SZ					32
#define	BUF_64_SZ					64
#define	BUF_80_SZ					80
#define	BUF_128_SZ					128
#define	BUF_256_SZ					256
#define	BUF_512_SZ					512
#define	BUF_1024_SZ					1024
#define	BUF_2048_SZ					2048
#define	BUF_4096_SZ					4096
#define	BUF_8192_SZ					8192
#define	BUF_16384_SZ				16384
#define	BUF_32768_SZ				32768
#define	BUF_65536_SZ				65536

/* ------------------------------------------------------------------------	*/
/*	Macros for firewall types												*/
/* ------------------------------------------------------------------------	*/
#define FIREWALL_BAD_TYPE			-1
#define FIREWALL_NONE_TYPE			0							/* "<None>"	*/
#define FIREWALL_SOCKS_TYPE			1							/* "SOCKS"	*/
#define FIREWALL_PROXY_TYPE			2							/* "PROXY"	*/
#define FIREWALL_NAT_TYPE			3							/* "NAT'ed"	*/
#define NUM_FIREWALL_TYPES			(FIREWALL_NAT_TYPE + 1)


#if defined(__cplusplus)
}
#endif	/* if defined(__cplusplus) */

#endif	/* if !defined(__EACONST_H__) */

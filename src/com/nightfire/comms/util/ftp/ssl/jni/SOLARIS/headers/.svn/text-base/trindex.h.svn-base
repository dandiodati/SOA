/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary property of	*/
/*	and confidential to bTrade, Incorporated.								*/
/*	Disclosure outside bTrade,Inc. is prohibited except by license agreement*/
/*	or other confidentiality agreement.										*/
/*																			*/
/*	Copyright (c) 1997 by bTrade, Incorporated								*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description:															*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/* MAINTENANCE HISTORY														*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19990106	PA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__TRINDEX_H__)
#	define	__TRINDEX_H__

/* ------------------------------------------------------------------------	*/
/*	Field location of various data in the transindex						*/
/* ------------------------------------------------------------------------	*/
#define TR_VERSION_LOC			0	/* EA version; e.g. 62.01.00.05			*/
#define TR_SENDER_LOC			1	/* FTP Server determined (active user)	*/
#define TR_RECVR_LOC			2	/* Sendto userId/Mailbox (destination)	*/
#define TR_CLASS_LOC			3	/* APRF or Message Class				*/
#define TR_FORMAT_LOC			4	/* E = EDI or U = Unformatted			*/
#define TR_SYSTYPE_LOC			5	/* W95, AIX, SUN, OS400, MVS, etc.		*/
#define TR_DESCRIP_LOC			6	/* Stored transfer name, AdHoc name, etc*/
#define TR_ORIGFILENAME_LOC		7	/* Send fileName (path not included)	*/
#define TR_CPVERSION_LOC		8	/* Compress version, e.g. 301.nn, 400.nn*/
#define TR_CPPARMS_LOC			9	/* Blank or SendParms entries abbrev C*3*/

/* Provided by EAFTP Server on PUT:											*/
#define TR_SESSIONID_LOC		10	/* EAFTP generated session ID			*/
#define TR_USERID_LOC			11	/* UserId of user logged in for session	*/
#define TR_UNIQUEFILENAME_LOC	12	/* EAFTP generated unique filename		*/
#define TR_PUTDATETIME_LOC		13	/* EAFTP generated (ctime)				*/
#define TR_PUTDURATION_LOC		14	/* EAFTP generated						*/
#define TR_FILESIZE_LOC			15	/* EAFTP determined						*/
#define TR_HOSTNAME_LOC			16	/* EAFTP determined						*/
#define TR_CHARFORMAT_LOC		17	/* A = ASCII, I = Binary				*/
#define TR_STATUS_LOC			18	/* Intransit, available [received,		*/
									/* accepted, rejected]					*/
#define TR_GETDATETIME_LOC		19	/* EAFTP generated (ctime)				*/
#define TR_GETDURATION_LOC		20	/* EAFTP generated						*/

#define TR_DESCRIP2_LOC			21	/* Description2 field (GS for EDI data)	*/
#define TR_DESCRIP3_LOC			22	/* Description3 field (ST for EDI data)	*/

#define NUM_TRINDEX_ENTRIES		TR_DESCRIP3_LOC + 1		/* Count of fields	*/

/* ------------------------------------------------------------------------	*/
/*	Sizing macros for the EAFTP transindex fields							*/
/* ------------------------------------------------------------------------	*/
/* Provided by TDClient on PUT:											*/
#define TR_VERSION_SZ			8	/* EA version; e.g. 62.01.00.05			*/
#define TR_SENDER_SZ			16	/* FTP Server determined (active user)	*/
#define TR_RECVR_SZ				16	/* Sendto userId/Mailbox (destination)	*/
#define TR_CLASS_SZ				16	/* APRF or Message Class				*/
#define TR_FORMAT_SZ			1	/* E = EDI or U = Unformatted			*/
#define TR_SYSTYPE_SZ			8	/* W95, AIX, SUN, OS400, MVS, etc.		*/
#define TR_DESCRIP_SZ			106	/* Stored transfer name, AdHoc name, etc*/
#define TR_ORIGFILENAME_SZ		128	/* Send fileName (path not included)	*/
#define TR_CPVERSION_SZ			6	/* Compress version, e.g. 301.nn, 400.nn*/
#define TR_CPPARMS_SZ			21	/* Blank or SendParms entries abbrev C*3*/

/* Provided by EAFTP Server on PUT:											*/
#define TR_SESSIONID_SZ			4	/* EAFTP generated session ID			*/
#define TR_USERID_SZ			16	/* UserId of user logged in for session	*/
#define TR_UNIQUEFILENAME_SZ	15	/* EAFTP generated unique filename		*/
#define TR_PUTDATETIME_SZ		4	/* EAFTP generated (ctime)				*/
#define TR_PUTDURATION_SZ		4	/* EAFTP generated						*/
#define TR_FILESIZE_SZ			4	/* EAFTP determined						*/
#define TR_HOSTNAME_SZ			24	/* EAFTP determined						*/
#define TR_CHARFORMAT_SZ		1	/* A = ASCII, B = Binary				*/
#define TR_STATUS_SZ			2	/* Intransit, available [received,		*/
									/* accepted, rejected]					*/
#define TR_GETDATETIME_SZ		4	/* EAFTP generated (ctime)				*/
#define TR_GETDURATION_SZ		4	/* EAFTP generated						*/

#define TR_DESCRIP2_SZ			128	/* Description2 field (ST for EDI data)	*/
#define TR_DESCRIP3_SZ			128	/* Description3 field (ST for EDI data)	*/

#	endif	/* if !defined(__TRINDEX_H__) */


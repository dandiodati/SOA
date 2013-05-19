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
/*	Description: definition of nettbl - stores known networks in a table	*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*			MAINTENANCE	HISTORY												*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19980321	PAA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#if !defined(__NETTBL_H__)
#define	__NETTBL_H__

/* ------------------------------------------------------------------------	*/
/*	Specify "C" linkage; initbl.c code defining the ini table is "C" only.	*/
/* ------------------------------------------------------------------------	*/
#ifdef  __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------	*/
/*	Known Network Styles													*/
/* ------------------------------------------------------------------------	*/
#define	BAD_STYLE					(-1)	/* Bad or missing network style	*/
#define	GENERIC_STYLE				0
#define	GENERIC_DOS_STYLE			1
#define	GENERIC_MVS_STYLE			2
#define	GENERIC_SSL_STYLE			3
#define	IGN_STYLE					4
#define	IFX_STYLE					5		/* NOT SUPPORTED IN XFERDLL !!!	*/
#define	FEDEX_STYLE					6
#define	EDS_STYLE					7		/* NOT SUPPORTED IN XFERDLL !!!	*/
#define	WALMART_STYLE				8
#define	EAFTP_STYLE					9
#define	CPFTP_STYLE					10
#define	GEIS_MARKIII_STYLE			11
#define	GEIS_EXPRESS_STYLE			12
#define	GEIS_SWITCH_STYLE			13
#define	CONNECTMAIL_STYLE			14
#define	STERLING_COMMERCE_STYLE		15
#define	MCI_EDINET_STYLE			16
#define	EMAIL_STYLE					17
#define	GISB_CLIENT_STYLE			18
#define	GISB_SERVER_STYLE			19
#define	ICCNET_STYLE				20
#define	LOCAL_ARCHIVE_STYLE			21
#define	SMIME_STYLE					22
#define	AS1_STYLE					23
#define	AS2_STYLE					24
#define GEIS_OPENNET_STYLE      	25
#define	GEIS_ILK_STYLE				26
#define	GEIS_ENTERPRISE_STYLE		27
#define	EDS_ELIT_STYLE				28
#define	STERLING_ENTERPRISE_STYLE	29
#define	NUM_NETWORK_STYLES		(STERLING_ENTERPRISE_STYLE + 1)

/* ------------------------------------------------------------------------ */
/*	Known Network Sub-Styles												*/
/* ------------------------------------------------------------------------ */
#define BAD_SUBSTYLE			(-1)
#define BTRADE_SUBSTYLE			0
#define QRS_SUBSTYLE			1
#define GXS_SUBSTYLE			2
#define EDS_SUBSTYLE			3
#define NUM_NETWORK_SUBSTYLES	(EDS_SUBSTYLE + 1)

/* ------------------------------------------------------------------------	*/
/*  Structure to store an known-network entry in the netTbl					*/
/* ------------------------------------------------------------------------	*/
typedef	struct netTbl_s	netTbl_t;
struct netTbl_s
{
	int				style;			/* Known FTP style supported by XFER	*/
	const char*		name;			/* Used for matching ini-file entry		*/
	int				updatePasswd;	/* True if can update FTP Server passwd	*/
	int				hasQuery;		/* True if network has Query function	*/
	int				hasAudit;		/* True if network has Audit function	*/
};

/* ------------------------------------------------------------------------ */
/*	Structure to store an known-network sub-style entry in the subNetTbl	*/
/* ------------------------------------------------------------------------ */
typedef struct subNetTbl_s	subNetTbl_t;
struct subNetTbl_s
{
	int				subStyle;		/* Known sub-style supported by EA/XFER */
	const char*		name;			/* Used for matching ini-file entry		*/
};

extern netTbl_t networkStylesTbl[NUM_NETWORK_STYLES+1];
extern subNetTbl_t networkSubStylesTbl[NUM_NETWORK_SUBSTYLES+1];

#ifdef  __cplusplus
}
#endif

#endif	/* #if !defined(__NETTBL_H__)	*/

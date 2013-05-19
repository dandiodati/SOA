/* -------------------------------------------------------------------------- */
/*                                                                            */
/*  This document contains material which is the proprietary property of and  */
/*  confidential to bTrade.com Incorporated.                                  */
/*  Disclosure outside bTrade.com is prohibited except by license agreement   */
/*  or other confidentiality agreement.                                       */
/*                                                                            */
/*  Copyright (c) 1998 by bTrade.com Incorporated                             */
/*  All Rights Reserved                                                       */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Description: All data types used throughout EA code (except ZAF objects)  */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*          MAINTENANCE HISTORY                                               */
/*                                                                            */
/* DATE     BY  BUG NO. DESCRIPTION                                           */
/* -------- --- ------- ----------------------------------------------------- */
/* 19980321 PAA         Initial version                                       */
/* -------------------------------------------------------------------------- */
#if !defined(__EATYPES_H__)
#define __EATYPES_H__

#if defined(__cplusplus)
extern "C" {
#endif

/* ------------------------------------------------------------------------ */
/*  Define logging-detail levels                                            */
/* ------------------------------------------------------------------------ */
typedef enum logLevel_s
{
    NoLogging   = 0,                /* Disables logging                     */
    LogFatal    = 1,                /* Only show FATAL error messages       */
    LogError    = 2,
    LogWarn     = 3,                /* Default setting                      */
    LogInfo     = 4,
    LogTrace    = 5,
    LogDebug    = 6                 /* Show all messages                    */
}   logLevel_t;

/* ------------------------------------------------------------------------ */
/*  EAAPI internal data types                                                 */
/* -------------------------------------------------------------------------- */
typedef struct cf_s cf_t;                       /* Used in cf objects (cf)    */
typedef struct control_s        control_t;      /* ini struct table (initblio)*/
typedef struct databaseSetup_s  databaseSetup_t;/* ini struct table (initblio)*/
typedef struct ediintSetup_s    ediintSetup_t;  /* ini struct table (initblio)*/
typedef struct emailSetup_s     emailSetup_t;   /* ini struct table (initblio)*/
typedef struct firewallSetup_s  firewallSetup_t;/* ini struct table (initblio)*/
typedef struct support_s        support_t;      /* ini struct table (initblio)*/
typedef struct delay_s          delay_t;        /* ini struct table (initblio)*/
typedef struct eaArgs_s         eaArgs_t;       /* Cmd-line arg data (eaargs) */
typedef struct eaDialConn_s     eaDialConn_t;   /* Data for one dial connectn */
typedef struct eaDialConnList_s eaDialConnList_t;   /* Current dial connectns */
typedef struct eaDialEntry_s    eaDialEntry_t;  /* Data for one dialer entry  */
typedef struct eaDialEntryList_s    eaDialEntryList_t;  /* Dialer entries list*/
typedef struct eaFileViewer_s   eaFileViewer_t; /* Assoc. of ext and program  */
typedef struct eaFileViewing_s  eaFileViewing_t;/* List of viewers and flags  */
typedef struct eaMib_s          eaMib_t;        /* The main guy     (eamib)   */
typedef struct eaMessage_s      eaMessage_t;    /* User-messages    (eamsg)   */
typedef struct eapath_s         eapath_t;       /* ini struct table (initblio)*/
/*typedef struct eaVersionData_s eaVersionData_t;   Vers info about EA exe */
typedef struct eaWCStruct_s     eaWCStruct_t;   /* For wild-card enumeration  */
typedef struct iniTbl_s         iniTbl_t;       /* Ini table        (initblio)*/
typedef struct ini_s            ini_t;          /* ini struct table (initblio)*/
typedef union iniTblIntData_u   iniTblIntData_t;/* For iniTbl_t     (initblio)*/
/*typedef enum logLevel_s           logLevel_t;     Used in cf objects (cf)*/
typedef struct maint_s          maint_t;        /* ini struct table (initblio)*/
typedef struct nameList_s       nameList_t;     /* ini struct table (initblio)*/
typedef struct namePair_s       namePair_t;     /* ini struct table (initblio)*/
typedef struct network_s        network_t;      /* ini struct table (initblio)*/
typedef struct receiveParms_s   receiveParms_t; /* ini struct table (initblio)*/
typedef struct registration_s   registration_t; /* ini struct table (initblio)*/
typedef struct security_s       security_t;     /* ini struct table (initblio)*/
typedef struct sendParms_s      sendParms_t;    /* ini struct table (initblio)*/
typedef struct sortIndex_s      sortIndex_t;    /* Struct for qsort'ing       */
typedef struct statusContext_s  statusContext_t;/* Maintains status for xfer  */

/*
  tradingPartner_t has been moved to commfunc.h
  typedef struct tradingPartner_s   tradingPartner_t;  Trading partner data
*/

typedef struct transfer_s       transfer_t;     /* ini struct table (initblio)*/
typedef struct xfer_s           xfer_t;         /* ini struct table (initblio)*/
typedef struct xferContext_s    xferContext_t;  /* ini struct table (initblio)*/
typedef struct xferParms_s      xferParms_t;    /* ini struct table (initblio)*/

/*typedef class EA_DATABASE     eaDb_t;         Table database  */

/* ------------------------------------------------------------------------ */
/*  Struct to pass data into and out of child threads                       */
/* ------------------------------------------------------------------------ */
typedef struct threadData_s     threadData_t;

/* ------------------------------------------------------------------------ */
/*  Structs for EAAPI Message types (CANCEL_MSG has no sub-structure)       */
/* ------------------------------------------------------------------------ */
typedef struct eaMsg_s              eaMsg_t;
typedef struct msgBoxMsg_s          msgBoxMsg_t;
typedef struct textMsg_s            textMsg_t;
typedef struct logFileMsg_s         logFileMsg_t;
typedef struct progressMsg_s        progressMsg_t;
typedef struct debugMsg_s           debugMsg_t;
typedef struct putStatusMsg_s       putStatusMsg_t;
typedef struct getFileSpecMsg_s     getFileSpecMsg_t;
typedef struct getUserInputMsg_s    getUserInputMsg_t;
typedef struct processingErrorMsg_s processingErrorMsg_t;
typedef struct viewFileMsg_s        viewFileMsg_t;

/*  Used by EA_LIST_WINDOW: */
typedef struct eaListFormatField_s  eaListFormatField_t;
typedef struct eaListFormat_s       eaListFormat_t;
typedef struct eaListDataField_s    eaListDataField_t;
typedef struct eaListData_s         eaListData_t;
typedef struct eaListDisplayData_s  eaListDisplayData_t;

#if defined(__cplusplus)
}
#endif  /* if defined(__cplusplus) */

#endif  /* if !defined(__EATYPES_H__) */

/* ------------------------------------------------------------------------ */
/*                                                                          */
/*  This document contains material which is the proprietary                */
/*  property of and confidential to bTrade.com Incorporated.                */
/*  Disclosure outside bTrade.com is prohibited except by                   */
/*  license agreement or other confidentiality agreement.                   */
/*                                                                          */
/*  Copyright (c) 1999 by bTrade.com Incorporated                           */
/*  All Rights Reserved                                                     */
/*                                                                          */
/* ------------------------------------------------------------------------ */
/*  Description: Platform-specific headers and macros                       */
/*                                                                          */
/* ------------------------------------------------------------------------ */
/*                  MAINTENANCE HISTORY                                     */
/*                                                                          */
/* DATE     BY  BUG NO. DESCRIPTION                                         */
/* -------- --- ------- --------------------------------------------------- */
/* 19990325 PAA         Initial version                                     */
/* ------------------------------------------------------------------------ */
#if !defined(__EAPLTFRM_H__)
#define __EAPLTFRM_H__

#if defined(__cplusplus)
extern "C" {
#endif

/* #include "aglobal.h" */              /* For UINT                         */
/* #undef BIG_ENDIAN */
#include "eaconst.h"                    /* For EA_UNIX                      */

/* ------------------------------------------------------------------------ */
/*  32-bit Windows (Windows 95, Windows NT)                                 */
/* ------------------------------------------------------------------------ */
#if defined(EA_WIN32) || defined(EA_WINNT)
#   include <windows.h>
#   include <process.h>                 /* For _beginthread()               */
#   include <io.h>
#   include <ras.h>                     /* For dynamic load of DLL          */
#   include <errno.h>
#if !defined(__cplusplus)
    typedef char    bool;               /* MSVC++ 5.0- uses 1-byte bools... */
#   define  false   0                   /* and since 'C' has no false       */
#   define  true    1                   /* and since 'C' has no true        */
#endif
#   define  ACCESS  _access
#   define  CPSTAT  _stat
#   define  STRUPR  _strupr
    typedef long            SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned long   UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */

/* ------------------------------------------------------------------------ */
/*  All flavours of UNIX (includes DEC's OpenVMS, even though it's not UNIX)*/
/* ------------------------------------------------------------------------ */
#elif defined(EA_UNIX)
#   include <unistd.h>
#   include <errno.h>
#   include <dirent.h>
#   define  ACCESS  access
#   if defined(AIX42) && defined(_LARGE_FILES)
#       define  CPSTAT  stat64
#   else
#       define  CPSTAT  stat
#   endif
#   define  STRUPR  eaStrUpr
#   define  TRIM    0177
#   if !defined(true)
#       define  false   0
#       define  true    1
#   endif

#   if defined(EA_DECUNIX) || defined(MACHINE64)    /* 64-bit machine       */
    typedef int             SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned int    UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */
#   else
    typedef long            SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned long   UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */
#   endif

/* HPUX10 (GNU) has native bool type under C++, but not under 'C'           */
/* DECUNIX has native bool type under C++                                   */
/* Use EA_SOLARIS251 for the sparc4;                                        */
/* use EA_SOLARIS251 and EA_SOLARIS26 for the sun250,                       */
/* to get the typedef below correct (sparc4 needs bool, solar250 does not   */
#   if !defined(EA_DECUNIX) && !defined(EA_HPUX10) && !defined(EA_SOLARIS26)
#   	if !defined(EA_HPUX11) && !defined(EA_AIX42)
    		typedef int bool;           /* Since 'C' has no bool...         */
#		elif !defined(__cplusplus)
    		typedef int bool;           /* Since 'C' has no bool...         */
#		endif
#   elif !defined(__cplusplus)
    	typedef int bool;               /* Since 'C' has no bool...         */
#   endif

#   if defined(EA_SOLARIS251)           /* usleep is missing from unistd.h! */
        int         usleep(unsigned int useconds);
        unsigned    sleep(unsigned useconds);
#   endif

/* ------------------------------------------------------------------------ */
/*  MVS                                                                     */
/* ------------------------------------------------------------------------ */
#elif defined(EA_MVS)
#   define  CPSTAT  stat
#   define  STRUPR  eaStrUpr
#   define  TRIM    0177
#   define  ACCESS  access
#   include <errno.h>
#   include <stdio.h>
#   if !defined(true)
#       define  false   0
#       define  true    1
#   endif
/*    typedef int bool; */
    typedef long            SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned long   UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */

/* ------------------------------------------------------------------------ */
/*  AS 400                                                                  */
/* ------------------------------------------------------------------------ */
#elif defined(EA_OS400)
/* PAA 8/29/2001: Commented out next 6 lines while porting EA 1.46 changes */
/* #   include <sys/stat.h>     */
/* #   include <fcntl.h>        */
/* #   include <ctype.h>        */
/* #   include <unistd.h>       */
/* #   include <errno.h>        */
/* #   define  S_IFDIR _S_IFDIR */
#   define  ACCESS  access
#   define  CPSTAT  stat
#   define  STRUPR  eaStrUpr
#   define  TRIM    0177
#   include <errno.h>
/*#   if !defined(true) */
/*#       define  false   0 */
/*#       define  true    1 */
/*#   endif */
    typedef int bool;
    typedef long            SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned long   UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */

/* ------------------------------------------------------------------------ */
/*  16-bit Windows (Windows 3.1, etc.)                                      */
/* ------------------------------------------------------------------------ */
#elif defined(EA_WIN16)
#   include <io.h>
#   include <errno.h>
#   define  ACCESS  access
#   define  CPSTAT  stat
#   define  STRUPR strupr
    typedef long            SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned long   UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */

/* ------------------------------------------------------------------------ */
/*  DOS                                                                     */
/* ------------------------------------------------------------------------ */
#elif defined(EA_DOS)
#   include <dos.h>
#   include <io.h>
#   include <errno.h>
#   include <process.h>
#   include <stdio.h>
#   define  ACCESS  access
#   define  CPSTAT  stat
#   define  STRUPR strupr
    typedef long            SINT4;      /* 4-byte integer                   */
#   if !defined(_AGLOBAL_H_)
    typedef unsigned long   UINT4;      /* 4-byte unsigned integer          */
#   endif /* if !defined(_AGLOBAL_H_) */

#endif

/* ------------------------------------------------------------------------ */
/*  Filenames for MVS                                                       */
/* ------------------------------------------------------------------------ */
#if defined(EA_MVS)
#define EAINI       "DD:EASYACC"
#define EXFERINI    "DD:EXFER"
#define BEXFERINI   "DD:BEXFER"
#define TPADDRSSINI "DD:TPADDRSS"
#define ALIASTBL    "DD:ALIAS"
#define AUDITLOG    "DD:AUDITLOG"
#define CERTFILE    "DD:PUBLKEYS"
#define CERTREQ     ""
#define CPLOOKUP    "DD:CPLOOKUP"
#define EALOG       "DD:EALOG"
#define EAINILOG    "DD:EAINILOG"
#define EAMEMLOG    "DD:EAMEMLOG"
#define EXPORTRTM   ""
#define NEWPRIV     ""
#define PRIVFILE    "DD:PRIVKEYS"
#define PRIVKEY     ""
#define SECFILE     "DD:SECFILE"
#define OUTMSG      "DD:OUTMSG"
#define COMPLOG     "DD:COMPLOG"
#define DCMPLOG     "DD:DCMPLOG"
#define INDIR       ""
#define OUTDIR      ""
#define RUNDIR      ""
#define SECDIR      ""
#define TEMPDIR     ""
#define ERRORDIR    ""
#define EACOMMLOG   "DD:EAFTPLOG"
#define EAXFERLOG   "DD:EXFERLOG"
#define IEBASELOG   "DD:IEBASELG"
#define SENDEDILOG  "DD:SEDILOG"
#define INPRO       "DD:INPRO"
#define INMSG       "DD:INMSG"
#define DESTFILE    "DD:WORK01"
#define QUOTEFILE   "DD:WORK02"
#define RCVRPT      "DD:WORK01"
#define SNDRPT      "DD:WORK02"
#define GERCVRPT    "DD:WORK01"
#define GESNDRPT    "DD:WORK02"
#define TEMPFILE    "DD:WORK01"
#define TEMPLIST    "DD:WORK03"
#define LISTFILE    "DD:WORK02"
#define STATLIST    "DD:WORK04"

#define IGNDIR      ""
#define MAINTDIR    ""
#define EASTATFILE  "DD:EASTATUS"
#define EALICENSE   "DD:EALICENS"
#define RESPLOG     "DD:RESPLOG"
#define TMPHDRFIL   "DD:WORK03"
/* #define INTERMSGFIL "DD:WORK02" */
/* #define ORIGCMPFIL  "DD:WORK02" */
#define SMTPMSG     (char*)"smtpmsg.fil"
#define SMTPHDR     (char*)"smtphdr.fil"
#define SMTPBODY    (char*)"smtpbody.fil"
#define TMPMDN      (char*)"tmpmdn.fil"
#define RESPTXT     (char*)"response.txt"     
#define FULLRESPTXT (char*)"full_response.txt"
/*#define SMTPMSG     "DD:SMWORK" */
/*#define SMTPHDR     "DD:SMWORK" */
/*#define SMTPBODY    "DD:SMWORK" */
/*#define TMPMSGSAV   "DD:SMWORK" */
/*#define RESPTXT     "DD:RESPTEXT" */
/*#define FULLRESPTXT "DD:FRESPTXT" */
#define EASMIME_LOGFILE_NAME	"DD:SMLOG"

/* ------------------------------------------------------------------------ */
/*  Filenames for OS400                                                     */
/* ------------------------------------------------------------------------ */
#elif defined(EA_OS400)
#define EAINI       "EASYACC"
#define EXFERINI    "EXFER"
#define BEXFERINI   "BEXFER"
#define TPADDRSSINI "TPADDRSS"
#define ALIASTBL    "ALIAS"
#define AUDITLOG    "*CURLIB/AUDITLOG"
#define CERTFILE    "CERT"
#define CERTREQ     ""
#define CPLOOKUP    "CPLOOKUP"
#define EALOG       "*CURLIB/EALOG"
#define EAINILOG    "*CURLIB/EAINILOG"
#define EAMEMLOG    "*CURLIB/EAMEMLOG"
#define EXPORTRTM   ""
#define NEWPRIV     ""
#define PRIVFILE    "PRIVATE"
#define PRIVKEY     ""
#define SECFILE     "*CURLIB/SECFILE"
#define OUTMSG      "*CURLIB/OUTMSG"
#define COMPLOG     "*CURLIB/COMPLOG"
#define DCMPLOG     "*CURLIB/DCMPLOG"
#define INDIR       ""
#define OUTDIR      ""
#define RUNDIR      ""
#define SECDIR      ""
#define TEMPDIR     ""
#define ERRORDIR    ""
#define EACOMMLOG   "*CURLIB/EAFTPLOG"
#define EAXFERLOG   "*CURLIB/EXFERLOG"
#define IEBASELOG   "*CURLIB/IEBASELG"
#define SENDEDILOG  "*CURLIB/SEDILOG"
#define INPRO       "INPRO"
#define INMSG       "INMSG"
#define DESTFILE    "*CURLIB/SYSUT1"
#define QUOTEFILE   "*CURLIB/SYSUT2"
#define RCVRPT      "*CURLIB/SYSUT1"
#define SNDRPT      "*CURLIB/SYSUT2"
#define GERCVRPT    "*CURLIB/SYSUT1"
#define GESNDRPT    "*CURLIB/SYSUT2"
#define TEMPFILE    "*CURLIB/SYSUT1"
#define TEMPLIST    "*CURLIB/SYSUT1"
#define LISTFILE    "*CURLIB/SYSUT2"
#define STATLIST    "*CURLIB/SYSUT3"

#define IGNDIR      ""
#define MAINTDIR    ""
#define EASTATFILE  "*CURLIB/EASTATUS"
#define EALICENSE   "EALICENS"
#define RESPLOG     "RESPLOG"
#define TMPHDRFIL   "*CURLIB/SYSUT4"
/* #define INTERMSGFIL "*CURLIB/SYSUT2" */
/* #define ORIGCMPFIL  "*CURLIB/SYSUT2" */
#if !defined(IFS)
#define SMTPMSG     "*CURLIB"
#define SMTPHDR     "*CURLIB"
#define SMTPBODY    "*CURLIB"
#define TMPMDN      "*CURLIB"
#define TMPMSGSAV   "*CURLIB"
#define RESPTXT     "*CURLIB"
#define FULLRESPTXT "*CURLIB"
#define EASMIME_LOGFILE_NAME	"*CURLIB/SMLOG"
#else
#define SMTPMSG     (char*)"smtpmsg.fil"
#define SMTPHDR     (char*)"smtphdr.fil"
#define SMTPBODY    (char*)"smtpbody.fil"
#define TMPMDN      (char*)"tmpmdn.fil"
#define RESPTXT     (char*)"response.txt"     
#define FULLRESPTXT (char*)"full_response.txt"
#define EASMIME_LOGFILE_NAME	"easmime.log"
#endif /* !defined(IFS) */

/* ------------------------------------------------------------------------ */
/*  Filenames for Windows, UNIX, and OpenVMS                                */
/* ------------------------------------------------------------------------ */
#else
#define EAINI       (char*)"tdclient.ini"
#define EXFERINI    (char*)"exfer.ini"
#define BEXFERINI   (char*)"bexfer.ini"
#define TPADDRSSINI (char*)"tpaddrss.ini"
#define ALIASTBL    (char*)"alias.tbl"
#define AUDITLOG    (char*)"audit.log"
#define CERTFILE    (char*)"cert.fil"
#define CERTREQ     (char*)"cert.req"
#define CPLOOKUP    (char*)"cplookup.tbl"
#define EALOG       (char*)"ea2k.log"
#define EAINILOG    (char*)"eaini.log"
#define EAMEMLOG    (char*)"eamem.log"
#define EXPORTRTM   (char*)"export.rtm"
#define NEWPRIV     (char*)"newpriv.key"
#define PRIVFILE    (char*)"private.fil"
#define PRIVKEY     (char*)"private.key"
#define SECFILE     (char*)"header.def"
#define OUTMSG      (char*)"baseout.msg"
#define COMPLOG     (char*)"compress.log"
#define DCMPLOG     (char*)"decomp.log"
#define INDIR       (char*)"incoming"
#define OUTDIR      (char*)"outgoing"
#define RUNDIR      (char*)"runtime"
#define SECDIR      (char*)"security"
#define TEMPDIR     (char*)"temp"
#define ERRORDIR    (char*)"error"
#define EACOMMLOG   (char*)"eacomm.log"
#define EAXFERLOG   (char*)"eaxfer.log"
#define IEBASELOG   (char*)"eaiebase.log"
#define SENDEDILOG  (char*)"sendedi.log"
#define INPRO       (char*)"basein.pro"
#define INMSG       (char*)"basein.msg"
#define DESTFILE    destFileName
#define QUOTEFILE   (char*)"quote.fil"
#define RCVRPT      (char*)"rcv.rpt"
#define SNDRPT      (char*)"snd.rpt"
#define GERCVRPT    (char*)"geis.rcv.rpt"
#define GESNDRPT    (char*)"geis.snd.rpt"
#define TEMPFILE    (char*)"temp.fil"
#define SENDCMPFILE (char*)"send.cmp"
#define TEMPLIST    (char*)"tmplist.fil"
#define LISTFILE    (char*)"list.fil"
#define STATLIST    (char*)"statlist.fil"

#define IGNDIR      (char*)"ign"
#define MAINTDIR    (char*)"maint"
#define EASTATFILE  (char*)"eastatus.txt"
#define EALICENSE   (char*)"license.txt"
#define RESPLOG     (char*)"resplog.txt"
#define TMPHDRFIL   (char*)"tmphdr.fil"
/* #define INTERMSGFIL (char*)"intermsg.txt" */
/* #define ORIGCMPFIL  (char*)"origcmp.txt" */
#define SMTPMSG     (char*)"smtpmsg.fil"
#define SMTPHDR     (char*)"smtphdr.fil"
#define SMTPBODY    (char*)"smtpbody.fil"
#define TMPMDN      (char*)"tmpmdn.fil"
/*#define TMPMSGSAV   (char*)"tmpmsg.sav" */
#define RESPTXT     (char*)"response.txt"     
#define FULLRESPTXT (char*)"full_response.txt"
#define EASMIME_LOGFILE_NAME	(char*)"easmime.log"

#endif

#if defined(__cplusplus)
}
#endif  /* if defined(__cplusplus) */

#endif  /* if !defined(__EAPLTFRM_H__) */

/* ------------------------------------------------------------------------	*/
/*																			*/
/*	This document contains material which is the proprietary property of	*/
/*	and confidential to Comm-Press Incorporated.							*/
/*	Disclosure outside Comm-Press is prohibited except by license agreement	*/
/*	or other confidentiality agreement.										*/
/*																			*/
/*	Copyright (c) 1998 by Comm-Press Incorporated							*/
/*	All Rights Reserved														*/
/*																			*/
/* ------------------------------------------------------------------------	*/
/*	Description:	Contains version info for EasyAccess 2000 Client builds	*/
/*					used by About box and XFER when talking to EAFTP server	*/
/* ------------------------------------------------------------------------	*/
/*			MAINTENANCE	HISTORY												*/
/*																			*/
/* DATE		BY	BUG NO.	DESCRIPTION											*/
/* -------- --- ------- ---------------------------------------------------	*/
/* 19980321	PAA			Initial version										*/
/* ------------------------------------------------------------------------	*/
#include "eapltfrm.h"
#include "xferdefs.h"

eaVersionData_t
eaVersionData[] =
{
	/* --------------------------------------------------------	*/
	/* Note: View with 10-space tab stops for WYSIWYG-like appearance*/
	/* --------------------------------------------------------	*/
	{"EasyAccess Version:	", "2K.01.31"		},
	{"Build Date:		", "02/01/2000"			},
	{"Command-Line Build Date:	", "02/01/2000"	},
#if defined(EA_DOS)
	{"System Type:		", "DOS"				},
#elif defined(EA_WIN16)
	{"System Type:		", "Win3.1"				},
#elif defined(EA_WIN32) && !defined(EA_WINNT)
	{"System Type:		", "Win95"				},
#elif defined(EA_WINNT)
	{"System Type:		", "WinNT4.0"			},
#elif defined(EA_AIX41)
	{"System Type:		", "AIX 4.1+"			},
#elif defined(EA_AIX42)
	{"System Type:		", "AIX 4.2+"			},
#elif defined(EA_SOLARIS251)
	{"System Type:		", "Solar2.6"			},
#elif defined(EA_SOLARIS26)
	{"System Type:		", "Solar2.6"			},
#elif defined(EA_HPUX10)
	{"System Type:		", "HPUX 10"			},
#elif defined(EA_SCOUNIX)
	{"System Type:		", "SCO UNIX"			},
#elif defined(EA_DECUNIX)
	{"System Type:		", "DEC UNIX"			},
#elif defined(EA_OPENVMS71)
	{"System Type:		", "OpenVMS"			},
#endif
	{"Compress Version:		", "4.41r 01/21/2000"},
	{"EA-FTP Version:		", "02/01/2000"		},
	{"Rimport version:		", "01/21/2000"		},
	{"ea2kw95d DLL version:	", "02/01/2000"		},
	{0,						0					}
};

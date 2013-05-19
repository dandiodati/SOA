// ----------------------------------------------------------------------------
//
//  This document contains material which is the proprietary property of and
//  confidential to bTrade.com Incorporated.
//  Disclosure outside bTrade.com is prohibited except by license agreement
//  or other confidentiality agreement.
//
//  Copyright (c) 1998 by bTrade.com Incorporated
//  All Rights Reserved
//
// ----------------------------------------------------------------------------
//  Description: Macros used by TDClient Message Display sub-system
//
// ----------------------------------------------------------------------------
//          MAINTENANCE HISTORY
//
// DATE     BY  BUG NO. DESCRIPTION
// -------- --- ------- -------------------------------------------------------
// 19980321 PAA         Initial version
// ----------------------------------------------------------------------------
#if !defined(__EA_MSG_H__)
#define __EA_MSG_H__

#include "cf.hpp"
#include "eadefs.h"
#if defined(EA_DLL)
#include "eaexcep.hpp"
#endif  // if defined(EA_DLL)

// ----------------------------------------------------------------------------
//  Prototype of EAAPI messaging functions
// ----------------------------------------------------------------------------
int             sendMsgBoxMsg(eaApiContext_t*, int code, int level, char* text,
                    bool* answer);
int             sendTextMsg(eaApiContext_t*, char level, char* text);
int             sendGetFileSpecMsg(eaApiContext_t*, char* fileSpec, int len);
int             sendGetUserInputMsg(eaApiContext_t*, char* prompt, char* userInput,
                    int len);

// ----------------------------------------------------------------------------
//  Prototype for eaDisplayMessage and associated helper functions
// ----------------------------------------------------------------------------
#if defined(EA_ZAF)
#include "zaf.hpp"
ZafEventType    eaDisplayMessage(cf_t*, int msgCode, int severityLevel,
                    bool batchMode, bool hasCancel, void* pEaApiObject,
                    const char* format, ...);
ZafEventType    OpenMessageDialog(ZafIChar *titleString,
                    ZafIChar *messageString, bool hasCancel = false);
ZafEventType    OpenWarningMessageDialog(ZafIChar *titleString,
                    ZafIChar *messageString, bool hasCancel = false);
ZafEventType    OpenSuccessMessageDialog(ZafIChar *titleString,
                    ZafIChar *messageString, bool hasCancel = false);

#else   // Else !defined(EA_ZAF)
int             eaDisplayMessage(cf_t*, int msgCode, int severityLevel,
                    bool batchMode, bool hasCancel, void* pEaApiObject,
                    const char* format, ...);
#endif  // if defined(EA_ZAF)

#endif  // if !defined(__EA_MSG_H__)

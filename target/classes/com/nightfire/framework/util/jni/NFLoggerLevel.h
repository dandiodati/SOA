/*
 * Copyright(c) 2000 Nightfire Software Inc.
 * All rights reserved.
 *
 * NOTE: This file was generated automatically. PLEASE DO NOT EDIT!
 */

// multiple inclusion protection
#ifndef _NF_NFLOGGERLEVEL_H_
#define _NF_NFLOGGERLEVEL_H_

// logging level
typedef struct
{
    unsigned logClass;
    unsigned logLevel;
    unsigned javaLevel;
} NFLogLevel;

// logging levels

static const NFLogLevel NF_MAX_LEVELS           = {0x00000001, 0x00000002, 300};
static const NFLogLevel NF_ALL_ERRORS           = {0x00000002, 0x00000000, 0};
static const NFLogLevel NF_ALL_WARNINGS         = {0x00000002, 0x00000001, 1};
static const NFLogLevel NF_NORMAL_STATUS        = {0x00000002, 0x00000002, 2};
static const NFLogLevel NF_EXCEPTION_CREATION   = {0x00000002, 0x00000004, 3};
static const NFLogLevel NF_EXCEPTION_STACK_TRACE= {0x00000002, 0x00000008, 4};
static const NFLogLevel NF_MEM_USAGE            = {0x00000002, 0x00000010, 5};
static const NFLogLevel NF_SYSTEM_CONFIG        = {0x00000002, 0x00000020, 6};
static const NFLogLevel NF_OBJECT_LIFECYCLE     = {0x00000002, 0x00000040, 7};
static const NFLogLevel NF_UNIT_TEST            = {0x00000002, 0x00000080, 8};
static const NFLogLevel NF_ASSERT               = {0x00000002, 0x00000100, 9};
static const NFLogLevel NF_BENCHMARK            = {0x00000004, 0x00000002, 10};
static const NFLogLevel NF_THREAD_BASE          = {0x00000008, 0x00000002, 20};
static const NFLogLevel NF_THREAD_ERROR         = NF_ALL_ERRORS;
static const NFLogLevel NF_THREAD_WARNING       = NF_ALL_WARNINGS;
static const NFLogLevel NF_THREAD_LIFECYCLE     = {0x00000008, 0x00000004, 21};
static const NFLogLevel NF_THREAD_STATUS        = {0x00000008, 0x00000008, 22};
static const NFLogLevel NF_IO_BASE              = {0x00000010, 0x00000002, 30};
static const NFLogLevel NF_IO_ERROR             = NF_ALL_ERRORS;
static const NFLogLevel NF_IO_WARNING           = NF_ALL_WARNINGS;
static const NFLogLevel NF_IO_STATUS            = {0x00000010, 0x00000004, 31};
static const NFLogLevel NF_IO_DATA              = {0x00000010, 0x00000008, 32};
static const NFLogLevel NF_IO_PERF_TIMER        = {0x00000010, 0x00000010, 33};
static const NFLogLevel NF_DB_BASE              = {0x00000020, 0x00000002, 40};
static const NFLogLevel NF_DB_ERROR             = NF_ALL_ERRORS;
static const NFLogLevel NF_DB_WARNING           = NF_ALL_WARNINGS;
static const NFLogLevel NF_DB_STATUS            = {0x00000020, 0x00000004, 41};
static const NFLogLevel NF_DB_DATA              = {0x00000020, 0x00000008, 42};
static const NFLogLevel NF_MAPPING_BASE         = {0x00000040, 0x00000002, 50};
static const NFLogLevel NF_MAPPING_ERROR        = NF_ALL_ERRORS;
static const NFLogLevel NF_MAPPING_WARNING      = NF_ALL_WARNINGS;
static const NFLogLevel NF_MAPPING_LIFECYCLE    = {0x00000040, 0x00000004, 51};
static const NFLogLevel NF_MAPPING_STATUS       = {0x00000040, 0x00000008, 52};
static const NFLogLevel NF_MAPPING_DATA         = {0x00000040, 0x00000010, 53};
static const NFLogLevel NF_MSG_BASE             = {0x00000080, 0x00000002, 60};
static const NFLogLevel NF_MSG_ERROR            = NF_ALL_ERRORS;
static const NFLogLevel NF_MSG_WARNING          = NF_ALL_WARNINGS;
static const NFLogLevel NF_MSG_LIFECYCLE        = {0x00000080, 0x00000004, 61};
static const NFLogLevel NF_MSG_STATUS           = {0x00000080, 0x00000008, 62};
static const NFLogLevel NF_MSG_DATA             = {0x00000080, 0x00000010, 63};
static const NFLogLevel NF_MSG_GENERATE         = {0x00000080, 0x00000020, 64};
static const NFLogLevel NF_MSG_PARSE            = {0x00000080, 0x00000040, 65};
static const NFLogLevel NF_XML_BASE             = {0x00000100, 0x00000002, 70};
static const NFLogLevel NF_XML_ERROR            = NF_ALL_ERRORS;
static const NFLogLevel NF_XML_WARNING          = NF_ALL_WARNINGS;
static const NFLogLevel NF_XML_LIFECYCLE        = {0x00000100, 0x00000004, 71};
static const NFLogLevel NF_XML_STATUS           = {0x00000100, 0x00000008, 72};
static const NFLogLevel NF_XML_DATA             = {0x00000100, 0x00000010, 73};
static const NFLogLevel NF_XML_GENERATE         = {0x00000100, 0x00000020, 74};
static const NFLogLevel NF_XML_PARSE            = {0x00000100, 0x00000040, 75};
static const NFLogLevel NF_XSL_LIFECYCLE        = {0x00000100, 0x00000080, 76};
static const NFLogLevel NF_RULES_BASE           = {0x00000200, 0x00000002, 80};
static const NFLogLevel NF_RULE_LIFECYCLE       = {0x00000200, 0x00000004, 81};
static const NFLogLevel NF_RULE_EXECUTION       = {0x00000200, 0x00000008, 82};
static const NFLogLevel NF_WORKFLOW_BASE        = {0x00000400, 0x00000002, 90};
static const NFLogLevel NF_WORKFLOW_LIFECYCLE   = {0x00000400, 0x00000004, 91};
static const NFLogLevel NF_WORKFLOW_STATUS      = {0x00000400, 0x00000008, 92};
static const NFLogLevel NF_WORKFLOW_EXECUTION   = {0x00000400, 0x00000010, 93};
static const NFLogLevel NF_WORKFLOW_DATA        = {0x00000400, 0x00000020, 94};
static const NFLogLevel NF_STATE_BASE           = {0x00000800, 0x00000002, 100};
static const NFLogLevel NF_STATE_ERROR          = NF_ALL_ERRORS;
static const NFLogLevel NF_STATE_WARNING        = NF_ALL_WARNINGS;
static const NFLogLevel NF_STATE_LIFECYCLE      = {0x00000800, 0x00000004, 101};
static const NFLogLevel NF_STATE_STATUS         = {0x00000800, 0x00000008, 102};
static const NFLogLevel NF_STATE_DATA           = {0x00000800, 0x00000010, 103};
static const NFLogLevel NF_STATE_TRANSITION     = {0x00000800, 0x00000020, 104};
static const NFLogLevel NF_USER_BASE            = {0x00001000, 0x00000002, 200};
static const NFLogLevel NF_EDI_BASE             = NF_USER_BASE;
static const NFLogLevel NF_EDI_ERROR            = NF_MSG_ERROR;
static const NFLogLevel NF_EDI_WARNING          = NF_MSG_WARNING;
static const NFLogLevel NF_EDI_LIFECYCLE        = {0x00002000, 0x00000002, 201};
static const NFLogLevel NF_EDI_STATUS           = {0x00002000, 0x00000004, 202};
static const NFLogLevel NF_EDI_DATA             = {0x00002000, 0x00000008, 203};
static const NFLogLevel NF_EDI_GENERATE         = {0x00002000, 0x00000010, 204};
static const NFLogLevel NF_EDI_PARSE            = {0x00002000, 0x00000020, 205};


//must be kept updated
static const unsigned NF_LOG_CLASS_COUNT = 8193;
static const unsigned NF_MAX_LOG_LEVEL = 300;

#endif // _NF_NFLOGGERLEVEL_H_


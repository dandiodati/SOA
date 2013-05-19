/*
 * Copyright (c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 *
 * NOTE: This file was generated automatically. PLEASE DO NOT EDIT!
 */

#include <NFLogger.h>

#include <string.h>

//****************************************************************************
// METHOD:      prepareLevelTable
//
// PURPOSE:     Initializes the global table mapping Java to C logging levels

void NFLogger::prepareLevelTable()
{
    // zero out the array
    memset(allLogLevels, 0, sizeof(NFLogLevel) * (NF_MAX_LOG_LEVEL + 1));

    // set known levels

    allLogLevels[NF_MAX_LEVELS.javaLevel]             = NF_MAX_LEVELS;
    allLogLevels[NF_ALL_ERRORS.javaLevel]             = NF_ALL_ERRORS;
    allLogLevels[NF_ALL_WARNINGS.javaLevel]           = NF_ALL_WARNINGS;
    allLogLevels[NF_NORMAL_STATUS.javaLevel]          = NF_NORMAL_STATUS;
    allLogLevels[NF_EXCEPTION_CREATION.javaLevel]     = NF_EXCEPTION_CREATION;
    allLogLevels[NF_EXCEPTION_STACK_TRACE.javaLevel]  = NF_EXCEPTION_STACK_TRACE;
    allLogLevels[NF_MEM_USAGE.javaLevel]              = NF_MEM_USAGE;
    allLogLevels[NF_SYSTEM_CONFIG.javaLevel]          = NF_SYSTEM_CONFIG;
    allLogLevels[NF_OBJECT_LIFECYCLE.javaLevel]       = NF_OBJECT_LIFECYCLE;
    allLogLevels[NF_UNIT_TEST.javaLevel]              = NF_UNIT_TEST;
    allLogLevels[NF_ASSERT.javaLevel]                 = NF_ASSERT;
    allLogLevels[NF_BENCHMARK.javaLevel]              = NF_BENCHMARK;
    allLogLevels[NF_THREAD_BASE.javaLevel]            = NF_THREAD_BASE;
    allLogLevels[NF_THREAD_LIFECYCLE.javaLevel]       = NF_THREAD_LIFECYCLE;
    allLogLevels[NF_THREAD_STATUS.javaLevel]          = NF_THREAD_STATUS;
    allLogLevels[NF_IO_BASE.javaLevel]                = NF_IO_BASE;
    allLogLevels[NF_IO_STATUS.javaLevel]              = NF_IO_STATUS;
    allLogLevels[NF_IO_DATA.javaLevel]                = NF_IO_DATA;
    allLogLevels[NF_IO_PERF_TIMER.javaLevel]          = NF_IO_PERF_TIMER;
    allLogLevels[NF_DB_BASE.javaLevel]                = NF_DB_BASE;
    allLogLevels[NF_DB_STATUS.javaLevel]              = NF_DB_STATUS;
    allLogLevels[NF_DB_DATA.javaLevel]                = NF_DB_DATA;
    allLogLevels[NF_MAPPING_BASE.javaLevel]           = NF_MAPPING_BASE;
    allLogLevels[NF_MAPPING_LIFECYCLE.javaLevel]      = NF_MAPPING_LIFECYCLE;
    allLogLevels[NF_MAPPING_STATUS.javaLevel]         = NF_MAPPING_STATUS;
    allLogLevels[NF_MAPPING_DATA.javaLevel]           = NF_MAPPING_DATA;
    allLogLevels[NF_MSG_BASE.javaLevel]               = NF_MSG_BASE;
    allLogLevels[NF_MSG_LIFECYCLE.javaLevel]          = NF_MSG_LIFECYCLE;
    allLogLevels[NF_MSG_STATUS.javaLevel]             = NF_MSG_STATUS;
    allLogLevels[NF_MSG_DATA.javaLevel]               = NF_MSG_DATA;
    allLogLevels[NF_MSG_GENERATE.javaLevel]           = NF_MSG_GENERATE;
    allLogLevels[NF_MSG_PARSE.javaLevel]              = NF_MSG_PARSE;
    allLogLevels[NF_XML_BASE.javaLevel]               = NF_XML_BASE;
    allLogLevels[NF_XML_LIFECYCLE.javaLevel]          = NF_XML_LIFECYCLE;
    allLogLevels[NF_XML_STATUS.javaLevel]             = NF_XML_STATUS;
    allLogLevels[NF_XML_DATA.javaLevel]               = NF_XML_DATA;
    allLogLevels[NF_XML_GENERATE.javaLevel]           = NF_XML_GENERATE;
    allLogLevels[NF_XML_PARSE.javaLevel]              = NF_XML_PARSE;
    allLogLevels[NF_XSL_LIFECYCLE.javaLevel]          = NF_XSL_LIFECYCLE;
    allLogLevels[NF_RULES_BASE.javaLevel]             = NF_RULES_BASE;
    allLogLevels[NF_RULE_LIFECYCLE.javaLevel]         = NF_RULE_LIFECYCLE;
    allLogLevels[NF_RULE_EXECUTION.javaLevel]         = NF_RULE_EXECUTION;
    allLogLevels[NF_WORKFLOW_BASE.javaLevel]          = NF_WORKFLOW_BASE;
    allLogLevels[NF_WORKFLOW_LIFECYCLE.javaLevel]     = NF_WORKFLOW_LIFECYCLE;
    allLogLevels[NF_WORKFLOW_STATUS.javaLevel]        = NF_WORKFLOW_STATUS;
    allLogLevels[NF_WORKFLOW_EXECUTION.javaLevel]     = NF_WORKFLOW_EXECUTION;
    allLogLevels[NF_WORKFLOW_DATA.javaLevel]          = NF_WORKFLOW_DATA;
    allLogLevels[NF_STATE_BASE.javaLevel]             = NF_STATE_BASE;
    allLogLevels[NF_STATE_LIFECYCLE.javaLevel]        = NF_STATE_LIFECYCLE;
    allLogLevels[NF_STATE_STATUS.javaLevel]           = NF_STATE_STATUS;
    allLogLevels[NF_STATE_DATA.javaLevel]             = NF_STATE_DATA;
    allLogLevels[NF_STATE_TRANSITION.javaLevel]       = NF_STATE_TRANSITION;
    allLogLevels[NF_USER_BASE.javaLevel]              = NF_USER_BASE;
    allLogLevels[NF_EDI_LIFECYCLE.javaLevel]          = NF_EDI_LIFECYCLE;
    allLogLevels[NF_EDI_STATUS.javaLevel]             = NF_EDI_STATUS;
    allLogLevels[NF_EDI_DATA.javaLevel]               = NF_EDI_DATA;
    allLogLevels[NF_EDI_GENERATE.javaLevel]           = NF_EDI_GENERATE;
    allLogLevels[NF_EDI_PARSE.javaLevel]              = NF_EDI_PARSE;
}


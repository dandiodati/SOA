/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.manager.common;


/**
 *  Interface containing names of events associated with state transitions.
 */
public interface CommonEventNames
{
    // Generic, error-type events.
    public static final String DATA_EXCEPTION_EVENT = "DATA_EXCEPTION_EVENT";
    public static final String SYSTEM_EXCEPTION_EVENT = "SYSTEM_EXCEPTION_EVENT";
    public static final String UNEXPECTED_MESSAGE_EVENT = "UNEXPECTED_MESSAGE_EVENT";

    public static final String MESSAGE_SAVE_EVENT = "MESSAGE_SAVE_EVENT";
    public static final String MESSAGE_SUBMIT_EVENT = "MESSAGE_SUBMIT_EVENT";

    public static final String MESSAGE_SENT_EVENT = "MESSAGE_SENT_EVENT";
    public static final String DONE_EVENT = "DONE_EVENT";
    public static final String DUE_DATE_RECEIVED_EVENT = "DUE_DATE_RECEIVED_EVENT";
    public static final String CANCEL_EVENT = "CANCEL_EVENT";

    //Generic manual update status events
    public static final String MANUAL_MESSAGE_SENT_EVENT = "MANUAL_MESSAGE_SENT_EVENT";

}

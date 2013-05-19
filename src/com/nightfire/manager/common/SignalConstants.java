/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/SignalConstants.java#1 $
 */
package com.nightfire.manager.common;

import com.nightfire.mgrcore.common.*;


/**
 *  Interface containing names of events associated with state Signals.
 */
public class SignalConstants implements MgrCoreSignals
{
    /**
     * Signal indicates the due date is received in a new order.
     */
    public static final String DUE_DATE_RECEIVED_SIGNAL = "dueDateReceivedNotification";

    /**
     * Signal indicates the due date is changed in a new order.
     */
    public static final String DUE_DATE_CHANGED_SIGNAL = "dueDateChangedNotification";

    /**
     * Signal indicates the order is cancelled.
     */
    public static final String CANCEL_ORDER_SIGNAL = "cancelOrder";

    /**
     * Signal indicates the order is supplemented.
     */
    public static final String SUPPLEMENT_SIGNAL = "supplementSignal";

    /**
     * Signal indicates the order is resent.
     */
    public static final String RESEND_SIGNAL = "resendSignal";

    /**
     * Identifies the boWork that will be completed/deleted when order is being resent.
     */
    public static final String WF_RESEND_TIMER = "ResendTimer";

    /**
     * Signal indicates the LSR order is complete.
     */
    public static final String LSR_COMPLETED_SIGNAL = "lsrCompletedNotification";

    /**
     * Signal indicates the LSR order is cancelled.
     */
    public static final String LSR_CANCELLED_SIGNAL = "lsrCancelledNotification";

    /**
     * Signal indicates telephone numbers are changed in a LSR order.
     */
    public static final String TN_CHANGED_SIGNAL = "tnChangedNotification";

    /**
     * Signal indicates the due date is received in an order modifier.
     */
    public static final String SUPP_DUE_DATE_RECEIVED_SIGNAL = "suppDueDateReceivedNotification";
    /**
     * Signal indicates the due date is changed in an order modifier.
     */
    public static final String SUPP_DUE_DATE_CHANGED_SIGNAL = "suppDueDateChangedNotification";

    /**
     * The path to due date in a due date signal.
     */
    public static final String DUE_DATE_LOCATION = "DueDate";

    /**
     * The path to the type of the event that triggers the signal.
     */
    public static final String EVENT_TYPE_LOCATION = "EventType";
}

package com.nightfire.order.utils;

import java.util.TimeZone;

public class CHOrderConstants 
{
    public static final String ICP = "ICP";
    public static final String UTC_TIMEZONE_STR = "UTC";
    public static TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    public static final String NEW_STATUS = "NEW_STATUS";
    public static final String OLD_STATUS = "OLD_STATUS";
    public static final String EVENT_CODE = "EVENT_CODE";
    public static final String LOG_SYSDATE_IN_UTC = "LOG_UTC_SYSDATE";
    public static final String ICP_TRANS_TABLE_NAME = "CH_ICP_TRANS";
    public static final String MP_CONTEXT_CORRELATIONID = "CORRELATIONID";
    public static final String ICP_TRANS_CORRELATIONID_COL = "CORRELATIONID";
    public static final String MP_CONTEXT_REQUESTNUMBER = "REQNO";
    public static final String ICP_TRANS_REQUESTNUMBER_COL = "REQUESTNUMBER";
    public static final String ICP_TRANS_CUSTOMERID_COL = "CUST_NAME";   
    public static final String CH_ORDER_STATUS = "STATUS";
    public static final String IS_VALID_EVENT = "IS_VALID_EVENT";
    public static final String MP_CONTEXT_MESSAGESUBTYPE = "MessageSubType";
    public static final String SAVE_EVENT = "save";

}


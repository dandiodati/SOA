package com.nightfire.comms.connectors;

public interface Constants
{

    String ORB_AGENT_ADDR_PROP = "ORBagentAddr";
    String ORB_AGENT_PORT_PROP = "ORBagentPort";
    String ORB_AGENT_SVC_ROOT  = "SVCnameroot";
    String ORB_AGENT_SVC_PROP  = "ORBservices";
    String SERVER_IP_ADDR_PROP = "OAipAddr";

    String DEBUG_LOG  = "LOG_FILE";
    String LOG_LEVEL  = "DEBUG_LOG_LEVELS";

    int VT_MAJOR_ERROR   = 0;
    int VT_SERIOUS_ERROR = 1;
    int VT_WARNING       = 2;
    int VT_NORMAL        = 3;
    int VT_DETAIL        = 4;
    int VT_DEBUG         = 5;

    int FLOW_SOURCE_WRAPPER_ERROR_CODE = 1010;
    int RUNTIME_EXCEPTION_CODE = 911;

    String DATAERROR        = "Data Error: ";
    String SYSTEMERROR      = "System Error: ";
    String NULLRESULTERROR  = "Null Result Error: ";

    String ASYNC_REQUEST_EVENT  = "asyncRequestEvent";
    String SYNC_REQUEST_EVENT   = "syncRequestEvent";
    String ASYNC_RESPONSE_EVENT = "asyncResponseEvent";
    String SYNC_RESPONSE_EVENT  = "syncResponseEvent";

    String DATA_ERROR_EVENT     = "dataErrorEvent";
    String SYSTEM_ERROR_EVENT   = "systemErrorEvent";
    String NULL_RESULT_EVENT    = "nullResultEvent";

    String REQUEST_EVENT_INTERFACE  = "NightFireEvents.RequestInterface";
    String RESPONSE_EVENT_INTERFACE = "NightFireEvents.ResponseInterface";
    String ERROR_EVENT_INTERFACE    = "NightFireEvents.ErrorInterface";

}

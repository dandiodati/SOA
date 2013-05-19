/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.webgui.core;

import  com.nightfire.framework.constants.*;

import  com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.webgui.core.resource.WebAppResourceFactory;



/**
 * This class provides a central location where general and "shared" constants
 * can reside.  These constants are specific to run-time UI framework.  It does not
 * contain any methods.  Any run-time UI classes should implement or refer to it.
 */

public interface ServletConstants
{
    //////////////////////////////////////////////////////////
    // Application context initialization parameter constants.
    //////////////////////////////////////////////////////////

    // character encoding for streams
    public static final String CHAR_ENCODING = "CHAR_ENCODING";

    public static final String PRODUCTION_MODE = PlatformConstants.PRODUCTION_MODE;

    public static final String RESOURCE_URL_CONVERTER = "RESOURCE_URL_CONVERTER";
    public static final String DEFAULT_RESOURCE_URL_CONVERTER = "DEFAULT_RESOURCE_URL_CONVERTER";


    public static final String WEB_APP_NAME =com.nightfire.framework.webapp.BasicContextManager.WEB_APP_NAME;

    public static final String HELP_DIRECTORY = "HELP_DIRECTORY";

    public static final String BOX_IDENTIFIER = "BOX_IDENTIFIER";

    public static final String MANAGER_BOX_IDENTIFIER = "MANAGER_BOX_IDENTIFIER";

   /**
     * Location in the application context to obtain a data adapter
     * @see DataAdapter
     */
    public static final String DATA_ADAPTER          = "DATA_ADAPTER";

    /**
     * Location in the application context to obtain a input validator
     * This class validates the xml BEFORE the data adapter has been applied.
     * @see InputValidator
     */
    public static final String INPUT_VALIDATOR       = "INPUT_VALIDATOR";


    /**
     * Location in the application context to obtain a protocol adapter
     * @see ProtocolAdapter
     */
    public static final String PROTOCOL_ADAPTER      = "PROTOCOL_ADAPTER";

    /**
     * Location in the application context to obtain a alias descriptor.
     * @see AliasDescriptor
     */
    public static final String ALIAS_DESCRIPTOR      = "ALIAS_DESCRIPTOR";

    /**
     * Location in the application context to obtain a field map descriptor
     * @deprecated
     */
    public static final String FIELD_MAP_DESCRIPTOR  = "FIELD_MAP_DESCRIPTOR";

    /**
     * Location in the application context to obtain a page flow coordinator.
     * @see PageFlowCoordinator
     */
    public static final String PAGE_FLOW_COORDINATOR = "PAGE_FLOW_COORDINATOR";

    /**
     * Repository-prefix property name.  This prefix should be common to all
     * repositories used in a web application.
     */
    public static final String REPOSITORY_PREFIX     = "REPOSITORY_PREFIX";


    /**
     * Separator/delimiter used in repository path name.
     */
    public static final String REPOSITORY_SEPARATOR  = RepositoryManager.REPOSITORY_SEPARATOR;


    //////////////////////////////////////////////////////////
    // variable names for objects set in the application, session, request,
    // or page scope
    //////////////////////////////////////////////////////////


    /**
     * Name for accessing the context's Properties object containing all context
     * parameters.
     */
    public static final String CONTEXT_PARAMS          = com.nightfire.framework.webapp.BasicContextManager.CONTEXT_PARAMS;

    /**
     * session cache which holds request, response, and misc beans associated with message ids.
     */
    public static final String MESSAGE_DATA_CACHE = "MessageDataCache";

    /**
     * Data bean (in page request scope) that the current message body and header associated with the current request.
     */
    public static final String REQUEST_BEAN  = "RequestBean";

    /**
     * Data bean (in page request scope) that the current response body associated with the current request.
     */
    public static final String RESPONSE_BEAN = "ResponseBean";

    /**
     * Id of the session bean containing user and session-specific information.
     */
    public static final String SESSION_BEAN  = "SessionBean";

    /**
     * Indicates the name of map which holds defaultmappings from business rule errors
     * to gui fields. Each key in the map is a meta file resource, and each value
     * is a BizRuleMapper.
     * Each node path must be same as the xml error path.
     * At the node there will then be an id which indentifies the related gui xml field.
     * This provides a default mapping which is created by the paths from the meta scheme
     */
    public static final String BIZ_RULE_DEFAULT_MAPPINGS = "BizRuleDefaultMappings";

    /**
     * Indicates the name of object which holds the most current mappings from business rule errors
     * to gui fields. This can be a BizRuleMapper or a map of BizRuleMappers.
     *
     * Each node path must be same as the xml error path.
     * At the node there will then be an id which indentifies the related gui xml field.
     *
     */
    public static final String BIZ_RULE_MAPPINGS = "BizRuleMappings";

    //SOA gui box identifier - as context parameter
    public static final String SOA_BOX_IDENTIFIER = "SOA";
    //ClearingHouse gui box identifier - as context parameter
    public static final String CH_BOX_IDENTIFIER = "CH";
    //WNP gui box identifier - as context parameter
    public static final String WNP_MANAGER_BOX_IDENTIFIER = "WNP";
    //IMM gui box identifier - as context parameter
    public static final String IMM_MANAGER_BOX_IDENTIFIER = "IMM";
    //ICP gui box identifier - as context parameter
    public static final String ICP_BOX_IDENTIFIER = "ICP";

    //////////////////////////////////////////////////////////
    // key control header fields/nodes  passed in the beans
    //////////////////////////////////////////////////////////

    /**
     * fields prefix to indicate a field belongs in the message body.
     */
    public static final String NF_FIELD_PREFIX                    = "NF_";

    /**
     * fields prefix to indicate a field belongs in the header.
     */
    public static final String NF_FIELD_HEADER_PREFIX             = "NFH_";


    public static final String MESSAGE_ID = "MessageId";

    public static final String LSR_ORDER_TAB = "lsr_order";

    public static final String INFLIGHT = "in-flight";

 	public static final String LAST_STATUS_DATE_FIELD = "Datetime";


     //global Page constants that are used by the servlet and other jsp tags.


    /**
     * action header field which indicates the action to perform.
     */
    public static final String NF_FIELD_ACTION_TAG                = NF_FIELD_HEADER_PREFIX + PlatformConstants.ACTION_NODE;

    /**
     * page header field which indicates the page to redirect to or
     * to indicate page buffering when used with NF_FIELD_ACTION_TAG.
     */
    public static final String NF_FIELD_HEADER_PAGE_TAG           = "NFH_Page";

    /**
     * header field which indicates a response code prefix to use for
     * response codes.
     */
    public static final String NF_FIELD_HEADER_RESPCODE_PREFIX              = NF_FIELD_HEADER_PREFIX + PlatformConstants.RESPONSE_CODE_PREFIX_NODE;

    /**
     * message id header field which indicates the message id to indentify
     * the the correct request and response beans to use.
     */
    public static final String NF_FIELD_HEADER_MESSAGE_ID_TAG     = NF_FIELD_HEADER_PREFIX + MESSAGE_ID;


    public static final String SUPPLIER        = "Supplier";
	public static final String TRADINGPARTNER  = "TradingPartner";
    public static final String SERVICE_TYPE     = "ServiceType";
    public static final String INTERFACE_VER = "InterfaceVersion";

    public static final String MESSAGE_TYPE    = "MessageType";

    public static final String USER_NAME_FIELD = "UserName";

    public static final String CUSTOMER_NAME_FIELD = "CustomerId";

    public static final String META_DATA_PARAM                 = "metaResource";
    public static final String META_DATA_PATH                 = NF_FIELD_HEADER_PREFIX + META_DATA_PARAM;

    // indicates that the message is read only
    public static final String READ_ONLY                 = "ReadOnly";

    // indicates that the message type does not change
    // used for actions in cases when the message type result stays the
    // same.
    public static final String PASSTHROUGH_MSG_TYPE = "PASSTHROUGH";


    //////////////////////////////////////////////////////////
    // Other xml node constants
    //////////////////////////////////////////////////////////

    public static final String RESPONSE_CODE_NODE  = PlatformConstants.RESPONSE_CODE_NODE;

    public static final String ERR_HEADER_DOC_NAME = "ErrorHeader";

    public static final String ERR_BODY_DOC_NAME   = "ErrorDescription";


    // the name of the xsl script for use on headers. Both managers and gateways use
    // use this now.
    public static final String HEADER_TRANSFORM_XSL = "requestHeader";


    //////////////////////////////////////////////////////////
    // Response code constants
    //////////////////////////////////////////////////////////

    //public static final String SUCCESS_RESPONSE_CODE = PlatformConstants.SUCCESS_VALUE;


    //public static final String INVALID_INPUT_RESPONSE_CODE = PlatformConstants.FAILURE_VALUE;


    public static final String PROC_EXCEPTION_RESPONSE_CODE = "processing-exception";

    public static final String SUCCESS      = PlatformConstants.SUCCESS_VALUE;

    public static final String FAILURE      = PlatformConstants.FAILURE_VALUE;

    public static final String NO_MATCH     = PlatformConstants.NO_MATCH_VALUE;




    // html new line
    public static final String NL =  "\r\n";




    //////////////////////////////////////////////////////////
    // Resource related constants
    //////////////////////////////////////////////////////////
    public static final String RESOURCE_DATA_ROOT                 = "RESOURCE_DATA_ROOT";

    public static final String RESOURCE_DATA_CACHE                = "RESOURCE_CACHE";

    public static final String RESOURCE_RELOAD_TIME              = "RESOURCE_RELOAD_TIME";

    public static final String RESOURCE_ELEMENT_SEPERATOR              = "-";

    public static final String IDLE_CLEANUP_TIME              = "IDLE_CLEANUP_TIME";
    public static final String XML_DOC_DATA       = WebAppResourceFactory.XML_DOC_DATA;
    public static final String PROPERTY_DATA       = WebAppResourceFactory.PROPERTY_DATA;
    public static final String META_DATA         = WebAppResourceFactory.META_DATA;
    public static final String TEMPLATE_DATA     = WebAppResourceFactory.TEMPLATE_DATA;
    public static final String XSL_TRANSFORM_DATA       = WebAppResourceFactory.XSL_TRANSFORM_DATA;

    public static final String META_DIRECTORY = "meta";
    public static final String TEMPLATE_DIRECTORY = "templates";
    public static final String XSL_DIRECTORY = "xsl";
    public static final String RESOURCE_DIRECTORY = "resources";


    public static final String RESOURCE_ROOT_PATH = "/WEB-INF/resources/";
    public static final String OUTGOING_XSL_SUFFIX    = "Outgoing";
    public static final String INCOMING_XSL_SUFFIX    = "Incoming";

    public static final String SVC_HANDLER_ACTIONS = "SvcHandlerActions";

    public static final String APPLY_BUSINESS_RULES                 = "ApplyBusinessRules";

    public static final String NF_FIELD_HEADER_APPLY_BUSINESS_RULES = NF_FIELD_HEADER_PREFIX + APPLY_BUSINESS_RULES;

    public static final String LOCKED_NODE = "Locked";

    public static final String RESUBMISSION_ATTEMPT_HEADER_FIELD               = "ResubmissionAttempt";

    public static final String RESUBMISSION_ATTEMPT_REDIRECT_PAGE_HEADER_FIELD = "ResubmissionAttemptRedirectPage";

    public static final String BASIC_REPLICATE_DISPLAY = "BASIC_REPLICATE_DISPLAY";

    public static final String LOCALMGR_RELATIVE_DATE = "LOCALMGR_RELATIVE_DATE";

    // preorder import

    public static final String PON_LOCATION = "Request.lsr_order.lsr.lsr_adminsection.PON";
	public static final String ATN_LOCATION = "Request.lsr_order.lsr.lsr_adminsection.ATN";
    public static final String SUP_LOCATION = "Request.lsr_order.lsr.lsr_adminsection.SUP";

	// Preorder to preorder data import

    public static final String TXTYP_LOCATION = "Request.lsr_preorder.RequestHeader.TXTYP";
	public static final String TXACT_LOCATION = "Request.lsr_preorder.RequestHeader.TXACT";

    // GUI Login and Logout actions
    public static final String SINGLE_SIGN_ON_LOGIN_ACTION = "singleSignOnLogin";
    public static final String LOGIN_ACTION = "login";
    public static final String LOGOUT_ACTION = "logout";

    public static final String TP_ALIAS_METHOD_DB = "DB";
    public static final String TP_ALIAS_METHOD_FS = "FS";
}

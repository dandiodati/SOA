/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager;

import  com.nightfire.webgui.core.ServletConstants;

import  com.nightfire.framework.constants.PlatformConstants;


/**
 * This interface provides a set of constants common to all Manager applications.
 */

public interface ManagerServletConstants extends ServletConstants
{
    ////////////////////////////////////////////////////////////////
    // The following constants are here temporarily since manager-
    // specific files are still residing in //webgui/core.  Once
    // these files have been moved to manager-specific depot, these
    // constants can and must be removed.
    ////////////////////////////////////////////////////////////////

    public static final String NEW_SERVICE_BUNDLE_ACTION      = "new-service-bundle";

    public static final String SAVE_SERVICE_BUNDLE_ACTION     = "save-service-bundle";

    public static final String VALIDATE_SERVICE_BUNDLE_ACTION = "validate-service-bundle";
    public static final String COPY_SERVICE_BUNDLE_ACTION = "copy-service-bundle";
    public static final String VALIDATE_ORDER_ACTION = "validate-order";

    public static final String GET_SPI_MESSAGE_HISTORY_ACTION = "get-spi-message-history";

	public static final String GET_SPI_MESSAGE_HISTORY_ACK_ACTION = "get-spi-message-history-ack";

    public static final String RESEND_ORDER_ACTION            = "resend-order";

    public static final String CANCEL_ORDER_ACTION            = "cancel-order";

    public static final String SUPPLEMENT_ORDER_ACTION        = "supplement-order";
    public static final String EDIT_ACTION                    = "edit-order";
    public static final String VIEW_ACTION                    = "view";
    public static final String VIEW_WORKITEMS_ACTION          = "query-workitems";
    public static final String VIEW_ADMIN_WORKITEMS_ACTION    = "administer-work-items";


    public static final String SEND_RESPONSE_ACTION           = "send-response";

    public static final String STATUS_FIELD                   = "CurrentState";

    public static final String BOID_FIELD                     = "BOID";

    public static final String SCID_FIELD                     = "SCID";

    public static final String SERVICE_BUNDLE_NAME            = "ServiceBundleName";

    ////////////////////////////////////////////////////////////////
    // End of constants-to-be-removed section.
    ////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////
    // Field and node names/ids.
    ////////////////////////////////////////////////////////////////

    public static final String SERVICE_COMPONENT_HEADER_FIELD    = ServletConstants.NF_FIELD_HEADER_PREFIX + "ServiceComponent";

    public static final String PREDEFINED_BUNDLE_ID_HEADER_FIELD = ServletConstants.NF_FIELD_HEADER_PREFIX + "PredefinedBundleId";


    public static final String SHOW_OUTBOUND_ACK_HEADER_FIELD = ServletConstants.NF_FIELD_HEADER_PREFIX + "ShowOutboundAck";

    public static final String APPLY_TEMPLATES_HEADER_FIELD = "ApplyTemplates";


    public static final String BUNDLE_DISPLAY_NAME               = "BundleDisplayName";

    public static final String COMPONENT_DISPLAY_NAME            = "DisplayName";

    public static final String META_DATA_NAME         	         = "MetaDataName";

    public static final String PREDEFINED_SC = "ServiceComponents";
    public static final String PREDEFINED_COUNT = "Count";

    ////////////////////////////////////////////////////////////////
    // Field values.
    ////////////////////////////////////////////////////////////////

    public static final String EMPTY_BUNDLE = "EmptyBundle";

    public static final String SAVED_STATUS = "Initial";

    public static final String INVALID_STATUS = "Invalid";

    public static final String NEW_STATUS   = "New";


    ////////////////////////////////////////////////////////////////
    // Context object names/ids.
    ////////////////////////////////////////////////////////////////

    public static final String SERVICE_BUNDLE_BEAN_BAG = "ServiceBundleBeanBag";

    public static final String BUNDLE_DEFS             = "BundleDefs";

    public static final String ADDITIONAL_INFO         = "AdditionalInfo";

    public static final String INVALID_ACTION          = "InvalidAction";


    ////////////////////////////////////////////////////////////////
    // Resource types.
    ////////////////////////////////////////////////////////////////

    public static final String BUNDLE_DEF_DATA = "BUNDLE_DEF_DATA";
    public static final String PREDEFINED_BUNDLE_DATA= "PREDEFINED_BUNDLE_DATA";

    ////////////////////////////////////////////////////////////////
    // Field and node values.
    ////////////////////////////////////////////////////////////////

    public static final String HEADER_TRANSFORMER_XSL = "header.xsl";



    ////////////////////////////////////////////////////////////////
    // properties
    ////////////////////////////////////////////////////////////////

    public static final String PREDEFINED_BUNDLE_DEF_PROP   = "PREDEFINED_BUNDLE_DEF";

    ////////////////////////////////////////////////////////////////
    // node names
    ////////////////////////////////////////////////////////////////
    public static final String ORDER_NODE = "Order";
    public static final String MODIFIER_NODE = "LatestModifier";

    ////////////////////////////////////////////////////////////////
    // Order Type
    ////////////////////////////////////////////////////////////////
    public static final String LSR_NP_SVC = "LSRNPSvcBundle";
    public static final String ICP_PORTIN_SVC = "ICPPortInSvcBundle";
    public static final String ICP_PORTIN_SOA_PORTIN_SVC = "ICPPortInSOASvcBundle";
    public static final String LSR_NP_SOA_SVC = "LSRNPSOASvcBundle";
    public static final String LSR_PREORDER_SVC = "LSRPreorderSvcBundle";
    public static final String SOA_PORTIN_SVC = "SOAPortInSvcBundle";
    
    public static final String LSR_ORDER = "LSROrder";
    public static final String LSR_PRE_ORDER = "LSRPreorder";
    public static final String ICP_PORTIN =  "ICPPortIn";
    public static final String SOA_PORTIN = "SOAPortIn";
    public static final String LSR_NUMBER_PORT = "NP";


    ////////////////////////////////////////////////////////////////
    // Template Messge xml
    ////////////////////////////////////////////////////////////////

    public static final String BODY = "Body";
    public static final String INFO = "Info";
    public static final String ORDER_TYPE = INFO + ".OrderType";
    public static final String SORTBY = INFO + ".SortBy";
    public static final String INFO_SUPPLIER = INFO + ".Supplier";
    public static final String REQUEST_TYPE = INFO + ".ReqType";
    public static final String INTERFACEVERSION = INFO + ".InterfaceVersion";


}

/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.manager.common;

import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.businessobject.BusinessObjectMetaData;

/**
 * Class containing manager constants
 */
public interface ManagerConstants extends MgrCoreConstants, MgrCoreSignals
{
    /**
     * The name of the workflow process as configured in the business object
     * meta-data application-specific section.
     */
    public static final String WORKFLOW_PROCESS_NAME = "WorkflowProcessName";

    /**
     * The repository category for external notification as configured in the business object
     * meta-data application-specific section.
     * Default is RepositoryCategories.EXTERNAL_NOTIFICATION_TEMPLATE_CONFIG.
     */
    public static final String NOTIFICATION_CATEGORY = "NotificationCategory";

    /**
     * The suffix used to indicate a definition template.
     */
    public static final String DEFINITION = "Definition";

    /**
     * The suffix used to construct the meta-data name for a service component.
     */
    public static final String SERVICE_COMPONENT            = "SvcComponent";

    /**
     * The suffix used to construct the name for a service component definition.
     */
    public static final String SERVICE_COMPONENT_DEFINITION = SERVICE_COMPONENT + DEFINITION;

    /**
     * The suffix used to construct the meta-data name for an order business object of type New.
     */
    public static final String NEW_ORDER                    = "NewOrder";

    /**
     * The suffix used to construct the meta-data name for an order business object of type Change.
     */
    public static final String CHANGE_ORDER                 = "ChangeOrder";

    /**
     * The suffix used to construct the meta-data name for an order business object of type Disconnect.
     */
    public static final String DISC_ORDER             = "DiscOrder";


    /**
     * The array of possible order types. These are used in constructing
     * and decomposing the names of Order business objects. 
     */
    public static final String[] ORDER_TYPES = { NEW_ORDER,
                                                 CHANGE_ORDER,
                                                 DISC_ORDER };

    /**
     * The suffix used to construct the meta-data name for an order modifier business object of type Supplement.
     */
    public static final String SUPP_ORDER_MODIFIER    = "SuppOrderModifier";

    /**
     * The suffix used to construct the meta-data name for an order modifier business object of type Cancel.
     */
    public static final String CANCEL_ORDER_MODIFIER        = "CancelOrderModifier";

    /**
     * The array of possible order modifier types. These are used
     * as suffixes for the names of Order Modifier business objects.
     */
    public static final String[] ORDER_MODIFIER_TYPES = { SUPP_ORDER_MODIFIER,
                                                          CANCEL_ORDER_MODIFIER };

    // Node to look up for event name during update status requests
    public static final String EVENT_NODE_NAME = "Event";
    
    //************************************************************************************
    // Convenience constants to access information in the incoming XML. The most commonly
    // accessed nodes are listed here.
    //************************************************************************************
    public static final String SUPPLIER_NAME_LOC = INFO_NODE + "." + SUPPLIER_NODE;
    public static final String DIVISION_NAME_LOC = INFO_NODE + "." + "Division";
    public static final String META_DATA_NAME_LOC = INFO_NODE + "." + META_DATA_NAME_NODE;
    public static final String BOID_LOC = INFO_NODE + "." + BOID_NODE;
    public static final String MANUAL_UPDATE_EVENT_LOC = INFO_NODE + "." + EVENT_NODE_NAME;

    // Business object fields required by infrastructure, with names that are fixed by the infrastructure.
    public static final String BO_BOID                  = BusinessObjectMetaData.BOID;
    public static final String BO_META_DATA_NAME        = BusinessObjectMetaData.META_DATA_NAME;
    public static final String BO_PARENT_BOID           = BusinessObjectMetaData.PARENT_BOID;
    public static final String BO_PARENT_META_DATA_NAME = BusinessObjectMetaData.PARENT_META_DATA_NAME;
    public static final String BO_CURRENT_STATE         = BusinessObjectMetaData.CURRENT_STATE;
    // Item that would be present in the application-specific part of the BO meta-data.
    public static final String BO_OBJECT_SUB_TYPE_NODE  = "BusinessObjectSubType";

    // Business object fields that are always present, whose names are common across
    // all objects (as determined by meta data).
  	public static final String BO_PARENT_TYPE        = "ParentType";
	  public static final String BO_CREATED_BY         = "CreatedBy";
	  public static final String BO_CREATE_DATE        = "CreateDate";
	  public static final String BO_COMPLETED_DATE     = "CompletedDate";
	  public static final String BO_LAST_MODIFIED_BY   = "LastModifiedBy";
	  public static final String BO_LAST_MODIFIED_DATE = "LastModifiedDate";
    public static final String BO_OWNED_BY   = "OwnedBy";
    public static final String BO_TELEPHONE_NUMBER   = "TelephoneNumber";
    public static final String BO_XML_BLOB           = "BusinessObjectXMLBlob";
    public static final String BO_XML_BLOB_MANAGER_SECTION           = BO_XML_BLOB + "." + "Manager";

    public static final String BO_XML_BLOB_OTHER_SECTION           = BO_XML_BLOB + "." + "Other";    

    // Business object fields always present on component/order-type business objects.
	  public static final String BO_ORDER_SUPPLIER           = "Supplier";
	  public static final String BO_ORDER_DIVISION           = "Division";
    public static final String BO_ORDER_TYPE               = "Type";
    public static final String BO_ORDER_SUBMITTED_DATE     = "SubmittedDate";
    public static final String BO_ORDER_LAST_RESPONSE      = "LastResponse";
    public static final String BO_ORDER_LAST_RESPONSE_DATE = "LastResponseDate";


    // Service bundle business object field name constants.
    public static final String BO_SVC_BUNDLE_NAME                  = "ServiceBundleName";
    public static final String BO_SVC_BUNDLE_EXTERNAL_ID           = "OrderID";
    public static final String BO_SVC_BUNDLE_TELEPHONE_NUMBER      = BO_TELEPHONE_NUMBER;
    public static final String BO_SVC_BUNDLE_PARENT_TYPE           = BO_PARENT_TYPE;
  	public static final String BO_SVC_BUNDLE_CREATED_BY            = BO_CREATED_BY;
  	public static final String BO_SVC_BUNDLE_CREATE_DATE           = BO_CREATE_DATE;
  	public static final String BO_SVC_BUNDLE_COMPLETED_DATE        = BO_COMPLETED_DATE;
  	public static final String BO_SVC_BUNDLE_LAST_MODIFIED_BY      = BO_LAST_MODIFIED_BY;
  	public static final String BO_SVC_BUNDLE_LAST_MODIFIED_DATE    = BO_LAST_MODIFIED_DATE;
    public static final String BO_SVC_BUNDLE_XML_BLOB              = BO_XML_BLOB;
    public static final String BO_SVC_BUNDLE_BOID                  = BO_BOID;
    public static final String BO_SVC_BUNDLE_CURRENT_STATE         = BO_CURRENT_STATE;
    public static final String BO_SVC_BUNDLE_META_DATA_NAME        = BO_META_DATA_NAME;
    public static final String BO_SVC_BUNDLE_PARENT_BOID           = BO_PARENT_BOID;
    public static final String BO_SVC_BUNDLE_PARENT_META_DATA_NAME = BO_PARENT_META_DATA_NAME;

    // Locations of key items in service bundle XML documents.
    public static final String SVC_BUNDLE_NAME_LOC              = INFO_NODE + "." + BO_SVC_BUNDLE_NAME;
    public static final String SVC_BUNDLE_EXTERNAL_ID_LOC       = INFO_NODE + "." + BO_SVC_BUNDLE_EXTERNAL_ID;

    // Constants relating to the Master / Slave relationship
    public static final String MASTER_ORDER_NODE                = "Master";
    public static final String MASTER_ORDER_TRUE               = MgrCoreConstants.TRUE;
    public static final String MASTER_ORDER_FALSE               = MgrCoreConstants.FALSE;
    public static final String MASTER_ORDER_BLOB_NODE           = BO_XML_BLOB_MANAGER_SECTION +
                                                                  "." + MASTER_ORDER_NODE;

    // Service component business object field name constants.
  	public static final String BO_SVC_COMPONENT_NAME                  = "ServiceComponentName";
    public static final String BO_SVC_COMPONENT_TELEPHONE_NUMBER      = BO_TELEPHONE_NUMBER;
    public static final String BO_SVC_COMPONENT_SUPPLIER              = BO_ORDER_SUPPLIER;
    public static final String BO_SVC_COMPONENT_TYPE                  = BO_ORDER_TYPE;
	  public static final String BO_SVC_COMPONENT_PARENT_TYPE           = BO_PARENT_TYPE;
	  public static final String BO_SVC_COMPONENT_CREATED_BY            = BO_CREATED_BY;
	  public static final String BO_SVC_COMPONENT_CREATE_DATE           = BO_CREATE_DATE;
	  public static final String BO_SVC_COMPONENT_COMPLETED_DATE        = BO_COMPLETED_DATE;
	  public static final String BO_SVC_COMPONENT_LAST_MODIFIED_BY      = BO_LAST_MODIFIED_BY;
	  public static final String BO_SVC_COMPONENT_LAST_MODIFIED_DATE    = BO_LAST_MODIFIED_DATE;
    public static final String BO_SVC_COMPONENT_XML_BLOB              = BO_XML_BLOB;
    public static final String BO_SVC_COMPONENT_BOID                  = BO_BOID;
    public static final String BO_SVC_COMPONENT_CURRENT_STATE         = BO_CURRENT_STATE;
    public static final String BO_SVC_COMPONENT_META_DATA_NAME        = BO_META_DATA_NAME;
    public static final String BO_SVC_COMPONENT_PARENT_BOID           = BO_PARENT_BOID;
    public static final String BO_SVC_COMPONENT_PARENT_META_DATA_NAME = BO_PARENT_META_DATA_NAME;

    // Message type for resend orders.
    public static final String RESEND_MESSAGE_TYPE = "Resend";

    //Node names used in the xml message that is returned by the getAllowableTransitions
    //method.
    public static final String TRANSITION_NODE = "Transition";
    public static final String NEXT_STATE_NODE = "NextState";
    public static final String EVENT_NODE = "Event";

    //**************************************************************************
    // Names of state methods that are invokeable from the business object
    // execute() method.
    //**************************************************************************

    // Method invoked to process gateway responses.
    public static final String BO_METHOD_GET_DESCRIPTION = "getDescription";

    // Method invoked to tell the BusinessObject to save data as appropriate.
    public static final String BO_METHOD_SAVE = "save";

    // Method invoked to query the BusinessObject to see if it's associated order can be submitted.
    public static final String BO_METHOD_CAN_BE_SUBMITTED = "canBeSubmitted";

    // Method invoked to tell the BusinessObject to submit the order.
    public static final String BO_METHOD_SUBMIT = "submit";

    // Method invoked to resend a message.
    public static final String BO_METHOD_RESEND = "resend";

    // Method invoked to determine if child business object is done.
    public static final String BO_METHOD_IS_DONE = "isDone";

    // Method invoked to inform that child business object is done.
    public static final String BO_METHOD_CHILD_DONE = "childDone";

    // Method invoked to receive a signal on a child business object.
    public static final String BO_METHOD_RECEIVE_SIGNAL = "receiveSignal";

    // Method invoked to indicate the type - master/slave of an order.
    public static final String BO_METHOD_SET_IS_MASTER = "setIsMaster";

    // Method invoked to query whether the state is ready to receive
    // a response of a particular type.
    public static final String BO_METHOD_IS_EXPECTED_MESSAGE = "isExpectedMessage";

    // Method invoked to get the list of allowable actions in the current state of the
    //business object. The list is returned in XML form.
    public static final String BO_METHOD_GET_ALLOWABLE_ACTIONS = "getAllowableActions";

    // Method invoked to get the list of allowable transitions in the current state of the
    //business object. The list is returned in XML form. It has the list of events acceptable
    //in that state and the next state that the business object will transition to on the
    //occurrence of an event.
    public static final String BO_METHOD_GET_ALLOWABLE_TRANSITIONS = "getAllowableTransitions";

    /**
    * The name of the method used to invoke the getGatewayDescriptionName
    * method on StateBase.
    */
    public static final String BO_METHOD_GET_GATEWAY_DESCRIPTION_NAME = "getGatewayDescriptionName";    

  /*
     * Constant defining the XML node name for storing slave only bundle start dates.
     */
    public static final String START_DATE_NODE_NAME = "StartDate";

    /*
     * Constant defining where the Start date for a slave only bundle exists in the
     * bundle data passed in at creation / save time.
     */
    public static final String SAVE_DATA_START_DATE_NODE = ManagerConstants.INFO_NODE +
                                                           "." + START_DATE_NODE_NAME;

    /*
     * Constant defining where the Start date for a slave only bundle is located in the
     * XML blob.
     */
    public static final String MANAGER_START_DATE_NODE = "Manager" + "." + START_DATE_NODE_NAME;


    /////// Constants relating to slave only bundles.///////////

    /*
     * Constant defining a node that contains a value indicating if bundle is a slave only bundle.
     */
    public static final String SLAVE_ONLY_BUNDLE_NODE = "SlaveOnlyBundle";

    /*
     * Constant defining where that the bundle consists of slaves alone.
     */
    public static final String MANAGER_SLAVE_ONLY_BUNDLE_NODE = "Manager" + "." + SLAVE_ONLY_BUNDLE_NODE;


    public static final String SLAVE_ONLY_BUNDLE_TRUE = TRUE;
    public static final String SLAVE_ONLY_BUNDLE_FALSE = FALSE;

    /**
     * Method invoked to figure out if this bundle is a slave only bundle.
     */
    public static final String BO_METHOD_IS_SLAVE_ONLY_BUNDLE = "isSlaveOnlyBundle"; 
	
    public static final String DONE_STATE = "Done";
    public static final String IN_SERVICE_STATE = "InService";
    public static final String CANCELLED_STATE = "Cancelled";

    public static final String BO_LSR_DUE_DATE = "DueDate";

    /*
     * Node containing the style sheet to be used by this business object.
     */
    public static final String XSL_STYLE_SHEET = "XSLStyleSheet";


    /*
     * Node in application specific portion of BO metadata indicating
     * that abortProcess should not be called in the Done state.
     */
    public static final String DISABLE_ABORT_ON_DONE = "DisableAbortOnDone";

    
}//ManagerConstants

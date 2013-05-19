/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.spi.common;

import com.nightfire.framework.util.*;


/**
 * Header node names to be used by the processors in all 
 * Gateways and the Router
 */
 public interface HeaderNodeNames
 {

     /*  Node names of header nodes */
   public static final String ACCOUNTID_NODE = "AccountID";

   public static final String REQUEST_NODE = "Request";

   public static final String SUBREQUEST_NODE = "Subrequest";

   public static final String SUBTYPE_NODE = "Subtype";

   public static final String SUPPLIER_NODE = "Supplier";

   public static final String CUSTOMER_ID_NODE = CustomerContext.CUSTOMER_ID_NODE;

   public static final String SUBDOMAIN_ID_NODE = CustomerContext.SUBDOMAIN_ID_NODE;

   public static final String USER_ID_NODE = CustomerContext.USER_ID_NODE;

   public static final String USER_PASSWORD_NODE = CustomerContext.USER_PASSWORD_NODE;

   public static final String APPLY_BUSINESS_RULES_NODE = "ApplyBusinessRules";

   public static final String INTERFACE_VERSION_NODE = CustomerContext.INTERFACE_VERSION_NODE;

   public static final String ACTION_NODE = "Action";

   public static final String SUBMIT = "submit";

   public static final String INPUT_SOURCE_NODE= "InputSource";

   public static final String ORDER_OID_NODE= "OrderOId";

   public static final String TRANS_OID_NODE= "TransOId";

   public static final String VALUE_ATTRIBUTE= "value";


 }

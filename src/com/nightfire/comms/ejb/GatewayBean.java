/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //comms/R4.4/com/nightfire/comms/ejb/GatewayBean.java#1 $
 */
package com.nightfire.comms.ejb;


import com.nightfire.rmi.*;


/**
 * Class providing remote interface for gateway session EJB.
 * Callable methods can be found in the RMIRequestHandler interface.
 */
public interface GatewayBean extends javax.ejb.EJBObject, RMIRequestHandler
{

}

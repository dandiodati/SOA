/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //comms/R4.4/com/nightfire/comms/ejb/GatewayBeanHome.java#1 $
 */
package com.nightfire.comms.ejb;


import java.rmi.RemoteException;
import javax.ejb.CreateException;


/**
 * Home used to access gateway session EJBs.
 */
public interface GatewayBeanHome extends javax.ejb.EJBHome
{
    /**
     * Create a gateway instance for processing requests whose configuration
     * is obtained from the deployment environment.
     *
     * @return  A newly-created gateway bean.
     *
     * @exception  RemoteException  Thrown on communications errors.
     * @exception  CreateException  Thrown on bean creation errors.
     */
    public GatewayBean create ( ) throws RemoteException, CreateException;
}

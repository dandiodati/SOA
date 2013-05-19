/*
 * Copyright NeuStar, Inc., 2006
 * This file contains confidential and proprietary information and may not be
 * distributed without prior written consent.
 */
package com.nightfire.framework.util;


import java.rmi.*;


/**
 * RMI interface for objects supporting remote thread monitoring capabilities.
 */
public interface RemoteOperationsAdmin extends Remote
{
   /**
    * Flush the cache of all objects registered with the CacheRegistrar.
    *
    */
   public void flushCache ( ) throws java.rmi.RemoteException;
}

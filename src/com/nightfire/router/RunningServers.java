/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import java.util.concurrent.locks.*;

import com.nightfire.router.util.*;
import com.nightfire.router.exceptions.UnKnownSPIException;

import com.nightfire.common.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;

import java.util.*;



/**
 * this class caches all UsageDescriptions objects and RequestHandler objects
 * for the SPIs. It also handles all synchronization for reader and writer
 * threads. This means that multiple threads can query this object for information,
 * and will only be blocked during updates to this AvailableServers object.
 * @author Dan Diodati
 */
public final class RunningServers implements AvailableServers
{

  public static final String STARTUP_EVENT_SUF = ".STARTUP";
  public static final String SHUTDOWN_EVENT_SUF = ".SHUTDOWN";

  public static final short STARTUP_EVENT_ID = 0;
  public static final short SHUTDOWN_EVENT_ID = 1;


  private ReentrantReadWriteLock locker = new ReentrantReadWriteLock();  //read write lock

  private RouterSupervisor sup;

  private AddrPortTree servers;

  public RunningServers(RouterSupervisor sup)
  {
     this.sup = sup;
     servers = new AddrPortTree();
  }

  /**
   * This updates the spi info and blocks all reading threads during an update.
   * Only the RouterSupervisor can call this method.
   */
  void updateSPIInfo(String addr, String port, ObjectLocator locator, int eventType, String event) throws ProcessingException
  {
          // do update threads have to be queued up for execution order ??

          //remove from locator cache to prevent retriving old spi handlers.
          // This is done always to account for cases when the server is killed forcefully
          // and never issues the shutdown event.
          locator.removeFromCache(event);

          try {

             locker.writeLock().lock();


             //synchronized block start
             if ( eventType == STARTUP_EVENT_ID ) {

                ServerObject so = new ServerObject();
                so.cosName = event;

                try {
                  setServerObjectHandles(locator, so, event);

                } catch (Exception e) {
                   throw new ProcessingException("Could not obtain reference to server : " + e.getMessage() );
                }
                servers.put(addr,port,event,so);

             }
             else if ( eventType == SHUTDOWN_EVENT_ID ) {
                servers.remove(addr, port, event);
             }



          }
          finally {
             locker.writeLock().unlock();
             // end synchronized block
          }
  }


  /* intializes a server object
  */
  private void setServerObjectHandles(ObjectLocator locator, ServerObject so, String serverName) throws CorbaException
  {

       org.omg.CORBA.Object obj = locator.find(serverName);

       if (obj == null)  {
          String error  = "Could not find " + serverName + " in Naming Service";
          Debug.log(this,Debug.ALL_ERRORS,error);
          throw new CorbaException(error);
       }
       so.requestHandler =  RequestHandlerHelper.narrow(obj);

       if (so.requestHandler == null ) {
          String error  = "Could not obtain request handler for  " + serverName;
          Debug.log(this,Debug.ALL_ERRORS,error);
          throw new CorbaException(error);
       }
       so.usageDescription = so.requestHandler.getUsageDescriptions();
       if (so.usageDescription == null ) {
          String error  = "Could not obtain usage description for " + serverName;
          Debug.log(this,Debug.ALL_ERRORS, error);
          throw new CorbaException(error);
       }
   }


   /**
   * Returns the ServerObject for the COSName at the default ORB port and address( The one specified in properties for the router)
   * @param COSName - the Corba Naming Service Name of the server to return.
   * @returns the ServerObject if found.

   * @throws UnKnownSPIException if the server is not available.
   */
  public ServerObject getServerObject(String COSName) throws UnKnownSPIException,ProcessingException
  {

             String addr = sup.getAdminServer().getProperty(RouterConstants.ORB_AGENT_ADDR);
             String port = sup.getAdminServer().getProperty(RouterConstants.ORB_AGENT_PORT);

             return (getServerObject(addr,port,COSName) );

  }

  /**
   * Returns the ServerObject for the COSName specified at a specific orb address and port. (can not be null)
   * @param ORBagentAddr the orb address to look at (can not be null)
   * @param ORBagentPort the orb port to look at. (can not be null)
   * @param COSName - the Corba Naming Service Name of the server to return.
   * @returns the RequestHandler representation of the server
   * @throws UnKnownSPIException if the server is not available.
   */
  public ServerObject getServerObject(String ORBagentAddr, String ORBagentPort, String COSName) throws UnKnownSPIException, ProcessingException
  {
      try {

             locker.readLock().lock();

             //synchronized block start
             ServerObject obj = servers.get(ORBagentAddr,ORBagentPort,COSName);

             if (obj == null)
               throw new  UnKnownSPIException("Server [" + COSName + "] at address [" + ORBagentAddr
                  + "] and port [" + ORBagentPort + "] not found.");
             return (obj);

          }
          finally {

             locker.readLock().unlock();
             // end synchronized block
          }




  }

   /**
   * This method takes a visitor object and calls the visit method on every leave node within
   * the filtering criteria(specified by the ServerObjectVisitor passed in).
   *
   * @param visitor Calls the visitor's visit method for every ServerObject Map found.
   * WARNING: This returns references to ServerObjects that should never be altered in any way.
   */
  public void traverseServerObjects(ServerObjectVisitor visitor) throws ProcessingException
  {

          try {

             locker.readLock().lock();

             //synchronized block start
             servers.traverse(visitor);

          }
          finally {

             locker.readLock().unlock();
             // end synchronized block
          }


  }

}

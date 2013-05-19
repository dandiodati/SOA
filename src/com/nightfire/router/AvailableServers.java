/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import com.nightfire.router.exceptions.*;
import com.nightfire.router.util.*;

import com.nightfire.common.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import java.util.*;
import org.omg.CORBA.*;

/**
 * Gives access to the currently running servers that the router is tracking.
 * (returned by the RouterSupervisor).
 * @author Dan Diodati
 */
public interface AvailableServers
{


  /**
   * Returns the ServerObject for the COSName at the default ORB port and address( The one specified in properties for the router)
   * @param COSName - the Corba Naming Service Name of the server to return.
   * @returns the ServerObject if found.

   * @throws UnKnownSPIException if the server is not available.
   */
  public ServerObject getServerObject(String COSName) throws UnKnownSPIException, ProcessingException;

  /**
   * Returns the ServerObject for the COSName specified at a specific orb address and port. (can not be null)
   * @param ORBagentAddr the orb address to look at (can not be null)
   * @param ORBagentPort the orb port to look at. (can not be null)
   * @param COSName - the Corba Naming Service Name of the server to return.
   * @returns the RequestHandler representation of the server
   * @throws UnKnownSPIException if the server is not available.
   */
  public ServerObject getServerObject(String ORBagentAddr, String ORBagentPort, String COSName) throws UnKnownSPIException, ProcessingException;


  /**
   * This method takes a visitor object and calls the visit method on every leave node within
   * the filtering criteria(specified by the ServerObjectVisitor passed in).
   *
   * @param visitor The visitor for each matching ServerObject.
   * WARNING: This returns references to ServerObjects that should never be altered in any way.
   * @throws ProcessingException if the server is not available.
   */
  public void traverseServerObjects(ServerObjectVisitor visitor) throws ProcessingException;


}





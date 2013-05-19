/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import java.util.*;

/**
 * defines a visitor that can be used to search for servers, create custom listings, etc
 * @author Dan Diodati
 */
  public interface ServerObjectVisitor
  {

     /**
      * The method is called on the vistor for every ServerObject map encountered.
      * @param serverObjects It is a server object at the current leave node.
      * ORBagentAddr     - the address for the serverobjects.
      * ORBagentPort     - the port for the serverobjects
      */
     public void visit(ServerObject serverObject, String ORBagentAddr, String ORBagentPort);

     /**
      * A filter to search on only the leave nodes of the specified ORB address.
      * This can speed the search up a lot.
      * @returns ORBagentAddr the ORB address to filter on. This should return null
      * if no filtering is required.
      *
      */
     public String getAddressFilter();
     /**
      * A filter to search on only the subnodes of the specified ORB port.
      * @returns ORBagentPort the ORB port to filter on. This should return null
      * if no filtering is required.
      *
      */
     public String getPortFilter();

     /**
      * This is called before every visit call.
      * If the the ServerObjectVistor has gotten enough information, then it can
      * return true to stop visiting nodes.
      * Otherwise this should always return false;
      *  @returns true to stop processing, otherwise return false.
      */
     public boolean isDone();
 }





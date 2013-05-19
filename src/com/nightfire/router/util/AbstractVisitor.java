/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.router.util;

import com.nightfire.router.*;


/**
 * This is a default ServerObjectVisitor that gives some default behaviors
 * to visitors.
 * @author Dan Diodati
 */

public abstract class AbstractVisitor implements ServerObjectVisitor
{
     private boolean done = false;
     private String portFilter = null;
     private String addrFilter = null;



     /**
      * A filter to search on only the leave nodes of the specified ORB address.
      * This can speed the search up a lot.
      * @returns ORBagentAddr the ORB address to filter on. The default is
      * null.
      *
      */
     public String getAddressFilter() {return addrFilter;};
     /**
      * A filter to search on only the subnodes of the specified ORB port.
      * @returns ORBagentPort the ORB port to filter on. The default is null.
      *
      */
     public String getPortFilter() {return portFilter;};

     /**
      * This is called before every visit call.
      *
      * @returns true to stop processing, otherwise return false. The default is false.
      */
     public boolean isDone() {return done; };


      /**
      * Sets the address filter value.
      * @param addr The address that will be filtered on.
      *
      */
     public void setAddressFilter(String addr) {addrFilter = addr;};

     /**
      * Sets the port filter value.
      * @param port The port that will be filtered on.
      *
      */
      public void setPortFilter(String port) {portFilter = port;};

     /**
      * This sets the done value for this visitor.
      * When this is set to true visiting of nodes will stop.
      */
     public void setDone(boolean done) {this.done = done;};



}

/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;

import org.omg.CORBA.*;

  /**
   * This represents a single server that the Router is tracking
   * cosName - is the The corba naming service name used for the object.
   * rh - is a corba reference to the server.
   * ud - a usage description of the server
   *
   * @author Dan Diodati
   */
  public final class ServerObject {
     public String cosName = null;
     public RequestHandler requestHandler = null;
     public UsageDescription[] usageDescription = null;

     public ServerObject()
     {
     }

     public ServerObject(String cosName, RequestHandler rh, UsageDescription[] ud)
     {
        this.cosName = cosName;
        requestHandler = rh;
        usageDescription = ud;
     }
  }






/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/soap/SOAPRequestHandlerInternal.java#1 $
 */
package com.nightfire.comms.soap;

import java.rmi.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.xml.*;

/**
 * Internal interface to be implemented by proxy classes to gateways and managers. 
 */
public interface SOAPRequestHandlerInternal
{

  /**
   * Proxy method for processSync method on RequestHandlerOperations and EJB invocations.
   *
   * @param header - the Nightfire style header.
   * @param message - the message to be submitted.
   *
   * @return String - the response from the synchronous call, with
   *                  String[0] = response header and
   *                  String[1] = response body.
   *
   * @exception java.rmi.RemoteException - Should be thrown if there is a
   *                        error processing the request.
   */
  public String[] process( RequestContext context ) throws RemoteException;

  /**
   * Perform any initialization in this method.
   */
  public void initialize () throws FrameworkException;

  /**
   * Container for request data and associated information.
   */
  public static class RequestContext
  {
      public XMLMessageParser headerParser = null;
      public String header = null;
      public String body = null;
      public String methodName = null;
      public String userID = null;
      public String customerID = null;
      public String password = null;
      public String subDomainId = null;
  }
}

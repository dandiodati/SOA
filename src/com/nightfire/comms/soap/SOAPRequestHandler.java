/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/soap/SOAPRequestHandler.java#1 $
 */
package com.nightfire.comms.soap;


/**
 * Interface that will serve as proxy to gateways and managers.
 * This interface maps to SOAPRequestHandler.wsdl. 
 */
public interface SOAPRequestHandler extends java.rmi.Remote
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
  public String[] processSync( String header, String message ) throws java.rmi.RemoteException;


  /**
   * Proxy method for processAsync method on RequestHandlerOperations and EJB invocations.
   *
   * @param header  - the Nightfire style header.
   * @param message - the message to be submitted.
   *
   * @exception java.rmi.RemoteException - Should be thrown if there is a
   *                        error processing the request.
   */
  public void processAsync( String header, String message ) throws java.rmi.RemoteException;

}

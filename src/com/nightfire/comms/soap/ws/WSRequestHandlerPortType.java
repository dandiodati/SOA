/**
 * WSRequestHandlerPortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package com.nightfire.comms.soap.ws;

public interface WSRequestHandlerPortType extends java.rmi.Remote {
    public void processAsync(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException;
    public java.lang.String processSync(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException;
}

/**
 * SOAPRequestHandlerService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.nightfire.comms.soap;

public interface SOAPRequestHandlerService extends javax.xml.rpc.Service {
    public java.lang.String getSOAPRequestHandlerAddress();

    public com.nightfire.comms.soap.SOAPRequestHandler getSOAPRequestHandler() throws javax.xml.rpc.ServiceException;

    public com.nightfire.comms.soap.SOAPRequestHandler getSOAPRequestHandler(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}

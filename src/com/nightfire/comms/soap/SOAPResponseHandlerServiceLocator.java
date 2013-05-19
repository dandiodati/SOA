/**
 * SOAPResponseHandlerServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.nightfire.comms.soap;

public class SOAPResponseHandlerServiceLocator extends org.apache.axis.client.Service implements com.nightfire.comms.soap.SOAPResponseHandlerService {

    // Use to get a proxy class for SOAPResponseHandler
    private final java.lang.String SOAPResponseHandler_address = "http://localhost:8080/axis/services/SOAPResponseHandler";

    public java.lang.String getSOAPResponseHandlerAddress() {
        return SOAPResponseHandler_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String SOAPResponseHandlerWSDDServiceName = "SOAPResponseHandler";

    public java.lang.String getSOAPResponseHandlerWSDDServiceName() {
        return SOAPResponseHandlerWSDDServiceName;
    }

    public void setSOAPResponseHandlerWSDDServiceName(java.lang.String name) {
        SOAPResponseHandlerWSDDServiceName = name;
    }

    public com.nightfire.comms.soap.SOAPResponseHandler getSOAPResponseHandler() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(SOAPResponseHandler_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getSOAPResponseHandler(endpoint);
    }

    public com.nightfire.comms.soap.SOAPResponseHandler getSOAPResponseHandler(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.nightfire.comms.soap.SOAPResponseHandlerSoapBindingStub _stub = new com.nightfire.comms.soap.SOAPResponseHandlerSoapBindingStub(portAddress, this);
            _stub.setPortName(getSOAPResponseHandlerWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.nightfire.comms.soap.SOAPResponseHandler.class.isAssignableFrom(serviceEndpointInterface)) {
                com.nightfire.comms.soap.SOAPResponseHandlerSoapBindingStub _stub = new com.nightfire.comms.soap.SOAPResponseHandlerSoapBindingStub(new java.net.URL(SOAPResponseHandler_address), this);
                _stub.setPortName(getSOAPResponseHandlerWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("SOAPResponseHandler".equals(inputPortName)) {
            return getSOAPResponseHandler();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.neustar.biz/clearinghouse/SOAPResponseHandler/1.0", "SOAPResponseHandlerService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("SOAPResponseHandler"));
        }
        return ports.iterator();
    }

}

/**
 * PortpsServiceLocator.java
 *
 */

package com.nightfire.spi.neustar_soa.portps;

public class PortpsServiceLocator extends org.apache.axis.client.Service implements com.nightfire.spi.neustar_soa.portps.PortpsService {

    public PortpsServiceLocator() {
    }


    public PortpsServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public PortpsServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for PortpsServiceBinding
    private java.lang.String PortpsServiceBinding_address = "https://portps.neustar.biz/portps/services/PortpsService";

    public java.lang.String getPortpsServiceBindingAddress() {
        return PortpsServiceBinding_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String PortpsServiceBindingWSDDServiceName = "PortpsServiceBinding";

    public java.lang.String getPortpsServiceBindingWSDDServiceName() {
        return PortpsServiceBindingWSDDServiceName;
    }

    public void setPortpsServiceBindingWSDDServiceName(java.lang.String name) {
        PortpsServiceBindingWSDDServiceName = name;
    }

    public com.nightfire.spi.neustar_soa.portps.PortpsServicePort getPortpsServiceBinding() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(PortpsServiceBinding_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getPortpsServiceBinding(endpoint);
    }

    public com.nightfire.spi.neustar_soa.portps.PortpsServicePort getPortpsServiceBinding(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.nightfire.spi.neustar_soa.portps.PortpsServiceBindingStub _stub = new com.nightfire.spi.neustar_soa.portps.PortpsServiceBindingStub(portAddress, this);
            _stub.setPortName(getPortpsServiceBindingWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setPortpsServiceBindingEndpointAddress(java.lang.String address) {
        PortpsServiceBinding_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.nightfire.spi.neustar_soa.portps.PortpsServicePort.class.isAssignableFrom(serviceEndpointInterface)) {
                com.nightfire.spi.neustar_soa.portps.PortpsServiceBindingStub _stub = new com.nightfire.spi.neustar_soa.portps.PortpsServiceBindingStub(new java.net.URL(PortpsServiceBinding_address), this);
                _stub.setPortName(getPortpsServiceBindingWSDDServiceName());
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
        java.lang.String inputPortName = portName.getLocalPart();
        if ("PortpsServiceBinding".equals(inputPortName)) {
            return getPortpsServiceBinding();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "PortpsService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "PortpsServiceBinding"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("PortpsServiceBinding".equals(portName)) {
            setPortpsServiceBindingEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}

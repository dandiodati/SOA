/**
 * SOAPResponseHandlerSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.nightfire.comms.soap;

public class SOAPResponseHandlerSoapBindingSkeleton implements com.nightfire.comms.soap.SOAPResponseHandler, org.apache.axis.wsdl.Skeleton {
    private com.nightfire.comms.soap.SOAPResponseHandler impl;
    private static java.util.Map _myOperations = new java.util.Hashtable();
    private static java.util.Collection _myOperationsList = new java.util.ArrayList();

    /**
    * Returns List of OperationDesc objects with this name
    */
    public static java.util.List getOperationDescByName(java.lang.String methodName) {
        return (java.util.List)_myOperations.get(methodName);
    }

    /**
    * Returns Collection of OperationDescs
    */
    public static java.util.Collection getOperationDescs() {
        return _myOperationsList;
    }

    static {
        org.apache.axis.description.OperationDesc _oper;
        org.apache.axis.description.FaultDesc _fault;
        org.apache.axis.description.ParameterDesc [] _params;
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("processEvent", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://www.neustar.biz/clearinghouse/SOAPResponseHandler/1.0", "processEvent"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("processEvent") == null) {
            _myOperations.put("processEvent", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("processEvent")).add(_oper);
    }

    public SOAPResponseHandlerSoapBindingSkeleton() {
        this.impl = new com.nightfire.comms.soap.SOAPResponseHandlerSoapBindingImpl();
    }

    public SOAPResponseHandlerSoapBindingSkeleton(com.nightfire.comms.soap.SOAPResponseHandler impl) {
        this.impl = impl;
    }
    public void processEvent(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException
    {
        impl.processEvent(in0, in1);
    }

}

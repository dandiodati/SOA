/**
 * SOAPRequestHandlerSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.nightfire.comms.soap;

public class SOAPRequestHandlerSoapBindingSkeleton implements com.nightfire.comms.soap.SOAPRequestHandler, org.apache.axis.wsdl.Skeleton {
    private com.nightfire.comms.soap.SOAPRequestHandler impl;
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
        _oper = new org.apache.axis.description.OperationDesc("processSync", _params, new javax.xml.namespace.QName("", "processSyncReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.neustar.biz/clearinghouse/SOAPRequestHandler/1.0", "ArrayOf_xsd_string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://www.neustar.biz/clearinghouse/SOAPRequestHandler/1.0", "processSync"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("processSync") == null) {
            _myOperations.put("processSync", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("processSync")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("processAsync", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://www.neustar.biz/clearinghouse/SOAPRequestHandler/1.0", "processAsync"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("processAsync") == null) {
            _myOperations.put("processAsync", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("processAsync")).add(_oper);
    }

    public SOAPRequestHandlerSoapBindingSkeleton() {
        this.impl = new com.nightfire.comms.soap.SOAPRequestHandlerSoapBindingImpl();
    }

    public SOAPRequestHandlerSoapBindingSkeleton(com.nightfire.comms.soap.SOAPRequestHandler impl) {
        this.impl = impl;
    }
    public java.lang.String[] processSync(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.processSync(in0, in1);
        return ret;
    }

    public void processAsync(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException
    {
        impl.processAsync(in0, in1);
    }

}

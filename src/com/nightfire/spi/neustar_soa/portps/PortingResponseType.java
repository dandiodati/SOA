/**
 * PortingResponseType.java
 *
 */

package com.nightfire.spi.neustar_soa.portps;

public class PortingResponseType  implements java.io.Serializable {
    private java.lang.String responseCode;

    private java.lang.String responseMessage;

    private com.nightfire.spi.neustar_soa.portps.TNResponseType[] TNResponseList;

    public PortingResponseType() {
    }
    
    public PortingResponseType(
           java.lang.String responseCode,
           java.lang.String responseMessage,
           com.nightfire.spi.neustar_soa.portps.TNResponseType[] TNResponseList) {
           this.responseCode = responseCode;
           this.responseMessage = responseMessage;
           this.TNResponseList = TNResponseList;
    }
    
    
    /**
     * Gets the responseCode value for this PortingResponseType.
     * 
     * @return responseCode
     */
    public java.lang.String getResponseCode() {
        return responseCode;
    }
    
    
    /**
     * Sets the responseCode value for this PortingResponseType.
     * 
     * @param responseCode
     */
    public void setResponseCode(java.lang.String responseCode) {
        this.responseCode = responseCode;
    }
    
    
    /**
     * Gets the responseMessage value for this PortingResponseType.
     * 
     * @return responseMessage
     */
    public java.lang.String getResponseMessage() {
        return responseMessage;
    }


    /**
     * Sets the responseMessage value for this PortingResponseType.
     * 
     * @param responseMessage
     */
    public void setResponseMessage(java.lang.String responseMessage) {
        this.responseMessage = responseMessage;
    }


    /**
     * Gets the TNResponseList value for this PortingResponseType.
     * 
     * @return TNResponseList
     */
    public com.nightfire.spi.neustar_soa.portps.TNResponseType[] getTNResponseList() {
        return TNResponseList;
    }


    /**
     * Sets the TNResponseList value for this PortingResponseType.
     * 
     * @param TNResponseList
     */
    public void setTNResponseList(com.nightfire.spi.neustar_soa.portps.TNResponseType[] TNResponseList) {
        this.TNResponseList = TNResponseList;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof PortingResponseType)) return false;
        PortingResponseType other = (PortingResponseType) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.responseCode==null && other.getResponseCode()==null) || 
             (this.responseCode!=null &&
              this.responseCode.equals(other.getResponseCode()))) &&
            ((this.responseMessage==null && other.getResponseMessage()==null) || 
             (this.responseMessage!=null &&
              this.responseMessage.equals(other.getResponseMessage()))) &&
            ((this.TNResponseList==null && other.getTNResponseList()==null) || 
             (this.TNResponseList!=null &&
              java.util.Arrays.equals(this.TNResponseList, other.getTNResponseList())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getResponseCode() != null) {
            _hashCode += getResponseCode().hashCode();
        }
        if (getResponseMessage() != null) {
            _hashCode += getResponseMessage().hashCode();
        }
        if (getTNResponseList() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getTNResponseList());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getTNResponseList(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(PortingResponseType.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "PortingResponseType"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ResponseCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseMessage");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ResponseMessage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("TNResponseList");
        elemField.setXmlName(new javax.xml.namespace.QName("", "TNResponseList"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "TNResponseType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("", "TNResponse"));
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}

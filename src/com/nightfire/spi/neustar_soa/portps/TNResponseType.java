/**
 * TNResponseType.java
 *
 */

package com.nightfire.spi.neustar_soa.portps;

public class TNResponseType  implements java.io.Serializable {
    private java.lang.String telephoneNumber;

    private java.lang.String queryStatus;

    private com.nightfire.spi.neustar_soa.portps.PortingInfoType portingInfo;

    private com.nightfire.spi.neustar_soa.portps.PortingInfoType[] portingHistory;

    public TNResponseType() {
    }

    public TNResponseType(
           java.lang.String telephoneNumber,
           java.lang.String queryStatus,
           com.nightfire.spi.neustar_soa.portps.PortingInfoType portingInfo,
           com.nightfire.spi.neustar_soa.portps.PortingInfoType[] portingHistory) {
           this.telephoneNumber = telephoneNumber;
           this.queryStatus = queryStatus;
           this.portingInfo = portingInfo;
           this.portingHistory = portingHistory;
    }


    /**
     * Gets the telephoneNumber value for this TNResponseType.
     * 
     * @return telephoneNumber
     */
    public java.lang.String getTelephoneNumber() {
        return telephoneNumber;
    }


    /**
     * Sets the telephoneNumber value for this TNResponseType.
     * 
     * @param telephoneNumber
     */
    public void setTelephoneNumber(java.lang.String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }


    /**
     * Gets the queryStatus value for this TNResponseType.
     * 
     * @return queryStatus
     */
    public java.lang.String getQueryStatus() {
        return queryStatus;
    }


    /**
     * Sets the queryStatus value for this TNResponseType.
     * 
     * @param queryStatus
     */
    public void setQueryStatus(java.lang.String queryStatus) {
        this.queryStatus = queryStatus;
    }


    /**
     * Gets the portingInfo value for this TNResponseType.
     * 
     * @return portingInfo
     */
    public com.nightfire.spi.neustar_soa.portps.PortingInfoType getPortingInfo() {
        return portingInfo;
    }


    /**
     * Sets the portingInfo value for this TNResponseType.
     * 
     * @param portingInfo
     */
    public void setPortingInfo(com.nightfire.spi.neustar_soa.portps.PortingInfoType portingInfo) {
        this.portingInfo = portingInfo;
    }


    /**
     * Gets the portingHistory value for this TNResponseType.
     * 
     * @return portingHistory
     */
    public com.nightfire.spi.neustar_soa.portps.PortingInfoType[] getPortingHistory() {
        return portingHistory;
    }


    /**
     * Sets the portingHistory value for this TNResponseType.
     * 
     * @param portingHistory
     */
    public void setPortingHistory(com.nightfire.spi.neustar_soa.portps.PortingInfoType[] portingHistory) {
        this.portingHistory = portingHistory;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof TNResponseType)) return false;
        TNResponseType other = (TNResponseType) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.telephoneNumber==null && other.getTelephoneNumber()==null) || 
             (this.telephoneNumber!=null &&
              this.telephoneNumber.equals(other.getTelephoneNumber()))) &&
            ((this.queryStatus==null && other.getQueryStatus()==null) || 
             (this.queryStatus!=null &&
              this.queryStatus.equals(other.getQueryStatus()))) &&
            ((this.portingInfo==null && other.getPortingInfo()==null) || 
             (this.portingInfo!=null &&
              this.portingInfo.equals(other.getPortingInfo()))) &&
            ((this.portingHistory==null && other.getPortingHistory()==null) || 
             (this.portingHistory!=null &&
              java.util.Arrays.equals(this.portingHistory, other.getPortingHistory())));
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
        if (getTelephoneNumber() != null) {
            _hashCode += getTelephoneNumber().hashCode();
        }
        if (getQueryStatus() != null) {
            _hashCode += getQueryStatus().hashCode();
        }
        if (getPortingInfo() != null) {
            _hashCode += getPortingInfo().hashCode();
        }
        if (getPortingHistory() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPortingHistory());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPortingHistory(), i);
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
        new org.apache.axis.description.TypeDesc(TNResponseType.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "TNResponseType"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("telephoneNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("", "TelephoneNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("queryStatus");
        elemField.setXmlName(new javax.xml.namespace.QName("", "QueryStatus"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("portingInfo");
        elemField.setXmlName(new javax.xml.namespace.QName("", "PortingInfo"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "PortingInfoType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("portingHistory");
        elemField.setXmlName(new javax.xml.namespace.QName("", "PortingHistory"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "PortingInfoType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("", "PortingInfo"));
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

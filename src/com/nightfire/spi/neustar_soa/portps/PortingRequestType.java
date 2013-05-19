/**
 * PortingRequestType.java
 *
 */

package com.nightfire.spi.neustar_soa.portps;

public class PortingRequestType  implements java.io.Serializable {
    private java.lang.String userName;

    private java.lang.String password;

    private java.lang.String getHistory;

    private java.lang.String[] TNList;

    public PortingRequestType() {
    }

    public PortingRequestType(
           java.lang.String userName,
           java.lang.String password,
           java.lang.String getHistory,
           java.lang.String[] TNList) {
           this.userName = userName;
           this.password = password;
           this.getHistory = getHistory;
           this.TNList = TNList;
    }


    /**
     * Gets the userName value for this PortingRequestType.
     * 
     * @return userName
     */
    public java.lang.String getUserName() {
        return userName;
    }


    /**
     * Sets the userName value for this PortingRequestType.
     * 
     * @param userName
     */
    public void setUserName(java.lang.String userName) {
        this.userName = userName;
    }


    /**
     * Gets the password value for this PortingRequestType.
     * 
     * @return password
     */
    public java.lang.String getPassword() {
        return password;
    }


    /**
     * Sets the password value for this PortingRequestType.
     * 
     * @param password
     */
    public void setPassword(java.lang.String password) {
        this.password = password;
    }


    /**
     * Gets the getHistory value for this PortingRequestType.
     * 
     * @return getHistory
     */
    public java.lang.String getGetHistory() {
        return getHistory;
    }


    /**
     * Sets the getHistory value for this PortingRequestType.
     * 
     * @param getHistory
     */
    public void setGetHistory(java.lang.String getHistory) {
        this.getHistory = getHistory;
    }


    /**
     * Gets the TNList value for this PortingRequestType.
     * 
     * @return TNList
     */
    public java.lang.String[] getTNList() {
        return TNList;
    }


    /**
     * Sets the TNList value for this PortingRequestType.
     * 
     * @param TNList
     */
    public void setTNList(java.lang.String[] TNList) {
        this.TNList = TNList;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof PortingRequestType)) return false;
        PortingRequestType other = (PortingRequestType) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.userName==null && other.getUserName()==null) || 
             (this.userName!=null &&
              this.userName.equals(other.getUserName()))) &&
            ((this.password==null && other.getPassword()==null) || 
             (this.password!=null &&
              this.password.equals(other.getPassword()))) &&
            ((this.getHistory==null && other.getGetHistory()==null) || 
             (this.getHistory!=null &&
              this.getHistory.equals(other.getGetHistory()))) &&
            ((this.TNList==null && other.getTNList()==null) || 
             (this.TNList!=null &&
              java.util.Arrays.equals(this.TNList, other.getTNList())));
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
        if (getUserName() != null) {
            _hashCode += getUserName().hashCode();
        }
        if (getPassword() != null) {
            _hashCode += getPassword().hashCode();
        }
        if (getGetHistory() != null) {
            _hashCode += getGetHistory().hashCode();
        }
        if (getTNList() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getTNList());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getTNList(), i);
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
        new org.apache.axis.description.TypeDesc(PortingRequestType.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://neustar.com/portps/PortpsService/", "PortingRequestType"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "UserName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("password");
        elemField.setXmlName(new javax.xml.namespace.QName("", "Password"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getHistory");
        elemField.setXmlName(new javax.xml.namespace.QName("", "GetHistory"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("TNList");
        elemField.setXmlName(new javax.xml.namespace.QName("", "TNList"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("", "TelephoneNumber"));
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

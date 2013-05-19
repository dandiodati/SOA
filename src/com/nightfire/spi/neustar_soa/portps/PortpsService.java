/**
 * PortpsService.java
 *
 */

package com.nightfire.spi.neustar_soa.portps;

public interface PortpsService extends javax.xml.rpc.Service {
    public java.lang.String getPortpsServiceBindingAddress();

    public com.nightfire.spi.neustar_soa.portps.PortpsServicePort getPortpsServiceBinding() throws javax.xml.rpc.ServiceException;

    public com.nightfire.spi.neustar_soa.portps.PortpsServicePort getPortpsServiceBinding(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}

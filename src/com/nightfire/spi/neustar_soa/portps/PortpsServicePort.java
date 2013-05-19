/**
 * PortpsServicePort.java
 *
 */

package com.nightfire.spi.neustar_soa.portps;

public interface PortpsServicePort extends java.rmi.Remote {
    public com.nightfire.spi.neustar_soa.portps.PortingResponseType getPortingInformation(com.nightfire.spi.neustar_soa.portps.PortingRequestType parameters) throws java.rmi.RemoteException;
}

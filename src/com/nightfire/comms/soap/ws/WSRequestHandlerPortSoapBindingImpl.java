/**
 * WSRequestHandlerPortSoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package com.nightfire.comms.soap.ws;

import com.nightfire.framework.util.Debug;
import com.nightfire.comms.soap.ServiceInitializer;
import com.nightfire.comms.soap.handler.WSRequestHandler;

import java.rmi.RemoteException;

public class WSRequestHandlerPortSoapBindingImpl implements WSRequestHandlerPortType
{
	
	private static final String GWS_SERVER_NAME = "GWS_SERVER_NAME";

    public void processAsync ( java.lang.String header, java.lang.String request ) throws java.rmi.RemoteException 
    {
        try
        {
            WSRequestHandler rh = new WSRequestHandler( );
            rh.setInstanceId((String)(ServiceInitializer.getInitParameters().get(GWS_SERVER_NAME)));

            rh.processAsync( header, request );
        }
        catch ( Throwable t )
        {
            Debug.error( t.toString() );

            throw new RemoteException( "WSRequest-handler processAsync() call failed", t );
        }
    }


    public java.lang.String processSync ( java.lang.String header, java.lang.String request ) throws java.rmi.RemoteException 
    {
        try
        {
            WSRequestHandler rh = new WSRequestHandler( );
            rh.setInstanceId((String)(ServiceInitializer.getInitParameters().get(GWS_SERVER_NAME)));

            return( rh.processSync( header, request ) );
        }
        catch ( Throwable t )
        {
            Debug.error( t.toString() );

            throw new RemoteException( "WSRequest-handler processSync() call failed", t );
        }
    }

}

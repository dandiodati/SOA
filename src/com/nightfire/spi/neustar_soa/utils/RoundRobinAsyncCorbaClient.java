package com.nightfire.spi.neustar_soa.utils;

import java.util.List;

import org.omg.CORBA.StringHolder;

import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;

public class RoundRobinAsyncCorbaClient extends RoundRobinCorbaClient {

    /**
     * Constructor.
     *
     * @param serverNames List the list containing the string
     *                         server names of the CORBA servers that
     *                         should be tried in turn by this client.
     */
    public RoundRobinAsyncCorbaClient( List serverNames,String serverKey, String serverTypePrefix,long downServerRetryTime ){

       super( serverNames,serverKey,serverTypePrefix,downServerRetryTime );

    }

    /**
     * This overrides the parent class to call processAsync()
     * on the given RequestHandler instance.
     *
     * @param header String the header to be sent.
     * @param request String the actual message string to be sent
     * @param handler RequestHandler the request handler instance
     *                to which the request will be delivered.
     *
     */
    protected void send(String header,
                        String request,
                        RequestHandler handler,
                        StringHolder response)
                        throws InvalidDataException,
                               CorbaServerException,
                               NullResultException{

       handler.processAsync(header, request);

    }


}

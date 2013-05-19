
/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.connectors;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.corba.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;


public class SimpleCorbaClient implements Constants{


     private CorbaPortabilityLayer cpl = null;

    /**
     * This method sends asynchronous request messages and header messages
     * to NightFire's interface.
     *
     * @param targetSPI      A String containing the name of the SPI to
     *                       which the request is to be sent.
     * @param requestXML     A String containing the XML request message.
     *                       See the SupplierExpress Integration Guide for
     *                       information on the form of this message.
     * @param headerXML      A String containing the XML header that is
     *                       used for routing messages. See the
     *                       SupplierExpress Integration Guide for information
     *                       on the form of this message.
     * @exception CorbaServerException  Thrown for system errors that prevent the
     *                                  successful transmission of a request.
     * @exception InvalidDataException  Thrown if errors exist in the request
     *                                  string that prevent the successful
     *
     */
    public void doProcessAsync ( String targetSPI,
                                 String requestXML, String headerXML )

        throws CorbaServerException, InvalidDataException, FrameworkException {

        ObjectLocator objLoc = new ObjectLocator(cpl.getORB());

        org.omg.CORBA.Object obj = null;

        Debug.log(Debug.UNIT_TEST, "SimpleCorbaClient: Finding target SPI: " + targetSPI);

        try {

            obj = objLoc.find(targetSPI);

        } catch (CorbaException e) {

            throw new CorbaServerException(CorbaServerExceptionType.UnknownError,
                "SimpleCorbaClient: Could not locate object:\n"
                + e.getMessage());
        }

        RequestHandler rh = RequestHandlerHelper.narrow(obj);

        try {

            rh.processAsync(headerXML, requestXML);

        } catch (InvalidDataException e) {

            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, "",
                                           e.errorMessage);
        }catch (CorbaServerException e) {

            throw new CorbaServerException(CorbaServerExceptionType.UnknownError,
                                           e.errorMessage);
        }
    }

    /**
     * This method sends asynchronous request messages and header messages
     * to NightFire's interface.
     *
     * @param targetSPI      A String containing the name of the SPI to
     *                       which the request is to be sent.
     * @param requestXML     A String containing the XML request message.
     *                       See the SupplierExpress Integration Guide for
     *                       information on the form of this message.
     * @param headerXML      A String containing the XML header that is
     *                       used for routing messages. See the
     *                       SupplierExpress Integration Guide for information
     *                       on the form of this message.
     * @exception CorbaServerException  Thrown for system errors that prevent the
     *                                  successful transmission of a request.
     * @exception InvalidDataException  Thrown if errors exist in the request
     *                                  string that prevent the successful
     *
     */
    public String doProcessSync ( String targetSPI,
                                 String requestXML, String headerXML )

        throws CorbaServerException, InvalidDataException, FrameworkException, NullResultException {

        ObjectLocator objLoc = new ObjectLocator(cpl.getORB());

        org.omg.CORBA.Object obj = null;

        Debug.log(Debug.UNIT_TEST, "SimpleCorbaClient: Finding target SPI: " + targetSPI);

        try {

            obj = objLoc.find(targetSPI);

        } catch (CorbaException e) {

            throw new CorbaServerException(CorbaServerExceptionType.UnknownError,
                "SimpleCorbaClient: Could not locate object:\n" );
        }

        Debug.log(Debug.UNIT_TEST, "Getting a Request Handler");
        RequestHandler rh = RequestHandlerHelper.narrow(obj);
        if(rh == null){

            throw new CorbaServerException(CorbaServerExceptionType.UnknownError,
                "SimpleCorbaClient: Request Handler null\n");

        }


        org.omg.CORBA.StringHolder response = new org.omg.CORBA.StringHolder("");

        try {

            rh.processSync(headerXML, requestXML, response);

        } catch (InvalidDataException e) {

            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, "",
                                           e.errorMessage);
        }catch (CorbaServerException e) {

            throw new CorbaServerException(CorbaServerExceptionType.UnknownError,
                                           e.errorMessage);
        }

        return response.value;
    }

    public CorbaPortabilityLayer initORB(Properties props)
    {
        if(cpl != null)
        {
          return cpl;
        }

        Debug.log(Debug.UNIT_TEST, "ORBagentAddr = " + props.getProperty(ORB_AGENT_ADDR_PROP));
        Debug.log(Debug.UNIT_TEST, "ORBagentPort = " + props.getProperty(ORB_AGENT_PORT_PROP));
        Debug.log(Debug.UNIT_TEST, "SVCnameroot = " + props.getProperty(ORB_AGENT_SVC_ROOT));
        Debug.log(Debug.UNIT_TEST, "ORBservices = " + props.getProperty(ORB_AGENT_SVC_PROP));

        String[] str = null;

        try
        {
            cpl = new CorbaPortabilityLayer(str, props, null);
        }
        catch (Exception e)
        {
            Debug.error("ERROR: SimpleCorbaClient.initORB(): Failed to create a CorbaPortabilityLayer object:\n" + e.getMessage());   
        }

        return cpl;
    }

}

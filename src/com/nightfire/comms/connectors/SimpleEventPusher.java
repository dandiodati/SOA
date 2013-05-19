
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


public class SimpleEventPusher {

      private CorbaPortabilityLayer cpl = null;
      private EventPushSupplier pushSupplier = null;

    /**
     * This method sends asynchronous request messages and header messages
     * to NightFire's interface.
     *
     * @param targetSPI      A String containing the name of the SPI to
     *                       which the request is to be sent.
     * @param requestXML     A String containing the XML request message.
     *                       See the SupplierExpress Integration Guide for
     *                       information on the form of this message.
     * @exception CorbaException  Thrown for system errors that prevent the
     *                                  successful transmission of a request.
     *
     */
    public void doSendEvent ( String channelName, String requestXML)
        throws CorbaException, FrameworkException {

        if(pushSupplier == null)
        {
          pushSupplier = new EventPushSupplier( cpl.getORB(), channelName, false );
        }

        pushSupplier.pushEvent(requestXML);
    }

    public CorbaPortabilityLayer initORB(Properties props)
    {
        if(cpl != null)
        {
          return cpl;
        }

        String[] str = null;

        try
        {
            cpl = new CorbaPortabilityLayer(str, props, null);
        }
        catch (Exception e)
        {
            Debug.error("ERROR: SimpleEventPusher.initORB(): Failed to create a CorbaPortabilityLayer object:\n" + e.getMessage());   
        }

        return cpl;
    }

}

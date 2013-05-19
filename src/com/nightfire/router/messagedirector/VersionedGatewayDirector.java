/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/R4.4/com/nightfire/router/messagedirector/VersionedGatewayDirector.java#1 $
 */
package com.nightfire.router.messagedirector;

import org.w3c.dom.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.spi.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.idl.*;
import com.nightfire.common.*;
import com.nightfire.router.*;

/**
 *  A message director that routes by using a combination of fields in the xml header
 *  for clearinghouse-ized gateways.
 * This message director will first try to find the server at on the local host.
 * If it is not found it will look through each of the alt orb address/ port combinations until
 * it finds the server. If it still can not find a server then it throws an exception.
 * @author Dan Diodati
 */
public class VersionedGatewayDirector
    extends IlecTypeDirector
{
    /**
     * This method determines if the current MessageDirector subclass can route this current message.
     *
     * @param header the xml header for this request comming in.
     * @param message the message comming in (will be xml in all current cases)
     * @return  boolean true if This MessageDirector can route this message or false if it can not.
     * @throws MessageException if the header is bad
     */

    public boolean canRoute(Document header, Object message) throws MessageException
    {
          XMLMessageParser parser = new XMLMessageParser(header);

          if ( !parser.exists(CustomerContext.INTERFACE_VERSION_NODE) )
          {
              return false;
          }
          else
          {
              return super.canRoute(header, message);
          }

     }

     /**
      * The method which determines the name of the SPI to route the message to.
      * This Director routes any header in the form of :
      *
      * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      * <!DOCTYPE header SYSTEM "file:./header.dtd">
      * <header>
      *   <Request value="lsr_preorder"/>
      *   <Supplier value="SBC"/>
      *   <InterfaceVersion value="LSOG6"/>
      *   <Subrequest value="address_validation"/>
      * </header>
      *
      * AlGORITHM:
      *
      *  Form the COS Name by the following procedure:
      *
      *  First an alias lookup is done for the Supplier name.
      *  Then fields are concatenated together to form a valid COS Name prepended
      *  with the starting root of the SPIs(defined in a property for RouterSupervisor).
      *
      * Next we check if there is an alias for the entire cos name.
      *
      * The default orb location is checked for the current cos name.
      * If none is found then we use the the cos name without the Subrequest part
      * to do another lookup.
      *
      *  If these two possible server names doesn't exist we next try to look through
      *  alternative orb locations. If we still do not find the server, then we throw a processingException
      *  with a message listing all orb locations and server names we went through.
      *  The above header would generated a cos name of
      *  "Nightfire.SPI.SBC.LSOG6.lsr_preorder.address_validation"
      *
      * An alternative name for this server will be:
      * "Nightfire.SPI.SBC.LSOG6.lsr_preorder" (no sub request)
      *
      * Both these names will be used to find the server.
      * @return RequestHandler - the RequestHandler representing the server.
      * @throws ProcessingException if processing fails
      * @throws MessageException if the message is bad.
      *
      */
     protected RequestHandler getHandler(Document header, Object message) throws ProcessingException, MessageException
     {

        RouterSupervisor sup = (RouterSupervisor) RouterSupervisor.getSupervisor();
        XMLMessageParser parser = new XMLMessageParser(header);

        String cosPrefix = sup.getProperty(RouterConstants.COS_NS_PREFIX_PROP);
         if (!StringUtils.hasValue(cosPrefix))
           throw new ProcessingException(RouterConstants.COS_NS_PREFIX_PROP + " property is empty or doesn't exist");

        StringBuffer cosName = new StringBuffer( cosPrefix );

        cosName.append(PERIOD);
        String temp;

        /**
         * Parsing requestType here so that it can be used to pass
         * as a parameter to validate the trading partner.
         *
         * here requestType will be in format such as "lsr_order", "lsr_preorder" etc.
         */
        String requestType = parser.getValue (HeaderNodeNames.REQUEST_NODE);
        String supplier = getRealName(parser.getValue(HeaderNodeNames.SUPPLIER_NODE));
        String subReqNodeValue = null;
        if(parser.exists (HeaderNodeNames.SUBREQUEST_NODE) )
             subReqNodeValue = parser.getValue(HeaderNodeNames.SUBREQUEST_NODE);

        String gwSuppName = getGWSupplier ( supplier, requestType, subReqNodeValue);

        cosName.append( gwSuppName );
        cosName.append(PERIOD);

        // Add InterfaceVersion
        cosName.append(parser.getValue(HeaderNodeNames.INTERFACE_VERSION_NODE) );
        cosName.append(PERIOD);

        cosName.append( requestType );


        if(StringUtils.hasValue(subReqNodeValue)) {
            cosName.append(PERIOD);
            cosName.append( subReqNodeValue );
        }


        String cid = null;

        if (parser.exists(HeaderNodeNames.CUSTOMER_ID_NODE) ) {
           cid = parser.getValue(HeaderNodeNames.CUSTOMER_ID_NODE);
        }


        StringBuffer errBuf = new StringBuffer();

        ServerObject obj = null;
        // first try to find a customer specific server
        if (StringUtils.hasValue(cid))
        {
            String cosNameWithCID = new StringBuffer(cosName.toString()).append("_").append(cid).toString();

            obj = findHandle(cosNameWithCID, subReqNodeValue, errBuf);
        }

        // if we did not find one try to find a default server
        if(obj == null)
            obj = findHandle(cosName.toString(),subReqNodeValue, errBuf);

        // If we still did not find one at this point
        // we never will
        if (obj == null)
           throw new ProcessingException("VersionedGatewayDirector: Could not locate server, the following servers were tried :\n" + errBuf.toString());

         Debug.log(Debug.MSG_STATUS, "VersionedGatewayDirector: Found server [" + obj.cosName +"]");

        return obj.requestHandler;
     }
}

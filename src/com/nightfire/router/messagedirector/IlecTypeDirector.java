/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.router.messagedirector;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.router.*;

import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.router.exceptions.UnKnownSPIException;
import com.nightfire.security.tpr.TradingPartnerRelationship;
import com.nightfire.security.tpr.TPRException;


import org.w3c.dom.*;

/**
 *  A message director that routes by using a combination of fields in the xml header.
 * This message director will first try to find the server at on the local host.
 * If it is not found it will look through each of the alt orb address/ port combinations until
 * it finds the server. If it still can not find a server then it throws an exception.
 * @author Dan Diodati
 */
public class IlecTypeDirector extends MessageDirectorBase implements RouterConstants
{

  /**
   * sets up properties,aliases,etc. so child classes NEED to call super.intialize().
   * @param key The property key
   * @param type The property type
   * @throws ProcessingException if intialization fails
   */
  public void initialize(String key, String type) throws ProcessingException
  {
     super.initialize(key,type);
  }

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

        if (!parser.exists(HeaderNodeNames.REQUEST_NODE) )
           return false;
        if (!parser.exists(HeaderNodeNames.SUBREQUEST_NODE) )
           return false;
        if (!parser.exists(HeaderNodeNames.SUPPLIER_NODE) )
           return false;

        return true;

   }


  /**
   * The method which determines the name of the SPI to route the message to.
   * This Director routes any header in the form of :
   *
   * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
   * <!DOCTYPE header SYSTEM "file:./header.dtd">
   * <header>
   *   <Request value="lsr_preorder"/>
   *   <Subrequest value="address_validation"/>
   *   <Subtype value="SAG"/>
   *   <Supplier value="Ameritech"/>
   * </header>
   *
   * where SubType is optional in the header.dtd.
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
   *  "Nightfire.SPI.blah.Ameritech.lsr_preorder.address_validation.SAG"
   *
   *  And if SubType also does not exist then it will not be added to the end:
   *  "Nightfire.SPI.Ameritech.lsr_preorder.address_validation"
   *
   * An alternative name for this server will be:
   * "Nightfire.SPI.blah.Ameritect.lsr_preorder.SAG" (no sub request)
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
      * Parsing requestType here so that it can be used as a
      * parameter to validate the trading partner (value from SUPPLIER_NODE).
      *
      * here requestType will be in format such as "lsr_order", "lsr_preorder" etc.
      */
     String requestType = parser.getValue(HeaderNodeNames.REQUEST_NODE);
     String supplier = getRealName(parser.getValue(HeaderNodeNames.SUPPLIER_NODE));

     String subReqNodeValue = null;
     if(parser.exists (HeaderNodeNames.SUBREQUEST_NODE) )
     {
       subReqNodeValue = parser.getValue(HeaderNodeNames.SUBREQUEST_NODE);
     }

     String gwSuppName = getGWSupplier ( supplier, requestType, subReqNodeValue);
     cosName.append( gwSuppName );
     cosName.append(PERIOD);

     cosName.append( requestType );

     if(StringUtils.hasValue(subReqNodeValue)) {
         cosName.append(PERIOD);
         cosName.append( subReqNodeValue );
     }


     if (parser.exists(HeaderNodeNames.SUBTYPE_NODE) ) {
        temp = getRealName(parser.getValue(HeaderNodeNames.SUBTYPE_NODE) );
        if (StringUtils.hasValue(temp) ) {
           cosName.append(PERIOD);
           cosName.append( temp );
        }
     }

     String cid = null;

     if (parser.exists(CustomerContext.CUSTOMER_ID_NODE) ) {
        cid = parser.getValue(CustomerContext.CUSTOMER_ID_NODE);
     }

     StringBuffer errBuf = new StringBuffer();

     ServerObject obj = null;


    // If ORB-Routing-RequestType list is available and
    // the current request-type is included in the pipe-separated-list,
    // Get the routing-condition that holds true for the current message.
    // Use the routing-condition as the key to locate the server from the alternate ORB.
    if ( StringUtils.hasValue (ORBRoutingRequestType) && ( StringUtils.indexOfIgnoreCase(ORBRoutingRequestType, requestType) > -1 ) )
    {
        StringTokenizer stk = new StringTokenizer(ORBRoutingRequestType, PIPE_SEP);
        boolean isValidRequestType = false;
        while (stk.hasMoreTokens())
        {
            if (stk.nextToken().equalsIgnoreCase(requestType))
            {
                isValidRequestType = true;
                break;
            }
        }

        if (isValidRequestType)
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "IlecTypeDirector: Request-Type [" + requestType + "] is valid for Conditional ORB Routing");

            // find the X-path which evaluates to true for the current message-body
            String ORBRoutingKey = getORBRoutingKey ( cosName.toString(), message );

            if ( StringUtils.hasValue (ORBRoutingKey) )
                obj = findHandle ( cosName.toString(), subReqNodeValue, ORBRoutingKey, errBuf );
        }
        else
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "IlecTypeDirector: Request-Type [" + requestType + "] is not valid for Conditional ORB Routing, so skipping conditional ORB Routing");
        }
    }

     // first try to find a customer specific server, if server was not located using the above alternate ORB
     if (StringUtils.hasValue(cid) && (obj == null))
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
        throw new ProcessingException("IlecTypeDirector: Could not locate server, the following servers were tried :\n" + errBuf.toString());

      Debug.log(Debug.MSG_STATUS, "IlecTypeDirector: Found server [" + obj.cosName +"]");

     return obj.requestHandler;
  }

    protected ServerObject findHandle(String cosName, String subReqNodeValue, StringBuffer errBuf) throws ProcessingException
    {

        Debug.log(Debug.MSG_STATUS,"IlecTypeDirector: Looking up alias for server name : " + cosName);

     String cosStringName = getRealName(cosName);
     String cosStringNameNoSubReq = null;

     // alternative name without subrequest
     if (StringUtils.hasValue(subReqNodeValue) ) {
		 int subReqNodeIndex = cosName.lastIndexOf(subReqNodeValue);
		 cosStringNameNoSubReq = cosName.substring (0, subReqNodeIndex - 1) + cosName.substring (subReqNodeIndex + subReqNodeValue.length());
     }


     // first try to find a server with either cosStringName or
     // cosStringNameNoSubReq
     ServerObject obj = findServer(cosStringName, cosStringNameNoSubReq, errBuf);

     // if we still can't find a server look in alternative orb locations
     if (obj == null) {
         Iterator iter = orbSpaces.iterator();
         OrbSpace space;

         while ( obj == null && iter.hasNext() ) {
             space = (OrbSpace)iter.next();
             obj = findServer(cosStringName, cosStringNameNoSubReq,space.addr,space.port, errBuf);

         }

      }

     return obj;

  }

    /**
     * Overloaded findHandle - supports matchedCondition parameter
     * The matchedCondition is used as a key to the map
     * containing X-Path to ORB mappings.
     * Alternate ORB configuration is obtained using this key,
     * And the server is located using this ORB, rather than the
     * Default ORB.
     *
     * @param cosName The name of the server
     * @param subReqNodeValue Sub-request value from request-header
     * @param routingCondition The X-Path to use as key to the ORB map
     * @param errBuf Errors will get appended here.
     * @return a <code>ServerObject</code> value
     */
    protected ServerObject findHandle(String cosName, String subReqNodeValue, String routingCondition, StringBuffer errBuf) throws ProcessingException
    {
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "IlecTypeDirector: findHandle - Looking up ORB for locating server [" + cosName + "] for condition [" + routingCondition + "]");

        String cosStringName = getRealName(cosName);
        String cosStringNameNoSubReq = null;

        // alternative name without subrequest
        if (StringUtils.hasValue(subReqNodeValue) ) {
            int subReqNodeIndex = cosName.lastIndexOf(subReqNodeValue);
            cosStringNameNoSubReq = cosName.substring (0, subReqNodeIndex - 1) + cosName.substring (subReqNodeIndex + subReqNodeValue.length());
        }

        OrbSpace space = (OrbSpace) conditionOrbMap.get(routingCondition);

        // first try to find a server with either cosStringName or  cosStringNameNoSubReq
        ServerObject obj = findServer(cosStringName, cosStringNameNoSubReq, space.addr, space.port, errBuf);

        if (!(obj == null))
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "IlecTypeDirector: findHandle - Found server [" + cosName + "] using alternate ORB Addr:Port [" + space.addr + ":" + space.port + "]");
        }
        return obj;
  }

    private ServerObject findServer(String name, String alt, StringBuffer errBuf) throws ProcessingException
    {
        return findServer(name, alt, null, null,errBuf);
    }


    /**
     * Returns a server with the a the specified name or
     * a server with the alternative name if one was not found.
     * If there is no serve with name or alt then null is returned
     *
     * @param name The name of the server
     * @param alt A alternative name to server if one with 'name' does not exist.
     * @param addr The address of the server
     * @param port The port of the server
     * @param errBuf Errors will get appended here.
     * @return a <code>ServerObject</code> value
     */
    private ServerObject findServer(String name, String alt, String addr, String port, StringBuffer errBuf) throws ProcessingException
    {
        ServerObject obj = null;
        try {
            if (addr == null)
                obj = RouterSupervisor.getAvailableServers().getServerObject(name);
            else
                obj = RouterSupervisor.getAvailableServers().getServerObject(addr,port,name);
        } catch (UnKnownSPIException uke) {
            errBuf.append(uke.getMessage() + "\n");
            if(StringUtils.hasValue(alt) ) {

                try {
                    if (addr == null)
                        obj = RouterSupervisor.getAvailableServers().getServerObject(alt);
                    else
                        obj = RouterSupervisor.getAvailableServers().getServerObject(addr,port,alt);
                } catch (UnKnownSPIException uke2) {
                    errBuf.append(uke2.getMessage() + "\n");
                }

            }

        }

        return obj;
    }

    /**
     * This method validates the coming trading partner name
     * with the transaction. If the trading partner name 
     * validates successfully then the gateway supplier name
     * corressponding to trading partner name will be returned
     * otherwise the coming <tpName> will be returned.
     *
     * @param tpName      - trading partner name
     * @param transaction - transation like "lsr_order" etc
     * @param service     - service like "loop" etc
     * @return            - String
     */
    protected String getGWSupplier (String tpName, String transaction, String service)
    {
        String gwSuppName = tpName;
        try
        {
            CustomerContext customerContext = CustomerContext.getInstance();
            TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(customerContext.getCustomerID());
            if (tpr.isValidTradingPartner ( tpName, transaction))
            {
                gwSuppName = tpr.getGatewaySupplier (tpName, transaction, service);
            }
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "IlecTypeDirector.getGWSupplier: For tpName [" + tpName +"], transaction [" + transaction +"] & service [" + service +"], gateway Supplier [" + gwSuppName +"] is found");
            }
        }
        catch (TPRException e)
        {
            Debug.log(Debug.MSG_STATUS, "IlecTypeDirector.getGWSupplier: Error: [" + e.getMessage () +"]");
        }
        catch (FrameworkException e)
        {
            Debug.log(Debug.MSG_STATUS, "IlecTypeDirector.getGWSupplier: Error: [" + e.getMessage () +"]");
        }
        return gwSuppName;
    }

    /**
     * If map containing X-Path condition to ORB mapping is not empty,
     * parse the input message body and test each X-Path iteratively
     * to find the first match.
     *
     * @param serverName The name of the server
     * @param message The input message
     * @return the X-Path condition that evalautes to true for the given message
     */
    protected String getORBRoutingKey ( String serverName, Object message)
    {
        String routingCondition = null;
        boolean matchFound = false;

        // Only parse the message, if any entry is available in the XpathCondition-ORB map
        if (!(conditionOrbMap.isEmpty()))
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "getORBRoutingKey: found [" + conditionOrbMap.size() + "] ORB-routing conditions");

            try {
                // create Document object from the message string
                Document messageBody = XMLLibraryPortabilityLayer.convertStringToDom(message.toString());
                Iterator iter = conditionOrbMap.keySet().iterator();

                while (iter.hasNext()) {
                    routingCondition = (String) iter.next();
                    ParsedXPath  xPath = new ParsedXPath (routingCondition);
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "getORBRoutingKey : Checking against xpath [" + routingCondition + "]");

                    // Check if the given Xpath-Condition holds true for the message body
                    // if so, set matchFound to true and break
                    if (xPath.getBooleanValue(messageBody) == true)
                    {
                    matchFound = true;
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "getORBRoutingKey : Found an xpath match with [" + routingCondition + "]");
                    break;
                    }
                    else
                    {
                        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log(Debug.MSG_STATUS, "getORBRoutingKey : Could not find an xpath match with [" + routingCondition + "]");
                    }
                } // End of while
            }
            catch (FrameworkException e)
            {
            Debug.log(Debug.MSG_ERROR, "getORBRoutingKey: Failed to evaluate xpath boolean expression. [" + routingCondition + "]\n " + e.getMessage() );
            }
        } // End of - if (!(serviceOrbMap.isEmpty()))

        // if any match was found, return that otherwise return null
        if (matchFound)
            return routingCondition;
        else
            return null;
    }
}

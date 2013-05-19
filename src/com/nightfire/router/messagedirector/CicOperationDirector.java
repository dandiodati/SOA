/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.messagedirector;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.*;
import com.nightfire.framework.message.*;

import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.util.*;

import com.nightfire.router.*;
import com.nightfire.router.util.*;

import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;



import org.w3c.dom.*;

/**
 *  A message director that routes by a cic code and operation type.
 * @author Dan Diodati
 */
public class CicOperationDirector extends MessageDirectorBase implements RouterConstants
{

  public final static String CIC_NODE = "CIC";
  public final static String OPERATION_NODE = "OPERATION";

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

        if (!parser.exists(CIC_NODE) )
           return false;
        if (!parser.exists(OPERATION_NODE) )
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
   *   <AccountID value = "blah" />
   * </header>
   *
   * where AccountID and SubType are option in the header.dtd.
   *
   * AlGORITHM:
   *
   *  Form the COS Name by the following procedure:
   *
   *  Fields are concatenated together to form a valid COS Name prepended
   *  with the starting root of the SPIs(defined in a property for RouterSupervisor).
   *
   *  The above header would generated a cos name of
   *  "Nightfire.SPI.blah.Ameritech.lsr_preorder.address_validation.SAG"
   *
   *  If account ID was not there then the following name would be generated:
   *  "Nightfire.SPI.Ameritech.lsr_preorder.address_validation.SAG"
   *
   *  And if SubType also does not exist then it will not be added to the end:
   *  "Nightfire.SPI.Ameritech.lsr_preorder.address_validation"
   *
   * @return RequestHandler - the RequestHandler representing the server.
   * @throws ProcessingException if processing fails
   * @throws MessageException if the message is bad.
   *
   */
  protected RequestHandler getHandler(Document header, Object message) throws ProcessingException, MessageException
  {

     XMLMessageParser parser = new XMLMessageParser(header);
     String provider = getRealName(parser.getValue(CIC_NODE) );

     String operation = parser.getValue(OPERATION_NODE);
    
     
     if (!StringUtils.hasValue(provider) || !StringUtils.hasValue(operation) )  {
        String err = "Missing values in header serviceProvider[" + provider
                                       + "] and operation type[" + operation + "]";

        Debug.log(Debug.ALL_ERRORS,"CicOperationDirector: " + err);
        throw new MessageException(err);
     }

     ProviderOperationVisitor visitor = new ProviderOperationVisitor(provider, operation);

     RouterSupervisor.getAvailableServers().traverseServerObjects(visitor);

     ServerObject obj = visitor.getMatchingServer();

     
     if (obj == null) {
        String err = "No matching server found with serviceProvider[" + provider
                                       + "] and operation type[" + operation + "]";

        Debug.log(Debug.ALL_ERRORS,"CicOperationDirector: " + err);

        throw new ProcessingException(err);
     }

     return obj.requestHandler;
  }

  /**
   * This visitor finds the first server object that has a matching service provider and operation.
   */
  private final class ProviderOperationVisitor extends PatternVisitor
  {

     private ServerObject serverObj;

     /**
      * constructor
      * @param serviceProvider The prefix of a server to match.
      * @param  operationType The operation to match on
      */
     public ProviderOperationVisitor(String serviceProvider, String operationType)
     {
        super(serviceProvider,null, operationType,null);
     }

     /**
      * The method is called on the vistor for every ServerObject encountered.
      * @param serverObjects It is a server object at the current leave node.
      * ORBagentAddr     - the address for the serverobjects.
      * ORBagentPort     - the port for the serverobjects
      */
     public void visit(ServerObject obj, String ORBagentAddr, String ORBagentPort)
     {
        for ( int i = 0; i < obj.usageDescription.length; i++ ) {
            if ( match (obj.usageDescription[i], serviceProvider, interfaceVersion, operationType, async) ) {
               serverObj = obj;
               setDone(true);  // stop visiting nodes
               break;         // break out of this local loop
            }
        }
     }

    /**
     * returns the matching server object found.
     *
     * @return ServerObject The matching server object. If no matching server object was found
     * then null is returned.
     */
     public ServerObject getMatchingServer() {
        return serverObj;
     }
  }

}
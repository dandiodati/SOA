/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.router.util;

import com.nightfire.router.*;
import com.nightfire.router.util.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import java.util.*;


/**
 * This visitor finds all ServerObject that matches the usageDescription pattern
 * @author Dan Diodati
 */
public class PatternVisitor extends AbstractVisitor
{

     private List list;

     protected String serviceProvider = null;;
     protected String interfaceVersion = null;
     protected String operationType = null;
     protected Boolean async = null;


     /**
      * This takes parameters to be used to match a usagedescription
      * It is based on Query by Example.
      * @param serviceProvider The service provider to match or null if it should match all service providers.
      * @param interfaceVersion The interface version or null if it should match all interface versions.
      * @param operationType  The operation type to match or null to match all operation types.
      * @param async true for async, false for sync, or null to match any type of server.
      */
     public PatternVisitor(String serviceProvider, String interfaceVersion, String operationType,
                           Boolean async ) {
        this.serviceProvider = serviceProvider;
        this.interfaceVersion = interfaceVersion;
        this.operationType = operationType;
        this.async = async;

        list = new ArrayList();
     }

     /**
      * The method is called on the vistor for every ServerObject map encountered.
      * @param serverObjects It is a server object at the current leave node.
      * ORBagentAddr     - the address for the serverobjects.
      * ORBagentPort     - the port for the serverobjects
      */
     public void visit(ServerObject obj, String ORBagentAddr, String ORBagentPort)
     {
        for ( int i = 0; i < obj.usageDescription.length; i++ ) {
            if ( match (obj.usageDescription[i], serviceProvider, interfaceVersion, operationType, async) )
               list.add(obj.usageDescription[i]);
        }
     }

     /**
      * This returns all the usageDescriptions that was found by the visitor.
      * @returns UsageDescription[] of all matching usage descriptions
      * or returns null if none were found.
      * Users will have to case each object into a UsageDescription
      */
     public UsageDescription[] getUsageDescriptions() {
       return ( (UsageDescription[]) list.toArray( new UsageDescription[list.size()] ) );
     }


  // checks to see if the current UsageDescription matches the query values.
  //
  protected boolean match(UsageDescription ud, String serviceProvider, String interfaceVersion, String operationType,
                           Boolean async)
  {


     if (serviceProvider != null && !ud.serviceProvider.equals(serviceProvider) )
        return false;

     if (operationType != null && !ud.OperationType.equals(operationType) )
        return false;

     if (async != null && !(ud.asynchronous == async.booleanValue() ) )
        return false;

     if (interfaceVersion != null && !ud.interfaceVersion.equals(interfaceVersion) )
        return false;


     return true;
  }

}
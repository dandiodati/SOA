/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.util;
import com.nightfire.router.*;

/**
 * finds the first ServerObject usagedescription that matches the pattern
 * @author Dan Diodati
 */
public class PatternExistenceVisitor extends PatternVisitor
{

    /**
      * This takes parameters to be used to match a usagedescription
      * It is based on Query by Example.
      * @param serviceProvider The service provider to match or null if it should match all service providers.
      * @param interfaceVersion The interface version or null if it should match all interface versions.
      * @param operationType  The operation type to match or null to match all operation types.
      * @param async true for async, false for sync, or null to match any type of server.
      */
   public PatternExistenceVisitor(String serviceProvider, String interfaceVersion, String operationType,
                           Boolean async ) {

     super(serviceProvider, interfaceVersion, operationType, async);
   }

  /**
   * The visit method that gets called.
   * @param obj The current server object
   * @param ORBagentAddr The orb address of this obj
   * @param ORBagentPort The orb port of this obj
   */
  public void visit(ServerObject obj, String ORBagentAddr, String ORBagentPort)
  {

        for ( int i = 0; i < obj.usageDescription.length; i++ ) {

            if ( match (obj.usageDescription[i], serviceProvider, interfaceVersion, operationType, async) ) {
               setDone(true);

               break;
            }
        }
  }



  /**
   * users will call this method to determine if a single matching pattern was found.
   * @return boolean returns true if visiting is done, otherwise false
   */
  public boolean isFound() {
     return isDone();
  }

}
/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.util;

import com.nightfire.router.*;
import com.nightfire.router.util.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.framework.util.*;
import java.util.*;



/**
 * This visitor finds all ServerObject that matches the usageDescription pattern
 * @author Dan Diodati
 */
public class DescribeVisitor extends AbstractVisitor
{

     private StringBuffer description;
     private static final String SEP = "|";
     private static final int ADDR_PAD = 17;
     private static final int PORT_PAD = 7;

     private String return_char = System.getProperty("line.separator");

     public DescribeVisitor()
     {

       description = new StringBuffer();
     }



     /**
      * The method is called on the vistor for every ServerObject map encountered.
      * @param serverObjects It is a server object at the current leave node.
      * ORBagentAddr     - the address for the serverobjects.
      * ORBagentPort     - the port for the serverobjects
      */
     public void visit(ServerObject obj, String ORBagentAddr, String ORBagentPort)
     {
        description.append("[orb address = ");
        description.append(ORBagentAddr);
        description.append("]" +  " [orb port = ");
        description.append(ORBagentPort);
        description.append("]" +  " [cos name = ");
        description.append(obj.cosName + "]");
        description.append(return_char);
     }

    /**
     * returns the description of server objects
     * @return String a description
     */
     public String getDescription() {
        return description.toString();
     }


}
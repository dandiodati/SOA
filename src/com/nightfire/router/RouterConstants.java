/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

/**
 * Constants used in the router
 * @author Dan Diodati
 */
public interface RouterConstants
{
    /**
    * Persistent property constants
    */


   public static final String NAME_PROP = "NAME";
   public static final String ROUTER_KEY = "ROUTER";

   public static final String SUPERVISOR_KEY = "SUPERVISOR_KEY";
   public static final String SUPERVISOR_TYPE = "SUPERVISOR_TYPE";

   public static final String ADMINSERVER_KEY = ROUTER_KEY;
   public static final String ADMINSERVER_TYPE = "ADMIN_SERVER";

   public static final String REQUESTSERVER_KEY = ROUTER_KEY;
   public static final String REQUESTSERVER_TYPE = "REQUEST_SERVER";

   public static final String SPICALLER_KEY = "SPI_CALLER_KEY";
   public static final String SPICALLER_TYPE = "SPI_CALLER_TYPE";

   // location of properties for Choice evaluator
   public static final String CHOICE_EVAL_KEY = "CHOICE_EVALUATOR_KEY";
   public static final String CHOICE_EVAL_TYPE = "CHOICE_EVALUATOR_TYPE";

   public static final String COS_NS_PREFIX_PROP = "COS_NS_PREFIX";

   public static final String ORB_AGENT_PORT  = "ORBagentPort";
   public static final String ORB_AGENT_ADDR  = "ORBagentAddr";

   public static final String PERIOD = ".";
   public static final String PIPE_SEP = "|";    

   /**
    * request type constants
    */
    public static final int ASYNC_REQUEST  = 0;
    public static final int SYNC_REQUEST  = 1;
    public static final int SYNC_W_HEADER_REQUEST  = 2;


}
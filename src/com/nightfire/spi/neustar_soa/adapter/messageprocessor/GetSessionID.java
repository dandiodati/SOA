///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACComServer;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;

/**
* This class retrieves the NPAC Session ID for a particular customer's
* SPID from the static NPAC Adapter found in the NPAC Com Server.
* This class assumes that an instance of NPACComServer has already
* been created in the same virtual machine where this processor
* is being executed.
*/
public class GetSessionID extends MessageProcessorBase {

    private static final String INPUT_LOCATION_PROP = "SPID_INPUT_LOCATION";

    private static final String SESSION_OUTPUT_LOCATION_PROP =
                                     "SESSION_OUTPUT_LOCATION";

    /**
    * Label used for logging. 
    */
    private static final String CLASSNAME = "GetSessionID";

    /**
    * The input location.
    */
    private String spidLocation;

    /**
    * The output location.
    */
    private String sessionOutputLocation;

    public void initialize(String key, String type) throws ProcessingException{

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)){
           Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME+" : Initializing...");
        }

        super.initialize(key, type);

        // get the input location of the customer's SPID
        spidLocation = getRequiredPropertyValue(INPUT_LOCATION_PROP);

        // get the output location where the session ID should be set
        sessionOutputLocation =
           getRequiredPropertyValue(SESSION_OUTPUT_LOCATION_PROP);
        
    }

    public NVPair[] process(MessageProcessorContext context,
                            MessageObject input )
                            throws MessageException,
                                   ProcessingException {
    	
    	ThreadMonitor.ThreadInfo tmti = null;

       // this is not a batching processor, so if the input it null,
       // then return null
       if(input == null) return null;

       // get the customer's SPID from the input location
       String spid = get(spidLocation, context, input).toString();

       // get the global NPAC Adapter reference
       NPACAdapter adapter = NPACComServer.getAdapter();

       if(adapter == null){

          Debug.error(CLASSNAME+
                      " : The NPAC Adapter instance has not been initialized.");

       }
       else{
    	   
    	// lookup the session ID for the SPID
    	String sessionID = adapter.getSessionID(spid);
    	try{
    		   tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] getting sessionID" + sessionID );

    		   if(sessionID == null){

    			   Debug.error(CLASSNAME+
                         " : No session could be found for SPID ["+
                         spid+"]");

    		   }
    		   else if( sessionID.equals( NPACConstants.UNINITIALIZED_SESSION_ID ) ){

    			   Debug.error(CLASSNAME+
                         " : The session for SPID ["+spid+
                         "] is not initialized.");

    		   }
    		   else{

    			   if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

    				   Debug.log(Debug.MSG_STATUS,
                          "Retrived session ID ["+sessionID+
                          "] for SPID ["+spid+"]");

    			   }

    			   // put the session ID in the desired output location
    			   super.set(sessionOutputLocation, context, input, sessionID);

    		   }
    		   
    		   
    	   }finally{
			
    		   ThreadMonitor.stop( tmti );
    	   }
       
       }
       return formatNVPair( input );   
    }
} 
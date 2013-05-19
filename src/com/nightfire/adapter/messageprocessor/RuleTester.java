/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.adapter.messageprocessor;

//JDK packages
import java.util.*;

//NightFire packages
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;

/**
 * Evaluate rulesets (in property order), updating the
 * output or context with the result of the first condition that evaluates 
 * to true.
 */
public class RuleTester extends ConditionalProcessorBase
{
   /**
   * A name used at the beginning of log messages.
   */
   private static final String className = "RuleTester";

    
    /**
     * Called to initialize this component.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        Debug.log( Debug.MSG_STATUS, "Initializing" +className+ "......" );

        super.initialize( key, type );

        Debug.log( Debug.SYSTEM_CONFIG, className+ ": Initialization done." );

    }


    /**
     * Process the input message and (optionally) return
     * a value.
     *
     * @param  input  Input MessageObject that contains the message to be processed.
     * @param  mpcontext The context
     *
     * @return  Optional output NVPair array, or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if processing fails.
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input ) 
        throws MessageException, ProcessingException
    {
	 
        Debug.log (Debug.MSG_STATUS, "Processing"+className+"...");
	
	// If input is null return null
        if ( input == null )
             return null;

	String resultValue = executeTest(mpcontext, input);
	
	if(resultValue != null)
	{ 
	set( resultLocation, mpcontext, input, resultValue);
        	
       		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        	{
                Debug.log( Debug.MSG_STATUS, "The ruleset evaluated to 'true', resulting in a return-value of [" 
                           + resultValue + "]." );
        	}
      	}	
    	else
        {        
       		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
		Debug.log( Debug.MSG_STATUS, "All the rulesets evaluated to 'false'." );
        }

        Debug.log (Debug.MSG_STATUS, "RuleTester processing done.");

        // send the result on through the chain according to properties.
	
        return( formatNVPair( input ) );
    }

}


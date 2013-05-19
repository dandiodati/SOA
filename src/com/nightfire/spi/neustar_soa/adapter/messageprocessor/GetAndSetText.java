////////////////////////////////////////////////////////////////////////////////
// @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.spi.common.driver.MessageProcessorBase;

import java.util.*;

import com.nightfire.adapter.messageprocessor.GetAndSet;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;

import com.nightfire.framework.util.*;

/**
 * This extension of the GetAndSet processor first tries normal GetAndSet
 * functionality, if no value was found, then it makes an attempt to find a text
 * value in the input XML.
 */
public class GetAndSetText extends GetAndSet {

   /**
    * Calls the parent's initialize() method. This just logs an extra message
    * to identify this as a GetAndSetText processor.
    */
   public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize(key, type);

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log(Debug.SYSTEM_CONFIG, "GetAndSetText: Initialized.");
		}

    }

    /**
     * Extract first text value available from the given locations.
     *
     * @param  locations  A set of '|' delimited XML locations to check.
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The requested value.
     *
     * @exception  MessageException  Thrown on non-processing errors.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected Object getValue ( String locations,
                                MessageProcessorContext mpContext,
                                MessageObject inputObject )
                                throws MessageException,
                                       ProcessingException
    {
    	
    	
    	ThreadMonitor.ThreadInfo tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] Getting Text Value" );
    	try{
        // check for the possibility of multiple paths
    		StringTokenizer st = new StringTokenizer( locations,
                                                  MessageProcessorBase.SEPARATOR );

    		Object result = null;

        // for each path and while we have not yet found a result
    		while ( st.hasMoreTokens() && result == null)
    		{

            String path = st.nextToken( );
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log( Debug.MSG_STATUS,
						   "Checking location [" + path +
						   "] for value ..." );
			}

            // this first checks to see if the value exists if the
            // input message or context
            if ( exists( path, mpContext, inputObject, false) ){

               // try to get the text value from the input message
               if (!path.startsWith(MessageProcessorBase.CONTEXT_START)) {
                  if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                  Debug.log(Debug.MSG_STATUS,
                            "Checking location [" + path +
                            "] for text value ...");
				  }

                  XMLMessageParser inputMessage =
                     new XMLMessageParser(inputObject.getDOM());

                  if (inputMessage.textValueExists(path)) {
                     result = inputMessage.getTextValue(path);
					 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 Debug.log(Debug.MSG_STATUS,
								   "Found text value [" + result + "]");
					 }
                  }

               }

               // if no text value was found, then get the value in the
               // default manner
               if (result == null) {

                  result = get(path, mpContext, inputObject);

               }

            }

        }
        return result;
        
    	}finally{
			
    		ThreadMonitor.stop( tmti );
    	}
    
    }

}
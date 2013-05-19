/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
//import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.idl.*;

/**
 * A router component that sends the request header and message to the SPI.
 * @author Dan Diodati
 */
public class SPICaller extends GatewayCaller implements RouterConstants{

  private static SPICaller singleton = new SPICaller();

  private SPICaller() {}

  public static SPICaller getInstance() {
		return singleton;
  }

  /**
   * initials this class
   * @param key The property key
   * @param type The property type
   * @throws ProcessingException if it fails to intialize
   */
  public void initialize(String key,String type) throws ProcessingException
  {
     if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        Debug.log(Debug.SYSTEM_CONFIG, "SPICaller - starting up");
     
     super.initialize(key, type);
  }

  /**
   * initials this class with out properties
   * @throws ProcessingException if it fails to intialize
   */
  public void initialize() throws ProcessingException
  {
     if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
         Debug.log(Debug.SYSTEM_CONFIG,"SPICaller - starting up");
  }

  /**
   * performs clean up
   * @throws ProcessingException if there is an error
   */
  public void cleanup() throws ProcessingException
  {
     if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
         Debug.log(Debug.SYSTEM_CONFIG,"SPICaller - shutting down");
     
     super.cleanup();
  }


    /**
     * Makes an sync call
     * @param rh A handle to the server to call
     * @param header The xml header
     * @param message The xml message
     * @param reqType - indicates the type of request, refer to RouterConstants.
     * @see RouterConstants
     * @return Object The response
     * @throws ProcessingException if there is a processing error.
     * @throws MessageException if there is an error with the message
     */
    public final Object processRequest(RequestHandler rh, String header, Object message, int reqType) throws ProcessingException,MessageException
    {
        
        try {
            
            Object data  = sendRequest(rh, header, (String) message, reqType);
            return data;
        }
        catch (MessageException e) {
            Debug.warning("SPICaller: The following error was returned from the gateway: " + e.getMessage() );
            throw e;
            
        } catch (ProcessingException e) {
            Debug.error("SPICaller: The following error was returned from the gateway: " + e.getMessage() );
            throw e;
        }
        
    }
}

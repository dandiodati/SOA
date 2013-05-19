/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.messagedirector;

import java.util.*;
import com.nightfire.router.*;
import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import org.w3c.dom.*;



/**
 *  Defines a MessageDirector which can route a message.
 *  New directors should use the MessageDirectorBase.
 * @author Dan Diodati
 */
public interface MessageDirector
{
   /**
    * Initialize this MessageDirector.
    * This is called during initialization of the router.
    * @param key The property key for this message director.
    * @param type The property type for this message director.
    * @throws ProcessingException if there is a fatal error during initialization.
    */
   public void initialize(String key,String type) throws ProcessingException;


   /**
    * Determines if this message director can route a message.
    * This is called by the ChoiceEvaluator to determine if this message director
    * knows how to route this message.
    * @param header The request header.
    * @param message The request message.
    * @return true if this message director can route this message, otherwise
    *          returns false.
    * @throws MessageException if there is an problem with the xml header or message.
    */
   public boolean canRoute(Document header, Object message) throws MessageException;

   /**
    * processes an asynchronous, synchronous, or synchronousWithHeader request
    * @param header The request header
    * @param message The request message
    * @param reqType - indicates the type of request, refer to RouterConstants.
    * @see RouterConstants
    * @return Response object if there is a response returned, otherwise null is returned.
    * @throws ProcessingException if there is a processing error
    * @throws MessageException if the request message or header is invalid.
    */
   public Object processRequest(Document header, Object message, int reqType) throws ProcessingException, MessageException;

   /**
    * performs any clean up when the router is shutdown.
    * @throws ProcessingException if there is an error during cleanup.
    */
   public void cleanup() throws ProcessingException;
}
////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.security.*;

/**
 * This is a message processor used to generically determine if a particular
 * user is allowed to perform a given action. The user and action
 * (or "permission") name are taken from locations in the message or
 * message context. If the user is authorized, then this processor just passes
 * the message through. If not, then a ProcessingException is thrown.
 */
public class SecurityAuthorizationProcessor extends MessageProcessorBase{

   /**
    * The name of the property used to indicate the location of the user
    * to be authorized. This is a required property.
    */
   public static final String USER_LOCATION_PROP = "USER_LOCATION";

   /**
    * The name of of the property used to indicate the action that the user is
    * trying to perform. This is a required property.
    */
   public static final String ACTION_LOCATION_PROP = "ACTION_LOCATION";

   /**
    * This is the optional output location for the security error message
    * (if there is one). If this property is not present, then
    * any security authorization failure message will be thrown as
    * a ProcessingException.
    */
   public static final String OUTPUT_LOCATION_PROP = "OUTPUT_LOCATION";

   /**
    * The value of the USER_LOCATION property.
    */
   private String userLocation;

   /**
    * The value of the ACTION_LOCATION property.
    */
   private String actionLocation;

   /**
    * The value of the OUTPUT_LOCATION, if any.
    */
   private String outputLocation;

   /**
    * Uses properties to get the locations to look for the .
    *
    * @param key String the property key used to get this processor's properties
    *                   from the persistent property table.
    * @param type String the property type used to get this processor's
    *                    properties from the persistent property table.
    * @throws ProcessingException thrown if the required properties cannot be
    *                             found.
    */
   public void initialize(String key, String type)
                          throws ProcessingException {

      // get all properties from the database for the give key and type
      super.initialize(key, type);

      userLocation = super.getRequiredPropertyValue( USER_LOCATION_PROP );
      actionLocation = super.getRequiredPropertyValue( ACTION_LOCATION_PROP );
      outputLocation = super.getPropertyValue( OUTPUT_LOCATION_PROP );

   }

   /**
    * Retrieves the user and requested action from their configured location
    * in either the message or the context and runs these values through
    * the security service to see if the user has permission to execute
    * the requested action. If an output location was specified, any
    * authorization failure message will get placed in that location. If no
    * output location was specified, and authorization fails, the message
    * will be thrown as a processing exception. If the authorization succeeds,
    * then the input message will get passed on to the next processor
    * unchanged.
    *
    * @param context MessageProcessorContext the message processing context.
    * @param messageObject MessageObject the input message.
    * @throws MessageException
    * @throws ProcessingException thrown
    * @return NVPair[] contains the original input message unchanged, addressed
    *                  to the next processor.
    */
   public NVPair[] process(MessageProcessorContext context,
                           MessageObject messageObject)
                           throws MessageException,
                                  ProcessingException {
	   
	  ThreadMonitor.ThreadInfo tmti = null;

      if( messageObject == null ){

         // This is the default behavior when a message processor gets a
         // null input.
         return null;

      }

      // get the values of the user name and the action that the user is
      // trying to perform
      String user = super.getString( userLocation, context, messageObject );
      String permission =
         super.getString( actionLocation, context, messageObject );

      try{
    	  
    	 tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
         // check to see if the user is authorized to perform this action
         SecurityService.getInstance().authorize( user, permission );

      }
      catch(Exception ex){

         Debug.warning("Security authorization failed for user ["+
                       user+"] and permission ["+permission+"]: "+ex);

         if( outputLocation != null ){

            // place the exception message into its configured output
            // location in the message or context.
            super.set( outputLocation, context, messageObject, ex.getMessage());

         }
         else{

            // no output location was specified, so rethrow the security
            // exception as a processing exception.
            throw new ProcessingException( ex.getMessage() );

         }

      }
      finally
      {
    	  ThreadMonitor.stop(tmti);
      }

      // return the original message unharmed
      return super.formatNVPair( messageObject );

   }

}

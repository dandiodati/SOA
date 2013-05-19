/**
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;

import com.nightfire.spi.common.driver.*;

/**
* This takes an input interface version value and sets that as
* the interface version in the CustomerContext. This is being added for
* the multi-version interface support and provides a means for setting the
* interface version in the CustomerContext for use later on.
*/
public class InterfaceVersionSetter extends MessageProcessorBase{

   public static final String INTERFACE_VERSION_LOCATION_PROP =
                                 "INTERFACE_VERSION_LOCATION";

   public static final String DEFAULT_INTERFACE_VERSION_PROP =
                                 "DEFAULT_INTERFACE_VERSION";

   /**
   * Initializes the interface version input location/default value based on the properties.
   *
   * @param  key   Property-key to use for locating initialization properties.
   * @param  type  Property-type to use for locating initialization properties.
   * @exception ProcessingException when initialization fails
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      interfaceVersionLoc = getPropertyValue( INTERFACE_VERSION_LOCATION_PROP );
      defaultInterfaceVersion = getPropertyValue( DEFAULT_INTERFACE_VERSION_PROP );

      if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
      {

          Debug.log(Debug.SYSTEM_CONFIG,
                   "InterfaceVersionSetter initialized: Interface Version location: [" +
                   interfaceVersionLoc + "], default Interface Version: [" +
                   defaultInterfaceVersion + "]." );
      }

   }

   /**
   * This takes the interface version value from the location specified by
   * the INTERFACE_VERSION_LOCATION_PROP property or the default value specified by
   * the DEFAULT_INTERFACE_VERSION_PROP and sets that value as the interface version
   * in the CustomerContext.
   *
   * @param  context The context
   * @param  input  Input message to process.
   *
   * @return  The given input, or null.
   *
   * @exception  ProcessingException  Thrown if processing fails or if no interface
   *                                  version can be obtained.
   * @exception  MessageException  Thrown if message is bad.
   */
   public NVPair[] process(MessageProcessorContext context, MessageObject input )
                           throws MessageException, ProcessingException
   {

      // the traditional message processor response to a null input
      if(input == null) return null;

      //Get interface version
      String interfaceVersion = null;
      if ( (interfaceVersionLoc != null) && ( exists(interfaceVersionLoc, context, input) ) )
      {
          interfaceVersion = get(interfaceVersionLoc, context, input).toString();
      }
      else
      {
          interfaceVersion = defaultInterfaceVersion;
      }

      if ( interfaceVersion == null )
          throw new ProcessingException( "No interface version found in location ["+
          interfaceVersionLoc + "] and default value [" + defaultInterfaceVersion + "]." );

      //Set the interface version
      try
      {
          if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
              Debug.log( Debug.MSG_STATUS, "Setting interface version ["+ interfaceVersion +
                                           "] in the customer context.");

          CustomerContext.getInstance().setInterfaceVersion(interfaceVersion);
      }
      catch(FrameworkException fex)
      {
         // at the time of this writing, while the setInterfaceVersion() says
         // that it throws a FrameworkException, the actual code never
         // throws that exception, so this should never happen, but ...
         throw new ProcessingException("Could not set the interface version to ["+
                                       interfaceVersion+"]: "+fex.getMessage());
      }

      // return the input message unharmed
      return formatNVPair(input);

   }

   /**
    * The input location of the interface version.
    */
    private String interfaceVersionLoc;

   /**
    * The default value for interface version.
    */
    private String defaultInterfaceVersion;

}

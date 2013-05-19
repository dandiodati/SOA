/**
 * UserIdSetter.java
 * 
 * Notes:
 * =====
 * This class gets the user id from <USERID_LOCATION_PROP> or <DEFAULT_USERID_PROP>
 * value and sets that as the userid CustomerContext,
 * which is available for the specific gateway chain.
 * 
 * @author(s) PhaniKumar, Venkatramanan.S
 * @version 1.0
 * @Copyright (c) 2005 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 *
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.util.CustomerContext;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.FrameworkException;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.spi.common.driver.MessageObject; 
 * @see com.nightfire.spi.common.driver.MessageProcessorBase;
 */

/**
 * Revision History
 * ---------------------
 * Rev#   Modified By     Date               Reason
 * -----  ----------      --------        --------------------------
 *  1     Venkatramanan   09/11/2005         Created
 *
 */

package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
* This takes an input userid value and sets that as
* the userid in the CustomerContext. 
*/
public class UserIdSetter extends MessageProcessorBase{

   public static final String USERID_LOCATION_PROP =
                                 "USERID_LOCATION";

   public static final String DEFAULT_USERID_PROP =
                                 "DEFAULT_USERID";

   /**
    * The input location of the userid.
    */
    private String userIdLoc;

   /**
    * The default value for userid.
    */
    private String defaultUserId;

   /**
   * Initializes the userid input location/default value based on the properties.
   *
   * @param  key   Property-key to use for locating initialization properties.
   * @param  type  Property-type to use for locating initialization properties.
   * @exception ProcessingException when initialization fails
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      userIdLoc = getPropertyValue( USERID_LOCATION_PROP );
      defaultUserId = getPropertyValue( DEFAULT_USERID_PROP );

      if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
      {

          Debug.log(Debug.SYSTEM_CONFIG,
                   "UserIdSetter initialized: UserId location: [" +
                   userIdLoc + "], default UserId : [" +
                   defaultUserId + "]." );
      }

   }

   /**
   * This takes the user id value from the location specified by
   * the USERID_LOCATION_PROP property or the default value specified by
   * the DEFAULT_USERID_PROP and sets that value as the userid
   * in the CustomerContext.
   *
   * @param  context The context
   * @param  input  Input message to process.
   *
   * @return  The given input, or null.
   *
   * @exception  ProcessingException  Thrown if processing fails or if no UserID
   *                                  can be obtained.
   * @exception  MessageException  Thrown if message is bad.
   */
   public NVPair[] process(MessageProcessorContext context, MessageObject input )
                           throws MessageException, ProcessingException
   {

      // the traditional message processor response to a null input
      if(input == null) return null;

      //Get user ID
      String userId = null;
      if ( (userIdLoc != null) && ( exists(userIdLoc, context, input) ) )
      {
          userId = get(userIdLoc, context, input).toString();
      }
      else
      {
          userId = defaultUserId;
      }

      if ( userId == null )
          throw new ProcessingException( "No userid found in location ["+
          userIdLoc + "] and default value [" + defaultUserId + "]." );

      //Set the User ID
      try
      {
          if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
              Debug.log( Debug.MSG_STATUS, "Setting UserID["+ userId +
                                           "] in the customer context.");

          CustomerContext.getInstance().setUserID(userId);
      }
      catch(FrameworkException fex)
      {
         // at the time of this writing, while the setUserID() says
         // that it throws a FrameworkException, the actual code never
         // throws that exception, so this should never happen, but ...
         throw new ProcessingException("Could not set the userid to ["+
                                       userId+"]: "+fex.getMessage());
      }

      // return the input message unharmed
      return formatNVPair(input);

   }


}

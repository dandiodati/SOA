package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;

import com.nightfire.spi.common.driver.*;

/**
* This takes an input customer ID value and sets that as
* the current customer ID in the CustomerContext. This is used
* in response chains where the customer ID must be determined and then set
* dynamically based on the content of a response message.    
*/
public class CustomerSetter extends MessageProcessorBase{

   public static final String DEFAULT_CUSTOMER_ID_LOCATION =
                                     CONTEXT_START+
                                     CustomerContext.CUSTOMER_ID_COL_NAME+"."+
                                     // the column name returned by Oracle is
                                     // all uppercase
                                     CustomerContext.CUSTOMER_ID_COL_NAME.toUpperCase();

   public static final String CUSTOMER_ID_LOCATION_PROP =
                                 "CUSTOMER_ID_LOCATION";

   /**
   * The input location of the customer ID.  
   */
   private String customerIDLoc;

   /**
   * Initializes the customer ID input location based on the properties.
   *
   * @param  key   Property-key to use for locating initialization properties.
   * @param  type  Property-type to use for locating initialization properties.
   * @exception ProcessingException when initialization fails
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      customerIDLoc = getPropertyValue(CUSTOMER_ID_LOCATION_PROP,
                                       DEFAULT_CUSTOMER_ID_LOCATION);

      if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){

         Debug.log(Debug.SYSTEM_CONFIG,
                   "CustomerSetter initialized: Customer ID location: ["+
                   customerIDLoc+"]");


      }

   }

   /**
   * This takes the customer ID value from the location specified by
   * the CUSTOMER_ID_LOCATION proeprty and sets that value as the current
   * customer in the CustomerContext.
   */
   public NVPair[] process(MessageProcessorContext context,
                           MessageObject input )
                           throws MessageException,
                                  ProcessingException{

      // the traditional message processor response to a null input
      if(input == null) return null;

      String customerID = get(customerIDLoc, context, input).toString();

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
         Debug.log( Debug.MSG_STATUS,
                    "Setting customer ID ["+customerID+
                    "] in the customer context.");
      }

      try{
         // set the new customer ID
         CustomerContext.getInstance().setCustomerID(customerID);
      }
      catch(FrameworkException fex){
         // at the time of this writing, while the setCustomerID() says
         // that it throws a FrameworkException, the actual code never
         // throws that exception, so this should never happen, but ...
         throw new ProcessingException("Could not set the customer ID to ["+
                                       customerID+"]: "+fex.getMessage());

      }

      // return the input message unharmed
      return formatNVPair(input);

   }
   
} 
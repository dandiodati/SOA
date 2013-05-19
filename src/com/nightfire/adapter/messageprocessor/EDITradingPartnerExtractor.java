package com.nightfire.adapter.messageprocessor;

import com.nightfire.adapter.converter.edi.util.EDIUtils;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
* This message processor takes an EDI string as an input and then extracts
*
*/
public class EDITradingPartnerExtractor extends MessageProcessorBase {

   /**
   * The length of an ISA segment.
   * In X.12 (EDI) the size of the ISA segment is always 105 characters.
   */
   private static final int ISA_SEGMENT_LENGTH = 105;

   /**
   * Start index of Customer ID in ISA segment.
   */
   private static final int CUSTOMER_ID_START_INDEX = 54;

   /**
   * End index of Customer ID in ISA segment.
   */
   private static final int CUSTOMER_ID_END_INDEX = 69;

   /**
   * By default the input EDI string is the input message itself. 
   */
   private static final String DEFAULT_INPUT_MESSAGE_LOC = INPUT_MESSAGE;

   /**
   * This is the default output location where the ISA segment will be set.
   */
   private static final String DEFAULT_ISA_OUTPUT_LOCATION = CONTEXT_START+
                                                             "ISA";

   /**
   * This is the default output location where the customer trading partner
   * ID will be set.
   */
   private static final String DEFAULT_TPID_OUTPUT_LOCATION = CONTEXT_START+
                                                              "TPID";

   /**
   * This is the name of the option property that allows the location of the
   * input EDI to be configured.
   */
   private static final String INPUT_MESSAGE_LOCATION_PROP =
                                  "INPUT_MESSAGE_LOCATION";

   /**
   * This is the name of the option property that allows the output location
   * for the ISA segment. 
   */
   private static final String ISA_OUTPUT_LOCATION_PROP = "ISA_OUTPUT_LOCATION";

   /**
   * This is the name of the option property that allows the output location
   * for the customer's trading partner ID to be configured. 
   */
   private static final String TPID_OUTPUT_LOCATION_PROP = "TPID_OUTPUT_LOCATION";   

   /**
   * This location in the input message or the context where the EDI
   * string can be found. This is configured from the INPUT_MESSAGE_LOCATION
   * property, if present. 
   */
   private String inputLoc;

   /**
   * This is the output location (probably somewhere in the context)
   * where the ISA segement should be placed in the output.
   */
   private String isaOutputLoc;

   /**
   * This is the output location (probably somewhere in the context)
   * where the customer trading partner ID, as retrieved from the
   * EDI, will be placed in this message processor's output.
   */
   private String tpidOutputLoc;

   /**
   * Initializes the input and output locations from properties.
   *
   * @param  key   Property-key to use for locating initialization properties.
   * @param  type  Property-type to use for locating initialization properties.
   * @exception ProcessingException when initialization fails
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      inputLoc     = getPropertyValue(INPUT_MESSAGE_LOCATION_PROP,
                                      DEFAULT_INPUT_MESSAGE_LOC);

      isaOutputLoc = getPropertyValue(ISA_OUTPUT_LOCATION_PROP,
                                      DEFAULT_ISA_OUTPUT_LOCATION);

      tpidOutputLoc = getPropertyValue(TPID_OUTPUT_LOCATION_PROP,
                                      DEFAULT_TPID_OUTPUT_LOCATION);

      if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){

         Debug.log(Debug.SYSTEM_CONFIG,
                   "EDITradingPartnerExtractor initialized: \n"+
                   "Input message location: ["+inputLoc+"]\n"+
                   "ISA output location: ["+isaOutputLoc+"]\n"+
                   "Customer trading partner ID output location: ["+
                   tpidOutputLoc+"]\n");
         

      }

   }

   /**
   * This gets the ISA segement and the customer trading partner ID contained
   * therein from the input EDI and sets these values in their
   * configured output location.
   */
   public NVPair[] process(MessageProcessorContext context,
                           MessageObject input )
                           throws MessageException,
                                  ProcessingException{

      // the message processor gag reflex
      if(input == null) return null;

      // get the incoming EDI String
      String edi = getString(inputLoc, context, input);

      // get the ISA from the EDI
      String isa = getISA(edi);

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS, "Retrieved ISA segment: ["+isa+"]");

      }
      // put the value into its output location
      set(isaOutputLoc, context, input, isa);


      // get the customer's trading partner ID from the ISA
      String tpid = getCustomerID(isa);

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS,
                   "Retrieved customer's trading partner ID: ["+tpid+"]");

      }
      // put the value into its output location
      set(tpidOutputLoc, context, input, tpid);

      return formatNVPair( input );

   }

   /**
   * This extracts the ISA segment from the given EDI message.
   *
   * @param ediMsg the EDI message
   * @throws MessageException if the given EDI message is not long
   *                          enough to contain an ISA segment.
   */
   public static String getISA(String ediMsg) throws MessageException
   {

     // get the ISA segment to send back
     if (ediMsg.length() < ISA_SEGMENT_LENGTH)
         throw new MessageException ("The received message is not an " +
                    "EDI message.  An EDI message must contain at least " +
                    ISA_SEGMENT_LENGTH +
                    " characters for the initial ISA segment.");

     return ediMsg.substring(0, ISA_SEGMENT_LENGTH);

   }

   /**
   * This gets the customer's trading partner ID from the given ISA
   * segment.
   *
   * @param isa the ISA segement. This should have been extracted from the
   *            EDI message using the getISA() method.
   */
   public static String getCustomerID(String isa)
   {

      String tpid = isa.substring(CUSTOMER_ID_START_INDEX,
                                  CUSTOMER_ID_END_INDEX);

      return tpid.trim();

   }


}
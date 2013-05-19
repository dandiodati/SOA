package com.nightfire.adapter.messageprocessor;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;

import com.nightfire.framework.message.common.xml.*;


/**
 * This class takes a String/DOM/HashMap as an input and returns it as the
 * value of NVPair array without modifying it. It operates on information in the
 * context. The main function on this method is to format values which are the
 * information associated with the requeseted trading partner. The formatted
 * values are concatenated as a string, then put back to the context.
 */
public class TradingPartnerInfoFormatter extends MessageProcessorBase
{
   /**
    * Prefix of the value of trading partner information.
    */
   private static final String PREFIX_OF_INFO_PROP = "PREFIX_OF_INFO";

   /**
    * The delimiter which separates the values.
    */
   private static final String SEPARATOR_PROP = "SEPARATOR";

   /**
    * The location of the trading partner information.
    */
   private static final String PARTNER_INFO_LOCATION_PROP = "PARTNER_INFO_LOCATION";

   /**
    * The location of the prefix for oracle sequence.
    */
   private static final String SEQUENCE_PREFIX_PROP = "SEQUENCE_PREFIX";

   /**
    * The prefix of information value.
    */
   private String prefixOfInfo = null;

   /**
    * The delimiter to separate the formatted value string
    */
   private String separator = null;

   /**
    *  The location of the information.
    */
   private String partnerInfoLocation = null;

   /**
    * The prefix for the sequence value.
    */
   private String sequencePrefix = null;


  /**
   * Retrieves properties from the database. These properties specify
   * the information for processing.
   *
   * @param key   A string which acts as a pointer to service provider
   *              and order type information in the database.
   * @param type  A string which specifies the type of processor this is.
   * @exception ProcessingException   Thrown if the specified properties
   *                                  cannot be found or are invalid.
   */
   public void initialize(String key, String type) throws ProcessingException
   {
      super.initialize(key, type);

      // Initialize a string buffer to contain all errors
      StringBuffer errorMessage = new StringBuffer();

      // Getting the necessary values from the persistent properties
      prefixOfInfo        = getRequiredPropertyValue(PREFIX_OF_INFO_PROP, errorMessage);
      separator           = getRequiredPropertyValue(SEPARATOR_PROP, errorMessage);
      partnerInfoLocation = getRequiredPropertyValue(PARTNER_INFO_LOCATION_PROP, errorMessage);
      sequencePrefix      = getRequiredPropertyValue(SEQUENCE_PREFIX_PROP, errorMessage);

      Debug.log( Debug.MSG_STATUS,
                 "TradingPartnerInfoFormatter: " + "\n" +
                 "   prefixOfInfo        = " + prefixOfInfo + "\n" +
                 "   separator           = " + separator + "\n" +
                 "   partnerInfoLocation = " + partnerInfoLocation + "\n" +
                 "   sequencePrefix      = " + sequencePrefix );

      if( errorMessage.length() > 0 )
      {
         // Some error occured during processing above
         Debug.log( Debug.ALL_ERRORS, "ERROR: TradingPartnerInfoFormatter: " +
                    "Failed initialization " + errorMessage.toString() );

         throw new ProcessingException( "ERROR: TradingPartnerInfoFormatter: " +
                    "Failed initialization " + errorMessage.toString() );
      }

   }

  /**
   * Retrieves the trading partner information from the context and formats
   * retrieves values into the specified form. Then the  formatted values are
   * put back to the context.
   * Returns a NVPair[](name/value pair).
   *
   * @param   input  Input message to process.
   * @param   mpc The context that stores control information
   * @return  Optional NVPair containing a Destination name and a Document,
   *          or null if none.
   * @exception  ProcessingException  Thrown if processing fails.
   * @exception  MessageException  Thrown if bad message.
   */
   public NVPair[] process(MessageProcessorContext mpc, MessageObject input)
      throws MessageException, ProcessingException
   {
      if( input == null )
      {
         return null;
      }

      String  value                 = null;
      String  nextSequenceValue     = null;
      boolean exists                = true;
      int     counter               = 0;
      int     separatorLength       = 0;
      int     formattedValuesLength = 0;
      int     beginIndex            = 0;

      StringBuffer formattedValues  = new StringBuffer();

      while( true )
      {
         exists = exists( prefixOfInfo + counter, mpc, input );

         Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: " + "\n" +
                    "exists                 = " + exists + "\n" +
                    "prefixOfInfo           = " + prefixOfInfo + "\n" +
                    "counter                = " + counter + "\n" +
                    "prefixOfInfo + counter = " + prefixOfInfo + counter );

         // check if the wanted value exists. prefixOfInfo is "@context.@TARGET_"
         if( exists == false )
         {
            break;
         }

         value = getString( get( prefixOfInfo + counter, mpc, input ) );

         Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: value = " + value );


         // if the value starts with prefix indicating a sequence
         if( value.startsWith( sequencePrefix ) )
         {
            value = getNextValueOfSequence( value );

            Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: value returned by getNextValueOfSequence(): " + "\n" +
                                         "value = " + value );
         }

         if( formattedValues.length() > 0 )
         {
            // concatinating the separator
            formattedValues =  formattedValues.append( separator );
         }

         formattedValues = formattedValues.append( value );

         Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: " + "\n" +
                                      "counter         = " + counter + "\n" +
                                      "formattedValues = " + formattedValues );

         counter++;
         
      }    // end while

      Debug.log( Debug.MSG_STATUS,  "TradingPartnerInfoFormatter: " + "\n" +
                                    "formattedValues = " + formattedValues );

      // set the formatted values to the context using super class method
      set( partnerInfoLocation, mpc, null, formattedValues.toString() );

      return( formatNVPair( input ) );
   }


  /**
   * Get the next value of a oracle sequence.
   *
   * @param      valueFromContext     The value obtained form the context.
   * @return     sequenceValue        The next value of the sequence.
   * @exception  ProcessingException  Thrown if processing fails.
   */
   private String getNextValueOfSequence( String valueFromContext ) throws ProcessingException
   {
      String sequenceName = null;
      String sequenceValue = null;

      Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: We will get the next value of a sequence." );

      // retriving the sequence  name by chopping off the prefix.
      sequenceName = valueFromContext.substring( sequencePrefix.length() );

      Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: sequenceName = " + sequenceName );

      try
      {
         // getting the next value of sequence
         sequenceValue = String.valueOf(PersistentSequence.getNextSequenceValue(sequenceName));

         Debug.log( Debug.MSG_STATUS, "TradingPartnerInfoFormatter: sequence sequenceValue = " + sequenceValue );
      }
      catch( DatabaseException e )
      {
         Debug.log( Debug.ALL_ERRORS, "ERROR: TradingPartnerInfoFormatter: \n" +
                                      "Could not retrieve persistent \n" +
                                      "sequence for " + sequenceName +
                                      "\n" + e.getMessage() );

         throw new ProcessingException
         ("ERROR: TradingPartnerInfoFormatter: Could not retrieve persistent " +
          "sequence for " + sequenceName + "\n" + e.getMessage());
      }

      return sequenceValue;
   }

}

package com.nightfire.adapter.messageprocessor;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;

import com.nightfire.framework.message.parser.xml.*;

/**
 * This class takes a MessageObject as an input and returns it as the
 * value of an NVPair array without modifying it. It operates on information in the
 * context. The main function of this class is to retrieve the information in the
 * header that is passed along with the input message and put it into the context.
 * It specifically retrieves the message type, sub type and supplier name information
 * from the header and puts it into the context.
 */
public class GetHeaderInfo extends MessageProcessorBase
{
   /**
    *  Location to store the message type info in the MessageProcessorContext.
    */
   private static final String MESSAGE_TYPE_LOCATION_PROP = "MESSAGE_TYPE_LOCATION";

   /**
    *  Location to store the message sub type info in the MessageProcessorContext.
    */
   private static final String MESSAGE_SUBTYPE_LOCATION_PROP = "MESSAGE_SUBTYPE_LOCATION";

   /**
    *  Location to store the supplier info in the MessageProcessorContext.
    */
   private static final String SUPPLIER_NAME_LOCATION_PROP = "SUPPLIER_NAME_LOCATION";

   /**
    *  Location to store the the key
    */
   private static final String REQUEST_KEY_PROP     = "REQUEST_KEY";

   /**
    *  Location to store the the key
    */
   private static final String SUB_REQUEST_KEY_PROP = "SUB_REQUEST_KEY";

   /**
    *  Location to store the the key
    */
   private static final String SUPPLIER_KEY_PROP    = "SUPPLIER_KEY";

   /**
    *  Location of the message type.
    */
   private String msgTypeLocation =  null;
   /**
    *  Location of the message sub type.
    */
   private String msgSubTypeLocation =  null;
   /**
    *  Location of the supplier information.
    */
   private String supplierNameLocation =  null;

   /**
    *  Location to store the key value.
    */
   private String requestKey     =  null;

   /**
    *  Location to store the key value.
    */
   private String subRequestKey  =  null;

   /**
    *  Location to store the key value.
    */
   private String supplierKey    =  null;


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
      msgTypeLocation      = getRequiredPropertyValue( MESSAGE_TYPE_LOCATION_PROP, errorMessage );
      msgSubTypeLocation   = getRequiredPropertyValue( MESSAGE_SUBTYPE_LOCATION_PROP, errorMessage );
      supplierNameLocation = getRequiredPropertyValue( SUPPLIER_NAME_LOCATION_PROP, errorMessage );
      requestKey           = getRequiredPropertyValue( REQUEST_KEY_PROP, errorMessage );
      subRequestKey        = getRequiredPropertyValue( SUB_REQUEST_KEY_PROP, errorMessage );
      supplierKey          = getRequiredPropertyValue( SUPPLIER_KEY_PROP, errorMessage );

      Debug.log( Debug.MSG_STATUS,
                 "GetHeaderInfo: msgTypeLocation is     : " + msgTypeLocation +
                 "               msgSubTypeLocation is  : " + msgSubTypeLocation +
                 "               supplierNameLocation is: " + supplierNameLocation +
                 "               requestKey is          : " + requestKey +
                 "               subRequestKey is       : " + subRequestKey +
                 "               supplierKey is         : " + supplierKey );


      if( errorMessage.length() > 0 )
      {
         // Some error occured during processing above
         Debug.log( Debug.ALL_ERRORS, "ERROR: GetHeaderInfo: " +
                    "Failed initialization " + errorMessage.toString() );

         throw new ProcessingException( "ERROR: GetHeaderInfo: " +
                    "Failed initialization " + errorMessage.toString() );
      }

   }

  /**
   * Retrieves the header information(REQUEST, SUBREQUEST, and SUPPLIER from
   * the MessageProcessorContext in DOM form.
   * Set the retreived values to the MessageProcessorContext.
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

      String msgTypeValue      = null;
      String msgSubTypeValue   = null;
      String supplierNameValue = null;
      String requestHeaderKey  = null;

      Document header = null;
      // this string represents "@context.@REQUEST_HEADER"
      requestHeaderKey = CONTEXT_START +
                         MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME;

      Debug.log( Debug.MSG_STATUS, "requestHeaderKey = "+requestHeaderKey );

      // extract the header from the context
      header = getDOM( get( requestHeaderKey, mpc, input ) );

      try
      {
         XMLMessageParser parser = new XMLMessageParser( header );

         // extract the values from the document
         msgTypeValue      =  parser.getValue( requestKey );
         msgSubTypeValue   =  parser.getValue( subRequestKey );
         supplierNameValue =  parser.getValue( supplierKey );

         Debug.log( Debug.MSG_STATUS, "In the header: msgTypeValue      = " + msgTypeValue + "\n" +
                       "                              msgSubTypeValue   = " + msgSubTypeValue + "\n" +
                       "                              supplierNameValue = " + supplierNameValue + "\n" );

      }
      catch( MessageException m )
      {
         Debug.log ( Debug.ALL_ERRORS, "ERROR: GetHeaderInfo: " + m.getMessage () );

         throw new MessageException ("ERROR: GetHeaderInfo: " + m.getMessage() );
      }

      // set the values to the context using super class method
      set( msgTypeLocation, mpc, null, msgTypeValue );
      set( msgSubTypeLocation, mpc, null, msgSubTypeValue );
      set( supplierNameLocation, mpc, null, supplierNameValue );

      return( formatNVPair( input ) );
   }

}

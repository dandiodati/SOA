package com.nightfire.adapter.messageprocessor;

import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;


/**
 * This class takes a String/DOM/HashMap and returns it as the value
 * of NVPair array without modifying it.
 * The persistent property indicates the name of the Oracle
 * sequence whose next sequence value is to be fetched.
 */
public class GetNextValOfSequence extends MessageProcessorBase
{

  /**
   * Name of the sequence
   */
   private static final String SEQ_NAME_PROP = "SEQ_NAME";
   /**
    * Location of the sequence in the context
    */
   private static final String SEQ_VAL_LOCATION_PROP = "SEQ_VAL_LOCATION";
   /**
    * Location of the sequence in the context
    */
   private static final String NEXT_PROCESSOR_NAME_PROP = "NEXT_PROCESSOR_NAME";
  /**
   * Length of the sequence value
   */
   private static final String MIN_SEQ_VAL_LENGTH_PROP = "MIN_SEQ_VAL_LENGTH";

   /**
    * Property indicating whether SQL operation should be part of overall driver transaction.
    */
    public static final String TRANSACTIONAL_LOGGING_PROP = "TRANSACTIONAL_LOGGING";

   /**
    * Length of the oracle sequence requested
    */
   private String minSeqValLength =  null;
   /**
    * Name of the oracle sequence requested
    */
   private String seqName =  null;
   /**
    * Location of the value
    */
   private String valueLocation =  null;
   /**
    * Name of the next processor
    */
   private String nextProcessorName =  null;
   /**
    * Property indicating whether context connection should be used.
    */
   private boolean usingContextConnection = true;

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

      String strTemp = null;   
    
      seqName           = getRequiredPropertyValue(SEQ_NAME_PROP, errorMessage);
      valueLocation     = getRequiredPropertyValue(SEQ_VAL_LOCATION_PROP, errorMessage);
      nextProcessorName = getRequiredPropertyValue(NEXT_PROCESSOR_NAME_PROP, errorMessage);
      minSeqValLength   = getPropertyValue(MIN_SEQ_VAL_LENGTH_PROP); 
      strTemp = getPropertyValue( TRANSACTIONAL_LOGGING_PROP );

      if ( StringUtils.hasValue( strTemp ) )
      {
          try {
              usingContextConnection = getBoolean( strTemp );
          } catch ( FrameworkException e ) {
              errorMessage.append ( "Property value for " + TRANSACTIONAL_LOGGING_PROP +
              " is invalid. " + e.getMessage ( ) + "\n" );
          }
      }
      Debug.log( Debug.MSG_STATUS,
                 "GetNextValOfSequence: seqName is           : " + seqName +
                 "                      valueLocation is     : " + valueLocation +
                 "                      nextProcessorName is : " + nextProcessorName );

      if( errorMessage.length() > 0 )
      {
         // Some error occured during processing above
         Debug.log( Debug.ALL_ERRORS, "ERROR: GetNextValOfSequence: " +
                    "Failed initialization " + errorMessage.toString() );

         throw new ProcessingException( "ERROR: GetNextValOfSequence: " +
                    "Failed initialization " + errorMessage.toString() );
      }

   }

  /**
   * Retrieves the next value of the given sequence by calling a method of
   * PersistetSequence. Set the retreived value to the MessageProcessorContext.
   * Returns a NVPair[](name/value pair).
   *
   * @param  input  Input message to process.
   * @param  mpc The context that stores control information
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

      String nextSeqValue;

      try
      {
         // Get the database connection from context only when
         // transaction logging is set to true
         // And context has already acquired a database connection.
         if ( usingContextConnection && mpc.hasAcquiredDBConnection())
         {
            Debug.log( Debug.MSG_STATUS, "Database logging is transactional and connection is available in the context, so getting connection from context." );
            nextSeqValue = String.valueOf(PersistentSequence.getNextSequenceValue(seqName,mpc.getDBConnection( )));
         }
         else
         {
            Debug.log( Debug.MSG_STATUS, "Either Database logging is not transactional or there is no connection available in the context, so passing null" );
            nextSeqValue = String.valueOf(PersistentSequence.getNextSequenceValue(seqName, null));
         }

         Debug.log( Debug.MSG_STATUS, "GetNextValOfSequence: The value for the sequence: " + "\n" +
                                seqName + " is: " + nextSeqValue );
      }
      catch( DatabaseException e )
      {
         Debug.log( Debug.ALL_ERRORS, "ERROR: GetNextValOfSequence: Could not retrieve persistent " +
                                "sequence for " + seqName + "\n" + e.getMessage());

         throw new ProcessingException
         ("ERROR: GetNextValOfSequence: Could not retrieve persistent " +
          "sequence for " + seqName + "\n" + e.getMessage());
      }

      int minLength = 0;

      //Pad the sequence value by prepending 0's when it is less than the specified minimun length
      if ( StringUtils.hasValue ( minSeqValLength ) )
      {
          try
          {
              minLength = StringUtils.getInteger ( minSeqValLength );
          }
          catch ( FrameworkException fe )
          {
              throw new ProcessingException ( fe.getMessage() );
          }

          if ( nextSeqValue.length() < minLength )
          {
              Debug.log ( Debug.UNIT_TEST, "GetNextValOfSequence: Prepending the sequence value " +
                          "with 0's to satisfy the minimum length criteria");
              nextSeqValue = StringUtils.padString ( nextSeqValue , minLength , true , '0' );
          }
          
      }

      set( valueLocation, mpc, null, nextSeqValue );

      return( formatNVPair( input ) );
   }
}

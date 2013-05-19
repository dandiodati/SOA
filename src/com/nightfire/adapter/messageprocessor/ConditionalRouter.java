package com.nightfire.adapter.messageprocessor;

import org.w3c.dom.*;

import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.common.ProcessingException;

import com.nightfire.spi.common.driver.*;

/**
* This class is used to route a MessageObject to the appropriate next
* processor based on whether or not a node exists in the the
* incoming MessageProcessorContext or MessageObject. This class
* can also route based whether the value of these nodes matches with
* a list of possible good values. 
*/
public class ConditionalRouter extends MessageProcessorBase{

   /**
   * A name used at the beginning of log messages.
   */
   private static final String className = "ConditionalRouter";

   /**
   * The location of the XML input message within the MessageObject or
   * MessageProcessorContext.
   */
   private String inputMessageLocation;

   /**
   * The separator to use when tokenizing property value to get the lists
   * of possible locations and possible values.
   */
   private String separator;

   /**
   * The list of possible locations to check to see if a condition is true.
   * This list is a string delimited by the <code>separator</code>.
   */
   private String checkLocations;

   /**
   * The list of possible values that would indicate a match if found at one of
   * the <code>checkLocations</code>.
   */
   private String[] matchValues;

   /**
   * The name of the next processor to which the input MessageObject will
   * be routed if the condition is found to be true.
   */
   private String trueProcessor;

   /**
   * The name of the next processor to which the input MessageObject will
   * be routed if the condition is found to be false.
   */
   private String falseProcessor;

   /**
   * Default separator value to be used if SEPARATOR_PROP is not set.
   */
   public static final String DEFAULT_SEPARATOR = "|";

   /**
   * Property indicating separator token used to separate individual location alternatives.
   */
   protected static final String SEPARATOR_PROP = "SEPARATOR";

   /**
   * The property that indicates the location of the input Document in
   * the MessageProcessorContext or MessageObject.
   */
   protected static final String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";

   /**
   * The property that specifies the
   * name of the processor to which the MessageObject should be routed
   * if the condition is true.
   */
   protected static final String POSITIVE_NEXT_PROCESSOR_PROP = "NEXT_PROCESSOR_IF_TRUE";

   /**
   * The property that specifies the
   * name of the processor to which the MessageObject should be routed
   * if the condition is false.
   */
   protected static final String NEGATIVE_NEXT_PROCESSOR_PROP = "NEXT_PROCESSOR_IF_FALSE";

   /**
   * The property containing a list of XML node names which should be
   * evaluated. The names will be separated by the <code>separator</code>
   * if there is more than one location to be checked.
   */
   protected static final String CONDITIONAL_LOCATION_PROP = "NODE_NAME";

   /**
   * The property containing a list of possible matching values that if found
   * at any of the <code>checkLocations</code> would indicate a match.
   * The values will be separated by the <code>separator</code>
   * if there is more than one possible matching value. This property is
   * optional.
   */
   protected static final String CONDITIONAL_VALUE_PROP = "NODE_VALUE";


   /**
   * This method loads the properties for this processor based on the given
   * key and type and configures itself based on these given properties.
   * @param key The property key for loading this processor's properties.
   * @param type The property type for loading this processor's properties.
   * @exception ProcessingException thrown if one or more required properties
   *                                are absent. 
   */
   public void initialize( String key, String type ) throws ProcessingException{

      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "Initializing "+className+"...");

      // Load properties
      super.initialize(key, type);

      // A buffer to hold error messages if any of the required properties
      // are missing.
      StringBuffer errorMessages = new StringBuffer();

      // Get names of next processors
      trueProcessor  = getRequiredPropertyValue( POSITIVE_NEXT_PROCESSOR_PROP, errorMessages );
      falseProcessor = getRequiredPropertyValue( NEGATIVE_NEXT_PROCESSOR_PROP, errorMessages );

      // Get location of input message in the MessageContext or MessageObject.
      // It is OK if this value is null. This indicates that the contents
      // of the input MessageObject should be used as the XML Message.
      inputMessageLocation = getPropertyValue( INPUT_MESSAGE_LOCATION_PROP );

      separator = getPropertyValue( SEPARATOR_PROP );

      if ( !StringUtils.hasValue( separator ) ){

         separator = DEFAULT_SEPARATOR;

      }

      checkLocations = getRequiredPropertyValue( CONDITIONAL_LOCATION_PROP, errorMessages );

      // If there were any missing properties, throw the exception now.
      if(errorMessages.length() > 0){

         throw new ProcessingException(className+": "+errorMessages.toString());

      }

      String matchProp = getPropertyValue( CONDITIONAL_VALUE_PROP );

      if(matchProp == null){

         // The optional list of match values was not specified.
         // We will only be checking for a location's existence.
         matchValues = null;

      }
      else{

          StringTokenizer matchTokens = new StringTokenizer(matchProp, separator);
          int matchValueCount = matchTokens.countTokens();
          matchValues = new String[matchValueCount];

          for(int i = 0; i < matchValueCount; i++){

             matchValues[i] = matchTokens.nextToken();

          }

      }

      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log( Debug.MSG_STATUS, className+": Initialization done." );

   }

   /**
   * The process method gets an XML Document out of the input location
   * in the given <code>context</code> or <code>input</code> and checks the
   * contents of this Document to see if the the configured condition is
   * true.
   *
   * @param context The current context for the process within which this
   *                processor is being executed. This may contain the XML input
   *                for this processor.
   * @param input The input MessageObject containing one or more input objects.
   *              One of these input objects may be the XML input message for
   *              this processor.
   *
   * @return  An NVPair array containing a single NVPair. The name of the
   *          pair will be equal to the value of the NEXT_PROCESSOR_IF_TRUE
   *          property if the condition is true, and it will be the
   *          value of NEXT_PROCESSOR_IF_FALSE if the condition is false.
   *          The value of the NVPair will be the <code>input</code>
   *          MessageObject.
   */
   public NVPair[] process(MessageProcessorContext context, MessageObject input )
                           throws MessageException, ProcessingException {

      // This is the standard MessageProcessor response to a null input.
      if(input == null){
         return null;
      }

      NVPair[] response = new NVPair[1];

      Document document = null;

      try{
         document = getDOM(inputMessageLocation, context, input);
      }
      catch(ProcessingException pex){

         throw new ProcessingException("Unable to find location ["+
                                       inputMessageLocation+
                                       "] as specified by the ["+
                                       INPUT_MESSAGE_LOCATION_PROP+
                                       "] property: "+pex.getMessage() );

      }

      XMLMessageParser parser = new XMLMessageParser(document);

      if( evaluateCondition(parser) ){

         response[0] = new NVPair(trueProcessor, input);

      }
      else{

         response[0] = new NVPair(falseProcessor, input);

      }

      return response;

   }


   /**
   * This checks to see if the condition is true or false by checking
   * the list of possible locations. If the list of <code>matchValues</code> is
   * null, this means that one of the specified node locations simply
   * needs to exist in the given <code>parser</code> in order to get a
   * true result. If a list of matching values is not null, any existing
   * locations must have a value. If the found value equals (case insensitive)
   * any of the values in the <code>matchValues</code> list, then this method
   * will return true. In all other cases, this method will return false.
   *
   * @param parser A parser representing the input message given to this processor.
   * @return  <b>true</b> if the condition is evaluated to be true and <b>false</b>
   *          if the configured conditions is found to be false.
   */
   public boolean evaluateCondition(MessageParser parser){

      boolean result = false;

      String location;
      String value = null;

      StringTokenizer locationTokens = new StringTokenizer(checkLocations, separator );

      // Iterate through list of location alternatives until
      // we find a match or until we run out of locations to check.
      while(locationTokens.hasMoreTokens() && !result){

          // Get the next location alternative
          location = locationTokens.nextToken();

          if( parser.exists( location  ) ){

              // If the list match values is null, this means that
              // we just need to confirm that a node exists
              // at one of the given locations.
              if(matchValues == null){

                 result = true;

              }
              else{

                  try{

                     value = parser.getValue( location );

                  }
                  catch( MessageException e ){

                      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

                         Debug.log(Debug.MSG_STATUS,
                                   className+
                                   ": Could not find value for location ["+
                                   location+
                                   "]: "+e.getMessage() );

                      }

                  }

                  // Check to see if we have found a value.
                  if(value != null){

                      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

                         Debug.log(Debug.MSG_STATUS,
                                   className+
                                   ": Found value ["+value+"] for location ["+
                                   location+
                                   "]");

                      }

                      // Check to see if the value we have found equals any
                      // values in out list of possible matches.
                      for(int i = 0; result != true && i < matchValues.length; i++){

                          if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

                             Debug.log(Debug.MSG_STATUS,
                                       className+
                                       ": Comparing value ["+value+"] to ["+
                                       matchValues[i]+
                                       "]");

                          }

                          if(value.equalsIgnoreCase(matchValues[i]) ){

                             Debug.log(Debug.MSG_STATUS,
                                       className+
                                       ": Value ["+value+"] matches ["+matchValues[i]+"]" );

                             result = true;

                          }

                      }

                  }

              }

          }

      }

      return result;

   }


}
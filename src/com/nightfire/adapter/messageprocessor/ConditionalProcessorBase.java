/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.adapter.messageprocessor;

import org.w3c.dom.Document;

//NightFire packages
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.message.util.xml.CachingXPathAccessor;
import com.nightfire.framework.rules.*;
import com.nightfire.adapter.util.*;
import com.nightfire.adapter.messageprocessor.messagetransformer.*;

//JDK packages
import java.util.*;
import java.io.File;
import java.io.FileFilter;

/**
 * Evaluate a series of Rule Sets (in property order), updating the
 * output or context with the result of the first condition that evaluates
 * to true.
 */
public abstract class ConditionalProcessorBase extends RuleProcessor
{
    /**
     * The identifier used when in log messages by this class.
     */
    public static final String className = "ConditionalProcessorBase";
    

   /**
     * The result value to use if the ruleset evaluates to 'true' (iterative).
     */
    public static final String EVALUATOR_RESULT_PROP = "EVALUATOR_RESULT";


    /**
     * Output result location - in MessageProcessorContext/MessageObject.
     */
    public static final String RESULT_LOCATION_PROP = "RESULT_LOCATION";


   /**
    * The location where Ruleset result value to be stored in.
    */
    protected String resultLocation;

    
    /*
     * List for storing the rulesets and result values 
     **/
    private List ruleSetNames;            


  /**
     * Called to initialize this component.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */


    public void initialize ( String key, String type ) throws ProcessingException
    {
      // Load properties
      super.initialize(key, type);

        StringBuffer errors = new StringBuffer();

        resultLocation = getRequiredPropertyValue( RESULT_LOCATION_PROP, errors);

        ruleSetNames = new LinkedList( );

      
	// Get the list of rulesets and the result values

        int i=0; 
	
	String ruleset = getPropertyValue( PersistentProperty.getPropNameIteration(EVALUATOR_CLASS_PROP, i) );
        
	String resultValue = getPropertyValue( PersistentProperty.getPropNameIteration(EVALUATOR_RESULT_PROP, i) );

 
	while(StringUtils.hasValue(ruleset) || StringUtils.hasValue(resultValue)) 
 	{
         	ruleSetNames.add(new RuleConfiguration(ruleset, resultValue));
         
	 	ruleset = getPropertyValue( PersistentProperty.getPropNameIteration(EVALUATOR_CLASS_PROP, ++i) );

			// Not incrementing "i" again as ++i is already done above

         	resultValue = getPropertyValue( PersistentProperty.getPropNameIteration(EVALUATOR_RESULT_PROP, i) );
            
 	}


      	if(ruleSetNames.size() == 0){
         	errors.append(className+"No ["+EVALUATOR_CLASS_PROP+ "] or ["+ EVALUATOR_RESULT_PROP +
				"] properties were specified.");
      	}


	if (errors.length() > 0){
    		throw new ProcessingException(className+": " + errors.toString());
      }

  }

    /**
     * Process the input message and (optionally) return
     * a value.
     *
     * @param  input  Input MessageObject that contains the message to be processed.
     * @param  mpcontext The context
     *
     * @return  true or false.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if processing fails.
     */

    public String executeTest ( MessageProcessorContext mpcontext, MessageObject input)
        throws MessageException, ProcessingException
    {

        if ( input == null )
           return null;

        ErrorCollection errors = new ErrorCollection();
        XPathAccessor parsedMessage = null;

        try{

           parsedMessage = getParsedMessage( mpcontext, input );

        }
        catch(FrameworkException fex){

            RuleError parseError = new RuleError(null,
                                                 "Could not parse input message: ",
                                                 fex);
            errors.addError(parseError);

        }

        Iterator ruleIter = ruleSetNames.iterator( );
        RuleConfiguration cond = null;

        while(ruleIter.hasNext())
        {

            cond = (RuleConfiguration)ruleIter.next( );

            boolean result = cond.evaluateRuleSet( mpcontext,
                                                   input,
                                                   parsedMessage );

            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                        Debug.log(Debug.MSG_STATUS,
                           className+":"+
                           "Execution Result is ["+
                           result+ "]");
            }

            if ( result == true )
            {
               if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                        Debug.log(Debug.MSG_STATUS,
                           className+":"+
                           "Evaluator Result is ["+
                           cond.getResult()+"].");
               }

               return  cond.getResult();

            }

        }

        return null;

    }

    /**
    * This retrieves an XPathAccessor for the XML messages contained in
    * either the context or the input message at the input message location.
    *
    */
    protected XPathAccessor getParsedMessage(MessageProcessorContext context,
                                             MessageObject input)
                                             throws FrameworkException{

        XPathAccessor parsedMessage = null;

        // Get the input message
        Object message = get(inputMessageLocation, context, input);

        if(message instanceof Document){

           // The message is an XML Document object.
           // There's no need to parse the input message again.
           // Just create an XPath accessor for the message.
           parsedMessage = new CachingXPathAccessor((Document) message);

        }
        else if(message instanceof String){

           // If the input message was a String, then parse the XML.
           // This saves us from having to parse the XML for each Evaluator.

           // A caching XPath accessor instance is used for improved
           // performance.

           parsedMessage = new CachingXPathAccessor((String) message);

        }
        else{

           // This will be caught just below and turned into a RuleError.
           // Throwing the exception here is probably not the most
           // efficient thing to do, but performance should not be an issue,
           // since this will only happen in the case where the gateway
           // has been designed or configured incorrectly, giving the
           // wrong type of input object, and needs to be fixed.

           throw new FrameworkException("The input message must be a String or and XML Document.The input object was of type: ["+message.getClass().getName()+"]");

        }

        return parsedMessage;

    }

    // Class used to encapsulate rule condition to evaluate.
    private class RuleConfiguration
    {

       private String ruleset;
       private String testResult;
       ErrorCollection errors = new ErrorCollection();

       // Create the rule condtion object.
       public RuleConfiguration (String condition, String result ){

          ruleset    = condition;
          testResult = result;
          
       }


       public boolean evaluateRuleSet( MessageProcessorContext mpcontext,
                                       MessageObject input,
                                       XPathAccessor parsedMessage)
                                       throws ProcessingException,
                                              MessageException
       {

             boolean success = false ;

             if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
             {
                Debug.log(Debug.MSG_STATUS,
                          className+":"+
                          "Evaluating message using RuleSet class ["+
                          ruleset+"]");
              }

             try
             {

                String[] classpath = getCustomClasspath(mpcontext,
                                                        input);
                                                        
                success = RuleEngine.evaluate(ruleset,
                                              parsedMessage,
                                              errors,
                                              classpath);

                if( success)
                {
                   if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                   {
                      Debug.log(Debug.MSG_STATUS,
                                className+":["+
                                ruleset+"] with test result ["+
                                testResult+"]"+
                                " validation was successful.");
                   }
                }

                else
                {

                   if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                   {
                      Debug.log(Debug.MSG_STATUS,
                                className+":"+
                                ruleset+
                                " validation failed.");
                   }

                }

             }
             catch(FrameworkException fex)
             {
                throw new ProcessingException(fex.getMessage());
             }

             return success;

       }

       public String getResult ( )
       {
          return testResult;
       }
       
     }


}

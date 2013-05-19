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
* processor based on the outcome of excecuting a series of rule-sets.
*/

public class RuleRouter extends ConditionalProcessorBase{

   /**
   * A name used at the beginning of log messages.
   */
   private static final String className = "RuleRouter";

  /**
   * The name of the default processor.
   */
   private String defaultProcessor;

  /**
   * If all the rulesets are evaluated to fasle, use the default processor.
   */
   public static final String DEFAULT_NEXT_PROCESSOR_PROP = "DEFAULT_NEXT_PROCESSOR";



  /**
   * This method loads the properties for this processor based on the given
   * key and type and configures itself based on these given properties.
   * @param key The property key for loading this processor's properties.
   * @param type The property type for loading this processor's properties.
   * @exception ProcessingException thrown if one or more required properties
   *                                are absent. 
   */
   public void initialize( String key, String type ) throws ProcessingException{

      Debug.log( Debug.MSG_STATUS, "Initializing "+className+"...");

      // Load properties
      super.initialize(key, type);

      StringBuffer errMsg = new StringBuffer();

      defaultProcessor = getRequiredPropertyValue(DEFAULT_NEXT_PROCESSOR_PROP, errMsg);

      if (errMsg.length() > 0)
      {
      throw new ProcessingException(className+": " + errMsg.toString());
      }

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
   *          pair will be equal to the value returned by the conditional Processor
   *          base if not null .
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

      String  nextProcessorName = executeTest(context, input);


      if( nextProcessorName != null ){
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
          Debug.log( Debug.MSG_STATUS, className+":"+
				"The ruleset evaluated to 'true', and the next processor is set to ["
                           + nextProcessorName + "]." );
        }
	set( resultLocation, context, input, nextProcessorName );
        response[0] = new NVPair(nextProcessorName, input);

      }
      else
      {
      	if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
             Debug.log( Debug.MSG_STATUS, className+":"+
			"All the rulesets evaluated to 'false', so the next processor is set to ["
			+defaultProcessor+"].");
        }
	set( resultLocation, context, input, defaultProcessor );
        response[0] = new NVPair(defaultProcessor, input);

      }

      return response;

   }


}

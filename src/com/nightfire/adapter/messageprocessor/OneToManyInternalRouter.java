package com.nightfire.adapter.messageprocessor;

import com.nightfire.spi.common.driver.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;

public class OneToManyInternalRouter extends MessageProcessorBase {

   /**
   * The identifier used when in log messages by this class.
   */
   private static String className = "OneToManyInternalRouter";

   /**
   * This method creates a name-value pair for each subsequent processor,
   * where the name will be each processor's respective name and
   * the value will be a new MessageObject that is a deep copy
   * of the original <code>input</code> MessageObject.
   *
   * @returns An array of the NVPairs containing one copy of the input per following processor.
   * @throws ProcessingException if the input object contains a null.
   */
   public NVPair[] process(MessageProcessorContext context, MessageObject input)
                           throws com.nightfire.common.ProcessingException, MessageException
   {

      int copyCount;
      NVPair[] result;

      if(input == null){

         return null;

      }

      // "toProcessorNames" is an array of the names for all of the
      // processors to whom copies of the input should be sent. This
      // array is created in MessageProcessorBase.initialize() by parsing the
      // value of the "NEXT_PROCESSOR_NAMES" property.
      // Creating a local reference to this array is a paranoid maneuver
      // that protects against the unlikely event that some rogue thread
      // should try to reinitialize "toProcessorNames" to a new array
      // while we are still using it.
      String[] processorNames = toProcessorNames;

      if(processorNames == null){

         // This means that there were no subsequent processors specified
         // by the "NEXT_PROCESSOR_NAMES" property. This is a really
         // weird case, but anythings's possible, so return an empty array.
         if(Debug.isLevelEnabled( Debug.MSG_STATUS )){

            Debug.log( Debug.MSG_STATUS, className+
                                         ": Creating [0] copies of input" );

         }

         result = new NVPair[0];

      }
      else{

         copyCount = processorNames.length;

         if(Debug.isLevelEnabled( Debug.MSG_STATUS )){

            Debug.log( Debug.MSG_STATUS, className+
                                         ": Creating ["+copyCount+
                                         "] copies of input" );

         }

         // Create one copy of the original input object for each of the
         // proceeding processors
         result   = new NVPair[copyCount];

         // get() will throw a ProcessingException it contains a null
         // object, so there is no need to check if "original" is null. 
         Object original = input.get();
         Object copy;

         if(Debug.isLevelEnabled( Debug.MSG_STATUS )){

            Debug.log( Debug.MSG_STATUS, className+
                                         ": Original input object:\n"+
                                         original );

         }

         for(int i = 0; i < copyCount; i++){

            copy = com.nightfire.adapter.util.DeepCopyUtil.copy(original);
            result[i] = new NVPair( processorNames[i], new MessageObject(copy) );

            if(Debug.isLevelEnabled( Debug.MSG_STATUS )){

               Debug.log( Debug.MSG_STATUS, className+
                                            ": Created copy number ["+i+"]:\n["+
                                            copy+
                                            "]\nfor processor ["+
                                            processorNames[i]+"]\n" );

            }

         }

      }

      return result;

   }

}

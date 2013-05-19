/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header $
 */
 
package com.nightfire.adapter.messageprocessor;


import org.w3c.dom.*;

import java.util.*;

import com.nightfire.framework.db.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.transformer.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * Combines values from multiple xml nodes to a generate a unique key value.
 * This calls can be used to generate unique key and type values for the DriverMessageProcessor.
 * It handles can be provided defaults values and each value can be indicated as optional.
 *
 */
public class XMLValueJoiner extends MessageProcessorBase
{
    private final static String INPUT_VALUES_LOC_PROP = "INPUT_VALUES_LOC";
    private final static String INPUT_VALUES_OPTIONAL_PROP = "OPTIONAL_VALUES";
    // in separator list of default values. Need to be equal to length of input values loc
    private final static String INPUT_DEFAULT_VALUES_PROP = "DEFAULT_VALUES";
    private final static String OUTPUT_LOC_PROP = "OUTPUT_LOC";
    private final static String OUTPUT_SEPARATOR_PROP = "OUTPUT_SEPARATOR";
    private final static String OUTPUT_SUFFIX_PROP = "OUTPUT_SUFFIX";


    List valGroups = new ArrayList();


    /**
     *
     *
     * @param  context  The  message context.
     *
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objects, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
        if (msgObj == null)
            return null;

        Debug.log(Debug.MSG_STATUS,"XMLValueJoiner - processing ...");
        Iterator iter = valGroups.iterator();
        ValueGrp item;

        while (iter.hasNext() ) {
            item = ( ValueGrp) iter.next();
            item.setOutput(context, msgObj);
        }

        return( formatNVPair( msgObj ) );
    }


    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
  public void initialize ( String key, String type ) throws ProcessingException
  {
     super.initialize(key,type);
     String inValues = null;
     String inValuesOptional = null;
     String inValuesDefaults = null;
     String outSep = null;
     String outSuffix = null;
     String outLoc = null;

     Debug.log(Debug.MSG_STATUS,"XMLValueJoiner - initializing ...");
     StringBuffer errors = new StringBuffer();
     ValueGrp item;

      outSep = getRequiredPropertyValue(OUTPUT_SEPARATOR_PROP,errors);

      for(int i =0; true; i++) {
         inValues = getPropertyValue(PersistentProperty.getPropNameIteration(INPUT_VALUES_LOC_PROP,i));

         //if inValues is null assume there are no more groups
         if (!StringUtils.hasValue(inValues))
            break;

         outLoc = getRequiredPropertyValue(PersistentProperty.getPropNameIteration(OUTPUT_LOC_PROP,i), errors);


         inValuesOptional  = getPropertyValue( INPUT_VALUES_OPTIONAL_PROP, "" );

         inValuesDefaults  = getPropertyValue( INPUT_DEFAULT_VALUES_PROP, "" );


         outSuffix  = getPropertyValue( PersistentProperty.getPropNameIteration(OUTPUT_SUFFIX_PROP,i) );


         // if there are errors with this group check the next group.
         // try to gather all possible errors
         if (errors.length() > 0 )
            continue;

         item = new ValueGrp(this, inValues, inValuesOptional,inValuesDefaults,SEPARATOR,outSep, outLoc, outSuffix,errors);
         valGroups.add(item);
      }


      if (errors.length() > 0 ) {
         Debug.error("XMLValueJoiner - failed to initialize : " + errors.toString() );
         throw new ProcessingException( "XMLValueJoiner - failed to initialize : " + errors.toString() );
      }

  }

   /**
    * needed for ValueGrp to be able to access MessageProcessorBase methods.
    */
  protected String getStringLocal ( String name, MessageProcessorContext context, MessageObject input ) throws MessageException, ProcessingException
  {
      return getString(name, context, input);
  }

   /**
    * needed for ValueGrp to be able to access MessageProcessorBase methods.
    */
  protected boolean existsLocal ( String name, MessageProcessorContext context, MessageObject input )
  {
     return exists(name,context,input);
  }

  /**
    * needed for ValueGrp to be able to access MessageProcessorBase methods.
    */
  protected void setLocal ( String name, MessageProcessorContext context,
  MessageObject output, Object value ) throws MessageException, ProcessingException
  {
     set(name,context,output,value);
  }

  private class ValueGrp {

       private String inSep = null;
       private String outSep = null;
       private String outSuffix = null;
       private String outLoc = null;
       private List values;
       private List defaults;
       private List optional;
       private XMLValueJoiner parent;


       public ValueGrp(XMLValueJoiner joiner, String inValues, String optionalValue, String defaultValues, String inSep, String outSep, String outLoc, String outSuffix, StringBuffer errors)
       {
          parent = joiner;

          String prev = null;
          String next = null;

          values = new ArrayList();
          defaults = new ArrayList();
          optional = new ArrayList();

          this.outLoc = outLoc;


          StringTokenizer toker = new StringTokenizer(inValues,inSep);

          while (toker.hasMoreTokens() ) {
             values.add(toker.nextToken());
          }

          Debug.log(Debug.SYSTEM_CONFIG, "Created list of value locations:[" + values.size()+"] [" + values + "]");


          toker = new StringTokenizer(defaultValues, inSep, true);
          while (toker.hasMoreTokens() ) {

             prev = next;

             next = toker.nextToken();
             if (next.equals(inSep) ) {
                // add null for starting a inSep or finding an empty token.
                if ( next.equals(prev) || prev == null)
                   defaults.add(null);
                // check if this is string ends with an inSep
                if ( !toker.hasMoreTokens() )
                   defaults.add(null);
                else
                  continue;
             }
             else
                defaults.add(next);


          }

          Debug.log(Debug.SYSTEM_CONFIG, "Created list of defaults :[" + defaults.size()+"] [" + defaults + "]");


          if (defaults.size() > 0  && defaults.size() != values.size() ) {
             errors.append("ERROR There must be a corresponding default( can be null) for each value in " + INPUT_VALUES_LOC_PROP + ": input value locations [" + values.size()+"] [" + values + "], defaults ["+ defaults.size()+"] [" + defaults+"]");
          }

          optional = new ArrayList();



          prev = null;
          next = null;
          toker = new StringTokenizer(optionalValue, inSep, true);
          boolean bool = false;


          if ( toker.countTokens() == 0 ) {
             bool =  false;
             for (int i =0; i < values.size(); i++ ) {
                optional.add(new Boolean(bool));
             }
          } else if ( toker.countTokens() == 1 ) {
             next = toker.nextToken();
             try {
                   bool =  StringUtils.getBoolean(next);
                } catch (FrameworkException e) {
                   errors.append("\nFailed to convert string to boolean: " + next );
                }
             for (int i =0; i < values.size(); i++ ) {
                optional.add(new Boolean(bool) );
             }
          } else {
             while (toker.hasMoreTokens() ) {
                prev = next;
                try {
                   next = toker.nextToken();
                   Debug.error("token " + "prev " + prev + " next " + next);
                   if (next.equals(inSep)) {
                      // add null for starting a inSep or finding an empty token.
                      if ( next.equals(prev) || prev == null)
                         optional.add(new Boolean(false) );
                      // check if this is string ends with an inSep
                      if ( !toker.hasMoreTokens() )
                         optional.add(new Boolean(false) );
                      else
                         continue;
                   }
                   else
                      optional.add( new Boolean(StringUtils.getBoolean(next)) );
                } catch (FrameworkException e) {
                   errors.append("\nFailed to convert string to boolean: " + next );
                }

             }
          }

          Debug.log(Debug.SYSTEM_CONFIG, "Created list of value optional indicators:[" + optional.size()+"] [" + optional + "]");


          if (optional.size() != values.size() ) {
             errors.append("ERROR There must be an corresponding optional indicator( can be null) for each value in " + INPUT_VALUES_LOC_PROP + ": input value locations [" + values.size()+"] [" + values + "], optional indicators ["+ optional.size()+"] [" + optional +"]");
          }

          this.outSep = outSep;
          this.outSuffix = outSuffix;
       }

       public void setOutput(MessageProcessorContext context, MessageObject msgObj) throws MessageException, ProcessingException
       {
          List outList = new ArrayList(values.size());

          String value, valLoc;

          StringBuffer errors = new StringBuffer();

          // check each location for a value.
          // [if the value does not exist]
          //     [if is required]
          //        [if no default]
          //           then produce an error
          //        [if default]
          //            return default
          //     [if is not required]
          //        [if no default]
          //            then skip value.
          //        [if default]
          //            return default.
          //  [if the value exists]
          //     return value
          //
          for (int i =0; i < values.size(); i++ ) {
             valLoc = (String)values.get(i);
             if (parent.existsLocal(valLoc, context, msgObj) ) {
                value = getStringLocal(valLoc,context,msgObj );
                if (StringUtils.hasValue(value) )
                   outList.add(value);
                else
                   addDefaultValue(outList, valLoc, i, errors);
             } else {
                addDefaultValue(outList,valLoc,i, errors);
             }
          }

          if (errors.length() > 0 ) {
             Debug.error("XMLValueJoiner failed to set output string: " + errors.toString());
             throw new ProcessingException(errors.toString());
          }

          StringBuffer outBuf = new StringBuffer();

          if (outList.size() > 0 )
             outBuf.append(outList.get(0));

          for (int i = 1; i < outList.size(); i++) {
             outBuf.append(outSep);
             outBuf.append(outList.get(i));
          }

          if (StringUtils.hasValue(outSuffix) ) {
             outBuf.append(outSep);
             outBuf.append(outSuffix);
          }

          if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
             Debug.log(Debug.MSG_STATUS, "XMLValueJoiner: Adding joined value [" + outBuf.toString() + "] at location [" + outLoc + "]");


          parent.setLocal(outLoc,context,msgObj, outBuf.toString() );


        }

        


        // check is the value loc contains a default.
        // also checks if value loc are optional or required.
        // adds an error to StringBuffer errors, if there is no default and the value loc is required.
        //
        private void addDefaultValue(List outList, String valLoc, int index, StringBuffer errors)
        {
           String defaultValue = null;

           if ( defaults.size() > index)
              defaultValue = (String) defaults.get(index);

           if ( ((Boolean)optional.get(index)).booleanValue() == false ) {
                   // required value
                   //check for default
                   if ( defaultValue != null) {
                      Debug.log(Debug.MSG_STATUS,"XMLValueJoiner - Using Default value [" + defaultValue + "]");
                      outList.add(defaultValue);
                   } else {
                      errors.append("\n Required value [" + valLoc + "] is missing and doesn't have a default value.");
                   }
           } else {
               // not required
               // check for a default
               if ( defaultValue != null) {
                  Debug.log(Debug.MSG_STATUS,"Using Default value [" + defaultValue + "]");
                  outList.add(defaultValue);
               } else
                  Debug.log(Debug.MSG_STATUS, "XMLValueJoiner - No value found for optional loc [" + valLoc + "], skipping.");
           }
        }


    }

}


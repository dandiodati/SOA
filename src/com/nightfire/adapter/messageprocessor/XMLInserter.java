/* RTFGenerator class
 * This class generated the RTF document from the XML message as input &
 * RTF template as a reference.
 */
package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.*;
import com.nightfire.adapter.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.parser.*;

import com.nightfire.framework.db.PersistentProperty;
import java.util.*;

/**
 * This class allows xml to be added to the current xml by the following ways:
 * 1. conditional - if a specific condition exists then it can add a constant value
 * 2. always     - add a constant value every time.
 *
 * For both these cases you can force a value to be over written or skipped if it has a value
 */
public class XMLInserter extends MessageProcessorBase {

  private final static String FIELDVALUEPAIRSEP_PROP   = "FIELD_VALUE_PAIR_SEPARATOR";
  private final static String ANDPAIRSEP_PROP   = "AND_PAIR_SEPARATOR";
  private final static String ORPAIRSEP_PROP   = "OR_PAIR_SEPARATOR";
  private final static String OVERWRITE_PROP   = "OVERWRITE_NODES";

  private final static String ADD_FIELD_NAME_PREFIX_PROP = "ADD_FIELD_NAME";
  private final static String ADD_FIELD_VALUE_PREFIX_PROP = "ADD_FIELD_VALUE";
  private final static String ADD_FIELD_PREFIX_PROP = "ADD_FIELD_DEFAULT_PREFIX";
  private final static String ADD_FIELD_IF_PROP = "ADD_FIELD_IF";

  private boolean overWrite = false;
  private String fieldValueSep = null;
  private String andSep = null;
  private String orSep = null;


  //private final static String ADD_FIELD_COPYFROM_PROP = "ADD_FIELD_COPY";

  private Hashtable addedFieldsTable;

  // inner container class to hold the conditional statement and value
  private class IfContainer {
      String ifCondition = null;
      String fieldValue = null;
  }

  private String addedFieldsPrefix = ""; // the prefix for each fields


  /**
   * This method loads properties in virtual machine.
   * @param key - Ket value of persistentproperty.
   * @param type - Type value of persistentproperty.
   * @exception - ProcessingException - this exception is thrown by super class
   *              for any problem with initialization .
   */
  public void initialize ( String key, String type ) throws ProcessingException
  {
    Debug.log( this, Debug.DB_STATUS, "Loading properties for XMLInserter.");
    super.initialize(key, type);

    String temp = null;
    if ( (temp = (String)adapterProperties.get(OVERWRITE_PROP)) != null) {
       overWrite = Boolean.getBoolean(temp);
    }

    fieldValueSep = getReqProp(FIELDVALUEPAIRSEP_PROP);
    andSep = getReqProp(ANDPAIRSEP_PROP);
    orSep = getReqProp(ORPAIRSEP_PROP);

    addedFieldsTable = loadAdditionalFields();

  }

  /**
   * This method is called from driver.
   * It does all processing to generate RTF document.
   * @param context - MessageProcessorContext.
   * @param type - input - this contains XML message that needs to e converted to RTF document.
   * @return NVPair[] - this array contains only one instance of NVPair.
   *                    name - value of NEXT_PROCESSOR_NAME; value - generated RTF document.
   * @exception - MessageException, ProcessingException - messageException is thrown if parsing of XML fails
   *              ProcessingException is thrown for any other significant error.
   */
  public NVPair[] execute ( MessageProcessorContext context, Object input ) throws MessageException, ProcessingException
  {
     String newXml = "";

    // processing is done so return null to driver
     if(input == null)
        return null;


    if (!(input instanceof String)) {
       throw new ProcessingException("XMLInserter.process - ERROR message passed in was not a string");
    }

    XMLMessageParser parser = copyMessageWithAddedFields((String) input);

    newXml = parser.getGenerator().generate();

    return( formatNVPair(newXml) );
  }


  // gets additional fields that are in the properties and load them into a hashtable
  // for later
  // trims dots off the end of the ADD_FIELD_NAME_PREFIX value.

  private Hashtable loadAdditionalFields() throws ProcessingException
  {
     Hashtable temp = new Hashtable();

     addedFieldsPrefix = (String)adapterProperties.get(ADD_FIELD_PREFIX_PROP);
     addedFieldsPrefix = trimDots(addedFieldsPrefix);

     boolean done = false;
     for (int i = 0; !done ; i++) {

        String addFieldNameProp = PersistentProperty.getPropNameIteration(ADD_FIELD_NAME_PREFIX_PROP, i);
        String addFieldValueProp = PersistentProperty.getPropNameIteration(ADD_FIELD_VALUE_PREFIX_PROP, i);
        String addIfProp = PersistentProperty.getPropNameIteration(ADD_FIELD_IF_PROP, i);

        String addFieldName = trimDots( (String) adapterProperties.get(addFieldNameProp) );
        String addFieldValue = (String) adapterProperties.get(addFieldValueProp);

        String addIf         = (String) adapterProperties.get(addIfProp);

        if (!StringUtils.hasValue(addFieldName) && !StringUtils.hasValue(addFieldValue) ) {
           // no more pairs so stop
           done = true;
           continue;
        }
        else if (!StringUtils.hasValue(addFieldName) || !StringUtils.hasValue(addFieldValue) ) {
           throw new ProcessingException("RTFGenerator.loadAdditionalFields - ERROR property "
                 + addFieldNameProp +"," + addFieldValue + " needs to be in pairs." +
                 " Missing either name or value.");
        }

        if (!StringUtils.hasValue(addedFieldsPrefix)) {
           throw new ProcessingException("RTFGenerator.loadAdditionalFields - ERROR property " +
           ADD_FIELD_PREFIX_PROP + " needs to have a value " );
        }

        IfContainer container = new IfContainer();

        if (StringUtils.hasValue(addIf) ) {
           container.ifCondition = addIf;
        }

        container.fieldValue = addFieldValue;

        temp.put(addFieldName, container);
     }

     return temp;
  }

  private String trimDots(String str)
  {
     if (str == null)
        return null;

     while (str.startsWith(".") ) {
        str = str.substring(1);
     }

     while (str.endsWith(".") ) {
        str = str.substring(0, str.length() - 1);
     }
     return str;
  }

  /**
   * adds the new fields to the current message
   */
  private XMLMessageParser copyMessageWithAddedFields(String message) throws ProcessingException, MessageException
  {

     Debug.log(this,Debug.XML_GENERATE, "XMLInserter.copyMessageWithAddedFields "
                + " - adding property fields to generated xml");

     XMLMessageParser p;
     XMLMessageGenerator gen;
     MessageContext addPrefixContext;

     try {
        p = new XMLMessageParser(message);
     } catch (MessageParserException mpe ) {
        String error = "XMLInserter.copyMessageWithAddedFields -ERROR parsing xml message";
        //Debug.log(this,Debug.XML_ERROR, error + ":" + mpe.toString() );
        Debug.log(this,Debug.ALL_ERRORS, error);
        throw new MessageException(error + ":" + mpe.toString() );
     }

     try {
        gen = (XMLMessageGenerator) p.getGenerator();
     }  catch (MessageParserException mpe ) {
        String error = "XMLInserter.copyMessageWithAddedFields -ERROR getting generator";
         Debug.log(this,Debug.ALL_ERRORS, error);
        throw new ProcessingException(error + ":" + mpe.toString() );
     }

     try {
        gen.create(addedFieldsPrefix);
        addPrefixContext = gen.getContext(addedFieldsPrefix);
     } catch (MessageException me ) {
        throw new ProcessingException("XMLInserter.copyMessageWithAddedFields - ERROR getting xml context from generator:" + me.toString());
     }

     Enumeration enumerator = addedFieldsTable.keys();

     while (enumerator.hasMoreElements() ) {
        boolean addField = true;

        String key = (String) enumerator.nextElement();

         IfContainer container = (IfContainer) addedFieldsTable.get(key);

         if (container.ifCondition != null) {
           try {

                 ConditionParser cp = new ConditionParser(container.ifCondition, p, fieldValueSep, andSep, orSep);
                 IfConditionChecker icc = new IfConditionChecker();


                  addField = cp.isTrue(icc); //if condition fails then the field won't be added
               } catch (FrameworkException me) {
                  throw new ProcessingException("XMLInserter.copyMessageWithAddedFields - ERROR checking conditional fields: "+ me.toString() );
               } catch (NoSuchElementException nsee) {
                  throw new ProcessingException("XMLInserter.copyMessageWithAddedFields - ERROR ADD_FIELD_IF property has an incorrect separator in it: "+ nsee.toString() );
               }
         }

         if (addField) {
        // if the added field has a dots in it then set its value directly instead of using
        // the ADD_FIELD_PREFIX
           try {

              if (key.indexOf(".") > -1)   {
                 if (!hasValue(p,key) || overWrite )
                    gen.setValue(key, container.fieldValue);
              }
              else  {
                 if (!hasValue(p, addedFieldsPrefix + key) || overWrite )
                   gen.setValue(addPrefixContext, key, container.fieldValue);
              }
           } catch (MessageGeneratorException mge) {
                 throw new MessageException("XMLInserter.copyMessageWithAddedFields - ERROR setting name[" + key + "] and value[" + container.fieldValue + "]: " + mge.toString());
           }
         }
     }


     return(p);
  }



  /**
    * checks if a parser node exists and has a value
    */
  private boolean hasValue(XMLMessageParser p, String node) throws MessageParserException
  {
     if (p.exists(node) && !p.getValue(node).equals("") )
        return true;
      else
        return false;
  }


  private String getReqProp(String name) throws ProcessingException
  {
     String temp = (String)adapterProperties.get(name);
     if (!StringUtils.hasValue(temp) ) {
        Debug.log(this,Debug.ALL_ERRORS, "XMLInserter: ERROR property " + name + " needs a value");
        throw new ProcessingException("ERROR property " + name + " needs a value");
     }
     return (temp);
  }

}


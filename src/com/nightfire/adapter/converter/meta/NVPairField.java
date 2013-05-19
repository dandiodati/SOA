package com.nightfire.adapter.converter.meta;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.XPathAccessor;

import java.util.List;
import java.util.ArrayList;

/**
* This class is used to describe name/value pair fields.
*/
public class NVPairField extends SimpleField{

   private static List PROP_NAMES = null;

   public static final String DELIMITER     = "delimiter";
   public static final String SUFFIX        = "suffix";
   public static final String WRITE_EMPTIES = "write_empty_values";

   /**
   * The default name/value delimiter, an equals sign "=".
   */
   public static String DEFAULT_DELIMITER = "=";

   /**
   * The delimiter that will be written between the name and value.
   */
   private String delimiter = DEFAULT_DELIMITER;

   /**
   * The default value that should get written out at the end of each
   * the name/value pair
   */
   public static String DEFAULT_SUFFIX = ";";

   /**
   * The value that will get written out at the end of each the name/value pair.
   * By default this is a semicolon. 
   */
   private String suffix = DEFAULT_SUFFIX;   

   /**
   * This is a flag to indicate whether the NVPair should be written out if
   * it's location is not present in the XML.
   * 
   */
   private boolean writeEmptyValues = false;


   public NVPairField(String name){

      super(name);

   }


   public NVPairField(String name, String path){

      super(name, path);

   }

   /**
   * This assigns a new delimiter to separate the name/value pair when it
   * is written out.
   *
   * @param the new delimiter to use.
   */
   public void setDelimiter(String delim){

      delimiter = delim;

   }

   /**
   * Accesses the current delimiter.
   *
   * @return the current delimiter being used to separate the name/value
   *         pair.
   */
   public String getDelimiter(){

      return delimiter;

   }

   /**
   * This sets value that will get written out at the end of each the
   * name/value pair.
   */   
   public void setSuffix(String suffix){

      this.suffix = suffix;

   }

   /**
   * This gets current value that will be written out at the end of each the
   * name/value pair.
   */
   public String getSuffix(){

      return suffix;

   }   

   public void setWriteEmptyValues(boolean writeEmpties){

      writeEmptyValues = writeEmpties;

   }

   public boolean isWriteEmptyValues(){

      return writeEmptyValues;

   }

   /**
   * Write the prefix, name, delimiter, value from the input, and
   * suffix to the output buffer in that order.
   * 
   * @param context This is the current path to use as a context.
   * @param input A parsed XML input message. 
   * @param output The buffer where the output will be written.
   */
   public void write(FieldContext  context,
                     XPathAccessor source,
                     StringBuffer  output){

      FieldContext valuePath = getFullPath(context);
      
      if( source.nodeExists( valuePath.getXPath() ) ){

         writeValue( getValue( valuePath, source ), output );

      }
      else if(writeEmptyValues){
         writeValue("", output);
      }

   }

   /**
   * Writes out this NV pair with the given value.
   *
   * @param value the value to use when writing the NV pair.
   * @param output the buffer to which the output will be written
   */
   protected void writeValue(String value, StringBuffer output){

       Debug.log(Debug.MAPPING_STATUS, "Output: ["+getName()+getDelimiter()+value+getSuffix()+"]");   
       output.append( getName() );
       output.append( getDelimiter() );
       output.append( value );
       output.append( getSuffix() );

   }

   public void setProperty(String propName, Object value){

      if(value == null){
         super.setProperty(propName, null);
      }
      else if( propName.equalsIgnoreCase(DELIMITER) ){

         delimiter = (String) value;

      }
      else if( propName.equalsIgnoreCase(SUFFIX) ){

         suffix = (String) value;

      }
      else if( propName.equalsIgnoreCase(WRITE_EMPTIES) ){

         try{

            writeEmptyValues = StringUtils.getBoolean( (String) value );

         }
         catch(Exception ex){

            Debug.log(Debug.ALL_WARNINGS,
                      "The value of property ["+
                      WRITE_EMPTIES+
                      " was not a boolean value: "+ex.getMessage());

         }

      }
      else{

         super.setProperty(propName, value);

      }

   }

   public List getPropertyNames(){

      if(PROP_NAMES == null){

         PROP_NAMES = new ArrayList();
         PROP_NAMES.addAll(super.getPropertyNames());
         PROP_NAMES.add(DELIMITER);
         PROP_NAMES.add(SUFFIX);
         PROP_NAMES.add(WRITE_EMPTIES);

      }

      return PROP_NAMES;

   }

   public Object getProperty(String propName){

      if( propName.equalsIgnoreCase(DELIMITER) ){

         return delimiter;

      }
      else if( propName.equalsIgnoreCase(SUFFIX) ){

         return suffix;

      }
      else if( propName.equalsIgnoreCase(WRITE_EMPTIES) ){

         return new Boolean( writeEmptyValues );

      }

      return super.getProperty(propName);

   }   

   /**
   * Gets the default value for the given property.
   */
   public Object getDefaultPropertyValue(String propName){

      if( propName.equalsIgnoreCase(DELIMITER) ){

         return DEFAULT_DELIMITER;

      }
      else if( propName.equalsIgnoreCase(SUFFIX) ){

         return DEFAULT_SUFFIX;

      }
      else if( propName.equalsIgnoreCase(WRITE_EMPTIES) ){

         // don't write empty NVPair fields by default
         return Boolean.FALSE;

      }

      return super.getDefaultPropertyValue(propName);

   }

}
/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.List;
import java.util.ArrayList;

import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.XPathAccessor;

/**
* This field represents a constant value.
*/
public class ConstantField extends FixedLengthField{

   /**
   * The names of properties that can be get or set for this type of field.
   */
   private static List propertyNames = null;

   /**
   * The name of the property used for setting this field's constant
   * value. If no value is set, this Field's name is used as the default
   * constant value.
   */
   public static final String VALUE = "constant"; 

   /**
   * This is the constant value for this field.
   */
   private String value;

   /**
   * Creates a constant field with the given value. The path for this
   * field will be null. A null path indicates that this constant
   * field should not be added when generating XML.
   */
   public ConstantField(String value){

      this( value, 0 );

   }

   public ConstantField(String value, String path){

      this( value, 0, path ); 

   }

   public ConstantField(String value, int offset){

      this( value, offset, null );

   }

   public ConstantField(String value, int offset, String path){

      this(value, value, offset, path );

   }  


   public ConstantField(String name, String value, int offset, String path){

      super( name, offset, value.length(), path );
      setValue(value);

   }

   /**
   * Gets the constant value.
   */   
   public String getValue(){

      if(value == null){
         return getName();
      }

      return value;

   }

   /**
   * Gets the length of this constant value.
   */
   public int getLength(){

      return getValue().length();

   }
   
   /**
   * This writes the constant field when generating output from XML.
   */
    public void write(FieldContext  context,
                      XPathAccessor input,
                      StringBuffer  output){

       // Pad the field and write it to the output buffer.
       // This assumes that the fixed length fields are being written
       // in order, and there is no need to position the value here.
       output.append( getValue() );

   }   

   /**
   * If this field's <code>path<code> is null, then this field will
   * not add anything to the output generator. If this field does have
   * a path value, then this will set this field's constant value at that
   * path in the generator.
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.  
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output){

      write(context, value, 0, output);

   }

   public void write(FieldContext context,
                     String value,
                     int currentOffset,
                     XMLMessageGenerator output){

      if(path != null){
         
         setValue(getFullPath(context), getValue(), output);

      }

   }

   protected void setValue(String newValue){

      value = replaceEscapeSequences(newValue);

   }

   /**
   * Replaces escape sequences in the given value: "\n", "\t", "\r", or "\\"
   * with a newline, tab, carriage return, or a backslash respectively.
   */
   protected String replaceEscapeSequences(String s){

      s = StringUtils.replaceSubstrings(s, "\\n", "\n");
      s = StringUtils.replaceSubstrings(s, "\\t", "\t");
      s = StringUtils.replaceSubstrings(s, "\\r", "\r");
      // order is important, replace the backslashes last      
      s = StringUtils.replaceSubstrings(s, "\\\\", "\\");      

      return s;

   }

   /**
   * Replaces any newlines, tabs, carriage returns, or backslashes with
   * the escape sequences: "\n", "\t", "\r", or "\\".
   */
   protected String addEscapeSequences(String s){

      // order is important, replace the backslashes first
      s = StringUtils.replaceSubstrings(s, "\\", "\\\\");
      s = StringUtils.replaceSubstrings(s, "\n", "\\n");
      s = StringUtils.replaceSubstrings(s, "\t", "\\t");
      s = StringUtils.replaceSubstrings(s, "\r", "\\r");

      return s;

   }

   /**
   * Sets the given value for the property with the given name.
   */
   public void setProperty(String propName, Object value){

      if(value != null && propName.equalsIgnoreCase( VALUE ) ){

         setValue( value.toString() );

      }
      else{

         super.setProperty(propName, value);

      }

   }

   /**
   * Gets the list of names of properties available for this field.
   */
   public List getPropertyNames(){

      if(propertyNames == null){

         propertyNames = new ArrayList();
         propertyNames.add(VALUE);
         propertyNames.add(FixedLength.POSITION);
         propertyNames.add(FixedLength.LENGTH);
         propertyNames.add(SimpleField.PATH);         

      }

      return propertyNames;

   }


   /**
   * Gets the value of the property with the given name. 
   */
   public Object getProperty(String propName){

      if( propName.equalsIgnoreCase(VALUE) ){

         if(value != null){

            // replace the newlines with \n's, etc... 
            return addEscapeSequences(value);

         }
         return value;

      }

      return super.getProperty(propName);

   }

   /**
   * Gets the default value for the given property name.
   */
   public Object getDefaultPropertyValue(String propName){

      if( propName.equalsIgnoreCase(VALUE) ){

         // the default value for a constant is the field's name
         return getName();

      }
      else if( propName.equalsIgnoreCase(PATH) ){

         // the default path for a constant is null, indicating
         // that the constant should NOT be written when generating XML
         return null;

      }
      else if( propName.equalsIgnoreCase(FixedLength.LENGTH) ){

         return new Integer( getValue().length() );

      }

      return super.getDefaultPropertyValue(propName);

   }

}
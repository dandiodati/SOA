/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.List;
import java.util.ArrayList;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

/**
* This field represents a fixed length field that repeats a fixed number of
* times. 
*/
public class RepeatingFixedLengthField extends FixedLengthFieldContainer{

    private static final int DEFAULT_COUNT = 1;

    /**
    * The number of times the fixed length field repeats. The default
    * value is one, in which case, the behavior of this field is the
    * same as its parent class.
    */
    private int count = DEFAULT_COUNT;

    /**
    * The list of available property names for this type of field.
    */
    private static List propertyNames = null;

    /**
    * The property name used to set and get the number of times this field
    * repeats.
    */
    public static final String COUNT = "count";

    /**
    * Constructs an instance with the given name.
    */
    public RepeatingFixedLengthField(String fieldName) {

       super(fieldName);

    }

    /**
    * Constructs an instance with the given field values. 
    */
    public RepeatingFixedLengthField(String fieldName,
                                     String path){

       super(fieldName, path);

    }

    /**
    * Gets the total length of this field which is the
    * length of the repeating field times the
    * repition count.
    */
    public int getLength(){

       return super.getLength() * count;

    }

    /**
    * Retrieves the number of times this field repeats.
    */
    public int getCount(){

       return count;

    }

    /**
    * Gets the names of available properties for this type of field. This
    * includes all properties of parent fields.
    */
    public List getPropertyNames(){

       if(propertyNames == null){

          propertyNames = new ArrayList();
          propertyNames.addAll(super.getPropertyNames());
          propertyNames.add(COUNT);

       }

       return propertyNames;

    }

    /**
    * Gets the value of the property with the given name.
    */
    public Object getProperty(String propName){

       if( propName.equalsIgnoreCase( COUNT ) ){

          return new Integer( getCount() );

       }
       else if(propName.equalsIgnoreCase( FixedLength.LENGTH ) ){

          return new Integer( super.getLength() );

       }

       return super.getProperty(propName);

    }

    /**
    * Gets the default value for the given property.
    */
    public Object getDefaultPropertyValue(String propName){

       if( propName.equalsIgnoreCase( COUNT ) ){

          return new Integer( DEFAULT_COUNT );

       }
       else if(propName.equalsIgnoreCase( FixedLength.LENGTH ) ){

          return new Integer( super.getLength() );

       }

       return super.getDefaultPropertyValue(propName);       

    }

    /**
    * Sets the value for the property with the given name.
    */
    public void setProperty(String propName, Object value){

       if( propName.equalsIgnoreCase(COUNT) ){

          if( value != null ){

             try{

                count = Integer.parseInt(value.toString());

             }
             catch(NumberFormatException nfex){

                Debug.log(Debug.ALL_ERRORS,
                          "The ["+COUNT+"] property for field ["+getName()+
                          "] does not have a valid integer value: "+
                          nfex.getMessage());

             }

          }
          else{

             Debug.log(Debug.ALL_WARNINGS,
                       "The ["+COUNT+"] property for field ["+getName()+
                       "] is null.");

          }


       }
       else{

          super.setProperty(propName, value);

       }

    }

    /**
    * Writes repeating values from an XML to a string output buffer in a
    * padded fixed length format.
    */
    public void write(FieldContext context,
                      XPathAccessor input,
                      StringBuffer output){

       // gets the context up to and including this field's subpath
       context = super.getFullPath(context);

       String xPath = context.getXPath();
       int nodeCount = 0;

       // for however many times the input location appears in the input,
       // write and pad the input. Stop if we reach the max number of
       // possible repeating nodes.
       while( input.nodeExists( xPath ) &&  nodeCount < this.count ){

          // Pad current value correctly and write its
          // child fields to the output buffer.
          if(getAlignment() == FixedLength.ALIGN_RIGHT){
             super.pad(output);
          }

          super.visit(context, input, output);

          if(getAlignment() == FixedLength.ALIGN_LEFT){
             pad(output);
          }

          // increment the path so it points to the next node matching
          // the context path
          context.add();
          xPath = context.getXPath();

          // increment the number of values found
          nodeCount++;          

       }

       // if there were not as many input values as the max repetition
       // count, then pad out the rest of the empty fields
       for(; nodeCount < this.count; nodeCount++){

          for(int j = 0; j < super.getLength(); j++){
             output.append( getPadChar() );
          }

       }

    }

   /**
   * This takes the given value, and starting at this field's position,
   * starts reading the repeating values for this field and writing them to the
   * next consecutive location in the output xml.
   *
   * @param context The current XML path in which to set the values.
   * @param value The value for the all of the combined repeating fields.
   * @param output The XML generator to which each field value should be written.
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output){

      write(context, value, 0, output);

   }

   /**
   * This takes the given value, and starting at this field's position,
   * starts reading the repeating values for this field and writing them to the
   * next consecutive location in the output xml.
   *
   * @param context The current XML path in which to set the values.
   * @param value The value for the all of the combined repeating field
   * @param currentOffset this offset plus the position tell the 
   * @param output The XML generator to which each field value should be written.
   */
   public void write(FieldContext context,
                     String value,
                     int currentOffset,
                     XMLMessageGenerator output){

      context = super.getFullPath(context);

      int offset = currentOffset + getPosition();

      int subsectionCount = 0;

      // Get the total length of each child field.
      // This MUST use super.getLength(), this.getLength() will
      // return the total length of all of this field's repetitions.
      int subsectionLength = super.getLength();

      try{

         String subsection = null;

         // while there are still subsections and we have not passed
         // the max count for this field.
         while( (subsection == null || subsection.trim().length() > 0)
                && subsectionCount < this.count ){

            // get the next subsection
            subsection = value.substring( offset, offset + subsectionLength );

            // set this field's values in the output XML
            super.writeWithFullPath(context, value, offset, output);

            // Increment the offset.
            offset += subsectionLength;

            // increment the context path so that the next value gets
            // written to the next XML node
            context.add();

            // increment the number of values found
            subsectionCount++;

         }

      }
      catch(IndexOutOfBoundsException ex){

         // one of our subsections went beyond the total length of the given
         // value string. that's not good.
         Debug.log(Debug.ALL_ERRORS,
                   "Could not populate ["+context.getXPath()+
                   "] for field:"+this.toString()+
                   "\nThe input value is not long enough: "+
                   ex.getMessage() );

      }

   }

}
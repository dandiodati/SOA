/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

import java.util.*;

import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

/**
* A field that contains a sorted collection of FixedLength field instances.
*/
public class FixedLengthFieldContainer extends FieldContainer
                                       implements FixedLength{

   /**
   * The collection that keeps the fields sorted by their position number.
   */
   protected SortedSet sortedFields = new TreeSet();

   /**
   * This is the sum total length of all FixedLength fields that
   * have been added to this container so far.
   */
   protected int totalSubfieldLength = 0;

   /**
   * The names of properties that can be get or set for this type of field.
   */
   private static List propertyNames = null;

   /**
   * The pad character used to fill this field to its full length when
   * the field's value is shorter than the field.
   */
   private char padChar = DEFAULT_ALIGN_LEFT_PAD;

   /**
   * The position of this field in a fixed length format.
   */
   private int position;

   /**
   * The alignment of this field: ALIGN_RIGHT or ALIGN_LEFT.
   */
   private short alignment = ALIGN_LEFT;

   /**
   * This is full length of the field including any extra padding.
   * So if the totalSubfieldLength is less than this requiredLength,
   * this output of this container will be padded to reach the full
   * required length.
   * By default, the value of this field is -1 indicating that no padding
   * should be done.
   */
   protected int requiredLength = -1;

   /**
   * Creates a fixed length field container with the given field name.
   * The name will also be used to construct the XML location of this
   * field and its subfields.
   *
   * @param name the field name
   */
   public FixedLengthFieldContainer(String name){

      super(name);

   }


   /**
   * Creates a fixed length field container with the given field name.
   *
   * @param name the field name
   * @param path the path segement used in constructing the XML location
   *             of this field and its subfields.
   */
   public FixedLengthFieldContainer(String name, String path){

      super(name, path);

   }

   /**
   * Creates a fixed length field container with the given field name.
   *
   * @param name the field name
   * @param path the path segement used in constructing the XML location
   *             of this field and its subfields.
   * @param requiredLength this is the full
   */
   public FixedLengthFieldContainer(String name,
                                    String path,
                                    int requiredLength){

      super(name, path);
      this.requiredLength = requiredLength;

   }

   /**
   * Adds a field to this field container. For this class, the added
   * field must define the FixedLength interface.
   *
   * @throws ClassCastException if the given field is not a FixedLength.
   */
   public void add(Field field){

      addFixedLengthField((FixedLength) field);

   }

   /**
   * Removes a field from this field container. For this class, the removed
   * field must be an instance of the FixedLength interface.
   *
   * @throws ClassCastException if the given field is not a FixedLength.
   */
   public boolean remove(Field field){

      return removeFixedLengthField((FixedLength) field);

   }

   /**
   * Adds a field description.
   *
   * @param field the fixed-length field to be added.
   */
   public boolean addFixedLengthField(FixedLength field){

     boolean result = sortedFields.add(field);

     if( result  ) {

        totalSubfieldLength += field.getLength();

     }
     else{

         if( Debug.isLevelEnabled(Debug.ALL_WARNINGS) ){
            Debug.log(Debug.ALL_WARNINGS,
                      "Could not add field ["+field+
                      "] to container ["+name+
                      "]. A field already exists with offset ["+
                      field.getPosition()+"]");
         }

     }

     return result;

   }

   /**
   * Removes a FixedLength field.
   *
   * @param field the FixedLength field to be removed.
   */
   public boolean removeFixedLengthField(FixedLength field){

      boolean result = sortedFields.remove(field);

      if(result){

         totalSubfieldLength -= field.getLength();

      }

      return result;

   }

   /**
   * This passes of the current context and input source to all of this
   * container's child fields in the order of their respective positions.
   * This overrides the parent function in order to
   * pad any extra required spaces using the set pad char.
   *
   * @param currentContext the XML path context.
   * @param source the parsed XML source.
   * @param output the buffer to which output will be appended.
   */
   public void visit(FieldContext  currentContext,
                     XPathAccessor source,
                     StringBuffer  output ){

      if(alignment == FixedLength.ALIGN_RIGHT){
         pad(output);
      }

      super.visit(currentContext, source, output);

      if(alignment == FixedLength.ALIGN_LEFT){
         pad(output);
      }

   }

   /**
   * Appends this container's path to the context (by calling <code>
   * getFullPath()</code>) and passes the
   * given value off to each of the sub fields for XML generation.
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output){

      writeFromOffset( context, value, position, output);
      
   }

   /**
   * Appends this container's path to the context (by calling <code>
   * getFullPath()</code>) and passes the
   * given value off to each of the sub fields for XML generation.
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param currentOffset this offset will get added to this field's
   *                      position, and the 
   * @param output The XML generator to which the field should be written.
   */
   public void write(FieldContext context,
                     String value,
                     int currentOffset,
                     XMLMessageGenerator output){

      writeFromOffset( context, value, position + currentOffset, output);

   }

   /**
   * Common functionality used by write methods.
   */
   protected void writeFromOffset(FieldContext context,
                                  String value,
                                  int currentOffset,
                                  XMLMessageGenerator output){

      FieldContext fullPath = getFullPath(context);
      writeWithFullPath(fullPath, value, currentOffset, output); 

   }

   /**
   * Takes the given context, as is, and passes the context and the
   * given value off to each of the sub fields for XML generation. This
   * method is overriden from FieldContainer.
   *
   * @param fullPath The full path to the output node.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.
   *
   * @see FieldContainer 
   */
   protected void writeWithFullPath(FieldContext fullPath,
                                    String value,

                                    XMLMessageGenerator output){

      writeWithFullPath(fullPath, value, position, output);


   }   

   /**
   * This method uses the given context as is instead of calling
   * getFullPath() to get the full path including this field's
   * path. This is used by subclasses that have already handled
   * this in their own way.
   */
   protected void writeWithFullPath(FieldContext fullContext,
                                    String value,
                                    int currentOffset,
                                    XMLMessageGenerator output){

      FixedLength currentField;

      for(int j = 0; j < size(); j++){

          currentField = (FixedLength) getFieldAt(j);
          currentField.write(fullContext, value, currentOffset, output);

      }

   }

   /**
   * Pad in any extra required length.
   */
   protected void pad(StringBuffer output){

      int extra = requiredLength - totalSubfieldLength;

      while(extra > 0){

         output.append(padChar);
         extra--;

      }

   }

   /**
   * Gets the field at the given index.
   */
   public Field getFieldAt(int index){

      return (Field) sortedFields.toArray()[index];

   }

   /**
   * Gets the number of fields in this container.
   */
   public int size(){

      return sortedFields.size();

   }

   /**
   * Gets the "required length" of this field or the sum total of this
   * container's child fields' lengths, whichever length is larger.
   */
   public int getLength(){

      return Math.max(requiredLength, totalSubfieldLength);

   }

   /**
   * Gets the offset position of this field from the beginning of the record.
   */
   public int getPosition(){

      return position;

   }

   /**
   * Gets the character to be used when padding this field container
   * to its full required length.
   */
   public char getPadChar(){

      return padChar;

   }

   /**
   * Gets the alignment to be used when padding this field. If the
   * alignment is ALIGN_LEFT, then the value of the field will be written
   * out first followed by any necessary padding. If the alignment is
   * ALIGN_RIGHT, then the padding will be written first followed by
   * the field value.
   *
   * @return a short value that is either equal to FixedLength.ALIGN_RIGHT or
   *         FixedLength.ALIGN_LEFT.
   */
   public short getAlignment(){

      return alignment;

   }

   /**
   * This defines the Comparable interface and allows instances of this
   * class to be sorted based on position.
   *
   * @param o Another FixedLength implementation to compare to.
   *
   */
   public int compareTo(Object o){

      return FixedLengthUtils.compare(this, o);

   }

   /**
   * Gets the list of names of properties available for this field.
   */
   public List getPropertyNames(){

      if(propertyNames == null){

         propertyNames = new ArrayList();
         propertyNames.addAll(super.getPropertyNames());
         propertyNames.addAll(FixedLengthUtils.getFixedLengthPropertyNames());

      }

      return propertyNames;

   }   

   /**
   * Gets the value of the property with the given name. 
   */
   public Object getProperty(String propName){

      Object result = FixedLengthUtils.getFixedLengthProperty(this, propName);

      if(result == null){

         result = super.getProperty(propName);

      }

      return result;

   }

   /**
   * Gets the default value for the given property name.
   */
   public Object getDefaultPropertyValue(String propName){

      Object result = FixedLengthUtils.getDefaultFixedLengthProperty(propName);

      if( propName.equalsIgnoreCase( FixedLength.LENGTH ) ){

         result = new Integer( totalSubfieldLength );

      }

      if(result == null){

         result = super.getDefaultPropertyValue(propName);

      }

      return result;      

   }

   /**
   * Sets the given value for the property with the given name.
   */
   public void setProperty(String propName, Object value){

      if(value == null){
         super.setProperty(propName, null);
      }
      else if( propName.equalsIgnoreCase( FixedLength.ALIGNMENT )){

         String align = value.toString();
         if( align.equalsIgnoreCase(FixedLength.RIGHT) ){
            alignment = FixedLength.ALIGN_RIGHT;
         }
         else{
            alignment = FixedLength.ALIGN_LEFT;
         }

      }
      else if( propName.equalsIgnoreCase( FixedLength.POSITION ) ){

         position = Integer.parseInt( value.toString() );

      }
      else if( propName.equalsIgnoreCase( FixedLength.LENGTH ) ){

         // set the total required length for this container
         requiredLength = Integer.parseInt( value.toString() );

      }
      else if( propName.equalsIgnoreCase( FixedLength.PAD ) ){

         padChar = value.toString().charAt(0);

      }
      else{

         super.setProperty(propName, value);

      }

   }

   /**
   * Returns true if this container contains the given childField.
   */
   public boolean contains(Field childField){

      return sortedFields.contains(childField);

   }   


}


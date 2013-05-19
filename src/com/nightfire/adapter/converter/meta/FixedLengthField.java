/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.XPathAccessor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class FixedLengthField extends SimpleField implements FixedLength{

    /**
    * The name of the property used for setting and getting "trimming"
    * attribute of this field.
    */    
    public static final String TRIM = "trim";

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
   * The length of this field.
   */
   private int length;

   /**
   * The alignment of this field: ALIGN_RIGHT or ALIGN_LEFT.
   */
   private short alignment = ALIGN_LEFT;

   /**
   * This flag is used to indicate whether or not to trim the pad char
   * from the value when read from fixed length format.
   * The default trim setting is true.
   */
   private boolean trimming = true;

   /**
   * This keeps track of whether the pad property has been set.
   * This is used to handle the awkward case caused by the fact that
   * setting this field's alignment also sets its default padChar value.
   * This is a problem when the "pad" property gets set before the alignment
   * when being read from file. The call to setAlignment() overrides the
   * previously set padChar. This attribute is used to keep this from
   * happening. If this Character object is null, then setAlignment() will
   * set a default PAD char. If this Character has a value, then
   * setAlignment() will only change the alignment value and not
   * mess with the padChar. 
   */
   private Character padPropertyValue = null;

   /**
   * Constructs an instance with the given name and 0 length.
   */
   public FixedLengthField(String fieldName) {

       this(fieldName, 0, 0, ALIGN_LEFT);

   }

   /**
   * Constructs an instance with the given field values and default
   * LEFT_ALIGNMENT.
   */
   public FixedLengthField(String fieldName,
                           int position,
                           int length) {

       this(fieldName, position, length, ALIGN_LEFT);

   }

   public FixedLengthField(String fieldName,
                           int position,
                           int length,
                           String path) {

       this(fieldName, position, length, ALIGN_LEFT, path);

    }    

    public FixedLengthField(String fieldName,
                            int    position,
                            int    length,
                            short  alignment) {

       this(fieldName, position, length, alignment, fieldName);                            

    }

    public FixedLengthField(String fieldName,
                            int    position,
                            int    length,
                            short  alignment,
                            String path) {

       super(fieldName, path);
       this.position  = position;
       this.length    = length;
       setAlignment(alignment);

    }

    /**
    * Gets the offset of this field.
    */
    public int getPosition(){

       return position;

    }

    /**
    * Gets the length of the field.
    */
    public int getLength(){

       return length;

    }


    /**
    * Sets the alignment for this field and the default pad character for
    * that alignment.
    *
    * @param alignment either FixedLength.ALIGN_RIGHT or FixedLength.ALIGN_LEFT.
    */
    protected void setAlignment(short alignment){

       this.alignment = alignment;

       // if the "pad" property is null, then set the default
       // pad char for this particular alignment
       if(padPropertyValue == null){

          if(alignment == ALIGN_RIGHT){
             setPadChar(DEFAULT_ALIGN_RIGHT_PAD);
          }
          else{
             setPadChar(DEFAULT_ALIGN_LEFT_PAD);
          }

       }

    }


    /**
    * Gets the alignment to be used when padding this field. If the
    * alignment is ALIGN_LEFT, then the value of the field will be written
    * out first followed by any necessary padding. If the alignment is
    * ALIGN_RIGHT, then the padding will be written first followed by
    * the field value.
    *
    * @return a short value that is either equals to FixedLength.ALIGN_RIGHT or
    *         FixedLength.ALIGN_LEFT.
    */    
    public short getAlignment(){

       return alignment;

    }

    /**
    * Sets the character used to pad this field.
    */
    protected void setPadChar(char pad){

       padChar = pad;

    }

    /**
    * Sets the pad character from the given property value.
    */
    protected void setPadChar(Character pad){

       padPropertyValue = pad;

       if(pad != null){
          setPadChar( pad.charValue() );
       }

    }    

    /**
    * Gets the character used to pad this field.
    */
    public char getPadChar(){

       return padChar;

    }

   /**
   * Sets whether or not to trim the pad char
   * from the value when read from fixed length format.
   * The default trimming value is true.
   */    
    protected void setTrimming(boolean trim){

       trimming = trim;

    }

   /**
   * Returns whether or not this field is trimming the pad char
   * from the value when read from fixed length format.
   * The default trimming value is true.
   */
    public boolean getTrimming(){

       return trimming;

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
    * Writes values from an XML to a string output buffer in a fixed length
    * format.
    */
    public void write(FieldContext context,
                      XPathAccessor input,
                      StringBuffer output){

       FieldContext fullPath = super.getFullPath(context);

       // Pad the field and write it to the output buffer.
       // This assumes that the fixed length fields are being written
       // in order, and there is no need to position the value here.
       pad( getValue( fullPath, input ), output );

    }

    /**
    * Pads the given string with the pad character so that it fills the length
    * of this field, and write the value to the output buffer.
    */
    public void pad(String padMe, StringBuffer output){

         if( padMe.length() > length ){

            Debug.log(Debug.ALL_ERRORS, "The value ["+padMe+
                                        "] for field ["+getName()+
                                        "] has a length greater than its maximum allowed length of ["+
                                        length+"]");
            padMe = padMe.substring(0, length);

         }

         if(alignment == ALIGN_LEFT){

            padMe = StringUtils.padString( padMe, length, false, padChar);

         }
         else{

            padMe = StringUtils.padString( padMe, length, true, padChar);

         }

         Debug.log(Debug.MAPPING_STATUS, "Output: ["+padMe+"]");
         output.append( padMe );

    }

   /**
   * Reads this field from the given input value and writes it to
   * the correct location in the output message generator.
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

   /**
   * Reads this field from the given input value and offset and writes it to
   * the correct location in the output message generator.
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param currentOffset the offset to which this field will add its position
   *                      when retrieving this field's value.
   * @param output The XML generator to which the field should be written.  
   */   
   public void write(FieldContext context,
                     String value,
                     int currentOffset,
                     XMLMessageGenerator output){

      value = getValue(value, currentOffset + position);

      if(trimming){
          value = trim( value );
      }

      if( value.length() > 0 ){

         setValue(getFullPath(context),
                  value,
                  output);

      }

   }
   
   /**
   * Gets the value for this field from the given source string
   * starting at the given start offset.
   * This is basically a fancy substring method that logs any necessary
   * warnings.
   *
   * @param source the source string from which to get the value
   * @param start the offset of the source from which to start getting
   *              the substring value
   *
   * @return the substring of the given source from the start offset
   *         to start + length of this field 
   */
   protected String getValue(String source, int start){

      int sourceLength = source.length();

      String value = "";
      int end = start + length;

      if(Debug.isLevelEnabled( Debug.MAPPING_STATUS )){

         Debug.log(Debug.MAPPING_STATUS,
                   "Retrieving value from index ["+start+
                   "] to ["+end+"] for field ["+getName()+"]" );

      }

      if(sourceLength < start || start < 0){

         Debug.log(Debug.ALL_WARNINGS,
                   "Could not generate XML value for fixed length field ["+
                   getName()+"]. The value ["+source+
                   "] does not contain this entire field: start ["+start+
                   "] length ["+length+"]" );

      }
      else if( sourceLength < end ){
                   
         Debug.log(Debug.ALL_WARNINGS,
                   "The value ["+source+
                   "] is too short to contain entire field ["+
                   getName()+"]: start ["+start+
                   "] length ["+length+"]" );

         // get as much of the field as we can
         value = source.substring(start);

      }
      else{

         value = source.substring(start, end);

      }

      if(Debug.isLevelEnabled( Debug.MAPPING_STATUS )){

         Debug.log(Debug.MAPPING_STATUS,
                   "Value: ["+value+"]" );

      }

      return value;

   }

   /**
   * Sets the given value for the property with the given name.
   */
   public void setProperty(String propName, Object value){

      if( propName.equalsIgnoreCase( PAD ) ){

         Character charObject = null;

         if( value != null ){

            if(value instanceof Character){

               charObject = (Character) value;

            }
            else{

               try{

                  value.toString().charAt(0);

               }
               catch(IndexOutOfBoundsException ex){

                  if( Debug.isLevelEnabled(Debug.ALL_ERRORS) ){

                     Debug.log(Debug.ALL_ERRORS,
                               "The ["+PAD+"] property for field ["+getName()+
                               "] has no value.");

                  }

               }

            }

         }

         setPadChar(charObject);

      }
      else if(value != null){

            if( propName.equalsIgnoreCase( ALIGNMENT )){

               String align = value.toString();
               if( align.equalsIgnoreCase(RIGHT) ){
                  setAlignment(ALIGN_RIGHT);
               }
               else{
                  setAlignment(ALIGN_LEFT);
               }

            }
            else if( propName.equalsIgnoreCase( POSITION ) ){

               position = Integer.parseInt( value.toString() );

            }
            else if( propName.equalsIgnoreCase( LENGTH ) ){

               length = Integer.parseInt( value.toString() );

            }
            else if( propName.equalsIgnoreCase( TRIM ) ){

               trimming = Boolean.valueOf( value.toString() ).booleanValue();

            }
            else{

               super.setProperty(propName, value);
               
            }

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
         propertyNames.addAll(super.getPropertyNames());
         propertyNames.addAll(FixedLengthUtils.getFixedLengthPropertyNames());
         propertyNames.add(TRIM);

      }

      return propertyNames;

   }


   /**
   * Gets the value of the property with the given name. 
   */
   public Object getProperty(String propName){

      Object result = FixedLengthUtils.getFixedLengthProperty(this, propName);

      if(result == null){

         // get the value of the trimming property
         if( propName.equalsIgnoreCase(TRIM) ){

            result = trimming ? Boolean.TRUE : Boolean.FALSE;

         }
         else{

            result = super.getProperty(propName);

         }

      }

      return result;

   }

   /**
   * Gets the default value for the given property name.
   */
   public Object getDefaultPropertyValue(String propName){

      Object result = FixedLengthUtils.getDefaultFixedLengthProperty(propName);

      if(result == null){

         if( propName.equalsIgnoreCase(TRIM) ){

            // trim the fixed length values from the padding side by default
            result = Boolean.TRUE;

         }
         else{

            result = super.getDefaultPropertyValue(propName);

         }

      }

      return result;

   }

   /**
   * This trims the pad char from one end of the given value. If
   * this field is left aligned, then the pad char will be trimmed from the
   * right end of the value, if this field is right aligned, then the
   * pad char will be trimmed from the beginning of the value.
   *
   * @param value the value to be trimmed.
   * @return The trimmed value. If the value is entirely made up of pad chars,
   *         then an empty string is returned.
   */
   private String trim(String value){

      int head = 0;
      int tail = value.length();

      if( getAlignment() == FixedLength.ALIGN_LEFT ){

         // trim from the tail index while there are still padChars
         while( (tail-1) >= head && value.charAt(tail-1) == padChar ){

            tail--;

         }

      }
      else{

         // trim from the beginning
         while( head < tail && value.charAt(head) == padChar ){

            head++;

         }

      }

      if(head > tail){

         // the entire value was made up of pad characters, so the value is
         // empty
         value = "";

      }
      else{
         value = value.substring(head, tail);
      }

      if(Debug.isLevelEnabled( Debug.MAPPING_STATUS )){

         Debug.log(Debug.MAPPING_STATUS,
                   "Trimmed value: ["+value+"]" );

      }

      return value;

   }

}
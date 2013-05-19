/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.List;
import java.util.ArrayList;

/**
* Common functionality used by implementations of the FixedLength
* interface. Mostly this class is needed because the FixedLengthField and
* FixedLengthFieldContainer classes share some functionality but
* have different parent classes.
*/
public abstract class FixedLengthUtils {

   /**
   * The list of properties for a fixed length field.
   */
   private static List propertyNames = null;

   /**
   * Used to compare the positions of two FixedLength implementations
   * for sorting purposes.
   *
   * @return the position of compareMe minus the position of toMe.
   *         If toMe is not an instance of FixedLength, then this
   *         returns 0;
   */
   public static int compare(FixedLength compareMe, Object toMe){

       int result = 0;

       if(toMe instanceof FixedLength){

          FixedLength field2 = (FixedLength) toMe;
          int position2 = field2.getPosition();
          result = compareMe.getPosition() - position2;

       }

       return result;

   }


   /**
   * Gets the value of a fixed length field property for
   * the given field.
   *
   * @param field the field whose property values should be retrieved
   * @param propName the name of the property to be retrieved.  
   */
   public static Object getFixedLengthProperty(FixedLength field,
                                               String propName){

      if( propName.equalsIgnoreCase( FixedLength.ALIGNMENT ) ){

         if( field.getAlignment() == FixedLength.ALIGN_RIGHT ){
            return FixedLength.RIGHT;
         }

         return FixedLength.LEFT;

      }
      else if( propName.equalsIgnoreCase( FixedLength.POSITION ) ){

         return new Integer( field.getPosition() );

      }
      else if( propName.equalsIgnoreCase( FixedLength.LENGTH ) ){

         return new Integer( field.getLength() );

      }
      else if( propName.equalsIgnoreCase(FixedLength.PAD) ){

         return new Character( field.getPadChar() );

      }

      return null;

   }

   /**
   * Gets the list of properties used by fixed length fields. These
   * currently are POSITION, LENGTH, ALIGNMENT, and PAD char. The
   * constants for these property names are found in the FixedLength interface.
   */
   public static List getFixedLengthPropertyNames(){

      if(propertyNames == null){

         propertyNames = new ArrayList();
         propertyNames.add(FixedLength.POSITION);
         propertyNames.add(FixedLength.LENGTH);
         propertyNames.add(FixedLength.ALIGNMENT);
         propertyNames.add(FixedLength.PAD);

      }

      return propertyNames;

   }

   /**
   * Gets the default value for a fixed length field property.
   *
   * @param propName the name of the property 
   */
   public static Object getDefaultFixedLengthProperty(String propName){

      if( propName.equalsIgnoreCase( FixedLength.ALIGNMENT ) ){

         return FixedLength.LEFT;

      }
      else if( propName.equalsIgnoreCase( FixedLength.POSITION ) ){

         return new Integer( 0 );

      }
      else if( propName.equalsIgnoreCase( FixedLength.LENGTH ) ){

         return new Integer( 0 );

      }
      else if( propName.equalsIgnoreCase(FixedLength.PAD) ){

         return new Character( FixedLength.DEFAULT_ALIGN_LEFT_PAD );

      }

      return null;

   }
   
} 
/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

/**
* The interface implemented by fixed length fields.
* This extends the Comporable interface so that FixedLength
* implementations can be sorted based on their position.
*/
public interface FixedLength extends Comparable, Field{

    public static final short ALIGN_RIGHT = 0;
    public static final short ALIGN_LEFT  = 1;

    /**
    * The default pad char for right-aligned fields. These fields
    * are usually numeric, and so the pad character is a zero.
    */
    public static final char DEFAULT_ALIGN_RIGHT_PAD = '0';

    /**
    * The default pad char for left-aligned fields. This default
    * character is a space.
    */    
    public static final char DEFAULT_ALIGN_LEFT_PAD  = ' ';

    /**
    * The name of the property used for setting and getting the position
    * or offset of a fixed length field.
    */
    public static final String POSITION  = "offset";

    /**
    * The name of the property used for setting and getting the length
    * of a fixed length field.
    */
    public static final String LENGTH    = "length";

    /**
    * The name of the property used for setting and getting the length
    * of a fixed length field.
    */    
    public static final String PAD       = "pad";

    /**
    * The name of the property used for setting and getting the alignment
    * of a fixed length field.
    */
    public static final String ALIGNMENT = "alignment";

    /**
    * A string value for the ALIGNMENT property indicating LEFT alignment.
    */
    public static final String LEFT      = "LEFT";

    /**
    * A string value for the ALIGNMENT property indicating RIGHT alignment.
    */
    public static final String RIGHT     = "RIGHT";

    /**
    * Gets the offset of the field.
    */
    public int getPosition();

    /**
    * Gets the length of the field.
    */
    public int getLength();

    /**
    * Gets the character to be used when padding this field to its full length.
    */
    public char getPadChar();

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
    public short getAlignment();

    /**
    * Reads this field from the given value string and assigns that
    * value to the appropriate location in the given generator.
    *
    * @param context The current XML path to use as a context.
    * @param value The value for the field.
    * @param output The XML generator to which the field should be written.
    */
    public void write(FieldContext context,
                      String value,
                      int currentOffset,
                      XMLMessageGenerator output);

} 
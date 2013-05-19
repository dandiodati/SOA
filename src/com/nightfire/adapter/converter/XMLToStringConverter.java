/**
* Copyright (c) 2001 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import com.nightfire.spi.common.driver.MessageObject;

import org.w3c.dom.Document;

/**
* This is the base class for converters that take XML input and convert
* it to string output in another format. 
*/
public abstract class XMLToStringConverter extends FromXMLConverter{

    /**
    * This takes in a XML Document, calls <code>convertToString</code>
    * to convert it to a String object, and returns the result.
    *
    * @param input The MessageObject containing the input Document.
    * @return The original message object now containing the output String.
    * @exception ConverterException if a DOM can not be retrieved from
    *                               the input message object. 
    */
    public Object convert(Document input) throws ConverterException{

       return convertToString(input);

    }

    /**
    * Implementations of this method will take the input Document and
    * return the correspnding string representation.
    *
    * @param input the input message
    * @return the converted XML result.
    */
    public abstract String convertToString(Document input)
                                           throws ConverterException;
   
} 
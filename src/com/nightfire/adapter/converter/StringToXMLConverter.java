/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import com.nightfire.spi.common.driver.MessageObject;

import org.w3c.dom.Document;

/**
* This is the base class for converters that take string input and generate
* XML output. 
*/
public abstract class StringToXMLConverter extends ToXMLConverter{

    /**
    * This takes in a string message, converts it to an XML Document, and 
    * returns the result.
    *
    * @param message The MessageObject containing the input message.
    * @return the DOM Document for the resulting XML.
    * @exception ConverterException if a string can not be retrieved from
    *                               the input message object. 
    */
    public Document convertToXML(MessageObject message) throws ConverterException{

       String inputString;

       try{

          inputString = message.getString();

       }
       catch(Exception ex){

          throw new ConverterException("Converter could not get input message: "+
                                       ex.getMessage() );

       }

       // Convert the input string and return the XML document
       return convert( inputString );

    }

    /**
    * Implementations of this method will take the input string and generate
    * the appropriate XML.
    *
    * @param input the string input message
    * @return the converted XML result.
    */
    public abstract Document convert(String input) throws ConverterException;

} 
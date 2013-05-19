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
* This is the base class for converters that generate XML output.
*/
public abstract class ToXMLConverter implements Converter{

    /**
    * This takes an input MessageObject, converts it to an XML Document, and 
    * returns the result.
    *
    * @param message The MessageObject containing the input message.
    * @return A new message object containing the resulting XML Document.
    * @exception ConverterException if the input message could not be converted.
    */
    public MessageObject convert(MessageObject message) throws ConverterException{

       // Convert the input to an XML document
       Document document = convertToXML( message );

       // Set the output document as the value
       // of the output message object.
       return new MessageObject(document );

    }

    /**
    * This method is called by <code>convert</code> to convert the input
    * MessageObject to an XML Document.
    *
    * @param message the input MessageObject.
    * @return the XML Document representation of the input message.
    */
    public abstract Document convertToXML(MessageObject message)
                                          throws ConverterException;

} 
/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;

import org.w3c.dom.Document;

/**
* This is the base class for converters that take an XML input.
*/
public abstract class FromXMLConverter implements Converter{

    /**
    * This takes in a XML Document, converts it, and returns the result.
    *
    * @param input The MessageObject containing the input Document.
    * @return A new message object containing the output String.
    * @exception ConverterException if a DOM can not be retrieved from
    *                               the input message object. 
    */
    public MessageObject convert(MessageObject input) throws ConverterException{

       Document document;

       try{

          document = input.getDOM();

       }
       catch(Exception ex){

          throw new ConverterException("Converter could not get a document from input message: "+
                                       ex.getMessage() );

       }

       // Convert the XML document to an output object
       Object output = convert( document );

       // create a new output MessageObject 
       MessageObject result = new MessageObject( output );

       return result;

    }

    /**
    * This method is called by the <code>convert()</code> method to convert
    * the input XML Document object into a new type of Object. 
    */
    public abstract Object convert(Document input) throws ConverterException;

    
    /**
    * This utility method creates a message parser for the given Domcument.
    * This method handles the try-catch block needed to catch and log
    * the possible (but unlikely) MessageException that may occur.
    */
    public static XMLMessageParser getParser(Document doc)
                                             throws ConverterException{

       try{

         return new XMLMessageParser(doc);

       }
       catch(MessageException mex){

          throw new ConverterException(mex.getMessage());

       }

    }

} 

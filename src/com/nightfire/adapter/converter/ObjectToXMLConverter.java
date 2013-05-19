/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import com.nightfire.framework.util.Debug;
import com.nightfire.spi.common.driver.MessageObject;

import org.w3c.dom.Document;

/**
* This is the base class for converters that take an input object and generate
* XML output. 
*/
public abstract class ObjectToXMLConverter extends ToXMLConverter{

    /**
    * This takes in an object representation of a message (e.g. a CORBA object,
    * a byte array, etc.) and converts it to an XML Document,
    * returning the result.
    *
    * @param message The MessageObject containing the input message.
    * @return The original message object containing a DOM Document for the
    *         resulting XML.
    * @exception ConverterException if the message could not be converted 
    */
    public Document convertToXML(MessageObject message)
                                 throws ConverterException{

       Object input;

       try{

          input = message.get();

       }
       catch(Exception ex){

          throw new ConverterException("Converter could not get Object from input message: "+
                                       ex.getMessage() );

       }

       // Convert the input string to an XML document
       return convert( input );

    }

    /**
    * Implementations of this method will take the input object and generate
    * the appropriate XML.
    *
    * @param input the input object
    * @return the converted XML result.
    */
    public abstract Document convert(Object input) throws ConverterException;
   
} 
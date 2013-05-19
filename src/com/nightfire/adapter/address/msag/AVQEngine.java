/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/main/com/nightfire/spi/address/msag/AVQEngine.java#3 $
 */

package com.nightfire.adapter.address.msag ;

import org.w3c.dom.*;

import com.nightfire.common.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;

/**
 * Abstract message protocol adapter for Address Validation Queries
 */
public abstract class AVQEngine extends MessageProcessorBase
{
    /**
     * Convert DOM Document to XML stream.
     *
     * @param  doc  Input DOM Document object to process.
     *
     * @return  Equivalent XML stream.
     *
     * @exception  MessageException  Thrown if processing fails.
     */
    protected String DocumentToString (Document doc)
    	throws ProcessingException, MessageException
    {
        try
        {
        	DocumentType docType = doc.getDoctype();
        	String docName = "ANONYMOUS";
        	if (docType != null) {
        		docName = docType.getName();
        	}
            XMLMessageGenerator generator = new XMLMessageGenerator( docName );

            generator.setDocument( doc );

            if(Debug.isLevelEnabled(Debug.XML_GENERATE))
                Debug.log( Debug.XML_GENERATE, "Generating XML stream for DOM Document object-tree:" );

            generator.log( );

            String xmlText = (String)generator.generate( );

            if(Debug.isLevelEnabled(Debug.XML_GENERATE))
                Debug.log(Debug.XML_GENERATE, "Generated XML document:\n" + xmlText );

            return xmlText;
        }
        catch ( MessageGeneratorException mge )
        {
            throw new ProcessingException("ERROR: AVQEngine: " +  mge.getMessage() );
        }
    }


    /**
     * Convert XML stream to DOM Document object.
     *
     * @param  xmlStream  String containing XML message stream.
     *
     * @return  Equivalent DOM Document object.
     *
     * @exception  MessageException  Thrown if processing fails.
     */
    protected Document StringToDocument ( String xmlStream )
    	throws MessageException
    {
        try
        {
            if(Debug.isLevelEnabled(Debug.XML_PARSE))
                Debug.log(Debug.XML_PARSE, "Parsing XML stream:\n" + xmlStream );

            // Create a parser to parse the XML document.
            XMLMessageParser parser = new XMLMessageParser( );

            // Give the parser the XML document to parse.
            parser.parse( xmlStream );

            if(Debug.isLevelEnabled(Debug.XML_PARSE))
                Debug.log(Debug.XML_PARSE, "Parsed XML document:" );

            parser.log( );

            // Extract DOM Document object from parser and return it.
            return( parser.getDocument() );
        }
        catch ( MessageParserException mpe )
        {
            throw new MessageException("ERROR: AVQEngine:" + mpe.getMessage() );
        }
    }
    /**
     * A utility method for creating an XMLMessageParser.
     * with a DOM representing the given XML String.
     *
     * @param  xmlToBeParsed The input XML.
     *
     * @return  The desired parser
     */
    public static XMLMessageParser getParser(String xmlToParse) throws ProcessingException,
											     MessageException
    {

        MessageParser parser;

        try {

           parser =
            (XMLMessageParser) MessageParserFactory.create(Message.XML_TYPE);

        } catch (MessageParserException e) {

            throw new ProcessingException("ERROR: AVQEngine: " +
                                       "Could not create parser. :" + e.getMessage());
        }

        try {

            parser.parse (xmlToParse);

        } catch(MessageParserException e) {
                throw new MessageException("ERROR: AVQEngine: " +
                          "Could not parse xml string: " + xmlToParse +
                          " Error message: " + e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "AVQEngine.getParser: Successfully parsed xml file. ");
        
        return (XMLMessageParser)parser;
    }
}


/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/DefaultXMLMessageProtocolAdapter.java#1 $
 */
 
package com.nightfire.adapter.messageprocessor;


import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * Default message protocol adapter for translating XML to DOM Document, and visa-versa.
 */
public class DefaultXMLMessageProtocolAdapter extends MessageProcessorBase
{
    /**
     * Process the input message (DOM or String) and (optionally) return
     * a value.
     * 
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Optional output message, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public NVPair[] execute ( MessageProcessorContext mpcontext, Object input ) throws MessageException, ProcessingException
    {
        if ( input == null )
            return null;

        // If it's a String, assume it's an XML stream and attempt to parse it into DOM.
        if ( input instanceof String )
        {
            return( formatNVPair(StringToDocument((String)input)) );
        }
        else   // Convert DOM to xml stream.
        if ( input instanceof Document )
        {
            return( formatNVPair(DocumentToString((Document)input)) );
        }

        throw new MessageException( "ERROR: DefaultXMLMessageProtocolAdapter: Unknown object type given to XML message protocol adapter [" +           
                                    input.getClass().getName() + "]" );

    }

   
    /**
     * Convert DOM Document to XML stream.
     * 
     * @param  doc  Input DOM Document object to process.
     *
     * @return  Equivalent XML stream.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected String DocumentToString ( Document doc ) throws ProcessingException
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
        
            Debug.log( this, Debug.XML_GENERATE, "Generating XML stream for DOM Document object-tree:" );
            try
            {
              generator.log( );
            }
            catch (MessageException me)
            {
               throw new ProcessingException (me.getMessage());
            }
            
            String xmlText = (String)generator.generate( );
            
            Debug.log( this, Debug.XML_GENERATE, "Generated XML document:\n" + xmlText );
            
            return xmlText;
        }
        catch ( MessageGeneratorException mge )
        {
            throw new ProcessingException( mge.toString() );
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
    protected Document StringToDocument ( String xmlStream ) throws MessageException
    {
        try
        {
            Debug.log( this, Debug.XML_PARSE, "Parsing XML stream:\n" + xmlStream );
            
            // Create a parser to parse the XML document.
            XMLMessageParser parser = new XMLMessageParser( );
            
            // Give the parser the XML document to parse.
            parser.parse( xmlStream );
            
            Debug.log( this, Debug.XML_PARSE, "Parsed XML document:" );
            parser.log( );
            
            // Extract DOM Document object from parser and return it.
            return( parser.getDocument() );
        }
        catch ( MessageParserException mpe )
        {
            throw new MessageException( mpe.toString() );
        }
    }
}


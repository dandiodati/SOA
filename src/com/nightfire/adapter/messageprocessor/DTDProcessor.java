/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.adapter.messageprocessor;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;


/**
 * Message-processor used to modify DTD (DOCTYPE) references in XML documents.
 */
public class DTDProcessor extends MessageProcessorBase
{
    /**
     * Property indicating type of operation (insert, remove, modify).
     */
    public static final String OPERATION_TYPE_PROP = "OPERATION_TYPE";
    
    /**
     * Property giving the DTD reference.
     */
    public static final String DTD_REFERENCE_PROP  = "DTD_REFERENCE";
    

    // Allowable operation-type values.
    public static final String OP_TYPE_INSERT = "INSERT";
    public static final String OP_TYPE_REMOVE = "REMOVE";
    public static final String OP_TYPE_MODIFY = "MODIFY";
    
    // Token indicating the end of an XML declaration.
    public static final String XML_DECLARATION_END_TOKEN = "?>";
    
    
    /**
     * Configure message-processor from database properties.
     *
     * @param key   Persistent property key value.
     * @param type  Persistent property type value.
     *
     * @exception  ProcessingException  Thrown if the specified properties
     *                                  cannot be found or are invalid.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize ( key, type );
        
        Debug.log( Debug.MSG_STATUS, "Initializing DTD processor ..." );
        
        operationType = getRequiredPropertyValue( OPERATION_TYPE_PROP );
    }
    
    
    /**
     * Perform any requested DTD processing on the input.
     *
     * @param  mpcontext The context that stores control information
     * @param  input  Input message to process.

     * @return  NVPair containing the transformed XML document.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if bad message.
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input )
                        throws MessageException, ProcessingException
    {
        Debug.log( Debug.MSG_STATUS, "Processing DTD reference ..." );
        
        //Usual processing during null round of MessageProcessingDriver
        if ( input == null )
        {
            Debug.log( Debug.MSG_STATUS, "Nothing to do for null input." );

            return null;
        }

        // Get XML stream from input.
        String xmlInput = input.getString( );
        
        String xmlOutput = null;
        
        // Perform processing based on operation type.
        if ( OP_TYPE_INSERT.equalsIgnoreCase( operationType ) )
        {
            Debug.log( Debug.MSG_STATUS, "Inserting DTD reference into XML document ..." );
            
            String dtdReference = getRequiredPropertyValue( DTD_REFERENCE_PROP );
            
            xmlOutput = insertDTDReference( xmlInput, dtdReference );
        }
        else
        if ( OP_TYPE_REMOVE.equalsIgnoreCase( operationType ) )
        {
            Debug.log( Debug.MSG_STATUS, "Removing DTD reference from XML document ..." );
            
            xmlOutput = removeDTDReference( xmlInput );
        }
        else
        if ( OP_TYPE_MODIFY.equalsIgnoreCase( operationType ) )
        {
            Debug.log( Debug.MSG_STATUS, "Modifying DTD reference from XML document ..." );

            String dtdReference = getRequiredPropertyValue( DTD_REFERENCE_PROP );

            xmlOutput = modifyDTDReference( xmlInput, dtdReference );
        }
        else
        {
            throw new ProcessingException( "ERROR: Unknown operation type [" + operationType + "]." );
        }

        return( formatNVPair( new MessageObject( xmlOutput ) ) );
    }
    
    
    /**
     * Insert given DTD reference into the XML text stream.
     *
     * @param  input  String containing XML document.
     * @param  dtdReference  String containing the DOCTYPE declaration.

     * @return  Transformed XML text stream.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    protected String insertDTDReference ( String input, String dtdReference ) throws MessageException
    {
        // Check for pre-existing DTD reference.
        int check = input.indexOf( XMLLibraryPortabilityLayer.DTD_REF_TOKEN );
        
        // If we found a DOCTYPE reference, signal error to caller.
        if ( check != -1 )
        {
            throw new MessageException( "ERROR: Input XML already has a DTD reference, so can't insert an additional one." );
        }

        // Locate end of XML declaration.
        int end = input.indexOf( XML_DECLARATION_END_TOKEN );
        
        if ( end == -1 )
        {
            throw new MessageException( "ERROR: Can't find end of XML declaration." );
        }
        
        // Advance index past end of XML declaration.
        end += XML_DECLARATION_END_TOKEN.length( );
        
        Debug.log( Debug.MSG_STATUS, "New DTD reference is [" + dtdReference + "]." );

        // Return input with DTD reference added.
        return( input.substring( 0, end ) + "\n" + dtdReference + input.substring( end ) );
    }
    
    
    /**
     * Remove DTD reference from the XML text stream.
     *
     * @param  input  String containing XML document.

     * @return  Transformed XML text stream.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    protected String removeDTDReference ( String input ) throws MessageException
    {
        // Locate start of DTD reference.
        int start = input.indexOf( XMLLibraryPortabilityLayer.DTD_REF_TOKEN );
        
        // If no DTD reference is present, return the unmodified input.
        if ( start == -1 )
        {
            Debug.log( Debug.MSG_STATUS, "There is no DTD reference to remove." );

            return input;
        }

        // Locate end of DTD reference.
        int end = input.indexOf( ">", start + XMLLibraryPortabilityLayer.DTD_REF_TOKEN.length() );
        
        // If we can't find the end, signal error to caller.
        if ( end == -1 )
        {
            throw new MessageException( "ERROR: Can't find end of DTD reference." );
        }
        
        // Return input with DTD reference removed.
        return( input.substring( 0, start ) + input.substring( end + 1 ) );
    }
    
    
    /**
     * Modify DTD reference in the XML text stream.
     *
     * @param  input  String containing XML document.
     * @param  dtdReference  String containing the DOCTYPE declaration.

     * @return  Transformed XML text stream.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    protected String modifyDTDReference ( String input, String dtdReference ) throws MessageException
    {
        String temp = removeDTDReference( input );

        return( insertDTDReference( temp, dtdReference ) );
    }
    
    
    private String operationType;
}

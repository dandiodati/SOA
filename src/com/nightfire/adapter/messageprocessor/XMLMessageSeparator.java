/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/XMLMessageSeparator.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;


/**
 * This is a generic message-processor for separating an XML message into small XML messages.
 * 
 */
public class XMLMessageSeparator extends MessageProcessorBase
{
    /**
     * Property indicating where the XML message locates.
     */
    public static final String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";

    /**
     * Property prefix giving the node to be choosen.
     */
    public static final String SOURCE_PREFIX_PROP = "SOURCE";

    /**
     * Property prefix giving location where the separated XML message will be stored.
     */
    public static final String TARGET_PREFIX_PROP = "TARGET";



    private LinkedList mapping = null;
    private String inputLocation = null;
    
    
    /**
     * Constructor.
     */
    public XMLMessageSeparator ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating XML message separator message-processor." );

        mapping = new LinkedList( );
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );
        
        // Get configuration properties specific to this processor.
        Debug.log( Debug.SYSTEM_CONFIG, "XMLMessageSeparator: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );
        
        inputLocation = getPropertyValue( INPUT_MESSAGE_LOCATION_PROP );
        

        // Loop until all mapping configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String source = getPropertyValue( PersistentProperty.getPropNameIteration( SOURCE_PREFIX_PROP, Ix ) );

            // If we can't find a source name, we're done.
            if ( !StringUtils.hasValue( source ) )
                break;

            String target = getRequiredPropertyValue( PersistentProperty.getPropNameIteration( TARGET_PREFIX_PROP, Ix ), errorBuffer );

            // Create a map and add it to the list.
            NVPair map = new NVPair( source, target );

            mapping.add( map );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Number of messages to save [" + mapping.size() + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "XMLMessageSeparator: Initialization done." );
    }


    /**
     * Extract the sub-message from the specified source and store them in the specified
     * target.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException 
    {
        if ( inputObject == null )
            return null;

        Debug.log( Debug.MSG_STATUS, "XMLMessageSeparator: processing ... " );
        
        // Separating the message.
        MessageObject outputObject = separateMessage( mpContext, inputObject );
        

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( outputObject ) );
    }

    
    /**
     * Separate the message and store in the message object.
     *
     * @param mpcontext The message context.
     * @param intputObject The input object.
     *
     * @return The message object that contains all separated messages.
     *
     * @exception MessageException  thrown if required value not found.
     * @exception ProcessingException thrown if any other processing error occurs
     */
    private MessageObject separateMessage ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        Debug.log (Debug.MSG_STATUS, "Separating messages..." );

        Document input = getDOM( inputLocation, context, inputObject);

        XMLMessageParser inputParser = new XMLMessageParser();
        inputParser.setDocument(input);
        
        MessageObject messageObject = new MessageObject();
        
        Iterator iter = mapping.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            NVPair map = (NVPair)iter.next( );
            String source = map.name;
            String target = (String) map.value;

            try
            {

                if (!inputParser.nodeExists(source))
                {
                    String errMsg = "ERROR: The node [" + source + "] specified " +
                        "was not found.";
                    
                    throw new ProcessingException( errMsg );
                }
                    
                Node node = inputParser.getNode(source);

                Document doc = convertNodeToDOM(node);
            
                set(target, context, messageObject, doc);
            }
            catch ( MessageException me )
            {
                Debug.logStackTrace(me);
                
                throw new ProcessingException( "ERROR: Failed to generate XML message for [" + source + "].\n" 
                                               + me.toString() );
            }
            
            
        }

        Debug.log (Debug.MSG_STATUS, "Done separating message." );

        return messageObject;
        
    }


    private static Document convertNodeToDOM ( Node node) throws MessageGeneratorException
    {
        String nodeName = node.getNodeName();

        XMLMessageGenerator gen = new XMLMessageGenerator(nodeName);

        gen.setValue(".", node);

        return gen.getDocument();
    }
    
}

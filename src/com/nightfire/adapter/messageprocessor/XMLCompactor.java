/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/XMLCompactor.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;

import com.nightfire.adapter.util.*;

/**
 * This message-processor is used to format XML messages into strings without
 * extra spaces or line breaks. It is intended to be used before a mercator processor
 * to format the XML messages into format that mercator accepts.
 * 
 */
public class XMLCompactor extends MessageProcessorBase
{
    /**
     * Property indicating whether to sort attributes.
     */
    public static final String SORT_ATTRIBUTE_FLAG_PROP = "SORT_ATTRIBUTE_FLAG";

    /**
     * Property indicating whether to expand leaf nodes.
     * If set to true a node will be output as "<node1/>";
     * Otherwise it will be formatted like "<node1></node1>".
     *
     */
    public static final String EXPAND_LEAF_NODE_FLAG_PROP = "EXPAND_LEAF_NODE_FLAG";

    /**
     * Property indicating where the XML messages are located.
     */
    public static final String INPUT_MESSAGE_LOCATION_PREFIX_PROP = "INPUT_MESSAGE_LOCATION";


    private String[] locations = null;
    private boolean sortAttrbutes = true;
    private boolean expandLeafNode = true;
    
    
    /**
     * Constructor.
     */
    public XMLCompactor ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating XMLCompactor." );
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
        Debug.log( Debug.SYSTEM_CONFIG, "XMLCompactor: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );

        // Get sort attribute flag
        String strTemp = getRequiredPropertyValue( SORT_ATTRIBUTE_FLAG_PROP, errorBuffer );
        if (strTemp != null)
        {
            try {
                sortAttrbutes = getBoolean(strTemp);
            } catch ( FrameworkException e ) {
                errorBuffer.append ( "Property value for " + SORT_ATTRIBUTE_FLAG_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }
        
        // Get expand leaf node flag.
        strTemp = getRequiredPropertyValue( EXPAND_LEAF_NODE_FLAG_PROP, errorBuffer );
        if (strTemp != null)
        {
            try {
                expandLeafNode = getBoolean(strTemp);
            } catch ( FrameworkException e ) {
                errorBuffer.append ( "Property value for " + EXPAND_LEAF_NODE_FLAG_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }


        // Loop until all input location has been found
        LinkedList locationList = new LinkedList();
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String location = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_MESSAGE_LOCATION_PREFIX_PROP, Ix ) );

            // If we can't find a source name, we're done.
            if ( !StringUtils.hasValue( location ) )
                break;

            locationList.add( location );
        }
        // Save list to array for efficency.
        locations = new String[locationList.size()];
        locationList.toArray(locations);
        Debug.log( Debug.SYSTEM_CONFIG, "Number of messages to compact [" + locations.length + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "XMLCompactor: Initialization done." );
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

        Debug.log( Debug.MSG_STATUS, "XMLCompactor: processing ... " );
        XMLCompactorUtil compactor = new XMLCompactorUtil(sortAttrbutes, expandLeafNode);
        // Compacting the messages.
        for (int i=0; i<locations.length; i++)
        {
            String location = locations[i];
            Document doc = getDOM(location, mpContext, inputObject);
            String xmlString = compactor.compact(doc);
            set(location, mpContext, inputObject, xmlString);
        }
        
        // Always return input value to provide pass-through semantics.
        return formatNVPair(inputObject);
    }

    
}

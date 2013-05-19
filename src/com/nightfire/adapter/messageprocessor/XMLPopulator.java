/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/XMLPopulator.java#4 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;


/**
 * A generic message-processor for populating attribute values on
 * an existing XML document.
 */
public class XMLPopulator extends MessageProcessorBase
{
    /**
     * Property indicating the location of the source XML document, if an
     * existing document is being modified.
     */
    public static final String XML_DOCUMENT_LOCATION_PROP = "XML_DOCUMENT_LOCATION";

    /**
     * Property indicating the name to use for the root node in the generated XML document,
     * if a new document is being created.
     */
    public static final String XML_ROOT_NODE_NAME_PROP = "XML_ROOT_NODE_NAME";

    /**
     * Property prefix giving the name of the target node.
     */
    public static final String TARGET_NODE_NAME_PREFIX_PROP = "TARGET_NODE_NAME";

    /**
     * Property prefix giving the name of the target node attribute.
     */
    public static final String TARGET_NODE_ATTRIBUTE_NAME_PREFIX_PROP = "TARGET_NODE_ATTRIBUTE_NAME";

    /**
     * Property prefix giving location of source value.
     */
    public static final String VALUE_SOURCE_LOCATION_PREFIX_PROP = "VALUE_SOURCE_LOCATION";

    /**
     * Property prefix giving default value for node attribute.
     */
    public static final String DEFAULT_VALUE_PREFIX_PROP = "DEFAULT_VALUE";

    /**
     * Property prefix indicating whether column value is optional or required.
     */
    public static final String OPTIONAL_PREFIX_PROP = "OPTIONAL";


    /**
     * Constructor.
     */
    public XMLPopulator ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating xml-populator message-processor." );

        nodeInfo = new LinkedList( );
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
        Debug.log( Debug.SYSTEM_CONFIG, "XMLPopulator: Initializing..." );

        xmlDocumentLocation = getPropertyValue( XML_DOCUMENT_LOCATION_PROP );
        
        Debug.log( Debug.SYSTEM_CONFIG, "Location of XML document to populate [" + xmlDocumentLocation + "]." );

        xmlRootNodeName = getPropertyValue( XML_ROOT_NODE_NAME_PROP );

        // Loop until all node population configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String targetNode = getPropertyValue( PersistentProperty.getPropNameIteration( TARGET_NODE_NAME_PREFIX_PROP, Ix ) );

            String targetAttribute = getPropertyValue( PersistentProperty.getPropNameIteration( TARGET_NODE_ATTRIBUTE_NAME_PREFIX_PROP, Ix ) );

            String sourceLocation = getPropertyValue( PersistentProperty.getPropNameIteration( VALUE_SOURCE_LOCATION_PREFIX_PROP, Ix ) );

            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_VALUE_PREFIX_PROP, Ix ) );

            String optional = getPropertyValue( PersistentProperty.getPropNameIteration( OPTIONAL_PREFIX_PROP, Ix ) );

            // If none of the configuration items has a value, we're done.
            if ( !StringUtils.hasValue(targetNode) && !StringUtils.hasValue(targetAttribute)
                 && !StringUtils.hasValue(sourceLocation) && !StringUtils.hasValue(defaultValue)
                 && !StringUtils.hasValue(optional) )
                break;

            try
            {
                // Create a new node data object and add it to the list.
                NodeData nd = new NodeData( targetNode, targetAttribute, sourceLocation, defaultValue, optional );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, nd.describe() );

                nodeInfo.add( nd );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create node data description:\n"
                                               + e.toString() );
            }
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Number of nodes to populate [" + nodeInfo.size() + "]." );

        Debug.log( Debug.SYSTEM_CONFIG, "XMLPopulator: Initialization done." );
    }


    /**
     * Populate the XML document with new node values.
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

        Debug.log( Debug.MSG_STATUS, "XMLPopulator: processing ... " );

        XMLMessageGenerator gen = null;

        try
        {
            if ( StringUtils.hasValue( xmlRootNodeName ) )
                gen = new XMLMessageGenerator( xmlRootNodeName );
            else
                // Get the DOM Document input, and use it to create an XML message generator to be used in population.
                gen = new XMLMessageGenerator( getDOM( xmlDocumentLocation, mpContext, inputObject ) );

            Iterator iter = nodeInfo.iterator( );

            // While nodes are available to populate ...
            while ( iter.hasNext() )
            {
                NodeData nd = (NodeData)iter.next( );

                String value = null;

                // If a source-location was given and it exists, extract the value.
                if ( StringUtils.hasValue(nd.sourceLocation) && exists( nd.sourceLocation, mpContext, inputObject ) )
                    value = getString( nd.sourceLocation, mpContext, inputObject );

                // If no value was available, use the default
                if ( value == null )
                {
                    value = nd.defaultValue;

                }

                // If no value can be obtained, but it is required, then throw an exception
                if ( value == null )
                {
                    // If the value is required ...
                    if ( nd.optional == false )
                    {
                        // Signal error to caller.
                        throw new MessageException( "ERROR: Missing required value for [" + nd.describe() + "]." );
                    }
                    else
                    //Skip setting value on node
                    {
                        Debug.log( Debug.MSG_DATA, "Skipping optional node [" + nd.describe() + "] since value is not available." );
                    }
                }
                else
                //Set the value
                {
                    Node n = gen.getDocument().getDocumentElement( );

                    // If the target node has been named, get it.  If not, just use the root document node.
                    if ( StringUtils.hasValue( nd.targetNode ) )
                        n = gen.getNode( n, nd.targetNode, true );

                    // Set the non-null value on the appropriate node attribute.
                    if ( StringUtils.hasValue( nd.targetAttribute ) )
                        XMLMessageBase.setNodeAttributeValue( n, nd.targetAttribute, value );
                    else
                        XMLMessageBase.setNodeValue( n, value );
                }
            }//while
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: XMLPopulator: Attempt to populate XML document failed with error: "
                            + e.getMessage();

            Debug.log( Debug.ALL_ERRORS, errMsg );

            // Re-throw the exception to the driver.
            throw new ProcessingException( errMsg );
        }

        set( xmlDocumentLocation, mpContext, inputObject, gen.getDocument() );

        return( formatNVPair( inputObject ) );
    }

    
    private String xmlDocumentLocation;
    private String xmlRootNodeName;
    private LinkedList nodeInfo;


    /**
     * Class NodeData is used to encapsulate a description of a node-population
     * description.
     */
    private static class NodeData
    {
        public final String targetNode;
        public final String targetAttribute;
        public final String sourceLocation;
        public final String defaultValue;
        public final boolean optional;

        public NodeData ( String targetNode, String targetAttribute,
                          String sourceLocation, String defaultValue, String optional ) throws FrameworkException
        {
            this.targetNode      = targetNode;
            this.targetAttribute = targetAttribute;
            this.sourceLocation  = sourceLocation;
            this.defaultValue    = defaultValue;
            if ( StringUtils.hasValue ( optional ) )
                this.optional    = StringUtils.getBoolean( optional );
            else
                this.optional = false;
        }


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Node-population description: " );

            if ( StringUtils.hasValue( targetNode ) )
            {
                sb.append( "target node name [" );
                sb.append( targetNode );
            }

            if ( StringUtils.hasValue( targetAttribute ) )
            {
                sb.append( "], target attribute name [" );
                sb.append( targetAttribute );
            }

            if ( StringUtils.hasValue( sourceLocation ) )
            {
                sb.append( "], source location [" );
                sb.append( sourceLocation );
            }

            if ( StringUtils.hasValue( defaultValue ) )
            {
                sb.append( "], default value [" );
                sb.append( defaultValue );
            }

            sb.append( "], optional [" );
            sb.append( optional );
            
            sb.append( "]." );

            return( sb.toString() );
        }
    }
}

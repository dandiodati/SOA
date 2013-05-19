/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/XMLCopier.java#4 $
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
 * A generic message-processor for copying complete XML documents
 * into another XML document at a configured location.
 */
public class XMLCopier extends MessageProcessorBase
{
    /**
     * Property indicating the location of the source XML document to
     * copy into the target XML document (iterated).
     */
    public static final String XML_SOURCE_LOCATION_PROP = "XML_SOURCE_LOCATION";

    /**
     * Property indicating the location of the target XML document to populate using
     * the source XML document (iterated).
     */
    public static final String XML_TARGET_LOCATION_PROP = "XML_TARGET_LOCATION";

    /**
     * Property giving the location of an item in the context or input that gives the actual location 
     * at which the source value should be inserted (iterated).
     */
    public static final String INSERT_LOCATION_PROP = "INSERT_LOCATION";

    /**
     * Property indicating whether a blind copying should be done or whether a node name comparison
     * should be done while copying the xml document to a particular location (iterated).
     */
    public static final String USE_TARGET_NAME_COMPARISON_PROP = "USE_TARGET_NAME_COMPARISON";

    /**
     * Property giving the default location at which the source value should be inserted if the location
     * can't be found in the context or input (iterated).
     */
    public static final String DEFAULT_INSERT_LOCATION_PROP = "DEFAULT_INSERT_LOCATION";

    /**
     * Property indicating whether the source XML document is optional or required (iterated).
     */
    public static final String OPTIONAL_PROP = "OPTIONAL";


    /**
     * Constructor.
     */
    public XMLCopier ( )
    {
        loggingClassName = StringUtils.getClassName( this );

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, loggingClassName + ": Creating ..." );

        copyInfo = new LinkedList( );
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
        if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
            Debug.log( Debug.SYSTEM_CONFIG, loggingClassName + ": Initializing..." );

        // Loop until all configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String xmlSourceLoc = getPropertyValue( PersistentProperty.getPropNameIteration( XML_SOURCE_LOCATION_PROP, Ix ) );

            String xmlTargetLoc = getPropertyValue( PersistentProperty.getPropNameIteration( XML_TARGET_LOCATION_PROP, Ix ) );

            String insertLoc = getPropertyValue( PersistentProperty.getPropNameIteration( INSERT_LOCATION_PROP, Ix ) );

            String defaultInsertLoc = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_INSERT_LOCATION_PROP, Ix ) );

            String optional = getPropertyValue( PersistentProperty.getPropNameIteration( OPTIONAL_PROP, Ix ) );

            String useTargetNameComparison = getPropertyValue( PersistentProperty.getPropNameIteration( USE_TARGET_NAME_COMPARISON_PROP, Ix ) );

            // If none of the configuration items has a value, we're done.
            if ( !StringUtils.hasValue(xmlSourceLoc) || !StringUtils.hasValue(xmlTargetLoc) )
                break;

            try
            {
                // Create a new node data object and add it to the list.
                CopyInfo ci = new CopyInfo( xmlSourceLoc, xmlTargetLoc, insertLoc, defaultInsertLoc, optional, useTargetNameComparison );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, "Adding the following copy configuration item:\n" + ci.describe() );

                copyInfo.add( ci );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );

                throw new ProcessingException( "ERROR: Could not create copy info description:\n"
                                               + e.toString() );
            }
        }

        if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
            Debug.log( Debug.SYSTEM_CONFIG, "Number of copy items [" + copyInfo.size() + "]." );

        if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
            Debug.log( Debug.SYSTEM_CONFIG, loggingClassName + ": Initialization done." );
    }


    /**
     * Populate the XML document with new node values.
     *
     * @param  context The context
     * @param  input  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
                        throws MessageException, ProcessingException 
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": processing ... " );

        if ( input == null )
            return null;

        try
        {
            Iterator iter = copyInfo.iterator( );
            
            // While copy configuration items are available ...
            while ( iter.hasNext() )
            {
                CopyInfo ci = (CopyInfo)iter.next( );
                
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Processing the following copy configuration item:\n" + ci.describe() );
                
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Getting source document [" + ci.xmlSourceLoc + "] ... " );
                
                Document sourceDoc = null;
                
                if ( StringUtils.hasValue( ci.xmlSourceLoc ) && exists( ci.xmlSourceLoc, context, input, false ) )
                    sourceDoc = getDOM( ci.xmlSourceLoc, context, input );
                
                if ( sourceDoc == null )
                {
                    if ( ci.optional )
                    {
                        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                            Debug.log( Debug.MSG_STATUS, "Skipping missing source xml [" 
                                       + ci.xmlSourceLoc + "], since it's optional." );
                        
                        continue;
                    }
                    else
                    {
                        throw new MessageException( "ERROR: Missing required source xml at location [" + ci.xmlSourceLoc + "]." );
                    }
                }
                
                Debug.log( Debug.MSG_STATUS, "Getting insertion location ..." );
                
                String insertLoc = ci.defaultInsertLoc;
                
                if ( StringUtils.hasValue( ci.insertLoc ) && exists( ci.insertLoc, context, input ) )
                    insertLoc = getString( ci.insertLoc, context, input );
                
                if ( !StringUtils.hasValue( insertLoc ) )
                {
                    throw new MessageException( "ERROR: Missing required document insertion location value." );
                }

                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Insertion location is [" + insertLoc + "]. " );

                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Getting target document [" + ci.xmlTargetLoc + "] ... " );
                
                XMLMessageGenerator target = new XMLMessageGenerator( getDOM( ci.xmlTargetLoc, context, input ) );

                // At this point we have all of the configured items, so proceed with the copy operation.

                // Get the root of the node tree to copy to the target.
                Node source = sourceDoc.getDocumentElement( );

                // If the named target node already exists ...
                if ( target.nodeExists( insertLoc ) )
                {
                    // Get the target node.
                    Node targetNode = target.getNode( insertLoc );

                    if ( (!ci.useTargetNameComparison) ||
                    ( (ci.useTargetNameComparison) && (source.getNodeName().equals( targetNode.getNodeName() ) ) ) )
                    //Either we are told to ignore the fact that the name of the target node
                    //matched the name of the source node, or we are told to compare them and we
                    //perform the following only if they match.
                    {
                        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                            Debug.log( Debug.MSG_STATUS, "Replacing existing target node at [" + insertLoc + "] named [" +
                            targetNode.getNodeName() + "] with new node named [" + source.getNodeName() + "]." );


                        // Replace the current target node with a copy of the source node
                        // by getting the target node's parent and doing a replace-child operation.
                        Node copy = XMLMessageBase.copyNode( target.getDocument(), source );

                        Node parent = targetNode.getParentNode( );

                        parent.replaceChild( copy, targetNode );
                    }
                    else
                    {
                        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                            Debug.log( Debug.MSG_STATUS, "Inserting copy of source node under target node named ["
                                       + insertLoc + "] ... " );

                        // Node names don't match, so append source node under
                        // the given insert location.
                        target.setValue( insertLoc, null, source  );
                    }
                }
                else
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                        Debug.log( Debug.MSG_STATUS, "Inserting copy of source node under target node named [" 
                                   + insertLoc + "] ... " );

                    // Node indicated by insert location doesn't exist, 
                    // so place source node under it (after creation).
                    target.setValue( insertLoc, null, source  );
                }

                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Altered document:\n" + target.generate() );

                // Make sure that the altered document gets placed back where it originated from.
                set( ci.xmlTargetLoc, context, input, target.getDocument() );
            }

            return( formatNVPair( input ) );
        }
        catch ( MessageException me )
        {
            throw me;
        }
        catch ( ProcessingException pe )
        {
            throw pe;
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: XMLCopier: Attempt to populate XML document failed with error: "
                + e.getMessage();
            
            Debug.error( errMsg );
            
            // Re-throw the exception to the driver.
            throw new MessageException( errMsg );
        }
    }


    /**
     * Class CopyInfo is used to encapsulate a description of a copy operation.
     */
    private static class CopyInfo
    {
        public final String xmlSourceLoc;
        public final String xmlTargetLoc;
        public final String insertLoc;
        public final String defaultInsertLoc;
        public final boolean optional;
        public final boolean useTargetNameComparison;

        public CopyInfo ( String xmlSourceLoc, String xmlTargetLoc, String insertLoc,
                          String defaultInsertLoc, String optional, String useTargetNameComparison ) throws FrameworkException
        {
            this.xmlSourceLoc = xmlSourceLoc;
            this.xmlTargetLoc = xmlTargetLoc;
            this.insertLoc = insertLoc;
            this.defaultInsertLoc = defaultInsertLoc;

            if ( StringUtils.hasValue ( optional ) )
                this.optional = StringUtils.getBoolean( optional );
            else
                this.optional = false;

            if ( StringUtils.hasValue ( useTargetNameComparison ) )
                this.useTargetNameComparison = StringUtils.getBoolean( useTargetNameComparison );
            else
                this.useTargetNameComparison = true;

        }


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Copy-info description: " );

                sb.append( "xml-source-loc [" );
                sb.append( xmlSourceLoc );
                sb.append( "], xml-target-loc [" );
                sb.append( xmlTargetLoc );

            if ( StringUtils.hasValue( insertLoc ) )
            {
                sb.append( "], insert-loc [" );
                sb.append( insertLoc );
            }

            if ( StringUtils.hasValue( defaultInsertLoc ) )
            {
                sb.append( "], default-insert-loc [" );
                sb.append( defaultInsertLoc );
            }

            sb.append( "], optional [" );
            sb.append( optional );

            sb.append( "], use-target-name-comparison [" );
            sb.append( useTargetNameComparison );

            sb.append( "]." );

            return( sb.toString() );
        }
    }


    private LinkedList copyInfo;

    // Abbreviated class name for use in logging.
    private String loggingClassName;
}

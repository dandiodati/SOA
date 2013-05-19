package com.nightfire.framework.test;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.iterator.*;
import com.nightfire.framework.message.iterator.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.mapper.*;


/*
 * Class for testing message parsing, generating and mapping.
 */
class XMLNodeManifest
{
    public static void main ( String[] args )
    {
        if ( args.length < 1 )
        {
            System.err.println( "\n\nUSAGE: XMLNodeManifest <xml-file-name> [<all-nodes>]\n\n" );
            System.exit( -1 );
        }

        // Debug.showLevels();
        // Debug.enableAll();

        try
        {
            System.out.println( "Reading XML file [" + args[0] + "]." );
            
            String xmlText = FileUtils.readFile( args[0] );
            
            System.out.println( "XML input [" + xmlText + "]." );
            
            XMLMessageParser p = new XMLMessageParser( );
            
            System.out.println( "Parsing ..." );
            
            p.parse( xmlText );
            
            System.out.println( "\n\n" );
            
            if ( args.length == 1 )
            {
                XMLValueMessageIterator iter = new XMLValueMessageIterator( );
                
                iter.set( p.getDocument() );
                
                while ( iter.hasNext() )
                {
                    NVPair nvp = iter.nextNVPair( );
                    
                    System.out.println( nvp.name );
                }
            }
            else
                listAllNodes( p.getDocument() );
        }
        catch ( Exception e )
        {
            System.err.println( "\n\n" + e );
        }
    }


    /**
     * Lists all non-whitespace nodes, their types, and any values.
     *
     * @param  d  Document object.
     */
    private static void listAllNodes ( Document d )
    {
        Node rootNode = d.getDocumentElement( );

        Node iterNode = rootNode;

        do
        {
            iterNode = nextNodeDepthFirst( iterNode, rootNode );

            if ( iterNode == null )
                break;

            StringBuffer sb = new StringBuffer( );

            // Ignore any all-whitespace text nodes.
            if ( iterNode.getNodeType() == Node.TEXT_NODE )
            {
                String value = iterNode.getNodeValue( );

                if ( value.trim().length() == 0 )
                    continue;
            }

            sb.append( XMLMessageBase.constructRelativeName( rootNode, iterNode ) );

            sb.append( " [type=" + iterNode.getNodeType() + "] " );
            
            if ( iterNode.getNodeType() == Node.TEXT_NODE )
                sb.append( "[value=" + iterNode.getNodeValue() + "]" );
                
            System.out.println( sb.toString() );
        }
        while ( true );
    }
    
    
    /**
     * Performs a depth-first search for next available node.
     *
     * @param  node  Node to begin search from.
     * @param  rootIterationNode  Root node that iterator was given.
     *
     * @return  Node, or null if none found.
     */
    private static Node nextNodeDepthFirst ( Node node, Node rootIterationNode )
    {
        if ( node == null )
            return null;

        Node newNode = null;
        
        // Attempt to go deep for children before we go wide for siblings.
        newNode = node.getFirstChild( );
        
        if ( newNode != null )
            return newNode;

        Debug.log( Debug.XML_DATA, "\tCurrent iterator node has no child nodes ..." );
        
        // If we're currently at root iteration node, we've reached the stopping point.
        if ( node == rootIterationNode )
            return null;

        // Didn't find any children, so try to find neighbor (sibling).
        newNode = node.getNextSibling( );
        
        if ( newNode != null )
            return newNode;
        
        Debug.log( Debug.XML_DATA, "\tCurrent iterator node has no sibling nodes ..." );

        // Didn't find any children or siblings, so walk back to parent and get parent's sibling.
        do
        {
            // Get the parent node and make it the current one.
            node = node.getParentNode( );
            
            // No parent indicates we're at root, which is stopping criteria.
            if ( node == null )
                break;
            
            // If we're currently at root iteration node, we've reached the stopping point.
            if ( node == rootIterationNode )
                return null;

            newNode = node.getNextSibling( );
        }
        while ( newNode == null );
        
        return newNode;
    }
}    
    

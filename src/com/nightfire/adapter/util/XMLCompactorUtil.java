/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/R4.4/com/nightfire/adapter/util/XMLCompactorUtil.java#1 $
 *
 */
package com.nightfire.adapter.util;

import org.w3c.dom.*;
import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.iterator.xml.*;

/**
 * This utility class convert the given XML message (DOM or String) to a compact string.
 * The string output does not contain any line breaks or unnecessary spaces. It can also sort
 * the node attributes or expend the leaf node if specified.
 * (Expand node means: "<A/>" --> "<A></A>".
 */
public class XMLCompactorUtil
{

    private static boolean sortAttributeValue = true;
    private static boolean expandLeafNode = true;

    private static final String OPEN_BRACKET = "<";
    private static final String CLOSE_BRACKET = ">";
    private static final String SLASH = "/";
    private static final String DOUBLE_QUOTES = "\"";
    private static final String SINGLE_WHITE_SPACE = " ";
    private static final String EQUALS_SIGN = "=";
    private static final String TEXT_NODE = "#text";

    /**
     * Convert the given XML String to a compact XML String
     */
    public XMLCompactorUtil ( )
    {
        Debug.log( this, Debug.XML_PARSE, "XMLCompactorUtil: Creating transformer to convert "+
                   "XML parser/stream to compact XML stream" );
    }

    /**
     * Constructor.
     *
     * @param vsortAttributeValue Flag that specfies whether attribute will be sorted
     * @param vexpandLeafNode    Flag that specfies whether to expand leaf node.
     *
     */
    public XMLCompactorUtil ( boolean vsortAttributeValue, boolean vexpandLeafNode )
    {
        this.sortAttributeValue = vsortAttributeValue;
        this.expandLeafNode = vexpandLeafNode;
        
        Debug.log( this, Debug.XML_PARSE, "XMLCompactorUtil: Creating transformer to convert "+
                   "XML parser/stream to compact XML stream" );
        Debug.log(Debug.XML_PARSE, "sort Attribute Values [" + sortAttributeValue +"]");
        Debug.log(Debug.XML_PARSE, "expand Leaf Nodes [" + expandLeafNode +"]");
    }

    /**
     * Convert the given XML message (DOM) to its equivalent string format
     * without line breaks and unnecessary spaces.
     *
     * @param  doc  XML DOM to convert.
     *
     * @return  The equivalent XML string in compacted form.
     *
     * @exception  MessageException  Thrown if object can't be compacted.
     */
    public String compact ( Document doc ) throws MessageException
    {
        if (doc == null)
        {
            throw new MessageException("ERROR: Cannot compact null DOM.");
        }
        
        StringBuffer buffer = new StringBuffer();
        Node rootNode = doc.getDocumentElement();

        processNode(rootNode, buffer);

        return buffer.toString();
        
    };//compact

    /**
     * Convert the given XML message (String) to its equivalent string format
     * without line breaks and unnecessary spaces.
     *
     * @param  xmlString  XML String to convert.
     *
     * @return  The equivalent XML string in compacted form.
     *
     * @exception  MessageException  Thrown if object can't be compacted.
     */
    public String compact ( String xmlString ) throws MessageException
    {
        if (xmlString == null)
        {
            throw new MessageException("ERROR: Cannot compact null string.");
        }
        
        XMLMessageParser parser = new XMLMessageParser(xmlString);
        
        return compact(parser.getDocument());
        
    };//compact

    /**
     * Format the node into string and iterate through its children,
     * and appended the output to the given buffer.
     *
     * @param  curNode   The node to be formatted.
     * @param  buffer    The buffer to be appended.
     *
     * @exception  MessageException  Thrown if object can't be processed.
     *
     */
    private void processNode (Node curNode, StringBuffer buffer) throws MessageException
    {
        String nodeName = curNode.getNodeName();
        if (nodeName != null)
            nodeName = nodeName.trim();

        buffer.append ( OPEN_BRACKET + nodeName);

        // Add attributes after node name.
        addAttributes(curNode, buffer);
      
        // If not expand leaf node, close the node with "/>".
        if ((XMLMessageBase.getXMLNodeChildCount(curNode) != 0))
        {
            // Node has children
            buffer.append ( CLOSE_BRACKET );
          
            // Recurse over child nodes
            Node[] children = XMLMessageBase.getChildNodes(curNode);

            for (int i=0; i<children.length; i++)
            {
                processNode(children[i], buffer);
            }
          
            buffer.append ( OPEN_BRACKET + SLASH + nodeName + CLOSE_BRACKET );
        }
        else if (expandLeafNode)
        {
            // Leaf node, expand foramt.
            buffer.append ( CLOSE_BRACKET );
            buffer.append ( OPEN_BRACKET + SLASH + nodeName + CLOSE_BRACKET );
        }
        else
        {
            // If not expand leaf node, close the node with "/>".
            buffer.append( SLASH );
            buffer.append ( CLOSE_BRACKET );
        }
      
    };//processNode

    /**
     * Method to add the list of attributes for the node to the buffer
     * that contains the XML string.
     *
     */
    private void addAttributes ( Node curNode, StringBuffer buffer ) throws MessageException
    {
        //The attribute list is built of the form
        //{ attname1="attvalue1" attname2="attvalue2"}
        //Fetch attributes
        NamedNodeMap attrs = curNode.getAttributes ();
        String nodeName = curNode.getNodeName();

        if (attrs.getLength() <= 0)
        {
            // no attributes.
            return;
        }

        else if (sortAttributeValue)
        {
            addAttributesSort(nodeName, attrs, buffer);
        }
        else
        {
            addAttributesNoSort(nodeName, attrs, buffer);
        }
        
    };//addAttributes

    /**
     * Add attributes in original order.
     *
     */
    private void addAttributesNoSort(String nodeName, NamedNodeMap attrs, StringBuffer buffer)
        throws MessageException
    {
        for (int i=0; i<attrs.getLength(); i++)
        {
            Node attrNode = attrs.item (i);
            String attrNodeName = attrNode.getNodeName();
            String attrNodeValue = attrNode.getNodeValue();
            if ( (attrNodeName == null) || (attrNodeValue == null) )
            {
                Debug.log (Debug.ALL_ERRORS, "ERROR: XMLCompactorUtil: Node "+ nodeName +
                           " has invalid attributes");
                throw new MessageException ("ERROR: XMLCompactorUtil: Node "+ nodeName +
                                            " has invalid attributes");
            }
            attrNodeName = attrNodeName.trim();

            buffer.append ( SINGLE_WHITE_SPACE );
            buffer.append ( attrNodeName );
            buffer.append ( EQUALS_SIGN + DOUBLE_QUOTES );
            buffer.append ( attrNodeValue);
            buffer.append ( DOUBLE_QUOTES );
        };//if
    }
    
    /**
     * Add attribute in sorted order.
     *
     */
    private void addAttributesSort(String nodeName, NamedNodeMap attrs, StringBuffer buffer)
        throws MessageException
    {
        //Create Hashtable of attributes and sort names
        Hashtable hashAttrs = new Hashtable ();
        SortedSet sortedSet = new TreeSet ();
        for (int i=0; i<attrs.getLength(); i++)
        {
            Node attrNode = attrs.item (i);
            String attrNodeName = attrNode.getNodeName();
            String attrNodeValue = attrNode.getNodeValue();
            if ( (attrNodeName == null) || (attrNodeValue == null) )
            {
                Debug.log (Debug.ALL_ERRORS, "ERROR: XMLCompactorUtil: Node "+ nodeName +
                           " has invalid attributes");
                throw new MessageException ("ERROR: XMLCompactorUtil: Node "+ nodeName +
                                            " has invalid attributes");
            }
            attrNodeName = attrNodeName.trim();

            //Check return values too
            String hashVal = (String) hashAttrs.put (attrNodeName, attrNodeValue);
            if (hashVal != null)
            {
                Debug.log (Debug.ALL_ERRORS, "ERROR: XMLCompactorUtil: Node "+ nodeName +
                           " has duplicate attributes");
                throw new MessageException ("ERROR: XMLCompactorUtil: Node "+ nodeName +
                                            " has duplicate attributes");
            }
            sortedSet.add (attrNodeName);
        }

        //Sorted set is ready now. Just iterate through it.
        Iterator iterator = sortedSet.iterator ();

        if ( (attrs != null) && (iterator != null) )
        {
            while ( iterator.hasNext () )
            {
                String attrName = (String) iterator.next ();
                buffer.append ( SINGLE_WHITE_SPACE );
                buffer.append ( attrName );
                buffer.append ( EQUALS_SIGN + DOUBLE_QUOTES );
                buffer.append ( (String) hashAttrs.get (attrName));
                buffer.append ( DOUBLE_QUOTES );
            };//while
        };//if
    }
    
    /**
     * Main method for testing purposes, invoked as
     * java -classpath %CLASSPATH% com.nightfire.spi.common.util.XMLCompactorUtil
     * test/test.xml
     */
    public static void main (String args[])
    {
        Debug.enableAll();
        Debug.showLevels();

        if (args.length != 1)
        {
            Debug.log (Debug.ALL_ERRORS, "XMLCompactorUtil: XML file name needed as input argument");
        }

        String MESSAGEFILE = args [0];
        // Read in message file
        FileCache messageFile = new FileCache();
        String inputMessage = null;
        String retValue = null;

        try
        {
            inputMessage = messageFile.get(MESSAGEFILE);
        }
        catch (FrameworkException e)
        {
            Debug.log(null, Debug.MAPPING_ERROR, "XMLCompactorUtil.main: " +
                      "FileCache failure: " + e.getMessage());
        }

        Debug.log (Debug.MSG_STATUS, "XMLCompactorUtil.main: Original XML String is : "
                   + inputMessage);
        XMLCompactorUtil xmlStringCompactor = new XMLCompactorUtil (true, false);

        try
        {
            retValue = xmlStringCompactor.compact ( inputMessage );
        }
        catch (MessageException me)
        {
            Debug.logStackTrace(me);
            Debug.log (Debug.ALL_ERRORS, "ERROR: XMLCompactorUtil.main: " + me.getMessage ( ) );
        }

        Debug.log (Debug.MSG_STATUS, "XMLCompactorUtil.main: Transformed XML String is : " + retValue);

    };//main

};//XMLCompactorUtil

/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
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

public class XMLStringCompactor
{

    private Node iterNode = null;
    private Node rootNode = null;
    private StringBuffer xmlBuf = null;
    private Stack stack = null;
    private static boolean compactAttributeValue = true;

    private static final int PARENT = 0;
    private static final int SIBLING = 1;
    private static final int SELF = 2;
    private static final int CHILD = 3;
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
    public XMLStringCompactor ( )
    {
        Debug.log( this, Debug.XML_PARSE, "XMLStringCompactor: Creating transformer to convert "+
        "XML parser/stream to compact XML stream" );
    }

    // this constructor allows you to set a flag which determines whether 
    // attribute values are compacted. If this constructor is not used, 
    // attribute values WILL be compacted.
    // if this constructor is given TRUE, attribute values will be compacted.
    // if this constructor is given FALSE, attribute values will not be compacted.
    public XMLStringCompactor ( boolean b )
    {
        compactAttributeValue = b;
        Debug.log( this, Debug.XML_PARSE, "XMLStringCompactor: Creating transformer to convert "+
        "XML parser/stream to compact XML stream" );
        Debug.log(Debug.XML_PARSE, "Compact Attribute Values [" + compactAttributeValue +"]");
    }


    /**
     * Convert the given XML String to some its equivalent DOM Document.
     *
     * @param  value  XML String value to transform.
     *
     * @return  The equivalent XML string in compacted form, with attributes
     * of all elements in the same order as defined in the corresponding DTD.
     *
     * @exception  ProcessingException  Thrown if object can't be transformed.
     */
    public String compact ( Object inputXMLObject ) throws ProcessingException, MessageException
    {
      //Initialize variables first
      iterNode = null;
      rootNode = null;
      xmlBuf = new StringBuffer ();
      stack = new Stack ();
      String retValue = null;

      if ( inputXMLObject instanceof String )
      {
        Debug.log( Debug.XML_PARSE, "XMLStringCompactor: Parsing XML stream to convert it compact XML stream..." );
        //Debug.log (Debug.MSG_STATUS, "XMLStringCompactor: Original XML string : " + inputXMLObject );
        // ------- Create a parser to parse the XML document. -------
        XMLMessageParser parser = new XMLMessageParser( );
        XMLMessageBase.enableValidation(false);
        //The DOCTYPE tag also has to be stripped out. Currently am using the XMLMangler.
        //This might be included as a method here later or better still can be included as
        //part of the XMLMessageParser
        String inputMessage = XmlMangler.deleteDOCTYPE ( (String) inputXMLObject );
        try
        {
          // ------- Give the parser the XML document to parse. -------
          parser.parse( inputMessage );
        }
        catch ( MessageParserException mpe )
        {
          throw new ProcessingException ( mpe.toString() );
        }

       /**XMLMessageGenerator xmlMessageGenerator = (XMLMessageGenerator) parser.getGenerator ();
       String outputMessage = null;
       try
       {
       outputMessage = xmlMessageGenerator.generate ();
       }
       catch (MessageGeneratorException mge)
       {
        throw new ProcessingException (mge.getMessage());
       }
       outputMessage = deleteSpaces (outputMessage);
       Debug.log (Debug.MSG_STATUS, "Compacted XML string : " + outputMessage );
       return outputMessage;
       **/

        //-------- Get the compacted XML document --------------
        retValue = getCompactXML (parser) ;
      }//if input is String
      else
      if ( inputXMLObject instanceof XMLMessageParser )
      {
        Debug.log (Debug.MSG_STATUS, "XMLStringCompactor: XMLMessage Parser received is : " + inputXMLObject.toString() );
        retValue = getCompactXML ( ( XMLMessageParser ) inputXMLObject );
      }
      else
        throw new ProcessingException ("ERROR: XMLStringCompactor: Invalid input format to process method");
      Debug.log (Debug.MSG_STATUS, "XMLStringCompactor: Compacted XML string : " + retValue );
      return ( retValue );
    };//transform

    //Following method not in use currently. Can be used if no attribute ordering
    //and no transformation of <nodename/> to <nodename></nodename> is required.
    //This method takes the xml string generated using the XMLMessageParser and
    //XMLMessageGenerator classes and strips out white spaces from the string.
    /**
     * Delete extra spaces within the newly generated XML string
     * @param xmlString Input xml string from which extraneous spaces have to be
     * deleted
     * @return String XML String with no extra spaces
     */
     private String deleteSpaces ( String xmlString )
     {
      Debug.log (Debug.MSG_STATUS, "XMLStringCompactor: Before compaction is :" + xmlString);
      xmlString = StringUtils.replaceSubstrings ( xmlString, " />", "/>" );
      xmlString = StringUtils.replaceSubstrings ( xmlString, "\" />", "\"/>");
      StringBuffer strBuf = new StringBuffer ();
      int open_brack = -1;
      int close_brack = -1;
      int start_indx = 0;
      while (true)
      {
        open_brack = xmlString.indexOf ('<', start_indx);
        close_brack = xmlString.indexOf ('>', open_brack);
        if ( (open_brack == -1) || (close_brack == -1) || (start_indx > open_brack ))
          break;

        //Append all that is before the "<"
        strBuf.append ((xmlString.substring (start_indx, open_brack)).trim());

        strBuf.append (xmlString.substring (open_brack, close_brack + 1) );

        //Set start index to be one after the ">"
        start_indx = close_brack + 1;

      };//while

      xmlString = strBuf.toString().trim();
      Debug.log (Debug.MSG_STATUS, "XMLStringCompactor: After compaction string is :" + xmlString);
      return xmlString;
     };//deleteSpaces


    /**
     * Create compact XML string from given parser
     *
     * @param parser XML parser
     *
     * @return The equivalent XML string in compacted form, with attributes
     * of all elements in the same order as defined in the corresponding DTD.
     *
     * @exception  ProcessingException  Thrown if object can't be transformed.
     */
    private String getCompactXML ( XMLMessageParser parser ) throws ProcessingException,
    MessageException
    {
      rootNode = ( Node ) parser.getDocument().getDocumentElement() ;
      iterNode = ( Node ) parser.getDocument().getDocumentElement() ;

      //Put root node into stack first for root node
      processNode (rootNode, SELF);

      Node tempNode = rootNode;

      //Traverse through the tree
      do
      {
        tempNode = advance ();
      }
      while ( tempNode != null );

      return xmlBuf.toString ( );

    };//getCompactXML

    /**
     * Create compact XML string for given Node
     *
     * @param nvpair NVPair consisting of name and value of node
     *
     * @return The equivalent XML string for the Node in compacted form,
     * with attributes
     * of all elements in the same order as defined in the corresponding DTD.
     *
     * @exception  ProcessingException  Thrown if object can't be transformed.
     */
    /**private String generateXMLString ( NVPair nvpair ) throws ProcessingException
    {
      Debug.log (Debug.MSG_STATUS, "NVPair is : name=" + nvpair.name + " value=" + nvpair.value);
      Node node = ( Node ) nvpair.value;
      Debug.log (Debug.MSG_STATUS, "Node statistics are: name=" + node.getNodeName() +
      " type=" + node.getNodeType() + " value=" + node.getNodeValue() );
      return null;
    };**/
    //generateXMLString

    /**
     * Advance the iterator, returning the next available node.
     *
     * @return  NVPair with XML node's name and value, or null
     *          if none available.
     */
    private Node advance ( ) throws MessageException, ProcessingException
    {
        // Skip any nodes due to whitespace in XML document.
        do
        {
            iterNode = nextNodeDepthFirst( iterNode, rootNode );
        }
        //while ( (iterNode != null) && (iterNode.getNodeType() == Node.TEXT_NODE) );
        while (iterNode != null);

        if ( iterNode == null )
            return null;

        return iterNode;
    }


    /**
     * Performs a depth-first search for next available node.
     *
     * @param  node  Node to begin search from.
     * @param  rootIterationNode  Root node that iterator was given.
     *
     * @return  Node, or null if none found.
     */
    private Node nextNodeDepthFirst ( Node node, Node rootIterationNode )
                                                  throws MessageException,
                                                         ProcessingException
    {
        Debug.log (Debug.MSG_STATUS, "XMLStringCompactor.nextNodeDepthFirst : Parent node is " +
        iterNode.getNodeName () );

        if ( node == null )
            return null;

        Node newNode = null;

        // Attempt to go deep for children before we go wide for siblings.
        newNode = node.getFirstChild( );

        if ( newNode != null )
        {
            processNode (newNode, CHILD);
            return newNode;
        }

        // Debug.log( Debug.XML_DATA, "\tCurrent iterator node has no child nodes ..." );

        // If we're currently at root iteration node, we've reached the stopping point.
        if ( node == rootIterationNode )
        {
            //Pop values from stack for closing tag for root node
            try
            {
              String popString = (String) stack.pop ();
              String addTag = popString.trim();
              if ( !addTag.equalsIgnoreCase (TEXT_NODE) )
                xmlBuf.append ( OPEN_BRACKET + SLASH + addTag + CLOSE_BRACKET );
            }
            catch (EmptyStackException ese)
            {
              Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Stack is empty");
              throw new ProcessingException ("ERROR: XMLStringCompactor: Stack is empty");
            }
            return null;
        }

        // Didn't find any children, so try to find neighbor (sibling).
        newNode = node.getNextSibling( );

        if ( newNode != null )
        {
            processNode (newNode, SIBLING);
            return newNode;
        }

        // Debug.log( Debug.XML_DATA, "\tCurrent iterator node has no sibling nodes ..." );

        // Didn't find any children or siblings, so walk back to parent and get parent's sibling.
        do
        {
            // Get the parent node and make it the current one.
            node = node.getParentNode( );

            // No parent indicates we're at root, which is stopping criteria.
            if ( node == null )
            {
                try
                {
                  String popString = (String) stack.pop ();
                  String addTag = popString.trim();
                  if ( !addTag.equalsIgnoreCase (TEXT_NODE) )
                    xmlBuf.append ( OPEN_BRACKET + SLASH + addTag + CLOSE_BRACKET );
                }
                catch (EmptyStackException ese)
                {
                  Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Stack is empty");
                  throw new ProcessingException ("ERROR: XMLStringCompactor: Stack is empty");
                }
                break;
            }

            processNode (node, PARENT);

            // If we're currently at root iteration node, we've reached the stopping point.
            if ( node == rootIterationNode )
            {

                try
                {
                  String popString = (String) stack.pop ();
                  String addTag = popString.trim();
                  if ( !addTag.equalsIgnoreCase (TEXT_NODE) )
                    xmlBuf.append ( OPEN_BRACKET + SLASH + addTag + CLOSE_BRACKET );
                }
                catch (EmptyStackException ese)
                {
                  Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Stack is empty");
                  throw new ProcessingException ("ERROR: XMLStringCompactor: Stack is empty");
                }
                return null;
            }

            newNode = node.getNextSibling( );

            if (newNode != null)
              processNode (newNode, SIBLING);

        }
        while ( newNode == null );

        return newNode;
    }

   /**
    * Process Node
    */
    private void processNode (Node curNode, int relation) throws MessageException, ProcessingException
    {

      //Append tag to strBuf
      //Push and/or pop from stack
      //Check if any attributes exist and append them too in SORTED order
      //Last node - create value.
      String addTag = null;
      String nodeValue = curNode.getNodeValue();
      if (nodeValue != null)
        nodeValue = nodeValue.trim();
      String nodeName = curNode.getNodeName();
      if (nodeName != null)
        nodeName = nodeName.trim();
      int nodeType = curNode.getNodeType();

      Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Processing THIS" +
      curNode.getNodeName () + " " +curNode.getNodeType() + " " +curNode.getNodeValue ());
      switch (relation)
      {
        case SELF        :
          Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Processing Self node " + curNode.getNodeName () );
          if ( !nodeName.equalsIgnoreCase(TEXT_NODE) )
          //if ( nodeType == Node.ELEMENT_NODE )
          {
            addAttributes ( curNode );
            //xmlBuf.append ( OPEN_BRACKET + nodeName + CLOSE_BRACKET );
          }
          if ( (nodeValue != null) || (!isEmpty (nodeValue) ) )
          {
            xmlBuf.append ( nodeValue );
          }
          stack.push ( nodeName );
          break;

        case CHILD       :
          Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Processing Child node " + curNode.getNodeName () );
          //if ( nodeType == Node.ELEMENT_NODE )
          if ( !nodeName.equalsIgnoreCase(TEXT_NODE) )
          {
            addAttributes ( curNode );
            //xmlBuf.append ( OPEN_BRACKET + nodeName + CLOSE_BRACKET );
          }
          if ( (nodeValue != null) || (!isEmpty (nodeValue) ) )
          {
            xmlBuf.append ( nodeValue );
          }
          stack.push ( nodeName );
          break;

        case SIBLING     :
          Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Processing sibling node " + curNode.getNodeName () );

          try
          {
            String popString = (String) stack.pop ();
            addTag = popString.trim();
          }
          catch (EmptyStackException ese)
          {
            Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Stack is empty");
            throw new ProcessingException ("ERROR: XMLStringCompactor: Stack is empty");
          }
          if ( !addTag.equalsIgnoreCase (TEXT_NODE) )
            xmlBuf.append ( OPEN_BRACKET + SLASH + addTag + CLOSE_BRACKET );
          if ( !nodeName.equalsIgnoreCase(TEXT_NODE) )
          //if ( nodeType == Node.ELEMENT_NODE )
          {
            addAttributes ( curNode );
            //xmlBuf.append ( OPEN_BRACKET + nodeName + CLOSE_BRACKET );
          }
          if ( (nodeValue != null) || (!isEmpty (nodeValue) ) )
          {
            xmlBuf.append ( nodeValue );
          }
          stack.push ( nodeName );
          break;

        case PARENT      :
          Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Processing parent node " + curNode.getNodeName () );
          try
          {
            String popString = (String) stack.pop ();
            addTag = popString.trim();
          }
          catch (EmptyStackException ese)
          {
            Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Stack is empty");
            throw new ProcessingException ("ERROR: XMLStringCompactor: Stack is empty");
          }

          //addTag = (( String ) stack.pop ()).trim();
          if ( !addTag.equalsIgnoreCase (TEXT_NODE) )
          {
            xmlBuf.append ( OPEN_BRACKET + SLASH + addTag + CLOSE_BRACKET );
          }
          break;

      } ;//switch

      if ( !stack.empty () )
        Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Stack is currently " + stack.toString () );
      else
        Debug.log ( Debug.MSG_STATUS, "XMLStringCompactor: Stack is empty" );
      //Removed following debugging as debug file becomes very large
      //Debug.log ( Debug.MSG_STATUS, "XML string generated is " + xmlBuf.toString () );
    };//processNode

   /**
    * Method to add the list of attributes for the node to the XML string
    */
    private void addAttributes ( Node curNode ) throws MessageException
    {
      //The attribute list is built of the form
      //<elementname attname1="attvalue1" attname2="attvalue2">
      String nodeName = curNode.getNodeName();
      if (nodeName != null)
        nodeName = nodeName.trim();

      xmlBuf.append ( OPEN_BRACKET + nodeName);

      //Fetch attributes
      NamedNodeMap attrs = curNode.getAttributes ();
      //Create Hashtable of attributes
      Hashtable hashAttrs = new Hashtable ();
      SortedSet sortedSet = new TreeSet ();
      for (int i=0; i<attrs.getLength(); i++)
      {
        Node attrNode = attrs.item (i);
        String attrNodeName = attrNode.getNodeName();
        String attrNodeValue = attrNode.getNodeValue();
        if ( (attrNodeName == null) || (attrNodeValue == null) )
        {
          Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Node "+ nodeName +
          " has invalid attributes");
          throw new MessageException ("ERROR: XMLStringCompactor: Node "+ nodeName +
          " has invalid attributes");
        }
        attrNodeName = attrNodeName.trim();
        if(compactAttributeValue)
	{
            attrNodeValue = attrNodeValue.trim();
        }
        //Check return values too
        String hashVal = (String) hashAttrs.put (attrNodeName, attrNodeValue);
        if (hashVal != null)
        {
          Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor: Node "+ nodeName +
          " has duplicate attributes");
          throw new MessageException ("ERROR: XMLStringCompactor: Node "+ nodeName +
          " has duplicate attributes");
        }
        sortedSet.add (attrNodeName);
      }
      //Sorted set is ready now. Just iterate thro it.
      Iterator iterator = sortedSet.iterator ();

      if ( (attrs != null) && (iterator != null) )
      {
        while ( iterator.hasNext () )
        {
          String attrName = (String) iterator.next ();
          xmlBuf.append ( SINGLE_WHITE_SPACE );
          xmlBuf.append ( attrName );
          xmlBuf.append ( EQUALS_SIGN + DOUBLE_QUOTES );
          xmlBuf.append ( (String) hashAttrs.get (attrName));
          xmlBuf.append ( DOUBLE_QUOTES );
        };//while
      };//if
      xmlBuf.append ( CLOSE_BRACKET );
    };//addAttributes

    /**
     * Method for checking if a string contains only white space
     * Returns TRUE if only white space remains
     */
     private boolean isEmpty (String str)
     {
      if (str != null)
      {
        if (str.trim().equalsIgnoreCase (""))
          return true;
        else
          return false;
      }
      return true;
     };//isEmpty

    /**
     * Main method for testing purposes, invoked as
     * java -classpath %CLASSPATH% com.nightfire.spi.common.util.XMLStringCompactor
     * test/test.xml
     */
    public static void main (String args[])
    {
      Debug.enableAll();
      Debug.showLevels();

      if (args.length != 1)
      {
        Debug.log (Debug.ALL_ERRORS, "XMLStringCompactor: XML file name needed as input argument");
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
            Debug.log(null, Debug.MAPPING_ERROR, "XMLStringCompactor.main: " +
                      "FileCache failure: " + e.getMessage());
      }

      Debug.log (Debug.MSG_STATUS, "XMLStringCompactor.main: Original XML String is : "
      + inputMessage);
      XMLStringCompactor xmlStringCompactor = new XMLStringCompactor ();

      try
      {
        retValue = xmlStringCompactor.compact ( inputMessage );
      }
      catch (ProcessingException pe)
      {
        Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor.main: " + pe.getMessage ( ) );
      }
      catch (MessageException me)
      {
        Debug.log (Debug.ALL_ERRORS, "ERROR: XMLStringCompactor.main: " + me.getMessage ( ) );
      }

      Debug.log (Debug.MSG_STATUS, "XMLStringCompactor.main: Transformed XML String is : " + retValue);

    };//main

};//XMLStringCompactor

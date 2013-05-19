/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.xml;

import org.w3c.dom.*;


import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;

import java.util.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.message.common.xml.*;


/**
 * XPath filter implementation.
 * All xpath expressions must evaluate to nodes that are allowed to pass through.
 * All attributes on any matching node passes though with it unless the setAttributeFilter
 * method is set.
 * The filtered xml object is a copy of the original.
 */
public class XPathFilter implements XMLFilter
{

    private List<ParsedXPath> xPaths;
    public static final String VALUE_ATTR_NAME = "value";

    private HashSet attrsFilter = null;

    public XPathFilter()
    {
       xPaths = new ArrayList<ParsedXPath>();
    }

    /**
     * Added a xpathpattern to filter on.
     * Multiple patterns can be added to filter on.
     *
     * @param xpath  xpath to use
     * @exception FrameworkException if the xpath is invalid.
     */
    public void addXPathPattern(String xpath) throws FrameworkException
    {
       ParsedXPath parsedXPath = new ParsedXPath(xpath);
       xPaths.add(parsedXPath);
    }


    public String filter(String xml) throws MessageException
    {
       XMLGenerator gen = new XMLPlainGenerator(xml);
       XMLGenerator out = filter(gen);
       return out.getOutput();
    }


    public Document filter(Document xml) throws MessageException
    {
       XMLGenerator gen = new XMLPlainGenerator(xml);
       XMLGenerator out = filter(gen);
       return out.getOutputDOM();
    }

    /**
     * Indicate the attributes that should pass through this filter.
     * by default all attributes pass through.
     * To specify which attributes pass through, pass in an array of attributes.
     * Or for all attributes set index 0 of attrs to a "*".
     * Note: This filter applies to all matching nodes.
     * @param attrs, The attributes to pass through.
     *
     */
    public void setAttributeFilter(String[] attrs)
    {
       if ("*".equals(attrs[0]) )
          attrsFilter = null;

       else {
          attrsFilter = new HashSet();
          for( int i = 0 ; i < attrs.length; i++ )
             attrsFilter.add(attrs[i]);
       }


    }

    public XMLGenerator filter(XMLGenerator xml) throws MessageException
    {
       XMLGenerator gen = new XMLPlainGenerator(xml.getDocument().getDocumentElement().getNodeName());
       // loop over all xpaths and get a list of nodes

       long time = Performance.startTiming(Debug.BENCHMARK);

       try {
         for(int i=0; i < xPaths.size(); i++ ) {
            ParsedXPath parsedXPath = xPaths.get(i);

            List matches = parsedXPath.getNodeList(xml.getDocument());

             // for each matching node get the path and value of the node.
             // and add it to the new generator.
            for (int j = 0; j < matches.size(); j++ ) {
               Node n = (Node)matches.get(j);

               String path = null;
               int type = n.getNodeType();
               switch (type){
                  case Node.ATTRIBUTE_NODE:
                    Debug.warning("XPathFilter: Patterns matching attribute node [" + n.getNodeName() +"] skipping.");
                    break;
                  case Node.ELEMENT_NODE:

                    path = xml.getXMLPath(n);

                    if ( StringUtils.hasValue(path) )  {

                       Node newNode = gen.create(path);
                       String contents = n.getTextContent();

                       // UOM change: if node has text contents for newNode then set those contents
                       // otherwise set create all the attributes node n has to newnode.
                       if (StringUtils.hasValue(contents) && (n.getChildNodes().getLength() == 1 && !(n.getChildNodes().item(0) instanceof Element)))
                       {
                           newNode.setTextContent(contents);
                       }
                       else {
                           createAttributes(gen, newNode, n);
                       }
                    }
                    break;
               }

            }
         }
       } catch ( FrameworkException e ) {
          throw new MessageException(e);
       }

       Performance.stopTiming(Debug.BENCHMARK,time, "XPathFilter finished processing");

       return gen;
    }

    private void createAttributes(XMLGenerator destGen, Node dest, Node source) throws MessageException
    {

        NamedNodeMap map = source.getAttributes();
        if (map == null)
           return;

        for( int i = 0; i < map.getLength(); i++ ) {
           Node attr =  map.item(i);
           String name = attr.getNodeName();
           String value = attr.getNodeValue();
           if (attrsFilter == null || attrsFilter.contains(name) )
              destGen.setAttribute(dest, name, value);
        }

    }

    public static final void main(String args[] )
    {

      StringBuffer bundle = new StringBuffer("<?xml version=\"1.0\"?>");
      bundle.append("<Body>\n<Info>\n");
      bundle.append("<MetaDataName value=\"blah\"/>\n");
      bundle.append("<Action value=\"Submit\"/>\n");
      bundle.append("</Info>\n");
      bundle.append("<Loop>\n");
      bundle.append("<Info><Supplier value=\"acme\"/><MetaDataName value=\"loop\"/></Info>");
      bundle.append(" <Request><lsr_order><Admin><Pon value=\"bdff\"/>");
      bundle.append(" <CCNA value=\"ccna\"/></Admin><contact><name value=\"dan\"/></contact></lsr_order></Request>");
      bundle.append("</Loop>");
      bundle.append("<E911>\n");
      bundle.append("  <Info><Supplier value=\"ilum\"/></Info>");
      bundle.append("  <Root><e911_info><b value=\"b\"/></e911_info></Root>\n");
      bundle.append("</E911>\n");
      bundle.append("<E911>\n");
      bundle.append("  <Info><Supplier value=\"ilum2\"/></Info>");
      bundle.append("  <Info><Supplier value=\"ilum2badinfo\"/></Info>");
      bundle.append("  <Root><e911_info><b value=\"b2\"/></e911_info></Root>\n");
      bundle.append("</E911>\n");
      bundle.append("</Body>\n");


      XPathFilter filter= new XPathFilter();
      try {
        filter.addXPathPattern("//MetaDataName");
        filter.addXPathPattern("//Action");
        filter.addXPathPattern("//Supplier");
        filter.addXPathPattern("//CCNA");
        String filteredXML = filter.filter(bundle.toString());

        System.out.println("Starting xml:\n " + bundle.toString());
        System.out.println("\nFiltered Xml:\n" + filteredXML );
      } catch (FrameworkException e) {
      }

      
    }





}

/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta.help;

// thirdparty imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.debug.*;


/**
 * Base class for help sections/files, etc. that contains information about
 * how to link to this spot in help
 */
abstract class LinkInfo
{
    // constants
    private static final String LINK_START_TAG = "{@link ";
    private static final String LINK_END_TAG   = "}";

    // member vars
    public String fileName     = null;
    public String anchor       = null;
    public String linkText     = null;
    public String nfName       = null;
    public HelpCreator creator;

    private DebugLogger log;
  
    /**
     * Constructor
     */
    protected LinkInfo(HelpCreator creator)
    {
        this.creator = creator;
        log = creator.log;
        
    }

    /**
     * Utility for working with the DOM.  Creates a new element and appends
     * it to the children of node.
     *
     * @param node  The node to append the new element to
     * @param name  The name for the new element
     *
     * @return The newly created element
     */
    protected Element addElem(Node node, String name)
    {
        Element newElem = node.getOwnerDocument().createElement(name);
        node.appendChild(newElem);

        return newElem;
    }

    /**
     * Utility for working with the DOM.  Creates a new element and appends
     * it to the children of node.  Then create a new text node and appends
     * it to the children of the new element.
     *
     * @param node  The node to append the new element to
     * @param name  The name for the new element
     * @param txt   The text for the element's text node
     *
     * @return The newly created element
     */
    protected Element addElem(Node node, String name, String txt)
    {
        Element newElem = node.getOwnerDocument().createElement(name);
        node.appendChild(newElem);

        Text txtNode = node.getOwnerDocument().createTextNode(txt);
        newElem.appendChild(txtNode);

        return newElem;
    }

    /**
     * Adds in help text nodes to an element
     *
     * @param elem  The element to add the nodes to
     * @param help  The parent node of the help text
     */
    protected void addHelpText(Node elem, Node help) throws FrameworkException
    {
        Node helpChild = help.getFirstChild();
        Document doc = elem.getOwnerDocument();

        // copy each child
        for (; helpChild != null; helpChild = helpChild.getNextSibling())
        {
            Node newHelp = XMLMessageGenerator.copyNode(doc, helpChild);
            
            elem.appendChild(newHelp);

            // expand any links
            finalizeHelp(newHelp);
        }
    }

    /**
     * Walks over a node and its children, expanding links where appropriate
     */
    protected void finalizeHelp(Node n)
    {
        // do the node and its siblings
        Node next = n.getNextSibling();
        while (n != null)
        {
            // visit any children first (first handles siblings)
            Node child = n.getFirstChild();
            if (child != null)
                finalizeHelp(child);

            // now clean up this node
            if (n instanceof Text)
                expandLinks(n.getParentNode(), (Text)n);

            n = next;
            if (n != null)
                next = n.getNextSibling();
        }
    }

    /**
     * Evaluates the text node on an element and expands it if any links
     * are included in the text.
     *
     * @param elem  The element that owns the text node
     * @param txt   The text node to evaluate
     */
    protected void expandLinks(Node elem, Text txt)
    {
        int fromIdx = 0;
        int atIdx;
        String val = txt.getNodeValue();

        while ( (atIdx = val.indexOf(LINK_START_TAG, fromIdx)) != -1)
        {
            // set the node value to the text up to the link
            txt.setNodeValue(val.substring(fromIdx, atIdx));

            // find the end of the link
            int endIdx = val.indexOf(LINK_END_TAG, fromIdx);
            if (endIdx == -1)
            {
                String msg = "Missing tag \"" + LINK_END_TAG
                          + "\" following link start at position " + atIdx
                          + ":\n"
                          + val.substring(0, atIdx + LINK_START_TAG.length())
                          + "****ERROR****";
                log.error( msg);
                creator.addError(msg + "\n");

                // just ignore it
                txt.setNodeValue(txt.getNodeValue() + val.charAt(atIdx));
                if (atIdx < (val.length() - 1))
                {
                    Text oldText = txt;
                    txt = elem.getOwnerDocument()
                        .createTextNode(val.substring(atIdx + 1));
                    insertAfter(elem, oldText, txt);
                }
                fromIdx = atIdx + 1;
            }
            else
            {
                Node lastNode = txt;

                // get the link name
                String id = val.substring(atIdx + LINK_START_TAG.length(),
                                          endIdx);

                LinkInfo lnk = (LinkInfo)creator.getLink(id);
                
                // create the HTML
                if (lnk != null)
                {
                    // figure out the URL
                    String href;
                    if (lnk.anchor != null)
                        href = lnk.fileName + "#" + lnk.anchor;
                    else
                        href = lnk.fileName;

                    // output the link
                    Element anchor = elem.getOwnerDocument()
                        .createElement("a");
                    anchor.setAttribute("onMouseOut",
                                        "window.status=''; return true;");
                    anchor.setAttribute("onMouseOver", "window.status='"
                                        + lnk.linkText + "';return true;");
                    anchor.setAttribute("href", href);
                    anchor.appendChild(elem.getOwnerDocument()
                                       .createTextNode(lnk.linkText)); 
                    insertAfter(elem, txt, anchor);
                    lastNode = anchor;
                }
                else
                {
                    String msg = "No such link as \"" + id + "\".";
                    log.error( msg);
                    creator.addError(msg + "\n");

                    // insert the id instead
                    txt.setNodeValue(txt.getNodeValue() + id);
                }

                // start at the position following the end tag
                fromIdx = endIdx + LINK_END_TAG.length();
                if (fromIdx < (val.length() - 1))
                {
                    Text oldText = txt;
                    txt = elem.getOwnerDocument()
                        .createTextNode(val.substring(fromIdx));
                    insertAfter(elem, lastNode, txt);
                }
            }
        }
    }

    /**
     * Inserts a new child after a sibling
     *
     * @param parent The parent node to insert the child into
     * @param sib    The sibling to come before the new child
     * @param child  The new child to insert
     */
    protected void insertAfter(Node parent, Node sib, Node child)
    {
        Node nextSib = sib.getNextSibling();
        if (nextSib == null)
            parent.appendChild(child);
        else
            parent.insertBefore(child, nextSib);
    }
}

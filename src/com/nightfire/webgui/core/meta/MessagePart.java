/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.util.List;
import java.util.HashMap;

// third-part imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.debug.*;


/**
 * MessagePart is the generic base class for any object which is part of a
 * Message.
 */
public abstract class MessagePart implements Cloneable
{
    /**
     * Part ID
     */
    protected String id = null;

    /**
     * Value to display in the GUI
     */
    protected String displayName = null;

    /**
     * Full part name
     */
    protected String fullName = null;

    /**
     * Abbreviation
     */
    protected String abbreviation = null;

    /**
     * Max Number of Nodes that can occur.
     */
    protected String maxOccurs = null;

    /**
     * Help text in DOM form
     */
    protected Node helpNode = null;

    /**
     * Full help text
     */
    protected String helpText = null;

    /**
     * relative XML path
     */
    protected PathElement path = null;

    /**
     * Parent message part
     */
    protected MessageContainer parent = null;

    protected DebugLogger log;

    /**
     * Default Constructor
     */
    public MessagePart()
    {
      log = DebugLogger.getLoggerLastApp(this.getClass());

    }

    /**
     * Returns this part's immediate parent
     */
    public MessageContainer getParent()
    {
        return parent;
    }

    /**
     * Obtains the top-level object (the Message) in the definition
     */
    public Message getRoot()
    {
        if (parent == null)
        {
            // if we are not a Message, we are disconnected from the tree, so
            // return null
            if (this instanceof Message)
                return (Message)this;
            else
                return null;
        }
        else
            return parent.getRoot();
    }

    /**
     * This returns the next MessagePart at the same level in the tree as
     * this part.  The item may be a sibling, the child of a parent's
     * sibling, or null if no next parts are in the tree at the same level.
     */
    public MessagePart getNextSibling()
    {
        // no parent means no siblings
        if (parent == null)
            return null;

        // get the list of siblings
        List sibs = parent.getChildren();
        int myIdx = sibs.indexOf(this);
        if (myIdx == -1)
            return null;

        // return the sibling that follows us
        if (myIdx + 1 < sibs.size())
            return (MessagePart)sibs.get(myIdx + 1);
        // or get our parent's sibling if we're the end of the list
        MessagePart parentSib = parent.getNextSibling();
        while (parentSib != null)
        {
            // the parent sibling must be a container and have a child
            // to provide a part at the same level
            if (parentSib instanceof MessageContainer)
            {
                MessagePart cousin = ((MessageContainer)parentSib).getChild(0);
                if (cousin != null)
                    return cousin;
            }

            // keep trying
            parentSib = parentSib.getNextSibling();
        }

        // no such sibling
        return null;
    }

    /**
     * This returns the previous MessagePart at the same level in the tree as
     * this part.  The item may be a sibling, the child of a parent's
     * sibling, or null if no previous parts are in the tree at the same level.
     */
    public MessagePart getPreviousSibling()
    {
        // no parent means no siblings
        if (parent == null)
            return null;

        // get the list of siblings
        List sibs = parent.getChildren();
        int myIdx = sibs.indexOf(this);
        if (myIdx == -1)
            return null;

        // return the sibling that follows us
        if (myIdx > 0)
            return (MessagePart)sibs.get(myIdx - 1);
        // or get our parent's sibling if we're the end of the list
        MessagePart parentSib = parent.getPreviousSibling();
        while (parentSib != null)
        {
            // the parent sibling must be a container and have a child
            // to provide a part at the same level
            if (parentSib instanceof MessageContainer)
            {
                MessagePart cousin = ((MessageContainer)parentSib).getChild(0);
                if (cousin != null)
                    return cousin;
            }

            // keep trying
            parentSib = parentSib.getPreviousSibling();
        }

        // no such sibling
        return null;
    }

    /**
     * Returns a copy of this part, using the given parent and path
     * components as new values for the copy
     *
     * @param parent   The parent MessageContainer for the copy
     * @param pathElem The path elements for the copy
     * @param msg      The root Message we're associated with
     *
     * @return A copy of this part
     */
    protected MessagePart copy(MessageContainer parent, PathElement pathElem,
                               Message msg)
    {
        return copy(parent, pathElem, msg, false);
    }

    /**
     * Returns a copy of this part, using the given parent and path
     * components as new values for the copy
     *
     * @param parent   The parent MessageContainer for the copy
     * @param pathElem The path elements for the copy
     * @param msg      The root Message we're associated with
     * @param force    If false, the returned copy may be the same as this
     *                 (if this is unreferenced), otherwise a full copy is
     *                 forced
     *
     * @return A copy of this part
     */
    protected MessagePart copy(MessageContainer parent, PathElement pathElem,
                               Message msg, boolean force)
    {
        try
        {
            MessagePart newPart;

            // if we don't have a parent, there's no need to create a copy,
            // this instance is available
            if ( (this.parent == null) && ( !(this instanceof Message)) &&
                 (!force) )
            {
                newPart = this;
                if (parent == null)
                    // this allows copy to be called more than once even if
                    // the parent is not yet known (although navigation will
                    // be off until the parent is set)
                    this.parent      = msg;
                else
                    this.parent      = parent;

                if (this.path == null)
                    this.path        = pathElem;
                else
                    this.path.parent = pathElem;
            }
            else
            {
                newPart = (MessagePart)clone();
                newPart.parent          = parent;
                if ( (path == null) ||
                     ((this.parent != null) ? (path == this.parent.path)
                                            : false) )
                    newPart.path        = pathElem;
                else
                {
                    newPart.path        = path.copy();
                    newPart.path.parent = pathElem;
                }

                // the new instance needs a unique id
                newPart.id = generateUniqueId(msg);
                msg.registerPart(newPart);
            }

            if ((pathElem != null) && (newPart.path != null) &&
                (pathElem != newPart.path))
            {
                pathElem.addChild(newPart.path);
                newPart.path.part = newPart;
            }

            return newPart;
        }
        catch (Exception ex)
        {
            log.error("",ex);
            log.error( ex.toString());
            return null;
        }
    }

    /**
     * Returns the part's ID
     * @deprecated  (Jan 20, 2005) This method does not conform to the Java Bean naming standard.  Use getId() instead.
     */
    public String getID()
    {
        return id;
    }

    /**
     * Returns the part's id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the display name for the part
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the full name of the part
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Returns the abbreviation that can be used for the part
     */
    public String getAbbreviation()
    {
        return abbreviation;
    }

    public String getMaxOccurs()
    {
        return maxOccurs;
    }

    public void setMaxOccurs(String maxnodes)
    {
      this.maxOccurs = maxnodes;
    }


    /**
     * Returns the helpText for the part
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Returns the helpText as a DOM node (the node is actually the parent
     * of the help text, which may be comprised of several sibling nodes).
     */
    public Node getHelpNode()
    {
        return helpNode;
    }

    /**
     * Returns the NightFire dot-separated XML path to this part from its
     * parent.  This method returns null if this part does not have a
     * corresponding element in the XML and it shares a path with its parent.
     */
    public String getXMLPath()
    {
        if (path == null)
            return null;
        if (parent != null)
            return path.getNFPathFrom(parent.path);
        return path.getNFPath();
    }

    /**
     * Returns the full NightFire dot-separated XML path to this part
     */
    public String getFullXMLPath()
    {
        if (path == null)
            return null;
        return path.getNFPath();
    }

    /**
     * Read this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     * @param path   Any path information to prepend to the definition
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, Message msg, PathElement path)
        throws FrameworkException
    {
        this.path = path;
        if (path != null)
            path.part = this;

        MessagePaths xpaths = msg.getXPaths();

        // name
        id = getString(xpaths.idPath, ctx);
        if (id == null)
            id = getString(xpaths.altIdPath, ctx);
        msg.registerPart(this);

        // displayName
        displayName = getString(xpaths.displayNamePath, ctx);

        // fullName
        fullName = getString(xpaths.fullNamePath, ctx);

        // abbreviation
        abbreviation = getString(xpaths.abbreviationPath, ctx);

        // helpText node
        List helpNodes = xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            helpNode = (Node)helpNodes.get(0);

            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }
    }

    /**
     * Read extensions to this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     * @param path   Any path information to prepend to the definition
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readExtensions(Node ctx, Message msg, PathElement path)
        throws FrameworkException
    {
        MessagePaths xpaths = msg.getXPaths();

        // displayName
        String newDisplayName = getString(xpaths.displayNamePath, ctx);
        if (StringUtils.hasValue(newDisplayName))
            displayName = newDisplayName;

        // fullName
        String newFullName = getString(xpaths.fullNamePath, ctx);
        if (StringUtils.hasValue(newFullName))
            fullName = newFullName;

        // abbreviation
        String newAbbr = getString(xpaths.abbreviationPath, ctx);
        if (StringUtils.hasValue(newAbbr))
            abbreviation = newAbbr;

        // helpText node
        List helpNodes = xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            helpNode = (Node)helpNodes.get(0);
            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }
    }

    /**
     * Sets the parent of this part
     */
    public void setParent(MessageContainer parent)
    {
        this.parent = parent;
    }

    /**
     * Gets the String value from a ParsedXPath, returning null if no value
     * is present
     */
    protected String getString(ParsedXPath xpath, Node ctx)
        throws FrameworkException
    {
        List nodes = xpath.getNodeList(ctx);
        if (nodes.size() < 1)
            return null;
        else
            return ((Node)nodes.get(0)).getNodeValue();
    }

    /**
     * Generates a unique id for this part (used when copies are being made
     * so that each instance can be distinguised using its id.
     */
    protected String generateUniqueId(Message msg)
    {
        int i = 2;
        while (msg.getMessagePart(id + "#" + i) != null)
            i++;

        return (id + "#" + i);
    }

    /**
     * A class used to represent an element in an XML path
     */
    public static class PathElement implements Cloneable
    {
        public String name = null;
        public PathElement parent = null;
        public MessagePart part = null;
        // mapping of child elements by name
        protected HashMap children = new HashMap();

        /**
         * Returns the NightFire dot-separated XML path
         */
        public String getNFPath()
        {
            StringBuffer buff = new StringBuffer();
            getNFPath(buff);

            return buff.toString();
        }

        /**
         * Returns the NightFire dot-separated XML path relative to another
         * place in the path
         *
         * @param start The PathElement path construction begins after
         *
         * @return The dot-separated path or null if asking for the path
         *         from this PathElement
         */
        public String getNFPathFrom(PathElement start)
        {
            if (start == this)
                return null;

            StringBuffer buff = new StringBuffer();
            getNFPathFrom(buff, start);

            return buff.toString();
        }

        /**
         * Creates a copy of this path
         */
        public PathElement copy()
        {
            PathElement newPath = null;
            try
            {
                newPath = (PathElement)clone();
                newPath.children = new HashMap();
            }
            catch (Exception ex)
            {
                // returns null
            }

            return newPath;
        }

        /**
         * Locates a child by name (may be a dot-separated path)
         */
        public PathElement getChild(String name)
        {
            // get the name of the first child in the path
            String searchName = name;
            String nextName = null;
            int idx = name.indexOf('.');
            if (idx != -1)
            {
                searchName = name.substring(0, idx);
                if (idx < name.length() - 1)
                    nextName = name.substring(idx + 1);
            }

            // get the child
            PathElement child = (PathElement)children.get(searchName);
            if ((child == null) || (nextName == null))
                return child;
            else
                return child.getChild(nextName);
        }

        /**
         * Adds a child
         */
        public void addChild(PathElement child)
        {
            child.parent = this;

            // if the child already exists add all the children from
            // the old one to the new child
            // or else all of the children from the old one
            // will be lost.
            PathElement old = (PathElement)children.put(child.name, child);
            if ( old != null)
                child.children.putAll(old.children);

        }

        /**
         * Used internally to obtain the NightFire path
         */
        protected void getNFPath(StringBuffer path)
        {
            if (parent == this)
                throw new IllegalStateException("parent is the same as this! (name: [" + name + "])");

            // prepend our parent's path
            if (parent != null)
                parent.getNFPath(path);

            // add our portion
            if (name != null)
            {
                if (path.length() > 0)
                    path.append('.');
                path.append(name);
            }
        }

        /**
         * Used internally to obtain the NightFire path from a starting point
         */
        protected void getNFPathFrom(StringBuffer path, PathElement start)
        {
            // prepend our parent's path, if they're not the start
            if ((parent != null) && (parent != start))
                parent.getNFPathFrom(path, start);

            // add our portion
            if (name != null)
            {
                if (path.length() > 0)
                    path.append('.');
                path.append(name);
            }
        }
    }

    /**
     * Returns a string description of this part
     */
    public String describe()
    {
        return id + "\n  type: " + this.getClass().getName()
                  + "\n  path: " + getFullXMLPath()
                  + "\n  relative path: " + getXMLPath();
    }
}

/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.FrameworkException;

/**
 * ComplexType is a group of message parts which can be referenced by
 * extension.
 */
public class ComplexType extends MessageContainer
{
    /**
     * Default constructor
     */
    public ComplexType()
    {
        super();
    }

    /**
     * Read children of this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     * @param path   Any path information to prepend to the definition
     * @param parent The parent MessagePart for the children
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public MessagePart[] readChildrenFromXML(Node ctx, Message msg,
                                             PathElement path,
                                             MessageContainer parent)
        throws FrameworkException
    {
        MessagePaths xpaths = msg.getXPaths();
        List children;

        // get the list of children
        children = xpaths.complexTypeChildPath.getNodeList(ctx);
        
        // load up each one
        ArrayList parts = new ArrayList();
        int len = children.size();
        for (int i = 0; i < len; i++)
        {
            // create the parts for each child
            MessagePart[] theseParts =
                msg.readPartsFromXML((Node)children.get(i), path, parent);

            if (theseParts != null)
            {
                // add the parts
                for (int j = 0; j < theseParts.length; j++)
                    parts.add(theseParts[j]);
            }
        }

        MessagePart[] allParts = new MessagePart[parts.size()];
        return (MessagePart[])parts.toArray(allParts);
    }

    /**
     * Returns a copy of all of the type's children
     *
     * @param parent   The parent MessageContainer for the copy
     * @param pathElem The path elements for the copy
     * @param msg      The root Message we're associated with
     *
     * @return A copy of this part
     */
    public MessagePart[] copyChildren(MessageContainer parent,
                                         PathElement pathElem, Message msg)
    {
        MessagePart[] newKids = new MessagePart[children.size()];

        String topName = (path == null) ? null : path.name;

        // copy all the children
        Iterator iter = children.iterator();
        for (int kid = 0; iter.hasNext(); kid++)
        {
            MessagePart child = (MessagePart)iter.next();

            // make sure the path is correct for the child
            PathElement thisChildPath =
                (child.path == null) ? child.path : child.path.parent;
            ArrayList pathList = new ArrayList();

            // figure out the full path
            while ( (topName == null) ? (thisChildPath != null) :
                    ( (thisChildPath != null) &&
                      (!topName.equals(thisChildPath.name)) ) )
            {
                pathList.add(thisChildPath);
                thisChildPath = thisChildPath.parent;
            }

            // get that path for the new child
            PathElement newChildPath = pathElem;
            for (int i = pathList.size() - 1; i >= 0; i--)
            {
                PathElement elem = (PathElement)pathList.get(i);
                PathElement newElem = (newChildPath == null) ?
                    null : newChildPath.getChild(elem.name);
                if (newElem == null)
                {
                    newElem = elem.copy();
                    if (newChildPath != null)
                        newChildPath.addChild(newElem);
                }
                newChildPath = newElem;
            }

            newKids[kid] = child.copy(parent, newChildPath, msg);
        }

        return newKids;
    }
}

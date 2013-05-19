/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.nightfire.framework.util.*;

import org.w3c.dom.*;

/**
 * MessageContainer is a MessagePart which may contain other MessageParts.
 */
public abstract class MessageContainer
    extends MessagePart {
  /*
   * XML document values
   */
  protected final static String ELEM_NAME = "xsd:element";
  protected final static String REPEATABLE_VAL = "unbounded";

  /**
   * The MessageParts contained within this part
   */
  protected ArrayList children = new ArrayList();

  /**
   * An unmodifiable view of our children
   */
  protected List childList = Collections.unmodifiableList(children);

  /**
   * Indicates whether this container is repeatable or not
   */
  protected boolean repeatable = false;

  /**
   * Indicates whether this container is optional or not
   */
  protected boolean optional = false;

  /**
   * Default constructor
   */
  public MessageContainer() {
    super();
  }

  /**
   * Adds a child
   *
   * @param child The child to add
   */
  protected void addChild(MessagePart child) {
    child.setParent(this);
    children.add(child);
  }

  /**
   * Obtains a particular child by index
   *
   * @param idx  The index of the child to retrieve
   */
  public MessagePart getChild(int idx) {
    return (MessagePart) children.get(idx);
  }

  /**
   * Obtains a particular child by id
   *
   * @param id  The id of the child to retrieve
   *
   * @return The requested child, or null if it does not exist
   */
  public MessagePart findChild(String id) {
    String lookFor = stripIdSuffix(id);

    Iterator iter = children.iterator();
    while (iter.hasNext()) {
      MessagePart child = (MessagePart) iter.next();
      if (lookFor.equals(stripIdSuffix(child.getID()))) {
        return child;
      }
    }

    return null;
  }

  /**
   * Obtains a list of all the children
   */
  public List getChildren() {
    return childList;
  }

  /**
   * Indicates whether this container may have multiple instances present
   * or not
   */
  public boolean getRepeatable() {
    return repeatable;
  }

  /**
   * Indicates whether this container is optional or required.
   * @return true if this container is optional otherwise false.
   */
  public boolean getOptional() {
    return optional;
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
  public void readFromXML(Node ctx, Message msg, PathElement path) throws
      FrameworkException {
    // read the common sections first
    super.readFromXML(ctx, msg, path);

    // see if this container repeats
    String val = getString(msg.getXPaths().repeatablePath, ctx);
    String opt = getString(msg.getXPaths().optionalPath, ctx);

    if (val != null) {
      repeatable = false;
      maxOccurs = val;
      if (val.equalsIgnoreCase(REPEATABLE_VAL)) {
        repeatable = true;
      }
      else {
        int lmaxOccurs = 0;
        try {
          lmaxOccurs = Integer.parseInt(val);
        }
        catch (NumberFormatException ex) {
          throw new FrameworkException(
              "Number Format Exception for maxOccurs attribute for Node: " +
              displayName);
        }
        if (lmaxOccurs > 1) {
          repeatable = true;
        }
      }
    }

    if (opt != null) {
      optional = Boolean.valueOf(opt).booleanValue();

      // read in our children
    }
    MessagePart[] kids = readChildrenFromXML(ctx, msg, path, this);

    // add them
    for (int i = 0; i < kids.length; i++) {
      addChild(kids[i]);
    }
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
                                           MessageContainer parent) throws
      FrameworkException {
    MessagePaths xpaths = msg.getXPaths();
    List children;

    // get the list of children
    if (ctx.getNodeName().equals(ELEM_NAME)) {
      children = xpaths.elemChildPath.getNodeList(ctx);
    }
    else {
      children = xpaths.seqChildPath.getNodeList(ctx);

      // load up each one
    }
    ArrayList parts = new ArrayList();
    int len = children.size();
    for (int i = 0; i < len; i++) {
      // create the parts for each child
      MessagePart[] theseParts =
          msg.readPartsFromXML( (Node) children.get(i), path, parent);
      if (theseParts != null)
      {
          // add the parts
          for (int j = 0; j < theseParts.length; j++) {
            parts.add(theseParts[j]);
          }
      }
    }

    MessagePart[] allParts = new MessagePart[parts.size()];
    return (MessagePart[]) parts.toArray(allParts);
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
  public void readExtensions(Node ctx, Message msg, PathElement path) throws
      FrameworkException {
    MessagePaths xpaths = msg.getXPaths();
    List extChildren;

    // get the list of children
    String extBase = getString(xpaths.extensionPath, ctx);
    if (StringUtils.hasValue(extBase)) {
      extChildren = xpaths.extensionChildPath.getNodeList(ctx);
    }
    else {
      extChildren = xpaths.elemChildPath.getNodeList(ctx);

      // load up each one
    }
    int len = extChildren.size();
    for (int i = 0; i < len; i++) {
      Node childNode = (Node) extChildren.get(i);

      // get the child's name and type
      String elemName = getString(xpaths.idPath, childNode);
      String stepName = stripIdSuffix(elemName);
      String nodeType = getString(xpaths.NFTypePath, childNode);
      String childExt = getString(xpaths.extensionPath, childNode);

      // set up the path step
      PathElement step = path;
      if (StringUtils.hasValue(stepName)) {
        if (path != null) {
          step = path.getChild(stepName);
        }
        if (step == null) {
          step = new PathElement();
          step.name = elemName;
          step.parent = path;
          if (path != null) {
            path.addChild(step);
          }
        }
      }

      // if the child exists, grab it
      MessagePart child = null;
      if (elemName != null) {

        // this should be a reference
        child = findChild(elemName);

        // handle elements unrelated to the GUI
      }
      if ( (nodeType == null) && (child == null) &&
          (!StringUtils.hasValue(childExt)) && (elemName != null)) {
        readExtensions(childNode, msg, step);
      }

      // otherwise, see if the child exists
      else if (child != null) {
        // see if this child is excluded
        String maxOccurs = getString(xpaths.maxOccursPath, childNode);
        if (maxOccurs == null) {
          maxOccurs = "";

        }
        if (maxOccurs.equals("0")) {
          children.remove(child);
        }
        else {

          // otherwise, have it update itself
          child.readExtensions(childNode, msg, child.path);
        }
      }
      else {
        // read the child as usual
        MessagePart[] newKids =
            msg.readPartsFromXML( (Node) extChildren.get(i), path, this);

        if (newKids != null)
        {
            // add it
            for (int j = 0; j < newKids.length; j++) {
                addChild(newKids[j]);
            }
        }
      }
    }
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
                             Message msg, boolean force) {
    MessageContainer newContainer =
        (MessageContainer)super.copy(parent, pathElem, msg, force);
    if (newContainer == this) {
      return newContainer;
    }

    if (newContainer == null) {
      System.out.println("newContainer is null");
    }
    if (children == null) {
      System.out.println("children is null");
    }
    newContainer.children = new ArrayList(children.size());
    newContainer.childList =
        Collections.unmodifiableList(newContainer.children);

    // copy all the children
    Iterator iter = children.iterator();
    while (iter.hasNext()) {
      MessagePart child = (MessagePart) iter.next();

      // make sure the path is correct for the child
      PathElement thisChildPath =
          (child.path == null) ? child.path : child.path.parent;
      ArrayList pathList = new ArrayList();

      // this is only for children who have their own elements
      if (isElement(child)) {
        // figure out the full path
        while ( (thisChildPath != null) &&
               (!namesEqual(newContainer.path, thisChildPath))) {
          pathList.add(thisChildPath);
          thisChildPath = thisChildPath.parent;
        }
      }

      // get that path for the new child
      PathElement newChildPath = newContainer.path;
      for (int i = pathList.size() - 1; i >= 0; i--) {
        PathElement elem = (PathElement) pathList.get(i);
        PathElement newElem = (newChildPath == null) ? null
            : newChildPath.getChild(elem.name);
        if (newElem == null) {
          newElem = elem.copy();
          if (newChildPath != null) {
            newChildPath.addChild(newElem);
          }
        }
        newChildPath = newElem;
      }

      newContainer.children.add(child.copy(newContainer,
                                           newChildPath, msg));
    }

    return newContainer;
  }

  /**
   * Strips an ID string of any # character and anything that follows.  To
   * allow unique ids to be used for parts with the same XML element name,
   * # is allowed in an id, but not retained as part of the XML path
   */
  protected String stripIdSuffix(String id) {
    if (id == null) {
      return id;
    }

    int invalidIdx = id.indexOf('#');

    if (invalidIdx == 0) {
      return "";
    }

    if (invalidIdx > -1) {
      return id.substring(0, invalidIdx);
    }

    return id;
  }

  /**
   * Tests a MessagePart to see if it has an element of its own
   */
  protected boolean isElement(MessagePart part) {
    if (part.path == null) {
      return true;
    }
    if (part.parent == null) {
      return true;
    }

    return (part.path != part.parent.path);
  }

  /**
   * Compares the names of two paths, handling nulls in either the paths
   * or the names
   */
  protected boolean namesEqual(PathElement p1, PathElement p2) {
    if ( (p1 == null) || (p2 == null)) {
      return (p1 == p2);
    }

    if ( (p1.name == null) || (p2.name == null)) {
      return (p1.name == p2.name);
    }

    return p1.name.equals(p2.name);
  }

  /**
   * Returns a description of this container.
   * <p>
   * Note: This is not very efficient.  It's intended only for diagnostic
   * output in a non-production environment.
   */
  public String describe() {
    StringBuffer buff = new StringBuffer();
    buff.append(id);
    buff.append("\n  type: ").append(this.getClass().getName());
    buff.append("\n  path: ").append(getFullXMLPath());
    buff.append("\n  children:");

    Iterator iter = children.iterator();
    while (iter.hasNext()) {
      String childStr = ( (MessagePart) iter.next()).describe();
      StringTokenizer tok = new StringTokenizer(childStr, "\n");
      while (tok.hasMoreTokens()) {
        buff.append("\n    ").append(tok.nextToken());
      }
    }

    return buff.toString();
  }
}

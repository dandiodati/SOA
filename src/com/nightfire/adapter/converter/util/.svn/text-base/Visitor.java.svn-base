/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.util;

import antlr.collections.AST;

/**
* A Visitor implementation is passed to a TreeWalker when walking a parsed
* ANTLR tree. As the walker traverses the tree, it passes each node to the
* given visitor, and the Visitor implementation "visits" the node performing
* the real work. So the traversal algorithm lives in the TreeWalker
* implementation, and Visitor instances can be interchanged to add the
* custom functionality.
*/
public interface Visitor{

   /**
   * As a TreeWalker walks an AST, it passes the current node to
   * the visit() method to do the real work. (For example, build an XML
   * structure, print the node value, search, etc.).
   *
   * @param node the current node to be visited.
   * @param currentPath this is a String representation of the path
   *                    from the root node of the AST to the given
   *                    node. The actual format of this path string
   *                    depends upon the particular TreeWalker implementation
   *                    that is using this Visitor.
   */
   public String visit(AST node, String currentPath);


}


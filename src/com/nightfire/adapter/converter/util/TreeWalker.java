/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.util;

import antlr.collections.AST;

/**
* Implementations of this class are used to walk a parser ANTLR tree,
* visiting each node and performing some custom operation.
*/
public interface TreeWalker{

   /**
   * Implementations of this method traverse an ANTLR tree starting with the
   * given root, passing each node to the given Visitor implementation.
   *
   * @param visitor as walk traverses each node, the node gets passed
   *                to the visit() method of this Visitor.
   * @param root The starting node from which to walk the ANTLR sibling tree.
   */
   public void walk(Visitor visitor, AST root);
   
} 
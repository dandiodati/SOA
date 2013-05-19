/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.util;

import antlr.collections.AST;

import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.Debug;

/**
* This class is used to walk a parsed ANTLR tree and visit each node using
* a preorder depth-first traversal.
*
*/
public class DepthFirstTreeWalker implements TreeWalker{

   /**
   * This is the default delimiter used to separate the path Strings that
   * are constructed as the tree is traversed. The default value is "."
   * to support the NightFire dotted XML path notation.
   */
   public static final String DEFAULT_DELIMITER = ".";

   /**
   * This method does a depth-first walk of an AST starting at the given
   * root node. Each traversed node (and its path) is passed to the
   * given Visitor class.
   *
   * @param visitor the visitor that visits each node and performs some
   *                action using the value of the node. For example,
   *                an XMLVisitor builds an XML tree to mirror the
   *                AST.
   * @param root The root node of a parsed Antlr tree.
   *
   */
   public void walk(Visitor visitor, AST root){

      String path = "";
      walk(visitor, root, path);

   }

   /**
   * This is the recursive version of the walk method. This version
   * of the method takes a String path that lets the current node know
   * the path from the root node to the current node. By default,
   * this path is in the NightFire dotted notation. So for example,
   * if the antlr tree is structured like:
   * <pre>
   *    root
   *       +-- container
   *                   +-- item
   * </pre>
   *
   * The path passed to walk for the root node will be "", "root" for the
   * container node, and "root.container" for the item node.
   *
   * @param visitor the visitor that visits each node and performs some
   *                action using the value of the node. For example,
   *                an XMLVisitor builds an XML tree to mirror the
   *                AST.
   * @param current the current ANTLR node.
   * @param path a String describing the path from the root to the current node. 
   */
   public void walk(Visitor visitor, AST current, String path){

      // the first child node of the current node
      AST firstBorn;

      // The current node count or index
      int count = 0;

      while(current != null){

         if( Debug.isLevelEnabled(Debug.MAPPING_STATUS) ){
            Debug.log(Debug.MAPPING_STATUS,
                      "Visiting node ["+current+"] context ["+path+"]");
         }

         // the AST is a sibling tree, get the
         firstBorn = current.getFirstChild();

         // Visit the current node.  
         String addPath = visitor.visit(current, path);

         if(StringUtils.hasValue(addPath)){

            // this constructs a new indexed path for the current node,
            // and passed that path
            walk(visitor,
                 firstBorn,
                 path  + getDelimiter() + getIndexedPath(addPath, count));

         }
         else{

            walk(visitor, firstBorn, path);

         }

         current = current.getNextSibling();
         count++;

      }

   }

   /**
   * This method is called by <code>walk()</code> to append a node's index
   * to its path. This method allows subclasses to overide the default indexing
   * behavior. By default, this returns a path in NightFire dotted notation.
   * (e.g. A path value of "root.container.item" and an index of 4,
   *       will return "root.container.item(4)".)
   * 
   *
   * @param path the path to which the given index needs to be appended.
   * @param index a zero-based index
   *
   */
   protected String getIndexedPath(String path, int index){

      String result = "";

      if(StringUtils.hasValue(path)){

         result = path + "(" + index + ")";

      }

      return result;

   }

   /**
   * This method is used by <code>walk()</code> to get the delimiter that
   * is used to separate subpaths. By default, this returns a ".", so that
   * the paths are constructed in NightFire "dotted" notation
   * (e.g. "parent.child.item"). This method allows subclasses to return
   * a custom delimiter.
   *
   */
   protected String getDelimiter(){

      return DEFAULT_DELIMITER;

   }

}
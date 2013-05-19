package com.nightfire.adapter.converter.util;

import antlr.collections.AST;

/**
* An example Visitor implementation that print each node as it visits them.
* This class provides a convenient way to view the output of
* an ANTLR parser in human-readable form. This class is for testing and
* demonstration purposes only. 
*/
public class PrintVisitor implements Visitor{

   /**
   * For the given node, this prints the path to the current AST node
   * and its text value in the format: "<path>=<node text value>".
   * This defines the Visitor interface.
   */
   public String visit(AST node, String path){

      // this prints out the current path and the text value of the
      // current node 
      System.out.println(path+"="+node.getText());

      // return the name of the node for use in assembling a path
      // to be passed to child nodes
      return node.getText();

   }

   /**
   * Uses a PrintVisitor to list the paths and values of all nodes in
   * an AST from the given root node down. 
   */
   public static void printTree(AST root){

      TreeWalker walker = new DepthFirstTreeWalker();
      walker.walk(new PrintVisitor(), root);

   }

} 

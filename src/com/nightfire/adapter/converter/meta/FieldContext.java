/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

/**
* FieldContext instances are passed from parent fields to child fields
* when data is being converted. A FieldContext represents the current
* location in an input or output XML. The FieldContext can be
* conveniently accessed as
* an XPath value or as NightFire dotted XML notation.
*/
public class FieldContext implements Cloneable{

   /**
   * The list containing the indexed subpath that make up the path for this
   * context.
   */
   private List path;

   /**
   * Creates an empty field context
   */
   public FieldContext() {

      path = new ArrayList();

   }

   public FieldContext(String initialPath){

      this();
      addPath(initialPath);


   }

   /**
   * Creates a new FieldContext instance that is a clone of this one,
   * and appends the given path to the new context. The path may be delimited
   * by dots (e.g. "root.element.name" ). If the path is null or has no value,
   * then <code>this</code> FieldContext is returned unchanged.
   */
   public FieldContext append(String newSubpath){

      FieldContext result = this;

      if( StringUtils.hasValue(newSubpath) ){

         result = (FieldContext) clone();
         result.addPath(newSubpath);

      }

      return result;

   }

   /**
   * Append a new subpath with the given name.
   */
   private void addPath(String subpath){

      StringTokenizer tokens = new StringTokenizer(subpath, ".");

      String nodeName;
      int index;
      int openParen;
      int closeParen;

      while(tokens.hasMoreTokens()){

         nodeName   = tokens.nextToken();
         openParen   = nodeName.lastIndexOf("(");
         closeParen  = nodeName.lastIndexOf(")");

         if( openParen != -1 ){

            try{

               String indexString = nodeName.substring(openParen+1, closeParen);
               nodeName =  nodeName.substring(0, openParen);
               index = Integer.parseInt(indexString);
               path.add( new Subpath(nodeName, index) );

            }
            catch(Exception ex){

               Debug.log(Debug.ALL_WARNINGS,
                         "Could not get index for path: "+
                         nodeName+": "+ex.getMessage());

               path.add( new Subpath(nodeName) );

            }

         }
         else{
            path.add( new Subpath(nodeName) );
         }

      }

   }


   /**
   * Assigns the a new path to this context.
   */
   protected void setPath(List newPath){

      path = newPath;

   }

   /**
   * This increments the last subpath's index by one.
   */
   public void add(){

      add(1);

   }

   /**
   * This increments the last subpath's index by the given ammount.
   */
   public void add(int increment){

      int length = path.size();
      if(length > 0){
         ( (Subpath) path.get(length-1) ).add(increment);
      }

   }

   /**
   * Removes all subpaths from this path.
   */
   public void clear(){

      path.clear();

   }

   /**
   * Resets the indexes of all subpaths.
   */
   public void reset(){

      for(int i = 0; i < path.size(); i++){

         ((Subpath)path.get(i)).reset();

      }

   }

   public boolean isEmpty(){

      return path.isEmpty();

   }

   public Object clone(){

      FieldContext result = new FieldContext();

      List pathClone = new ArrayList();

      for(int i = 0; i < path.size(); i++){

         pathClone.add( ((Subpath)path.get(i)).clone() );

      }

      result.setPath(pathClone);
      
      return result;

   }

   /**
   * Returns the current XML path in XPath notation.
   * (e.g. "/root/container/element[1]").
   */
   public String getXPath(){

      StringBuffer result = new StringBuffer("/*");

      Subpath current;

      for(int i = 0; i < path.size(); i++){

         current = (Subpath) path.get(i);
         result.append("/");
         result.append(current.getXPath());

      } 

      return result.toString();

   }

   /**
   * Returns the current XML path as a relative path in XPath notation.
   * (e.g. "./container/element[1]").
   */
   public String getRelativeXPath(){

      StringBuffer result = new StringBuffer(".");

      Subpath current;

      for(int i = 0; i < path.size(); i++){

         current = (Subpath) path.get(i);
         result.append("/");
         result.append(current.getXPath());

      } 

      return result.toString();

   }   

   /**
   * Returns the current XML path in dotted NightFire notation.
   * (e.g. "container.element(0)").
   */
   public String getNFPath(){

      StringBuffer result = new StringBuffer();

      Subpath current;

      for(int i = 0; i < path.size(); i++){

         current = (Subpath) path.get(i);
         result.append(".");
         result.append(current.getNFPath());

      }

      if(result.length() == 0){
         result.append(".");
      }

      return result.toString();

   }

   /**
   * Returns the path created by <code>getNFPath()</code>.
   */
   public String toString(){

     return getNFPath();

   }


   private class Subpath implements Cloneable{

      private int index;

      private String subpath;

      public Subpath(String path){

         subpath = path;
         reset();

      }

      public Subpath(String path, int startIndex){

         subpath = path;
         index = startIndex;

      }

      /**
      * Returns the XPath location represented by this subpath.
      */
      public String getXPath(){

         return subpath+"["+(index+1)+"]";

      }

      /**
      * Returns the indexed NightFire XML path represented by this subpath.
      */      
      public String getNFPath(){

         String result = subpath;

         if(index > 0){
            result += "("+index+")";
         }

         return result;


      }

      public String toString(){

         return getNFPath();

      }

      public int getIndex(){

         return index;

      }

      /**
      * Increments the index by 1.
      */
      public void add(){

         index++;

      }

      /**
      * Increments the index by the given ammount.
      */
      public void add(int increment){

         index += increment;

      }      

      /**
      * Resets the index to 0.
      */
      public void reset(){

         index = 0;

      }

      /**
      * Returns a new Subpath instance with this Subpath's current path and
      * index.
      */
      public Object clone(){

         return new Subpath(subpath, index);

      }

   }

}
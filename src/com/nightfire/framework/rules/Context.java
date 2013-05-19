package com.nightfire.framework.rules;

import java.util.*;
import java.io.StringReader;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.message.util.xml.CachingXPathAccessor;

import org.w3c.dom.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class provides functionality that is useful when a node (or nodes)
* in a rule's context repeats. When a Context String describes more than
* one node, this class can be used to generate a distinct, indexed path to
* each of those nodes.
*/
public class Context{

  public static final String XPATH_INDEX_BRACKET = "[";

  public static final String XPATH_WILDCARD_CLOSING = "*]";  

  /**
  * If this String "[*]" is found in the XPath context, it will
  * indicate that the node at that location may repeat.
  */
  public static final String FOREACH_INDICATOR = XPATH_INDEX_BRACKET +
                                                 XPATH_WILDCARD_CLOSING;

  /**
  * The list of node names and counters for this Context.
  */
  private List parsedContext = new ArrayList();

  /**
  * The source message which will be checked for repetitive nodes.
  */
  private XPathAccessor source;

  /**
  * This is a handle on the original context value. This will be returned
  * once from <code>getNextPath</code> when there were no "foreach" portions
  * indicated in the original context. 
  */
  private String originalContext = null;

  /**
  * The current counter. This is the counter that will be incremented next in
  * the search for repetitive nodes.
  */
  private Counter current = null;

  /**
  * This constructs a Context object from the given context String and for the
  * given source xml.
  *
  * @param context The context string given in a Rule definition. This string
  *                may contain "[*]" markers indicating that some nodes may
  *                repeat.
  * @param xml     The source XML input message.
  *
  * @exception FrameworkException if the xml cannot be parsed.
  */
  public Context(String context, String xml) throws FrameworkException{

     this(context,  new CachingXPathAccessor( xml ) );

     source.useInternalCaching( true );
  }


  /**
  * This constructs a Context object from the given context and for the
  * given source accessor.
  *
  * @param context The context string given in a Rule definition. This string
  *                may contain "[*]" markers indicating that some nodes may
  *                repeat.
  * @param accessor An XPathAccessor to the parsed XML input message.
  */  
  public Context(String context, XPathAccessor accessor){

     source = accessor;


     int start = 0;
     int wildcardIndex = context.indexOf(FOREACH_INDICATOR);

     while(wildcardIndex != -1){

        parsedContext.add( context.substring(start, wildcardIndex) );

        // Add Counter a object for each of the [*]'s in the XPath context         
        Counter counter = new Counter();
        if(current != null){
           current.setNext( counter );
        }
        current = counter;
        parsedContext.add( current );

        start = wildcardIndex + FOREACH_INDICATOR.length();

        wildcardIndex = context.indexOf( FOREACH_INDICATOR, start );
        
     }

     // add the remainder of the context string
     parsedContext.add( context.substring( start ) );

     // If there were no counters, then save a reference to the given
     // context
     if( current == null ){

        originalContext = context;

     }

  }

  /**
  * This method steps through each existing node described by this context
  * and returns an indexed XPath that is distinct to each of these nodes.
  *
  * <br>For example, with this original context String:
  * <code>/root/grandparent[*]/parent[*]/child[*]</code>
  * and the following XML:
  * <pre>
      <?xml version="1.0"?>
      <root>
         <grandparent>
            <parent>
               <child value="one" />
               <child value="two" />
            </parent>
            <parent>
               <child value="three" />
            </parent>
         </grandparent>
         <grandparent>
            <parent>
               <child value="five" />
            </parent>
            <parent>
               <child value="six" />
               <child value="seven" />
            </parent>
         </grandparent>
      </root>
    </pre>
  *
  * ... the first time this method is called, it will return a path of:<br>
  * <code>/root/grandparent[1]/parent[1]/child[1]</code><br>
  * ... and then ... <br>
  * <code>/root/grandparent[1]/parent[1]/child[2]</code><br>
  * ... and so on until all existing paths are exausted:<br>
  * <code>/root/grandparent[1]/parent[2]/child[1]</code><br>
  * <code>/root/grandparent[2]/parent[1]/child[1]</code><br>
  * <code>/root/grandparent[2]/parent[2]/child[1]</code><br>
  * <code>/root/grandparent[2]/parent[2]/child[2]</code><br>
  *
  * @return An indexed XPath String specifying the next node that exists for
  *         this Context. When all iterative paths have been exausted,
  *         this method will return null.
  */
  public String getNextPath(){

     String result = null;

     // Check to see if we are done iterating
     if(current == null){

        if(originalContext != null){
           // There were no repeating sections in the originalContext,
           // so just return that context.
           result = originalContext;
           originalContext = null;
        }
        else{
           // All of the paths described by this context have been exhausted
           result = null;
        }

        return result;
        
     }

     // If result is not null, then we have found a path with an existing node.
     // If current == null, then we have iterated through all
     // Counters and exhausted them. Either way, we're done.
     while( result == null && current != null ){

        // check to see if current path exists
        if( exists( pathToCurrent() ) ){

           // found an existing node 
           result = toString();
           // Set the last counter as the current
           current = current.getTail();

        }
        else{
           // if path doesn't exist, reset this counter and all below it,
           // go to the previous counter.
           // This previous counter will then get incremented before we loop.
           current.reset();
           current = current.getPrevious();

        }

        if(current != null){
           current.count();
        }

     }

     return result;

  }

  /**
  * Checks the source to see if a Node exists at the given path.
  */
  private boolean exists(String path){

     return source.nodeExists(path);

  }

  public boolean isIterative(){

     // Checks to see if there is a counter at all.
     // If there is, then this context still has iterative sections.
     return !(current == null);

  }

  /**
  * Returns the XPath up to and including the current counter. This is
  * used to check to see if a parent repetitive node exists before
  * bothering to look for its children.
  */
  private String pathToCurrent(){

     StringBuffer result = new StringBuffer();

     Object next;

      for (Object aParsedContext : parsedContext) {
          next = aParsedContext;
          result.append(next);

          // Stop once we've reached the current counter
          if (next == current) {
              break;
          }

      }

      return result.toString();

  }

  /**
  * Returns the current value of this context as a XPath String including the
  * current index values.
  */
  public String toString(){

     StringBuffer result = new StringBuffer(parsedContext.size());

     for (Object aParsedContext : parsedContext) {
          result.append(aParsedContext);
      }

      return result.toString();

  }

  /**
  * The Counter class is used to keep count at a particular nesting level
  * in the context.
  */
  class Counter {

     /**
     * This Counter is part of a two-way linked-list with the other
     * Counters in this Context. This is a reference to the counter that
     * comes before this one in this list.
     */
     private Counter prev = null;

     /**
     * This Counter is part of a two-way linked-list with the other
     * Counters in this Context. This is a reference to the counter that
     * comes after this one in this list.
     */
     private Counter next = null;

     /**
     * This Counter's current count.
     */
     private int count = 1;

     /**
     * Add one to the current count.
     */
     void count(){
        count++;
     }

     /**
     * Reset the count for this and all proceeding Counters to 1.
     */
     void reset(){

        // The XPath indexing is one-based.
        // Reset the count to 1.
        count = 1;

        // reset all proceeding Counters
        if(next != null){
           next.reset();
        }
        
     }

     /**
     * Get the Counter value in a form that can be used in an XPath as
     * an index. This is the current count sandwiched between two square
     * brackets
     */
     public String toString(){

        return "["+count+"]";

     }

     /**
     * Gets the Counter that precedes this one.
     */
     public Counter getPrevious(){
        return prev;
     }

     /**
     * Gets the Counter that comes after this one.
     */
     public Counter getNext(){
        return next;
     }

     /**
     * Gets the last Counter in this linked-list of Counters.
     */
     public Counter getTail(){

        Counter result = this;
        while(result.getNext() != null){
           result = result.getNext();
        }

        return result;

     }

     /**
     * Sets the Counter that comes after this one.
     */
     public void setNext(Counter child){

        next = child;
        child.prev = this;

     }

  }

  /**
  * This is for testing. It lists all of the indexed XPaths that can be found
  * for in the given file for the given
  * XPath context. If there are no repeating elements of the XPath
  * context, then the original context is printed as is.  
  */
  public static void main(String[] args){

      String usage = "java "+Context.class.getName()+" <xml file> <xpath context>";

      if(args.length != 2){

         System.err.println(usage);
         System.exit(-1);

      }

      System.out.println("Reading file ["+args[0]+"]");

      try{

         String xml = FileUtils.readFile( args[0] );

         Context test = new Context(args[1], xml);
         String path = null;

         do{
            path = test.getNextPath();
            System.out.println("Current Path: "+path);
         }while(path != null);

      }
      catch(Exception ex){

         ex.printStackTrace();

      }

  }

} 

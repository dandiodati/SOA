package com.nightfire.framework.rules;

import java.util.*;
import java.lang.reflect.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.Node;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class provides a set of standard functions that are useful when defining
* rules. This class is extended by RuleEvaluatorBase, thus providing all of
* these standard functions for use by generated rule Evaluators.
*
*/
public abstract class StandardFunctions {

   /**
   * An XPATH constant indicating the parent node.
   */
   private static String PARENT    = "..";

   /**
   * An XPATH wildcard indicating that this subpath can be found anywhere
   * in the XML message.
   */
   private static String WILD      = "//";


   /**
   * The XPATH path separator.
   */
   private static String SEPARATOR = "/";

   /**
   * The current parsed XML source on which member functions such as <code>
   * value()</code> and <code>present()</code> will operate.
   */
   protected XPathAccessor source = null;

   /**
   * The current XPATH context which member functions such as <code>
   * value()</code> and <code>present()</code> will use.
   */
   protected String context;

   /**
   * This is used to cache compiled XPath instances.
   */
   private Map xpathCache = new HashMap();


   /**
   * This is used to cache the values in lookup for the next node iteration.
   */
   private HashMap lookupCache = null;

   /**
    * This is used by the unique() method to cache whether a given location
    * is unique or not. The key for this map is the XPath location, the
    * value is a Boolean indicating whether it is unique or not.
    */
   private HashMap uniqueLocationMap = null;


    protected Map getXpathCache() {
        return xpathCache;
    }

   /**
   * Assigns the current XML source message on which member functions such as
   * <code>value()</code>, <code>present()</code>, etc. will operate.
   *
   * @param src An XML message.
   */
    public void setSource(String src) throws FrameworkException 
    {
        XPathAccessor xpa = new XPathAccessor( src );

        xpa.useInternalCaching( true );

        setSource( xpa );
    }

    /**
   * Assigns the current parsed XML source on which member functions such as <code>
   * value()</code>, <code>present()</code>, etc. will operate.
   *
   * @param src An XPathAccessor to a parsed XML.
   */
   public void setSource(XPathAccessor src){

      source = src;

   }

   /**
   * Assigns the current XPATH context value which member functions such as <code>
   * value()</code>, <code>present()</code>, etc. will use.
   *
   * @param ctx An XPATH path.
   */
   public void setContext(String ctx){

      this.context = ctx;

   }

   /**
   * Gets the current XPATH context.
   *
   * @return ctx An XPATH path.
   */
   public String getContext(){

      return context;

   }

   /**
   * Gets the given XPATH location as a path relative to the given XPATH context.
   * If <code>location</code> is an absolute path, then it is returned
   * unchanged. Otherwise, for every leading ".." in the location, one
   * level of nesting is chopped off of the context value, and the ".." is
   * dropped from the location. When all of the ".."'s have been exausted,
   * the context and the location are concatenated together and returned.
   *
   * @param location An XPATH location.
   * @param context  An XPATH to which the location should be appended.
   *
   * @return an XPATH String that is equivalent to the given
   * <code>location</code> within the given <code>context</code>.
   */
   public static String getLocationInContext(String location, String context){

      // check for an absolute path
      if( location.startsWith( WILD ) || location.startsWith( SEPARATOR ) ){

         return location;

      }

      while( location.startsWith(PARENT) ){

         location = location.substring(PARENT.length());
         if( location.startsWith(SEPARATOR) ){

            if(location.length() == SEPARATOR.length()){
               // if the location IS just the SEPARATOR
               location = "";
            }
            else{
               location = location.substring(SEPARATOR.length());
            }

         }

         if( ! (context.equals( WILD ) || context.equals( SEPARATOR ) ) ){

            if( context.endsWith(SEPARATOR) ){
               context = context.substring(0,
                                           context.length()-2);
            }

            int lastSlash = context.lastIndexOf(SEPARATOR);

            if( lastSlash != -1 ){

               context = context.substring(0, lastSlash+1);

            }

         }

      }

      if(! context.endsWith(SEPARATOR) ){
         context += SEPARATOR;
      }

      if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
      Debug.log(Debug.RULE_EXECUTION, "Location: ["+context+location+"]");

      return context+location;

   }

   /**
   * This checks to see if the current context location is present in
   * the current message source. "Present" is defined as having an XML node
   * that exists at this location. If the node has a "value" attribute,
   * the value must not be an empty String "".
   *
   * @return true if the current context node is present, false otherwise.
   */
   public boolean present(){

      return present(".");

   }

   /**
   * This checks to see if the given location, relative to the current context,
   * is present in the current message source.
   * "Present" is defined as having an XML node
   * that exists at this location. If the node has a "value" attribute,
   * the value must not be an empty String "".
   *
   * @param location The XPATH location whose presence we want to check.
   *
   * @return true if the location is present in the current context,
   *         false otherwise.
   */
   public boolean present(String location){

      return present(location, context, source, xpathCache);

   }

   /**
   * This checks to see if the given location the given context location is
   * present in the given message. "Present" is defined as having an XML node
   * that exists at this location. If the node has a "value" attribute,
   * the value must not be an empty String "".
   *
   * @param location The XPATH location whose presence we want to check.
   * @param context The XPATH context.
   * @param message The source message in which to find the location.
   *
   * @return true if the current context node is present, false otherwise.
   */
   public static boolean present(String location,
                                 String context,
                                 XPathAccessor message){

      return present(location, context, message, DummyMap.getInstance() );

   }

   /**
   * This checks to see if the given location the given context location is
   * present in the given message. "Present" is defined as having an XML node
   * that exists at this location. If the node has a "value" attribute,
   * the value must not be an empty String "".
   *
   * @param location The XPATH location whose presence we want to check.
   * @param context The XPATH context.
   * @param message The source message in which to find the location.
   * @param cache   This Map is used to cache XPath instances. This
   *                saves us from having to reparse an XPath string when
   *                this instance is reused.
   *
   * @return true if the current context node is present, false otherwise.
   */
   public static boolean present(String location,
                                 String context,
                                 XPathAccessor message,
                                 Map cache){

      boolean success = false;
      List nodes;

      try{

        location = getLocationInContext(location, context);
        nodes = message.getList(location, cache);
        int count = nodes.size();
        success = ( count > 0);

        if( Debug.isLevelEnabled(Debug.MSG_PARSE) && count > 1){

           Debug.log(Debug.MSG_PARSE,
                     "More than one node found for location: ["+location+"]");

        }

        // If the node exists, check to make sure that it has a value that
        // is not equal to "". Nodes with no "value" attribute are also
        // considered to be "present"
        if(success){

           Node node = (Node) nodes.get(0);
           String value = null;

           try{
              // If this is a value node, then get the value and check it...
              if( XMLMessageBase.isValueNode( node ) ){
                 value = XMLMessageBase.getNodeValue( node );
              }
              else if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

                 Debug.log(Debug.RULE_EXECUTION,
                           "Location ["+location+
                           "] is present but has no value attribute");

              }

           }
           catch(MessageException mex){

              if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

                 Debug.log(Debug.RULE_EXECUTION,
                           "Could not get value for location ["+location+
                           "]: "+RuleUtils.getExceptionMessage(mex));

              }

           }

           // If there is a value, it can't be empty
           if(value != null){

              if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

                 Debug.log(Debug.RULE_EXECUTION,
                           "Location ["+location+
                           "] value equals ["+value+"]");

              }
              success = !value.equals("");
           }


        }

      }
      catch(Exception ex){

            Debug.log(Debug.ALL_ERRORS,
                      "An error occurred while checking for the presence of location ["
                      +location+
                      "]: "+
                      RuleUtils.getExceptionMessage(ex) );
      }

      if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

         Debug.log(Debug.RULE_EXECUTION, "Location ["+location+
                                         "] present : ["+
                                         success+"]");

      }

      return success;

   }


   /**
   * This checks to see if the current context location is absent from
   * the current message source. "Absent" is defined as having no XML node
   * that exists at this location. A node is also considered absent if
   * it has a "value" attribute, but the value of the attribute is an
   * empty String "".
   *
   * @return true if the current context node is absent, false otherwise.
   */
   public boolean absent(){

      return absent(".", context, source, xpathCache);

   }

   /**
   * This checks to see if the given location is absent from
   * the current message source. "Absent" is defined as having no XML node
   * that exists at this location. A node is also considered absent if
   * it has a "value" attribute, but the value of the attribute is an
   * empty String "".
   *
   * @param location The XPATH location whose absence we want to check.
   *
   * @return true if the current context node is absent, false otherwise.
   */
   public boolean absent(String location){

      return absent(location, context, source, xpathCache);

   }

   /**
   * This checks to see if the given location is absent from
   * given context of the given message source.
   * "Absent" is defined as having no XML node
   * that exists at this location. A node is also considered absent if
   * it has a "value" attribute, but the value of the attribute is an
   * empty String "".
   *
   * @param location The XPATH location whose absence we want to check.
   * @param context The XPATH context.
   * @param message The source message in which to check for the location.
   */
   public static boolean absent(String location,
                                String context,
                                XPathAccessor message){

      return absent(location, context, message, DummyMap.getInstance() );

   }

   /**
   * This checks to see if the given location is absent from
   * given context of the given message source.
   * "Absent" is defined as having no XML node
   * that exists at this location. A node is also considered absent if
   * it has a "value" attribute, but the value of the attribute is an
   * empty String "".
   *
   * @param location The XPATH location whose absence we want to check.
   * @param context The XPATH context.
   * @param message The source message in which to check for the location.
   * @param cache   This Map is used to cache XPath instances. This
   *                saves us from having to reparse an XPath string when
   *                this instance is reused.
   */
   public static boolean absent(String location,
                                String context,
                                XPathAccessor message,
                                Map cache){

      boolean success = false;

      try{

        success = ! present(location, context, message, cache);

      }
      catch(Exception ex){

         Debug.log(Debug.ALL_ERRORS,
                   "An error occurred while checking for the absence of location ["
                   +location+
                   "]: "+
                   RuleUtils.getExceptionMessage(ex) );

      }

      Debug.log(Debug.RULE_EXECUTION, "Location ["+location+"] absent : ["+
                                      success+"]");

      return success;

   }

   /**
   * This returns the value of the current context path.
   *
   * @return The value of the node at the current context location or a value
   *         equal to the empty String, "", if the context node has no value.
   */
   public Value value(){

      return value(".", context, source, xpathCache);

   }

   /**
   * This returns the value of the given location. If the location is a relative
   * path, it will be relative to the current context.
   *
   * @param location The path to the node whose value we want.
   *
   * @return The value of the node at the given location current context location.
   *         A value equal to the empty String, "", is returned if the
   *         location does not exist or if the located
   *         node has no value.
   */
   public Value value(String location){

      return value(location, context, source, xpathCache);

   }

   /**
   * This returns the value of the given location. If the location is a relative
   * path, it will be relative to the given <code>aContext</code>.
   *
   * @param location The path to the node whose value we want.
   * @param aContext This XPath context is used instead of the current context.
   *
   * @return The value of the node at the given location relative to the given
   *         context location.
   *         A value equal to the empty String, "", is returned if the
   *         location does not exist or if the located
   *         node has no value.
   */
   public Value value(String location, String aContext){

      return value(location, aContext, source, xpathCache);

   }


   /**
   * This returns the value of the given location. If the location is a relative
   * path, it will be relative to the current context.
   *
   * @param location The path to the node whose value we want.
   * @param context  The context path to use.
   * @param message  The parsed message in which the location should be found.
   *
   * @return The value of the node at the given location and given context within
   *         the given message.
   *         A value equal to the empty String, "", is returned if the given
   *         location does not exist or if the located
   *         node has no value.
   */
   public static Value value(String location,
                             String context,
                             XPathAccessor message){

      return value(location, context, message, DummyMap.getInstance() );

   }

   /**
   * This returns the value of the given location. If the location is a relative
   * path, it will be relative to the current context.
   *
   * @param location The path to the node whose value we want.
   * @param context  The context path to use.
   * @param message  The parsed message in which the location should be found.
   * @param cache    This Map is used to cache XPath instances. This
   *                 saves us from having to reparse an XPath string when
   *                 this instance is reused.
   *
   *
   * @return The value of the node at the given location and given context within
   *         the given message.
   *         A value equal to the empty String, "", is returned if the given
   *         location does not exist or if the located
   *         node has no value.
   */
   public static Value value(String location,
                             String context,
                             XPathAccessor message,
                             Map cache){

      String result = null;

      try{

         location = getLocationInContext(location, context);

         List nodes = message.getList(location, cache);

         int count = nodes.size();

         if( count > 0 ){

            if( Debug.isLevelEnabled(Debug.MSG_PARSE) && count > 1){

               Debug.log(Debug.MSG_PARSE,
                         "More than one node found for location: ["+
                         location+"]");

            }

            Node node = (Node) nodes.get(0);

            if(XMLMessageBase.isValueNode(node)){
               result = XMLMessageBase.getNodeValue( node );
            }
            else{
               // the XPath referred to an attribute node or some other
               // node that isn't a "value" node
               result = node.getNodeValue();
            }

         }
         else if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "Could not find value for location ["+location+"]");
         }

      }
      catch(FrameworkException fex){

         if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "Could not find value for location ["+location+"]: "+
                      RuleUtils.getExceptionMessage(fex) );
         }

      }

      result = (result == null) ? "" : result;

      if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
         Debug.log(Debug.RULE_EXECUTION, "Value of location ["+location+
                                         "] : ["+
                                         result+"]");
      }

      return new Value(result);

   }

   /**
   * This method provides functionality for looking up a particular
   * repeating node based on its value.
   * Usage example: <code>lookup("/container/item[*]/name", value() )</code>.
   * This example returns the path to a "name" node with same value as the
   * current context node.
   * THIS METHOD USES THE LOOKUPMAP TO IMPROVE PERFORMANCE.
   *
   * @see #lookup(String, String)
   * @param context The repetitive XPath context to check for the given value.
   *                Repeating nodes in this XPath should be marked with "[*]".
   *                For example, the path: <code>/root/container/item[*]/name</code>
   *                indicates that the <code>item</code> node repeats, and that
   *                <code>lookup</code> should check all of the <code>item</code>
   *                nodes to see if their <code>name</code> node has the given
   *                value.
   * @param lookupValue An instance of the Value class that is returned from
   *                    calls to the <code>value</code> functions. This is the
   *                    node value that lookup will be looking for.
   * @return the distinct path to the first found node that has the given
   * <code>lookupValue</code>. This is returned as an instance of a LookupPath
   * object. This LookupPath can be used with certain versions of the
   * <code>value</code>, <code>present</code>, and <code>absent</code> methods.
   *
   */
  public LookupPath lookup(String context, Value lookupValue){

     if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
        Debug.log(Debug.RULE_EXECUTION, "lookup: Looking for value ["+
                                        lookupValue+
                                        "] in context ["+
                                        context+"]");
     }


     if(lookupCache==null)
        lookupCache = new HashMap();

     Map lookupMap = (Map) lookupCache.get(context);

     if ( lookupMap == null )
     {
         lookupMap = initializeLookupMap(context);
     }

     LookupPath result;

     String path = (String) lookupMap.get(lookupValue);

     if ( path != null )
     {

         if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
             Debug.log(Debug.RULE_EXECUTION, "lookup: Found path [" +
                       path +
                       "] with value [" +
                       lookupValue + "].");
         }

         result = new LookupPath( path );

     }
     else {
         if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
             Debug.log(Debug.RULE_EXECUTION,
                       "lookup: No path found in context [" +
                       context +
                       "] with value [" +
                       lookupValue + "].");
         }
         result = new LookupPath();
     }
     return result;
  }


  /**
  * This method populates the lookupMap and sets the lookupContext based
  * on the input context. The method goes through all the paths in the context
  * and populates the lookupMap with these paths using the values as indicies
  * for the HaspMap.
  *
  * @see #lookup(String, String)
  * @param context The repetitive XPath context to check for the given value.
  *                Repeating nodes in this XPath should be marked with "[*]".
  *                For example, the path: <code>/root/container/item[*]/name</code>
  *                indicates that the <code>item</code> node repeats, and that
  *                <code>lookup</code> should check all of the <code>item</code>
  *                nodes to see if their <code>name</code> node has the given
  *                value.
  *
  */
  private Map initializeLookupMap( String context )
  {

      Map lookupMap = new HashMap();
      Context locations = new Context(context, source);
      String currentPath = locations.getNextPath();
      Value pathValue;

      // While there are still paths left, and while we have
      // not found a result.
      while(currentPath != null){

         pathValue = value(currentPath, currentPath, source);

         if( pathValue.hasValue() ){

            if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
               Debug.log(Debug.RULE_EXECUTION, "initializeLookupMap: Placing value ["+
                                               pathValue+
                                               "] for path ["+
                                               currentPath+"] into the lookupMap.");
            }

            lookupMap.put(pathValue.toString(), currentPath);

         }

         currentPath = locations.getNextPath();

      }

      if(lookupCache==null)
        lookupCache = new HashMap();
      // map the given context to the newly created lookup map
      lookupCache.put( context, lookupMap );

      return lookupMap;

  }

  /**
  * This method clears the lookup cache so that the cache will be recreated
  * from the next message that the rule is executed on.
  */
  public void cleanup()
  {
      if(lookupCache!=null)
      lookupCache.clear();

      if(uniqueLocationMap!=null)
      uniqueLocationMap.clear();

      if ( source != null )
          source.cleanup( );
  }

   /**
   * This method provides functionality for looking up a particular
   * repeating node based on its value. So given a value and a repetitive
   * XPath context to check, this method will return the distinct, indexed
   * XPath to the node with a matching value (if one exists). If no node exists
   * in the given <code>context</code> with the the given
   * <code>lookupValue</code>, then the returned path is flagged as "not found".
   *
   *   <p><b>Usage:</b>
   *   <p>Given this sample XML:
   *   <pre>
   *
   *   &lt;root>
   *      &lt;somedetail_container>
   *          &lt;somedetail>
   *            &lt;NUM value="111"/>
   *         &lt;/somedetail>
   *         &lt;somedetail>
   *            &lt;NUM value="222"/>
   *         &lt;/somedetail>
   *         &lt;somedetail>
   *            &lt;NUM value="333"/>
   *         &lt;/somedetail>
   *     &lt;/somedetail_container>
   *     &lt;somedetails_container>
   *        &lt;somedetails>
   *           &lt;NUM value="111"/>
   *           &lt;FOO value="AAA"/>
   *        &lt;/somedetails>
   *        &lt;somedetails>
   *           &lt;NUM value="222"/>
   *           &lt;FOO value="BBB"/>
   *        &lt;/somedetails>
   *        &lt;somedetails>
   *           &lt;NUM value="333"/>
   *        &lt;/somedetails>
   *     &lt;/somedetails_container>
   *  &lt;/root>
   *
   *  </pre>
   *
   *  <p>Given this sample XML, here are some ways that the <code>lookup</code>
   *  function can be used:
   *  <pre>
   *     lookup("/root/somedetails_container/somedetails/NUM", "222");
   *  </pre>
   *  <p>This returns the discrete path to the node that has the value "222"
   *  which is:
   *  <pre>
   *     /root/somedetails_container/somedetails[2]/NUM
   *  </pre>
   *  <p>This can be used to synchronize with other node values.
   *  Assume that the XPath context for the following rule is
   *  <code>/root/somedetail_container/somedetail[*]/NUM</code>:
   *  <pre>
   *     lookup("/root/somedetails_container/somedetails/NUM", value() )
   *  </pre>
   *  <p><code>lookup</pre> will return the path to the
   *  <code>/root/somedetails_container/somedetails/NUM</code> node
   *  that has the same value as the current
   *  <code>/root/somedetail_container/somedetail/NUM</code> node. And this
   *  rule will get evaluated for each
   *  <code>/root/somedetail_container/somedetail/NUM</code>
   *  node that exists. So given the sample XML above, this <code>lookup</code>
   *  will get evaluated 3 times and return these paths:
   *  <pre>
   *     /root/somedetails_container/somedetails[1]/NUM
   *     /root/somedetails_container/somedetails[2]/NUM
   *     /root/somedetails_container/somedetails[3]/NUM
   *  </pre>
   *  <p>So a check like:
   *  <pre>
   *     present( lookup("/root/somedetails_container/somedetails/NUM", value() ) )
   *  </pre>
   *  ... can be done. This checks to make sure that for each context node,
   *  ("/root/somedetail_container/somedetail/NUM" in the example above), there
   *  is also a "/root/somedetails_container/somedetails/NUM" node
   *  with the same value.
   *
   *  A check could also be done on the nodes relative to the found
   *  path like:
   *
   *     present( "../FOO", lookup("/root/somedetails_container/somedetails/NUM", value() ) )
   *
   *  This looks up the "/root/somedetails_container/somedetails/NUM" path that has
   *  the same value as the current context node, and then, given that path, it looks
   *  for the relative node "../FOO" and checks to see if that node is present.
   *
   *  <p>So this checks to see if the following paths are present:
   *  <pre>
   *     /root/somedetails_container/somedetails[1]/NUM/../FOO
   *     /root/somedetails_container/somedetails[2]/NUM/../FOO
   *     /root/somedetails_container/somedetails[3]/NUM/../FOO
   *  </pre>
   *  <p>This <code>present</code> statement
   *  will return these results respectively when this
   *  statement is evaluated (given our sample XML above):
   *  <pre>
   *     true
   *     true
   *     false
   *  </pre>
   *
   * @param context The repetitive XPath context to check for the given value.
   *                Repeating nodes in this XPath should be marked with "[*]".
   *                For example, the path: <code>/root/container/item[*]/name</code>
   *                indicates that the <code>item</code> node repeats, and that
   *                <code>lookup</code> should check all of the <code>item</code>
   *                nodes to see if their <code>name</code> node has the given
   *                value.
   * @param lookupValue This is the node value that lookup will be looking
   *                    trying to match.
   *
   * @return the distinct path to the first found node that has the given
   * <code>lookupValue</code>. This is returned as an instance of a LookupPath
   * object. This LookupPath can be used with certain versions of the
   * <code>value</code>, <code>present</code>, and <code>absent</code> methods.
   *
   */
   public LookupPath lookup(String context, String lookupValue){

      return lookup(context, value(lookupValue));

   }


   /**
   * This method provides functionality for looking up a particular
   * repeating node in a given parsed XML source based on its value.
   * THIS METHOD DOES NOT USE THE LOOKUPMAP
   *
   * @see #lookup(String, String)
   * @param context The repetitive XPath context to check for the given value.
   *                Repeating nodes in this XPath should be marked with "[*]".
   *                For example, the path: <code>/root/container/item[*]/name</code>
   *                indicates that the <code>item</code> node repeats, and that
   *                <code>lookup</code> should check all of the <code>item</code>
   *                nodes to see if their <code>name</code>
   * @param lookupValue This is the node value that lookup will be looking
   *                    trying to match.
   * @param source A parsed XML source where this lookup will be done.
   *
   * @return the distinct path to the node that has the given
   * <code>lookupValue</code>. This is returned as an instance of a LookupPath
   * object. This LookupPath can be used with certain versions of the
   * <code>value</code>, <code>present</code>, and <code>absent</code> methods.
   *
   */
   public static LookupPath lookup(String context,
                                   String lookupValue,
                                   XPathAccessor source){

      if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
         Debug.log(Debug.RULE_EXECUTION, "Looking for value ["+
                                         lookupValue+
                                         "] in context ["+
                                         context+"]");
      }

      LookupPath result = null;
      Value foundValue;
      Context locations = new Context(context, source);
      String currentPath = locations.getNextPath();

      // While there are still paths left, and while we have
      // not found a result.
      while(currentPath != null && result == null){

         if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION, "Looking for value ["+
                                            lookupValue+
                                            "] in path ["+
                                            currentPath+"]");
         }

         foundValue = value(currentPath, currentPath, source);

         if( foundValue.hasValue() && foundValue.equals( lookupValue ) ){

            if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
               Debug.log(Debug.RULE_EXECUTION, "Found path ["+
                                               currentPath+
                                               "] with value ["+
                                               lookupValue+"]");
            }

            result = new LookupPath( currentPath );

         }

         currentPath = locations.getNextPath();

      }

      if(result == null){

         if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
               Debug.log(Debug.RULE_EXECUTION, "No path found in context ["+
                                               context+
                                               "] with value ["+
                                               lookupValue+"]");
         }
         // Return an empty "NOT_FOUND" LookupPath
         result = new LookupPath();

      }

      return result;

   }

   /**
   * Gets the value of the given XPath <code>location</code> relative to the
   * given LookupPath.
   *
   * <p>Usage example:
   * <code>value( "../VER", lookup("/section_container/section[*]/PON", "123") )</code>
   * This example looks up the path to the PON with the value "123", and then
   * retrieves the value of its sister node "VER".
   *
   * @param location a ralative XPath whose value will be retrieved.
   * @param foundPath A LookupPath returned from a call to <code>lookup</code>.
   *
   * @see #lookup(String, String)
   *
   * @return The value of the node at the given location relative to the
   *         given <code>foundPath</code>.
   *         A value equal to the empty String, "", is returned if the given
   *         location does not exist or if the located
   *         node has no value, or if the given <code>foundPath</code> is
   *         marked as not found.
   */
   public Value value(String location, LookupPath foundPath){

      if( foundPath.isNotFound() ){

         return new Value();

      }

      return value(location, foundPath.getPath());

   }

   /**
   * Checks to see if any matching path was found from a call to
   * <code>lookup</code>.
   *
   * @see #lookup(String, String)
   *
   * @param foundPath A LookupPath returned from a call to <code>lookup</code>.
   *
   * @return false if the <code>foundPath</code> is marked as not found, true
   *         otherwise.
   */
   public boolean present(LookupPath foundPath){

      return ! foundPath.isNotFound();

   }

   /**
   * Checks to see if the given XPath <code>location</code> is present
   * relative to the given LookupPath.
   *
   * @param location a ralative XPath whose presence will be checked.
   * @param foundPath A LookupPath returned from a call to <code>lookup</code>.
   *
   * @see #lookup(String, String)
   *
   * @return true if the <code>foundPath</code> is note marked as not found
   *         and if the given <code>location</code> relative to the
   *         <code>foundPath</code> is present,
   *         false otherwise.
   */
   public boolean present(String location, LookupPath foundPath){

      if( foundPath.isNotFound() ){
         return false;
      }

      return present(location, foundPath.getPath(), source, xpathCache);

   }

   /**
   * Checks to see that there were no matching paths found from a call to
   * <code>lookup</code>.
   *
   * @see #lookup(String, String)
   *
   * @param foundPath A LookupPath returned from a call to <code>lookup</code>.
   *
   * @return true if the <code>foundPath</code> is marked as not found, false
   *         otherwise.
   */
   public boolean absent(LookupPath foundPath){

      return foundPath.isNotFound();

   }


   /**
   * Checks to see if the given XPath <code>location</code> is absent
   * relative to the given LookupPath.
   *
   * @param location a ralative XPath whose absence will be checked.
   * @param foundPath A LookupPath returned from a call to <code>lookup</code>.
   *
   * @see #lookup(String, String)
   * @return true if the <code>foundPath</code> is marked as not found, or
   *         true if the given <code>location</code> relative to the
   *         <code>foundPath</code> is absent, false otherwise.
   */
   public boolean absent(String location, LookupPath foundPath){

      if( foundPath.isNotFound() ){

         return true;

      }

      return absent(location, foundPath.getPath(), source, xpathCache);

   }


    /**
    * This counts the number of nodes that match the current XPath
    * context.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to count ALL nodes matching this XPath.
    *
    * @return The number of nodes matching the current context XPath.
    */
    public int count(){

       return count(".");

    }

    /**
    * This counts the number of nodes that match the given XPath <code>
    * <code>location</code> (relative to the current XPath
    * context).
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to count ALL nodes matching this XPath.
    *
    * @param location the location whose nodes we want to count.
    *
    * @return The number of nodes matching the given <code>location</code>
    *         XPath.
    */
    public int count(String location){

       // Replace all specific indexes (e.g. [2]) with [*]
       String contextWithWildCards = replaceIndices(context);

       // Get location relative to the context
       location = getLocationInContext(location, contextWithWildCards);

       int count = 0;

       try{

          List nodes = source.getList(location, xpathCache);
          count = nodes.size();

       }
       catch(FrameworkException fex){

             Debug.log(Debug.ALL_ERRORS,"An error occurred while trying to count the nodes for path ["+
                         location+"]: "+RuleUtils.getExceptionMessage(fex));
       }

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION,
                    "Counted ["+count+
                    "] nodes for path ["+
                    location+"]");
       }

       return count;

    }

    /**
    * This counts the number of nodes that match the current XPath
    * context that have the given <code>match</code> value.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to count ALL nodes matching this XPath.
    *
    * @param match if the value of a node that matches the context path
    *              has this value, then it will be counted.
    *              This comparison will be
    *              case insensitive, so a Value of "CAT" will match "cat".
    *
    * @return The number of nodes matching the given <code>location</code>
    *         XPath with that have the given <code>match</value>.
    */
    public int count(Value match){

        return count(".", match);

    }

    /**
    * This counts the number of nodes that match the current XPath
    * context that have the given <code>match</code> value.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to count ALL nodes matching this XPath.
    *
    * @param location an XPath location to be counted
    * @param match if the value of a node
    *              equals this value, then it will be counted.
    *              This comparison will be
    *              case insensitive, so "CAT" will match "cat".
    *
    * @return The number of nodes matching the given <code>location</code>
    *         XPath with that have the given <code>match</value>.
    */
    public int count(String location, Value match){

       return count(location, match.toString());

    }


    /**
    * This counts the number of nodes that match the current XPath
    * context that have the given <code>match</code> value.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to count ALL nodes matching this XPath.
    *
    * @param location an XPath location to be counted
    * @param match if the value of a node equals this value,
    *              then it will be counted. This comparison will be
    *              case insensitive, so "CAT" will match "cat".
    *
    * @return The number of nodes matching the given <code>location</code>
    *         XPath with that have the given <code>match</value>.
    */
    public int count(String location, String match){

       // Replace all specific indexes (e.g. [2]) with [*]
       String contextWithWildCards = replaceIndices(context);

       // Get location relative to the context
       location = getLocationInContext(location, contextWithWildCards);

       int count = 0;

       try{

          List nodes = source.getList(location, xpathCache);
          int totalCount = nodes.size();

          for(int i = 0; i < totalCount; i++){

             try{

                String nodeValue = XMLMessageBase.getNodeValue( (Node) nodes.get(i) );
                if( nodeValue.equalsIgnoreCase(match) ){
                   count++;
                }

             }
             catch(MessageException mex){

                   Debug.log(Debug.ALL_ERRORS,
                             "Could not find value for node with index ["+i+
                             "] found at location ["+location+"]: "+
                             RuleUtils.getExceptionMessage(mex) );
             }

          }

       }
       catch(FrameworkException fex){

             Debug.log(Debug.ALL_ERRORS, "An error occurred while trying to count the nodes for path ["+
                         location+"]: "+RuleUtils.getExceptionMessage(fex));
       }

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION,
                    "Counted ["+count+
                    "] nodes for path ["+
                    location+"] with value ["+match+"]");
       }

       return count;

    }

    /**
    * Gets an array of all nodes in the given source that match the given
    * XPath location.
    *
    * @param location full XPath location.
    * @param src a parsed XML source.
    *
    * @return an array of all nodes that match the given location. If
    *         the location is not found, an array of length 0 will be
    *         returned.
    */
    protected static Node[] getNodes(String location, XPathAccessor src){

       Node[] nodes = new Node[0];

       try{

          nodes = src.getNodes(location);

       }
       catch(FrameworkException fex){

             Debug.log(Debug.ALL_ERRORS,
                       "Could not retrieve nodes for location ["+
                       location+"]: "+fex.getMessage());
       }

       return nodes;

    }

    /**
    * This checks to see if all nodes that match the current context path
    * have unique values.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to ALL nodes matching this XPath for uniqueness.
    *
    * @return true if all nodes matching the current context XPath
    *         have unique values, false if a value repeats.
    */
    public boolean unique(){

       return unique(".");

    }

    /**
    * This checks to see if all nodes that match the given <code>location</code>
    * (relative to the current context path) have unique values.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to ALL nodes matching this XPath for uniqueness.
    *
    * @param location An XPath (relative to the current context)
    *                 whose matching nodes will be checked for uniqueness.
    *
    * @return true if all nodes matching the current context XPath
    *         have unique values, false if a value repeats.
    */
    public boolean unique(String location){

       // Replace all specific indexes (e.g. [2]) with [*]
       String contextWithWildCards = replaceIndices(context);

       // Get location relative to the context
       location = getLocationInContext(location, contextWithWildCards);

       boolean result = false;

       if(uniqueLocationMap==null)
                uniqueLocationMap = new HashMap();
       Boolean uniqueness = (Boolean) uniqueLocationMap.get( location );

       if( uniqueness == null ){

          result = checkUniqueness( location );
          // cache the resulting value
          uniqueLocationMap.put( location, result );

       }
       else {

         result = uniqueness;

       }

       return result;

    }

    /**
    * This checks to see if all nodes that match the given <code>location</code>
    * (relative to the current context path) have unique values. This private
    * method differs from the unique() method in that this does
    * not use the cache to determine if a path is unique.
    *
    * <p>Caution: Any numbered indexes in the context will be replaced with
    *             wildcard. (e.g. "/root/container[3]/item[2]/name" will
    *             become "/root/container[*]/item[*]/name".) This is
    *             in order to ALL nodes matching this XPath for uniqueness.
    *
    * @param location An XPath (relative to the current context)
    *                 whose matching nodes will be checked for uniqueness.
    *
    * @return true if all nodes matching the current context XPath
    *         have unique values, false if a value repeats.
    */
    protected boolean checkUniqueness(String location){

         List nodes;

         try {
           nodes = source.getList(location, xpathCache);
         }
         catch (FrameworkException fex) {

             Debug.log(Debug.ALL_ERRORS,
                       "Could not retrieve nodes for location [" +
                       location + "]: " + fex.getMessage());
           return false;

         }

         int count = nodes.size();

         Set existingNodes = new HashSet(count);

         String value;

         for (int i = 0; i < count; i++) {

           // Get value of current node
           try {
             value = XMLMessageBase.getNodeValue( (Node) nodes.get(i) );
           }
           catch (MessageException mex) {

               Debug.log(Debug.ALL_ERRORS,
                         "Could not get value for node number [" +
                         (i + 1) + "] in path [" + location + "]: " +
                         mex.getMessage());

             value = "";

           }

           // Check to see if a node with this value exists
           if (existingNodes.contains(value)) {

             if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
               Debug.log(Debug.RULE_EXECUTION,
                         "All nodes found for path [" + location +
                         "] do not have unique values. The value [" +
                         value + "] is repeated.");
             }

             // the value is not unique
             return false;

           }

           // Add this value to the list of discovered values
           existingNodes.add(value);

         }

         if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
           Debug.log(Debug.RULE_EXECUTION,
                     "All nodes found for path [" + location +
                     "] have unique values.");
         }

         return true;

    }

    /**
    * This method takes that name of a method in the Value class, and executes
    * that method for the value of every node found that matches the given path.
    * <code>forAll</code> checks to make sure that the method returns true
    * for ALL found values.
    *
    * @param path An XPath representing a repeating node whose Values should
    *             all be checked.
    *             Repeating nodes in this XPath should be marked with "[*]".
    *             For example, the path: <code>/root/container/item[*]/name</code>
    *             indicates that the <code>item</code> node repeats.
    * @param method  A Method object representing a method in the Value class.
    *                The given method must return a boolean value and take
    *                the same number of arguments as there are elements
    *                of <code>args</code>.
    * @param args The parameters that will be passed to the given method when it
    *             is invoked.
    *
    * @return if the method with the given name returns true for ALL values
    *         in the given path, then this will return true. If the
    *         indicated method returns false for any value in the path, then
    *         <code>forAll</code> will return false. If a method cannot
    *         be invoked for any reason, then this will return false.
    */
    protected final boolean forAll(String path, Method method, Object[] args){

       boolean result = true;

       path = getLocationInContext(path, context);

       Context locations = new Context(path, source);
       String currentPath = locations.getNextPath();

       // While there are still paths left to check, and while the method
       // is still returning true for all values...
       while(currentPath != null && result){

          result = invokeMethod(currentPath, method, args);
          currentPath = locations.getNextPath();

       }

       return result;

    }

    /**
    * This method takes that name of a method in the Value class, and executes
    * that method for the value of every node found that matches the given path.
    * <code>forAny</code> checks to make sure that the method returns true
    * for AT LEAST ONE of the found values.
    *
    * @param path An XPath representing a repeating node whose Values should
    *             all be checked.
    *             Repeating nodes in this XPath should be marked with "[*]".
    *             For example, the path: <code>/root/container/item[*]/name</code>
    *             indicates that the <code>item</code> node repeats.
    * @param method A Method object representing a method in the Value class.
    *                   The given method must return a boolean value and take
    *                   the same number of arguments as there are elements
    *                   of <code>args</code>.
    * @param args The parameters that will be passed to the given method when it
    *             is invoked.
    *
    * @return if the method with the given name returns true for ANY value
    *         in the given path, then this will return true. If the
    *         indicated method does not return true for any values in the path,
    *         then <code>forAny</code> will return false. If a method cannot
    *         be invoked for any reason, then this will return false.
    */
    protected final boolean forAny(String path, Method method, Object[] args){

       boolean result = false;

       path = getLocationInContext(path, context);

       Context locations = new Context(path, source);
       String currentPath = locations.getNextPath();

       // While there are still paths left to check, and while the method
       // is still returning false for all values. As soon as we
       // have found a result that is true, then this whole method will
       // return true.
       while(currentPath != null && !result){

          result = invokeMethod(currentPath, method, args);
          currentPath = locations.getNextPath();

       }

       return result;

    }

    /**
    * Gets the Value object instance in the given path, and invokes the given
    * method on that Value instance.
    *
    * @param path The XPath to a node. The value of this location will be
    *             retrieved as a Value object, and the given method with
    *             then be invoked on this instance.
    * @param method The method that will be invoked. This method
    *               must be a method of the Value class and it must
    *               return a boolean value.
    * @param args The parameters that should be passed to the method.
    *
    * @return the boolen result of invoking the given method. If an exception
    *         ocurred while invoking the method or if the method did not
    *         return a boolean value tben false will be returned.
    */
    private boolean invokeMethod(String path,
                                 Method method,
                                 Object[] args){

      Value foundValue = value(path);
      return invokeMethod(foundValue, method, args);

    }

    /**
    * Invokes the given method on the given instance of Value.
    *
    * @param valueInstance This is an instance of the Value class on which
    *                      the given method will be invoked.
    * @param method The method that will be invoked. This method
    *               must be a method of the Value class and it must
    *               return a boolean value.
    * @param args The parameters that should be passed to the method.
    *
    * @return the boolen result of invoking the given method. If an exception
    *         ocurred while invoking the method or if the method did not
    *         return a boolean value tben false will be returned.
    */
    private boolean invokeMethod(Value valueInstance,
                                 Method method,
                                 Object[] args){

      if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

         String argList = getArgString(args);

         Debug.log(Debug.RULE_EXECUTION, "Executing method ["+
                                         method.getName()+
                                         "] with argument(s) ["+
                                         argList+
                                         "] on value ["+
                                         valueInstance+"]");
      }

      Object returnValue;

      try{
         returnValue = method.invoke(valueInstance, args);
      }
      catch(Exception oops){

         Throwable problem = oops;
         if(oops instanceof InvocationTargetException){
            problem = ((InvocationTargetException) oops).getTargetException();
         }

         Debug.log(Debug.ALL_ERRORS,
                      "The method ["+
                      method.getName()+
                      "] could not be invoked: "+
                      problem);
         return false;

      }

      if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
         Debug.log(Debug.RULE_EXECUTION, "Method ["+
                                         method.getName()+
                                         "] returned ["+
                                         returnValue+
                                         "] for value ["+
                                         valueInstance+"]");
      }

      if(returnValue instanceof Boolean){
         return (Boolean) returnValue;
      }

      // Only methods that return a boolean are allowed
      Debug.log(Debug.ALL_ERRORS,
                   "The method ["+
                   method.getName()+
                   "] did not return a boolean value");

      return false;

    }



    /**
    * Creates a String listing the elements of the given array. This is
    * a utility method, the String will only be used for logging purposes.
    *
    * @param args an array of parameter objects.
    *
    * @return The String version of each of the args concatented together
    *         and separated by commas.
    */
    private String getArgString(Object[] args){

       StringBuffer argBuffer = new StringBuffer();

       for(int i = 0; i < args.length; i++){

          if(i > 0) argBuffer.append(", ");

          argBuffer.append(args[i].toString());

       }

       return argBuffer.toString();

    }

    /**
    * Finds the method in the com.nightfire.framework.rules.Value class
    * that matches the given name and parameter list.
    *
    * @return the Method in the Value class with the given name or null
    *         if no such method is found.
    */
    private Method getValueMethod(String methodName, Class[] paramTypes){

       Method method = null;

       try{
          method = Value.class.getMethod(methodName, paramTypes);
       }
       catch(Exception ex){

          if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
             Debug.log(Debug.RULE_EXECUTION,
                       "A method in the Value class could not be found with the name ["+
                       methodName+
                       "]: "+
                       RuleUtils.getExceptionMessage(ex) );
          }

       }

       return method;

    }

    /**
    * This method takes that name of a method in the Value class, and executes
    * that method for the value of every node found that matches the given path.
    * <code>forAll</code> checks to make sure that the method returns true
    * for ALL found values.
    *
    * <p>Usage Example:
    * <pre>
    *   forAll("/Root/container/item[*]/NAME", "hasValue")
    * </pre>
    * This example checks that all NAME nodes have a value that is not an empty String.
    *
    * @param path An XPath representing a repeating node whose Values should
    *             all be checked.
    *             Repeating nodes in this XPath should be marked with "[*]".
    *             For example, the path: <code>/root/container/item[*]/name</code>
    *             indicates that the <code>item</code> node repeats.
    * @param methodName The name of a method in the Value class. The named
    *                   method must take no parameters and return a
    *                   boolean value.
    *
    * @return if the method with the given name returns true for ALL values
    *         in the given path, then this will return true. If the
    *         indicated method returns false for any value in the path, then
    *         <code>forAll</code> will return false. If a method with the given
    *         name is not found or cannot be invoked, then this method will return false.
    */
    public boolean forAll(String path, String methodName){

       Class[] noParams = new Class[0];
       Method function = getValueMethod(methodName, noParams);

       // Did we find a matching method?
       if(function == null){
          return false;
       }

       return forAll(path, function, new Object[0]);

    }



    /**
    * This method takes that name of a method in the Value class, and executes
    * that method for the value of every node found that matches the given path.
    * <code>forAny</code> checks to make sure that the method returns true
    * for AT LEAST ONE of the found values.
    *
    * <p>Usage Example:
    * <pre>
    *   forAny("/Root/container/item[*]/NAME", "hasValue")
    * </pre>
    * This example checks that at least one of the NAME nodes has a value that
    * is not an empty String.
    *
    * @param path An XPath representing a repeating node whose Values should
    *             all be checked.
    *             Repeating nodes in this XPath should be marked with "[*]".
    *             For example, the path: <code>/root/container/item[*]/name</code>
    *             indicates that the <code>item</code> node repeats.
    * @param methodName The name of a method in the Value class. The named
    *                   method must take no parameters and return a
    *                   boolean value.
    *
    * @return if the method with the given name returns true for ANY values
    *         in the given path, then this will return true. If the
    *         indicated method returns false for all of the value in the path,
    *         then <code>forAny</code> will return false. If a method with the given
    *         name is not found or cannot be invoked, then this method will return false.
    */
    public boolean forAny(String path, String methodName){

       Class[] noParams = new Class[0];
       Method function = getValueMethod(methodName, noParams);

       // Did we find a matching method?
       if(function == null){
          return false;
       }

       return forAny(path, function, new Object[0]);

    }

    /**
    * This method takes that name of a method in the Value class, and executes
    * that method for the value of every node found that matches the given path.
    * <code>forAll</code> checks to make sure that the method returns true
    * for ALL found values.
    *
    * <p>Usage Example:
    * <pre>
    *   forAll("/Root/container/item[*]/NAME", "startsWith", {"A"})
    * </pre>
    * This example checks that all NAME nodes start with the String "A".
    *
    * @param location An XPath representing a repeating node whose Values should
    *             all be checked.
    *             Repeating nodes in this XPath should be marked with "[*]".
    *             For example, the path: <code>/root/container/item[*]/name</code>
    *             indicates that the <code>item</code> node repeats.
    * @param methodName The name of a method in the Value class. The named
    *                   method must return a boolean value and take
    *                   the same number of arguments as there are elements
    *                   of <code>args</code>.
    * @param args The parameters that will be passed to the given method when it
    *             is invoked.
    *
    * @return if the method with the given name returns true for ALL values
    *         in the given path, then this will return true. If the
    *         indicated method returns false for any value in the path, then
    *         <code>forAll</code> will return false. If a method with the given
    *         name is not found or cannot be invoked, then this method will return false.
    */
    public boolean forAll(String location, String methodName, String[] args){

       Class[] params;

       params = new Class[args.length];
       for(int i = 0; i < args.length; i++){

          params[i] = String.class;

       }

       Method function = getValueMethod(methodName, params);

       if(function == null){

          // If no matching method was found, check to see if there is a
          // matching method that takes a String array as a parameter...
          params = new Class[1];
          params[0] = String[].class;
          function = getValueMethod(methodName, params);

          // Did we find any matching method?
          if(function == null){
             return false;
          }

          Object[] arrayArg = new Object[1];
          arrayArg[0] = args;
          return forAll(location, function, arrayArg);

       }

       return forAll(location, function, args);

    }

    /**
    * This method takes that name of a method in the Value class, and executes
    * that method for the value of every node found that matches the given path.
    * <code>forAny</code> checks to make sure that the method returns true
    * for AT LEAST ONE of the found values.
    *
    * <p>Usage Example:
    * <pre>
    *   forAny("/Root/container/item[*]/NAME", "equals", {"A"})
    * </pre>
    * This example checks that at least one of the NAME node values equals the
    * String "A".
    *
    * @param location An XPath representing a repeating node whose Values should
    *             all be checked.
    *             Repeating nodes in this XPath should be marked with "[*]".
    *             For example, the path: <code>/root/container/item[*]/name</code>
    *             indicates that the <code>item</code> node repeats.
    * @param methodName The name of a method in the Value class. The named
    *                   method must return a boolean value and take
    *                   the same number of arguments as there are elements
    *                   of <code>args</code>.
    * @param args The parameters that will be passed to the given method when it
    *             is invoked.
    *
    * @return if the method with the given name returns true for ANY value
    *         in the given path, then this will return true. If the
    *         indicated method does not return true for at least one value
    *         in the given path, then
    *         <code>forAny</code> will return false. If a method with the given
    *         name is not found or cannot be invoked, then this method will return false.
    */
    public boolean forAny(String location, String methodName, String[] args){

       Class[] params;

       params = new Class[args.length];
       for(int i = 0; i < args.length; i++){

          params[i] = String.class;

       }

       Method function = getValueMethod(methodName, params);

       if(function == null){

          // If no matching method was found, check to see if there is a
          // matching method that takes a String array as a parameter...
          params = new Class[1];
          params[0] = String[].class;
          function = getValueMethod(methodName, params);

          // Did we find any matching method?
          if(function == null){
             return false;
          }

          Object[] arrayArg = new Object[1];
          arrayArg[0] = args;
          return forAny(location, function, arrayArg);

       }

       return forAny(location, function, args);

    }

    /**
    * This is a utility method for replacing distinct indices in a XPath
    * like "/root/container/item[2]/name" with a wildcard like
    * "/root/container/item[*]/name". This is necessary for methods like
    * <code>count()</code> and <code>unique()</code> where we want to
    * inspect ALL nodes at this XPath level.
    *
    * @param indexedXPath An XPath with specific indexes like
    *                     "/root/container[3]/item[2]/name" whose indexes
    *                     will be replaced.
    *
    * @return the resulting XPath with wildcards "*" in the place of any
    *         numbered indexes.
    */
    protected String replaceIndices(String indexedXPath){

       String result;

       try{
           result = CachingRegexUtils.replaceAll( "\\[[0-9]+\\]", indexedXPath, "[*]" );
       }
       catch(Exception fex){
          // This shouldn't happen, but if it does, log the error and
          // return the given path.
          Debug.log(Debug.ALL_ERRORS,
                    "Could not replace indices for path ["+
                    indexedXPath+
                    "]: "+fex.getMessage() );
          result = indexedXPath;

       }

       return result;

    }


   /**
   * This class is returned from the <code>lookup</code> method.
   *
   * @see #lookup(String, String)
   *
   */
   public static class LookupPath{

      /**
      * If the path has this value, then a matching path was not found.
      */
      public static final String NOT_FOUND = "";

      /**
      * The XPath represented by this class.
      */
      private String path;

      /**
      * Constructs a LookupPath with a default value of NOT_FOUND.
      */
      public LookupPath(){

         path = NOT_FOUND;

      }

      /**
      * Constructs a LookupPath with the given XPath.
      */
      public LookupPath(String path){

         this.path = path;

      }

      /**
      * This returns true if the path is equal to NOT_FOUND.
      */
      public boolean isNotFound(){

         return path.equals(NOT_FOUND);

      }

      /**
      * This sets the path equal to NOT_FOUND.
      */
      public void setNotFound(){

         path = NOT_FOUND;

      }

      /**
      * Gets the current XPath.
      */
      public String getPath(){

         return path;

      }

      /**
      * Sets the current XPath.
      */
      public void setPath(String path){

         this.path = path;

      }

   }

}

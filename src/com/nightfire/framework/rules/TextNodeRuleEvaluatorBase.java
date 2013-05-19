package com.nightfire.framework.rules;

import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.w3c.dom.Node;

/**
 * This class has been extended from RuleEvaluatorBase to support Text Based XML
 * It overrides the basic functions which retrieve Node value.
 *
 * Overridden methods: value, present, count, checkUniqueness
 */
public abstract class TextNodeRuleEvaluatorBase extends RuleEvaluatorBase
{
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

      return present(location, context, source, getXpathCache() );

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

       return present(location, foundPath.getPath(), source, getXpathCache() );

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
               // If this is a text node, then get the value and check it...
               if( node != null && node.getChildNodes().getLength() > 0 ){
                  value = node.getTextContent();
               }
               else if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

                  Debug.log(Debug.RULE_EXECUTION,
                            "Location ["+location+
                            "] is present but it is not a text leaf node");
               }

            }
            catch(Exception ex){

               if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

                  Debug.log(Debug.RULE_EXECUTION,
                            "Could not get value for location ["+location+
                            "]: "+RuleUtils.getExceptionMessage(ex));

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
             else
                success = false;


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
    * This returns the value of the current context path.
    *
    * @return The value of the node at the current context location or a value
    *         equal to the empty String, "", if the context node has no value.
    */
    public Value value(){

       return value(".", context, source, getXpathCache());

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

       return value(location, context, source, getXpathCache());

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

       return value(location, aContext, source, getXpathCache());

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

             // check if the node is a leaf node then only get the content
             // otherwise its not a valid "Text" node 
             if( node.getChildNodes().getLength() == 1 ){
                 result = node.getTextContent();
             }
             else{
                // the XPath referred to some other node that isn't a "text" node
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

          List nodes = source.getList(location, getXpathCache());
          int totalCount = nodes.size();

          for(int i = 0; i < totalCount; i++){

            try{
                Node node = (Node) nodes.get(i);
                String nodeValue = node.getTextContent();

                if( nodeValue.equalsIgnoreCase(match) ){
                   count++;
                }

             }
             catch(Exception ex){

                   Debug.log(Debug.ALL_ERRORS,
                             "Could not find value for node with index ["+i+
                             "] found at location ["+location+"]: "+
                             RuleUtils.getExceptionMessage(ex) );
             }

          }

       }
       catch(FrameworkException fex){

             Debug.log(Debug.ALL_ERRORS,"An error occurred while trying to count the nodes for path ["+
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
           nodes = source.getList(location, getXpathCache());
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
             Node node = (Node) nodes.get(i);
             value = node.getTextContent();
           }
           catch (Exception mex) {

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
    * This checks to see if the current context location is absent from
    * the current message source. "Absent" is defined as having no XML node
    * that exists at this location. A node is also considered absent if
    * it has a "value" attribute, but the value of the attribute is an
    * empty String "".
    *
    * @return true if the current context node is absent, false otherwise.
    */
    public boolean absent(){

       return absent(".", context, source, getXpathCache());

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

      return absent(location, context, source, getXpathCache());

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

       return absent(location, foundPath.getPath(), source, getXpathCache());

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
    * Creates a RuleError with the current ID and description of the Rule
    * Evaluator. Aslo sets the given context path and that path's value on
    * the new Error object.
    */
    protected RuleError getError(String contextPath){

       // Get the current context value if it has one.
       String currentContextValue = value(".", contextPath, source).toString();

       return new RuleError(getID(),
                            getDescription(),
                            contextPath,
                            currentContextValue);

    }

    /**
    * Creates a RuleError with the current ID and description of the Rule
    * Evaluator and also the sets the context of the error as the given path
    * and looks up the value of that path.
    */
    protected RuleError getError(String message, String contextPath, Exception ex){

       // Get the current context value if it has one.
       String currentContextValue = value(".", contextPath, source).toString();

       return new RuleError(getID(),
                            message + ex.toString(),
                            contextPath,
                            currentContextValue);

    }
}

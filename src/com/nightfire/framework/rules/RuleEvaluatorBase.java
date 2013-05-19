///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.framework.rules;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.order.CHOrderEvalContext;
import java.sql.*;

/**
* This is the parent class for all generated rule Evaluator implementations.
* Evaluators execute rules at runtime.
*
*/
public abstract class RuleEvaluatorBase extends ActionFunctions
                                        implements XMLEvaluator{


   /**
   * The ID of the Rule which generated this Evaluator implementation.
   */
   private String id;

   /**
   * The description of the Rule which generated this Evaluator implementation.
   */
   private String description;

   /**
     * Location of request header in the Rule Context.
     */
    public static final String RULE_CONTEXT_HEADER_LOCATION = "RULE_CONTEXT_HEADER_LOCATION";

    /**
     * Properties for Order based schema
     */
    public static final String OLD_ORDER_STATUS_PROP = "OLD_ORDER_STATUS";
    public static final String OLD_ORDER_VER_PROP = "OLD_ORDER_VER";
    public static final String OLD_ORDER_MSGTYPE_PROP = "OLD_ORDER_MSGTYPE";
    public static final String OLD_ORDER_SUPPLIER_PROP = "OLD_ORDER_SUPPLIER";

    /**
     * Database column names for CH_ORDER table
     */
    public static final String CH_ORDER_STATUS_COLUMN_NAME = "STATUS";
    public static final String CH_ORDER_VERSION_COLUMN_NAME = "ORDER_TYPE_VERSION";
    public static final String CH_ORDER_TYPE_COLUMN_NAME = "ORDER_TYPE";
    public static final String CH_ORDER_SUPPLIER_COLUMN_NAME = "SUPPLIER_NAME";

   /**
     * XPath of request header root node, in the Rule Context.
     */
    public static final String RULE_HEADER_ROOT_NODE = "Header.";


   private RuleContext rContext=null;

   /**
   * This method takes an XML input message, parses it, and validates its
   * contents by checking
   * to see if an implication is true. The implication is in the
   * form P implies Q (if P then Q, P -> Q, Q when P, etc.). The value
   * of P is determined by calling the <code>evaluateCondition()</code>
   * method against the given <code>source</code>. The value of Q is found by
   * calling the <code>evaluateAssertion()</code>
   * method. These methods are abstract and are implented by
   * child classes of this class. These child classes will usually be generated
   * by the Rules2Java tool.
   *
   * @param source An XML Message that will be evaluated.
   * @param errors Any RuleErrors are accumulated in this parameter.
   *
   * @return true if the implication evaluates to true for the given input
   *         source, false otherwise.
   */
   public boolean evaluate(String source, ErrorCollection errors){

		return evaluate(source, errors, null);

   }

   /**
   * This method takes an XML input message, parses it, and validates its
   * contents by checking
   * to see if an implication is true. The implication is in the
   * form P implies Q (if P then Q, P -> Q, Q when P, etc.). The value
   * of P is determined by calling the <code>evaluateCondition()</code>
   * method against the given <code>source</code>. The value of Q is found by
   * calling the <code>evaluateAssertion()</code>
   * method. These methods are abstract and are implented by
   * child classes of this class. These child classes will usually be generated
   * by the Rules2Java tool.
   *
   * @param source An XML Message that will be evaluated.
   * @param errors Any RuleErrors are accumulated in this parameter.
   * @param ruleContext  RuleContext object shared by the rules
   *
   * @return true if the implication evaluates to true for the given input
   *         source, false otherwise.
   */
   public boolean evaluate(String source, ErrorCollection errors, RuleContext ruleContext){

      boolean success = false;

      XPathAccessor src;

      try{
         src = new CachingXPathAccessor(source);

         src.useInternalCaching( true );

         success = evaluate( src, errors, ruleContext );

         if ( Debug.isLevelEnabled( Debug.RULE_LIFECYCLE ) )
             Debug.log( Debug.RULE_LIFECYCLE, src.describeCachedXPaths( false ) );
      }
      catch(FrameworkException fex){

         errors.addError( getError("The input message was not valid XML input: ",
                                   fex) );

      }

      return success;

   }


   /**
   * This method takes an input message and validates it by checking
   * to see if an implication is true. The implication is in the
   * form P implies Q (if P then Q, P -> Q, Q when P, etc.). The value
   * of P is determined by calling the <code>evaluateCondition()</code>
   * method against the given <code>source</code>. The value of Q is found by
   * calling the <code>evaluateAssertion()</code>
   * method. These methods are abstract and are implented by
   * child classes of this class. These child classes will usually be generated
   * by the Rule Designer tool.
   * This is not thread safe. Only one thread should use an instance of this
   * class at a time. The caller is responsible for making sure that the source
   * message does not change while we are evaluating this rule.
   *
   * @param source By taking an XPathAccessor instead of an XML message,
   *               this saves us from having to re-parse an XML String
   *               that already has an XPathAccessor.
   * @param errors Any RuleErrors are accumulated in this parameter.
   *
   * @return true if this rule evaluates to true for the given input
   *         source, false otherwise.
   */
   public boolean evaluate(XPathAccessor source,
                           ErrorCollection errors){

     return evaluate(source, errors, (RuleContext)null);

   }

 /**
   * This method takes an input message and validates it by checking
   * to see if an implication is true. The implication is in the
   * form P implies Q (if P then Q, P -> Q, Q when P, etc.). The value
   * of P is determined by calling the <code>evaluateCondition()</code>
   * method against the given <code>source</code>. The value of Q is found by
   * calling the <code>evaluateAssertion()</code>
   * method. These methods are abstract and are implented by
   * child classes of this class. These child classes will usually be generated
   * by the Rule Designer tool.
   * This is not thread safe. Only one thread should use an instance of this
   * class at a time. The caller is responsible for making sure that the source
   * message does not change while we are evaluating this rule.
   *
   * @param source By taking an XPathAccessor instead of an XML message,
   *               this saves us from having to re-parse an XML String
   *               that already has an XPathAccessor.
   * @param errors Any RuleErrors are accumulated in this parameter.
   * @param ruleContext  RuleContext object shared by the rules
   *
   * @return true if this rule evaluates to true for the given input
   *         source, false otherwise.
   */
   public boolean evaluate(XPathAccessor source,
                           ErrorCollection errors, RuleContext ruleContext){

     boolean success = true;

     if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){
        Debug.log(Debug.RULE_EXECUTION, "Evaluating rule ["+id+"]");
     }

     // Set the source message for use by the StandardFunctions
     super.setSource(source);

     // Save a copy of the original XPath context String so that it can
     // be restored to its original value when we are done here.
     String originalContext = super.getContext();

     // The context XPath may describe more than one Node in the source tree.
     // This Context object provides a distinct context path for each existing
     // node.
     Context paths = new Context(originalContext, source);

     String contextPath = paths.getNextPath();

     // If no node was found matching the originalContext, then
     // the contextPath will be null. In order that the rule
     // executes at least once, the originalPath will be used
     // as the contextPath in this case.
     // This is the fix for CR12156.
     if( contextPath == null ){

        contextPath = originalContext;

     }

     // evaluate the rule for each existing path that matches the context path
     while( contextPath != null ){

         // If any single evaluation fails, then flag the
         // overall result as a failure.
         if(! evaluate(source, errors, contextPath, ruleContext ) ){

            success = false;

         }

         contextPath = paths.getNextPath();

     }

     return success;

   }

   /**
   * This method takes an input message and determines whether it passes
   * this rule for the given context.
   *
   * <p>Note: This method is not thread safe.
   * Only one thread should use an instance of this
   * class at a time. The caller is responsible for making sure that the source
   * message does not change while we are evaluating this rule.
   *
   * @param source The input message to be evaluated.
   * @param errors if the evaluation fails, a RuleError will be added to this
   *               collection.
   * @param contextPath The particular XPath context for which this rule should
   *                    be evaluated.
   *
   * @return true if this rule evaluates to true for the given
   *         source and context path, false otherwise.
   */
   public boolean evaluate(XPathAccessor source,
                           ErrorCollection errors,
                           String contextPath){

      return evaluate(source, errors, contextPath, null);

   }

   /**
   * This method takes an input message and determines whether it passes
   * this rule for the given context.
   *
   * <p>Note: This method is not thread safe.
   * Only one thread should use an instance of this
   * class at a time. The caller is responsible for making sure that the source
   * message does not change while we are evaluating this rule.
   *
   * @param source The input message to be evaluated.
   * @param errors if the evaluation fails, a RuleError will be added to this
   *               collection.
   * @param contextPath The particular XPath context for which this rule should
   *                    be evaluated.
   *
   * @return true if this rule evaluates to true for the given
   *         source and context path, false otherwise.
   */
   public boolean evaluate(XPathAccessor source,
                           ErrorCollection errors,
                           String contextPath, RuleContext ruleContext){

      // default the result to successful
      boolean result = true;

      // the condition, p, portion of if p then q
      boolean p = false;

	  // set the ruleContext object passed into the rContext so that all methods of the class can use it.
	  rContext=ruleContext;

      // Save a copy of the original XPath context String so that it can
      // be restored to its original value when we are done here.
      String originalContext = super.getContext();

      // set the given context for use when executing the StandardFunctions
      super.setContext(contextPath);

       try{
         p = evaluateCondition();
      }
      catch(Exception ex){

         String conditionFailed = "The condition could not be evaluated: ";

         errors.addError( getError(conditionFailed, contextPath, ex) );

         Debug.log(Debug.ALL_ERRORS, conditionFailed + ex);

         // mark this evaluation as a failure
         result = false;

      }

      // This short-circuits the evaluation. If the condition fails, then this
      // rule does not apply, and
      // there is no reason to evaluate the assertion, q
      if(p){

          try{

             // p is true.
             // if p is true, then our assertion, q, must also be true in order
             // for this entire implication to be considered true. So the result,
             // at this point, is simply equal to q.
             result = evaluateAssertion();

          }
          catch(Exception ex){

             String assertionFailed = "The assertion could not be evaluated: ";

             errors.addError(getError(assertionFailed, contextPath, ex));

             Debug.log(Debug.ALL_ERRORS, assertionFailed + ex);

             Debug.logStackTrace(ex);

             // mark this evaluation as a failure because of the exception
             result = false;

          }

      }

      // If the evaluation failed, add a new error with the rule ID,
      // description, and current context info to the list of errors.
      if( !result ){

         errors.addError( getError(contextPath) );

      }

      // Reset the context back to the original context.
      // This is necessary for the case where this RuleEvaluatorBase has been
      // cached and will be reused.
      super.setContext(originalContext);

	  // remove the reference to the RuleContext object
	  rContext=null;

      return result;

   }


   /**
   * Implemented by a generated subclass to determine if the conditional (P)
   * is true for the current source.
   */
   protected abstract boolean evaluateCondition();


   /**
   * Implemented by a generated subclass to determine if the assertion (Q)
   * is true for the current source.
   */
   protected abstract boolean evaluateAssertion();

   /**
   * Creates a RuleError with the current ID and description of the Rule
   * Evaluator.
   */
   private RuleError getError(){

      return new RuleError(getID(), getDescription());

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

   /**
   * Creates a RuleError with the current ID, the given error message,
   * and the given exception message.
   *
   */
   private RuleError getError(String errorMessage, Exception ex){

      return new RuleError(getID(), errorMessage, ex);

   }

   /**
   * Gets the ID for the Rule that this Evaluator enforces.
   */
   protected String getID(){

      return id;

   }

   /**
   * Sets the ID for the Rule that this Evaluator enforces.
   */
   public void setID(String id){

      this.id = id;

   }

   /**
   * Gets the description for the Rule that this Evaluator enforces.
   */
   public String getDescription(){

      return description;

   }

   /**
   * Sets the ID for the Rule that this Evaluator enforces.
   */
   public void setDescription(String desc){

      this.description = desc;

   }

   /**
   * Gets a String that contains the Rule ID and Description for this
   * Evaluator.
   */
   public String toString(){

      StringBuffer result = new StringBuffer("\nRule Evaluator ID: [");
      result.append( getID() );
      result.append("]\nDescription:       [");
      result.append( getDescription() );
      result.append("]\n");

      return result.toString();

   }

   /**
   * Returns the shared DB Connection from the RuleContext object.
   */

   protected Connection getDBConnection ( ) throws FrameworkException{

		if(rContext != null)
		   return rContext.getDBConnection();
		else
			return null;
   }

   /**
   *  Test to see if there is a value associated with the named item in the ruleContext map. 
   */
   protected boolean existsValue ( String name ){
		if(rContext != null)
		   return rContext.exists(name);
		else
			return false;
   }

   /**
   * Gets the value associated with the named item from the ruleContext map. 
   */
   protected Object getContextValue( String name ){
		if(rContext != null && StringUtils.hasValue(name) )
        {
            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "parameter to be obtained: " + name);
		    
            if(name.startsWith(RULE_HEADER_ROOT_NODE))
            {
                name = "/" + name.replaceAll("\\.", "/");
                String retVal = headerValue(name).toString();
                
                if(Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "header property : " + name
                        + " its value " + retVal);
                
                return retVal;
            }

            return rContext.get(name);
        }
		else
			return null;
   }

   /**
   * Sets the name and value in the ruleContext.
   */
   protected void setContextValue( String name, Object value ) throws FrameworkException{
		if(rContext != null)
		{
			rContext.set(name, value);
            	}
		else
		{
			throw new FrameworkException("Context doesn't exist so values cannot be set");
		}
   }

  /**
   * Gets the value associated with the named item from the header available in ruleContext map.
   */
   protected Value headerValue(String path) {
		if(rContext != null)
        {
            XPathAccessor accessor = null;
            Object header = rContext.get(RULE_CONTEXT_HEADER_LOCATION);

            // Fixed TD #7741: ASR Receive request may or may not have header available 
            if(null==header)
            {
                Debug.log(Debug.ALL_WARNINGS, "Header is not available in context");
                return new Value();
            }

            //check if the Header object is unparsed; if so, get XPathAccessor object
            // and set it in Rule Context for use in subsequent processing.
            if(header instanceof String)
            {
                 try
                 {
                    Debug.log(Debug.RULE_EXECUTION,"Getting String Header from RuleContext for processing...");
                    accessor = new XPathAccessor((String)header);
                    Debug.log(Debug.RULE_EXECUTION,"Setting parsed Header XPathAccessor object in RuleContext...");
                    rContext.set(RULE_CONTEXT_HEADER_LOCATION,accessor);
                 } catch (FrameworkException e)
                 {
                    Debug.log(Debug.ALL_ERRORS, "Could not create or set Header XPathAccessor in RuleContext" + e);
                    return null;
                 }
            }
            else if(header instanceof XPathAccessor)
            {
                Debug.log(Debug.RULE_EXECUTION,"Getting parsed Header from RuleContext for processing... ");
                accessor = (XPathAccessor) header;
            }
            else
            {
                // Should not come here.
                Debug.log(Debug.ALL_ERRORS, "Header should be of type String or XPathAccessor");
                return null;
            }
           return value(path,super.getContext(),accessor);
        }
		else
		   return null;
   }

    /**
     * This method is used to load ORDER information from Order based schema
     * for given Product
     *
     * @param transTableName TRANSACTION table name for the product
     * @param cols database column names of TRANSACTION table to uniquely identify order from trans table
     * @param vals values for the key column names
     */
    public boolean loadOldCHOrderInfo(String transTableName, String[] cols, String[] vals)
    {

        CHOrderEvalContext chOrderEvalContext = new CHOrderEvalContext();
        try
        {
            chOrderEvalContext.initialize(getDBConnection(), transTableName, cols, vals);
            setContextValue(OLD_ORDER_STATUS_PROP, chOrderEvalContext.getAttribute(CH_ORDER_STATUS_COLUMN_NAME));
            setContextValue(OLD_ORDER_VER_PROP, chOrderEvalContext.getAttribute(CH_ORDER_VERSION_COLUMN_NAME));
            setContextValue(OLD_ORDER_MSGTYPE_PROP, chOrderEvalContext.getAttribute(CH_ORDER_TYPE_COLUMN_NAME));
            setContextValue(OLD_ORDER_SUPPLIER_PROP, chOrderEvalContext.getAttribute(CH_ORDER_SUPPLIER_COLUMN_NAME));
        }
        catch (FrameworkException e)
        {
            Debug.log(Debug.ALL_ERRORS, "CH Order Information could not be loaded.");
            Debug.logStackTrace(e);
            return false;
        }
        return true;
    }

    /**
     * Method to get most recent status of the Order from CH_ORDER table (Order Based Schema)
     * @return
     */
    public Value getOldStatus()
    {
        String retVal = (String) getContextValue(OLD_ORDER_STATUS_PROP);

        if (retVal != null)
            return new Value(retVal);

        Debug.log( Debug.ALL_ERRORS, "RuleEvaluatorBase.getOldSatus(): Could not find old status of the Order. " +
                "CH Order Information not found.");

        return new Value("");
    }

    /**
     * Method to get most recent request version of the Order from CH_ORDER table (Order Based Schema)
     * @return
     */
    public Value getOldVer()
    {
        String retVal = (String) getContextValue(OLD_ORDER_VER_PROP);

        if (retVal != null)
            return new Value(retVal);

        Debug.log( Debug.ALL_ERRORS, "RuleEvaluatorBase.getOldSatus(): Could not find old status of the Order. " +
                "CH Order Information not found.");

        return new Value("");
    }

    /**
     * Method to get most recent Message type of the Order from CH_ORDER table (Order Based Schema)
     * @return
     */
    public Value getOldMsgType()
    {
        String retVal = (String) getContextValue(OLD_ORDER_MSGTYPE_PROP);

        if (retVal != null)
            return new Value(retVal);

        Debug.log( Debug.ALL_ERRORS, "RuleEvaluatorBase.getOldSatus(): Could not find old status of the Order. " +
                "CH Order Information not found.");

        return new Value("");
    }

    /**
     * Method to get most recent supplier of the Order from CH_ORDER table (Order Based Schema)
     * @return
     */
    public Value getOldSupplier()
    {
        String retVal = (String) getContextValue(OLD_ORDER_SUPPLIER_PROP);

        if (retVal != null)
            return new Value(retVal);

        Debug.log( Debug.ALL_ERRORS, "RuleEvaluatorBase.getOldSatus(): Could not find old status of the Order. " +
                "CH Order Information not found.");

        return new Value("");
    }
}

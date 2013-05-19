//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
//////////////////////////////////////////////////////////////////////////////

package com.nightfire.framework.rules;

import java.util.List;
import java.util.ArrayList;
import com.nightfire.framework.util.*;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

import com.nightfire.framework.message.util.xml.CachingXPathAccessor;
import com.nightfire.framework.message.util.xml.XPathAccessor;

/**
* This class is part of a performance enhancement that groups rule
* evaluator instances based on their XPath context. Rules with
* the same context get added to an instance of this class. When this
* class is executed, it only iterates through all of the nodes that match the
* context XPath once,
* executing each rule once against each matching XPath location. This should
* be a performance improvement over iterating through the matching nodes once
* for each rule.
*
*
*/
public class ContextGroupEvaluator implements XMLEvaluator
{

   /**
   * The XPath context against which all rules in this group will be evaluated.
   */
   private String context;

   /**
   * The list of rule evaluators in this group.
   */
   public List evaluators = new ArrayList();

   public ContextGroupEvaluator(String context)
   {

      this.context = context;

   }

   /**
   * Adds the given rule evaluator to this group. Rules added to this
   * group are assumed to have the same context path as all of the
   * other rules in this group. Added rules will be executed when
   * this group is evaluated.
   *
   * @param rule the rule to add.
   */
   public void add(RuleEvaluatorBase rule){

     evaluators.add(rule);

   }

   /**
   *
   *
   * @param parsedMessage the parsed XML input message.
   * @param errors Any rule errors that occur will be added to this collection.
   *
   * @return true if the <code>parsedMessage</code> is valid with respect to
   *         all rules in this group, false otherwise.
   *
   */
   public boolean evaluate(XPathAccessor parsedMessage,
                           ErrorCollection errors){

      return evaluate(parsedMessage, errors, null);

   }

   /**
   *
   *
   * @param parsedMessage the parsed XML input message.
   * @param errors Any rule errors that occur will be added to this collection.
   * @param ruleContext  RuleContext object shared by the rules
   *
   * @return true if the <code>parsedMessage</code> is valid with respect to
   *         all rules in this group, false otherwise.
   *
   */
   public boolean evaluate(XPathAccessor parsedMessage,
                           ErrorCollection errors, RuleContext ruleContext){

      boolean success = true;

      if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){

        Debug.log(Debug.RULE_EXECUTION,
                  "Evaluating all rules with context ["+context+"]");
      }

      // The context XPath may describe more than one Node in the source tree.
      // This Context object provides a distinct context path for each existing
      // node.
      Context paths = new Context(context, parsedMessage);

      String currentPath = paths.getNextPath();

      // This ensures that the rules in this group get evaluated at least once
      if( currentPath == null ){

        currentPath = context;

      }

      String origPath = currentPath;

      RuleEvaluatorBase currentRule;

      // evaluate the rule group once for each existing path that
      // matches the context path

      long x = DateUtils.getCurrentTimeValue();

      int numOfPaths = 0;

      while( currentPath != null ){

          numOfPaths++;

          for (Object evaluator : evaluators) {

              currentRule = (RuleEvaluatorBase) evaluator;

              if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
                  Debug.log(Debug.RULE_EXECUTION,
                          "Evaluating rule [" + currentRule.getID() + "]");
              }

              currentRule.setSource(parsedMessage);

              long startTime = System.currentTimeMillis();

              if (!DisabledRules.isDisabled(currentRule.getClass().getName()) &&
                !currentRule.evaluate(parsedMessage, errors, currentPath, ruleContext)) {
                  // If any single evaluation fails, then flag the
                  // overall result as a failure.
                  success = false;
              }

              long stopTime = System.currentTimeMillis();

              if (Debug.isLevelEnabled(Debug.RULE_EXECUTION) && (stopTime - startTime > 0))
                  Debug.log(Debug.RULE_EXECUTION, "Rule [" + currentRule.getClass().getName() +
                          "] execution time was [" + (stopTime - startTime) + "] msec.");
          }

          currentPath = paths.getNextPath();

      }


      if ( Debug.isLevelEnabled( Debug.RULE_EXECUTION ) )
      {
          long y = DateUtils.getCurrentTimeValue();
          Debug.log(Debug.RULE_EXECUTION, "BENCHMARK: Executed rules with context [" + origPath +
                    "] through [" + numOfPaths + "] xpaths in [" + (y - x) + "] msec.");
         Debug.log(Debug.RULE_EXECUTION,
                  "Done evaluating all rules with context ["+context+"]");
      }

      return success;

   }

   /**
   * Determines whether the given source follows the
   * rule(s) enforced by this Evaluator or not. This defines the Evalutor
   * interface.
   *
   * @param source The XML input to be evaluated.
   * @param errors Any RuleErrors that occur while during
   *               evaluation will be added to this collection.
   *
   * @return whether the evaluation was successful.
   */
   public boolean evaluate( String source,
                            ErrorCollection errors ){
 
      return evaluate(source, errors, null);

   }

   /**
   * Determines whether the given source follows the
   * rule(s) enforced by this Evaluator or not. This defines the Evalutor
   * interface.
   *
   * @param source The XML input to be evaluated.
   * @param errors Any RuleErrors that occur while during
   *               evaluation will be added to this collection.
   *
   * @return whether the evaluation was successful.
   */
   public boolean evaluate( String source,
                            ErrorCollection errors, RuleContext ruleContext ){

      try{
         XPathAccessor parsedMessage = new CachingXPathAccessor(source);

         parsedMessage.useInternalCaching( true );

         boolean result = evaluate( parsedMessage, errors, ruleContext );

         if ( Debug.isLevelEnabled( Debug.RULE_LIFECYCLE ) )
             Debug.log( Debug.RULE_LIFECYCLE, parsedMessage.describeCachedXPaths( true ) );

         return result;
      }
      catch(FrameworkException fex){

         errors.addError( new RuleError("Rule Group",
                                        "The input message was not valid XML: ",
                                        context,
                                        fex) );

      }

      return false;

   }


   public void cleanup(){

       for (Object evaluator : evaluators) {

           XMLEvaluator currentRule = (XMLEvaluator) evaluator;
           currentRule.cleanup();

       }

   }

   /**
   * Gets a String that describes this group.
   */
   public String toString(){

      StringBuffer result = new StringBuffer("\n> Rules with context [");
      result.append( context );
      result.append("]:\n");

       for (Object evaluator : evaluators) {
           result.append(evaluator.toString());

       }

       result.append("\n> End of Rules with context of [").append(context).append("]");

      return result.toString();

   }

}

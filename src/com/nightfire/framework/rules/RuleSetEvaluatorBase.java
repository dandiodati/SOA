///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.framework.rules;

import java.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.util.*;

/**
* This is the parent class for Evaluators that are generated for
* a RuleSet. This class contains a list of Rule Evaluators. Each
* of these Evaluator is run on an input message to see if the message
* is valid for this entire set.
*
* <p>
* This class inherits RuleEvaluatorBase's functionality.
* This will be used as the "trigger" rule to determine if this RuleSet
* evaluator should be applied to the input at all. If
* <code>super.evaluate()</code> returns <code>true</code>, then all
* of the Rule Evaluators in this set will get executed, otherwise, this
* rule set will be skipped. This is useful for
* "turning off" a rule set evaluator in the case where the input it
* would ordinarily check is
* missing.
*/
public abstract class RuleSetEvaluatorBase implements XMLEvaluator{

  /**
  * The name of the RuleSet that is being evaluated.
  */
  protected String name;

  /**
  * This evaluator that will act as the "trigger" for this RuleSetEvaluator.
  * If the trigger evaluates the input as true, then the entire rule set
  * represented by this evaluator will get executed, otherwise, if
  * the trigger evaluates to false, this entire rule set will get skipped.
  *
  */
  protected XMLEvaluator trigger = null;

  /**
  * The list of Evaluators that should be executed by this rule set evaluator.
  */
  private List evaluators = new ArrayList();

  /**
  * This maps an XPath context to a rule evaluator.
  */
  private Map contextMap = new HashMap();

  /**
  * Constructs a Rule Set Evaluator with the given <code>ruleSetName</code>.
  */
  public RuleSetEvaluatorBase(String ruleSetName){

     name = ruleSetName;

  }

  /**
  * Sets the "trigger" Evaluator. This evaluator that will act as the trigger
  * for this RuleSetEvaluator.
  * If the trigger evaluates the input as true, then the entire rule set
  * represented by this evaluator will get executed, otherwise, if
  * the trigger evaluates to false, this entire rule set will get skipped.
  *
  */
  public void setTrigger(XMLEvaluator triggerEvaluator){

     trigger = triggerEvaluator;

  }

  /**
  * Add an Evaluator to the list of Evaluators that this Evaluator executes.
  *
  * @param rule A rule evaluator implementation to be added.
  */
  public void addEvaluator(RuleEvaluatorBase rule){

     String ruleContext = rule.getContext();
     Object existing = contextMap.get( ruleContext );

     if(existing instanceof ContextGroupEvaluator){

        // A group already exists for rules with this context.
        // Add the rule to this group.
        ContextGroupEvaluator group = (ContextGroupEvaluator) existing;

        group.add( rule );

     }
     else if(existing instanceof RuleEvaluatorBase){

        // A rule already exists in this rule set with the same context
        // as the given rule.

        // Remove the existing rule from this set. The original ordering
        // of the rule in this case is lost, but this sacrifice seems
        // trivial and is made for the sake of performance.
        RuleEvaluatorBase existingRule = (RuleEvaluatorBase) existing;
        contextMap.remove(ruleContext);
        evaluators.remove(existingRule);

        // Create a group to contain the rules that share this context.
        // Grouping the rules in this way provides a performance enhancement.
        ContextGroupEvaluator group =
           new ContextGroupEvaluator( ruleContext );

        group.add( existingRule );
        group.add( rule );

        // add the new group to this rule set
        contextMap.put(rule.getContext(), group);
        evaluators.add(group);

     }
     else{

        contextMap.put(ruleContext, rule);
        evaluators.add(rule);

     }

  }

  /**
  * This parses the input XML and then evaluates this input
  * using each of the added Evaluators.
  *
  * @param source The source XML message to be evaluated.
  * @param errors Any RuleErrors that are created while evaluating this
  *               Rule Set will be added to this collection.
  *
  * @return false if any of the Evaluators in the list fails, true if
  *               they all pass successfully.
  */
  public boolean evaluate(String source, ErrorCollection errors ){

      return evaluate(source, errors, null);

  }

  /**
  * This parses the input XML and then evaluates this input
  * using each of the added Evaluators.
  *
  * @param source The source XML message to be evaluated.
  * @param errors Any RuleErrors that are created while evaluating this
  *               Rule Set will be added to this collection.
  * @param ruleContext  RuleContext object shared by the rules
  *
  * @return false if any of the Evaluators in the list fails, true if
  *               they all pass successfully.
  */
  public boolean evaluate(String source, ErrorCollection errors, RuleContext ruleContext ){

     boolean success = true;

     try{

         XPathAccessor src = new CachingXPathAccessor(source);

         src.useInternalCaching( true );

         success = evaluate(src, errors, ruleContext);
     }
     catch(FrameworkException fex){

         errors.addError( new RuleError( name,
                                         "This rule set could not be evaluated, because the input was not in the correct format: ",
                                         fex ) );
         success = false;

     }

     return success;

  }

  /**
  * This evaluates the given parsed message
  * using each of the added Evaluators. This rule set evaluator will
  * only get executed if the "trigger" rule evaluates to true,
  * otherwise, this will skip the rule set evaluation, and return true.
  *
  * @param parsedSource The a parsed XML message submitted for evaluation.
  * @param errors Any RuleErrors that are created while evaluating this
  *               Rule Set will be added to this collection.
  *
  * @return false if any of the Evaluators in the list fails, true if
  *               they all pass successfully.
  */
  public boolean evaluate(XPathAccessor parsedSource, ErrorCollection errors){

      return evaluate(parsedSource, errors, null) ;

  }

  /**
  * This evaluates the given parsed message
  * using each of the added Evaluators. This rule set evaluator will
  * only get executed if the "trigger" rule evaluates to true,
  * otherwise, this will skip the rule set evaluation, and return true.
  *
  * @param parsedSource The a parsed XML message submitted for evaluation.
  * @param errors Any RuleErrors that are created while evaluating this
  *               Rule Set will be added to this collection.
  * @param ruleContext  RuleContext object shared by the rules
  *
  * @return false if any of the Evaluators in the list fails, true if
  *               they all pass successfully.
  */
  public boolean evaluate(XPathAccessor parsedSource, ErrorCollection errors, RuleContext ruleContext){

      boolean success = true;
       XMLEvaluator rule;

       ErrorCollection triggerResult = new ErrorCollection();

       // If the trigger rule is valid, then execute the rules in this set
       if( trigger == null || trigger.evaluate(parsedSource, triggerResult, ruleContext) ){

           for (Object evaluator : evaluators) {
               rule = (XMLEvaluator) evaluator;

               long startTime = System.currentTimeMillis();

               if (!DisabledRules.isDisabled(rule.getClass().getName()) &&
                       !rule.evaluate(parsedSource, errors, ruleContext))
                   success = false;

               long stopTime = System.currentTimeMillis();

               if (Debug.isLevelEnabled(Debug.RULE_EXECUTION) && (stopTime - startTime > 0))
                   Debug.log(Debug.RULE_EXECUTION, "Rule [" + rule.getClass().getName() +
                           "] execution time was [" + (stopTime - startTime) + "] msec.");
           }

       }
       else if( Debug.isLevelEnabled( Debug.RULE_EXECUTION ) ){

          // The trigger rule failed, there is no need to
          // evaluate this rule set.
          Debug.log(Debug.RULE_EXECUTION, "Skipping Rule Set Evaluator: "+
                                          name+
                                          "\n"+
                                          triggerResult.toString() );

       }

       if ( Debug.isLevelEnabled( Debug.RULE_LIFECYCLE ) )
           Debug.log( Debug.RULE_LIFECYCLE, parsedSource.describeCachedXPaths( true ) );

       // clean up any caches or resources
       cleanup();

       return success;

  }

  /**
   * Cleanup all children.
   */
  public void cleanup(){

      for (Object evaluator : evaluators) {
          XMLEvaluator rule = (XMLEvaluator) evaluator;
          rule.cleanup();

      }

  }

   /**
   * This returns a String that lists the name of this Evaluator as well
   * as the String version of each member of the list of evaluators executed
   * by this one.
   */
   public String toString(){

      StringBuffer result = new StringBuffer("\nRule Set Evaluator: [");
      result.append( name );
      result.append("]\n");

       for (Object evaluator : evaluators) {
           result.append(evaluator.toString());

       }

       return result.toString();

   }

}

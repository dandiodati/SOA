package com.nightfire.framework.rules;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class holds the data representing a rule. This class does not actually
* evaluate the rule that it represents.
*/
public class Rule implements EvaluatorDefinition{

  /**
  * The rule ID.
  */ 
  protected String id;

  /**
  * The description of this rule.
  */
  protected String description;

  /**
  * The XPath context in which this Rule should be evaluated.
  */
  protected String context;

  /**
  * This represents the conditional portion of the logical implication
  * that is used by this rule's evaluator to
  * determining if the evaluation is true or false.
  * The value of this String must be a java expression that
  * evaluates to a boolean value.
  */
  protected String condition;

  /**
  * This represents the assertion of the logical implication
  * that is used by this rule's evaluator to
  * determining if the that evaluation is true or false.
  * The value of this String must be a java expression that
  * evaluates to a boolean value.
  */
  protected String assertion;

  /**
  * The RuleSet that contains this Rule. This assumes
  * that this rule is only part of one RuleSet.
  */
  protected RuleSet parent;


  /**
  * This field can contain optional notes added by the rule designer.
  * The description field is the description of the rule that will
  * be returned as part of the error when the rule fails evaluation.
  * The comments field are internal comments about the rule that the
  * end user will never see, such as "TODO: Finish writing this rule".
  */
  protected String comments;

  /**
  * This creates a rule with all null field values.
  */
  public Rule() {

     id          = null;
     description = null;
     context     = null;
     condition   = null;
     assertion   = null;
     parent      = null;
     comments    = null;

  }

  /**
  * This contructs a Rule and sets all field values to their
  * corresponding parameter values.
  *
  * @param ID This Rule's ID
  * @param description The description of this rule.
  * @param context The XPath context in which this Rule should be evaluated.
  * @param condition This represents the conditional portion of the logical
  * implication that is used by this Rule's evaluator to
  * determining if the evaluation is true or false.
  * The value of this String must be a java expression that
  * evaluates to a boolean value.
  * @param assertion This represents the assertion of the logical implication
  * that is used by this Rule's evaluator to
  * determining if the that evaluation is true or false.
  * The value of this String must be a java expression that
  * evaluates to a boolean value.
  * @param parent The RuleSet that contains this Rule. This assumes
  * that this rule is only part of one RuleSet.
  *
  */
  public Rule(String  ID,
              String  description,
              String  context,
              String  condition,
              String  assertion,
              RuleSet parent,
              String  comments){

     this.id          = ID;
     this.description = description;
     this.context     = context;
     this.condition   = condition;
     this.assertion   = assertion;
     this.parent      = parent;
     this.comments    = comments;

  }

  /**
  * Assigns this Rule's <code>id</code>.
  *
  * @param ID The new id.
  *
  */
  public void setID(String ID){

     id = ID;

  }

  /**
  * Accesses this Rule's <code>id</code>.
  *
  * @return the current id.
  */ 
  public String getID(){

     return id;

  }

  /**
  * Assigns this Rule's <code>description</code>.
  *
  * @param description The new description.
  */
  public void setDescription(String description){

     this.description = description;

  }

  /**
  * Accesses this Rule's <code>description</code>.
  *
  * @return the current description of this Rule.
  */ 
  public String getDescription(){

     return description;

  }

  /**
  * Assigns this Rule's <code>context</code>. If
  * the given context is null or an empty String,
  * the context will be set to ".", an XPath value
  * that represents the current XPath context node.
  *
  * @param context The new context.
  */
  public void setContext(String context){

     if( !StringUtils.hasValue(context) ){
        // If the context is empty,
        // then set the context to the current location
        context = ".";
     }
     this.context = context;

  }

  /**
  * Accesses this Rule's <code>context</code>.
  *
  * @return the current context.
  */ 
  public String getContext(){

     return context;

  }

  /**
  * Assigns this Rule's <code>condition</code>.
  *
  * @param condition The new condition.
  */
  public void setCondition(String condition){

     this.condition = condition;

  }

  /**
  * Accesses this Rule's <code>condition</code>.
  *
  * @return the current condition statement.
  */ 
  public String getCondition(){

     return condition;

  }

  /**
  * Assigns this Rule's <code>assertion</code>.
  *
  * @param assertion The new assertion.
  */
  public void setAssertion(String assertion){

     this.assertion = assertion;

  }

  /**
  * Accesses this Rule's <code>assertion</code>.
  *
  * @return the current assertion statement.
  */ 
  public String getAssertion(){

     return assertion;

  }

  /**
  * Creates a new Rule with the same field values as this one.
  *
  * @return a copy of this Rule.
  */
  public Rule copy(){

     return new Rule(id, description, context, condition, assertion, parent, comments);

  }

  /**
  * Assigns this Rule's parent RuleSet.
  *
  * @param set The RuleSet that contains this rule.
  */
  public void setParent(RuleSet set){

     parent = set;

  }

  /**
  * Accesses this Rule's parent RuleSet.
  *
  * @return the RuleSet which contains this Rule.
  */ 
  public RuleSet getParent(){

     return parent;

  }

  /**
  * Assigns this Rule's <code>comments/code> field.
  *
  * @param comments The new comments for this Rule.
  */
  public void setComments(String comments){

     this.comments = comments;

  }

  /**
  * Accesses this Rule's <code>condition</code>.
  *
  * @return the current condition statement.
  */ 
  public String getComments(){

     return comments;

  }  

  /**
  * Get the package name for an Evaluator generated for this 
  * Rule. A Rule's evaluator package will be the same as
  * the package of its parent RuleSet.
  *
  * @return The String package name for a generated Evaluator.
  */
  public String getEvaluatorPackage(){

     return parent.getEvaluatorPackage();

  }

  /**
  * Returns the simple class name for the Evaluator that should be 
  * generated for this Rule. This takes this Rule's id, and 
  * makes it a valid Java class name by replacing any non-alpha-numeric
  * characters with underscores "_".
  *
  * @return a String that is the simple class name for an Evaluator
  *         of this Rule.
  */
  public String getEvaluatorClassName(){

     return RuleUtils.makeAlphaNumeric( getID() );

  }

  /**
  * Returns the full class name for the Evaluator that 
  * should be generated for this Rule. 
  *
  * @return The package name and the simple Evaluator 
  *         class name for this Rule joined in the middle
  *         by a ".".
  */  
  public String getFullEvaluatorClassName(){

     return getEvaluatorPackage()+"."+getEvaluatorClassName();

  }

  /**
  * Implemented from the EvaluatorDefinition interface, this returns this
  * Rule's ID.
  *
  * @return the same value as <code>getID()</code>.
  */
  public String getName(){

     return getID();

  }

  /**
  * This accesses the name of the parent class that will
  * be used as the base class for the Evaluator generated for
  * this rule. This is the same evaulator parent class that will
  * be used by all rules in this rule's parent rule set.
  *
  * @return the name of the parent class that will
  * be used as the base class for the Evaluator generated
  * from this Rule.
  *
  */
  public String getEvaluatorParentClass(){

     String result = null;

     if(parent != null){

        result = parent.getEvaluatorParentClass(); 

     }

     return result;

  }

  /**
  * Constructs a String containing the field names
  * and values for this Rule.
  *
  * @return A String containing the field names
  * and values for this Rule.
  */
  public String toString(){

     StringBuffer result = new StringBuffer("\n");
     result.append("Rule ID:     [");
     result.append(id);
     result.append("]\nDescription: [");
     result.append(description);
     result.append("]\nContext:     [");
     result.append(context);
     result.append("]\nCondition:   [");
     result.append(condition);
     result.append("]\nAssertion:   [");
     result.append(assertion);
     result.append("]\nComments:    [");
     result.append(comments);
     result.append("]\n");

     return result.toString();

  }

}
package com.nightfire.framework.rules;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* Implementations of this interface will evaluate 
* whether the a given input meets
* some criteria (such as a rule or set of rules).
* 
*/
public interface Evaluator {

   /**
   * Determines whether the given source follows the 
   * rule(s) enforced by this Evaluator or not. 
   *
   * @param source The input source to be evaluated.
   * @param errors Any RuleErrors that occur while during 
   *               evaluation will be added to this collection.
   *
   * @return Whether the evaluation was successful.
   */
   public abstract boolean evaluate( String source,
                                     ErrorCollection errors );

   /**
   * Determines whether the given source follows the 
   * rule(s) enforced by this Evaluator or not. 
   *
   * @param source The input source to be evaluated.
   * @param errors Any RuleErrors that occur while during 
   *               evaluation will be added to this collection.
   * @param ruleContext  RuleContext object shared by the rules
   *               
   *
   * @return Whether the evaluation was successful.
   */
   public abstract boolean evaluate( String source,
                                     ErrorCollection errors, RuleContext ruleContext );
  
  
} 
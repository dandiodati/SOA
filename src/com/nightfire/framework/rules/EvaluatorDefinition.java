package com.nightfire.framework.rules;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This interface should be implemented by classes that describe how to
* create an Evaluator class.
*/
public interface EvaluatorDefinition {

  /**
  * Gets the package name for an Evaluator generated for this 
  * EvaluatorDefinition.
  *
  * @return The String package name for a generated Evaluator.
  */
  public String getEvaluatorPackage();

  /**
  * Returns the simple class name (without the package) for the Evaluator
  * that should be generated for this EvaluatorDefinition.
  *
  * @return a String that is the simple class name for an Evaluator created
  *         for this EvaluatorDefinition.
  */
  public String getEvaluatorClassName();

  /**
  * Returns the full class name for the Evaluator that 
  * should be generated for this EvaluatorDefinition. 
  *
  * @return The full classname (the package name and the simple class name)
  *         for this EvaluatorDefinition.
  */  
  public String getFullEvaluatorClassName();

  /**
  * Returns the ID or Name of this Evaluator.
  */
  public String getName();

} 
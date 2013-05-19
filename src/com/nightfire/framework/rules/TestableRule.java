package com.nightfire.framework.rules;

import java.util.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import java.io.FileNotFoundException;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class is a Rule which also has a set of associated
* RuleTests that may be executed.
*/
public class TestableRule extends Rule{

  /**
  * The set of tests for this Rule.
  */
  private TestCollection tests = new TestCollection();

  /**
  * This creates a testable rule with all null field values.
  */
  public TestableRule() {

     super();

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
  public TestableRule(String  ID,
                      String  description,
                      String  context,
                      String  condition,
                      String  assertion,
                      RuleSet parent,
                      String  comments){

     super(ID, description, context, condition, assertion, parent, comments);

  }  

  /**
  * Adds a test for this Rule.
  *
  * @param test the test to be added.
  */
  public void addTest(RuleTest test){

     tests.add(test);

  }

  /**
  * Gets the list of tests for this Rule.
  *
  * @return the collection of tests for this Rule.
  */ 
  public TestCollection getTests(){

     return tests;

  }

  /**
  * Executes the list of tests for this Rule.
  *
  * @param errors Any RuleErrors that are created by failed tests will
  *               be added to this collection.
  *
  * @return The number of tests that failed.
  */ 
  public int runTests(ErrorCollection errors){

     return runTests(errors, null);

  }

  /**
  * Executes the list of tests for this Rule.
  *
  * @param errors Any RuleErrors that are created by failed tests will
  *               be added to this collection.
  * @param reloadPath If this value is not null, it indicates that
  *                   the Evaluator classes may have been recompiled since
  *                   this JVM was started, and the Evaluators should
  *                   be reloaded from this path.
  *
  * @return The number of tests that failed.
  */
  public int runTests(ErrorCollection errors, String reloadPath){

     int failureCount = 0;

     synchronized(tests){

        int count = tests.getSize();

        for(int i = 0; i < count; i++){

           if( ! runTest(i, errors, reloadPath) ){

              failureCount++;

           }

        }

     }

     return failureCount;

  }


  /**
  * Executes the numbered test for this Rule.
  *
  * @param testNumber the index of the test to be executed.
  * @param errors Any RuleErrors that are created by failed tests will
  *               be added to this collection.
  *
  * @return true if the test was successful, false otherwise.
  */ 
  public boolean runTest(int testNumber, ErrorCollection errors){

     return runTest(testNumber, errors, null);

  }

  /**
  * Executes the numbered test for this Rule.
  *
  * @param testNumber the index of the test to be executed.
  * @param errors Any RuleErrors that are created by failed tests will
  *               be added to this collection.
  * @param reloadPath If this value is not null, it indicates that
  *                   the Evaluator classes may have been recompiled since
  *                   this JVM was started, and the Evaluators should
  *                   be reloaded from this path.
  *
  * @return true if the test was successful, false otherwise.
  */
  public boolean runTest(int testNumber, ErrorCollection errors, String reloadPath){

     boolean success = false;

     RuleTest test = tests.getElementAt(testNumber);

     Debug.log(Debug.RULE_EXECUTION, this+"Executing test number ["+
                                     (testNumber+1)+"]"+test);

     // An error message with 1-based index to indicate this test failed
     String failed = "Test case number "+(testNumber+1)+" failed. ";

     ErrorCollection tempErrs = new ErrorCollection();

     try{

        success = RuleEngine.evaluate(this, test.getContent(), tempErrs, reloadPath);

        if(success){

           if(! test.isPositive() ){

              // This was a negative test case but it passed. So, really,
              // the test failed.
              success = false;
              errors.addError(new RuleError(getID(),
                                            failed+getDescription(),
                                            getContext() ));

           }

        }
        else{

           if( test.isPositive() ){

              // This was a positive test case, but it failed. Mark as a
              // failure and copy any errors over.

              RuleError[] errs = tempErrs.getErrors();
              String errMessage;
              for(int i = 0; i < errs.length; i++){
                 errMessage = errs[i].getMessage();
                 errMessage = failed + errMessage;
                 errs[i].setMessage(errMessage);
                 errors.addError(errs[i]);
              }

           }
           else{

              // This was a negative test case. It was meant to fail.
              success = true;

           }

        }

     }
     catch(FrameworkException fex){

        errors.addError( new RuleError(getID(),
                                       failed+"This rule could not be evaluated: ",
                                       fex ) );

     }

     Debug.log(Debug.RULE_EXECUTION,"Test successful: ["+success+"]");

     return success;

  }

  /**
  * This validates all of this Rule's XML test data using DTD
  * from the file with the given <code>dtdFileName</code>.
  *
  * @param errors if the test is invalid, a RuleError will be added to this
  *               collection describing the problem.
  * @param dtdFileName the name of the DTD file that should be used to validate
  *                    this test's XML.
  *
  * @return the number of tests that were invalid.
  *
  * @throws FileNotFoundException if the named DTD file does not exist. 
  */
  public int validateTests(ErrorCollection errors, String dtdFileName)
                           throws FileNotFoundException{

     int failureCount = 0;

     synchronized(tests){

        int count = tests.getSize();

        for(int i = 0; i < count; i++){

           if(! validateTest(i, errors, dtdFileName) ){

              failureCount++;

           }

        }

     }

     return failureCount;

  }

  /**
  * This validates all of this Rule's XML test data using DTD
  * from the file with the given <code>dtdFileName</code>.
  *
  * @param testNumber the index of the test whose validity will be checked.
  * @param errors if the test is invalid, a RuleError will be added to this
  *               collection describing the problem.
  * @param dtdFileName the name of the DTD file that should be used to validate
  *                    this test's XML.
  *
  * @return whether or not the test is valid.
  *
  * @throws FileNotFoundException if the named DTD file does not exist.  
  */
  public boolean validateTest(int testNumber,
                              ErrorCollection errors,
                              String dtdFileName)
                              throws FileNotFoundException{

     RuleUtils.fileExists(dtdFileName);

     boolean valid = true;

     RuleTest test = tests.getElementAt(testNumber);  

     try{
        RuleUtils.validateXML(test.getContent(), dtdFileName);
     }
     catch(MessageException mex){

        String failureMessage = "Test case number "+
                                (testNumber+1)+
                                " is invalid. ";
        String message = mex.getMessage();

        // Format the DTD validation message to remove some of the
        // ugliness
        if(StringUtils.hasValue(message)){

           int messIndex = message.indexOf("Message [");
           if(messIndex != -1){
              message = message.substring(messIndex);
           }
           
        }
        
        errors.addError( new RuleError(getID(), failureMessage + message) );
        valid = false;

     }

     return valid;

  }

  /**
  * Creates a new TestableRule with the same field values as this one.
  * This also copies the set of tests associated with this TestableRule.
  *
  * @return a copy of this Rule.
  */
  public Rule copy(){

     TestableRule copy = new TestableRule(id, description, context, condition, assertion, parent, comments);

     synchronized(tests){

        for(int i = 0; i < tests.getSize(); i++){

           copy.addTest( tests.getElementAt(i).copy() );

        }

     }

     return copy;

  }


}
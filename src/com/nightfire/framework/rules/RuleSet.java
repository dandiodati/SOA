package com.nightfire.framework.rules;

import java.util.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* The RuleSet class contains a list of Rules. 
*/
public class RuleSet implements EvaluatorDefinition{

    /**
    * The name of this RuleSet.
    */
    private String name;

    /**
    * The list of child rules contained in this set.
    */
    private Vector rules;

    /**
    * The name of the package under which this RuleSet's generated Evaluator
    * classes will be placed. The default package is "rules".
    */
    private String basePackageName = RuleConstants.RULES;

    /**
    * The Evaluator created for this rule will determine if this RuleSet
    * should be evaluated or skipped entirely. This is useful for checking
    * to see if a form exists before evaluating all of the rules for that
    * form. 
    */
    private Rule trigger = null;

    /**
    * The name of a DTD file that will be used to validate the
    * XML test data of the rules in this set. 
    */
    private String dtdFile = null;

    /**
    * This is the name of the class that will be set as the parent class
    * for the Evaluators generated for the rules in this set. 
    */
    private String evaluatorParentClass = null;

    /**
     * Create a Rule-Set with the given name.
     *
     * @param  name  Name of the Rule-Set.
     */
    public RuleSet ( String name )
    {
        Debug.log( this, Debug.RULE_LIFECYCLE, "Creating Rule-Set object named \"" + name + "\"" );

        this.name  = name;
        rules  = new Vector( );
    }


    /**
     * Get a copy of the Rule-Set.
     *
     * @return  New Rule-Set object that is a copy of the current one.
     */
    public RuleSet copy ( )
    {
        if(Debug.isLevelEnabled(Debug.RULE_LIFECYCLE))
        Debug.log( this, Debug.RULE_LIFECYCLE, "Creating a copy of Rule-Set named \"" + name + "\"" );

        // Create new Rule-Set object.
        RuleSet rs = new RuleSet( name );

        int len = rules.size( );

        // Copy current Rule-Set's rules to new Rule-Set.

        for (Object rule : rules) {
            Rule r = (Rule) rule;

            rs.add(r.copy());
        }

        // copy the trigger rule
        rs.setTrigger( getTrigger().copy() );

        // copy the associated dtd file
        rs.dtdFile = getDTDFile();

        return rs;
    }


    /**
    * Inserts the given rule and the specified index. If the index is
    * out of bounds, then a runtime exception will occur.
    *
    * @param r The Rule to be inserted.
    * @param index The index at which to insert the Rule.
    */
    public void insert (Rule r, int index){

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
        Debug.log( this, Debug.RULE_EXECUTION, "Inserting Rule \"" +
                   r.getID() + "\" into Rule-Set \"" + name + "\" at index: "+
                   index );
        r.setParent(this);
        rules.add(index, r);

    }

    /**
     * Add the given Rule to the Rule-Set's list of rules.
     *
     * @param  r  Rule to append to Rule-Set.
     */
    public void add ( Rule r )
    {
        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
        Debug.log( this, Debug.RULE_EXECUTION, "Adding Rule \"" +
                   r.getID() + "\" to Rule-Set \"" + name + "\"" );

        r.setParent(this);
        rules.add( r );
    }

    /**
     * Remove the given Rule to the Rule-Set's list of rules.
     *
     * @param  r  Rule to remove from Rule-Set.
     *
     * @return true if the given Rule was removed, false if the Rule
     *         did not exist in this RuleSet to begin with.
     */    
    public boolean remove(Rule r){

        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
       Debug.log( this, Debug.RULE_EXECUTION, "Removing Rule \"" +
                  r.getID() + "\" from Rule-Set \"" + name + "\"" );

       if( rules.remove(r) ){
          r.setParent(null);
          return true;
       }

       return false;

    }

    /**
    * Checks to see if this RuleSet contains a Rule with the given
    * <code>ruleID</code>.
    *
    * @param ruleID the rule ID to be found.
    *
    * @return true if there exists at least one Rule with the given
    *         <code>ruleID</code>, false otherwise.
    */
    public boolean contains(String ruleID){

       if(ruleID == null) return false;

       boolean success = false;

       // please don't add or remove rules while we are doing this
       synchronized(rules){

          String id;

           for (Object rule : rules) {
               id = ((Rule) rule).getID();
               if (id.equals(ruleID)) {
                   success = true;
                   break;
               }

           }

       }

       return success;

    }

    /**
     * Get the Rule-Set's name.
     *
     * @return  The Rule-Set's name.
     */
    public String getName ( )
    {
        return name;
    }

    /**
     * Sets the Rule-Set's name.
     *
     * @param name  The Rule-Set's name.
     */    
    public void setName (String name)
    {
        this.name = name;
    }


    /**
     * Log description of Rule-Set to the diagnostic log.
     */
    public void log ( )
    {
        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
        Debug.log( this, Debug.RULE_EXECUTION, this.toString() );
    }

    /**
    * Gets an array of the Rules currently contained in this RuleSet.
    *
    * @return an array of Rules.
    */
    public Rule[] getRules(){

       Rule[] ruleArray = null;

       try{

          int len = rules.size();
          ruleArray = new Rule[len];
          rules.toArray(ruleArray);

       }
       catch(Exception ex){

          Debug.log(Debug.ALL_ERRORS, "Could not get array of rules: "+ex);

       }

       return ruleArray;

    }

    /**
    * Returns a String that lists the name of this RuleSet as well as the
    * String version of every Rule contained in this RuleSet.
    */
    public String toString(){

       StringBuffer result = new StringBuffer("\nRule Set: ");
       result.append(name);
       result.append("\n");
       result.append("DTD File: ");
       result.append(dtdFile);
       result.append("\n");
       result.append("Evaluator Parent Class: ");
       result.append(evaluatorParentClass);
       result.append("\n");       

        for (Object rule : rules) {

            result.append(rule.toString());

        }
        result.append("\n");

       return result.toString();

    }

    /**
    * Sets the base package name (e.g. "com.nightfire") for any Evaluators
    * that get generated from this RuleSet definition.
    *
    * @param base A valid Java package name (e.g. "com.nightfire").
    */
    public void setBasePackageName(String base){

       basePackageName = base;

    }

    /**
    * Gets the base package name that should be used when generating Evaluator
    * classes based on this RuleSet.
    *
    * @return String Java package name.
    */
    public String getBasePackageName(){

       return basePackageName;

    }

    /**
    * Gets the full package name for the Evaluator generated for this rule
    * set. This package name is assembled by appending a dot , ".", and this
    * RuleSet's name to the base package name.
    * This method is defined for the EvaluatorDefinition interface.    
    */
    public String getEvaluatorPackage(){

       return basePackageName+"."+getEvaluatorClassName();

    }

    /**
    * The class name (with the full package name) for the Evaluator
    * generated for this RuleSet.
    * This method is defined for the EvaluatorDefinition interface.
    *
    * @return The name of this RuleSet with all non-alphanumeric characters
    *         replaced by underscores "_".
    */
    public String getEvaluatorClassName(){

       return RuleUtils.makeAlphaNumeric( getName() );

    }

    /**
    * Gets the full class name (including package name)
    * for the Evaluator generated for this RuleSet.
    * This method is defined for the EvaluatorDefinition interface.
    *
    * @return The Evalutor package and the Evalutaor class name joined by a "."
    *         in the middle. 
    */
    public String getFullEvaluatorClassName(){

       return getEvaluatorPackage()+"."+getEvaluatorClassName();

    }

    /**
    * This runs the tests for any TestableRules contained in this RuleSet.
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
    * This runs the tests for any TestableRules contained in this RuleSet.
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

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
       Debug.log(Debug.RULE_EXECUTION,"Running tests for rule set ["+name+"]");

       int failureCount = 0;

       synchronized(rules){

          Object current;

          for (Object rule : rules) {

               current = rule;
               if (current instanceof TestableRule) {

                   failureCount += ((TestableRule) current).runTests(errors, reloadPath);

               }

           }

       }

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
       Debug.log(Debug.RULE_EXECUTION,"["+failureCount+"] tests failed");

       return failureCount;

    }


    /**
    * Accesses the trigger Rule for this RuleSet. If this Rule evaluates
    * to true, then the entire RuleSet gets executed. If false, then this
    * RuleSet will be skipped at evaluation time.
    *
    * @return the trigger Rule.
    */
    public Rule getTrigger(){

       return trigger;

    }

    /**
    * Assigns the trigger Rule for this RuleSet. If this Rule evaluates
    * to true, then the entire RuleSet gets executed. If false, then this
    * RuleSet will be skipped at evaluation time.
    *
    * @param contingency the trigger Rule upon which execution of this RuleSet
    *                    is contingent.
    */
    public void setTrigger(Rule contingency){

       trigger = contingency;

    }


    /**
    * This accesses the name of the DTD file that will be used when the
    * <code>validateTestData()</code> method is called.
    *
    * @return the name of the current DTD file being used for test XML
    * validation. null will be returned if this field has not been set.
    *
    */
    public String getDTDFile(){

       return dtdFile;

    }

    /**
    * This sets the name of the DTD file that will be used in
    * validating the test XML data.
    *
    * @param filename the path to a DTD file.
    *
    */
    public void setDTDFile(String filename){

       dtdFile = filename;

    }

    /**
    * This accesses the name of the parent class that will
    * be used as the base class for Evaluators
    * generated for the rules in this set
    *
    * @return the name of the parent class that will
    * be used as the base class for Evaluators
    * generated for the rules in this set.
    *
    */
    public String getEvaluatorParentClass(){

       return evaluatorParentClass;

    }

    /**
    * This sets the name of the parent class that will
    * be used as the base class for Evaluators
    * generated for the rules in this set.
    *
    * @param parentClassName the name of the parent class that will
    *                        be used as the base class for Evaluators
    *                        generated for the rules in this set.
    *
    */
    public void setEvaluatorParentClass(String parentClassName){

       evaluatorParentClass = parentClassName;

    }

    /**
    * This validates any XML test data for the rules in this rule set using the
    * currently set <code>dtdFile</code>.
    *
    * @param errors a collection of any validation errors that occur.
    *
    * @return the number of test cases which contained invalid XML.
    *
    * @throws FileNotFoundException if the dtdFile currently set for this
    *                               rule set does not exist.
    */
    public int validateTestData(ErrorCollection errors)
                                throws FileNotFoundException{

       if(dtdFile != null){
          return validateTestData(errors, dtdFile);
       }

        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
       Debug.log(Debug.RULE_EXECUTION,
                 "Rule set ["+
                 name+
                 "] does not have a set DTD file to validate against." );
                 
       return 0;

    }

    /**
    * This validates any XML test data for the rules in this rule set using the
    * DTD file with the given name.
    *
    * @param errors a collection of any validation errors that occur.
    *
    * @return the number of test cases which contained invalid XML.
    *
    * @throws FileNotFoundException if the named DTD file does not exist.     
    */    
    public int validateTestData(ErrorCollection errors, String useThisDTDFile)
                                throws FileNotFoundException{

        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
       Debug.log(Debug.RULE_EXECUTION,
                 "Validating test data for rule set ["+
                 name+"]");

       int failureCount = 0;

       synchronized(rules){

          Object current;

          for (Object rule : rules) {

               current = rule;
               if (current instanceof TestableRule) {

                   failureCount += ((TestableRule) current).validateTests(errors,
                           useThisDTDFile);

               }

           }

       }

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION))
       Debug.log(Debug.RULE_EXECUTION,"["+failureCount+"] tests are invalid");

       return failureCount;

    }

}


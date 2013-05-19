package com.nightfire.framework.rules;

import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class contains utility methods for translating XML rule definitions
* into Rule and RuleSet instances, and for translating Rules and RuleSets
* into a XML representation.
*/
public abstract class Rules2XML {

   public static final String ROOT_NAME      = RuleConstants.RULES;

   public static final String RULE_SET       = "ruleset";
   public static final String RULE_NODE      = "rulenode";
   public static final String RULE           = "rule";
   public static final String ID             = "id";
   public static final String DESCRIPTION    = "description";
   public static final String CONTEXT        = "context";
   public static final String ASSERTION      = "assertion";
   public static final String CONDITION      = "condition";
   public static final String COMMENTS       = "comments";   
   public static final String NAME_ATTR      = "name";
   public static final String TEST           = "test";
   public static final String TEST_CONTAINER = "test_container";
   public static final String TYPE           = "type";
   public static final String POSITIVE       = "positive";
   public static final String NEGATIVE       = "negative";
   public static final String DATA           = "data";
   public static final String TRIGGER        = "trigger";
   public static final String DTD            = "dtd";
   public static final String PARENT         = "parent_class";         

   private static final String CDATA_OPEN    = "<![CDATA[";
   private static final String CDATA_CLOSE   = "]]>";   
   private static final String XML_INDICATOR = "<?xml";
   private static final String XML_HEADER    = "<?xml version=\"1.0\"?>\n";

   /**
   * This takes a List containing RuleSet instances and generates the
   * appropriate XML for these RuleSets and their Rules.
   *
   * @param ruleSets A List that should contain only instances
   *                 of RuleSet.
   * @return An XML definition of the RuleSets in the given List.
   * @throws FrameworkException if an element of the <code>ruleSets</code>
   *         List is not an instance of RuleSet or if there is an error
   *         while generating the XML.
   */
   public static String toXML(List ruleSets) throws FrameworkException{

      XMLMessageGenerator gen = new XMLMessageGenerator(ROOT_NAME);

      synchronized(ruleSets){

         int length = ruleSets.size();
         RuleSet rules;

         for(int i = 0; i < length; i++){

            rules = null;

            try{

               rules = (RuleSet) ruleSets.get(i);
               
               int offset = 0;

               // Write out a node containing the DTD file name
               String dtdFile = rules.getDTDFile();
               if( StringUtils.hasValue(dtdFile) ){
                  gen.setValue(RULE_SET+"("+i+")."+DTD, dtdFile);
                  offset++;
               }

               // Write out a node containing the rule evaluator parent
               // class name
               String parent = rules.getEvaluatorParentClass();
               if( StringUtils.hasValue(parent) ){
                  gen.setValue(RULE_SET+"("+i+")."+PARENT, parent);
                  offset++;
               }

               // Write out the "trigger" rule, if there is one.
               Rule trigger = rules.getTrigger();
               if(trigger != null){
                  setRuleXML(trigger, RULE_SET+"("+i+")."+TRIGGER+"("+offset+").", gen);
                  // if there is a trigger, the rule indices will have to
                  // be offset by one so that they all end up in the
                  // right place in the XML.
                  offset++;
               }

               Rule[] array = rules.getRules();

               Rule rule = null;
               String ruleNodePath = null;
               String path = null;
               String testpath = null;

               for(int j = 0; j < array.length; j++){

                  rule = array[j];
                  ruleNodePath = RULE_SET+"("+i+")."+RULE_NODE+"("+(j+offset)+")";
                  path = ruleNodePath+"."+RULE+".";
                  testpath = ruleNodePath+"."+TEST_CONTAINER;

                  Debug.log(Debug.MSG_STATUS, rule.toString());

                  setRuleXML(rule, path, gen);

                  if(rule instanceof TestableRule){

                     TestCollection testData = ((TestableRule) rule).getTests();
                     RuleTest test;
                     int positiveCount = 0;
                     int negativeCount = 0;
                     String content;
                     CDATASection cdata;
                     Node testNode;
                     String fullTestPath;

                     for(int k = 0; k < testData.getSize(); k++){

                        test = testData.getElementAt(k);
                        content = CDATA_OPEN+test.getContent()+CDATA_CLOSE;

                        fullTestPath = testpath +"."+TEST+"("+k+")";

                        // Set the test content value as CDATA
                        setCDATA(gen, fullTestPath, DATA, test.getContent());
                        // Set the test description
                        gen.setTextValue(fullTestPath+"."+DESCRIPTION, getSafeValue(test.getDescription()));

                        if(test.isPositive()){
                           gen.setAttributeValue(fullTestPath, TYPE, POSITIVE);
                        }
                        else{
                           gen.setAttributeValue(fullTestPath, TYPE, NEGATIVE);
                        }

                     }

                  }

               }

               gen.setAttributeValue(RULE_SET+"("+i+")",
                                     NAME_ATTR,
                                     rules.getName() );

            }
            catch(ClassCastException ccex){

               throw new FrameworkException("Element "+i+
                                            ": ["+ruleSets.get(i)+
                                            "] of list ["+ruleSets+
                                            "] is not a RuleSet.");

            }
            catch(MessageException mex){

               throw new FrameworkException("Could not create XML: "+
                                            mex.getMessage());

            }

         }

      }

      return gen.generate();

   }

   /**
   * This method sets the fields of the given rule on the given generator
   * relative to the given path.
   *
   * @param rule the source rule.
   * @param path the location in the XML under which the fields should
   *             be generated.
   * @param gen the target generator. 
   *
   * @throws MessageException if a field cannot be set on the given generator.
   */
   private static void setRuleXML(Rule rule, String path, XMLMessageGenerator gen)
                                  throws MessageException{

       gen.setTextValue(path+ID,getSafeValue(rule.getID()));
       gen.setTextValue(path+DESCRIPTION,getSafeValue(rule.getDescription()));
       gen.setTextValue(path+CONTEXT,getSafeValue(rule.getContext()));

       // These fields will be CDATA to avoid XML parsing problems
       setCDATA(gen, path, ASSERTION,   getSafeValue(rule.getAssertion()).trim() );
       setCDATA(gen, path, CONDITION,   getSafeValue(rule.getCondition()).trim() );
       setCDATA(gen, path, COMMENTS, getSafeValue(rule.getComments()).trim() );

   }

   /**
   * This takes an XML definition of some RuleSet(s) and translates it into
   * instances of RuleSet and its Rules.
   * @return a List of the created RuleSet objects.
   */
   public static List toRuleSets(String xml) throws FrameworkException{

      List results = new ArrayList();

      XMLMessageParser parser = new XMLMessageParser(xml);

      String name;
      int count = 0;
      String path = RULE_SET+"("+count+")";

      // iterate through each rule set
      while( parser.exists(path) ){

          // create the rule set
          name = parser.getAttributeValue( path, NAME_ATTR );
          Debug.log(Debug.MSG_STATUS, "Creating Rule Set ["+name+"]");
          RuleSet set = new RuleSet( name );

          getDTDFile(set, path+"."+DTD, parser);
          getEvaluatorParentClass(set, path+"."+PARENT, parser);          

          // get the trigger rule, if there is one
          Rule trigger = null;
          String triggerPath = path +"."+TRIGGER;
          if( parser.exists(triggerPath) ){

             trigger = new Rule();

             try{
                 populateRule(trigger, triggerPath, parser);
             }
             catch(NullPointerException noID){
                 // The rule ID was null
                 trigger.setID(name+"-"+TRIGGER);

             }

             // Set the trigger for this rule set.
             set.setTrigger(trigger);

          }

          int subcount  = 0;
          
          // create all the rules
          String subpath = path +"."+RULE_NODE+"("+subcount+")."+RULE;
          // We need to check for the trigger node, so that we can
          // skip over it.
          String triggerPathCheck = triggerPath+"("+subcount+")";

          while( parser.exists(subpath) || parser.exists(triggerPathCheck) ){

              Node trigNode = parser.getNode(triggerPathCheck);
              // Only get rule node if this node is not the trigger node,
              // otherwise skip it.
              if(trigNode == null || !trigNode.getNodeName().equals(TRIGGER) ){

                  TestableRule rule = new TestableRule();

                  try{
                     populateRule(rule, subpath, parser);
                  }
                  catch(NullPointerException noID){
                     // The rule ID can not be null
                     throw new FrameworkException("The following rule at index ["+
                                                  subcount+"] of rule set ["+
                                                  count+"] has an empty ID value: "+
                                                  rule.toString());
                  }

                  // Get any test cases
                  int testcount = 0;
                  String testpath = path +"."+RULE_NODE+"("+subcount+")."+
                                    TEST_CONTAINER+"."+TEST+"(0)";              
                  String   type;
                  boolean  positiveTest;
                  String   content;
                  String   description;
                  RuleTest test;

                  while( parser.exists( testpath ) ){

                     try{
                        type = parser.getAttributeValue(testpath, TYPE);
                        positiveTest = type.equals(POSITIVE);
                     }
                     catch(MessageException mex){
                        Debug.log(Debug.ALL_ERRORS, "No ["+TYPE+
                                                    "] attribute set for path ["+
                                                    testpath+"]");
                        positiveTest = true;
                     }

                     content = getTextValue(testpath+"."+DATA, parser);
                     // Trim the content and check that it begins with the <?xml
                     // XML header
                     content = content.trim();
                     if( ! content.startsWith(XML_INDICATOR) ){
                        content = XML_HEADER + content;
                     }

                     description = getTextValue(testpath+"."+DESCRIPTION, parser);

                     test = new RuleTest(content, positiveTest, description);
                     Debug.log(Debug.MSG_STATUS, test.toString() );
                     rule.addTest(test);

                     testcount++;
                     testpath =  path +"."+RULE_NODE+"("+subcount+")."+
                                 TEST_CONTAINER+"."+TEST+"("+testcount+")";

                  }

                  set.add(rule);

              }

              subcount++;
              subpath  = path +"."+RULE_NODE+"("+subcount+")."+RULE;
              triggerPathCheck = triggerPath+"("+subcount+")";              

          }

          results.add(set);

          count++;
          path = RULE_SET+"("+count+")";

      }

      return results;

   }

   /**
   * This is a utility method for retrieving the name of a DTD file from
   * a rule set's XML definition. The DTD file is for use in validating
   * test XML for rules within the rule set. If a <code>dtd</code> node is found
   * in the given parser, the value of that node will be set as the given
   * RuleSet's DTD. The node will then be removed from the parser to avoid
   * its confusion with rule nodes later on.
   *
   * @param set The RuleSet whose DTD file will be set.
   * @param path The path to the dtd node in the parser.
   * @param parser a parsed XML containing the definition of the rule set.
   *
   * @throws FrameworkException if there is an error getting or removing
   * the value of the given <code>path</code>.
   */
   protected static void getDTDFile(RuleSet set,
                                    String path,
                                    XMLMessageParser parser)
                                    throws FrameworkException {

      if(parser.exists(path)){

         try{

             String dtdFile = parser.getValue(path);

             Debug.log(Debug.RULE_LIFECYCLE, "Setting DTD file ["+
                                             dtdFile+
                                             "] for Rule Set ["+
                                             set.getName()+"]");

             set.setDTDFile(dtdFile);

             // Remove the node
             parser.removeNode(path);

         }
         catch(MessageException mex){

            throw new FrameworkException(
                      "An error occured while getting the DTD file name for Rule Set ["+
                      set.getName()+
                      "]: "+
                      RuleUtils.getExceptionMessage(mex));

         }

      }

   }


   /**
   * This is a utility method for retrieving the name of the evaluator parent
   * class from
   * a rule set's XML definition. The parent class is used as the
   * based class when generating rule Evaluators for this rule set.
   * If a <code>parent_class</code> node is found
   * in the given parser, the value of that node will be set as the given
   * RuleSet's parent evaluator class.
   * The node will then be removed from the parser to avoid
   * its confusion with rule nodes later on.
   *
   * @param set The RuleSet whose parent evaluator class name will be set.
   * @param path The path to the evaluator "parent_class" node in the parser.
   * @param parser a parsed XML containing the definition of the rule set.
   *
   * @throws FrameworkException if there is an error getting or removing
   * the value of the given <code>path</code>.
   */
   protected static void getEvaluatorParentClass(RuleSet set,
                                                 String path,
                                                 XMLMessageParser parser)
                                                 throws FrameworkException {

      if(parser.exists(path)){

         try{

             String evaluatorClass = parser.getValue(path);

             Debug.log(Debug.RULE_LIFECYCLE, "Setting evaluator parent class ["+
                                             evaluatorClass+
                                             "] for Rule Set ["+
                                             set.getName()+"]");

             set.setEvaluatorParentClass(evaluatorClass);

             // Remove the node
             parser.removeNode(path);

         }
         catch(MessageException mex){

            throw new FrameworkException(
                      "An error occured while getting the evaluator parent class for Rule Set ["+
                      set.getName()+
                      "]: "+
                      RuleUtils.getExceptionMessage(mex));

         }

      }

   }   

   /**
   * This method takes a given rule object and populates its fields
   * from the path in the given parser.
   *
   * @param rule the Rule to be populated.
   * @param path the path to the XML nodes which contain the values for the
   *             rule's fields.
   * @param parser the parsed XML from which to get the field values. 
   *
   */
   protected static void populateRule(Rule rule,
                                      String path,
                                      XMLMessageParser parser){

        rule.setComments(getTextValue(path+"."+COMMENTS, parser).trim());                                      
        rule.setAssertion(getTextValue(path+"."+ASSERTION, parser).trim());
        rule.setCondition(getTextValue(path+"."+CONDITION, parser).trim());
        rule.setContext(getTextValue(path+"."+CONTEXT, parser));
        rule.setDescription(getTextValue(path+"."+DESCRIPTION, parser));
        rule.setID(getTextValue(path+"."+ID, parser));

   }


   /**
   * Gets the text value of the specified node's path out of the
   * given source if the path exists.
   *
   * @param path The path from which to get the text value.
   * @param source The parsed XML source from which to get the value.
   *
   * @return The value of the specified text node or an empty String if the
   *         node has no text value.
   */
   private static String getTextValue(String path, XMLMessageParser source){

      String result = "";

      try{

         if(source.nodeExists(path)) result = source.getTextValue(path);

      }
      catch(MessageException mex){

         Debug.log(Debug.MSG_WARNING, "There is no text value for node ["+
                                      path+"]: "+mex.getMessage() );

      }

      return result;

   }

   /**
   * Creates a CDATA section with the given node <code>name</code> and
   * <code>value</code> and sets it at the given pathin the given generator.
   */
   private static void setCDATA(XMLMessageGenerator gen,
                                String path,
                                String name,
                                String value)
                                throws MessageException {

      String fullPath = path+"."+name;
      gen.create(fullPath);

      // Only create the CDATA section if there is a value
      if(StringUtils.hasValue(value)){

         Node node  = gen.getNode(fullPath);
         CDATASection cdata = gen.getDocument().createCDATASection(name);
         cdata.setData(value);
         node.appendChild(cdata);
         
      }

   }

   /**
   * This is used to make sure that a String value is not null.
   *
   * @return "" if <code>s</code> is null, or the original
   *         <code>s</code> value otherwise
   */
   private static String getSafeValue(String s){

      return (s == null) ? "" : s; 

   }

   public static void main(String[] args){

      String usage = "java "+Rules2XML.class.getName()+"[-verbose] <xml rules file name>";

      if(args.length < 1 || args.length > 2){

         System.err.println(usage);
         System.exit(-1);

      }

      String fileName = null;
      boolean verbose = false;

      for(int i = 0; i < args.length; i++){

         if(args[i].equals("-verbose")){
            verbose = true;
         }
         else{
            fileName = args[i];
         }

      }

      if(fileName == null){

         System.err.println(usage);
         System.err.println("No file name specified.");
         System.exit(-1);

      }

      if(verbose){
         Debug.enableAll();
      }
      else{
         Debug.enable(Debug.ALL_ERRORS);
      }

      System.out.println("Reading file ["+fileName+"]");

      try{

         String xml = FileUtils.readFile(fileName);
         System.out.println("Converting XML to Rule Sets");
         List ruleSets = toRuleSets(xml);

         System.out.println("Converted to Rule Sets:\n");         
         for(int i = 0; i < ruleSets.size(); i++){

            System.out.println(ruleSets.get(i));

         }

         System.out.println("Converting back to XML...");
         String result = toXML(ruleSets);

         System.out.println("XML: ["+result+"]");

      }
      catch(Exception ex){

         ex.printStackTrace();

      }

   }

} 
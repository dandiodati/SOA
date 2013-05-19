package com.nightfire.framework.rules;

import java.util.*;

import com.nightfire.framework.util.Debug;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class is used to contain any RuleErrors 
* that may be created during rule evaluation.
*/
public class ErrorCollection {

  // These are constants used to populate the XML version of an ErrorCollection
  public static final String ROOT           = "Errors";
  public static final String CONTAINER      = "ruleerrorcontainer";
  public static final String NODE           = "ruleerror";
  public static final String ID             = "RULE_ID";
  public static final String MESSAGE        = "MESSAGE";
  public static final String CONTEXT        = "CONTEXT";
  public static final String CONTEXT_VALUE  = "CONTEXT_VALUE";
  public static final String NODE_PATH      = CONTAINER+"."+NODE;
  public static final String HALT_MESSAGE_EXEC_NODE      = "HALT_MESSAGE_EXECUTION";
  private boolean setHaltMessageExecNode = false;
  
  /**
  * The list of RuleError instances.
  */
  private List errors;

  /**
  * This contructor creates an empty ErrorCollection.
  */
  public ErrorCollection() {

     errors = new ArrayList();

  }
  
  public ErrorCollection(boolean setHaltMessageExecNode)
  {
    this();
    this.setHaltMessageExecNode = setHaltMessageExecNode;
  }

  /**
  * Adds a RuleError to the collection. This is not a synchronized operation.
  *
  * @param error The RuleError to be added.  
  */
  public void addError(RuleError error){

     errors.add(error);

  }

  /**
  * Returns an array of the errors in this
  * collection.
  *
  * @return a RuleError array of the RuleErrors
  *         in this collection. 
  */ 
  public RuleError[] getErrors(){

     RuleError[] result = null;

     try{

        int length = errors.size();
        result = new RuleError[length];
        errors.toArray( result );

     }
     catch(Exception ex){

        Debug.log(Debug.ALL_ERRORS, "Could not get error array: "+
                                    RuleUtils.getExceptionMessage(ex) );

     }

     return result;  

  }

  /**
  * This gets an Iterator instance for cycling through
  * the current list of error.
  *
  * @return An Iterator for the list of RuleErrors.
  */ 
  public Iterator getIterator(){

     return errors.iterator();

  }

  /**
  * This iterates through each of the errors contained
  * in the collection, calls <code>toString()</code>
  * on each RuleError, and then appends that to the
  * resulting String that is then ultimately returned.
  *
  * @return a String listing of all the errors in this 
  *         collection. 
  */
  public String toString(){

     StringBuffer result = new StringBuffer();
     Iterator iter = getIterator();

     while( iter.hasNext() ){

        result.append( iter.next().toString() );

     }

     return result.toString();

  }


  /**
  * Converts this error collection to its XML format.
  *
  * @return the XML respresentation of this error collection
  */
  public String toXML(){

     return toXML(this);

  }

  /**
  * Converts the given error collection to its XML format.
  *
  * @param errors an error collection.
  * @return the XML respresentation of the given error collection. If the
  *         XML could not be generated or if the given <code>errors</code>
  *         object is null, then a null value will be returned.
  */
  public static String toXML(ErrorCollection errors){

     String result = null;

     try{

        XMLMessageGenerator generator = new XMLMessageGenerator(ROOT);
        generator.create(CONTAINER);

        RuleError[] errorArray = errors.getErrors();
        String currentPath;

        for(int i = 0; i < errorArray.length; i++){

           currentPath = NODE_PATH+"("+i+").";

           generator.setValue(currentPath+ID,            getSafeValue(errorArray[i].getID()) );
           generator.setValue(currentPath+MESSAGE,       getSafeValue(errorArray[i].getMessage()) );
           generator.setValue(currentPath+CONTEXT,       getSafeValue(errorArray[i].getContext()) );
           generator.setValue(currentPath+CONTEXT_VALUE, getSafeValue(errorArray[i].getContextValue()) );
        }

        if(errors.setHaltMessageExecNode)
            generator.setValue(HALT_MESSAGE_EXEC_NODE, Boolean.TRUE.toString());

        result = generator.generate();

     }
     catch(MessageException mex){

        Debug.log(Debug.ALL_ERRORS,
                  "Could not generate XML for error collection: "+
                  errors+
                  mex.getMessage());

     }
     catch(NullPointerException npex){

        Debug.log(Debug.ALL_ERRORS,
                  "Could not generate XML for error collection: "+
                  errors+
                  npex);

     }

     return result;

  }

  /**
  * Converts the given XML to its equivalent ErrorCollection.
  *
  * @param xml an XML message representing an Error Collection.
  * @return an error collection containing the errors from the
  *         given XML. If the given XML can not be parsed,
  *         then an empty ErrorCollection will be returned.
  *
  */
  public static ErrorCollection toErrorCollection(String xml){

     ErrorCollection result = new ErrorCollection();

     try{

        XMLMessageParser parser = new XMLMessageParser();
        parser.parse(xml);
        String currentPath, id, mess, context, value;

        // While there are more "ruleerror" nodes
        for(int count = 0; parser.exists(NODE_PATH+"("+count+")"); count++){

           currentPath = NODE_PATH+"("+count+").";
           id      = parser.getValue(currentPath+ID);
           mess    = parser.getValue(currentPath+MESSAGE);
           context = parser.getValue(currentPath+CONTEXT);
           value   = parser.getValue(currentPath+CONTEXT_VALUE);

           result.addError( new RuleError(id, mess, context, value) );

        }

     }
     catch(MessageException mex){

        Debug.log(Debug.ALL_ERRORS,
                  "Could not parse an error collection from XML: "+
                  xml+"\n"+mex.getMessage());

     }

     return result;

  }

  /**
  * If the original value is null, then this returns an empty string. If
  * the original string has value, then it is returned unchanged.
  */
  private static String getSafeValue(String original){

     return (original == null) ? "" : original;

  }

  /**
  * For testing conversion to XML and back again.
  */
  public static void main(String[] args){

    try{

     String xml = com.nightfire.framework.util.FileUtils.readFile(args[0]);

     System.out.println("Read XML:\n"+xml);

     ErrorCollection test = ErrorCollection.toErrorCollection(xml);

     System.out.println("Converted XML to:\n"+test);

     xml = ErrorCollection.toXML(test);

     System.out.println("Converted back to XML:\n"+xml);

    }
    catch(Exception ex){

       ex.printStackTrace();

    }

  }


}
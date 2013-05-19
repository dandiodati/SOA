package com.nightfire.framework.rules;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This structure holds data about an error that occurred
* during rule evaluation.
*/
public class RuleError {

  private static final String ID            = "RULE ID: ";
  private static final String MESSAGE       = "MESSAGE: ";
  private static final String CONTEXT       = "CONTEXT: ";
  private static final String CONTEXT_VALUE = "VALUE:   ";

  /**
  * The Rule ID of the rule whose evaluation has failed.
  */
  private String id;

  /**
  * The message that should accompany this error. This is usually the
  * rule description.
  */
  private String message;

  /**
  * The XPath context within which the rule failed, if available. 
  */
  private String context;

  /**
  * The value of the context node, if it is available and has a value.
  */
  private String contextValue;

  public RuleError(String id, String mess, Exception ex){

     this(id, mess + RuleUtils.getExceptionMessage(ex) );

  }

  public RuleError(String id, String mess) {

     this(id, mess, (String) null);

  }

  public RuleError(String id, String mess, String context, Exception ex){

     this(id, mess + RuleUtils.getExceptionMessage(ex), context );

  }

  public RuleError(String id, String mess, String context, String value, Exception ex){

     this(id, mess + RuleUtils.getExceptionMessage(ex), context, value );

  }

  public RuleError(String id, String mess, String context) {
  
     this(id, mess, context, (String) null);
     
  }


  public RuleError(String id, String mess, String context, String value) {

     this.id = id;
     this.message = mess;
     this.context = context;
     this.contextValue = value;
     
  }    

  /**
  * Returns a String that lists the current field values for this error. 
  */
  public String toString(){

     StringBuffer result = new StringBuffer(ID);
     result.append(id);
     result.append("\n");
     result.append(MESSAGE);
     result.append(message);
     result.append("\n");
     result.append(CONTEXT);
     result.append(context);
     result.append("\n");
     result.append(CONTEXT_VALUE);
     result.append("[");
     result.append(contextValue);
     result.append("]");

     result.append("\n\n");

     return result.toString();     

  }

  /**
  * Gets the ID of the Rule for which this error occurred.
  *
  * @return the Rule ID.
  */
  public String getID(){
     return id;
  }
                        
  /**
  * Sets the ID of the Rule for which this error occurred.
  *
  * @param the Rule ID.
  */
  public void setID(String ID){
     id = ID;
  }

  /**
  * Gets the error message.
  *
  * @return the error message.
  */
  public String getMessage(){
     return message;
  }
                              
  /**
  * Sets the error message.
  *
  * @param the error message.
  */
  public void setMessage(String mess){
     message = mess;
  }

  /**
  * Gets the context for which this error was created.
  */
  public String getContext(){
     return context;
  }

  /**
  * Sets the current context in which this error 
  * occurred.
  */
  public void setContext(String ctx){
     this.context = ctx;
  }
    
  /**
  * Gets the value of the context (if any).
  *
  * @return the value of the context. Empty values ("" or null) are possible.
  */
  public String getContextValue(){
     return contextValue;
  }

  /**
  * Sets the value of the context (if any) at the time the error occurred.
  *
  * @param value the value of the context.
  */
  public void setContextValue(String value){
     this.contextValue = value;
  }

}
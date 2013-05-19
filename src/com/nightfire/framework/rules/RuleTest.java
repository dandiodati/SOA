package com.nightfire.framework.rules;

import com.nightfire.framework.message.parser.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class contains the data for testing a Rule. The test can be flagged as
* either positive or negative.
*/
public class RuleTest {

  /**
  * The data that will tested.
  */
  private String content;

  /**
  * An optional description of what the test is all about.
  */
  private String description;

  /**
  * Flag indicating if this is a positive or negative test case.
  */
  private boolean positive;

  /**
  * This constructs a positive test with the given content.
  *
  * @param content the test data
  */
  public RuleTest(String content) {

     this(content, true);

  }


  /**
  * This constructs a RuleTest.
  *
  * @param content the test data
  * @param positive whether this is a positive or negative test.
  */
  public RuleTest(String content, boolean positive) {

     this(content, positive, "");

  }


  /**
  * This constructs a RuleTest.
  *
  * @param content the test data
  * @param positive whether this is a positive or negative test.
  * @param description a description of this test
  */
  public RuleTest(String content, boolean positive, String description){

     this.content     = content;
     this.positive    = positive;
     setDescription(description);

  }

  /**
  * Gets the data content of this test.
  *
  * @return the content
  */
  public String getContent(){

     return content;

  }

  /**
  * Sets the data content of this test.
  *
  * @param content the new content
  */
  public void setContent(String content){

     this.content = content;

  }

  /**
  * Check if this is a positive test case or not.
  *
  * @return true if this is a positive test case, false otherwise.
  */  
  public boolean isPositive(){

     return positive;

  }

  /**
  * Set whether this is a positive test case or not.
  *
  * @param b true if this will be a positive test case, false otherwise.
  */    
  public void setPositive(boolean b){

     positive = b;

  }

  /**
  * Gets the description of this test.
  *
  * @return the description.
  */
  public String getDescription(){

     return description;

  }

  /**
  * Sets the description of this test.
  *
  * @param the new description of this test.
  */
  public void setDescription(String description){

     if(description == null) description = "";
     this.description = description;

  }  

  /**
  * This returns a String that lists the description of this test,
  * whether it is positive or negative and the test data content of this
  * test.
  */
  public String toString(){

     StringBuffer result = new StringBuffer("\nTest Description: [");
     result.append(description);
     result.append("]\nPositive: [");
     result.append(positive);
     result.append("]\n");
     result.append("Test Content:\n");
     result.append(content);
     result.append("\n");     

     return result.toString();  

  }

  /**
  * Creates a new copy of this RuleTest.
  */
  public RuleTest copy(){

     return new RuleTest(content, positive, description);

  }

}
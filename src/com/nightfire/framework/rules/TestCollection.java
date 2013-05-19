package com.nightfire.framework.rules;

import java.util.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* A collection of RuleTest objects.
*/
public class TestCollection{

  /**
  * The list of tests.
  */
  private List tests = new Vector();


  /**
  * Adds a RuleTest.
  */
  public void add(RuleTest test){

     tests.add(test);

  }


  /**
  * Gets the RuleTest at the given index.
  */  
  public RuleTest getElementAt(int index){

     return (RuleTest) tests.get(index);

  }

  /**
  * Gets the number of elements in this collection.
  */
  public int getSize(){

     return tests.size();

  }

  /**
  * Removes the given test.
  *
  * @return true if the test was removed successfully, false if it was not
  *         part of the collection to begin with.
  */
  public boolean remove(RuleTest test){

     return tests.remove(test);

  }
  
} 
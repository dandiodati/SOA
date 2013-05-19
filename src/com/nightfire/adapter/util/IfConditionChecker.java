package com.nightfire.adapter.util;

import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import java.util.*;

 /**
   * This class takes in a Vector of name value pairs that it uses to check the MessageDataSource
   * object with.
   */
    public class IfConditionChecker implements ConditionInterpreter
  {

   private String nullIndicator = "NULL";

   /**
     * set the the null indicator
     * default is NULL
     */
    public void setNullIndicator(String str) {
       nullIndicator = str;
    }
    
   /**
     * get the the null indicator
     */
    public String getNullIndicator(String str) {
       return( nullIndicator );
    }

     /**
      * Does an AND operation on the pairs, using short circuit evaluation
      * If any of the name value pairs have a value of "NULL", then the name should
      * not exist, should have an empty value, or be null in the MessageDataSource
      * EXAMPLE:
      * one = blah & two = blah & three = NULL   - the only way this would return true is if
      *                                            one and two has values of blah and three has
      *                                            no value, empty value or null in the MessageDataSource
      */
     public boolean getAndAnswer(Vector pairs, MessageDataSource src) throws MessageException
     {

           for (int i = 0; i < pairs.size();i++ ) {
              NVPair pair = (NVPair) pairs.get(i);
              String name = pair.name;
              String value = (String)pair.value;
              //System.out.println("name " + name + " value " + value);
              if (!src.exists(name) && !value.equals(nullIndicator) ) {
                return false;
              } else if (src.exists(name) ) {
                 if ( StringUtils.hasValue(src.getValue(name))
                                                              && value.equals(nullIndicator) ) {
                   return false;
                 } else if (!value.equals(src.getValue(name)) && !value.equals(nullIndicator) ) {
                    return false;
                 }
              }
           }

           return true;
      }

      /**
      * Does an OR operation on the pairs, using short circuit evaluation
      */
      public boolean getOrAnswer(Vector pairs, MessageDataSource src) throws MessageException
      {

           for (int i = 0; i < pairs.size();i++ ) {
              NVPair pair = (NVPair) pairs.get(i);
              String name = pair.name;
              String value = (String)pair.value;

              if (!src.exists(name) && value.equals(nullIndicator) ) {
                 return true ;
              } else if (src.exists(name) ) {
                 if ( !StringUtils.hasValue(src.getValue(name))
                                                              && value.equals(nullIndicator) ) {
                    return true;
                 } else if ( value.equals(src.getValue(name)) ) {
                    return true ;
                 }
              }
           }

           return false;
        }
  }


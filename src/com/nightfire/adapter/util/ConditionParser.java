package com.nightfire.adapter.util;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.*;
import java.util.*;


/**
 * This class parses a condtional string and uses an ConditionInterpreter to return
 * the answer.
 * EXAMPLE default condition strs
 *  1. one=blah&two=blah&three=blah  - this is an AND condtion with fields one, two, three
 * 2. one=blah|two=blah|three=blah   - this is an OR condtion with fields one, two, three
 */
public class ConditionParser {

    private String fieldValueSeparator = "=";       // default settings
    private String andPairSeparator = "&";
    private String orPairSeparator = "|";


    private MessageDataSource source;
    private Vector pairsContainer;

    private final static int AND_MODE = 1;
    private final static int OR_MODE = 2;
    private int mode;

     /**
      * This assumes you want to parse any type of MessageDataSource object
      * @param fieldValuePairs - refer to default condition string at top
      * @param src any MessageDataSource class that can provide data.
      */
     public ConditionParser (String fieldValuePairs, MessageDataSource src) throws MessageException,  NoSuchElementException
     {
         this(fieldValuePairs, src, null,null,null);
     }


     /**
      * This assumes you want to parse any type of MessageDataSource object with different separator values
      * @param fieldValuePairs - refer to default condition string at top
      * @param src any MessageDataSource class that can provide data.
      * @param fieldValueSep - the field name and value pair separator. If null, then uses default.
      * @param andPairSep  - a pair separator to determine if an AND operation should occur.If null, then uses default.
      * @param orPairSep  - a pair separator to determine if an OR operation should occur. If null, then uses default.
      */
     public ConditionParser (String fieldValuePairs, MessageDataSource src, String fieldValueSep, String andPairSep, String orPairSep) throws MessageException,  NoSuchElementException
     {
        if (fieldValueSep != null) {
           fieldValueSeparator = fieldValueSep;
        }

        if (andPairSep != null) {
           andPairSeparator = andPairSep;
        }

        if (orPairSep != null) {
           orPairSeparator = andPairSep;
        }

        parse(fieldValuePairs, src);

     }


     private void parse (String fieldValuePairs, MessageDataSource src) throws MessageException, NoSuchElementException
     {
        String separator = null;
        StringTokenizer tok = null;
        pairsContainer = new Vector();


        if (fieldValuePairs.indexOf(andPairSeparator) > -1 )  {
           tok = new StringTokenizer(fieldValuePairs, andPairSeparator);
           mode = AND_MODE;
           Debug.log(this,Debug.MSG_STATUS, "ConditionParser- Using AND mode");

        } else if (fieldValuePairs.indexOf(orPairSeparator) > -1 )  {
           tok = new StringTokenizer(fieldValuePairs, orPairSeparator);
           mode = OR_MODE;
           Debug.log(this,Debug.MSG_STATUS, "ConditionParser- Using OR mode");
        }

        while (tok.hasMoreTokens() ) {
           String token = tok.nextToken();
           StringTokenizer st = new StringTokenizer(token, fieldValueSeparator);
           String name = st.nextToken();
           String value = st.nextToken();
           Debug.log(this,Debug.MSG_PARSE, "ConditionParser -adding field[" + name + "], value["
                                            + value + " to internal container");
           pairsContainer.add(new NVPair(name, value) );
        }

        source = src;

     }

     /** execute to get an answer
     * @param inter the class the checks the if condition
     */
     public boolean isTrue(ConditionInterpreter inter) throws MessageException, FrameworkException {

        if (mode == AND_MODE) {
           return ( inter.getAndAnswer(pairsContainer, source) );
        } else if (mode == OR_MODE) {
           return ( inter.getOrAnswer(pairsContainer, source) );
        } else {
           return false;
        }
     }

  }


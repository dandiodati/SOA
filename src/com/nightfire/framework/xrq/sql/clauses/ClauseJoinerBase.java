package com.nightfire.framework.xrq.sql.clauses;


import org.w3c.dom.*;

import com.nightfire.framework.xrq.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;

import com.nightfire.framework.util.*;
import java.util.*;


/**
 * Base class for clauses which join several clauses together, such as an AND or OR.
 */
public abstract class ClauseJoinerBase extends ClauseObject
{


  /**
   * Adds operator (defined by getClauseSeparator() ) to do different type of joining of clauses. child classes
   * will defined the type of join.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string. Returns an empty string if there
   * were no sub clauses.
   */
  public String eval (Node clauseNode) throws MessageException, FrameworkException
  {

     StringBuffer andStr = new StringBuffer();

     Node[] children = XMLMessageBase.getChildNodes(clauseNode);
     String name;
     StringBuffer clauses = new StringBuffer();

     List childList = evalClauses(children);

     if (childList.isEmpty() )  {
        Debug.log(Debug.MSG_GENERATE, clauseName + " Missing child clauses returning empty clause" );
         if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, describeClauseResults(""));
        return "";
     }

     Iterator childIter = childList.iterator();

     andStr.append((String)childIter.next());

     while (childIter.hasNext() ) {
         andStr.append(" " + getClauseSeparator() + " ");
        andStr.append((String)childIter.next() );
     }


     String str = andStr.toString();

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS,describeClauseResults(str));

     return (str );
  }

  /**
   * Evaluates all sub clauses, returns a list of Clauses that returned a result.
   *
   */
  private List evalClauses(Node [] children) throws MessageException, FrameworkException {

     String temp;
     List cList = new ArrayList();

     for(int i = 0; i < children.length; i++ ) {
        temp = evalSubClause( children[i] );
        if (StringUtils.hasValue(temp) ) {
           cList.add(temp);
        }
     }

     return cList;

  }

  /**
   * child classes overload this method to define the separator used to join the
   * Clauses together. (Such as AND).
   * @returns The string to use as a separator.
   */
  protected abstract String getClauseSeparator();



}
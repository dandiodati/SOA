package com.nightfire.framework.xrq.sql.clauses;

import org.w3c.dom.*;

import com.nightfire.framework.xrq.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import java.util.*;



/**
 * Groups sub clauses together via parenthesises.
 */
public class Group extends ClauseObject
{

/**
   * groups sub clauses together if there are sub clauses, otherwise returns an empty string.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed, or if the statement node is missing.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public String eval (Node clauseNode) throws FrameworkException, MessageException
  {

     StringBuffer groupStr = new StringBuffer("( ");

     String subClauseStrs = evalAllSubClauses(clauseNode);
     if (!StringUtils.hasValue(subClauseStrs) ) {
        Debug.log(Debug.MSG_GENERATE, clauseName + " no sub clauses, returning an empty string");
        if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
           Debug.log(Debug.MSG_STATUS, describeClauseResults(""));

        return "";
     }

     groupStr.append(subClauseStrs);
     groupStr.append(" )");

     String str = groupStr.toString();

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(str));

     return (str );
  }


}

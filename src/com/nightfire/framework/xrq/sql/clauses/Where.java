package com.nightfire.framework.xrq.sql.clauses;

import com.nightfire.framework.xrq.*;
import org.w3c.dom.*;

import com.nightfire.framework.message.MessageException;

import java.util.*;
import com.nightfire.framework.xrq.utils.*;

import com.nightfire.framework.util.*;


/**
 * Build an sql where clause.
 */
public class Where extends ClauseObject
{


  public String eval (Node clauseNode) throws FrameworkException, MessageException
  {

     StringBuffer errors = new StringBuffer();

     StringBuffer whereStr = new StringBuffer("WHERE ");

     String subClauseStrs = evalAllSubClauses(clauseNode);
      if (!StringUtils.hasValue(subClauseStrs) ) {
        Debug.log(Debug.MSG_GENERATE, clauseName + " no sub clauses, returning an empty string");
        return "";
     }

     whereStr.append(subClauseStrs);

      String str = whereStr.toString();
     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(str));

     return (str );
  }
} 
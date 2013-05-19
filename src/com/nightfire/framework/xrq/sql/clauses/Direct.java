package com.nightfire.framework.xrq.sql.clauses;


import com.nightfire.framework.xrq.*;
import org.w3c.dom.*;

import com.nightfire.framework.message.MessageException;

import java.util.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.util.*;


/**
 * Clause object which can take an SQL statement directlty.
 * This allows the flexiblity to create an sql statement and pass it in to the
 * XRQ architecture.
 */
public class Direct extends ClauseObject
{

  public static final String STATEMENT_NODE = "statement";


  /**
   * returns the direct query string in statement node. This allows direct creation
   * of a destination query.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed, or if the statement node is missing.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public String eval(Node clauseNode) throws FrameworkException, MessageException
  {
     HashSet fields = new HashSet();
     fields.add(STATEMENT_NODE);
     StringBuffer errors = new StringBuffer();

     ChainedHashMap nodes = getReqRuntimeValues(clauseNode, fields, errors);

     if (errors.length() > 0 ) {
        Debug.error("Direct: failed to create sql string : " + errors.toString() );
        throw new MessageException(errors.toString() );
     }


     String str = nodes.getFirst(STATEMENT_NODE);

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(str));

     return (str);
  }


}

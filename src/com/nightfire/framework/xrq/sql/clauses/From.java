package com.nightfire.framework.xrq.sql.clauses;

import com.nightfire.framework.xrq.*;
import org.w3c.dom.*;

import com.nightfire.framework.message.MessageException;
import java.util.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.util.*;


/**
 * Handles the creation of a sql From clause.
 */
public class From extends ClauseObject
{
  public static final String FIELD_NODE = "field";

  private Set reqFields;

  /**
   * initalizes this class. Sets up properties, required, and optional fields.
   *
   * @param clauseName - the clause name to used as identification, and used as a node
   * in the XRQ xml request.
   * @param props - The static properties associated with this clause object.
   *
   * @throws FrameworkException if the Clause fails to in
   */
  public void initialize(String clauseName, Map props) throws FrameworkException
  {
    super.initialize(clauseName, props);

    // add list of required fields
    reqFields = new HashSet();
    reqFields.add(FIELD_NODE);

  }


  /**
   * evaluates a From clause and creates the sql from part of a statement.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed, or if the statement node is missing.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public String eval (Node clauseNode) throws FrameworkException, MessageException
  {

     StringBuffer errors = new StringBuffer();

     StringBuffer fromStr = new StringBuffer("FROM ");
     ChainedHashMap nodes = getReqRuntimeValues(clauseNode, reqFields, errors);

     if (errors.length() > 0 ) {
        Debug.error("From: failed to create sql string : " + errors.toString() );
        throw new MessageException(errors.toString() );
     }


     List fieldNodes = (List)nodes.get(FIELD_NODE);

     for (int i = 0; i < fieldNodes.size(); i++ ) {
        if ( i > 0 )
           fromStr.append(", ");

        fromStr.append((String)fieldNodes.get(i));

     }

     String str = fromStr.toString();

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(str) );

     return (str );
  }


}

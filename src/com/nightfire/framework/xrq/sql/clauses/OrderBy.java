package com.nightfire.framework.xrq.sql.clauses;


import com.nightfire.framework.xrq.*;
import org.w3c.dom.*;

import com.nightfire.framework.message.MessageException;
import java.util.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.util.*;


/**
 * Creates the order by sql statement.
 */
public class OrderBy extends ClauseObject
{
  public static final String FIELD_NODE = "field";
  public static final String ASCENDING_NODE = "ascending";

  public static final String ASCENDING_DEFAULT_PROP = "DEFAULT_ASCENDING_ORDER";


  private Set reqFields;
  private Set optFields;
  private boolean ascDir;


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

    optFields = new HashSet();
    optFields.add(ASCENDING_NODE);

    String temp;

    try {
       temp  = PropUtils.getRequiredPropertyValue(props, ASCENDING_DEFAULT_PROP);
    } catch (FrameworkException e) {
       throw new FrameworkException(clauseName + ": " + e.getMessage());
    }

    try {
          ascDir = StringUtils.getBoolean(temp);
    } catch (FrameworkException e) {
       throw new FrameworkException(clauseName + " : " + e.getMessage() );
    }

  }


  /**
   * creates the sql ORDER BY part of an sql statement. If there are no
   * fields defined then an empty string is returned.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed, or if the statement node is missing.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public String eval(Node clauseNode) throws FrameworkException, MessageException
  {

     StringBuffer errors = new StringBuffer();

     StringBuffer orderByStr = new StringBuffer("ORDER BY ");


     ChainedHashMap nodes = getReqRuntimeValues(clauseNode, reqFields, errors);

     if (errors.length() > 0 ) {
        Debug.log(Debug.MSG_GENERATE, clauseName + " no sub clauses, returning an empty string");
        return "";
     }

     List fieldNodes = (List)nodes.get(FIELD_NODE);

     for (int i =0; i < fieldNodes.size(); i++ ) {
        if ( i > 0 )
           orderByStr.append(", ");

        orderByStr.append((String)fieldNodes.get(i));
     }
     
     if (isAscending(clauseNode) ) {
        orderByStr.append(" ASC");
     } else {
        orderByStr.append(" DESC");
     }

     String str = orderByStr.toString();

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(str) );

     return (str );
  }



  private boolean isAscending(Node clauseNode) throws MessageException {

    ChainedHashMap optNodes = getOptRuntimeValues(clauseNode, optFields);

    String ascStr = optNodes.getFirst(ASCENDING_NODE);
    boolean asc;

    if (StringUtils.hasValue(ascStr) ) {
      try {
        asc = StringUtils.getBoolean(ascStr);
      } catch (FrameworkException e) {
         throw new MessageException(clauseName + ": " + ASCENDING_NODE + ", " + e.getMessage() );
      }
      return asc;
    } else
       return ascDir;

  }


}

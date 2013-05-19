package com.nightfire.framework.xrq.sql.clauses;


import com.nightfire.framework.xrq.*;
import org.w3c.dom.*;

import com.nightfire.framework.message.MessageException;
import java.util.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.util.*;


/**
 * builds a sql Select clause
 */
public class Select extends ClauseObject
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
   * build the sql query clause from the xml clause node.
   */
  public String eval (Node clauseNode) throws FrameworkException, MessageException
  {

     StringBuffer errors = new StringBuffer();

     StringBuffer selectStr = new StringBuffer("SELECT ");
     ChainedHashMap nodes = getReqRuntimeValues(clauseNode, reqFields, errors);
     if (errors.length() > 0 ) {
        Debug.error("Select: failed to create sql string : " + errors.toString() );
        throw new MessageException(errors.toString() );
     }
     List fieldNodes = (List) nodes.get(FIELD_NODE);

     for (int i = 0; i < fieldNodes.size(); i++ ) {
        if ( i > 0 )
           selectStr.append(", ");

        selectStr.append((String)fieldNodes.get(i));
     }

     String str = selectStr.toString();
     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS,describeClauseResults(str));

     return (str );
  }


}

package com.nightfire.framework.xrq;

import org.w3c.dom.*;
import java.util.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;


public interface Clause
{
  
  public static final String CLAUSE_CLASS_PROP = "CLAUSE_CLASS";

  /**
   * initalizes the Clause obj.
   * @param clauseName - the clause name to used as identification, and used as a node
   * in the XRQ xml request.
   * @param props - The static properties associated with this clause object.
   *
   * @throws FrameworkException if the Clause fails to in
   */
  public void initialize(String clauseName, Map props) throws FrameworkException;

  /**
   * evaluates the clause and converts to the specific query string.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public String eval(Node clauseNode) throws MessageException, FrameworkException;

} 
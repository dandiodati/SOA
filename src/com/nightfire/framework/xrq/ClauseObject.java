package com.nightfire.framework.xrq;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.message.MessageException;


import  com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.db.*;

import com.nightfire.framework.util.*;



import com.nightfire.framework.xrq.utils.*;



/**
 * Abstract Base class which provides basic functionality for Clauses.
 */
public abstract class ClauseObject implements Clause
{
  protected Map props;
  protected String clauseName;


  public static final String SUBCLAUSE_SORT_PROP = "SUBCLAUSE_SORT";
  public static final String SUBCLAUSE_EVAL_ALL_PROP = "SUBCLAUSE_EVAL_ALL";

  private List orderedSubClauses;
  private boolean evalAllSubClauses = true;
  private ClauseFactory factory;


  /**
   * Used to set the factory on the root clause object.
   * NOTE: This must be set by by the creating class, or a null pointer exception will result.
   */
  protected void setClauseFactory(ClauseFactory factory)
  {
     this.factory = factory;
  }

  /**
   * initalizes the Clause obj.
   * @param clauseName - the clause name to used as identification, and used as a node
   * in the XRQ xml request.
   * @param props - The static properties associated with this clause object.
   * If the SUBCLAUSE_EVAL_ALL_PROP property is not defined, then it defaults to true. This
   * indicates that all nodes under clauseName should be evaluated in no particular order.
   * @see evalSubClause
   *
   * @throws FrameworkException if the Clause fails to initialize.
   */
  public void initialize(String clauseName, Map props) throws FrameworkException
  {
     this.props = props;
     this.clauseName = clauseName;

     String temp = PropUtils.getPropertyValue(props,SUBCLAUSE_EVAL_ALL_PROP, "true" );

    try {
       evalAllSubClauses = StringUtils.getBoolean(temp);
     } catch (FrameworkException fe) {
       throw new FrameworkException(clauseName +": " + fe.getMessage());
     }
     Debug.log(Debug.SYSTEM_CONFIG, clauseName + ": Evaluate all sub clauses? [" + evalAllSubClauses + "]");
     // define a list so that it is never null. The size is checked in evalAllSubClauses
     orderedSubClauses = new ArrayList();

     for (int i =0; true; i++ ) {
        temp = PropUtils.getPropertyValue(props,PersistentProperty.getPropNameIteration(SUBCLAUSE_SORT_PROP,i) );
        if ( !StringUtils.hasValue(temp) )
           break;

        orderedSubClauses.add(temp);
     }

     if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
        Debug.log(Debug.SYSTEM_CONFIG, clauseName + ": Ordered SubClauses [" + orderedSubClauses.toString() + "]");

  }



  /**
   * evaluates the clause and converts to the specific query string.
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public abstract String eval(Node clauseNode) throws MessageException, FrameworkException;


   /**
   * Obtains all runtime nodes under the parent node clauseNode. Each node that is obtained
   * is removed from the parent clauseNode.
   *
   * If nodes are not found they are skipped, and no exception is thrown.
   *
   *
   * @param clauseNode - The parent node of all runtime nodes.
   * @param fieldsToObtain - The runtime nodes to search for.
   *
   * @throws MessageException - if the xml message is malformed.
   */
  protected final ChainedHashMap getOptRuntimeValues(Node clauseNode, Set fieldsToObtain) throws MessageException
  {
    ChainedHashMap map = new ChainedHashMap();

      Node[] children;
      String name;
     try {

       children = XMLMessageBase.getChildNodes(clauseNode);
       for (int i =0; i < children.length; i++ ) {
         name = children[i].getNodeName();
         if (fieldsToObtain.contains(name ) ) {
            try {
               Debug.log(Debug.MSG_STATUS,clauseName + ": Looking for field [" + name +"]");
               String value = XMLMessageBase.getNodeValue(children[i]);
               if (StringUtils.hasValue(value) && !value.trim().equals("''") ) {
                 map.put(name, XMLMessageBase.getNodeValue(children[i]) );
                 clauseNode.removeChild(children[i]);
               }
            } catch (MessageException me) {
               Debug.log(Debug.MSG_STATUS, clauseName + ": Could not obtain optional runtime node: "+ me.getMessage() );
            }
         }
       }
     } catch ( MessageException me) {
        Debug.warning(clauseName + ": Failed to obtain children nodes: "+ me.getMessage() );
        throw me;
     }

    return map;
  }

   /**
   * Obtains all required runtime nodes under the parent node clauseNode. Each node that is obtained
   * is removed from the parent clauseNode.
   *
   * @param clauseNode - The parent node of all runtime nodes.
   * @param fieldsToObtain - The required runtime nodes to search for.
   * @param errors - A list of missing required fields. Assumes that this string buffer has already been created.
   * The size of the buffer can be tested to check if any required fields were missing.
   *
   * @throws MessageException - if the xml message is malformed
   */
  protected final ChainedHashMap getReqRuntimeValues(Node clauseNode, Set fieldsToObtain, StringBuffer errors) throws MessageException
  {
    ChainedHashMap map = getOptRuntimeValues(clauseNode, fieldsToObtain );

    Set found = map.keySet();


    Set required = new HashSet(fieldsToObtain);

    required.removeAll(found);

    if (!required.isEmpty() ) {
       Iterator iter = required.iterator();
       errors.append("\n");
       while (iter.hasNext() ){
          String field = (String)iter.next();
          errors.append(clauseName + " : Missing Required Node [" + field + "]\n");
       }

    }

    return map;

  }


  /**
   * evaluates all child clauses of clauseNode. This assumes that all runtime nodes have been removed
   * from the xml. Sub nodes are evaluated either in a defined order or in no particular order depending
   * on the configured properties for a Clause.
   *
   * Parsing of sub clauses depends on the following property settings:
   * <UL>
   * <LI>
   * If SUBCLAUSE_SORT_PROP properties are defined(iterative property) and
   *  the SUBCLAUSE_EVAL_ALL_PROP property is true, then evaluation occurs
   * in the order specified by the SUBCLAUSE_SORT_PROP properties. And other sub clauses not specified
   * are evaluated in no particular order.
   * </LI>
   * <LI>
   * If SUBCLAUSE_SORT_PROP properties are defined(iterative property) and
   *  the SUBCLAUSE_EVAL_ALL_PROP property is false, then ONLY the clauses defined by the SUBCLAUSE_SORT_PROP
   * properties are evaluated in the order specified.
   * </LI>
   * <LI>
   * If SUBCLAUSE_SORT_PROP properties are not defined(iterative property) and
   *  the SUBCLAUSE_EVAL_ALL_PROP property is true, then all sub clauses are evaluated in no particular order
   * (This is the most common case).
   * </LI>
   * <LI>
   * If SUBCLAUSE_SORT_PROP properties are not defined(iterative property) and
   *  the SUBCLAUSE_EVAL_ALL_PROP propery is false, then no sub clauses will get evaluated.
   * In most cases this may only apply to leaf clauses. And is useful to make sure a leaf clause will
   * never evaluate any sub clauses.
   * </LI>
   * </UL>
   * <B>NOTE:Defining SUBCLAUSE_SORT_PROP properties are a slight performance hit, so they should only
   * be defined if order of sub clause evaluation is neccessary.</B>
   *
   * @param clauseNode - The parent node of all the sub clauses to look for.
   *
   * @throws MessageException - if the xml message is malformed.
   * @throws FrameworkException if there is a system error.
   */
  protected final String evalAllSubClauses(Node clauseNode) throws  MessageException, FrameworkException
  {
     Node[] children = XMLMessageBase.getChildNodes(clauseNode);

     LinkedList clauseList = new LinkedList(Arrays.asList(children) );

     String sortedNameToFind, clauseChildName;
     Node child;

     StringBuffer clauses = new StringBuffer();
     Iterator clauseChildIter;
     if ( orderedSubClauses.size() > 0 ) {
        Iterator sortedIter = orderedSubClauses.iterator();
        clauseChildIter = clauseList.iterator();

        while (sortedIter.hasNext() ) {
           sortedNameToFind = (String) sortedIter.next();
           // start at the beginning of the list
           clauseChildIter = clauseList.iterator();
           clauses.append(evalMatchingNode(sortedNameToFind, clauseChildIter) );
           // if there is a ordered clause list add the first one.
           if (sortedIter.hasNext() )
              clauses.append(" ");

        }
     }

     //start the sub clause iterator at the beginning
     clauseChildIter = clauseList.iterator();

     // add any nodes that were not specified in the orderedSubClause list, only if
     // evalAllSubClauses is true
     if (clauseChildIter.hasNext() && evalAllSubClauses) {
        child = (Node) clauseChildIter.next();

        // Add the first extra node, if no clauses where added, then add on at the beginning of the StringBuffer
        // otherwise separate with a space
        if (clauses.length() == 0 ) {
           clauses.append(evalSubClause(child));
        } else {
           clauses.append(" " + evalSubClause(child));
        }

        // add any others that are left, separated by spaces
        while( clauseChildIter.hasNext()) {
           child = (Node) clauseChildIter.next();
           clauses.append(" " + evalSubClause(child));
        }
     }
     
     return clauses.toString().trim();


  }

  // looks for the node nameToFind within the clauseSubNodes iterator.
  // If found the evalSubClause is called and the child is removed from clauseSubNodes iterator.
  private String evalMatchingNode(String nameToFind, Iterator clauseSubNodes) throws MessageException, FrameworkException
  {

        Node child;
        String clauseChildName;

        String results;
        while (clauseSubNodes.hasNext() ) {
           child = (Node) clauseSubNodes.next();
           clauseChildName = child.getNodeName();
           // only if the child is the next item in the sorted node list then add it
           if (clauseChildName.equals( nameToFind ) ) {
              results = evalSubClause(child);
              clauseSubNodes.remove();
              return results;
           }
        }

        return "";
  }

  /**
   * Evaluates a specific sub clause of a parent clause.
   * Uses the ClauseFactory to create the Clause specified by the name of the clauseSubNode,
   * then calls eval on that Clause Object.
   *
   * @param clauseSubNode - The parent node for a Clause object (used to identify the Clause).
   * @throws MessageException - if the xml message is malformed.
   * @throws FrameworkException if there is a system error.
   */
  protected final String evalSubClause(Node clauseSubNode) throws MessageException, FrameworkException
  {
     String name = clauseSubNode.getNodeName();


     if (factory == null) {
        String err = clauseName + " : Clause Object does not have a ClauseFactory set.";
        Debug.error(err);
        throw new FrameworkException(err);
     }
     
     Clause temp = factory.getClause(name);

     return (temp.eval(clauseSubNode) );
  }

  /**
   * Describes the generated statement, by this clause.
   * @param results The results that this clause produced.
   * @returns The string with a pretty printed message, that can be used for logging.
   *
   */
  public final String describeClauseResults(String results) {
     return ("\nCLAUSE RESULTS:\n NAME [" + clauseName + "]\n CLAUSE OBJ [" + getClass().getName() + "]\n RESULTS [" + results + "]\n");
  }

 







}
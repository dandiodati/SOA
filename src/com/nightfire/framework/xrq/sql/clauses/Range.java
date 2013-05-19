package com.nightfire.framework.xrq.sql.clauses;


import java.util.*;
import com.nightfire.framework.xrq.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.xrq.*;
import com.nightfire.framework.xrq.utils.*;

import org.w3c.dom.*;



/**
 * This is a leaf clause which can be used to retrieve data between two values.
 * Dates are supported.
 */
public class Range extends SQLDateFormattingBase
{

  public static final String FIELD_FROM_NODE= "fromValue";
  public static final String FIELD_TO_NODE= "toValue";
  public static final String FIELD_NODE= "field";



  private Set reqFields;

  private Set optFields;



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


    // build list of optional fields
    optFields = new HashSet();
    optFields.add(FIELD_TO_NODE);
    optFields.add(FIELD_FROM_NODE);
    optFields.add(XrqConstants.DATE_FORMAT_NODE);
    optFields.add(LITERAL_INDICATOR_NODE);

  }



  /**
   * evaluates a Range node and creates a range comparison clause.
   * Examples:
   * 1) PON >= 2
   * 2) PON <= 5
   * 3) PON bewteen 2 and 5
   * 3) DATE >= TO_DATE('11-23-2000','MM-DD-YYYY');
   *
   * @param clauseNode The parent node of the clause. This will be the node with the
   * name of the clause.
   * @throws MessageException - If the xrq xml request is malformed.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   *  NOTE: If any of the required fields are missing, then an empty string is returned.
   */
  public String eval(Node clauseNode) throws FrameworkException, MessageException
  {

     StringBuffer clauseStr = new StringBuffer();

     String result = makeComparison(clauseNode);

      if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(result));

     return (result );
  }


  // returns the date format node if it exists.
  private String getDateFormat(ChainedHashMap optNodes) throws MessageException, FrameworkException {

    String dFormat = optNodes.getFirst(XrqConstants.DATE_FORMAT_NODE);

    dFormat = setupDateFormat(dFormat);

    return dFormat;

  }

  /**
   * creates the comparison.
   * ALGORITHM:
   * If the fromValue and toValue both exist then an sql BETWEEN is used to find all
   * data between the values. If only fromValue is defined, then the data greater than or equal to
   * this value is returned. If only toValue is defined, then the data less than or equal to the is value
   * is returned.
   *
   * @throws MessageException if the xml is malformed or an incorrect operator is defined.
   */
  private String makeComparison(Node clauseNode) throws MessageException, FrameworkException
  {

     StringBuffer results = new StringBuffer();

     StringBuffer errors = new StringBuffer();


     ChainedHashMap reqNodes = getReqRuntimeValues(clauseNode, reqFields,errors);

     ChainedHashMap optNodes = getOptRuntimeValues(clauseNode, optFields);

     String dformat = getDateFormat(optNodes);

     String from    = optNodes.getFirst(FIELD_FROM_NODE);
     String to      = optNodes.getFirst(FIELD_TO_NODE);

     boolean isLiteral =  getLiteralIndicator(optNodes);

     if (errors.length() > 0 || (from == null && to == null) ) {
        Debug.log(Debug.MSG_GENERATE, clauseName + " Missing required fields, or both fromValue [" + from+ "] and toValue [" + to+ "] are empty, returning empty clause:" + errors.toString() );
        return "";
     }

     String field = reqNodes.getFirst(FIELD_NODE);

     if (StringUtils.hasValue(from) && StringUtils.hasValue(to) ) {
        results.append("( ");
        results.append(field);
        //results.append(" >= ");
        results.append(" between ");
        results.append(convertIfDate(from,dformat, isLiteral));
        results.append(" and ");
        //results.append(field);
        //results.append(" <= ");
        results.append(convertIfDate(to,dformat, isLiteral));
        results.append(" )");
     } else if (StringUtils.hasValue(from)) {
        results.append(field);
        results.append(" >= ");
        results.append(convertIfDate(from, dformat, isLiteral));
     } else if (StringUtils.hasValue(to) ) {
        results.append(field);
        results.append(" <= ");
        results.append(convertIfDate(to,dformat, isLiteral));
     }

     return results.toString();

  }



  /**
   * converts single or multi field values into an sql date if dateFormat is not null
   * and the str is not a literal.
   * @param str - the field value(s) to check.
   * @param the dateFormat field value.
   * @param noQuotes Indicates that the field should or should not be wrapped in quotes
   */
  private String convertIfDate(String str, String dateFormat, boolean noQuotes) throws MessageException
  {


     if (!noQuotes)
        str = ClauseUtils.addSqlQuotes(str);


     if (dateFormat != null && !ClauseUtils.isLiteral(str) )
        str = buildDateStr(str, dateFormat);
     return (str);
  }


} 
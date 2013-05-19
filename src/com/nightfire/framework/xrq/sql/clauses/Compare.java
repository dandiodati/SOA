package com.nightfire.framework.xrq.sql.clauses;

import com.nightfire.framework.message.*;

import com.nightfire.framework.util.*;

import com.nightfire.framework.xrq.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.locale.*;
import java.text.*;

import org.w3c.dom.*;
import java.util.*;


/**
 * This is a leaf clause used for creating a SQL comparison clause.
 *
 * It supports:
 * <UL>
 * <LI> several types of operations such as =, <, <=, >, or >= </LI>
 * <LI> Wild cards and position markers are supported and such queries get automatically converted
 * to sql like operations.</LI>
 * <LI>Date comparison and formatting</LI>
 * <LI>Multiple field comparison to multiple values</LI>
 * <LI>Values can be literals( other DB columns) or string values( identified by quotes around the value).</LI>
 * </UL>
 */
public class Compare extends SQLDateFormattingBase
{

  // posssible child xml nodes for this Clause.
  public static final String FIELD_VALUE_NODE= "fieldValues";
  public static final String FIELD_NODE= "field";
  public static final String TYPE_NODE = "type";

  // properties
  public static final String WILD_CARD_PROP = "WILD_CARD_CHAR";
  public static final String POSITION_PROP = "POSTION_MARKER_CHAR";

  //sql mappings
  public static final char SQL_WILD_CARD = '%';
  public static final char SQL_PATTERN_POSITION_MARKER = '_';


  private Set reqFields;
  private Set optFields;
  private String wildCard;
  private String placeMarker;


  private DateFormat dateFormatter;
  private DateFormat sqlDateFormatter;




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
    reqFields.add(FIELD_VALUE_NODE);
    reqFields.add(TYPE_NODE);

    // build list of optional fields
    optFields = new HashSet();
    optFields.add(XrqConstants.DATE_FORMAT_NODE);
    optFields.add(LITERAL_INDICATOR_NODE);

    StringBuffer err = new StringBuffer();
    String wildStr= null, posWildStr = null;

    wildCard = PropUtils.getRequiredPropertyValue(props, WILD_CARD_PROP, err);
    placeMarker = PropUtils.getRequiredPropertyValue(props, POSITION_PROP, err);

    if (err.length() > 0 ) {
       String errorStr = "Compare: " + err;
       Debug.error(errorStr);
       throw new FrameworkException(errorStr);
    }


  }



  /**
   * evaluates a Compare node and forms an sql comparison.
   * Examples:
   * 1) PON < 2
   * 2) PON like '123*B'
   * 3) DATE = TO_DATE('11-23-2000','MM-DD-YYYY');
   * 4) TYPE in ('response', 'request')
   *
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

     ChainedHashMap optNodes = getOptRuntimeValues(clauseNode, optFields);

     String sqlDateFormat = setupDateFormat(clauseNode, optNodes);
     String result = makeComparison(clauseNode, sqlDateFormat, optNodes);

      if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
       Debug.log(Debug.MSG_STATUS, describeClauseResults(result));

     return (result );
  }

  // returns the date format node if it exists.
  private String setupDateFormat(Node clauseNode,ChainedHashMap optNodes) throws MessageException, FrameworkException
  {

    Date date = null;

    String formatFlag = optNodes.getFirst(XrqConstants.DATE_FORMAT_NODE);

    String sqlDateFormat = null;

   return (setupDateFormat(formatFlag) );


  }

  /**
   * creates the comparison.
   * ALGORITHM:
   * 1. If the quotes_values node is present, then all values become non literals and are quoted.
   * 2. if there is only a single value(no comma separated values) and there is a wild card or position marker and the value is not a literal
   *    then do a like sql comparison.
   * 3. if the field value contains multiple values.
   *   if the operator is an equal sign(=) then do an in sql operation with all possible values.
   *   else if the operator is a not equal sign(!=) then do a not in sql operation with all possible values.
   *   else throw a message exception since operator is not allowed.
   * 4. Just do a sql comparison using the defined operator.
   * NOTE: If the DATE_FORMAT_NODE is defined then convert the fields value(s) into dates before creating comparison.
   *
   * @throws MessageException if the xml is malformed or an incorrect operator is defined.
   */
  private String makeComparison(Node clauseNode, String dFormat,ChainedHashMap optNodes) throws MessageException
  {

     StringBuffer results = new StringBuffer();

     StringBuffer warnings = new StringBuffer();


     ChainedHashMap reqNodes = getReqRuntimeValues(clauseNode, reqFields, warnings);

     if (warnings.length() > 0 ) {
        Debug.log(Debug.MSG_GENERATE, clauseName + " Missing required fields returning empty clause:" + warnings.toString() );
        return "";
     }


     String field = reqNodes.getFirst(FIELD_NODE);
     String op    = reqNodes.getFirst(TYPE_NODE);

      // false indicates that all field values should be quoted
     boolean isLiteral = getLiteralIndicator(optNodes);


     String firstFieldValue = reqNodes.getFirst(FIELD_VALUE_NODE);

     if (!isLiteral)
        firstFieldValue = ClauseUtils.addSqlQuotes(firstFieldValue);


     if ( !ClauseUtils.checkForMultiValue(firstFieldValue) && ClauseUtils.checkForWild(firstFieldValue, wildCard, placeMarker ) ) {
        op = "like";
        firstFieldValue = firstFieldValue.replace(wildCard.charAt(0),SQL_WILD_CARD);
        firstFieldValue =  firstFieldValue.replace(placeMarker.charAt(0),SQL_PATTERN_POSITION_MARKER);
        // convert the first to a date value if a date format is specified.
        firstFieldValue = convertIfDate(firstFieldValue, dFormat);

     } else if ( ClauseUtils.checkForMultiValue(firstFieldValue) ) {
        if (op.equals("=") || op.equals("!=") ) {
           if (op.equals("=") )
              op = "in";
           else
              op = "not in";

           firstFieldValue = convertIfDate(firstFieldValue, dFormat);
           // group the multiple fields with parens
           firstFieldValue =  "(" + firstFieldValue + ")";

        } else {
           Debug.warning("Operator [" + op + "] not allowed for mulitiple values.");
           Debug.warning("Using only the first value of multiple values : " + firstFieldValue );
           // get the first value only
           firstFieldValue = firstFieldValue.substring(0, firstFieldValue.indexOf(XrqConstants.FIELD_VAL_SEP)).trim();
           // convert the first to a date value if a date format is specified.
           firstFieldValue = convertIfDate(firstFieldValue, dFormat);

        }

     } else {
        // convert a single value to a date if needed
        firstFieldValue = convertIfDate(firstFieldValue, dFormat);
     }

     // build the clause
     joinFieldOpValue(field, op, firstFieldValue, results);
     return results.toString();

  }

  // creates a field operator value combination
  private void joinFieldOpValue(String field, String op, String value, StringBuffer results) throws MessageException
  {
      results.append(field);
      results.append(" " + op + " ");
      results.append(value );
  }

  /*



  /**
   * converts single or multi field values into an sql date if dateFormat is not null.
   * @param str - the field value(s) to check.
   * @param the dateFormat field value.
   * @param forceQuotes - indicates to wrap any literal with quotes
   */
  private String convertIfDate(String str, String dateFormat) throws MessageException
  {
     StringBuffer resultBuf = new StringBuffer();
     String token = null;

     if (dateFormat != null ) {
        if ( ClauseUtils.checkForMultiValue(str ) ) {
            StringTokenizer tok = new StringTokenizer(str, XrqConstants.FIELD_VAL_SEP);

           while (tok.hasMoreTokens()) {
              token = tok.nextToken();

              if (!ClauseUtils.isLiteral(token) )
                 resultBuf.append(buildDateStr(token, dateFormat ) );
              else
                 resultBuf.append(token);

              // if there is a token after this one then add a separator.
              if (tok.hasMoreTokens() )
                 resultBuf.append(", ");
           }


        } else {
           if (!ClauseUtils.isLiteral(str) )
              resultBuf.append( buildDateStr(str, dateFormat) );
           else
              resultBuf.append(str);
        }

     } else
        return str;
     



     return resultBuf.toString();
  }

  


}
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
 * Base clause which handles formatting and convertion of dates.
 */
public abstract class SQLDateFormattingBase extends ClauseObject
{


  private DateFormat dateFormatter;
  private DateFormat sqlDateFormatter;

   /*
    * indicates that a value is a literal, column name, numeric value, etc, that should
    * not be alternated. Normally quotes are wrapped around all values. If this node is
    * true then the xml must handle all quotes around values as needed.
    *
    */
   public static final String LITERAL_INDICATOR_NODE = "literalValues";



  /**
   * sets up the date formatters and converters.
   * based on the formatFlag.
   */
  protected String setupDateFormat( String formatFlag) throws MessageException, FrameworkException
  {

    //needs synchronization to prevent date formatters from being corrupted.
    Date date = null;
    String sqlDateFormat = null;

    try {
     if ( !StringUtils.hasValue(formatFlag) )
        sqlDateFormat = null;
     else if ( formatFlag.equals(XrqConstants.DATE_FLAG) ) {
        dateFormatter = NFLocaleUtils.getdateOrTimeFormatter( NFLocale.getDateFormat() );
        sqlDateFormatter = NFLocaleUtils.getdateOrTimeFormatter(XrqConstants.DB_CONVERT_DATE);
        sqlDateFormat = XrqConstants.SQL_DATE_FORMAT;
        Debug.log(Debug.MSG_STATUS, clauseName + " Setup date format, locale format[" + NFLocale.getDateFormat() +"], and sql date conversion format [" + XrqConstants.DB_CONVERT_DATE + "]");
     } else if (formatFlag.equals(XrqConstants.TIME_FLAG) ) {
        dateFormatter = NFLocaleUtils.getdateOrTimeFormatter( NFLocale.getTimeFormat() );
        sqlDateFormatter = NFLocaleUtils.getdateOrTimeFormatter(XrqConstants.DB_CONVERT_TIME);
        sqlDateFormat = XrqConstants.SQL_TIME_FORMAT;
        Debug.log(Debug.MSG_STATUS, clauseName + " Setup date format, locale format[" + NFLocale.getTimeFormat() +"], and sql date conversion format [" + XrqConstants.DB_CONVERT_TIME + "]");
     } else if (formatFlag.equals(XrqConstants.DATE_TIME_FLAG) ) {
        dateFormatter = NFLocaleUtils.getdateOrTimeFormatter( NFLocale.getDateTimeFormat() );
        sqlDateFormatter = NFLocaleUtils.getdateOrTimeFormatter(XrqConstants.DB_CONVERT_DATETIME);
        sqlDateFormat = XrqConstants.SQL_DATETIME_FORMAT;
        Debug.log(Debug.MSG_STATUS, clauseName + " Setup date format, locale format[" + NFLocale.getDateTimeFormat() +"], and sql date conversion format [" + XrqConstants.DB_CONVERT_DATETIME + "]");
     } else {
        dateFormatter = NFLocaleUtils.getdateOrTimeFormatter(formatFlag);
        sqlDateFormatter = NFLocaleUtils.getdateOrTimeFormatter(XrqConstants.DB_CONVERT_DATETIME);
        sqlDateFormat = XrqConstants.SQL_DATETIME_FORMAT;
        Debug.log(Debug.MSG_STATUS, clauseName + " Setup date format, date format[" + formatFlag +"], and sql date conversion format [" + XrqConstants.DB_CONVERT_DATETIME + "]");
     }
    } catch (FrameworkException e) {
       throw new FrameworkException(e);
    }
    return  sqlDateFormat;


  }






  /*
   * Builds a sql to_date method
   * @param date The date to convert.
   * @param sqlDateFormat the sql date format for the sql to_date function.
   */
  protected String buildDateStr(String date, String sqlDateFormat) throws MessageException
  {

     String format = null;
     try {
        if ( sqlDateFormat != null ) {
           // remove the quotes from the date value
           date = date.substring(1,date.length() -1);
           
           Date dateObj = dateFormatter.parse(date);
           String sqlDate = sqlDateFormatter.format(dateObj);
           date = "TO_DATE('" + sqlDate +"','" + sqlDateFormat + "')";
        }
     } catch (Exception e) {
        Debug.error("Failed to parse date: " + date + ": " + e.getMessage());
        throw new MessageException(clauseName + ": Invalid date : " + date + ": " + e.getMessage());
     }



     return date;
  }


  protected boolean getLiteralIndicator(ChainedHashMap nodes) throws MessageException
  {
     String isLiteralStr = nodes.getFirst(LITERAL_INDICATOR_NODE);

     if (StringUtils.hasValue(isLiteralStr) ) {
        try {
           Debug.log(Debug.MSG_STATUS, "getLiteralIndicator - indicator's value is [" + isLiteralStr + "]");
           return(StringUtils.getBoolean(isLiteralStr));
        } catch (FrameworkException fe) {
            String err = clauseName + " Invalid value for " + LITERAL_INDICATOR_NODE + " node: " + fe.getMessage();
            Debug.error(err);
            throw new MessageException (err);
        }
     }

     return false;

  }

}

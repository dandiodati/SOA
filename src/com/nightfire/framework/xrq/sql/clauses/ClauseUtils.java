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
 * Additional utilities that can be used be Clauses.
 */
public class ClauseUtils
{


  /*
   * checks if a single field value contains a wild card, or position marker
   * @returns true if there is a pattern match to be performed otherwise false.
   */
  public static boolean checkForWild(String source, String wildCard, String placeMarker )
  {
     if ( !isLiteral(source) && ( source.indexOf(wildCard) > -1 || source.indexOf(placeMarker) > -1) )
        return true;
     else
        return false;
  }



  /**
   * checks if the string is a literal or a string not wrapped in quotes
   * @param str the string to check.
   * @returns true if str is a literal
   */
  public static boolean isLiteral(String str)
  {
     if ( !str.startsWith("'") && !str.endsWith("'") )
       return true;
     else
        return false;
  }

  /**
   * Adds quotes before and after the string only if it doesn't have them already.
   *
   * @param str the string to check.
   * @returns The string with quotes.
   */
  public static String addSqlQuotes(String str)
  {
      StringBuffer buf = new StringBuffer();
      String temp;

      if (checkForMultiValue(str ) ) {
         StringTokenizer tok = new StringTokenizer(str, XrqConstants.FIELD_VAL_SEP);

         while (tok.hasMoreTokens() ) {
            temp = tok.nextToken().trim();
            if ( isLiteral(temp) )
               temp = "'" + temp + "'";

            buf.append(temp);

            if ( tok.hasMoreTokens() )
               buf.append(XrqConstants.FIELD_VAL_SEP);
         }

         return buf.toString();
         
      } else {

         if ( !isLiteral(str) )
            return str;
         else
            return ("'" + str + "'");
      }
  }


  /**
   * checks if the field values contains multiple values separated by FIELD_VAL_SEP.
   * @see XrqConstants.FIELD_VAL_SEP
   */
  public static boolean checkForMultiValue(String str)
  {
     boolean multi = false;

     if (str.indexOf(XrqConstants.FIELD_VAL_SEP) > -1 )  {
        multi = true;
     }

     return multi;
  }





}
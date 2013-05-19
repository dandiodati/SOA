package com.nightfire.framework.rules;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.RegexUtils;
import com.nightfire.framework.util.StringUtils;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* A wrapper around java.lang.String, this class provides
* the same functionality as String, but will shield the
* user from any Exceptions that may be thrown that
* would interrupt rule evaluation. This class also has
* added functionality that is useful when writing
* rule definitions.
*
* @see java.lang.String
*/
public class Value implements Comparable{

 /**
 * Used by <code>hasFormat</code> to indicate a numeric character.
 */
 public static final char NUMERIC_FORMAT = 'N';


 /**
 * Used by <code>hasFormat</code> to indicate an alpha character.
 */
 public static final char ALPHA_FORMAT   = 'A';


 /**
 * Used by <code>hasFormat</code> to indicate any alphanumeric character.
 */
 public static final char ANY_FORMAT     = 'X';


 /**
 * Used by <code>hasFormat</code> to escape special format characters.
 */
 public static final char ESCAPE_CHAR    = '\\';

 public static String ESCAPE_STRING      = "\\";

 /**
 * The current value. This is the String instance that this class wraps.
 */
 private String value = null;

 /**
 * Initializes a newly created Value object so that it represents an empty character sequence.
 */
 public Value(){
    value = new String();
 }

 /**
 * Construct a new Value by converting the specified array of bytes using the platform's default character encoding.
 */
 public Value(byte[] bytes){
    value = new String(bytes);
 }

 /**
 * Construct a new Value by converting the specified subarray of bytes using the platform's default character encoding.
 */
 public Value(byte[] bytes, int offset, int length){
    value = new String(bytes, offset, length);
 }

 /**
 * Construct a new Value by converting the specified subarray of bytes using the specified character encoding.
 */
 public Value(byte[] bytes, int offset, int length, String enc){
    try{
       value = new String(bytes, offset, length, enc);
    }
    catch(UnsupportedEncodingException noSupport){
       Debug.log(Debug.ALL_ERRORS, "Could not create Value: "+noSupport);
    }
 }

 /**
 * Construct a new Value by converting the specified array of bytes using the specified character encoding.
 */
 public Value(byte[] bytes, String enc){

    try{
       value = new String(bytes, enc);
    }
    catch(UnsupportedEncodingException noSupport){
       Debug.log(Debug.ALL_ERRORS, "Could not create Value: "+noSupport);
    }

 }

 /**
 * Allocates a new Value so that it represents the sequence of characters currently contained in the character array argument.
 */
 public Value(char[] value){
    this.value = new String(value);
 }

 /**
 * Allocates a new Value that contains characters from a subarray of the character array argument.
 */
 public Value(char[] value, int offset, int count){
    this.value = new String(value, offset, count);
 }

 /**
 * Initializes a newly created Value object so that it represents the same sequence of characters as the argument; in other words, the newly created Value is a copy of the argument Value.
 */
 public Value(String value){
    this.value = value;
 }

 /**
 *
 */ 
 public Value(Value object){
    this.value = object.value;
 }

 /**
 * Allocates a new Value that contains the sequence of characters currently contained in the Value buffer argument.
 */
 public Value(StringBuffer buffer){
    this.value = buffer.toString();
 }



 /**
 * Returns the character at the specified index.
 */
 public char charAt(int index){
    return value.charAt(index);
 }

 /**
 * Compares this to another Object.
 */

 public int compareTo(Object o){
         int i = 0;
             try
             {
                i = value.compareTo(o.toString());
             }
             catch (ClassCastException ex)
             {
                Debug.log(Debug.EXCEPTION_STACK_TRACE, "compareTo(Object 0): o is not String Object." + ex.getMessage());
             }

        return i;
 } 

 /**
 * Compares this object's value the given String lexicographically.
 */
 public int compareTo(String anotherString){
    return value.compareTo(anotherString);
 }

 /**
 * Compares this object's value the given String lexicographically.
 */
 public int compareTo(Value anotherString){
    return value.compareTo(anotherString.value);
 } 

 /**
 * Compares two strings lexicographically, ignoring case considerations.
 */
 public int compareToIgnoreCase(String str){
    return value.compareToIgnoreCase(str);
 }

 
 /**
 * Compares two strings lexicographically, ignoring case considerations.
 */
 public int compareToIgnoreCase(Value str){
    return value.compareToIgnoreCase(str.value);
 }

 
 /**
 * Concatenates the specified string to the end of this string.
 */
 public Value concat(String str){
    return new Value(value.concat(str));
 }

 /**
 * Concatenates the specified string to the end of this string.
 */
 public Value concat(Value str){
    return new Value(value.concat(str.value));
 }



 /**
 * Returns a String that is equivalent to the specified character array.
 */
 public static Value copyValueOf(char[] data){
    return new Value(String.copyValueOf(data));
 }




 /**
 * Returns a String that is equivalent to the specified character array.
 */
 public static Value copyValueOf(char[] data, int offset, int count){
    return new Value(String.copyValueOf(data, offset, count));
 }



 /**
 * Tests if this string ends with the specified suffix.
 */
 public boolean endsWith(String suffix){
    return value.endsWith(suffix);
 }


 /**
 *  Tests if this string ends with the specified suffix.
 */
 public boolean endsWith(Value suffix){
    return value.endsWith(suffix.value);
 }



 /**
 *   Compares this string to the specified object.
 */
 public boolean equals(Object anObject){
    return value.equals(anObject);
 }


 /**
 *  Compares this string to the specified String.
 */
 public boolean equals(String anObject){
    return value.equals(anObject);
 }


 /**
 *  Compares this string to the specified Value.
 */
 public boolean equals(Value anObject){
    return value.equals(anObject.value);
 }


 /**
 *   Compares this String to another String, ignoring case considerations.
 */
 public boolean equalsIgnoreCase(String anotherString){
    return value.equalsIgnoreCase(anotherString);
 }


 /**
 *  Compares this String to another Value, ignoring case considerations.
 */
 public boolean equalsIgnoreCase(Value anotherString){
    return value.equalsIgnoreCase(anotherString.value);
 }



 /**
 * Convert this String into bytes according to the platform's default character encoding, storing the result into a new byte array.
 */
 public byte[] getBytes(){
    return value.getBytes();
 }


 /**
 * Convert this String into bytes according to the specified character encoding, storing the result into a new byte array.
 */
 public byte[] getBytes(String enc){

    try{
       return value.getBytes(enc);
    }
    catch(UnsupportedEncodingException noSupport){
       Debug.log(Debug.ALL_ERRORS, "Could not getBytes from Value: "+noSupport);
    }

    return new byte[0];

 }


 /**
 * Copies characters from this string into the destination character array.
 */
 public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin){
    value.getChars(srcBegin, srcEnd, dst, dstBegin);
 }




 /**
 * Returns a hashcode for this string.
 */
 public int hashCode(){
    return value.hashCode();
 }


 /**
 * Returns the index within this string of the first occurrence of the specified character.
 */
 public int indexOf(int ch){
    return value.indexOf(ch);
 }


 /**
 * Returns the index within this string of the first occurrence of the specified character, starting the search at the specified index.
 */
 public int indexOf(int ch, int fromIndex){
    return value.indexOf(ch, fromIndex);
 }


 /**
 *
 */
 public int indexOf(String str){
    return value.indexOf(str);
 }


 /**
 * Returns the index within this string of the first occurrence of the specified substring.
 */
 public int indexOf(Value str){
    return value.indexOf(str.value);
 }

 
 /**
 * Returns the index within this string of the first occurrence of the specified substring.
 */
 public int indexOf(String str, int fromIndex){
    return value.indexOf(str, fromIndex);
 }


 /**
 *   Returns the index within this string of the first occurrence of the specified substring.
 */
 public int indexOf(Value str, int fromIndex){
    return value.indexOf(str.value, fromIndex);
 }

 
 /**
 *  Returns a canonical representation for the string object.
 */
 public Value intern(){
    return new Value(value.intern());
 }

 
 /**
 * Returns the index within this string of the last occurrence of the specified character.
 */
 public int lastIndexOf(int ch){
    return value.lastIndexOf(ch);
 }

 /**
 * Returns the index within this string of the last occurrence of the specified character, searching backward starting at the specified index.
 */
 public int lastIndexOf(int ch, int fromIndex){
    return value.lastIndexOf(ch, fromIndex);
 }

 /**
 * Returns the index within this string of the rightmost occurrence of the specified substring.
 */
 public int lastIndexOf(String str){
     return value.lastIndexOf(str);
 }

 /**
 * Returns the index within this string of the last occurrence of the specified substring.
 */
 public int lastIndexOf(String str, int fromIndex){
     return value.lastIndexOf(str, fromIndex);
 }

 /**
 * Returns the length of this string.
 */
 public int length(){
    return value.length();
 }

 /**
 * This method checks to see if tbe length of this Value is equal to the given
 * length.
 *
 * @param length The length to be checked.
 * @return true if the length of this Value is equal to the given length,
 *              false otherwise.
 */
 public boolean length(int length){

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
       Debug.log(Debug.RULE_EXECUTION, "Length of value ["+value+
                                       "] == ["+length+"]: "+
                                       (length() == length));

    }

    return (length() == length);

 }


 /**
 * This method checks to see if tbe length of this Value is equal to the given
 * length.
 *
 * @param length The length to be checked. This String must be convertable
 *               to a valid integer format.
 *
 * @return true if the length of this Value is equal to the given length,
 *              false otherwise. This method will also return false
 *              and log a message if the given length string is not a valid
 *              integer.
 */ 
 public boolean length(String length){

    int i;

    try{
       i = Integer.parseInt(length);
    }
    catch(Exception ex){
       // Null Pointer or NumberFormatException
       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION, "Length ["+length+"] is not a valid integer value");
       }
       return false;
    }

    return length(i);

 }

 /**
 * Tests if two string regions are equal.
 */
 public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len){
    return value.regionMatches(ignoreCase, toffset, other, ooffset, len);
 }

 /**
 * Tests if two string regions are equal.
 */
 public boolean regionMatches(boolean ignoreCase, int toffset, Value other, int ooffset, int len){
    return value.regionMatches(ignoreCase, toffset, other.value, ooffset, len);
 }

 /**
 * Tests if two string regions are equal.
 */
 public boolean regionMatches(int toffset, String other, int ooffset, int len){
    return value.regionMatches(toffset, other, ooffset, len);
 }

 /**
 * Tests if two string regions are equal.
 */
 public boolean regionMatches(int toffset, Value other, int ooffset, int len){
    return value.regionMatches(toffset, other.value, ooffset, len);
 }

 /**
 * Returns a new string resulting from replacing all occurrences of oldChar in this string with newChar.
 */
 public Value replace(char oldChar, char newChar){
    return new Value(value.replace(oldChar, newChar));
 }

 /**
 * Tests if this string starts with the specified prefix.
 */
 public boolean startsWith(String prefix){
    return value.startsWith(prefix);
 }

 /**
 *  Tests if this string starts with the specified prefix.
 */
 public boolean startsWith(Value prefix){
    return value.startsWith(prefix.value);
 }


 /**
 *  Tests if this string starts with the specified prefix beginning a specified index.
 */
 public boolean startsWith(String prefix, int toffset){
    return value.startsWith(prefix, toffset); 
 }

 /**
 *  Tests if this string starts with the specified prefix beginning a specified index.
 */
 public boolean startsWith(Value prefix, int toffset){
    return value.startsWith(prefix.value, toffset);
 }


 /**
 *  Returns a new string that is a substring of this string.
 */
 public Value substring(int beginIndex){

    try{
       return new Value(value.substring(beginIndex));
    }
    catch(IndexOutOfBoundsException oob){
       return new Value("");
    }

 }

 /**
 *  Returns a new string that is a substring of this string.
 */
 public Value substring(int beginIndex, int endIndex){
 
    try{
       return new Value(value.substring(beginIndex, endIndex));
    }
    catch(IndexOutOfBoundsException oob){
       return new Value("");
    }
    
 }

 /**
 *  Converts this string to a new character array.
 */
 public char[] toCharArray(){
    return value.toCharArray();
 }

 /**
 * Converts all of the characters in this String to lower case using the rules of the default locale, which is returned by Locale.getDefault.
 */
 public Value toLowerCase(){
    return new Value(value.toLowerCase());
 }

 /**
 *  Converts all of the characters in this String to lower case using the rules of the given Locale.
 */
 public Value toLowerCase(Locale locale){
    return new Value(value.toLowerCase(locale)); 
 }

 /**
  * Splits this string around matches of the given regular expression.
  */
 public Value[] split(String regex, int limit) {
     
     String[] tokens = value.split( regex, limit );

     Value[] values = new Value[tokens.length];
     
     for( int i = 0; i < values.length; i++ ){
         values[i] = new Value(tokens[i]);
     }
     return values;
 }
 
 /**
  * Splits this string around matches of the given regular expression.
  */
 public Value[] split(String regex) {
     return split(regex, 0);
 }

 /**
 * Returns the String value contained by the Value.
 */
 public String toString(){
    return (value == null) ? "" : value;
 }

 /**
 * Converts all of the characters in this String to upper case using the rules of the default locale, which is returned by Locale.getDefault.
 */
 public Value toUpperCase(){
    return new Value(value.toUpperCase());
 }

 /**
 * Converts all of the characters in this String to upper case using the rules of the given locale.
 */
 public Value toUpperCase(Locale locale){
    return new Value(value.toUpperCase(locale));
 }

 /**
 *  Removes white space from both ends of this string.
 */
 public Value trim(){
    return new Value(value.trim());
 }

 /**
 * Returns the string representation of the boolean argument.
 */
 public static Value valueOf(boolean b){
    return new Value(String.valueOf(b));
 }

 /**
 * Returns the string representation of the char argument.
 */
 public static Value valueOf(char c){
    return new Value(String.valueOf(c));
 }

 /**
 * Returns the string representation of the char array argument.
 */
 public static Value valueOf(char[] data){
    return new Value(String.valueOf(data));
 }

 /**
 * Returns the string representation of a specific subarray of the char array argument.
 */
 public static Value valueOf(char[] data, int offset, int count){
    return new Value(String.valueOf(data, offset, count));
 }

 /**
 * Returns the string representation of the double argument.
 */
 public static Value valueOf(double d){
    return new Value(String.valueOf(d));
 }

 /**
 * Returns the string representation of the float argument.
 */
 public static Value valueOf(float f){
    return new Value(String.valueOf(f));
 }

 /**
 * Returns the string representation of the long argument.
 */
 public static Value valueOf(int i){
    return new Value(String.valueOf(i));
 }

 /**
 * Returns the string representation of the long argument.
 */
 public static Value valueOf(long l){
    return new Value(String.valueOf(l));
 }

 /**
 * Returns the string representation of the Object argument.
 */
 public static Value valueOf(Object obj){
    return new Value(String.valueOf(obj));
 }

/////////////////////////////////////////////////////////////////////////////
// From here down are additional methods not found in String
/////////////////////////////////////////////////////////////////////////////


 /**
 * Returns true if the value of this object is not null or an empty String,
 * false otherwise.
 */
 public boolean hasValue(){
    return (value != null && value.length() > 0);
 }

 /**
 * Determines if an index value is in the range of this String. 
 */
 public boolean inBounds(int index){

    return (index >= 0 && index <= value.length());

 }

 /**
 * This method checks to see if this Value is a member of the given array of
 * values.
 *
 * @param set the range of possible values.
 *
 * @return true if this Value is equal to any member of the given set, false
 *              otherwise.
 */
 public boolean isMember(String[] set){

      if(set == null){

         Debug.log(Debug.ALL_ERRORS,"The set passed to isMember() was null");

      }
      else{

         for ( int Ix = 0;  Ix < set.length;  Ix ++ )
         {
             if ( set[Ix] != null )
             {
                 if ( set[Ix].equals( value ) )
                     return true;
             }
             else
             {
                 if ( value == null )
                     return true;
             }
         }
      }

      return false;

 }

 /**
 * This checks to see if the current value is numeric and if it is
 * in the range between <code>min</code> and <code>max</code> inclusively.
 *
 * @param min The minimum value allowed
 * @param max The maximum value allowed
 *
 * @return true if the current value is numeric and is greater than or
 *         equal to <code>min</code> and less than or equal <code>max</code>,
 *         false otherwise.
 */
 public boolean inRange(int min, int max){

    int i;

    try{
       i = Integer.parseInt(value);
    }
    catch(Exception ex){
       // Null Pointer or NumberFormatException
       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] is not a valid integer value");
       }
       return false;
    }

    // inclusive test
    return (i >= min && i <= max);

 }

 /**
 * This checks to see if this Value is all letters.
 *
 * @return true if this value is all alpha characters, false otherwise.
 */
 public boolean isAlpha(){

    for(int i = 0; i < value.length(); i++){

       if(! Character.isLetter( value.charAt(i) ) ){

          return false;

       }

    }

    return true;

 }


 /**
 * This checks to see if this Value is all letters or whitespace characters.
 *
 * @return true if this value is all alpha or whitespace characters,
 *         false otherwise.
 */
 public boolean isAlphaWithWhitespace(){

    char current;

    for(int i = 0; i < value.length(); i++){

       current = value.charAt(i);

       if(! ( Character.isLetter( current  ) ||
              Character.isWhitespace( current ) ) ){

          return false;

       }

    }

    return true;

 }

 /**
 * This checks to see that every character of this Value is a digit.
 * (e.g. "123", "55555", or "42" would be return true, but "-1", "3.14",
 *  or "ABC" would return false, because they contain non-digit characters.)
 *
 * @return true if this value is all numeric characters, false otherwise.
 */
 public boolean isDigits(){

    return StringUtils.isDigits(value);

 }

 /**
 * This checks to see if this Value is in a valid numeric format.
 * (e.g. valid numeric values are "123", "3.14", "-1", and "-90.5")
 *
 * @return true if this value is in a numeric format, false otherwise.
 */
 public boolean isNumeric(){

     try{

        Double.parseDouble(value);

     }
     catch(NumberFormatException nfex){

        // not a number
        return false;

     }

     return true;

 }

 /**
 * This checks to see if this Value matches the SimpleDateFormat
 * String "MM-dd-yyyy" and also checks the the format of this Value
 * is "NN-NN-NNNN". This method is equivalent to:
 * <code>isDate("MM-dd-yyyy") && hasFormat("NN-NN-NNNN")</code>
 *
 * @see java.text.SimpleDateFormat
 *
 */
 public boolean isDate(){

    return isDate("MM-dd-yyyy") && hasFormat("NN-NN-NNNN");

 }

 /**
 * This checks to see if this Value matches the given SimpleDateFormat String.
 *
 * @param format See the docs for java.text.SimpleDateFormat for a full
 *               description of this format.
 *
 * @see java.text.SimpleDateFormat
 */
 public boolean isDate(String format){

    try{

        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.getCalendar().setLenient(false);
        Date date = formatter.parse(value);

    }
    catch(ParseException badFormat){

       // The date was not in the correct format
       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION, "["+value+
                                          "] is not a real date or does not match date format ["+
                                          format+"]");
       }
       return false;

    }

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
       Debug.log(Debug.RULE_EXECUTION, "["+value+
                                       "] has date format ["+
                                       format+"] and is a real date");
    }

    return true;

 }

 /**
 * This method checks to see if this Value has a particular format.
 *
 * @param format This string decribes the format. The following table
 *               describes this string:
 * <table border=1>
 * <tr>
 * <th>Format Character</th><th>Meaning</th>
 * </tr><tr>
 * <td>X</td><td>Any single character is allowed at this position</td>
 * </tr><tr>
 * <td>A</td><td>Only alphabetic characters are allowed in this position</td>
 * </tr><tr>
 * <td>N</td><td>Only numeric characters only are allowed in this position</td>
 * </tr><tr>
 * <td>\</td><td>This is the escape character. This means that the next format character (be it an X, an A, an N, or another \) will be taken as a literal character.</td>
 * </tr><tr>
 * <td><i>Any Other Character</i></td><td>The value of the character at this position must be equal to this character given in the format string.</td>
 * </tr>
 * </table>
 * The length of the format string (ignoring escape characters "\")
 * must match the length of this Value.
 *
 * @return true if this Value matches the given format, false otherwise.
 */
 public boolean hasFormat(String format){

    return hasFormat(format, value);

 }

 /**
 * This is a staic method to checks if a given String value has a
 * particular format.
 *
 * @param format This string decribes the format. The following table
 *               describes this string:
 * <table border=1>
 * <tr>
 * <th>Format Character</th><th>Meaning</th>
 * </tr><tr>
 * <td>X</td><td>Any single character is allowed at this position</td>
 * </tr><tr>
 * <td>A</td><td>Only alphabetic characters are allowed in this position</td>
 * </tr><tr>
 * <td>N</td><td>Only numeric characters only are allowed in this position</td>
 * </tr><tr>
 * <td>\</td><td>This is the escape character. This means that the next format character (be it an X, an A, an N, or another \) will be taken as a literal character.</td>
 * </tr><tr>
 * <td><i>Any Other Character</i></td><td>The value of the character at this position must be equal to this character given in the format string.</td>
 * </tr>
 * </table>
 * The length of the format string (ignoring escape characters "\")
 * must match the length of this Value.
 *
 * @param value the String whose format will be validated.
 *
 * @return true if this Value matches the given format, false otherwise.
 */
 public static boolean hasFormat(String format, String value){

     boolean success = true;
     
     boolean escapeChar = false;
     char current;
     char formatChar;
     int i, j;
     for(i = 0, j = 0; i < format.length(); i++, j++){

        try{
           current = value.charAt(j);
        }
        catch(IndexOutOfBoundsException ioob){

           if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
               Debug.log(Debug.RULE_EXECUTION,
                         "The length of format String ["+format+
                         "] does not match the length of value ["+
                         value+"]");
           }
           return false;

        }

        formatChar = format.charAt(i);

        if(escapeChar){

           // The format char is a literal and must match the
           // current char exactly
           if( current != formatChar ){
              if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                 Debug.log(Debug.RULE_EXECUTION,
                           "Character ["+current+"] at index ["+i+
                           "] of value ["+value+"] does not match ["+
                           formatChar+"] of format ["+format+"]");
              }
              return false;
           }

           // put the flag down
           escapeChar = false;

        }
        else{

            switch(formatChar){

               case NUMERIC_FORMAT :

                    // If the character is not a digit, return false;
                    if( ! Character.isDigit(current) ){

                       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                          Debug.log(Debug.RULE_EXECUTION,
                                    "Character ["+current+"] at index ["+i+
                                    "] of value ["+value+"] is not numeric");
                       }
                       return false;

                    }
                    break;

               case ALPHA_FORMAT   :

                    // If the character is not an alpha character, return false;
                    if( ! Character.isLetter(current) ){

                        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                           Debug.log(Debug.RULE_EXECUTION,
                                     "Character ["+current+"] at index ["+i+
                                     "] of value ["+value+"] is not an alpha character");
                        }
                        return false;

                    }
                    break;

               case ANY_FORMAT     :

                    // This is a wild card, any alphanumeric character
                    // in this position is valid
                    // If the character is not an alpha character, return false;
                    if( ! Character.isLetterOrDigit(current) ){

                        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                           Debug.log(Debug.RULE_EXECUTION,
                                     "Character ["+current+"] at index ["+i+
                                     "] of value ["+value+"] is not an alpha or numeric character");
                        }
                        return false;

                    }
                    break;

               case ESCAPE_CHAR    :

                    // set the escape flag
                    escapeChar = true;
                    // set the current char index back one, so that it
                    // will compare to the escaped format char
                    j--;
                    break;

               default:

                    // Any other character must match exactly
                    if( current != formatChar ){

                       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                          Debug.log(Debug.RULE_EXECUTION,
                                    "Character ["+current+"] at index ["+i+
                                    "] of value ["+value+"] does not match ["+
                                    formatChar+"] of format ["+format+"]");
                       }
                       return false;

                    }


            }

        }

    }

    // Check to see if the value String is longer than the format String 
    if(j != value.length()){

        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "The length of format String ["+format+
                      "] does not match the length of value ["+
                      value+"]");
        }
        success = false;

    }

    return success;

 }

 /**
 * This method checks to see if this Value matches the given Perl 5 pattern.
 *
 * @param a Perl 5 pattern.
 *
 * @return true if this Value matches the given pattern, false otherwise.
 *
 */
    public boolean matches(String perl5Pattern)
    {
        try
        {
            // match = RegexUtils.match(perl5Pattern, value);
            return( CachingRegexUtils.matches( perl5Pattern, value ) );
        }
        catch(Exception fex)
        {
            if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
            {
                Debug.log( Debug.ALL_ERRORS,"An error occured while trying to match the Perl5 pattern ["
                           + perl5Pattern + "]: " + fex.getMessage() );
            }
        }

        return false;
    }


 /**
 * Checks to see if this Value, when converted to an integer, is equal to
 * the given number.
 *
 * @param i An integer for comparison.
 *
 * @return false if this Value is not in a valid integer format or if the
 *               integer form of this value is not equal to <code>i</code>,
 *               otherwise true.
 */
 public boolean equals(int i){

    boolean success;

    try{

       int intValue = Integer.parseInt(value);
       success = (intValue == i);

    }
    catch(NumberFormatException nfex){


       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "["+
                      value+
                      "] is not a valid integer value and cannot be compared to ["+
                      i+"].");
       }
       success = false;

    }

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

       if(success){
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] equals ["+i+"]");
       }
       else{
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] does not equal ["+i+"]");
       }

    }

    return success;

 }

 /**
 * Checks to see if this Value, when converted to an integer, is greater than
 * the given number.
 *
 * @param i An integer for comparison.
 *
 * @return false if this Value is not in a valid integer format or if the
 *               integer form is less than or equal to <code>i</code>,
 *               otherwise true.
 */
 public boolean greaterThan(int i){

    boolean success;

    try{

       int intValue = Integer.parseInt(value);
       success = (intValue > i);

    }
    catch(NumberFormatException nfex){


       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "["+
                      value+
                      "] is not a valid integer value and cannot be compared to ["+
                      i+"].");
       }
       success = false;

    }

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

       if(success){
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] > ["+i+"]");
       }
       else{
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] <= ["+i+"]");
       }

    }    

    return success;

 } 

 /**
 * Checks to see if this Value, when converted to an integer, is greater than
 * or equal to the given number.
 *
 * @param i An integer for comparison.
 *
 * @return false if this Value is not in a valid integer format or if the
 *               integer form is less than <code>i</code>,
 *               otherwise true.
 */
 public boolean greaterThanOrEquals(int i){

    return greaterThan(i) || equals(i);

 }

 /**
 * Checks to see if this Value, when converted to an integer, is less than
 * the given number.
 *
 * @param i An integer for comparison.
 *
 * @return false if this Value is not in a valid integer format or if the
 *               integer form is greater than or equals to <code>i</code>,
 *               otherwise true.
 */
 public boolean lessThan(int i){

    return ! greaterThanOrEquals(i);

 }

 /**
 * Checks to see if this Value, when converted to an integer, is less than
 * or equal to the given number.
 *
 * @param i An integer for comparison.
 *
 * @return false if this Value is not in a valid integer format or if the
 *               integer form is greater than <code>i</code>,
 *               otherwise true.
 */
 public boolean lessThanOrEquals(int i){

    return ! greaterThan(i);

 } 

 /**
 * Checks to see if this Value, when converted to an double precision floating
 * point decimal number, is equal to the given number.
 *
 * @param f A double floating point number for comparison.
 *
 * @return false if this Value is not in a valid double format or if the
 *               double form of this value is not equal to <code>f</code>,
 *               otherwise true.
 */
 public boolean equals(double f){

    boolean success;

    try{

       double doubleValue = Double.parseDouble(value);
       success = (doubleValue == f);

    }
    catch(NumberFormatException nfex){


       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "["+
                      value+
                      "] is not a valid double precision floating point value and cannot be compared to ["+
                      f+"].");
       }
       success = false;

    }

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

       if(success){
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] equals ["+f+"]");
       }
       else{
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] does not equal ["+f+"]");
       }

    }    

    return success;

 }

 /**
 * Checks to see if this Value, when converted to a double, is greater than
 * the given number.
 *
 * @param f A double number for comparison.
 *
 * @return false if this Value is not in a valid double format or if the
 *               double form is less than or equal to <code>f</code>,
 *               otherwise true.
 */
 public boolean greaterThan(double f){

    boolean success;

    try{

       double floatValue = Double.parseDouble(value);
       success = (floatValue > f);

    }
    catch(NumberFormatException nfex){


       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "["+
                      value+
                      "] is not a valid double precision floating point value and cannot be compared to ["+
                      f+"].");
       }
       success = false;

    }

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

       if(success){
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] > ["+f+"]");
       }
       else{
          Debug.log(Debug.RULE_EXECUTION, "["+value+"] <= ["+f+"]");
       }

    }    

    return success;

 } 

 /**
 * Checks to see if this Value, when converted to a double, is greater than
 * or equal to the given number.
 *
 * @param f A double for comparison.
 *
 * @return false if this Value is not in a valid double format or if the
 *               double form is less than <code>f</code>,
 *               otherwise true.
 */
 public boolean greaterThanOrEquals(double f){

    return greaterThan(f) || equals(f);

 }

 /**
 * Checks to see if this Value, when converted to a double, is less than
 * the given number.
 *
 * @param f A double for comparison.
 *
 * @return false if this Value is not in a valid double format or if the
 *               double form is greater than or equals to <code>f</code>,
 *               otherwise true.
 */
 public boolean lessThan(double f){

    return ! greaterThanOrEquals(f);

 }

 /**
 * Checks to see if this Value, when converted to a double, is less than
 * or equal to the given number.
 *
 * @param f A double for comparison.
 *
 * @return false if this Value is not in a valid double format or if the
 *               double form is greater than <code>f</code>,
 *               otherwise true.
 */
 public boolean lessThanOrEquals(double f){

    return ! greaterThan(f);

 }

 /**
 * Checks to see if this Value, when converted to a boolean, is equal to
 * the given boolean. This Value will be considered true if it is equal to
 * "Y", "YES", "TRUE", or "T", ignoring case. This Value will be considered
 * false if it is equals to "N", "NO", "FALSE", or "F", ignoring case.
 *
 * @param b An boolean for comparison.
 *
 * @return false if this Value is not in a valid boolean or if the
 *               boolean form of this value is not equal to <code>b</code>,
 *               otherwise true.
 */
 public boolean equals(boolean b){

    boolean success;

    try{

       boolean booleanValue = StringUtils.getBoolean(value);
       success = (booleanValue == b);

    }
    catch(FrameworkException fex){


       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "["+
                      value+"] is not valid boolean value and cannot be compared to ["+b+"].");
       }
       success = false;

    }

    if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

       if(success){
          Debug.log(Debug.RULE_EXECUTION, "The boolean value of ["+value+"] equals ["+b+"]");
       }
       else{
          Debug.log(Debug.RULE_EXECUTION, "The boolean value of ["+value+"] does not equal ["+b+"]");
       }

    }    

    return success;

 }

 /**
 * This checks to see if this Value has a valid North American telephone number
 * format. 
 *
 * @return true if this value has the format "NNN-NNN-NNNN", false otherwise.
 *
 * @see #hasFormat(String)
 */
 public boolean isTelephoneNumber(){

    return hasFormat("NNN-NNN-NNNN");

 }


 /**
 * Checks to see if this Value is a valid phone number with an optional
 * extention that is up to four digits long. (e.g. 555-555-1234,
 * 555-555-1234-4444, 555-555-1234-333, etc.) 
 *
 * @return true if this Value has the format "NNN-NNN-NNNN" or
 *              "NNN-NNN-NNNN-NNNN" where the extension "-NNNN" is
 *              optional, false otherwise.
 */
 public boolean isTelephoneNumberWithExtension(){

    int length = value.length();

    if(length < 12 || length > 17){

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION,
                      "The length of a telephone number with an extension must be between 12 and 17 characters.");
       }

       // the value is not the right length to be a phone number
       return false;

    }

    // Check that the first 12 characters are a valid telephone number
    if( substring(0, 12).isTelephoneNumber() ){

        if(length == 12){

           // the value is a phone number without an extension
           return true;

        }

        if(value.charAt(12) == '-' && length != 13){

           for(int i = 13; i < length; i++){

              // If any of the extension characters are not digits,
              // return false
              if( ! Character.isDigit( value.charAt(i) ) ){

                 if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                          Debug.log(Debug.RULE_EXECUTION,
                                    "Character ["+value.charAt(i)+
                                    "] at index ["+i+
                                    "] of value ["+value+"] is not numeric");
                 }

                 return false;

              }

           }

           // All extension characters are digits
           return true;

        }
        else{

             if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
                      Debug.log(Debug.RULE_EXECUTION,
                                "Character ["+value.charAt(12)+
                                "] at index [12] of telephone number value ["+
                                value+
                                "] does not match [-] or is not followed by an extension number.");
             }

             return false;

        }

    }

    return false;

 }

 /**
 * This is a utility method that check to see if this Value is a date
 * that is later than the given number of days from the current date.
 *
 * @param dateFormat The format String of the date format that will be used
 *                   to parse this Value into a date.
 *                   See the docs for java.text.SimpleDateFormat for a full
 *                   description of this format.
 *
 * @param numDays The number of days that will be added to the current date.
 *
 * @return If this Value does not match the given date format, or if this
 *         Value represents a date that is earlier or equal to than the
 *         current date plus the given number of days, then this method
 *         will return false. If this Value is a date after the current
 *         date plus the given number of days, then it will return true.
 *
 * @see java.text.SimpleDateFormat 
 */
 public boolean isLaterThan(String dateFormat, int numDays){

    boolean result = true;

    try{

        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        formatter.getCalendar().setLenient(false);
        Date date = formatter.parse(value);
        Calendar dateValue = new GregorianCalendar();
        dateValue.setTime(date);
        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION, "["+value+"] parsed as date ["+
                                            date+"]");
        }

        Calendar comparison = new GregorianCalendar();
        comparison.set(Calendar.HOUR_OF_DAY, 0);
        comparison.set(Calendar.MINUTE, 0);
        comparison.set(Calendar.SECOND, 0);                
        comparison.add(Calendar.DAY_OF_YEAR, numDays);


        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
            Debug.log(Debug.RULE_EXECUTION, "Comparing ["+value+"] to date ["+
                                            comparison.getTime()+"]");
        }

        result = dateValue.after(comparison);

        if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){

           if(result){
              Debug.log(Debug.RULE_EXECUTION, "Date ["+dateValue.getTime()+
                                               "] is later than date ["+
                                              comparison.getTime()+"]");
           }
           else{
              Debug.log(Debug.RULE_EXECUTION, "Date ["+dateValue.getTime()+
                                               "] is not later than date ["+
                                              comparison.getTime()+"]");
           }

        }

    }
    catch(ParseException badFormat){

       // The date was not in the correct format
       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION, "["+value+
                                          "] is not a real date or does not match date format ["+
                                          dateFormat+"]");
       }
       result = false;

    }

    return result;


 }

 public static void main(String[] args){


 //For testing
 /*
    Debug.enableAll();
    try{

        Value numTest1 = new Value("123");
        Value numTest2 = new Value("3.14");
        Value numTest3 = new Value("-1");
        Value numTest4 = new Value("-90.5");
        Value numTest5 = new Value("ABC");

        System.out.println(numTest1+" isNumeric "+numTest1.isNumeric());
        System.out.println(numTest2+" isNumeric "+numTest2.isNumeric());
        System.out.println(numTest3+" isNumeric "+numTest3.isNumeric());
        System.out.println(numTest4+" isNumeric "+numTest4.isNumeric());
        System.out.println(numTest5+" isNumeric "+numTest5.isNumeric());

        String format = "NNN-NNN-NNNN";

        Value test1 = new Value("123-456-7890");
        Value test2 = new Value("123-456 7890");
        Value test3 = new Value("12A-456-7890");

        Value telephone1 = new Value("123-456");
        Value telephone2 = new Value("123-456-7890-");
        Value telephone3 = new Value("123-456-7890-1");
        Value telephone4 = new Value("123-456-7890-22");
        Value telephone5 = new Value("123-456-7890-333");
        Value telephone6 = new Value("123-456-7890-4444");
        Value telephone7 = new Value("123-456-ABCD-4444");
        Value telephone8 = new Value("123-456-7890-A");
        Value telephone9 = new Value("123-456-7890-DDDD");
        Value telephone10 = new Value("X23-456-7890-1234");


        System.out.println(test1+" isTelephoneNumber: "+test1.isTelephoneNumber());
        System.out.println(test1+" isTelephoneNumberWithExtension: "+test1.isTelephoneNumberWithExtension());
        System.out.println(test2+" isTelephoneNumber: "+test2.isTelephoneNumber());
        System.out.println(test2+" isTelephoneNumberWithExtension: "+test2.isTelephoneNumberWithExtension());
        System.out.println(test3+" isTelephoneNumber: "+test3.isTelephoneNumber());
        System.out.println(test3+" isTelephoneNumberWithExtension: "+test3.isTelephoneNumberWithExtension());


        System.out.println(telephone1+" isTelephoneNumber: "+telephone1.isTelephoneNumber());
        System.out.println(telephone1+" isTelephoneNumberWithExtension: "+telephone1.isTelephoneNumberWithExtension());
        System.out.println(telephone2+" isTelephoneNumber: "+telephone2.isTelephoneNumber());
        System.out.println(telephone2+" isTelephoneNumberWithExtension: "+telephone2.isTelephoneNumberWithExtension());
        System.out.println(telephone3+" isTelephoneNumber: "+telephone3.isTelephoneNumber());
        System.out.println(telephone3+" isTelephoneNumberWithExtension: "+telephone3.isTelephoneNumberWithExtension());
        System.out.println(telephone4+" isTelephoneNumber: "+telephone4.isTelephoneNumber());
        System.out.println(telephone4+" isTelephoneNumberWithExtension: "+telephone4.isTelephoneNumberWithExtension());
        System.out.println(telephone5+" isTelephoneNumber: "+telephone5.isTelephoneNumber());
        System.out.println(telephone5+" isTelephoneNumberWithExtension: "+telephone5.isTelephoneNumberWithExtension());
        System.out.println(telephone6+" isTelephoneNumber: "+telephone6.isTelephoneNumber());
        System.out.println(telephone6+" isTelephoneNumberWithExtension: "+telephone6.isTelephoneNumberWithExtension());
        System.out.println(telephone7+" isTelephoneNumber: "+telephone7.isTelephoneNumber());
        System.out.println(telephone7+" isTelephoneNumberWithExtension: "+telephone7.isTelephoneNumberWithExtension());
        System.out.println(telephone8+" isTelephoneNumber: "+telephone8.isTelephoneNumber());
        System.out.println(telephone8+" isTelephoneNumberWithExtension: "+telephone8.isTelephoneNumberWithExtension());
        System.out.println(telephone9+" isTelephoneNumber: "+telephone9.isTelephoneNumber());
        System.out.println(telephone9+" isTelephoneNumberWithExtension: "+telephone9.isTelephoneNumberWithExtension());
        System.out.println(telephone10+" isTelephoneNumberWithExtension: "+telephone10.isTelephoneNumberWithExtension());



        System.out.println(test1+" hasFormat "+format+": "+test1.hasFormat(format));
        System.out.println(test2+" hasFormat "+format+": "+test2.hasFormat(format));
        System.out.println(test3+" hasFormat "+format+": "+test3.hasFormat(format));

        Value test4 = new Value("ABCD");
        Value test5 = new Value("ABCDE");
        Value test6 = new Value("AB3D");
        Value test7 = new Value("AB-D");
        Value test8 = new Value("ABC");

        format = "AAAA";
        System.out.println(test4+" hasFormat "+format+": "+test4.hasFormat(format));
        System.out.println(test5+" hasFormat "+format+": "+test5.hasFormat(format));
        System.out.println(test6+" hasFormat "+format+": "+test6.hasFormat(format));
        System.out.println(test7+" hasFormat "+format+": "+test7.hasFormat(format));
        System.out.println(test8+" hasFormat "+format+": "+test8.hasFormat(format));

        Value test9  = new Value("N23ABC");
        Value test10 = new Value("N23BBC");

        format = "\\NNN\\AAA";
        System.out.println(test9+" hasFormat "+format+": "+test9.hasFormat(format));
        System.out.println(test10+" hasFormat "+format+": "+test10.hasFormat(format));

        Value test11 = new Value("X1@#$^&$ _\n}\t");
        format = "XXXXXXXXXXXXX";
        System.out.println(test11+" hasFormat "+format+": "+test11.hasFormat(format));

        Value test12 = new Value("\\ANX\\");
        format = "\\\\\\A\\N\\X\\\\";
        System.out.println(test12+" hasFormat "+format+": "+test12.hasFormat(format));

        Value date = new Value("12-32-2001");
        System.out.println(date+" isDate : "+date.isDate("MM-dd-yyyy"));        

        Value date2 = new Value("12-01-1600");
        System.out.println(date2+" isDate : "+date2.isDate("MM-dd-yyyy"));

        Value date3 = new Value("12-01-2000");
        System.out.println(date3+" isDate : "+date3.isDate("MM-dd-yyyy"));

        Value date4 = new Value("02-30-2000");
        System.out.println(date4+" isDate : "+date4.isDate("MM-dd-yyyy"));

        Value date5 = new Value("13-01-2000");
        System.out.println(date5+" isDate : "+date5.isDate("MM-dd-yyyy"));

        Value date6 = new Value("1-1-2001");
        System.out.println(date6+" isDate : "+date6.isDate("MM-dd-yyyy"));

        Value alpha1 = new Value("ABCDEF");
        Value alpha2 = new Value("ABC DEF");
        Value alpha3 = new Value("ABC\tDEF");
        System.out.println(alpha1+" isAlpha : "+alpha1.isAlpha());
        System.out.println(alpha2+" isAlpha : "+alpha2.isAlpha());
        System.out.println(alpha3+" isAlpha : "+alpha3.isAlpha());

        System.out.println(alpha1+" isAlphaWithWhitespace : "+alpha1.isAlphaWithWhitespace());
        System.out.println(alpha2+" isAlphaWithWhitespace : "+alpha2.isAlphaWithWhitespace());
        System.out.println(alpha3+" isAlphaWithWhitespace : "+alpha3.isAlphaWithWhitespace());


    }
    catch(Throwable ex){

       ex.printStackTrace();

    }

   */

 }


}

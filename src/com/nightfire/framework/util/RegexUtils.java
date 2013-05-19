package com.nightfire.framework.util;

import java.util.*;
import com.nightfire.framework.util.*;
import org.apache.oro.text.perl.*;
import org.apache.oro.text.regex.*;

/**
 *  RegexUtils is a utility class for string manipulation.
 *  The key functionality of this class is pattern matching and string
 *  substituion.
 **/

public class RegexUtils
{

   /**
    * This function takes a perl5 regular expression as the pattern
    * to perform pattern matching and string substitution.
    *
    *  "s/pattern/replacement/[g][i][m][o][s][x]"
    *
    *  g - substitute globally (all occurence)
    *  i - case insensitive
    *  m - treat the input as consisting of multiple lines
    *  o - interpolate once
    *  s - treat the input as consisting of a single line
    *  x - enable extended  expression syntax incorporating whitespace and comments
    *
    * to perform string substitution.
    * Unless the [g] option is specified, the dafault is to replace only the
    * first occurrence.
    *
    * @param perl5Pattern - Perl5 regular expression
    * @param input        - input string
    *
    * @return result - processed string. The original input is returned when there
    *                  is no match
    * @exception    -  FrameworkException is thrown when either pattern or input is null  
    **/
   public static String substitute(String perl5Pattern, String input)
   throws FrameworkException
   {
      if( (perl5Pattern == null) || (input == null) )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): Either input or pattern is null."+
                                      "input = "+ input + ", " + "pattern = " + perl5Pattern);
      }

      if( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
      {
         Debug.log( Debug.MSG_STATUS, "RegexUtils: replaceAll: perl5Pattern = "+ perl5Pattern +
                    " input = " + input );
      }

      Perl5Util util = new Perl5Util();
      String result = input;

      try
      {
         result = util.substitute( perl5Pattern, input );
      }
      catch(RuntimeException e)
      {
         throw new FrameworkException("RegexUtils: substitute: "+e.getMessage());
      }

      return result;
   }

   /**
    * This function substitues ALL occurence of the pattern in the input with the replacement
    * passed in. This function can handle such situation as when some characters in
    * the replacement is also used in the pattern.
    *
    * i.e.) pattern     - &    or &/[option]
    *       replacement - &amp or &amp/[option]
    *
    * @param pattern     pattern string to match
    * @param input       input string
    * @param replacement replacement string to be replaced with
    *
    * @return      returns processed string. Default return value is the original input string.
    * @exception   throws FrameworkException when either pattern/input/replacement
    *              is null.
    */
   public static String replaceAll(String pattern, String input, String replacement)
   throws FrameworkException
   {
      if( (pattern == null) || (replacement == null) || (input == null) )
      {
         Debug.log(Debug.ALL_ERRORS, "RegexUtils.replaceAll(): pattern or replacement or input"+
                   "is null.");

         throw new FrameworkException("ERROR: RegexUtils: replaceAll(): pattern or replacement or input is null.");
      }

      Perl5Util util = new Perl5Util();

      String negativeLookAheadPattern = makePerl5MatchPatternNegativeLookAhead( pattern, replacement );
      String substitutionPattern = makePerl5SubstitutionPattern( negativeLookAheadPattern, replacement );

      String result = util.substitute( substitutionPattern, input );

      return result;
   }


   /**
    * This function can take a single input string, an array of pattern, and
    * an array of replacement strings. The size of the two arrays must match since a pattern
    * will be replaced with the string in the replacement found at the same
    * index level as the pattern string in the patterns[].
    * It calls replaceAll(pattern, input, replacement) for the single input/pattern/
    * replacement arguments.
    *
    * @param patterns      an array of pattern strings
    * @param input         input string
    * @param replacements  an array of replacement strings
    *
    * @return result       returns processed string
    * @exception           throws FrameworkException when either pattern/input/replacement
    *                      is null.
    **/
   public static String replaceAll(String patterns[], String input, String replacements[])
   throws FrameworkException
   {

      if( input == null )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): input or pattern is null.");
      }

      if( (patterns.length < 0) || (replacements.length < 0) || (patterns.length != replacements.length) )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): The length of the patterns and the replacements must be the same." +
                                      "patterns.length = "+patterns.length + " replacements.length = "+replacements.length );
      }

      for( int i = 0; i < patterns.length; i++ )
      {
         // replace all the occurence of patterns[i] in the input with replacement[i]
         input = replaceAll( patterns[i], input, replacements[i] );
      }

      return input;
   }

   /**
    * This function substitues ALL occurence of the pattern in the input with the
    * replacement passed in. This function can take a single input, an array of patterns
    * and replacement strings the size of the two arrays must much since a pattern
    * will be replaced with the string in the replacement found at the same
    * index level as the pattern string in the patterns[]. Also an array of beginTokens
    * and an array of endTokens are passed in to perform bound replacement.
    *
    * @param patterns      array of pattern strings
    * @param input         input string
    * @param replacements  array of replacement strings
    * @param beginTokens   array of begin boundary tokens
    * @param endTokens     array of end boundary tokens
    *
    * @return result       returns processed string
    * @exception           throws FrameworkException when either pattern/input/replacement
    *                      is null.
    *
    **/
   public static String replaceAll(String patterns[], String input, String replacement[],
                                   String beginTokens[], String endTokens[])
   throws FrameworkException
   {
      if( input == null )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): input is null.");
      }

      if( patterns.length <= 0 || replacement.length <= 0 )
      {
         throw new FrameworkException("RegexUtils: replaceAll(): there are no patterns or replacements. " +
                                      "patterns.length = " + patterns.length + "replacement.length = "+ replacement.length);
      }

      if( beginTokens.length != endTokens.length )
      {
         throw new FrameworkException("RegexUtils: replaceAll(): The number of begin tokens and end tokens should match. " +
                                      "beginTokens.length = " + beginTokens.length + "endTokens.length = "+ endTokens.length);
      }

      for( int i = 0; i < beginTokens.length; i++ )
      {
         input = replaceAll( patterns, input, replacement, beginTokens[i], endTokens[i] );
      }

      return input;
   }



   /**
    * This function substitues ALL occurence of the pattern in the input
    * passed in. This function can take a single input, an array of pattern
    * and replacement strings. Size of the two arrays must much since a pattern
    * will be replaced with the string in the replacement found at the same
    * index level as the pattern string in the patterns[].
    * The substitution is done on the string bound by the begin token and the end
    * token. If either/both not found on the input string the function returns the
    * original input string.
    *
    * @param patterns     array of pattern strings
    * @param input        input string
    * @param replacement  an array of replacement strings
    * @param beginToken   begin boundary token
    * @param endToken     end boundary token
    *
    * @return             returns the processed string
    * @exception          throws FrameworkException when either pattern/input/replacement
    *                     is null.
    *
    **/
   public static String replaceAll(String patterns[], String input, String replacements[],
                                   String beginToken, String endToken)
   throws FrameworkException
   {
      if( input == null )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): input is null.");
      }

      if( ( patterns.length != replacements.length ) || patterns.length <= 0 || replacements.length <= 0 )
      {
         throw new FrameworkException("RegexUtils: replaceAll(): " +
                                      "The size of patterns and replacements doesn't match or no elements at all." +
                                      "\npatterns.length = " + patterns.length + "replacements.length = " + replacements.length);

      }

      String perl5Pattern = null;

      // for the number of pattern in the array
      for( int i = 0; i < patterns.length; i++ )
      {

         /* if the replacement string includes pattern string
         /* checkReplacement checks if pattern string is found on the replacement string
         /* it returns true.
          */
         if( checkReplacement( patterns[i], replacements[i] ) )
         {
            // form the perl5Pattern to do the negative look-ahead
            perl5Pattern = makePerl5MatchPatternNegativeLookAhead( patterns[i], replacements );
            patterns[i] =  perl5Pattern;
         }

         input = replaceAll( patterns[i], input, replacements[i], beginToken, endToken );
      }

      return input;
   }

   /**
    * This function substitues ALL occurence of the pattern in the input
    * passed in. This function can take a single input, an array of pattern
    * and replacement strings. Size of the two arrays must much since a pattern
    * will be replaced with the string in the replacement found at the same
    * index level as the pattern string in the patterns[].
    * The substitution is done on the string bound by the begin token and the end
    * token. If either/both not found on the input string the function returns the
    * original input string.
    *
    * @param patterns     array of pattern strings
    * @param input        input string
    * @param replacement  an array of replacement strings
    * @param beginToken   begin boundary token
    * @param endToken     end boundary token
    * @param beginTokens[] list of beginTokens - this is used for the operation of
    *                     negative look ahead if necessary
    *
    * @return             returns the processed string
    * @exception          throws FrameworkException when either pattern/input/replacement
    *                     is null.
    *
    **/
   public static String replaceAll(String patterns[], String input, String replacements[],
                                   String beginToken, String endToken, String beginTokens[])
   throws FrameworkException
   {
      if( input == null )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): input is null.");
      }

      if( ( patterns.length != replacements.length ) || patterns.length <= 0 || replacements.length <= 0 )
      {
         throw new FrameworkException("RegexUtils: replaceAll(): " +
                                      "The size of patterns and replacements doesn't match or no elements at all." +
                                      "\npatterns.length = " + patterns.length + "replacements.length = " + replacements.length);

      }

      String perl5Pattern = null;

      // for the number of pattern in the array
      for( int i = 0; i < patterns.length; i++ )
      {

         /* if the replacement string includes pattern string
         /* checkReplacement checks if pattern string is found on the replacement string
         /* it returns true.
          */
         if( checkReplacement( patterns[i], replacements[i] ) )
         {
            // form the perl5Pattern to do the negative look-ahead
            perl5Pattern = makePerl5MatchPatternNegativeLookAhead( patterns[i], replacements );
            patterns[i] =  perl5Pattern;
         }

         input = replaceAll( patterns[i], input, replacements[i], beginToken, endToken, beginTokens );
      }

      return input;
   }

   /**
    * This function substitues ALL occurence of the pattern in the input
    * passed in. This function can take a single input, a perl5 match pattern,
    * an array of replacement strings, begin token, and end token.
    *
    * @param perl5Regex    perl5 regular expression for pattern match
    *                      i.e.) &(?!amp) - this regular expression matches
    *                      every occurence of '&' which is not followed by "amp".
    *                      For example, it will match '&' or "&anystring" but
    *                      "&amp".
    *
    * @param input         input string
    * @param replacement   array of replacement strings
    * @param beginToken    begin boundary token
    * @param endToken      end boundary token
    *
    * @return result       returns processed string
    * @exception           throws FrameworkException when either pattern/input/replacement
    *                      is null.
    **/
   public static String replaceAll(String perl5Regex, String input, String replacement[],
                                   String beginToken, String endToken)
   throws FrameworkException
   {
      if( input == null )
      {
         throw new FrameworkException("RegexUtil: replaceAll(): input is null.");
      }

      if( (perl5Regex == null) && (replacement.length <= 0) )
      {
         Debug.log(Debug.ALL_ERRORS, "RegexUtils.replaceAll()): No patterns and tokens specified." );
         throw new FrameworkException("RegexUtils: replaceAll(): No patterns and tokens specified.");
      }

      for( int i = 0; i < replacement.length; i++ )
      {
         input = replaceAll( perl5Regex, input, replacement[i], beginToken, endToken );
      }

      return input;
   }


   /**
    * This function substitues ALL the occurence of the pattern in the input
    * with the replacement passed in. The substitution is done only in the
    * range bound by the begin token and the end token.
    *
    * @param pattern     pattern string to match
    * @param input       input string
    * @param replacement replacement string to replace with
    * @param beginToken  begin boundary token
    * @param endToken    end boundary token
    *
    * @return      returns processed string if both beginToken and the endToken are
    *              successfully found, returns the unproessed original input otherwise.
    * @exception   throws a FrameworkException when either pattern/input/replacement
    *              is null.
    */
   public static String replaceAll(String pattern, String input, String replacement,
                                   String beginToken, String endToken)
   throws FrameworkException
   {
      if( (pattern == null) || (replacement == null) || (input == null) )
      {
         Debug.log(Debug.ALL_ERRORS,"RegexUtils: replaceAll(): pattern or replacement or input is null. "+
                   "\npattern = " + pattern + "\ninput = " + input + "\nreplacement = " + replacement  );
         throw new FrameworkException("RegexUtils: replaceAll(): pattern or replacement or input is null.");
      }

      Perl5Util util = new Perl5Util();
      StringBuffer resultBuffer = new StringBuffer();

      if( (beginToken == null) || (endToken == null) )
      {
         // either beginToken or endToken cannot be null, however both can be null.
         throw new FrameworkException("RegexUtils: replaceAll(): Either begin or end token is null. BeginToken = " +
                                       beginToken+", "+"endToken = "+endToken);
      }
      else // do pattern match
      {
         // making the Perl5 regular expression format pattern
         String begin = makePerl5MatchPattern( beginToken );
         String end   = makePerl5MatchPattern( endToken );
         boolean beginTokenMatch = true;

         // if begin token found
         while( beginTokenMatch )
         {

            // check the input for each iteration. when there is no input, break out of the loop.
            if( input == null )
               break;

            beginTokenMatch = util.match( begin, input );

            if( beginTokenMatch )
            {
               String negativeLookAheadPattern = makePerl5MatchPatternNegativeLookAhead( pattern, replacement );
               int beginOffsetForBeginToken = util.beginOffset(0);
               int endOffsetForBeginToken = util.endOffset(0);

               // the input after the begin token
               String subInput = input.substring( endOffsetForBeginToken );

               // if end token passed in was an empty string
               if( endToken.equals("") )
               {
                  String result = null;
                  try
                  {
                     result = replaceAllWithBeginToken( pattern, input, replacement, beginToken );
                  }
                  catch(FrameworkException e) {
                     Debug.log(Debug.ALL_ERRORS, "RegexUtils: replaceAll() failed."+e.getMessage());
                  }
                  return result;
               }
               else if( util.match( end, subInput ) ) // endToken found
               {
                  // begin offset for the end token relative to the input not subInput
                  int beginOffsetForEndToken = endOffsetForBeginToken + util.beginOffset(0);
                  int endOffsetForEndToken = endOffsetForBeginToken + util.endOffset(0);

                  // pre is the string before the beginToken and the beginToken
                  String pre = input.substring( 0, endOffsetForBeginToken );
                  resultBuffer.append( pre );

                  // current is the string between the beginToken and the endToken
                  String current = input.substring( endOffsetForBeginToken, beginOffsetForEndToken );

                  // theRest is the rest of the input string after the endToken
                  String theRest = input.substring( endOffsetForEndToken );
                  current = skipOrphanedBeginToken( begin, current, resultBuffer );

                  current = replaceAll( pattern, current, replacement );

                  resultBuffer.append( current );
                  resultBuffer.append( input.substring( beginOffsetForEndToken, endOffsetForEndToken ) );
                  input = theRest;
               }
               else // endToken not found
               {
                  resultBuffer.append( input );
                  break;
               }
            }// if beginToken found
            else // beginToken not found
            {
               resultBuffer.append( input );
               break;
            }
         }// while begin token found

      }// else do pattern match

      return resultBuffer.toString();
   }

   /**
    * This function substitues ALL the occurence of the pattern in the input
    * with the replacement passed in. The substitution is done only in the
    * range bound by the begin token and the end token.
    *
    * @param pattern     pattern string to match
    * @param input       input string
    * @param replacement replacement string to replace with
    * @param beginToken  begin boundary token
    * @param endToken    end boundary token
    * @param beginTokens[] an array of beginTokens - this is used for negative look ahead
    *                    where there are other begin tokens to be recognized.
    *
    * @return      returns processed string if both beginToken and the endToken are
    *              successfully found, returns the unproessed original input otherwise.
    * @exception   throws a FrameworkException when either pattern/input/replacement
    *              is null.
    */
    public static String replaceAll(String pattern, String input, String replacement,
                                    String beginToken, String endToken, String beginTokens[])
   throws FrameworkException
   {
      if( (pattern == null) || (replacement == null) || (input == null) )
      {
         Debug.log(Debug.ALL_ERRORS,"RegexUtils: replaceAll(): pattern or replacement or input is null. "+
                   "\npattern = " + pattern + "\ninput = " + input + "\nreplacement = " + replacement  );
         throw new FrameworkException("RegexUtils: replaceAll(): pattern or replacement or input is null.");
      }

      Perl5Util util = new Perl5Util();
      StringBuffer resultBuffer = new StringBuffer();

      if( (beginToken == null) || (endToken == null) )
      {
         // either beginToken or endToken cannot be null, however both can be null.
         throw new FrameworkException("RegexUtils: replaceAll(): Either begin or end token is null. BeginToken = " +
                                       beginToken+", "+"endToken = "+endToken);
      }
      else // do pattern match
      {
         // making the Perl5 regular expression format pattern
         String begin = makePerl5MatchPattern( beginToken );
         String end   = makePerl5MatchPattern( endToken );
         boolean beginTokenMatch = true;

         // if begin token found
         while( beginTokenMatch )
         {

            // check the input for each iteration. when there is no input, break out of the loop.
            if( input == null )
               break;

            beginTokenMatch = util.match( begin, input );

            if( beginTokenMatch )
            {
               String negativeLookAheadPattern = makePerl5MatchPatternNegativeLookAhead( pattern, replacement );
               int beginOffsetForBeginToken = util.beginOffset(0);
               int endOffsetForBeginToken = util.endOffset(0);

               // the input after the begin token
               String subInput = input.substring( endOffsetForBeginToken );

               // if end token passed in was an empty string
               if( endToken.equals("") )
               {
                  String result = null;
                  try
                  {
                     result = replaceAllWithBeginToken( pattern, input, replacement, beginToken );
                  }
                  catch(FrameworkException e) {
                     Debug.log(Debug.ALL_ERRORS, "RegexUtils: replaceAll() failed."+e.getMessage());
                  }
                  return result;
               }
               else if( util.match( end, subInput ) ) // endToken found
               {

                  // begin offset for the end token relative to the input not subInput
                  int beginOffsetForEndToken = endOffsetForBeginToken + util.beginOffset(0);
                  int endOffsetForEndToken = endOffsetForBeginToken + util.endOffset(0);

                  // pre is the string before the beginToken and the beginToken
                  String pre = input.substring( 0, endOffsetForBeginToken );
                  resultBuffer.append( pre );

                  // current is the string between the beginToken and the endToken
                  String current = input.substring( endOffsetForBeginToken, beginOffsetForEndToken );

                  // theRest is the rest of the input string after the endToken
                  String theRest = input.substring( endOffsetForEndToken );

                  // current is the string between begin token and the endtoken
                  current = skipOrphanedBeginToken( begin, current, resultBuffer );

                  if( isOtherBeginTokenThere( beginToken, current, beginTokens ) == false )
                  {
                     current = replaceAll( pattern, current, replacement );

                  }// isOtherBeginTokenThere

                  resultBuffer.append( current );
                  resultBuffer.append( input.substring( beginOffsetForEndToken, endOffsetForEndToken ) );
                  input = theRest;
               }
               else // endToken not found
               {
                  resultBuffer.append( input );
                  break;
               }
            }// if beginToken found
            else // beginToken not found
            {
               resultBuffer.append( input );
               break;
            }
         }// while begin token found

      }// else do pattern match

      return resultBuffer.toString();
   }

   /**
    * This function checks in the input if there is other tokens in the begingTokens array
    * than begin. This utility function is used on a string which is between a valid begin token
    * and all possible other begin tokens. If there is another begin token of different kind is
    * found, the substitution shouldn't occur. Consider this situation:
    *
    * i.e.)  <beginToken_0> "some string <beginToken_1> here to do the substitution on" <endToken_0>
    *        <beginToken_0> "another some string" <endToken_1>
    *
    * In this case, the replaceAll() function finds the matched pair <beginToken_0> and <endToken_0>
    * and attempt to perform the substitution. However, since there is another boundary _1 is
    * overlapped with _0 boundary, we do not want to perform a string substitution.
    *
    * @param replacement replacement string to replace with
    * @param beginToken  begin boundary token
    * @param endToken    end boundary token
    * @return true if any of other beginTokens in the beginToken[] than begin is
    *         found in the input string, false otherwise
    */
   private static final boolean isOtherBeginTokenThere(String begin, String input, String beginTokens[])
   throws FrameworkException
   {
      Perl5Util util= new Perl5Util();

      for( int i = 0; i < beginTokens.length; i++ )
      {
         // look for the other token
         if( !beginTokens[i].equals( begin ) )
         {
            String perl5Pattern = makePerl5MatchPattern( beginTokens[i] );
            // if the input contains at least one beginToken which differs from the begin
            if( util.match( perl5Pattern, input ) )
            {
               Debug.log(Debug.MSG_STATUS, "RegexUtils: isOtherBeginTokenThere: token = "+perl5Pattern);
               return true;
            }
         }
      }

      return false;
   }

   /** The function checks and handles the situation where another begin token is found
    * in between the first begin token and the first end token. Such first beginToken is
    * ignored. The current input to be passed in to the replacement function will be the
    * string between the LAST begin token and the FIRST end token found.
    *
    * @param beginToken   the pattern to look for
    * @param current      input string
    * @param resultBuffer the buffer to hold the result
    *
    * @return      returns a Vector containing the substrings of the input that occur
    */
   private static final String skipOrphanedBeginToken(String beginToken, String current, StringBuffer resultBuffer)
  {
     Perl5Util util= new Perl5Util();
     boolean nextBeginTokenMatch = true;
     MatchResult matchResult = null;
     String remainderStr = null;
     String subCurrent = null;
     StringBuffer currentBuffer = new StringBuffer();
     String pre = null;

     // subCurrent is copy of the current to check if there is any next beginTokens
     subCurrent = current;

     while( nextBeginTokenMatch )
     {
        nextBeginTokenMatch = util.match( beginToken, subCurrent );

        if( nextBeginTokenMatch == true )
        {
           // pre is the string before the beginToken
           pre = util.preMatch();
           currentBuffer.append( pre );

           // get the  matched beginToken
           matchResult = util.getMatch();
           currentBuffer.append( matchResult.toString() );

           // get the remaining string after the beginToken
           remainderStr = util.postMatch();
           subCurrent = remainderStr;
        }
        else // there is no match
        {
           // appending the string before the last begin token to the result
           resultBuffer.append( currentBuffer.toString() );

           // finally get the string to perform the substitution on
           current = subCurrent;
           break;
        }
     }// while

     return current;
   }

   /**
    * Splits the input string into strings separated by the delimiter and put them into a
    * Vector.
    *
    * @param delimiter splitting delimiter
    * @param input     input string
    *
    * @return      returns a Vector containing the substrings of the input that occur
    *              between delimiter. Returns the Vector with original string as only the
    *              element when the delimiter is not found in the input string.
    * @exception   throws a FrameworkException when either input/delimiter is null.
    */
   public static Vector split(String delimiter, String input)
   throws FrameworkException
   {
      if( input == null || delimiter == null )
      {
         throw new FrameworkException("RegexUtils: split(): input or delimiter is null."+
                                      "input =" + input + ", " + "delimiter = "+delimiter);
      }

      Perl5Util util = new Perl5Util();
      Vector result =  new Vector();

      // convert the delimiter to the perl5 format
      delimiter = makePerl5MatchPattern( delimiter );

      try
      {
         // Populate the vector argument with split strings
          util.split( result, delimiter, input, util.SPLIT_ALL );
      }
      catch(MalformedPerl5PatternException e)
      {
         throw new FrameworkException("RegexUtils: split():"+e.getMessage());
      }

      return result;
   }


   /**
    * This function returns boolean value true when the pattern is found in
    * the input string. Returns false otherwise.
    *
    * @param pattern     pattern string to match
    * @param input       input string
    * @return      returns true if a match is found in the input string passed in,
    *              false otherwise.
    * @exception   throws a FrameworkException when either pattern/input is null.
    */
   public static boolean match(String pattern, String input)
   throws FrameworkException
   {
      if( input == null || pattern == null )
      {
         throw new FrameworkException("RegexUtils: match(): input or pattern is null."+
                                      "input =" + input + ", " + "pattern = "+pattern);
      }

      Perl5Util util = new Perl5Util();
      boolean isMatched = false;

      // convert the pattern to the perl5 format
      pattern = makePerl5MatchPattern( pattern );
      isMatched = util.match( pattern, input );

      return isMatched;
   }

   /**
    * This function returns the matched string in input when the pattern is found.
    *
    * @param pattern     pattern string to match
    * @param input       input string
    *
    * @return      returns the portion of the matched string in the input
    * @exception   throws a FrameworkException when either pattern/input is null.
    */
   public static String getMatch(String pattern, String input)
   throws FrameworkException
   {
      if( input == null || pattern == null )
      {
         throw new FrameworkException("RegexUtils: getMatch(): input or pattern is null."+
                                      "input =" + input + ", " + "pattern = "+pattern);
      }

      Perl5Util util = new Perl5Util();
      boolean isMatched = false;
      MatchResult matchResult = null;

      // default return value is the original input
      String result = input;

      pattern = makePerl5MatchPattern( pattern );
      
      if( util.match( pattern, input ) )
      {
         matchResult = util.getMatch();
         result = matchResult.toString();
      }

      return result;
   }


   /**
    * This function returns the begin offset of the input string where the first matched
    * pattern is found.
    *
    * @param pattern     pattern string to match
    * @param input       input string
    *
    * @return      returns the offset of the beginning of the first matched pattern.
    *              if the match not found, it returns -1.
    * @exception   throws a FrameworkException when either pattern/input is null.
    */
   public static int getBeginOffset(String pattern, String input)
   throws FrameworkException
   {
      if( input == null || pattern == null )
      {
         throw new FrameworkException("RegexUtils: getBeginOffset(): input or pattern is null." +
                                      "input = " + input + ", " +  "pattern = " + pattern);
      }

      Perl5Util util = new Perl5Util();
      int beginOffset = -1;
      String formatPattern = null;

      formatPattern = makePerl5MatchPattern( pattern );

      if( util.match( formatPattern, input ) )
      {
         beginOffset = util.beginOffset(0);
      }
      else
      {
         Debug.log(Debug.MSG_STATUS, "RegexUtils: getBeginOffset(): No match found.");
      }

      return beginOffset;
   }

   /**
    * This function makes a perl5 match pattern from non-format pattern.
    * This function also checks for the case of special character. It the patter
    * passed in is such a chracter, the escape character back slash is attached
    * in the front.
    *
    * i.e.) input  "&"
    *       output "/&/"
    *
    * @param pattern     pattern string to match
    *
    * @return            returns the perl5 format pattern
    * @exception         throws a FrameworkException when the pattern is null.
    **/
   private static String makePerl5MatchPattern(String pattern)
   throws FrameworkException
   {
      if( pattern == null )
      {
         throw new FrameworkException("RegexUtil: makePerl5MatchPattern(): the pattern is null.");
      }

      StringBuffer perl5Pattern = new StringBuffer();
      String formatPattern = pattern;

      // check for the special characters which needs to be preceeed it by a backslash
      if( pattern.equals("|") || pattern.equals(")") || pattern.equals("$") ||
          pattern.equals("*") || pattern.equals("^") || pattern.equals("/") ||
          pattern.equals("+") || pattern.equals(".") || pattern.equals("[") ||
          pattern.equals("?") || pattern.equals("(") || pattern.equals("]") )
      {
         // add the escape character
         formatPattern = "\\" + pattern;
      }

      /* check if the pattern has the option. user can specify the pattern with the
       * perl5 option:  pattern/[i][m][s][x]
       * checkOption returns true if the pattern has index of the one of
       * those:  /i, /m, /s, /x
       */
      if( checkOption( formatPattern ) )
      {
         perl5Pattern.append("/");
         perl5Pattern.append(formatPattern);
      }
      else
      {
         perl5Pattern.append("/");
         perl5Pattern.append(formatPattern);
         perl5Pattern.append("/");
      }

      return perl5Pattern.toString();
   }

   /**
    * This function checks if the pattern has the option for pattern
    * matching.
    *
    * @param pattern     pattern string to match
    *
    * @return            returns true if the pattern has the option, false otherwise.
    * @exception         throws a FrameworkException when the pattern is null.
    **/
   private static boolean checkOption(String pattern)
   throws FrameworkException
   {
      if( pattern == null )
      {
         throw new FrameworkException("RegexUtil: checkOption: the pattern is null.");
      }

      if( pattern.endsWith("/i") || pattern.endsWith("/m") || pattern.endsWith("/s") ||
          pattern.endsWith("/x") )
      {
         return true;
      }
      else
      {
         return false;
      }

   }

   /**
    * This function makes a perl5 match pattern from non-format pattern for the
    * substitution.
    * This function also checks for the case of special character. It the patter
    * passed in is such a chracter, the escape character back slash is attached
    * in the front. The format is:
    *
    *     s/pattern/replacement/
    *
    * i.e.) input: pattern     - "
    *              replacement - &quot
    *
    *       output:              s/\"/&quot/
    *
    * @param pattern      pattern string to match
    * @param replacement  replacement string
    *
    * @return            returns the perl5 format for substitution
    * @exception         throws a FrameworkException when the pattern/replacement is null.
    *
    **/
   private static String makePerl5SubstitutionPattern(String pattern, String replacement)
   throws FrameworkException
   {
      if( replacement == null || pattern == null )
      {
         throw new FrameworkException("RegexUtils: makePerl5SubstitutionPattern: pattern or replacement is null."+
                                      "pattern = "+ pattern + " replacement = " + replacement);
      }

      StringBuffer regex = new StringBuffer();
      String formatPattern = pattern;

      // check for the special characters which needs to be preceeed it by a backslash
      if( pattern.equals("|") || pattern.equals(")") || pattern.equals("$") ||
          pattern.equals("*") || pattern.equals("^") || pattern.equals("/") ||
          pattern.equals("+") || pattern.equals(".") || pattern.equals("[") ||
          pattern.equals("?") || pattern.equals("(") || pattern.equals("]") )
      {
         // add the escape character
         formatPattern = "\\" + pattern;
      }

      // the case the replacement has an option
      if( checkOption( replacement ) )
      {
         regex.append("s/");
         regex.append(formatPattern);
         regex.append("/");
         regex.append(replacement);
         regex.append("g");

      }
      else
      {
         regex.append("s/");
         regex.append(formatPattern);
         regex.append("/");
         regex.append(replacement);
         regex.append("/g");
      }

      return regex.toString();
   }

   /**
    * This function replace only the first pattern matched with the replacement.
    *
    * @param pattern      pattern string to match
    * @param input        input string
    * @param replacement  replacement string
    *
    * @return             returns the processed string
    * @exception          throws a FrameworkException when the pattern/replacement is null.
    *
    */
   public static String replaceFirst(String pattern, String input, String replacement)
   throws FrameworkException
   {
      if( (pattern == null) || (input == null) || (replacement == null) )
      {
         throw new FrameworkException("RegexUtil: replaceFirst(): pattern or input cannot be null. "
                                      +"pattern = " + pattern + "input = " + input + "replacement = "+replacement );
      }

      String result = null;
      String perl5SubstitutionPattern = null;
      Perl5Util util = new Perl5Util();

      // default return value
      result = input;
      
      perl5SubstitutionPattern = makePerl5SubstitutionPattern( pattern, replacement );
      pattern = makePerl5MatchPattern( pattern );

      if( util.match( pattern, input ) )
      {
         result = util.substitute( perl5SubstitutionPattern, input );
      }

      return result;
   }


   /**
    * This function replace only the first pattern matched with the replacement.
    *
    * @param pattern      pattern string to match
    * @param input        input string
    * @param replacement  replacement string
    *
    * @return             returns the processed string
    * @exception          throws a FrameworkException when the pattern/replacement is null.
    */
   public static String replaceLast(String pattern, String input, String replacement)
   throws FrameworkException
   {
      if( (pattern == null) || (input == null) || (replacement == null) )
      {
         throw new FrameworkException("RegexUtil: replaceLast(): pattern or input cannot be null. "
                                      +"pattern = " + pattern + "input = " + input + "replacement = "+replacement );
      }

      Perl5Util util = new Perl5Util();
      MatchResult matchResult = null;
      String result = null;
      String pre = null;
      String post = null;
      StringBuffer resultBuffer = new StringBuffer();

      int length = input.length();

      String regex = makePerl5SubstitutionPattern( pattern, replacement );
      pattern = makePerl5MatchPattern( pattern );

      // counts the number of match and grab the last one
      while( util.match( pattern, input )  )
      {
         // getting the string before match
         pre = util.preMatch();
         resultBuffer.append( pre );

         // get the matched string
         matchResult = util.getMatch();
         resultBuffer.append( matchResult.toString() );

         // get the post string after the match
         post = util.postMatch();

         // the post becomes the new input 
         input = post;
      }

      // get the last match found
      matchResult = util.getMatch();

      // do the string replacement on the pattern found
      result = util.substitute( regex, matchResult.toString() );

      resultBuffer.append( result );
      resultBuffer.append( post );

      return resultBuffer.toString();
   }

   /**
    * This function locates the first match on the begin token and replace all the
    * pattern with the replacement the area after the begin token is located.
    *
    * @param pattern      pattern string to match
    * @param input        input string
    * @param replacement  replacement string
    * @param beginToken   begin token string
    *
    * @return    processed string. If there's no match on the begin token, it returns
    *            the original input by default.
    * @exception throws a FrameworkException when the input/pattern/replacement is null.
    */
   public static String replaceAllWithBeginToken(String pattern, String input, String replacement,
                                                 String beginToken)
   throws FrameworkException
   {
      if( input == null || pattern == null || replacement == null )
      {
         throw new FrameworkException("RegexUtils: replaceAllWithBeginToken: input or pattern or replacement is null." +
                                      "\ninput = " + input + "\npattern = " + pattern + "\nreplacement = " + replacement);
      }

      Perl5Util util = new Perl5Util();

      // default return value
      String result = input;

      // conversion to the valid perl5 pattern regex
      String formatPattern = makePerl5MatchPattern( pattern );

      // if the beginToken is null, just do the regular replaceAll
      if( beginToken == null )
      {
         result = replaceAll( formatPattern, input, replacement );
      }
      // find if there is begin token in the input string
      else if( util.match( beginToken, input ) )
      {
         // pre has the string before the match and the mathced pattern
         String pre = input.substring( 0, util.endOffset(0) );

         // get the rest of the string after the begin token is found
         input = input.substring( util.endOffset(0) );
         
         result = replaceAll( pattern, input, replacement );
         result = pre + result;
      }

      return result;
   }


   /**
    * This function composes the perl regular expression to perform pattern matching
    * with negative look ahead. This becomes handy when a replacement string includes
    * some string that is also a pattern.
    * i.e.) pattern         - ",     &
    *       replacement     - &quot, &amp
    *       original input  - I didn't &0 to say "GOO".
    *
    * After the first substitution with " and &quot:
    *       processed input - I didn't &0 to say &quotGOO&quot.
    *
    * When we do the second substitution with '&', we do not want to perform the pattern
    * matching on the '&' of "&quot" since it is already a replacement.
    * If we give the pattern like this with perl's negative look-ahead functionality:
    *
    * not-negative look-ahead pattern - &
    * negative look-ahead pattern     - &(?!quot)
    *
    * After the second substitution with & and &amp with non-negative look-ahead:
    *       processed input - I didn't &amp0 to say &ampquotGOO&ampquot.
    *
    * After the second substitution with & and &amp with negative look-ahead:
    *       processed input - I didn't &amp0 to say &quotGOO&quot.

    * @param  pattern     - pattern string to match
    * @param  replacement - replacement
    *
    * @return returns the format pattern for perl5 negative look-ahead
    * @exception throws a FrameworkException when the input/pattern/replacement is null.
    *
    */
   private static String makePerl5MatchPatternNegativeLookAhead(String pattern, String replacement)
   throws FrameworkException
   {
      if( pattern == null || replacement == null )
      {
         throw new FrameworkException("RegexUtils: makePerl5MatchPatternNegativeLookAhead: pattern or replacement is null." +
                                      "\npattern = " + pattern + "\nreplacement = " + replacement);
      }

      String result = pattern;
      StringBuffer patternBuffer = new StringBuffer();

      Perl5Util util = new Perl5Util();
      String formatPattern = makePerl5MatchPattern( pattern );

      // check if the pattern string is a part of the replacement string
      if( util.match( formatPattern, replacement ) )
      {
         if( replacement.startsWith( pattern ) )
         {
            result = util.postMatch();
            patternBuffer.append( pattern );
            patternBuffer.append("(?!");
            patternBuffer.append(result);
            patternBuffer.append(")");
            result = patternBuffer.toString();
         }
         else
         {
            throw new FrameworkException("ERROR: RegexUtils: makePerl5MatchPatternNegativeLookAhead: The pattern in the " +
                                         "replacement string should be at the beginning.");
         }
      }

      return result;
   }

   /**
    * This function composes the perl regular expression to perform pattern matching
    * with negative look ahead. This becomes handy when a replacement string includes
    * some string that is also a pattern.
    *
    * i.e.) pattern         - &
    *       replacement     - &quot, &amp
    *
    * @param  pattern      - pattern string to match
    * @param  replacements - an array of replacement strings
    *
    * @return returns the format pattern for perl5 negative look-ahead
    * @exception throws a FrameworkException when the pattern/replacement is null.
    */
   private static String makePerl5MatchPatternNegativeLookAhead(String pattern, String replacements[])
   throws FrameworkException
   {
      if( pattern == null || replacements.length < 0 )
      {
         throw new FrameworkException("RegexUtils: makePerl5MatchPatternNegativeLookAhead: pattern or replacement is null." +
                                      "\npattern = " + pattern + "\nreplacements.length = " + replacements.length);
      }

      String result = null;
      StringBuffer patternBuffer = new StringBuffer();

      Perl5Util util = new Perl5Util();
      String formatPattern = makePerl5MatchPattern(pattern);

      for( int i=0; i<replacements.length; i++ )
      {
         if( util.match( formatPattern, replacements[i] ) )
         {
            if( replacements[i].startsWith( pattern ) )
            {
               result = util.postMatch();

               // very first one
               if( i == 0 )
               {
                  patternBuffer.append( pattern );
                  patternBuffer.append("(?!");
               }
               patternBuffer.append(result);

               // the last one
               if( i == (replacements.length - 1) )
               {
                  patternBuffer.append(")");
               }
               else // not the last one, cat the perl5 separater
               {
                  patternBuffer.append("|");
               }

            }
            else
            {
               throw new FrameworkException("ERROR: RegexUtils: makePerl5MatchPatternNegativeLookAhead: The pattern in the " +
                                            "replacement string should be at the beginning.");
            }
         }
         else // no match found meaning invalid use of this function
         {
            throw new FrameworkException( "ERROR: RegexUtils: makePerl5MatchPatternNegativeLookAhead: "+
                                          "Invalid use of the function.");
         }
      }

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) )
      {
         Debug.log(Debug.MSG_STATUS, "RegexUtils: makePerl5MatchPatternNegativeLookAhead: patternBuffer.toString() = "+patternBuffer.toString());
      }

      return patternBuffer.toString();
   }

   /**
    * This function checks if the replacement includes pattern string.
    *
    * i.e 1) if ( pattern = &, replacement = &amp ) returns true
    * i.e 2) if ( pattern = ", replacement = &quot ) returns false
    *
    * @param  pattern     - pattern string
    * @param  replacement - replacement string
    * @return    returns true if pattern string is a part of the replacement string,
    *            false, otherwise.
    * @exception throws a FrameworkException when the pattern/replacement is null.
    */
   public static boolean checkReplacement(String pattern, String replacement)
   throws FrameworkException
   {
      if( pattern == null || replacement == null )
      {
         throw new FrameworkException("RegexUtils: checkReplacement: pattern or replacement is null." +
                                      "\npattern = " + pattern + "\nreplacement = " + replacement);
      }

      boolean check = false;
      Perl5Util util = new Perl5Util();
      String formatPattern = makePerl5MatchPattern( pattern );

      // check if the pattern string is a part of the replacement string
      if( util.match( formatPattern, replacement ) )
      {
         if( replacement.startsWith( pattern ) )
         {
            check = true;
         }
      }

      return check;
   }


  public static void main(String args[])
  {
    Debug.enableAll();

    String input;
    String pattern;
    String result = null;
    String replacement;

    Perl5Util util = new Perl5Util();

    System.out.println("\nThis example performs ampersand replacement: & -> &amp");
    input = "<ILEC value=\"I have &0.>and &2<ILEC value=\"I have &1.\"/><ILEC value=\"I have &3.\"/>";

    String beginToken = "value=\"";
    String endToken   = "\"/>";
    pattern = "&";
    replacement = "&amp";
    System.out.println("input = " + input);
    System.out.println("pattern = " + pattern);
    System.out.println("replacement = " + replacement);
    System.out.println("beginToken = " + beginToken);
    System.out.println("endToken = " + endToken);
    try{
    result = replaceAll(pattern, input, replacement, beginToken, endToken);
    }catch(FrameworkException e){}
    System.out.println("main: result = " + result);

    System.out.println("\nThis example performs the bound replacement.");
    String patterns[]     = { "\"",     "'",     "<",   ">",   "&" };
    String replacements[] = { "&quot",  "&apos", "&lt", "&gt", "&amp" };
    beginToken = "name=\"";
    endToken   = "/test_2>";
    input = "<Test0 value=\"I am \"string\" and I got <'>.\"/><Test1 value=\"I am another with &.\"/>";

    System.out.println("input = " + input);
    System.out.println("beginToken = " + beginToken);
    System.out.println("endToken = " + endToken);
    try{
    result = replaceAll(patterns, input, replacements, beginToken, endToken);
    }catch(FrameworkException e){}
    System.out.println("\n\n\ndriver main: final result = " + result);

    return;

  } // main
} // class

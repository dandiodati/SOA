/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.spi.common.driver.*;

/**
 * TokenReplacer is a utility message processor which is used to handle special
 * characters in a message. Special character(s) are specified in the properties
 * as pattern(s). Pattern matching is performed on the message string, and the matched
 * string is replaced with the replacement found in the property.
 * This message processor replaces characters with the replacements
 * specified in the properties by means of perl5 expressions.
 *
 * The input messages may be found at the location indicated by
 * the property INPUT_MESSAGE_LOCATION_$. If this property doesn't exist, the message
 * processor gets the input from the given MessageObject. User can specify PATTERN_TO_MATCH_#
 * and TOKEN_TO_REPLACE_# pair for the substitution. PATTERN_TO_MATCH_# has the pattern
 * string to find the match on the input string and TOKEN_TO_REPLACE_# indicates the
 * replacement string to replace the pattern with. When BEGIN_BOUNDARY_# and END_BOUNDARY_#
 * are specified in the property, bound pattern match and substitution
 * can be done. BEGIN_BOUNDARY_# indicates the start of the boundary and END_BOUNDARY_# indicates
 * the end of the boundary. Then a pattern match/substitution can be performed on the bound part
 * of the input string.
 * Each message specified by INPUT_MESSAGE_LOCATION_$ is subjected to all the replacements
 * specified in the properties. Each INPUT_MESSAGE_LOCATION_$ contains the trasformed message
 * after this messageprocessor has finished processing.
 */

public class TokenReplacer extends MessageProcessorBase
{

    /**
     * Location of the input message
     */
    private static final String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";

    /**
     * Replacement string
     */
    private static final String TOKEN_TO_REPLACE_PROP = "TOKEN_TO_REPLACE";

    /**
     * Pattern string to match
     */
    private static final String PATTERN_TO_MATCH_PROP = "PATTERN_TO_MATCH";

    /**
     * Beginning of boundary for matching a pattern - optional
     */
    private static final String BEGIN_BOUNDARY_PROP = "BEGIN_BOUNDARY";

    /**
     * End of boundary for matching a pattern - optional
     */
    private static final String END_BOUNDARY_PROP = "END_BOUNDARY";

    /**
     * List to hold message locations
     */
    private List inputMessageList = null;

    /**
     * List to store patterns to be replaced and associated information like boundaries etc.
     */
    private List tokenList = null;


    /**
     * An inner class for storing pattern, bounds for pattern and replacement
     * token information
     */
    public class Token
    {
       private String pattern = null;
       private String replacementToken = null;
       private String beginBoundary = null;
       private String endBoundary = null;

       public Token ( String pattern, String replacementToken, String beginBoundary, String endBoundary )
          throws ProcessingException
       {
          //Perform check on arguments first..

          //Either both bounds should be null or both should exist
          if ( ( !StringUtils.hasValue(beginBoundary) && StringUtils.hasValue(endBoundary) ) ||
          ( StringUtils.hasValue(beginBoundary) && !StringUtils.hasValue(endBoundary) ) )
              throw new ProcessingException ("Values for boundaries are, " +
              "beginBoundary = [" + beginBoundary +
              "], endBoundary = [" + endBoundary +
              "]. Either both boundaries should be null or both should exist.");

          //When the replacement token has empty value, the corresponding pattern will be replaced with ""(empty string).
          if( replacementToken == null )
              replacementToken = "";

          this.pattern = pattern;
          this.replacementToken = replacementToken;
          this.beginBoundary = beginBoundary;
          this.endBoundary = endBoundary;
       }

       public String getReplacementToken()
       {
          return replacementToken;
       }

       public String getPattern()
       {
          return pattern;
       }

       public String getBeginBoundary()
       {
          return beginBoundary;
       }

       public String getEndBoundary()
       {
          return endBoundary;
       }

       /**
        * Method to describe contents of this object
        */
       public String toString ()
       {
          StringBuffer desc = new StringBuffer ( );
          desc.append ( "Pattern = [" );
          desc.append ( pattern );

          desc.append ( "] ReplacementToken = [" );
          desc.append ( replacementToken );

          desc.append ( "] BeginBoundary = [" );
          desc.append ( beginBoundary );

          desc.append ( "] EndBoundary = [" );
          desc.append ( endBoundary );

          desc.append ( "]." );

          return desc.toString ( );

       }

       /**
        * Method to describe contents of this object
        */
       public String describe ()
       {
          return toString ();
       }

    }//Token

    /**
     * Called to initialize this component.
     * Retrieves property from the persistent property.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
     {
        Debug.log( Debug.MSG_STATUS, "TokenReplacer: Initialization starting.");

        super.initialize(key, type);
        getInputMessageLocation();
        getTokens();

        Debug.log( Debug.MSG_STATUS, "TokenReplacer: Initialization done." );

    }//initialize


    /**
     * Process the input message. (Optionally) return a value.
     * If Input location is not found in the persisten property, it tries to
     * get the input from MO. The processed message is set back to where the input
     * message is found.
     *
     * @param   input     MessageObject that contains the message to be processed.
     * @param   mpcontext The message context
     * @return  Optional output NVPair array, or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input )
    throws MessageException, ProcessingException
    {

        //Processing here involves calls to RegexUtil class's methods, which basically
        //wrap perl5 expressions.

        if( input == null )
        {
           Debug.log (Debug.MSG_STATUS, "TokenReplacer: process: input is null.");
           return null;
        }

        Debug.log (Debug.MSG_STATUS, "TokenReplacer: processing start..");

        String inputMessageLocation = null;
        int inputMessageCounter = 0;
        int messageSize = 0;

        if( inputMessageList.size() <= 0 )
        //The message will be the only message in the MessageObject
        {
           messageSize = 1;
        }
        else
        {
           messageSize = inputMessageList.size();
        }

        // Perform replacement for each input message
        do
        {
           // get the location of a input message
           if( inputMessageList.size() > 0 )
           {
              inputMessageLocation = (String)inputMessageList.get( inputMessageCounter );
           }

           String inputMessage = getString( inputMessageLocation, mpcontext, input );

           // number of the boundary pairs
           int numTokens = tokenList.size();

           // Perform replacements for all the patterns specified
           for( int i = 0; i < numTokens; i++ )
           {

              //get the pattern and the replacement token and the boundaries ( if any ) specified
              //for that pattern.
              Token token = (Token)tokenList.get(i);
              String pattern = token.getPattern();
              String replacementToken = token.getReplacementToken();
              String beginBoundary = token.getBeginBoundary();
              String endBoundary   = token.getEndBoundary();

              // do replacement now
              try
              {
                 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                 {
                    Debug.log (Debug.MSG_STATUS, "TokenReplacer: performing replacment on: inputMessage = " +
                              inputMessage + "\n with " + token.describe() );

                 }

                 Debug.log(Debug.MSG_STATUS, "TokenReplacer: Calling RegexUtil.replaceAll().");

                 if ( !StringUtils.hasValue (beginBoundary) )
                 //Either both boundaries will be present or both will be absent.
                    inputMessage = RegexUtils.replaceAll ( pattern, inputMessage, replacementToken );
                 else
                    inputMessage = RegexUtils.replaceAll
                             ( pattern, inputMessage, replacementToken, beginBoundary, endBoundary );

                 if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log ( Debug.MSG_STATUS, "TokenReplacer: Message [" + inputMessageCounter +
                    "] after [" + i + "]th replacement is " + inputMessage );

              }
              catch(FrameworkException e)
              {
                 throw new MessageException("TokenReplacer: ERROR: replaceAll failed. "+
                                                e.getMessage());
              }
           }// for each pattern to be replaced

           // Replacing original message with processed message
           set( inputMessageLocation, mpcontext, input, inputMessage );

           inputMessageCounter++;

        } while( inputMessageCounter < messageSize );

        Debug.log (Debug.MSG_STATUS, "TokenReplacer processing done.");

        // send the result on through the chain according to properties.
        return formatNVPair ( input );

    }//process


    /**
     * Utility function called by initialize() to retrieve the location of the
     * input message.
     */
    private void getInputMessageLocation()
    {
        Debug.log( Debug.MSG_STATUS, "TokenReplacer: getInputMessageLocation() called.");

        inputMessageList  = new LinkedList();

        Debug.log( Debug.MSG_STATUS, "TokenReplacer: getInputMessageLocation(). before the loop.");

        // Loop until all column configuration properties have been read ...
        for ( int i = 0;  true;  i++ )
        {
            String inputMessageLocation = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_MESSAGE_LOCATION_PROP, i ) );

            // If we can't find input message, we are done.
            if ( !StringUtils.hasValue( inputMessageLocation ) )
            {
                Debug.log( Debug.MSG_STATUS, "TokenReplacer: getInputMessageLocation: No input message location found in the property.");
                break;
            }

            inputMessageList.add( i, inputMessageLocation );
        }
    }//getInputMessageLocation


    /**
     * This function retrieves properties related to the string manipulation.
     * It gets all the token(s), pattern(s), and begin and end boundary indicator(s).
     * It iterates until no pattern is found in the property.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     **/
    private void getTokens() throws ProcessingException
    {
        tokenList = new LinkedList();

        // Loop until all pattern properties have been read
        for ( int i = 0;  true;  i++ )
        {
            String pattern = getPropertyValue( PersistentProperty.getPropNameIteration( PATTERN_TO_MATCH_PROP, i ) );

            // If we can't find a pattern, we are done.
            if ( !StringUtils.hasValue( pattern ) )
            {
                break;
            }

            String replacementToken = getPropertyValue( PersistentProperty.getPropNameIteration( TOKEN_TO_REPLACE_PROP, i ) );

            String beginBoundary = getPropertyValue( PersistentProperty.getPropNameIteration( BEGIN_BOUNDARY_PROP, i ) );

            String endBoundary = getPropertyValue( PersistentProperty.getPropNameIteration( END_BOUNDARY_PROP, i ) );

            // create a new object to hold pattern/token information and add it to the list.
            Token token = null;
            try
            {
                token = new Token ( pattern, replacementToken, beginBoundary, endBoundary );
            }
            catch ( ProcessingException e )
            {
                throw new ProcessingException ( "ERROR: TokenReplacer: Iteration [" +
                i + "] of properties contained error : " + e.getMessage ( ) );
            }

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "TokenReplacer: Adding replacement set [" + i + "] " +
                "with values " + token.describe() );

            tokenList.add( i, token );
        }//for each pattern specified

    }//getTokens

}// TokenReplacer

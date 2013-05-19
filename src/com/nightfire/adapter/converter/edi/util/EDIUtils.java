/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi.util;

//Java imports
import java.io.StringReader;
import java.util.*;

//NF imports
import com.nightfire.framework.util.*;
import com.nightfire.adapter.converter.edi.EDIMessageConstants;

/**
 * Utilities to aid in parsing and generating EDI.
 */
public abstract class EDIUtils implements EDIMessageConstants{

    /**
     * Extract the String value of the designated Data Element.
     *
     * @param message The EDI message.
     * @param elemDelim The value of the Data Element Delimiter.
     * @param segDelim The value of the Segment Delimiter.
     * @param lineNumber The Line number (segment number) the 
     *                   desired Data Element resides in. 
     * @param elementNumber The number of the desired Data Element
     *                      in the given segment.
     * @return The String value of the designated Data Element. 
     * @exception FrameworkException if the Data Element or Segment don't
     *                               exist, or if the EDI message can not
     *                               be parsed using the given delimiters.
     */
    public static final String getElement( String message, String elemDelim,
            String segDelim, int lineNumber, int elementNumber ) throws FrameworkException{

        String segment = getSpecificToken( message, segDelim, lineNumber );
        String element = getSpecificToken( segment, elemDelim, elementNumber );
        return element;
    }

    /**
     * Extract the String value of the specified token from the given String. 
     *
     * @param aString The String from which to extract the specified token.
     * @param aDelim The delimiter on which to tokenize.
     * @param tokenPos The place in the token string of the desired token. 
     * @return The value of the desired token.
     * @exception FrameworkException if the desired token can not be found in the String.
     */
    public static final String getSpecificToken(String aString, String aDelim, int tokenPos)
            throws FrameworkException
    {

        StringReader sr = new StringReader( aString );

        ParsingTokenizer nizer = new ParsingTokenizer( sr );
        nizer.addDelimiter( aDelim );
        //Some field can be empty, so no trimming.
        nizer.setTrimming( false );

        // keep getting tokens
        int type = nizer.nextToken();
        int pos = 0;

        while( pos < tokenPos ){

            if(type == ParsingTokenizer.EOF)
                throw new FrameworkException("No token found at position " + tokenPos);

            if( (type = nizer.nextToken()) == ParsingTokenizer.RETRIEVED_TOKEN){

                if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
                    Debug.log( Debug.UNIT_TEST, "Pos = " + pos +
                            " token = " + nizer.getToken() );
                pos++;
            }
        }

        String token = nizer.getToken();
        if( Debug.isLevelEnabled(Debug.MSG_PARSE) )
                Debug.log( Debug.MSG_PARSE, "Token found [" + token + "]");
        return token;
    }
}

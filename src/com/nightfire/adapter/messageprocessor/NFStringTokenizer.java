/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/NFStringTokenizer.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;

import com.nightfire.framework.util.*;

/**
 * This class returns tokens separated by the delimiter as a group.
 * The number of tokens is number of delimters in the string plus 1. All tokens separated by
 * the delimiter will be returned even if there are empty string.
 * This rule includes the following cases:
 *     A. An empty string will be returned from an empty string input.
 *     B. If the input string starts with a delimiter, an empty token will be returned before
 * the position is advanced.
 *     C. If the input string ends with a delimiter, an empty token will be returned
 * after the last delimiter.
 *
 * Note: The java.util.StringTokenizer only tokenize a string on one charater
 * that can be any in the specified list.
 * 
 * Note: This class is not thread-safe and nextToken() is not synchronized
 * since it does not make sense to use a tokenizer in multiple threads.
 * 
 */
public class NFStringTokenizer
{

    private String delimiter;
    private String str;

    private int currentPosition;

    private int maxPosition;
    private int delimiterLength;
    
    
    /**
     * Constructor.
     *
     * @param  source The string to tokenized.
     * @param  delim  The string that separates the tokens.
     *
     * @exception FrameworkException If delim is null or empty string OR source is null.
     */
    public NFStringTokenizer(String source, String delim )
        throws FrameworkException
    {
        if (source == null)
        {
            throw new FrameworkException("ERROR: Cannot construct NFStringTokenier with null string.");
        }

        if (!StringUtils.hasValue(delim))
        {
            throw new FrameworkException("ERROR: Cannot construct NFStringTokenier with null or empty delimiter.");
        }
        
        this.str = source;
        this.delimiter = delim;

        currentPosition = 0;
        maxPosition = str.length();
        delimiterLength = delimiter.length();
        
    }


    /**
     * Count number of tokens remaining without advance current position.
     *
     * @return  The number of remaining tokens in current string.
     *
     */
    public int countTokens()
    {
        int thePosition = currentPosition;

        int numTokens = 0;

        int index = -1;

        // Loop until no more delimiter is found.
        while (thePosition <= maxPosition)
        {
            numTokens ++;

            
            index = str.indexOf(delimiter, thePosition);
            if (index < 0)
            {
                // No more delimter found.
                thePosition = maxPosition + 1;
                
            }
            else
            {
                thePosition = index + delimiterLength;
            }
        };
        
        return numTokens;
                    
    }

    /**
     * Return next token and advance current position.
     *
     * @return  The next token.
     *
     * @exception FrameworkException Thrown if no more tokens left. 
     */
    public String nextToken() throws FrameworkException
    {
	if (currentPosition > maxPosition) {
	    throw new FrameworkException("ERROR: No more tokens left.");
	}

        String token;
        int start = currentPosition;
        int index = str.indexOf(delimiter, start);
        if (index < 0)
        {
            // No more delimter found, return the rest.
            token = str.substring(currentPosition);
            currentPosition = maxPosition+1;
            
        }
        else
        {
            // Found delimiter, return the substring from start to index-1
            token = str.substring(start, index);
            // Skip to position after delimiter
            currentPosition = index + delimiterLength;
        }

        return token;
                    
    }


    /**
     * Return true if more tokens can be retrived.
     *
     * @return  Whether an token can be retrived.
     *
     */
    public boolean hasMoreTokens()
    {
	if (currentPosition > maxPosition) {
	    return false;
	}
        else
        {
            return true;
        }
        
    }
    

    public static void main (String[] args) throws FrameworkException
    {
        String testDelimiter = "***";

        String[] testStrings =
        {
            "ABC***DEF***GHI***JKL",
            "ABC***DEF***GHI***",
            "***DEF***GHI***JKL",
            "***DEF***GHI***",
            "ABC***",
            "***JKL",
            "******",
            "*****",
            "***",
            "ABC",
            "",
        };

        Debug.enableAll();
        
        for (int i=0; i<testStrings.length; i++)
        {
            test(testStrings[i], testDelimiter);
        }

        // Test source is null
        test(null, testDelimiter);

        // Test delimiter is null
        test("AAA", null);

        // Test delimiter is empty
        test("AAA", "");
        
    }


    private static void test(String source, String delim) throws FrameworkException
    {
        Debug.log(Debug.UNIT_TEST, "Tokenizing [" + source + "] with [" + delim + "]...");

        NFStringTokenizer st = null;
        
        try
        {
            st = new NFStringTokenizer(source, delim);
        }
        catch (FrameworkException fe)
        {
            Debug.logStackTrace(fe);

            Debug.log(Debug.ALL_ERRORS, "ERROR: Cannot construct NFStringTokenizer with [" +
                      source + ", " + delim + "].\n" + fe);

            return;
            
        }
        

        // Test countTokens
        int numTokens = st.countTokens();
        
        Debug.log(Debug.UNIT_TEST, "\tFound [" + numTokens + "] tokens...");

        // Test hasMoreTokens and nextToken
        int i=0;
        
        while (st.hasMoreTokens())
        {
            i++;
            
            String token = st.nextToken();
            Debug.log(Debug.UNIT_TEST, "\tToken [" + i + "] = [" + token + "]...");
        }

        Debug.log(Debug.UNIT_TEST, "\tCounted [" + i + "] tokens using nextToken().");

        if (numTokens != i)
        {
            throw new Error("ERROR: countTokens() and hasMoreTokens() differs.");
        }
        
        try
        {
            st.nextToken();
            throw new Error("ERROR: More tokens retrieved even if hasMoreTokens()returned false.");
            
        }
        catch (FrameworkException fe)
        {
            Debug.log(Debug.UNIT_TEST, "\tOne more nextToken got FrameworkException.\n" + fe.getMessage());
        }
    }
            
}



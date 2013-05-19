/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;

import java.util.StringTokenizer;


/**
 * The StringUtils class provides general-purpose string manipulation methods.
 */
public final class StringUtils
{

    /**
     * This accepts the string to be tokenize as per delimiter character
     * and convert to string array.
     *
     * @param str to be tokenized
     * @param deLim character to delimit the string
     * @return string array
     */
    public static final String[] getArray (String str, String deLim)
    {
        String COMMA_STR = "COMMA";
        String COMMA = ",";

        if (!StringUtils.hasValue ( str ))
        {
            return new String [0];
        }

        if (!StringUtils.hasValue ( deLim ))
        {
            return new String [] {str};
        }

        StringTokenizer strToken = new StringTokenizer (str, deLim);
        String [] arr = new String [strToken.countTokens ()];

        int i=0;
        while(strToken.hasMoreTokens())
        {
            arr [i] = strToken.nextToken();
            arr [i] = arr [i].replaceAll ( COMMA_STR, COMMA );
            i++;
        }

        return arr;
    }

    /**
     * Test to see if string is non-null and has a non-zero length.
     *
     * @param  item  String to test.
     *
     * @return  'true' if string is non-null and has a non-zero length, otherwise 'false'.
     */
    public static final boolean hasValue ( String item )
    {
        return( (item != null) && (item.length() > 0) );
    }

    /**
     * Test to see if string is non-null and has a non-zero length.
     * Also that it is not equals to NULL. Case is not considered.
     *
     * @param  item  String to test.
     *
     * @return  'true' if string is non-null and has a non-zero length, otherwise 'false'.
     */
    public static final boolean hasValue ( String item, boolean checkNULLString )
    {
        boolean status = hasValue ( item );

        if (checkNULLString && status && item.equalsIgnoreCase ( "NULL" ))
            status = false;

        return status;
    }


    /**
     * Pad the given number out to the given length with the given padding character.
     *
     * @param  num  The numeric value to pad.
     * @param  len  The length to pad out to.
     * @param  prepend  If 'true', value is left-padded, otherwise, it's right-padded.
     * @param  padChar  The character to use in padding.
     *
     * @return  A string containing the padded number.
     */
	public static final String padNumber ( int num, int len, boolean prepend, char padChar )
    {
		return( padString( String.valueOf( num ), len, prepend, padChar) );
	}


    /**
     * Pad the given string out to the given length with the given padding character.
     *
     * @param  str  The value to pad.
     * @param  len  The length to pad out to.
     * @param  prepend  If 'true', value is left-padded, otherwise, it's right-padded.
     * @param  padChar  The character to use in padding.
     *
     * @return  A string containing the padded number.
     */
	public static final String padString ( String str, int len, boolean prepend, char padChar )
    {
        int strLen = str.length( );

        // Do nothing if string is alread long enough
		if ( strLen >= len )
			return str;

		StringBuffer sb = new StringBuffer( );

		if ( prepend == false )
			sb.append( str );

		for ( int Ix = strLen;  Ix < len;  Ix++ )
        {
			sb.append( padChar );
		}

		if ( prepend == true )
			sb.append( str );

		return( sb.toString() );
	}


    /**
     * Replace all occurrences of the substring in the target string with replacement
     * string and return the transformed value.
     *
     * @param  target       Target string to transform.
     * @param  substr       String to replace in 'target'.
     * @param  replacement  String to use in replacement of 'substr'.
     *
     * @return  Transformed string.
     */
	public static final String replaceSubstrings ( String target, String substr, String replacement )
    {
        StringBuffer sb = new StringBuffer( );

        int substrLen = substr.length( );

        do
        {
            // Locate start of substring to be replaced.
            int startIndex = target.indexOf( substr );

            // If we can't find substring, we're done, so append remaining part of
            // target to transformed string.
            if ( startIndex == -1 )
            {
                sb.append( target );

                break;
            }

            // Append text from start of 'target' to start of 'substring' to new string.
            sb.append( target.substring( 0, startIndex ) );

            // Replace substring with that passed-in.
            sb.append( replacement );

            // Make new target equal to remaining part of target just past the substring we just replaced.
            target = target.substring( startIndex + substrLen );
        }
        while ( true );

        return( sb.toString() );
    }

    /**
    * Performs the same functionality as <code>replaceSubstrings</code>
    * except that case is ignored when looking for occurrences of
    * <code>substr</code> in <code>target</code>.
    */
    public static final String replaceSubstringsIgnoreCase
                                                  ( String target,
                                                   String substr,
                                                   String replacement )
    {

        StringBuffer sb = new StringBuffer( );

        int substrLen = substr.length( );
        String littleTarget = target.toLowerCase();
        String littleSub    = substr.toLowerCase();

        do
        {
            // Locate start of substring to be replaced.
            int startIndex = littleTarget.indexOf( littleSub );

            // If we can't find substring, we're done, so append remaining part of
            // target to transformed string.
            if ( startIndex == -1 )
            {
                sb.append( target );

                break;
            }

            // Append text from start of 'target' to start of 'substring' to new string.
            sb.append( target.substring( 0, startIndex ) );

            // Replace substring with that passed-in.
            sb.append( replacement );

            // Make new target equal to remaining part of target just past the substring we just replaced.
            target = target.substring( startIndex + substrLen );
            littleTarget = littleTarget.substring( startIndex + substrLen );
        }
        while ( true );

        return( sb.toString() );

    }
	
    /**
     * Replace all occurrences of the given word in the target string with replacement
     * string and return the transformed value.
     *
     * @param  target       Target string to transform.
     * @param  word         Word (or sub-string) to replace in 'target'.
     * @param  replacement  String to use in replacement of 'word'.
     *
     * @return  Transformed string.
     */
	public static final String replaceWord( String target, String word, String replacement ){
        StringBuffer sb = new StringBuffer( );
		// Split the target string into individual words
        String[] result = target.split("\\s");
		// Check each word, if it matches the word to be replaced, replace it with replacement string
        for (int x=0; x<result.length; x++){
            if(result[x].equals(word)) result[x] = replacement;
            sb.append(result[x]).append(" ");
        }
        return sb.toString().trim();
    }

    /**
    * Returns the index of the given key String ignoring case when looking
    * for a match.
    *
    * @returns the index if the <code>key</code> if found, -1 otherwise.
    *
    */
    public static int indexOfIgnoreCase(String target, String key){

       String littleTarget = target.toLowerCase();
       String littleKey    = key.toLowerCase();

       return littleTarget.indexOf(littleKey);

    }


	/**
	 * Properly formats text string for a Database by
	 * adding a single quote "'"
	 *
	 * @param	text Unformatted
	 * @return	Formatted string.
	 */
	public static String dbFormat(String text)
	{
		String str = new String(text);

		if (str.indexOf("'") > (-1))
		{
			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < str.length(); i++)
			{
				char letter = str.charAt(i);

				sb.append(letter);

				if (letter == '\'')
					sb.append("'");
			}

			str = sb.toString();
		}

		return str;
	}


	/**
	 * Creates new string consisting of only digits from the original one.
	 *
	 * @param	text with mix of digits and characters
	 * @return	output as string of digits only.
	 */
	public static String getDigits(String text)
	{
		StringBuffer sb = new StringBuffer();

		byte[] number = text.getBytes();
		for (int i = 0; i < number.length; i++)
		{
			if (Character.isDigit((char) number[i]))
				sb.append((char) number[i]);
		}

		return sb.toString();
	}


    /**
     * Return the boolean equivalent of the string argument.
     *
     * @param  value  Value containing string representation of a boolean value.
     *
     * @return Boolean true/false depending on the value of the input.
     *
     * @exception FrameworkException  Thrown if input does not have a valid value.
     */
    public static final boolean getBoolean ( String value ) throws FrameworkException
    {
        if ( !StringUtils.hasValue( value ) )
        {
            throw new FrameworkException( "ERROR: Can't convert a null/empty string value to a boolean." );
        }

        value = value.trim( );

        for ( int Ix = 0;  Ix < trueValues.length;  Ix ++ )
        {
            if ( value.equalsIgnoreCase( trueValues[Ix] ) )
                return true;
        }

        for ( int Ix = 0;  Ix < falseValues.length;  Ix ++ )
        {
            if ( value.equalsIgnoreCase( falseValues[Ix] ) )
                return false;
        }

        //Construct error message containing list of valid values
        StringBuffer validValues = new StringBuffer( );

        for ( int Ix = 0;  Ix < trueValues.length;  Ix ++ )
        {
            if ( Ix > 0 )
                validValues.append( ", " );

            validValues.append( trueValues[Ix] );
        }

        for ( int Ix = 0;  Ix < falseValues.length;  Ix ++ )
        {
            validValues.append( ", " );
            validValues.append( falseValues[Ix] );
        }

        throw new FrameworkException ( "ERROR: Candidate boolean value [" + value
                                       + "] not in valid-value set [" + validValues.toString() + "]." );
    }


    /**
     * Return the boolean equivalent of the string argument.
     *
     * @param  value  Value containing string representation of a boolean value.
     *
     * @param defaultBool Default boolean to use if the value is empty
     * or if it is an invalid value.
     * @return Boolean true/false depending on the value of the input.
     *
     * @exception FrameworkException  Thrown if input does not have a valid value.
     */
    public static final boolean getBoolean ( String value , boolean defaultBool)
    {
        if ( !StringUtils.hasValue( value ) )
            return defaultBool;

        try {
            return getBoolean(value);
        }
        catch (FrameworkException e) {
            Debug.warning("Invalid boolean value : " + e.getMessage() +", defaulting to " + defaultBool);
            return defaultBool;
        }
    }

    /**
     * Test the argument to see if it consists exclusively of digits.
     *
     * @param  value  String candidate to test.
     *
     * @return  'true' if the string contains just the characters [0-9], otherwise 'false'.
     */
    public static final boolean isDigits ( String value )
    {
        int len = value.length( );

        if ( len < 1 )
            return false;

        for ( int Ix = 0;  Ix < len;  Ix ++ )
        {
            if ( !Character.isDigit( value.charAt( Ix ) ) )
                return false;
        }

        return true;
    }

    /**
     * Test the argument to see if it consists exclusively of letters.
     *
     * @param  value  String candidate to test.
     *
     * @return  'true' if the string contains just letters, otherwise 'false'.
     */
    public static final boolean isLetter ( String value )
    {
        int len = value.length( );

        if ( len < 1 )
            return false;

        for ( int Ix = 0;  Ix < len;  Ix ++ )
        {
            if ( !Character.isLetter( value.charAt( Ix ) ) )
                return false;
        }

        return true;
    }


    /**
    * This is a utility method used to determine if the entire contents of the
    * given string is whitespace. Character.isWhitespace() is used to determine
    * whether the characters of the string are whitespace.
    *
    * @param text the string to be tested.
    * @returns true if every character in text is whitespace, false otherwise.
    *          If a null text value is passed in, this logs a warning, and
    *          returns false.
    */
    public static boolean isWhitespace(String text)
    {

       if(text == null)
       {
          Debug.warning("null value passed to StringUtils.isWhitespace()");
          return false;
       }

       for(int i = 0; i < text.length(); i++)
       {

          if(! Character.isWhitespace( text.charAt(i) ) )
          {
             return false;
          }

       }

       return true;

    }


    /**
     * Convert the string argument to its equivalent integer value.
     *
     * @param  value  String value containing an integer.
     *
     * @return  Integer equivalent of argument.
     *
     * @exception  FrameworkException  Thrown if argument is invalid.
     */
    public static final int getInteger ( String value ) throws FrameworkException
    {
        if ( !StringUtils.hasValue( value ) )
        {
            throw new FrameworkException ( "ERROR: Can't convert a null/empty string value to an integer." );
        }

        try
        {
            return( Integer.parseInt( value.trim() ) );
        }
        catch ( NumberFormatException nfe )
        {
            throw new FrameworkException( "ERROR: Invalid integer value [" + value
                                          + "]:\n" + nfe.toString() );
        }
    }


    /**
     * Gets name of the calling class
     *
     * @param   obj of type Object to get the name.
     * @return  Actual class name.
     */
    public static final String getClassName(Object obj)
    {
        String name = null;

        if (null == obj)
            return name;

        name = obj.getClass().getName();
        int index = name.lastIndexOf('.') == -1 ? 0 : name.lastIndexOf('.') + 1;
        return name.substring(index, name.length());
    }

    // Class only has static methods, so don't allow instances to be created!
    private StringUtils ( )
    {
        // NOT TO BE USED !!!
    }


    // Set of values that imply a true value.
    private static final String[] trueValues = {"Y", "YES", "TRUE", "T"};

    // Set of values that imply a false value.
    private static final String[] falseValues = {"N", "NO", "FALSE", "F"};
}

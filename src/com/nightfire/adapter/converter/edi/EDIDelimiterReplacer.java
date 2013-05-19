/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import java.io.*;
import com.nightfire.framework.util.*;

/**
 * This class is responsible for replacing delimiters in an EDI message.
 * This class defaults to replacing the delimiters with the default
 * delimiters specified in EDIMessageConstants.
 */
public class EDIDelimiterReplacer implements EDIMessageConstants{

    public static EDIDelimiters defaultDelimiters = new EDIDelimiters(DEFAULT_NF_DE_DELIMITER,
                                                        DEFAULT_NF_SUB_DE_DELIMITER,
                                                        DEFAULT_NF_SEGMENT_DELIMITER,
                                                        "");

    /**
     * Replace all of the delimiters in the EDI Message as specified in 
     * the oldDelimiters member value, with the delimiters specified
     * in the newDelimiters member value.
     *
     * @param input The EDI Message upon which to perform the replacement,
     * @param oldDelimiters The delimiters which will be 
     *                     removed from the EDI Message.
     * @return The EDI Message with the new set of delimiters.
     * @exception FrameworkException if an error occurs
     */
    public static String replaceDelimiters(String input, EDIDelimiters oldDelimiters) 
    throws FrameworkException
    {
        return replaceDelimiters(input, oldDelimiters, defaultDelimiters);
    }

    /**
     * Replace all of the delimiters in the EDI Message as specified in 
     * the oldDelimiters member value, with the delimiters specified
     * in the newDelimiters member value.
     *
     * @param input The EDI Message upon which to perform the replacement,
     * @param oldDelimiters The delimiters which will be 
     *                     removed from the EDI Message.
     * @param nfDelimiters The delimiters which will be 
     *                     inserted into the EDI Message.
     * @return The EDI Message with the new set of delimiters.
     * @exception FrameworkException if an error occurs
     */
    public static String replaceDelimiters(String input, EDIDelimiters oldDelimiters,
                                           EDIDelimiters nfDelimiters) 
    throws FrameworkException
    {

        String output = null;

        String safeDelimiter = null;

        //Replace ElementDelimiter
        output = RegexUtils.replaceAll(
                          getSafeDelimiter(oldDelimiters.getElementDelimiter()),
                          input,
                          nfDelimiters.getElementDelimiter());

        //Replace SubElementDelimiter
        output = RegexUtils.replaceAll(
				       getSafeDelimiter(oldDelimiters.getSubElementDelimiter()),
                          output,
                          nfDelimiters.getSubElementDelimiter());

        //Replace SegmentDelimiter
        output = RegexUtils.replaceAll(
                          getSafeDelimiter(oldDelimiters.getSegmentDelimiter()),
                          output,
                          nfDelimiters.getSegmentDelimiter());

        output = trimEDIMessage( output, nfDelimiters.getSegmentDelimiter() );

        return output;
    }

    /**
     * Return a version of the delimiter string which will safely
     * pass though the RegexUtils.
     * Currently, only a '\' is unsafe, and is made safe by escaping
     * it to provide '\\'.
     *
     * @param delimiter The delimiter upon which to ensure safety.
     * @return The safe version of the delimiter.
     */
    public static String getSafeDelimiter(String delimiter)
    {
        if(EDIMessageConstants.SLASH.equals(delimiter))
        {
            Debug.log(Debug.EDI_LIFECYCLE, "Slash as delimiter: escaping it.");
            return EDIMessageConstants.ESCAPED_SLASH;
        }
        return delimiter;
    }

    /**
     * Return a trimmed version of the message where any character after the
     * segment delimiter of the IEA segment is truncated.
     *
     * @param input The input message to be truncated.
     * @param segmentDelimiter The delimiter after which the IEA segment will
     * be truncated.
     * @return The trimmed version of the input message.
     */
    public static String trimEDIMessage( String input, String segmentDelimiter )
    {
        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST,
                            "input = [" + input + "]");

        StringBuffer outputBulk =
            new StringBuffer( input.substring( 0, input.lastIndexOf( IEA ) ) );

        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST,
                            "Bulk retained = [" + outputBulk.toString() + "]");

        String outputRest = input.substring( input.lastIndexOf( IEA ) );

        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST,
                            "IEA segment = [" + outputRest + "]");

        //Assumption: There will always be a segment delimiter
        outputRest = outputRest.substring( 0,
                outputRest.indexOf( segmentDelimiter ) + segmentDelimiter.length() );

        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST,
                            "IEA segment retained = [" + outputRest + "]");

        outputBulk.append(outputRest);

        return outputBulk.toString();
    }

    /**
     * Test main. 
     *
     * @param args a <code>String[]</code> value
     */
    public static void main(String[] args){

        Debug.enableAll();
        String input = null;
        try{

            input = FileUtils.readFile(args[0]);
            Debug.log(Debug.UNIT_TEST, input);

            EDIDelimiters oldDelim = EDIDelimiterExtractor.extractDelimiters(input);

            String output = EDIDelimiterReplacer.replaceDelimiters(input, oldDelim);
            Debug.log(Debug.UNIT_TEST, output);
            EDIDelimiters oldDelim2 = EDIDelimiterExtractor.extractDelimiters(output);
            oldDelim2.describe();

        }
        catch(Exception e){

            Debug.error(e.getMessage());
            Debug.logStackTrace(e);
        }
    }
}



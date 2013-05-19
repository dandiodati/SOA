/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.adapter.converter.edi.util.EDIUtils;

/**
 * Class responsible for performing a partial parse on an EDI message
 * and extracting the critical parsing information. Critical parsing 
 * information consists of the EDI Version of the transaction sets
 * in the message, and the three delimiters used in the message.
 */
public class EDIDelimiterExtractor implements EDIMessageConstants{

    /**
     * Ensure that the given String is at least the given minimum length. 
     *
     * @param stringChecked The String to be checked.
     * @param minLength The minimum length against which to check the given String.
     * @exception FrameworkException if the given String is not at least the 
     *                               the desired length.
     */
    private static void checkStringLength(String stringChecked, int minLength)
            throws FrameworkException{

        if( stringChecked.length() < minLength)
            throw new FrameworkException("String [" + stringChecked +
                    "] is supposed to be at least " + minLength + " char long");

    }

    /**
     * Perform a partial Parse of the EDI Message to extract the 
     * critical parsing information. 
     *
     * @param message The EDI message from which to extract critical parsing information.
     * @return EDIDelimiters instance containing the critical parsing information 
     *         extracted from the member EDI Message.
     * @exception FrameworkException if an error occurs
     */
    public static EDIDelimiters extractDelimiters(String message) 
    throws FrameworkException
    {

        try
        {
            //Get element delimiter
            checkStringLength( message, DE_DELIM_LOC + DE_DELIM_MAX_SIZE );
            // Assumption: DE delimiter is only 1 char long but can have any value
            String elemDelim = String.valueOf( message.charAt(DE_DELIM_LOC) );

            if( Debug.isLevelEnabled(Debug.MSG_PARSE) )
                Debug.log( Debug.MSG_PARSE, "Element Delimiter [" + elemDelim + "]");

            //Get last element of ISA, that includes the Segment Terminator
            //and begining of GS Segment
            String lastIsaElem = EDIUtils.getSpecificToken(message, elemDelim, SUB_DE_DELIM_POSITION);

            //Get SubElement separator
            //Assumption: Last element of ISA segment has length = 1.
            checkStringLength( lastIsaElem, SUB_DE_DELIM_LENGTH + SEGMENT_DELIM_MAX_SIZE );

            //Assumption: Sub DE delimiter can have any value
            String subElemDelim = String.valueOf( lastIsaElem.charAt(0) );

            if( Debug.isLevelEnabled(Debug.MSG_PARSE) )
                Debug.log( Debug.MSG_PARSE, "Sub Element Delimiter [" + subElemDelim + "]");

            //Get Segment Delimiter
            //New Assumption: Segment delimiter can have any value if its length
            // is 1 char, otherwise, can be up to 3 characters and check against
            // a list of valid values.

            //Get Delimiter
            String segDelim = lastIsaElem.substring(
                                SUB_DE_DELIM_LENGTH,
                                lastIsaElem.indexOf(EDIMessageConstants.GS)
                              );
            if( Debug.isLevelEnabled(Debug.MSG_PARSE) )
                Debug.log( Debug.MSG_PARSE, "Segment Delimiter [" + segDelim + "]");

            //Get EDI Version
            String ediVersion =
                    EDIUtils.getElement(
                                          message,
                                          elemDelim,
                                          segDelim,
                                          EDI_VERSION_LINE_NUMBER,
                                          EDI_VERSION_ELEMENT_NUMBER
                                        );
            if( Debug.isLevelEnabled(Debug.MSG_PARSE) )
                Debug.log( Debug.MSG_PARSE, "EDI Version [" + ediVersion + "]");

            return new EDIDelimiters(elemDelim, subElemDelim, segDelim, ediVersion);
        }
        catch(StringIndexOutOfBoundsException siobe){

            throw new FrameworkException("Delimiters Extraction failed:\n" + siobe.toString());
        }
        catch(FrameworkException fe){

            throw new FrameworkException("Delimiters Extraction failed:\n" + fe.toString());
        }
    }

    /**
     * Test main.
     *
     * @param args Name of a file containing an EDI Message.
     */
    public static void main(String[] args){

        Debug.enableAll();
        Debug.showLevels();

        if(args.length < 1){

            Debug.error("java EDIDelimiterExtractor <EDI File Path>");
            System.exit(0);
        }

        try{

            String msg = FileUtils.readFile(args[0]);

            Debug.log( Debug.UNIT_TEST, "Input Message = [" + msg + "]" );

            EDIDelimiters del = extractDelimiters( msg );
            del.describe();
        }
        catch(Exception e){

            Debug.error(e.toString());
            Debug.logStackTrace(e);
        }
    }
}

/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 * @author:Srinivas R Pakanati
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/UnicodeCharReplacer.java#1 $
 */

package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.io.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;

/**
 * Replaces characters in the input message with the specified characters.
 * The replaceable characters and the replacement characters must be specified
 * in unicode numeric representation ( hexadecimal number ). For example
 * to replace a single quote ' character either use the code \u0027 or simply 0027
 */
public class UnicodeCharReplacer extends MessageProcessorBase
{

    public static final String ORIGINAL_UNICODE_VALUE_ITER_PROP = "ORIGINAL_UNICODE_VALUE";

    public static final String NEW_UNICODE_VALUE_ITER_PROP = "NEW_UNICODE_VALUE";
    
    public static final int HEXADECIMAL_RADIX = 16;
    
    public static final String UNICODE_CHARACTER_ESCAPE = "\\u";


    /**
     * Processes the input message and returns the name value pair
     * of a specified message processor and a transformed message object
     *
     * @param  context The Message Processor context
     *
     * @param  inputObject  Input message Object to process.
     *
     * @return  Optional NVPair containing a Destination name and a Document,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if the given input message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject inputObject )
        throws MessageException, ProcessingException 
    {

	//When the given input message is null do not proceed with further processing
	if ( inputObject == null )
	{
	    return null;
	}
	
	//Extract the message as a String
	String message = inputObject.getString();

	//Loop through the iterative properties and get the replaceable and replacing characters
        int counter = 0;

        while (true)
        {
            
            String originalCharPropTag;

            String originalCharPropValue;

            String newCharPropTag;

            String newCharPropValue;

            originalCharPropTag = PersistentProperty.getPropNameIteration(  ORIGINAL_UNICODE_VALUE_ITER_PROP, counter );

            originalCharPropValue = (String) getPropertyValue( originalCharPropTag );

	    newCharPropTag = PersistentProperty.getPropNameIteration( NEW_UNICODE_VALUE_ITER_PROP, counter );
	    
	    newCharPropValue = (String) getPropertyValue( newCharPropTag );
            
            if ( ! StringUtils.hasValue ( originalCharPropValue ) )
            {			
                break;
            }
                            
	    //Remove any \ u escape from the property values
	    originalCharPropValue = StringUtils.replaceSubstrings ( originalCharPropValue, UNICODE_CHARACTER_ESCAPE, "");
	    if ( newCharPropValue != null ) 
	    {
		newCharPropValue = StringUtils.replaceSubstrings ( newCharPropValue, UNICODE_CHARACTER_ESCAPE, "");
	    }

	    char originalChar;

	    try
	    {
		originalChar = ( char ) Integer.parseInt ( originalCharPropValue , HEXADECIMAL_RADIX );
	    }
	    catch ( NumberFormatException ne )
	    {
		throw new ProcessingException ( "Please specify a valid hexadicimal integer value for property [" + 
						originalCharPropTag +"]. Current value is [" + originalCharPropValue +"]" );
	    }
	    
	    //Initialize the replacing character as a space
	    char newChar = ' ';

	    if ( StringUtils.hasValue ( newCharPropValue ) )
	    {
		try
		{
		    newChar = ( char )  Integer.parseInt ( newCharPropValue, HEXADECIMAL_RADIX ) ;
		}
		catch ( NumberFormatException ne )
		{
		    throw new ProcessingException ( "Please specify a valid hexadecimal integer value for property [" + 
						    newCharPropTag +"]. Current value is [" + newCharPropValue +"]" );
		}
	    }

	    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
	    {
		Debug.log( Debug.MSG_STATUS, "The original character's Unicode Value is [" + 
			   UNICODE_CHARACTER_ESCAPE + originalCharPropValue +"], displayed as [" + 
			   originalChar +"]" );
	    }
	    
	    //Default Replacing Char is a space
	    if ( newChar != ' ')
	    {
		if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
		{
		    Debug.log( Debug.MSG_STATUS, "The new character's Unicode Value is [" + 
			       UNICODE_CHARACTER_ESCAPE + newCharPropValue +"], displayed as [" + 
			       newChar +"]" );
		}
	    }
	    else
	    {
	        Debug.log( Debug.MSG_STATUS, "Using the default replacement character [" + newChar +"]" );
	    }

	    //If the specified character exists in the message
	    if ( message.indexOf ( originalChar ) != -1 )
	    {
		
		if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
		{
		    Debug.log( Debug.MSG_STATUS, "Message before replacement =[" + message +"]" );
		    Debug.log( Debug.MSG_STATUS, "Replacing [" + originalChar +"] with [" + newChar +"]" );
		}

		//Replace the character
		message =  message.replace (  originalChar, newChar  );

		if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
		{
		    Debug.log( Debug.MSG_STATUS, "Message after replacement =[" + message +"]" );
		}
	    }

	    //Increment the loop counter
            counter ++;
	    
        } // End while
	

	// Return the transformed message 
        return formatNVPair( message );
    }


}

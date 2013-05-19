/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.text.*;
import java.io.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.db.*;


/**
 * Message-processor that constructs a text message from a template file
 * containing replacement tokens. The replacement values are taken from
 * the context or input message.
 */
public class TextMessageFormatter extends MessageProcessorBase
{
    /**
     * Name of the template message file.
     */
    public static final String TEMPLATE_MESSAGE_FILE_NAME_PROP = "TEMPLATE_MESSAGE_FILE_NAME";


    /**
     * Constructor.
     */
    public TextMessageFormatter ( )
    {
        className = StringUtils.getClassName( this );

        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating TextMessageFormatter message-processor." );
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, className + ": Initializing..." );

        templateMessageFileName = getRequiredPropertyValue( TEMPLATE_MESSAGE_FILE_NAME_PROP );
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "template message file name [" 
                       + templateMessageFileName + "]." );
        
        errorContextLocation = getPropertyValue( MessageProcessingDriver.ERROR_CONTEXT_LOC_PROP );
        
        // Use default if no configured value was given.
        if ( !StringUtils.hasValue( errorContextLocation ) )
            errorContextLocation = MessageProcessingDriver.DEFAULT_ERROR_CONTEXT_LOC;
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error-context location in the message-processor context [" 
                       + errorContextLocation + "]." );
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, className + ": Initialization done." );
    }


    /**
     * Populate template message from extracted values from context/input.
     *
     * @param  context The context
     * @param  input  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException 
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Processing ..." );

        if ( input == null )
            return null;

        // Populated message.
        StringBuffer message = new StringBuffer( );

        try
        {
            String templateMessage = fileCache.get( templateMessageFileName );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Template message before token replacement:\n" + templateMessage );

            // Loop over text in template until no more replacement tokens can be found.
            for ( int startIndex = templateMessage.indexOf( START_TOKEN );  
                  startIndex != -1;  
                  startIndex = templateMessage.indexOf( START_TOKEN ) )
            {
                // Append template message up to start of replacement token to
                // the output message.
                message.append( templateMessage.substring( 0, startIndex ) );

                // Advance index past start token indicator.
                startIndex += START_TOKEN.length( );

                // Locate the end of the replacement token.
                int endIndex = templateMessage.indexOf( END_TOKEN, startIndex );

                if ( endIndex == -1 )
                {
                    String err = "ERROR: Malformed replacement token in template file [" 
                        + templateMessageFileName + "] near location [" + startIndex + "].";

                    Debug.error( err );

                    throw new ProcessingException( err );
                }

                // Extract the token to be replaced from the message.
                String token = templateMessage.substring( startIndex, endIndex );

                // Append the token's replacement value to the output message.
                message.append( getTokenValue( token, context, input ) );

                // Adjust template message so that it consists of all remaining text 
                // that follows the token just replaced.
                templateMessage = templateMessage.substring( endIndex + END_TOKEN.length() );
            }

            // Append the remaining token-free part of the template message to the output message.
            message.append( templateMessage );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Resulting message after token replacement:\n" + message.toString() );
        }
        catch ( ProcessingException pe )
        {
            throw pe;
        }
        catch ( MessageException me )
        {
            throw me;
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Done processing." );

        // Return the populated message.
        return( formatNVPair( message.toString() ) );
    }


    /**
     * Get the indicated token from the context or input.
     *
     * @param  token  The token whose value should be obtained.
     * @param  context  The context.
     * @param  input  Input message.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    private String getTokenValue ( String token, MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException 
    {
        String value = null;
        
        boolean optional = false;
        
        if ( token.endsWith( OPTIONAL_INDICATOR ) )
        {
            optional = true;
            
            token = token.substring( 0, token.length() - OPTIONAL_INDICATOR.length() );
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
            Debug.log( Debug.MSG_DATA, "Obtaining value for token [" + token + "].  Optional? [" + optional + "]." );
        
        // Handle 'special' tokens that don't map to regular input/context values.
        if ( token.startsWith( SYSTEM_DATE_TOKEN ) )
            value = getSystemDate( token );
        else
        if ( token.equals( ERROR_MESSAGE_TOKEN ) )
            value = getExceptionMessage( context, optional );
        else if ( token.equals( CustomerContext.CUSTOMER_ID_PROP ) )
        {
            try
            {
                value = CustomerContext.getInstance().getCustomerID( );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( e.getMessage() );
            }
        }
        else if ( token.equals( CustomerContext.USER_ID_NODE ) )
        {
            try
            {
                value = CustomerContext.getInstance().getUserID( );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( e.getMessage() );
            }
        }
        else
        {
            // Handle standard input/context values.
            if ( exists( token, context, input ) )
                value = getString( token, context, input );
            else
            {
                if ( optional )
                {
                    Debug.log( Debug.MSG_DATA, "Optional token is absent." );
                    
                    return "";
                }
                else
                {
                    String err = "ERROR: Missing required value [" + token + "].";
                    
                    Debug.error( err );
                    
                    throw new MessageException( err );
                }
            }
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
            Debug.log( Debug.MSG_DATA, "Token [" + token + "] value is [" + value + "]." );
        
        return value;
    }


    /**
     * Get the current system time.
     *
     * @param  token  The system-time token - possibly containing a
     *                date format string.
     *
     * @return  The properly-formatted system time.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    private String getSystemDate ( String token )
        throws MessageException, ProcessingException 
    {
        Date current = new Date( );
        
        // Advance past token indicating system date.
        token = token.substring( SYSTEM_DATE_TOKEN.length() );

        // A comma indicates that the rest of the token string
        // is a date format specification.
        int formatStart = token.indexOf( "," );
        
        if ( formatStart == -1 )
            return( current.toString() );
        else
        {
            SimpleDateFormat sdf = new SimpleDateFormat( token.substring( formatStart + 1 ) );
            
            return( sdf.format( current ) );
        }
    }


    /**
     * Get the exception message from the context.
     *
     * @param  context  The context.
     * @param  optional  Flag indicating whether exception 
     *                   is required or not.
     *
     * @return  The exception message.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    private String getExceptionMessage ( MessageProcessorContext context, boolean optional )
        throws MessageException, ProcessingException 
    {
        if ( !context.exists( errorContextLocation, false ) )
        {
            if ( optional )
            {
                Debug.log( Debug.MSG_DATA, "Optional exception is absent." );
                
                return "";
            }
            else
            {
                String err = "ERROR: Missing required exception at context location [" + errorContextLocation + "].";
                
                Debug.error( err );
                
                throw new MessageException( err );
            }
        }

        // Get the error-context from the message-processor context, and extract 
        // values for use in access key and message token replacement.
        List errors = (List)context.get( errorContextLocation );
        
        if ( errors == null )
            throw new ProcessingException( "ERROR: Error-context not available in message-processor context." );
        
        if ( errors.size() == 0 )
            throw new ProcessingException( "ERROR: No items available in list of error-contexts." );
        
        MessageProcessingDriver.ErrorContext ec = (MessageProcessingDriver.ErrorContext)errors.get( 0 );
        
        return( ec.error.getMessage() );
    }


    // String indicating the start of a replacement token.
    private static final String START_TOKEN = "{";
    // String indicating the end of a replacement token.
    private static final String END_TOKEN = "}";
    // String indicating that the replacement token is optional.
    private static final String OPTIONAL_INDICATOR = "?";
    // String indicating the current system date token.
    private static final String SYSTEM_DATE_TOKEN = "SYSDATE";
    // String indicating the current exception message token.
    private static final String ERROR_MESSAGE_TOKEN = "ERROR_MESSAGE";


    // Name of file containing template message.
    private String templateMessageFileName;
    // A global cache of message file contents.
    private static FileCache fileCache = new FileCache( );
    // The location in the context where the list of ErrorContext objects can be found.
    private String errorContextLocation;
    // Abbreviated class name for use in logging.
    private String className;
}

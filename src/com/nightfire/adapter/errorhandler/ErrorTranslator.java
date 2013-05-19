/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.adapter.errorhandler;


import java.util.*;
import java.text.*;
import java.io.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;


/**
 * Message-processor used to translate error messages resulting from 
 * exceptions thrown by message-processors.  Typically used as part of
 * an error-handler configured on the Driver.
 */
public class ErrorTranslator extends MessageProcessorBase
{
    /**
     * Property giving the location of the file containing exception translation messages.
     */
    public static final String TRANSLATION_FILE_NAME_PROP = "TRANSLATION_FILE_NAME";
    
    /**
     * Property giving the format of the key used to access the translated error 
     * messages. (Iterated)
     * The formatting tokens obey the java.text.MessageFormat conventions, with:
     * 0 = message-processor class name (including enclosing packages) 
     * 1 = The name of the message-processor, as known by the Driver.
     * 2 = The type of exception (MessageException/ProcessingException).
     * 3 = The exception message.
     * 4 = The current system time.
     * 5 = The customer-id.
     * 6 = The user-id.
     */
    public static final String MESSAGE_KEY_FORMAT_PROP = "MESSAGE_KEY_FORMAT";


    /**
     * Constructor.
     */
    public ErrorTranslator ( )
    {
        className = StringUtils.getClassName( this );
    }
    
    
    /**
     * Called to initialize a message processor object.
     * 
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize( key, type );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Initializing ..." );
        
        translationFileName = getPropertyValue( TRANSLATION_FILE_NAME_PROP );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error-translations file name [" + translationFileName + "]." );
        
        errorContextLocation = getPropertyValue( MessageProcessingDriver.ERROR_CONTEXT_LOC_PROP );
        
        // Use default if no configured value was given.
        if ( !StringUtils.hasValue( errorContextLocation ) )
            errorContextLocation = MessageProcessingDriver.DEFAULT_ERROR_CONTEXT_LOC;
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error-context location in the message-processor context [" 
                       + errorContextLocation + "]." );
        
        messageKeyFormats = new LinkedList( );

        // While iterated properties are present ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String msgKeyFormat = getPropertyValue( PersistentProperty.getPropNameIteration( MESSAGE_KEY_FORMAT_PROP, Ix ) );
            
            // One of the two locations must have a value, or we're done.
            if ( !StringUtils.hasValue( msgKeyFormat ) )
            {
                if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                    Debug.log( Debug.SYSTEM_CONFIG, "Got [" + Ix + "] error-mapping format keys." );

                break;
            }

            // Remember the context configuration item for later driver execution.
            messageKeyFormats.add( msgKeyFormat );
        }
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error message translation keys:\n" + messageKeyFormats.toString() );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, className + ": Done initializing." );
    }
    
    
    /**
     * Translate the exception message, if a mapping is found between
     * the old exception and a key in the translations file.
     * 
     * @param  context  The  message context.
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objects, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Processing ..." );
        
        // We're not batching, so there's nothing to do for null inputs.
        if ( input == null )
            return null;
        
        Exception translatedException = null;
        
        try
        {
            Properties errorTranslations = new Properties( );
            
            // Load the properties file containing the error-translation mappings, if available.
            if ( StringUtils.hasValue( translationFileName ) )
            {
                FileUtils.FileStatistics fs = FileUtils.getFileStatistics( translationFileName );
                
                if ( fs.exists  && fs.readable && fs.isFile )
                    errorTranslations = FileUtils.loadProperties( errorTranslations, translationFileName );
            }
            
            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Error message translations:\n" + errorTranslations.toString() );
            
            // Get the error-context from the message-processor context, and extract 
            // values for use in access key and message token replacement.
            List errors = (List)context.get( errorContextLocation );
            
            if ( errors == null )
                throw new ProcessingException( "ERROR: Error-context not available in message-processor context." );
            
            if ( errors.size() == 0 )
                throw new ProcessingException( "ERROR: No items available in list of error-contexts." );
            
            MessageProcessingDriver.ErrorContext ec = (MessageProcessingDriver.ErrorContext)errors.get( 0 );
            
            // Construct the object array to use in java.text.MessageFormat token replacement
            // using items associated with the exception being processed.
            Object[] keyComponents = new Object [ 7 ];
            
            keyComponents[0] = ec.processor.getClass().getName( );
            keyComponents[1] = ec.processor.getName( );
            keyComponents[2] = StringUtils.getClassName( ec.error );
            keyComponents[3] = ec.error.getMessage( );
            keyComponents[4] = new Date( );
            keyComponents[5] = CustomerContext.getInstance().getCustomerID( );
            keyComponents[6] = CustomerContext.getInstance().getUserID( );
            
            Debug.log( Debug.MSG_DATA, "Available Error message tokens:" );
            
            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
            {
                for ( int Ix = 0;  Ix < keyComponents.length;  Ix ++ )
                    Debug.log( Debug.MSG_DATA, "[" + Ix + "] = [" + keyComponents[Ix] + "]." );
            }

            String translatedMessage = null;
            
            // Iterate over keys until we find the first match, if any.
            // When match is found, translate the message.
            Iterator iter = messageKeyFormats.iterator( );
            
            while ( iter.hasNext() )
            {
                String keyFormat = (String)iter.next( );
                
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Error translations access key format [" + keyFormat + "]." );
                
                String key = MessageFormat.format( keyFormat, keyComponents );
                
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Translated message access key [" + key + "]." );
                
                translatedMessage = (String)errorTranslations.get( key );
                
                if ( StringUtils.hasValue( translatedMessage ) )
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "Translated message before replacements [" + translatedMessage + "]." );
                    
                    translatedMessage = MessageFormat.format( translatedMessage, keyComponents );
                    
                    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "Translated message after replacements [" + translatedMessage + "]." );
                    
                    break;
                }
                else
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "No match for translation error mesage key [" + key + "] ..." );
                }
            }
            
            // Create the exception to be thrown using the translated message.
            if ( StringUtils.hasValue( translatedMessage ) )
            {
                if ( ec.error instanceof MessageException )
                    translatedException = new MessageException( translatedMessage );
                else
                    translatedException = new ProcessingException( translatedMessage );
                
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Returning the following exception:\n" + translatedException );
            }
            else
                Debug.log( Debug.MSG_DATA, "Returning null error translation." );
        }
        catch ( Exception e )
        {
            Debug.error( e.toString() );
            
            Debug.error( Debug.getStackTrace() );

            // We don't thrown an exception here, since the default behavior of
            // returning null is appropriate in the case of internal exceptions.
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Done processing." );
        
        // Return any results as name/value-pairs.
        return( formatNVPair( translatedException ) );
    }
    
    
    // A list of java.text.MessageFormat format-compatible strings
    // providing translations access key formats.
    private List messageKeyFormats;

    private String translationFileName;

    // The location in the context where the list of ErrorContext objects can be found.
    private String errorContextLocation = null;

    // Abbreviated class name for use in logging.
    private String className;
}

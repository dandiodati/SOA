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
 * Message-processor used to filter out exceptions that shouldn't
 * be processed by subsequent message-processors.  Typically used 
 * as part of an error-handler configured on the Driver.
 */
public class ErrorFilter extends MessageProcessorBase
{
    /**
     * Property giving the location of the file containing error filter values (optional).
     */
    public static final String ERROR_FILTER_FILE_NAME_PROP = "ERROR_FILTER_FILE_NAME";
    
    /**
     * Property giving the format of the key used to access the error filter keys.
     * (Iterated)
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
     * Property giving the interpretation of the filter value.  If 'false', the error will 
     * be filtered-out if a match is found with a value of 'ignore'.  If 'true', the error 
     * won't be filtered-out if no match is found. (Optional - default value is false.)
     */
    public static final String INVERT_FILTER_FLAG_PROP = "INVERT_FILTER_FLAG";
    
    
    // Flag value indicating that error should be filtered out and ignored.
    public static final String IGNORE_FLAG_VALUE = "ignore";


    /**
     * Constructor.
     */
    public ErrorFilter ( )
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
        
        errorFilterFileName = getPropertyValue( ERROR_FILTER_FILE_NAME_PROP );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error-filter file name [" + errorFilterFileName + "]." );
        
        errorContextLocation = getPropertyValue( MessageProcessingDriver.ERROR_CONTEXT_LOC_PROP );
        
        // Use default if no configured value was given.
        if ( !StringUtils.hasValue( errorContextLocation ) )
            errorContextLocation = MessageProcessingDriver.DEFAULT_ERROR_CONTEXT_LOC;
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error-context location in the message-processor context [" 
                       + errorContextLocation + "]." );
        
        invertFilterSense = false;  // Default value is 'false'.
        
        String temp = getPropertyValue( INVERT_FILTER_FLAG_PROP );
        
        try
        {
            if ( StringUtils.hasValue( temp ) && (StringUtils.getBoolean( temp ) == true) )
                invertFilterSense = true;
        }
        catch ( Exception e )
        {
            Debug.error( e.toString() );

            throw new ProcessingException( e.toString() );
        }

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "invert-filter? [" + invertFilterSense + "]." );
        
        errorFilterKeys = new LinkedList( );
        
        // While iterated properties are present ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String msgKeyFormat = getPropertyValue( PersistentProperty.getPropNameIteration( MESSAGE_KEY_FORMAT_PROP, Ix ) );
            
            // No more items are available, so exit loop.
            if ( !StringUtils.hasValue( msgKeyFormat ) )
            {
                if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                    Debug.log( Debug.SYSTEM_CONFIG, "Got [" + Ix + "] error-filter format keys." );

                break;
            }

            // Remember the context configuration item for later driver execution.
            errorFilterKeys.add( msgKeyFormat );
        }
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error message translation keys:\n" + errorFilterKeys.toString() );

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
        
        boolean ignore = false;

        Exception originatingException = null;

        try
        {
            Properties errorFilters = new Properties( );
            
            // Load the properties file containing the error-filter mappings, if available.
            if ( StringUtils.hasValue( errorFilterFileName ) )
            {
                FileUtils.FileStatistics fs = FileUtils.getFileStatistics( errorFilterFileName );
                
                if ( fs.exists  && fs.readable && fs.isFile )
                    errorFilters = FileUtils.loadProperties( errorFilters, errorFilterFileName );
            }

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Error filters:\n" + errorFilters.toString() );
            
            // Get the error-context from the message-processor context, and extract 
            // values for use in access key and message token replacement.
            List errors = (List)context.get( errorContextLocation );
            
            if ( errors == null )
                throw new ProcessingException( "ERROR: Error-context not available in message-processor context." );
            
            if ( errors.size() == 0 )
                throw new ProcessingException( "ERROR: No items available in list of error-contexts." );
            
            MessageProcessingDriver.ErrorContext ec = (MessageProcessingDriver.ErrorContext)errors.get( 0 );
            
            originatingException = ec.error;

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
            
            Debug.log( Debug.MSG_DATA, "Available Error-filter tokens:" );
            
            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
            {
                for ( int Ix = 0;  Ix < keyComponents.length;  Ix ++ )
                    Debug.log( Debug.MSG_DATA, "[" + Ix + "] = [" + keyComponents[Ix] + "]." );
            }

            // Iterate over keys until we find the first match, if any.
            // When match is found, check the corresponding flag value.
            Iterator iter = errorFilterKeys.iterator( );
            
            while ( iter.hasNext() )
            {
                String keyFormat = (String)iter.next( );
                
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Error-filter access key format [" + keyFormat + "]." );
                
                String key = MessageFormat.format( keyFormat, keyComponents );
                
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Translated error-filter access key [" + key + "]." );
                
                String ignoreValue = (String)errorFilters.get( key );
                
                if ( StringUtils.hasValue( ignoreValue ) && ignoreValue.equalsIgnoreCase( IGNORE_FLAG_VALUE ) )
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "Got an ignore-match for error-filter [" + key + "] ..." );

                    ignore = true;

                    break;
                }
            }
        }
        catch ( Exception e )
        {
            Debug.error( e.toString() );
            
            Debug.error( Debug.getStackTrace() );

            // We don't thrown an exception here, since the default behavior of
            // returning null is appropriate in the case of internal exceptions.
        }
        
        // If the flag is 'true', invert the outcome of processing.
        if ( invertFilterSense )
            ignore = !ignore;

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, className + ": Done processing." );
        
        if ( ignore )
        {
            NVPair[] results = new NVPair[ 1 ];
            
            results[ 0 ] = new NVPair( MessageProcessingDriver.TO_COMM_SERVER, null );
            
            return results;
        }
        else
            return( formatNVPair( input ) );
    }
    
    
    // A list of java.text.MessageFormat format-compatible strings
    // providing translations access key formats.
    private List errorFilterKeys;

    private String errorFilterFileName;

    // The location in the context where the list of ErrorContext objects can be found.
    private String errorContextLocation = null;

    // Flag indicating whether meaning of filter should be inverted or not.
    private boolean invertFilterSense;

    // Abbreviated class name for use in logging.
    private String className;
}

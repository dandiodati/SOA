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
 * Message-processor used to write externally formatted error messages 
 * out to the current Debug log file.
 */
public class ErrorDebugLogger extends MessageProcessorBase
{
    /**
     * Property giving the format of the debug message (required).
     * The embedded formatting tokens obey the java.text.MessageFormat conventions, with:
     * 0 = Message-processor class name (including enclosing packages) 
     * 1 = The name of the message-processor, as known by the Driver.
     * 2 = The type of exception (MessageException/ProcessingException).
     * 3 = The exception message.
     * 4 = The current system time.
     * 5 = The customer-id.
     * 6 = The user-id.
     */
    public static final String MESSAGE_FORMAT_PROP = "MESSAGE_FORMAT";

    /**
     * Property indicating the Debug log level at which the message should be written out 
     * (optional - default is 0).
     */
    public static final String LOG_AT_LEVEL_PROP = "LOG_AT_LEVEL";


    /**
     * Constructor.
     */
    public ErrorDebugLogger ( )
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
        
        StringBuffer errors = new StringBuffer( );

        errorContextLocation = getPropertyValue( MessageProcessingDriver.ERROR_CONTEXT_LOC_PROP );
        
        // Use default if no configured value was given.
        if ( !StringUtils.hasValue( errorContextLocation ) )
            errorContextLocation = MessageProcessingDriver.DEFAULT_ERROR_CONTEXT_LOC;
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error-context location in the message-processor context [" 
                       + errorContextLocation + "]." );
        
        messageFormat = getRequiredPropertyValue( MESSAGE_FORMAT_PROP, errors );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Error message format [" + messageFormat + "]." );

        String temp = getPropertyValue( LOG_AT_LEVEL_PROP );

        if ( StringUtils.hasValue( temp ) )
        {
           try
           {
              logAsLevel = StringUtils.getInteger( temp );
           }
           catch ( Exception e )
           {
              errors.append( e.getMessage() );
           }
        }
        else
            logAsLevel = Debug.ALL_ERRORS;

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Log message at debug level [" + logAsLevel + "]." );

        if ( errors.length() > 0 )
        {
            Debug.error( errors.toString() );

            throw new ProcessingException( errors.toString() );
        }

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
        
        // If the configured logging level isn't enable, do nothing.
        if ( !Debug.isLevelEnabled( logAsLevel ) )
            return( formatNVPair( input ) );

        try
        {
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
            
            Debug.log( Debug.MSG_DATA, "Available error-message tokens:" );
            
            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
            {
                for ( int Ix = 0;  Ix < keyComponents.length;  Ix ++ )
                    Debug.log( Debug.MSG_DATA, "[" + Ix + "] = [" + keyComponents[Ix] + "]." );
            }

            // Write out the constructed log message with the indicated level.
            Debug.log( logAsLevel, MessageFormat.format( messageFormat, keyComponents ) );
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

       return( formatNVPair( input ) );
    }
    
    
    // UNIT-TEST Application entry point.
    public static void main ( String[] args )
    {
        if ( args.length < 5 )
        {
            System.err.println( "\n\nUSAGE: ErrorDebugLogger <db-name> <db-user> <db-password> <prop-key> <prop-type>" );

            System.exit( -1 );
        }

        Debug.enableAll( );
        Debug.showLevels( );
        Debug.configureFromProperties( );

        String dbName           = args[0];
        String dbUser           = args[1];
        String dbPassword       = args[2];
        String propKey          = args[3];
        String propType         = args[4];

        try
        {
            String cid = System.getProperty( CustomerContext.CUSTOMER_ID_PROP );

            if ( cid != null )
                CustomerContext.getInstance().setCustomerID( cid );

            DBInterface.initialize( dbName, dbUser, dbPassword );

            ErrorDebugLogger edl = new ErrorDebugLogger( );

	    edl.initialize( propKey, propType );

	    edl.toProcessorNames = new String[ 1 ];
	    edl.toProcessorNames[ 0 ] = "NEXT";
            
	    MessageObject mo = new MessageObject( null );

            MessageProcessorContext context = new MessageProcessorContext( );

	    LinkedList ll = new LinkedList( );

	    ll.addFirst( new MessageProcessingDriver.ErrorContext( edl, mo, new ProcessingException( "An error occurred." ) ) );

            context.set( MessageProcessingDriver.DEFAULT_ERROR_CONTEXT_LOC, ll );

            edl.process( context, mo );

            DBInterface.closeConnection( );
        }
        catch ( Exception e )
        {
            System.err.println( e );

            e.printStackTrace( );
        }
    }


    private String messageFormat;

    private int logAsLevel;

    // The location in the context where the list of ErrorContext objects can be found.
    private String errorContextLocation = null;

    // Abbreviated class name for use in logging.
    private String className;
}

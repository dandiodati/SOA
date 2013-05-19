/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header:  $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.io.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;


/**
 * A message processor that can be used to test gateway configurations
 * with respect to normal and exception execution.
 */
public class TestMessageProcessor extends MessageProcessorBase
{
    /**
     * Property giving the name of the file containing any configuration properties.
     */
	public static final String CONFIG_PROP_FILE_NAME_PROP = "CONFIG_PROP_FILE_NAME";
    
    /**
     * Property giving the name of the file containing any response to return for 
     * a non-null input.  If not given, the default is to echo the input.
     */
	public static final String RESULT_VALUE_FILE_NAME_PROP = "RESULT_VALUE_FILE_NAME";
    
    /**
     * Property giving the name of the file containing the contents of the 
     * message exception to throw.  If not given, the default is to not throw the exception.
     */
    public static final String MESSAGE_EXCEPTION_FILE_NAME_PROP = "MESSAGE_EXCEPTION_FILE_NAME";
    
    /**
     * Property giving the name of the file containing the contents of the 
     * processing exception to throw.  If not given, the default is to not throw the exception.
     */
    public static final String PROCESSING_EXCEPTION_FILE_NAME_PROP = "PROCESSING_EXCEPTION_FILE_NAME";
    
    
    /**
     * Constructor.
     */
    public TestMessageProcessor ( )
    {
        Debug.log( Debug.UNIT_TEST, "Creating test message-processor." );
    }
    
    
    /**
     * Called to initialize a message processor object.
     * 
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  MessageException  Thrown if initialization fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize( key, type );
        
        try
        {
            String configPropFile = getPropertyValue( CONFIG_PROP_FILE_NAME_PROP );
            
            if ( StringUtils.hasValue( configPropFile ) )
            {
                Debug.log( Debug.UNIT_TEST, "Loading configuration property file [" + configPropFile + "] ..." );

                FileInputStream fis = new FileInputStream( configPropFile );
                
                fileProps.load( fis );
                
                fis.close( );
                
                Debug.log( Debug.UNIT_TEST, "File-based configuration properties:\n" + fileProps );
            }
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }
    }
    
    
    /**
     * A test processor that just passes its input to output, or throws
     * exceptions (under the control of configuration).
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
        throws MessageException, ProcessingException 
    {
        if ( inputObject == null )
            return null;
        
        Debug.log( Debug.UNIT_TEST, "Executing test message-processor." );
        
        if ( Debug.isLevelEnabled( Debug.UNIT_TEST ) )
        {
            Debug.log( Debug.UNIT_TEST, "Test message-processor input:\n" 
                       + inputObject.getString() );
        }
        
        String exceptionFile = getProperty( MESSAGE_EXCEPTION_FILE_NAME_PROP );
        
        if ( StringUtils.hasValue( exceptionFile ) )
        {
            try
            {
                throw new MessageException( FileUtils.readFile( exceptionFile ) );
            }
            catch ( Exception e )
            {
                throw new MessageException( e );
            }
        }
        
        
        exceptionFile = getProperty( PROCESSING_EXCEPTION_FILE_NAME_PROP );
        
        if ( StringUtils.hasValue( exceptionFile ) )
        {
            try
            {
                throw new ProcessingException( FileUtils.readFile( exceptionFile ) );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( e );
            }
        }
        
        
        String resultFile = getProperty( RESULT_VALUE_FILE_NAME_PROP );
        
        if ( StringUtils.hasValue( resultFile ) )
        {
            try
            {
                String result = FileUtils.readFile( resultFile );

                if ( Debug.isLevelEnabled( Debug.UNIT_TEST ) )
                {
                    Debug.log( Debug.UNIT_TEST, "Test message-processor response:\n" 
                               + result );
                }

                return( formatNVPair( result ) );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( e );
            }
        }
        else
        {
            Debug.log( Debug.MSG_STATUS, "Returning test message-processor's input to driver." );

            return( formatNVPair( inputObject ) );
        }
    }


    /**
     * Return the value for the named property from the file-based
     * or persistent properties (in that order).
     *
     * @param propName The property whose value is to be returned
     *
     * @return Value of named property, or null if not available.
     */
    protected String getProperty ( String propName )
    {
        String value = (String)fileProps.get( propName );
        
        if ( StringUtils.hasValue( value ) )
            return value;
        
        return( getPropertyValue( propName ) );
    }
    
    
    private Properties fileProps = new Properties( );
}

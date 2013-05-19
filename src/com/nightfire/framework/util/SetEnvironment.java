/*
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;

import java.util.*;
import java.io.*;


/**
 * Utility to configure system properties from other properties or flat-file.
 *
 */
public class SetEnvironment
{
    /**
     * Load properties into execution environment from a flat-file.
     *
     * @param  props  Properties to load into execution environment.
     * @param  log  If 'true', value of property named "LOG" is obtained and 
     *              system output and error logs are redirected to it.
     *
     * @exception  FrameworkException  Thrown on error.
     */
    public static void setSystemProperties ( Properties props, boolean log ) throws FrameworkException
    {
        try 
        {
            Debug.log( null, Debug.IO_STATUS, "Adding system properties to properties." );
            
            Properties systemProps = System.getProperties();
            
            Enumeration keys = systemProps.keys(); 
            
            while ( keys.hasMoreElements() ) 
            {
                String key = (String)keys.nextElement();
                
                if(props.containsKey(key) == false)
                {
                    props.put( key, systemProps.get(key) );
                }
            }
            
            System.setProperties( props );
            
            if ( log )
            {
                String logFileName = System.getProperty( "LOG" );
                
                if (logFileName != null)
                {
                    // Don't redirect if indicated log file is console.
                    if ( !logFileName.equalsIgnoreCase( "console" ) )
                    {
                        Debug.log( null, Debug.IO_STATUS, "Redirecting log messages to file [" + logFileName + "]." );
                        
                        PrintStream logFile = new PrintStream( new FileOutputStream(logFileName), true );
                        
                        System.setErr(logFile);
                        System.setOut(logFile);
                    }
                }
            }
            
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            {
                StringWriter sw = new StringWriter( );
                PrintWriter  pw = new PrintWriter( sw );

                System.getProperties().list( pw );

                Debug.log( null, Debug.SYSTEM_CONFIG, 
                           "------------------------- BEGIN SYSTEM PROPERTIES ----------------------------" );
                Debug.log( null, Debug.SYSTEM_CONFIG, sw.toString() );
                Debug.log( null, Debug.SYSTEM_CONFIG, 
                           "-------------------------- END SYSTEM PROPERTIES -----------------------------" );
            }
        }
        catch(Exception e) 
        {
            String errMsg = "ERROR: Failure during configuration from properties:\n" + e.toString();

            Debug.log( null, Debug.ALL_ERRORS, errMsg );

            throw new FrameworkException( errMsg );
        }
    }
    
    
    /**
     * Load properties into execution environment from a flat-file.
     *
     * @param  propertyFileName  Name of file containing properties to load.
     * @param  log  If 'true', value of property named "LOG" is obtained and 
     *              system output and error logs are redirected to it.
     *
     * @exception  FrameworkException  Thrown on error.
     */
    public static void setSystemProperties ( String propertyFileName, boolean log ) throws FrameworkException
    {
        try 
        {
            Debug.log( null, Debug.IO_STATUS, "Loading properties file [" + propertyFileName + "] ..." );
            
            Properties props = new Properties( );
            FileInputStream fis = new FileInputStream( propertyFileName );
            props.load( fis );
            fis.close( );
            
            Debug.log( null, Debug.IO_STATUS, "Successfully loaded properties file [" + propertyFileName + "]." );
            
            setSystemProperties( props, log );
        }
        catch( Exception e ) 
        {
            String errMsg = "ERROR: Could not load configuration properties files:\n" + e.toString();

            Debug.log( null, Debug.ALL_ERRORS, errMsg );

            throw new FrameworkException( errMsg );
        }
    }
    
    
    // Should never have to create this object.
    private SetEnvironment( )
    {
        // NOT USED !!!
    }
}







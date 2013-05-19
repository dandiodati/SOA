
/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/util/Debug.java#1 $
 */

package com.nightfire.framework.util;


import org.apache.log4j.Level;

import java.util.Map;
import java.util.Hashtable;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.util.Date;
import java.util.HashMap;
import java.io.File;
import java.io.Writer;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;

import com.nightfire.framework.debug.DebugLevel;


/**
 * The Debug class provides general-purpose diagnostic logging facilities.
 */
public final class Debug
{
    /**
     * Name of property indicating which logging levels to enable.
     */
    public static final String DEBUG_LOG_LEVELS_PROP = "DEBUG_LOG_LEVELS";

    /**
     * Name of property indicating the name of the log file.
     */
    public static final String LOG_FILE_NAME_PROP = "LOG_FILE";

    /**
     * Name of property indicating the maximum number of entries to write to a log file.
     */
    public static final String MAX_DEBUG_WRITES_PROP = "MAX_DEBUG_WRITES";

    /**
     * Name of property indicating the maximum number of log files to create before
     * previously-created ones are overwritten in a circular buffer fashion.
     */
    public static final String MAX_LOG_FILE_COUNT_PROP = "MAX_LOG_FILE_COUNT";

    /**
     * Name of property indicating the Java class providing process information.
     */
    public static final String PROCESS_INFO_CLASS_NAME_PROP = "PROCESS_INFO_CLASS_NAME";

    /**
     * Name of property indicating the time (in seconds) between configuration checks.
     */
    public static final String DEBUG_CONFIG_CHECK_TIME_PROP = "DEBUG_CONFIG_CHECK_TIME";


    /**
     *  Property for base locations where the logs are going to be placed for the corresponding Instance.
     */
    public static final String BASE_LOG_DIRECTORY_PROP = "BASE_LOG_DIRECTORY";

    /**
     * Maximum number of logging levels supported (0 - MAX_LEVELS-1).
     */
    public static final int MAX_LEVELS = 300;

    // Prefix text that all error and warning log messages should begin with.
    public static final String ERROR_MSG_PREFIX   = "ERROR: ";
    public static final String WARNING_MSG_PREFIX = "WARNING: ";


    // ------- Built-in Framework logging-levels. -------

    public static final int ALL_ERRORS             = 0;
    public static final int ALL_WARNINGS           = 1;
    public static final int NORMAL_STATUS          = 2;
    public static final int EXCEPTION_CREATION     = 3;
    public static final int EXCEPTION_STACK_TRACE  = 4;
    public static final int MEM_USAGE              = 5;
    public static final int SYSTEM_CONFIG          = 6;
    public static final int OBJECT_LIFECYCLE       = 7;
    public static final int UNIT_TEST              = 8;
    public static final int ASSERT                 = 9;

    public static final int BENCHMARK = 10;

    public static final int THREAD_BASE      = 20;
    public static final int THREAD_ERROR     = ALL_ERRORS;
    public static final int THREAD_WARNING   = ALL_WARNINGS;
    public static final int THREAD_LIFECYCLE = THREAD_BASE + 1;
    public static final int THREAD_STATUS    = THREAD_BASE + 2;

    public static final int IO_BASE       = 30;
    public static final int IO_ERROR      = ALL_ERRORS;
    public static final int IO_WARNING    = ALL_WARNINGS;
    public static final int IO_STATUS     = IO_BASE + 1;
    public static final int IO_DATA       = IO_BASE + 2;
    public static final int IO_PERF_TIMER = IO_BASE + 3;

    public static final int DB_BASE    = 40;
    public static final int DB_ERROR   = ALL_ERRORS;
    public static final int DB_WARNING = ALL_WARNINGS;
    public static final int DB_STATUS  = DB_BASE + 1;
    public static final int DB_DATA    = DB_BASE + 2;

    public static final int MAPPING_BASE      = 50;
    public static final int MAPPING_ERROR     = ALL_ERRORS;
    public static final int MAPPING_WARNING   = ALL_WARNINGS;
    public static final int MAPPING_LIFECYCLE = MAPPING_BASE + 1;
    public static final int MAPPING_STATUS    = MAPPING_BASE + 2;
    public static final int MAPPING_DATA      = MAPPING_BASE + 3;

    public static final int MSG_BASE      = 60;
    public static final int MSG_ERROR     = ALL_ERRORS;
    public static final int MSG_WARNING   = ALL_WARNINGS;
    public static final int MSG_LIFECYCLE = MSG_BASE + 1;
    public static final int MSG_STATUS    = MSG_BASE + 2;
    public static final int MSG_DATA      = MSG_BASE + 3;
    public static final int MSG_GENERATE  = MSG_BASE + 4;
    public static final int MSG_PARSE     = MSG_BASE + 5;

    public static final int XML_BASE      = 70;
    public static final int XML_ERROR     = ALL_ERRORS;
    public static final int XML_WARNING   = ALL_WARNINGS;
    public static final int XML_LIFECYCLE = XML_BASE + 1;
    public static final int XML_STATUS    = XML_BASE + 2;
    public static final int XML_DATA      = XML_BASE + 3;
    public static final int XML_GENERATE  = XML_BASE + 4;
    public static final int XML_PARSE     = XML_BASE + 5;
    public static final int XSL_LIFECYCLE = XML_BASE + 6;

    public static final int RULES_BASE      = 80;
    public static final int RULE_LIFECYCLE  = RULES_BASE + 1;
    public static final int RULE_EXECUTION  = RULES_BASE + 2;

    public static final int WORKFLOW_BASE      = 90;
    public static final int WORKFLOW_LIFECYCLE = WORKFLOW_BASE + 1;
    public static final int WORKFLOW_STATUS    = WORKFLOW_BASE + 2;
    public static final int WORKFLOW_EXECUTION = WORKFLOW_BASE + 3;
    public static final int WORKFLOW_DATA      = WORKFLOW_BASE + 4;

    public static final int STATE_BASE        = 100;
    public static final int STATE_ERROR       = ALL_ERRORS;
    public static final int STATE_WARNING     = ALL_WARNINGS;
    public static final int STATE_LIFECYCLE   = STATE_BASE + 1;
    public static final int STATE_STATUS      = STATE_BASE + 2;
    public static final int STATE_DATA        = STATE_BASE + 3;
    public static final int STATE_TRANSITION  = STATE_BASE + 4;

    public static final int SECURITY_BASE     	= 110;
    public static final int SECURITY_CONFIG    	= SECURITY_BASE+1;
    public static final int SECURITY_LIFECYCLE  = SECURITY_BASE+2;

    // --------------------------------------------------------------------------------


    /**
     * All non-framework logging level values should be above USER_BASE.
     */
    public static final int USER_BASE = 200;


    // Move these legacy levels outta here at appropriate point.
    public static final int EDI_BASE      = USER_BASE;
    public static final int EDI_ERROR     = MSG_ERROR;
    public static final int EDI_WARNING   = MSG_WARNING;
    public static final int EDI_LIFECYCLE = EDI_BASE + 1;
    public static final int EDI_STATUS    = EDI_BASE + 2;
    public static final int EDI_DATA      = EDI_BASE + 3;
    public static final int EDI_GENERATE  = EDI_BASE + 4;
    public static final int EDI_PARSE     = EDI_BASE + 5;

    // Metrics Db Logging levels
    public static final int ALL_METRICS_LOGGING = USER_BASE + 6;
    public static final int LOGIN_LOGOUT_METRICS_LOGGING = USER_BASE + 7;
    public static final int ORDER_METRICS_LOGGING = USER_BASE + 8;
    public static final int QUERY_METRICS_LOGGING = USER_BASE + 9;

    /**
     * Name of property file containing log level settings that will overide the
     * persistent property values, if present.
     */
    public static final String ALT_CONFIG_FILE_NAME = "debug.properties";


    /**
     * Convenience method for logging messages at level ALL_ERRORS.
     *
     * @param  msg  Message to log.
     */
    public static final void error ( String msg )
    {
        LogRedirector redirector = (LogRedirector)logRedirect.get();
        if ( redirector != null ) {
          redirector.log(ALL_ERRORS, msg);
          return;
        }

        Log4jWrapper.error(msg);
    }


    /**
     * Convenience method for logging messages at level ALL_WARNINGS.
     *
     * @param  msg  Message to log.
     */
    public static final void warning ( String msg )
    {
        LogRedirector redirector = (LogRedirector)logRedirect.get();
        if ( redirector != null ) {
          redirector.log(ALL_WARNINGS, msg);
          return;
        }

        Log4jWrapper.warn(msg);
    }


    /**
     * Log the message if the logging-level is enabled.
     *
     * @param  level  Logging-level at which output message will appear.
     * @param  msg    Message to log.
     */
    public static final void log ( int level, String msg )
    {
        LogRedirector redirector = (LogRedirector)logRedirect.get();
        if ( redirector != null ) {
          redirector.log(level, msg);
          return;
        }

        Level log4jLevel = DebugLevel.convertNFLevel(level);
        Log4jWrapper.log(log4jLevel,msg);
    }


    /**
     * Log the message if the logging-level is enabled.
     *
     * @param  obj    Object to use in check for per-class logging.
     * @param  level  Logging-level at which output message will appear.
     * @param  msg    Message to log.
     * @deprecated  use log(int level,String msg)
     */
    public static final void log ( Object obj, int level, String msg )
    {
        LogRedirector redirector = (LogRedirector)logRedirect.get();
        if ( redirector != null ) {
          redirector.log(level, msg);
          return;
        }

        log(level,msg);
    }


    /**
     * Execute the given assertion, printing its description and a stack trace upon failure
     * (followed by the immediate termination of the application).
     *
     * @param  assertion             Boolean expression being asserted as true.
     * @param  assertionDescription  Text description of assertion.
     */
    public static final void assertTrue ( boolean assertion, String assertionDescription )
    {
        if ( using )
        {
            if ( isLevelEnabled( Debug.ASSERT ) )
            {
                // If assertion fails ...
                if ( assertion == false )
                {
                    // Log message describing assertion with stack trace.
                    log( Debug.ASSERT, "\nASSERTION FAILED: \"" + assertionDescription +
                               "\"\n" + getStackTrace() + "\n" );

                    // Stop application execution.
                    System.exit( -1 );
                }
            }
        }
    }


    /**
     * Set the name of the log file that all messages associated
     * with this thread should be logged to.  For pooled threads,
     * this value should be set each time a thread is used.  When
     * the thread is done being used, it should call this method again
     * with a null value to reset the logging back to the default log.
     *
     * @param  fileName  Name of log file, or null if none.
     * @deprecated
     */
    public static final void setThreadLogFileName ( String fileName )
    {
        threadLocalData.set( fileName );
    }

  /**
   * Sets a log redirector for the current incomming thread. Set the redirector
   * class that all message associates with this thread should be redirected
   * too. When this class is set, logging will only go to the redirector
   * class and not to the file defined in LOG_FILE.
   *
   * @param r a <code>LogRedirector</code> instance
   */
  public static final void setThreadLogRedirector ( LogRedirector r )
    {
        logRedirect.set(r);
    }



    /**
     * Log all public String data members if the logging-level is enabled.
     *
     * @param  obj    Object to display members from (also used in check for per-class logging).
     * @param  level  Logging-level at which output message will appear.
     * @param  msg    Message to log in addition to public non-static string data members.
     * @deprecated
     */
    public static final void logPublicStringDataMembers ( Object obj, int level, String msg )
    {
        if ( using )
        {
            if ( obj == null )
                return;

            if ( (level < 0) || (level >= MAX_LEVELS) || (enabledLevels[level] == false) )
                return;

            if ( perClassLogging == true )
            {
                if ( !isLoggingEnabledForClass( obj ) )
                    return;
            }

            log( obj, level, msg );

            try
            {
                Field[] fields = obj.getClass().getFields( );

                for ( int Ix = 0;  Ix < fields.length;  Ix ++ )
                {
                    int mods = fields[Ix].getModifiers( );
                    Class c  = fields[Ix].getType( );

                    if ( Modifier.isPublic(mods) && String.class.equals(c) )
                    {
                        String val = (String)(fields[Ix].get(obj));

                        if ( val == null )
                            log( obj, level, "\t" + fields[Ix].getName() + " = NULL" );
                        else
                            log( obj, level, "\t" + fields[Ix].getName() + " = \"" +
                                 val + "\"" );
                    }
                }
            }
            catch ( Exception e )
            {
                System.err.println( "ERROR: in Debug.logPublicStringDataMembers():\n" + e.toString() );
            }
        }
    }


    /**
     * Enable the given logging-level.
     *
     * @param  level  Logging-level to enable.
     * @deprecated
     */
    public static final void enable ( int level )
    {
        if ( (level >= 0) && (level < MAX_LEVELS) )
            enabledLevels[level] = true;
        else
        {
            System.err.println( "ERROR: Invalid logging level [" + level + "]" );
        }
    }

    /**
     * Disable the given logging-level.
     *
     * @param  level  Logging-level to disable.
     * @deprecated
     */
    public static final void disable ( int level )
    {
        if ( (level >= 0) && (level < MAX_LEVELS) )
            enabledLevels[level] = false;
        else
        {
            System.err.println( "ERROR: Invalid logging level [" + level + "]" );
        }
    }


    /**
     * Enable logging for the class represented by given object.
     *
     * @param  obj  Object to enable class-level logging for.
     * @deprecated
     */
    public static final void enableForClass ( Object obj )
    {
        // Only allow once per class.
        if ( isLoggingEnabledForClass( obj ) )
            return;

        // Value isn't important, so use empty string.
        enabledClasses.put( obj.getClass().getName(), "" );
    }

    /**
     * Disable logging for the class represented by given object.
     *
     * @param  obj  Object to enable class-level logging for.
     * @deprecated
     */
    public static final void disableForClass ( Object obj )
    {
        // If it's not already enabled, we don't have to disable it.
        if ( !isLoggingEnabledForClass( obj ) )
            return;

        enabledClasses.remove( obj.getClass().getName() );
    }


    /**
     * Enable all logging-levels.
     */
    public static final void enableAll ( )
    {
       Log4jWrapper.setLevel(Level.ALL);
    }

    /**
     * Disable all logging-levels.
     */
    public static final void disableAll ( )
    {
        Log4jWrapper.setLevel(Level.OFF);
    }


    /**
     * Test to see if given logging-level is enabled.
     *
     * @param  level  Logging-level to test.
     *
     * @return  'true' if level is enabled, otherwise 'false'.
     */
    public static final boolean isLevelEnabled ( int level )
    {
        LogRedirector redirector = (LogRedirector)logRedirect.get();
        if ( redirector != null ) {
            return redirector.isLevelEnabled(level);
        }

        return Log4jWrapper.isEnabledFor(level);
    }

    /**
     * Test to see if per-class logging is enabled.
     *
     * @return  'true' if per-class is enabled, otherwise 'false'.
     * @deprecated
     */
    public static final boolean isPerClassLoggingEnabled ( )
    {
        return perClassLogging;
    }


    /**
     * Test to see if per-class logging is enabled for given class
     *
     * @param  obj  Object whose class should be used in test.
     *
     * @return  'true' if per-class is enabled, otherwise 'false'.
     * @deprecated
     */
    public static final boolean isLoggingEnabledForClass ( Object obj )
    {
        return( enabledClasses.containsKey( obj.getClass().getName() ) );
    }


    /**
     * Get logging-levels from system property named 'DEBUG_LOG_LEVELS',
     * whose value should be a whitespace-separated list of integers,
     * the word 'all', or the word 'none'.
     * This method is kept here for backward compatibility.
     */
    public static final void configureFromProperties ( )
    {
        try
        {
            configuredLogLevels = getProperty( null, DEBUG_LOG_LEVELS_PROP );

          //  if ( configuredLogLevels == null )
             //   return;

            Level levelToSet = DebugLevel.convertNFLevels(configuredLogLevels);
            System.out.println( "Setting logging levels to [" + levelToSet + "]");

            String maxWritesProp = getProperty( null, MAX_DEBUG_WRITES_PROP );

            if ( maxWritesProp != null )
            {
                maxNumWrites = Long.parseLong( maxWritesProp );

                System.out.println( "Maximum number of writes to a log file is set to [" + maxNumWrites + "]" );
            }

            String maxLogFileCountProp = getProperty( null, MAX_LOG_FILE_COUNT_PROP );

            if ( maxLogFileCountProp != null )
            {
                maxLogFileCount = Integer.parseInt( maxLogFileCountProp );

                System.out.println( "Maximum number of log files to create [" + maxLogFileCount + "]" );
            }

            String configCheckFrequencyProp = getProperty( null, DEBUG_CONFIG_CHECK_TIME_PROP );

            if ( configCheckFrequencyProp != null )
            {
                configCheckFrequency = Long.parseLong( configCheckFrequencyProp );

                // Prevent too frequent checks, which would impact performance.
                if ( configCheckFrequency < 10 )
                    configCheckFrequency = 10;

                System.out.println( "Configuration check frequency is set to every ["
                                    + configCheckFrequency + "] seconds." );
            }

        	// Check debug log files.
            defaultLogFileName = getProperty( null, LOG_FILE_NAME_PROP );

            // Place the default log file object in the per-thread cache.
            if ( defaultLogFileName == null )
                putLogFile( NFConstants.NF_DEFAULT_RESOURCE, new LogFileState( null, maxLogFileCount ) );
            else
            {
                putLogFile( defaultLogFileName, new LogFileState( defaultLogFileName, maxLogFileCount ) );

                System.out.println( "Default log file is set to [" + defaultLogFileName + "]" );
            }

            processInfoClassName = getProperty( null, PROCESS_INFO_CLASS_NAME_PROP );

            Log4jWrapper.configure(levelToSet,defaultLogFileName,
                        maxWritesProp,String.valueOf(maxLogFileCount));
            startConfigListener( );
        }
        catch ( SecurityException se )
        {
            System.err.println( "ERROR: in Debug.configureFromProperties():\n" + se.toString() );
        }
        catch ( NoSuchElementException nsee )
        {
            System.err.println( "ERROR: in Debug.configureFromProperties():\n" + nsee.toString() );
        }
        catch ( NumberFormatException nfe )
        {
            System.err.println( "ERROR: Non-numeric value in debug levels\n\t" + nfe.toString() );
        }
    }


    /**
     * Gets value of property named "LOG_FILE" and redirects system output and error logs to the named file;
     * Get logging-levels from system property named 'DEBUG_LOG_LEVELS',
     * whose value should be a whitespace-separated list of integers,
     * the word 'all', or the word 'none'.
     */
    public static final void configureFromPropertiesNew ( )
    {
    	configureFromProperties( System.getProperties() );
    }


    /**
     * Get logging-levels from system property named 'DEBUG_LOG_LEVELS',
     * whose value should be a whitespace-separated list of integers,
     * the word 'all', or the word 'none'.
     *
     */
    public static final void configureFromProperties ( Hashtable props )
    {
        try
        {
            // maps to log4j.rootLogger=ALL, R
        	// First reconfigure debug log levels.
            configuredLogLevels = getProperty( props, DEBUG_LOG_LEVELS_PROP );

            //if ( configuredLogLevels == null )
              //  return;

            if ( configuredLogLevels == null )
                configuredLogLevels = "1"; // WARN

            // Get the Level object corresponding to debug level set in persistent property
            Level levelToSet = DebugLevel.convertNFLevels(configuredLogLevels);
			System.out.println( "Setting logging levels to [" + levelToSet + "]");

            // place the level in system properties for use by JNI code
             setSystemProperty(DEBUG_LOG_LEVELS_PROP, configuredLogLevels);

            // ignore max write property
            String maxWritesProp = getProperty( props, MAX_DEBUG_WRITES_PROP );
            if(maxWritesProp!= null)
            {
                maxNumWrites = Long.parseLong( maxWritesProp );

                System.out.println( "Maximum number of writes to a log file is set to [" + maxNumWrites + "]" );

                // place the value in system properties for use by JNI code
                setSystemProperty(MAX_DEBUG_WRITES_PROP, maxWritesProp);
            }

            // maps to log4j.appender.R.MaxBackupIndex
            String maxLogFileCountProp = getProperty( props, MAX_LOG_FILE_COUNT_PROP );

            if ( maxLogFileCountProp != null )
            {
                maxLogFileCount = Integer.parseInt( maxLogFileCountProp );
                System.out.println( "Maximum number of log files to create [" + maxLogFileCount + "]" );

                // place the value in system properties for use by JNI code
                setSystemProperty(MAX_LOG_FILE_COUNT_PROP, maxLogFileCountProp);
            }

            String configCheckFrequencyProp = getProperty( props, DEBUG_CONFIG_CHECK_TIME_PROP );

            if(configCheckFrequencyProp!= null)
            {
                configCheckFrequency = Long.parseLong( configCheckFrequencyProp );

                // Prevent too frequent checks, which would impact performance.
                if ( configCheckFrequency < 10 )
                    configCheckFrequency = 10;

                // place the value in system properties for use by JNI code
                setSystemProperty(DEBUG_CONFIG_CHECK_TIME_PROP, configCheckFrequencyProp);
                System.out.println( "Configuration check time is set to [" + configCheckFrequency + "] seconds." );
            }


        	// Check debug log files.
            defaultLogFileName = getProperty( props, LOG_FILE_NAME_PROP );

            if(StringUtils.hasValue(defaultLogFileName))
            {
                System.out.println( "Default log file is set to [" + defaultLogFileName + "]" );
                    // place the value in system properties for use by JNI code
                setSystemProperty(LOG_FILE_NAME_PROP, defaultLogFileName);
            }
            else
                System.out.println( "No Default log file is specified, logs will be written to console");

            // maps to layout %C{1}; currently doesnt work since we are instantiating a single logger.
            processInfoClassName = getProperty( props, PROCESS_INFO_CLASS_NAME_PROP );

            Log4jWrapper.configure(levelToSet,defaultLogFileName,
                        maxWritesProp,String.valueOf(maxLogFileCount));

            startConfigListener( );
        }
        catch ( SecurityException se )
        {
            System.err.println( "ERROR: in Debug.configureFromProperties():\n" + se.toString() );
        }
        catch ( NoSuchElementException nsee )
        {
            System.err.println( "ERROR: in Debug.configureFromProperties():\n" + nsee.toString() );
        }
        catch ( NumberFormatException nfe )
        {
            System.err.println( "ERROR: Non-numeric value in debug configuration property\n\t" + nfe.toString() );
        }
    }


    /**
     * Set the log-levels from the configuration string.
     *
     * @param  levels  Logging levels as they appear in
     *                 the configuration properties.
     *
     */
    private static void configureLevels ( String levels )
    {
        if ( levels == null )
            return;

        if ( "all".equalsIgnoreCase( levels ) )
        {
            Log4jWrapper.setLevel(Level.ALL);
        }
        else if ( "none".equalsIgnoreCase( levels ) )
        {
            Log4jWrapper.setLevel(Level.OFF);
        }
        else
        {

            Level levelToSet = DebugLevel.convertNFLevels(levels);
            Log4jWrapper.setLevel(levelToSet);
        }

    }


    /**
     * Set the given system property value, or catch and log the failure to do
     * so if the SecurityManager disallows the setting of system properties.
     *
     * @param  key  The name of the system property.
     * @param  value  The value of the system property.
     */
    private static final void setSystemProperty ( String key, String value )
    {
        try
        {
            System.setProperty( key, value );
        }
        catch ( Exception e )
        {
            System.err.println( "System property [" + key
                                + "] could not be set to ["
                                + value + "]:\n" + e.toString() );
        }
    }


    /**
     * Show logging-level as a part of each message.
     * @deprecated
     */
    public static void showLevels ( )
    {
        showLevels = true;
    }

    /**
     * Don't show logging-level as a part of each message.
     * @deprecated
     */
    public static void hideLevels ( )
    {
        showLevels = false;
    }


    /**
     * Enable class name checking as logging criteria.
     * @deprecated
     */
    public static void enablePerClassLogging ( )
    {
        perClassLogging = true;
    }

    /**
     * Disable class name checking as logging criteria.
     * @deprecated
     */
    public static void disablePerClassLogging ( )
    {
        perClassLogging = false;
    }


    /**
     * Enable thread id logging.
     */
    public static void enableThreadLogging ( )
    {
        threadLoggingEnabled = true;
    }

    /**
     * Disable thread id logging.
     * @deprecated
     */
    public static void disableThreadLogging ( )
    {
        threadLoggingEnabled = false;
    }


    /**
     * Disable time-stamp printing in each log message.
     * @deprecated
     */
    public static void disableTimeStamping ( )
    {
        timeStampingEnabled = false;
    }


    /**
     * Disable customer-id printing in each log message.
     */
    public static void disableCustomerIdLogging ( )
    {
        //cidLoggingEnabled = false;
        Log4jWrapper.disableCidLogging();
    }


    /**
     * Get a stack trace in string form showing the stack calls
     * from the point of call.
     *
     * @return  String containing the stack trace.
     */
    public static final String getStackTrace ( )
    {
        Throwable t = new Throwable( );

        return getStackTrace(t);
    }


    /**
     * Get a stack trace in string form for a Throwable.
     *
     * @return  String containing the stack trace.
     */
    public static final String getStackTrace ( Throwable t )
    {
        StringWriter sw = new StringWriter( );

        PrintWriter pw = new PrintWriter( sw );

        t.printStackTrace( pw );

        return( sw.toString() );
    }


    /**
     * Log the stack trace for a Throwable object.
     *
     * @param  t  A Throwable object instance.
     */
    public static final void logStackTrace ( Throwable t )
    {
        LogRedirector redirector = (LogRedirector)logRedirect.get();
        if ( redirector != null ) {
          redirector.logStackTrace(EXCEPTION_STACK_TRACE, t);
          return;
        }

    	log( EXCEPTION_STACK_TRACE, getStackTrace(t) );
    }


    /**
     * Write any log messages, using either System.out or
     * a file-based writer.
     */
    private static final void write ( Writer logWriter, String msg ) throws IOException
    {
        if ( logWriter == null )
            System.out.print( msg );
        else
            logWriter.write( msg );
    }


    /**
     * Get the LogFileState object associated with this log.
     * NOTE: This method should only be called once per log entry.
     *
     * @return  The LogFileState object associated with the log file.
     * @deprecated
     */
    private static final synchronized LogFileState getLogFile ( )
    {
        // By default use the log not associated with any particular thread.
        String name = defaultLogFileName;

        // See if there is any thread-specific file name.
        Object obj = threadLocalData.get( );

        // If the thread-local object has a file name, use it.
        if ( (obj != null) && (obj instanceof String) )
            name = (String)obj;

        LogFileState log = null;

        // Get the appropriate file from the log file cache.
        if ( name == null )
            log = (LogFileState)logFiles.get( NFConstants.NF_DEFAULT_RESOURCE );
        else
            log = (LogFileState)logFiles.get( name );

        // If the log file hasn't already been created, create it now
        // and add it to the cache for subsequent re-use.
        if ( log == null )
        {
            log = new LogFileState( name, maxLogFileCount );

            if ( name == null )
                putLogFile( NFConstants.NF_DEFAULT_RESOURCE, log );
            else
                putLogFile( name, log );
        }

        return log;
    }


    /**
     * Add the log file to the cache of log files.
     *
     * @param  key  Name by which the log file is accessed.
     * @param  file  Object encapsulating log file state.
     * @deprecated
     */
    private static final void putLogFile ( String key, LogFileState file )
    {
        logFiles.put( key, file );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Currently-active log files:\n"
                       + logFiles.keySet().toString() );
    }


    /**
     * Get named property from the given property hash table (if non-null),
     * and if no value is found, attempt to get it from the system properties.
     *
     * @param  props  Optional hash table to search first for property.
     * @param  name  Name of property.
     *
     * @return  Property value, or null if not found.
     */
    private static final String getProperty ( Hashtable props, String name )
    {
        String value = null;

        if ( props != null )
            value = (String)props.get( name );

        if ( value == null )
            value = System.getProperty( name );

        return value;
    }


    /**
     * Get an operating system-specific description of the current
     * JVM process, suitable for associating an executing process
     * with an application and associated log file.
     *
     * @return  OS-specific description of the current process.
     */
    private static final String getProcessInfo ( )
    {
        try
        {
            if ( !StringUtils.hasValue( processInfoClassName ) )
                processInfoClassName = getProperty( null, PROCESS_INFO_CLASS_NAME_PROP );

            if ( StringUtils.hasValue( processInfoClassName ) && (processInfoObject == null) )
            {
                processInfoObject = (ProcessInfo)ObjectFactory.create( processInfoClassName,
                                                                       com.nightfire.framework.util.ProcessInfo.class );
            }

            if ( processInfoObject != null )
                return( processInfoObject.describe() );
        }
        catch ( Throwable t )
        {
            System.err.println( t );
        }

        return null;
    }


    /**
     * Start the background thread that checks for the 'debug.properties' file
     * that enables users to dynamically change the log levels.
     */
    private static void startConfigListener ( )
    {
        // Start the background config thread for dynamic log configuration.
        if ( configThread == null )
        {
            configThread = new Thread( new DynamicConfigListener( configCheckFrequency,
                                                                  configuredLogLevels ) );

            configThread.setDaemon( true );

            configThread.start( );
        }
    }


    /**
     * Stop the background thread that checks for the 'debug.properties' file
     * that enables users to dynamically change the log levels.
     */
    private static void stopConfigListener ( )
    {
        if ( configThread != null )
        {
            configThread.interrupt( );

            configThread = null;
        }
    }


    /**
     * Class implementing object that runs in background thread, checking for
     * the 'debug.properties' file and configuring the logging levels based
     * on its contents.
     */
    private static class DynamicConfigListener implements Runnable
    {
        /**
         * Create a configuration listener object.
         *
         * @param  configCheckFreq  The rate (in seconds) that the configuration
         *                           should be checked.
         * @param  previousLogLevels  The logging levels that were in effect before
         *                            any dynamic changes.
         */
        public DynamicConfigListener ( long configCheckFreq, String previousLogLevels )
        {
            this.configCheckFreq = configCheckFreq;

            this.previousLogLevels = previousLogLevels;
        }


        /**
         * Executes in background thread, periodically checking for the presence of a file named
         * 'debug.properties' and using its contents to dynamically configure the logging levels.
         */
        public void run ( )
        {
            try
            {
                // The config thread should be run at the lowest priority.
                Thread.currentThread().setPriority( Thread.MIN_PRIORITY );

                do
                {
                    // Sleep configured amount between checks.
                    Thread.sleep( configCheckFreq * 1000 );

                    // Skip cleanup if parent thread has indicated that config thread should exit.
                    if ( Thread.currentThread().isInterrupted() )
                        break;

                    // Check for 'debug.properties' file.
                    File f = new File( ALT_CONFIG_FILE_NAME );

                    if ( f.exists() && f.isFile() && f.canRead() )
                    {
                        // If the file has changed since the last check, re-read its contents.
                        long test = f.lastModified( );

                        if ( test != lastModified )
                        {
                            Properties props = new Properties( );

                            FileInputStream fis = null;

                            try
                            {
                                fis = new FileInputStream( ALT_CONFIG_FILE_NAME );

                                props.load( fis );
                            }
                            finally
                            {
                                try
                                {
                                    if ( fis != null )
                                        fis.close( );
                                }
                                catch ( Exception e )
                                {
                                    // Nothing sensible can be done here.
                                }
                            }

                            // First, check if there is an application-specific setting as indicated by a
                            // DEBUG_LOG_LEVELS property prefixed by the specific log file name.
                            String levels = props.getProperty( defaultLogFileName + "." + DEBUG_LOG_LEVELS_PROP );

                            // If an application-specific value isn't available, attempt to get a non-specific
                            // configuration value.
                            if ( !StringUtils.hasValue( levels ) )
                                levels = props.getProperty( DEBUG_LOG_LEVELS_PROP );

                            // If the file gives log-level configuration, use it to configure the system,
                            // remember the last time the file was changed (for next time) and remember
                            // that we did so.
                            if (StringUtils.hasValue(levels))
                            {
                                configureLevels( levels );

                                usingDefaultConfig = false;
                            }
                            else  // No DEBUG_LOG_LEVELS property was found.
                            {
                                // If the property isn't present, but we're still using the file's configuration,
                                // revert back to the persistent property configuration.
                                if (!usingDefaultConfig)
                                {
                                    //configureLevels( previousLogLevels );

                                    usingDefaultConfig = true;
                                }
                            }

                            // Remember the last time the file was modified for the next time through.
                            lastModified = test;
                        }
                    }
                    else  // No 'debug.properties' file was found.
                    {
                        // If the file isn't present, but we're still using the file's configuration,
                        // revert back to the persistent property configuration.
                        if ( !usingDefaultConfig)
                        {
                            //configureLevels( previousLogLevels );

                            usingDefaultConfig = true;
                        }
                    }
                }
                while ( true );
            }
            catch ( InterruptedException ie )
            {
                // This exception is expected during shutdown of this thread via an interrupt() call.
            }
            catch ( Exception e )
            {
                System.err.println( "Unexpected error in debug dynamic configuration thread:\n"
                                    + e.toString() );
            }
        }

        private long configCheckFreq = 300;

        String previousLogLevels = null;

        private long lastModified = 0;

        private boolean usingDefaultConfig = true;
    }


    /**
     * Unit testing ...
     */
    public static void main ( String[] args )
    {
        try
        {
            Properties props = new Properties();
            props.put( DEBUG_LOG_LEVELS_PROP, "INFO" );
            props.put( LOG_FILE_NAME_PROP, "mylogs.log" );
            props.put( MAX_DEBUG_WRITES_PROP, "10000" );
            props.put( PROCESS_INFO_CLASS_NAME_PROP, "true" );

            Debug.showLevels( );
            Debug.configureFromProperties(props);

            String test = new String();

            /*
             * Level 0-Error, 1-Warning, 2-Info, 
             */
            // System.out.println( "Test: Class-level logging disabled." );
            //Debug.showLevels();
            Debug.log( 0, "--Should display message at error level._1" );
            Debug.log( 1, "--Should display message at warning level._2" );
            Debug.log( 2, "--Should display message at 2 level._3" );
            
            
            Debug.log( XML_DATA, "Should XML_DATA display message at warning level._4" );
            Debug.log( Debug.MAPPING_STATUS, "Should'nt display Mapping status._5" );
            Debug.log(  Debug.XML_LIFECYCLE, "Shouldn't display XML lifecycle._6_1" );
            Debug.log(  Debug.DB_DATA, "Shouldn't display XML lifecycle._6_2" );
            Debug.log(  Debug.BENCHMARK, "Shouldn't display XML lifecycle._6_3" );
            Debug.log(  Debug.EDI_STATUS, "Shouldn't display XML lifecycle._6_4" );
            Debug.log(  Debug.DB_BASE, "Shouldn't display XML lifecycle._6_5" );
            Debug.log(  Debug.MSG_LIFECYCLE, "Shouldn't display XML lifecycle._6_6" );
            Debug.log(  Debug.MSG_PARSE, "Shouldn't display XML lifecycle._6_7" );
            Debug.log(  Debug.MSG_GENERATE, "Shouldn't display XML lifecycle._6_8" );
            Debug.log(  Debug.MSG_STATUS, "Shouldn't display XML lifecycle._6_9" );
            Debug.log(  Debug.MSG_ERROR, "Shouldn't display XML lifecycle._6_10" );
            Debug.log(  Debug.MSG_WARNING, "Shouldn't display XML lifecycle._6_11" );
            
            
            
            Debug.enablePerClassLogging();
            Debug.enableForClass( test );

            // System.out.println( "Test: Class-level logging enabled." );
            Debug.log( 11, "Message at level 11 Shouldn't display._7" );
            Debug.log( 2, "Should display at message level 2._8" );
            Debug.log( Debug.EXCEPTION_CREATION, "Should display at level error._9" );

            
            Debug.logPublicStringDataMembers( test, 1, "Public string data members._10" );

            Debug.disableForClass( test );

            //Debug.enableAll();
            //Debug.log(0, "ENABELING ALL");
            // System.out.println( "Test: Class-level logging disabled for this class." );
            Debug.log( 0, "Shouldn display at error level._11" );
            Debug.log( 1, "Should display._12" );
            Debug.log( 2, "Should display._13" );
            Debug.log( Debug.MAPPING_STATUS, "Should display Mapping status._13" );
            Debug.log( Debug.XML_LIFECYCLE, "Should display XML lifecycle._14" );
            
            //Debug.log( 0, ""+Debug.MSG_DATA);
            
            Debug.disableAll();
            
            Debug.log( 0, "Should'nt display ._15");
            Debug.warning("Should not display._16");
            Debug.error("Should not display._17");
            Debug.log(Debug.IO_ERROR, "Shouldn't display._18");
            Debug.log(Debug.MAPPING_STATUS, "Should display Mapping status._19");

        }
        catch ( Exception e )
        {
            String err = Debug.getStackTrace( e );

            try
            {
                Writer fw = new FileWriter( "DebugTestException.log", false );
                fw.write( err );
                fw.close( );
            }
            catch ( IOException ioe )
            {

            }

            e.printStackTrace();
        }
    }


    /**
     * Class encapsulating all state required by one log file.
     */
    private static class LogFileState
    {
        /**
         * Create an object encapsulating the log file's state.
         *
         * @param  fileName  Name of the log file.
         */
        public LogFileState ( String fileName, int maxLogFileCount )
        {
            logFileName = fileName;

            this.maxLogFileCount = maxLogFileCount;

            logWriter = null;

            curNumWrites = 0;

            curFileNum = 0;

            timestamp = new SimpleDateFormat( "HH:mm:ss.SSS" );
        }


        /**
         * Get the Writer object associated with this log.
         * NOTE: This method should only be called once per log entry,
         * and clients should lock against this object for the duration
         * of one write of a complete log message to the associated log file.
         *
         * @return  Writer associated with log file.
         * @deprecated
         */
        public final Writer getLogFile ( )
        {
            // If we have a file name, and it's not 'console' ...
            if ( (logFileName != null) && !"console".equalsIgnoreCase( logFileName ) )
            {
                // Check for file rotation need based on whether maximum-number-of-writes property was set or not.
                if ( maxNumWrites != NO_MAX_WRITES )
                {
                    // Bump the number-of-writes counter.
                    curNumWrites ++;

                    // If we've filled up the now active file ...
                    if ( curNumWrites >= maxNumWrites )
                    {
                        // Get the index of the current victim file to replace.
                        if ( curFileNum < maxLogFileCount )
                            curFileNum ++;
                        else
                            curFileNum = 1;

                        // Close active file before moving it to current file (if it exists).
                        if ( logWriter != null )
                        {
                            try
                            {
                                logWriter.close( );
                            }
                            catch ( IOException ioe )
                            {
                                System.err.println( "ERROR: Could not close log file:\n" + ioe.toString() );
                            }

                            // Null out file object so that a new one will be opened.
                            logWriter = null;
                        }

                        // If it exists, rename last active file to current file.
                        File replacement = new File( logFileName );

                        if ( replacement.exists() )
                        {
                            String victimFileName = logFileName + curFileNum;

                            // Delete the pre-existing current file before renaming the last active file to have the same name.
                            File victim = new File( victimFileName );

                            if ( victim.exists() )
                            {
                                try
                                {
                                    victim.delete( );
                                }
                                catch ( Exception e )
                                {
                                    System.err.println( "ERROR: Could not delete log file:\n" + e.toString() );
                                }
                            }

                            try
                            {
                                victim = new File( victimFileName );

                                replacement.renameTo( victim );
                            }
                            catch ( Exception e )
                            {
                                System.err.println( "ERROR: Could not rename log file:\n" + e.toString() );
                            }
                        }
                    }
                }

                // If file isn't opened, open it now.
                if ( logWriter == null )
                {
                    try
                    {
                        // Reset the number-of-writes counter since we're opening a new file.
                        // (We set it to 1 because the timestamp written below is the first log entry.)
                        curNumWrites = 1;

                        // Currently-active log file doesn't have current-file-number suffix in name.
                        logWriter = new BufferedWriter( new FileWriter( logFileName ) );

                        // Timestamp the newly-created log.
                        Date d = new Date( );

                        write( logWriter, "******* Log creation date [" + d.toString() + "]. *******\n" );

                        // If the application is configured to provide a ProcessInfo class, use it
                        // to get a description of the process and write it to the log file.
                        String pi = getProcessInfo( );

                        if ( StringUtils.hasValue( pi ) )
                            write( logWriter, pi + "\n" );
                    }
                    catch ( IOException ioe )
                    {
                        System.err.println( "ERROR: Could not redirect log messages:\n" + ioe.toString() );
                    }
                }
            }

            return logWriter;
        }

        // Used for timestamp formatting.
        public SimpleDateFormat timestamp;

        // Name of log file.
        private final String logFileName;

        // File to write data to.
        private Writer logWriter;

        // Current number of writes to this file.
        private long curNumWrites;

        // Current file number in circular buffer of files (i.e, 1->2->1 ...).
        private int curFileNum;

        // Default number of files is 2.
        private int maxLogFileCount = 2;
    }


    private static boolean [] enabledLevels = new boolean [ MAX_LEVELS ];

    private static Hashtable enabledClasses = new Hashtable( );

    private static boolean showLevels = false;
    private static boolean perClassLogging = false;
    private static boolean timeStampingEnabled = true;
    private static boolean threadLoggingEnabled = false;
    private static boolean cidLoggingEnabled = true;

    private static String defaultLogFileName;

    private static String configuredLogLevels;

    private static String processInfoClassName;

    private static ProcessInfo processInfoObject;

    private static final int NO_MAX_WRITES = -1;
    private static long maxNumWrites = NO_MAX_WRITES;

    // The maximum number of files to create before wrapping and overwriting an existing file
    // in a circular buffer fashion. (Ex: A value of 3 means 1->2->3->1->...)
    private static int maxLogFileCount = 2;

    // Default configuration check time is every 5 minutes.
    private static long configCheckFrequency = 300;

    // Per-thread log file names placed here.
    private static InheritableThreadLocal threadLocalData = new InheritableThreadLocal( );

    private static InheritableThreadLocal logRedirect = new InheritableThreadLocal( );

    // Container of LogFile objects, accessible by the file names placed
    // into the 'threadLocalData' object by client threads.
    private static Map logFiles = new HashMap( );

    private static Thread configThread;

    // Set this to 'false' to optimize out calls.
    private static final boolean using = true;


  /**
   * Defines a instance of a object which can all logging gets redirected to.
   * This allows a different set of logging API to be integrated with the
   * standard NF Debug logging class.
   *
   */
  public static interface LogRedirector
  {
    /**
     * The message and level to log.
     *
     * @param level The Debug.<level> to log at.
     * @param message The message to log.
     */
    public void log(int level, String message);

    /**
     * Test if a level is enabled.
     *
     * @param level The Debug.<level> to test.
     * @return a <code>boolean</code> value
     */
    public boolean isLevelEnabled(int level);

    /**
     * Log a stack trace to the log file.
     *
     * @param level The Debug.<level> to log at.
     * @param t The exception that got thrown.
     */
    public void logStackTrace(int level, Throwable t);

  }
}

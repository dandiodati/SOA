/**
 * Copyright 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.framework.util;

// jdk imports

// thirdparty imports
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

// nightfire imports
import com.nightfire.framework.util.Debug;

/**
 * DebugAppender is an Appender that writes to the NightFire debug log
 */
public class DebugAppender extends AppenderSkeleton
{
    /** The default NightFire log level */
    protected int defaultLevel = Debug.USER_BASE + 1;

    /**
     * Default constructor
     */
    public DebugAppender()
    {
    }

    /**
     * Constructor for a default log level
     *
     * @param defaultLevel The default log level to use when appending events
     */
    public DebugAppender(int defaultLevel)
    {
        this.defaultLevel = defaultLevel;
    }

    /**
     * Get the default log level
     */
    public int getDefaultLevel()
    {
        return defaultLevel;
    }

    /**
     * Set the default log level
     */
    public void setDefaultLevel(int defaultLevel)
    {
        this.defaultLevel = defaultLevel;
    }

    /**
     * Writes to the log
     *
     * @param event  The event to log
     */
    public void append(LoggingEvent event)
    {
        // how we log this depends on the logging level
        Level srcLevel = event.getLevel();
        int nfLevel = defaultLevel;
        
        if (srcLevel.equals(Level.ERROR))
            nfLevel = Debug.ALL_ERRORS;
        else if (srcLevel.equals(Level.FATAL))
            nfLevel = Debug.ALL_ERRORS;
        else if (srcLevel.equals(Level.WARN))
            nfLevel = Debug.ALL_WARNINGS;

        // log the event if that level is enabled
        if (Debug.isLevelEnabled(nfLevel))
            Debug.log(nfLevel, event.getRenderedMessage());
    }

    /**
     * Closes this appender.  This method does nothing, it is required by
     * the interface.
     */
    public void close()
    {
    }

    /**
     * This indicates to the Configurator that we do not want the logging
     * event laid out for us, we handle that through the use of our own
     * logging class.
     */
    public boolean requiresLayout()
    {
        return false;
    }
}

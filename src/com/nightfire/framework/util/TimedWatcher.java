/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/framework/util/TimedWatcher.java#1 $
 */

package com.nightfire.framework.util;


import java.util.*;
import java.io.*;
import java.text.*;


/**
 * Provides the ability to emit warning messages if an event doesn't happen
 * within a configurable amount of time.
 */
public class TimedWatcher implements Runnable
{
    /**
     * Default time in seconds to wait (5 minutes) if not specified.
     */
    public static final int DEFAULT_WATCH_TIME = 300;


    /**
     * Created a timed-watcher that will wait for the given
     * amount of time (in seconds) before emiting the given
     * warning message if not cancelled beforehand.
     * 
     * @param seconds  The amount of time to wait (in seconds) before sounding the alarm.
     * @param warningMessage  The message to emit to the log as a warning when
     *                        the time is exceeded.
     * @param interrupt  Flag indicating whether watcher should interrupt parent thread
     *                   when watch time is exceeded (true) or not (false).
     */
    public TimedWatcher ( int seconds, String warningMessage, boolean interrupt )
    {
        if ( seconds > 0 )
            watchTime = seconds;
        else
            watchTime = DEFAULT_WATCH_TIME;

        this.warningMessage = warningMessage;

        wantsInterrupt = interrupt;

        parentThread = Thread.currentThread( );

        // If caller wants parent thread to be cancelled, get a reference to the thread
        // that this object was constructed in.
        if ( wantsInterrupt == true )
        {
            if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
                Debug.log( Debug.THREAD_STATUS, "Timed-watcher is configured to interrupt parent thread [" 
                           + parentThread.toString() + "] if watch time is exceeded." );
        }

        if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
            Debug.log( Debug.THREAD_STATUS, "Creating timed-watcher that will wait for [" + watchTime
                       + "] seconds before displaying message [" + warningMessage + "] ..." );
    }


    /**
     * Created a timed-watcher that will wait for the default
     * amount of time (5 minutes) before emiting the given
     * warning message if not cancelled beforehand.
     * 
     * @param warningMessage  The message to emit to the log as a warning when
     *                        the time is exceeded.
     */
    public TimedWatcher ( String warningMessage )
    {
        this( DEFAULT_WATCH_TIME, warningMessage, false );
    }


    /**
     * Create a background thread that will wait for the given amount 
     * of time before emiting the warning message if not cancelled.
     */
    public void watch ( ) throws FrameworkException
    {
        watchThread = new Thread( this, Thread.currentThread().getName() + ":TimedWatcher" );

        watchThread.setDaemon( true );

        Debug.log( Debug.THREAD_STATUS, "Starting timed-watcher ..." );

        watchThread.start( );
    }


    /**
     * Cancel the background watcher thread.
     */
    public void cancel ( )
    {
        synchronized ( this )
        {
            Debug.log( Debug.THREAD_STATUS, "Cancelling the timed-watcher ..." );

            cancelled = true;

            notifyAll( );
        }
    }


    /**
     * Executes in the watcher's background thread, waiting for the 
     * configured time to expire and emit warnings or to be cancelled.
     */
    public void run ( )
    {
        Debug.log( Debug.THREAD_STATUS, "Started timed-watcher." );

        long start = System.currentTimeMillis( );

        long waitTime = watchTime;

        synchronized ( this )
        {
            while( !cancelled )
            {
                if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
                    Debug.log( Debug.THREAD_STATUS, "Timed-watcher now waiting for [" + waitTime  + "] seconds ..." );

                try
                {
                    wait( waitTime * 1000 );
                }
                catch ( Exception e )
                {
                    Debug.warning( "Timed-watch interrupted: " + e.toString() );
                }

                if ( cancelled )
                {
                    Debug.log( Debug.THREAD_STATUS, "Timed-watcher has been cancelled." );

                    break;
                }

                double delta = ((double)(System.currentTimeMillis() - start))/1000.0;

                Debug.warning( "Wait-time on parent thread [" + parentThread.toString() + "] of [" + delta 
                               + "] secs exceeds preset watch-time of [" + watchTime + "] secs: \"" + warningMessage + "\"" );

                if ( wantsInterrupt )
                {
                    Debug.warning( "Timed-watcher is now interrupting parent thread [" 
                                   + parentThread.toString() + "] ..." );

                    parentThread.interrupt( );
                }

                waitTime = 1;
            }
        }
    }


    // Unit-test.
    public static void main ( String[] args )
    {
        Debug.enableAll( );
        Debug.showLevels( );
        Debug.enableThreadLogging( );

        try
        {
            TimedWatcher tw1 = new TimedWatcher( 2, "test #1 message", false );

            tw1.watch( );

            Thread.currentThread().sleep( 7000 );

            tw1.cancel( );

            Thread.currentThread().sleep( 3000 );

            Debug.log( Debug.UNIT_TEST, "Test #1 done." );


            TimedWatcher tw2 = new TimedWatcher( 2, "test #2 message.", true );

            try
            {
                tw2.watch( );

                Thread.currentThread().sleep( 4000 );
            }
            finally
            {
                tw2.cancel( );
            }

            Debug.log( Debug.UNIT_TEST, "Test #2 done." );
        }
        catch ( Exception e )
        {
            System.err.println( e );

            e.printStackTrace( );
        }
    }


    private Thread watchThread;
    private Thread parentThread;
    private boolean wantsInterrupt = false;
    private int watchTime;
    private String warningMessage;
    private boolean cancelled = false;
}

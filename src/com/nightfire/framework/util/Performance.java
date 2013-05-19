/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;


/**
 * The Debug class provides general-purpose performance logging facilities.
 */
public final class Performance
{
    /**
     * Maximum number of timers supported (0 - MAX_TIMERS-1).
     */
    public static final int MAX_TIMERS = 100;
    
    
    /**
     * Start benchmark timer with given message as key.
     * @param  message  Message to print when done, and 
     *                  key used to retrieve time.
     */
    public static final void startBenchmarkLog ( String message )
    {
        if ( using )
            benchmarkHash.put( message, new Double(System.currentTimeMillis()) );
    }
    
    
    /**
     * Stop benchmark timer and print message with elapsed time.
     * @param  message  Message to print, and key used to
     *                  retrieve start time.
     */
    public static final void finishBenchmarkLog ( String message )
    {
        if ( using )
        {
            double end = System.currentTimeMillis( );
            
            Double startObj = (Double)benchmarkHash.get( message );
            
            if( startObj == null )
                Debug.log( Debug.ALL_WARNINGS, 
                           "WARNING: Benchmark call mismatch - finish call encountered with no matching start call." );
            else
            {
                benchmarkHash.remove( message );
                
                double start = startObj.doubleValue( );
                
                double diff = (end - start)/SECONDS_TO_MSEC;
                
                Debug.log( null, Debug.BENCHMARK, "BENCHMARK: " + message );
                Debug.log( null, Debug.BENCHMARK, "Elapsed time: [" +  diff + "] seconds."  );
            }
        }
    }
    

    /**
     * Start the timer if the logging-level is enabled.
     *
     * @param  level     Logging-level at which output message will appear.
     * @param  timerNum  Number of timer to start. 
     */
    public static final void startTimer ( int level, int timerNum )
    {
        if ( using )
        {
            if ( !Debug.isLevelEnabled( level ) )
                return;
            
            if ( (timerNum >= 0) && (timerNum < MAX_TIMERS) )
                timers[ timerNum ] = System.currentTimeMillis( );
        }
    }
    
    
    /**
     * Stop the timer and log the elapsed time and message if the logging-level is enabled.
     *
     * @param  level     Logging-level at which output message will appear.
     * @param  timerNum  Number of timer to start. 
     * @param  msg       Message to log.
     */
    public static final void stopTimer ( int level, int timerNum, String msg )
    {
        if ( using )
        {
            if ( (timerNum < 0) || (timerNum >= MAX_TIMERS) )
                return;
            
            long stop = System.currentTimeMillis( );
            
            // Convert to seconds.
            double elapsed = ((double)(stop - timers[timerNum]))/SECONDS_TO_MSEC;
            
            // Allow running timers.
            timers[ timerNum ] = stop;
            
            // Debug.log( null, level, msg + "\n\tELAPSED TIME: [" + elapsed + "] seconds." );
            Debug.log( level, "ELAPSED TIME [" + elapsed + "] sec:  " + msg );
        }
    }
    
    
    /**
     * Start the timer if the logging-level is enabled.
     *
     * @param  level  Logging-level at which output message will appear.
     *
     * @return  The start time.
     */
    public static final long startTiming ( int level )
    {
        if ( Debug.isLevelEnabled( level ) )
            return( System.currentTimeMillis() );
        else
            return 0;
    }
    
    
    /**
     * Stop the timer and log the elapsed time and message if the logging-level is enabled.
     *
     * @param  level      Logging-level at which output message will appear.
     * @param  startTime  Value returned by startTimer() call.
     * @param  msg        Message to log.
     */
    public static final void stopTiming ( int level, long startTime, String msg )
    {
        if ( Debug.isLevelEnabled( level ) )
        {            
            long stopTime = System.currentTimeMillis( );
            
            Debug.log( level, "ELAPSED TIME [" + (stopTime - startTime) + "] msec:  " + msg );
        }
    }


    /**
     * Log the memory-usage message if the logging-level is enabled.
     *
     * @param  level  Logging-level at which output message will appear.
     * @param  msg    Message to log.
     */
    public static final void logMemoryUsage ( int level, String msg )
    {
        if ( using )
        {
            Runtime r = Runtime.getRuntime( );
            
            long total = r.totalMemory( );
            long free  = r.freeMemory( );
            
            Debug.log( null, level, msg + "\n\tMEMORY: Total:[" + total + "], Free:[" + free + "]" );
        }
    }
    

    // Should never need to create Performance objects!
    private Performance ( )
    {
        // NOT USED!
    }



    private static double SECONDS_TO_MSEC = 1000.0;
    
    private static long [] timers = new long [ MAX_TIMERS ];
    
    private static Hashtable benchmarkHash = new Hashtable( );

    // Set this to 'false' to optimize out calls.
    private static final boolean using = true;
}

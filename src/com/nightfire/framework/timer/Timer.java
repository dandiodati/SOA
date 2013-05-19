/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.timer;


import java.util.*;
import java.awt.event.*;

import com.nightfire.framework.util.*;


/**
 * Class implementing a Timer which runs in a separate
 * thread.  NOTE: This should be replaced by the Swing Timer when it works with
 * our choosen JDK version!!!
 */
public class Timer implements Runnable 
{
    /**
     * Creates a Timer which wakes up every 'interval' seconds and
     * invokes actionPerformed() against any given ActionListener objects.
     *
     * @param  interval  Amount of time (in seconds) to wait between processing.
     */
    public Timer ( int interval ) 
    {
        Debug.log( this, Debug.THREAD_LIFECYCLE, "Creating Timer object." );

        setTimerInterval( interval );

        listeners = new Vector( );

        thread = new Thread( this );

        // Make this thread a daemon, so process will exit if it
        // is the only running thread in the VM.
        thread.setDaemon( true );

        thread.start( );
    }

    /**
     * Creates a Timer which wakes up every 'interval' seconds and
     * invokes actionPerformed() against given ActionListener object.
     *
     * @param  interval  Amount of time (in seconds) to wait between processing.
     * @param  listener  Listener to invoke at every timeout interval.
     */
    public Timer ( int interval, ActionListener listener ) 
    {
        this( interval );

        addListener( listener );
    }
    

    /**
     * Starts the thread and then enters loop where it sleeps for the 
     * specified interval, wakes up, invokes actionPerformed() 
     * on the ActionListener, and then repeats the process.
     */
    public void run ( ) 
    {
        Debug.log( this, Debug.THREAD_STATUS, "Starting timer." );

        // Forever loop ...
        while ( true ) 
        {
            try 
            { 
                // Don't fall back to sleep if user has indicated shutdown.
                if ( shutdownInProgress )
                {
                    Debug.log( Debug.THREAD_STATUS, "Shutdown has been called, so timer thread is exiting ..." );
                    
                    return;
                }

                Debug.log( this, Debug.THREAD_STATUS, "Timer going to sleep for [" + interval + "] seconds ..." );

                // Convert msec to seconds and sleep on it.
                thread.sleep( interval * 1000 ); 

                Debug.log( this, Debug.THREAD_STATUS, "Timer sleep interval expired at [" + 
                           DateUtils.getCurrentTime() + "]." );
            }
            catch ( InterruptedException e ) 
            {
                Debug.log( this, Debug.THREAD_WARNING, "WARNING: Timer sleep was interrupted." );

                if ( shutdownInProgress )
                {
                    Debug.log( Debug.THREAD_STATUS, "Shutdown has been called, so timer thread is exiting ..." );

                    return;
                }
                else
                {
                    // From the Sun-provided API documentation:
                    // For this technique to work, it's critical that any method that catches an interrupt exception and is not prepared to deal with it
                    // immediately reasserts the exception. We say reasserts rather than rethrows, because it is not always possible to rethrow the
                    // exception. If the method that catches the InterruptedException is not declared to throw this (checked) exception, then it should
                    // "reinterrupt itself" with the following incantation: 
                    //    Thread.currentThread().interrupt();
                    // This ensures that the Thread will reraise the InterruptedException as soon as it is able. 

                    Thread.currentThread().interrupt( );
                }
            }
            
            Debug.log( this, Debug.THREAD_STATUS, "Timer invoking actionPerformed() against listeners." );

            int len = listeners.size( );

            // Don't allow other threads to add or remove listeners while we're invoking them!
            synchronized( this )
            {
                // Loop over listeners, invoking each one in turn.
                for ( int Ix = 0;  Ix < len;  Ix ++ )
                {
                    Debug.log( this, Debug.THREAD_STATUS, "Invoking Timer listener number [" + (Ix + 1) + "] ..." );
                    
                    ActionListener al = (ActionListener)listeners.elementAt( Ix );

                    // Pass in this timer so that listener can correlate event with it.
                    ActionEvent ae = new ActionEvent( this, 0, null );
                    
                    al.actionPerformed( ae );
                }
            }

            Debug.log( this, Debug.THREAD_STATUS, "Done processing timeout against Timer." );
        }
    }


    /**
     *  Add a timer listener to the Timer's list of processors to invoke at expiration time.
     *
     * @param  listener  Listener to invoke at every timeout interval.
     */
    public synchronized void addListener ( ActionListener listener ) 
    {
        listeners.addElement( listener );

        Debug.log( this, Debug.THREAD_STATUS, "Adding listener to Timer.  Listener count is now [" + 
                   listeners.size() + "]." );
    }


    /**
     *  Remove a timer listener from the Timer's list of processors to invoke at expiration time.
     *
     * @param  listener  Listener to remove.
     */
    public synchronized void removeListener ( ActionListener listener ) 
    {
        if ( listeners.contains( listener ) )
        {
            listeners.removeElement( listener );

            Debug.log( this, Debug.THREAD_STATUS, "Removing listener from Timer.  Listener count is now [" + 
                       listeners.size() + "]." );
        }
        else
        {
            Debug.log( this, Debug.ALL_WARNINGS, 
                       "WARNING: Couldn't find listener to remove from Timer's handlers list." );
        }
    }


    /**
     *  Set the Timer's expiration interval (in seconds).
     *
     * @param  interval  Time between invocation of handlers.
     */
    public synchronized void setTimerInterval ( int seconds ) 
    {
        Debug.log( this, Debug.THREAD_STATUS, "Setting Timer sleep interval to [" + 
                   seconds + "] seconds." );

        interval = seconds;
    }


    /**
     * Kill the thread associated with this object.
     */
    public void shutDown ( )
    {
        if ( thread != null )
        {
            Debug.log( this, Debug.THREAD_LIFECYCLE, "Shutting down timer." );

            try
            {
                // Set flag indicating that we're in the process of shutting down.
                shutdownInProgress = true;

                // Break out of sleep(), if we're currently sleeping.
                thread.interrupt( );

                thread = null;
            }
            catch ( SecurityException se )
            {
                Debug.log( this, Debug.ALL_ERRORS, "ERROR: Could not kill timer thread:\n\t" + 
                           se.toString() );
            }
        }
    }
    

    /**
     * Used to allow replacement of Swing Timer in ASR Server.
     */
    public void stop ( )
    {
        // NO-OP
    }


    /**
     * Used to allow replacement of Swing Timer in ASR Server.
     */
    public void restart ( )
    {
        // NO-OP
    }


    /**
     * Get the thread associated with this timer.
     *
     * @return  Thread within which the timer is executing.
     */
    public Thread getThread ( )
    {
        return thread;
    }
    

    /**
     * Unit-test.
     */
    public static void main ( String[] args )
    {
        Debug.enableAll( );
        Debug.showLevels( );

        try
        {
            Timer timer = new Timer( 5 );

            Thread t = timer.getThread( );

            Thread.currentThread().sleep( 12 * 1000 );

            timer.shutDown( );

            Debug.log( Debug.UNIT_TEST, "Joining timer thread and waiting for it to exit ..." );

            t.join( );

            Debug.log( Debug.UNIT_TEST, "Timer thread has exited." );
        }
        catch ( Exception e )
        {
            System.err.println( e );

            e.printStackTrace( );
        }
    }


    private long    interval;
    private Vector  listeners;
    private Thread  thread;
    private boolean shutdownInProgress = false;
}

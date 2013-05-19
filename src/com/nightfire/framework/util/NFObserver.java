/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.util;

/**
 * Utility to execute an object (of type NFNotifier) in a separate
 * thread to prevent uncontrolled blocking of the current thread.
 * The Observer's executeNotifier() method will return when the
 * notifier thread finishes executing, or the timout expires - whichever
 * comes first.
 * 
 * @see NFNotifier
 */
public class NFObserver
{
    private NFNotifier notifier;
    private int timeout;
    private Object context;
    private boolean isNotified = false;
    private Thread notifierThread = null;


    /**
     * Constructor.
     *
     * @param notifier The notifier to execute.
     * @param timeout The maximum amount of time (in msec) to wait for
     *                the notifier to complete its execution.
     */
    public NFObserver ( NFNotifier notifier, int timeout )
    {
        this.notifier = notifier;
        this.timeout = timeout;
    }

    
    /**
     * Execute the notifier in its own thread.
     * When the Observer's maximum time to wait 
     * passes, do not interrupt the Notifier Thread.
     * This behavior is provided for backwards
     * compatibility. 
     */
    public void executeNotifier ( )
    {
        executeNotifier(false);
    }

    /**
     * Execute the notifier in its own thread.
     * When the Observer's maximum time to wait 
     * passes, interrupt the NotifierThread based
     * on the value of the given interruptNotifier 
     * flag.
     *
     * @param boolean Flag which specifies whether to 
     * interrupt the Notifier thread when the Observer's
     * maximum wait time is reached.
     * If true, interrupt the Notifier thread.
     */
    public void executeNotifier ( boolean interruptNotifier )
    {
        // Make the notifier aware of its parent observer, so it
        // can notify it.
        notifier.setObserver( this );

        // Start the notifier executing in its own thread.
        notifierThread = new Thread( notifier );

        // Adjust the thread name to be a composit of the parent thread name and the 
        // new thread name to simplify execution flow tracing in the logs.
        notifierThread.setName( Thread.currentThread().getName() + ":" + notifierThread.getName() );

        notifierThread.start();
        
        // Wait until the notifier finishes executing, or the timer expires.
        synchronized( this )
        {
            try
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log(Debug.MSG_STATUS, "NF-Observer waiting for up to [" 
                              + (timeout/1000) +"] seconds to get notified." );

                int waitCount = 0;

                long startTime = System.currentTimeMillis( );

                // While we haven't been notified or have exceeded the time to wait ...
                while ( !isNotified() && ((System.currentTimeMillis() - startTime) < timeout) )
                {
                    waitCount ++;

                    this.wait( timeout );

                    Debug.log(Debug.MSG_STATUS, "NF-Observer just returned from wait." );
                }

                if ( Debug.isLevelEnabled( Debug.BENCHMARK ) )
                    Debug.log( Debug.BENCHMARK, "NF-Observer waited [" + waitCount + "] times for a total of [" 
                               + (System.currentTimeMillis() - startTime) + "] msec." );

                Debug.log(Debug.MSG_STATUS, "NF-Observer is done waiting." );
            }
            catch( InterruptedException e )
            {
                Debug.log(Debug.ALL_WARNINGS, "WARNING: NF-Observer Thread was interruped.  Reason:\n" 
                          + e.getMessage() );
            }
        }

        if(interruptNotifier)
        {
            notifierThread.interrupt();
        }
    }


    /**
     * Set an arbitrary 'context' object on the observer.  Typically used by
     * the notifier to indicate things to the notifier.
     *
     * @param  context  A data object 
     */
    public void setContext ( Object context )
    {
        this.context = context;
    }


    /**
     * Get the 'context' object from the observer.
     *
     * @return  Observer's internal 'context' object.
     */
    public Object getContext ( )
    {
        return context;
    }
    

    /**
     * Update the notification flag and wake-up the observer.
     */
    public void notifyObserver ( )
    {
        Debug.log(Debug.MSG_STATUS, "NFObserver notifyObserverCalled()." );

        // Only notfiy of the Notifier Thread was not interrupted.
        if(!notifierThread.isInterrupted())
        {
            Debug.log(Debug.MSG_STATUS, "NFObserver notified by NFNotifier." );

            synchronized ( this )
            {
                isNotified = true;

                this.notify();
            }
        }
        else
        {
            Debug.log(Debug.MSG_STATUS, "Notifier Interrupted before notifying." );
            Debug.log(Debug.MSG_STATUS, "Ignoring Notify." );
        }
    }


    /**
     * Checks whether the observer has already been notified
     *
     * @return  'true' if observer was notified before timeout, otherwise 'false'.
     */
    public boolean isNotified ( )
    {
        return isNotified;
    }
}


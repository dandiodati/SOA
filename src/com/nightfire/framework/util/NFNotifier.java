/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.util;

/**
 * Utility class allowing user to encapsulate execution of
 * notifier in separate thread which is monitored by an observer.
 *
 * @see NFObserver.
 */
public abstract class NFNotifier implements Runnable
{
    protected NFObserver observer;
    

    /**
     * Non-public constructor, since instances of this class can't be instantiated.
     */
    protected NFNotifier ( )
    {
    }


    /**
     * Execute the notifier.
     * In all cases where the Notifier is interrupted
     * prior to completion, it must throw a RuntimeException
     * to indicate that. 
     *
     * @param NFObserver the observer
     */
    protected abstract void executeNotifier ( NFObserver observer );


    /**
     * Set the observer on the notifier.
     *
     * @param observer  The observer executing this notifier.
     */
    public void setObserver ( NFObserver observer )
    {
        this.observer = observer;
    }
    

    /**
     * Execute the notifier and then notify the observer.
     * In all cases during executeNotifier() where the 
     * Notifier is interrupted prior to completion, it 
     * must throw a RuntimeException to indicate that. 
     */
    public void run ( )
    {
        try
        {
            executeNotifier( observer );
            notifyObserver( );
        }
        catch(RuntimeException e)
        {
            Debug.warning("ExecuteNotifier aborted with a RuntimeException. " + 
                          "Assuming per contract that the Thread was interrupted." +
                          "\n" + e);
        }
    }


    /**
     * Notifies the observer
     */
    private void notifyObserver ( )
    {
        observer.notifyObserver( );
    }
}

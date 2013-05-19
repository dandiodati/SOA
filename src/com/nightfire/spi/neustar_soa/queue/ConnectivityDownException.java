package com.nightfire.spi.neustar_soa.queue;

import com.nightfire.mgrcore.queue.QueueException;

/**
 * This particular flavor of QueueException is used to indicate
 * that a message could not be delivered because connectivity is down.
 * This is a transient error that will be corrected when connectivity is
 * reestablished. Checking this error type will be used to keep the error
 * count from increasing on a queued message, so that it will not exceed
 * the maximum number of errors and get resent.
 */
public class ConnectivityDownException extends QueueException{

    /**
     * Create a queue exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public ConnectivityDownException(String msg)
    {
        super(msg);
    }


    /**
     * Create a queue exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public ConnectivityDownException(Exception e)
    {
        super(e);
    }

}

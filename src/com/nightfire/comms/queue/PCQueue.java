/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.queue;

import java.util.ArrayList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;


/**
 * PCQueue is an implementation of a producer/consumer queue.
 */
public class PCQueue
{
    /**
     * The queue
     */
    private ArrayList queue = new ArrayList();

    /**
     * The number of consumers currently blocked on the queue.
     */
    private int consumerCount = 0;

    /**
     * Indicates the queue is being shutdown.
     */
    private boolean shutdownFlag = false;

    /**
     * This places the an item on the queue.
     *
     * @param item  The item to place on the queue
     * @exception   ProcessingException Thrown if the queue is shutting
     *                                  down.
     */
    public void enqueue(Object item) throws ProcessingException
    {
        // acquire the queue monitor
        synchronized(queue)
        {
            // check the shutdown state
            if (shutdownFlag)
                throw new ProcessingException(
                    "Invalid queue state for enqueue operation.  Queue has shut down.");

            // add the item
            queue.add(item);

            // notify the next consumer
            queue.notify();
        }
    }

    
    /**
     * This method attempts to remove an item from the queue.  If no item is
     * available, this method blocks until an item is available or the queue
     * is shutdown.  This is equivalent to calling dequeue(0).
     *
     * @return An item retrieved from the queue or null if the queue was
     *         shutdown.
     */
    public Object dequeue()
    {
        return dequeue(0);
    }

    
    /**
     * This method attempts to remove an item from the queue.  If no item is
     * available, this method blocks until an item is available, the queue is
     * shutdown, or timeout number of milliseconds elapse.  If timeout is
     * zero, the method blocks until an item is available or the queue is
     * shutdown.
     *
     * @return An item retrieved from the queue or null if the queue was
     *         shutdown or timeout milliseconds elapsed.
     */
    public Object dequeue(long timeout)
    {
        // aquire the queue monitor
        synchronized (queue)
        {
            // see if we're shutting down
            if (shutdownFlag)
                return null;

            // see if there is anything available on the queue
            if (queue.size() > 0)
                return queue.remove(0);

            // nothing is available so we'll have to block
            
            // increment the number of waiting consumers
            incrementConsumers();

            // wait on the queue
            try
            {
                queue.wait(timeout);
            }
            catch (InterruptedException e)
            {
                // this should not normally happen
                Debug.log(Debug.ALL_WARNINGS,
                          "A thread was unexpectedly interrupted while blocking on PCValQueue");
            }

            // if we're shutting down or timed out, return immediately
            if ( (shutdownFlag) || (queue.size() <= 0) )
            {
                decrementConsumers();
                return null;
            }

            // remove the next available item from the queue
            Object o = queue.remove(0);

            // indicate one less consumer is blocking
            decrementConsumers();

            // return the removed item
            return o;
        }
    }

    
    /**
     * This causes the queue to release any threads blocking on dequeue
     * operations and disallow any more queue operations.
     */
    public void shutdown()
    {
        // set the shutdown flag
        synchronized(queue)
        {
            shutdownFlag = true;

            // wake up all the threads blocking on the queue
            queue.notifyAll();
        }

        // block until the last thread has released itself
        waitForLastConsumer();
    }

    
    /**
     * This returns the number of threads currently performing dequeue
     * operations.
     *
     * @return  A count of threads currently blocked for dequeue operations.
     */
    public int getConsumerCount()
    {
        // see if we're shutting down
        if (shutdownFlag)
            // wait until the shutdown is over
            waitForLastConsumer();

        // return the count of consumers
        return consumerCount;
    }
    
    
    /**
     * Blocks until no more consumers are waiting on the queue.
     */
    private synchronized void waitForLastConsumer()
    {
        // wait for the count to decrement
        while (consumerCount > 0)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                // just loop around again
            }
        }
    }
    

    /**
     * Increments the count of consumers waiting on the queue.  Note that
     * this method synchronizes on the PCQueue instance, which acts as the
     * lock for the consumer count.
     */
    private synchronized void incrementConsumers()
    {
        consumerCount++;
    }

    
    /**
     * Decrements the count of consumers waiting on the queue.  Note that
     * this method synchronizes on the PCQueue instance, which acts as the
     * lock for the consumer count.  This method also notifies any threads
     * waiting on this PCQueue instance when no consumers remain.
     */
    private synchronized void decrementConsumers()
    {
        // decrease the consumer count
        consumerCount--;

        // if no more consumers remain, let anyone waiting know
        if (consumerCount <= 0)
            notifyAll();
    }
}

/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.queue;

import java.util.*;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;
import com.nightfire.spi.common.communications.ComServerBase;


/**
 * The PCQueueManager inherits from ComServerBase to
 * allow producer/consumer queues to exist for the life of an SPI.  It
 * contains a collection of PCQueues which can be accessed by key.  For a
 * class within the SPI to use a queue, it must first call the static
 * getInst() method of PCQueueManager, which returns the shared class
 * instance.  Using this same instance is what allows all of the SPI to use
 * the same queues.
 */
public class PCQueueManager extends ComServerBase
{
    /**
     * Collection of PCQueues
     */
    private HashMap queues                 = new HashMap();

    /**
     * Used to determine when a queue should be released.
     */
    private HashMap queueCount             = new HashMap();

    /**
     * Used to indicate when the SPI is shutting down
     */
    private volatile boolean shutdownFlag  = false;

    /**
     * Shared PCQueueManager instance
     */
    private static PCQueueManager instance = null;

    
    /**
     * This is the constructor for the class.  It initializes its data which
     * is not dependent on database configuration.
     */
    public PCQueueManager(String key, String type) throws ProcessingException
    {
        super(key, type);
        Debug.log(Debug.NORMAL_STATUS, "Initializing the Queue Manager...");

        // Save this instance, but only if we don't
        // already have one
        if (instance == null)
            instance = this;
    }

    
    /**
     * This method returns a reference to the currently running instance of
     * PCQueueManager.  This allows all classes running in the same JVM to
     * use the same queue manager.
     *
     * @return The shared instance of PCQueueManger or null if it has not
     *         been initialized.
     */
    public static PCQueueManager getInst()
    {
        return instance;
    }

    
    /**
     * This places the object item on the queue identified by key.
     *
     * @param key   Identifies the queue to use
     * @param item  The item to place on the queue
     *
     * @exception   ProcessingException   Thrown if no consumers are waiting
     *                                    on the queue or if the queue is
     *                                    shutting down.
     */
    public void enqueue(Object key, Object item) throws ProcessingException
    {
        // acquire the lock for the queues
        synchronized(queues)
        {
            // check to see if we're shutting down
            if (shutdownFlag)
                // no insertions allowed after shutdown
                throw new ProcessingException(
                    "Invalid queue manager state for enqueue operation.  " +
                    "The queue manager has shut down.");

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS,
                          "PCQueueManager received an enqueue request for [" +
                          key + "].");

            // see if we have a queue for the key
            if (!queues.containsKey(key))
                // you can only add items to queues with waiting consumers
                throw new ProcessingException("No queue found for key [" +
                    key + "].  The queue must have a consumer to perform " +
                    "an enqueue operation.");

            // get the queue
            PCQueue queue = (PCQueue) queues.get(key);

            // enqueue the item
            queue.enqueue(item);
        }
    }

    
    /**
     * This method attempts to remove an item from the queue identified by
     * key.  If no item is available, this method blocks until an item is
     * available or the SPI is shutdown.  This is equivalent to calling
     * dequeue(key, 0).
     *
     * @param key   Identifies the queue to take the item from.
     *
     * @returns     An item from the queue, or null if none was available.
     */
    public Object dequeue(Object key)
    {
        // wraps dequeue(Object, long)
        return dequeue(key, 0);
    }

    
    /**
     * This method attempts to remove an item from the queue identified by
     * key.  If no item is available, this method blocks until an item is
     * available, the SPI is shutdown, or timeout number of milliseconds
     * elapse.  If timeout is zero, the method blocks until an item is
     * available or the SPI is shutdown.
     *
     * @param key     Identifies the queue to take the item from.
     * @param timeout The maximum number of milliseconds to wait.  0 indicates
     *                no maximum.
     *
     * @returns       An item from the queue, or null if none was available.
     */
    public Object dequeue(Object key, long timeout)
    {
        // get the key for the operation
        PCQueue queue = getForDequeue(key);

        // make sure we're not shutting down
        if (queue == null)
            return queue;

        // perform the dequeue operation
        Object result = queue.dequeue(timeout);

        // check to see if we need to clean up the queue
        cleanupForDequeue(key);

        return result;
    }

    
    /**
     * Locates or creates queues for use by dequeue.
     *
     * @param key   The key to associate with the queue.
     */
    private PCQueue getForDequeue(Object key)
    {
        PCQueue queue = null;

        // acquire the lock for the queues
        synchronized(queues)
        {
            // check to see if we're shutting down
            if (shutdownFlag)
                // we've shutdown, so just return null
                return queue;

            if (Debug.isLevelEnabled(Debug.MAPPING_BASE))
                Debug.log(Debug.MAPPING_BASE,
                          "PCQueueManager : Searching for [" + key +
                          "] for a dequeue operation.");

            // see if we have such a queue
            if (queues.containsKey(key))
            {
                if (Debug.isLevelEnabled(Debug.MAPPING_BASE))
                    Debug.log(Debug.MAPPING_BASE, "PCQueueManager : Found queue [" + key + "].");
                
                // use the existing queue
                queue = (PCQueue)queues.get(key);

                // update the count of consumers
                Integer count = (Integer) queueCount.get(key);
                queueCount.put(key, new Integer(count.intValue() + 1));
            }
            else
            {
                if (Debug.isLevelEnabled(Debug.MAPPING_BASE))
                    Debug.log(Debug.MAPPING_BASE, "PCQueueManager : Creating queue [" + key + "].");
                
                // create a new queue
                queue = new PCQueue();

                // add it to the collection
                queues.put(key, queue);

                // add a counter
                queueCount.put(key, new Integer(1));
            }
        }

        // return the queue
        return queue;
    }
    

    /**
     * Cleans up after dequeue has finished with a queue.
     *
     * @param key   The key associated with the queue.
     */
    private void cleanupForDequeue(Object key)
    {
        // acquire the queues lock
        synchronized (queues)
        {
            // get the consumer count for the queue
            Integer count = (Integer)queueCount.get(key);

            // see if we're the only consumer
            if (count.intValue() == 1)
            {
                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "Releasing queue [" + key + "].");
                
                // we're the last one, so clean up our resources
                queues.remove(key);
                queueCount.remove(key);
            }
            else
            {
                // decrease the number of consumers
                queueCount.put(key, new Integer(count.intValue() - 1));
            }
        }
    }

    
    /**
     * This is where processing begins when PCQueueManager is launched in its
     * own thread by the SPI.
     */
    public void run()
    {
        // just wait around until we're shut down
        while (!shutdownFlag)
        {
            try
            {
                synchronized(this)
                {
                    wait();
                }
            }
            catch(InterruptedException e)
            {
                // just loop around
            }
        }
    }
    

    /**
     * This indicates the running thread should shutdown.  This calls
     * shutdown on all queues in its collection and then returns.
     */
    public void shutdown()
    {
        // acquire the queues lock
        synchronized (queues)
        {
        	if(Debug.isLevelEnabled(Debug.MAPPING_BASE))
        		Debug.log(Debug.MAPPING_BASE,
                      "PCQueueManager is shutting down.");
            
            // set the shutdown flag
            shutdownFlag = true;

            // get an iterator for all of our queues
            Iterator iter = queues.entrySet().iterator();
            
            // walk through each entry
            while (iter.hasNext())
            {
                // get the value
                PCQueue queue = (PCQueue) ((Map.Entry) iter.next()).getValue();

                // shut it down
                queue.shutdown();
            }
        }

        // notify the main thread
        synchronized(this)
        {
            notifyAll();
        }
    }
}

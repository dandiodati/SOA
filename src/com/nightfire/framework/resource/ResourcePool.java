/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/resource/ResourcePool.java#1 $
 *
 */

package com.nightfire.framework.resource;

import java.util.Vector;
import java.util.Enumeration;

import com.nightfire.framework.util.*;
import com.nightfire.framework.environment.*;

/**
 * Manages a pool of resources so that they can be reused.
 *
 */
public abstract class ResourcePool implements Runnable
{
    private int initSize;
    private int maxSize;

    // Maximum time in millis to wait before acquireResource returns. Default to 60 seconds.
    private long maxWaitTime = DEFAULT_MAX_WAIT_TIME;

    private Vector acquiredResources;
    private Vector availableResources;

    private boolean beingDestroyed;

    // Last time that a resource was acquired.
    private long lastAccessTime;
    // Time without activity to wait before cleaning up unused resources.
    private long idleCleanupInterval = DEFAULT_MAX_RESOURCE_AGING_TIME;

    private Thread cleanupThread;


    /**
     * Constant to allow returning from acquireResource() without waiting.
     */
    public static final int NOT_SET = -1;

    /**
     * Constant defining the default maximum time (in minutes) to wait before
     * freeing-up available resources when there are no active client requests.
     */
    public static final long DEFAULT_MAX_RESOURCE_AGING_TIME = 60;

    /**
     * Constant defining the default maximum time (in seconds) to wait for a
     * database connection to become available before giving up.
     */
    public static final long DEFAULT_MAX_WAIT_TIME = 60;


    private static final long MSEC_PER_SEC = 1000;

    private static final long MSEC_PER_MINUTE = 60 * MSEC_PER_SEC;


    /**
     * Constructs a resource pool with the specified parameters.
     *
     * @param	maxSize		Maximum pool size.
     * @param	initSize	Initial pool size.
     *
     * @exception ResourceException Thrown if failed to construct pool.
     *
     */
    protected ResourcePool(int maxSize, int initSize) throws ResourceException
    {
        if (maxSize <= 0 || initSize < 0)
        {
            throw new ResourceException("ERROR: Cannot constructing pool with [" + maxSize + ", " + initSize
                                     + "]. The maximum size must be positive and initial size non-negative.");
        }

        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Constructing resource pool [" + maxSize + ", " + initSize + "]...");

        if (initSize > maxSize) {
            Debug.log(Debug.ALL_WARNINGS, "WARNING: RESOURCE_POOL: Initial pool size[" + initSize + "] is greater than maximum size [" + maxSize +
                            "], using maximum size instead.");

            initSize = maxSize;
        }
        this.maxSize = maxSize;
        this.initSize = initSize;

        availableResources = new Vector(maxSize);
        acquiredResources = new Vector(maxSize);

        for (int i=0; i<initSize; i++ )
        {
            Object aResource = createResource();
            availableResources.addElement(aResource);

        }

        beingDestroyed = false;

        lastAccessTime = System.currentTimeMillis( );

        // Start the background cleanup thread for connection pool aging.
        cleanupThread = new Thread( this );

        cleanupThread.setDaemon(true);

        cleanupThread.start( );

        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Constructed resource pool [" + this.maxSize + ", " + this.initSize + "].");
    }


    /**
     * Set the maximum number of resources available from the resource pool.
     *
     * @param  newMaxSize  New maximum value of resources.
     */
    public void setMaxPoolSize ( int newMaxSize )
    {
        if ( newMaxSize > 0 )
        {
            int oldValue = maxSize;

            // Set new Value.
            maxSize = newMaxSize;

            Debug.log( Debug.DB_STATUS, "Replaced maxSize value: [" + oldValue +
                            "] with the new value: [" + maxSize + "]" );
        }
        else {

            String errMsg = "Invalid newMaxSize value: [" + newMaxSize +
                            "] Keeping the current value: [" + maxSize + "]";
            Debug.error( errMsg );
        }
    }


    /**
     * Set the maximum amount of time to wait for resource availability
     * from the resource pool.
     *
     * @param  seconds  Maximum time, in seconds, to wait before giving up and throwing exception.
     */
    public void setMaxResourceWaitTime ( int seconds )
    {
        if ( seconds > 0 )
        {
            // Convert seconds to milliseconds.
            maxWaitTime = seconds * MSEC_PER_SEC;
        }
        else {
            maxWaitTime = DEFAULT_MAX_WAIT_TIME * MSEC_PER_SEC;
        }

        Debug.log( Debug.DB_STATUS, "Maximum time to wait for resource being set to [" +
                   seconds + "] seconds." );
    }


    /**
     * Set the time to wait before cleaning up available resources when
     * there is no activity against the resource pool. (resource aging)
     *
     * @param  minutes  Number of minutes without pool access activity
     *                  before cleanup should be performed.
     */
    public void setIdleCleanupTime ( int minutes )
    {
        if ( minutes > 0 )
        {
            idleCleanupInterval = minutes;

            Debug.log( Debug.DB_STATUS, "Time to wait before idle resource cleanup: [" +
                       idleCleanupInterval + "] minutes." );
        }
    }


    /**
     * Returns whether the resource is currently available or not.
     *
     * @return true if resource is available.
     */
    public boolean isResourceAvailable()
    {
        if (beingDestroyed)
        {
            Debug.log( Debug.DB_STATUS, "Resource is being destroyed, so it's not currently available." );

            return false;
        }
        return (acquiredResources.size() < maxSize);
    }

    /**
     * Returns whether the resource belongs to the pool or not.
     *
     * @return true if resource is available.
     */
    public boolean isPoolResource(Object resource)
    {
        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log( Debug.DB_STATUS, poolStatus() );

        return ( acquiredResources.contains(resource) ||
                 availableResources.contains(resource) );
    }

    /**
     * Acquires a resource from pool. Creates a new resource if neccesary. If max
     *
     * @return The acquired resource.
     *
     * @exception ResourceException Thrown if failed to grab one from pool.
     */
    protected Object acquireResource() throws ResourceException
    {
        Object aResource = null;

        if ( maxWaitTime == NOT_SET )
        {
            aResource = acquireResourceNoWait( );
        }
        else
        {
            aResource = acquireResource( maxWaitTime );
        }
        return aResource;
    }


    /**
     * Acquires a resource from pool. Creates a new resource if neccesary.
     * Do not wait for resource availability.
     *
     * @return The acquired resource.
     *
     * @exception ResourceException Thrown if failed to grab one from pool.
     */
    protected Object acquireResourceNoWait() throws ResourceException
    {
        Object aResource = getResourceInternal();

        if (aResource == null)
        {
            throw new ResourceException("ERROR: Cannot get resource from pool since pool is empty.");
        }

        return aResource;
    }


    /**
     * Acquires a resource from pool within the specified time period. Creates a new resource if neccesary.
     *
     * @param    timeout    The time in millis to wait until a non-null resource is acquired.
     *
     * @return The acquired resource.
     *
     * @exception ResourceException Thrown if failed to grab exception from pool.
     */
    protected synchronized Object acquireResource(long timeout) throws ResourceException
    {

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Trying to acquire resource, will wait up to [" + timeout + "] milliseconds...");

        if (timeout < 0)
        {
            throw new ResourceException("ERROR: Cannot get resource with a negative timeout [" + timeout + "].");
        }

        Object aResource = null;
        long enterTime = System.currentTimeMillis();

        while (aResource == null)
        {
            aResource = getResourceInternal();

            if (aResource == null)
            {
                // Got a null resource, wait for a resource to be returned.
                long timePassed = System.currentTimeMillis() - enterTime;
                if (timeout > 0 && timePassed >= timeout) {
                    //Debug.log(Debug.DB_STATUS, "WARNING: RESOURCE_POOL: Aquiring resource has timed out.");
                    //break;
                    throw new ResourceException("ERROR: Attempt to acquire resource has timed out by exceeding max-wait-time of ["
                                                + timeout + "] msec.  Current pool configuration: " + poolStatus() );
                }
                try
                {
                    // Waiting until timeout.
                    wait(timeout - timePassed);
                }
                catch (InterruptedException ie)
                {
                }
            }

        }

        return aResource;

    }


    /**
     * Returns the resource to pool.
     *
     * @param    aResource    The resource to put back in pool.
     *
     * @exception ResourceException Thrown if failed to put back the resource.
     */
    protected synchronized void releaseResource(Object aResource) throws ResourceException
    {
        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Returning resource [" + aResource + "]...");

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
           Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: " + this.poolStatus());

        if (beingDestroyed)
        {
            // Pool is being destroyed, do not return to pool.
            Debug.log(Debug.DB_STATUS, "WARNING: RESOURCE_POOL: Resource returned while pool being destroyed.");
        }
        else
        {
            if (!acquiredResources.contains(aResource))
            {
                // If this resource was not acquired from this pool, throw exception.
                throw new ResourceException("ERROR: The resource [" + aResource +
                            "] you are returning was not acquired from the pool.");
            }

            // Move back the resource from acquired pool to available pool.
            acquiredResources.removeElement(aResource);
            availableResources.addElement(aResource);
        }

        // Notifying all waiting threads.
        notifyAll();

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: " + this.poolStatus());

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Returned resource [" + aResource + "].");

    }


    /**
     * Destroys all resources in free pool and in used pool.
     *
     * @exception ResourceException Thrown if failed to destroy the pool.
     */
    protected synchronized void destroyAll() throws ResourceException
    {
        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Destroying the resource pool...");

        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: " + this.poolStatus());

        if (beingDestroyed)
        {
            return;
        }
        else
        {
            beingDestroyed = true;
            notifyAll();
        }

        // Tell the cleanup thread that we're done with it.
        cleanupThread.interrupt( );

        // Destroys all resource that is in free pool.
        while (availableResources.size() > 0 )
        {
            Object aResource = availableResources.firstElement();
            destroyResource(aResource);
            availableResources.removeElementAt(0);
        }

        // Destroys all resource that is being used also.
        while (acquiredResources.size() > 0 )
        {
            Object aResource = acquiredResources.firstElement();
            destroyResource(aResource);
            acquiredResources.removeElementAt(0);
        }

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: " + this.poolStatus());

        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Destroyed the resource pool.");

    }


    /**
     * Creates a new resource.
     *
     * @return The new created resource.
     *
     * @exception ResourceException Thrown if failed to create a resouce.
     */
    protected abstract Object createResource() throws ResourceException;


    /**
     * Validates the resource.
     *
     * @param    aResource    The resource to be validated.
     *
     * @return true if resource is valid.
     *
     * @exception ResourceException Thrown if failed to valid the resouce.
     */
    protected abstract boolean validateResource(Object aResource) throws ResourceException;


    /**
     * Destroys the resource.
     *
     * @param    aResource    The resource to be destroyed.
     *
     * @exception ResourceException Thrown if failed to destroy the resouce.
     */
    protected abstract void destroyResource(Object aResource) throws ResourceException;



    /**
     * Acquires a resource from pool. Creat a new resource if neccesary. Returns null in case
     * of pool is full.
     *
     * @return The acquired resource.
     *
     * @exception ResourceException Thrown if failed to grab one from pool.
     */
    private synchronized Object getResourceInternal() throws ResourceException
    {
        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Acquiring a resource...");

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: " + this.poolStatus());

        if (beingDestroyed)
        {
            //return null;
            throw new ResourceException("ERROR: Can't get resource since pool is being destroyed.");
        }

        Object aResource = null;

        // Repeated get from available pool until a good resource is return or nothing left in available pool.
        while (availableResources.size() > 0 && aResource == null)
        {
            // Get the first element from available pool and remove it from pool.
            aResource = availableResources.firstElement();
            availableResources.removeElementAt(0);

            if (aResource != null)
            {
                if (!validateResource(aResource))
                {
                    Debug.log(Debug.DB_STATUS, "WARNING: RESOURCE_POOL: Found an invalid resource, destroy it.");
                    destroyResource(aResource);
                    aResource = null;
                }
            }
            else
            {
                Debug.log(Debug.DB_STATUS, "WARNING: RESOURCE_POOL: Found a null resource, skip.");
            }
        }

        if (aResource != null)
        {
            // Got one from pool. Before return, add it to acquired pool.
            acquiredResources.addElement(aResource);
        }
        else
        {
            // None is available from pool.
            if (acquiredResources.size() >= maxSize)
            {
                // Pool is empty, return null. This is to let the timeout routine continue waiting.
                Debug.log(Debug.DB_STATUS, "WARNING: RESOURCE_POOL: Pool is empty, return null.");
            }
            else
            {
                // checkedOut < maxSize
                // Pool is not full, create a new one and return it.
                Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: No valid resource in pool, creating a new one...");
                aResource = createResource();

                // Before return, add it to acquired pool.
                acquiredResources.addElement(aResource);
            }
        }


        Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: " + this.poolStatus());

        if( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log(Debug.DB_STATUS, "RESOURCE_POOL: Resource [" + aResource + "] was acquired.");

        // Remember the last time we acquired a resource for aging.
        lastAccessTime = System.currentTimeMillis( );

        return aResource;
    }

    /**
     * Get the resource pool statistics in string form. For debug purpose.
     *
     * @return The curent size of pool and the number of resource being used.
     *
     */
    public String poolStatus()
    {
        return "Pool[init-size=" + initSize + ", max-size=" + maxSize + ", available="
               + availableResources.size() + ", acquired=" + acquiredResources.size()
               + ", max-wait-time=" + ((float)maxWaitTime)/1000.0 + " sec]";
    }

    /**
     * Take the decision whether we need to execute the clean up thread for resource pool.
     *
     * @return true if the time difference of current time and pool last access time is more then idle interval.
     *
     */

    public boolean isCleanUpRequired(long idleInterval )
    {
        // this method will be called only when its subclass has not overwrite this. hence this method contains the default behaviour
        // Calculate the time since a resource was last acquired.
        long now = System.currentTimeMillis( );
        long delta =  (now - lastAccessTime)/MSEC_PER_MINUTE;
        if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Time since last resource access for pool ["
                           + getClass().getName() + "]: [" + delta + "] minutes." );

        return (delta >= idleInterval);
    }


    /**
     * Execution entry point for internal thread that performs
     * resource pool aging and cleanup of unused resources.
     */
    public void run ( )
    {
        if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Starting resource pool cleanup thread, which will wait ["
                   + idleCleanupInterval + "] minutes between cleanup attempts." );

        try
        {
            // The cleanup thread should be run at the lowest priority.
            Thread.currentThread().setPriority( Thread.MIN_PRIORITY );

            do
            {
                Thread.sleep( idleCleanupInterval * MSEC_PER_MINUTE );

                // Skip cleanup if parent thread has indicated that cleanup thread should exit.
                if ( Thread.currentThread().isInterrupted() )
                    break;

                if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Resource pool cleanup thread just woke up." );


                boolean isCleanupReq = isCleanUpRequired(idleCleanupInterval);


                // If the value for the isCleanupReq comes out to be true then proceed with the cleanup thread
                if ( isCleanupReq )
                {
                    if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                        Debug.log( Debug.OBJECT_LIFECYCLE, "Before pool cleanup: " + poolStatus() );

                    // Lock the resource pool object and free-up all available resources.
                    // (NOTE: We don't touch any outstanding resources that are in use by clients!)
                    synchronized ( this )
                    {
                        // First, free-up all resources in the available container.
                        while ( availableResources.size() > 0 )
                        {
                            Object aResource = availableResources.firstElement();
                            destroyResource(aResource);
                            availableResources.removeElementAt(0);
                        }

                        Debug.log( Debug.OBJECT_LIFECYCLE, "After destroying available resources: " + poolStatus() );

                        // Now, initialize resources up to initial low water mark count, which at this
                        // point is the initial size minus the currently outstanding acquired resources.
                        int minCount = initSize - acquiredResources.size();

                        // If there are more acquired resources than the initial size,
                        // we'll do nothing.
                        if ( minCount < 0 )
                            minCount = 0;

                        while ( availableResources.size() < minCount )
                        {
                            Object aResource = createResource( );

                            availableResources.addElement( aResource );
                        }

                        if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                            Debug.log( Debug.OBJECT_LIFECYCLE, "After re-initializing: " + poolStatus() );
                    }

                    if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                        Debug.log( Debug.OBJECT_LIFECYCLE, "After pool cleanup: " + poolStatus() );
                }
            }
            while ( true );
        }
        catch ( InterruptedException ie )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Resource pool cleanup thread has been interrupted and is now exiting ..." );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Resource pool cleanup thread for ["
                       + getClass().getName() + "] encountered exception:\n" + e.toString() );
        }
    }
}

/**
 * Copyright (c) 1998 - 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header:$
 */

package com.nightfire.framework.util;

import java.util.*;

import com.nightfire.framework.cache.*;


/**
 * Abstract class implementing generalized factory/cache infrastructure.
 * The class implements the CachingObject interface so that its cache can
 * be cleared out if necessary.
 */
public abstract class FactoryCacheBase implements CachingObject
{
    /**
     * Hook that must be imlemented by concrete leaf class to create an
     * object of the requested type.
     *
     * @param  initializer  An optional object argument to be passed
     *                      to the constructor for the new object.
     *
     * @return  An instance of the requested object.
     *
     * @exception  FrameworkException  Thrown if object can't be created.
     */
    protected abstract Object createObject ( Object initializer ) throws FrameworkException;


    /**
     * Get an object instance from the factory - either by creating it anew
     * or by getting a previously-created one from the cache.
     *
     * @param   key  Name uniquely identifying requested resource type.
     *
     * @return  Object instance.
     *
     * @exception  FrameworkException  Thrown if creation fails.
     */
    public Object get ( String key ) throws FrameworkException
    {
        return( get( key, null ) );
    }


    /**
     * Get an object instance from the factory - either by creating it anew
     * or by getting a previously-created one from the cache.
     *
     * @param   key  Name uniquely identifying requested resource type.
     * @param  initializer  An optional object argument to be passed
     *                      to the constructor for the new object.
     *
     * @return  Object instance.
     *
     * @exception  FrameworkException  Thrown if creation fails.
     */
    public Object get ( String key, Object initializer ) throws FrameworkException
    {
        Object ret;

        if ( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Getting object [" + key + "] from factory/cache." );

        // If caching is disabled, always create a new object.
        if ( cache == null )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating a new object." );

            return ( createObject( initializer ) );
        }

        // Make sure only one thread can remove a cached item at a time.
        synchronized ( cache )
        {
            LinkedList list = (LinkedList)cache.get( key );

            // If a cache entry associated with the key was found, and
            // it contains a value ..
            if ( (list != null) && (list.size() > 0) )
            {
                if ( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) )
                {
                    Debug.log(Debug.OBJECT_LIFECYCLE, "Factory/cache has [" + list.size()
                               + "] items of type [" + key + "] before get()." );
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Factory/cache has [" + outstandingObjects.size()
                               + "] outstanding Objects being used before get()." );
                }

                // Remove the cached item from the list and return it to the caller.
                ret = list.removeFirst();
                outstandingObjects.put(ret, ret);
                return ret;
            }


            Debug.log( Debug.OBJECT_LIFECYCLE,
                       "No cached item was available, so creating a new one." );

            ret = createObject( initializer );

            outstandingObjects.put(ret, ret);
        }

        return ret;

    }

    /**
     * Return an object instance to the factory/cache.
     *
     * @param   key  Name uniquely identifying requested resource type.
     * @param   obj  Object to return to cache.
     */
    public void put ( String key, Object obj )
    {
        // If caching is disabled, or object is null, do nothing.
        if ( (cache == null) || (obj == null) )
            return;

        // Make sure only one thread can add a cached item at a time.
        synchronized ( cache )
        {
            List list = (List)cache.get( key );

            // If the cache doesn't already have an entry for this key ...
            if ( list == null )
            {
                // Create a new empty list to contain items of this type
                // and add it to the cache.
                list = new LinkedList( );

                cache.put( key, list );
            }

            if(outstandingObjects.containsKey(obj))
            {
                // Add the item passed-in to the cache list.
                list.add( obj );
                outstandingObjects.remove(obj);
            }

            if ( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) )
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Factory/cache has [" + list.size()
                           + "] items of type [" + key + "] after put()." );
                Debug.log( Debug.OBJECT_LIFECYCLE, "Factory/cache has [" + outstandingObjects.size()
                           + "] outstanding Objects being used by clients after put()." );
            }
        }
    }


    /**
     * A string describing the current cache contents for the factory.
     *
     * @return  String giving the key and count for each cache entry.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Factory-cache contents for [" );
        sb.append( getClass().getName() );
        sb.append( "]:\n" );

        if ( cache != null )
        {
            Iterator iter = cache.entrySet().iterator( );

            while ( iter.hasNext() )
            {
                Map.Entry entry = (Map.Entry)iter.next( );

                sb.append( '\t' );
                sb.append( "key [" );
                sb.append( (String)entry.getKey() );
                sb.append( "], count [" );
                sb.append( ((List)entry.getValue()).size() );
                sb.append( "]\n" );
            }
        }

        return( sb.toString() );
    }

    /**
     * Method invoked by the cache-flushing infrastructure
     * to indicate that the cache should be emptied.
     *
     * @exception FrameworkException if cache cannot be cleared.
     */
    public void flushCache ( ) throws FrameworkException
    {
        if ( cache != null )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Flushing factory cache ..." );

            // Make sure only one thread can access the cache at a time.
            synchronized ( cache )
            {
                cache.clear( );
                outstandingObjects.clear();
            }
        }
    }


    /**
     * Construct a factory/cache.
     */
    protected FactoryCacheBase ( )
    {
        // Enable caching by default.
        this( true );
    }


    /**
     * Construct a factory/cache.
     *
     * @param enableCaching  Flag indicating whether factory should cache components or not.
     */
    protected FactoryCacheBase ( boolean enableCaching )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating factory/cache of type ["
                   + getClass().getName() + "]. Caching? [" + enableCaching + "]." );

        if ( enableCaching )
        {
            cache = new HashMap( );

            //Register for clearing.
            CacheManager.getRegistrar().register( this );
        }
    }


    // Each map entry is a linked-list of cached objects of the single type
    // associated with the key value.
    private Map cache;

    private Map outstandingObjects = new HashMap();

}

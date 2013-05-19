/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;


/**
 * Class providing a container of name-value pairs.
 * 
 */
public class NVPairContainer
{
    /**
     * Create a name-value pair container object.
     */
    public NVPairContainer ( )
    {
        container = new Vector( );
    }


    /**
     * Get the internal container of NVPair objects, sorted 
     * by order of insertion (first to last).
     *
     * @return  The container (vector) of name-value pair objects.
     */
    public Vector getOrderedContainer ( )
    {
        return container;
    }


    /**
     * Get the internal container of NVPair objects as a hash
     * table with 'name' item as key.
     *
     * @return  The container (hashtable) of name-value pair objects.
     */
    public Hashtable getHashContainer ( )
    {
        Hashtable hash = new Hashtable( );

        for ( int Ix = 0;  Ix < container.size();  Ix ++ )
        {
            NVPair item = (NVPair)container.elementAt( Ix );

            hash.put( item.name, item.value );
        }

        return hash;
    }


    /**
     * Get the value associated with the given name.
     *
     * @param  name  The name from the name-value pair.
     *
     * @return  The associated value, or null if not found.
     */
    public final Object get ( String name )
    {
        // Loop over vector's contents, searching for named item.
        for ( int Ix = 0;  Ix < container.size();  Ix ++ )
        {
            NVPair item = (NVPair)container.elementAt( Ix );

            // Found it, so return value.
            if ( name.equals( item.name ) )
                return( item.value );
        }

        return null;   // Still here?  Didn't find named item.
    }


    /**
     * Add the given name-value pair to the collection.
     *
     * @param  name  The name from the name-value pair.
     * @param  name  The value from the name-value pair.
     *
     * @return  Any previous value associated with the name, or null if none.
     */
    public final Object put ( String name, Object value )
    {
        // See if named item already exists in vector.
        for ( int Ix = 0;  Ix < container.size();  Ix ++ )
        {
            NVPair item = (NVPair)container.elementAt( Ix );

            // Found item with same name, so replace it with one passed-in.
            if ( name.equals( item.name ) )
            {
                container.setElementAt( new NVPair( name, value ), Ix );

                // Return the previous value.
                return( item.value );
            }
        }

        // Named item wasn't found, so add it to end of vector.
        container.addElement( new NVPair( name, value ) );

        return null;
    }


    private Vector container;
}


package com.nightfire.framework.util;

import  java.util.HashMap;


/**
 * A utility data lookup class which acts similarly to a HashMap except that
 * object lookup and retrieval can be accomplished with 2 keys, in this case,
 * a key and a type.  Note that this implementation is not synchronized.  It
 * is the responsibility of the accessing threads to synchronize the usage.
 */
 
public class KeyTypeLookup
{
    private HashMap outterMap;
    
    
    /**
     * Constructor
     */
    public KeyTypeLookup()
    {
        outterMap = new HashMap();       
    }
    
    /**
     * Get the value based on the specified key and type.
     *
     * @param  key   The key to look up.
     * @param  type  The type to look up.
     *
     * @return  Corresponding value, null if no match is found.
     */
    public Object get(Object key, Object type)
    {
        HashMap innerMap = (HashMap)outterMap.get(key);
        
        if (innerMap == null)
        {
            return null;   
        }
        
        return innerMap.get(type);
    }
    
    /**
     * Add the value based on the specified key and type.
     *
     * @param  key    The key to use when adding.
     * @param  type   The type to use when adding.
     * @param  value  The value to add.
     *
     * @return  An existing value with the specified key and type, if any.
     */
    public Object put(Object key, Object type, Object value)
    {
        HashMap innerMap = (HashMap)outterMap.get(key);
        
        if (innerMap == null)
        {
            innerMap = new HashMap();
            
            outterMap.put(key, innerMap);
        }
        
        Object existingValue = innerMap.put(type, value);
        
        return existingValue;
    }
    
    /**
     * Clear out the whole data lookup.
     */
    public void clear()
    {
        outterMap.clear();   
    }
    
    /**
     * Clear out the data lookup associated with the specified key.
     *
     * @param  key  The key associated with the data lookup to clear.
     */
    public void clear(Object key)
    {
        HashMap innerMap = (HashMap)outterMap.get(key);
        
        if (innerMap != null)
        {
            innerMap.clear();
        }
    }
    
    /**
     * Remove the key and its associated data lookup.
     *
     * @param  key  The key associated with the data lookup to remove.
     */
    public void remove(Object key)
    {
        outterMap.remove(key);               
    }
    
    /**
     * Remove the type and its associated data lookup, for the specified key.
     *
     * @param  key   The key associated with the type to remove.
     * @param  type  The type to remove.
     *
     * @return  The existing value corresponding to the specified key and type.
     */
    public Object remove(Object key, Object type)
    {
        HashMap innerMap = (HashMap)outterMap.get(key);
        
        if (innerMap == null)
        {
            return null;
        }
        
        return innerMap.remove(type);
    }

    /**
     * Obtain the number of existing keys.
     *
     * @return  The number of existing keys.
     */
    public int size()
    {
        return outterMap.size();
    }
    
    /**
     * Obtain the number of existing types for the specified key.
     *
     * @param  key  The key to provide the count of its associated types.
     *
     * @return  The number of existing types for the specified key.
     */
    public int size(Object key)
    {
        HashMap innerMap = (HashMap)outterMap.get(key);
        
        if (innerMap == null)
        {
            return 0;   
        }
        
        return innerMap.size();
    }
}

package com.nightfire.framework.xrq.utils;


import java.util.*;


/**
 * A Hash map which hold duplicate keys and their values in a list.
 */
public class ChainedHashMap extends HashMap
{
    /**
     * Adds a value to the list.
     * values with duplicate keys are added to a list.
     */
    public List put(Object key, String value) {

          List temp = (List)get(key);
          if (temp == null)
             temp = new ArrayList();

          temp.add(value);
          put(key,temp);

       return temp;
    }

    /**
     * gets the first item from the List of objects located by the specified key.
     * @param key - The key used to locate the first matching value.
     */
    public String getFirst(Object key) {
       List temp = (List) get(key);
       if (temp == null)
          return null;

       return ((String)temp.get(0) );
    }


} 
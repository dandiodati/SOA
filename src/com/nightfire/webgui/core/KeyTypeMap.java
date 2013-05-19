/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;
  
import  java.io.*;
import  java.util.*;

    /**
     * A maps that uses two keys to hold objects. The first key is the grouping,
     * and the type is the key to that grouping.
     */
    public final class KeyTypeMap extends FixedSizeHashMap
    {

       /**
        * Constructs a new, empty map with the specified initial
        * capacity and the specified load factor.
        *
        * @param      initialCapacity   the initial capacity of the HashMap.
        * @param      loadFactor        the load factor of the HashMap
        * @throws     IllegalArgumentException  if the initial capacity is less
        *               than zero, or if the load factor is nonpositive.
        */
       public KeyTypeMap(int initialCapacity, float loadFactor) {
          super(initialCapacity,loadFactor);

       }

       /**
        * Constructs a new, empty map with the specified initial capacity
        * and default load factor, which is <tt>0.75</tt>.
        *
        * @param   initialCapacity   the initial capacity of the HashMap.
        * @throws    IllegalArgumentException if the initial capacity is less
        *              than zero.
       */
       public KeyTypeMap(int initialCapacity) {
	        super(initialCapacity);
       }

       /**
        *
        */
       public KeyTypeMap() {
	       super();
       }

       /**
        * Constructs a new map with the same mappings as the given map.  The
        * map is created with a capacity of twice the number of mappings in
        * the given map or 11 (whichever is greater), and a default load factor,
        * which is <tt>0.75</tt>.
        */
        public KeyTypeMap(Map t) {
          super(t);
       }


       public Object put(Object key, Object value )
       {
          throw new java.lang.UnsupportedOperationException("Method not supported");
       }


       /**
        * Returns a map of types and their values
        */
       public synchronized Object get(Object key)
       {
          return super.get(key);
       }


       public synchronized Object remove(Object key)
       {
          return super.remove(key);
       }


       public synchronized Object get(Object key, Object type)
       {

           Object map = super.get(key);

          if (map == null)
             return null;
          else
             return ((HashMap)map).get(type);


       }

       public synchronized Object remove(Object key, Object type)
       {
           Object map = super.get(key);
           Map keyMap;

          if (map == null)
             return null;
          else
             return ((HashMap)map).remove(type);


       }


       public synchronized Object put(Object key, Object type, Object value )
       {

          Object map = super.get(key);
          Map keyMap;

          if (map == null)
             keyMap = new HashMap();
          else
             keyMap = (HashMap) map;

          super.put(key, keyMap);

          return (keyMap.put(type, value) );
       }

        public synchronized String describe()
        {  
            return super.describe();
        }

       public static final void main(String[] args)
       {
          KeyTypeMap map = new KeyTypeMap();


          String item="item";

          map.put("key", "type", item);


          String got = (String)map.get("key", "type");

          if (item.equals(got) )
             System.out.println("found it");
          else
             System.out.println("not found");
             
       

       }

    }


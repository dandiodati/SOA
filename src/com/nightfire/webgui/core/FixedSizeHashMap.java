/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;
  
import  java.io.*;
import  java.util.*;  



    /**
     * Fixed size hash map which will only grow to a specific size
     * It stores all keys in a queue.
     * only supports a few operations, put, remove
     */
    public class FixedSizeHashMap extends HashMap
    {

        private int maxSize = -1;


        LinkedList queue = new LinkedList();



       /**
        * Constructs a new, empty map with the specified initial
        * capacity and the specified load factor.
        *
        * @param      initialCapacity   the initial capacity of the HashMap.
        * @param      loadFactor        the load factor of the HashMap
        * @throws     IllegalArgumentException  if the initial capacity is less
        *               than zero, or if the load factor is nonpositive.
        */
       public FixedSizeHashMap(int initialCapacity, float loadFactor) {
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
       public FixedSizeHashMap(int initialCapacity) {
	        super(initialCapacity);
       }

       /**
        *
        */
       public FixedSizeHashMap() {
	       super();
       }

       /**
        * Constructs a new map with the same mappings as the given map.  The
        * map is created with a capacity of twice the number of mappings in
        * the given map or 11 (whichever is greater), and a default load factor,
        * which is <tt>0.75</tt>.
        */
        public FixedSizeHashMap(Map t) {
          super(t);
       }


       /**
        * set the max size of the hash map
        */
       public void setMaxSize(int maxSize) {
	       this.maxSize = maxSize;
       }

       /**
        * set the max size of the hash map
        */
       public int getMaxSize() {
	       return maxSize;
       }



       public Object put(Object key, Object value )
       {
          Object oldObj;

          if (maxSize > 0 ) {
          // if a max size is specified then check if the queue is filled
          // if it is full then get the oldest key and remove it from the map.
             if (queue.size() >= maxSize ) {
                Object oldKey = queue.removeFirst();
           
                oldObj = super.remove(oldKey);
             }
             // now add the key to the end of the queue
             // and add the key/value into the map.
             // if a key is already in the queue then remove it so that
             // it gets shifted to the end of the queue
             // this gives priority to just recently viewed data
             if ( queue.contains(key) ) {
               queue.remove(queue.indexOf(key));
             }
             // now add the item to the end of the list
             queue.add(key);

          }
          
          return super.put(key,value);

       }
      
      public String describe()
      {  
        return queue.toString();
      }
      

       public Object remove(Object key)
       {
          queue.remove(key);
          return(super.remove(key) );
       }
    }


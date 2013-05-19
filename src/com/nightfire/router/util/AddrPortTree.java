/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.util;

import java.util.*;
import com.nightfire.router.*;


/**
 * This is a tree structure for holding server objects.
 *
 * It maintains a tree structure using the address and port of the
 * server objects. All Serverobjects live at leaf nodes only.
 * @author Dan Diodati
 */
public final class AddrPortTree
    {
      private final int INITIAL_SIZE = 50;
      private final float LOAD_FACTOR = (float).75;

      private int size = 0;

      private HashMap root = new HashMap(INITIAL_SIZE,LOAD_FACTOR);

      /**
       * returns the number of items in this tree.
       * @return int The size
       */
      public int size()
      {
         return size;
      }

      /**
       * Add a serverobject to the tree
       * @param address The address the serverobject is located at.
       * @param port The port the serverobject is located at.
       * @param objName The key for this serverobj - used for retrieval.
       * @param obj An initialized serverobject to add to the tree
       */
      public void put(String address,String port, String objName, ServerObject obj)
      {
         Map addrTemp;
         Map portTemp;



         if (root.containsKey(address) ) {
            addrTemp = (HashMap) root.get(address);
            if (addrTemp.containsKey(port) ) {
               portTemp = (HashMap)addrTemp.get(port);
               portTemp.put(objName, obj);
            }
            else {
               portTemp = new HashMap(INITIAL_SIZE,LOAD_FACTOR);
               portTemp.put(objName, obj);
               addrTemp.put(port,portTemp);
            }
         }
         else  {

            addrTemp = new HashMap(INITIAL_SIZE,LOAD_FACTOR);
            portTemp = new HashMap(INITIAL_SIZE,LOAD_FACTOR);
            portTemp.put(objName, obj);
            addrTemp.put(port,portTemp);
            root.put(address,addrTemp);
         }

          size++;
      }

       /**
        * Get a server object from the tree.
        *
        * @param address The address the serverobject is located at.
        * @param port The port the serverobject is located at.
        * @param objName The key for this serverobj - used for retrieval.
        */
       public ServerObject get(String address,String port, String objName)
       {
         Map addrTemp;
         Map portTemp;

         ServerObject obj = null;

         if (root.containsKey(address) )  {
            addrTemp = (HashMap) root.get(address);
            if (addrTemp.containsKey(port) ) {
               portTemp = (HashMap)addrTemp.get(port);

               if ( portTemp.containsKey(objName) ) {
                  obj = (ServerObject)portTemp.get(objName);
               }
            }
         }
         return obj;
       }

       /**
        * remove a server object from the tree.
        *
        * @param address The address the serverobject is located at.
        * @param port The port the serverobject is located at.
        * @param objName The key for this serverobj - used for removal.
        * @return ServerObject returns the serverobject that was just removed.
        */
       public ServerObject remove(String address,String port, String objName)
       {
         Map addrTemp;
         Map portTemp;

         ServerObject obj = null;

         if (root.containsKey(address) )  {
            addrTemp = (HashMap) root.get(address);
            if (addrTemp.containsKey(port) ) {
               portTemp = (HashMap)addrTemp.get(port);

               if ( portTemp.containsKey(objName) ) {
                  obj = (ServerObject) portTemp.remove(objName);
                  size--;
               }
            }
         }

         return obj;
       }

       /**
        * checks if the current serverobject exists in this tree instance.
        *
        * @param address The address the serverobject is located at.
        * @param port The port the serverobject is located at.
        * @param objName The key for this serverobj - used for removal.
        * @return boolean returns true if the serverobject exists, otherwise returns false.
        */
       public boolean contains(String address,String port,String objName)
       {
         Map addrTemp;
         Map portTemp;

         if (root.containsKey(address) )   {
            addrTemp = (HashMap) root.get(address);

            if (addrTemp.containsKey(port) ) {
               portTemp = (HashMap)addrTemp.get(port);

               if ( portTemp.containsKey(objName) ) {
                  return true;
               }
            }
         }
         return false;
       }

       /**
        * traverses every server object in this tree
        *
        * @param visitor A ServerObjectVisitor to be called on each serverobject.
        */
       public void traverse(ServerObjectVisitor visitor)
       {

         String addrFilter = visitor.getAddressFilter();
         String portFilter = visitor.getPortFilter();
         String addrKey = null;
         String portKey = null;

         Iterator iter;

         Map addrTemp;
         Map portTemp;

         Iterator addrIt = root.keySet().iterator();

          while (addrIt.hasNext() && !visitor.isDone() ) {
             addrKey = (String) addrIt.next();

             if (addrFilter == null || addrKey.equals(addrFilter) ) {
                addrTemp = (Map) root.get(addrKey);

                Iterator portIt = addrTemp.keySet().iterator();

                while (portIt.hasNext() && !visitor.isDone() ) {
                  portKey = (String) portIt.next();

                  if (portFilter == null || portKey.equals(portFilter) ) {
                    portTemp = (Map)addrTemp.get(portKey);
                    // should the servers be held in a tree instead of a hashmap?
                    //visitor.visit(Collections.unmodifiableMap(portTemp), addrKey, portKey );
                    iter = portTemp.values().iterator();



                    while (iter.hasNext() && !visitor.isDone() ) {
                       visitor.visit( (ServerObject) iter.next(),addrKey, portKey);
                    }
                  }
                }
             }
          }

       }

    }

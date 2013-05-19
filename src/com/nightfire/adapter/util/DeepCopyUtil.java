package com.nightfire.adapter.util;

import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.MessageException;

public abstract class DeepCopyUtil {

   /**
   * This method knows how to do a deep copy of Map, List, and xml
   * Document instances. If a null value is passed in to be copied, then a null
   * will be returned. This method assumes that there are no cicular
   * references within a given Collection object (e.g. A given Map
   * must not contain another Map that has the first Map as one
   * of its elements.) If this method is called on an object with this
   * kind of circular reference, it will lead to an infinite loop.
   *
   * @param original The original object to be copied.
   * @return If the object is of type Document, List, or Map, a deep copy
   * of the orginal object will be returned, otherwise, the original
   * input object will be returned in place of the copy. If the original
   * is null, then a null is returned. 
   */
   public static Object copy(Object original){

      Object result;

      if(original == null){

         result = null;

      }
      else if(original instanceof String){

         // The value of a String cannot be altered, so it is OK
         // to have multiple refereneces to the same value.
         result = original;

      }
      else if(original instanceof Document){

         try{

            result = XMLLibraryPortabilityLayer.cloneDocument( (Document) original );

         }
         catch(Exception mex){

            Debug.log(Debug.ALL_ERRORS,
                      "The given Document object could not be copied: "
                      +mex.getMessage()+
                      " The original object ["+original+"] will be used instead.");

            result = original;

         }

      }
      else if(original instanceof Map){

         result = copyMap( (Map) original );

      }
      else if(original instanceof List){

         result = copyList( (List) original );

      }
      else{

         // If the object cannot be cloned, then just return
         // the original object.
         if(Debug.isLevelEnabled(Debug.ALL_WARNINGS)){

            Debug.log(Debug.ALL_WARNINGS, "Unable to make a deep copy of ["+
                                          original.getClass().getName()+
                                          ". The original instance ["+original+
                                          "] will be used instead of a copy.");

         }

         result = original;

      }

      return result;                                     

   }

   /**
   * This method creates a new Map and makes a copy of each entry
   * in the original Map with which to populate the new Map.
   *
   * @param original An implementation of Map (a Hashtable or HashMap) to be deep copied.
   * @return A deep copy of the original Map. If the original is null,
   *  then a null is returned. 
   */
   public static Map copyMap(Map original){

      Map result = null;

      if(original != null){

          Object currentKey;
          Object currentValue;
          Object valueCopy;

          // We don't want anyone adding or removing anything from our
          // original Map while we are busy copying it.
          synchronized(original){

              result = getNewMap(original);

              Set keySet    = original.keySet();
              Iterator keys = keySet.iterator();

              while( keys.hasNext() ){

                 currentKey   = keys.next();
                 currentValue = original.get(currentKey);

                 valueCopy = copy(currentValue);

                 result.put(currentKey, valueCopy);

              }

          }

      }

      return result;

   }


   /**
   * This method create a new List and copies every element of the give
   * <code>original</code> List into this new List and returns it.
   * If the given List is of type ArrayList, LinkedList, or Vector,
   * the returned List will be an instance of the same class, otherwise,
   * the default List implementation is a Vector.
   * This method will make deep copies of any List, Map, or Document
   * objects contained within the input List.
   *
   * @return A new List containing deep copies of every element in the given List.
   * If the original is null, then a null is returned.
   */
   public static List copyList(List original){

      Object currentValue;
      Object valueCopy;

      List result = null;

      if(original != null){

          // We synchronize on the list to keep any bozos from jostling it
          // around while it is being copied.
          synchronized(original){

              result = getNewList(original);

              Iterator values = original.listIterator();

              while( values.hasNext() ){

                 currentValue = values.next();
                 valueCopy    = copy(currentValue);

                 result.add(valueCopy);

              }

          }

      }

      return result;

   }

   /**
   * This method creates a new List with the same concrete class type
   * and size as the given <code>original</code> List. This method knows
   * how to create instances of ArrayList, LinkedList, and Vector.
   * If the <code>original</code> List is of a type that is not known
   * by this method, a new Vector will be returned as a default.
   *
   * @param original A List implementation.
   * @return An empty List of the same type as the original List. If the
   * original is null, null is returned.
   */
   protected static List getNewList(List original){

      List result = null;

      if(original != null){

          if(original instanceof ArrayList){

             result = new ArrayList(original.size());

          }
          else if(original instanceof LinkedList){

             result = new LinkedList();

          }
          else{

             result = new Vector(original.size());

          }

      }

      return result;

   }


   /**
   * This method creates a new Map of the same concrete class type
   * and size as the given <code>original</code> Map. This method knows
   * how to create instances of Hashtable and HashMap.
   * If the <code>original</code> Map is of a type that is not known
   * by this method, a new HashMap will be returned as a default.
   *
   * @param original A Map implementation.
   * @return An empty Map of the same type and capacity as the original Map.
   * If the original is null, null is returned.
   */
   protected static Map getNewMap(Map original){

      Map result = null;

      if(original != null){

         if(original instanceof Hashtable){

            result = new Hashtable(original.size());

         }
         else{

            result = new HashMap(original.size());

         }

      }

      return result;

   }

}
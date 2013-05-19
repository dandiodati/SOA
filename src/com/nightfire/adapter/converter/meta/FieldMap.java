/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.*;

/**
* This class is a FieldContainer that provides access to its fields
* via String keys.
* By default, the field data is keyed
* off of the field names. An array of all available field names can
* be accessed using the <code>getKeys()</code> method. 
*/
public class FieldMap extends FieldContainer{

    /**
    * This table holds all of the Field instances that have
    * been added to this map. By default, the field names are
    * used as the keys.
    */
    protected Map fieldMap = new HashMap();

    public FieldMap(){

       super("", null);

    }

    public FieldMap(String name){

       super(name);

    }

    public FieldMap(String name, String path){

       super(name, path);

    }

    /**
    * This gets the field for the given field name.
    *
    * @param key the key for accessing a field. By default,
    *            this is the field's name.
    */
    public Field getField(String key){

       return (Field) fieldMap.get(key);

    }


    /**
    * This is a utility method for checking to see if a field exists. 
    *
    * @param key the key for accessing a field's info. By default,
    *            this is the field's name.
    *
    * @return true if a field with the given path can be found in this
    *         map, false otherwise.
    */
    public boolean exists(String key){

       return (fieldMap.get(key) != null);

    }

    /**
    * This returns an array containing the unique keys for all of the fields
    * in this accessor. The keys are returned in order in the order the fields
    * were added.
    *
    * @return an array of the keys for this map.
    */
    public String[] getKeys(){

       int size = size();

       String[] keys = new String[size];

       for(int i = 0; i < size; i++){

           keys[i] = getKey( getFieldAt( i ) );

       }

       return keys;

    }

    /**
    * This gets the key that should be used to store and access the given
    * field. The default key value is the field's name. This is overriden
    * by the MapFieldInfoAccessor to use the field's XPath instead.
    *
    * @param field the Field whose key we'd like to know.
    * @return the key to use for accessing this field.
    */
    public String getKey(Field field){

       return field.getName();

    }

    /**
    * This adds a field to this map.
    */
    public void add(Field field){

       fieldMap.put( getKey(field), field);
       super.add(field);

    }

    /**
    * Removes a field from this map.
    */
    public boolean remove(Field field){

       Object removed = null;
       String key = getKey( field );

       if(exists(key)){
          removed = fieldMap.remove(field);
       }
       
       return super.remove(field) && (removed != null);

    }

    /**
    * Returns true if an entry exists in the map for the given childField's
    * key. 
    */
    public boolean contains(Field childField){

       return exists( getKey(childField) );

    }

}



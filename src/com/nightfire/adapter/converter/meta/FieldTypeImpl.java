/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.Map;
import com.nightfire.framework.util.FrameworkException;

public class FieldTypeImpl implements FieldType{

   /**
   * The name of this type.
   */
   private String name;

   /**
   * The class to use for this field type.
   */
   private Class type;

   /**
   * The prototype instance of the field type created by this class.
   * This template instance is used to create new instances of that type,
   * and to store and retrieve property values for this type. Using the
   * prototype object is useful since it knows all the default property
   * values for class of its particular type.
   */
   private Field prototype;

   public FieldTypeImpl(String name, Class type, Map properties)
                        throws FrameworkException{

      setName( name );
      setFieldClass( type );
      prototype.setProperties(properties);

   }

   /**
   * Gets the name of this type.
   */
   public String getName(){

      return name;

   }

   public Map getProperties(){

      return prototype.getProperties();

   }

   /**
   * Sets the value of the property with the given name.
   */
   public void setProperty(String propName, Object value){

      prototype.setProperty(propName, value);

   }

   /**
   * Gets the value of the property with the given name.
   */
   public Object getProperty(String propName){

      return prototype.getProperty(propName);

   }

   /**
   * Gets the default value for the given property name.
   */
   public Object getDefaultPropertyValue(String name){

      return prototype.getDefaultPropertyValue(name);

   }

   /**
   * Sets the new name of this type.
   */
   public void setName(String name){

     this.name = name;

   }

   /**
   * Sets the class that should be instantiated when newInstance() is called.
   * This class must implement the Field interface.
   */
   public void setFieldClass(Class fieldClass) throws FrameworkException{

      prototype = FieldFactory.newInstance(getName(), fieldClass);
      type = fieldClass;

   }

   /**
   * Gets the class that is instantiated when newInstance() is called.
   */   
   public Class getFieldClass(){

      return prototype.getFieldClass();

   }

   /**
   * Create a new Field of this type. This method assumes that the
   * class being created has a constructor that takes a single string (name)
   * argument.
   *
   * @param name the name for the new field.
   */
   public Field newInstance(String name) throws FrameworkException{

       Field result = prototype.newInstance(name);
       result.setType(this);
       return result;

   }


   /**
   * Writes out the name and properties of this type.
   */
   public String toString(){

      StringBuffer result = new StringBuffer("\nfield type: [");

      result.append(name);
      result.append("]\nclass: [");
      result.append(type);
      result.append("]\n");

      java.util.Set keys = getProperties().keySet();
      java.util.Iterator names = keys.iterator();
      Object currentKey = null;
      String key = null;

      while(names.hasNext()){

         currentKey = names.next();
         key = currentKey.toString();
         result.append(key);
         result.append(":\t\t[");
         result.append( getProperty( key ) );
         result.append("]\n");

      }

      return result.toString();     

   }

   /**
   * Returns true if the Field represented by this type is a container and
   * contains the given child field.
   */
   public boolean contains(Field childField){

      return prototype.contains(childField);

   }

} 
/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.Map;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.ObjectFactory;

/**
* This interface describes a field type that is used to hold common
* properties shared by all fields having this type.
*/
public interface FieldType{

   /**
   * Gets the name of this type.
   */
   public String getName();

   /**
   * Sets the name of this type.
   */
   public void setName(String name);

   /**
   * Gets the map of property names and values associated with this type.
   */
   public Map getProperties();

   /**
   * Sets the value of the property with the given name.
   */
   public void setProperty(String propName, Object value);

   /**
   * Gets the value of a particular property by name.
   */
   public Object getProperty(String propName);

   /**
   * Gets the default value for the given property name.
   */
   public Object getDefaultPropertyValue(String name);   

   /**
   * Sets the class type to use when creating instances of this type.
   *
   * @throws FrameworkException if the given Class could not be found,
   *                            is incompatible with this type, or
   *                            is not an instance of Field.
   */
   public void setFieldClass(Class newType) throws FrameworkException;

   /**
   * Gets the Class type that will actually be used when creating a Field
   * instance of this type.
   */
   public Class getFieldClass();

   /**
   * Create a new Field of this type. This method assumes that the
   * class being created has a constructor that takes a single string (name)
   * argument.
   *
   * @param name the name for the new field.
   *
   * @throws FrameworkException if a new instance of this type could not be
   *                            created
   */
   public Field newInstance(String name) throws FrameworkException;

   /**
   * Returns true if the Field represented by this type is a container and
   * contains the given child field.
   */
   public boolean contains(Field childField);


}
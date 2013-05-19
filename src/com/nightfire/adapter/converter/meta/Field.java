/**
* Copyright (c) 2001 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import java.util.Map;
import java.util.List;

import org.w3c.dom.Document;

/**
* A Field contains the meta data that describes an element of a message.
* This interface assumes that we want to convert the message to and from
* an XML representation and declares methods to do just that.
*/
public interface Field extends FieldType{

   /**
   * The name of this field.
   */
   public String getName();

   /**
   * Sets the new name of the field.
   */
   public void setName(String newName);

   /**
   * Sets the attributes of this field based on the given properties.
   */
   public void setProperties(Map properties);

   /**
   * Gets the list of property names used by this field implementation.
   */   
   public List getPropertyNames();

   /**
   * Gets the value of this field's property with the given name.
   */
   public Object getProperty(String name);

   /**
   * Sets the value of this field's property with the given property name.
   */
   public void setProperty(String propName, Object value);

   /**
   * Gets the default value for the given property name.
   */
   public Object getDefaultPropertyValue(String name);

   /**
   * Write this field as a string from the given XML Document. 
   *
   * @param context This is the current path to use as a context.
   * @param input The input XML from which to extract the output value. 
   * @param output The output value for this field will get written to this
   *               buffer.
   */
   public void write(FieldContext context, Document input, StringBuffer output);

   /**
   *
   * @param context This is the current path to use as a context.
   * @param input the XML input message. 
   * @param output The output value for this field will get written to this
   *               buffer.
   */
   public void write(FieldContext context, XPathAccessor input, StringBuffer output);

   /**
   * Reads this field from the given value string and assigns that
   * value to the appropriate location in the given generator. 
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.  
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output);

   /**
   * Creates a new object that is a copy of the Field instance.
   *
   */
   public Field copy();

   /**
   * Returns the field type used to create this field.
   */
   public FieldType getType();

   /**
   * Sets the parent type for this field.
   *
   * @throws FrameworkException if the Class described by the new type
   *                            is not the same as the type of this
   *                            Field instance. 
   */
   public void setType(FieldType newType) throws FrameworkException;

   /**
   * Returns true if the Field implementation is a container and contains
   * the given child field.
   */
   public boolean contains(Field childField);

} 
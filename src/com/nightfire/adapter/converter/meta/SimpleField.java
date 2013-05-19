/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.XPathAccessor;

import org.w3c.dom.Document;

import java.util.*;

/**
* The SimpleField simply knows how to take a value from a location in an
* XML and write it to an output buffer, and it can also do the reverse, take
* an input String value and stick it into the proper location in an XML. 
*/
public class SimpleField implements Field{

   private static List PROP_NAMES = null;

   public static final String PATH = "path";

   /**
   * The name of this field.
   */
   protected String name;

   /**
   * The relative XML path to this field.
   */
   protected String path;

   /**
   * The type for this field.
   */
   protected FieldType type = null;

   /**
   * This constructs a simple field with the given name. By default, this name
   * will also be used as this field's path (all spaces in the name will be
   * replaced by underscores when the name is used as the path).
   *
   * @param name the field name.
   */
   public SimpleField(String name){

      this( name, name );

   }

   /**
   * This constructs a simple field with the given name and path.
   *
   * @param name the field name.
   * @param path the path to this field (relative to this field's parent field).
   */   
   public SimpleField(String name, String path){

      setName(name);
      setPath(path);

   }

   /**
   * Assigns the name of this field.
   */
   public String getName(){

      return name;

   }

   /**
   * Accesses the name of this field.
   */
   public void setName(String name){

      this.name = name;

   }

   /**
   * Accesses the path name for this field. This is a relative path in the
   * NightFire
   * dotted notation that helps designate where this field lives
   * in an XML format message.
   */
   public String getPath(){

      return path;

   }

   /**
   * Sets the path name for this field. This path should be a relative path in
   * the NightFire dotted notation. Any spaces in the given path will be
   * replaced with underscores. 
   */
   protected void setPath(String newPath){

      if(newPath != null){
         this.path = newPath.replace(' ','_');
      }
      else{
         this.path = null;
      }

   }   

   /**
   * This constructs a path for this field, relative to the given context.
   * If this field's path is null or has no value, then the given context
   * is returned unchanged. 
   *
   * @context the context to which this field's path should be appended. 
   */
   public FieldContext getFullPath(FieldContext context){

      return context.append(path);

   }

   /**
   * Gets the field type object for this field. This may be null. 
   */
   public FieldType getType(){

      return type;

   }

   /**
   * Sets the type that is used to instantiate this Field.
   *
   * @throws FrameworkException if the Class described by the new type
   *                            is not the same as the type of this
   *                            Field instance. In this case, a new Field
   *                            needs to be created of this type instead.
   */
   public void setType(FieldType newType) throws FrameworkException{

      // this field will now have no parent type
      if(newType == null){
         type = null;
         return;
      }

      // check that the class for the new type is the same as the type
      // of the current instance
      setFieldClass( newType.getFieldClass() );

      type = newType;

      // copy the type's properties into this field
      copyProperties(type, this);

   }

   /**
   * This defines the FieldType interface to return the Class type to use
   * when creating new instances of this type of Field. 
   */
   public Class getFieldClass(){

      return getClass();

   }

   /**
   * This method is only here to implement the FieldType interface.
   * This is not allowed since this field instance can not change its
   * class type.
   *
   * @throws FrameworkException if the given Class is not the same
   *                            as the type of this Field instance.
   */
   public void setFieldClass(Class typeClass) throws FrameworkException{

      if(! this.getClass().equals(typeClass) ){

         throw new FrameworkException("The type ["+
                                      typeClass.getName()+
                                      "] is not compatible with field ["+
                                      getName()+"] which is of type ["+
                                      getClass()+"]");

      }

   }   

   /**
   * This wraps the XML input in an XPath accessor and passes it off to the
   * other <code>write</code> method to do the real work.
   *
   * @param context This is the current path to use as a context.
   * @param xmlInput the XML input.
   * @param output The output value for this field will get written to this
   *               buffer.
   */
   public void write(FieldContext context,
                     Document xmlInput,
                     StringBuffer output){

      XPathAccessor source;

      try{
         source = new XPathAccessor(xmlInput);
      }
      catch(FrameworkException mex){

         Debug.log(Debug.ALL_ERRORS, "Field ["+getName()+
                                     "] could not create an XML accesor for input:\n"+
                                     xmlInput+
                                     "\n: "+mex.getMessage() );
         return;
         
      }

      write(context, source, output);

   }

   /**
   * Calls <code>getValue()</code> and appends the resulting value to the
   * output buffer.
   *
   * @param context This is the current path to use as a context.
   * @param input the XML input message.
   * @param output The buffer where the output will be written.
   */
   public void write(FieldContext  context,
                     XPathAccessor input,
                     StringBuffer  output){

      FieldContext fullPath = getFullPath(context);
      String value = getValue( fullPath, input );

      if( Debug.isLevelEnabled(Debug.MAPPING_STATUS) ){

         Debug.log(Debug.MAPPING_STATUS, "Writing value ["+value+
                                         "] for field ["+this+"] and context ["+
                                         fullPath+"]");

      }

      output.append( value );

   }   

   /**
   * This utilty method returns the value of the given path from the given
   * parsed XML source.
   * If the given value path does not exist, then this returns an empty string.
   */
   public String getValue(FieldContext valuePath, XMLMessageParser source){

      String value = "";

      String xmlPath = valuePath.getNFPath();

      if( source.exists(xmlPath) ){

         try{
            value = source.getValue( xmlPath );
         }
         catch(MessageException mex){

            Debug.log(Debug.ALL_ERRORS,
                      "Could not get value for field ["+
                      getName()+"] from path ["+xmlPath+"]: "+
                      mex.getMessage());

         }

      }

      return value;

   }

   /**
   * This utilty method returns the value of the given path from the given
   * XPathAccessor source.
   * If the given value path does not exist, then this returns an empty string.
   */
   public String getValue(FieldContext valuePath, XPathAccessor source){

      String value = "";

      String xPath = valuePath.getXPath();

      if( source.nodeExists( xPath ) ){

         try{

            value = source.getValue( xPath + "/@value" );
            if( Debug.isLevelEnabled(Debug.MAPPING_STATUS) ){

               Debug.log(Debug.MAPPING_STATUS, "Retreived value: ["+value+"]");

            }

            if(value == null){
               value = "";
            }

         }
         catch(FrameworkException mex){

            Debug.log(Debug.ALL_ERRORS,
                      "Could not get value for field ["+
                      getName()+"] from path ["+xPath+"]: "+
                      mex.getMessage());

         }

      }

      return value;

   }   

   /**
   * If this field's <code>path<code> is null, then this field will
   * not add anything to the output generator. If this field does have
   * a path value, then this will set the given value at that
   * path in the generator.
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.  
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output){

      if(path != null){
         
         setValue(getFullPath(context), value, output);

      }

   }

   

   /**
   * A utility method for setting the given value at the given path in the
   * given XML generator and logging any error that occurs. 
   */
   public void setValue(FieldContext outputPath,
                        String value,
                        XMLMessageGenerator output){

      try{

         output.setValue(outputPath.getNFPath(), value);

      }
      catch(MessageException mex){

         Debug.log(Debug.ALL_ERRORS,
                   "Could not set value ["+value+"] for field ["+
                   getName()+"] at path ["+outputPath+"]: "+
                   mex.getMessage());

      }

   }

   /**
   * Returns a string listing the names and values of this field's
   * properties.
   */
   public String toString(){

      StringBuffer result = new StringBuffer("\nfield name:\t[");
      result.append(getName());
      result.append("]\ntype:\t\t[");
      FieldType currentType = getType();
      if(currentType == null){
         result.append(getClass().getName());
      }
      else{
         result.append(currentType.getName());
      }

      result.append("]\nclass:\t\t[");
      result.append(getClass().getName());      

      List propNames = getPropertyNames();
      String currentName;

      for(int i = 0; i < propNames.size(); i++){

         result.append("]\n");

         currentName = (String) propNames.get(i);

         result.append(currentName);
         result.append(":\t\t[");

         Object value = getProperty(currentName);

         if(value != null){
            result.append( value.toString() );
         }
         else{
            result.append("null");
         }

      }

      result.append("]\n");

      return result.toString();

   }


   /**
   * Iterates through each key and value in the given
   * properties and calls setProperty() on each of these pairs.
   */
   public void setProperties(Map properties){

      Set keys = properties.keySet();

      Iterator iter = keys.iterator();

      while(iter.hasNext()){

         String key = iter.next().toString();
         setProperty( key, properties.get(key) );

      }

   }

   public void setProperty(String propName, Object value){

      if( propName.equalsIgnoreCase(PATH) ){

         setPath( (String) value );

      }
      else if( Debug.isLevelEnabled(Debug.ALL_WARNINGS) ){

         Debug.log(Debug.ALL_WARNINGS,
                   "No property found for name ["+
                   propName+
                   "]. The available property names are: ["+
                   getPropertyNames()+"]");

      }

   }

   /**
   * Gets the list of
   */
   public List getPropertyNames(){

      if(PROP_NAMES == null){

         PROP_NAMES = new ArrayList();
         PROP_NAMES.add(PATH);

      }

      return PROP_NAMES;

   }

   /**
   * Gets the value of this field's property with the given name.
   */
   public Object getProperty(String name){

      if( name.equalsIgnoreCase(PATH) ){

         return path;

      }

      return null;

   }

   /**
   * Gets the default value for the given property. This is used in
   * determining if a property value needs to be written to the XML
   * definition file. If the current property value is equal to the default
   * value for this field, then there is not reason to write that property
   * to the XML.
   *
   */
   public Object getDefaultPropertyValue(String name){

      if( name.equalsIgnoreCase(PATH) ){

         // the default path value is equal to the name of this field
         return getName();

      }

      return null;

   }

   /**
   * Creates a new Field of this
   * type and then calls the static <code>copyProperties()</code> method
   * to copy all of this field's properties into the new instance. This
   * method differs from newInstance() in that the new Field will have the
   * same type as this field instead of this field itself becoming the
   * type for the created field.
   *
   */
   public Field copy(){

      Field newField = null;

      try{
         newField = FieldFactory.newInstance( name, getClass() );
      }
      catch(FrameworkException fex){

         Debug.log(Debug.ALL_ERRORS, fex.getMessage());
         newField = new SimpleField( getName() );

      }

      try{
         newField.setType( this.getType() );
      }
      catch( FrameworkException fex){

         // this should never happen 
         Debug.log(Debug.ALL_ERRORS,
                   "Could not set type for copy of field ["+name+
                   "]: "+fex.getMessage());

      }

      copyProperties(this, newField);      

      return newField;

   }

   

   /**
   * Creates a new Field of this
   * type and then calls the static <code>copyProperties()</code> method
   * to copy all of this field's properties into the new instance. This
   * defines the FieldType interface. This Field instance will become the type
   *
   *
   * @param newName the field name to use for the new instance.
   * @throws FrameworkException if the new instance could not
   *                            be created.
   */
   public Field newInstance(String newName) throws FrameworkException{

      Field newField = FieldFactory.newInstance( newName, getClass() );

      // set this Field as the FieldType for the new instance
      newField.setType(this);

      // copy this field's properties into the new instance
      copyProperties(this, newField);

      return newField;

   }

   /**
   * Copies all of the properties of the <code>from</code>
   * FieldType to the <code>to</code> Field.
   */
   public static void copyProperties(FieldType from, Field to){

      Map fromProps = from.getProperties();

      // only copy the path over if it has been set to something other than the
      // the default value, which is to use the field's name
      String fromPath = (String) fromProps.get(PATH);
      if( fromPath != null && fromPath.equals(from.getName()) ){

         fromProps.remove(PATH);

      }

      to.setProperties(fromProps);

   }

   /**
   * Defines the FieldType interface. This returns an unsynchronized Map
   * of this field's properties.
   */
   public Map getProperties(){

      Map props = new HashMap();
      List names = getPropertyNames();
      String key;

      for(int i = 0; i < names.size(); i++){

         key = names.get(i).toString();
         props.put( key, getProperty(key) );

      }

      return props;

   }

   /**
   * The SimpleField is not a FieldContainer, so this will always return
   * false.
   */
   public boolean contains(Field childField){

      return true;

   }

   /**
   * Returns true if the given object is a field of this type with the same
   * properties as this instance.
   */
   public boolean equals(Object o){

      if(o == this){
         return true;
      }

      // check that the object is of the same type as this one
      if( getClass().equals(o.getClass()) ){

         Field compareMe = (Field) o;
         boolean sameName = getName().equals(compareMe.getName());
         boolean sameProperties =  getProperties().equals(compareMe.getProperties()) ;

         return sameName && sameProperties;

      }

      // the given object was not of the same type as this one
      return false;

   }

}
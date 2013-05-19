/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.meta;

import java.util.*;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;

import com.nightfire.framework.util.ObjectFactory;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;

import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.cache.CacheManager;

import org.w3c.dom.Node;

/**
* This class is used to load Field instances from an XML file.
*/
public class FieldFactory {

   public static final String ROOT = "root";

   public static final String NAME = "name";

   public static final String TYPE_CONTAINER = "type_container";

   public static final String TYPE = "type";

   public static final String FIELD_CONTAINER = "field_container";

   public static final String FIELD = "field";

   public static final String PROPERTY = "properties.property";

   public static final String FIELD_TYPE_PATH = TYPE_CONTAINER+"."+TYPE;

   public static final String FIELD_PATH = FIELD_CONTAINER+"."+FIELD;

   public static final String CLASS = "class";

   public static final String CHILDFIELDS_PATH = FIELD_CONTAINER+"."+FIELD;

   public static final String NULL = "null";

   public static final String DEFAULT = "Default";

   protected static FieldMapCache mapCache = new FieldMapCache();

   static{

      // register the map cache so that it can be flushed and the maps reloaded 
      CacheManager.getRegistrar().register(mapCache);

   }

   /**
   * Loads a FieldMap from the file with the given name.
   * If the map has already been loaded, then the cached instance
   * is used instead of reloading.
   */
   public static FieldMap getMap(String fileName) throws FrameworkException{

      FieldMap map = (FieldMap) mapCache.get(fileName);

      if(map == null){

         map = loadMap(fileName);
         mapCache.put(fileName, map);

      }

      return map;

   }

   /**
   * This calls <code>getMap()</code> to get the map for the given
   * file name, and then gets the field with the given name from that map.
   */
   public static Field getField(String fileName, String fieldName)
                                throws FrameworkException{

      FieldMap map = getMap(fileName);

      return map.getField(fieldName);

   }

   /**
   * Loads XML from the given file and passes it to <code>createMap()</code>
   * to create a new FieldMap.
   */
   protected static FieldMap loadMap(String fileName) throws FrameworkException{

      String xml = FileUtils.readFile(fileName);
      return createMap(xml);

   }

   /**
   * Creates a field map from the given XML.
   */
   protected static FieldMap createMap(String xml) throws FrameworkException{

      FieldMap result = new FieldMap();

      XMLMessageParser source = new XMLMessageParser(xml);

      // get the map of type names to their respective types
      Map types = getTypeMap(source);

      Field currentField;
      int i = 0;
      String currentPath = FIELD_PATH + "(0)";

      while( source.exists(currentPath) ){

         currentField = createField( source, types, currentPath );
         result.add(currentField);
         currentPath = FIELD_PATH + "(" + (++i) + ")";

      }

      return result;

   }

   /**
   * Reads a meta file and returns a Map containing the field types defined
   * in that file keyed by type name. 
   */
   public static Map getTypeMap(String metaFileName) throws FrameworkException{

      String xml = FileUtils.readFile(metaFileName);
      return getTypeMap( new XMLMessageParser(xml) );

   }

   /**
   * Gets a map of types keyed by type name. 
   */
   protected static Map getTypeMap(XMLMessageParser typeAccessor)
                                   throws FrameworkException{

      Map types = new OrderedMap();

      String currentName;
      String currentType;
      String typePath;
      String fieldPath;
      Class  currentClass;
      Map    currentProperties;
      String currentPath = FIELD_TYPE_PATH + "(0)";
      FieldType type = null;
      int i = 0;
      
      while( typeAccessor.exists(currentPath) ){

         typePath = currentPath + "." + CLASS;

         if(! typeAccessor.exists(typePath) ){

            // A Class name was not specified for this type.
            // Check to see if a field definition was given instead.
            fieldPath = currentPath + "." + FIELD;
            if( typeAccessor.exists(fieldPath) ){

               // get the field definition to be used as a template type
               type = createField(typeAccessor,
                                  types,
                                  fieldPath);

               if( Debug.isLevelEnabled(Debug.MAPPING_LIFECYCLE) ){
                  Debug.log(Debug.MAPPING_LIFECYCLE,
                            "Created template field type: "+type);
               }

            }
            else{

               throw new FrameworkException("No class name or field definition was specified for type at path: ["+
                                            currentPath+"]");

            }

         }
         else{

            try{
               currentName  = typeAccessor.getValue( currentPath + "." + NAME );
            }
            catch(MessageException noName){

               throw new FrameworkException("No name was specified for the type at path: ["+
                                            currentPath+"]");

            }

            try{
               currentType  = typeAccessor.getValue(typePath);
            }
            catch(MessageException noName){

               throw new FrameworkException("No class name was specified for the type: ["+
                                            currentName+"]");

            }

            try{
               currentClass = Class.forName(currentType);
            }
            catch(ClassNotFoundException cnfex){
               throw new FrameworkException("Could not find class ["+currentType+
                                            "] for type definition ["+currentName+
                                            "]");
            }

            currentProperties = getProperties( typeAccessor, currentPath );

            // create the type implementation
            type = new FieldTypeImpl(currentName,
                                     currentClass,
                                     currentProperties);

            if( Debug.isLevelEnabled(Debug.MAPPING_LIFECYCLE) ){
               Debug.log(Debug.MAPPING_LIFECYCLE, "Created field type:\n"+type);
            }

         }

         // add to type table
         types.put( type.getName(), type );

         currentPath = FIELD_TYPE_PATH + "("+(++i)+")";

      }

      return types;

   }

   protected static Field createField(XMLMessageParser source,
                                      Map types,
                                      String currentPath)
                                      throws FrameworkException{

      Field field = null;
      String name;
      String typeName;

      try{
         name = source.getValue(currentPath + "." + NAME);
      }
      catch(MessageException noName){

         throw new FrameworkException("No name was specified for the field at path: ["+
                                      currentPath+"]");

      }

      try{
         typeName = source.getValue(currentPath + "." + TYPE);
      }
      catch(MessageException noName){

         Debug.log(Debug.MSG_STATUS,
                   "No type was specified for field ["+
                   name+"] using default type value of ["+DEFAULT+"]");
         typeName = DEFAULT;

      }

      Object typeObject = types.get(typeName);

      if(typeObject == null){

         try{
            // check if the type is a class, and create
            // and instance of that class
            Class fieldClass = Class.forName(typeName);
            field = newInstance(name, fieldClass);

         }
         catch(ClassNotFoundException cnfex){

            throw new FrameworkException("Type ["+typeName+
                                         "] used by field ["+name+
                                         "] is not defined.");

         }

      }
      else if(typeObject instanceof FieldType){

         // create a new instance of that type
         FieldType type = (FieldType) typeObject;
         field = type.newInstance(name);

      }
      else{

            throw new FrameworkException("Type ["+typeName+
                                         "] used by field ["+name+
                                         "] is not an instance of "+
                                         FieldType.class.getName() );

      }


      Map properties = getProperties(source, currentPath);
      field.setProperties(properties);


      String childFieldPath = currentPath + "." + CHILDFIELDS_PATH + "(0)";
      int j = 0;

      // read subfields, if any
      if(field instanceof FieldContainer){

         FieldContainer container = (FieldContainer) field;

         while( source.exists(childFieldPath) ){

            Field temp = createField( source, types, childFieldPath );
            container.add( temp );
            childFieldPath = currentPath + "." +
                             CHILDFIELDS_PATH + "(" + (++j) + ")";

         }

      }
      else if( source.exists(childFieldPath) ){

         if( Debug.isLevelEnabled(Debug.ALL_WARNINGS) ){
            Debug.log(Debug.ALL_WARNINGS,
                      "Field ["+name+
                      "] has child fields but is not a field container.");
         }

      }
      

      if( Debug.isLevelEnabled(Debug.MAPPING_LIFECYCLE) ){
         Debug.log(Debug.MAPPING_LIFECYCLE, "Created field:\n"+field);
      }

      return field;

   }

   protected static Map getProperties(XMLMessageParser propertyAccessor,
                                      String currentPath)
                                      throws FrameworkException{

      Map map = new HashMap();

      String path = currentPath + "." + PROPERTY;
      currentPath = path + "(0)";
      int i = 0;
      Object value = null;

      while( propertyAccessor.exists(currentPath) ){


         try{

            value = propertyAccessor.getValue(currentPath);

            if(value.equals(NULL)){
               value = null;
            }

            try{

               map.put( propertyAccessor.getAttributeValue(currentPath, NAME),
                        value );

            }
            catch(MessageException noName){

               Debug.log(Debug.ALL_ERRORS,
                         "The property at ["+currentPath+
                         "] has no ["+
                         NAME+"] attribute.");

            }

         }
         catch(MessageException noValue){

               Debug.log(Debug.ALL_ERRORS,
                         "The property at ["+currentPath+
                         "] has no value.");

         }

         // increment index
         currentPath = path + "(" + (++i) + ")";
         // reset value
         value = null;

      }

      if( Debug.isLevelEnabled(Debug.MAPPING_LIFECYCLE) ){
         Debug.log(Debug.MAPPING_LIFECYCLE, "Read properties:\n"+map);
      }

      return map;

   }

   /**
   * Converts the given field to its XML equivalent.
   */
   public static String fieldToXML(FieldMap field) throws MessageException{

      // create a table to hold the types used by the fields
      Map typeTable = new HashMap();

      return fieldToXML(field, typeTable);

   }

   /**
   * Converts the given field to its XML equivalent.
   */
   public static String fieldToXML(FieldMap field,
                                   Map typeTable) throws MessageException{

      XMLMessageGenerator generator = new XMLMessageGenerator(ROOT);

      // Create a type container in the output XML.
      // We need to do this first, so that when the type definitions
      // are added to the XML, they will come before the field definitions.
      generator.setTextValue(TYPE_CONTAINER, "");

      // for each field in the map, write that field to the XML
      String[] fieldNames = field.getKeys();
      FieldContext path = new FieldContext(FIELD_CONTAINER+"(1)."+FIELD); 

      for(int i = 0; i < fieldNames.length; i++){

         Field temp = field.getField( fieldNames[i] );

         // call the recursive fieldToXML
         fieldToXML(temp, path, generator, typeTable);

         path.add();

      }

      // write the type definitions out to XML
      typesToXML(typeTable.values(), generator);

      return generator.generate();

   }

   /**
   * Takes a collection of FieldType instances and writes their XML equivalents
   * to the given generator.
   */
   private static void typesToXML(Collection types,
                                  XMLMessageGenerator generator)
                                  throws MessageException{

      Iterator typeIter = types.iterator();
      FieldType currentType;
      FieldContext typePath = new FieldContext(TYPE_CONTAINER+"."+TYPE);
      // fieldToXML calls for a map parameter which isn't used at all in this
      // case
      Map dummyMap = new HashMap();

      while(typeIter.hasNext()){

         currentType = (FieldType) typeIter.next();

         if(currentType instanceof Field){

            fieldToXML((Field) currentType,
                       typePath.append(FIELD),
                       generator,
                       dummyMap);

         }
         else{

            // write name and Field Class for this type
            generator.setValue(typePath.getNFPath()+"."+NAME,
                               currentType.getName());
            generator.setValue(typePath.getNFPath()+"."+CLASS,
                               currentType.getFieldClass().getName());

            Map typeProps = currentType.getProperties();
            Iterator propNames = typeProps.keySet().iterator();
            String currentName;
            Object propertyValue;
            FieldContext propertyPath = typePath.append( PROPERTY );

            while(propNames.hasNext()){

               currentName = propNames.next().toString();

               propertyValue = typeProps.get(currentName);

               if(propertyValue == null){
                  propertyValue = NULL;
               }

               setProperty(currentType,
                           propertyPath,
                           currentName,
                           propertyValue,
                           generator);

            }

         }

         typePath.add();

      }

   }

   /**
   *
   * @param field the field to convert to XML.
   * @param path the path under which to append the field's XML.
   * @param output this is where the output XML will get generated.
   * @param typeTable the table in which the field types that are found will be
   *                  stored.
   */
   public static void fieldToXML(Field field,
                                 FieldContext path,
                                 XMLMessageGenerator output,
                                 Map typeTable)
                                 throws MessageException{

      output.setValue(path.getNFPath()+"."+NAME, field.getName() );

      FieldType type = field.getType();
      if(type == null){
         // the field has no parent type, so just use its class name
         output.setValue(path.getNFPath()+"."+TYPE, field.getClass().getName() );
      }
      else{

         output.setValue(path.getNFPath()+"."+TYPE, type.getName() );
         // add type to type table if not already there
         if(! typeTable.containsKey(type.getName()) ){
            typeTable.put( type.getName(), type );            
         }

      }

      // set properties
      List props = field.getPropertyNames();
      Iterator keys = props.iterator();
      FieldContext propertyPath = path.append( PROPERTY );
      String key;
      Object value;
      String currentPath;

      while(keys.hasNext()){

         currentPath = propertyPath.getNFPath();

         key   = keys.next().toString();
         value = field.getProperty(key);

         if(value == null){
            // use the constant that represents a null
            value = NULL;
         }

         if(type == null){
            // if there is no type, then just set the property
            // value in the XML
            setProperty(field, propertyPath, key, value, output);
         }
         else{

            // Check first to see if the type has the same value for the
            // current property. If the type already has the same value for
            // this property as the field does, then there is no need to
            // write out the value for this individual field.
            Object typePropertyValue = type.getProperty(key);

            if(typePropertyValue == null){
               typePropertyValue = NULL;
            }

            if(! value.toString().equals(typePropertyValue.toString()) ){

               setProperty(field, propertyPath, key, value, output);

            }

         }

      }

      // add child fields
      if(field instanceof FieldContainer){

         FieldContainer container = (FieldContainer) field;

         path = path.append(FIELD_CONTAINER+"."+FIELD);

         Field child;

         for(int i = 0; i < container.size(); i++){

            child = container.getFieldAt(i);

            // add the child field definition only if it isn't already
            // defined by in the parent type
            if(type == null || ! type.contains(child) ){

               fieldToXML( child, path, output, typeTable );
               path.add();

            }

         }

      }

   }


   /**
   * Sets the name and value attributes of the XML node at the given
   * path in the given XML generator.
   */
   private static void setProperty(FieldType field,
                                   FieldContext path,
                                   String name,
                                   Object value,
                                   XMLMessageGenerator output)
                                   throws MessageException{

      // First check to see if the property value differs from the
      // default value for this type of field. If the property value
      // is the same as the default value, then there is no need
      // to write it out to the XML definition, since the default
      // value is adequate.
      Object defaultValue = field.getDefaultPropertyValue(name);

      if(defaultValue == null){
         defaultValue = NULL;
      }

      if(! value.toString().equals( defaultValue.toString() ) ){

         String currentPath = path.getNFPath();
         output.setAttributeValue(currentPath, NAME, name);
         output.setValue(currentPath, value.toString());
         path.add();

      }

   }

   /**
   * Create a new Field of the given type. This method assumes that the
   * class being created has a constructor that takes a single string (name)
   * argument.
   *
   * @param name the name for the new field.
   * @param FieldTypeImpl the class to be created.
   */
   public static Field newInstance(String name, Class fieldType) throws FrameworkException{

       Object[] args = new Object[1];
       args[0] = name;
       return (Field) ObjectFactory.create(fieldType.getName(), args, Field.class);

   }

   /**
   * For testing purposes, this reads field definitions from a given XML
   * file name, creates the corresponding Field objects in memory,
   * prints out the values of these objects, and then converts these
   * objects back to an XML definition and prints out this XML.
   */
   public static void main(String[] args){

      String inputFile = null;

      String usage = "usage: java "+FieldFactory.class.getName()+
                     " [-verbose|-verboseAll] <input xml file>";

      for(int i = 0; i < args.length; i++){

         if( args[i].equals("-verbose") ){

            Debug.enable(Debug.NORMAL_STATUS);
            Debug.enable(Debug.ALL_ERRORS);
            Debug.enable(Debug.ALL_WARNINGS);
            Debug.enable(Debug.MAPPING_LIFECYCLE);
            Debug.enable(Debug.MAPPING_STATUS);
            Debug.enable(Debug.MAPPING_BASE);

         }
         else if( args[i].equals("-verboseAll") ){

            Debug.enableAll();

         }
         else if(inputFile == null){

            inputFile = args[i];

         }

      }

      if(inputFile == null){

         System.err.println(usage);
         System.exit(1);      

      }

      try{

         FieldMap map = FieldFactory.getMap(inputFile);

         Map typesTable = FieldFactory.getTypeMap(inputFile);

         System.out.println( recursiveToString( map ) );

         System.out.println( FieldFactory.fieldToXML( map, typesTable ) );

      }
      catch(Exception ex){

         ex.printStackTrace();

      }

   }

   /**
   * Does a toString() on the given field, and then recurses through all
   * of the field's subfields calling toString() on them as well.
   *
   */
   public static String recursiveToString(Field field){

      StringBuffer result = new StringBuffer();
      recursiveToString( (Object) field, result);
      return result.toString();

   }

   private static void recursiveToString(Object field, StringBuffer buffer){

      buffer.append( field.toString() );

      if(field instanceof FieldContainer){

         FieldContainer container = (FieldContainer) field;

         int count = container.size();

         for(int i = 0; i < count; i++){

            recursiveToString( container.getFieldAt(i), buffer );

         }
         
      }


   }

   /**
   * This map implementation that maintains the order in which its
   * elements are added.
   */
   static class OrderedMap implements Map{

      private List keys = new LinkedList();

      private Map table = new HashMap();

      private Comparator sorter = new OrderComparator();

      public void clear(){

         keys.clear();
         table.clear();

      }

      public boolean containsKey(Object key){

         return table.containsKey(key);

      }

      public boolean containsValue(Object value){

         return table.containsValue(value);

      }

      public Set entrySet(){

         Set set = new TreeSet( sorter  );
         set.add(  table.entrySet() );

         return set;

      }

      public boolean equals(Object o){

         if(o instanceof OrderedMap){

            OrderedMap compareMe = (OrderedMap) o;
            return (compareMe.keys.equals(this.keys) &&
                    compareMe.table.equals(this.table) );

         }

         return false;

      }

      public Object get(Object key){

         return table.get(key);

      }

      public boolean isEmpty(){

         return table.isEmpty();

      }


      public Set keySet(){

         Comparator noSorting = new Comparator(){

            public int compare(Object o1, Object o2){
               return 0;
            }

         };

         Set keySet = new TreeSet( noSorting );
         keySet.addAll(keys);
         return keySet;

      }

      public Object put(Object key, Object value){

         Object result = table.put(key, value);
         if(result == null){
            // add the key if there was no value previously associated with
            // this key
            keys.add(key);
         }

         return result;

      }

      public void putAll(Map t){

         Iterator keyIter = t.keySet().iterator();

         Object key;
         while(keyIter.hasNext()){

            key = keyIter.next();
            put(key, t.get(key));

         }

      }

      public Object remove(Object key){

         keys.remove(key);
         return table.remove(key);

      }

      public int size(){

         return table.size();

      }

      /**
      * Returns the collection of values in the order in which they
      * were added to this table.
      */
      public Collection values(){

         Collection values = new ArrayList( size() );

         for(int i = 0; i < keys.size(); i++){

            values.add( get(keys.get(i)) );

         }

         return values;

      }

      public String toString(){

         StringBuffer result = new StringBuffer("{"); 

         for(int i = 0; i < keys.size(); i++){

            result.append(keys.get(i));
            result.append( get(keys.get(i)) );

         }

         result.append("}");

         return result.toString();

      }

      // used to sort the key set
      class OrderComparator implements Comparator{

         public int compare(Object o1, Object o2){

            if(o1 instanceof Map.Entry){
               o1 = ((Map.Entry) o1).getKey();
            }

            if(o2 instanceof Map.Entry){
               o2 = ((Map.Entry) o2).getKey();
            }

            int index1 = OrderedMap.this.keys.indexOf(o1);
            int index2 = OrderedMap.this.keys.indexOf(o2);

            return  index1 - index2;

         }

      }      

   }

   /**
   * This class handles the caching and flushing of FieldMap instances.
   */
   static class FieldMapCache implements CachingObject{

       /**
       * The map where the FieldMaps will be cached based on their meta file
       * name.
       */
       private Map cache = new Hashtable();


       /**
       * Adds a field map to the cached keyed by its source file name.
       */
       public void put(String metaFileName, FieldMap field){

          cache.put(metaFileName, field);

       }

       /**
       * Retrieves a cached FieldMap based on its meta file name.
       */
       public FieldMap get(String metaFileName){

          return (FieldMap) cache.get(metaFileName);

       }

       /**
        * This clears the map cache.
        * This method is invoked by the CacheManager.
        *
        * @exception FrameworkException required by the interface,
        *                               but not thrown by this implementation.
        */
       public void flushCache ( ) throws FrameworkException{

          cache.clear();

       }

       public String toString(){

          return this.getClass().getName()+":"+cache;

       }

   }

}
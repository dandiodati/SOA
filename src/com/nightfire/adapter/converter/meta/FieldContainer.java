/**
* Copyright (c) 2001, 2002 NightFire Software, Inc. All rights reserved.
*/

package com.nightfire.adapter.converter.meta;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.util.xml.XPathAccessor;

import org.w3c.dom.*;

/**
* A Field Container is a field that contains other child fields.
*/
public class FieldContainer extends SimpleField{

   /**
   * The list of child fields.
   */
   protected List fields = new ArrayList();

   /**
   * Creates a field container with the given name. The name is also used
   * as the path as in SimpleField.
   */
   public FieldContainer(String name){

      super(name);

   }

   /**
   * Creates a field container with the given name and path.
   */   
   public FieldContainer(String name, String path){

      super(name, path);

   }

   /**
   * Adds a child field to this container.
   *
   * @param field the field to be added.
   */
   public void add(Field field){

      fields.add(field);

   }

   /**
   * Removes a child field from this container.
   *
   * @param field the field to be remove.
   * @returns true if the field was succesfully removed, false if the field
   *          was not a child of this container. 
   */
   public boolean remove(Field field){

      return fields.remove(field); 

   }

   /**
   * Gets the field at the given index.
   *
   */
   public Field getFieldAt(int index){

      return (Field) fields.get(index);   

   }

   /**
   * Returns the current number of fields in this container.
   */
   public int size(){

      return fields.size();

   }

   /**
   * For the given path, this visits all of the subfields of this container.
   */
   public void visit(FieldContext  currentPath,
                     XPathAccessor source,
                     StringBuffer  output){

      // iterate through sub-fields
      for(int j = 0; j < size(); j++){

         visit(currentPath, source, output, j);   

      }

   }

   protected void visit(FieldContext  currentPath,
                        XPathAccessor source,
                        StringBuffer output,
                        int fieldIndex){

         Field currentField = getFieldAt(fieldIndex);

         if( Debug.isLevelEnabled(Debug.MAPPING_STATUS) ){

            Debug.log(Debug.MAPPING_STATUS, "Visiting field ["+
                                            currentField.getName()+
                                            "] for path ["+currentPath+"]");

         }

         currentField.write(currentPath, source, output);

   }

   /**
   * This passes of the current context and input source to all of this
   * container's child fields and their subfields to format the incoming
   * XML data.
   *
   * @param currentContext the XML path context.
   * @param source the parsed XML source.
   * @param output the buffer to which output will be appended.
   */
   public void write(FieldContext  currentContext,
                     XPathAccessor source,
                     StringBuffer  output ){

      if(! StringUtils.hasValue(path) ){


         // if there is no path for this field, then just
         // pass the given context to each of the child fields
         visit(currentContext, source, output);

      }
      else{


         // if this container does have a path, visit each node
         // that matches that path

         currentContext = getFullPath(currentContext);
         String xPath =  currentContext.getXPath();

         if( currentContext.isEmpty() ){

            visit( currentContext, source, output );

         }
         else{

            // Iterate through all existing paths at this level
            while( source.nodeExists( xPath ) ){

               visit( currentContext, source, output );
               currentContext.add();
               xPath =  currentContext.getXPath();

            }

         }

      }

   }

   /**
   * Appends this container's path to the context (by calling <code>
   * getFullPath()</code>) and passes the
   * given value off to each of the sub fields for XML generation.
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output){

      FieldContext fullPath = getFullPath(context);
      writeWithFullPath(fullPath, value, output);

   }

   /**
   * Appends this container's path and given index to the context
   * and passes the new context and the
   * given value off to each of the sub fields for XML generation.
   * So for example, if the path for this container is "foo" and the
   * given index value is 3, then "foo(3)" will be appended to the
   * current context. 
   *
   * @param context The current XML path to use as a context.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.
   * @param index This is a zero-based index used to create an indexed
   *              path to tell the XML Generator which output node to
   *              use in the case where these nodes repeat. 
   */
   public void write(FieldContext context,
                     String value,
                     XMLMessageGenerator output,
                     int index){

      FieldContext fullPath = getFullPath(context);
      fullPath.add(index);
      writeWithFullPath(fullPath, value, output);                     

   }

   /**
   * Takes the given context, as is, and passes the context and the
   * given value off to each of the sub fields for XML generation.
   *
   * @param fullPath The full path to the output node.
   * @param value The value for the field.
   * @param output The XML generator to which the field should be written.   
   */
   protected void writeWithFullPath(FieldContext fullPath,
                                    String value,
                                    XMLMessageGenerator output){

      Field currentField;

      for(int j = 0; j < size(); j++){

          currentField = getFieldAt(j);
          currentField.write(fullPath, value, output);

      }                                 

   }

   /**
   * Returns a string listing the property names and values
   * plus a list of its child
   * fields.
   */
   public String toString(){

      StringBuffer result = new StringBuffer(super.toString());
      result.append("contains:\t[");

      for(int j = 0; j < size(); j++){

          if(j > 0) result.append(", ");
          result.append( getFieldAt(j).getName() );

      }

      result.append("]\n");

      return result.toString();

   }

   /**
   * Creates a copy of this Field with the same  
   * type and then calls the static <code>copyProperties()</code> method
   * to copy all of this field's properties into the new instance.
   * Then copies of all child fields are added to the new instance.
   */
   public Field copy(){

      FieldContainer newContainer = (FieldContainer) super.copy();

      for(int i = 0; i < size(); i++){

         // add a copy of the current child field to the new container
         newContainer.add( getFieldAt(i).copy() );

      }

      return newContainer;

   }

   /**
   * Creates a new Field of this
   * type and then calls the static <code>copyProperties()</code> method
   * to copy all of this field's properties into the new instance.
   * Then copies of all child fields are added to the new instance.
   */
   public Field newInstance(String newName) throws FrameworkException{

      FieldContainer newContainer = (FieldContainer) super.newInstance(newName);

      for(int i = 0; i < size(); i++){

         // add a copy of the current child field to the new container
         Field child = getFieldAt(i);
         newContainer.add( child.copy() );

      }

      return newContainer;

   }

   /**
   * Returns true if this container contains the given childField.
   */
   public boolean contains(Field childField){

      return fields.contains(childField);

   }

}
/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.beans;

import com.nightfire.webgui.core.*;

import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.message.common.xml.*;
import  javax.servlet.ServletException;

import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.parser.xml.*;
import  com.nightfire.framework.message.generator.xml.*;
import  com.nightfire.framework.constants.PlatformConstants;
import  com.nightfire.framework.message.*;
import com.nightfire.framework.debug.*;

import java.util.*;

import com.nightfire.framework.message.transformer.*;

/**
 * An XML implementation of the NFBean, which  holds data as XMLGenerators.
 * By default it creates XMLPlainGenerator, but other types can be passed in.
 */
 
public class XMLBean implements NFBean
{

    /**
     * The xmlgenerator which hold the header xml.
     */
    private XMLGenerator headerDataGen;


    // map representation of the current header generator
    private Map headerDataMap = null;


    private String id = "";

    private boolean dirty = false;
    

    /**
     * The xmlgenerator which hold the body xml.
     */
    private XMLGenerator bodyDataGen;

     // map representation of the current body generator
    private Map bodyDataMap = null;

    private Object headerTransform =null, bodyTransform = null;

  protected DebugLogger log;
  
    public XMLBean() throws ServletException
    {
       this((String)null, (String) null);
    }



     /**
     * Sets the id of this bean.
     *
     * @param id The id to identify this bean
     *
     */
    public void setId(String id)
    {
       this.id = id;
    }


    /**
     * Returns the id of this bean.
     * @param The id.
     */
    public String getId()
    {
       return id;
    }

    /**
     * Indicates if this bean's data is currently dirty/out of sync.
     *
     * @return true if the data id dirty
     */
    public boolean getDirty() 
    {
        return dirty;
    }
    
    /**
     * Sets a flag to indicate if this bean's data is dirty/out of sync.
     *
     * @param dirty true means dirty, false mean ok.
     */
    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }
    

    /**
     * Constructor.
     *
     * @param  headerData  Map of header fields.
     * @param  bodyData    Map of body fields.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public XMLBean(Map headerData, Map bodyData) throws ServletException
    {
       this();
       addToHeaderData(headerData);
       addToBodyData(bodyData);
    }


    /**
     * Constructor.
     *
     * @param  headerData  XML header document.
     * @param  bodyData    XML body document.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public XMLBean(String headerData, String bodyData) throws ServletException
    {
        log = DebugLogger.getLoggerLastApp(getClass() ); 
        try
        {
            if (StringUtils.hasValue(headerData))
                headerDataGen = new XMLPlainGenerator(headerData);
            else
                headerDataGen = new XMLPlainGenerator(PlatformConstants.HEADER_NODE);

            if (StringUtils.hasValue(bodyData))
                bodyDataGen = new XMLPlainGenerator(bodyData);
            else
                bodyDataGen = new XMLPlainGenerator(PlatformConstants.BODY_NODE);


        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: XMLBean.XMLBean(): Failed to create an XMLBean:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }
    
    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public XMLBean(XMLGenerator header, XMLGenerator body) throws ServletException
    {
       log = DebugLogger.getLoggerLastApp(getClass() );
       try {
           if (header != null)
              headerDataGen = (XMLGenerator)header;
           else
              headerDataGen = new XMLPlainGenerator(PlatformConstants.HEADER_NODE);
        
           if (body != null)
              bodyDataGen = (XMLGenerator) body;
           else
              bodyDataGen = new XMLPlainGenerator(PlatformConstants.BODY_NODE);

 
        } catch (Exception e) {
            String errorMessage = "ERROR: XMLBean.XMLBean(): Failed to create an XMLBean:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }


    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public XMLBean(Document header, Document body) throws ServletException
    {
         log = DebugLogger.getLoggerLastApp(getClass() );
      
         try
        {
            if (header != null)
                headerDataGen = new XMLPlainGenerator(header);
            else
                headerDataGen = new XMLPlainGenerator(PlatformConstants.HEADER_NODE);

            if (body != null)
                bodyDataGen = new XMLPlainGenerator(body);
            else
                bodyDataGen = new XMLPlainGenerator(PlatformConstants.BODY_NODE);


        }
        catch (Exception e) {
            String errorMessage = "ERROR: XMLBean.XMLBean(): Failed to create an XMLBean:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }
    
    /**
     * Obtain the header data structure object.
     *
     *
     * @return  XMLGenerator object.
     */
    public Object getHeaderDataSource()
    {
        return headerDataGen;
    }
    
    /**
     * Obtain the body data structure object.
     *
     * @return  XMLGenerator object.
     */
    public Object getBodyDataSource()
    {
        return bodyDataGen;
    }


    public void setHeaderDataSource(Object headerDataSource)
    {

        XMLGenerator old = this.headerDataGen;
        
        headerDataGen = (XMLGenerator)headerDataSource;

        // Add the new generator to the generator group if one exists
        // if this is not done, then extra template will be left in the bundle's template
        // group
        XMLGeneratorGroup grp = old.getGroup();
        try {
            if ( grp != null) {
                String name = grp.getGeneratorName(old);
                grp.setGenerator(name, (XMLGenerator)headerDataSource);
            }
        } catch (MessageException e ) {
            log.error("Could not set new header generator on parent group : " + e.getMessage());
        }



       
    }


    public void setBodyDataSource(Object bodyDataSource)
    {

        XMLGenerator old = this.bodyDataGen;
        
              
        // Add the new generator to the generator group if one exists
        // if this is not done, then extra template will be left in the bundle's template
        // group
        XMLGeneratorGroup grp = old.getGroup();
        try {
            if ( grp != null) {
                String name = grp.getGeneratorName(old);
                grp.setGenerator(name, (XMLGenerator)bodyDataSource);
            }
        } catch (MessageException e ) {
            log.error("Could not set new generator on parent group : " + e.getMessage());
        }
        bodyDataGen = (XMLGenerator)bodyDataSource;
    }




    /**
     * Set a header field value.
     * If the key started with ServletConstants.NF_FIELD_HEADER_PREFIX it is stripped off.
     */
    public void setHeaderValue(String key, String value) throws ServletException
    {
       try {
          headerDataGen.setValue(stripKey(key, ServletConstants.NF_FIELD_HEADER_PREFIX), value);
       } catch ( MessageException e) {
          String err =  "XMLBean: Could not set header key [" + key + "] with value [" + value + "]: " + e.getMessage();
          log.error( err);
          throw new ServletException (err);
       }
    }

    /**
     * Set a body field value.
     * If the key started with ServletConstants.NF_FIELD_PREFIX it is stripped off.
     */
    public void setBodyValue(String key, String value) throws ServletException
    {
       try {
          key = stripKey(key, ServletConstants.NF_FIELD_PREFIX);


          // sets the value on the node
          bodyDataGen.setValue(key, value);

          // set the path to the field as an id for the field.
          bodyDataGen.setAttribute(bodyDataGen.getNode(key), "id", key);

       } catch (MessageException e) {
          String err =  "XMLBean: Could not set body key [" + key + "] with value [" + value + "]: " + e.getMessage();
          log.error( err);
          throw new ServletException (err);
       }
    }

    /**
     * Get a header field value.
     * If the key started with ServletConstants.NF_FIELD_HEADER_PREFIX it is stripped off.
     */
    public String getHeaderValue(String key)
    {
       key = stripKey(key, ServletConstants.NF_FIELD_HEADER_PREFIX);
       if (!headerDataGen.exists(key) )
          return "";

       try {
          return headerDataGen.getValue(key);
       } catch (MessageException e) {
         return "";
       }
    }

    /**
     * Get a body field value.
     * If the key started with ServletConstants.NF_FIELD_PREFIX it is stripped off.
     */
    public String getBodyValue(String key)
    {

       key = stripKey(key, ServletConstants.NF_FIELD_PREFIX);
       if (!bodyDataGen.exists(key) )
          return "";


       try {
          return bodyDataGen.getValue(key);
       } catch (MessageException e) {
          return "";
       }

    }


    
    /**
     * Add header data to this bean. All existing keys in the bean are replaced
     * by the same key from the map.
     * All fields starting with ServletConstants.NF_FIELD_HEADER_PREFIX are added as header fields.
     * All fields starting with ServletConstants.NF_FIELD_PREFIX are added as body fields.
     * All other fields are ignored.
     *
     * @param  Data -  Map of data
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void addToHeaderData(Object data) throws ServletException
    {
       if (!(data instanceof Map))
         throw new ServletException("XMLBean: addToHeaderData - Only Map objects are supported");


        try {
               Iterator iter = ((Map)data).entrySet().iterator();

               while (iter.hasNext() ) {
                  Map.Entry entry = (Map.Entry) iter.next();
                  String key = (String)entry.getKey();
                  String value = (String) entry.getValue();
                  setHeaderValue(key, value);
               }

        }
        catch (Exception e) {
            String errorMessage = "ERROR: XMLBean.addToHeaderData(): Failed to add the specified header data to the Bean:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }

    /**
     * Add body data to this bean. All existing keys in the bean are replaced
     * by the same key from the Object.
     * All fields starting with ServletConstants.NF_FIELD_HEADER_PREFIX are added as header fields.
     * All fields starting with ServletConstants.NF_FIELD_PREFIX are added as body fields.
     * All other fields are ignored.
     *
     * @param  Data -  Map of data
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void addToBodyData(Object data) throws ServletException
    {
        if (! (data instanceof Map) )
         throw new ServletException("XMLBean: addToHeaderData - Only Map objects are supported");


        try {
               Iterator iter = ((Map)data).entrySet().iterator();

               while (iter.hasNext() ) {
                  Map.Entry entry = (Map.Entry) iter.next();
                  String key = (String)entry.getKey();
                  String value = (String) entry.getValue();
                  setBodyValue(key, value);
               }

        }
        catch (Exception e) {
            String errorMessage = "ERROR: XMLBean.addToBodyData(): Failed to add the specified body data to the Bean:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }


    /**
     * Obtains the response code given in the header data.  This is applicable only
     * in the case where the Bean encapsulates the response data.
     *
     * @return  Response code.
     */
    public String getResponseCode()
    {
        try {
            if (headerDataGen != null) {
                return headerDataGen.getValue(PlatformConstants.RESPONSE_CODE_NODE);
            }
        }
        catch (Exception e) {
          log.warn("XMLBean.getResponseCode(): Failed to locate the response-code node [" + PlatformConstants.RESPONSE_CODE_NODE + "] in the XML header document:\n" + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Convenient method for displaying the string representation of the header data.
     *
     * @return  Header data string.
     */
    public String describeHeaderData()
    {
        try
        {
            if (headerDataGen != null)
            {
                return headerDataGen.describe();
            }
        }
        catch (Exception e)
        {
          log.error("XMLBean.describeHeaderData(): Failed to generate the XML header document:\n" + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Convenient method for displaying the string representation of the body data.
     *
     * @return  Body data string.
     */
    public String describeBodyData()
    {
        try
        {
            if (bodyDataGen != null)
            {
                return bodyDataGen.describe();
            }
        }
        catch (Exception e)
        {
          log.error("XMLBean.describeBodyData(): Failed to generate the XML body document:\n" + e.getMessage());
        }
        
        return null;
    }



    
    /**
     * returns the header data as a map object
     */
    public Map getHeaderAsMap()
    {
       if ( headerDataMap == null)
          headerDataMap = new BeanMap(headerDataGen, ServletConstants.NF_FIELD_HEADER_PREFIX);

       return headerDataMap;

    }



    /**
     * returns the header data as a map object
     */
    public Map getBodyAsMap()
    {
       if ( bodyDataMap == null)
          bodyDataMap = new BeanMap(bodyDataGen, ServletConstants.NF_FIELD_PREFIX);

       return bodyDataMap;
    }

    /**
     * inner map object which wraps the generator.
     */
   private class BeanMap extends AbstractMap
   {
       private XMLGenerator gen;
       private String prefix;
      
       public BeanMap(XMLGenerator gen, String prefix)
       {
         this.gen = gen;
         this.prefix = prefix;
       }

      public Set entrySet()
      {
         throw new UnsupportedOperationException("Method not supported");
      }


      public boolean containsKey(Object key)
      {
         String k = stripKey((String) key, prefix);
         boolean exists = gen.exists(k);
         log.debug("Exists field ["+ k +"]?, " + exists );
         return exists;
      }


      public Object get(Object key)
      {
         String value = null;
         String k = null;
         try {
            k = stripKey((String) key, prefix);
            if (gen.exists(k) )
               value = gen.getValue(k);
         } catch (MessageException e) {
         }
         return value;
      }


      public Object put(Object key, Object value) throws UnsupportedOperationException
      {
         String oldValue = null;

         String valueStr = (String) value;

         String k = null;
         try {
            k = stripKey((String) key, prefix);
            if (gen.exists(k) )
               oldValue = gen.getValue(k);

            gen.setValue(k, valueStr);
         } catch (MessageException e) {
         }
         log.debug("Setting field [" + k + "] with value [" + valueStr +"].");

         return oldValue;
      }

   }



   private String stripKey(String key, String prefix)

      {
         String k = key;
         if (k != null && k.startsWith(prefix) )
           k = k.substring(prefix.length());

         return k;
      }



    public NFBean getFinalCopy() throws MessageException

    {


       XMLBean copy = null;

       try {


          if ( log.isDebugEnabled() )
          {
             log.debug("creating a new copy of [" + this.getClass() +"]" );
          }
          copy = (XMLBean) this.getClass().newInstance();

          copy.setId(getId());
          // make a deep copy of the xml header and body
          copy.bodyDataGen = bodyDataGen.getOutputCopy();
          copy.headerDataGen = headerDataGen.getOutputCopy();

          // make a reference to the current transform objects
          // since it can be reused.
          copy.headerTransform = headerTransform;
          copy.bodyTransform = bodyTransform;

          // if there is a group associated with this group then create a 
          // copy of the group for the copy to reference
          // this assumes that if the header generator is in the group then the body generator
          // will always be in the group.
          if (bodyDataGen.getGroup() != null) {
              
              XMLGeneratorGroup newGroup = new XMLGeneratorGroup();

              // create a shallow copy the the original generator group
              //
              Iterator iter = bodyDataGen.getGroup().getGeneratorMap().entrySet().iterator();
              while (iter.hasNext()) {
                  Map.Entry e = (Map.Entry)iter.next();
                  String id = (String)e.getKey();
                  
                  XMLGenerator gen = (XMLGenerator) e.getValue();
                  // replace the old generator with the new copy in this new group
                  // this call will also set the group on the new generator
                  // This all adds other generators from the old group into this
                  // copied one.
                  if (gen == bodyDataGen)
                      newGroup.setGenerator(id, copy.bodyDataGen);
                  else if (gen == headerDataGen)
                      newGroup.setGenerator(id, copy.headerDataGen);
                  else
                      newGroup.setGenerator(id, gen);
                  
              }
          }
          

       } catch (Exception e ) {

          log.error("Failed to create final output bean: " + e.getMessage(),e );

          throw new MessageException(e.getMessage() );

       }

       return copy;

    }



    /**
     * Removes the indicated field from the header data.
     *
     * @param  field  The field to be removed.
     *
     * @return  true if the field existed and was removed, false otherwise.
     */
    public boolean removeHeaderField(String field)
    {
        try
        {   
            headerDataGen.remove(stripKey(field, ServletConstants.NF_FIELD_HEADER_PREFIX));     
        }
        catch (Exception e)
        {
            log.warn("removeHeaderField(): Failed to remove header field [" + field + "].  It may not exist.");
            
            return false;
        }
                
        if (log.isDebugEnabled())
        {
            log.debug("removeHeaderField(): Header field [" + field + "] has been removed successfully.");
        }
        
        return true;
    }





    /**
     * Sets a header transform object which can be of type XMLFilter or XSLMessageTransformer.
     * Passing a null transform will disable the header transformation
     */
    public void setHeaderTransform(Object transform) throws ServletException
    {
        if ( transform != null )
        {
            if ( transform instanceof XMLFilter || transform instanceof XSLMessageTransformer ) {
                headerTransform  = transform;
            } else
                throw new ServletException("XMLBean Invalid transform object, must be of type XSLFilter or XSLMessageTransformer");
        }
        else
        {
            headerTransform = null;
        }
    }

     /**
     * Sets a body transform object which can be of type XMLFilter or XSLMessageTransformer.
     * Passing a null transform will disable the body transformation
     */
    public void setBodyTransform(Object transform) throws ServletException
    {

        if ( transform != null )
        {
            if ( transform instanceof XMLFilter || transform instanceof XSLMessageTransformer ) {
                bodyTransform  = transform;
            } else
                throw new ServletException("XMLBean Invalid transform object, must be of type XMLFilter or XSLMessageTransformer");
        }
        else
        {
            bodyTransform = null;
        }
    }


    public Object getHeaderTransform()
    {
       return headerTransform;
    }


    public Object getBodyTransform()
    {
       return bodyTransform;
    }

    public void transform() throws MessageException
    {
        if (log.isDebugDataEnabled())
        {
	        log.debugData("transform(): Header data before transformation:\n" + headerDataGen.describe());
	    }

        // to maintain the generator group we obtain from the current
        // generator first
        // then perform the transformation and reset it.
        XMLGenerator oldBody = (XMLGenerator)getBodyDataSource();
        XMLGenerator oldHead = (XMLGenerator)getHeaderDataSource();
       


        if (headerTransform != null)
        {
            if (headerTransform instanceof XMLFilter)
            {
                headerDataGen = ((XMLFilter)headerTransform).filter(headerDataGen);
            }
            else if (headerTransform instanceof XSLMessageTransformer)
            {
                // Create a new XML document that "wraps" both the header and the body
                // to allow inspection of body fields in order to form the more "complex"
                // header fields.
                
                XMLGenerator headerAndBodyData = new XMLPlainGenerator("Data");
                
                Node         rootNode          = headerAndBodyData.getDocument().getDocumentElement();
                
                headerAndBodyData.copy(rootNode, headerDataGen.getDocument().getDocumentElement());
                
                headerAndBodyData.copy(rootNode, bodyDataGen.getDocument().getDocumentElement());
                
                if (log.isDebugDataEnabled())
                {
	                log.debugData("transform(): Combined header and body XML data used in the header transformation:\n" + headerAndBodyData.describe());
	            }
                
                headerDataGen = new XMLPlainGenerator(((XSLMessageTransformer)headerTransform).transform(headerAndBodyData.getOutputDOM()));
            }
            
            if (log.isDebugDataEnabled())
            {
                log.debugData("transform(): Header data after transformation:\n" + headerDataGen.describe());
            }
        }
        else
        {
            log.debug("transform(): No header transformer exists.  Skipping header transformation ...");
        }

        if (log.isDebugDataEnabled())
        {
            log.debugData("transform(): Body data before transformation:\n" + bodyDataGen.describe());
        }

        if (bodyTransform != null)
        {
            if (bodyTransform instanceof XMLFilter)
            {
                bodyDataGen = ((XMLFilter)bodyTransform).filter(bodyDataGen);
            }
            else if (bodyTransform instanceof XSLMessageTransformer)
            {
                bodyDataGen = new XMLPlainGenerator(((XSLMessageTransformer)bodyTransform).transform(bodyDataGen.getOutputDOM()));
            }	
            
            if (log.isDebugDataEnabled())
            {
                log.debugData("transform(): Body data after transformation:\n" + bodyDataGen.describe());
    	    }
        }
        else
        {
            log.debug("transform(): No body transformer exists.  Skipping body transformation ...");
        }


 
        //now we get the new transformed data and add it back in the group
       
        XMLGeneratorGroup grpBody = oldBody.getGroup();
        XMLGeneratorGroup grpHead = oldHead.getGroup();
        
               
       try {
          if ( grpBody != null) {
             String name = grpBody.getGeneratorName(oldBody);
             grpBody.setGenerator(name, bodyDataGen);
          }
          
          if (grpHead != null) {
             String name = grpHead.getGeneratorName(oldHead);
             grpHead.setGenerator(name, headerDataGen);
          }
              
       } catch (MessageException e ) {
          log.error("Could not set transformed generator on parent group : " + e.getMessage());
       }

    }

    /**
     * Clears all transform objects set on this bean
     */
    public void clearTransforms()
    {
       headerTransform = null;
       bodyTransform = null;
    }

}

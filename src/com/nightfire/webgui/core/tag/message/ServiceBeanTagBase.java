/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;


import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.xml.*;

/**
 * A tag that acts as a factory for creating service related beans.
 * These beans will always have supplier,version and a service type associated with them.
 *
 */
public abstract class ServiceBeanTagBase extends BeanTagBase
{


    private boolean toGuiXML = true;

    private boolean copy = false;
    
    private String supplier = null;

    private String version = null;
    
    // set from child implementation or from header field in existing bean
    private String serviceType = null;
    

    protected Object service = null;
        
   /**
    * The xml document to add
    * to the session buffer.
    */
    private Object data;
    

     /**
     * The service to use
     */
    public void setService(Object service) throws JspException
    {
        setDynAttribute("service", service, Object.class);
        
    }

     /**
     * The version to use
     */
    public void setVersion(String version) throws JspException
    {
        setDynAttribute("version", version, String.class);
    }

    
    
    public String getVersion()
    {
        return version;
    }
    



    /**
     * Provides the supplier for this data.
     * @param supplier the supplier associated with this data.
     *
     */
    public void setSupplier(String supplier) throws JspException
    {
       setDynAttribute("supplier", supplier, String.class);  
    }

    public String getSupplier()
    {
        return supplier;
    }

    

     /**
     * The xml to use
     */
    public void setData(Object data) throws JspException
    {
        setDynAttribute("data", data, Object.class);
    }


     /**
     * Indicates if the xml data passed in need to be transformed to gui xml.
     * Be default this is set to false.
     *
     */
    public void setToGuiXML(String bool) throws JspException
    {
        setDynAttribute("toGuiXML", bool, String.class);
    }



     /**
     * Indicates if the data passed in should be copied before any modifications occur.
     *
     */
    public void setCopy(String bool) throws JspException
    {
        setDynAttribute("copy", bool, String.class);
    }

    public void setMessageType(String msgType) throws JspException    
    {
        setDynAttribute("messageType", msgType, String.class);
    }
    
    /**
     * Returns the service type for this bean.
     *
     * @return a <code>String</code> value
     */
    public abstract String getServiceType();

    /**
     * Returns the message type object based on id. This should
     * return a default messagetype if it cannot find any with the specified id.
     *
     * @param bean a <code>XMLBean</code> value
     * @return a MessageTypeDef object.
     */
    public abstract MessageTypeDef getMessageType(String id) throws JspTagException;
    
    

    
    /**
     * Returns the message type object associated with the bean. This should
     * return a default messagetype if it can not determine one.
     *
     * @param bean a <code>XMLBean</code> value
     * @return a MessageTypeDef object.
     */
    public abstract MessageTypeDef getMessageType(XMLBean bean) ;


    public String getMessageTypeName()
    {
        return getBean().getHeaderValue(ServletConstants.MESSAGE_TYPE);
    }
    
    public int doStartTag() throws JspException
    {

        // we have to initialize service first since child classes check the type of service object.
        service = getDynAttribute("service");

        if (service == null) {
            throw new JspTagException("Null Service object passed in");
        }

        

        super.doStartTag();
        
        return EVAL_BODY_INCLUDE;
        
    }
    
 


    public NFBean createBean() throws JspException
    {

        XMLBean bean = null;


        supplier = (String) getDynAttribute("supplier");
        
        version = (String)getDynAttribute("version");
        

        data = (Object) getDynAttribute("data");

        String msgType = (String)getDynAttribute("messageType");
        

        
        toGuiXML = StringUtils.getBoolean((String)getDynAttribute("toGuiXML"), true);

        copy = StringUtils.getBoolean((String)getDynAttribute("copy"), false);


        if (copy == true && data == null) {
          log.error("Copy can only be used when data is not null");
          throw new JspTagException("Copy can only be used when data is not null");
        }
        
        
        if (toGuiXML == false && data != null && !(data instanceof XMLBean)) {
            log.error("When toGuiXML is false, then a XMLBean with the gui xml must be passed in");
            throw new JspTagException("When toGuiXML is false, then a XMLBean with the gui xml must be passed in");
        }

       

        serviceType = getServiceType();
        




        if (!StringUtils.hasValue(supplier) && data == null) {
            log.error("supplier value is null.");
            throw new JspTagException("supplier is a required attribute when data does not exist.");
        }

        if (!StringUtils.hasValue(version) && data == null) {
            log.error("version value is null.");
            throw new JspTagException("version is a required attribute when data does not exist.");
        }



        
        try {
            

            if ( data != null) {
                if ( data instanceof XMLBean) {
                    bean = (XMLBean) data;
                }
                else if (data instanceof XMLGenerator) {
                    bean = createNewBean((XMLGenerator) data);
                }
                else if (data instanceof Document) {
                    bean = createNewBean(new XMLPlainGenerator((Document) data));
                }
                else if (data instanceof String) {
                    bean = createNewBean (new XMLPlainGenerator((String)data));
                    // if this is a string then it is already a copy so set can set the 
                    // copy to false because it does not matter.
                    copy = false;
                }
                else
                    throw new JspTagException("Invalid type [" + data.getClass() +"] passed in for data attribute. Must be of type String,Document,XMLGenerator, or XMLBean.");
          
            }
            else
                bean = createNewBean(null);
        
       

        
            // the only disadvantage of this is that it will also finalize 
            // and xml templates in the bean
            if (copy == true)
                bean = (XMLBean)bean.getFinalCopy();

            // if this is a bean with non gui xml
            // then remove the message type if it exists to
            // force evaluation for body again.
            // else if this is not to gui xml(used when is a new bean or 
            // gui xml already) and a message type is specied then use it.
            if (toGuiXML == true) {
                if (StringUtils.hasValue(bean.getHeaderValue(ServletConstants.MESSAGE_TYPE)))
                    bean.removeHeaderField(ServletConstants.MESSAGE_TYPE);
            } else if (StringUtils.hasValue(msgType)) {
                bean.setHeaderValue(ServletConstants.MESSAGE_TYPE, msgType);
            }
            
            

            // set needed header fields into created bean
            setHeaderValues(bean);
        
            transformData(bean);
        

            return bean;

        } catch (Exception e) {
            log.error(e.getMessage());
            
            throw new JspException(e.getMessage(), e);
        }
        
    }

    

    /**
     * Runs an incoming xsl script if that data is not in gui xml format.
     * otherwise leaves the data as is.
     *
     * @param bean a <code>XMLBean</code> value
     * @exception JspException if an error occurs
     */
    private void transformData(XMLBean bean) throws JspException
    {

        try {

            String id = bean.getHeaderValue(ServletConstants.MESSAGE_TYPE);
            
            MessageTypeDef mType = null;
            

            if (StringUtils.hasValue(id))
                mType = getMessageType(id);
            else
                mType = getMessageType(bean);

                if (mType == null ) {
                    log.error("Could not associate a message type for bean request data:\n" + bean.describeHeaderData() + "\n" +  bean.describeBodyData());
                    throw new JspTagException("Could not determine message type");
                }

                String messageType = mType.getName();
               
                
                bean.setHeaderValue(ServletConstants.MESSAGE_TYPE, messageType);

            
            if (toGuiXML) {


                //Set incoming transformer.
                String incomingBodyTransformPath = ServletUtils.getTransformResourcePath(serviceType, supplier, version, ServletUtils.TRANSFORM_DIR_IN, messageType);

                if (log.isDebugEnabled())
                    log.debug("transformData: Getting incoming body-transformer object from [" + incomingBodyTransformPath + "] ...");
                   

                Object bodyTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), incomingBodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);


                bean.setBodyTransform(bodyTransformer);

                log.debug("Executing incoming body transform object on the bean ...");
                bean.transform();
                bean.clearTransforms();
            }
           
        } catch (Exception e) {
            log.error("Failed to transform data on bean : " +  e.getMessage() );
            throw new JspException(e.getMessage(), e);
        }
    }
    

    protected void setHeaderValues(XMLBean bean) throws Exception {

        // if any of the header fields exist with in the bean then use those
        // otherwise use ones that were passed in or obtaine from a child implementation
        //
        XMLGenerator gen = (XMLGenerator) bean.getHeaderDataSource();
        
        if (gen.exists(ServletConstants.SERVICE_TYPE))
            serviceType = bean.getHeaderValue(ServletConstants.SERVICE_TYPE);

        if (gen.exists(ServletConstants.SUPPLIER))
            supplier = bean.getHeaderValue(ServletConstants.SUPPLIER);

        if (gen.exists(ServletConstants.INTERFACE_VER))
            version = bean.getHeaderValue(ServletConstants.INTERFACE_VER);
        
        
        bean.setHeaderValue(ServletConstants.SERVICE_TYPE, serviceType);
        bean.setHeaderValue(ServletConstants.SUPPLIER, supplier);
        bean.setHeaderValue(ServletConstants.INTERFACE_VER, version);

    }
    
       
    /**
     * Creates a bean using the data passed in or creates a new instance of a bean.
     *
     * @param bodyData a <code>XMLGenerator</code> value
     * @return a <code>XMLBean</code> value
     */
    protected abstract XMLBean createNewBean(XMLGenerator bodyData) throws JspException;
    
    
    
    

    public void release() 
    {
        super.release();
        
        supplier = null;
        service = null;
        data = null;
        version = null;
        copy = false;
        toGuiXML = true;
        copy = false;
    }
    
        
        
}

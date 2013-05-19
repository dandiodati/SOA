/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;

import  com.nightfire.webgui.manager.svcmeta.*;
import  com.nightfire.webgui.manager.beans.*;
import  com.nightfire.webgui.core.svcmeta.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.core.tag.*;
import com.nightfire.framework.message.common.xml.*;
import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.manager.ManagerServletConstants;

/**
 * This tag updates a specified component bean in the bundle bean with the given
 * component data.  Both the header and body portions are updated.
 */
 
public class UpdateComponentBeanTag extends LoadBundleBeanDetailTag
{
    private Object beanData;


    /**
     * Setter method for the component data used in updating the bean.
     *
     * @param  beanData  Component data for use in the update.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBeanData(Object beanData) throws JspException
    {
       this.beanData = TagUtils.getDynamicValue("beanData", beanData, Object.class, this, pageContext);
    }

    /**
     * Redefinition of doStartTag() in VariableTagBase.  This method processes the 
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
        setup();
            
        if (!StringUtils.hasValue(beanName) || !StringUtils.hasValue(messageId))
        {
            String errorMessage = "Invalid JSP tag [UpdateComponentBeanTag] attribute values encountered: beanName [" + beanName + "], messageId [" + messageId + "]";
            
            log.error("doStartTag(): " + errorMessage);
            
            throw new JspException(errorMessage);
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Updating component bean [" + beanName + "] in the bundle using message id [" + messageId + "] ...");
        }
        
        if (beanData != null)
        {
            XMLPlainGenerator    dataGenerator        = getXMLGenerator();

            BundleBeanBag        bundleBean           = (BundleBeanBag)TagUtils.getBeanInMsgCache(pageContext, messageId, SERVICE_BUNDLE_BEAN_BAG);

            ServiceComponentBean currentComponentBean = (ServiceComponentBean)bundleBean.getBean(beanName);
            
            String               serviceType          = currentComponentBean.getServiceType();
            
            if (currentComponentBean == null)
            {
                String errorMessage = "Failed to locate UpdateComponentBeanTag's specified component bean [" + beanName + "] in the bundle bean.";
                
                log.error("doStartTag(): " + errorMessage);
                
                throw new JspException(errorMessage);
            }

            Object currentHeaderData = currentComponentBean.getHeaderDataSource();
            
            try
            { 
                ServiceComponentBean newComponentBean = new ServiceComponentBean();
                
                newComponentBean.setHeaderDataSource(currentHeaderData);
                
                newComponentBean.setServiceType(serviceType);
                
                newComponentBean.decompose(dataGenerator, dataGenerator.getNode("0"));
                                
                String       supplier           = newComponentBean.getHeaderValue(SUPPLIER);
         
                StringBuffer resourcePathBuffer = new StringBuffer();
                
                resourcePathBuffer.append(pageContext.getServletContext().getInitParameter(RESOURCE_DATA_ROOT));
                
                resourcePathBuffer.append("/");
                
                resourcePathBuffer.append(serviceType);
                
                resourcePathBuffer.append("-");
                
                resourcePathBuffer.append(supplier);
                                                
                String resourcePath = resourcePathBuffer.toString();                                                
                              
                String messageType  = getMessageType(bundleBean, newComponentBean);
                
                setMetaData(newComponentBean, resourcePath, messageType);
                
                setTransformers(newComponentBean, resourcePath, messageType);

                if (loadTemplate(bundleBean))
                {
                    setTemplateData(newComponentBean, resourcePath, messageType);
                }
                
                bundleBean.addBean(beanName, newComponentBean);
            }
            catch (Exception e)
            {
                String errorMessage = " Failed to create and setup the new component bean, and set it in the bundle: " + e.getMessage();
                
                log.error("doStartTag(): " + errorMessage);
                
                throw new JspException(errorMessage);
            }     
            
            if (log.isDebugEnabled())
            {
                log.debug("doStartTag(): For UpdateCB Updated bundle bean has the following content:\n " + bundleBean.describeBeans(true));    
            }
        }
        
        return SKIP_BODY;
    }
    
    /**
     * Obtain an XMLPlainGenerator instance out of the specified component data.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  An instance of XMLPlainGenerator.
     */
    private XMLPlainGenerator getXMLGenerator() throws JspException
    {
        try
        {
            if (beanData instanceof XMLPlainGenerator)
            {
                return (XMLPlainGenerator)beanData;
            }
            else if (beanData instanceof String)
            {
                return new XMLPlainGenerator((String)beanData);   
            }
            else if (beanData instanceof Document)
            {
                return new XMLPlainGenerator((Document)beanData);   
            }

            return null;
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to create an XMLPlainGenerator instance: " + e.getMessage();
            
            log.error("getXMLGenerator(): " + errorMessage);
            
            throw new JspException(errorMessage);
        }
    }
	   
    /**
     * Obtain the message type, which is used to determine meta, transform, and template
     * information.
     *
     * @param  bundleBean     BundleBeanBag instance.
     * @param  componentBean  ServiceComponentBean instance.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     *
     * @return  Message type.
     */
    private String getMessageType(BundleBeanBag bundleBean, ServiceComponentBean componentBean) throws Exception
    {
        String         serviceType    = componentBean.getServiceType();
        
        HashMap        bundleDefs     = (HashMap)pageContext.getServletContext().getAttribute(BUNDLE_DEFS);
        
        String         bundleName     = bundleBean.getHeaderValue(SERVICE_BUNDLE_NAME);
     
        BundleDef      bundleDef      = (BundleDef)bundleDefs.get(bundleName);
           
        ComponentDef   componentDef   = bundleDef.getComponent(serviceType);
        
        Document       bodyDocument   = ((XMLGenerator)componentBean.getBodyDataSource()).getDocument();
        
        MessageTypeDef messageTypeDef = componentDef.getMessageType(bodyDocument);
        
        if (log.isDebugEnabled())
        {
            log.debug("getMessageType(): Obtaining the MessageTypeDef object from component bean's body data:\n" + ((XMLGenerator)componentBean.getBodyDataSource()).getOutput());
        }
        
        if (messageTypeDef == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getMessageType(): Failed to obtain a MessageTypeDef object from the body data.  Will use the default instead.");    
            }
            
            messageTypeDef = componentDef.getDefaultMessageType();     
        }
 
        String messageType = messageTypeDef.getName();
        
        if (log.isDebugEnabled())
        {
            log.debug("getMessageType(): Returning message type [" + messageType + "] ...");
        }
        
        // As a side-effect, update the component display name in the header data.
        
        componentBean.setHeaderValue(COMPONENT_DISPLAY_NAME, componentDef.getDisplayName());
        
        return messageType;
    }
     
    /**
     * Determine whether the template should be used for this component bean.
     *
     * @param  bundleBean  BundleBeanBag instance.
     *
     * @return  true or false.
     */
    private boolean loadTemplate(BundleBeanBag bundleBean) throws JspTagException
    {
        boolean loadTemplate = false;
        try{

            if(ServletUtils.getWebAppContextPath(pageContext.getServletContext()).endsWith("-local"))
            {   
                if(messageId != null)
                {
                    BundleBeanBag oldBag=(BundleBeanBag)TagUtils.getBeanInMsgCache(pageContext, messageId, SERVICE_BUNDLE_BEAN_BAG);

                    if(oldBag != null && "true".equals(oldBag.getHeaderValue(ManagerServletConstants.APPLY_TEMPLATES_HEADER_FIELD)) )
                    {
                       loadTemplate=true;
                    }
                }
                
                if (    loadTemplate && 
                        !(SAVED_STATUS.equals(bundleBean.getHeaderValue(STATUS_FIELD)) ) 
                    )
                {
                    loadTemplate = false;
                }
            }
            else
            {
                if (SAVED_STATUS.equals(bundleBean.getHeaderValue(STATUS_FIELD)))
                {
                    loadTemplate = true;
                }
            }
        }
        catch (Exception e ) {
          String err = "UpdateComponentBeanTag : Failed to decide templates loading: " + e.getMessage();
          log.error(err);
          log.error("",e);
          throw new JspTagException(err);
       }
        
        return loadTemplate;
    }
    
    /**
     * Overriding parent's release().
     */
    public void release()
    {
        super.release();
        
        beanData = null;
    }
}
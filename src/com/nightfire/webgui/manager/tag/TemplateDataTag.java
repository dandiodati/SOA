/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import com.nightfire.framework.message.common.xml.*;

import  java.util.*;
import  javax.servlet.jsp.*;
import  javax.servlet.ServletRequest;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.generator.MessageGeneratorException;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.manager.svcmeta.*;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.beans.*;
import  com.nightfire.webgui.core.beans.NFBean;
 

/**
 *  Each order's  body data source generator gets checked in here. 
 *  Always set XML generator to evaluate body.
 */

public class TemplateDataTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{        
        
    private BundleBeanBag existingBean;
    
    private BundleDef     bundleDef;
        
    private XMLGenerator  predefinedBundleDef;

    /**
     * Getter method for the existing service-bundle bean.
     *
     * @return  Existing service-bundle bean.
     */
    public Object getExistingBean()
    {
        return existingBean;
    }
 
    /**
     * Setter method for the existing service-bundle bean.
     *
     * @param  existingBean  Existing service-bundle bean.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setExistingBean(Object existingBean) throws JspException
    {
        this.existingBean = (BundleBeanBag)TagUtils.getDynamicValue("existingBean", existingBean, BundleBeanBag.class, this, pageContext);
    }
    
    /**
     * Getter method for the the bundle definition object.
     *
     * @return  Bundle definition object.
     */
    public Object getBundleDef()
    {
        return bundleDef;
    }
    
    /**
     * Setter method for the bundle definition object.
     *
     * @param  bundleDef  Bundle definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBundleDef(Object bundleDef) throws JspException
    {
        this.bundleDef = (BundleDef)TagUtils.getDynamicValue("bundleDef", bundleDef, BundleDef.class, this, pageContext);
        
        if (this.bundleDef == null)
        {
            String errorMessage = "ERROR: " + getClass().getName() + ".setBundleDef(): The bundle definition object passed in via attribute [BundleDef] is null.";

            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
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
        super.doStartTag();
      
        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Starting tag processing ...");    
        }

        List compDefsList = bundleDef.getComponents();

        
        try
        {     
            for(int i =0; i < compDefsList.size();++i)
            {

              ComponentDef cmpDef = (ComponentDef) compDefsList.get(i);


				// decomposing the existing bean into components. the existing bean is an instance of BundlebeanBag
              
                Collection serviceComponentBeans = existingBean.getBeans(cmpDef.getID());
                
                
                if (serviceComponentBeans != null && serviceComponentBeans.size() > 0)
                {

						
                    Iterator iter = serviceComponentBeans.iterator();
                    while(iter.hasNext() )
                    {
                        ServiceComponentBean bean = (ServiceComponentBean) iter.next();
					
                        



						if ( (bean.getBodyDataSource() instanceof XMLTemplateGenerator))

						{
					// do nothing.
						
						}


								
						
						else if (bean.getBodyDataSource() instanceof XMLPlainGenerator)
						{	

								try
							{

			bean.setBodyDataSource(new XMLTemplateGenerator(((XMLPlainGenerator)bean.getBodyDataSource()).getOutput()));

   					         
                            }
                            catch(Exception e)
                            {
                                //if MultiMatchException occurs do nothing
                                if(e instanceof XMLTemplateGenerator.MultiMatchException)
                                {
                                }
                                else throw e;
                            }

						}

						else  {

							continue;	
							
						}
					
                            String messageType = cmpDef.getDefaultMessageType().getName();
   					       log.debug(" messageType  " + messageType);    

                            String resourceRoot = pageContext.getServletContext().getInitParameter(RESOURCE_DATA_ROOT);
   					       log.debug(" resourceRoot  " + resourceRoot);    

                            String keyPath;
							if(StringUtils.hasValue(bean.getHeaderValue(ServletConstants.INTERFACE_VER)))
							{
									
							
							keyPath= ((new StringBuffer(resourceRoot)).append('/').append(cmpDef.getID()).append('-').append(bean.getHeaderValue(SUPPLIER)).append('-').append(bean.getHeaderValue(INTERFACE_VER))).toString();
	if (log.isDebugEnabled())
        {
            log.debug("got interfaceversion from service component bean :- "+bean.getHeaderValue(INTERFACE_VER) );    
        }
	

							}					else
							keyPath= ((new StringBuffer(resourceRoot)).append('/').append(cmpDef.getID()).append('-').append(bean.getHeaderValue(SUPPLIER))).toString();

                            try
                            {
                                setTemplateData(bean, messageType, keyPath);
                            }
                            catch(Exception e)
                            {	

									
                                throw new JspException(e);
                            }
                        

                    }

                }

             }//for loop
        }
        catch(Exception e )
        {

            
            throw new JspException(e.getMessage());
        }
        setVarAttribute(existingBean);
        
        ServletRequest servletRequest = pageContext.getRequest();

        String messageId = servletRequest.getParameter(ServletConstants.NF_FIELD_HEADER_PREFIX + ServletConstants.MESSAGE_ID);
        
        if (!StringUtils.hasValue(messageId))
        {
            NFBean         requestBean    = (NFBean)servletRequest.getAttribute(REQUEST_BEAN);
            messageId = requestBean.getHeaderValue(NF_FIELD_HEADER_MESSAGE_ID_TAG);
        }
        
        if(StringUtils.hasValue(messageId))
        {
            TagUtils.setBeanInMsgCache(pageContext, messageId, varName, existingBean);
        }

        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Updated bundle bean has the following content:\n " + existingBean.describeBeans(true));    
        }
        
        return SKIP_BODY;
    }
    
    

    /**
     * Set the template-data object on the specified service-component bean, if
     * one exists.
     *
     * @param  bean     Service-component bean.
     * @param messageType  The message type for this bean
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    private void setTemplateData(ServiceComponentBean bean, String messageType, String keyPath) throws Exception
    {

        String templateDataPath = ((new StringBuffer(keyPath)).append('/').append(TEMPLATE_DIRECTORY).append('/').append(messageType).append(".xml")).toString();
        
        
        
        Object templateData = ServletUtils.getLocalResource(pageContext.getRequest(), templateDataPath, ServletConstants.TEMPLATE_DATA, false);

        if (templateData != null)
        {    
            templateData = TagUtils.getTemplateGenerator(pageContext, (XMLPlainGenerator)templateData);
            log.debug("setTemplateData(): Merging template-data object with loaded service-component bean ...");
            XMLGenerator old = (XMLGenerator) bean.getBodyDataSource();
            ((XMLGenerator)templateData).merge(old);
            bean.setBodyDataSource((XMLGenerator)templateData);
        }
    }
    
}


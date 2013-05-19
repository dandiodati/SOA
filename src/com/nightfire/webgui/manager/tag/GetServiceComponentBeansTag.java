/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  java.util.*;

import  javax.servlet.jsp.*;

import  com.nightfire.framework.util.*;

import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.beans.XMLBean;
import  com.nightfire.webgui.core.ServletConstants;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.beans.*;


/**
 * GetServiceComponentsTag is responsible for obtaining a list of service-component
 * beans of the indicated type from the specified service-bundle bean bag, and
 * setting it in the output scoped variable.
 */

public class GetServiceComponentBeansTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{       
    private String        serviceType;
    
    private BundleBeanBag bundleBean;
    
    
    /**
     * Getter method for the service-component type.
     *
     * @return  Service-component type.
     */
    public String getServiceType()
    {
        return serviceType;
    }
    
    /**
     * Setter method for the service-component type.
     *
     * @param  serviceType  Service-component type.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setServiceType(String serviceType) throws JspException
    {
        this.serviceType = (String)TagUtils.getDynamicValue("serviceType", serviceType, String.class, this, pageContext);
    }
    
    /**
     * Getter method for the service-bundle bean bag object.
     *
     * @return  Service-bundle bean bag object.
     */
    public Object getBundleBean()
    {
        return bundleBean;
    }
    
    /**
     * Setter method for the service-bundle bean bag object.
     *
     * @param  bundleBean  Service-bundle bean bag object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBundleBean(Object bundleBean) throws JspException
    {
        this.bundleBean = (BundleBeanBag)TagUtils.getDynamicValue("bundleBean", bundleBean, BundleBeanBag.class, this, pageContext);
        
        if (this.bundleBean == null)
        {
            String errorMessage = "ERROR: GetServiceComponentBeansTag.setBundleBean(): The service-bundle bean bag object passed in via attribute [bundleBean] is null.";
            
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

        Collection serviceComponentBeans = bundleBean.getBeans(serviceType);
        
        if (log.isDebugEnabled())
        {

            int count = 0;

            if (serviceComponentBeans != null && serviceComponentBeans.size() > 0)
            {
                StringBuffer logMessage = new StringBuffer("GetServiceComponentBeansTag.doStartTag(): Their IDs are: ");
                Iterator iter = serviceComponentBeans.iterator();
                while(iter.hasNext() )
                {
                    ServiceComponentBean bean = (ServiceComponentBean) iter.next();
                    logMessage.append(bean.getId()).append(" ");
                }

                log.debug("doStartTag(): There are [" + serviceComponentBeans.size() + "] of type [" + serviceType + "].");
                log.debug("doStartTag(): " + logMessage.toString());
            }
        }
        
        setVarAttribute(serviceComponentBeans);
        
        return SKIP_BODY;
    }
        
    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
        
        serviceType = null;
        
        bundleBean  = null;
    }
}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  javax.servlet.jsp.JspException;

import  com.nightfire.framework.util.Debug;

import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.ServletConstants;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.beans.ServiceComponentBean;
import  com.nightfire.webgui.manager.svcmeta.ComponentDef;


/**
 * ConstructServiceInfoTag is the base class which provides children classes with
 * tag attributes for service-definition and service-component bean objects.
 */

public abstract class ConstructServiceInfoTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{   
    protected ComponentDef         serviceDef;
    
    protected ServiceComponentBean serviceBean;
    
    
    /**
     * Getter method for the service-component definition object.
     *
     * @return  Service-component definition object.
     */
    public Object getServiceDef()
    {
        return serviceDef;
    }
    
    /**
     * Setter method for the service-component definition object.
     *
     * @param  serviceDef  Service-component definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setServiceDef(Object serviceDef) throws JspException
    {
        this.serviceDef = (ComponentDef)TagUtils.getDynamicValue("serviceDef", serviceDef, ComponentDef.class, this, pageContext);
        
        if (this.serviceDef == null)
        {
            String errorMessage = "ERROR: " + getClass().getName() + ".setServiceDef(): The service-component definition object passed in via attribute [serviceDef] is null.";
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
    }
    
    /**
     * Getter method for the service-component bean object.
     *
     * @return  Service-component bean object.
     */
    public Object getServiceBean()
    {
        return serviceBean;
    }

    /**
     * Setter method for the ervice-component bean object.
     *
     * @param  serviceBean  Service-component bean object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setServiceBean(Object serviceBean) throws JspException
    {
        this.serviceBean = (ServiceComponentBean)TagUtils.getDynamicValue("serviceBean", serviceBean, ServiceComponentBean.class, this, pageContext);
        
        if (this.serviceBean == null)
        {
            String errorMessage = "ERROR: " + getClass().getName() + ".setServiceDef(): The service-component bean object passed in via attribute [serviceBean] is null.";
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
    }

    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
        
        serviceDef  = null;
        
        serviceBean = null;
    }
}
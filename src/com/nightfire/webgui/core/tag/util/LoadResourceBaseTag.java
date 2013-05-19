/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  java.net.*;

import  javax.servlet.*;
import  javax.servlet.jsp.*;

import  com.nightfire.framework.util.*;

import  com.nightfire.webgui.core.*;

import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.tag.VariableTagBase;


/**
 * This base tag class provides support common to all resource-loading tags.  In
 * short, it provides support for loading a specified resource and setting it in
 * the context for access by other components.
 */
 
public abstract class LoadResourceBaseTag extends VariableTagBase
{
    private String  resourcePath;
    

    /**
     * The children classes must implement this method to return each's own
     * resource type.  Refer to {@link ResourceDataCache} for various types
     * of resource.
     *
     * @return  Type of the resource to load.
     */
    public abstract String getResourceType();

    /**
     * Setter method for the resource path name.
     *
     * @param  resourcePath  Resource path name.
     */
    public void setResourcePath(String resourcePath) throws JspException
    {
        this.resourcePath = (String)TagUtils.getDynamicValue("resourcePath",resourcePath, String.class,this,pageContext);
    }
     
    /**
     * This method returns a true by default.  It indicates that if a resource is
     * not found, an exception will be thrown.  A child class can override this
     * to have the opposite behavior.
     *
     * @return  true if the resource is required, false if it's optional.
     */
    protected boolean isRequired()
    {
        return true;
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
      
        ServletContext servletContext = (ServletContext)pageContext.getServletContext();
        
        URL            resourceURL    = null;
        
        String         resourceType   = getResourceType();        
        
        if (!StringUtils.hasValue(resourceType))
        {
            String errorMessage = "ERROR: " + StringUtils.getClassName(this) + ".doStartTag(): The resource type is not specified.";
 
            log.error(errorMessage);
                
            throw new JspException(errorMessage);
        }

        Object resource = null;
        
        
        try {
            
            resource = ServletUtils.getLocalResource(pageContext.getRequest(), resourcePath, resourceType, isRequired());
        } catch (ServletException e) {
            String err = "\nCould not obtain resource [" + resourcePath +"]"
                      + "\nRequired? [" + isRequired() +"]"
                      + "\nResource Type [" + resourceType +"]"
                      +"\nError: " + e.getMessage();
            log.error(err);
        
            throw new JspTagException(err);         
        }
               
        // modify thre resource if needed before returning it
        if ( resource != null)
            resource = modifyResource(resource);  
                             
        // if resource is optional then the results may have been null
        setVarAttribute(resource);
            
            return SKIP_BODY;
    }
    
  
    /**
     * Provides a way for child classes to modify a resource before it is set as an output
     * variable. Default is no modification.
     *
     * @param resource The resource that was loaded from the Resource data cache.
     * @return The modified resource.
     */
    public Object modifyResource(Object resource) throws JspTagException
    {
       return resource;
    }

    
    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
        
        resourcePath = null;
    }
}

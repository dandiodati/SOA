/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.util;

import  java.util.HashMap;

import  javax.servlet.jsp.*;

import  com.nightfire.framework.util.Debug;

import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.manager.svcmeta.BundleDef;
import  com.nightfire.webgui.manager.ManagerServletConstants;


/**
 * LoadBundleDefTag is responsible for loading a specified bundle-definition
 * resource and setting it in the context for access by other components.
 */

public class LoadBundleDefTag extends VariableTagBase implements ManagerServletConstants
{
    private String bundleName;
    
    
    /**
     * Getter method for the bundle name.
     *
     * @return  Bundle name.
     */
    public String getBundleName()
    {
        return bundleName;   
    }

    /**
     * Setter method for the bundle name.
     *
     * @param  bundleName  bundle name.
     */
    public void setBundleName(String bundleName) throws JspException
    {
        setDynAttribute("bundleName", bundleName, String.class);
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
      
        bundleName = (String)getDynAttribute("bundleName");
        HashMap   bundleDefs = (HashMap)pageContext.getServletContext().getAttribute(BUNDLE_DEFS);
        
        BundleDef bundleDef  = (BundleDef)bundleDefs.get(bundleName);
        
        if (bundleDef == null)
        {
            String errorMessage = "ERROR: LoadBundleDefTag.doStartTag(): No BundleDef object with name [" + bundleName + "] exists in the servlet-context's [" + BUNDLE_DEFS + "] lookup.";
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
        
        setVarAttribute(bundleDef);
        
        return SKIP_BODY;
    }
    
    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
                
        bundleName = null;
    }
}

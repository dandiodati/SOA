/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.util;

import  java.util.*;

import  javax.servlet.jsp.*;

import  org.w3c.dom.Document;

import  com.nightfire.framework.util.*;

import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.core.ServletConstants;
import  com.nightfire.webgui.manager.beans.BundleBeanBag;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.core.resource.ResourceDataCache;
import com.nightfire.webgui.core.ServletUtils;


/**
 * GetBundleDefNameTag is responsible for outputing the bundle-definition name.
 * This is useful for loading the bundle-definition into its corresponding object
 * model for building the pages.
 */

public class GetBundleDefNameTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{
    private BundleBeanBag existingBean;
        
    
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
        setDynAttribute("existingBean", existingBean, BundleBeanBag.class);
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
      
        existingBean = (BundleBeanBag)getDynAttribute("existingBean");

        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Starting tag processing ...");    
        }

        try 
        {
            String bundleDefName;

            if (existingBean != null) 
            {
                //service bundle name is set by old preparebundletag.  
                bundleDefName = existingBean.getHeaderValue(SERVICE_BUNDLE_NAME);
                if (!StringUtils.hasValue(bundleDefName)) 
                    bundleDefName = existingBean.getHeaderValue(META_DATA_NAME);
                
            } 
            else 
            {
                String       predefinedBundleName = (String)pageContext.getRequest().getParameter(PREDEFINED_BUNDLE_ID_HEADER_FIELD);
                
                String predefinedDefPath = (String)props.get(PREDEFINED_BUNDLE_DEF_PROP);

                
                Document doc = (Document)ServletUtils.getLocalResource(pageContext.getRequest(), predefinedDefPath, ResourceDataCache.XML_DOC_DATA, true);

                XMLGenerator predefinedBundleDef = new XMLPlainGenerator(doc);
                
                
                bundleDefName                     = predefinedBundleDef.getValue(predefinedBundleName + ".BundleDefinition");
            }


            if (!StringUtils.hasValue(bundleDefName))
            {
                String errorMessage = "ERROR: GetBundleDefNameTag.doStartTag(): Could not obtain bundle def name.";
                
                log.error(errorMessage);
                
                throw new JspException(errorMessage);
            }

            setVarAttribute(bundleDefName);

            if (log.isDebugEnabled())
            {
                log.debug("doStartTag(): The bundle-definition name is determined to be [" + bundleDefName + "].");
            }
        }
        catch (Exception e)
        {
            String errorMessage = "doStartTag(): Failed to obtained the bundle-definition name from the predefined-bundle definition:\n" + e.getMessage();

            log.error(errorMessage);

            throw new JspException(errorMessage);
        }

        return SKIP_BODY;
    }    
    
    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
            
        existingBean = null;
    }
}

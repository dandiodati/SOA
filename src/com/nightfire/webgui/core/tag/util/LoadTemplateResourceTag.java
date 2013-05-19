/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  javax.servlet.jsp.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.webgui.core.tag.util.LoadResourceBaseTag;
import  com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.*;
import com.nightfire.webgui.core.ServletConstants;



/**
 * LoadTemplateResourceTag is responsible for loading a specified template resource
 * and setting it in the context for access by other components.
 */

public class LoadTemplateResourceTag extends LoadResourceBaseTag
{
    public Object modifyResource(Object resource) throws JspTagException
    {
        try
        {
            return TagUtils.getTemplateGenerator(pageContext, (XMLPlainGenerator)resource);
        }
        catch (Exception e)
        {
            log.error("modifyResource(): " + e.getMessage());
            
            throw new JspTagException(e.getMessage());
        }
    }
    
    /**
     * Obtain the type of resource to load.
     *
     * @return  Type of the resource to load.
     */
    public String getResourceType()
    {
        return ServletConstants.TEMPLATE_DATA;
    }
   
    /**
     * This method indicates whether a resource is required.  In this case, it
     * is not so a false is always returned.
     *
     * @return  true if the resource is required, false if it's optional.
     */
    protected boolean isRequired()
    {
        return false;
    }
}

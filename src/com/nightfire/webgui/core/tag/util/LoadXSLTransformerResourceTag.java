/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/tag/util/LoadXSLTransformerResourceTag.java#1 $
 */

package com.nightfire.webgui.core.tag.util;

import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.webgui.core.tag.util.LoadResourceBaseTag;
import com.nightfire.webgui.core.ServletConstants;


/**
 * LoadXSLTransformerResourceTag is responsible for loading a specified 
 * XSL Transformer resource and setting it in the context for access by other components.
 */

public class LoadXSLTransformerResourceTag extends LoadResourceBaseTag
{
    /**
     * Obtain the type of resource to load.
     *
     * @return  Type of the resource to load.
     */
    public String getResourceType()
    {
        return ServletConstants.XSL_TRANSFORM_DATA;
    }
}

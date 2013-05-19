/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.webgui.core.tag.util.LoadResourceBaseTag;


/**
 * LoadXMLResourceTag is responsible for loading a specified XML resource and
 * setting it in the context for access by other components.
 */

public class LoadXMLResourceTag extends LoadResourceBaseTag
{
    /**
     * Obtain the type of resource to load.
     *
     * @return  Type of the resource to load.
     */
    public String getResourceType()
    {
        return ResourceDataCache.XML_DOC_DATA;
    }
}
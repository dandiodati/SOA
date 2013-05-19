/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.webgui.core.tag.util.LoadResourceBaseTag;
import com.nightfire.webgui.core.ServletConstants;


/**
 * LoadMetaResourceTag is responsible for loading a specified meta resource and
 * setting it in the context for access by other components.
 */

public class LoadMetaResourceTag extends LoadResourceBaseTag
{

    /**
     * Obtain the type of resource to load.
     *
     * @return  Type of the resource to load.
     */
    public String getResourceType()
    {
        return ServletConstants.META_DATA;
    }
}

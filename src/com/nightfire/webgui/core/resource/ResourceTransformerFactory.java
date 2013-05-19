/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/ResourceTransformerFactory.java#1 $
 */

package com.nightfire.webgui.core.resource;
import com.nightfire.framework.util.*;

/**
 * A Factory to create ResourceTransformer used by the ResourceDataCache.
 * 
 */
public interface ResourceTransformerFactory
{
    /**
     * Creates a Transformer class that can be used to create the specified type of resource.
     *
     * @param type - The type of object to transform.
     * @return A Transformer to convert this type.
     * NOTE: If type is unsupported this method should return null.
     *
     * @exception Throws a FrameworkException when an error occurs during creation of the transformer.
     *
     */
    public ResourceTransformer create(String type) throws FrameworkException;

}


    
/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.resource;

import java.net.URL;
import java.util.List;

import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.manager.svcmeta.BundleDef;
import  com.nightfire.webgui.core.resource.ResourceTransformer;



/**
 * A manager-specific resource transformer, which transforms the specified resource
 * into the BundleDef object.
 */
 
public class BundleDefResourceTransformer implements ResourceTransformer 
{

    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        return new BundleDef(url);
    }


    /** 
     * Returns included references from the generated resource.
     */
    public List getReferredResources(URL url, String enc) 
    {

        //NOTE: In this case we are returning null, since this
        // resource does not get reloaded.
        // In the future if we want to reload this
        // we will also need to change the BundleDef object to
        // use the URIResolver to resolve includes. Or 
        // create some generic solution to resolve included files within xml.
        //
        return null;
    }  

}

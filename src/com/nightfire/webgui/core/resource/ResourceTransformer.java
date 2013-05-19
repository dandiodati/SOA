/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/ResourceTransformer.java#1 $
 */

package com.nightfire.webgui.core.resource;

import  com.nightfire.framework.util.*;
import  java.net.*;
import  java.io.*;
import java.util.List;

/**
 * A class the transforms a resource's data into a object that can be manipulated easily.
 * Note This class must be thread save.
 */
public interface ResourceTransformer
{
 
   /**
    * Transforms a URL of the data into the needed object type.
    * @param enc The character encoding to use while loading the url.
    * @param  url  The url to the data to transform.
    */
   public Object transformData(URL url, String enc) throws FrameworkException;


    /**
     * Returns a list of URL objects to any included or referenced resources.
     * All of these referred resources affect the reloading of the main resource.
     * 
     *
     * @param url The url to the main resource being loadded.
     * @param enc The character encoding for the resource.
     * @return A list of URL objects to each referred resource or null if
     * there are no referenced resources.
     */
    public List getReferredResources(URL url, String enc);
    

}

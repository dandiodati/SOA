/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 */

package com.nightfire.webgui.core.resource;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;


/**
 * Handles converting a object into a url.
 *
 */
public interface URLConverter
{

    /**
     * Converts the path object into a url.
     * A null object can be passed in.
     *
     * @param path An object describing a url.
     * @return A url created from the path object
     * @exception MalformedURLException If the input object can not be converted to a URL.
     */
    public URL getURL(Object path) throws MalformedURLException;

    /**
     * Returns a set of URLs to all resources under/in the specified pathObj.
     * 
     *
     * @param pathObj An object describing a location of resources.
     * @return a <code>Set</code> of URL objects.
     * @exception MalformedURLException If any resources can not be converted to URL objects.
     */
    public Set getResourceURLs(Object pathObj) throws MalformedURLException;
    
    
}

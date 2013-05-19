/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  javax.servlet.*;
import  javax.servlet.http.HttpSession;
import java.util.*;



/**
 * <p><strong>ProtocolAdapter</strong> is an interface which needs to be implemented
 * by any classes wishing to send requests to the processing layer.</p> 
 */

public interface ProtocolAdapter
{

    /**
     * Intializes this component.
     * @param props Servlet properties containing configuration.
     * @param context The servlet context being loaded.
     * @exception  ServletException  Thrown when an error occurs during initialization.
     */
    public void init(Properties props, ServletContext context) throws ServletException;

    /**
     * This method sends a request to the processing layer.
     *
     * @param  request  Request DataHolder.
     * @param  session  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  DataHolder Response object returned from the processing layer.
     */
    public DataHolder sendRequest(DataHolder request, HttpSession session) throws ServletException;
}

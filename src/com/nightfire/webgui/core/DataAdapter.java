/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  javax.servlet.*;
import  javax.servlet.http.*;

import  com.nightfire.framework.util.MessageData;

import  com.nightfire.webgui.core.beans.NFBean;
import com.nightfire.framework.message.common.xml.*;

import java.util.*;

/**
 * <p><strong>DataAdapter</strong> is an interface that needs to be implemented
 * by any classes wishing to format the request and response data, to and from 
 * the processing layer.</p>
 */

public interface DataAdapter
{

    /**
     * Intializes this component.
     * @param props Servlet properties containing configuration.
     * @param context The servlet context being loaded.
     * @exception  ServletException  Thrown when an error occurs during initialization.
     */
    public void init(Properties props, ServletContext context) throws ServletException;

    /**
     * This method transforms the request-data Bean into a request object of type
     * which the processing layer is expecting.
     *
     * @param  requestData     Request data Bean.
     * @param  servletRequest  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  A DataHolder which contains the transformed message.
     */
    public DataHolder transformRequest(NFBean requestDataBean, HttpServletRequest servletRequest) throws ServletException;
        
    /**
     * This method transforms the response obtained from the back end into a
     * MessageData object.
     *
     * @param  responseData  Response data Holder.
     * @param  session       HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  A DataHolder object, which basically composes of a transformed response header
     *          XML and a response body XML. 
     */
    public DataHolder transformResponse(DataHolder responseData, HttpSession session) throws ServletException;
}
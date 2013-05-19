/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  javax.servlet.*;
import  javax.servlet.http.HttpSession;

import  com.nightfire.framework.util.MessageData;

import  com.nightfire.webgui.core.beans.NFBean;
import java.util.*;


/**
 * <p><strong>InputValidator</strong> represents the interface that each application
 * which requires request-data validation in the front-end needs to implement.</p>
 * NOTE: This class must not alter the DataHolders passed.
 */

public interface InputValidator
{


    /**
     * Intializes this component.
     * @param props Servlet properties containing configuration.
     * @param context The servlet context being loaded.
     * @exception  ServletException  Thrown when an error occurs during initialization.
     */
    public void init(Properties props, ServletContext context) throws ServletException;


    /**
     * Validates the request data against the internal cached validation info.
     *
     * @param  guiRequest  Request DataHolder holding the data before going through the data adapter(GUI formated xml).
     * @param  outputRequest DataHolder holding the data AFTER going through the data adapter(destination formated xml).

     * @param  session          HttpSession object.
     *
     * @return  If validation fails, a DataHolder object containing a response
     *          header XML and a response body XML is returned.  Else, a null is
     *          returned.
     * NOTE: No modification to the DataHolder inputs are allowed by this class
     * The DataHolder should be treated as read only.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public DataHolder validate(DataHolder guiRequest, DataHolder outputRequest, HttpSession session) throws ServletException;
}

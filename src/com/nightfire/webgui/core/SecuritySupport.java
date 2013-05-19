/**
 * Copyright (c) 2003 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  javax.servlet.http.*;

import  com.nightfire.framework.util.*;

import  com.nightfire.framework.debug.*;


/**
 * <p><strong>SecuritySupport</strong> represents a session listener class that
 * provides cleanup support to {@link SecurityFilter SecurityFilter} class.</p>   
 */
 
public class SecuritySupport implements HttpSessionListener
{
    private DebugLogger log;
    
    
    /**
     * Indicates that a session has been created
     *
     * @param  event  HttpSessionEvent object.
     */
    public void sessionCreated(HttpSessionEvent event)
    {
        HttpSession session    = event.getSession();
        
        String      webAppName = ServletUtils.getWebAppContextPath(session.getServletContext());
        
        log                    = DebugLogger.getLogger(webAppName, getClass());   
    }

    /**
     * Indicates that a session is going to be destroyed.
     *
     * @param  event  HttpSessionEvent object.
     */
    public void sessionDestroyed(HttpSessionEvent event)
    {
        HttpSession session = event.getSession();
        
        if (log.isDebugEnabled())
        {
            log.debug("sessionDestroyed(): Cleaning up security cache associated with session [" + session.getId() + "] for web application [" + session.getServletContext().getServletContextName() + "] ...");
        }
        
        SecurityFilter.sessionDestroyed(session);        
    }
}

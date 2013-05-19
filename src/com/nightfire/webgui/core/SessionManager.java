/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  java.lang.*;
import  java.util.*;

import  javax.servlet.http.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.resource.*;





/**
 * SessionManager performs necessary initialization and
 * cleanup during the creation and destruction of sessions within a webapp.
 */
 
public class SessionManager implements HttpSessionListener
{

    public static final String PAGE_HISTORY_SIZE          = "PAGE_HISTORY_SIZE";
    private static final int    DEFAULT_PAGE_HISTORY_SIZE  = 6;

    private DebugLogger log;
  
   // Global counter indicating how many outstanding gateway requests there are.
    private static int outstandingSessionCount = 0;


    public static int getSessionCount ( )
    {
        return outstandingSessionCount;
    }


    /**
     * Indicates that a session has been created
     *
     * @param  event  HttpSessionEvent object.
     */
    public void sessionCreated(HttpSessionEvent event)
    {
        outstandingSessionCount ++;

        HttpSession session = event.getSession();
        String webAppName = ServletUtils.getWebAppContextPath(session.getServletContext()); 
        log = DebugLogger.getLogger(webAppName, getClass());   

        Properties contextProps = (Properties) session.getServletContext().getAttribute(ServletConstants.CONTEXT_PARAMS);


        // create a url converter for this session to resolve resources
        // this assumes that we will have access to the session bean.
        WebAppURLConverter c = new WebAppURLConverter(session);  
        session.setAttribute(ServletConstants.RESOURCE_URL_CONVERTER,c);

        initMessageDataCache(session, contextProps);

    }

    /**
     * Indicates that a session is going to be destroyed.
     *
     * @param  event  HttpSessionEvent object.
     */
    public void sessionDestroyed(HttpSessionEvent event)
    {
        outstandingSessionCount --;

        log.info("Destroying Session [" + event.getSession().getId() +"] for web app [" 
                 + event.getSession().getServletContext().getServletContextName() + "].");
    }


     /**
     * This method creates the MessageDataCache when a session is first created.
     *
     *
     * @param  session  The HttpSession object.
     */
    private void initMessageDataCache(HttpSession session, Properties props)
    {

        int pageHistorySize;

       // Get the page history size specified in the parameters, or use the default.

        String pageHistorySizeString = props.getProperty(PAGE_HISTORY_SIZE);

        if (log.isDebugEnabled()) {
            log.debug("initMessageDataCache: Page history size is set to [" + pageHistorySizeString + "].");
        }

        try {
            pageHistorySize = Integer.parseInt(pageHistorySizeString);
        }
        catch (Exception e) {
            log.warn("initMessageDataCache: Context initialization parameter [" + PAGE_HISTORY_SIZE + "] must be a positive integer value.  A default size [" + DEFAULT_PAGE_HISTORY_SIZE + "] will be used.");

            pageHistorySize = DEFAULT_PAGE_HISTORY_SIZE;
        }

        // Assure that concurrent requests from the same session don't try to
        // create the session's data cache at the same time.
        KeyTypeMap msgDataCache;

        msgDataCache = (KeyTypeMap)session.getAttribute(ServletConstants.MESSAGE_DATA_CACHE);

        if (msgDataCache == null) {
           if (log.isDebugEnabled())
              log.debug("initMessageDataCache: Creating bean message-data cache of size [" + pageHistorySize + "] ...");

           msgDataCache = new KeyTypeMap();
           msgDataCache.setMaxSize(pageHistorySize);

           session.setAttribute(ServletConstants.MESSAGE_DATA_CACHE, msgDataCache);
        }
    }
    

}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.resource;

import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.util.CustomerContext;
import java.net.MalformedURLException;
import java.util.*;
import com.nightfire.framework.repository.RepositoryManager;



/**
 * Handles converting a object into a url.
 *
 */
public class WebAppURLConverter implements URLConverter
{

    HttpSession session;

    DebugLogger log;
    String cid;
    ServletContext context;



    public WebAppURLConverter(HttpSession session)
    {
        this.session = session;
        this.context = session.getServletContext();
        log = DebugLogger.getLoggerLastApp(getClass());
    }


    public WebAppURLConverter(ServletContext context)
    {
        this.context = context;
        log = DebugLogger.getLoggerLastApp(getClass());
    }


    /**
     * Obtains a URL reference to the object specified by the parameter pathObj.
     * The path is a full path relative to the current web app servlet context.
     *
     */
    public URL getURL(Object pathObj) throws MalformedURLException
    {


        String cid = CustomerContext.DEFAULT_CUSTOMER_ID;

        if( session != null) {

            SessionInfoBean sBean = (SessionInfoBean) session.getAttribute(ServletConstants.SESSION_BEAN);
            cid = sBean.getCustomerId();

        }


        if ( pathObj == null)
            return null;
        else if (!(pathObj instanceof String)) {
            throw new MalformedURLException("Invalid Object type");
        }

        String path = (String)pathObj;


        // if this is a url to the repository then form the url and return it.
        if (path.startsWith(RepositoryManager.URL_PREFIX)) {
            URL context = new URL (RepositoryManager.URL_PREFIX, "localhost", -1, "/", RepositoryManager.URL_HANDLER);
            URL url = new URL(context, path, RepositoryManager.URL_HANDLER);

            return url;
        }



        // if there is a customer specific path in the resource path then
        // return it directly.

        if (isCustomerSpecificPath(cid, path) || isCustomerSpecificPath(CustomerContext.DEFAULT_CUSTOMER_ID, path))
        {
			return context.getResource(path);
		}

        int idx = path.indexOf("/WEB-INF/");

        String start = path.substring(0, idx + "/WEB-INF/".length());
        String rest = path.substring(idx+ "/WEB-INF/".length());

        StringBuffer cidBuf  = new StringBuffer(start);
        cidBuf.append(cid).append("/").append(rest);
        URL cidUrl = context.getResource(cidBuf.toString());


        StringBuffer defaultBuf  = new StringBuffer(start);
        defaultBuf.append(CustomerContext.DEFAULT_CUSTOMER_ID).append("/").append(rest);
        String dPath = defaultBuf.toString();


        // return customer specific url if it exists otherwise return
        // default path url.
        // It the default path url does not exist then a null will be returned.



        if (cidUrl != null)
            return cidUrl;
        else
            return context.getResource(dPath);

    }

	/**
	 * Utility method which determines whether a resource path is customer-specific.
	 * A path is considered customer-specific if it resembles the following:
	 *
	 *    /WEB-INF/HAWAIITEL/resources/Loop-HAWAIITEL-LSOG6/xsl/requestOutgoing.xsl
	 *
	 * Where HAWAIITEL represents the CustomerID.  It is assumed that only the first
	 * occurence of the CustomerID is considered.
	 *
	 * @param  cid   Customer id.
	 * @param  path  The path to examine.
	 *
	 * @return  true or false.
	 */
	private boolean isCustomerSpecificPath(String cid, String path)
	{
		if ((path.indexOf("/" + cid + "/") > -1) || (path.indexOf("\\" + cid + "\\") > -1))
		{
			return true;
		}

		return false;
	}

    /**
     * Obtains a set of resource URL objects under the specificed webapp directory.
     * The path is a full path relative to the current web app servlet context.
     * This function is not recursive so sub directories under
     * pathObj are not traversed.
     *
     */
    public Set getResourceURLs(Object pathObj) throws MalformedURLException
    {

        if ( pathObj == null)
            return null;
        else if (!(pathObj instanceof String)) {
            throw new MalformedURLException("Invalid Object type");
        }

        String path = (String)pathObj;

        Set paths = context.getResourcePaths(path);

        Iterator iter = paths.iterator();

        HashSet newSet = new HashSet();

        while (iter.hasNext()) {
            path = (String)iter.next();
            if( !path.endsWith("/")) {
                newSet.add(getURL(path));
            }
        }

        return newSet;
    }

}

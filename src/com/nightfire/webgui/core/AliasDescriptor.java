/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.core;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;

import org.w3c.dom.*;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.util.ReadWriteLock;
import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.resource.*;
import com.nightfire.webgui.core.xml.*;



/**
 * <p><strong>AliasDescriptor</strong> represents one of ControllerServlet's
 * workers. Its responsibility is to provide support for string translation.
 * This feature is used mostly in response display.  A response value may need
 * to be "mapped" to another string value, whether to make it user-friendly,
 * to have it comply with other displays, etc.</p>
 * The GetAliasTag uses this class to retrieve the mapped value for a string.
 * The aliases are cached by the ResourceDataCache in conjunction with the AliasDataResourceTransformer.
 */

public class AliasDescriptor
{
    private static final String ALIASES = "ALIASES";

    private DebugLogger log;

    private String resourcePath;

    private ServletContext context;

    /**
     * Constructor.
     *
     * @param  properties  Servlet initialization properties.
     * @param context The servlet context getting loaded.
     *
     * @exception  ServletException  Thrown when an error occurs during initialization.
     */
    public AliasDescriptor(Properties properties, ServletContext context) throws ServletException
    {
        String webAppName = ServletUtils.getWebAppContextPath(context);
        log = DebugLogger.getLogger(webAppName, this.getClass());

        resourcePath = (String)properties.getProperty(ALIASES);

        this.context = context;

        if (log.isDebugEnabled())
        {
            log.debug("Alias repository path is [" + resourcePath + "].");
        }

        if (!StringUtils.hasValue(resourcePath))
        {
            log.warn("The servlet initialization parameter [" + ALIASES + "] does not exist.  No alias support will be provided.");
        }
        else
        {
            try {

                // call getLocalResource method to force the loading of resource
                // all loading of aliases are handled by the listener.
                // We want to load aliases at initialization time itself.
                ServletUtils.getLocalResource(context, resourcePath, WebAppResourceFactory.ALIAS_DATA, false);

            } catch (ServletException e) {
                log.error("Failed to load aliases " + e.getMessage());
                return;
            }

        }

    }//constructor AliasDescriptor

    /**
     * Obtain an alias for the specified alias group and value.
     *
     * @param groupId The group id of the alias
     * @param value The value to obtain an alias for
     * @param returnValue if true returns the passed in value if no alias exists.
     * If false and no alias exists then a null is passed back.
     * @return  The alias for the specified group id and value, if any.
     */
    public String getAlias(ServletRequest req, String groupId, String value, boolean returnValue)
    {

        Map aliasGroups = null;
        try
        {
            // call getLocalResource method to force the loading of customer specific
            // resources
            // all loading of aliases are handled by the listener.
            aliasGroups = (Map)ServletUtils.getLocalResource(req, resourcePath, WebAppResourceFactory.ALIAS_DATA, false);
        }
        catch (ServletException e)
        {
            log.warn("Could not load aliases: "+ e.getMessage());
        }

        String alias = null;

        if (log.isDebugEnabled())
           log.debug("Looking for alias: group [" + groupId + "], value [" + value +"]" );

        if (aliasGroups == null)
        {
            if (log.isDebugEnabled())
                log.debug("getAlias(): No alias support is being provided.");

            if (returnValue)
            {
                alias = value;
            }
            //else alias = null
        }
        else
        {
            Map aliases = (Map)aliasGroups.get(groupId);
            if (aliases != null )
                alias = (String)aliases.get(value);

            if (!StringUtils.hasValue(alias) && returnValue)
            {
                alias = value;
            }
        }

        if ( log.isDebugEnabled() )
            log.debug("Returning value: " + alias );

        return alias;

    }//getAlias

}//end class AliasDescriptor


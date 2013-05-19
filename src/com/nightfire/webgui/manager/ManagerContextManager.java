/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/core/ContextManager.java#13 $
 */

package com.nightfire.webgui.manager;

import  java.net.*;
import  java.util.*;
import  javax.servlet.*;

import  org.w3c.dom.*;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.core.resource.*;
import  com.nightfire.webgui.core.svcmeta.*;
import  com.nightfire.webgui.manager.svcmeta.BundleDef;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.resource.*;
import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.util.xml.*;
import  com.nightfire.framework.message.common.xml.*;

/**
 * <p><strong>ManagerContextManager</strong> extends the functionality of the generic
 * ContextManager.  It's manager-specific, hence the leading Manager in the class name.</p>
 */
 
public class ManagerContextManager extends ContextManager implements ManagerServletConstants
{        
    private static final String BUNDLE_DEF_ROOT                     = "BUNDLE_DEF_ROOT";

    public void contextInitialized(ServletContextEvent event)
    {
        super.contextInitialized(event);
        
        ServletContext    servletContext    = event.getServletContext();
        
        ResourceDataCache resourceDataCache = (ResourceDataCache)servletContext.getAttribute(ServletConstants.RESOURCE_DATA_CACHE);
        
        resourceDataCache.setResourceTransformerFactory(new ManagerWebAppResourceFactory(servletContext));
                
        initializeBundleDefLookup(servletContext);
        
    }
    
    
    /**
     * Sets up an application-wide lookup which allows servlet components to
     * access available bundle-definition (BundleDef) objects.
     *
     * @param  servletContext  ServletContext object.
     */
    private void initializeBundleDefLookup(ServletContext servletContext)
    {
              
    	String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
       	Properties initParameters=null;
		try {
			initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
		} catch (FrameworkException fe) {
    		log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
    		throw new RuntimeException("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
    	}
        String  bundleDefRoot = initParameters.getProperty(BUNDLE_DEF_ROOT);
        
        HashMap bundleDefs    = new HashMap();
        
        servletContext.setAttribute(BUNDLE_DEFS, bundleDefs);
        
        if (!StringUtils.hasValue(bundleDefRoot))
        {
            log.error("initializeBundleDefLookup(): Context initialization parameter [" + BUNDLE_DEF_ROOT + "] must exist in web.xml and have a valid path.");
        }
        else
        {
            Set bundleDefCandidates = servletContext.getResourcePaths(bundleDefRoot + "/");
            
            if (bundleDefCandidates == null)
            {
                log.error("initializeBundleDefLookup(): The given bundle-definition root directory [" + bundleDefRoot + "] does not contain any bundle-definition XML files.");
            }
            else
            {
                Iterator iterator = bundleDefCandidates.iterator();
                
                while (iterator.hasNext())
                {
                    String bundleDefCandidate = (String)iterator.next();   
                    
                    if (bundleDefCandidate.endsWith(".xml"))
                    {
                        try
                        {
                            URL resourceURL = servletContext.getResource(bundleDefCandidate);
                            
                            if (resourceURL == null)
                            {
                                log.error("initializeBundleDefLookup(): [" + bundleDefCandidate + "] does not map to a resource. Skipping it ...");
                            }
                            else
                            {   
                                BundleDef bundleDef = null;
                                
                                try {
                                    bundleDef   = new BundleDef(resourceURL);
                                }
                                catch (BuildContext.InvalidSvcException e) {
                                    log.warn("Skipping bundle def, " + e .getMessage() );
                                    continue;
                                    
                                }
                                // add bundle defs with bundle def id and
                                // bundle meta data name 
                                // so that they can be looked up either way.
                                String bundleDefId = bundleDef.getID();
                                String bundleMDN = bundleDef.getMetaDataName();
                                bundleDefs.put(bundleDefId, bundleDef);
                                bundleDefs.put(bundleMDN, bundleDef);

                                
                                log.info("initializeBundleDefLookup(): Added BundleDef object obtained from [" + bundleDefCandidate + "], with id [" + bundleDefId + "], and metadataname [" + bundleMDN+"] to the lookup.");
                            }
                        }
                        catch (MalformedURLException  e)
                        {
                            log.error("initializeBundleDefLookup(): [" + bundleDefCandidate + "] is not a valid path name:\n" + e.getMessage());
                        }
                        catch (Exception e)
                        {
                            log.error("initializeBundleDefLookup(): " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Added BUNDLE_DEF_DATA and PREDEFINED_BUNDLE_DATA
     */
    public class ManagerWebAppResourceFactory extends WebAppResourceFactory
    {

        /**
         * Sets the Servlet Context 
         *
         * @param  servletContext  The Servlet Context 
         *
         */
        public ManagerWebAppResourceFactory( ServletContext servletContext )
        {
            super(servletContext);
            addTransformer(BUNDLE_DEF_DATA, new BundleDefResourceTransformer());
            addTransformer(PREDEFINED_BUNDLE_DATA, new PredefinedBundleDefTransformer() );

        }
    }
    
}

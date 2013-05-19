/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * XslTransformerResourceTransformer.java
 *
 *
 * Created: Wed Sep 11 16:24:22 2002
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/XslTransformerResourceTransformer.java#1 $
 */

package com.nightfire.webgui.core.resource;

import java.net.URL;
import java.util.*;
import javax.servlet.ServletContext;

import com.nightfire.framework.message.transformer.XSLMessageTransformer;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.webgui.core.WebAppURIResolver;
import com.nightfire.framework.debug.DebugLogger;


class XslTransformerResourceTransformer implements ResourceTransformer 
{
    private ServletContext context = null;

    private DebugLogger log;
    
    public XslTransformerResourceTransformer ( ServletContext context )
    {
        this.context = context;

        String webAppName = ServletUtils.getWebAppContextPath(context); 
        log = DebugLogger.getLogger(webAppName, getClass());

    }
    
    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
   
        if ( context == null )
        {
            throw new FrameworkException ("XslTransformerResourceTransformer.transformData(): ServletContext set on the constructor cannot be null");
        }
        
        WebAppURIResolver resolver = new WebAppURIResolver( context );

        
        
        return new XSLMessageTransformer ( url, resolver );
    }


    public List getReferredResources(URL url, String enc) 
    {
        try {
            
            WebAppURIResolver resolver = new WebAppURIResolver( context );
            // (use encoding specified in the XML document instead of enc)
            XSLMessageTransformer t = new XSLMessageTransformer ( url, resolver );
        
            Set links =  resolver.getAllResolvedURIs();
        

            return (new ArrayList(links));

        }
        catch (Exception e) {
            log.error("Could not obtain referred resources for [" + url + "]:" + e.getMessage());
            return null;
            
        }
       
    }


}

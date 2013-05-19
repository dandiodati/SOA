/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * MetaDataResourceTransformer.java
 *
 *
 * Created: Wed Sep 11 16:24:22 2002
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/MetaDataResourceTransformer.java#1 $
 */

package com.nightfire.webgui.core.resource;

import java.net.URL;
import java.util.*;
import javax.servlet.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.webgui.core.*;
import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.meta.Message;

class MetaDataResourceTransformer implements ResourceTransformer 
{

    private ServletContext context = null;
    private DebugLogger log;
  
    public MetaDataResourceTransformer ( ServletContext context )
    {
        this.context = context;
        
        String webAppName = ServletUtils.getWebAppContextPath(context); 
        log = DebugLogger.getLogger(webAppName, getClass());
        
    }

    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        log.debug("transformData(): Returning a new Message object ...");

        WebAppURIResolver resolver = new WebAppURIResolver( context );

        // (use encoding specified in the XML document instead of enc)
        Message msg = new Message(url, resolver);

        if ( log.isDebugEnabled() ) {
           log.debug("transformData(): \nMessage Object Description :\n " + msg.describe() );
        }

        return msg;

    }


    public List getReferredResources(URL url, String enc) 
    {
        WebAppURIResolver resolver = new WebAppURIResolver( context );
        // (use encoding specified in the XML document instead of enc)
        try {
            
            Message msg = new Message(url, resolver);
        }
        catch (Exception e) {
            log.warn("Failed to parse meta to obtain included resources: " + e.getMessage());
        }
        
        Set links =  resolver.getAllResolvedURIs();
        

       return (new ArrayList(links));
       
    }
  
}

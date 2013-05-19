/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * PropertyDataResourceTransformer.java
 *
 *
 * Created: Wed Sep 11 16:24:22 2002
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/PropertyDataResourceTransformer.java#1 $
 */

package com.nightfire.webgui.core.resource;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

import com.nightfire.webgui.core.ServletUtils;

import java.net.URL;
import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import com.nightfire.framework.debug.*;
import java.util.List;

class PropertyDataResourceTransformer implements ResourceTransformer 
{
    private DebugLogger log;

    public PropertyDataResourceTransformer ()
    {
      log = DebugLogger.getLoggerLastApp(getClass());
    }
    

    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        log.debug("transformData(): Returning a new Properties object ...");
        
        InputStream inputStream = null;
        
        Properties properties = new Properties();
        try
        {
            // properties must use ISO-8859-1 encoding
            inputStream           = url.openStream();
            properties.load(inputStream);
            
            if (log.isDebugEnabled())
            {
                log.debug("transformData(): Loaded [" + properties.size() + "] properties.");
            }
        }
        catch (Exception e)
        {
            String errorMessage = "PropertyDataResourceTransformer.transformData(): Failed to load properties [" + url.toString() +"] :\n" + e.getMessage();
            log.error(errorMessage);
        }
        finally
        {
            try
            {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (Exception e)
            {
                log.error("transformData(): Failed to close the input stream:\n" + e.getMessage());
            }
        }
        
        return properties;
    }

    public List getReferredResources(URL url, String enc) 
    {
        return null;
    }
    
        
}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * TemplateDataResourceTransformer.java
 *
 *
 * Created: Wed Sep 11 16:24:22 2002
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/TemplateDataResourceTransformer.java#1 $
 */

package com.nightfire.webgui.core.resource;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.transformer.XSLMessageTransformer;

import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;

import java.net.URL;
import java.io.IOException;
import java.util.List;

class TemplateDataResourceTransformer implements ResourceTransformer 
{
    
    public TemplateDataResourceTransformer ()
    {
    
    }


    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        // enc is ignored in favor of the XML document's specified encoding

        
        try
        {
            // returns a plain generator so that the other resource can create
            // new template generators each time.
            return new XMLPlainGenerator(url.openStream());
        }
        catch (IOException ex)
        {
            throw new FrameworkException("Could not open URL [" + url +
                                         "]: " + ex);
        }
    }

    public List getReferredResources(URL url, String enc) 
    {
        return null;
    }  

}

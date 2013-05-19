/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * XmlDocumentResourceTransformer.java
 *
 *
 * Created: Wed Sep 11 16:24:22 2002
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/XmlDocumentResourceTransformer.java#1 $
 */

package com.nightfire.webgui.core.resource;

import java.io.IOException;
import java.net.URL;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.util.xml.XMLRefLinker;
import com.nightfire.framework.util.Debug;
import org.w3c.dom.Document;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.webgui.core.ServletUtils;
import java.util.List;

class XmlDocumentResourceTransformer implements ResourceTransformer 
{
    public XmlDocumentResourceTransformer ()
    {
        
    }
    
    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        // enc is ignored in favor of the XML document's specified encoding
        
             
        try
        {
            // returns a plain generator so that the other resource can create
            // new template generators each time.
            Document doc = new XMLPlainGenerator(url.openStream()).getDocument();

            // resolve id links within the xml document file
            XMLRefLinker linker = new XMLRefLinker(doc);
            doc = linker.resolveRefs();
            
            return doc;
            

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

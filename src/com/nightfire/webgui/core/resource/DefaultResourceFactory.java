/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header:$
 */

package com.nightfire.webgui.core.resource;
import com.nightfire.framework.util.*;
import javax.servlet.ServletContext;
import java.util.*;
import com.nightfire.framework.debug.*;




/**
 * Default TransformerFactory that handles XML_DOC_DATA
 * and PROPERTY_DATA.
 */
public class DefaultResourceFactory implements ResourceTransformerFactory
{

    /**
     * Indicates an XML Document resource type (org.w3c.dom.Document).
     */
    public static final String XML_DOC_DATA       = "XML_DOC_DATA";

    /**
     * Indicates a resource which holds properties.
     */
    public static final String PROPERTY_DATA       = "PROPERTY_DATA";


    private DebugLogger log;
    

    private HashMap transformers;

    public DefaultResourceFactory()
    {
        log = DebugLogger.getLoggerLastApp(getClass());
        transformers = new HashMap();
        transformers.put(XML_DOC_DATA, new XmlDocumentResourceTransformer());
        transformers.put(PROPERTY_DATA, new PropertyDataResourceTransformer());
    }


    
    /**
     * Can be used to add more transformer types.
     *
     * @param type a <code>String</code> value
     * @param transformer an <code>Object</code> value
     */
    public void addTransformer(String type, Object transformer)
    {
        transformers.put(type, transformer);
    }

    
    public ResourceTransformer create(String type) throws FrameworkException
    {
        if (transformers.containsKey(type)) {
            ResourceTransformer rt = (ResourceTransformer)transformers.get(type);
            
                
            if (log.isDebugEnabled()) 
                log.debug("Creating a transformer: Type [" + type +"], class [" + rt.getClass().getName() +"]");
 
            return rt;
            
        }
        else
            {
                String errorMessage = "Encountered an unknown resource type [" + type + "].";

                log.error(errorMessage);

                return null;
            }
    }
}







    

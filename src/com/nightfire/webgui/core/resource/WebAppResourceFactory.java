/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header:$
 */

package com.nightfire.webgui.core.resource;
import com.nightfire.framework.util.*;
import javax.servlet.ServletContext;


/**
 * Added META_DATA and TEMPLATE_DATA types
 */
public class WebAppResourceFactory extends DefaultResourceFactory
{

    /**
     * Indicates a meta-data resource type (com.nightfire.webgui.core.meta.Message).
     */
    public static final String META_DATA         ="META_DATA";

    /**
     * Indicates a data-template resource type.
     */
    public static final String TEMPLATE_DATA     = "TEMPLATE_DATA";


    /**
     * Indicates a resource which holds XSL Transformer.
     */
    public static final String XSL_TRANSFORM_DATA       = "XSL_TRANSFORM_DATA";

    /**
     * Indicates a alias resource type. The alias resource type maps to a Map object.
     */
    public static final String ALIAS_DATA     = "ALIAS_DATA";


    protected ServletContext servletContext = null;

    /**
     * Sets the Servlet Context 
     *
     * @param  servletContext  The Servlet Context 
     *
     */
    public WebAppResourceFactory( ServletContext servletContext )
    {
        super();
        this.servletContext = servletContext;
        
        addTransformer(META_DATA, new MetaDataResourceTransformer(servletContext));
        addTransformer(TEMPLATE_DATA, new TemplateDataResourceTransformer());
        addTransformer(XSL_TRANSFORM_DATA,new XslTransformerResourceTransformer( servletContext ));
        addTransformer(ALIAS_DATA,new AliasDataResourceTransformer( servletContext ));        
    }

}


    

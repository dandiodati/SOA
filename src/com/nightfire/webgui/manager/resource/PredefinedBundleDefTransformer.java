/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.resource;

import  java.net.URL;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.common.xml.*;

import  com.nightfire.webgui.core.resource.*;
import  com.nightfire.webgui.manager.svcmeta.BundleDef;
import com.nightfire.framework.message.util.xml.XMLRefLinker;
import java.util.List;


/**
 * A manager-specific resource transformer, which transforms the specified resource
 * into the BundleDef object.
 */
 
public class PredefinedBundleDefTransformer implements ResourceTransformer 
{
    public Object transformData(String xml) throws FrameworkException
    {
        // Not implemented.
        
        return null;
    }
    
    public Object transformData(URL url, String enc) throws FrameworkException
    {
        try {
            
            // read the xml from the stream
            XMLGenerator defGenerator = new XMLPlainGenerator(url.openStream());   
            // resolve any references
            XMLRefLinker linker = new XMLRefLinker(defGenerator.getDocument());
            // create a new plain generator with the all refs resolved
            defGenerator = new XMLPlainGenerator(linker.resolveRefs());
            
            return defGenerator;

        }
        catch (Exception e) {
            throw new FrameworkException(e.getMessage());
        }
    }
    


    /** 
     * Returns included references from the generated resource.
     */
    public List getReferredResources(URL url, String enc) 
    {
        return null;
    }  
        
       

}

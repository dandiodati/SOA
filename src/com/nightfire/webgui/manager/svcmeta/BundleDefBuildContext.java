/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.svcmeta;

// jdk imports
import java.io.*;
import java.net.*;
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.framework.debug.*;



    /**
     * Maintains context information while constructing a Service object from
     * an XML document
     */
public class BundleDefBuildContext extends BuildContext
{

    /** Index of global service component definitions */
    private HashMap svcDefs = new HashMap();


    /**
     * Constructor
     */
    public BundleDefBuildContext() throws FrameworkException
    {
      	super();      	
    }


    /**
     * Registers an service component definition
     */
    public void registerSvcDef(ComponentDef def)
    {
        String name = def.getID();
        if (!StringUtils.hasValue(name))
            return;

        if (svcDefs.containsKey(name))
            log.warn("Overriding svc definition with id [ "
                     + name + "].");

        svcDefs.put(name, def);
    }

    /**
     * Returns a registered global service component definition
     * , or null if not found
     */
    public ComponentDef getSvcDef(String name)
    {
        if (!svcDefs.containsKey(name))
            return null;

        return (ComponentDef)svcDefs.get(name);
    }

}


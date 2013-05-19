/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports

// nightfire imports
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

/**
 * ResourceLoader handles obtaining resources (such as referenced XML documents
 * needed by the Message definition framework.
 */
public abstract class ResourceLoader
{
    /**
     * Property containing the class name to use
     */
    public static final String CLASS_NAME_PROP =
        "com.nightfire.webgui.ResourceLoaderClassName";
    /**
     * Default class name
     */
    public static final String DEFAULT_CLASS =
        "com.nightfire.webgui.core.meta.ClassLoaderResourceLoader";

    /**
     * Instance
     */
    private static ResourceLoader inst = null;

    /**
     * Obtains an XML document resource
     *
     * @param name  The name of the resource
     *
     * @return The parsed message in an XMLMessageParser
     *
     * @exception FrameworkException Thrown if the resource cannot be
     *                               obtained or parsed
     */
    public abstract XMLMessageParser getXMLResource(String name)
        throws FrameworkException;
    
    /**
     * Obtains a string resource
     *
     * @param name  The name of the resource
     *
     * @return The resources as a string
     *
     * @exception FrameworkException Thrown if the resource cannot be
     *                               obtained
     */
    public abstract String getStringResource(String name)
        throws FrameworkException;

    /**
     * Obtains a binary resource
     *
     * @param name  The name of the resource
     *
     * @return The resources as a byte array
     *
     * @exception FrameworkException Thrown if the resource cannot be
     *                               obtained
     */
    public abstract byte[] getBinaryResource(String name)
        throws FrameworkException;

    /**
     * Obtains an instance of ResourceLoader
     */
    public static ResourceLoader getInstance() throws FrameworkException
    {
        // see if we're already set up
        if (inst != null)
            return inst;

        synchronized (ResourceLoader.class)
        {
            if (inst == null)
            {
                try
                {
                    // get the class to use
                    String className = System.getProperty(CLASS_NAME_PROP,
                                                          DEFAULT_CLASS);

                    Debug.log(Debug.MSG_STATUS,
                              "ResourceLoader creating an instance of ["
                              + className + "].");

                    Class implementor = Class.forName(className);

                    // create an instance
                    inst = (ResourceLoader)implementor.newInstance();
                }
                catch (Exception ex)
                {
                    throw new FrameworkException(ex);
                }
            }

            return inst;
        }
    }
}

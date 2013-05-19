/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.io.*;

// nightfire imports
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

/**
 * ClassLoaderResourceLoader first tries to use the local file system to
 * load requested resources and fails over to the system class loader
 * if no such file exists.
 */
public class ClassLoaderResourceLoader extends ResourceLoader
{
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
    public XMLMessageParser getXMLResource(String name)
        throws FrameworkException
    {
        // get the string
        String xml = getStringResource(name);

        // parse it
        return new XMLMessageParser(xml);
    }

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
    public String getStringResource(String name)
        throws FrameworkException
    {
        Reader r = null;
        int bytesRead = 0;
        char[] buff = new char[1024];
        StringBuffer sb = new StringBuffer();

        // see if there is such a file
        File f = new File(name);
        if (f.exists())
        {
            try
            {
                // read from the local file
                r = new BufferedReader(new FileReader(f));
            }
            catch (Exception ex)
            {
                throw new FrameworkException(ex);
            }
        }
        else // otherwise, use the class loader to locate it
        {
            InputStream is = ClassLoader.getSystemResourceAsStream(name);
            if (is == null)
                throw new FrameworkException("Could not locate resource [" +
                                             name + "]");
            
            r = new BufferedReader(new InputStreamReader(is));
        }

        // read the whole document
        try
        {
            while ( (bytesRead = r.read(buff)) != -1)
            {
                sb.append(buff, 0, bytesRead);
            }
        }
        catch (IOException ex)
        {
            throw new FrameworkException(ex);
        }
        finally
        {
            try { r.close(); }
            catch (Exception ex)
            {
                Debug.log(Debug.ALL_WARNINGS, "Could not close resource ["
                          + name + "]: " + ex);
            }
        }

        // return it
        return sb.toString();
    }

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
    public byte[] getBinaryResource(String name)
        throws FrameworkException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        int bytesRead = 0;
        byte[] buff = new byte[1024];

        // see if there is such a file
        File f = new File(name);
        if (f.exists())
        {
            try
            {
                // read from the local file
                is = new BufferedInputStream(new FileInputStream(f));
            }
            catch (Exception ex)
            {
                throw new FrameworkException(ex);
            }
        }
        else // otherwise, use the class loader to locate it
        {
            InputStream sysIs = ClassLoader.getSystemResourceAsStream(name);
            if (sysIs == null)
                throw new FrameworkException("Could not locate resource [" +
                                             name + "]");
            
            is = new BufferedInputStream(sysIs);
        }

        // read the whole document
        try
        {
            while ( (bytesRead = is.read(buff)) != -1)
            {
                bos.write(buff, 0, bytesRead);
            }
        }
        catch (IOException ex)
        {
            throw new FrameworkException(ex);
        }
        finally
        {
            try { is.close(); }
            catch (Exception ex)
            {
                Debug.log(Debug.ALL_WARNINGS, "Could not close resource ["
                          + name + "]: " + ex);
            }
        }

        // return it
        return bos.toByteArray();
    }
}

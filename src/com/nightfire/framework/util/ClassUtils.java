/**
 * Copyright (c) 2004 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.util;

import java.net.URL;
import java.net.MalformedURLException;

import java.io.File;

import java.util.Set;
import java.util.HashSet;

/**
 * <p>Title: NFI</p>
 * <p>Description: Allows the identification of the origin of a class loaded in memory.</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: NightFire</p>
 * @author Thanh
 * @version 1.0
 */
public class ClassUtils
{
    public static String getOrigin( Object o )
    {
        String origin = "";

        if( o == null )
        {
            Debug.warning("Tried to lookup a null object");
            return origin;
        }
        String name = o.getClass().getName();

        // This is not synchronized
        if( previouslyQueried.contains( name ) )
            return origin;

        previouslyQueried.add( name );
        origin = getSource( o );

        if( origin == "" )
            origin = getResource( o );

        return "Traced up origin to: " + origin;
    }

    public static String getSource( Object o )
    {
        String source = "";
        try
        {
            Debug.log(Debug.UNIT_TEST, "looking up source for " + o.getClass().getName() );

            Class cls = o.getClass();

            if (cls == null)
                throw new IllegalArgumentException ("null input: cls");

            URL result = null;
            final String clsAsResource = cls.getName ().replace ('.', '/').concat (".class");

            final java.security.ProtectionDomain pd = cls.getProtectionDomain ();
            // java.lang.Class contract does not specify if 'pd' can ever be null;
            // it is not the case for Sun's implementations, but guard against null
            // just in case:
            if (pd != null)
            {
                final java.security.CodeSource cs = pd.getCodeSource ();
                // 'cs' can be null depending on the classloader behavior:
                if (cs != null)
                    result = cs.getLocation ();

                if (result != null)
                {
                    // Convert a code source location into a full class file location
                    // for some common cases:
                    if ("file".equals (result.getProtocol ()))
                    {
                        try
                        {
                            if (result.toExternalForm ().endsWith (".jar") ||
                                result.toExternalForm ().endsWith (".zip"))
                                result = new URL ("jar:".concat (result.toExternalForm ())
                                    .concat("!/").concat (clsAsResource));
                            else if (new File (result.getFile ()).isDirectory ())
                                result = new URL (result, clsAsResource);
                        }
                        catch (MalformedURLException ignore)
                        {
                        }
                    }
                    source = result.toString();
                }
            }
        }
        catch( Exception e )
        {
            Debug.error( "Failed getting source: " + e.toString() );
            Debug.logStackTrace( e );
        }
        return source;
    }

    public static String getResource( Object o )
    {
        String ressource = "";
        try
        {
            Debug.log(Debug.UNIT_TEST, "looking up ressource for " + o.getClass().getName() );
            Class cls = o.getClass();

            if (cls == null)
                throw new IllegalArgumentException ("null input: cls");

            URL result = null;
            final String clsAsResource = cls.getName ().replace ('.', '/').concat (".class");

            // Try to find 'cls' definition as a resource; this is not
            // documented to be legal, but Sun's implementations seem to allow this:
            final ClassLoader clsLoader = cls.getClassLoader ();

            result = clsLoader != null ?
                clsLoader.getResource (clsAsResource) :
                ClassLoader.getSystemResource (clsAsResource);

            if( result != null )
                ressource = result.toString();
        }
        catch( Exception e )
        {
            Debug.error( "Failed getting ressource: " + e.toString() );
            Debug.logStackTrace( e );
        }
        return ressource;
    }

    private static Set previouslyQueried = new HashSet();
}
/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header:$
 */

package com.nightfire.framework.util;

import java.io.*;
import java.net.*;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

/**
 * This class provides utility methods for
 */
public abstract class URLUtils
{
    /**
    * Changes the windows file separators ("\") to the URL file separator.
    *
    * @param url -- url string to be transformed
    *
    * @return The url input String containing only URL-like separators
    */
    public static String fixUrlSeparator(String url)
    {
        url = url.replace('\\', '/');

        return url;
    }

    /**
     * Reads the content of the resource at the URL location and return the
     * content as String
     *
     * @param location URL location of the resource to read
     *
     * @return The content of the resource at the provided location
     *
     * @exception FrameworkException thrown if the resource cannot be read.
     */
    public static String getUrlResource( String location ) throws FrameworkException
    {
        location = fixUrlSeparator( location );

        BufferedReader in = null;

        try
        {
            URL url = new URL( location );
            in = new BufferedReader( new InputStreamReader( url.openStream() ) );

            String inputLine;
            StringBuffer output = new StringBuffer();

            while ((inputLine = in.readLine()) != null){

                output.append( inputLine );
                output.append( "\n" );
            }

            if( Debug.isLevelEnabled( Debug.IO_DATA ) )
                Debug.log( Debug.IO_DATA, "content=[" + output + "]" );
            return output.toString();
        }
        catch(Exception e)
        {
            Debug.error( e.toString() );
            throw new FrameworkException( e );
        }
        finally{

            if(in != null )
            {
                try
                {
                    in.close();
                }
                catch(IOException ioe)
                {
                    Debug.warning( "Couldn't close stream: " + ioe.getMessage() );
                }
            }
        }
    }

    /**
     * Return a String with a description of the URL passed as input.
     *
     * @param url URL to describe
     *
     * @return The URL description
     *
     */
    public static String describeUrl(URL url)
    {
        if( url == null)
            return null;

        StringBuffer sb = new StringBuffer();
        sb.append( "Protocol [" );
        sb.append( url.getProtocol() );
        sb.append( "]\nHost [" );
        sb.append( url.getHost() );
        sb.append( "]\nPort [" );
        sb.append( url.getPort() );
        sb.append( "]\nPath [" );
        sb.append( url.getPath() );
        sb.append( "]\nRef [" );
        sb.append( url.getRef() );
        sb.append( "]" );

        return sb.toString();
    }

    /**
     * Test if a path is a URL path
     *
     * @param path String containing the path to test
     *
     * @return true if the path is a URL and false otherwise.
     */
    public static boolean isURL( String path )
    {
        boolean isUrl = true;

        try{

            URL url = new URL( path );
            if( Debug.isLevelEnabled( Debug.IO_STATUS ) ){

                Debug.log( Debug.IO_STATUS,
                    "Path [" + url + "] is a URL:\n" + describeUrl( url ) );
            }

        }
        catch(MalformedURLException mue)
        {
            isUrl = false;
        }

        return isUrl;
    }
}

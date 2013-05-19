/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;
import com.nightfire.framework.cache.*;


/**
 * The FileCache class provides general-purpose file caching methods.
 */
public class FileCache implements CachingObject
{
    /**
     * Constructor for non-transforming file cache.
     */
    public FileCache ( )
    {
        this( null );
    }


    /**
     * Constructor for object that will cache file's transformed contents.
     *
     * @param  transformer  String transformer to apply to file
     *                      contents before caching.
     */
    public FileCache ( StringTransformer transformer ) 
    {
        this.transformer = transformer;

        availableFiles = new Hashtable( );

        CacheManager.getRegistrar().register( this );

    }


    /**
     * Get the file contents given the file name.  If file is in cache, its value will be returned.
     * Otherwise, file will first be loaded from disk and placed into cache before returning it.
     *
     * @param  fileName  The name of the file.
     *
     * @return  The associated file contents.
     *
     * @exception  FrameworkException  Thrown if file isn't in cache and can't be read, or isn't a String.
     */
    public synchronized String get ( String fileName ) throws FrameworkException
    {
        Object value = getObject( fileName );

        if ( value instanceof String )
            return( (String)value );
        else
        {
            throw new FrameworkException( "ERROR: Attempt to get String from file cache value which is not a String [" +
                                          value.getClass().getName() + "]." );
        }
    }


    /**
     * Get the file contents given the file name.  If file is in cache, its value will be returned.
     * Otherwise, file will first be loaded from disk and placed into cache before returning it.
     *
     * @param  fileName  The name of the file.
     *
     * @return  The associated file contents (possibly transformed).
     *
     * @exception  FrameworkException  Thrown if file isn't in cache and can't be read.
     */
    public synchronized Object getObject ( String fileName ) throws FrameworkException
    {
        Debug.log( this, Debug.IO_STATUS, "Checking for file named [" + fileName + "] in file cache ..." );

        // Check to see if file is already in cache.
        Object fileContents = availableFiles.get( fileName );

        // Found it in cache, so return contents.
        if ( fileContents != null )
        {
            Debug.log( this, Debug.IO_STATUS, "\tFile found in cache, so returning its contents." );

            return( fileContents );
        }

        // File not in cache, so read it in and store in cache for subsequent re-use.
        fileContents = FileUtils.readFile( fileName );

        // If transformer is available, apply it to the file's contents.
        if ( transformer != null )
            fileContents = transformer.transform( (String)fileContents );

        putObject( fileName, fileContents );

        return fileContents;
    }


    /**
     * Place the file contents in the cache, with it's name as a key.
     *
     * @param  fileName      The name of the file.
     * @param  fileContents  The contents of the file.
     */
    public synchronized void put ( String fileName, String fileContents )
    {
        putObject( fileName, fileContents );
    }


    /**
     * Place the file contents in the cache, with it's name as a key.
     *
     * @param  fileName      The name of the file.
     * @param  fileContents  The contents of the file.
     */
    public synchronized void putObject ( String fileName, Object fileContents )
    {
        Debug.log( this, Debug.IO_STATUS, "Placing file contents in cache with access key ["+ fileName + "]." );
        
        Object obj = availableFiles.put( fileName, fileContents );

        if ( obj != null )
        {
            Debug.log( this, Debug.ALL_WARNINGS, "WARNING:  File already exists in cache, so replacing its cache value." );
        }
    }


    /**
     * Test to see if file is in cache.
     *
     * @param  fileName  The name of the file to test.
     *
     * @return  'true' if cache contains file, otherwise 'false'.
     */
    public synchronized boolean contains ( String fileName )
    {
        return( availableFiles.contains( fileName ) );
    }


    /**
     * Remove file from cache.
     *
     * @param  fileName  The name of the file to remove from cache.
     */
    public synchronized void remove ( String fileName )
    {
        Debug.log( this, Debug.IO_STATUS, "Removing file with access key ["+ fileName + "] contents in cache." );

        Object obj = availableFiles.remove( fileName );

        if ( obj == null )
        {
            Debug.log( this, Debug.ALL_WARNINGS, "WARNING:  File not in cache, so can't remove its value." );
        }
    }


    /**
     * Log all keys found in the cache to diagnostic/debugging log.
     */
    public synchronized void logKeys ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Current file cache contents:\n" );

        Enumeration e = availableFiles.keys( );

        while ( e.hasMoreElements() )
        {
            String name = (String)e.nextElement( );
            
            Object obj = availableFiles.get( name );

            String type = null;

            if ( obj != null )
                type = obj.getClass().getName( );

            sb.append( '\t' );
            sb.append( name );
            sb.append( " (" );
            sb.append( type );
            sb.append( ")\n" );
        }

        Debug.log( null, Debug.IO_DATA, sb.toString() );
    }

    /**
     * Method invoked by the cache-flushing infrastructure
     * to indicate that the cache should be emptied.
     *
     * @exception FrameworkException if cache cannot be cleared.
     */
    public synchronized void flushCache ( ) throws FrameworkException
    {
        availableFiles.clear();
    }

    private Hashtable availableFiles;

    private StringTransformer transformer;
}

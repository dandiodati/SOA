/**
 * Copyright 2004 NeuStar, Inc.
 * All rights reserved.
 */
 
package com.nightfire.framework.util;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;

/**
 * This generic class redirects the standard output to the log file used by the
 * Debug class for this JVM.
 * A user of this class only needs to invoke StdOutLogger.initialize() to redirect the
 * standard output to the log file associated with this JVM.
 */
public class StdOutLogger extends PrintStream
{

    /**
     * Constructor to mimic PrintStream behaviour.
     */
    private StdOutLogger ( OutputStream out )
    {
        super( out );
    }

    /**
     * Redirect the System.out to the standard debug logs by invoking StdOutLogger.initialize().
     * This is all that needs to be invoked. The actual re-direction of the bytes will take
     * place in the methods that override those in PrintStream, behind the scence.
     *
     * The StdOutLogger will redirect the standard output to the debug log file, only if
     * it has not already been done.
     */
    public static void initialize()
    {
        if( !(System.out instanceof StdOutLogger) )
        {
            synchronized ( StdOutLogger.class )
            {
                // redirect standard output to debug log file.
                if( !(System.out instanceof StdOutLogger) )
                {
                    //The ByteArrayOutputStream won't be used, but will be created only
                    //once per VM.
                    System.setOut( new StdOutLogger( new ByteArrayOutputStream() ) );
                }

            }
        }
    }//initialize

    /**
     * Writes len bytes from the specified byte array starting at offset off to
     * this byte array output stream.
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     */
    public void write( byte b[], int off, int len )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(b, off, len);

        if( Debug.isLevelEnabled(Debug.IO_DATA) )
            Debug.log(Debug.IO_DATA, baos.toString());

        try
        {
            baos.close();
        }
        catch ( IOException e )
        {
            Debug.warning( "Could not close stream: " + e.toString() );
        }
    }//write

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param b the byte to be written.
     */
    public void write( int b )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(b);
        if( Debug.isLevelEnabled(Debug.IO_DATA) )
            Debug.log(Debug.IO_DATA, baos.toString());

        try
        {
            baos.close();
        }
        catch ( IOException e )
        {
            Debug.warning( "Could not close stream: " + e.toString() );
        }

    }//write

}

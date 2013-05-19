/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia;

// jdk imports
import java.io.*;

// third party imports

// nightfire imports
import com.nightfire.framework.util.*;
import com.nightfire.comms.ia.asn.HexFormatter;

/**
 * This is an input stream filter that can display additional information on
 * the data read from the underlying input stream.
 */
public class ReadFullyInputStream extends FilterInputStream
{
    // public methods

    /**
     * Constructor
     *
     * @param in The underlying input stream
     */
    public ReadFullyInputStream(InputStream in)
    {
        super(in);
    }

    /**
     * Reads and display the byte array read.
     *
     * @param b   The buffer into which the data is read
     * @param off The start offset in b for the data
     * @param len The number of bytes to read
     *
     * @return The number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     *
     * @exception IOException Thrown if an I/O error occurs in the underlying
     *                        stream.
     */
    public int read(byte[] b, int off, int len) throws IOException
    {
        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST, "Off " + off + "\nlen " + len);

        int res = super.read(b, off, len);

        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
            Debug.log( Debug.UNIT_TEST, new HexFormatter(b).toString() );

        return res;
    }
}

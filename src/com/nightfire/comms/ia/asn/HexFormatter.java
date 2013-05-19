/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia.asn;

// jdk imports

// third party imports

// NightFire imports
import com.nightfire.framework.util.Debug;

/**
 * Formatts binary data in a hex/ascii combo suitable for logging.
 */
public class HexFormatter
{
    /**
     * Formatted data.
     */
    private String data = "";

    /*
     * Constants
     */
    private static final int LINE_LENGTH            = 78;
    private static final int PER_CHARACTER_OVERHEAD = 4;
    private static final int PER_LINE_OVERHEAD      = 2;
    private static final int CHARS_PER_LINE         = 19;
    private static final int HEX_LEN                = 57;

    /**
     * Constructor.
     *
     * @param bytes   The array of bytes to format.
     */
    public HexFormatter(byte[] bytes)
    {
        // format the data
        format(bytes);
    }

    /**
     * Returns the formatted data.
     *
     * @return The formatted data
     */
    public String toString()
    {
        return data;
    }

    /**
     * Performs formatting for the data.
     *
     * @param bytes  The data to format.
     *
     * @exception    IOException  Thrown if an error occurs while formatting.
     */
    private void format(byte[] bytes)
    {
        // final buffer
        StringBuffer buff = new StringBuffer(bytes.length *
                                             PER_CHARACTER_OVERHEAD +
                                             bytes.length / CHARS_PER_LINE *
                                             PER_LINE_OVERHEAD + 1);
        // buffer for ascii line
        StringBuffer asciiBuff = new StringBuffer(LINE_LENGTH);
        // buffer for hex line
        StringBuffer hexBuff = new StringBuffer(LINE_LENGTH);

        // step through the entire array
        for (int i = 0; i < bytes.length; i++)
        {
            // check for the need to move to a new line
            if (asciiBuff.length() >= CHARS_PER_LINE)
            {
                // dump the hex
                buff.append(hexBuff.toString());
                buff.append("[");

                // dump the ascii
                buff.append(asciiBuff.toString());
                buff.append("]");

                // move to the next newline
                buff.append("\n");

                // clear the buffers
                hexBuff.setLength(0);
                asciiBuff.setLength(0);
            }

            // format the hex
            String strHex = Integer.toHexString(((int)bytes[i]) & 0x000000FF);
            if (strHex.length() < 2)
                hexBuff.append("0");
            hexBuff.append(strHex.toUpperCase());
            hexBuff.append(" ");

            // format the ascii
            if (bytes[i] >= ' ')
                asciiBuff.append((char)bytes[i]);
            else
                asciiBuff.append(".");
        }

        // write the last line

        // write the hex
        buff.append(hexBuff.toString());

        // padd to where ascii begins
        for (int i = hexBuff.length(); i < HEX_LEN; i++)
            buff.append(" ");

        // write the ascii
        buff.append("[");
        buff.append(asciiBuff.toString());
        buff.append("]");

        // save the text
        data = buff.toString();
    }
}

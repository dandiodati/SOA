/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia3.asn;

// jdk imports

// third party imports

// NightFire imports
import com.nightfire.framework.util.Debug;

/**
 * StringEncoding is a utility for converting between byte arrays and
 * Strings using a specific character encoding.
 */
public class StringEncoding
{
    /** The character encoding to use */
    private String enc = null;

    /**
     * Default constructor
     */
    public StringEncoding()
    {
    }

    /**
     * Constructor for a character encoding
     *
     * @param enc  The character encoding to use
     */
    public StringEncoding(String enc)
    {
        this.enc = enc;
    }

    /**
     * Gets the character encoding used for byte conversion.
     */
    public String getEncoding()
    {
        return enc;
    }

    /**
     * Sets the character encoding used for byte conversion.
     */
    public void setEncoding(String enc)
    {
        this.enc = enc;
    }

    /**
     * Converts a byte array to a string using our character encoding
     */
    public String getString(byte[] bytes)
    {
        try
        {
            if (enc != null)
                return new String(bytes, enc);
        }
        catch (Exception ex)
        {
            // don't continue to use a bad encoding
            Debug.log(Debug.ALL_WARNINGS, "Unknown encoding [" + enc + "]: "
                      + ex);
            enc = null;
        }

        return new String(bytes);
    }

    /**
     * Converts a string to a byte array using our character encoding
     */
    public byte[] getBytes(String str)
    {
        try
        {
            if (enc != null)
                return str.getBytes(enc);
        }
        catch (Exception ex)
        {
            // don't continue to use a bad encoding
            Debug.log(Debug.ALL_WARNINGS, "Unknown encoding [" + enc + "]: "
                      + ex);
            enc = null;
        }

        return str.getBytes();
    }
}

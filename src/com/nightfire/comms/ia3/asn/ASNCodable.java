/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia3.asn;

// jdk imports
import java.io.InputStream;
import java.io.OutputStream;

// third party imports

// NightFire imports
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;

/**
 * ASNCodable is an interface implemented by objects which can encode or
 * decode themselves per an ASN.1 definition.
 */
public interface ASNCodable
{
    /** The value to use for the DER encoding and decoding scheme */
    public static final String DER_CODING = "DER";

    /**
     * Called to encode the object.
     *
     * @param encoding  The encoding scheme to use
     * @param os        The stream to encode to
     *
     * @exception ProcessingException Thrown if an error occurs while encoding
     * @exception MessageException    Thrown if a data error occurs
     */
    public void encode(String encoding, OutputStream os)
        throws ProcessingException, MessageException;

    /**
     * Called to decode the object.
     *
     * @param decoding  The decoding scheme to use
     * @param is        The stream to decode from
     *
     * @exception ProcessingException Thrown if an error occurs while decoding
     * @exception MessageException    Thrown if a data error occurs
     */
    public void decode(String decoding, InputStream is)
        throws ProcessingException, MessageException;
}

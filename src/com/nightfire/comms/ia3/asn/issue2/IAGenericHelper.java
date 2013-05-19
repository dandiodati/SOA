/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia3.asn.issue2;

// jdk imports
import java.io.InputStream;
import java.io.OutputStream;

// third party imports
import cryptix.asn1.encoding.Factory;
import cryptix.asn1.io.ASNReader;
import cryptix.asn1.io.ASNWriter;

// NightFire imports
import com.nightfire.common.ProcessingException;
import com.nightfire.comms.ia3.asn.ASNCodable;
import com.nightfire.comms.ia3.asn.StringEncoding;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;

/**
 * IAGenericHelper is a helper class for working with IAGeneric.
 */
public class IAGenericHelper implements ASNCodable
{
    /** The actual IAGeneric */
    private IAGeneric genericMsg;
    
    /**
     * Default constructor
     */
    public IAGenericHelper()
    {
        genericMsg = new IAGeneric();
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public IAGenericHelper(String oid)
    {
        genericMsg = new IAGeneric();

        // set the OID
        genericMsg.getContentType().value(oid);
    }

    /**
     * Gets the OID
     */
    public String getContentType()
    {
        return genericMsg.getContentType().stringValue();
    }

    /**
     * Sets the OID
     */
    public void setContentType(String oid)
    {
        genericMsg.getContentType().value(oid);
    }

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
        throws ProcessingException, MessageException
    {
        try
        {
            ASNWriter coder = Factory.getEncoder(encoding);
            coder.open(os);
            genericMsg.encode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IAGeneric encoding failed: " + ex);
        }
    }

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
        throws ProcessingException, MessageException
    {
        try
        {
            ASNReader coder = Factory.getDecoder(decoding);
            coder.open(is);
            genericMsg.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IAGeneric decoding failed: " + ex);
        }
    }
    
    /**
     * Called to decode the object.
     *
     * @param decoding  The decoding scheme to use
     * @param is        The stream to decode from
     *
     * @exception ProcessingException Thrown if an error occurs while decoding
     * @exception MessageException    Thrown if a data error occurs
     */
    public void decode( ASNReader coder )
        throws ProcessingException, MessageException
    {
        try
        {        
            genericMsg.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IAGeneric decoding failed: " + ex);
        }
    }
        
    public String describe()
    {
        return genericMsg.toString();
    }
}

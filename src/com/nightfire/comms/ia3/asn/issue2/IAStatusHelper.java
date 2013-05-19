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
 * IAStatusHelper is a helper class for working with IAStatus.
 */
public class IAStatusHelper implements ASNCodable
{
    /** The actual IAStatus */
    private IAStatus iaStatus;
    
    /**
     * Default constructor
     */
    public IAStatusHelper()
    {
        this(Module.instance().getOID("iaStatusMessage").stringValue());
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public IAStatusHelper(String oid)
    {
        iaStatus = new IAStatus();

        // set the OID
        iaStatus.getIaStatusMessage().value(oid);
    }

    /**
     * Gets the OID
     */
    public String getIAStatusMessage()
    {
        return iaStatus.getIaStatusMessage().stringValue();
    }

    /**
     * Sets the OID
     */
    public void setIAStatusMessage(String oid)
    {
        iaStatus.getIaStatusMessage().value(oid);
    }

    /**
     * Gets the status content
     */
    public byte[] getIAStatus()
    {
        return iaStatus.getIaStatus().byteArrayValue();
    }

    /**
     * Sets the status content
     */
    public void setIAStatus(byte[] status)
    {
        iaStatus.getIaStatus().value(status);
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
            iaStatus.encode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IAStatus encoding failed: " + ex);
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
            iaStatus.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IAStatus decoding failed: " + ex);
        }
    }
    
    
    public String describe()
    {
        return iaStatus.toString();
    }
}

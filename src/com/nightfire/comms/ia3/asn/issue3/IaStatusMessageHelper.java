/**
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia3.asn.issue3;

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
 * IaStatusMessageHelper is a helper class for working with IaStatusMessage.
 */
public class IaStatusMessageHelper implements ASNCodable
{
    /** The actual IaStatusMessage */
    private IaStatusMessage ias;
    
    /**
     * Default constructor
     */
    public IaStatusMessageHelper()
    {
        this(Module.instance().getOID("interactiveAgent").stringValue());
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public IaStatusMessageHelper(String oid)
    {
        ias = new IaStatusMessage();

        // set the OID
        //IaStatusMessage.getIaStatusMessage().value(oid);
    }   

    /**
     * Gets the status content
     */
    public byte[] getIaStatusMessage()
    {
        return ias.byteArrayValue();
    }

    /**
     * Sets the status content
     */
    public void setIaStatusMessage(byte[] status)
    {
        ias.value(status);
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
            ias.encode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IaStatusMessage encoding failed: " + ex);
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
            ias.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IaStatusMessage decoding failed: " + ex);
        }
    }
    
    
    public String toString()
    {
        return ias.toString();
    }
}

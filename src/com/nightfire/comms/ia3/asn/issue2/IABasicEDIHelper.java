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
 * IABasicEDIHelper is a helper class for working with IABasicEDI.
 */
public class IABasicEDIHelper implements ASNCodable
{
    /** The actual IABasicEDI */
    private IABasicEDI basicEDI;
    
    /** The character encoding to use */
    private StringEncoding enc = new StringEncoding("US-ASCII");

    /**
     * Default constructor
     */
    public IABasicEDIHelper()
    {
        this(Module.instance().getOID("plainEDImessage").stringValue());
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public IABasicEDIHelper(String oid)
    {
        basicEDI = new IABasicEDI();

        // set the OID
        basicEDI.getPlainEDImessage().value(oid);
    }

    /**
     * Gets the character encoding used for byte conversion.
     * This is not the same as the encoding scheme used for an encode or
     * decode operation.
     */
    public String getEncoding()
    {
        return enc.getEncoding();
    }

    /**
     * Sets the character encoding used for byte conversion.
     * This is not the same as the encoding scheme used for an encode or
     * decode operation.
     */
    public void setEncoding(String enc)
    {
        this.enc.setEncoding(enc);
    }

    /**
     * Gets the OID
     */
    public String getPlainEDIMessage()
    {
        return basicEDI.getPlainEDImessage().stringValue();
    }

    /**
     * Sets the OID
     */
    public void setPlainEDIMessage(String oid)
    {
        basicEDI.getPlainEDImessage().value(oid);
    }

    /**
     * Gets the EDI content
     */
    public String getEDIContent()
    {
        return enc.getString(basicEDI.getEdiContent().byteArrayValue());
    }

    /**
     * Sets the EDI content
     */
    public void setEDIContent(String edi)
    {
        basicEDI.getEdiContent().value(enc.getBytes(edi));
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
            basicEDI.encode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IABasicEDI encoding failed: " + ex);
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
            basicEDI.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IABasicEDI decoding failed: " + ex);
        }
    }
    
    
    public String describe()
    {
       return  basicEDI.toString();
    }
}

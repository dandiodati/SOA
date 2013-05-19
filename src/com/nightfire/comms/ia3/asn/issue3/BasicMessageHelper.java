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
 * BasicMessageHelper is a helper class for working with BasicMessage.
 */
public class BasicMessageHelper implements ASNCodable
{
    /** The actual BasicMessage */
    private BasicMessage basicEDI;
    
    /** The character encoding to use */
    private StringEncoding enc = new StringEncoding("US-ASCII");

    /**
     * Default constructor
     */
    public BasicMessageHelper()
    {
        this(Module.instance().getOID("interactiveAgent").stringValue());
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public BasicMessageHelper(String oid)
    {
        basicEDI = new BasicMessage( oid );
        // set the OID
        //basicEDI.getPlainEDImessage().value(oid);
    }
    
    public BasicMessage getBasicMessage()
    {
        return basicEDI;
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
     * Gets the EDI content
     */
    public String getEDIContent()
    {
        //return enc.getString( basicEDI.getBasicMessage2().stringValue().getBytes() );
        return basicEDI.getBasicMessage2().stringValue();
    }

    /**
     * Sets the EDI content
     */
    public void setEDIContent(String edi)
    {
        //String msg = new String( enc.getBytes(edi) );
        
        basicEDI.setBasicMessage2(edi);
    }
    
    public boolean isBasicMessage2() {
      return basicEDI.isBasicMessage2();
   }
    
    public boolean isBasicMessage3() {
      return basicEDI.isBasicMessage3();
   }
     
    public boolean isBasicMessage4() {
      return basicEDI.isBasicMessage4();
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
            throw new ProcessingException("BasicMessage encoding failed: " + ex);
        }
    }
    
    public String getName()
    {
        return basicEDI.name();
    }
    
    public String toString()
    {
        return basicEDI.toString();
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
            throw new ProcessingException("BasicMessage decoding failed: " + ex);
        }
    }
     
}

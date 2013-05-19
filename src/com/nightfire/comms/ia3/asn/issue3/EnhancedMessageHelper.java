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
import java.io.ByteArrayOutputStream;


// third party imports
import cryptix.asn1.encoding.Factory;
import cryptix.asn1.io.ASNReader;
import cryptix.asn1.io.ASNWriter;
import cryptix.asn1.lang.OctetString;

// NightFire imports
import com.nightfire.common.ProcessingException;
import com.nightfire.comms.ia.asn.ASNData;
import com.nightfire.comms.ia3.asn.ASNCodable;
import com.nightfire.comms.ia3.asn.StringEncoding;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;

/**
 * EnhancedMessageHelper is a helper class for working with EnhancedMessage.
 */
public class EnhancedMessageHelper implements ASNCodable
{
    /** The actual EnhancedMessage */
    private EnhancedMessage eMsg;
    
    /** The character encoding to use */
    private StringEncoding enc = new StringEncoding("US-ASCII");  

    /**
     * Default constructor
     */
    
    public EnhancedMessageHelper()      
    {       
       eMsg = new EnhancedMessage(); 
    }
    
    
    public EnhancedMessageHelper( com.nightfire.comms.ia.asn.msg.IaReceiptMessage receipt )
        throws ProcessingException, MessageException
    {       
        
        this(Module.instance().getOID("interactiveAgent").stringValue(), receipt);
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public EnhancedMessageHelper(String oid, com.nightfire.comms.ia.asn.msg.IaReceiptMessage receipt ) 
        throws ProcessingException, MessageException
    {
        
        ByteArrayOutputStream tmpOs = new ByteArrayOutputStream();
       
        receipt.encode(ASNData.DER_CODING, tmpOs);
        byte[] derMsg = tmpOs.toByteArray();
        eMsg = new EnhancedMessage( oid, OctetString.getInstance(derMsg) );        
    }
    
    
    /*
     * This constructor is used as a fix to Cryptix libraries 
     * which does not handle Choice constructs well.  The strategy
     * is to append the byte[] we with the bytes for the following
     * ASN.1 construct, CONTEXT_CONSTRUCTED_2, this is the construct
     * recieved in SBC live testing.
     *
     *  byte[] - the encoded IaReceiptMessage
     *
     */
    public EnhancedMessageHelper( byte[] iaReceiptMessage )
    {       
       
       byte[] fix = new byte[iaReceiptMessage.length + 2];
           
       fix[0] = (byte) 162;  
       fix[1] = (byte) 126;

       int index = 2;
       for ( int k = 0; k < iaReceiptMessage.length; k++ )
       {
          fix[index++] = iaReceiptMessage[k];
       }
        
       eMsg = new EnhancedMessage( OctetString.getInstance( fix ) );
    }
    
    public EnhancedMessage get()
    {
        return eMsg;
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
            eMsg.encode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("EnhancedMessage encoding failed: "
                                          + ex);
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
            eMsg.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("EnhancedMessage decoding failed: "
                                          + ex);
        }
    }
        
    
    public byte[] getValue()
    {
       return eMsg.byteArrayValue();
    }
       
    public String toString()
    {
        return eMsg.toString();
    }
}

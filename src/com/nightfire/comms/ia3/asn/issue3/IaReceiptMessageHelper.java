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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
 * IaReceiptMessageHelper is a helper class for working with IaReceiptMessage.
 */
public class IaReceiptMessageHelper implements ASNCodable
{
    /** The actual IaReceiptMessage */
    private IaReceiptMessage basicReceipt;
    
    /** The character encoding to use */
    private StringEncoding enc = new StringEncoding("US-ASCII");

    /** The format for the date/time stamp */
    private SimpleDateFormat tsFmt = new SimpleDateFormat("yyyyMMddHHmmss'Z'");

    /**
     * Default constructor
     */
    public IaReceiptMessageHelper()
    {
        this(Module.instance().getOID("interactiveAgent").stringValue());
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public IaReceiptMessageHelper(String oid)
    {
        basicReceipt = new IaReceiptMessage();
    
        // set the time zone for the date/time stamp
        tsFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
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
     * Gets the ISA segment
     */
    public String getISASegment()
    {
        return enc.getString(basicReceipt.getUniqueIdentifier().getIsaSegment().byteArrayValue());
    }

    /**
     * Sets the ISA segment
     */
    public void setISASegment(String edi)
    {
        basicReceipt.getUniqueIdentifier().getIsaSegment()
            .value(enc.getBytes(edi));
    }

    /**
     * Gets the date/time stamp
     *
     * @param MessageException Thrown if the date/time stamp has an invalid
     *                         format
     */
    public Date getDateTimeStamp() throws MessageException
    {
        String ts = 
            basicReceipt.getDateTimeStamp().stringValue();

        try
        {
            return tsFmt.parse(ts);
        }
        catch (Exception ex)
        {
            Debug.log(Debug.MSG_STATUS, "Invalid time stamp: [" + ts + "].");
            Debug.logStackTrace(ex);
            throw new MessageException("Invalid date/time stamp [" + ts
                                       + "]: " + ex);
        }
    }

    /**
     * Gets the date/time stamp in string form (unparsed)
     */
    public String getDateTimeStampString()
    {
        return basicReceipt.getDateTimeStamp()
            .stringValue();
    }

    /**
     * Sets the date/time stamp
     */
    public void setDateTimeStamp(Date dte)
    {
        String ts = tsFmt.format(dte);
        basicReceipt.getDateTimeStamp().value(ts);
    }
    
    
    /**
     * Sets the date/time stamp
     */
    public void setDateTimeStamp(String dte)
    {
        basicReceipt.getDateTimeStamp().value(dte);
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
            basicReceipt.encode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IaReceiptMessage encoding failed: "
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
            basicReceipt.decode(coder);
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IaReceiptMessage decoding failed: "
                                          + ex);
        }
    }
    
    
    public String toString()
    {
        return basicReceipt.toString();
    }
}

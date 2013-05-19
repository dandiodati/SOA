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
public class IaMessageHelper implements ASNCodable
{
    /** The actual IaReceiptMessage */
    private IaMessage iaMessage;
    
    /** The character encoding to use */
    private StringEncoding enc = new StringEncoding("US-ASCII");

    /**
     * Default constructor
     */
    public IaMessageHelper()
    {
        this(Module.instance().getOID("interactiveAgent").stringValue());
    }

    /**
     * Constructor for an OID
     *
     * @param oid  The OID to use
     */
    public IaMessageHelper(String oid)
    {
        iaMessage = new IaMessage(oid);
        
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
    
    public String toString()
    {
        return iaMessage.toString();
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
            iaMessage.encode(coder);
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
            // decode a generic message
                       
            ASNReader coder = Factory.getDecoder(decoding);
            
            Debug.log(this, Debug.MSG_DATA,
                      "Got coder ..." + coder.getClass().getName() );
            
            coder.open(is);
                         
            Debug.log(this, Debug.MSG_DATA,
                      "Opening input stream ...");
            
            Debug.log(this, Debug.MSG_DATA,
                      "Attempting to decode an IA message.");
            
            
            try 
            {
                
                 Debug.log(this, Debug.MSG_DATA,
                      "Trying to decode IaStatusMessage ....");
                IaStatusMessage ias = new IaStatusMessage();
                ias.decode(coder);
                return;
            }
            catch (Exception e)
            {   Debug.log(this, Debug.MSG_DATA,
                      "IaStatusMessage decode failed.");
            }
            
            coder.reset();
            
            try
            {
                Debug.log(this, Debug.MSG_DATA,
                      "Trying to decode a BasicMessage ...");
                BasicMessage ba = new BasicMessage();           
                ba.decode(coder);
                return;
            }
            catch (Exception e)
            {
                Debug.log(this, Debug.MSG_DATA,
                      "BasicMessage decode failed.");
            }
                   
            coder.reset();
            
            Debug.log(this, Debug.MSG_DATA,
                      "Trying to decode an EnhancedMessage ...");
            EnhancedMessage em = new EnhancedMessage();
            em.decode(coder);
            
            // decode a generic message
            Debug.log(this, Debug.MSG_DATA,
                      "Finished decoding an EnhancedMessage.");
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new ProcessingException("IAMessage decoding failed: "
                                          + ex);
        }
    }
        
    public BasicMessage getBasicMessage()
    {
        return iaMessage.getBasicMessage();
    }
        
    public IaStatusMessage getIaStatusMessage()
    {
        return iaMessage.getIaStatusMessage();
    }
    
    public EnhancedMessage getEnhancedMessage()
    {
        return iaMessage.getEnhancedMessage();
    }
    
    
    public void setBasicMessage(BasicMessage msg)
    {
        iaMessage.setBasicMessage( msg );
    }
    
    public void setIaStatusMessage(IaStatusMessage msg )
    {
        iaMessage.setIaStatusMessage( msg );
    }
    
    public void setEnhancedMessage( EnhancedMessage msg )
    {
        iaMessage.setEnhancedMessage( msg );
    }
    
       
    public int getType()
    {
        if ( iaMessage.isBasicMessage() ) return 1;
        if ( iaMessage.isIaStatusMessage() ) return 2;
        if ( iaMessage.isEnhancedMessage() ) return 3;
        return -1;
    }
        
}

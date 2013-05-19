/*
 * Copyright(c) 22000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue3.EnhancedMessageHelper;
import com.nightfire.comms.ia.asn.ASNData;

import com.nightfire.framework.message.MessageException;
import com.nightfire.common.ProcessingException;

public class EnhancedMessage extends EnhancedMessageHelper implements ASNData
{
    /**
     * Default constructor
     */
    
    
    public EnhancedMessage()
    {
        super(  );
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
    public EnhancedMessage( byte[] iaReceiptMessage )
    {       
    	super( iaReceiptMessage );   
    }
    
   
}

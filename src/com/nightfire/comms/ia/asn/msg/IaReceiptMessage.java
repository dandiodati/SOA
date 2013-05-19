/*
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue3.IaReceiptMessageHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IaReceiptMessage extends IaReceiptMessageHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IaReceiptMessage()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IaReceiptMessage(String oid)
    {
        super(oid);
    }
}

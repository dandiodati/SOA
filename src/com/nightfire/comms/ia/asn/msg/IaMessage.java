/*
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue3.IaMessageHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IaMessage extends IaMessageHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IaMessage()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IaMessage(String oid)
    {
        super(oid);
    }
}

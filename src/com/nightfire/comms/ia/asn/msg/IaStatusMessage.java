/*
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue3.IaStatusMessageHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IaStatusMessage extends IaStatusMessageHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IaStatusMessage()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IaStatusMessage(String oid)
    {
        super(oid);
    }
}

/*
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue3.BasicMessageHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class BasicMessage extends BasicMessageHelper implements ASNData
{
    /**
     * Default constructor
     */
    public BasicMessage()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public BasicMessage(String oid)
    {
        super(oid);
    }
}

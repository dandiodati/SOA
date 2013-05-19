/*
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue2.IAStatusHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IAStatus extends IAStatusHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IAStatus()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IAStatus(String oid)
    {
        super(oid);
    }
}

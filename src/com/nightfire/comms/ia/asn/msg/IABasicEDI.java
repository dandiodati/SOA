/*
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue2.IABasicEDIHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IABasicEDI extends IABasicEDIHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IABasicEDI()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IABasicEDI(String oid)
    {
        super(oid);
    }
}

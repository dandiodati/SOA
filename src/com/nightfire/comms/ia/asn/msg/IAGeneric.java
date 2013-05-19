/*
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue2.IAGenericHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IAGeneric extends IAGenericHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IAGeneric()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IAGeneric(String oid)
    {
        super(oid);
    }
}

/*
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia.asn.msg;

import com.nightfire.comms.ia3.asn.issue2.IABasicReceiptHelper;
import com.nightfire.comms.ia.asn.ASNData;

public class IABasicReceipt extends IABasicReceiptHelper implements ASNData
{
    /**
     * Default constructor
     */
    public IABasicReceipt()
    {
        super();
    }

    /**
     * Constructor for an OID
     */
    public IABasicReceipt(String oid)
    {
        super(oid);
    }
}

/**
 * Copyright(c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia.asn;

// jdk imports

// third party imports

// NightFire imports
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.ObjectFactory;

/**
 * ASNDefinition contains an object identifier (OID) for the message type it
 * defines.  It must be able to construct a concrete class for encoding and
 * decoding messages which match its OID.
 */
public class ASNDefinition
{
    /**
     * The OID for this message type.
     */
    private String oid;

    /**
     * The name of the class implementing this message type.
     */
    private String impl;

    /**
     * The constructor for an ASNDefinition.  It accepts an object identifier
     * and the fully qualified name of a class which implements that structure.
     *
     * @param oid   The plain text oid of the message
     * @param impl  The name of the class implementing it
     */
    public ASNDefinition(String oid, String impl)
    {
        // initialize members
        this.oid = oid;
        this.impl = impl;

        // debug information
        Debug.log(this, Debug.SYSTEM_CONFIG, "New ASNDefinition:\n" +
                                             "  OID   [" + oid + "]\n" +
                                             "  Class [" + impl + "]");
    }

    /**
     * Returns the OID for this definition.
     *
     * @return The OID for this definition
     */
    public String getOID()
    {
        return oid;
    }

    /**
     * Creates and returns a new instance of the class which implements
     * this defintion.
     *
     * @return A new instance of this message type
     */
    public ASNData newInstance() throws ProcessingException
    {
        Debug.log(this, Debug.OBJECT_LIFECYCLE, "Creating a new instance of " +
                  impl + ".");

        try
        {
            // use ObjecFatory to create the class
            ASNData obj = (ASNData)ObjectFactory.create(impl);

            return obj;
        }
        // could throw a FrameworkException or a casting exception
        catch(Exception e)
        {
            throw new ProcessingException(e.toString());
        }
    }
}

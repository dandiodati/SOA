/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.framework.util;

import  java.io.Serializable;


/**
 * <p><strong>MessageData</strong> is a general data container class which
 * composes of a header and a body.  It is typically used to encapsulate
 * a data's header XML and body XML.</p>
 */
 
public class MessageData implements Serializable
{
    public String header;
    public String body;
    
    public MessageData(String header, String body)
    {
        this.header = header;
        this.body   = body;
    }
}

/**
 * Copyright (c) 2003 NeuStar, Inc.  All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/util/RemoteContext.java#1 $
 */
package com.nightfire.framework.util;


/**
 * Class that represents the context containing information that can be passed
 * from the client to the remote server.
 */
public class RemoteContext implements java.io.Serializable
{
    /**
     * Constructs a remote context containg the given user and customer id.
     *
     * @param  user  Current user.
     * @param  cid   Current customer id.
     *
     */
    public RemoteContext(String user, String cid)
    {
        this.user = user;
        this.cid= cid;
    }

    /** 
     * Get the user id from the context.
     *
     * @return  The user identifier.
     *
     */
    public String getUser ( )
    {
        return user;
    }

    /** 
     * Get the customer id from the context.
     *
     * @return  The customer identifier.
     *
     */
    public String getCustomerID ( )
    {
        return cid;
    }

    private String user;
    private String cid;

}



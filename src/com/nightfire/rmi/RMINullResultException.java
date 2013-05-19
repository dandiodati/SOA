/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.rmi;


import java.rmi.*;


public class RMINullResultException extends java.rmi.RemoteException
{
    public RMINullResultException ( String errorMessage )
    {
        super( errorMessage );
    }
};

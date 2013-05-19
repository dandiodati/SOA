/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.rmi;


import java.rmi.*;


public class RMIServerException extends java.rmi.RemoteException
{
    // errorType values.
    public static final int UnknownError        = 0;
    public static final int DatabaseError       = 1;
    public static final int CommunicationsError = 2;
    public static final int AccessDeniedError   = 3;


    public RMIServerException ( String errorMessage )
    {
        super( errorMessage );
    }


    public RMIServerException ( int errorType, String errorMessage )
    {
        this( errorMessage );

        this.errorType = errorType;
    }


    public int errorType;
};

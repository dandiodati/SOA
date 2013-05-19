/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.rmi;


import java.rmi.*;


public class RMIInvalidDataException extends java.rmi.RemoteException
{
    // errorType values.
    public static final int UnknownDataError   = 0;
    public static final int MalformedDataError = 1;
    public static final int MissingDataError   = 2;
    public static final int InvalidDataError   = 3;


    public RMIInvalidDataException ( String errorMessage )
    {
        super( errorMessage );
    }


    public RMIInvalidDataException ( int errorType, String errorMessage )
    {
        this( errorMessage );

        this.errorType = errorType;
    }


    public String header;

    public int errorType;
};

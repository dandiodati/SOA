/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.util;


import java.rmi.*;
import java.io.*;


/**
 * Base class for exceptions used by rest of library.
 */
public class FrameworkException extends Exception
{
    /**
     * Create a Framework exception object with the given message.
     *
     * @param  msg  Message associated with exception.
     */
    public FrameworkException ( String msg )
    {
        super( msg );
        /*
        Debug.warning( "Exception object being created in FrameworkException (String): " +  msg );

        if ( stackTraceEnabled )
        {
            StringWriter sw = new StringWriter( );

            printStackTrace( new PrintWriter(sw) );

            Debug.warning( "Trace for exception object being created in FrameworkException (String): " + sw.toString() );
        }
        */
    }


    /**
     * Create a Framework exception object with the given message.
     *
     * @param  statusCode  User-defined numeric error code.
     * @param  msg  Message associated with exception.
     */
    public FrameworkException ( int statusCode, String msg )
    {
        this( msg );

        this.statusCode = statusCode;
    }


    /**
     * Create a Framework exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public FrameworkException ( Exception e )
    {
        super( getExceptionMessage( e ) );

        /*
        Debug.warning( "Exception object being created in FrameworkException (String): " +  e.toString() );

        if ( stackTraceEnabled )
        {
            StringWriter sw = new StringWriter( );

            e.printStackTrace( new PrintWriter(sw) );

            Debug.warning( "Trace for exception object being created in FrameworkException (String): " +  sw.toString() );
        }
        */
    }


    /**
     * Create a Framework exception object with the given exception's message.
     *
     * @param  statusCode  User-defined numeric error code.
     * @param  e  Exception object used in creation.
     */
    public FrameworkException ( int statusCode, Exception e )
    {
        this( e );

        this.statusCode = statusCode;
    }


    /**
     * Get the user-defined numeric error code associated with
     * this exception.
     *
     * @return  Numeric error code.
     */
    public int getStatusCode ( )
    {
        return statusCode;
    }


    /**
     * Get the exception argument's undecorated text message, if possible.
     * If the exception is a remote exception with a nested server-side
     * detail exception, that wrapped exception will be used.
     *
     * @return  The exception argument's text message, or null if unavailable.
     */
    public static String getExceptionMessage ( Exception e )
    {
        if ( e == null )
            return null;

        // Corba Exception don't give much detail in their getMessage() method
        else if (e instanceof com.nightfire.idl.CorbaServerException)
            return ((com.nightfire.idl.CorbaServerException)e).toString();

        else if (e instanceof com.nightfire.idl.InvalidDataException)
            return ( (com.nightfire.idl.InvalidDataException)e ).toString();

        else
        {
            Throwable t = e;

            if ( (t instanceof java.rmi.RemoteException) && (((java.rmi.RemoteException)t).detail != null) )
                t = ((java.rmi.RemoteException)t).detail;

            if( t.getMessage() != null )
                return( t.getMessage() );
            else
                return( t.toString() );
        }
    }


    /**
     * Enable showing of stack trace at point of exception object creation.
     *
     * @deprecated
     */
    public static void showStackTrace ( )
    {
        stackTraceEnabled = true;
    }


    /**
     * Disable showing of stack trace at point of exception object creation.
     * 
     * @deprecated
     */
    public static void hideStackTrace ( )
    {
        stackTraceEnabled = false;
    }


    private static boolean stackTraceEnabled = false;

    protected int statusCode;
}

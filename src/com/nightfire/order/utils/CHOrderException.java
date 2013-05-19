package com.nightfire.order.utils;

import com.nightfire.framework.util.*;

/**
 * Class for exceptions used by Order processing package.
 *
 * @author Abhishek Jain
 */
public class CHOrderException extends FrameworkException
{
    /**
     * Create a CHOrderException object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public CHOrderException ( String msg )
    {
        super( msg );
    }


    /**
     * Create a CHOrderException object with the given message.
     *
     * @param  statusCode  User-defined numeric error code.
     * @param  msg  Error message associated with exception.
     */
    public CHOrderException ( int statusCode, String msg )
    {
        super( statusCode, msg );
    }


    /**
     * Create a CHOrderException object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public CHOrderException ( Exception e )
    {
        super( e );
    }


    public CHOrderException ( String msg,Exception cause )
    {
        super(msg);
        initCause(cause);
    }

    /**
     * Create a CHOrderException object with the given exception's message.
     *
     * @param  statusCode  User-defined numeric error code.
     * @param  e  Exception object used in creation.
     */
    public CHOrderException ( int statusCode, Exception e )
    {
        super( statusCode, e );
    }
}

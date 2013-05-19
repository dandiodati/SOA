/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.common;


import com.nightfire.framework.util.*;


/**
 * Base class for exceptions used by message processing package.
 */
public class ProcessingException extends FrameworkException
{
    /**
     * Create a message exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public ProcessingException ( String msg )
    {
        super( msg );
    }


    /**
     * Create a message exception object with the given message.
     *
     * @param  statusCode  User-defined numeric error code.
     * @param  msg  Error message associated with exception.
     */
    public ProcessingException ( int statusCode, String msg )
    {
        super( statusCode, msg );
    }


    /**
     * Create a message exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public ProcessingException ( Exception e )
    {
        super( e );
    }


    /**
     * Create a message exception object with the given exception's message.
     *
     * @param  statusCode  User-defined numeric error code.
     * @param  e  Exception object used in creation.
     */
    public ProcessingException ( int statusCode, Exception e )
    {
        super( statusCode, e );
    }
}

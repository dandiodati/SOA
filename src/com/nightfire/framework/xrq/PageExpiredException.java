package com.nightfire.framework.xrq;

import com.nightfire.framework.message.*;


/**
 * Thrown when a pages has expired an no longer exists.
 */
public class PageExpiredException extends MessageException
{

  /**
     * Create a resource exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public PageExpiredException ( String msg )
    {
        super( msg );
    }


    /**
     * Create a resource exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public PageExpiredException ( Exception e )
    {
        super( e );
    }
}

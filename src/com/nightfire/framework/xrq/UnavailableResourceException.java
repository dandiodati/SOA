package com.nightfire.framework.xrq;

import com.nightfire.framework.resource.ResourceException;

/**
 * An exception which indicates that resources are busy and that
 * any requests that produced it should be tried again at a later point in time.
 */
public class UnavailableResourceException extends ResourceException
{

  /**
     * Create a resource exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public UnavailableResourceException ( String msg )
    {
        super( msg );
    }


    /**
     * Create a resource exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public UnavailableResourceException ( Exception e )
    {
        super( e );
    }
} 
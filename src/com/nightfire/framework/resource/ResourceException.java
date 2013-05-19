/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/resource/ResourceException.java#1 $
 *
 */

package com.nightfire.framework.resource;

import com.nightfire.framework.util.*;


/**
 * Base class for exceptions thrown by resource package.
 */
public class ResourceException extends FrameworkException
{
    /**
     * Create a resource exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public ResourceException ( String msg )
    {
        super( msg );
    }


    /**
     * Create a resource exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public ResourceException ( Exception e )
    {
        super( e );
    }
}

/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.adapter.address.msag ;

import com.nightfire.framework.util.* ;

/**
 * Provides exceptions specific to the use of the MercatorAdapter
 * class.
 */
public class AVQEngineException extends FrameworkException
{
    /**
     * Creates an instance of AVQEngineException with the given message.
     *
     * @param msg   The message associated with the exception
     */
    public AVQEngineException (String msg) {

        super(msg);
    }
   
    /**
     * Creates an instance of AVQEngineException
     * with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public AVQEngineException ( Exception e )
    {
        this( e.getMessage() );
    }
}




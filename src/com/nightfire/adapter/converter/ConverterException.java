/**
* Copyright (c) 2001 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import com.nightfire.framework.message.MessageException;

/**
* ConverterExceptions is a MessageException that gets thrown when a
* Converter can not parse a given input.
*/
public class ConverterException extends MessageException{

    /**
     * Create an exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public ConverterException ( String msg )
    {
        super( msg );
    }

    /**
     * Create an exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public ConverterException ( Exception e )
    {
        super( e );
    }

} 
/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import com.nightfire.spi.common.driver.MessageObject;

/**
* This is the interface implemented by a Converter classes. Converters
* take an input in one message format and convert the input to an equivalent
* representation in a new format. 
*/
public interface Converter{

    /**
    * This takes in a message, converts it, and returns the result.
    *
    * @param input A MessageObject containing the input message.
    * @return A MessageObject containing the converted message. The content
    *         of the message object depends on the individual Converter
    *         implementation. 
    */
    public MessageObject convert(MessageObject input) throws ConverterException;

} 
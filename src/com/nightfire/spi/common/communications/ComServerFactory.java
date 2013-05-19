/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.spi.common.communications;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;


/**
 * Factory for creating communication server objects.
 */
public class ComServerFactory
{
    /**
     * Factory used to create server communications objects.
     *
     * @param  classname  Name of communications server type as a fully-qualified (with packages) class name.
     *
     * @return  Newly-created communications server object.
     *
     * @exception  ProcessingException  Thrown if named server type can't be created.
     */
    public static ComServerBase createServer (String className, String key, String type) 
        throws ProcessingException
    {
        String[] args = {key, type};
        return (createServer(className, args));
    }


    /**
     * Factory used to create server communications objects.
     *
     * @param  classname        Name of communications server type as a fully-qualified (with packages) class name.
     * @param  constructorArgs  Array of objects passed as arguments to constructor.
     *
     * @return  Newly-created communications server object.
     *
     * @exception  ProcessingException  Thrown if named server type can't be created.
     */
    public static ComServerBase createServer (String className, Object constructorArgs[])
        throws ProcessingException
    {
        try
        {
            return ((ComServerBase) ObjectFactory.create(className, constructorArgs, ComServerBase.class));
        }
        catch (FrameworkException fe)
        {
            throw new ProcessingException (fe.getMessage());
        }
    }
}

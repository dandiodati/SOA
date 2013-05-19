/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;



/**
 * Interface supporting string to arbitrary-object transformation.
 */
public interface StringTransformer
{
    /**
     * Convert the given string to some other object type/value.
     *
     * @param  value  String value to transform.
     *
     * @return  The transformed object.
     *
     * @exception  FrameworkException  Thrown if object can't be transformed.
     */
    public Object transform ( String value ) throws FrameworkException;
}

/*
 * Copyright (c) 1998 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


/**
 * Interface providing data used in template token replacement.
 */
public interface TokenDataSource
{
    /**
     * Test to see if the named item exists.
     *
     * @param  name  Name of item.
     *
     * @return  'true' if named item exists, otherwise 'false'.
     */
    public boolean exists ( String name );


    /**
     * Get the value associated with the named item.  
     *
     * @param  name  Name of item whose value should be retrieved.
     *
     * @return  String containing named item's value.
     *
     * @exception  FrameworkException  Thrown if named item's value can't be retrieved.
     */
    public String getValue ( String name ) throws FrameworkException;
}

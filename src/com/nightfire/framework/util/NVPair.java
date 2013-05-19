/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;


/**
 * Class providing a name-value pair grouping.
 *
 */
public class NVPair
{
    /**
     * String name.
     */
    public String name;

    /**
     * Object value.
     */
    public Object value;


    /**
     * Constructor
     *
     * @param  name  Read-only string name.
     * @param  value  Read-only object value.
     */
    public NVPair ( String name, Object value )
    {
        this.name  = name;
        this.value = value;
    }

    /**
     * Setter for name attribute.  This allows for NVPair to conform to the
     * Java Bean standard, which is useful in JSP expression language, etc.
     *
     * @param  name  Name portion of NVPair.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Getter for name attribute.  This allows for NVPair to conform to the
     * Java Bean standard, which is useful in JSP expression language, etc.
     *
     * @return  Name portion of NVPair.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Setter for value attribute.  This allows for NVPair to conform to the
     * Java Bean standard, which is useful in JSP expression language, etc.
     *
     * @param  name  Value portion of NVPair.
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * Getter for value attribute.  This allows for NVPair to conform to the
     * Java Bean standard, which is useful in JSP expression language, etc.
     *
     * @return  Value portion of NVPair.
     */
    public Object getValue()
    {
        return value;
    }
}

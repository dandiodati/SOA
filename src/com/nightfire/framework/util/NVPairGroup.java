/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;


/**
 * Class providing a group of name-value pair.
 *
 */
public class NVPairGroup extends NVPair
{


    /**
     * Unique Id
     */
    public String id;
    

    public NVPairGroup(String name, List nvPairs)
    {
        super(name, nvPairs);
    }
    

    /**
     * Constructor
     *
     * @param id a <code>String</code> value
     * @param  name  Read-only string name.
     * @param nvPairs A list of NVPairs
     */
    public NVPairGroup ( String id, String name, List nvPairs )
    {

        super(name, nvPairs);
        this.id = id;
        
    }

    /**
     * Setter for id attribute.  This allows for NVPair to conform to the
     * Java Bean standard, which is useful in JSP expression language, etc.
     *
     * @param  id  Sets the unique id of this group
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Getter for id attribute.  This allows for NVPair to conform to the
     * Java Bean standard, which is useful in JSP expression language, etc.
     *
     * @return  Name portion of NVPair.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Setter for value/list attribute.  This is the same as calling
     * setValue(value).
     *
     * @param  name  Value portion of NVPair.
     */
    public void setPairs(List value)
    {
        this.value = value;
    }

    /**
     * Getter for value/list attribute.  This is the same as calling
     * (List)getValue().
     *
     * @return  Value portion of NVPair.
     */
    public List getPairs()
    {
        return (List)value;
    }
}

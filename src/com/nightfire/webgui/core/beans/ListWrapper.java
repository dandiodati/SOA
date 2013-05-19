/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.beans;

import java.util.List;


/**
 * A bean which wrapps a list and provides access to the size of a list.
 *
 *
 */
 
public class ListWrapper
{

    private List list;
    

    public ListWrapper(List list)
    {
        this.list = list;
        
    }
    
    
    public List getList()
    {
        return list;
        
    }
    
    public int getSize()
    {
        return list.size();
    }

}

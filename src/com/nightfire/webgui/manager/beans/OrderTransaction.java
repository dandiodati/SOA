/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/manager/beans/ServiceComponentBean.java#23 $
 */

package com.nightfire.webgui.manager.beans;


import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;

import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.manager.*;

import javax.servlet.*;
import javax.servlet.http.*;


import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.transformer.*;

import  com.nightfire.framework.constants.PlatformConstants;
import  com.nightfire.framework.message.*;



import java.util.*;

/**
 * An object that represents a single order history transaction
 *
 */

public class OrderTransaction
{

    private HashMap fields;

    private List actions;


    private String id;


    public OrderTransaction()
    {
        fields = new HashMap();
        actions = new ArrayList();
    }



    public OrderTransaction(Node row) throws MessageException
    {
        XMLPlainGenerator gen = new XMLPlainGenerator(row.getOwnerDocument());
        Node children[] = gen.getChildren(row);

        for(int i = 0; i< children.length; i++ ) {
            addField(children[i].getNodeName(), gen.getValue(children[i]));
        }
    }


    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }


    public void addField(String name, String value)
    {
        fields.put(name,value);
    }

    public Map getFields()
    {
        return fields;
    }

    public List getActions()
    {
        return actions;
    }

    public void setActions(List actions)
    {
        this.actions = actions;
    }




}

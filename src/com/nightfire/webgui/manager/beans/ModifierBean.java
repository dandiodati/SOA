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
 * An XMLBean which represents a modifier to a service component bean.
 *
 */
 
public class ModifierBean extends InfoBodyBase implements ManagerServletConstants, PlatformConstants
{

    ServiceComponentBean parent = null;

    public static final String ID ="LatestModifier";
    

    public ModifierBean()  throws ServletException
    {
        super();
    }

    /**
     * Constructor.
     *
     * @param  headerData  Map of header fields.
     * @param  bodyData    Map of body fields.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public ModifierBean(Map headerData, Map bodyData) throws ServletException
    {
       super(headerData, bodyData);
    }

    /**
     * Constructor.
     *
     * @param  headerData  XML header document.
     * @param  bodyData    XML body document.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public ModifierBean(String headerData, String bodyData) throws ServletException
    {
        super(headerData, bodyData);

    }
    
    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public  ModifierBean(XMLGenerator header, XMLGenerator body) throws ServletException
    {
       super(header, body);
    }

    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public  ModifierBean(Document header, Document body) throws ServletException
    {
       super(header, body);
    }

    public void setParent(ServiceComponentBean parent)
    {
       this.parent = parent;
    }

    /**
     * Returns parent ServiceComponentBean
     *
     * @return a <code>ServiceComponentBean</code> value
     */
    public ServiceComponentBean getParent()
    {
       return parent;
    }


    /**
     * Returns grandparent BundleBeanBag
     *
     * @return a <code>BundleBeanBag</code> value
     */
    public BundleBeanBag getParentBag()
    {
        return parent.getParentBag();
    }

    public void setId(String id) 
    {
        // modifier id is appended with ID constant
        if (! id.endsWith(ID))
            super.setId(id +"." + ID);
        else
            super.setId(id);
    }



    





}

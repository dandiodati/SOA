/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.util;

import  java.util.HashMap;

import  javax.servlet.jsp.*;

import  com.nightfire.framework.util.Debug;

import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.manager.svcmeta.*;

import  com.nightfire.webgui.manager.ManagerServletConstants;


/**
 * LoadComponentDefTag is responsible for obtaining a ComponentDef object from
 * a BundleDef object based on service id.
 *
 */

public class LoadComponentDefTag extends VariableTagBase implements ManagerServletConstants
{
    private String serviceId;
    private BundleDef bundleObj;


    /**
     * Set the bundle def object
     *
     */
    public void setBundleDef(Object bundle) throws JspException
    {
        setDynAttribute("bundleDef", bundle, BundleDef.class);
    }

    /**
     * Setter method for the service component id.
     *
     * @param svcId The id of the service component to obtain.
     */
    public void setServiceId(String svcId) throws JspException
    {
        setDynAttribute("serviceId", svcId, String.class);
    }

    /**
     * Redefinition of doStartTag() in VariableTagBase.  This method processes the
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
      	super.doStartTag();

        serviceId = (String)getDynAttribute("serviceId");
        bundleObj = (BundleDef) getDynAttribute("bundleDef");

        ComponentDef comp = bundleObj.getComponent(serviceId);

        setVarAttribute(comp);

        return SKIP_BODY;
    }

    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();

        bundleObj = null;
        serviceId = null;
    }
}

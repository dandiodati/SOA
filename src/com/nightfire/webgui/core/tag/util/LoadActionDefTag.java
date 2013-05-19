/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  javax.servlet.*;
import  javax.servlet.jsp.*;

import com.nightfire.webgui.manager.svcmeta.*;
import com.nightfire.webgui.gateway.svcmeta.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.svcmeta.ActionDef;




/**
 * This tag loads a ActionDef object identified by
 * an id.
 */

public class LoadActionDefTag extends VariableTagBase
{
    


    protected Object service = null;
        
    private String actionId;

     /**
     * The service to use
     */
    public void setService(Object service) throws JspException
    {
        setDynAttribute("service", service, Object.class);
        
    }
     /**
     * The bundle xml to use
     */
    public void setActionId(String id ) throws JspException
    {
       setDynAttribute("actionId", id, String.class);
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

        service = getDynAttribute("service");
        actionId = (String)getDynAttribute("actionId");
        

        if (!StringUtils.hasValue(actionId)) {
            log.error("Null action id passed in");
            throw new JspTagException("Null action id passed in");
        }

        if ( service == null) {
            log.error("Null service object passed in.");
            throw new JspTagException("Null service object passed in.");
        }
        
        ActionDef action = null;
        
        if (service instanceof ComponentDef) {
            ComponentDef compDef = (ComponentDef)service;
            action = compDef.getActionDef(actionId);
            if ( action == null)
                action = compDef.getModifierInfo().getActionDef(actionId);
            if (action == null)
                action = compDef.getHistoryInfo().getActionDef(actionId);
        }
        else if ( service instanceof ServiceDef)
            action = ((ServiceDef)service).getActionDef(actionId);
        else
            throw new JspTagException("Unknown service object passed in. Must be of type ServiceDef or ComponentDef");
        
        
        if (log.isDebugEnabled() && action != null)
                log.debug("Got actionDef [" + action.getActionName() +"]");
        else
            log.warn("Could not find action def [" + action.getActionName() +"]");
        
        
        setVarAttribute(action);

        return SKIP_BODY;
        
    }
        
      
}

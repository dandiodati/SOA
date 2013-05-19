/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.gateway.tag.util;

import  javax.servlet.*;
import  javax.servlet.jsp.*;

import com.nightfire.webgui.gateway.*;
import com.nightfire.webgui.gateway.svcmeta.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.framework.util.StringUtils;

/**
 * LoadSvcGroupDefTag loads a ServiceGroupDef object identified by
 * an id.
 */

public class LoadSvcGroupDefTag extends VariableTagBase
{
    
     /**
     * The bundle xml to use
     */
    public void setSvcGroupId(String id ) throws JspException
    {
       this.id = (String)TagUtils.getDynamicValue("svcGroupId", id, String.class, this, pageContext);
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

        if (StringUtils.hasValue(id)) {
         
            ServiceGroupDefContainer con = (ServiceGroupDefContainer)pageContext.getServletContext().getAttribute(GatewayConstants.SERVICE_GROUP_DEF_CONTAINER);
            if ( con == null) {
                log.error("Could not locate service group def container");
                throw new JspTagException("Could not locate service group def container");
            }

            setVarAttribute(con.getServiceGroup(id));
        }
        else {
            log.error("Service Group Id is required to have a value.");
            throw new JspTagException("Service group id is null.");
        }
        
        return SKIP_BODY;
        
    }
        
      
}

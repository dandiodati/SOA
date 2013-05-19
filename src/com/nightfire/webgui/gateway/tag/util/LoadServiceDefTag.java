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
 * LoadServiceDefResourceTag loads a ServiceDef object identified by
 * an id.
 */

public class LoadServiceDefTag extends VariableTagBase
{
    
     /**
     * The bundle xml to use
     */
    public void setServiceId(String id ) throws JspException
    {
       this.id = (String)TagUtils.getDynamicValue("serviceId", id, String.class, this, pageContext);
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

        if (!StringUtils.hasValue(id)) {
            log.error("Null service id passed in");
            throw new JspTagException("Null service id passed in");
        }

        ServiceDefContainer con = (ServiceDefContainer)pageContext.getServletContext().getAttribute(GatewayConstants.SERVICE_DEF_CONTAINER);

        if ( con == null) {
            log.error("Could not locate service def container");
            throw new JspTagException("Could not locate service def container");
        }
        

        ServiceDef service = con.getService(id);

        log.debug("Got service def [" + service.getID() +"]");
        
        
        setVarAttribute(service);

        return SKIP_BODY;
        
    }
        
      
}

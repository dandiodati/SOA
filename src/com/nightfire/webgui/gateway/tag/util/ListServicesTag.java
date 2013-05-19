/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.gateway.tag.util;

import java.util.*;
import java.net.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;

import com.nightfire.webgui.gateway.svcmeta.*;
import com.nightfire.webgui.gateway.*;

/**
 * This tag creates a ListWrapper of all the services available for a given service group.
 * This list will be accessible via the value specified by "var" in the jsp tag.
 */
public class ListServicesTag extends VariableTagBase
{

    private ServiceGroupDef serviceGroup = null;

    /**
     * Set the service group object to get services from.
     *
     * @param obj The service group object
     */
    public void setServiceGroup ( Object obj ) throws JspException
    {
       this.serviceGroup = (ServiceGroupDef)TagUtils.getDynamicValue("serviceGroup", obj,
                           ServiceGroupDef.class, this, pageContext);
    }

    /**
     * Starts procesing of this tag.
     * This method obtains the list of services available for the serviceGroup object passed in the tag,
     * and returns that information in a List form placed at the location defined by
     * the variable var and the scope specified.
     *
     * Each item in the list is a NVPair type where:
     *     name = display name of order type (example "Loop with NP")
     *     value = key of the order type (example "LSNP")
     *
     * @throws JspException on processing error.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        try
        {
            //container for display_name-key information about the services.
            ArrayList serviceList = new ArrayList();

            String name = null;
            String value = null;

            //Get all the services.
            List services = serviceGroup.getServices();
            for ( Iterator iter = services.iterator(); iter.hasNext(); )
            {
                //Get information for each service.
                ServiceDef service = (ServiceDef) iter.next();
                name = service.getDisplayName();
                value = service.getID();

                if (service.getUserCreatable())
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Adding service [" + name + "]:[" + value +"] to list.");
                    }

                    serviceList.add( new NVPair( name, value ) );
                }
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "Returning [" + serviceList.size() + "] services." );
            }

            //Now set the services on the location specified by var and scope variables.
            setVarAttribute( new ListWrapper(serviceList) );
        }
        catch (Exception e )
        {
            String err = "Failed to get service types: " + e.getMessage();
            log.error(err, e);
            throw new JspTagException(err);
        }

        return SKIP_BODY;
    }

    public void release()
    {
       super.release();
       serviceGroup = null;
    }

}

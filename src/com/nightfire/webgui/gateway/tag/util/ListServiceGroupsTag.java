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
 * This tag creates a ListWrapper of all the available services from the ServiceGroupDefContainer
 * object contained in the servlet context.
 * This list will be accessible via the value specified by "var" in the jsp tag.
 */
public class ListServiceGroupsTag extends VariableTagBase
{

    /**
     * Starts procesing of this tag.
     * This method uses the serviceGroupsContainer object to obtain the list of available
     * order types and returns that information in a List form placed at the location defined by
     * the variable var and the scope specified.
     *
     * Each item in the list is a NVPair type where:
     *     name = display name of order type (example "LSR Order")
     *     value = key of the order type (example "LSROrder")
     *
     * @throws JspException on processing error.
     */

    private boolean templateSupported = false;

    /**
     * Sets templateSupported that the results are currently sorted in.
     * @ param boolean Possible values are true and false.
     *
     */
    public void setTemplateSupported(String bool)  throws JspException
    {
       try {
          if ( StringUtils.hasValue(bool) )
             this.templateSupported = StringUtils.getBoolean(bool);
       } catch (FrameworkException e) {
          log.warn("Not a valid value for templateSupported attribute [" + bool + "],\n " + e.getMessage() + " defaulting to false.");
       }
    }

    public int doStartTag() throws JspException
    {
        super.doStartTag();

        //Get a handle on the service groups container first.
        ServiceGroupDefContainer serviceGroupsContainer = (ServiceGroupDefContainer)pageContext.getServletContext().getAttribute(GatewayConstants.SERVICE_GROUP_DEF_CONTAINER);
        if ( serviceGroupsContainer == null) {
            log.error("Could not locate service group def container");
            throw new JspTagException("Could not locate service group def container");
        }

        try
        {
            //container for display_name-key information about the service groups.
            ArrayList serviceGroupList = new ArrayList();

            String name = null;
            String value = null;
            //modifyPermission represents the Modify-Permission associated with this Service
            String modifyPermission = null;
            //Get all the service groups.
            List serviceGroups = serviceGroupsContainer.getServiceGroups();


            for ( Iterator iter = serviceGroups.iterator(); iter.hasNext(); )
            {
                //Get information for each service group.
                ServiceGroupDef serviceGroup = (ServiceGroupDef) iter.next();
                name = serviceGroup.getDisplayName();
                value = serviceGroup.getID();
				modifyPermission = serviceGroup.getModifyPermission();
		        				 
			   //if ModifyPermission Node is absent, create the permission using Service Group ID.
                if (modifyPermission==null)	{
                    modifyPermission=GatewayConstants.BASIC_GUI_PREFIX + value;
                }

                if( !ServletUtils.isAuthorized(pageContext.getSession(), modifyPermission ) )
                    continue;

                if ( (!templateSupported) || serviceGroup.getTemplateSupport() ) // This would check if to display only the Template Support Groups
                {	
					if (serviceGroup.getUserCreatable())
					{
						if ( log.isDebugEnabled() )
						{
							log.debug( "Adding service group[" + name + "]:[" + value +"] to list.");
						}
						serviceGroupList.add( new NVPair( name, value ) );
					}
                }
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "Returning [" + serviceGroupList.size() + "] service groups." );
            }

            //Now set the order types on the location specified by var and scope variables.
            setVarAttribute( new ListWrapper(serviceGroupList) );
        }
        catch (Exception e )
        {
            String err = "Failed to get order types: " + e.getMessage();
            log.error(err, e);
            throw new JspTagException(err);
        }

        return SKIP_BODY;
    }

    public void release()
    {
       super.release();
    }
}

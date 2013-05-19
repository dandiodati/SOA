/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.tag.message;

import java.util.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.tag.message.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.manager.svcmeta.ComponentDef;
import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.webgui.core.svcmeta.MessageTypeDef;
import com.nightfire.webgui.manager.beans.ServiceComponentBean;
import com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.webgui.manager.beans.ModifierBean;
import com.nightfire.webgui.manager.beans.InfoBodyBase;
import com.nightfire.webgui.core.ServletConstants;




/**
 * A tag that acts as a factory for creating a service component based order.
 * These beans will always have supplier,version and a service type associated with them.
 *
 */
public class CreateServiceComponentBeanTag extends ServiceBeanTagBase
{




    public String getServiceType()
    {
        return ((ComponentDef)service).getID();
    }


    public MessageTypeDef getMessageType(String id) throws JspTagException
    {
        MessageTypeDef msgType = null;
        
        Iterator iter = ((ComponentDef)service).getMessageTypeDefs().iterator();
        while (iter.hasNext()) {
            msgType = (MessageTypeDef) iter.next();
            if (msgType.getName().equals(id))
                break;
                
        }

        if (msgType == null)
            throw new JspTagException("Invalid Message type[" + id +"] passed in.");
        
        return msgType;
        
    }
    

    public MessageTypeDef getMessageType(XMLBean bean)
    {

        
        try {
                    
            XMLGenerator body = (XMLGenerator) bean.getBodyDataSource();
            MessageTypeDef  mType = ((ComponentDef)service).getMessageType(body.getOutputDOM());
       
            if (mType == null)
                mType = ((ComponentDef)service).getDefaultMessageType();

            return mType;

        }
        catch (Exception e) {
            log.error("Failed to determine message type : " + e.getMessage());
            return null;
        }

    }



    public NFBean createBean() throws JspException
    {

        if (! (service instanceof ComponentDef) ) {
            throw new JspTagException("Invalid service object["+ service.getClass()+"] passed in. Must be of type ComponentDef");
        }

        return super.createBean();


    }


    protected XMLBean createNewBean(XMLGenerator data) throws JspException
    {
        try {
            
            ServiceComponentBean bean = new ServiceComponentBean();
            
            if (data != null) {
                Node svNode = data.getNode("0");
                bean.decompose(data, svNode);
            }
        
                
            return bean;
        } catch (Exception e) {
            throw new JspException("Failed to create bean", e);
        }
    }


    protected void setHeaderValues(XMLBean bean) throws Exception {
        super.setHeaderValues(bean);        
        bean.setHeaderValue(ManagerServletConstants.COMPONENT_DISPLAY_NAME, ((ComponentDef)service).getFullName());
 
    }


}

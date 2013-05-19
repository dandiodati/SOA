/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.gateway.tag.message;


import java.util.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.tag.message.*;
import com.nightfire.webgui.gateway.svcmeta.ServiceDef;
import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.webgui.core.svcmeta.MessageTypeDef;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.webgui.core.ServletConstants;


/**
 * A tag that acts as a factory for creating a service based order.
 * These beans will always have supplier,version and a service type associated with them.
 *
 */
public class CreateServiceBeanTag extends ServiceBeanTagBase
{
 

    public String getServiceType()
    {
        return ((ServiceDef)service).getID();
    }
    
    public MessageTypeDef getMessageType(String id) throws JspTagException
    {
        MessageTypeDef msgType = null;
        
        Iterator iter = ((ServiceDef)service).getMessageTypeDefs().iterator();
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
            MessageTypeDef mType = ((ServiceDef)service).getMessageType(body.getOutputDOM());
        
            if (mType == null)
                mType = ((ServiceDef)service).getDefaultMessageType();

            return mType;
        }
        catch (Exception e) {
            log.error("Failed to determine message type : " + e.getMessage());
            return null;
        }
        
        
    }
    


    public NFBean createBean() throws JspException
    {

        if (! (service instanceof ServiceDef) ) {
            throw new JspTagException("Invalid service object["+ service.getClass()+"] passed in. Must be of type ServiceDef");
        }
        
        return super.createBean();
        
        
    }

    protected XMLBean createNewBean(XMLGenerator bodyData) throws JspException
    {
        try {
            
            XMLGenerator headerData = new XMLPlainGenerator(PlatformConstants.HEADER_NODE);

            XMLBean bean = null;
            
            if ( bodyData == null) {
                bodyData = new XMLPlainGenerator(PlatformConstants.BODY_NODE);
            }
           
            bean = new XMLBean(headerData, bodyData);
            
            
        
            return bean;
            
        } catch (Exception e) {
            throw new JspException("Failed to create bean", e);
        }
        
        
    }

    

     
        
}

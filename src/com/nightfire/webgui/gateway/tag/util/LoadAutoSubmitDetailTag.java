/**
 * Copyright (c) 2001 NightFire Software, Inc.  Al rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.gateway.tag.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.gateway.svcmeta.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.resource.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.xml.*;



/**
 * This tag loads xml data into an xml bean for auto submition.
 * It determines the the type of message (via messageTypes),
 * runs an incoming xsl sheet. It then sets an action xsl sheet that must
 * change this gui xml into the outgoing format. Templates are applied before
 * the action xsl sheet is set. Note this is different then other cases where
 * the template is set after the action xsl, but in this case the action
 * xsl script generates the final outgoing xsl.
 *
 */

public class LoadAutoSubmitDetailTag extends LoadMessageDetailTag
{


    /**
     * Set the body-transformer object on the specified service-component bean.
     * This method is overloaded here to set the incoming transformer too.
     *
     * @param  bean     Service-component bean.
     * @param svcType The service type for this bean
     * @param supplier The supplier for this bean
     * @param messageType The message type which is used as the file name
     *
     * @return a <code>String</code> value
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected String setTransformers(XMLBean bean, String svcType, String supplier, String version, String messageType) throws Exception
    {
        //Set incoming transformer.
        String incomingBodyTransformPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_IN, messageType);

        if (log.isDebugEnabled())
        {
            log.debug("setBodyTransformer(): Getting incoming body-transformer object from [" + incomingBodyTransformPath + "] ...");
        }

        Object bodyTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), incomingBodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);


        bean.setBodyTransform(bodyTransformer);

        log.debug("Executing incoming body transform object on the bean ...");
        bean.transform();
        bean.clearTransforms();


        if(action == null) {
            log.error("Action Def Object must be passed in");
            throw new JspTagException("Action Def Object must be passed in");
        }

        //perform action xsl
        String key = action.getActionName();
            
        //IMPORTANT: we need to reset the message type here since 
        //the action changed the message type
        messageType = action.getMessageType();
         
        String actionPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_NONE, key);

        if (log.isDebugEnabled())
            log.debug("setBodyTransformer(): Getting action transform object from [" + actionPath + "] ...");


        Object actionTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), actionPath, ServletConstants.XSL_TRANSFORM_DATA);


        bean.setBodyTransform(actionTransformer);

        // in this case we run all xsl scripts here so that we
        // we are in outgoing xml already. This allows the header
        // generation to access the gui header and outgoing xml.
        // 
        log.debug("Executing action transform object on the bean ...");
        bean.transform();
        // no body transformation will be done by the data adapter 
        bean.clearTransforms();
       

        return messageType;
        
 
   
    }


    protected void setMetaData(XMLBean bean, String svcType, String supplier, String version, String messageType) throws Exception
    {
        //do nothing, for auto submit we do not need to display the message.
    }


}

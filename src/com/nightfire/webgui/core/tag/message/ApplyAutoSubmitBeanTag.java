/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;



import java.util.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.XMLBean;
import com.nightfire.webgui.core.svcmeta.ActionDef;

/**
 * A tag that modifies a service related bean by setting the metadata, outgoing transform
 * and template resources.
 * Must be used within the body of a ModifyServiceTagBase class.
 *
 */
public class ApplyAutoSubmitBeanTag extends ApplyActionBeanTag
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

        // clear all transforms first
        bean.clearTransforms();

        
        //perform action xsl
        String key = action.getActionName();
            
        // we need to reset the message type here
        // if it is not a passthrough. 
        // Even in the passthrough case we always execute the action xsl on the body
        // since we do not have an outgoing xsl script for auto submit.
        // This is different then the apply action bean tag, since we have another
        // outgoing xsl to run in that case.
        if(!action.getMessageType().equals(ActionDef.PASS_MSG_TYPE_NAME))
            messageType = action.getMessageType();
         
        String actionPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_NONE, key);

        if (log.isDebugEnabled())
            log.debug("setBodyTransformer(): Getting action transform object from [" + actionPath + "] ...");


        Object actionTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), actionPath, ServletConstants.XSL_TRANSFORM_DATA);


        bean.setBodyTransform(actionTransformer);

        // in this case we run all xsl scripts here so that we
        // we are in outgoing xml already. This allows the header
        // generation to access the gui header and outgoing xml.
        log.debug("Executing action transform object on body of bean ...");
        bean.transform();
        // no body transformation will be done by the data adapter 
        bean.clearTransforms();
            
        
        // set the header xsl
        // we let the dataAdapter execute the header xsl so that it
        // can obtain any needed fields first.
        String outgoingHeaderTransformPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_NONE , ServletConstants.HEADER_TRANSFORM_XSL );

        if (log.isDebugEnabled())
            log.debug("setTransformer(): Getting outgoing header transformer from ["+ outgoingHeaderTransformPath +"] ...");
        
        Object hTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), outgoingHeaderTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
        bean.setHeaderTransform(hTransformer);


        return messageType;
        
 
   
    }

    
}



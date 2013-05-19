/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;



import java.util.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.XMLBean;
import com.nightfire.webgui.core.svcmeta.ActionDef;
import com.nightfire.webgui.core.svcmeta.MessageTypeDef;

/**
 * A tag that modifies a service related bean by setting the metadata, outgoing transform
 * and template resources.
 * Must be used within the body of a ModifyServiceTagBase class.
 *
 */
public class ApplyActionBeanTag extends SetupBeanTag
{

    //default to true
    protected ActionDef action = null;


    /**
     * Indicates if templates should be loaded. This defaults to true.
     *
     */
    public void setAction(Object action)  throws JspException
    {
        setDynAttribute("action", action, ActionDef.class);
    }


    public int doStartTag() throws JspException
    {



        action  = (ActionDef)getDynAttribute("action");

        if(action == null) {
            // log does not exist at this point since we did not call super.doStartTag
            throw new JspTagException("ActionDef object is null.");
        }



        super.doStartTag();


        return SKIP_BODY;


    }




    /**
     * Set the body-transformer object on the specified bean.
     * This method is overloaded here to set the incoming transformer too.
     *
     * @param  bean     bean to operate on.
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     * @param messageType The message type which is used as the file name
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected String setTransformers(XMLBean bean, String svcType, String supplier, String version, String messageType) throws Exception
    {



        // and if the action changes the message type.
        if(!action.getMessageType().equals(ActionDef.PASS_MSG_TYPE_NAME) &&
           !action.getMessageType().equals(messageType)) {
            String key = action.getActionName();

            if (log.isDebugEnabled() )
                log.debug("Applying action ["+ key + "], Changing message type from [" + messageType + "] to [" + action.getMessageType() +"]");


            //IMPORTANT: we need to reset the message type here since
            //the action changed the message type
            messageType = action.getMessageType();

            String actionPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_NONE, key);

            if (log.isDebugEnabled())
                log.debug("setBodyTransformer(): Getting action transform object from [" + actionPath + "] ...");


            Object actionTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), actionPath, ServletConstants.XSL_TRANSFORM_DATA);

            Object oldHeaderTransform = bean.getHeaderTransform();
            Object oldBodyTransform   = bean.getBodyTransform();

            bean.clearTransforms();

            // modify body only
            bean.setBodyTransform(actionTransformer);

            log.debug("Executing action transform object on the bean ...");
            bean.transform();

            bean.clearTransforms();

            // set back the old transforms
            bean.setHeaderTransform(oldHeaderTransform);
            bean.setBodyTransform(oldBodyTransform);


        } else {
            log.debug("Action [" + action.getActionName() +"] message type result is the same as the current message type. No need to apply action.");
        }


        //Let default behaviour for setting outgoing transformer take effect.
        return super.setTransformers( bean, svcType, supplier, version, messageType);

    }




    public void release()
    {
        super.release();

        action = null;

    }

}



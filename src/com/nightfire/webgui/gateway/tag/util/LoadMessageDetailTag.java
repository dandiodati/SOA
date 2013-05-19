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
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.resource.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.xml.*;



/**
 * This tag loads xml data into an xml bean, and places it in
 * the request and into the message data cache. It also sets the meta file and
 * applying incoming transforms, sets outing transforms, sets templates, and sets some control header fields.
 *
 */

public class LoadMessageDetailTag extends SetupNewMessageDetailTag
{

   /**
    * The xml document to add
    * to the session buffer.
    */
    protected Object data;

    protected ActionDef action;



     /**
     * The xml to use
     */
    public void setData(Object data) throws JspException
    {
       this.data = TagUtils.getDynamicValue("data", data, Object.class, this, pageContext);
    }

     /**
     * The xml to use
     */
    public void setAction(Object action) throws JspException
    {
       this.action = (ActionDef)TagUtils.getDynamicValue("action", action, ActionDef.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     *
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        return SKIP_BODY;


    }


    protected XMLBean createBean() throws FrameworkException, JspException, ServletException
    {


       try {


           XMLGenerator bodyData = null;

           XMLGenerator headerData = new XMLPlainGenerator("Header");

           XMLBean bean = null;



           if ( data != null) {
               if ( data instanceof XMLGenerator)
                   bodyData = (XMLGenerator)data;
               else if (data instanceof String )
                   bodyData = new XMLPlainGenerator((String)data);
              else if (data instanceof Document)
                  bodyData = new XMLPlainGenerator((Document)data);

               return new XMLBean(headerData, bodyData);


           }
           else {
               log.error("Data attribute requires xml data");

               throw new JspTagException("Data attribute requires xml data");
           }





       } catch (ServletException e) {
           String err = "Failed to create bean : " + e.getMessage();

           log.error(err);
           throw new JspTagException(err);


       } catch (FrameworkException e ) {
          String err = "Failed to create bean : " + e.getMessage();
          log.error(err,e);
          throw new JspTagException(err);
       }

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

        if (action != null) {
 
            boolean changeMessageType = !action.getMessageType().equals(messageType) ? true : false ;
            boolean changeServiceType = action.getServiceTypeToConvert()!= null ? true : false;
            //perform action xsl if an action is passed in
            // and if the action changes the message type or action converts the service type
            if(changeMessageType || changeServiceType) {
                String key = action.getActionName();
                Object actionTransformer =null;

                if (log.isDebugEnabled() )
                {
                    if (changeMessageType)
                        log.debug("Applying action ["+ key + "], Changing message type from [" + messageType + "] to [" + action.getMessageType() +"]");
                    else
                        log.debug("Applying action ["+ key + "], Changing service type to [" + action.getServiceTypeToConvert() +"]");
                }
            
                //IMPORTANT: we need to reset the message type here since 
                //the action changed the message type
                messageType = action.getMessageType();
           
                String actionPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_NONE, key);

                if (log.isDebugEnabled())
                    log.debug("setBodyTransformer(): Getting action transform object from [" + actionPath + "] ...");

                try
                {
                    actionTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), actionPath, ServletConstants.XSL_TRANSFORM_DATA);
                }
                catch(ServletException se)
                {
                    log.error("XSL "+actionPath +" not found for action "+key);
                    actionTransformer= ServletUtils.getLocalResource(pageContext.getRequest(), ServletConstants.RESOURCE_ROOT_PATH+"/"+ServletConstants.XSL_DIRECTORY+"/passThrough.xsl", ServletConstants.XSL_TRANSFORM_DATA);
                }

                bean.setBodyTransform(actionTransformer);

                log.debug("Executing action transform object on the bean ...");
                bean.transform();
                bean.clearTransforms();
            }else {
                log.debug("Action [" + action.getActionName() +"] message type result is the same as the current message type. No need to apply action.");
            }
        }
        
        


        //Let default behaviour for setting outgoing transformer take effect.
        return super.setTransformers( bean, svcType, supplier, version, messageType);
    }

    public void release()
    {
       super.release();

       data = null;
       action = null;


    }

}

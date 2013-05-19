/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.*;

import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;

import  java.util.*;

import  javax.servlet.*;
import javax.servlet.http.*;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;
import java.net.*;
import java.io.*;

import org.w3c.dom.*;


/**
 * This tag is responsible for setting xml message data into the session buffer for future
 * access.
 *
 * It can provide a new message id (It uses the same sequence of message ids as
 * the main ControllerServlet so that there will not be a conflict), or use an existing
 * one.
 *
 */
public class SetMessageTag extends VariableTagBase
{

   /**
    * The xml document to add
    * to the session buffer.
    */
    private Object fieldData;

    private String beanType;

    /**
     * The message id that child classes need to set if the attribute is provided.
     */
    private String messageId = null;


     /**
     * The location of the field data.
     */
    public void setFieldData(Object fieldData) throws JspException
    {
       this.fieldData = TagUtils.getDynamicValue("fieldData", fieldData, Object.class, this, pageContext);
    }

    /**
     * Provides a current message id to use.
     * @param id A valid message id to access an existing data bean.
     *
     */
    public void setMessageId(String id) throws JspException
    {
       messageId = (String) TagUtils.getDynamicValue("messageId", id, String.class, this, pageContext);
    }


    public void setBeanType(String type)
    {
       beanType = type;
    }

    /**
     * Starts procesing of this tag.
     * This method evaluates the fieldData variable and adds a data bean to the session
     * identified by message id.
     * fieldData can be of type String, Document or XMLGenerator.
     * NOTE: This class only added xml to the body and does not add header fields.
     *
     * The following behavior exists for each type:
     *
     * 1. String or Document - An xml of the body that should be added.
     * 2. XMLGenerator - A generator with just the body xml. This can be
     *                   used to pass in a template generator to add support for templates.
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();
      

       if ( StringUtils.hasValue(beanType) ) {
          if ( !(beanType.equals(ServletConstants.REQUEST_BEAN) || beanType.equals(ServletConstants.RESPONSE_BEAN) )  )
             log.warn("Using a bean type [" + beanType + "], which is not " + ServletConstants.REQUEST_BEAN + " or " +  ServletConstants.RESPONSE_BEAN);
       } else
          beanType = ServletConstants.REQUEST_BEAN;

       BufferedReader buf = null;
       HttpSession session = pageContext.getSession();

       NFBean bean;

       try {


       XMLGenerator bodyParser = null, headerParser = null;

       if ( fieldData == null) {
          bodyParser = new XMLPlainGenerator(PlatformConstants.BODY_NODE);
       } else if (fieldData instanceof String ) {
          bodyParser = new XMLPlainGenerator((String) fieldData);
       } else if (fieldData instanceof XMLGenerator ) {
          bodyParser =  (XMLGenerator)fieldData;
       } else if (fieldData instanceof Document ) {
          bodyParser = new XMLPlainGenerator((Document) fieldData);
       } else {
          String err = "SetMessageTag: Invalid field data passed in, Only support objects String, Document, or XMLGenerator.";
          log.error(err);
          throw new JspTagException( err);
       }


       bean = new XMLBean(new XMLPlainGenerator(PlatformConstants.HEADER_NODE), bodyParser);


       log.debug("Trying to add data to NFBean with messageId [" + messageId + "].");

       messageId = TagUtils.setBeanInMsgCache(pageContext, messageId, beanType, bean);


       if (log.isDebugEnabled() )
          log.debug("Setting body xml with messageId["+ messageId +"] :\n" + bean.describeBodyData());

       // sets the message id at the defined var variable.
       setVarAttribute(messageId);

       } catch (Exception e ) {
          String err = "SetMessageTag : Failed to set message data: " + e.getMessage();
          log.error(err);
          log.error("",e);
          throw new JspTagException(err);
       }
       
       return SKIP_BODY;
    }



    public void release()
    {
       super.release();

       beanType = null;
       fieldData = null;
       messageId = null;
    }

}

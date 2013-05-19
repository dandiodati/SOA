/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.*;


import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;

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
 * This tag creates a link from a bean to a bean within a bean bag.
 *
 */
public class LinkBeanTag extends NFTagSupport
{


    private NFBean sourceBean;

    private String sourceId;

    private String linkBean;

    /**
     * The message id that child classes need to set if the attribute is provided.
     */
    private String messageId = null;


    /**
     * The source bean to link to.
     */
    public void setSourceBean(Object sourceBean) throws JspException
    {
       this.sourceBean = (NFBean)TagUtils.getDynamicValue("sourceBean", sourceBean, NFBean.class, this, pageContext);
    }


     /**
     * The source bean id, used if sourceBean is a NFBeanBag.
     */
    public void setSourceId(String sourceId) throws JspException
    {
       this.sourceId = (String)TagUtils.getDynamicValue("sourceId", sourceId, String.class, this, pageContext);
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


    /**
     * The name of the bean bag to create.
     *
     */
    public void setLinkBean(String link)  throws JspException
    {
       linkBean = (String) TagUtils.getDynamicValue("linkBean", link, String.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     *
     */
    public int doStartTag() throws JspException
    {
       super.doStartTag();
      
       NFBean bean = null;
       if (sourceBean != null && StringUtils.hasValue(messageId)) {

          String beanId;
          if ( sourceBean instanceof NFBeanBag) {
             bean = ((NFBeanBag)sourceBean).getBean(sourceId);
             if (log.isDebugEnabled() )
                log.debug("Creating a link from bean bag: bean Id [" +  sourceId + "] to link [" + linkBean +"], Using messageId [" + messageId +"]");
          } else {
             bean = sourceBean;
              if (log.isDebugEnabled() )
                log.debug("Creating a link from bean, Id [" +  bean.toString() + "] to link [" + linkBean +"], Using messageId [" + messageId +"]");
          }
          

          if (bean == null)
             log.error("Failed to obtain sourceBean, skipping link creation.");
          else
             TagUtils.setBeanInMsgCache(pageContext, messageId, linkBean, bean);
       }  else
          log.error("MessageId [" + messageId +"] or source Bean [" + sourceBean +"] is null, skipping link creation.");
       
       return SKIP_BODY;
    }


    public void release()
    {
       super.release();

       linkBean = null;
       sourceBean = null;
       messageId = null;
       sourceId = null;
    }

}

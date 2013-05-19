/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;


import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;

import com.nightfire.framework.constants.*;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.core.beans.*;

/**
 * Base tag for tags that want to set a bean in the message cache
 */
public abstract class BeanTagBase extends VariableTagBase
{

    private String messageId;
    private String beanName;

    private NFBean bean;


    public void setMessageId(String id )  throws JspException
    {
        setDynAttribute("messageId", id, String.class);
    }

    public void setBeanName(String name )  throws JspException
    {
        setDynAttribute("beanName", name, String.class);
    }


    public NFBean getBean()
    {
        return bean;
    }

    public String getBeanName()
    {
        return beanName;
    }
    

    public void setBean(NFBean bean)
    {
        this.bean = bean;
    }


    /**
     * Called when a new tag is encountered after the attributes have been
     * Child classes need to call super.doStartTag after any calls to getDynAttribute but before
     * before any processing
     * if they overload the doStartTag.
     *
     * Initializes resources for sub classes.
     *
     * @return SKIP_BODY So by default it does not process the body of the tag.
     *
     * If a child class needs to access its body , then it should overload the
     * doStartTag, call super.doStartTag first and then
     * return the correct code for body evaluation.
     *
     * @see javax.servlet.jsp.tagext.Tag
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        messageId = (String)getDynAttribute("messageId");

        beanName = (String) getDynAttribute("beanName");

        // create the bean upfront so that it is available for modification for inner tags.
        bean = createBean();


        return EVAL_BODY_INCLUDE;

    }


    /**
     * This is called in the doStartTag method to create the initial working bean that
     * will be set in the the message cache at a later time.
     *
     * @return a <code>NFBean</code> value
     * @exception JspException if an error occurs
     */
    public abstract NFBean createBean() throws JspException;



    public int doEndTag() throws JspException
    {

        super.doEndTag();

        // now set the bean after excuting all inner tags.

        // if we are in a container tag then add the bean to it.
        // otherwise add the bean to the message cache.


        ContainerBeanTagBase tag  = (ContainerBeanTagBase) findAncestorWithClass(this, ContainerBeanTagBase.class);

        if (tag != null)
            ((ContainerBeanTagBase)tag).addBean(beanName, bean);
        else
            messageId = TagUtils.setBeanInMsgCache(pageContext,messageId,beanName,bean);

        setVarAttribute(messageId);

        return EVAL_PAGE;

    }


    public void release()
    {
        super.release();
        bean = null;


        beanName = null;

    }








}

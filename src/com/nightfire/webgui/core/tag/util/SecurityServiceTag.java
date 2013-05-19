/**
 * Copyright (c) 2003 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  java.io.*;
import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.http.*;

import  com.nightfire.security.*;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.beans.*;


/**
 * This tag represents a convenient service that performs authorization against NF
 * Security Service.  This is extremely useful in allowing the pages to show or not 
 * to show action widgets (buttons, links) based on the user's granted access level.
 * Keep in mind that this tag can only be used after the user has successfully been
 * authenticated.
 */
 
public class SecurityServiceTag extends VariableBodyTagBase implements ServletConstants
{    
    private String action;
    
    
    /**
     * Setter method for tag attribute 'action'.
     *
     * @param  action  The action to set to.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setAction(Object action) throws JspException
    {
        this.action = (String)TagUtils.getDynamicValue("action", action, Object.class, this, pageContext);
    }
  
    /**
     * Redefinition of parent's doStartTag().  This method is invoked before
     * tag-body evaluation.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  EVAL_BODY_TAG or SKIP_BODY.
     */
    public int doStartTag() throws JspException
    {        
        super.doStartTag();
        
        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Authorizing action [" + action + "] via NF Security Service ...");
        }

        try
        {
            boolean isAuthorized = ServletUtils.isAuthorized(pageContext.getSession(), action);
        
            if (isAuthorized)
            {
                return EVAL_BODY_TAG;
            }
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to perform authorization on action [" + action + "]: " + e.getMessage();
            
            log.error("doStartTag(): " + errorMessage);
            
            throw new JspTagException(errorMessage);
        }
        
        return SKIP_BODY;
    }

    /**
     * Redefinition of doAfterBody() in BodyTagSupport.  This method is invoked
     * after every tag-body evaluation.
     *
     * @exception  JspException  Thrown when the result cannot be written to the 
     *                           output stream.
     *
     * @return  EVAL_BODY_TAG or SKIP_BODY.
     */
    public int doAfterBody() throws JspException
    {
        try
        {
            getBodyContent().writeOut(getPreviousOut());
        }
        catch (IOException e)
        {
            String errorMessage = "Failed to write the result to the output stream:\n" + e.getMessage();
            
            log.error("doAfterBody(): " + errorMessage);
            
            throw new JspTagException(errorMessage);
        }
        
        return SKIP_BODY;
    }
        
    /**
     * This method gets called by the servlet container before the tag is destroyed.
     */
    public void release()
    {
        super.release();
    }
}
 
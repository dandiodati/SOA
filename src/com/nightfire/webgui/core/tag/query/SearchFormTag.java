/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.query;

import com.nightfire.webgui.core.tag.message.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.tag.*;

import com.nightfire.webgui.core.meta.*;



import java.io.*;
import javax.servlet.jsp.*;

import javax.servlet.jsp.tagext.*;

import com.nightfire.framework.util.*;



/**
 * The Search form tag represents a Form in an XML message.
 * It must have a ancestor of type MessageTag.
 * This tag is used by a search page.
 */
public class SearchFormTag extends MessageContainerBaseTag
{


    /**
     * Creates html for the start of a form.
     * @return {@link javax.servlet.jsp.tagext.EVAL_BODY_INCLUDE} to evaluate the body
     * of this tag.
     */
    public int doStartTag() throws JspException
    {

        super.doStartTag();

        if ( log.isDebugEnabled() )
           log.debug("Creating Search Form [" + curPart.getID() +"]");

        try {
          pageContext.getOut().write("<TABLE class=\"" + TagConstants.CLASS_FIELD_TABLE+ "\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><TD><img src=\"" + contextPath +"/"+ TagConstants.IMG_SEARCH_TITLE + "\" alt=\"Search\" /></TD></tr></table>" );
        }
        catch (Exception ex)
        {
            String err = "SearchFormTag Failed to write start of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }


        return EVAL_BODY_INCLUDE;

    }

}

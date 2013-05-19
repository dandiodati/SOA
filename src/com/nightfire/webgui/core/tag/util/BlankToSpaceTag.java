/** 
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/tag/util/BlankToSpaceTag.java#1 $
 */
package com.nightfire.webgui.core.tag.util;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import com.nightfire.framework.util.StringUtils;

import com.nightfire.webgui.core.tag.*;


/**
 * <p><strong>BlankToSpaceTag</strong> is the tag handler which prints a 
 * non breaking space character when the given string has an empty value.
 *
 * <p>Sample Usage: 
 * 
 * <nf:BlankToSpace fieldValue="$SomeFieldvalue"/>
 *
 * or
 * 
 * <nf:BlankToSpace><x:out select="SomeField/@value"/></nf:BlankToSpace>
 *
 */
 
public class BlankToSpaceTag extends VariableBodyTagBase
{  
    /**
     * the String Value
     */
    private String fValue = null;

    /**
     * the name of tag attribute
     */
    private static String VALUE_ATTRIBUTE = "fieldValue";

    /**
     * Flag to determine the use of tag attribute on the JSP
     */
    private boolean usedTagAttribute = false;

    
    /**
     * Sets the field value from the tag attribute to the local variable
     */
    public void setFieldValue ( Object fieldValue ) throws JspException
    {
            this.fValue = (String) TagUtils.getDynamicValue( VALUE_ATTRIBUTE , fieldValue, Object.class, this, pageContext);
            usedTagAttribute = true;
    }

    /**
     * Prints the &nbsp; if the specified string is empty.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY.
     */
    public int doEndTag() throws JspException
    {
        //When the value is specified through the body of the tag 
        if ( ! usedTagAttribute )
        {
            log.debug("Reading the value from the body of the tag");
            BodyContent body = getBodyContent();
            if ( body != null ) fValue = body.getString();
        }

	//At this point, the value should have either been passed through the attribute
        //or through the body of the tag.
        log.debug("Value given to substitue with space is [" + fValue +"]");
        
        try
        {
            if ( ! StringUtils.hasValue ( fValue ) )
            {
                log.debug("Printing a non breaking space");
                pageContext.getOut().print("&nbsp;");
            }
            else
            {    
                pageContext.getOut().print( fValue );
            }
        }
        catch(Exception e)
        {
            throw new JspException("Couldn't print value to the output. Reason : " + e.getMessage() );
        }

        return EVAL_PAGE;
    }
        
    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();
        fValue = null;
    }
    
}

/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.navigation;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;



/**
 * Abtract class that represents a BarTag used to display a navigation bar.
 * This class contains common routines used by each BarTag.
 * NOTE: Each BarTag must exist within a BarGroupTag.
 */
public abstract class BarTagBase extends VariableTagBase
{


    /**
     * Defines the name which gets displayed for this bar.
     */
    protected String name;


    /**
     * Holds a reference to the parent BarGroup tag.
     */
    protected BarGroupTag barGrp;


    /**
     * Tag setter method.
     * Sets the name of the bar which is used as the title of the bar.
     */
    public void setDisplayName(String name) throws JspException
    {
       this.name = (String) TagUtils.getDynamicValue("displayName", name, String.class, this, pageContext);
    }


    /**
     * Redefinition of doStartTag() in TagSupport.  This method processes the
     * start tag for this instance.
     * This method obtains reference to the parent BarGroup tag.
     * Note: Child classes must call super.doStartTag() at the beginning of a doStartTag
     * , if defined.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {

       // replace any spaces with html entities so that no line wrapping
       // happens
       if (StringUtils.hasValue(name) )
          name = StringUtils.replaceSubstrings(name, " ", "&nbsp;");
          
       barGrp = (BarGroupTag) this.findAncestorWithClass(this, BarGroupTag.class);

       if ( barGrp == null) {
          String err = StringUtils.getClassName(this) + " : Failed to find a parent BarGroupTag. This tag must exist within a BarGroupTag.";
          log.error(err);
          throw new JspTagException(err);
       }

       // add this Bar to the parent so that it knows about this child.
       barGrp.getBarList().add(id);

       return SKIP_BODY;

    }


    public void release()
    {
        super.release();
        barGrp = null;
        name = null;

    }
}
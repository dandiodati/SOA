/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;





/**
 * This is a tag replaces spaces in strings with html &nbsp; entites for displaying.
 *
 */
public class ReplaceSpacesTag extends VariableTagBase
{


    private String value = null;


    /**
     * Sets the value of the parameter
     */
    public void setValue(String value) throws JspException
    {
       this.value = (String) TagUtils.getDynamicValue("value", value, String.class, this, pageContext);
    }




    /**
     * Starts processing of the tag.
     * @return EVAL_BODY_BUFFERED is returned so that the body is evaluated.
     */
    public int doStartTag() throws JspException
    {

      super.doStartTag();
      
       try  {

          String newStr = TagUtils.escapeHtmlSpaces(value);

          if ( newStr == null)
             newStr = "";

          if ( this.varExists() )
             this.setVarAttribute(newStr);
          else
             pageContext.getOut().print(newStr);



       } catch (Exception e) {
          String err = StringUtils.getClassName(this) + " Failed to write out escaped string: " + e.getMessage();
          log.error(err);
          throw new JspTagException(err);
       }

       return SKIP_BODY;

    }


     /**
      * Cleans up resources.
      */
     public void release()
    {
       super.release();
       value = null;


    }

}

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
 * This is a tag which provides an name and value pair or parameter to a parent tag.
 * It can be used with tags within the NFTL that support it.
 *
 */
public class ParamTag extends NFBodyTagSupport
{


    /**
     * Name of the parameter. Must be set as an attribute.
     */
    private String name;

    /**
     * Value of the parameter. Must be set as an attribute.
     * If not defined the body of the tag is used.
     */
    private Object value = null;


     /**
     * Sets the name of the parameter
     */
    public void setName(String name)  throws JspException
    {
       this.name = (String) TagUtils.getDynamicValue("name", name, String.class, this, pageContext);
    }


    /**
     * Sets the value of the parameter
     */
    public void setValue(Object value) throws JspException
    {
       this.value = TagUtils.getDynamicValue("value", value, Object.class, this, pageContext);
    }


    private ParamContainerTagBase parent;

    /**
     * Starts processing of the tag.
     * @return EVAL_BODY_BUFFERED is returned so that the body is evaluated.
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();
      
       Tag parObj = getParent();
       try  {

          while(parObj!= null && !(parObj instanceof ParamContainerTagBase) )
          {
             parObj  = parObj.getParent();
          }

          if (parObj == null)
             throw new NullPointerException("no parent found");

          parent = (ParamContainerTagBase) parObj;
          
          if (value != null)
          {
             parent.setParam(name.trim(), value);
          }

       } catch (Exception e) {
          String err = StringUtils.getClassName(this) + " : Failed to find the correct parent type. This tag must exist within a ParamContainerTagBase: " + e.getMessage();
          log.error(err);
          throw new JspTagException(err);
       }

       return BodyTagSupport.EVAL_BODY_BUFFERED;

    }
    
    /**
     * Processes body of tag if it exists.
     * Obtains the value of this tag by the value attribute or the body of the tag.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doAfterBody() throws JspException
    {  
        // in an older container the method will get called even if there
        // is no body
        // so we check if a value does not exists first
        if (bodyContent != null)
        {
            if ( value == null )
               parent.setParam(name.trim(), bodyContent.getString().trim() );
            
            bodyContent.clearBody();
        }
        
        return SKIP_BODY;
    }
    
     /**
      * Cleans up resources.
      */
     public void release()
    {
       super.release();
       name = null;
       value = null;
       

    }

}

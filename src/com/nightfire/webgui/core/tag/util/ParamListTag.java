/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
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
 * This is a tag is an instance of the ParamTag and can contains instances of
 * ParamTag within itself.
 */
public class ParamListTag extends ParamContainerTagBase
{

  /**
   * Name of the parameter. Must be set as an attribute.
   */
  private String name;

   /**
   * Sets the name of the parameter
   */
  public void setName(String name) throws JspException
  {
     setDynAttribute("name", name, String.class);
  }


  public int doStartTag() throws JspException
  {
      super.doStartTag();
      name = (String) getDynAttribute("name");
      return EVAL_BODY_BUFFERED;

  }
    
      
  /**
   * Starts processing of the tag.
   * @return EVAL_PAGE is returned so that the body is evaluated.
   */
  public int doEndTag() throws JspException
  {
    Tag parObj = getParent();

    while(parObj!= null && !(parObj instanceof ParamContainerTagBase) )
    {
       parObj  = parObj.getParent();
    }
    if (parObj == null)
      throw new NullPointerException("no parent found");

    NVPairGroup pairList = new NVPairGroup(name, id, getParamList());
    ((ParamContainerTagBase)parObj).setParam( pairList);

    return EVAL_PAGE;
  }


  public void release()
  {
      super.release();

      name = null;
  }


}

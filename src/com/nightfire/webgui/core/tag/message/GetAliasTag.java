/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;

import com.nightfire.webgui.core.meta.*;



import java.io.*;
import javax.servlet.jsp.*;

import javax.servlet.jsp.tagext.*;

import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;




/**
 * A tag which returns an alias of a value
 * Used to obtain values that will be used as a label on a page.
 */
public class GetAliasTag extends VariableTagBase
{


    private String group;
    private String value;
    private String defaultValue;


    /**
     * Set the alias group
     */
    public void setGroup(String group) throws JspException
    {
       this.group = (String) TagUtils.getDynamicValue("group",group, String.class,this,pageContext);
    }

    /**
     * set the value attribute to retrieve an alias for
     */
    public void setValue(String value) throws JspException
    {
        this.value = (String) TagUtils.getDynamicValue("value",value, String.class,this,pageContext);
    }


    /**
     * Set default value to use if an alias is not found
     */
    public void setDefaultValue(String value) throws JspException
    {
       this.defaultValue= (String) TagUtils.getDynamicValue("defaultValue",value, String.class,this,pageContext);
    }


    /**
     *
     */
    public int doStartTag() throws JspException
    {
      setup();
      
        if (log.isDebugEnabled() )
           log.debug("Trying to get alias for group [" + group +"], value [" + value +"], defaultValue [" + defaultValue +"]");

        AliasDescriptor ad = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE );
        String output = "";

        if ( StringUtils.hasValue(defaultValue) ) {
           String res = ad.getAlias(pageContext.getRequest(), group, value, false);
           if (StringUtils.hasValue(res) )
              output =  res;
           else
              output =  defaultValue;
        } else
        {
            String res = ad.getAlias(pageContext.getRequest(),group, value, true);
            if (StringUtils.hasValue(res) )
                output =  res;
        }

         if (log.isDebugEnabled() )
           log.debug("Got a return value of  ["+ output +"]");


        try
        {
           if (this.varExists() )
              this.setVarAttribute(output);
           else
               pageContext.getOut().write(output);

        }
        catch (Exception ex)
        {
            String err = "GetAliasTag Failed to write start of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }



        return EVAL_BODY_INCLUDE;

    }


    public void release()
    {
       super.release();

       group = null;
       value = null;
       defaultValue = null;
    }

}

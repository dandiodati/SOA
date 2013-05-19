/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  org.w3c.dom.*;

import  java.io.*;
import  java.util.*;

import  javax.servlet.jsp.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.common.xml.*;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.tag.*;


/**
 * This tag outputs an html SELECT element based on the list of options passed
 * in.  It also provides a hook for Javascript callback when a selection occurs.
 *
 * Sample usage: <gui:createDropDownList name="NF_UserDropDown" data="${userList}" selectItem="Tom" jsOnChangeHandler="userSelected" jsOnClickHandler="dropdownClicked"/>
 *
 * where: NF_UserDropDown  is the name of the SELECT element.  This is required.
 *        userList         is the option list, of Java type List.  Each element in the
 *                         the list is of type NVPair, where its 'name' is the value of
 *                         the option and its 'value' is the display name of the option.
 *                         This is required.
 *        selectItem       is the actual value of the item to be displayed as selected.
 *                         Without this attribute, the first item is selected.  This is
 *                         optional.
 *        userSelected     is the name of Javascript function that executes for the
 *                         'onChange' event handler.  The script will be passed the
 *                         SELECT object as its single argument.  This is optional.
 *        dropdownClicked  is the name of Javascript function that executes for the
 *                         'onClicked' event handler.  The script will be passed the
 *                         SELECT object as its single argument.  This is optional.
 */

public class CreateDropDownList extends VariableTagBase
{
    private String  name;

    private Object  data;

    private String  selectItem;

    private String  jsOnChangeHandler;

    private String  jsOnClickHandler;

    private String  htmlAttributes;

    private boolean showEmpty;


    /**
     * Setter for the 'name' attribute.
     *
     * @param  name  Name of the drop-down element.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setName(String name) throws JspException
    {
        this.name = (String)TagUtils.getDynamicValue("name", name, String.class, this, pageContext);
    }

    /**
     * Setter for the 'data' attribute.
     *
     * @param  data  Options to use in the drop-down element.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setData(Object data) throws JspException
    {
        this.data = (Object)TagUtils.getDynamicValue("data", data, Object.class, this, pageContext);
    }

    /**
     * Setter for the 'selectItem' attribute.
     *
     * @param  selectItem  Option value to select when the drop-down is displayed.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setSelectItem(String selectItem) throws JspException
    {
        this.selectItem = (String)TagUtils.getDynamicValue("selectItem", selectItem, String.class, this, pageContext);
    }

    /**
     * Setter for the 'jsOnChangeHandler' attribute.
     *
     * @param  jsOnChangeHandler  Javascript function name to use for onChange event handler.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setJsOnChangeHandler(String jsOnChangeHandler) throws JspException
    {
        this.jsOnChangeHandler = (String)TagUtils.getDynamicValue("jsOnChangeHandler", jsOnChangeHandler, String.class, this, pageContext);
    }

    /**
     * Setter for the 'jsOnClickHandler' attribute.
     *
     * @param  jsOnClickHandler  Javascript function name to use for onClick event handler.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setJsOnClickHandler(String jsOnClickHandler) throws JspException
    {
        this.jsOnClickHandler = (String)TagUtils.getDynamicValue("jsOnClickHandler", jsOnClickHandler, String.class, this, pageContext);
    }

    /**
     * Setter for the 'htmlAttributes' attribute.
     *
     * @param  htmlAttributes  Html attributes to set on the SELECT element.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setHtmlAttributes(String htmlAttributes) throws JspException
    {
        this.htmlAttributes = htmlAttributes;
    }

    /**
     * The name of the bean bag to create.
     *
     */
    public void setShowEmpty(String bool)  throws JspException
    {
         String b = (String)TagUtils.getDynamicValue("showEmpty", bool, String.class, this, pageContext);

         showEmpty = StringUtils.getBoolean(b, false);
    }

    /**
     * Redefinition of parent's doStartTag().  This method processes the start
     * start tag for this instance.  It is invoked by the JSP page implementation
     * object
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        StringBuffer html = new StringBuffer("<select name=\"");

        html.append(name);

        html.append("\"");

        if (StringUtils.hasValue(htmlAttributes))
        {
            html.append(" ");

            html.append(htmlAttributes);
        }

        if (StringUtils.hasValue(jsOnChangeHandler))
        {
            html.append(" onChange=\"");

            html.append(jsOnChangeHandler);

            html.append("(this)");
        }

        if (StringUtils.hasValue(jsOnClickHandler))
        {
            html.append("\" onClick=\"");

            html.append(jsOnClickHandler);

            html.append("(this)");
        }

        html.append("\">");

        if (data == null)
        {
            log.debug("doStartTag(): The data is null, the drop-down will contain no options at this time.");
        }
        else if (data instanceof List)
        {
           // add an empty option if show empty is true
            if(showEmpty)
                ((List)data).add(0, new NVPair("", ""));

            int size = ((List)data).size();

            for (int i = 0; i < size; i++)
            {
                NVPair item = (NVPair)((List)data).get(i);

                html.append("<option value=\"");

                html.append(item.value);

                html.append("\"");

                // If an item to be selected is specified then watch out for it,
                // otherwise select the first item as a default.

                if (StringUtils.hasValue(selectItem))
                {
                    if (selectItem.equals(item.value))
                    {
                        html.append(" selected");
                    }
                }
                else
                {
                    if (i == 0)
                    {
                        html.append(" selected");
                    }
                }

                html.append(">");

                html.append(item.name);

                html.append("</option>");
            }
        }
        else
        {
            log.error("doStartTag(): Tag attribute 'data' only accepts argument of type java.util.List at this time.  The drop-down will contain no options at this time.");
        }

        html.append("</select>");

        try
        {
            pageContext.getOut().println(html.toString());
        }
        catch (IOException e)
        {
            String errorMessage = "Failed to write the result to the output stream:\n" + e.getMessage();

            log.error("doStartTag(): " + errorMessage);

            throw new JspException(errorMessage);
        }

        return SKIP_BODY;
    }

    public void release()
    {
       super.release();

       name              = null;

       data              = null;

       selectItem        = null;

       jsOnChangeHandler = null;

       jsOnClickHandler  = null;
    }
}

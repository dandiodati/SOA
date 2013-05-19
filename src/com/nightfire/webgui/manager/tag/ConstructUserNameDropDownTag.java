/** 
 * Copyright (c) 2003 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import javax.servlet.jsp.*;

import com.nightfire.framework.util.StringUtils;

import com.nightfire.security.*;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.tag.*;


/**
 * This tag generates an HTML drop-down list of user names based on the specified role, or
 * lack thereof.  NF's SecurityService is utilized to obtain this user list.
 *
 * Usage:  <mgr:ConstructUserNameDropDown name="NF_MyDropDown" role="csr" nameToSelect="tom" blankEntryDisplayText="Unassigned"/>
 *
 *         where role, defaultSelection, and blankEntryDisplayText attributes are optional.
 *         Values for all 3 attributes can be an expression, i.e., ${dropDownName}, ${role},
 *         ${nameToSelect}, ${blankEntryDisplayText}.
 */

public class ConstructUserNameDropDownTag extends NFTagSupport implements ServletConstants
{
    private String name;
  
    private String role;

    private String nameToSelect;    

    private String blankEntryDisplayText;

    
    /**
     * Sets the drop-down name with the value passed in from the tag attribute.
     *
     * @param  name  Name of the drop-down list.
     */
    public void setName(Object name) throws JspException
    {
        this.name = (String)TagUtils.getDynamicValue("name", name, Object.class, this, pageContext);
    }

    /**
     * Sets the role with the value passed in from the tag attribute.
     *
     * @param  name  Role to look up.
     */
    public void setRole(Object role) throws JspException
    {
        this.role = (String)TagUtils.getDynamicValue("role", role, Object.class, this, pageContext);
    }

    /**
     * Sets the default name to select in the list with the value passed in from the tag attribute.
     *
     * @param  nameToSelect  Name in the list to select.
     */
    public void setNameToSelect(Object nameToSelect) throws JspException
    {
        this.nameToSelect = (String)TagUtils.getDynamicValue("nameToSelect", nameToSelect, Object.class, this, pageContext);
    }

    /**
     * Sets the display text for the default 1st entry in the drop-down list that is blank, with the
     * value passed in from the tag attribute.
     *
     * @param  blankEntryDisplayText  The text to show for the default 1st entry in the drop-down list.
     */
    public void setBlankEntryDisplayText(Object blankEntryDisplayText) throws JspException
    {
        this.blankEntryDisplayText = (String)TagUtils.getDynamicValue("blankEntryDisplayText", blankEntryDisplayText, Object.class, this, pageContext);
    }
    
    /**
     * Redefinition of parent's method.  This is where the list of user names are obtained
     * and the HTML drop-down list is constructed.
     * 
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        SecurityService securityService = null;
        
        try
        {
            securityService = SecurityService.getInstance( TagUtils.getCustomerId(pageContext) );
        }
        catch (com.nightfire.security.SecurityException e)
        {
            String errorMessage = "Failed to obtain an instance of SecurityService: " + e.getMessage();

            log.error("doStartTag(): " + errorMessage);
                                                
            throw new JspTagException(errorMessage);
        }

        String[] userNames = null;
 
        if (StringUtils.hasValue(role))
        {
	        try
		    {
		        userNames = securityService.getAllUsers(role);
		    }
	        catch (Exception e)
		    {
                String errorMessage = "Failed to obtain all users with role [" + role + "] via SecurityService: " + e.getMessage();

                log.error("doStartTag(): " + errorMessage);

                throw new JspTagException(errorMessage);
		    }
        }
        else
        {
            userNames = securityService.getUserIds();
        }

        if (!StringUtils.hasValue(blankEntryDisplayText))
        {
            blankEntryDisplayText = "";
        }
        
        StringBuffer dropDownList = new StringBuffer("<select name=\"");

        dropDownList.append(name);

        dropDownList.append("\"><option value=\"\">");
        
        dropDownList.append(blankEntryDisplayText);
        
        dropDownList.append("</option>");

        for (int i = 0; i < userNames.length; i++)
        {
            dropDownList.append("<option ");

            if (userNames[i].equals(nameToSelect))
            {
                dropDownList.append("selected ");
            }

            dropDownList.append("value=\"");

            dropDownList.append(userNames[i]);

            dropDownList.append("\">");

            dropDownList.append(userNames[i]);

            dropDownList.append("</option>");    
        }

        dropDownList.append("</select>");

        try
        {
            pageContext.getOut().println(dropDownList.toString());
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to write the generated drop-down list HTML to the page: " + e.getMessage();

            log.error("doStartTag(): " + errorMessage);

            throw new JspTagException(errorMessage);
        }

        return SKIP_BODY;        
    }
    
    /**
     * Redefinition of parent's method.  It gets called right before this tag is destroyed.
     */
    public void release()
    {
        super.release();
    }
}

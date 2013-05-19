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
 * This tag generates an HTML drop-down list of user roles available in the system.  NF's
 * SecurityService is utilized to obtain these user roles.
 *
 * Usage:  <mgr:ConstructUserRoleDropDown name="NF_MyDropDown" roleToSelect="csr" blankEntryDisplayText="empty" onChange="userRoleChangedCallBack()"/>
 *
 *         where roleToSelect, blankEntryDisplayText, and onChange attributes are optional.
 *         Values for name, roleToSelect, and blankEntryDisplayText attributes can be an 
 *         expression, i.e., ${dropDownName}, ${roleToSelect}, ${blankEntryDisplayText}.
 */

public class ConstructUserRoleDropDownTag extends NFTagSupport implements ServletConstants
{
    private String name;

    private String roleToSelect;    

    private String blankEntryDisplayText;

    private String onChange;
    
    
    /**
     * Sets the drop-down name with the value passed in from the tag attribute.
     *
     * @param  name  Name of the drop-down list.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setName(Object name) throws JspException
    {
        this.name = (String)TagUtils.getDynamicValue("name", name, Object.class, this, pageContext);
    }

    /**
     * Sets the default role to select in the list with the value passed in from the tag attribute.
     *
     * @param  roleToSelect  Role in the list to select.
     */
    public void setRoleToSelect(Object roleToSelect) throws JspException
    {
        this.roleToSelect = (String)TagUtils.getDynamicValue("roleToSelect", roleToSelect, Object.class, this, pageContext);
    }

    /**
     * Sets the display text for the default 1st entry in the drop-down list that is blank, with the
     * value passed in from the tag attribute.
     *
     * @param  blankEntryDisplayText  The text to show for the default 1st entry in the drop-down list.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBlankEntryDisplayText(Object blankEntryDisplayText) throws JspException
    {
        this.blankEntryDisplayText = (String)TagUtils.getDynamicValue("blankEntryDisplayText", blankEntryDisplayText, Object.class, this, pageContext);
    }
    
    /**
     * Sets the Javascript call-back method that gets called when the value of the
     * drop-down changes.
     *
     * @param  onChange  The Javascript call-back method.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setOnChange(String onChange) throws JspException
    {
        this.onChange = onChange;
    }
    
    /**
     * Redefinition of parent's method.  This is where the list of user roles are obtained
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
            securityService = SecurityService.getInstance ( TagUtils.getCustomerId(pageContext) );
        }
        catch (com.nightfire.security.SecurityException e)
        {
            String errorMessage = "Failed to obtain an instance of SecurityService: " + e.getMessage();

            log.error("doStartTag(): " + errorMessage);
                                                
            throw new JspTagException(errorMessage);
        }

        
	/*
	 * No longer get all the roles in defined in the SecurityService
	 * Rather, we use a 'filter' role to retrieve all the workitem related roles.
	 */
	//String[] roles = securityService.getRoleIds();
        String[] roles = securityService.getAllSubRoles("WorkItemManagement");

        if (!StringUtils.hasValue(blankEntryDisplayText))
        {
            blankEntryDisplayText = "";
        }
        
        StringBuffer dropDownList = new StringBuffer("<select name=\"");

        dropDownList.append(name);

        dropDownList.append("\"");
        
        if (StringUtils.hasValue(onChange))
        {
            dropDownList.append(" onChange=\"");
            
            dropDownList.append(onChange);
            
            dropDownList.append("\"");
        }
        
        dropDownList.append("><option value=\"\">");
        
        dropDownList.append(blankEntryDisplayText);
        
        dropDownList.append("</option>");

        for (int i = 0; i < roles.length; i++)
        {
           
           //don't include the WorkItemManagement role in the output list
	   if ( roles[i].equals("WorkItemManagement") )
		continue;
		
	    dropDownList.append("<option ");

            if (roles[i].equals(roleToSelect))
            {
                dropDownList.append("selected ");
            }

            dropDownList.append("value=\"");

            dropDownList.append(roles[i]);

            dropDownList.append("\">");

            dropDownList.append(roles[i]);

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

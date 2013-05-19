/*
 * Copyright(c) 2001 NeuStar Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.helplinks;

import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.mgrcore.common.HelpLinks;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

/**
 * This tag returns the help URL
 * based on input screen name
 * and the order type.
 */
public class HelpLinksTag extends VariableTagBase
{
    private String screenName;
    private String orderType;
    private String defaultHelpUrl;

    public int doStartTag() throws JspException
    {
        super.doStartTag();

        try
        {
            String ANY = "any";
            HelpLinks helpLinks = HelpLinks.getInstance ();
            String helpUrl = helpLinks.getHelpURL ( screenName, orderType );
            defaultHelpUrl = helpLinks.getHelpURL ( ANY, ANY );
            /**
             * In case if any help page url is not found
             * for any combination of screen name and
             * order type then default help url will
             * be shown to the user.
             */
            if (!StringUtils.hasValue ( helpUrl ))
            {
                helpUrl = defaultHelpUrl;
            }
            log.debug("HelpLinksTag.doStartTag: Retrieved help url [" + helpUrl +"] for screen name ["+screenName+"] and order type ["+orderType+"]");
            setVarAttribute(helpUrl);
        }
        catch (Exception e )
        {
            e.printStackTrace ();
            String err = "HelpLinksTag : Error occured: " + e.getMessage();
            log.error(err);
            throw new JspTagException(err);
       }
       return SKIP_BODY;
    }

    /**
     * Sets screen name
     * @param screenName
     */
    public void setScreenName ( String screenName ) throws JspException
    {
        this.screenName = (String) TagUtils.getDynamicValue("screenName", screenName, String.class, this, pageContext);
    }

    /**
     * Sets the order type
     * @param orderType
     */
    public void setOrderType ( String orderType ) throws JspException
    {
        this.orderType = (String) TagUtils.getDynamicValue("orderType", orderType, String.class, this, pageContext);
    }
}
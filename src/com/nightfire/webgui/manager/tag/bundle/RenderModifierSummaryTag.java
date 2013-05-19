/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.bundle;

import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.http.HttpServletRequest;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.util.xml.ParsedXPath;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.meta.Field;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.framework.message.common.xml.XMLGenerator;
import  com.nightfire.webgui.manager.beans.ServiceComponentBean;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.manager.beans.InfoBodyBase;
import com.nightfire.webgui.core.tag.navigation.support.MenuLinkElement;
import com.nightfire.webgui.manager.beans.ModifierBean;
import com.nightfire.webgui.core.svcmeta.ActionDef;




/**
 * Renders the html for the displaying the latest modifier. 
 */

public class RenderModifierSummaryTag extends RenderOrderSummaryTag
{   
        
    protected String getJScriptProcessActionEventMethod()
    {
        return "processModifierSummaryActionEvent";
    }


	protected ActionDef getActionDef(String id) 
    {
        return serviceDef.getModifierInfo().getActionDef(id);
    }	

   
 
    protected void createTitleBar(StringBuffer output, StringBuffer jscript, boolean showActions,XMLMessageGenerator xmlmsg) throws JspException
    {
	    
        createTitleBar( output,  jscript, showActions);
      
    }
    
    
    protected void createTitleBar(StringBuffer output, StringBuffer jscript, boolean showActions) throws JspException
    {

        
        String       bundleStatus = ((ModifierBean)bean).getParent().getParentBag().getHeaderValue(STATUS_FIELD);
        
        

        output.append("<!-- Only exists if there is a modifier -->");
        output.append("<!-- a separator between order and modifier-->");
        output.append("<tr style=\"height:20\"><td><img src=\"").append(contextPath).append("/images/shim.gif\" height=\"20\" width=\"1\"/></td></tr>");

        output.append("<tr><td>");
        output.append("<Table border=\"0\" width=\"100%\"><tr>");
        output.append("<td><img src=\"").append(contextPath).append("/images/shim.gif\" height=\"1\" width=\"25\"/></td>");

        output.append("<td>");
        output.append("<Table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">");
              
        output.append("<!--  modifier action bar -->");
        
        output.append("<tr>");
        output.append("<td>");
        output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
        output.append("<tr>");
        output.append("<td class=\"ServiceCaptionBar\">&nbsp;&nbsp;&nbsp;Latest&nbsp;Modifier&nbsp;Summary</td>");
        output.append("<td class=\"ServiceCaptionBar\">&nbsp;</td>");
        output.append("<td class=\"ServiceCaptionBar\" style=\"text-align:right\">");


        output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('");
        output.append(VIEW_ACTION).append("', '").append(bean.getId()).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Modifier Details')\">");
        output.append("<img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(contextPath).append("/images/ServiceActionButton.gif\">View</a>");

        
        if (showActions) {
            output.append("&nbsp&nbsp;");
            createActions(output, jscript, bundleStatus, bean);
        }
        
        output.append("&nbsp&nbsp;&nbsp;&nbsp;");


        

        output.append("</td>");
        output.append("</tr>");
        output.append("</table>");
        output.append("</td>");
        output.append("</tr>");
        output.append("<!-- end modifier action bar -->");
        
    }


    protected void createActionMenu(StringBuffer output, StringBuffer jscript, List actions)
    {
        StringBuffer rootMenu = new StringBuffer("<img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(contextPath).append("/images/ServiceActionButton.gif\"/>Actions");
        MenuLinkElement elem = new MenuLinkElement(rootMenu.toString(), "ServiceActionButton", "ModifierSummary", (Collection)actions);

        output.append(elem.getHTML());        
    }

    protected void createSummaryFields(StringBuffer output, List summaryFields, InfoBodyBase bean, int cellWidth) throws JspException
    {
        
        List      mSummary     = serviceDef.getModifierInfo().getSummaryFields();

        int newWidth = 100/mSummary.size();
        super.createSummaryFields(output, mSummary, bean, newWidth );


      
        output.append("<!-- end  modifer info -->");
        output.append("</table>");
        output.append("</td></tr></table>");
        output.append("</td></tr>");
        output.append("<!-- end modifier info -->");
    }
    


}




/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/manager/tag/navigation/HorizontalLinkBarTag.java#7 $
 */
package com.nightfire.webgui.core.tag.navigation;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import com.nightfire.webgui.core.tag.*;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.webgui.core.DataHolder;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.util.NVPair;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.nightfire.webgui.core.tag.util.ParamContainerTagBase;

/**
 * This is a tag handler class which Builds a horizontal bar with navigation
 * links.
 *
 * usage
 * <gui:HorizontalLinkBar id="siteNav" var="html" >
 *     <gui:param name="Home">
 *       <c:url value="/pages/index.jsp"/>
 *     </gui:param>
 *     <gui:param name="Search">
 *       <c:url value="/pages/search.jsp"/>
 *     </gui:param>
 * </gui:HorizontalLinkBar>
 * or
 * <gui:HorizontalLinkBar id="siteNav" var="html" items="links">
 * </gui:HorizontalLinkBar>
 *
 *
 */

public class HorizontalLinkBarTag extends ParamContainerTagBase implements ServletConstants
{

    /**
     * Parses the drop down list and creates the HTML selection object
     */
    public int doEndTag() throws JspException
    {

      List paramList = this.getParamList();

      List links = getParamList();
      

      if ( (links == null || links.size() == 0))
      {
        throw new JspException ( "HorizontalLinkBarTag: The tag attribute [items] or body Param tag must be specified on the JSP");
      }

        //Create HTML selection list here
        StringBuffer htmlSelectionBuffer = new StringBuffer();

        htmlSelectionBuffer.append("<Table class=\"").append(this.getId()).append("\">");
        
        htmlSelectionBuffer.append("<tr>");
        try
        {
            int itemSize = links.size();
            for ( int i =0; i < itemSize ; i++ )
            {
              NVPair item = (NVPair) links.get(i);
              htmlSelectionBuffer.append("<td><a href=\"javascript:linkBarClicked('");
              htmlSelectionBuffer.append(this.getId()).append("', '").append(item.getName());
              
              htmlSelectionBuffer.append("', '").append(item.getValue()).append("');\" onmouseover=\"return displayStatus('").append(item.getName()).append("');\" onmouseout=\"return displayStatus('');\">");
              
              htmlSelectionBuffer.append(item.getName()).append("</a></td>");

            }

            htmlSelectionBuffer.append("</tr></Table>");

            if  (varName == null) {
              pageContext.getOut().println( htmlSelectionBuffer.toString() );
            }
            else {
              setVarAttribute(htmlSelectionBuffer.toString());
            }

        }
        catch (Exception e)
        {

            if ( e instanceof JspException )
            {
                throw (JspException) e;
            }
            else
            {
              log.error("Encountered processing error:\n" +  e.getMessage() );
              throw new JspException(e.getMessage(), e);
            }
        }

        return EVAL_PAGE;
    }

    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();
    }

}

/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.navigation;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;





/**
 * This tag builds the html navigation bar which represents forms of the request or response.
 * The output html is written to the current JspWriter.
 * This class uses the javascripts objects : FormBar, FormBarItem.
 *
 */
public class FormBarTag extends BarTagBase
{


    /**
     * Redefinition of doStartTag() in TagSupport.  This method processes the
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
       super.doStartTag();

      try {


         HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
         String contextPath = request.getContextPath();


        Message meta =  barGrp.getMetaData();
        List children;

        if (meta == null)
           children = new ArrayList();
        else
           children = (List) meta.getChildren();

        JspWriter html = pageContext.getOut();

        StringBuffer jscript = (StringBuffer) barGrp.getVarJscriptValue();

        String grpId =  barGrp.getId();

        genJScript(grpId, id, children, jscript);


        html.print("<tr>");
        html.print("<td>");

        html.print("<table class=\"FormBar\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"");
        html.print("<TR><TD>");

        html.print("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"\">");
        html.print("<tr valign=\"top\">");
        html.print("<td>");
        html.print("<Table  border=\"0\" cellpadding=\"0\" cellpadding=\"0\">");
        html.print("<TR valign=\"top\">");
        html.print("<TD class=\"FormBarName\">" + name + "</font></TD>");
        html.print("</TR>");
        html.print("</Table>");
        html.print("</td>");
        html.print("<td align=\"left\">");
        html.print("<Table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
        html.print("<TR>");
        html.print("<TD>" );
        html.print("<img src=\"" + contextPath + "/" + TagConstants.IMG_SHIM + "\" width=\"1\" height=\"10\" border=\"0\"></td>");
        html.print("</TD>");
        html.print("</TR>");
        html.print("<TR>");

        Iterator iter = children.iterator();
        while (iter.hasNext() ) {
          Form form = (Form) iter.next();


          String abbrev = form.getAbbreviation();
          String fullName = grpId +"." + id +"." + abbrev;

          String displayName =abbrev;

          if ( form.getRepeatable())
                   displayName += "*";

          String itemLoc = "barItems['" + fullName + "']";
          html.print("<td rowspan=\"2\"><img src=\"" + contextPath + "/" + TagConstants.IMG_SHIM + "\" width=\"10\" height=\"5\" border=\"0\"></td>");
          html.print("<td>");
          html.print("<div id=\""+ fullName + ".deselect\"" + "><table cellpadding=\"0\" cellspacing=\"0\"><tr><td class=\"FormBarItem_deselect\"><a href=\"javascript:" + itemLoc + ".updateParents()\" onMouseOver=\"javascript:" + itemLoc +".showStatus();return true;\" onMouseOut=\"javascript:" + itemLoc +".hideStatus();return true;\">" + displayName + "</A></td></tr></table></div>");
          html.print("<div id=\""+ fullName + ".select\"" + " style=\"display: none\"><table cellpadding=\"0\" cellspacing=\"0\"><tr><td class=\"FormBarItem_select\"><a onMouseOver=\"javascript:" + itemLoc +".showStatus();return true;\" onMouseOut=\"javascript:" + itemLoc +".hideStatus();return true;\">" + displayName + "</A></td></tr></table></div>");
          html.print("</td>");
        }

        html.print("<TR>");
        html.print("</Table>");
        html.print("</TD>");
        html.print("</tr>");
        html.print("</table>");
        html.print("</TD><TR></table></tr>");


      } catch (Exception e)
      {
            String errorMessage = "ERROR: FormBarTag.doAfterBody(): Failed to create page \n" + e.getMessage();

            log.error(errorMessage);
            log.error("",e);
            throw new JspException(errorMessage);
      }


      return SKIP_BODY;

    }

    /**
     * generated need javascript code for this bar
     * @param grpName The name of the grp or its id.
     * @parram id The id for this bar.
     * @param forms, a list of all meta Form objects
     * @param jscript a buffer to place the generated jscritp code.
     */
    private void genJScript(String grpName, String id, List forms, StringBuffer jscript)
    {

       jscript.append("\n<!-- Creating Form Bar " + id + " -->\n");
       jscript.append("var formItemsByMetaId = new Array();\n");
       jscript.append("var " + id + " = new FormBar('" + id + "', null);\n");


       Iterator iter = forms.iterator();

       
       while (iter.hasNext() ) {
          Form form = (Form) iter.next();
          String abbrev = form.getAbbreviation();
          String fullName = grpName +"." + id +"." + abbrev;

          jscript.append("\n<!-- Adding Form Bar Item -->\n");
          // create a new bar item with the abbrievation as the name
          jscript.append(" barItem = new FormBarItem('" + abbrev + "','formItemSelected','" + form.getFullName()+ "');\n" );
          // add the item to the current bar
          jscript.append(" "+ id + ".addChild(barItem);\n" );
          // add a unqiue id to identify this bar in the jscript code.
          jscript.append(" barItems['" + fullName + "'] = barItem;\n");

          jscript.append(" formItemsByMetaId['" + form.getID() + "'] = barItem;\n");
       }

    }


}
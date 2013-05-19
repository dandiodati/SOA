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
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;


/**
 * This tag builds the html navigation bar which represents sections of the request or response.
 * This class uses the javascripts objects : SectionBar, SectionBarRowItem, SectionBarItem
 *
 */
public class SectionBarTag extends BarTagBase
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

        Message meta =  barGrp.getMetaData();
        List children;

        if (meta == null)
           children = new ArrayList();
        else
           children = (List) meta.getChildren();

      try {

        JspWriter html = pageContext.getOut();

        StringBuffer jscript = (StringBuffer) barGrp.getVarJscriptValue();


        String grpId =  barGrp.getId();

        genJScript(grpId, id, children, jscript);

         html.print("<tr>");
         html.print("<td>");

         html.print("<table class=\"SectionBar\" border=\"0\" width=\"100%\">");

         Iterator iter = children.iterator();
         html.print("<tr><td class=\"SectionBarRow\">");

          while (iter.hasNext() ) {
             Form form = (Form) iter.next();
             String formAbbrev = form.getAbbreviation();
             String formFullName = grpId +"." + id +"." + formAbbrev;

             html.print("<DIV style=\"display: none;\" id=\"" + formFullName + "\">");

             Iterator secIter = form.getChildren().iterator();

             while (secIter.hasNext() ) {
                Section sec = (Section)secIter.next();
                String abbrev = sec.getAbbreviation();
                String secFullName = formFullName + "." + abbrev;

                String itemLoc = "barItems['" + secFullName + "']";
                String displayName  = sec.getDisplayName();

                if ( sec.getRepeatable()) {
                   displayName += "*";
                }

                html.print("<a id=\"" + secFullName + "\" href=\"javascript:" + itemLoc +".updateParents();\" onMouseOver=\""+ itemLoc + ".showStatus();return true;\" onMouseOut=\"" + itemLoc +".hideStatus();return true;\">"+ displayName + "</a>");
    
                html.print("&nbsp;&nbsp;&nbsp;&nbsp;");

             }

             html.print("</DIV>");

          }

          html.print("</td></tr>");
          html.print("</table></tr>");

     

      } catch (Exception e)
      {
            String errorMessage = "ERROR: listAvailPagesTag.doAfterBody(): Failed to create page list.\n" + e.getMessage();

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
    private void genJScript(String grpId, String id, List forms, StringBuffer jscript)
    {

       jscript.append("\n<!-- Creating SectionBar  -->\n");
       jscript.append("var secItemsByMetaId = new Array();\n");
       jscript.append("  var " + id + " = new SectionBar('" + id + "', null);\n");

       Iterator iter = forms.iterator();
      
       while (iter.hasNext() ) {
          Form form = (Form) iter.next();
          String formAbbrev = form.getAbbreviation();
          String fullName = grpId +"." + id +"." + formAbbrev;
          jscript.append("\n<!-- Adding Section Bar Row  -->\n");
          jscript.append("    row = new SectionBarRowItem('" + formAbbrev + "',null);\n" );
          jscript.append("    " + id + ".addChild(row);\n" );

          Iterator secIter = form.getChildren().iterator();
          while (secIter.hasNext() ) {
             Section sec = (Section) secIter.next();
             String abbrev = sec.getAbbreviation();
             String secFullName = fullName +"." + abbrev;

             jscript.append("\n<!-- Adding Section Bar Item " + secFullName + " -->\n");
             jscript.append(" secItem = new SectionBarItem('" + abbrev + "','" + sec.getID() +"','sectionItemSelected','" + sec.getFullName()+ "');\n" );
             jscript.append(" row.addChild(secItem);\n" );
             jscript.append(" barItems['" + secFullName + "'] = secItem;\n");

             jscript.append(" secItemsByMetaId['" + sec.getID() + "'] = secItem;\n");
          }

       }



    }


}
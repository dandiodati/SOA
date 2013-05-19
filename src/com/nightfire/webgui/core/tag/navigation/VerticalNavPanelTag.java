/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.navigation;

import java.io.*;
import java.util.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.util.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import javax.servlet.*;


/**
 * This is a tag which creates a vertical navigation panel menu. It can be build for a list of
 * NVPair/NVPairGroup objects or from param/paramlist tags.
 */
public class VerticalNavPanelTag extends ParamContainerTagBase
{

    public static final String CSS_ID_OUTER_PANEL="VertPanelOuter";
    public static final String CSS_ID_INNER_PANEL="VertPanelInner";
    public static final String CSS_CLASS_INNER_PANEL="VertPanelInner";
    public static final String CSS_CLASS_HANDLE="VertPanelHandle";
    public static final String CSS_CLASS_LINK= "VertPanelLink";
    public static final String CSS_CLASS_LINK_OVER="VertPanelLinkOver";
    public static final String CSS_CLASS_LINK_SEL="VertPanelLinkSelected";

    public static final short JSCRIPT_SLIDE_SPEED = 4;
    public static final short JSCRIPT_MOUSE_OUT_WAIT = 100;
    public static final short JSCRIPT_YOFFSET = 150;
    public static final short JSCRIPT_STATICYOFFSET = 30;
    public static final short JSCRIPT_XOFFSET = 0;
    public static final short JSCRIPT_WIDTH = 150; // Must be a multiple of 10!

   /**
    * The jscript function that gets called
    * when a panel is selected
    */
   public static final String JSCRIPT_CALLBACK = "vertNavPanelClicked";


   // a variable to place the generated javascript code
    private String varJscript;

    private String selectedItem;



    protected boolean supportsNVPairGroups()
    {
        return true;
    }



   /**
   *
   */
   public void setSelectedItem(String item) throws JspException
   {
       setDynAttribute("selectedItem", item, String.class);
   }


   /**
     * Sets the name of the variable which will hold the generated javascript code
     * needed to control a tab menu.
     */
    public void setVarJscript(String varName)
    {
       varJscript = varName;
    }




    /**
     * Processing of the end tag.  Build the html and javascript code for a tab menu.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  EVAL_PAGE  This gets returned when there is no exception.
     */
    public int doEndTag() throws JspException
    {

        // normalize the id so it will match a tab id
        selectedItem = normalizeTabId((String)getDynAttribute("selectedItem"));


        List links = getParamList();


       StringBuffer html = new StringBuffer();


       html.append("<DIV ID=\"").append(CSS_ID_OUTER_PANEL).append("\" class=\"").append(getId()).append("\"  style=\"visibility:hidden;Position : Absolute ;Left : 0 ;");
       html.append("Top : ").append(JSCRIPT_YOFFSET).append(";Z-Index : 20;width: ").append(JSCRIPT_WIDTH * 2).append("\">").append(TagConstants.NL);


       html.append("  <DIV ID=\"").append(CSS_ID_INNER_PANEL);
       html.append("\" style=\"left : " ).append(-JSCRIPT_WIDTH);
       html.append("\" class=\"").append(CSS_CLASS_INNER_PANEL).append("\" onmouseover=\"verPanelMoveOut()\" onmouseout=\"verPanelMoveBack()\">").append(TagConstants.NL);
       html.append("    <table border=\"0\" cellpadding=\"0\" cellspacing=\"1\">").append(TagConstants.NL);

       Iterator iter = links.iterator();
       boolean firstRow = true;

       while (iter.hasNext()) {
           NVPair pair = (NVPair)iter.next();

           String id = null;


           if (pair instanceof NVPairGroup)
               id = ((NVPairGroup)pair).getId();
           else
               id = (String) pair.value;



           log.debug("Adding param name[" + pair.name +"], value [" + id +"]");


           html.append("<TR>");


           if(StringUtils.hasValue(selectedItem) && selectedItem.equals(id)) {
               html.append("<TD class=\"").append(CSS_CLASS_LINK_SEL);
           } else {

               html.append("<TD class=\"").append(CSS_CLASS_LINK);

               html.append("\" onmouseout=\"this.className='").append(CSS_CLASS_LINK);
               html.append("';\" onmouseover=\"this.className='").append(CSS_CLASS_LINK_OVER);
               html.append("';\" onClick=\"").append(JSCRIPT_CALLBACK);
               html.append("('").append(id).append("','").append(pair.name).append("','").append(id).append("');");
           }


           html.append("\">").append(TagConstants.NL);

           html.append("<DIV  ALIGN=\"center\">");
           // right now the panel can only be a fixed width of 150,
           //so let a long display name wrap. Don't use TagUtils.escapeHtmlSpaces
           // until we support a wider panel.
           html.append("<FONT>").append(pair.name).append("</FONT>");
           html.append("</DIV>").append(TagConstants.NL);
           html.append("</TD>").append(TagConstants.NL);

           if(firstRow) {
               // create the bar which expands up to 100 menu items.

               html.append("<TD class=\"").append(CSS_CLASS_HANDLE).append("\" rowspan=\"100\"><p><font><B>N<br>a<br>v<br>i<br>g<br>a<br>t<br>i<br>o<br>n<br></B></font></p>");
               html.append("</TD>").append(TagConstants.NL);
               firstRow = false;
           }


           html.append("</tr>").append(TagConstants.NL);
       }


       html.append("</table>");
       html.append("</DIV>");

       html.append("</DIV>");





        try
        {




          if ( varExists() )  {
              setVarAttribute(html.toString());
           } else {
             getPreviousOut().println(html.toString());
           }

          StringBuffer jscript = new StringBuffer();
          createJScript(jscript);

          VariableSupportUtil.setVarObj(varJscript, jscript.toString(), scope, pageContext);

        }
        catch (Exception ex)
        {
            String err ="Failed to build html/jscript code: " + ex.getMessage();
            log.error(err);
            throw new JspException(err);
        }

        super.doEndTag();

        return EVAL_PAGE;
    }


    /**
     * Creates the jscript code needed to control the switching bewtseen tabs.
     * @param tabs A list of TabItem objects.
     * @param jscript The StringBuffer to hold the javascript code.
     */
     private void createJScript(StringBuffer jscript)
    {

        jscript.append("<SCRIPT language=\"JavaScript1.2\">");

        jscript.append("slideSpeed=").append(JSCRIPT_SLIDE_SPEED).append(";");

        jscript.append("waitTime=").append(JSCRIPT_MOUSE_OUT_WAIT).append(";");
        jscript.append("YOffset=").append(JSCRIPT_YOFFSET).append(";");
        jscript.append("XOffset=").append(JSCRIPT_XOFFSET).append(";");
        jscript.append("staticYOffset=").append(JSCRIPT_STATICYOFFSET).append(";");
        jscript.append("menuWidth=").append(JSCRIPT_WIDTH).append(";");

        jscript.append("</SCRIPT>");

        jscript.append("<SCRIPT>setTimeout('initSlide();', 1);</SCRIPT>");

    }



   /**
    * Normalizes a tab id.
    */
    private static final String normalizeTabId(String id)
    {
      if ( !StringUtils.hasValue(id) )
         return id;

      // remove any spaces in id
      id = StringUtils.replaceSubstrings(id.trim(), " ", "");

      return id;
    }





     /**
      * Cleans up resources.
      */
     public void release()
    {
        super.release();

        selectedItem = null;
        varJscript = null;

    }


}

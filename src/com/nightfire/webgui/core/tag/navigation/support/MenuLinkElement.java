/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.navigation.support;

import java.io.*;
import java.util.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.tag.message.support.*;



import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.webgui.core.tag.TagUtils;





/**
 * This object handles creating a menu of links on a html page.
 * It can be used as a widget for other tag objects.
 */
public class MenuLinkElement extends HtmlElement
{

    public static final String CSS_CLASS_MENU="Menu";
    public static final String CSS_CLASS_MENU_LINK="MenuLink";
    public static final String CSS_CLASS_MENU_LINK_OVER="MenuLinkOver";
    public static final String CSS_CLASS_MENU_LINK_TOP="MenuLinkTop";
    

   /**
    * The jscript function that gets called
    * when a panel is selected
    */
    public static final String JSCRIPT_CALLBACK = "menuLinkItemSelected";
    public static final String JSCRIPT_INITFUNC = "initMenuLink";

 
    /**
     * Creates a MenuLinkElement.
     *
     * @param menuRootHtml The html code that gives the root of the menu. This can be some text
     * or some html with some text. It should be very simple such as:
     * - Action Menu
     * - <img src='image.jpg'>Action Menu
     *
     * @param menuRootAnchorClass A css class for the root anchor that wil be created.
     * @param id A unique id for this menu.
     * @param menuPairs This will be a list of NVPairs where the name will be in
     * the display name and the value will be the value that is sent.
     */
    public MenuLinkElement(String menuRootHtml, String menuRootAnchorClass, String id, Collection menuPairs)
   {
       super();
       
      
       html.append("<a class=\"").append(menuRootAnchorClass).append("\" href=\"#\" onmouseover=\"javascript:").append(JSCRIPT_INITFUNC).append("('").append(id).append("');\" onMouseOut=\"javascript:cancelMenuLink('").append(id).append("');\">");

      html.append(menuRootHtml);


      html.append("<div id=\"").append(id).append("\" style=\"position:absolute;left:0px;top:10px;width:55px;height:1px;visibility:hidden;\" onmouseout=\"hideMenuLink(this)\" onmouseover=\"showMenuLink(this);\">");
      html.append("<Table  class=\"").append(CSS_CLASS_MENU).append("\" border=\"0\" cellspacing=\"2\" cellpadding=\"1\">");

      // need an empty cell or else when there is just one item in the menu it jumps around.
      html.append("<tr><td class=\"").append(CSS_CLASS_MENU_LINK_TOP).append("\"></td></tr></tr>");
      
      Iterator iter = menuPairs.iterator();
      while (iter.hasNext() ) {
          NVPair pair = (NVPair) iter.next();
          
          StringBuffer func = new StringBuffer(JSCRIPT_CALLBACK).append("('").append(id).append("','").append(pair.getName()).append("','").append((String)pair.getValue()).append("');");
          html.append("<tr>");
          html.append("<td class=\"").append(CSS_CLASS_MENU_LINK).append("\" onmouseOver=\"this.className='").append(CSS_CLASS_MENU_LINK_OVER).append("'\" onmouseout=\"this.className='").append(CSS_CLASS_MENU_LINK).append("'\" onClick=\"").append(func.toString()).append("\">");
          html.append(TagUtils.escapeHtmlSpaces(pair.getName())).append("</td>");
          html.append("</tr>");
      }
      

      html.append("</table>");
      html.append("</div>");
      html.append("</a>");
      
   }
 
}

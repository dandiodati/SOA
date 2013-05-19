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
 * This is a tag which creates a tab menu. It can build a tab menu from a meta file or from
 * param tags in the body.
 */
public class TabMenuTag extends ParamContainerTagBase
{


   /**
    * The top left corner image of a tab.
    */
   public static final String IMG_TOPLEFT = "images/Tab_TopLeft.gif";

   /**
    * The top right corner image of a tab.
    */
   public static final String IMG_TOPRIGHT = "images/Tab_TopRight.gif";


   /**
    * The MessageContainer to build the tabs from.  The needs to be set by a set method on the tag.
    */
   private MessageContainer msgPart;

   private String selectedTab;

   /**
    * The jscript function that gets called
    * when a tab is selected.
    */
   public static final String JSCRIPT_LISTENER = "tabSelected";

   private String contextPath;

   // a variable to place the generated javascript code
    private String varJscript;

    private String varIndex;
    

    /**
     * Sets the MessageContainer meta part used to build the tab menu.
     *
     * @param msgCont The MessageContainer object.
     *
     */
    public void setMsgPart(Object msgCont) throws JspException
    {
       msgPart = (MessageContainer) TagUtils.getDynamicValue("msgPart",msgCont, MessageContainer.class,this,pageContext);
    }


   /**
   * sets the default tab to be selected.
   * Takes in a string which can be in the form of
   * an index '0', tab name 'blah', or tab name with index 'blah(2)'
   * If the selected tab can not be found or if one is not provided, then the first tab is selected
   * @param tab The expression to select a tab.
   */
   public void setSelectedTab(String tab) throws JspException
   {
      selectedTab = (String)TagUtils.getDynamicValue("selectedTab", tab, String.class, this, pageContext);

      // normalize the id so it will match a tab id
      selectedTab = normalizeTabId(selectedTab);
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
     * And variable indicating the id of the tab
     * that got selected. This selects the default first tab that the user 
     * has permission to view when there is no selected tab passed in.
     * If no tab is found to be selected( this occurs when user has no
     * permission to view any tabs) then a null is returned.
     */
    public void setVarIndex(String varIndex)
    {
        this.varIndex = varIndex;
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
       contextPath = TagUtils.getWebAppContextPath(pageContext);

       StringBuffer html = new StringBuffer();
       StringBuffer jscript = new StringBuffer();

       html.append("<Table class=\""+ TagConstants.CLASS_TAB_MENU + "\" cellpadding=\"0\" cellspacing=\"0\">");
       html.append("<TR>");

       List tabs = null;

       if ( msgPart != null)
          tabs = createMenuFromMeta();
       else if ( this.getParamList().size() > 0 )
          tabs = createMenuFromParams();

       String selectedId =  getSelectedTab(tabs);

       //Set the index in the context.
       VariableSupportUtil.setVarObj(varIndex, selectedId, scope, pageContext);

       createTabHtml(tabs, selectedId, html);

       html.append("</TR></Table>");


         if ( log.isDebugEnabled() )
           log.debug("doAfterBody Finishing html.");

        try
        {
          if ( varExists() )  {
              setVarAttribute(html.toString());
           } else {
             getPreviousOut().println(html.toString());
           }

           createJScript(tabs, jscript);

           VariableSupportUtil.setVarObj(varJscript, jscript.toString(), scope, this.pageContext);

        }
        catch (Exception ex)
        {
            String err = "TabMenuTag: Failed to create jsp page: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }

        return EVAL_PAGE;
    }

    /**
     * Detemines the tab to select by default.
     * Tries to find the tab by the expression set in the selectedTab attribute.
     * The attribute can have the following patterns:
     * tab name - The name of the tab to select.
     * index    - The index of the tab to select, starting at 0.
     * tab name ( index ) - The n tab with the specified tab name.
     *
     * example: Tabs are as follows: Bob, John, Pat, Blah, John, Dan
     *   1 - will selected the first John tab.
     *   Pat - will selected the Pat tab.
     *   John(1) - Will select the second John tab.
     */
    private String getSelectedTab(List tabs)
    {

       int index = -1;
       String selectedId = null;

      // if no tab was found to match the expression in selectedTab, then choose
       // the first tab.
       if (tabs == null || tabs.size() == 0) {
           log.warn("No tabs available for selection");
           return null;
       }

       if ( log.isDebugEnabled() )
          log.debug("Selecting tab [" + selectedId +"]");

       // if the selectedTab attribute has no value use the first tab
       if (!StringUtils.hasValue(selectedTab) )
          return ((TabItem)tabs.get(0)).id;


       // translate the list of tabs into an xml doc
       // to evaluate the selectedTab expression.
       try {
         XMLPlainGenerator xml = new XMLPlainGenerator("Body");

         Iterator iter = tabs.iterator();
         while (iter.hasNext() ) {
            TabItem tab = (TabItem) iter.next();



            log.debug("Setting node [" + tab.id + "] with value [" + tab.id +"]");
            xml.setValue(tab.id, tab.id);
         }

         if (log.isDebugEnabled() )
            log.debug("Using xml to evaluate selectedTab [" + selectedTab +"] :\n" + xml.getOutput());

         // evaluate the selected tab
         if (xml.exists(selectedTab) )
            selectedId = xml.getValue(selectedTab);
       } catch (MessageException m) {
          log.warn("Could not evaluate selectedTab value[" + selectedId +"] :" + m.getMessage());
       }

       // if no tab was found to match the expression in selectedTab, then choose
       // the first tab.
       if ( selectedId == null) {
          log.warn("Could not evaluate selectedTab value [" + selectedId +"]: selecting first tab.");
          selectedId = ((TabItem)tabs.get(0)).id;
       }

       if ( log.isDebugEnabled() )
          log.debug("Selecting tab [" + selectedId +"]");

       return selectedId;
    }


    /**
     * Creates a List of Tab items from the parameters passed in the body of this tag.
     * The name of the parameter is used as the name of the tab, and the value of the
     * parameter is used as the description of the tab.
     * Each name of a tab is used to create a unique id in the form of
     * 'name(instance count)'
     * So if there are two tabs with the same name the first id would be 'name(0)'
     * and the second id would be 'name(1)'
     *
     */
    private List createMenuFromParams()
    {
       List tabs = new ArrayList();

       List children = this.getParamList();

       Iterator iter = children.iterator();

       Map indexes = new HashMap();

       int i = 0;

       while ( iter.hasNext() ) {
          NVPair pair = (NVPair) iter.next();
          Integer index = (Integer) indexes.get(pair.name);
          if (index == null)
            index = new Integer(0);


          // maintain the instance count for each item
          tabs.add(new TabItem(pair.name +"("+ index.intValue() + ")", pair.name, (String)pair.value ) );
          index = new Integer(index.intValue()  + 1);
          indexes.put(pair.name, index);
       }

       return tabs;

    }

    /**
     * Creates a List of Tab items from a meta file. Obtains a list of children
     * of the MessageContainer passed into the msgPart attribute. The name of the tab is
     * obtained from the display name of the child MessagePart, and the description is obtained
     * from the fullname of the child MessagePart.
     * The id of each tab is created by from the id of child  message part.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    private List createMenuFromMeta() throws JspException
    {
       List tabs = new ArrayList();

       List children = msgPart.getChildren();

       for( ListIterator iter = children.listIterator(); iter.hasNext(); )
       {
          MessagePart part = (MessagePart) iter.next();

          // replace # that could be in the id so that it can be used as xml nodes
          // later.
          TabItem item =
                  new TabItem( part.getID(), part.getDisplayName(), part.getFullName() );

          // Only Section and lower have access to the getCustomValue method
          if( part instanceof com.nightfire.webgui.core.meta.Section )
          {
              log.debug("Reading display permission from Section");
              String permission = ( ( Section )part ).getCustomValue("display-permission");
              item.setPermission( permission );

              if( log.isDebugEnabled() )
                  log.debug( "Tab obtained from metadata: " + item );

              // Filtering based on permission.
              try
              {
                  if( item.permission != null &&
                      !ServletUtils.isAuthorized(
                      pageContext.getSession(), item.permission ) )
                      continue;
              }
              catch (ServletException ex)
              {
                  throw new JspException( ex.toString() );
              }
          }

          tabs.add( item );
       }

       return tabs;

    }

    /**
     * Creates the jscript code needed to control the switching bewtseen tabs.
     * @param tabs A list of TabItem objects.
     * @param jscript The StringBuffer to hold the javascript code.
     */
     private void createJScript(List tabs, StringBuffer jscript)
    {

       jscript.append("\n<!-- Creating Tab Menu  " + id + " -->\n");
       jscript.append("var tabs_" + id + "= new Array();\n");
       jscript.append("var " + id + " = new TabMenu('" + id + "', '").append(JSCRIPT_LISTENER).append("');\n");


       Iterator iter = tabs.iterator();


       while (iter.hasNext() ) {
          TabItem tab = (TabItem) iter.next();


          jscript.append("\n<!-- Adding Tab  -->\n");

          jscript.append(" tab = new Tab('").append(tab.id).append("','").append(tab.desr).append("');\n");
          // add the item to the current bar
          jscript.append(" "+ id + ".addChild(tab);\n" );
          // add a full unique id to identify this tab in the jscript code.
          jscript.append(" tabs_" + id + "['" + id + "." + tab.id + "'] = tab;\n");

       }

    }

    /**
     * Creates the html for the tabs.
     * @param tabs A list of TabItems
     * @param selectedId The selected tab id to choose initially.
     * @param html The StringBuffer to hold the html.
     */
    private void createTabHtml(List tabs, String selectedId, StringBuffer html)
    {

       String shimh2 = "<img border=\"0\" src=\"" + contextPath +"/" + TagConstants.IMG_SHIM + "\" width=\"1\" height=\"2\"/>";
       String shimh8 = "<img border=\"0\" src=\"" + contextPath +"/" + TagConstants.IMG_SHIM + "\" width=\"1\" height=\"8\"/>";
       String shimh1 = "<img border=\"0\" src=\"" + contextPath +"/" + TagConstants.IMG_SHIM + "\" width=\"1\" height=\"1\"/>";



       Iterator iter = tabs.iterator();
        while (iter.hasNext() ) {
          boolean selected = false;

          TabItem tab = (TabItem) iter.next();
          //replace all spacing with html non-blocking spaces so that they stay one a single line.
          String name = StringUtils.replaceSubstrings(tab.name.trim(), " ", "&nbsp;");

          if (tab.id.equals(selectedId) )
             selected = true;

          String fullId = id +"." + tab.id;

          String itemLoc = "tabs_" + id +"['" + fullId + "']";


          html.append("<td valign=\"bottom\">");

          html.append("   <div id =\"" + fullId +".deselect\"");
          if ( selected )
             html.append(" style=\"display: none\" ");
          html.append(">");
          html.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
          html.append("<col width=\"8\"><col><col width=\"8\">");
          html.append("<tr>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_TOP_LEFT +"\"><img border=\"0\" src=\"" + contextPath +"/" + IMG_TOPLEFT +"\" width=\"8\" height=\"8\"/></td>");

          html.append("<td class=\""+TagConstants.CLASS_TAB_TOP +"\">").append(shimh8).append("</td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_TOP_RIGHT +"\"><img border=\"0\" src=\"" + contextPath +"/" + IMG_TOPRIGHT +"\" width=\"8\" height=\"8\"/></td>");

          html.append("</tr>");
          html.append("<tr>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_LEFT + "\">").append(shimh1).append("</td>");
          html.append("<td class=\"" +TagConstants.CLASS_TAB_TEXT +"\">").append("<a href=\"javascript:" + itemLoc + ".updateMenu()\" onMouseOver=\"javascript:" + itemLoc +".showStatus();return true;\" onMouseOut=\"javascript:" + itemLoc +".hideStatus();return true;\">").append(name).append("</A></td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_RIGHT + "\">").append(shimh1).append("</td>");
          html.append("</tr>");
          html.append("<tr>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_BOTTOM +"\">").append(shimh2).append("</td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_BOTTOM +"\">").append(shimh2).append("</td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_BOTTOM +"\">").append(shimh2).append("</td>");
          html.append("</tr>");
          html.append("</table>");
          html.append("</div>");

          html.append("<div id =\"" + fullId +".select\"");
          if ( !selected )
             html.append(" style=\"display: none\" ");
          html.append(">");
          html.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
          html.append("<col width=\"8\"><col><col width=\"8\">");
          html.append("<tr>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_TOP_LEFT +"\"><img border=\"0\" src=\"" + contextPath +"/" + IMG_TOPLEFT +"\" width=\"8\" height=\"8\"/></td>");
          html.append("<td class=\""+TagConstants.CLASS_TAB_TOP +"\">").append(shimh8).append("</td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_TOP_RIGHT +"\"><img border=\"0\" src=\"" + contextPath +"/" + IMG_TOPRIGHT +"\" width=\"8\" height=\"8\"/></td>");
          html.append("</tr>");
          html.append("<tr>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_LEFT + "\">").append(shimh1).append("</td>");
          html.append("<td class=\"" +TagConstants.CLASS_TAB_TEXT_SELECTED +"\">").append("<a onMouseOver=\"javascript:" + itemLoc +".showStatus();return true;\" onMouseOut=\"javascript:" + itemLoc +".hideStatus();return true;\">").append(name).append("</A></td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_RIGHT + "\">").append(shimh1).append("</td>");
          html.append("</tr>");
          html.append("<tr>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_BOTTOM_SELECTED +"\">").append(shimh2).append("</td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_BOTTOM_SELECTED +"\">").append(shimh2).append("</td>");
          html.append("<td class=\"" + TagConstants.CLASS_TAB_BOTTOM_SELECTED +"\">").append(shimh2).append("</td>");
          html.append("</tr>");
          html.append("</table>");
          html.append("</div>");

          html.append("</td>");

       }

    }

    /**
     * Class that represents each tab on the tab menu.
     */
    private static class TabItem
    {
       public String id;
       public String name;
       public String desr;
       public String permission;

       public TabItem(String id, String name, String desr)
       {
          this.id = TabMenuTag.normalizeTabId(id);

          this.name = name.trim();
          this.desr = desr.trim();
       }

       public void setPermission( String permission )
       {
           this.permission = permission;
       }

       public String toString()
       {
           return id + ":" + name + ":" + desr + ((permission != null)?":"+permission:"");
       }


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
      // replace # with _ so that any id can be set as a xml attribute value
      id = id.replace('#','_');
      return id;
    }



     /**
      * Cleans up resources.
      */
     public void release()
    {
        super.release();

        msgPart = null;
        selectedTab = null;
        varJscript = null;
    }


}

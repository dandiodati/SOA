/**
 * Copyright (c) 2005 NeuStar, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.navigation;

import java.util.*;

import javax.servlet.jsp.*;
import javax.servlet.ServletException;

import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;

/**
 * This tag creates a stackable navigation tab bar based on the meta file input parameter.
 */
public class TabNavigationTag extends VariableTagBase
{
    private MessageContainer tabContainer;

    private String           tabToSelect;

    private String           varTabCount;

    private int              tabsPerRow = 8;

    private String           varFirstTab;

    private String           suffixValue = "";

    /**
     * Sets the suffix of the search section if passed.
     *
     * @param suffix suffix String
     * @throws JspException  Thrown when an error occurs during processing.
     */
    public void setSuffixValue(String suffix) throws JspException
    {
        this.suffixValue = suffix;
    }

    /**
     * Sets the meta component that represents the container of all the tabs.
     *
     * @param  tabContainer  MessageContainer object.
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setTabContainer(Object tabContainer) throws JspException
    {
        setDynAttribute("tabContainer", tabContainer, MessageContainer.class);
    }

    /**
     * Sets the tab id to select.
     *
     * @param  tabToSelect  Tab id to select.
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setTabToSelect(Object tabToSelect) throws JspException
    {
        setDynAttribute("tabToSelect", tabToSelect, String.class);
    }

    /**
     * Sets the number of tabs to be displayed in a row.
     * This count must be between 4 to 10, default value is 8.
     *
     * @param  tabsPerRow.
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setTabsPerRow (String tabsPerRow) throws JspException
    {
        int val = 0;
        try
        {
            if (StringUtils.hasValue(tabsPerRow))
                val = Integer.parseInt (tabsPerRow);
        }
        catch (NumberFormatException e) {
            val = 0;
        }
        catch (Exception e) {
            val = 0;
        }

        if (val >= 4 && val <= 10)
            this.tabsPerRow = val;
    }

    /**
     * Sets the name of the context attribute used to indicate the number of tabs
     * to be displayed.  This is useful in case there is no tab to display, the
     * page can display an appropriate to the user.
     *
     * @param  varTabCount  Context attribute name to indicate the tab count.
     */
    public void setVarTabCount(String varTabCount)
    {
        this.varTabCount = varTabCount;
    }

    /**
     * Sets the name of the context attribute used to indicate the id of the first
     * tab, of all the tabs to be displayed.  This is useful in the case where a
     * default search form needs to be identified and displayed.
     *
     * @param  varFirstTab  Context attribute name to indicate the first tab id.
     */
    public void setVarFirstTab(String varFirstTab)
    {
        this.varFirstTab = varFirstTab;
    }

    /**
     * Normalize the tab-to-select id so that it can be compare against the tab
     * ids specified in the meta.
     *
     * @param  tabToSelect  Tab id to select.
     * @return  Normalized tab id to select.
     */
    private String normalizeTabToSelect(String tabToSelect)
    {
        if (StringUtils.hasValue(tabToSelect))
        {
            tabToSelect = StringUtils.replaceSubstrings(tabToSelect.trim(), " ", "");

            return tabToSelect.replace('#','_');
        }

        return "";
    }

    /**
     * Redefinition of doStartTag() in VariableTagBase.  This method processes the
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        tabContainer = (MessageContainer)getDynAttribute("tabContainer");

        tabToSelect  = (String)getDynAttribute("tabToSelect");

        tabToSelect  = normalizeTabToSelect(tabToSelect);

        List         children      = tabContainer.getChildren();

        int          childrenCount = children.size();

        int          tabIndex      = 0;

        int          tabsInRow     = 0;

        String       firstTab      = "0.0";

        StringBuffer html          = new StringBuffer();

        html.append("<table class=\"TabTable\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        boolean firstRow = true;
        boolean startTR = true;
        boolean isWSPExists = StringUtils.hasValue(suffixValue);
        TreeMap<Integer, Section> wspTabs = new TreeMap<Integer, Section>();
        TreeMap<Integer, Section> nonwspTabs = new TreeMap<Integer, Section>();
        TreeMap<Integer, Section> finalTabs;

        for (int i = 0; i < childrenCount; i++)
        {
            MessagePart tab = (MessagePart)children.get(i);

            if (tab instanceof Section)
            {
                String tabPermission = ((Section)tab).getCustomValue("display-permission");

                // check the tab id and tab suffix passed, if they match then only show the tab
                String tabSuffix = ((Section)tab).getCustomValue("check-suffix");
                String tabId = tab.getId();
                boolean showTab = true;

                if(!StringUtils.hasValue(tabSuffix))
                {
                    tabSuffix = "";
                }
                showTab = tabSuffix.equalsIgnoreCase(suffixValue) && tabId.endsWith(suffixValue);

                try
                {
                    if (StringUtils.hasValue(tabPermission) && ServletUtils.isAuthorized(pageContext.getSession(), tabPermission))
                    {
                        if (isWSPExists && showTab)
                            wspTabs.put(i, (Section)tab);
                        else if(!StringUtils.hasValue(tabSuffix))
                            nonwspTabs.put(i, (Section)tab);
                    }
                }
                catch (ServletException e)
                {
                    String errorMessage = "Failed to authorize order-type permission via SecurityService.";
                    log.error("doStartTag(): " + errorMessage + "\n" + e.toString());
                    throw new JspException(errorMessage);
                }
            }
        }

        // if isWSPExists then only process the tabs else just consider nonwspTabs
        finalTabs = nonwspTabs;
        if (isWSPExists && nonwspTabs != null && nonwspTabs.size() > 0 && wspTabs != null)
        {
            for (Integer ntabNum : nonwspTabs.keySet())
            {
                Section ntab = nonwspTabs.get(ntabNum);

                String tempId = ntab.getId() + suffixValue;
                boolean found = false;
                for (Integer tabNum : wspTabs.keySet())
                {
                    Section wtab = wspTabs.get(tabNum);
                    if (tempId.equals(wtab.getId()))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    wspTabs.put(ntabNum, nonwspTabs.get(ntabNum));
            }
            finalTabs = wspTabs;
        }

        for (Integer tabNum : finalTabs.keySet())
        {
            Section tab = finalTabs.get(tabNum);

            if (tab.getChildren().size() < 1) continue;
            if (startTR)
            {
                startTR = false;
                html.append("<tr>");
            }

            String tabAppearance = "TabUnselected";

            if ((!StringUtils.hasValue(tabToSelect) && (tabIndex == 0)) || tab.getId().equals(tabToSelect))
            {
                tabAppearance = "TabSelected";
            }

            if (log.isDebugEnabled())
            {
                log.debug("doStartTag(): Adding tab [" + tab.getId() + "] with property [" + tabAppearance + "] ...");
            }

            html.append("<td class=\"").append(tabAppearance).append("\">");

            html.append("<a class=\"").append(tabAppearance);

            html.append("\" href=\"javascript:tabSelected('");

            html.append(tab.getId()).append("')\">");

            html.append(tab.getDisplayName());

            html.append("&nbsp;</a>").append("</td>");

            if (firstTab.equals("0.0"))
            {
                firstTab = tab.getId();
            }

            tabIndex++;
            tabsInRow++;

            if (tabsInRow == tabsPerRow)
            {
                html.append("</tr>");
                startTR = true;
                tabsInRow = 0;
                if (firstRow) firstRow = false;
            }
        }

        if ((tabIndex > 0) && (((tabIndex - 1) % tabsPerRow) < tabsPerRow-1))
        {
            html.append("</tr>");
        }

        html.append("</table>");

        setVarAttribute(html.toString());

        VariableSupportUtil.setVarObj(varTabCount, String.valueOf(tabIndex), scope, pageContext);

        VariableSupportUtil.setVarObj(varFirstTab, firstTab, scope, pageContext);

        return EVAL_PAGE;
    }

    /**
     * Cleans up resources.
     */
     public void release()
     {
         super.release();

         tabContainer = null;

         tabToSelect  = null;

         varTabCount  = null;

         varFirstTab  = null;
     }
}

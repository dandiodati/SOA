/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta.help;

// jdk imports
import java.util.*;

// thirdparty imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

/**
 * Help definition
 */
class HelpDefinition extends LinkInfo
{
    public String guiName      = null;
    public String obfName      = null;
    public String obfAbbr      = null;
    public Node   description  = null;
    public String[] options    = null;
    public String[] optHelp    = null;
    public String[] optDisplay = null;
    public String[] examples   = null;

    /**
     * Constructor
     */
    public HelpDefinition(HelpCreator creator)
    {
        super(creator);
    }

    /**
     * Writes this help definition to a buffer
     */
    public void writeHelp(Node body) throws FrameworkException
    {
        // bookmark the field (empty text forces <a></a> syntax for
        // browsers that may not like <a />)
        Element a = addElem(body, "a", " ");
        a.setAttribute("name", anchor);
        
        // everything goes in a table
        Element table = addElem(body, "table");
        table.setAttribute("class", "HelpField");
        table.setAttribute("width", "100%");
        Element col = addElem(table, "col");
        col.setAttribute("width", "50%");
        col = addElem(table, "col");
        col.setAttribute("width", "50%");

        // field name
        Element row = addElem(table, "tr");
        Element head = addElem(row, "th", obfName);
        head.setAttribute("colspan", "2");

        // obf abbreviation
        if (StringUtils.hasValue(obfAbbr))
        {
            row = addElem(table, "tr");
            Element cell = addElem(row, "td", "Abbreviation:");
            cell.setAttribute("class", "Label");
            cell = addElem(row, "td", obfAbbr);
        }

        // description
        row = addElem(table, "tr");
        Element cell = addElem(row, "td", "Description");
        cell.setAttribute("class", "Label");
        cell.setAttribute("colspan", "2");
        if (description != null)
        {
            row = addElem(table, "tr");
            cell = addElem(row, "td");
            cell.setAttribute("colspan", "2");
            addHelpText(cell, description);
        }

        // options
        if (options != null)
        {
            row = addElem(table, "tr");
            cell = addElem(row, "td");
            cell.setAttribute("colspan", "2");

            Element optTable = addElem(cell, "table");
            optTable.setAttribute("width", "100%");
            optTable.setAttribute("border", "0");
            optTable.setAttribute("class", "HelpField");
            col = addElem(optTable, "col");
            col.setAttribute("width", "30%");
            col = addElem(optTable, "col");
            col.setAttribute("width", "30%");
            col = addElem(optTable, "col");
            col.setAttribute("width", "40%");

            row = addElem(optTable, "tr");
            cell = addElem(row, "td", "Option");
            cell.setAttribute("class", "Label");
            cell = addElem(row, "td", "Value");
            cell.setAttribute("class", "Label");
            cell = addElem(row, "td", "Description");
            cell.setAttribute("class", "Label");

            for (int i = 0; i < options.length; i++)
            {
                row = addElem(optTable, "tr");
                cell = addElem(row, "td", optDisplay[i]);
                cell = addElem(row, "td", options[i]);
                cell = addElem(row, "td", optHelp[i]);
            }
        }

        // examples
        if (examples != null)
        {
            row = addElem(table, "tr");
            cell = addElem(row, "td", "Examples");
            cell.setAttribute("class", "Label");
            cell.setAttribute("colspan", "2");

            for (int i = 0; i < examples.length; i++)
            {
                row = addElem(table, "tr");
                cell = addElem(row, "td", examples[i]);
                cell.setAttribute("colspan", "2");
            }
        }
    }
}

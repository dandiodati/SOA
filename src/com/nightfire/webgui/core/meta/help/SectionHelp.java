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
 * Section help definition
 */
class SectionHelp extends LinkInfo
{
    public String guiName      = null;
    public String obfName      = null;
    public Node   description  = null;
    public ArrayList help      = new ArrayList();


    /**
     * Constructor
     */
    public SectionHelp(HelpCreator creator)
    {
        super(creator);
    }

    /**
     * Writes this help definition to a buffer
     */
    public void writeHelp(Document doc) throws FrameworkException
    {
        // set up the HTML document
        Element head = addElem(doc.getDocumentElement(), "head");
        addElem(head, "title", guiName + " Help");
        Element link = addElem(head, "link");
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", HelpWriter.STYLE_SHEET_NAME);

        // section title
        Element body = addElem(doc.getDocumentElement(), "body");
        addElem(body, "h1", guiName);

        // section help
        if (description != null)
        {
            Element table = addElem(body, "table");
            table.setAttribute("class", "HelpField");
            table.setAttribute("width", "100%");
            Element col = addElem(table, "col");
            col.setAttribute("width", "50%");
            col = addElem(table, "col");
            col.setAttribute("width", "50%");
            Element row = addElem(table, "tr");
            Element cell = addElem(row, "td");
            cell.setAttribute("colspan", "2");
            addHelpText(cell, description);
        }

        // walk the list of fields
        int len = help.size();
        for (int i = 0; i < len; i++)
        {
            HelpDefinition hd = (HelpDefinition)help.get(i);
            hd.writeHelp(body);
        }
    }
}

/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta.help;

// jdk imports
import java.util.*;
import java.io.*;

// thirdparty imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.util.xml.DOMWriter;

/**
 * Form help definition
 */
class FormHelp extends LinkInfo
{
    public String guiName      = null;
    public String obfName      = null;
    public Node   description  = null;
    public ArrayList sections  = new ArrayList();

    /**
     * Constructor
     */
    public FormHelp(HelpCreator creator)
    {
        super(creator);
    }

    /**
     * Writes out the help definitions
     */
    public void writeHelp(File dir)
        throws FrameworkException, IOException
    {
        // create the form HTML
        Document html =
            XMLLibraryPortabilityLayer.getNewDocument("html", null);

        writeLocalHelp(html);
            
        // write it to disk
        String strHtml = DOMWriter.toString(html);
        File f = new File(dir, fileName);
        FileUtils.writeFile(f.getAbsolutePath(), strHtml);

        // walk through each section
        Iterator iter = sections.iterator();
        while (iter.hasNext())
        {
            SectionHelp section = (SectionHelp)iter.next();
            
            // create the html
            html = XMLLibraryPortabilityLayer.getNewDocument("html", null);
            section.writeHelp(html);

            // write the file
            strHtml = DOMWriter.toString(html);
            f = new File(dir, section.fileName);
            FileUtils.writeFile(f.getAbsolutePath(), strHtml);
        }
    }

    /**
     * Create Form help
     */
    private void writeLocalHelp(Document html) throws FrameworkException
    {
        // set up the HTML document

        // head
        Element head = addElem(html.getDocumentElement(), "head");
        addElem(head, "title", guiName + " Help");
        Element link = addElem(head, "link");
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", HelpWriter.STYLE_SHEET_NAME);

        // body
        Element body = addElem(html.getDocumentElement(), "body");

        // form title
        addElem(body, "h1", guiName);

        // form help
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

        // list of sections
        Element table = addElem(body, "table");
        table.setAttribute("class", "HelpField");
        table.setAttribute("width", "100%");
        Element col = addElem(table, "col");
        col.setAttribute("width", "50%");
        col = addElem(table, "col");
        col.setAttribute("width", "50%");
        Element row = addElem(table, "tr");
        Element header = addElem(row, "th", "Sections on this form:");
        header.setAttribute("colspan", "2");

        int len = sections.size();
        for (int i = 0; i < len; i++)
        {
            SectionHelp sh = (SectionHelp)sections.get(i);

            row = addElem(table, "tr");
            Element cell = addElem(row, "td");
            cell.setAttribute("colspan", "2");
            Element anchor = addElem(cell, "a", sh.guiName);
            anchor.setAttribute("href", sh.fileName);
            anchor.setAttribute("onMouseOut", "window.status='';return true;");
            anchor.setAttribute("OnMouseOver", "window.status='" + sh.guiName
                                + "'; return true;");
        }
    }
}

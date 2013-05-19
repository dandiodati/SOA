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
 * Top-level help generation class
 */
class MessageHelp extends LinkInfo
{
    // constants
    /** used in place of the &nbsp; entity */
    private static final String NBSP = " ";

    // member vars
    private ArrayList help;
    private File dir;
    private String helpName;

    /**
     * Constructor
     */
    public MessageHelp(HelpCreator creator, ArrayList help, File dir,
                       String helpName)
    {
        super(creator);

        this.help = help;
        this.dir  = dir;
        this.helpName = helpName;
    }

    /**
     * Writes out the table of contents
     */
    public void writeTOC() throws FrameworkException, IOException
    {
        Document doc = XMLLibraryPortabilityLayer.getNewDocument("html", null);
        Element html = doc.getDocumentElement();

        Element head = addElem(html, "head");
        addElem(head, "title", helpName + " Help Table of Contents");
        Element link = addElem(head, "link");
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", HelpWriter.STYLE_SHEET_NAME);
        // script needs <script></script> syntax instead of <script/>
        Element script = addElem(head, "script", " ");
        script.setAttribute("src", HelpWriter.JSCRIPT_NAME);
        script.setAttribute("language", "JavaScript");

        Element body = addElem(html, "body");
        Element table = addElem(body, "table");
        table.setAttribute("id", "TOC");
        table.setAttribute("class", "TOCTable");
        table.setAttribute("border", "0");
        table.setAttribute("cellspacing", "1");
        table.setAttribute("cellpadding", "1");
        Element col = addElem(table, "col");
        col.setAttribute("width", "30");
        col = addElem(table, "col");
        col.setAttribute("width", "30");

        // walk through each form
        Iterator iter = help.iterator();
        int i = 0;
        while (iter.hasNext())
        {
            FormHelp form = (FormHelp)iter.next();
            
            // create its listing
            Element row = addElem(table, "tr");
            row.setAttribute("class", "TOCChapter");
            Element cell = addElem(row, "td");
            Element a = addElem(cell, "a");
            a.setAttribute("href", "javascript:toggle(" + i + ")");
            a.setAttribute("onMouseOut", "window.status='';return true;");
            a.setAttribute("onMouseOver",
                           "window.status='expand / collapse';return true;");
            Element img = addElem(a, "img");
            img.setAttribute("border", "0");
            img.setAttribute("src", "../images/" + HelpWriter.CLOSED_CHAP_NAME);

            cell = addElem(row, "td");
            cell.setAttribute("colspan", "2");
            a = addElem(cell, "a", form.linkText);
            a.setAttribute("target", HelpWriter.HELP_TARGET);
            a.setAttribute("href", form.fileName);
            a.setAttribute("onMouseOut", "window.status='';return true;");
            a.setAttribute("onMouseOver", "window.status='" + form.guiName
                           + "';return true;");

            // create a listing for each section
            Iterator iter2 = form.sections.iterator();
            while (iter2.hasNext())
            {
                i++;

                SectionHelp section = (SectionHelp)iter2.next();

                row = addElem(table, "tr");
                row.setAttribute("class", "TOCPage");
                cell = addElem(row, "td", NBSP);
                cell = addElem(row, "td");
                img = addElem(cell, "img");
                img.setAttribute("border", "0");
                img.setAttribute("src", "../images/" + HelpWriter.PAGE_NODE_NAME);
                cell = addElem(row, "td");
                a = addElem(cell, "a", section.linkText);
                a.setAttribute("target", HelpWriter.HELP_TARGET);
                a.setAttribute("href", section.fileName);
                a.setAttribute("onMouseOut", "window.status='';return true;");
                a.setAttribute("onMouseOver", "window.status='"
                               + section.guiName + "';return true;");
            }

            i++;
        }

        // write the file
        String xml = DOMWriter.toString(doc);
        File f = new File(dir, "TOC.html");
        FileUtils.writeFile(f.getAbsolutePath(), xml);
    }

    /**
     * Writes out the index
     */
    public void writeIdx(TreeMap idxEntries)
        throws FrameworkException, IOException
    {
        Document doc = XMLLibraryPortabilityLayer.getNewDocument("html", null);
        Element html = doc.getDocumentElement();

        Element head = addElem(html, "head");
        addElem(head, "title", helpName + " Help Index");
        Element link = addElem(head, "link");
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", HelpWriter.STYLE_SHEET_NAME);
        // script needs <script></script> syntax instead of <script/>
        Element script = addElem(head, "script", " ");
        script.setAttribute("src", HelpWriter.JSCRIPT_NAME);
        script.setAttribute("language", "JavaScript");

        Element body = addElem(html, "body");

        // get the index entries
        Iterator keyIter = idxEntries.keySet().iterator();
        while (keyIter.hasNext())
        {   
            String name = (String)keyIter.next();

            // key the list of references for this entry
            ArrayList list = (ArrayList)idxEntries.get(name);
            Iterator iter = list.iterator();

            while (iter.hasNext())
            {
                LinkInfo lnk = (LinkInfo)iter.next();

                String href;
                if (lnk.anchor != null)
                    href = lnk.fileName + "#" + lnk.anchor;
                else
                    href = lnk.fileName;

                Element a = addElem(body, "a", name);
                a.setAttribute("class", "IdxEntry");
                a.setAttribute("href", href);
                a.setAttribute("target", HelpWriter.HELP_TARGET);
                a.setAttribute("onMouseOut", "window.status='';return true;");
                a.setAttribute("onMouseOver", "window.status='" + lnk.linkText
                               + "'; return true;");
            }
        }

        // write the file
        String xml = DOMWriter.toString(doc);
        File f = new File(dir, "HelpIndex.html");
        FileUtils.writeFile(f.getAbsolutePath(), xml);
    }
}

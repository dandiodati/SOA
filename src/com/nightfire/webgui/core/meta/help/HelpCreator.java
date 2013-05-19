/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta.help;

// jdk imports
import java.util.*;
import java.io.*;

// third party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.debug.*;


/**
 * Creates online Help HTML for a Message
 */
public class HelpCreator
{
    // constants

    // member variables

    /** Holds the collection of generated files so links can be checked */
    private HashMap links;
    /** The list of entries for the index */
    private TreeMap idxEntries;
    /** The collection of error messages and warnings */
    private StringBuffer errors;
 
    /** Handles outputting the help files */
    private HelpWriter writer;

    protected DebugLogger log;
  
    // public methods

    /**
     * Constructor
     */
    public HelpCreator()
    {
        writer = new HelpWriter(this);
        log = DebugLogger.getLoggerLastApp(HelpCreator.class);
        
    }

    /**
     * Builds rules for a Message.  If help was successfully created, but
     * it may contain errors (e.g. broken links), a warning message is
     * returned.
     *
     * @param message The Message to build the rules for
     * @param dir     The directory to output the help to
     *
     * @return A string of warning messges, this will be a zero-length
     *         string if no errors or warnings occured.
     *
     * @exception FrameworkException Thrown if the rules cannot be created
     */
    public String createHelp(Message message, File dir)   throws FrameworkException
    {
       return createHelp(message, dir, null);
    }

    /**
     * Builds rules for a Message.  If help was successfully created, but
     * it may contain errors (e.g. broken links), a warning message is
     * returned.
     *
     * @param message The Message to build the rules for
     * @param dir     The directory to output the help to
     * @param sharedContent Static html pages.
     *
     * @return A string of warning messges, this will be a zero-length
     *         string if no errors or warnings occured.
     *
     * @exception FrameworkException Thrown if the rules cannot be created
     */
    public String createHelp(Message message, File dir, File sharedContent)
        throws FrameworkException
    {
        dir.mkdirs();
        // assemble the help definitions
        links = new HashMap();
        idxEntries = new TreeMap();
        errors = new StringBuffer();
        ArrayList help = new ArrayList();

        createHelp(new HashMap(), help, message);

        // build the documents
        try
        {         
            writer.writeHelp(help, dir, sharedContent, message.getFullName(), idxEntries);
        }
        catch (IOException ioex)
        {
            log.error("",ioex);
            throw new FrameworkException(ioex);
        }

        return errors.toString();
    }

    /**
     * Adds an error message to the collection
     */
    public void addError(String err)
    {
        errors.append(err);
    }

    /**
     * Returns the link info for an id
     */
    public Object getLink(String id)
    {
        return links.get(id);
    }


    /**
     * Ensures IDs are suitable for our purposes (valid URL)
     */
    private String normalizeID(String id)
    {
        return id.replace('#', '_');
    }

    /**
     * Visits message parts, creating help
     */
    private void createHelp(HashMap visited, ArrayList help, MessagePart part)
    {
        // see if we've been here before
        if (visited.containsKey(part.getID()))
        {
            // stop here
            return;
        }

        // if this is a form, it starts a new set of files
        if (part instanceof Form)
        {
            FormHelp fHelp = new FormHelp(this);
            help.add(fHelp);

            fHelp.guiName      = part.getFullName();
            fHelp.nfName       = normalizeID(part.getID());
            fHelp.description  = part.getHelpNode();
            fHelp.fileName     = fHelp.nfName + "Help.html";
            fHelp.linkText     = fHelp.guiName;

            links.put(fHelp.nfName, fHelp);
            addToIndex(fHelp.guiName, fHelp);

            createFormHelp(visited, fHelp, part);
            return;
        }

        // otherwise, visit our children
        visited.put(part.getID(), part);
        Iterator iter = ((MessageContainer)part).getChildren().iterator();
        while (iter.hasNext())
        {
            MessagePart child = (MessagePart)iter.next();

            createHelp(visited, help, child);
        }

        // pop the visited node
        visited.remove(part.getID());
    }

    /**
     * Visits forms, creating help
     */
    private void createFormHelp(HashMap visited, FormHelp fh, MessagePart part)
    {
        // see if we've been here before
        if (visited.containsKey(part.getID()))
        {
            // stop here
            return;
        }

        // if this is a section, it starts a new file
        if (part instanceof Section)
        {
            SectionHelp sHelp = new SectionHelp(this);
            fh.sections.add(sHelp);

            // GUI name
            if (StringUtils.hasValue(part.getFullName()))
                sHelp.guiName  = part.getFullName();
            else if (StringUtils.hasValue(part.getDisplayName()))
                sHelp.guiName  = part.getDisplayName();
            else
                sHelp.guiName  = part.getID();

            sHelp.nfName       = normalizeID(part.getID());
            sHelp.description  = part.getHelpNode();
            sHelp.fileName     = sHelp.nfName + "Help.html";
            sHelp.linkText     = sHelp.guiName;

            links.put(sHelp.nfName, sHelp);
            addToIndex(sHelp.guiName, sHelp);

            createSectionHelp(visited, sHelp, part);
            return;
        }

        // otherwise, visit our children
        visited.put(part.getID(), part);
        Iterator iter = ((MessageContainer)part).getChildren().iterator();
        while (iter.hasNext())
        {
            MessagePart child = (MessagePart)iter.next();

            createFormHelp(visited, fh, child);
        }

        // pop the visited node
        visited.remove(part.getID());
    }

    /**
     * Visits sections, creating help
     */
    private void createSectionHelp(HashMap visited, SectionHelp sHelp,
                                   MessagePart part)
    {
        // see if we've been here before
        if (visited.containsKey(part.getID()))
        {
            // stop here
            return;
        }

        // if this is a field, it has actual help
        if (part instanceof Field)
        {
            createFieldHelp(sHelp, (Field)part);
            return;
        }

        // otherwise, visit our children
        visited.put(part.getID(), part);
        Iterator iter = ((MessageContainer)part).getChildren().iterator();
        while (iter.hasNext())
        {
            MessagePart child = (MessagePart)iter.next();

            createSectionHelp(visited, sHelp, child);
        }

        // pop the visited node
        visited.remove(part.getID());
    }

    /**
     * Creates help for a field
     */
    private void createFieldHelp(SectionHelp section, Field field)
    {
        if(TagUtils.isHidden(field) )
        {
            return ;
        }
        // start with the basic information
        HelpDefinition hd = new HelpDefinition(this);
        section.help.add(hd);

        String abbr = field.getAbbreviation();

        hd.fileName     = section.fileName;
        hd.anchor       = normalizeID(field.getID());
        hd.nfName       = hd.anchor;
        
        // GUI name
        if (StringUtils.hasValue(field.getDisplayName()))
            hd.guiName      = field.getDisplayName();
        else
            hd.guiName      = field.getID();

        // Full name
        if (StringUtils.hasValue(field.getFullName()))
            hd.obfName      = field.getFullName();
        else if (StringUtils.hasValue(field.getDisplayName()))
            hd.obfName      = field.getDisplayName();
        else
            hd.obfName      = field.getID();

        hd.obfAbbr      = abbr;
        hd.description  = field.getHelpNode();

        // link text
        hd.linkText     = field.getFullName();
        if (hd.linkText != null)
        {
            if (StringUtils.hasValue(abbr) &&
                (!hd.linkText.equals(abbr)) )
            hd.linkText     = hd.linkText + " ("
                + field.getAbbreviation() + ")";
        }
        else
        {
            if (StringUtils.hasValue(hd.guiName))
                hd.linkText = hd.guiName;
            else if (StringUtils.hasValue(field.getID()))
                hd.linkText = field.getID();
            else
                hd.linkText = "";
        }

        links.put(hd.nfName, hd);

        addToIndex(hd.obfName, hd);
        if (hd.guiName != null)
        {
            if (!hd.guiName.equals(hd.obfName))
                addToIndex(hd.guiName, hd);
        }
        addToIndex(hd.obfAbbr, hd);

        // the rest comes from the DataType
        DataType dt = field.getDataType();
        if (dt == null)
            return;

        OptionSource src = dt.getOptionSource();
        if (src != null)
        {
            hd.options    = src.getOptionValues();
            hd.optDisplay = src.getDisplayValues();
            hd.optHelp    = src.getDescriptions();
        }

        hd.examples   = dt.getExamples();
        if (hd.examples != null)
        {
            if (hd.examples.length == 0)
                hd.examples = null;
        }                
    }

    /**
     * Adds an entry to the index
     *
     * @param str  The value to display in the index
     * @param link The link
     */
    private void addToIndex(String str, LinkInfo link)
    {
        if (str == null)
            return;

        ArrayList list = null;
        if (idxEntries.containsKey(str))
            list = (ArrayList)idxEntries.get(str);
        else
        {
            list = new ArrayList();
            idxEntries.put(str, list);
        }

        list.add(link);
    }
}

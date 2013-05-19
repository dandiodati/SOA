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
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;

import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.debug.*;


/**
 * Outputs online Help HTML for a Message
 */
class HelpWriter
{
    // constants
    public  static final String HELP_TARGET    = "HelpPage";
    public  static final String STYLE_SHEET_NAME = "nfhelp.css";
    public  static final String JSCRIPT_NAME     = "help.js";
    public  static final String CLOSED_CHAP_NAME = "ClosedChapter.gif";
    private static final String OPEN_CHAP_NAME   = "OpenChapter.gif";
    public  static final String PAGE_NODE_NAME   = "Page.gif";

    private static final String[] IMG_FILES =
    {
        CLOSED_CHAP_NAME,
        OPEN_CHAP_NAME,
        PAGE_NODE_NAME,
        "HelpFlameBar.gif",
        "HelpTabs_Contents.gif",
        "HelpTabs_Contents_f2.gif",
        "HelpTabs_Index.gif",
        "HelpTabs_Index_f2.gif",
        "HelpTabs_r1_c3.gif",
        "HelpTitle.gif",
        "shim.gif"
    };

    private static final String[] TEXT_FILES =
    {
        STYLE_SHEET_NAME,
        JSCRIPT_NAME,
        "HelpTabs.html",
        "HelpWelcome.html",
        "LaunchHelp.html",
        "OnlineHelp.html"
    };


    /** The HelpCreator that owns us */
    private HelpCreator creator;

    private DebugLogger log;
  

    /**
     * Constructor
     */
    public HelpWriter(HelpCreator creator)
    {
        this.creator = creator;
        log = creator.log;
        
        
    }

    /**
     * Writes out the help definitions
     *
     */
    public void writeHelp(ArrayList help, File dir, String helpName,
                          TreeMap idx)  throws FrameworkException, IOException
    {
       writeHelp(help, dir, null, helpName, idx);
    }

    /**
     * Writes out the help definitions
     * @param dir The directory to place the generated help pages. All static content
     * is expected to live a directory above this one.
     *
     * @param sharedCommon Common content files that need to be copied. If null then it is skipped.
     */
    public void writeHelp(ArrayList help, File dir, File sharedCommon, String helpName,
                          TreeMap idx)
        throws FrameworkException, IOException
    {
        // walk through each form
        Iterator iter = help.iterator();
        while (iter.hasNext())
        {
            FormHelp form = (FormHelp)iter.next();
            
            // create the pages
            form.writeHelp(dir);
        }

        MessageHelp msgHelp = new MessageHelp(creator, help, dir, helpName);

        // create a table of contents
        msgHelp.writeTOC();

        // create an index
        msgHelp.writeIdx(idx);


        // if shared content exists the copy it over to the help directory
        if (sharedCommon != null)   {
          for (int i = 0; i < TEXT_FILES.length; i++)
            {
               File f = new File(sharedCommon.getAbsolutePath() + "/" + TEXT_FILES[i]);
               File dest = new File(dir.getAbsolutePath() + "/" + TEXT_FILES[i]);
               try {
                 copyFile(f,dest);
               } catch (FrameworkException e) {
                  log.error("Failed to copy help page: " + e.getMessage());
               }

            }

            //Copy the image files.
            for (int i = 0; i < IMG_FILES.length; i++)
            {
               File f = new File ( sharedCommon.getAbsolutePath() + "/images/" + IMG_FILES[i] );
               // Get the parent of the destination where images is to be copied.
               File parentDirFile = new File ( dir.getParent() + "/images/" + IMG_FILES[i] );
               // Check if images folder exist at the parent path.
               if ( ! parentDirFile.exists () )
               {
                    File parentDir = new File ( parentDirFile.getParent () );
                    // If images folder does not exist then create it.
                    parentDir.mkdir ();
               }
               try {
                 // Copy images from source to destination.  
                 copyImageFile ( f, parentDirFile );
               } catch ( FrameworkException e ) {
                  log.error( "Failed to copy help image files : " + e.getMessage() );
               }

            }
        }
    }
    
    // copies a file if the dest does not exist
    private void copyFile (File src, File dest) throws FrameworkException
    {
       if (src.exists() &&  !dest.exists() ) {
          String copy = FileUtils.readFile(src.getAbsolutePath());
          FileUtils.writeFile(dest.getAbsolutePath(), copy, false);
       }
    }

    // copies a binary file if the dest does not exist
    private void copyImageFile (File src, File dest) throws FrameworkException
    {
       if ( src.exists () &&  ! dest.exists () ) {
          byte[] copy = FileUtils.readBinaryFile (src.getAbsolutePath () );
          FileUtils.writeBinaryFile (dest.getAbsolutePath (), copy, false );
       }
    }

}

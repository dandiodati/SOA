/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports

// third party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.FrameworkException;

/**
 * OptionSource is an interface for obtaining a valid list of options,
 * their display values, and their help text.  It allows the data to be
 * obtained from a variety of sources without all of those sources needing to
 * be suppored directly by DataType.
 */
public interface OptionSource
{
    /**
     * Returns the list of option values from this source.
     *
     * @return An array of option text values
     */
    public String[] getOptionValues();

    /**
     * Returns the list of display values associated with the options.  The
     * length of the list returned must match the length of the list returned
     * by getOptionValues().
     *
     * @return An array of option display values
     */
    public String[] getDisplayValues();

    /**
     * Returns the list of descriptions (help text) associated with the
     * options.  The length of the list returned must match the length of the
     * list returned by getOptionValues().
     *
     * @return An array of option descriptions
     */
    public String[] getDescriptions();

    /**
     * Read this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, Message msg) throws FrameworkException;
}

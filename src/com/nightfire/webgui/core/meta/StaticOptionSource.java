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
 * StaticOptionSource is an OptionSource whose options are an unchanging
 * list specified directly in the XML meta data.
 */
public class StaticOptionSource implements OptionSource
{
    // constants

    // member variables

    /**
     * Option values
     */
    protected String[] optionValues = null;

    /**
     * Display values
     */
    protected String[] displayValues = null;

    /**
     * Descriptions
     */
    protected String[] descriptions = null;

    // public methods

    /**
     * Default constructor
     */
    public StaticOptionSource()
    {
    }

    /**
     * Returns the list of option values from this source.
     *
     * @return An array of option text values
     */
    public String[] getOptionValues()
    {
        return optionValues;
    }

    /**
     * Returns the list of display values associated with the options.  The
     * length of the list returned must match the length of the list returned
     * by getOptionValues().
     *
     * @return An array of option display values
     */
    public String[] getDisplayValues()
    {
        return displayValues;
    }

    /**
     * Returns the list of descriptions (help text) associated with the
     * options.  The length of the list returned must match the length of the
     * list returned by getOptionValues().
     *
     * @return An array of option descriptions
     */
    public String[] getDescriptions()
    {
        return descriptions;
    }

    /**
     * Read this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, Message msg) throws FrameworkException
    {
        MessagePaths xpaths = msg.getXPaths();

        optionValues  = xpaths.optionValuesPath.getValues(ctx);
        displayValues = xpaths.displayValuesPath.getValues(ctx);
        descriptions  = xpaths.descriptionsPath.getValues(ctx);

        if ( (optionValues.length != displayValues.length) ||
             (optionValues.length != descriptions.length) )
            throw new FrameworkException("Count of option values ("
                + optionValues.length + "), display values ("
                + displayValues.length + "), and descriptions ("
                + descriptions.length + ") must match.");
    }

    // private and protected methods

    // inner classes
}

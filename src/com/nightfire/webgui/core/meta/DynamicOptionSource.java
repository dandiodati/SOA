/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// third party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.FrameworkException;

/**
 * DynamicOptionSource is an OptionSource whose options can be changed either
 * by directly setting String[] of option values, display values and description or
 * by reading from XML meta data.
 */
public class DynamicOptionSource extends StaticOptionSource
{
    /**
     * Default constructor
     */
    public DynamicOptionSource()
    {
        super();
    }


    /**
     * sets the list of option values, display values and descriptions.
     */
    public void setOptionData(String[] optionVals, String[] displayVals, String[] descs) throws FrameworkException
    {
        if ( (optionVals.length != displayVals.length) ||
             (optionVals.length != descs.length) 
        )
        throw new FrameworkException("Count of option values ("
            + optionVals.length + "), display values ("
            + displayVals.length + "), and descriptions ("
            + descs.length + ") must match.");

        optionValues = optionVals;
        displayValues = displayVals;
        descriptions = descs;
    }

 
}

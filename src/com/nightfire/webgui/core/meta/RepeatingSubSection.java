/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.FrameworkException;

/**
 * RepeatingSubSection is a collection of Fields which may have multiple
 * instances appearing on a single section (e.g. a line item on a
 * purchase order). RepeatingSubSections may only occur within a
 * {@link Section}.
 * <p>
 * RepeatingSubSections contain {@link Field Fields}.
 */
public class RepeatingSubSection extends MessageContainer
{
    /**
     * Default constructor
     */
    public RepeatingSubSection()
    {
    }
}

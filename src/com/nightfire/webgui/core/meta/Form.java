/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports

/**
 * Form is a grouping of sections.  It is the first layer below a message.  A
 * Form is essentially presented to the user as a menu containing the sections
 * within the form.
 * <p>
 * A Form contains one or more {@link Section Sections}.
 */
public class Form extends MessageContainer
{
    /**
     * Default constructor
     */
    public Form()
    {
        super();
    }
}

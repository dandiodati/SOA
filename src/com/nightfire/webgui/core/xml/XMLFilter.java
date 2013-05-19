/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.xml;

import org.w3c.dom.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;



/**
 * This interface represents a class that can filter
 * values from an xml message.
 *
 */
public interface XMLFilter
{


    /**
     * Apply filtering to the xml string.
     * @param The xml message.
     * @return The filtered xml string.
     * @exception MessageException is throw if there is an error processing the xml.
     */
    public String filter(String xml) throws MessageException;


    /**
     * Apply filtering to the xml document.
     * @param The xml message.
     * @return The filtered xml document.
     * @exception MessageException is throw if there is an error processing the xml.
     */
    public Document filter(Document xml) throws MessageException;


    /**
     * Apply filtering to the xml generator.
     * @param The xml message.
     * @return The filtered xml generator.
     * @exception MessageException is throw if there is an error processing the xml.
     */
    public XMLGenerator filter(XMLGenerator xml) throws MessageException;
}

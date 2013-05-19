/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.message.support;

/**
 * Defines an object which handles the layout of HtmlContainers and HtmlElements.
 */
public interface HtmlElementLayout
{

   /**
    * Formats an HtmlContainer into the current layout.
    * @param container - The container holding all of the html elements and subContainers.
    * @return Html code with the current layout applied.
    *
    * NOTE: The HtmlContainer object contains a {@link HtmlElement#describe() describe} method which lists the structure
    * and the contents of each HtmlElement which can be useful for building a custom layout class.
    */
   public String doLayout(HtmlContainer container);


}

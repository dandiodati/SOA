/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/ResourceChangeListener.java#1 $
 */

package com.nightfire.webgui.core.resource;


/**
 * This interface must be implemented by any classes that wish to be notified of
 * changes to resources utilized by the system.
 */

public interface ResourceChangeListener
{
    /**
     * A convenience function which gets called during registration.  It normally
     * contains initialization code.
     */
    public void register();
    
    /**
     * This function gets called when a resource has changed.
     *
     * @param  event  A ResourceChangeEvent object which describes the event and
     *                the resource which has changed.
     */
    public void resourceChange(ResourceChangeEvent event);
    
    /**
     * A convenience function which gets called during deregistration.  It normally
     * contains cleanup code.
     */
    public void deregister();
}
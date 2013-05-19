/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/ResourceChangeEvent.java#1 $
 */

package com.nightfire.webgui.core.resource;


/**
 * A "ResourceChange" event gets delivered whenever a resource under monitor has
 * been changed.  A ResourceChangeEvent object is sent as an argument to the
 * PropertyChangeListener's resourceChange() method.
 */

public class ResourceChangeEvent
{
    private String resourceName;
    
    private String resourceType;
    
    private Object oldValue;
    
    private Object newValue;
    
    
    /**
     * Constructor.
     *
     * @param  resourceName  Name or ID of the resource.
     * @param  resourceType  Type of the resource.
     * @param  newValue      The old resource object.
     * @param  oldValue      The new resource object.
     */
    public ResourceChangeEvent(String resourceName, String resourceType, Object newValue, Object oldValue)
    {
        this.resourceName = resourceName;
        
        this.resourceType = resourceType;
        
        this.oldValue     = oldValue;
        
        this.newValue     = newValue;
    }
    
    /**
     * Getter method for the resource name.
     *
     * @return  Name or ID of the resource.
     */
    public String getResourceName()
    {
        return resourceName;
    }
    
    /**
     * Getter method for the resource type.
     *
     * @return  Type of the resource.
     */
    public String getResourceType()
    {
        return resourceType;   
    }
    
    /**
     * Getter method for the old resource object.
     *
     * @return  The old resource object.
     */
    public Object getOldValue()
    {
        return oldValue;   
    }
    
    /**
     * Getter method for the new resource object.
     *
     * @return  The new resource object.
     */
    public Object getNewValue()
    {
        return newValue;
    }
}
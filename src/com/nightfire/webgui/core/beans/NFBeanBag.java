/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.beans;

import  javax.servlet.ServletException;
import com.nightfire.framework.message.MessageException;

import  com.nightfire.framework.constants.PlatformConstants;

import java.util.*;


/**
 * Represents a class where the body is a set of beans with ids.
 * Some NFBean body operations may not be supported.
 *
 */
 
public interface NFBeanBag extends NFBean
{

    /**
     * Returns the name of the BeanBag.
     */
    public String getName();


    /**
     * Obtain the bean with the specified id.
     * If the bean does not exist a null is returned.
     * @param id - The id of the bean to obtain.
     * @return Returns a NFBean instance or null if a bean does not exist.
     */
    public NFBean getBean(String id);


     /**
     * Indicates if this bean exists in the bean bag
     * @param id The bean id to test for.
     * @return true if the bean with the specified id exists otherwise false.
     */
    public boolean exists(String id);


    /**
     * Adds a bean to this BeanBag with the following id.
     * @param id The key to add the bean as.
     * @param bean The NFBean to add to this BeanBag.
     * @return Returns the bean which previously existed at this id, or null if none existed.
     */
    public NFBean addBean(String id, NFBean bean);


    /**
     * Remove the bean with the specified id.
     *
     * @param id - The id of the bean to remove.
     * @return Returns the bean which was removed or null if not bean existed.
     */
    public NFBean removeBean(String id);


     /**
     * Returns the beans within this bean bag as a Map object
     * @return Map of NFBeans
     */
    public Map getBodyAsMap();

    /**
     * Describes the beans within this bean bag.
     * @param includeBeanData - if true prints out the data within each bean, otherwise just the bean ids.
     * @return String description of this bean bag.
     */
    public String describeBeans(boolean includeBeanData);


    /**
     * Removes all beans within this bean bag.
     */
    public void clear();
}
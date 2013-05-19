/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  javax.servlet.ServletException;
import  javax.servlet.http.HttpSession;


/**
 * <p><strong>UserAuthenticator</strong> is an interface which needs to be 
 * implemented by classes responsible for authenticate user name/password 
 * pair, via internal or external lookup.</p>
 */

public interface UserAuthenticator
{
    /**
     * This method retrieves the role of a user, if his/her account exists.
     *
     * @param  userName  User-name.
     * @param  password  Password.
     * @param  session   HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  User role.  A null is returned if authentication fails.
     */
    public String getRole(String userName, String password, HttpSession session) throws ServletException;
    
    /**
     * This method checks whether the user has authorization to perform the specified
     * action.
     *
     * @param  action    The action to be performed.
     * @param  userName  User-name.
     * @param  session   HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  True if the user is authorized, false otherwise.
     */
    public boolean authenticate(String action, String userName, HttpSession session) throws ServletException;
}
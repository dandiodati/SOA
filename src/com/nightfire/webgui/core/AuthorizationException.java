/*
 * AuthorizationException.java
 */

package com.nightfire.webgui.core;


import javax.servlet.ServletException;

public class AuthorizationException extends javax.servlet.ServletException 
{
    
    /** Creates a new instance of AuthorizationException */
    public AuthorizationException() 
    {
        super();
    }
    
    public AuthorizationException(String message)
    {
        super(message);
    }
    
    
    public AuthorizationException(String message, Throwable rootCause)
    {
        super(message, rootCause);
    }
    
}

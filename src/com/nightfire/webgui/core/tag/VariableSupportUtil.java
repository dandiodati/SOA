/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;
import  com.nightfire.framework.util.*;
import com.nightfire.framework.debug.*;




/**
 * tag utility which provides support for retrieving and setting variables for tags.
 */
 
public class VariableSupportUtil
{
    /**
     * indicates a JSP page scope location
     */
    public static final String PAG_SCOPE = "page";

    /**
     * indicates a JSP request scope location
     */
    public static final String REQ_SCOPE = "request";

    /**
     * indicates a JSP session scope location
     */
    public static final String SES_SCOPE = "session";

    /**
     * indicates a JSP application scope location
     */
    public static final String APP_SCOPE = "application";


    /**
     * sets a variable at the specified location.
     *
     * @param varName The variable name to set. (if null method returns null).
     * @param value The value of the variable.
     * @param scope The scope of the variable (if null defaults to PAG_SCOPE)
     * @param pageContext The current JSP Page {@link javax.servlet.jsp.PageContext}
     *
     */
    public static void setVarObj(String varName, Object value, String scope, PageContext pageContext)
    {
       String webAppName = TagUtils.getWebAppContextPath(pageContext);
       DebugLogger log = DebugLogger.getLogger(webAppName, VariableSupportUtil.class);
   
       if (StringUtils.hasValue(varName) ) {
          int scopeInt = getScopeType(scope);

          Object lock = null;
          if ( scopeInt == PageContext.APPLICATION_SCOPE )
            lock = pageContext.getServletContext();
          else if (scopeInt == PageContext.SESSION_SCOPE)
             lock = pageContext.getSession();

          if (value != null) {
            //log.debug(  "Setting attribute [" + varName + "] of type [" + StringUtils.getClassName(value) + "] at scope [" + scope +"]");

            if ( lock != null)
               synchronized (lock) {
                  pageContext.setAttribute(varName, value, scopeInt);
               }
            else
               pageContext.setAttribute(varName, value, scopeInt);
          } else {
             log.debug(  "Removing attribute [" + varName + "] of type [" + StringUtils.getClassName(value) + "] at scope [" + scope +"]");
             if ( lock != null)
               synchronized (lock) {
                  pageContext.removeAttribute(varName,scopeInt);
               }
             else
               pageContext.removeAttribute(varName, scopeInt);
          }
       }
    }


    /**
     * gets a value from specified variable.
     *
     * @param varName The variable name to get. (if null method returns null).
     * @param scope The scope of the variable (if null defaults to PAG_SCOPE)
     * @param pageContext The current JSP Page {@link javax.servlet.jsp.PageContext}
     * @return Object The value of the variable, or null of not found.
     *
     */
    public static Object getVarObj(String varName, String scope, PageContext pageContext)
    {
       String webAppName = TagUtils.getWebAppContextPath(pageContext);
       DebugLogger log = DebugLogger.getLogger(webAppName, VariableSupportUtil.class);
       if (StringUtils.hasValue(varName) ) {
          int scopeInt = getScopeType(scope);
          Object attr = null;

          if ( scopeInt == PageContext.APPLICATION_SCOPE ) {
             synchronized ( pageContext.getServletContext() ) {
                attr = pageContext.getAttribute(varName, scopeInt);
             }
          } else if ( scopeInt == PageContext.SESSION_SCOPE ) {
             synchronized ( pageContext.getSession() ) {
                 attr = pageContext.getAttribute(varName, scopeInt);
             }
          } else
              attr =  pageContext.getAttribute(varName, scopeInt);

          log.debug("Returning attribute [" + varName + "] of type [" + StringUtils.getClassName(attr) + "] at scope [" + scope +"]");

          return attr;
       } else
          return null;
    }

    /**
     * converts the string scope into the numeric representation
     * for the pageContext object. 
     */
    private static int getScopeType(String scope ) {

        int attrScope = PageContext.PAGE_SCOPE;

        if (REQ_SCOPE.equals(scope) )
           attrScope = PageContext.REQUEST_SCOPE;
        else if (SES_SCOPE.equals(scope) )
           attrScope = PageContext.SESSION_SCOPE;
        else if (APP_SCOPE.equals(scope) )
           attrScope = PageContext.APPLICATION_SCOPE;

        return attrScope;
    }


}

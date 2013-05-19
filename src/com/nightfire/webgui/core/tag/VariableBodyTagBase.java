/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;
import  com.nightfire.framework.util.*;



/**
 * Base class to provide set methods for var and scope attributes, and provides simplified
 * methods for retriving/setting a output variable indicated by the var attribute at any scope.
 * Used for BodyTagSupport classes. 
 */
public abstract class VariableBodyTagBase extends NFBodyTagSupport
{

    /**
     * the name of the variable to set
     */
    protected String varName;

    /**
     * the scope to set the variable at.
     */
    protected String scope;


    /**
     * sets the name of the variable which holds the results of this tag.
     * @varName -name of variable.
     */
    public void setVar(String varName)
    {
        this.varName = varName;
    }



     /**
     * sets the the scope of the variable. Possible values are:
     * scope, request, session, application. Refer to JSTL documentation for more information.
     * @scope -scope value.
     * If no scope is provided it defaults to {@link javax.servlet.jsp.PageContext.PAGE_SCOPE}
     */
    public void setScope(String scope)
    {
        this.scope = scope;
    }

    /**
     * indicates if the var attribute was set and has a value.
     * @return true if the var attribute has a value otherwise false.
     */
    public boolean varExists() {
       return (StringUtils.hasValue(varName) );

    }

    /**
     * creates a variable defined by the setVar method and at the scope defined
     * by the setScope method with the value of value.
     *
     * @param value The value of the variable to set.
     */
    protected void setVarAttribute(Object value) {
       VariableSupportUtil.setVarObj(varName, value, scope, pageContext);
    }


    /**
     * gets a value from the variable defined by the setVar method and at the scope defined
     * by the setScope method.
     */
    public Object getVarAttribute() {
       return VariableSupportUtil.getVarObj(varName, scope, pageContext);
    }


     /**
     * Free resources after the tag evaluation is complete
     */
    public void release()
    {
        super.release();

        varName  = null;
        scope    = null;
    }


}

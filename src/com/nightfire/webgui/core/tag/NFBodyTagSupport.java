/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;
import  com.nightfire.framework.util.*;

import com.nightfire.webgui.core.*;
import com.nightfire.framework.debug.*;
import java.util.Properties;
import java.util.HashMap;


/**
 * Base class which all body tags need to extend. Provides common
 * configuration support, such as logging configuration.
 * Each tag must call the setup method in there doStartTag method, if 
 * they overload the doStartTag method.
 */

public abstract class NFBodyTagSupport extends BodyTagSupport
{

   
    /**
     * A logger for the current child class.
     *
     */
    protected DebugLogger log;

    /**
     * Web application properties for use by child classes.
     *
     */
    protected Properties props;


    // map used to store attribute expressions for later evaluation.
    private HashMap attrMap = new HashMap();
 
  /**
   * Starts processing of the tag.
   * @return EVAL_BODY_BUFFERED is returned so that the body is NOT evaluated.
   */
  public int doStartTag() throws JspException
  {
    setup();
    
    reset();
    
    return EVAL_BODY_BUFFERED;
  }
  
  public void setup() 
  {
    String webAppName = TagUtils.getWebAppContextPath(pageContext); 
    log = DebugLogger.getLogger(webAppName, getClass());

    props = (Properties)pageContext.getServletContext().getAttribute(ServletConstants.CONTEXT_PARAMS);
      
      if (props == null) {
           log.error("Web app properties not found, creating empty properties");
           props = new Properties();
       }
  
  }
  
    /**
     * Allows child classes to perform any initialization tasks as required.
     */
    public void reset()
    {
    }


    public int doEndTag() throws JspException
    {
        reset();
        return EVAL_PAGE;
    
    }
 

    /**
     * Sets an attribute and value for dynamic evaluation.
     * This must be called in setter methods, and later the getDynAttribute must
     * be called to obtain the value.
     * 
     * 
     * @param attrName The name of the attribute.
     * @param object The attribute expression or could be the actual value.
     * @param type The expected type for the attribute value.
     * @exception JspException if an error occurs
     */
    public void setDynAttribute(String attrName, Object object, Class type) throws JspException
    {
        TagUtils.setDynAttribute(attrMap, attrName, object,  type);
    }


    /**
     * Returns a dynamic value for a attribute. A attribute may evaluate to a runtime statement
     * or expression language statement. This method must always be executed in the 
     * the doStartTag() method.
     *
     * <b>NOTE: Any values that are strings, get evaulated as expression language.
     * So if a runtime expression evalulates to a String which contains a '${' it
     * will get evaluated again as an expression language statement. This rare case should never occur
     * and will not be an issue once JSP supports both runtime expressions and expresion language.</b>
     *
     * @param attrName The name of the attribute.
     * @return Returns the object that is evaluated via expression language.
     * The object will always be of type expectedType and can be null.
     *
     * @exception JspException if the expectedType does not match or if an value contains an invalid expression language statement.
     */
    public Object getDynAttribute(String attrName) throws JspException
    {
        return TagUtils.getDynAttribute(attrMap, attrName, this, pageContext);
    }


    
     /**
     * Free resources after the tag evaluation is complete.
     * Child classes must call super.release();
     */
    public void release()
    {
        super.release();
        attrMap.clear();   
    }

}

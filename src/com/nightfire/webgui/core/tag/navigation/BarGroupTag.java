/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.navigation;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;


/**
 * This tag builds a set of navigation bars which allows navigation on the request or response.
 * Requires the meta file. This class generates both html and the needed javascript code
 * to control the navigation bar. It expects the the BarGroup javascript object to
 * be defined.
 *
 */
public class BarGroupTag extends VariableBodyTagBase
{



    // a variable to place the generated javascript code
    private String varJscript;

    // Meta object root node.
    private Message metaObj;

    // string buffer to hold the javascript code
    private StringBuffer jscriptBody;

     // string buffer to hold the javascript init code
    private String varJscriptInit;
     // string buffer to hold the javascript init code
    private String jscriptInit;

    // a list of all inner bars that are in this group
    private List barList;


    /**
     * Set the meta data Message object
     * @param message The expression which evaluates to a Message object.
     */
    public void setMeta(Object meta) throws JspException
    {
       metaObj = (Message) TagUtils.getDynamicValue("meta", meta, Message.class, this, pageContext);
    }


    /**
     * Creates a BarGroupTag.
     */
    public BarGroupTag()
    {
       barList = new ArrayList();
       jscriptBody = new StringBuffer();

    }

    /**
     * Returns the Meta data object, which can be used to obtain information on this message.
     * Used by inner tags.
     * @return The meta data object.
     */
    public Message getMetaData() {
       return metaObj;
    }

    /**
     * Returns the StringBuffer which is holding the main javscript code.
     * Used by inner tags.
     * @return The javscript string buffer.
     */
    public StringBuffer getVarJscriptValue() {
       return jscriptBody;
    }

    /**
     * Returns a list that each inner Bar tag needs to add itself to.
     * This class has no way of finding out about its child classes, so
     * the children are responsible for letting this tag know about them.
     * @return A list of all inner Bar tags.
     */
    public List getBarList() {
       return barList;
    }


    /**
     * Sets the name of the variable which will hold the generated javascript code
     * needed to modify the forms on the fly.
     */
    public void setVarJscript(String varName)
    {
       varJscript = varName;
    }

     /**
     * Sets the name of the variable which will hold the initialization javascript code
     * needed to modify the forms on the fly( this should be used in a HTML's body onLoad event).
     */
    public void setVarJscriptInit(String varName)
    {
       varJscriptInit = varName;
    }

     /**
     * Redefinition of doInitBody() in BodyTagSupport.  This method processes the
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     */
    public void doInitBody() throws JspException
    {


         try {

            if ( metaObj == null) {
               log.error("Meta Data Object not found");
               throw new JspTagException("Meta Data Object not found");
            }
            
            jscriptInit =  "javascript:" + id + ".initialize()";

            bodyContent.print("<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
            bodyContent.print("<td class=\"" + TagConstants.CLASS_LEFT_PADDING + "\"></td>");

            bodyContent.print("<td><Table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");

         }  catch (Exception e ) {
            String err = "BarGroupTag : Could not write to body content :" + e.getMessage();
            log.error(err);
            log.error("",e);
            throw new JspTagException(err);

       }

       jscriptBody.append("\n<!-- Creating BarGroup  -->\n");
       jscriptBody.append("var barItems = new Array();\n");
       jscriptBody.append("var " + id + " = new BarGroup('" + id + "');\n");


    }


    /**
     * Redefinition of doAfterBody() in BodyTagSupport.  This method processes the
     * start tag for this instance.
     * On completion the generated html and javascript from the inner tags are writen
     * to the variables defined by the attributes 'var' and 'varJscript'. If var is
     * not defined then the html is written to the current JspWriter.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doAfterBody() throws JspException
    {

        try {

           bodyContent.print("</Table></td></tr></table>");


           if ( varExists() )  {
              String html = bodyContent.getString();
              setVarAttribute(html);
           } else {
              bodyContent.writeOut(this.getPreviousOut());
           }


           genJscript(jscriptBody);


           VariableSupportUtil.setVarObj(varJscript, jscriptBody.toString(), scope, this.pageContext);
           VariableSupportUtil.setVarObj(varJscriptInit, jscriptInit, scope, this.pageContext);


        } catch (Exception e)
        {
            String errorMessage = "ERROR: BarGroupTag.doAfterBody(): Failed to create bar group.\n" + e.getMessage();

            log.error(errorMessage);
            log.error("",e);
            throw new JspException(errorMessage);
        }
        
         return this.SKIP_BODY;
    }
    
    /**
     * Overrides parent's reset to perform initialization tasks, mostly to
     * commodate tag reuse feature of the servlet container.
     */
    public void reset()
    {
        barList.clear();
        
        jscriptBody = new StringBuffer();
    }

    /**
     * generates the javscript code to control this Bar group
     */
    private void genJscript(StringBuffer jscriptBody)
    {

        Iterator iter = barList.iterator();
        jscriptBody.append("\n<!-- Adding child bars to BarGroup  -->\n");
        while (iter.hasNext() ) {

          jscriptBody.append( id + ".addChild(" + (String)iter.next() + ");\n");
        }
    }




    public void release()
    {
        super.release();
        barList.clear();
        jscriptBody = new StringBuffer();
        metaObj = null;
        varJscript = null;
        varJscriptInit = null;

    }
}
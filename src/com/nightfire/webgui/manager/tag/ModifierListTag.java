/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import org.w3c.dom.*;

import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.http.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.util.xml.ParsedXPath;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.meta.*;
import  com.nightfire.webgui.core.svcmeta.*;
import  com.nightfire.webgui.manager.svcmeta.*;

import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.webgui.manager.beans.ServiceComponentBean;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.framework.message.MessageException;
import javax.servlet.*;


/**
 * Builds the html to display the modifier list page.
 */

public class ModifierListTag extends VariableTagBase
{
    private static KeyTypeLookup parsedXPathLookup            = new KeyTypeLookup();

    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    private AliasDescriptor aliasDescriptor;

    private ComponentDef def = null;

    private Object xmlData = null;

    private String contextPath;



    /**
     * Setter method to set the component defintion which describes modifiers for this
     * listing.
     *
     * @param bool A boolean value
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setComponentDef(String compDef) throws JspException
    {

        def  = (ComponentDef)TagUtils.getDynamicValue("componentDef", compDef, ComponentDef.class, this, pageContext);

    }

    /**
     * Setter method to pass in the xml data which contains the list of modifiers.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setData(String data) throws JspException
    {

        xmlData  = (Object)TagUtils.getDynamicValue("data", data, Object.class, this, pageContext);

    }



    /**
     * Redefinition of doStartTag() in VariableTagBase.  This method processes the
     * start tag for this instance.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

       contextPath =  TagUtils.getWebAppContextPath(pageContext);

        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);

        ModifierInfo mi = def.getModifierInfo();

        if(mi == null) {
            String err = "No Modifier information available for component [" + def.getID() +"] in bundle def [" + def.getBundleDef().getID()+"]";
            log.error(err);
            throw new JspTagException(err);
        }


        List summaryFields = mi.getSummaryFields();
        List actions       = mi.getActionDefs();

        XMLGenerator xmlResponseData = null;

        StringBuffer output = new StringBuffer();

        try {

        if ( xmlData != null) {
              if ( xmlData instanceof XMLGenerator)
                  xmlResponseData = (XMLGenerator)xmlData;
              else if (xmlData instanceof String )
                  xmlResponseData = new XMLPlainGenerator((String)xmlData);
              else if (xmlData instanceof Document)
                  xmlResponseData = new XMLPlainGenerator((Document)xmlData);
        }
        else {
            log.error("data property is null");
            throw new JspTagException("data property is null");
        }



        output.append("<table cellpadding='0' cellspacing='0' border='0' width='100%'>");

        output.append("<tr>").append("<td>");

        output.append("<table cellpadding='0' cellspacing='0' border='0' width='100%'>");

        output.append("<tr>");

        output.append("<td class='ServiceCaptionBar'>").append(def.getDisplayName());
        output.append("</td>");

        output.append("<td class='ServiceCaptionBar'>&nbsp;</td>");

        output.append("<td class='ServiceCaptionBar' style='width:50%;text-align:right'>");

        // Always display a View button.
        
        output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('view-order', '/pages/modifiers/modifier-view-detail-redirect.jsp')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Modifier Details')\"><img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif").append("\">").append("View").append("</a>&nbsp&nbsp;");

        Iterator aIter = actions.iterator();
        while (aIter.hasNext() ) {
            ActionDef def = (ActionDef)aIter.next();


            try
            {
                if ( ServletUtils.isAuthorized(pageContext.getSession(), def.getActionName() ) )
                {
                    String redirectPage = def.getRedirectPage();
                    
                    output.append("<a class='ServiceActionButton' href=\"javascript:performServiceAction('");
                    output.append(def.getActionName());
                    output.append("', '");
                    
                    if (StringUtils.hasValue(redirectPage))
                    {
                        output.append(redirectPage);
                    }
                    
                    output.append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('");
                    output.append(def.getFullName()).append("')\"><img class='ServiceActionButton' border='0' src='");
                    output.append(contextPath +"/" +"/images/ServiceActionButton.gif").append("'>");

                    output.append(def.getDisplayName());

                    output.append("</a>&nbsp;&nbsp;");
                }
            }
            catch (ServletException ex)
            {
                String errMsg =
                        new StringBuffer( "Failed to verify authorization for action "
                        ).append( def.getActionName() ).append( "]\n"
                        ).append( ex.getMessage() ).toString();
                log.error( errMsg );
                throw new JspTagException( errMsg );
            }
        }


            output.append("</td>");
            output.append("</tr>");
            output.append("</table>");
            output.append("</td>");
            output.append("</tr>");
			output.append("<tr>");
            output.append("<td>");
            output.append("<table cellpadding='0' cellspacing='0' border='0' width='100%'>");
            output.append("<tr>");
            output.append("<td class='ServiceSummaryFirstColumn'></td>");
            output.append("<td>");
            output.append("<table cellpadding='0' cellspacing='0' border='1' width='100%'>");
            output.append("<tr>");
            output.append("<th class='ServiceSummary'>&nbsp;</th>");

            Iterator sIter = summaryFields.iterator();

            while (sIter.hasNext() ) {
                Field sf = (Field)sIter.next();

                output.append("<th class='ServiceSummary' style='width:15%'>");
                output.append(sf.getDisplayName());
                output.append("</th>");
            }

            output.append("</tr>");
            output.append("<form name='modifierListForm'>");

            Node[] results = xmlResponseData.getChildren("DataContainer");

            for(int i =0; i < results.length; i++ ) {

                output.append("<tr>");
                output.append("<td class='ServiceSummary'>");

               output.append("<input ");
               if (results.length == 1)
                   output.append("checked='true' ");
               output.append("type='radio' name='modifier' value='&NFH_BOID=");

               output.append(getValue(xmlResponseData,results[i],"BOID", false));
               output.append("&NFH_MetaDataName=");
               output.append(getValue(xmlResponseData,results[i],"MetaDataName", false));
               output.append("&NFH_SCID=");
               output.append(pageContext.getRequest().getParameter("NFH_SCID"));
               output.append("'>");
               output.append("</td>");


                sIter = summaryFields.iterator();
                while (sIter.hasNext() ) {
                    Field sf = (Field)sIter.next();

                    output.append("<td class='ServiceSummary'>");
                    output.append(getValue(xmlResponseData,results[i],sf, true));
                    output.append("</td>");
                }
                output.append("</tr>");
            }

            output.append("</form>");
            output.append("</table>");
            output.append("</td>");
            output.append("</tr>");
            output.append("</table>");
            output.append("</td>");
            output.append("</tr>");
            output.append("</table>");


        }
        catch (MessageException e) {
            log.error("Error parsing xml :  " + e.getMessage());
            throw new JspTagException("Error while parsing results : " + e.getMessage());
        }

        try
        {
            if (varExists())
               setVarAttribute(output.toString());
            else
               pageContext.getOut().print(output.toString());
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: doStartTag(): Failed to write to JspWriter output-stream.\n" + e.getMessage();

            log.error(errorMessage);

            throw new JspTagException(errorMessage);
        }

        return SKIP_BODY;
    }

    private String getValue(XMLGenerator gen , Node parent, String path, boolean useAlias)
    {
        String temp = null;

        try {

            if (gen.exists(parent, path)) {

                temp = gen.getValue(parent, path);
                if(useAlias)
                    temp = aliasDescriptor.getAlias(pageContext.getRequest(), path, temp, true);
            }


        }
        catch (MessageException e) {
            log.error("Could not obtain value of xml node [" + path +"], returning empty string");
        }

        return convertSpace(temp);


    }

    private String convertSpace(String value)
    {
        if(StringUtils.hasValue(value))
            return value;
        else
            return "&nbsp;";


    }



    private String getValue(XMLGenerator gen, Node parent, Field f, boolean useAlias)
    {

        String xPath             = f.getCustomValue("XPath");

        String path = null;

        String value = null;


        if(!StringUtils.hasValue(xPath) ) {
            return getValue(gen, parent, f.getXMLPath(), useAlias);
        }
        else {
            String      serviceType = def.getID();

            ParsedXPath parsedXPath = (ParsedXPath)parsedXPathLookup.get(serviceType, xPath);

            try
            {

                if (parsedXPath == null)
                {
                    synchronized (parsedXPathLookup)
                    {
                        parsedXPath = (ParsedXPath)parsedXPathLookup.get(serviceType, xPath);

                        if (parsedXPath == null)
                        {
                            parsedXPath = new WebParsedXPath(xPath, gen.getGroup());
                            parsedXPathLookup.put(serviceType, xPath, parsedXPath);
                        }
                    }
                }


                // try to get the value specified in the xpath custom value
                if( parsedXPath.valueExists(parent)) {
                    value = parsedXPath.getValue(parent);
                    if(useAlias)
                        value = aliasDescriptor.getAlias(pageContext.getRequest(), f.getXMLPath(), value, true);
                }

            }
            catch (Exception e)
            {
                log.error("Failed to obtain the value for field [" + f.getID() + "], Trying field name as xml path instead:\n" + e.getMessage());
            }


            // if no value was found then try to use the path of the
            // field
            //
            if( !StringUtils.hasValue(value))
                return getValue(gen, parent, f.getXMLPath(), useAlias);
            else
                return convertSpace(value);
        }



    }





    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();

        aliasDescriptor = null;

        def = null;

        xmlData = null;

        contextPath = null;

    }
}

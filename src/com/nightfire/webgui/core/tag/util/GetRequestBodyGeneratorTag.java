/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/core/tag/util/GetRequestBodyGeneratorTag.java
 */
package com.nightfire.webgui.core.tag.util;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;

import com.nightfire.framework.util.*;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *  <p><strong>GetRequestBodyGenerator</strong> is the tag handler which gathers
 * all request parameters and put them in a xml generator to be created and
 * stored in the associated var.
 *
 *
 * The attributes used in the tag are path, and var
 *
 *
 * <p>Sample Usage:
 *
 * <nf:getRequestBodyGenerator var="bodyGen" path="x.y.z" />
 *
 */

public class GetRequestBodyGeneratorTag extends VariableTagBase implements ServletConstants, PlatformConstants
{
    /**
     * the objects to hold the xml request content.
     */
    private String path;

    /**
     * the name of tag attribute
     */
    private static String XML_PATH_ATTRIBUTE = "path";

    /**
     * Sets the xml node name that will be created in the XMLGenerator - set on the tag attribute on jsp.
     * The following xml documents are supported: String
     * @param  nodeName  nodeName used as input.
     */
    public void setPath (Object path ) throws JspException
    {
        this.path = (String) TagUtils.getDynamicValue( XML_PATH_ATTRIBUTE , path , Object.class, this, pageContext);
    }

    /**
     * Starts procesing of this tag.
     * This method constructs an XML Generator containing the node name and value
     * given as attributes to the tab.
     */
    public int doStartTag() throws JspException

    {
        super.doStartTag();

        //Variables to hold the values passed through the tag attributes
        XMLPlainGenerator gen = null;

        try
        {

          log.debug("Creating the xml generator ...");

          gen = new XMLPlainGenerator("Body");

          Enumeration params = pageContext.getRequest().getParameterNames();

          log.debug("Populating the xml generator ...");

           while ( params.hasMoreElements() )
           {
               String paramName = (String) params.nextElement();

               if ( paramName.startsWith( "NF_" ) )
               {
                   String nodeName = paramName.substring(3);

                   if (StringUtils.hasValue(path))
                   {
                       nodeName = ((new StringBuffer(path)).append(".").append(nodeName)).toString();
                   }

                   String nodeValue = pageContext.getRequest().getParameter(paramName);

                   gen.setValue(nodeName, nodeValue);

                   log.debug("doStartTag(): Added [" + nodeName + ", " + nodeValue + "] to the XMLGenerator instance.");
               }
           }

           setVarAttribute(gen);

           return SKIP_BODY;
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: GetRequestBodyGeneratorTag.doStartTag(): Encountered processing error:\n" + e.getMessage();

            log.error( errorMessage, e);

            throw new JspException(errorMessage);
        }
    }

    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();

        path  = null;
    }

}

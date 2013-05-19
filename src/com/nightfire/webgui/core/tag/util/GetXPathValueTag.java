/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.*;

import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.util.xml.ParsedXPath;

import  java.util.*;

import  javax.servlet.*;
import javax.servlet.http.*;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;
import java.net.*;
import java.io.*;

import org.w3c.dom.*;


/**
 * This tag is responsible for getting an XPath @value from an xml data.
 *
 */
public class GetXPathValueTag extends VariableTagBase
{


   /**
    * The XPath containg @value.
    */
	private String path;

   /**
    * The xml document to look for the XPath value.
    */
    private Object xml;

     /**
     * Sets the XPath to use. This will usually be a String variable.
     */
    public void setPath(String path) throws JspException
    {
       this.path = (String) TagUtils.getDynamicValue("path", path, String.class, this, pageContext);
    }

     /**
     * Sets the xml document to use. This will usually be a variable.
     * The following xml documents are supported: String, XMLMessageParser, Document.
     * @param  xml  xml used as input.
     */
    public void setXml(Object xml) throws JspException
    {
        this.xml = TagUtils.getDynamicValue("xml", xml, Object.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     * This method evaluates the xml variable and gets xpath value.
     */
    public int doStartTag() throws JspException
    {
      	super.doStartTag();

      	if (!StringUtils.hasValue(path) || (xml == null))
      	{
			String errorMessage = "Tag attributes 'path' and 'xml' must have a valid non-null value.";

			log.error("doStartTag(): " + errorMessage);

			throw new JspException(errorMessage);
		}

		Document xmlDoc = null;

		if (xml instanceof String)
		{
			try
			{
				XMLGenerator generator = new XMLPlainGenerator((String)xml);

				xmlDoc                 = generator.getDocument();
			}
			catch (Exception e)
			{
				String errorMessage = "Failed to create an XMLPlainGenerator instance from the specified 'xml' value:\n" + e.getMessage();

				log.error("doStartTag(): " + errorMessage);

				throw new JspException(errorMessage);
			}
		}
		else if (xml instanceof XMLGenerator)
		{
			xmlDoc = ((XMLGenerator)xml).getDocument();
		}
		else if (xml instanceof Document)
		{
			xmlDoc = (Document)xml;
		}
		else
		{
			String errorMessage = "Attribute 'xml' must be of type String, XMLGenerator, or Document.";

			log.error("doStartTag(): " + errorMessage);

			throw new JspException(errorMessage);
		}

       	try
       	{
			ParsedXPath xpath           = new ParsedXPath(path);

          	String[]    candidateValues = xpath.getValues(xmlDoc);

			String      xpathValue      = null;

          	if (candidateValues.length > 0)
			{
				xpathValue = candidateValues[0];

				if (candidateValues.length > 1)
				{
					log.warn("Matching " + candidateValues.length + " XPath values for [" + path + "].");
				}

			}

			if (this.varExists() )
			{
				this.setVarAttribute(xpathValue);
			}
			else
			{
				// When the attribute "var" isn't specified, output the value.
				pageContext.getOut().write(xpathValue);
			}

       	}
       	catch (Exception e)
       	{
			String errorMessage = "Failed to obtain the xpath value from the specified xml document:\n" + e.getMessage();

			log.error("doStartTag(): " + errorMessage);

			throw new JspException(errorMessage);
      	}

       return SKIP_BODY;
    }



    public void release()
    {
       super.release();

       path = null;
       xml = null;
    }

}

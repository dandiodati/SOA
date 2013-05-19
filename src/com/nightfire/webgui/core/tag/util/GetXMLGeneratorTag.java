/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/core/tag/util/GetXMLGeneratorTag.java
 */
package com.nightfire.webgui.core.tag.util;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *  <p><strong>GetXMLGeneratorTag</strong> is the tag handler which allows for a
 * xml generator to be created and stored in the associated var.
 *
 *
 * The attributes used in the tag on the JSP nodeName, nodeValue, generator, var
 * and rootNode.
 *
 *
 * <p>Sample Usage:
 *
 * <nf:getXMLGenerator var="headerGen" nodeName="Action" nodeValue="queuy-order" rootNode="Header" />
 *
 */

public class GetXMLGeneratorTag extends VariableTagBase implements ServletConstants, PlatformConstants
{
    /**
     * the objects to hold the xml request content.
     */
    private String nodeName;
    private String nodeValue;
    private String rootNode;
    private String svcHandlerAction;
    private Object generator;

    /**
     * the name of tag attribute
     */
    private static String XML_NODE_NAME_ATTRIBUTE = "nodeName";
    private static String XML_NODE_VALUE_ATTRIBUTE = "nodeValue";
    private static String XML_GENERATOR_ATTRIBUTE = "generator";
    private static String XML_ROOT_NODE = "rootNode";

    /**
     * Sets the xml node name that will be created in the XMLGenerator - set on the tag attribute on jsp.
     * The following xml documents are supported: String
     * @param  nodeName  nodeName used as input.
     */
    public void setNodeName (Object nodeName ) throws JspException
    {
        this.nodeName = (String) TagUtils.getDynamicValue( XML_NODE_NAME_ATTRIBUTE , nodeName , Object.class, this, pageContext);
    }

    /**
     * Sets the xml node value that will be created in the XMLGenerator - set on the tag attribute on jsp.
     * The following xml documents are supported: String
     * @param  nodeValue  nodeValue used as input.
     */
    public void setNodeValue (Object nodeValue ) throws JspException
    {
        this.nodeValue = (String) TagUtils.getDynamicValue( XML_NODE_VALUE_ATTRIBUTE , nodeValue , Object.class, this, pageContext);
    }

    /**
     * Sets the svcHandlerAction value that will be created in the XMLGenerator - set on the tag attribute on jsp.
     * The following xml documents are supported: String
     * @param  svcHandlerAction  svcHandlerAction used as input.
     */
    public void setSvcHandlerAction (Object svcHandlerAction ) throws JspException
    {
        this.svcHandlerAction = (String) TagUtils.getDynamicValue( XML_NODE_NAME_ATTRIBUTE , svcHandlerAction , String.class, this, pageContext);
    }

     /**
     * Sets the XMLPlainGenerator where the node will be created - set on the tag attribute on jsp.
     * The following xml documents are supported: XMLGenerator
     * @param  gen  gen used as input.
     */
    public void setGenerator (Object generator ) throws JspException
    {
        this.generator = TagUtils.getDynamicValue( XML_GENERATOR_ATTRIBUTE , generator , Object.class, this, pageContext);
    }

    /**
     * Sets the xml root node that will be used to create the XMLGenerator - set on the tag attribute on jsp.
     * The following xml documents are supported: String
     * @param  nodeValue  nodeValue used as input.
     */
    public void setRootNode (Object rootNode ) throws JspException
    {
       this.rootNode = (String) TagUtils.getDynamicValue( XML_ROOT_NODE , rootNode , Object.class, this, pageContext);
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

        //Make sure required attributes have values
        if ( nodeName==null || nodeValue==null )
        {
            //Throws JSP Exception if requried values are NULL
            throw new JspException ( "ERROR: No value is present for the required node value or node name attributes." );
        }


        try
        {

            if ( generator!=null )
           {
               gen = (XMLPlainGenerator) generator;
           }
           else
           {
               if ( rootNode!=null )
               {
                   log.debug("Creating the xml generator ...");
                   gen = new XMLPlainGenerator(rootNode);
               }
               else
               {
                   //Throws JSP Exception if requried values are NULL
                   throw new JspException ( "ERROR: rootNode is a required field when there is no generator." );
               }
           }

           if (log.isDebugEnabled())
           {
               log.debug("doStartTag(): Setting [" + nodeName + ", " + nodeValue + "] on the generator ...");
           }

            /*
                If the value of svcHandlerAction var is "true" then 
                the cross check against the actions present in svcHandlerActions.xml is bypassed
             */
            if (StringUtils.hasValue(svcHandlerAction))
            {
                if (gen.exists("svcHandlerAction"))
                {
                   gen.setValue("svcHandlerAction", svcHandlerAction);
                }
                else
                {
                   gen.create("svcHandlerAction");
                   gen.setValue("svcHandlerAction", svcHandlerAction);
                }
            }
           gen.setValue(nodeName, nodeValue);
        }
        catch (MessageException me)
        {
            String errorMessage = "ERROR: GetXMLGeneratorTag.doStartTag(): Encountered an error while setting"
                                    + " the node value:\n" + me.getMessage();

            log.error( errorMessage, me );

            throw new JspException(errorMessage);
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: GetXMLGeneratorTag.doStartTag(): Encountered processing error:\n" + e.getMessage();

            log.error( errorMessage, e);

            throw new JspException(errorMessage);
        }



        //Set the returned response object if the "var" attribute is specified in the tag
        if ( varExists() )
        {
          setVarAttribute( gen ) ;
        }


        return SKIP_BODY;

    }

    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();

        nodeName  = null;
        nodeValue  = null;
        generator = null;
        rootNode = null;
    }

}

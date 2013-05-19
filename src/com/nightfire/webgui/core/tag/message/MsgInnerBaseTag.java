/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;


import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.*;

import com.nightfire.framework.constants.*;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;

import com.nightfire.framework.message.common.xml.*;

/**
 * Base tag for all tags that live within a MessageTag.
 */
public abstract class MsgInnerBaseTag extends VariableTagBase
{


    private MessageTag msgTag;

    private XMLGenerator messageData;

    private boolean isReadOnly;

    /**
     * The current webapp context path.
     */
    protected String contextPath;

    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    protected AliasDescriptor aliasDescriptor;

    /**
     * The path to the location of the help pages, relative to the current webapp context.
     */
    protected String helpDir;

   
    /**
     * Called when a new tag is encountered after the attributes have been
     * Child classes need to call super.doStartTag first before any processing
     * if they overload the doStartTag.
     *
     * Initializes resources for sub classes.
     *
     * @return SKIP_BODY So by default it does not process the body of the tag.
     *
     * If a child class needs to access its body , then it should overload the
     * doStartTag, call super.doStartTag first and then
     * return the correct code for body evaluation.
     *
     * @see javax.servlet.jsp.tagext.Tag
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();
      

        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);

        try
        {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            contextPath = request.getContextPath();


           msgTag = (MessageTag)findAncestorWithClass(this, MessageTag.class);

           //if (log.isDebugEnabled())
           //   log.debug(" Getting ancestor tag of type [" + msgTag.getClass().getName() + "]");


           isReadOnly = msgTag.isMsgReadOnly();
           messageData = (XMLGenerator) msgTag.getMessageData();

           Properties webappProps = (Properties)pageContext.getAttribute(ServletConstants.CONTEXT_PARAMS, pageContext.APPLICATION_SCOPE);
           helpDir = PropUtils.getPropertyValue(webappProps,ServletConstants.HELP_DIRECTORY, "help");
           
           //if (log.isDebugEnabled())
           //   log.debug(" Getting message data of type [" + messageData.getClass().getName() + "]");


           return SKIP_BODY;
        }
        catch (Exception e)
        {
            String err = StringUtils.getClassName(this) + " This class needs to have an ancestor MessageTag. " + e.getMessage();
            log.error(err);
            log.error("",e);
            throw new JspException(err);
        }
    }


    /**
     * Returns the current message data object
     * @return The xml message object.
     */
    protected XMLGenerator getMessageData()
    {
       return messageData;
    }

    /**
     * Returns the root MessageTag ancestor object.
     * @return MessageTag which is the top most tag.
     */
    protected MessageTag getMessageTag()
    {
       return msgTag;
    }


    /**
     * indicates if this message should be read onll.
     * @return true for read only or false for read write.
     */
    protected boolean isReadOnly()
    {
       return isReadOnly;
    }

    /*
     * adds an index to the end of an xml path for a new node to be created.
     * This does not create the new node in the message object it just returns
     * and xml path to the location of a new node.
     * assumes that we always create nodes at index 1 and greater, since
     * index 0 will always be used for display.
     * Examples:
     *  1. if xmlPath is lsr.admincontainer.admin and there are no admin nodes
     *   the this will return lsr.admincontainer.admin(1). (creating location 0 and 1)
     *  2.  if xmlPath is lsr.admincontainer.admin and there is 1 admin node
     *   the this will return lsr.admincontainer.admin(1). (creating location 1).
     *  3.  if xmlPath is lsr.admincontainer.admin and there are 3 admin nodes
     *   the this will return lsr.admincontainer.admin(4). (creating location 4).
     * @param message - The message xml to check in.
     * @param xmlPath - The path to the new node that needs to be added(without an index).
     * @return The xml path to create a new at the location specified.
     */
    protected String addNewXmlPathIndex(XMLGenerator message, String xmlPath)
    {

       try {

               // to add new nodes find out how many nodes currently exist.
               // then add another one. If none exist add two new nodes,
               //  one for the blank displayed one , and another new one.
               // This prevents the user from have to click add twice.
               //
               // next add a new index on the xml path
               int count = 1;
               if ( message.exists(xmlPath) ) {
                  Node n = message.getNode(xmlPath);
                  Node p = n.getParentNode();
                  count = message.getChildCount(p, n.getNodeName());
               }
               xmlPath = xmlPath +"(" + count + ")";
           } catch (MessageException e) {
              log.error(StringUtils.getClassName(this) + ": Could not create xml path for adding a node, new nodes won't be added: " +e.getMessage());

           }

       return xmlPath;


    }

    /**
     * Escapes special characters for use in an HTML attribute
     *
     * @param elem The buffer to output to
     * @param val  The value to escape
     *
     */
    public void escapeAttr(HtmlElement elem, String val)
    {
        if (!StringUtils.hasValue(val))
            return;

        elem.append( TagUtils.escapeAttr(val) );

    }

    /**
     * Escapes special characters for use in an HTML attribute
     *
     * @param sb The buffer to output to
     * @param val  The value to escape
     *
     */
    public void escapeAttrStr(StringBuffer sb, String val)
    {
        if (!StringUtils.hasValue(val))
            return;

        sb.append( TagUtils.escapeAttr(val) );
    }
}

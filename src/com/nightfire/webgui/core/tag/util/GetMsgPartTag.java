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
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;

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
 * Utility class which retrieves a specific MessagePart from
 * a meta Message object. Used by the Message building tags.
 * @see com.nightfire.webgui.core.tag.message
 */
public class GetMsgPartTag extends VariableTagBase
{

    /**
     * The current meta data to use
     */
    private Message meta;

    /**
     * The default MessagePart id.
     */
    private String defaultId;


    /**
     * Set the meta data object for this request.
     *
     */
    public void setId(String id)
    {
       //setId is already defined in parent class
       try {
          this.id = (String) TagUtils.getDynamicValue("id",id, String.class, this, pageContext);
       } catch (JspException e) {
          log.warn("Failed to get dynamic id, setting id to null: " + e.getMessage());
          this.id = null;
       }

    }

    /**
     * Set the meta data object for this request.
     *
     */
    public void setMeta(Object meta) throws JspException
    {
       this.meta = (Message) TagUtils.getDynamicValue("meta", meta, Message.class, this, pageContext);
    }


    /**
     * Set the default id for this MessagePart
     * @param id The id of default MessagePart.
     */
    public void setDefaultId(String defaultId) throws JspException
    {
         this.defaultId = (String)TagUtils.getDynamicValue("defaultId" , defaultId, String.class, this, pageContext);
    }

    /**
     * Gets the message part with the specified id.
     * If id does not have a value then defaultId is used.
     * It then sets the message part as a variable(var).
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();


       String curId;

       if (StringUtils.hasValue(id) ) {
          curId = id;
          log.debug("Getting part with id [" + curId +"]");
       } else {
          curId = defaultId;
          log.debug("Getting part with default id [" + curId +"]");
       }

       try {

          if ( meta == null )
             throw new JspTagException("Meta Message object is null");

          int index;

          MessagePart part = findPart(meta, curId);


          if (part == null)
             throw new JspTagException("MessagePart with id [" + curId +"] not found.");


          log.debug("Got message part with id [" + part.getID() +"]");

         // sets the message id at the defined var variable.
         setVarAttribute(part);

       } catch (Exception e ) {
          String err = "GetMsgPartTag : Failed to get meta message part: " + e.getMessage();
          log.error(err);
          log.error("",e);
          throw new JspTagException(err);
       }

       return SKIP_BODY;
    }

    /**
     * Looks for the message container by id, index value, or full xml path.
     * Index values can have multiple levels (0.2.1).
     * You can have multiple ids separated by ".", or a full xml path.
     *
     * @param part The message to look for the part in.
     * @param id the id of the part, String or indexes separated by '.'.
     */
    private MessageContainer findPart(MessageContainer part, String id) throws JspTagException
    {

       String curId = id;
       int loc;

       int indexId;


       // first check if the id points to the part directly as an xml path.

       if (part instanceof Message)  {
          MessageContainer found = (MessageContainer) ((Message) part).getMessagePart(curId);
          if ( found != null)
             return found;
       }

       // if the part is still null then check if
       while ( part != null && StringUtils.hasValue(curId)) {

          if ( (loc = curId.indexOf(".")) > -1 ) {
             curId = id.substring(0,loc);
             id = id.substring(loc +1);
          } else
             id = null;

           try {
              try {
                 indexId = Integer.parseInt(curId);
                  // is an index
                  part = (MessageContainer)part.getChild(indexId);

              } catch (NumberFormatException nfe) {
                 if (part instanceof Message)  {
                    part = (MessageContainer) ((Message) part).getMessagePart(curId);
                    return part;
                 } else {
                    String err = "Failed to get message container with [" + curId +"]";
                   log.error(err);
                   throw new JspTagException (err);
                }
              }
              curId = id;
           } catch (ClassCastException ce) {
              String err = "Index id of [" +curId + "] is not a MessageContainer:" + ce.getMessage();
              log.error(err);
              throw new JspTagException (err);
           }
       }


        return part;
    }


}

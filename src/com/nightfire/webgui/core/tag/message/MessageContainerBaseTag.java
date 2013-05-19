/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;

import java.io.IOException;
import java.util.*;
import javax.servlet.http.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;



/**
 * Base class for all tags that handle the processing of meta data MessageContainer groups
 * such as a Section and Form.
 * This class must exist within a {@link MessageTag} JSP tag.
 *
 */
public abstract class MessageContainerBaseTag extends MsgInnerBaseTag
{

    /**
     * The current message part which needs to be set by a child class before
     * calling doStartTag
     */
    protected MessageContainer curPart;

    /**
     * The xml index to a child class which needs to be set by a child class before
     * calling doStartTag
     */
    protected int xmlChildIndex = 0;

     /**
     * current xml node path that the current MessageContainer is referring to. No indexes
     * exist on this xml path. Provides a xml path related
     * to this MessageContainer.
     *
     */
    protected String curXmlPath;

    /**
     * if true indicates that this message container does not have a xml node
     * to generate
     */
    private boolean skipXmlNode = false;


    /**
     * current xml node path( with indexes) that the current MessageContainer is referring to.
     * This refers to specific indexes of repeating xml nodes.
     * Used when accessing the xml node instances related to this MessageContainer.
     */
    protected String indexedCurXmlPath;

    private int curCount = 0;

    /**
     * Represents a newline character
     * @see com.nightfire.webgui.core.tag.TagConstants#NL
     */
    protected static final String NL = TagConstants.NL;


    /**
     * Tag setter method.
     * Sets the current messageContainer for this form
     * @param msgCont The MessageContainer object.
     *
     */
    public void setMsgPart(Object msgCont) throws JspException
    {
       curPart = (MessageContainer) TagUtils.getDynamicValue("msgPart",msgCont, MessageContainer.class,this,pageContext);
    }


    /**
     * Tag setter method.
     * sets the current selected xml index of this repeating form.
     * @param index The index of the selected form.
     */
    public void setXmlIndex(String index) throws JspException
    {
      index = (String) TagUtils.getDynamicValue("xmlIndex",index, String.class,this,pageContext);

       if ( StringUtils.hasValue(index) )
          xmlChildIndex = Integer.parseInt(index);
       else
          xmlChildIndex = 0;
    }


   /**
     * Updates the passed in path(within indexes) by adding the indexes to the path
     * so that it points to the correct xml nodes.
     * Indexes are added to the path for this current tag and all parent tags.
     *
     * @param path The xml path to the current MessagePart without indexes.
     * @return The updated path with indexes reflecting the current xml message node
     * and all of the parents current index.
     */
    public String updateXmlPathIndex(String path)
    {
       if (path.startsWith(curXmlPath) ) {
          if (log.isDebugEnabled() )
             log.debug(" Updating path [" + path +"], current path ["+curXmlPath +"], xml index [" + xmlChildIndex +"]");

          if ( !skipXmlNode )
             path = curXmlPath +"(" + xmlChildIndex +")" + path.substring(curXmlPath.length());

          Tag parent = getParent();
          if (parent != null && parent instanceof MessageContainerBaseTag)
            path =  ((MessageContainerBaseTag)parent).updateXmlPathIndex(path);
       }

       return path;
    }


    /**
     * returns the current MessageContainer that is selected.
     */
    public MessageContainer getCurrentPart()
    {
       return curPart;
    }
    


    /**
     * Handles the tracking of the current xml node
     * associated with this MessageContainer component.
     *
     * NOTE:Child classes must call super.doStartTag if they overwrite this method.
     *
     * @return EVAL_BODY_INCLUDE to evaluate the body
     * of this tag.
     * @throws JspException if The message object can't be accessed.
     * @see javax.servlet.jsp.tagext
     */
    public int doStartTag() throws JspException
    {

         super.doStartTag();

        try
        {
             // if the current index drops below 0 then reset it.
             // prevents non repeating forms or sections from using other indexes
             if (xmlChildIndex < 0 || !curPart.getRepeatable())
                xmlChildIndex = 0;

             curXmlPath = curPart.getFullXMLPath();

             XMLGenerator gen = getMessageData();
             
             // if this part has no local xml path then
             // we should skip it during any xml path generation
             if ( curPart.getXMLPath() == null)
                skipXmlNode = true;

               
             indexedCurXmlPath = updateXmlPathIndex(curXmlPath);
                
             
             // Obtain the actual number of child nodes 
             // tied to this message container path
             // get the index from the xml path, since it it may have
             // been updated by a parent tag instance.
             if (gen != null) {
                 int index = indexedCurXmlPath.lastIndexOf(".");
                 String parentPath = null;
                 String nodeName = null;
                 
                 if (index > 0) {
                     parentPath = indexedCurXmlPath.substring(0, index);
                     nodeName = curXmlPath.substring(curXmlPath.lastIndexOf(".")+1);
                 }
                 
                 if ( parentPath !=null && gen.exists(parentPath) ) {
                     Node n = gen.getNode(parentPath);
                     curCount = gen.getChildCount(n, nodeName);
                 }
                 
             }
             

             // if the index ever gets bigger than the current count reset to the last index
             // to prevent the creation of new forms or sections.
             // This can occur when changing from a section A to section B when section
             // A has more repeating sections than section B.
             // Or when the template dynamically changes the number 
             // of repeating containers.
             
             // Also prevents xmlChildIndex from going below 0.
             // This can also occur when there are no child nodes the first time a section or
             // form is visited.
             if (xmlChildIndex >= curCount && curCount > 0) {
                  xmlChildIndex = curCount -1;
                  indexedCurXmlPath = updateXmlPathIndex(curXmlPath);
             }
             



           return EVAL_BODY_INCLUDE;
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
     * Overrides parent's reset to perform initialization tasks, mostly to
     * commodate tag reuse feature of the servlet container.
     */
    public void reset()
    {
        curCount = 0;
    }

    /**
     * For this specific MessageContainer this returns the total number of xml nodes that exist.
     * If this message part does not repeat, than 0 or 1 xml nodes only should exist.
     * @return The number of instances xml nodes representing this MessagePart.
     */
    protected int getCount()
    {
       return curCount;
    }


    /**
     * Free resources after the tag evaluation is complete
     */
    public void release()
    {
        super.release();

        curPart = null;
        xmlChildIndex = 0;

        curXmlPath = null;
        indexedCurXmlPath = null;
        curCount = 0;
    }





}

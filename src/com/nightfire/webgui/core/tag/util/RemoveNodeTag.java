/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.core.tag.util;

import java.util.*;
import java.net.*;
import java.io.*;

import org.w3c.dom.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;


/**
 * This tag removes the node "targetNode" from the xml document passed in the
 * "source" param, only if the targetNode is present in the xml document.
 * The modified document is placed at the value specified
 * by "var" in the jsp tag. The modified xml is of type XMLGenerator.
 */
public class RemoveNodeTag extends VariableTagBase
{
    //Generic object that can hold either String, Document or XMLGenerator type data.
    private Object sourceObj = null;

    //Name of node to be removed.
    private String targetNodeName = null;

    /**
     * Set the source object to remove node from.
     *
     * @param obj The source xml.
     */
    public void setSource ( Object obj ) throws JspException
    {
       this.sourceObj = TagUtils.getDynamicValue("source", obj, Object.class, this, pageContext);
    }

    /**
     * Set the name of the node to be removed.
     *
     * @param obj The name of the node.
     */
    public void setTargetNodeName ( Object obj ) throws JspException
    {
       this.targetNodeName = (String)TagUtils.getDynamicValue("targetNodeName", obj, String.class, this, pageContext);
    }

    /**
     * Starts procesing of this tag.
     * The object passed into source could be of type String, Document or XMLGenerator.
     * The "targetNode" is removed from the document and the source is regenerated
     * as XMLGenerator and placed at the value (if)specified by "var".
     *
     * @throws JspException on processing error.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        XMLGenerator gen = null;

        try
        {
            //Get the source in the right format.
            if ( sourceObj instanceof String )
            {
                gen = new XMLPlainGenerator( (String) sourceObj );
            }
            else if (sourceObj instanceof XMLGenerator )
            {
                gen = (XMLGenerator)sourceObj;
            }
            else if (sourceObj instanceof Document )
            {
                gen = new XMLPlainGenerator((Document) sourceObj);
            }
            else
            {
                String err = "RemoveNodeTag: Invalid source passed in, only supports String, Document, or XMLGenerator.";
                log.error(err);
                throw new JspTagException( err);
            }

            //The synchronization added below is to take care of 2 frames simultaneously
            //submitting the same request and a race condition occurs in which the outcome
            //might be error as one of the threads can see the node, but cannot remove it,
            //because it is already removed by the other thread.
            
            //Remove node now.
            if ( gen.exists( targetNodeName ) )
            {
                synchronized( RemoveNodeTag.class )
                {
                    if ( gen.exists( targetNodeName ) )
                    {
                        gen.remove( targetNodeName );

                        if (log.isDebugEnabled() )
                            log.debug( "Modified xml after removing node [" + targetNodeName + "] is\n" + gen.getOutput() );
                    }
                    else
                        log.debug( "Node to remove is absent." );
                }//synchronized
            }
            else
                log.debug( "Node to remove is absent." );

            // Set the modified xml at the defined var variable as an XMLGenerator.
            setVarAttribute( gen );

        }
        catch (Exception e )
        {
            String err = "RemoveNodeTag : Could not remove node [" +targetNodeName + "], " + e.getMessage();
            log.error(err);
            log.error("",e);
            throw new JspTagException(err);
        }

        return SKIP_BODY;
    }

    public void release()
    {
       super.release();
       sourceObj = null;
       targetNodeName = null;
    }

}


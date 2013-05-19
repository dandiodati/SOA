/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;


/**
 * This class provides support for param tags in the body of a tag.
 * Any tags that want to use param tags need to extend this class.
 *
 */
public class ParamContainerTagBase extends VariableBodyTagBase
{


    boolean prependToList = false;
    
    // a list of all inner params
    private List params;

    // reference to outside list passed in
    // do not clear this list or it will wipe out list passed in.
    // all items from this list get added to the params list.
    private Collection itemsList;
    


    public ParamContainerTagBase()
    {
      params = new ArrayList();

    }

    /**
     * Indicates if this tag supports the use of ParamListTags.
     * By Default it does not. Child classes need to overload this
     * method if they want this support.
     *
     * @return a <code>boolean</code> value
     */
    protected boolean supportsNVPairGroups()
    {
        return false;
    }

    /**
     * This only applies when using a list of items passed in and
     *  body Param tags together. If true, then any Param tags defined in the body
     * of the tag get added before a list passed into the items attribute
     * If false then Param tags get appended to the end of a list passed into the 
     * the items attribute. The default is false (Params get appended to any list passed
     * in).
     *
     * @return a <code>boolean</code> value
     */
    public void setPrependParams(String bool)
    {
        prependToList = StringUtils.getBoolean(bool, false);        
    }
    

    /**
     * Returns a set of all params in the body of this tag.
     *
     * @return A list of NVPair objects.
     */
    public List getParamList() {
       return params;
    }


    /**
     * Sets a parameter on this tag. This method should be called by inner
     * param tags. Can also be used to add more params from another source.
     */
    public void setParam(String name, Object value)
    {
       params.add(new NVPair(name, value) );
    }


    

   /**
     * Sets a parameter on this tag. This method should be called by inner
     * param tags. Can also be used to add more params from another source.
     */
    public void setParam(NVPair pair) throws JspException
    {
        if (pair instanceof NVPairGroup && !supportsNVPairGroups())
            throw new JspException("NVPairGroup is not supported by this tag " + getClass().getName() );
        
        params.add(pair );
    }

    /**
     * Setter method that allows a list of NVPairs to be passed in
     * to this tag on top of using inner param/paramlist tags.
     * @param data - the data of the drop down list data
     */
    public void setItems( Object items ) throws JspException
    {
      setDynAttribute("items", items, Collection.class);

    }

    public int doStartTag() throws JspException
    {
        super.doStartTag();
        
        itemsList = (Collection)getDynAttribute("items");

        if (!prependToList && itemsList!= null && itemsList.size() > 0 ) 
            params.addAll(itemsList);

        return EVAL_BODY_BUFFERED;
    }
    

    /**
     * Wipes out the body content after execution of inner param tags.
     */
    public int doAfterBody() throws JspException
    {

        bodyContent.clearBody();
        
        if(prependToList && itemsList != null && itemsList.size() > 0 ) 
            params.addAll(itemsList);
        

        return SKIP_BODY;
    }

    /**
     * Overrides parent's reset to perform initialization tasks, mostly to
     * commodate tag reuse feature of the servlet container.
     */
    public void reset()
    {
        if (params != null)
            params.clear();

    }

    /**
     * Cleans up resources.
     */
    public void release()
    {
       super.release();
        
       params = null;
       itemsList = null;
       
    }
}

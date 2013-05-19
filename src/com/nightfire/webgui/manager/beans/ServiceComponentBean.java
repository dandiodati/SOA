/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/manager/beans/ServiceComponentBean.java#1 $
 */

package com.nightfire.webgui.manager.beans;


import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;

import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.manager.*;

import javax.servlet.*;
import javax.servlet.http.*;


import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.transformer.*;

import  com.nightfire.framework.constants.PlatformConstants;
import  com.nightfire.framework.message.*;




import java.util.*;

/**
 * An XMLBean which represents a Service Component in a bundle.
 *
 */
 
public class ServiceComponentBean extends InfoBodyBase implements ManagerServletConstants, PlatformConstants
{

    BundleBeanBag parent = null;


    //the following three are left for backwards compatibility
    public static final String HISTORY_DISPLAY_STATUS_FIELD = "HistoryDisplayStatus";
    
    public static final String EXPAND_STATUS                = "Expand";
    
    public static final String COLLAPSE_STATUS              = "Collapse";
  

    private ModifierBean modifier = null;

    private Map history = null;
    

    /**
     * Map used to hold custom data objects related to this bean.
     * NOTE: A call to getFinalCopy() will NOT copy this data to the copy.
     */
    public        final Map    customData                   = new HashMap();


    public ServiceComponentBean()  throws ServletException
    {
        super();

    }
    /**
     * Constructor.
     *
     * @param  headerData  Map of header fields.
     * @param  bodyData    Map of body fields.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public ServiceComponentBean(Map headerData, Map bodyData) throws ServletException
    {
       super(headerData, bodyData);
    }

    /**
     * Constructor.
     *
     * @param  headerData  XML header document.
     * @param  bodyData    XML body document.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public ServiceComponentBean(String headerData, String bodyData) throws ServletException
    {
        super(headerData, bodyData);

    }
    
    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public  ServiceComponentBean(XMLGenerator header, XMLGenerator body) throws ServletException
    {
       super(header, body);
    }

    /**
     * Constructor.
     *
     * @param  headerData  XMLGenerator object for the header data.
     * @param  bodyData    XMLGenerator object for the body data.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public  ServiceComponentBean(Document header, Document body) throws ServletException
    {
       super(header, body);
    }

    public void setParentBag(BundleBeanBag bag)
    {
       parent = bag;
    }

    public BundleBeanBag getParentBag()
    {
       return parent;
    }

    
    /**
     * Set a Map of OrderTransaction objects on this order and modifier.
     * This is a list of transactions associated with this order and modifier,
     * the key for each entry should be the unique id of each OrderTransaction object.
     *
     * @param orderTransactions a <code>Map</code> value
     */
    public void setHistory(Map orderTransactions) 
    {
        history = orderTransactions;
    }

    public Map getHistory() 
    {
        return history;
    }
    

    /**
     * Sets any associated modifier bean on this bean.
     *
     * @param bean a <code>ModifierBean</code> value
     */
    public void setModifierBean(ModifierBean bean) throws ServletException
    {
        modifier = bean;
        // modifier should have the same service type as this bean so we set it here.
        // incase it did not have the correct name root node when decomposing.
        modifier.setServiceType(getServiceType());
        modifier.setId(getId());
        modifier.setParent(this);
        
    
    }

    
    public void setId(String id) 
    {
        // update modifier id too
        super.setId(id);
        if (modifier != null)
            modifier.setId(id);
    }
    

    /**
     * Removes any associated modifier bean on this bean.
     *
     * @param bean Returns the bean that jot removed. Or null if there
     * was no modifier bean.
     */
    public ModifierBean removeModifierBean() throws ServletException
    {
        ModifierBean oldBean = modifier;
        
        modifier = null;
        return oldBean;
    }
    
        
    
    /**
     * Gets the active modifier bean for this service ocomponent bean.
     * Returns null if there is no modifier.
     *
     * @return a <code>ModifierBean</code> value
     */
    public ModifierBean getModifierBean()
    {
        return modifier;
    }

    /**
     * Copies this bean and any existing modifier
     */
    public NFBean getFinalCopy() throws MessageException
    {
       ServiceComponentBean  bean = (ServiceComponentBean) super.getFinalCopy();
       if ( modifier != null) {
           try {
               bean.setModifierBean((ModifierBean)modifier.getFinalCopy());
           }
           catch (ServletException e) {
               throw new MessageException(e.getMessage());
           }
           
       }
       
       return bean;
    }

    /**
     * Convenient method for displaying the string representation of the header data.
     *
     * @return  Header data string.
     */
    public String describeBodyData()
    {
        
        
        
        if ( modifier != null) {
            StringBuffer descr = new StringBuffer(super.describeBodyData());
            descr.append("------------  ").append(modifier.getId()).append(" Modifier Bean ------------\n");
            descr.append("     Modifier Header:\n");
            descr.append("").append(modifier.describeHeaderData());
            descr.append("     Modifier Body:\n");
            descr.append("").append(modifier.describeBodyData());
            
            return descr.toString();
            
        }
        
        
        return super.describeBodyData();
        
        
    }





}

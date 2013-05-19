/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.TagSupport;
import  javax.servlet.http.HttpServletRequest;

import  com.nightfire.framework.util.*;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.beans.BundleBeanBag;
import  com.nightfire.webgui.core.svcmeta.*;
import  com.nightfire.webgui.manager.svcmeta.*;


/**
 * ConstructServiceActionButtonsTag is responsible for drawing the service-level
 * buttons which allows the user to perform actions at the service level.  
 */

public class ConstructServiceActionButtonsTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{   
    public static final String VIEW_WORKITEMS_ACTION = "query-workitems";
    public static final String VIEW_ADMIN_WORKITEMS_ACTION = "administer-work-items";

    public static final String EDIT_ORDER_ACTION = "edit-order";

    public static final String EDIT_DISPLAY = "Edit/View";
    public static final String ADD_DISPLAY = "Add";
    public static final String DEL_DISPLAY = "Delete";

    private ComponentDef  serviceDef;
    
    private BundleBeanBag bundleBean;
    
    
    /**
     * Getter method for the service-component definition object.
     *
     * @return  Service-component definition object.
     */
    public Object getServiceDef()
    {
        return serviceDef;
    }
    
    /**
     * Setter method for the service-component definition object.
     *
     * @param  serviceDef  Service-component definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setServiceDef(Object serviceDef) throws JspException
    {
        this.serviceDef = (ComponentDef)TagUtils.getDynamicValue("serviceDef", serviceDef, ComponentDef.class, this, pageContext);
        
        if (this.serviceDef == null)
        {
            String errorMessage = "ERROR: ConstructServiceActionButtonsTag.setServiceDef(): The service-component definition object passed in via attribute [serviceDef] is null.";
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
    }
    
    /**
     * Getter method for the service-bundle bean bag object.
     *
     * @return  Service-bundle bean bag object.
     */
    public Object getBundleBean()
    {
        return bundleBean;
    }
    
    /**
     * Setter method for the service-bundle bean bag object.
     *
     * @param  bundleBean  Service-bundle bean bag object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBundleBean(Object bundleBean) throws JspException
    {
        this.bundleBean = (BundleBeanBag)TagUtils.getDynamicValue("bundleBean", bundleBean, BundleBeanBag.class, this, pageContext);
        
        if (this.bundleBean == null)
        {
            String errorMessage = "ERROR: ConstructServiceActionButtonsTag.setBundleBean(): The service-bundle bean bag object passed in via attribute [bundleBean] is null.";
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
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
      
        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Starting tag processing ...");    
        }
        
        StringBuffer output       = new StringBuffer();
        
        String       bundleStatus = bundleBean.getHeaderValue(STATUS_FIELD);
        
        String       serviceType  = serviceDef.getID();
         
        // always display the edit order button
        output.append("<a class=\"ServiceActionButton\" href=\"javascript:getServiceDetail('").append(serviceType).append("', '").append(EDIT_ORDER_ACTION).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('Edit/View Order Details')\"><img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif").append("\">").append(EDIT_DISPLAY).append("</a>&nbsp&nbsp;");       

//      Until the backend supports editing of saved bundles and update them accordingly,
//      disable the feature of adding or deleting components on previously saved bundles
//      Currently, the backend will have no clue what components have been added or deleted
//      from the previously saved bundle - so deleting the check on SAVED_STATUS. Only display
//      the add and delete button for a brand new bundle.
//      if (!StringUtils.hasValue(bundleStatus) || bundleStatus.equals(SAVED_STATUS))
        if (!StringUtils.hasValue(bundleStatus))
        {
            output.append("<a class=\"ServiceActionButton\" href=\"javascript:addService('").append(serviceType).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('Add Component')\"><img class=\"ServiceActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif").append("\">").append(ADD_DISPLAY).append("</a>&nbsp;&nbsp;");
            
            output.append("<a class=\"ServiceActionButton\" href=\"javascript:deleteServices('").append(serviceType).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('Delete Component')\"><img class=\"ServiceActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif").append("\">").append(DEL_DISPLAY).append("</a>&nbsp&nbsp;");
        }
        else if (!bundleStatus.equals(SAVED_STATUS))
        {
           // add the view worklist and admin worklist buttons
           // if the user has permission

            try {
                
                if (ServletUtils.isAuthorized(pageContext.getSession(), VIEW_WORKITEMS_ACTION))
                   output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('").append(VIEW_WORKITEMS_ACTION).append("', '").append(serviceType).append("', '").append("Work List").append("', '')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Work Items for this order and its modifiers.')\"><img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif").append("\">").append("Work List").append("</a>&nbsp&nbsp;");

                if (ServletUtils.isAuthorized(pageContext.getSession(), VIEW_ADMIN_WORKITEMS_ACTION))
                    output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('").append(VIEW_ADMIN_WORKITEMS_ACTION).append("', '").append(serviceType).append("', '").append("Admin Work List").append("', '')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('Administer Work Items for this order and its modifiers.')\"><img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif").append("\">").append("Admin Work List").append("</a>&nbsp&nbsp;");      
            } catch (Exception e) {
                String errorMessage = "Failed to authorize the user for via NF Security Service: " + e.getMessage();
                    
                log.error("doStartTag(): " + errorMessage);
                    
                throw new JspTagException(errorMessage);
            }

            Object[] validActions = serviceDef.getActionDefs().toArray();
            
            for (int i = 0; i < validActions.length; i++)
            {
                ActionDef aDef = (ActionDef)validActions[i];
                
                String action = aDef.getActionName();
                String redirectPage = aDef.getRedirectPage();
                java.awt.Dimension newWin = aDef.getNewWindow();
                try
                {
                    if (ServletUtils.isAuthorized(pageContext.getSession(), action))
                    {
                        output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('");
                        
                        output.append(action);
                        
                        output.append("', '");
                        
                        output.append(serviceType);
                        output.append("', '");

                        output.append(aDef.getDisplayName());
                        output.append("', '");
                        if ( StringUtils.hasValue(redirectPage)) {
                            output.append(redirectPage);
                        }
                        output.append("'");
                        
	                  if (newWin != null) {
                           output.append(",");
                           output.append(newWin.getWidth()).append(", ");
				   output.append(newWin.getHeight());
                        }
                        
                        output.append(")\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('");
                     
                        output.append(aDef.getDisplayName());

                        output.append("')\"><img class=\"ServiceActionButton\" border=\"0\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/ServiceActionButton.gif\">");
                        
                        output.append(aDef.getDisplayName());

                        output.append("</a>&nbsp;&nbsp;");
                    }
                }
                catch (Exception e)
                {
                    String errorMessage = "Failed to authorize the user for action [" + action + "] via NF Security Service: " + e.getMessage();
                    
                    log.error("doStartTag(): " + errorMessage);
                    
                    throw new JspTagException(errorMessage);
                }
            }
        }
        
        try
        {
            pageContext.getOut().print(output.toString());
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ConstructServiceActionButtonsTag.doStartTag(): Failed to write to JspWriter output-stream.\n" + e.getMessage();

            log.error(errorMessage);

            throw new JspTagException(errorMessage);
        }
        
        return SKIP_BODY;
    }
        
    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
        
        serviceDef = null;
        
        bundleBean = null;
    }
}

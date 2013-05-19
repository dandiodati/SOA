/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.tag;

import com.nightfire.webgui.core.xml.*;

import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.resource.*;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.manager.svcmeta.*;
import com.nightfire.webgui.manager.*;

import com.nightfire.framework.constants.PlatformConstants;

import java.net.*;
import java.io.*;
import javax.servlet.jsp.*;

import javax.servlet.jsp.tagext.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;


import org.w3c.dom.*;

import com.nightfire.framework.rules.*;

/**
 * Builds a listing of business rule errors for each service component in a bundle.
 * Uses The ValidationErrorTag to build the list of errors that get displayed for each service component.
 *
 *
 */
public class ValidationErrorGroupTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{

    public static final String SELECTED_GROUP_PREFIX = "ValidationErrors.";

    /**
     *
     */
    private XMLGenerator bizRuleErrors;
    private BundleBeanBag requestBundleBag;

    private int errorIndex = 0;
    private List errorGrpListings;

    private BundleDef bundleDef;
    Map   bizRuleMappers;

    /**
     * Setter method to set the validation error xml response.
     * @param errors The Object that holds the validation errors
     */
    public void setErrors(Object errors)  throws JspException
    {
       setDynAttribute("errors", errors, XMLGenerator.class);
    }



     /**
     * Setter method to set the bundle bean bag which produced the biz rule errors.
     *
     */
    public void setServiceBundleBean(Object bag)  throws JspException
    {
        setDynAttribute("serviceBundleBean",bag, BundleBeanBag.class);
    }



    /**
     *  Generate biz rule error bars and code to switch to different errors
     *
     *
     */
    public int doStartTag() throws JspException
    {

        super.doStartTag();

        bizRuleErrors= (XMLGenerator) getDynAttribute("errors");
        requestBundleBag = (BundleBeanBag) getDynAttribute("serviceBundleBean");
        
         if (requestBundleBag == null || bizRuleErrors == null)
        {
           String errorMessage = "ValidationErrorGroupTag: Service Bundle bean or XML Business rule errors are null.";
           log.error(errorMessage);
           throw new JspException(errorMessage);
        }


        Map   bundleDefs = (Map)pageContext.getAttribute(BUNDLE_DEFS, PageContext.APPLICATION_SCOPE);

        // get the bundle id using service bundle name for backwards compatiblity
        // later we can remove this
        String bundleId = requestBundleBag.getHeaderValue(ManagerServletConstants.SERVICE_BUNDLE_NAME);
        
        // the new style is to always use the meta data name as the id.
        if (!StringUtils.hasValue(bundleId))
            bundleId = requestBundleBag.getHeaderValue(ManagerServletConstants.META_DATA_NAME);
        
        bundleDef  = (BundleDef)bundleDefs.get(bundleId);

        if (bundleDef == null)
        {
           String errorMessage = "ValidationErrorGroupTag: No BundleDef object with name [" + bundleId + "] exists in the servlet-context's [" + BUNDLE_DEFS + "] lookup.";
           log.error(errorMessage);
           throw new JspException(errorMessage);
        }
        // get the current set of biz rule mappers created by the data adapter
        bizRuleMappers = (Map)pageContext.getAttribute(ServletConstants.BIZ_RULE_MAPPINGS, PageContext.REQUEST_SCOPE);

        if (bizRuleMappers == null ) {
           String errorMessage = "ValidationErrorGroupTag: Failed to obtain biz rule mappings from request.";
           log.error(errorMessage);
           throw new JspException(errorMessage);
        }

         // for all available service component create a list of
         // errors for each type of component
         try {
            errorGrpListings = createErrGrps(pageContext.getRequest(), bundleDef,
                                          bizRuleMappers, requestBundleBag, bizRuleErrors);
         } catch (Exception e) {
            log.error("Failed to get create xml biz error groups: " + e.getMessage());
            throw new JspException(e);
         }

        if ( errorGrpListings == null || errorGrpListings.isEmpty() ) {
           log.error("Failed to create error group listings");
           throw new JspException("Could not create error group listings.");
        }
        
        createServiceTypeHeader();

        return EVAL_BODY_INCLUDE;
    }

    /**
     * Creates the heading for the error set of each service type, with a drop-down
     * menu to navigate to each component of that type.
     *
     * @exception  JspException  Thrown when a processing error is encountered.
     */
    private void createServiceTypeHeader() throws JspException
    {
        NVPair errorSet          = (NVPair)errorGrpListings.get(errorIndex);
        
        String selectedComponent = pageContext.getRequest().getParameter(SELECTED_GROUP_PREFIX + errorSet.name);
        
        if (log.isDebugEnabled())
        {
            log.debug("createServiceTypeHeader(): The errorred component selected is [" + selectedComponent + "].");   
        }

        String htmlHeader = createServiceTypeHtmlHeader(selectedComponent, errorSet);

        try 
        {
            pageContext.getOut().println("<Table cellpadding=\"0\" cellspacing=\"0\" class=\"ServiceErrorTable\"><tr><td>");
            
            pageContext.getOut().println(htmlHeader);
        } 
        catch (IOException e)
        {
            String errorMessage = "Failed to output html to page:\n" + e.getMessage();
            
            log.error("createServiceTypeHeader(): " + errorMessage);
            
            throw new JspException(errorMessage);
        }
        
        ValidationErrorInfo info = findSelectedInfo(selectedComponent, (List)errorSet.value);

        setVarAttribute(info);
    }

    /**
     * Addes the rest of the needed html/jscript code to finish a single service component's
     * errors. It also starts another iteration for the next service component.
     *
     */
    public int doAfterBody() throws JspException
    {

        try {
           pageContext.getOut().println("</td></tr></Table>");
           pageContext.getOut().println("<Table cellspacing=0 cellpadding=0 class=ServiceSeparator>");
           pageContext.getOut().println("<tr><td><img src=\"" + TagUtils.getWebAppContextPath(pageContext)+ "/images/shim.gif\"/>");
           pageContext.getOut().println("</td></tr></Table>");
        } catch (IOException e) {
           log.error("Failed to end of service table to page : " + e.getMessage());
           throw new JspException(e);
        }

       if ( errorGrpListings.size() > errorIndex + 1) {
         //still more errors other service components eval the body again for another
         // service component
         errorIndex++;
          //create error heading  with dropdown menu
         createServiceTypeHeader();
         return EVAL_BODY_AGAIN;
       } else
          return SKIP_BODY;

    }

    /**
     * Helper method to create the header bar for each service type.
     *
     * @param  selectedComponent  Selected errorred component.
     * @param  errorSet           Error data of each service type.
     
     * @return  Header bar html.
     */
    private String createServiceTypeHtmlHeader(String selectedComponent, NVPair errorSet)
    {
        StringBuffer html        = new StringBuffer();
                           
        String       serviceType = errorSet.name;
                      
        List         errorList   = (List)errorSet.value;
           
        String       displayName = bundleDef.getComponent(serviceType).getDisplayName();
       
        if (log.isDebugEnabled())
        {
            log.debug("createServiceTypeHtmlHeader(): Creating html header bar for service type with display name [" + displayName + "] ...");
        }

        html.append("<table class=\"ServiceErrBar\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
           
        html.append("<tr>");
           
        html.append("<td >&nbsp;").append(displayName).append("</td>");
           
        html.append("<td>&nbsp;</td>");
           
        html.append("<td class=\"ServiceErrSelection\">");
           
        createGotoMenu(html, selectedComponent, serviceType, errorList);
           
        html.append("</td>");
           
        html.append("</tr>");
           
        html.append("</table>");

        return html.toString();
    }

    /**
     * Creates a go-to-component drop-down menu for each service type.
     *
     * @param  html               Html to output to page.
     * @param  selectedComponent  Selected component of a particular service type.
     * @param  serviceType        Service type.
     * @param  errorList          List of errors.  Each corresponds to a component.
     */
    protected void createGotoMenu(StringBuffer html, String selectedComponent, String serviceType, List errorList)
    {
        
        // if this is a modifier the change id to parent servicecomponent
        if ( StringUtils.hasValue(selectedComponent)) {
            int scIndex = selectedComponent.indexOf(".");
            if (scIndex > -1) 
                selectedComponent = selectedComponent.substring(0, scIndex);
        }
        

        html.append("Go To: <select name=\"");
        
        html.append(SELECTED_GROUP_PREFIX + serviceType);
        
        html.append("\" onChange=\"gotoErrorComponent(this)\">");

        int errorCount      = errorList.size();
        
        List componentBeans = requestBundleBag.getBeans(serviceType);
        
        for (int i = 0; i < errorCount; i++) 
        {
            ValidationErrorInfo  info = (ValidationErrorInfo)errorList.get(i);
            
            InfoBodyBase bean = info.getServiceComponentBean();
            if ( bean instanceof ModifierBean) 
                bean = ((ModifierBean)bean).getParent();
            

            if (bean.getId().equals(selectedComponent))
            {
                html.append("<option selected=\"selected\" value=\"");
            }
            else
            {
                html.append("<option value=\"");
            }

            // Assure that each of the errorred component beans are in the right
            // sequence, as in the bundle.
            
            int index = componentBeans.indexOf(bean);

            html.append(bean.getId()).append("\">");
            
            html.append("Order").append(" #").append(index + 1);
            
            html.append("</option>").append(NL);
        }

        html.append("</select>");
        
        if (log.isDebugEnabled())
        {
            log.debug("createGotoMenu(): Created a go-to-component drop-down menu for service type [" + serviceType + "] ...");
        }
    }

    /**
     * Finds the errors of a component that matches the selectedId.
     * If the selectedId is null, then the first set of errors for this component
     * is selected.
     */
    private ValidationErrorInfo findSelectedInfo(String selectedId, List errors) throws JspException
    {

       // if this is a modifier the change id to parent servicecomponent
        if ( StringUtils.hasValue(selectedId)) {
            int index = selectedId.indexOf(".");
            if (index > -1) 
                selectedId = selectedId.substring(0, index);
        }
        
        
       if ( errors != null && errors.size() > 0 &&  !StringUtils.hasValue(selectedId) )
          return (ValidationErrorInfo)errors.get(0);

       for (int i=0; i < errors.size(); i++ ) {
          ValidationErrorInfo item = (ValidationErrorInfo)errors.get(i);
          if ( item.getServiceComponentBean().getId().equals(selectedId))
             return item;
       }

       // throw a null point exception if the field was not found
       log.error("Failed to find selected id [" + selectedId +"]");
       throw new JspException("Failed to find selected id [" + selectedId +"]");


    }




    /**
     * Returns a list that holds NVPairs of service groups. Each NVPair contains the
     * name of the service group and the value is another
     * list with all the components which produced an error.
     */
    private List createErrGrps(ServletRequest request, BundleDef bundleDef, Map bizRuleMappers, BundleBeanBag bundleBag, XMLGenerator bizRuleErrors) throws MessageException, ServletException
    {
        ServletContext context = ((HttpServletRequest)request).getSession().getServletContext();
        
        KeyTypeMap messageDataCache = (KeyTypeMap)pageContext.getSession().getAttribute(ServletConstants.MESSAGE_DATA_CACHE);
                    
        String     messageId        = pageContext.getRequest().getParameter(ServletConstants.NF_FIELD_HEADER_MESSAGE_ID_TAG);

        NFBean     requestBean      = (NFBean)messageDataCache.get(messageId, ServletConstants.REQUEST_BEAN);
    
        String     action           = requestBean.getHeaderValue(NF_FIELD_ACTION_TAG);
        
        if (action.equals(VALIDATE_SERVICE_BUNDLE_ACTION) || action.equals(NEW_SERVICE_BUNDLE_ACTION))
        {
            return createBundleErrorGroups(request, bundleDef, bizRuleMappers, bundleBag, bizRuleErrors);   
        }
        else
        {
            return createComponentErrorGroup(request, requestBean, bizRuleMappers, bundleBag, bizRuleErrors);   
        }
    }
    
    /**
     * Creates a list of business-rule errors grouped by component type.  This
     * method handles the bundle validation case.
     *
     * @param  context         ServletContext object.
     * @param  bundleDef       BundleDef object.
     * @param  bizRuleMappers  Lookup for business-rule map.
     * @param  bundleBag       BundleBeanBag object.
     * @param  bizRuleErrors   Business-rule error XML.
     *
     * @exception  MessageException  Thrown when a processing error is encountered.
     * @exception  ServletException  Thrown when a processing error is encountered.
     *
     * @return  A list of business-rule errors.
     */
    private List createBundleErrorGroups(ServletRequest req, BundleDef bundleDef, Map bizRuleMappers, BundleBeanBag bundleBag, XMLGenerator bizRuleErrors) throws MessageException, ServletException
    {        
        ArrayList errorGroups = new ArrayList();
        
        Iterator  compIter    = bundleDef.getComponents().iterator();

        // Iterate through all the components in this bundle.
        
        while (compIter.hasNext())
        {
            ComponentDef comp  = (ComponentDef)compIter.next();
                  
            String       id    = comp.getID();

            int          count = bizRuleErrors.getChildCount(bizRuleErrors.getDocument().getDocumentElement(), id);
                
            if (log.isDebugEnabled())
            {
                log.debug("createBundleErrorGroups(): There are [" + count + "] components of type [" + id + "] with business-rule errors."); 
            }

            List bundleBeanGrpList = bundleBag.getBeans(id);

            // Make sure that there is at least one component of this type with
            // errors and that the bundle bean contains at least one component of
            // this type.
            
            if ((count > 0) && (bundleBeanGrpList != null)) 
            {
                int componentTypeCount = bundleBeanGrpList.size();
                
                if (componentTypeCount > 0)
                {
                    List   errorList = new ArrayList();
                   
                    NVPair pair      = new NVPair(id, errorList);
                   
                    errorGroups.add(pair);
                    
                    // Iterate through all the components in the returned error list.
                    
                    for (int i = 0; i < count; i++) 
                    {
                        Node n = bizRuleErrors.getNode(id + "(" + i + ")");

                        
                        // The error-description node itself is just under this node.
                        Element e = (Element)n;   
                        String errorDescription = e.getAttribute("value").trim();

                        int    index            = Integer.parseInt(bizRuleErrors.getAttribute(n, "index"));
                            
                        if (index >= componentTypeCount) 
                        {
                            log.warn("createBundleErrorGroups(): The index [" + index + "] given in the errorred component (in the business-rull error) is greater than the number of [" + id + "] components in the bundle, skipping ...");

                            continue;
                        }
                            
                        // Use the index indicated in each errorred component to
                        // retrieve the corresponding component bean from the bundle.
                        
                        ServiceComponentBean bean     = (ServiceComponentBean)bundleBeanGrpList.get(index);

                        String               metaFile = bean.getHeaderValue(ServletConstants.META_DATA_PATH);
                           
                        if (log.isDebugEnabled())
                        {
                            log.debug("createBundleErrorGroups(): Locating the business-rule map for component bean [" + bean.getId() + "] ...");
                        }
                        
                        BizRuleMapper mapper = (BizRuleMapper)bizRuleMappers.get(bean.getId());
                        
                        if (log.isDebugEnabled() && (mapper != null))
                        {
                            log.debug("createBundleErrorGroups(): Located the business-rule map:\n" + mapper.describe());
                        }
                        
                        if (mapper == null)
                        {
                            log.debug("createBundleErrorGroups(): Failed to locate the business-rule map.  Proceeding without the map ...");
                        }
                        
                        if (log.isDebugEnabled())
                        {
                            log.debug("createBundleErrorGroups(): Loading meta resource [" + metaFile + "] for error field navigation ...");
                        }

                        Message meta = (Message)ServletUtils.getLocalResource(req, metaFile, ServletConstants.META_DATA);

                        errorList.add(new ValidationErrorInfo(mapper, errorDescription, meta, bean) );
                    }
                }
            }
        }

        if ((errorGroups == null) || (errorGroups.size() == 0))
        {
            String errorMessage = "Failed to match any business rules to service components in the bundle.";
            
            log.error("createBundleErrorGroups(): " + errorMessage);
            
            throw new ServletException(errorMessage);
        }
        
        return errorGroups;
    }

    /**
     * Creates a list of business-rule errors based on a component validation.
     *
     * @param  context         ServletContext object.
     * @param  requestBean     Request bean object in the message cache.
     * @param  bizRuleMappers  Lookup for business-rule map.
     * @param  bundleBag       BundleBeanBag object.
     * @param  bizRuleErrors   Business-rule error XML.
     *
     * @exception  MessageException  Thrown when a processing error is encountered.
     * @exception  ServletException  Thrown when a processing error is encountered.
     *
     * @return  A list of business-rule errors.
     */
    private List createComponentErrorGroup(ServletRequest req, NFBean requestBean, Map bizRuleMappers, BundleBeanBag bundleBag, XMLGenerator bizRuleErrors) throws MessageException, ServletException
    {        
        String componentId = requestBean.getHeaderValue(SCID_FIELD);  
                
        if (log.isDebugEnabled())
        {
            log.debug("createComponentErrorGroup(): Looking up component bean with id [" + componentId + "] in the bundle ...");
        }
        
        InfoBodyBase componentBean    = (InfoBodyBase)bundleBag.getBean(componentId);
                
        BizRuleMapper        mapper           = (BizRuleMapper)bizRuleMappers.get(componentId);
        
        String               metaFile         = componentBean.getHeaderValue(ServletConstants.META_DATA_PATH);
        
        Message              meta             = (Message)ServletUtils.getLocalResource(req, metaFile, ServletConstants.META_DATA);
        
        String               serviceType      = componentBean.getServiceType();
        
        // The error-description node itself is just under this node.
        Element e = (Element)bizRuleErrors.getNode(serviceType);
        
        String errorDescription = e.getAttribute("value").trim();
                
        if (log.isDebugEnabled())
        {
            log.debug("createComponentErrorGroup(): Located component bean with id [" + componentId + "] in the bundle.");
            
            log.debug("createComponentErrorGroup(): Located the business-rule map:\n" + mapper.describe());
            
            log.debug("createComponentErrorGroup(): Loaded the meta resource [" + metaFile + "] for error field navigation.");
            
            log.debug("createComponentErrorGroup(): Obtained component's error description:\n" + errorDescription);
        }
       
        List   errorGroups = new ArrayList();
                  
        List   errorList   = new ArrayList();

        NVPair errorGroup  = new NVPair(serviceType, errorList);
               
        errorList.add(new ValidationErrorInfo(mapper, errorDescription , meta, componentBean));
        
        errorGroups.add(errorGroup);
        
        return errorGroups;
    }
    
    /**
     * Overrides parent's reset to perform initialization tasks, mostly to
     * commodate tag reuse feature of the servlet container.
     */
    public void reset()
    {
        errorIndex = 0;
    }

    public void release()
    {
       super.release();

       bizRuleErrors = null;
       bizRuleMappers = null;
       bundleDef = null;
       requestBundleBag = null;

    }


    /**
     * Provides all the validation error info related to a single service component.
     */
    public class ValidationErrorInfo
    {

       private BizRuleMapper bizMapper;
       private String xmlBizErrors;
       private Message meta;
       private InfoBodyBase scBean;

       protected ValidationErrorInfo(BizRuleMapper mapper, String bizErrors, Message meta, InfoBodyBase bean)
       {
          bizMapper = mapper;
          xmlBizErrors = bizErrors;
          this.meta = meta;
          scBean = bean;

       }


        /**
        * Returns the business rule mapper which provides mappings from biz rule errors
        * to the associated gui fields.
        * @param The biz rule mapper for this service component.
        */
       public BizRuleMapper getBizMapper()
       {
          return bizMapper;
       }

       /**
        * Returns the xml of business rule errors.
        * @param The business rule errors produced for this service component.
        */
       public String getXMLBizErrors()
       {
          return xmlBizErrors;
       }

       /**
        * Returns the meta file associated with the current service component.
        * @returns the Gui meta file which desribes the page associated with this bean.
        *
        */
       public Message getMeta()
       {
          return meta;
       }

        /**
        * Returns the outgoing service component or modifier bean which produced this set of validation errors.
        * @return The ServiceComponentBean or ModifierBean for these validation errors.
        */
       public InfoBodyBase getServiceComponentBean()
       {
          return scBean;

       }


    }



}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import com.nightfire.framework.message.common.xml.*;

import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.ServletRequest;

import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.tag.util.LoadTemplateResourceTag;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.manager.svcmeta.*;
import  com.nightfire.webgui.core.beans.NFBean;
import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.meta.Message;


/**
 * PrepareBundleBeanTag is responsible for setting up a service-bundle bean bag to 
 * be used for display, manipulation, and submission.  The adding and removing of
 * service-component beans from the bean bag are also handled by this tag.
 */

public class PrepareBundleBeanTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{        
    private static final String DELETE_SERVICE_COMPONENT_ACTION = "delete-service-component";
          
    private static final String ADD_SERVICE_COMPONENT_ACTION    = "add-service-component";
          
    private static final String COMPONENT_COUNT_ALLOWED         = "ComponentCountAllowed";
        
    private static final String COMPONENT_TYPE                  = "ComponentType";
        
    private BundleBeanBag existingBean;
    
    private BundleDef     bundleDef;
        
    private NFBean        additionalInfo;
        
    private XMLGenerator  predefinedBundleDef;

    private String       applyTemplates   = "true";

   
     

    /**
     * Getter method for the existing service-bundle bean.
     *
     * @return  Existing service-bundle bean.
     */
    public Object getExistingBean()
    {
        return existingBean;
    }

    /**
     * Setter method for the existing service-bundle bean.
     *
     * @param  existingBean  Existing service-bundle bean.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setExistingBean(Object existingBean) throws JspException
    {
        this.existingBean = (BundleBeanBag)TagUtils.getDynamicValue("existingBean", existingBean, BundleBeanBag.class, this, pageContext);
    }
    
     
    /**
     * method to set applyTemplates field.
     * 
     * @param applyTemplates  value to set    
     * 
     * @throws JspException  Thrown when an error occurs during processing.
     */
    public void setApplyTemplates(Object applyTemplates) throws JspException 
    {    
        this.applyTemplates = (String) applyTemplates;
    }
    
    /**
     * Getter method for the the bundle definition object.
     *
     * @return  Bundle definition object.
     */
    public Object getBundleDef()
    {
        return bundleDef;
    }
    
    /**
     * Setter method for the bundle definition object.
     *
     * @param  bundleDef  Bundle definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBundleDef(Object bundleDef) throws JspException
    {
        this.bundleDef = (BundleDef)TagUtils.getDynamicValue("bundleDef", bundleDef, BundleDef.class, this, pageContext);
        
        if (this.bundleDef == null)
        {
            String errorMessage = "ERROR: " + getClass().getName() + ".setBundleDef(): The bundle definition object passed in via attribute [BundleDef] is null.";

            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
    }

    /**
     * Getter method for the additional-info object.
     *
     * @return  Additional-info object.
     */
    public Object getAdditionalInfo()
    {
        return additionalInfo;
    }
    
    /**
     * Setter method for the additional-info object.
     *
     * @param  additionalInfo  Additional-info object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setAdditionalInfo(Object additionalInfo) throws JspException
    {
        this.additionalInfo = (NFBean)TagUtils.getDynamicValue("additionalInfo", additionalInfo, NFBean.class, this, pageContext);
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
        
        ServletRequest servletRequest = pageContext.getRequest();
        
        NFBean         requestBean    = (NFBean)servletRequest.getAttribute(REQUEST_BEAN);
        
        if (existingBean == null)
        {            
            String predefinedBundleName = requestBean.getHeaderValue(PREDEFINED_BUNDLE_ID_HEADER_FIELD);
            
            try {
                    
                String predefinedDefPath = (String)props.get(PREDEFINED_BUNDLE_DEF_PROP);
                Document tmpDoc = (Document)ServletUtils.getLocalResource(pageContext.getRequest(), predefinedDefPath, ServletConstants.XML_DOC_DATA, true);

                predefinedBundleDef = new XMLPlainGenerator(tmpDoc);
                

                if (predefinedBundleName.equals(EMPTY_BUNDLE))
                {            
                    if (log.isDebugEnabled())
                    {
                        log.debug("doStartTag(): Creating a brand new empty service-bundle bean bag ...");    
                    }
                    
                    existingBean = new BundleBeanBag();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doStartTag(): Creating a brand new predefined service-bundle bean bag ...");    
                    }
                    
                    existingBean = createPredefinedBundleBean(predefinedBundleName);
                }
                
                existingBean.setHeaderValue(SERVICE_BUNDLE_NAME, bundleDef.getID());
                
                existingBean.setHeaderValue(BUNDLE_DISPLAY_NAME, bundleDef.getDisplayName());

                existingBean.setHeaderValue(META_DATA_NAME, bundleDef.getMetaDataName());
            }
            catch (Exception e)
            {
                String errorMessage = "ERROR: PrepareBundleBeanTag.doStartTag(): Failed to create a new bean bag for the predefined bundle [" + predefinedBundleName + "]:\n" + e.getMessage();
                
                log.error(errorMessage);
                
                throw new JspException(errorMessage);
            }
            
            setVarAttribute(existingBean);
            
            String messageId = pageContext.getRequest().getParameter(ServletConstants.NF_FIELD_HEADER_PREFIX + ServletConstants.MESSAGE_ID);
            
            if (!StringUtils.hasValue(messageId))
            {
                messageId = requestBean.getHeaderValue(NF_FIELD_HEADER_MESSAGE_ID_TAG);
            }
            
            TagUtils.setBeanInMsgCache(pageContext, messageId, varName, existingBean);
        }
        else
        {
            String action = servletRequest.getParameter(NF_FIELD_ACTION_TAG);

            if (action == null)
            {
                action = "";
            }
            
            if (log.isDebugEnabled())
            {
                log.debug("doStartTag(): A service-bundle bean bag already exists.  The requested action is [" + action + "].");
            }
            
            if (action.equals(ADD_SERVICE_COMPONENT_ACTION))
            {
                String componentToAdd = servletRequest.getParameter(SERVICE_COMPONENT_HEADER_FIELD);
                
                if (componentIsAddable(componentToAdd))
                {                
                    try
                    {
                        existingBean.addBean(componentToAdd, createServiceComponentBean(componentToAdd, ServiceComponentBean.COLLAPSE_STATUS));
                    }
                    catch (Exception e)
                    {
                        log.error("PrepareBundleBeanTag.doStartTag(): Failed to create and add a new ServiceComponentBean instance to the existing bundle:\n" + e.getMessage());
                    }
                }
                else
                {
                    servletRequest.setAttribute(COMPONENT_TYPE, componentToAdd);
                    
                    servletRequest.setAttribute(INVALID_ACTION, ADD_SERVICE_COMPONENT_ACTION);
                                        
                    servletRequest.setAttribute(COMPONENT_COUNT_ALLOWED, String.valueOf(bundleDef.getComponent(componentToAdd).getMaxOccurs()));
                }
            }
            else if (action.equals(DELETE_SERVICE_COMPONENT_ACTION))
            {
                Object[] componentsToDelete = getComponentsToDelete(requestBean);
 
                String   componentType      = getServiceComponentType((String)componentsToDelete[0]);
                
                if (componentsAreDeletable(componentType, componentsToDelete.length))
                {
                    for (int i = 0; i < componentsToDelete.length; i++)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("doStartTag(): Removing service-component bean [" + (String)componentsToDelete[i] + "] from the service-bundle bean bag ...");   
                        }
                                
                        existingBean.removeBean((String)componentsToDelete[i]);
                    }
                }
                else
                {
                    servletRequest.setAttribute(COMPONENT_TYPE, componentType);
                    
                    servletRequest.setAttribute(INVALID_ACTION, DELETE_SERVICE_COMPONENT_ACTION);
                    
                    servletRequest.setAttribute(COMPONENT_COUNT_ALLOWED, String.valueOf(bundleDef.getComponent(componentType).getMinOccurs()));
                }
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Updated bundle bean has the following content:\n " + existingBean.describeBeans(true));    
        }
        
        return SKIP_BODY;
    }
    
    /**
     * Utility method to extract service-component type from the fullly indexed
     * service-component id, i.e., ICPPortIn from ICPPortIn(0).
     *
     * @param  componentId  Fully indexed service-component id.
     *
     * @return  Service-component type.
     */
    private String getServiceComponentType(String componentId)   
    {
        String componentType  = componentId;
        
        int    openParenIndex = componentId.indexOf('(');
        
        if (openParenIndex > 0)
        {
            componentType = componentId.substring(0, openParenIndex);
        }
        
        return componentType;
    }
     
    /**
     * Determines whether a service component can be added to the bundle.
     *
     * @param  componentType  Type of component.
     * 
     * @return  true if it is addable, false otherwise.
     */
    private boolean componentIsAddable(String componentType)
    {
        ComponentDef componentDef    = bundleDef.getComponent(componentType);
        
        int          maxCountAllowed = componentDef.getMaxOccurs();
        
        int          currentCount    = existingBean.getBeanCount(componentType);
        
        if (log.isDebugEnabled())
        {
            log.debug("componentIsAddable(): There are [" + currentCount + "] " + componentType + " in the bundle currently.  The maximum number allowed is [" + maxCountAllowed + "].");
        }
        
        if (currentCount < maxCountAllowed)
        {
            return true;   
        }
        
        return false;
    }

    /**
     * Determines whether service components can be deleted from the bundle.
     *
     * @param  componentType   Type of component.
     * @param  numberToDelete  Number of components to delete.
     * 
     * @return  true if they are deletable, false otherwise.
     */
    private boolean componentsAreDeletable(String componentType, int numberToDelete)
    {
        ComponentDef componentDef    = bundleDef.getComponent(componentType);
        
        int          minCountAllowed = componentDef.getMinOccurs();
        
        int          currentCount    = existingBean.getBeanCount(componentType);
        
        if (log.isDebugEnabled())
        {
            log.debug("componentsAreDeletable(): There are [" + currentCount + "] " + componentType + " in the bundle currently.  The minimum number allowed is [" + minCountAllowed + "].");
        }
        
        if ((currentCount - numberToDelete) >= minCountAllowed)
        {
            return true;   
        }
        
        return false;
    }
    
    /**
     * Constructs the list of indexed service names to be deleted from the bundle,
     * from the request parameters.
     *
     * @param  requestBean  Bean that contains request data.
     *
     * @return  List of indexed service names.
     */
    private Object[] getComponentsToDelete(NFBean requestBean)
    {        
        ArrayList      componentList      = new ArrayList();
                
        ServletRequest servletRequest     = pageContext.getRequest();
        
        Enumeration    requestParams      = servletRequest.getParameterNames();
        
        String         indexedFieldPrefix = SERVICE_COMPONENT_HEADER_FIELD + "-";
        
        while (requestParams.hasMoreElements())
        {
            String requestParam = (String)requestParams.nextElement();
                        
            if (requestParam.startsWith(indexedFieldPrefix))
            {
                componentList.add(servletRequest.getParameter(requestParam));
            }
        }
        
        return componentList.toArray();
    }
    
    /**
     * Creates a bean bag based on the indicated bundle name.
     *
     * @param  predefinedBundleName  Predefined bundle name.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  Created bean bag.
     */
    private BundleBeanBag createPredefinedBundleBean(String predefinedBundleName) throws JspException
    {        
        try
        {
            BundleBeanBag bundleBean = new BundleBeanBag();

            String bundleDef = predefinedBundleDef.getValue(predefinedBundleName + ".BundleDefinition");
            bundleBean.setHeaderValue(ManagerServletConstants.SERVICE_BUNDLE_NAME, bundleDef);

            if (predefinedBundleDef.exists(predefinedBundleName))
            {
                String  componentsPath = predefinedBundleName + ".ServiceComponents";
                
                Node[]  components     = predefinedBundleDef.getChildren(componentsPath);
                
                for (int i = 0; i < components.length; i++)
                {
                    String componentName = components[i].getNodeName();
                    
                    int    count         = 1;
                    
                    try
                    {
                        count = Integer.parseInt(predefinedBundleDef.getValue(componentsPath + "." + i + ".Count"));    
                    }
                    catch (Exception e)
                    {
                        log.error("PrepareBundleBeanBag.createPredefinedBundleBean(): Encountered a non-integer component count in predefined bundle definition, at bundle and component [" + predefinedBundleName + ", " + componentName + "].  A 1 will be used instead.");
                    }
    
                    if (log.isDebugEnabled())
                    {
                        log.debug("createPredefinedBundleBean(): Adding [" + count + "] new [" + componentName + "] service-component bean to the new [" + predefinedBundleName + "] service-bundle bean ...");
                    }

                    for (int j = 0; j < count; j++)
                    {
                        bundleBean.addBean(componentName, createServiceComponentBean(componentName, ServiceComponentBean.COLLAPSE_STATUS));
                    }       
                }
            }
            else
            {
                log.error("PrepareBundleBeanBag.createPredefinedBundleBean(): Failed to locate [" + predefinedBundleName + "] bundle in the predefined-bundle definition document.  An empty bundle will be created instead.");
            }
            
            return bundleBean;
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: PrepareBundleBeanBag.createPredefinedBundleBean(): Failed to create a [" + predefinedBundleName + "] bundle bean bag based on the predefined-bundle definition document:\n" + e.getMessage();
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
    }
    
    /**
     * Creates a ServiceComponentBean object and setting the necessary header
     * information on it.
     *
     * @param  componentType         Type of component/service.
     * @param  historyDisplayStatus  Toggle status of the service-history display.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    private ServiceComponentBean createServiceComponentBean(String componentType, String historyDisplayStatus) throws Exception
    {
        XMLGenerator         additionalInfoParser = (XMLGenerator)additionalInfo.getBodyDataSource();
        
        ServiceComponentBean bean                 = new ServiceComponentBean();
                                           
        if (additionalInfoParser.exists(componentType))
        {
            Node[] additionalInfoFields = additionalInfoParser.getChildren(componentType);

            for (int i = 0; i < additionalInfoFields.length; i++)
            {
                String fieldName  = additionalInfoFields[i].getNodeName();
                
                String fieldValue = additionalInfoParser.getNodeValue(additionalInfoFields[i]);
                
                bean.setHeaderValue(fieldName, fieldValue);
                
                if (log.isDebugEnabled())
                {
                    log.debug("createServiceComponentBean(): Set new [" + componentType + "] service-component bean's header with [" + fieldName + ", " + fieldValue + "].");
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("createServiceComponentBean(): There is no additional information to be added to the [" + componentType + "] service-component bean's header.");
            }
        }
        
        bean.setHeaderValue(STATUS_FIELD, NEW_STATUS);
        
        bean.setHeaderValue(ServiceComponentBean.HISTORY_DISPLAY_STATUS_FIELD, historyDisplayStatus);
        //outbound acknowledgemet to Ilec will not be shown until user gives indication 
        //using checkbox on bundle home page
        bean.setHeaderValue(ManagerServletConstants.SHOW_OUTBOUND_ACK_HEADER_FIELD, "false");
        
        String messageType = bean.getHeaderValue(ServletConstants.MESSAGE_TYPE);
        ComponentDef componentDef = bundleDef.getComponent(componentType);
        
        if (componentDef != null) 
        {
            bean.setHeaderValue(COMPONENT_DISPLAY_NAME, componentDef.getDisplayName());
            if (!StringUtils.hasValue(messageType) )
              messageType = componentDef.getDefaultMessageType().getName();

        } else {
          log.error("Reference to non-existing Component Definition [" + componentType +"]");
          throw new JspTagException("Reference to non-existing Component Definition [" + componentType +"]");
        }
        
          

        String resourceRoot = pageContext.getServletContext().getInitParameter(RESOURCE_DATA_ROOT);
        String keyPath = ((new StringBuffer(resourceRoot)).append('/').append(componentType).append('-').append(bean.getHeaderValue(SUPPLIER))).toString();

        setMetaData(bean, messageType, keyPath);

        // ApplyTemplates is an optional attribute. If user does not set it, it remians true by default.
        if("true".equalsIgnoreCase(this.applyTemplates))
        {
            setTemplateData(bean, messageType, keyPath);
        }
                
        setHeaderTransformer(bean, keyPath);
                
        setBodyTransformer(bean, messageType, keyPath);
        
        return bean;
    }
    
    /**
     * Set the meta-data object on the specified service-component bean, if one
     * exists.
     *
     * @param  bean     Service-component bean.
     * @param messageType  The message type for this bean
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    private void setMetaData(ServiceComponentBean bean, String messageType, String keyPath) throws Exception
    {

        String metaDataPath = ((new StringBuffer(keyPath)).append('/').append(META_DIRECTORY).append('/').append(messageType).append(".xml")).toString();

        if (log.isDebugEnabled())
        {
            log.debug("setMetaData(): Setting meta-data path [" + metaDataPath + "] in the service-component bean's header ...");
        }
        
        bean.setHeaderValue(META_DATA_PATH, metaDataPath);

        // try to create just one root node. 
        // If the service component is never edited
        // then only a body node exists and the outgoing xsl
        // script has a chance to creat an empty xml doc, which fails later.
        // 
        // To fix it we create just the top message root node
        // to give a chance for the outgoing xsl to match it
        // and create at least just a single root node when 
        // no other data exists.
        Message msgObj = (Message)ServletUtils.getLocalResource(pageContext.getRequest(), metaDataPath, ServletConstants.META_DATA, false);
        
        if (msgObj != null) {
            XMLGenerator gen = (XMLGenerator) bean.getBodyDataSource();
            gen.create(msgObj.getXMLPath());
        }

        
        
    }
    
    /**
     * Set the template-data object on the specified service-component bean, if
     * one exists.
     *
     * @param  bean     Service-component bean.
     * @param messageType  The message type for this bean
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    private void setTemplateData(ServiceComponentBean bean, String messageType, String keyPath) throws Exception
    {

        String templateDataPath = ((new StringBuffer(keyPath)).append('/').append(TEMPLATE_DIRECTORY).append('/').append(messageType).append(".xml")).toString();
        
        if (log.isDebugEnabled())
        {
            log.debug("setDataTemplate(): Getting template-data object from [" + templateDataPath + "] ...");
        }
        
        Object templateData = ServletUtils.getLocalResource(pageContext.getRequest(), templateDataPath, ServletConstants.TEMPLATE_DATA, false);



        if (templateData != null)
        {
             // create a new template generator via the load template resource tag's modify resource method
             
            templateData = TagUtils.getTemplateGenerator(pageContext, (XMLPlainGenerator)templateData);

            log.debug("setDataTemplate(): Setting template-data object on the service-component bean ...");
            
            bean.setBodyDataSource(templateData);
        }
    }
    /**
     * Set the header-transformer object on the specified service-component bean.
     *
     * @param  bean     Service-component bean.
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    private void setHeaderTransformer(ServiceComponentBean bean, String keyPath) throws Exception
    {

        String headerTransformPath = ((new StringBuffer(keyPath)).append('/').append(XSL_DIRECTORY).append('/').append(HEADER_TRANSFORMER_XSL)).toString();
        
        if (log.isDebugEnabled())
        {
            log.debug("setHeaderTransformer(): Getting header-transformer object from [" + headerTransformPath + "] ...");
        }
        
        Object headerTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), headerTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
        
        log.debug("setHeaderTransformer(): Setting header-transformer object on the service-component bean ...");
            
        bean.setHeaderTransform(headerTransformer);
    }
    
    /**
     * Set the body-transformer object on the specified service-component bean.
     *
     * @param  bean     Service-component bean.
     * @param messageType  The message type for this bean
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    private void setBodyTransformer(ServiceComponentBean bean, String messageType, String keyPath) throws Exception
    {

        String bodyTransformPath = ((new StringBuffer(keyPath)).append('/').append(XSL_DIRECTORY).append('/').append(messageType).append(OUTGOING_XSL_SUFFIX).append(".xsl")).toString();
        
        if (log.isDebugEnabled())
        {
            log.debug("setBodyTransformer(): Getting body-transformer object from [" + bodyTransformPath + "] ...");
        }
        
        Object bodyTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), bodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);


        log.debug("setBodyTransformer(): Setting body-transformer object on the service-component bean ...");

        bean.setBodyTransform(bodyTransformer);
    }
    
    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
        
        existingBean   = null;
        
        additionalInfo = null;
    }
}

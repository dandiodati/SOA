/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.manager.*;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.manager.svcmeta.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.resource.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.util.LoadTemplateResourceTag;
import com.nightfire.webgui.core.xml.*;


/**
 * This tag creates a bundle bean bag from a bundle xml, and places it in
 * the request and into the message data cache. It also sets the meta file and
 * transforms the data from outgoing xml into gui specific xml.
 *
 */
public class LoadBeanBagTag extends VariableTagBase implements ServletConstants, ManagerServletConstants
{

   /**
    * The xml document to add
    * to the session buffer.
    */
    protected Object bundleData;

    protected String beanName;

    /**
     * The message id that child classes need to set if the attribute is provided.
     */
    protected String messageId = null;

     // create an instance of the load template tag that can be used to load
    // new template generators
    private LoadTemplateResourceTag templateResourceTag = new LoadTemplateResourceTag();

    // template loading flag
    private boolean loadTemplates = false;



     /**
     * The bundle xml to use
     */
    public void setBundleData(Object bundleData) throws JspException
    {
       this.bundleData = TagUtils.getDynamicValue("bundleData", bundleData, Object.class, this, pageContext);
    }

    /**
     * Provides a current message id to use.
     * @param id A valid message id to access an existing data bean.
     *
     */
    public void setMessageId(String id) throws JspException
    {
       messageId = (String) TagUtils.getDynamicValue("messageId", id, String.class, this, pageContext);
    }


    /**
     * The name of the bean bag to create.
     *
     */
    public void setBeanName(String name)  throws JspException
    {
       beanName = (String) TagUtils.getDynamicValue("beanName", name, String.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     *
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();


       try {
           BundleBeanBag bag = new BundleBeanBag(beanName);

			if(StringUtils.hasValue(messageId))
			{
				BundleBeanBag oldBag=(BundleBeanBag)TagUtils.getBeanInMsgCache(pageContext, messageId, beanName);

				if( oldBag != null )
				{
                    String applyTemplateFromOldBag = oldBag.getHeaderValue(ManagerServletConstants.APPLY_TEMPLATES_HEADER_FIELD);

                    if(StringUtils.hasValue(applyTemplateFromOldBag))
                    {
                        bag.setHeaderValue(ManagerServletConstants.APPLY_TEMPLATES_HEADER_FIELD, applyTemplateFromOldBag);
                    }

                    if( "true".equals(applyTemplateFromOldBag) )
                    {
                        loadTemplates=true;
                    }
				}
			}

	   if ( bundleData != null) {
              if ( bundleData instanceof XMLGenerator)
                 bag.decompose((XMLGenerator)bundleData);
              else if (bundleData instanceof String )
                 bag.decompose((String)bundleData);
              else if (bundleData instanceof Document)
                 bag.decompose((Document)bundleData);

              messageId = TagUtils.setBeanInMsgCache(pageContext, messageId, beanName, bag);

           }  else
              log.debug("Creating an empty bundle bean bag : " + bag.describeBeans(false));


           setupBundleInfo(bag);



           if (log.isDebugEnabled() )
                 log.debug("Created the following bundle bean bag : "  + bag.describeBeans(true));

           // sets the message id at the defined var variable.
           setVarAttribute(messageId);

       } catch (Exception e ) {
          String err = "LoadBeanBagTag : Failed to create Bundle bean bag data: " + e.getMessage();
          log.error(err);
          log.error("",e);
          throw new JspTagException(err);
       }

       return SKIP_BODY;
    }

    public void setupBundleInfo(BundleBeanBag bag) throws JspException, ServletException
    {

      // get the action the was used
       NFBean         requestBean    = (NFBean)pageContext.getAttribute(REQUEST_BEAN, PageContext.REQUEST_SCOPE);

       String action = null;
       if (requestBean != null)
         action =  requestBean.getHeaderValue( NF_FIELD_ACTION_TAG);

      HashMap   bundleDefs = (HashMap)pageContext.getServletContext().getAttribute(BUNDLE_DEFS);
      String bundleMDN = bag.getHeaderValue(ManagerServletConstants.META_DATA_NAME);

      boolean loadedBundleDef = false;
      BundleDef bundleDef = null;
      if ( bundleDefs != null && StringUtils.hasValue(bundleMDN)) {

          bundleDef  = (BundleDef)bundleDefs.get(bundleMDN);
          if (bundleDef != null)
              loadedBundleDef = true;
      }

      if ( !loadedBundleDef ) {
            String errorMessage = "LoadBeanBagTag: No BundleDef object with name [" + bundleMDN + "] exists in the servlet-context's [" + BUNDLE_DEFS + "] lookup.";
            log.error(errorMessage);
            throw new JspException(errorMessage);
      }

      // set the id of the service bundle def
      bag.setHeaderValue(SERVICE_BUNDLE_NAME, bundleDef.getID());

      bag.setHeaderValue(BUNDLE_DISPLAY_NAME, bundleDef.getDisplayName());

      Map beans = (Map) bag.getBodyAsMap();
      Iterator bIter = beans.values().iterator();

      //Local manager does not allow 'template application' for SAVED_STATUS and Copy Action
     // Even if bundle header has ApplyTemplates as true.
     if(ServletUtils.getWebAppContextPath(pageContext.getServletContext()).endsWith("-local"))
     {
        if ( loadTemplates && !(SAVED_STATUS.equals(bag.getHeaderValue(ManagerServletConstants.STATUS_FIELD)) || COPY_SERVICE_BUNDLE_ACTION.equals(action) ))
            loadTemplates = false;
     }
     else
     {
          // if this is a saved bundle or we just performed a copy of a previous bundle
          // then load the templates

        if ( SAVED_STATUS.equals(bag.getHeaderValue(ManagerServletConstants.STATUS_FIELD)) || COPY_SERVICE_BUNDLE_ACTION.equals(action) )
            loadTemplates = true;
     }


      while (bIter.hasNext() ) {
         ServiceComponentBean bean = (ServiceComponentBean) bIter.next();

         String messageType = null;

         ComponentDef componentDef = bundleDef.getComponent(bean.getServiceType());
         if (componentDef != null)
         {
            bean.setHeaderValue(COMPONENT_DISPLAY_NAME, componentDef.getDisplayName());
            Document doc = ((XMLGenerator)bean.getBodyDataSource()).getDocument();

            MessageTypeDef mType = componentDef.getMessageType(doc);
            if ( mType == null)
              mType = componentDef.getDefaultMessageType();

            messageType = mType.getName();

         }

	   if (!StringUtils.hasValue(messageType) ) {
            log.error("Could not associate a message type for bundle id [" + bundleMDN
			     + "] and request data:\n" + ((XMLGenerator)bean.getBodyDataSource()).describe());
            throw new JspTagException("Could not associate a message type with the request.");
         }

         String serviceType = bean.getServiceType();
         String supplier = bean.getHeaderValue(ServletConstants.SUPPLIER);
         String version = bean.getHeaderValue(ServletConstants.INTERFACE_VER);

         try {
             setMetaData(bean, serviceType, supplier, version, messageType);
             setTransformers(bean, serviceType, supplier, version, messageType);

            if ( loadTemplates )
               setTemplateData(bean, serviceType, supplier, version, messageType);

         } catch (Exception e) {
            log.error("Failed to set meta data or body tranformon bean : " +  e.getMessage() );
            throw new JspException(e);
         }
      }

    }


    /**
     * Set the meta-data object on the specified service-component bean, if one
     * exists.
     *
     * @param  bean     Service-component bean.
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     * @param messageType The message type which is used as the file name
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected void setMetaData(ServiceComponentBean bean, String service, String supplier, String version, String messageType) throws Exception
    {


        String metaDataPath = ServletUtils.getMetaResourcePath(service, supplier, version, messageType);

        if (log.isDebugEnabled())
        {
            log.debug("setMetaData(): Setting meta-data path [" + metaDataPath + "] in the service-component [" + bean.getId() +"] bean's header ...");
        }

        bean.setHeaderValue(ServletConstants.META_DATA_PATH , metaDataPath);
    }


    /**
     * Set the body-transformer object on the specified service-component bean.
     *
     * @param  bean     Service-component bean.
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     * @param messageType The message type which is used as the file name
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected void setTransformers(ServiceComponentBean bean, String service, String supplier, String version, String messageType) throws Exception
    {


        String incomingBodyTransformPath = ServletUtils.getTransformResourcePath(service, supplier, version, "in", messageType);

        String outgoingBodyTransformPath = ServletUtils.getTransformResourcePath(service, supplier, version, "out", messageType);

        String outgoingHeaderTransformPath = ServletUtils.getTransformResourcePath(service, supplier, version, null, ServletConstants.HEADER_TRANSFORM_XSL);


        if (log.isDebugEnabled())
        {
            log.debug("setBodyTransformer(): Getting incoming body-transformer object from [" + incomingBodyTransformPath + "] ...");
        }

        Object bodyTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), incomingBodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);


        bean.setBodyTransform(bodyTransformer);

        log.debug("setBodyTransformer():Executing incoming body transform object on the service-component bean ...");
        bean.transform();
        bean.clearTransforms();

        if (log.isDebugEnabled())
        {
            log.debug("setBodyTransformer(): Getting outgoing transformer objects from [" + outgoingHeaderTransformPath + "],["+ outgoingBodyTransformPath +"] ...");
        }

        Object transformer = ServletUtils.getLocalResource(pageContext.getRequest(), outgoingHeaderTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
        bean.setHeaderTransform(transformer);

        transformer = ServletUtils.getLocalResource(pageContext.getRequest(), outgoingBodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
        bean.setBodyTransform(transformer);

    }


    /**
     * Merges a new template-data object with the specified service-component bean, if
     * one exists.
     *
     * @param  bean     Service-component bean.
     * @param  keyPath  Portion of the resource path which represents the service
     *                  type and supplier.
     * @param messageType The message type which is used as the file name
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected void setTemplateData(ServiceComponentBean bean, String service, String supplier, String version, String messageType) throws Exception
    {

        String templateDataPath = ServletUtils.getTemplateResourcePath(service, supplier, version, messageType);

        if (log.isDebugEnabled())
        {
            log.debug("setDataTemplate(): Getting template-data object from [" + templateDataPath + "] ...");
        }

        Object templateData = ServletUtils.getLocalResource(pageContext.getRequest(), templateDataPath, ServletConstants.TEMPLATE_DATA, false);



        if (templateData != null)
        {
             // create a new template generator via the load template resource tag's modify resource method
            templateData = TagUtils.getTemplateGenerator(pageContext, (XMLPlainGenerator)templateData);

            log.debug("setDataTemplate(): Merging template-data object with loaded service-component bean ...");

            XMLGenerator old = (XMLGenerator) bean.getBodyDataSource();

            ((XMLGenerator)templateData).merge(old);
            bean.setBodyDataSource((XMLGenerator)templateData);
        }
    }


    public void release()
    {
       super.release();

       beanName = null;
       bundleData = null;
       messageId = null;
    }

}

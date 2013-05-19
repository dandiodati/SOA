/**
 * Copyright (c) 2001 NightFire Software, Inc.  Al rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.gateway.tag.util;

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
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.gateway.svcmeta.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.resource.*;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.xml.*;
import javax.servlet.jsp.JspTagException;
import com.nightfire.framework.constants.PlatformConstants;


/**
 * This tag creates a new xml bean, and places it in
 * the request and into the message data cache. It also sets the meta file and
 * applying incoming transforms, sets outing transforms, sets templates, and sets some control header fields.
 *
 */
public class SetupNewMessageDetailTag extends VariableTagBase implements ServletConstants
{

    protected boolean readOnlyData = false;

    private String beanName;

    // default to true
    protected boolean useTemplates = true;

    protected ServiceDef service;

    protected String supplier = null;

    protected String version = null;
    


    /**
     * The message id that child classes need to set if the attribute is provided.
     */
    protected String messageId = null;


     /**
     * The service to use
     */
    public void setService(Object service) throws JspException
    {
       this.service = (ServiceDef)TagUtils.getDynamicValue("service", service, ServiceDef.class, this, pageContext);
    }

     /**
     * The version to use
     */
    public void setVersion(String version) throws JspException
    {
       this.version = (String)TagUtils.getDynamicValue("version", version, String.class, this, pageContext);
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
        beanName = (String) name;
    }

    /**
     * Indicates if templates should be loaded. This defaults to true.
     *
     */
    public void setUseTemplates(String bool)  throws JspException
    {
         String b = (String)TagUtils.getDynamicValue("useTemplates", bool, String.class, this, pageContext);

         useTemplates = StringUtils.getBoolean(b, true);
    }


    /**
     * Indicates that the data is readonly data and will be used for display
     * only. No outgoing transform scripts will be set on the bean.
     * Defaults to false.
     *
     */
    public void setReadOnlyData(String bool)  throws JspException
    {
         String b = (String)TagUtils.getDynamicValue("readOnlyData", bool, String.class, this, pageContext);

         readOnlyData = StringUtils.getBoolean(b, readOnlyData);
    }



    /**
     * Provides the supplier for this data.
     * @param supplier the supplier associated with this data.
     *
     */
    public void setSupplier(String supplier) throws JspException
    {
       this.supplier = (String) TagUtils.getDynamicValue("supplier", supplier, String.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     *
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();

       try {


           XMLGenerator bodyData = null;



           if (service == null) {
               log.error("Invalid ServiceDef object passed in");

               throw new JspTagException("Invalid ServiceDef object passed in");
           }

           if (!StringUtils.hasValue(supplier)) {
               log.error("supplier value is null.");
               throw new JspTagException("supplier is a required attribute");
           }

           XMLBean bean = createBean();

            messageId = TagUtils.setBeanInMsgCache(pageContext, messageId, beanName, bean);


           setup(bean);

           if (log.isDebugDataEnabled() )
               log.debugData("Created the following bean:\nHeader:\n " + bean.describeHeaderData() +"\nBody:\n" + bean.describeBodyData());


           // sets the message id at the defined var variable.
           setVarAttribute(messageId);

       } catch (ServletException e) {
           String err = "Failed to create bean : " + e.getMessage();

           log.error(err);
           throw new JspTagException(err);


       } catch (FrameworkException e ) {
          String err = "Failed to create bean : " + e.getMessage();
          log.error(err,e);
          throw new JspTagException(err);
       }

       return SKIP_BODY;
    }


    /**
     * Creates the bean that will be initialized.
     * This method can be overwritten by child classes to
     * setup a bean differently.
     *
     * @return a <code>XMLBean</code> value
     */
    protected XMLBean createBean() throws FrameworkException, JspException, ServletException
    {
        XMLGenerator headerData = new XMLPlainGenerator(PlatformConstants.HEADER_NODE);
        XMLGenerator bodyData = new XMLPlainGenerator(PlatformConstants.BODY_NODE);

        return new XMLBean(headerData, bodyData);
    }





    /**
     * Describe <code>setup</code> method here.
     *
     * @param bean a <code>XMLBean</code> value
     * @exception JspException if an error occurs
     * @exception ServletException if an error occurs
     */
    private void setup(XMLBean bean) throws JspException, ServletException
    {

         String messageType = null;

         bean.setHeaderValue(ServletConstants.SERVICE_TYPE, service.getID());

         bean.setHeaderValue(ServletConstants.SUPPLIER, supplier);
         bean.setHeaderValue(ServletConstants.INTERFACE_VER, version);

         if(readOnlyData)
             bean.setHeaderValue(ServletConstants.READ_ONLY,"true");


         Document doc = ((XMLGenerator)bean.getBodyDataSource()).getDocument();

         MessageTypeDef mType = service.getMessageType(doc);

	   if (mType == null ) {
           mType = service.getDefaultMessageType();
            log.debug("Could not associate a message type, using default");
         }

       //if this is still null throw an exception
	   if (mType == null ) {
            log.error("Could not associate a message type for bean request data:\n" + ((XMLGenerator)bean.getBodyDataSource()).describe());
            throw new JspTagException("Could not determine message type");

         }


         messageType = mType.getName();




         try {
             // do transform first since the message type may change
             // in child implementations of this class.
             messageType = setTransformers(bean, service.getID(), supplier,version, messageType);

             bean.setHeaderValue(ServletConstants.MESSAGE_TYPE, messageType);

             setMetaData(bean, service.getID(), supplier, version , messageType);

            if ( useTemplates )
                setTemplateData(bean, service.getID(), supplier,version,  messageType);

         } catch (Exception e) {
            log.error("Failed to set meta or transform data on bean : " +  e.getMessage() );
            throw new JspTagException(e.getMessage());
         }

    }


    /**
     * Set the meta-data object on the specified bean, if one
     * exists.
     *
     * @param  bean     bean to operate on.
     * @param svcType The service type for this bean
     * @param supplier The supplier for this bean
     * @param version The version for this data.
     * @param messageType The message type which is used as the file name
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected void setMetaData(XMLBean bean, String svcType, String supplier, String version,String messageType) throws Exception
    {

        String metaDataPath = ServletUtils.getMetaResourcePath(svcType, supplier, version, messageType);


        if (log.isDebugEnabled())
        {
            log.debug("setMetaData(): Setting meta-data path [" + metaDataPath + "] in bean's header ...");
        }

        bean.setHeaderValue(ServletConstants.META_DATA_PATH , metaDataPath);
    }


    /**
     * Set the body-transformer object on the specified bean.
     *
     * @param  bean     bean to operate on.
     * @param svcType The service type for this bean
     * @param supplier The supplier for this bean
     * @param version The version of this data
     * @param messageType The message type which is used as the file name
     * @return a new message type value. Since a transform may change it.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected String setTransformers(XMLBean bean, String svcType, String supplier, String version, String messageType) throws Exception
    {

        //Set outgoing transformer
        if(!readOnlyData) {
            String outgoingBodyTransformPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_OUT, messageType);

            if (log.isDebugEnabled())
                {
                    log.debug("setBodyTransformer(): Getting outgoing transformer objects from ["+ outgoingBodyTransformPath +"] ...");
                }

            Object transformer = ServletUtils.getLocalResource(pageContext.getRequest(), outgoingBodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
            bean.setBodyTransform(transformer);
        }
        
        return messageType;
        

    }


    /**
     * Merges a new template-data object with the specified bean, if
     * one exists.
     *
     * @param  bean     bean to operate on.
     * @param svcType The service type for this bean
     * @param supplier The supplier for this bean
     * @param version The version of this data
     * @param messageType The message type which is used as the file name
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     */
    protected void setTemplateData(XMLBean bean, String svcType, String supplier, String version,  String messageType) throws Exception
    {

        String templateDataPath = ServletUtils.getTemplateResourcePath(svcType, supplier,version,messageType);

        if (log.isDebugEnabled())
        {
            log.debug("setDataTemplate(): Getting template-data object from [" + templateDataPath + "] ...");
        }

        Object templateData = ServletUtils.getLocalResource(pageContext.getRequest(), templateDataPath, ServletConstants.TEMPLATE_DATA, false);



        if (templateData != null)
        {
             // create a new template generator via the load template resource tag's modify resource method
            templateData = TagUtils.getTemplateGenerator(pageContext, (XMLPlainGenerator)templateData);

            log.debug("setDataTemplate(): Merging template-data object with loaded bean ...");

            XMLGenerator old = (XMLGenerator) bean.getBodyDataSource();

            ((XMLGenerator)templateData).merge(old);
            bean.setBodyDataSource((XMLGenerator)templateData);
        }
    }


    public void release()
    {
       super.release();

       beanName = null;
       service = null;
       messageId = null;
       supplier = null;
       version = null;
       
       readOnlyData = false;

       useTemplates = true;

    }

}

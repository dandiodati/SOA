/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;


import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.XMLBean;
import com.nightfire.webgui.core.tag.TagUtils;

/**
 * A tag that modifies a service related bean by setting the metadata, outgoing transform
 * and template resources.
 * Must be used within the body of a ModifyServiceTagBase class.
 *
 */
public class SetupBeanTag extends ModifyServiceBeanTagBase
{

    protected boolean readOnlyData = false;

    // default to true
    protected boolean useTemplates = true;


    /**
     * Indicates if templates should be loaded. This defaults to true.
     *
     */
    public void setUseTemplates(String bool)  throws JspException
    {
        setDynAttribute("useTemplates", bool, String.class);
    }



    /**
     * Indicates that the data is readonly data and will be used for display
     * only. No outgoing transform scripts will be set on the bean.
     * Defaults to false.
     *
     */
    public void setReadOnlyData(String bool)  throws JspException
    {
        setDynAttribute("readOnlyData", bool, String.class);
    }




    public int doStartTag() throws JspException
    {

        super.doStartTag();
        
        XMLBean bean = (XMLBean)parentTag.getBean();
        
        useTemplates = StringUtils.getBoolean((String)getDynAttribute("useTemplates"), true);
        readOnlyData = StringUtils.getBoolean((String)getDynAttribute("readOnlyData"), false);
        
        String messageType = parentTag.getMessageTypeName();
        String supplier    = parentTag.getSupplier();
        String serviceType = parentTag.getServiceType();
        String version     = parentTag.getVersion();
        

        

        try {

            if(readOnlyData)
                bean.setHeaderValue(ServletConstants.READ_ONLY,"true");
            else
                bean.setHeaderValue(ServletConstants.READ_ONLY,"false");
            
            // do transform first since the message type may change
            // in child implementations of this class.
            messageType = setTransformers(bean, serviceType, supplier, version, messageType);
        
            // set message type again incase it changed
            bean.setHeaderValue(ServletConstants.MESSAGE_TYPE, messageType);

            setMetaData(bean, serviceType, supplier, version , messageType);

            if ( useTemplates )
                setTemplateData(bean, serviceType, supplier, version,  messageType);

        } catch (Exception e) {
            log.error("Failed to set meta or transform data on bean : " +  e.getMessage() );
            throw new JspTagException(e.getMessage());
        }

        return SKIP_BODY;
        
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
                log.debug("setTransformer(): Getting outgoing body transformer from ["+ outgoingBodyTransformPath +"] ...");
              

            Object transformer = ServletUtils.getLocalResource(pageContext.getRequest(), outgoingBodyTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
            bean.setBodyTransform(transformer);
        }


        String outgoingHeaderTransformPath = ServletUtils.getTransformResourcePath(svcType, supplier, version, ServletUtils.TRANSFORM_DIR_NONE , ServletConstants.HEADER_TRANSFORM_XSL );

        if (log.isDebugEnabled())
            log.debug("setTransformer(): Getting outgoing header transformer from ["+ outgoingHeaderTransformPath +"] ...");
        
        Object hTransformer = ServletUtils.getLocalResource(pageContext.getRequest(), outgoingHeaderTransformPath, ServletConstants.XSL_TRANSFORM_DATA);
        bean.setHeaderTransform(hTransformer);
                

        return messageType;
        
            
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
        
        useTemplates = true;
        readOnlyData = false;
    }
    
}

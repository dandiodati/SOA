/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;

import  javax.servlet.*;
import  javax.servlet.jsp.*;

import  org.w3c.dom.*;

import  com.nightfire.framework.message.MessageException;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.transformer.XSLMessageTransformer;
import  com.nightfire.framework.util.*;

import  com.nightfire.security.tpr.*;
import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.beans.SessionInfoBean;
import  com.nightfire.webgui.core.tag.*;




/**
 *
 */

public class ConvertToActiveTPVersionTag extends VariableTagBase
{

    private Object  data = null;

    private String varTransformOccured = null;

    private boolean transformOccured = false;

    private String varNewVersion = null;
    

    private String service =null;

    private String supplier = null;

    private boolean conversionRequired = false;

    private String version = null;



     /**
     * The service to use
     *
     * @param service Service
     * @throws JspException on error
     */
    public void setService(String  service) throws JspException
    {
        this.service = (String)TagUtils.getDynamicValue("service", service, String.class, this, pageContext);
    }



    /**
     * Provides the supplier for this data.
     * @param supplier the supplier associated with this data.
     * @throws JspException on error
     *
     */
    public void setSupplier(String supplier) throws JspException
    {
       this.supplier = (String) TagUtils.getDynamicValue("supplier", supplier, String.class, this, pageContext);
    }

    /**
     * Provides the conversionRequiredPath for this data.
     * @param conversionRequired - the conversionRequiredPath associated with this data.
     * @throws JspException on error
     *
     */
    public void setConversionRequired(String conversionRequired) throws JspException
    {
        this.conversionRequired = StringUtils.getBoolean(conversionRequired, false);
    }


     /**
      * The active version to use
      * @param version - the version associated with this data.
      * @throws JspException on error
      */
    public void setVersion(String version) throws JspException
    {
       this.version = (String)TagUtils.getDynamicValue("version", version, String.class, this, pageContext);
    }



     /**
     * The output variable to place the transformed flag.
     * The value is true or false and indicates if a transformation
     * occured.
     * @param var - the var.
     * @throws JspException on error
     */
    public void setVarTransformOccured(String var) throws JspException
    {
        this.varTransformOccured = var;
    }

     /**
     * The output variable to place the new version that that data is in.
     * If the data did not change then version passed in is returned.
     * @param var - the var.
     * @throws JspException on error
     *
     */
    public void setVarNewVersion(String var) throws JspException
    {
        this.varNewVersion = var;
    }



    /**
     * Setter for the 'data' attribute.
     *
     * @param  data  Options to use in the drop-down element.
     *
     * @exception  JspException  Thrown when an error occurs during attribute processing.
     */
    public void setData(Object data) throws JspException
    {
        this.data = (Object)TagUtils.getDynamicValue("data", data, Object.class, this, pageContext);
    }

 



    /**
     * Redefinition of parent's doStartTag().  This method processes the start
     * start tag for this instance.  It is invoked by the JSP page implementation
     * object
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY if there is no exception.
     */
    public int doStartTag() throws JspException
   {
       super.doStartTag();
           

       String cid = CustomerContext.DEFAULT_CUSTOMER_ID;
        
       if( pageContext.getSession() != null) {
            
           SessionInfoBean sBean = (SessionInfoBean) pageContext.getSession().getAttribute(ServletConstants.SESSION_BEAN);
           cid = sBean.getCustomerId();
            
       }

       if (!StringUtils.hasValue(supplier) ||
           !StringUtils.hasValue(service) ||
           !StringUtils.hasValue(version))
           throw new JspTagException("Invalid properties, supplier, service, or version are null");
       
           

       try {
           
           TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(cid);
        
           XMLGenerator bodyParser = null;
        

           if (data instanceof String ) {
               bodyParser = new XMLPlainGenerator((String) data);
           } else if (data instanceof XMLGenerator ) {
               bodyParser =  (XMLGenerator)data;
           } else if (data instanceof Document ) {
               bodyParser = new XMLPlainGenerator((Document) data);
           } else {
               String err = "Invalid data passed in, Only support objects String, Document, or XMLGenerator.";
               log.error(err);
               throw new JspTagException( err);
           }

           String activeVer = tpr.getMostRecentVersion(supplier, service);

           String transaction = tpr.getValidTransaction(service);

           String gwSupplier = tpr.getGatewaySupplier(supplier,transaction, service);

           if((tpr.conversionRequired(supplier, service, version) ) || (conversionRequired && (!activeVer.equals ( version ))) ) {

               String urlStr = "repository://localhost/maps.conversionMaps." + gwSupplier +"." + service +"."
                   + activeVer + "/From" + version;

               // urlStr = "file:///repository/conversionMaps/" + supplier +"/" + service +"/" 
               //     + activeVer + "/From" + version;



               log.debug("Conversion required, looking for resource " + urlStr);
               
              
               XSLMessageTransformer transformer = (XSLMessageTransformer)ServletUtils.getLocalResource(pageContext.getRequest(),urlStr, ServletConstants.XSL_TRANSFORM_DATA, true);
           
               bodyParser = new XMLPlainGenerator(transformer.transform(bodyParser.getOutputDOM()));

               transformOccured = true;

           
           }
           else
           {
               activeVer = version;
               log.debug("No conversion required");
           }


           // set the verision if the var is defined
           if(varNewVersion != null)
               VariableSupportUtil.setVarObj(varNewVersion, activeVer, scope, pageContext);
    
  
            // set the transform occured flag if set
            if(varTransformOccured != null)
                VariableSupportUtil.setVarObj(varTransformOccured, String.valueOf(transformOccured), scope, pageContext);

            // set the xml transformer
            if(bodyParser != null)
                setVarAttribute(bodyParser);    
        }
        catch (MessageException e)
        {
            
            String errorMessage = "Failed to parse or transform data: " + e.getMessage();

            log.error("doStartTag(): " + errorMessage);

            throw new JspTagException(errorMessage);
        }
        catch (TPRException e)
        {
            
            String errorMessage = "Failed to get trading partner information: " + e.getMessage();
            

            log.error("doStartTag(): " + errorMessage);

            throw new JspTagException(errorMessage);
        } catch (ServletException e ) {
           String errorMessage = "Failed to get transform resource : " + e.getMessage();

           log.error("doStartTag(): " + errorMessage);

           throw new JspTagException(errorMessage);
       }
       
        return SKIP_BODY;
    }

    public void release()
    {
       super.release();

       data       = null;

       service = null;

       supplier = null;
       version = null;

       varTransformOccured = null;
       transformOccured = false;
       
    }
}

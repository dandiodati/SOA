/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.manager.*;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.meta.help.*;
import com.nightfire.webgui.core.beans.*;


import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;

import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.manager.svcmeta.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.security.tpr.TradingPartnerRelationship;

import  java.util.*;

import  javax.servlet.*;
import javax.servlet.http.*;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;
import java.net.*;
import java.io.*;

import org.w3c.dom.*;
import com.nightfire.webgui.core.tag.message.BodyTag;



/**
 * This tag creates a meta file for that can be used for prompting the
 * user for addtional service component information.
 * It creates a xml bean,{@link ManagerServletConstants#ADDITIONAL_INFO},
 * that contains the following body structure.
 *
 *   &lt;Body&gt;
 *     &lt;Service type&gt;
 *        &lt;FieldName value=""/&gt;
 *        ...
 *     &lt;/Service type&gt;
 *   &lt;/Body&gt;
 *
 *    where Service type is the service component type
 *    and FieldName(s) is the additional info fields.
 *
 * The generated meta field will be in the form of
 *   Message object
 *      Form Object
 *         Section object
 *           Field object
 *           ...
 * The Message and Form objects will be dummy objects.
 * The section object will have the same id as the equivlent ComponentDef object (Service component).
 * Each field will be the fields obtained from the additional info fields from each ComponentDef object.
 * By looping over the Section objects, a Set of message, SimpleForm, and body tags can render
 * the fields on a page.
 */
public class PrepareAdditionalInfoTag extends VariableTagBase
{

    private BundleDef bundleDef;
    private BundleBeanBag bundleDataBag;
    private XMLGenerator data;


    // indicates if there are any non probibited fields that the
    // user needs to change.
    private boolean fieldsToPrompt = false;
    
    //This field is an optional attribute so that local manager application 
    //can use it for applying trading partner relationship.
    private boolean applyTPR = false;
    
    //for standalone Svc Component TPRelations can be applied before showing bundle home jsp
    private int componentCount = 0;
    /**
     * The boolean attribute that indicates requirement of applying trading partner relations
     */
    public void setApplyTPR(boolean applyTPRelation) throws JspException
    {
       this.applyTPR = applyTPRelation;
    }
     /**
     * The bundle definition object that describes all servicecomponents
     *
     */
    public void setBundleDef(Object bundleDef) throws JspException
    {
       this.bundleDef = (BundleDef)TagUtils.getDynamicValue("bundleDef", bundleDef, BundleDef.class, this, pageContext);
    }


    /**
     * The bundle bag with service component beans to
     * retrieve the data from.
     */
    public void setBundleDataBag(Object bundleDataBag) throws JspException
    {
       this.bundleDataBag = (BundleBeanBag)TagUtils.getDynamicValue("bundleDataBag", bundleDataBag, BundleBeanBag.class, this, pageContext);

    }

    /**
     * Starts procesing of this tag.
     *
     */
    public int doStartTag() throws JspException
    {
       super.doStartTag();
      

       try {

        EditableMessage msg = new EditableMessage();
        msg.setID("AdditionalInfoMsg");

        EditableForm holder = new EditableForm();
        holder.setID("AdditionalInfoForm");
        msg.add(holder);
        msg.registerPart(holder);

        List componentList  = bundleDef.getComponents();
        Iterator compIter   = componentList.iterator();
        componentCount      = componentList.size();

        if ( bundleDataBag == null )
            bundleDataBag = new BundleBeanBag();

        data = new XMLPlainGenerator(PlatformConstants.BODY_NODE);
        Map addInfoRefs = new HashMap();

        List tpSupplierList = null;
        String customerId = TagUtils.getCustomerId(pageContext);
        TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(customerId);

         while ( compIter.hasNext() ) {

            ComponentDef cdef = (ComponentDef) compIter.next();
            List parts = cdef.getAddlInfoFields();
            // if there are additional info fields
            // then create a form for this service component else do not
            if ( parts != null && parts.size() > 0 ) {

                String id = cdef.getID();

                EditableForm form = new EditableForm();


                // convert the data in the component def into a form object
                form.setDisplayName(cdef.getDisplayName());
                form.setID(id);
                form.setFullName(cdef.getFullName());

                boolean readOnly = false;

                if (bundleDataBag.exists(id +"(0)") ) {
                    readOnly = true;
                    NFBean bean = bundleDataBag.getBean(id +"(0)");
                    XMLGenerator headerGen = (XMLGenerator) bean.getHeaderDataSource();
                    // create a node with the id of the component
                    // and add all header fields for the associated service component bean
                    // under this node.
                    // this is done for when an order is saved
                    // and brought up again
                    String formXmlPath = form.getXMLPath();

                    Node cnode = data.create(formXmlPath);
                    data.copyChildren(cnode, headerGen.getDocument().getDocumentElement() );
                }


                //  Each Svc Component has its own supplier sets for a logged on customer.
                 if(applyTPR)
                 {
                    tpSupplierList = new ArrayList();
                    
                    Collection tpSuppliersCollection = tpr.getEnabledGatewaySuppliers(id);
                    if(tpSuppliersCollection  == null) {
                        throw new JspException(" TP Suppliers collection is null in DB for service [" +
                            id + "]." );
                    }
                    tpSupplierList = new ArrayList(tpSuppliersCollection);
                 }

               
                // add all fields to current form
                addAddFields(addInfoRefs, msg, form, parts, readOnly, tpSupplierList);
                // if there are any field to prompt for then add the form
                if ( form.getChildren().size() > 0 && fieldsToPrompt)
                   holder.add(form);
            }
                

            

         }
          // try to get the message id from a parameter, then the request bean
          String messageId = pageContext.getRequest().getParameter(ServletConstants.NF_FIELD_HEADER_PREFIX + ServletConstants.MESSAGE_ID);
          if ( !StringUtils.hasValue(messageId) ) {
             NFBean reqBean  = (NFBean)pageContext.getAttribute(ServletConstants.REQUEST_BEAN, PageContext.REQUEST_SCOPE);
             messageId = reqBean.getHeaderValue(ServletConstants.MESSAGE_ID);
          }

          XMLTemplateGenerator dataTemplate = new XMLTemplateGenerator(data.getDocument());

          XMLBean bean = new XMLBean(null, dataTemplate);
          TagUtils.setBeanInMsgCache(pageContext, messageId, ManagerServletConstants.ADDITIONAL_INFO, bean );
          if (log.isDebugEnabled() ) {
             log.debug("Created meta object :\n" + holder.describe());
             log.debug("Created AdditionalInfo bean :\nHEADER:\n" + bean.describeHeaderData());
             log.debug("Created AdditionalInfo bean :\nBODY:\n" + bean.describeBodyData());
          }
          setVarAttribute(holder);

       } catch (Exception e ) {
          String err = "Failed setup additional info objects : " + e.getMessage();
          log.error(err, e);
          throw new JspTagException(err);
       }

       return SKIP_BODY;
    }


    private void addAddFields(Map addInfoRefs, EditableMessage msg, EditableForm form, List parts, boolean readOnly, List tpSupplierList) throws FrameworkException
    {
        // add all the additional info fields onto the created form object
        // and add each field to the xml message
        
        for(int i = 0; i < parts.size(); i++ ) {
           ServiceField field = (ServiceField) parts.get(i);
           String fId = field.getID();
                      
           // clone the bundle field so that the original meta file field is not edited
           field = field.copy();
           
           // if there is a reference to an existing field then
           // add it as a hidden field
           log.debug("Checking AdditionalInfo field [" + fId +"]");

           DataType fieldDT = field.getDataType();
           
           
           OptionSource os = fieldDT.getOptionSource();
           boolean singleValField = os != null && os.getOptionValues().length == 1;
         
            if(applyTPR && tpSupplierList != null && fId.startsWith(ServletConstants.SUPPLIER))
            { 
                DataType newFieldDT = new DataType();
                //In future, DataType object also needs clonning so that Field copy() can return
                // clonned DatatType. 
                newFieldDT.setMaxLen(fieldDT.getMaxLen());
                newFieldDT.setFormat(fieldDT.getFormat());
                newFieldDT.setOptionSource(fieldDT.getOptionSource());
                newFieldDT.setBaseType(fieldDT.getBaseType());

                //TP Relationship needs to be applied on supplier field.
                List optionValsList = new ArrayList();
                List optionDispList = new ArrayList();
                List optionDescList = new ArrayList();

                String [] optValues = os.getOptionValues();
                String [] displayValues = os.getDisplayValues();
                String [] descriptionValues = os.getDescriptions();

                //preparing new OptionSource
                for ( int Ix=0; Ix < optValues.length; Ix++ )
                {
                    if( tpSupplierList.contains( optValues[Ix] ) )
                    {
                        optionValsList.add(optValues[Ix]);
                        optionDispList.add(displayValues[Ix]);
                        optionDescList.add(descriptionValues[Ix]);
                    }
               
                }//for
          
                optValues = new String[optionValsList.size()];
                displayValues = new String[optionDispList.size()];
                descriptionValues = new String[optionDescList.size()];

                optionValsList.toArray(optValues);
                optionDispList.toArray(displayValues);
                optionDescList.toArray(descriptionValues);

                DynamicOptionSource changeOptionCtl = new DynamicOptionSource();  
                changeOptionCtl.setOptionData(optValues, displayValues, descriptionValues );
                
                //if no tpr is configured
                if (optionValsList.size() == 0)
                {   //if tradingpartnerrelationship table does not have any supplier configured
                    // with the domain then consider this case as singleValField==true
                    // this case will not prompt for additional information.
                    singleValField = true;
                }

                //if supplier list is empty, use bundle def xmls i.e. do not replace it with new list (TP and bundle def)
                
                if(optionValsList.size() != 0)
                {
                    //replacing field's Option Source
                    newFieldDT.setOptionSource(changeOptionCtl);
                    //for singleValField option
                    os=changeOptionCtl;
                    //clonned field needs new data type with new optionsource. 
                    //As field clone does not clone DataType Object. This will prevent 
                    //modification in meta file.
                    field.setDataType(newFieldDT);
                    singleValField = optionValsList.size() == 1;

                }
                else if(componentCount == 1)// If bundle has single SVC Component with 0 number of trading partners
                {
                     throw new FrameworkException("Transaction for trading partner "+os.getOptionValues()[0]+" not configured for customer ["+TagUtils.getCustomerId(pageContext)+"].");
                }

   
            }          
            
           
           // if the field alreay exists then make a reference to the existing 
           // field and do not prompt for this field   
           if ( addInfoRefs.get(fId) != null ) {
              
             // reset the parent path to be under the current form
             // this must be done bewfore getting the full path to the field
             field.setParentPath(form.getPathElement());

              // get a path reference to the referenced field
              String refPath = ((ServiceField)addInfoRefs.get(fId)).getFullXMLPath();

              log.debug("Setting field as hidden.");
              String path = field.getFullXMLPath();
              Node child = data.create(path);
              // add field to form to set xml path


              // get nf xml path and convert to xpath
              refPath = "//" + refPath.replace('.', '/');
              refPath = changetoXpathIndexes(refPath);

              // now add a template link from this field to the referenced field
              data.setAttribute(child, "link", refPath);
              data.setAttribute(child, "readOnly", "true");
             
           } else if ( singleValField) {
             //else if this field only has a single value in the drop down list
             // there is no reason to prompt for it so do not add the field
             //But we add it to the xml so that the field value exists
                
                field.setParentPath(form.getPathElement());
                String path = field.getFullXMLPath();
                data.setValue(path, os.getOptionValues()[0]);
             
           } else   {
             // else add the field as a required field with no empty value
              
              addInfoRefs.put(fId, field);
              field.setParentPath(form.getPathElement());
              field.setDataType(new RequiredDataType() );
              field.setCustomValue(BodyTag.SHOW_EMPTY_VALUE, "f");
              
              String path = field.getFullXMLPath();
              form.add(field);
              msg.registerPart(field);
              
	        // if this is readonly field then add it as prohibited
		  // else
              // indicate that there is at least one required field that
              // the user needs to be prompted for.
              // This is used later to prevent the prompting of fields
              // when they are all prohibited anyway.

              if (readOnly)
                 field.setDataType(new ProhibitedDataType() );
		  else
                 fieldsToPrompt = true;
              
           }

       
        }
                

    }


    private String changetoXpathIndexes(String path)
    {

       for (int sdx = path.indexOf('('); sdx > -1; sdx = path.indexOf('(') ) {
          int edx = path.indexOf(')', sdx +1 );
          int index = Integer.parseInt(path.substring(sdx +1 , edx) );
          index += 1;
          path = path.substring(0, sdx) + "[" + index + "]" + path.substring(edx +1);
       }

       return path;
    }

    public void release()
    {
       super.release();

       bundleDataBag = null;
       bundleDef = null;
       data = null;
    }



    private class HiddenDataType extends DataType
    {
       public HiddenDataType()
       {
          super.name = "hidden";
       }
    }

    private class ProhibitedDataType extends DataType
    {
       public ProhibitedDataType()
       {
          super.usage = DataType.PROHIBITED;
       }
    }

    private class RequiredDataType extends DataType
    {
       public RequiredDataType()
       {
          super.usage = DataType.REQUIRED;
       }
    }


    private class EditableMessage extends Message
    {
       public EditableMessage() throws FrameworkException
       {
          super();
       }

       public void setID(String id)
        {
            this.id =  id;
            path = new PathElement();
            path.name = id;
            path.parent = null;
            path.part = this;
        }

         /**
         * Adds a child
         *
         * @param child The child to add
         */
        public void add(MessagePart child)
        {
           super.addChild(child);
        }
    }



    private class EditableForm extends Form
    {

        public void setID(String id)
        {
            this.id =  id;
            path = new PathElement();
            path.name = id;
            path.parent = null;
            path.part = this;
        }


        public void setDisplayName(String name)
        {
            displayName = name;
        }


        public void setFullName(String name)
        {
            fullName = name;
        }

        /**
         * Adds a child
         *
         * @param child The child to add
         */
        public void add(MessagePart child)
        {
           super.addChild(child);
        }

        public PathElement getPathElement()
        {

           return path;
        }


    }


}

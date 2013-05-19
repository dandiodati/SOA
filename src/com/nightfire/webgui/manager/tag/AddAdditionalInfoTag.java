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
import com.nightfire.security.tpr.TradingPartnerRelationship;
import com.nightfire.framework.message.MessageException;


/**
 * This tag creates a html and java script for addtional service component
 * information. There are two modes:
 * 1. if addInfoMode = "all",
 *    For each ComponentDef in the bundle, add all info fields from the
 *    ComponentDef and add the count field.
 * 2. If the addInfoMode = "needed"
 *    If the componentDef exists in the BundleBeanBa, only display
 *    info field(s) which is not in the ServiceCOmponentBean. If all info fields exists,
 *    don't prompt the user anything. Don't show any count info.
 *
 * Rules:
 * 1. In the "all" mode, If field is linked:
 *    - if service component instance exists, do not allow the user to change supplier;
 *    - if service component instance = 0 and all linked fields have
 *      no service componenet instance, allow the user to change supplier with
 *      a list of supplier values;
 *    - If service component instance doesn't exist and other linked fields have
 *      one or more service componenet instance(s) with existing supplier,
 *      only prompt the existing supplier in the selection box.
 * 2. If trading partner name exists in the component def, prompt for this field
 *    in both "all" and "needed" modes. See special handling for
 *    trading partner names.
 * 3. If trading partner name field does not exist, prompt for supplier name.
 *    See special Handling for Supplier Name (regardless if Supplier name
 *    is defined in the infoFields).
 *
 * Special Handling:
 * 1. TradingPartnerName is NOT from the bundleDef config file. We have to query
 *    TradingPartnerRelationship.getEnabledTradingPartners() to get a set of
 *    TradingPartnerNames.
 * 2. Supplier name is not based on the component def, we need to find the value
 *    via TradingPartnerRelationship.
 *
 * 3. Linked field is defined as multiple fields contain same field object. For example,
 * in a Loop bundledef, it may contain Loop with Supplier and DS with Supplier.
 * Both Supplier fields point to the same field definition.
 * In this case, a java script is created dynamically use the field XMLPath
 * as name. Depending on the data type, it will be created using either
 * createTextFun() or createSelectFun(). The java script function is used to
 * keep multiple fields value in sync. With the above example, if the user change
 * the supplier of Loop, the supplier of DS will be changed automatically.
 *
 * Restrictions:
 * 1. Currently, when the user want to delete occurrence, they can't select which
 *    which instance to delete.
 * 2. When they modify supplier or tradingpartner, all occurrence will be modified.
 */

public class AddAdditionalInfoTag extends VariableTagBase implements ManagerServletConstants
{
    private BundleDef bundleDef;
    private BundleBeanBag bundleDataBag;
    private String mode;
    // linkFieldMap stores fields which contains the same field def:
    // key: field (ServiceField)
    // value: LinkField
    private Map linkFieldMap = null;

    // infoFields contains all additional info fields need to be displayed.
    // it is used to check linked field
    // key: field (ServiceField)
    // value:  String NFH_<componentDefID>.<field XMLPath>
    private Map infoFields = null ;

    private Map infoCounts = null;

    // a variable to place the generated javascript code
    private String varJscript;
    // mode
    public static final String ALL_MODE = "all";
    public static final String NEEDED_MODE = "needed";
    // special handling for supplier
    public static final String TRADING_PARTNER = "TradingPartnerName";
    public static final String SUPPLIER = "Supplier";
    private boolean supplierExists;
    private boolean tpExists;
    private ServiceField supplierField = null;

    public static final int COUNT_SIZE = 4;


    private XMLPlainGenerator predefinedBundles;
    private String predefinedBundleName;


     public static final String HTML_HEAD = "\n<TR><TD width=\"100%\">"
         + "<table cellpadding=\"0\" cellspacing=\"0\" border =\"0\" width=\"100%\"><tr><td class=\"leftPadding\">"
         + "</td><td><table border='0' cellpadding='0' cellspacing='0' width=\"100%\" class='SimpleFormTable'>"
         + "<tr><td width=\"100%\"> "
         + "<table cellpadding=\"0\" cellspacing=\"0\" border= \"0\" class=\"FieldTable\" width=\"100%\">"
         + "<tr><td valign=\"top\" width=\"50%\">"
         + "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" class=\"ColumnTable\">"
         + "<tr><td>"
         + " <Table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">";

     public static final String HTML_TAIL = "</td></tr></Table></td></tr></table></td><td width=\"100%\"> "
         + "</td></tr></Table></td></tr></Table></td></tr></Table></td></tr>\n";

     public static final String SEP_HTML = "\n<tr><td width=\"100%\" class=\"FormHeading\"><br></td></tr>\n";
    /**
     * Setter method for the bundle definition object.
     *
     * @param  bundleDef  Bundle definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBundleDef(Object bundleDef) throws JspException
    {
        setDynAttribute("bundleDef", bundleDef, BundleDef.class);
    }


    /**
     * The bundle bag with service component beans to
     * retrieve the data from.
     */
    public void setBundleDataBag(Object bundleDataBag) throws JspException
    {
      setDynAttribute("bundleDataBag", bundleDataBag, BundleBeanBag.class);

    }

    /**
     * Setter method for the selected predefined bundle name.
     *
     * @param  name The name of the predefined bundle.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setPredefinedBundleName(String name) throws JspException
    {
        setDynAttribute("predefinedBundleName", name, String.class);
    }

    /**
     * Setter method for the predefined bundle xml.
     *
     * @param  bundles An XMLPlainGenerator that contains the predefined bundle xml.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setPredefinedBundles(Object bundles) throws JspException
    {
        setDynAttribute("predefinedBundles", bundles, Object.class);
    }




   public void setAddInfoMode(String name) throws JspException
   {
       setDynAttribute("addInfoMode", name, String.class);
   }

   /**
     * Sets the name of the variable which will hold the generated javascript code
     * needed to control a tab menu.
     */
    public void setVarJscript(String varName)
    {
       varJscript = varName;
    }

   /**
     * Starts procesing of this tag.
     *
     */
    public int doStartTag() throws JspException
    {
       super.doStartTag();
       bundleDef = (BundleDef) getDynAttribute("bundleDef");
       mode = (String)getDynAttribute("addInfoMode");
       bundleDataBag = (BundleBeanBag) getDynAttribute("bundleDataBag");
       Object data = (Object)getDynAttribute("predefinedBundles");


       try {

           if ( data != null) {
               if (data instanceof XMLPlainGenerator) {
                   predefinedBundles = (XMLPlainGenerator) data;
               }
               else if (data instanceof Document) {
                   predefinedBundles = new XMLPlainGenerator((Document) data);
               }
               else if (data instanceof String) {
                   predefinedBundles = new XMLPlainGenerator((String)data);
               }
               else
                   throw new JspTagException("Invalid type [" + data.getClass() +"] passed in for predefinedBundles attribute. Must be of type String,Document, or XMLPlainGenerator.");
           }
       }
       catch (Exception e ) {
           throw new JspException(e.getMessage());
       }

       predefinedBundleName = (String)getDynAttribute("predefinedBundleName");


       if (bundleDef == null || mode == null) {
         throw new JspException("bundleDef is null or addInfoMode is null" );
       }

       if (NEEDED_MODE.equalsIgnoreCase(mode) && (predefinedBundles == null|| predefinedBundleName == null)) {
         throw new JspException("When in needed mode properties PredefinedBundles or PredefinedBundleName can not be null. " );
       }
       else if (ALL_MODE.equalsIgnoreCase(mode) && bundleDataBag == null) {
         throw new JspException("When in all mode property BundleDataBag can not be null. " );
       }

       // inputFiels contains [componentDefID, list of info fields,...]
       List additionalInfoFields = null;
       boolean isCount = false;
       if (NEEDED_MODE.equalsIgnoreCase(mode))
           additionalInfoFields = loadNeeded();
       else if (ALL_MODE.equalsIgnoreCase(mode)) {
         isCount = true;
           additionalInfoFields = loadAll();
       }
       else
          throw new JspTagException("Invalid mode [" + mode +"] passed in for addInfoMode attribute. Must be either needed or all.");

        // generate html
       StringBuffer html = new StringBuffer();
       try {
        if (additionalInfoFields != null && additionalInfoFields.size() > 0) {
          Iterator it = additionalInfoFields.iterator();
          // loop thru each component def/ list of additional field(s)
          String selectedSupplier = null;
          while (it.hasNext()) {
            String id = (String) it.next(); // componentDefID
            selectedSupplier = getCurrentFieldValue(id, SUPPLIER);


             List allFields = (List) it.next(); // info fields for additional info
            // loop thru a list of additional fields
            if (allFields != null && allFields.size() > 0) {
              // create Component def id section with count if it's "all" mode
              boolean occurrenceExist = createSVCHtml(id, html, isCount);
             for (int i = 0; i < allFields.size(); i++) {
                ServiceField fld = (ServiceField) allFields.get(i);
                boolean linked = false;
                boolean linkOccurrenceExist = false;
                // check if it's linked field.
                if (linkFieldMap != null &&
                    linkFieldMap.containsKey(fld)) {
                  linked = true;
                  linkOccurrenceExist = ((LinkField)linkFieldMap.get(fld)).occurrenceExist;
                  if (selectedSupplier == null || selectedSupplier.length() ==0)
                    selectedSupplier = ((LinkField)linkFieldMap.get(fld)).value;
                  else
                     ((LinkField)linkFieldMap.get(fld)).setValue(selectedSupplier);
                }
                createInfoHtml(id, fld, html, linked, occurrenceExist, linkOccurrenceExist); // create info sections
              }
              html.append(SEP_HTML); // seperator line
           }
          }
        }
      } catch (Exception e ) {
        throw new JspTagException("doStartTag got exception: " + e.getMessage());
      }


        // setting var for generated html and dynamic java script
          if ( varExists() && html.length() > 0)  {
            setVarAttribute(html.toString());
           }
           if (html != null && html.length() > 0) {
             // create java script(s) for link field
             String javaScript = createJScript();
             if (javaScript != null)
                 VariableSupportUtil.setVarObj(varJscript, javaScript, scope, this.pageContext);
           }

        return SKIP_BODY;
     }

     private List loadAll() throws JspException
     {
       List inputFields = new ArrayList();
       infoCounts = new HashMap();
       try {
         Iterator compIter = bundleDef.getComponents().iterator();
         while ( compIter.hasNext() ) {
            ComponentDef cdef = (ComponentDef) compIter.next();
            // get current, max and min occurrence and save at infoCounts
            getInfoCount(cdef);
              // Process info fields
            List inputs = retrieveAllInfoFields(cdef.getAddlInfoFields(), cdef.getID());

              if (inputs != null && inputs.size() > 0) {
                inputFields.add(cdef.getID());
                inputFields.add(inputs);
              }
          }
        } catch (Exception e ) {
          throw new JspTagException("loadAll got exception: " + e.getMessage());
        }
        return inputFields;
     }


    private String getCurrentFieldValue(String serviceId, String name)
    {
        String value = null;

        if (bundleDataBag !=null && bundleDataBag.exists(serviceId + "(0)")) {


              value = bundleDataBag.getBean(serviceId + "(0)").getHeaderValue(name);
        }
        else if ( predefinedBundles != null) {
            try {
                value = predefinedBundles.getValue(predefinedBundleName +"." + PREDEFINED_SC
                                                       + "." +  serviceId +"." + name);
            }
            catch (MessageException e) {
            }
        }

        return value;

    }



     private List loadNeeded() throws JspException
     {


       List inputFields = new ArrayList();
       try {

         Iterator compIter = bundleDef.getComponents().iterator();
         Node scParent = null;
         if ( predefinedBundles != null)
             scParent = predefinedBundles.getNode(predefinedBundleName +"." + PREDEFINED_SC);

         while ( compIter.hasNext() ) {
            ComponentDef cdef = (ComponentDef) compIter.next();
            int count = 0;
            if ( bundleDataBag != null)
                count = bundleDataBag.getBeanCount(cdef.getId());
            else if ( scParent != null && predefinedBundles.exists(scParent, cdef.getId())) {
                String countStr = predefinedBundles.getValue(scParent, cdef.getId() +"." + PREDEFINED_COUNT);
                count = Integer.parseInt(countStr);
            }

            if (log.isDebugEnabled())
                log.debug("Have service [" + cdef.getId()+"] with count [" + count+"]");


            if (count < 1) continue;

              // Process info fields
            List inputs = retrieveInfoBagOrPredef(cdef.getAddlInfoFields(), cdef.getID(), bundleDataBag, predefinedBundles, scParent);



              if (log.isDebugEnabled() )
                  log.debug("Created info fields : " + inputs);

              if (inputs != null && inputs.size() > 0) {
                inputFields.add(cdef.getID());
                inputFields.add(inputs);
              }
          }
       } catch (Exception e ) {
         throw new JspTagException("loadNeeded() encountered problem: " + e.getMessage());
       }
       return inputFields;
    }

    private void getInfoCount(ComponentDef componentDef) throws JspException
    {
      if (componentDef == null) {
        return;
      }
      List counts = new ArrayList();
      // store current, min and max occurrence counts
      if (bundleDataBag == null)
        counts.add(new Integer(0));
      else
        counts.add(new Integer(bundleDataBag.getBeanCount(componentDef.getID())));

      counts.add(new Integer(componentDef.getMinOccurs()));
      counts.add(new Integer(componentDef.getMaxOccurs()));

      infoCounts.put(componentDef.getID(), counts);
    }


    private List retrieveAllInfoFields(List parts, String id)
    {

      List allFields = new ArrayList();

      NFBean bean = null;

      supplierExists = false;
      tpExists = false;
      supplierField = null;


      for (int i = 0; i < parts.size(); i++) {
        ServiceField field = (ServiceField) parts.get(i);
        String value =null;



        addInfoField(id, field, allFields, null);
      }
      return allFields;
    }

    private List retrieveInfoBagOrPredef(List parts, String id,
                                             BundleBeanBag bundleDataBag, XMLGenerator predefBd, Node predefNode)
    {

      List allFields = new ArrayList();


      NFBean bean = null;

      if ( bundleDataBag != null)
          bean = bundleDataBag.getBean(id + "(0)");

      supplierExists = false;
      tpExists = false;
      supplierField = null;

      for (int i = 0; i < parts.size(); i++) {
        ServiceField field = (ServiceField) parts.get(i);
        String value = null;
        if (bean != null)
            value = bean.getHeaderValue(field.getXMLPath());
        else if ( predefBd != null) {

            try {
                value = predefBd.getValue(predefNode, id +"." + field.getXMLPath());
            }
            catch (MessageException e) {
            }
        }


        if ( !StringUtils.hasValue(value) ) {
          // mode is needed and  info field doesn't exist
          addInfoField(id, field, allFields, value);
        }
      }
      return allFields;
    }

    private void addInfoField(String id, ServiceField field, List allFields, String value)
     {

       if (field.getXMLPath().indexOf(SUPPLIER) != -1) {
         // check if TP exists
         if (tpExists) {
           return;
         }
         supplierExists = true;
         supplierField = field;
       }
       if (field.getXMLPath().indexOf(TRADING_PARTNER) != -1) {
         // check if Supplier exists
         if (supplierExists) {
           // take supplier out of allFields, infoFields and linkFieldMap
           String xmlPath = supplierField.getXMLPath();
           if (linkFieldMap != null && linkFieldMap.containsKey(supplierField)) {
             // if Supplier is linked
             LinkField linkField = (LinkField) linkFieldMap.get(supplierField);
             (linkField.linkFields).remove(ServletConstants.NF_FIELD_HEADER_PREFIX +
                                           id + "." + xmlPath);
             if ( (linkField.linkFields).size() == 1) {
               // take out from linkFieldMap
               linkFieldMap.remove(supplierField);
               linkField = null;
             }
           }
           else {
             // take out from the infoFields
             if (infoFields != null && infoFields.containsKey(supplierField))
               infoFields.remove(supplierField);
           }
           allFields.remove(allFields.indexOf(supplierField));
           supplierExists = false;
           supplierField = null;

         }
         tpExists = true;
       }
       allFields.add(field);
       createInfoField(id, field, value);
    }

    private void createInfoField(String id, ServiceField field, String value)
     {
       if (infoFields == null)
         infoFields = new HashMap();

       if (infoFields.containsKey(field)) {
         // if info field object exists, make this field a link field
         createLinkField(id, field, field.getDataType().getType(), value);
       }
       else {
         infoFields.put(field,
                        ServletConstants.NF_FIELD_HEADER_PREFIX
                        + id + "." + field.getXMLPath() );
       }
     }

     private void createLinkField(String id, ServiceField field, int dataType, String value)
     {
       if (linkFieldMap == null)
         linkFieldMap = new HashMap();

       List list = null;
       LinkField linkField = null;
       if (linkFieldMap.containsKey(field)) {
         linkField = (LinkField) linkFieldMap.get(field);
         (linkField.linkFields).add(ServletConstants.NF_FIELD_HEADER_PREFIX + id + "." + field.getXMLPath());
         linkField.setOccurrence(getServiceComponentCount(id));
         linkField.setValue(value);
      }
       else {
         list = new ArrayList();
         String attributeName = (String) infoFields.get(field);
         list.add( attributeName); // previous attribute name.
         list.add(ServletConstants.NF_FIELD_HEADER_PREFIX + id + "." + field.getXMLPath());
         linkFieldMap.put(field, new LinkField(list, dataType, (getServiceComponentCount(attributeName) > 0 ||  getServiceComponentCount(id) > 0) , value));
       }

     }
     private int getServiceComponentCount(String name) {
       if (name.indexOf(ServletConstants.NF_FIELD_HEADER_PREFIX) != -1) {
         name = name.substring(ServletConstants.NF_FIELD_HEADER_PREFIX.length(),
                                 name.indexOf("."));
       }
       if ( bundleDataBag != null)
           return bundleDataBag.getBeanCount(name);
       else
           return 0;

     }
     // create additional info fields HTML block
     private void createInfoHtml(String id, ServiceField field, StringBuffer html,
                                 boolean linked, boolean occurrenceExist,
                                 boolean linkOccurrenceExist)
     throws Exception {
       // sanity check
       if (field == null || id == null)
         return;

       if (field.getDataType().getType() == DataType.TYPE_ENUMERATED)
         createSelectHtml(id, field, html, linked, occurrenceExist, linkOccurrenceExist);
       else
         createTextHtml(id, field, html, linked, occurrenceExist);
     }

    private boolean createSVCHtml(String id, StringBuffer html, boolean isCount)
    {
      boolean occurrenceExist = false;
      html.append("\n<tr><td width=\"100%\" class=\"FormHeading\">" + id + "</td></tr>\n");
      if (isCount && infoCounts != null) {
       List list = (List) infoCounts.get(id);
       if (list != null && list.size() > 0) {
         if (((Integer)list.get(0)).intValue() > 0)
           occurrenceExist = true;
         createCountHtml(id, list, html);
        }
     }
     return occurrenceExist;

   }
   private void createHtmlInfoBlock(String className,
                                    String fullName,
                                    String displayName,
                                    String attributeName,
                                    String value,
                                    int valueSize,
                                    String onChange,
                                    StringBuffer html,
                                    boolean occurrenceExist)
   {
     html.append(HTML_HEAD);
     html.append("<tr><td class=\"" + className + "\">");
     html.append(" <a TABINDEX=\"-1\" onMouseOut=\"return displayStatus('');\"");
     html.append(" onMouseOver=\"return displayStatus('" + fullName + "');\">");
     html.append( displayName + " </a></td><td>");
     html.append("<INPUT TYPE=\"text\" name=\""
                 + attributeName + "\"");
     if (value != null)
       html.append(" value=\"" + value +"\"");
     if (occurrenceExist)
       html.append(" disabled");
     if (valueSize > 0)
       html.append(" size=\"" + valueSize +"\"  maxlength=\"" + valueSize + "\"");
     if (onChange != null)
       html.append(" onChange=\"" + onChange + "\"");
     html.append("/>");
     html.append(HTML_TAIL);
   }

   private void createCountHtml(String id, List list, StringBuffer html)
   {
     createHtmlInfoBlock(TagConstants.CLASS_FIELD_LABEL_REQUIRED,
                        "Count",
                        "Occurrence Count",
                        ServletConstants.NF_FIELD_HEADER_PREFIX + id + ".Count",
                        ((Integer)list.get(0)).toString(),
                        COUNT_SIZE,
                        "return ComponentCount(this," + list.get(1) + "," + list.get(2)+ ");",
                        html,
                        false);
   }

    private void createSelectHtml(String id, ServiceField field, StringBuffer html, boolean linked,
                                  boolean occurrenceExist, boolean linkOccurrenceExist)
    throws Exception
    {

      String currentValue = getCurrentFieldValue(id, field.getXMLPath());

      html.append(HTML_HEAD);
      // TODO: how to know if a feild is optional or required
      html.append("<tr><td class=\"FieldLabelRequired\">");
      html.append(" <a TABINDEX=\"-1\" onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('" + field.getFullName() + "');\">");
      html.append(field.getDisplayName() + "</a>");
      html.append("</td>");
      html.append("<td>");
      html.append("<select name=\"" + ServletConstants.NF_FIELD_HEADER_PREFIX
                  + id + "." + field.getXMLPath() + "\"");
      // if occurrence exists, we do not allow the user to edit any field
      if (occurrenceExist)
        html.append(" disabled");
      if (linked)
        html.append(" onChange=\"" + field.getXMLPath() + "(this)\"");

      html.append(">");

      boolean isTP = (field.getXMLPath().indexOf(TRADING_PARTNER) == -1) ? false : true;
      boolean isSupplier = (field.getXMLPath().indexOf(SUPPLIER) == -1) ? false : true;
      if (isTP || isSupplier) {
        // for TradingPartnerName or Supplier, we want to get options from
        // TradingPartnerRelationship object
        // special handling for trading partner names
        String cid = CustomerContext.DEFAULT_CUSTOMER_ID;


        if (pageContext.getSession() != null) {
          SessionInfoBean sBean = (SessionInfoBean) pageContext.getSession().
              getAttribute(ServletConstants.SESSION_BEAN);
          cid = sBean.getCustomerId();
        }

        // get Trading Partner names
          TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(
              cid);


          if (tpr != null) {
           Collection partnerNames = null;



            if (isTP)
              partnerNames = tpr.getEnabledTradingPartners(id);
            else
              partnerNames = tpr.getEnabledGatewaySuppliers(id);

            partnerNames = filterTradingPartners(id, partnerNames);

            if (partnerNames != null) {
              Iterator it = partnerNames.iterator();
	      String supplierName = null;
              while (it.hasNext()) {
               String name = (String) it.next();
               if (!occurrenceExist && !linkOccurrenceExist) {
                 html.append("<option ");
                 if (name.equalsIgnoreCase(currentValue)) html.append("selected ");
                   
		 if ( isTP )
		 {
			 html.append(" value=\"" + name + "\">" +
                             getAliasName(field, tpr.getSupplierName(name) + " (" + name + ")" ) + "</option>");
		 }
		 else
		 {
			 html.append(" value=\"" + name + "\">" +
                             getAliasName(field, name ) + "</option>");
		 }
			 
               }
               else if ( name.equalsIgnoreCase(currentValue) &&
                         (occurrenceExist || (!occurrenceExist && linkOccurrenceExist) ) ) {

		       if ( isTP )
		       {
                 		html.append("<option selected ").append(" value=\"" + name + "\">" +
                            		getAliasName(field, tpr.getSupplierName(name) + " (" + name + ")" ) + "</option>");
		       }
		       else
		       {
			       html.append("<option selected ").append(" value=\"" + name + "\">" +
                            		getAliasName(field, name ) + "</option>");
		       }	  
               }
              }
            }
            else {
              throw new JspTagException("There is no TradingPartner setup for customer [" + cid + "]");
            }
          }
          else {
            throw new JspTagException("There is no TradingPartnerRelationship setup for customer [" + cid + "]");
          }
      }
      else {
        // if it's not TradingPartnerName or Supplier, we want to get options from
        // component def
        // not trading partner
        DataType dataType = field.getDataType();
        if (dataType != null) {
          OptionSource options = dataType.getOptionSource();
          if (options != null) {
            String[] optValues = options.getOptionValues();
            String[] displayValues = options.getDisplayValues();
            for (int i = 0; i < displayValues.length; i++) {
              html.append("<option value=\"" + optValues[i] + "\">" +
                          displayValues[i] + "</option>");
            }
          }
        }
      }

      html.append("</select>");
      html.append(HTML_TAIL);
    }

    /**
     * Filters the supplier/trading-partner list returned from the db query,
     * based on the constraint defined in the Supplier/TradingPartner field
     * of the ServiceComponent's AdditionalInfo section.  If the field contains
     * Options in it's DataType, then this filter kicks in.  Otherwise the no
     * filter is performed.
     *
     * @param  componentId      Component id.
     * @param  tradingPartners  Trading-partner list returned from db query.
     *
     * @return  A new (or original) list of trading-partner names.
     */
    private Collection filterTradingPartners(String componentId, Collection tradingPartners)
    {
        Collection newTradingPartners = tradingPartners;

        if ((tradingPartners != null) && !tradingPartners.isEmpty())
        {
            ComponentDef componentDef         = bundleDef.getComponent(componentId);

            List         additionalInfoFields = componentDef.getAddlInfoFields();

            Iterator     infoIterator         = additionalInfoFields.iterator();

            while (infoIterator.hasNext())
            {
                Field field = (Field)infoIterator.next();

                // There should only be either Supplier or TradingPartner, but not both.
                // The first one wins.

                if ((field.getXMLPath().indexOf(SUPPLIER) != -1) || (field.getXMLPath().indexOf(TRADING_PARTNER) != -1))
                {
                    DataType     dataType     = field.getDataType();

                    OptionSource optionSource = dataType.getOptionSource();

                    if (optionSource != null)
                    {
                        String[] optionValues = optionSource.getOptionValues();

                        if (optionValues.length > 0)
                        {
                           newTradingPartners       = new TreeSet();

                           List     optionValueList = getList(optionValues);

                           Iterator tpIterator      = tradingPartners.iterator();

                           while (tpIterator.hasNext())
                           {
                               String tpName = (String)tpIterator.next();

                               if (optionValueList.contains(tpName))
                               {
                                   if (log.isDebugEnabled())
                                   {
                                       log.debug("filterTradingPartners(): Adding supplier/trading-partner [" + tpName + "] to the new list ...");
                                   }

                                   newTradingPartners.add(tpName);
                               }
                           }
                        }
                    }

                    break;
                }
            }
        }

        return newTradingPartners;
    }

    /**
     * Utility method which takes in a String array and returns List object.
     * It is assumed that stringArray argument is not null.
     *
     * @param  stringArray  String[] object.
     *
     * @return  List object.
     */
    private List getList(String[] stringArray)
    {
        List list = new LinkedList();

        for (int i = 0; i < stringArray.length; i++)
        {
            list.add(stringArray[i]);
        }

        return list;
    }

    private String getAliasName(Field sField, String value)
    {
      String alias = null;
      AliasDescriptor aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);
      if (aliasDescriptor != null) {
        alias = aliasDescriptor.getAlias(pageContext.getRequest(),
                                         sField.getFullXMLPath(), value, true);
      }
      return alias;
    }

    private void createTextHtml(String id, ServiceField field, StringBuffer html, boolean linked, boolean occurrenceExist)
    {
      String onChange = null;
      if (linked)
        onChange =  field.getXMLPath() + "(this)";

      createHtmlInfoBlock(TagConstants.CLASS_FIELD_LABEL_REQUIRED,
                         field.getFullName(),
                         field.getDisplayName(),
                         ServletConstants.NF_FIELD_HEADER_PREFIX + id + "." + field.getXMLPath(),
                         null,
                         0,
                         onChange,
                         html,
                         occurrenceExist);
    }

   private void createSeperatorHtml(StringBuffer html)
   {
     html.append(
         "\n<tr><td width=\"100%\" class=\"FormHeading\"><br></td></tr>\n");
   }


     private String createJScript()
    {
      if  (linkFieldMap == null || linkFieldMap.size() == 0)
        return null;
      StringBuffer jscript = new StringBuffer();
      Iterator it = linkFieldMap.keySet().iterator();
      // loop thru all linked fields
      while (it.hasNext()) {
        ServiceField field = (ServiceField)it.next();
        LinkField linkField = (LinkField) linkFieldMap.get(field);

        if (linkField.dataType == DataType.TYPE_ENUMERATED)
          createSelectFunc(jscript, linkField.linkFields , field.getXMLPath());
        else
          createTextFunc(jscript, linkField.linkFields, field.getXMLPath());

      }
       return jscript.toString();
    }

    private void createSelectFunc(StringBuffer jscript, List list, String fieldID)
   {
     jscript.append("function " + fieldID + "(fld) \n{ ");
     for (int i = 0; i < list.size(); i++)
     {
             jscript.append("\nfield = document.forms[0].elements[\"" + list.get(i) + "\"];");
             jscript.append("\nfield.selectedIndex = fld.selectedIndex;");
     }
     jscript.append("\n}");
   }

   private void createTextFunc(StringBuffer jscript, List list, String fieldID)
  {
    jscript.append("function " + fieldID + "(fld) \n{ ");
    jscript.append("val = fld.value;");
    for (int i = 0; i < list.size(); i++)
    {
            jscript.append("\nfield = document.forms[0].elements[\"" + list.get(i) + "\"];");
            jscript.append("\nfield.value = val;");
    }
    jscript.append("\n}");
  }

    public void release()
    {
       super.release();

       bundleDataBag = null;
       bundleDef = null;
       varJscript = null;
       mode = null;
       supplierExists = false;
        tpExists = false;
        supplierField = null;
       if (infoFields != null) {
         infoFields.clear();
         infoFields = null;
       }
       if (linkFieldMap != null ) {
         linkFieldMap.clear();
         linkFieldMap = null;
       }
     }

     /**
      * Class that represents each tab on the tab menu.
      */
     private static class LinkField
     {
        public List linkFields;
        public int dataType;
        public boolean occurrenceExist;
        public String value;
        public LinkField(List linkFields, int dataType,  boolean occurrenceExist,
                         String value)
        {
           this.linkFields = linkFields;
           this.dataType = dataType;
           this.occurrenceExist = occurrenceExist;
           this.value = value;
        }
        public void setValue(String aValue) {
          if ((this.value == null || this.value.length() == 0)
            && aValue != null && aValue.length() > 0)
            this.value = aValue;
       }
        public void setOccurrence(int count) {
          if (!occurrenceExist && count > 0)
            occurrenceExist = true;
       }
        public String toString()
        {
            return dataType + ":" + dataType + ":" + " linkFields : " + linkFields.toString()
                + " occurrenceExist: " + occurrenceExist + " value: " + value;
        }


     }


}

/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.tag.message;

import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.ServletException;

import org.w3c.dom.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.tag.message.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.tag.VariableSupportUtil;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.webgui.manager.svcmeta.*;
import com.nightfire.webgui.manager.tag.OrderBeanInfo;
import com.nightfire.webgui.core.ServletConstants;




/**
 * This tag loads a ServiceComponentBean  and modifiers for a full detail xml
 * (Refer to GetFullOrderDetailHandler
 *
 */
public class LoadFullOrderBeanTag extends ContainerBeanTagBase implements ManagerServletConstants
{
   /**
   * The xml document to add
   * to the session buffer.
   */
  private BundleDef bundleDef = null;
  private ComponentDef componentDef = null;
  private String orderInfoVar = null;
  private Iterator beanIter = null;
  private XMLPlainGenerator serviceComponent = null;

   /**
    * Setter method for variable that will hold the current orderbeaninfo object.
    * Used to pass scbean information to inner tags.
    *
    * @param  orderIfo The variable name of the object.
    *
    * @exception  JspException  Thrown when an error occurs during processing.
    */
   public void setVarOrderInfo(String orderInfo) throws JspException
   {
       orderInfoVar = orderInfo;
   }

   /**
   * The bundle def to access
   */
  public void setBundleDef(Object bundleDef) throws JspException
  {
      setDynAttribute("bundleDef", bundleDef, BundleDef.class);

  }

  /**
  * The xml to use
  */
 public void setData(Object data) throws JspException
 {
     setDynAttribute("data", data, Object.class);
 }

 public NFBean createBean() throws JspException
 {

   bundleDef = (BundleDef) getDynAttribute("bundleDef");
   Object data = (Object) getDynAttribute("data");



   try {

     if (data != null) {
       if (data instanceof XMLPlainGenerator) {
         serviceComponent = (XMLPlainGenerator) data;
       }
       else if (data instanceof Document) {
         serviceComponent = new XMLPlainGenerator( (Document) data);
       }
       else if (data instanceof String) {
         serviceComponent = new XMLPlainGenerator( (String) data);
       }
       else
         throw new JspTagException("Invalid type [" + data.getClass() + "] passed in for serviceComponent attribute. Must be of type String,Document, or XMLPlainGenerator.");
     }

     String svcName = serviceComponent.getNode("0").getNodeName();
     componentDef = bundleDef.getComponent(svcName);

   }
   catch (Exception e) {
     throw new JspException(e.getMessage());
   }



   // sanity check
   if (bundleDef == null | serviceComponent == null) {
     String errorMessage =
         "The bundle definition or service component data is null.";
     log.error(errorMessage);
     throw new JspException(errorMessage);
   }

   // build a list of service component bean and modifier beans
   // that need to be created by the inner classes
   beanIter = createSCBeans().iterator();

   // set the initial order info object before the first loop of the body.
   if (beanIter.hasNext()) {
     OrderBeanInfo entry = (OrderBeanInfo) beanIter.next();
     VariableSupportUtil.setVarObj(orderInfoVar, entry, scope, pageContext);
   }

   // no need to initialize the service component bean since servicecomponentBean
   // will be created in addBean() method.
   return null;
 }

 public int doAfterBody() throws JspException
 {
     // for each OrderBeanInfo object we let the inner tag create the bean
       if (beanIter.hasNext()) {
         OrderBeanInfo entry = (OrderBeanInfo) beanIter.next();
         VariableSupportUtil.setVarObj(orderInfoVar, entry, scope, pageContext);
         return EVAL_BODY_AGAIN;
       } else {
           // finished created all the sc beans.
           // load history info
           loadHistory();
          return SKIP_BODY;
       }

 }
 private String getAttribute(Node node, String path)
     throws com.nightfire.framework.message.MessageException
 {
   if (serviceComponent.exists(node, path)) {
     return serviceComponent.getValue(node, path);
   }
   return null;
 }

 private List createSCBeans() throws JspException
 {
   List scBeans = null;
   try {

     // get the service component name, order node and modifier node
       Node svNode = serviceComponent.getNode("0");

     String svComponentName = svNode.getNodeName();
     if (!serviceComponent.exists(svComponentName + "." + ManagerServletConstants.ORDER_NODE)) {
       throw new JspException("There's no order node in the service component [" + svComponentName + "]");
     }
     Node orderNode = serviceComponent.getNode(svComponentName + "." + ManagerServletConstants.ORDER_NODE);

     Node modifierNode = null;

     if (serviceComponent.exists(svComponentName +"." + ManagerServletConstants.MODIFIER_NODE))
         modifierNode = serviceComponent.getNode(svComponentName + "." +  ManagerServletConstants.MODIFIER_NODE);


     if (modifierNode != null ) {
       // modifier node exists, create a list of two items so body will be executed twice.
       scBeans = new ArrayList(2);
     }
     else {
       scBeans = new ArrayList(1);
     }

     // add OrderBeanInfo for service component bean
     if (orderNode != null) {


       // will create a ServiceComponentBean
         ServiceComponentBean bean = new ServiceComponentBean();
         bean.decompose(serviceComponent, orderNode);
         bean.setServiceType(svComponentName);

         scBeans.add(new OrderBeanInfo(bean,componentDef,  svComponentName));

     }

     // add OrderBeanInfo for modifier bean
     if (modifierNode != null) {
         ModifierBean bean = new ModifierBean();
         bean.decompose(serviceComponent, modifierNode);
         bean.setServiceType(svComponentName);

         scBeans.add(new OrderBeanInfo(bean,componentDef, svComponentName));

     }

     return scBeans;
   }
   catch (Exception e) {
     String errorMessage =
         "createSCBeans(): Failed to create a bag for service component and modifier bean:\n" +
         e.getMessage();
     log.error(errorMessage);
     throw new JspException(errorMessage);
   }
 }

 public void addBean(String beanName, NFBean bean)
 {
   try {
     if (bean instanceof ServiceComponentBean) {
       setBean(bean);
     }
     else if (bean instanceof ModifierBean) {
       ( (ServiceComponentBean) getBean()).setModifierBean( (ModifierBean)
           bean);
     }
   }
   catch (Exception e) {
     String errorMessage = "addBean(): Failed to addBean\n" + e.getMessage();
     log.error(errorMessage);
   }
 }

 private void addNewItems(List items, List newItems)
 {
   if (newItems != null && newItems.size() > 0 && items != null) {
     for(int i = 0; i < newItems.size(); i++) {
       Object item = (Object) newItems.get(i);
       if (!items.contains(item)) {
         items.add(item);
       }
     }
   }
 }

 private void loadHistory() throws JspException
 {
   try
   {
     ServiceComponentBean bean = (ServiceComponentBean)getBean();
     String svcName = serviceComponent.getNode("0").getNodeName();

     // if there is no history then just return.. Otherwise if we have history
     // and the componentdef has no history info then throw an excepion
     // else continue and build history.
     // This allows components that do not have history to not require
     // having history info.
     if ( !serviceComponent.exists(svcName + ".History") ) {
         return;
     }
     else if ( componentDef.getHistoryInfo() == null) {
       throw new JspException("componentDef [" + componentDef.getID() + "] has no history info.");
     }

     Node[] transactions = serviceComponent.getChildren(svcName + ".History");
     if (transactions == null || transactions.length == 0) {
       throw new JspException("componentDef [" + componentDef.getID() + "] has no transactions under history info.");
     }


     List historyFields = new ArrayList();
     historyFields.addAll(componentDef.getHistoryInfo().getSummaryFields());
     addNewItems(historyFields, componentDef.getHistoryInfo().getQueryFields());



     Map orderTransactions = new TreeMap();
     for (int i = 0; i < transactions.length; i++) {
       OrderTransaction orderTransaction = new OrderTransaction();
       // add fields in OrderTransaction
       for (int j = 0; j < historyFields.size(); j++) {
         String fieldId = ((MessagePart) historyFields.get(j)).getXMLPath();

         if (serviceComponent.exists(transactions[i], fieldId))
             orderTransaction.addField(fieldId,
                                       serviceComponent.getValue(transactions[i], fieldId));

        }

       // add supplier if we have not already added it
       // this removes the requirement of always having to define this field
       // in query fields.
        if (orderTransaction.getFields().get(ServletConstants.SUPPLIER)== null &&
            serviceComponent.exists(transactions[i], ServletConstants.SUPPLIER) ) {
            orderTransaction.addField(ServletConstants.SUPPLIER,
                                      serviceComponent.getValue(transactions[i], ServletConstants.SUPPLIER));
        }

        // add version if we have not already added it
        // this removes the requirement of always having to define this field
        // in query fields.
        if (orderTransaction.getFields().get(ServletConstants.INTERFACE_VER)== null &&
            serviceComponent.exists(transactions[i], ServletConstants.INTERFACE_VER) ) {
            orderTransaction.addField(ServletConstants.INTERFACE_VER,
                                      serviceComponent.getValue(transactions[i], ServletConstants.INTERFACE_VER));
        }

       // add to Map
       orderTransaction.setId(String.valueOf(i));
       orderTransactions.put(new Integer(i), orderTransaction);
     }
     bean.setHistory(orderTransactions);
   }
   catch (Exception e)
      {
        String errorMessage = "loadHistory encounter errors. \n" + e.getMessage();
        log.error(errorMessage);
        throw new JspException(errorMessage);
      }
 }

    public void release()
    {
        super.release();
        componentDef = null;
        bundleDef = null;
        beanIter = null;
        orderInfoVar = null;
        serviceComponent = null;
    }

}

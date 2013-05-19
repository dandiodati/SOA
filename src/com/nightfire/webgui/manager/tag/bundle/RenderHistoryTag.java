/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.bundle;

import  java.util.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;

import  org.w3c.dom.*;

import  com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.meta.Field;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.manager.beans.ServiceComponentBean;
import com.nightfire.webgui.manager.svcmeta.*;
import com.nightfire.webgui.core.svcmeta.ActionDef;
import com.nightfire.webgui.core.tag.navigation.support.*;
import com.nightfire.security.tpr.TradingPartnerRelationship;



/**
 * Generates the html for the history of the order.
 */

public class RenderHistoryTag extends VariableTagBase implements ManagerServletConstants, ServletConstants
{

    public static final String VIEW_ACTION = "view";
    public static final String VIEW_DISPLAY = "View";

    private ComponentDef  serviceDef;

    private ServiceComponentBean bean;

    private boolean showActions = true;

    public String contextPath = null;

   // a variable to place the generated javascript code
    private String varJscript;

    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    private AliasDescriptor aliasDescriptor;
    private boolean templateSupport = false;


    /**
     * Setter method for the service-component definition object.
     *
     * @param  service  Service-component definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setService(Object service) throws JspException
    {
        setDynAttribute("service", service, ComponentDef.class);
    }

    public void setBean(Object bean) throws JspException
    {
        setDynAttribute("bean", bean, ServiceComponentBean.class);
    }

    public void setShowActions(String bool) throws JspException
    {
        setDynAttribute("showActions", bool, String.class);
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



        serviceDef = (ComponentDef)getDynAttribute("service");
        bean       =  (ServiceComponentBean)getDynAttribute("bean");

        showActions = StringUtils.getBoolean((String)getDynAttribute("showActions"), true);

        contextPath = TagUtils.getWebAppContextPath(pageContext);


       if (this.serviceDef == null) {
            String errorMessage = "The service-component definition object passed in via attribute [service] is null.";
            log.error(errorMessage);
            throw new JspException(errorMessage);
        }

       if (bean == null) {
            String errorMessage = "The bean passed in via attribute [bean] is null.";
            log.error(errorMessage);
            throw new JspException(errorMessage);
        }

        StringBuffer output       = new StringBuffer();
        StringBuffer jscript       = new StringBuffer();


        Map historyData = bean.getHistory();

        // if there is no history just return and do nothing
        if (historyData == null)
            return SKIP_BODY;


        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);


        HistoryInfo info = serviceDef.getHistoryInfo();
        List summaryFields = info.getSummaryFields();
        List detailFields  = info.getQueryFields();

        // add an extra column for view/actions
        int cellWidth = 100/(summaryFields.size() + 1);

        renderHistoryHeader(output, summaryFields, cellWidth);

        try {
          renderHistoryRows(output, historyData, summaryFields, jscript, cellWidth, info);
            // set html code
            if ( varExists() )
                setVarAttribute(output.toString());
            else
                pageContext.getOut().println(output.toString());
        }
        catch (Exception e) {
            throw new JspException(e);
        }

        // set jscript code
        VariableSupportUtil.setVarObj(varJscript, jscript.toString(), scope, pageContext);

        return SKIP_BODY;
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

    private void renderHistoryHeader(StringBuffer output, List summaryFields, int cellWidth)
    {


        Iterator headerIter = summaryFields.iterator();


        output.append("<!-- history table -->");


        output.append("<tr style=\"height:35\"><td><img src=\"").append(contextPath).append("/images/shim.gif\" height=\"35\" width=\"1\"/></td></tr>");

        output.append("<tr>");

        output.append("<td align=\"center\">");
        output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"95%\">");
        output.append("<tr>");
        output.append("<td class=\"HistoryBar\">Order History</td>");
        output.append("</tr>");
        output.append("</table>");
        output.append("</td>");
        output.append("</tr>");
        output.append("<!-- end order history bar -->");

           output.append("<!-- order history header -->");

            output.append("<tr>");
            output.append("<td align=\"center\">");

            output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"90%\">");
            output.append("<tr>");

            output.append("<td>");
            output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"2\" width=\"100%\">");
            output.append("<tr>");
            output.append("<th onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('Message Actions');\" class=\"HistorySummary\" style=\"width:").append(cellWidth).append("%\">Actions</th>");

        while (headerIter.hasNext()) {
            Field header = (Field) headerIter.next();

            output.append("<th onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('").append(header.getFullName()).append("');\" class=\"HistorySummary\" style=\"width:").append(cellWidth).append("%\">");
            output.append(TagUtils.escapeHtmlSpaces(header.getDisplayName())).append("</th>");
        }

        output.append("</tr>");
        output.append("</table>");
        output.append("</td>");
        output.append("</tr>");

        output.append("<!-- end order history headers -->");



    }

    private void renderHistoryRows(StringBuffer output, Map historyData, List summaryFields, StringBuffer jscript, int cellWidth, HistoryInfo info)
    throws Exception
    {
        Iterator iter = historyData.entrySet().iterator();

        if (showActions)
            createActionCallBackJscript(jscript,info);



        while (iter.hasNext() ) {
            Map.Entry entry = (Map.Entry) iter.next();
            Integer key = (Integer) entry.getKey();
            OrderTransaction orderTrans = (OrderTransaction) entry.getValue();

            output.append("<!-- start order history rows -->");
            output.append("<tr>");
            output.append("<td><table cellpadding=\"0\" cellspacing=\"0\" border=\"1\" width=\"100%\">");

            output.append("<tr>");
            output.append("<td class=\"HistorySummary\" style=\"width:").append(cellWidth).append("%\">");

            output.append("<a class=\"ServiceActionButton\" href=\"javascript:performHistoryServiceAction('");
            output.append(VIEW_ACTION).append("', '").append("OrderHistory.").append(orderTrans.getId()).append("','").append(VIEW_DISPLAY).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View History Details')\">");
            output.append("<img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(contextPath).append("/images/ServiceActionButton.gif\">View</a>");


            if (showActions) {
                output.append("&nbsp&nbsp;");
                createActions(output, orderTrans, jscript, info);
            }
            output.append("</td>");

            //output.append("&nbsp;&nbsp;");

            Map dataValues = orderTrans.getFields();

            Iterator sumIter = summaryFields.iterator();



            while(sumIter.hasNext()) {
                Field f = (Field) sumIter.next();
                String value = (String)dataValues.get(f.getXMLPath());



                if (!StringUtils.hasValue(value)) {
                    value = "&nbsp;";
                } else
                    value = value = aliasDescriptor.getAlias(pageContext.getRequest(), f.getFullXMLPath(), value, true);


                output.append("<td class=\"HistorySummary\" style=\"width:").append(cellWidth).append("%\">");
                output.append(value).append("</td>");
            }

            output.append("</tr></table></td>");
            output.append("</tr>");
            output.append("<!-- end order history row -->");


        }



    }



    private void createActions(StringBuffer output, OrderTransaction orderTrans, StringBuffer jscript, HistoryInfo info)
    throws Exception
    {
        String       bundleStatus = bean.getParentBag().getHeaderValue(STATUS_FIELD);

        

        List allowedActions = orderTrans.getActions();

        if (allowedActions == null || allowedActions.size() == 0) {
          // get allowable actions
            Field field = info.getTypeField();
            if (field == null) {
              String errorMessage = "componentDef [" + serviceDef.getID() +
                  "] has no default message type.";
              log.error(errorMessage);
              throw new JspException(errorMessage);
            }

            Map fieldMap = orderTrans.getFields();
            if (fieldMap == null) {
              return;
            }
            allowedActions = new ArrayList();

            // add all service component bean's allowable actions
            List actions = serviceDef.getActions(bean, (String) fieldMap.get(field.
                getXMLPath()));
            if (actions != null && actions.size() > 0)
              allowedActions.addAll(actions);

            ModifierBean modifier = bean.getModifierBean();
            if (modifier != null) {
              // add modifier allowable actions if modifier bean exists
              addNewItems(allowedActions, serviceDef.getActions(modifier,
                  (String) fieldMap.get(field.getXMLPath())));
            }
            if (allowedActions != null && allowedActions.size() > 0) {
              orderTrans.setActions(allowedActions);
            }
        }

        // holds final list of actions that user can perform
        List actions = new ArrayList();



        if (allowedActions != null) {
            Iterator actionIter = allowedActions.iterator();

            while (actionIter.hasNext()) {
                String actionName = (String)actionIter.next();

                boolean authorized = false;

                try {
                    authorized = ServletUtils.isAuthorized(pageContext.getSession(), actionName);
                }
                catch (Exception e) {
                    log.error("Failed during authentication: " + e);
                }


                if (authorized) {

                    ActionDef aDef = (ActionDef)info.getActionDef(actionName);

                    if (aDef != null) {

                        NVPair allowableAction = new NVPair(aDef.getDisplayName(), aDef.getActionName());

                        if ( log.isDebugEnabled() )
                            log.debug("Action Name [" + allowableAction.name+ "], value [" + allowableAction.value+"]");

                       templateSupport = serviceDef.getTemplateSupport();
                       String interfaceVer=null;
                       String activeVer=null;
                       if(templateSupport)
                       {
                        log.debug("ServiceDef ID: " + serviceDef.getId());
                        interfaceVer= bean.getParentBag().getBean(serviceDef.getId()+ "(0)").getHeaderValue("InterfaceVersion");

                        log.debug("InterfaceVersion: " + interfaceVer);

                        String cid =  CustomerContext.getInstance().getCustomerID();
                        TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(cid);

                        String service = bean.getParentBag().getBean(serviceDef.getId()+ "(0)").getHeaderValue("ServiceType");
                        log.debug("Service: " + service);

                        if(service.equals("NP") || service.equals("CSR") || service.equals("AV"))
                        {
                        String supplier = bean.getParentBag().getBean(serviceDef.getId()+ "(0)").getHeaderValue("Supplier");
                        log.debug("Supplier: " + supplier);

                        activeVer = tpr.getMostRecentVersion(supplier, service);
                        log.debug("CID: " + cid);
                        }

                        else
                        activeVer = interfaceVer;

                        log.debug("Active InterfaceVersion: " + activeVer);
                       }
                       if(templateSupport)
                       {
                         if(!(allowableAction.value.equals("create-template")) || ((allowableAction.value.equals("create-template")) && (interfaceVer.equals(activeVer))))
                         actions.add(allowableAction);
                       }
                        else
                          actions.add(allowableAction);
                    }

                }

            }

        }

        if (actions.size() > 0)
            createActionMenu(output, jscript, actions, orderTrans.getId());
    }


    private void createActionCallBackJscript(StringBuffer jscript, HistoryInfo info)
    {

        jscript.append("<SCRIPT language=\"Javascript\">");
        jscript.append("function ").append("processHistoryActionEvent(action, tid) {");
        jscript.append("if (action == '') {}");

        Iterator actionIter = info.getAllAllowableActions().iterator();

        while (actionIter.hasNext()) {

            ActionDef aDef = (ActionDef)actionIter.next();
            String actionName = aDef.getActionName();




            String redirectPage = aDef.getRedirectPage();
            java.awt.Dimension newWin = aDef.getNewWindow();


            jscript.append("else if (action == '").append(actionName).append("') {");
            jscript.append("performHistoryServiceAction('");

            jscript.append(actionName);
            jscript.append("', tid");

            jscript.append(", '");

            jscript.append(serviceDef.getDisplayName());


            jscript.append("',");
            jscript.append(aDef.getEditRequired());


            if ( StringUtils.hasValue(redirectPage)) {
                jscript.append(",'");
                jscript.append(redirectPage);
                jscript.append("'");
            }


            if (newWin != null) {
                jscript.append(",");
                jscript.append(newWin.getWidth()).append(", ");
                jscript.append(newWin.getHeight());
            }

            jscript.append(");");
            // close else if
            jscript.append("}");


        }

        jscript.append("}");
        jscript.append("</SCRIPT>");


    }


    private void createActionMenu(StringBuffer output, StringBuffer jscript, Collection actions, String rowId)
    {
        StringBuffer rootMenu = new StringBuffer("<img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(contextPath).append("/images/ServiceActionButton.gif\"/>Actions");
        MenuLinkElement elem = new MenuLinkElement(rootMenu.toString(), "ServiceActionButton", "OrderHistory." + rowId, actions);
        output.append(elem.getHTML());
    }



    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintainance to be performed.
     */
    public void release()
    {
        super.release();

        serviceDef = null;

        bean = null;

        showActions = true;

        contextPath = null;

        varJscript = null;

        aliasDescriptor = null;
    }

}

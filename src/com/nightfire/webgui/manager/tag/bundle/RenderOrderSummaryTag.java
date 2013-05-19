/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.mgrcore.im.query.QueryEngine;
import com.nightfire.webgui.core.AliasDescriptor;
import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.webgui.core.meta.Field;
import com.nightfire.webgui.core.svcmeta.ActionDef;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.VariableSupportUtil;
import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.webgui.core.tag.message.support.HtmlElement;
import com.nightfire.webgui.core.tag.navigation.support.MenuLinkElement;
import com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.webgui.manager.beans.InfoBodyBase;
import com.nightfire.webgui.manager.beans.ServiceComponentBean;
import com.nightfire.webgui.manager.svcmeta.ComponentDef;


/**
 * RenderOrderSummaryTag is responsible for drawing the order summary.
 */

public class RenderOrderSummaryTag extends VariableTagBase implements
		ManagerServletConstants, ServletConstants {

	public static final String VIEW_DISPLAY = "View";

	protected ComponentDef serviceDef;

	protected InfoBodyBase bean;

	private boolean showActions = true;

	private boolean showTitleBar = true;

	private boolean templateSupport = true;

	public String contextPath = null;

	private XMLGenerator messageData;

	/**
	 * The HTMLElement that holds an empty line for to provide padding for the
	 * HtmlLayout class.
	 */
	public static final HtmlElement SKIP_LINE = new HtmlElement(1, true, false);

	// a variable to place the generated javascript code
	private String varJscript;

	/**
	 * The aliasDescriptor which can be used for field value alias lookup.
	 */
	private AliasDescriptor aliasDescriptor;

	/**
	 * Setter method for the service-component definition object.
	 *
	 * @param service
	 *            Service-component definition object.
	 *
	 * @exception JspException
	 *                Thrown when an error occurs during processing.
	 */
	public void setService(Object service) throws JspException {
		setDynAttribute("service", service, ComponentDef.class);
	}

	public void setBean(Object bean) throws JspException {
		setDynAttribute("bean", bean, InfoBodyBase.class);
	}

	public void setShowActions(String bool) throws JspException {
		setDynAttribute("showActions", bool, String.class);
	}

	public void setShowTitleBar(String bool) throws JspException {
		setDynAttribute("showTitleBar", bool, String.class);
	}

	/**
	 * Sets the name of the variable which will hold the generated javascript
	 * code needed to control a tab menu.
	 */
	public void setVarJscript(String varName) {
		varJscript = varName;
	}

	/**
	 * Redefinition of doStartTag() in VariableTagBase. This method processes
	 * the start tag for this instance.
	 *
	 * @exception JspException
	 *                Thrown when an error occurs during processing.
	 *
	 * @return SKIP_BODY if there is no exception.
	 */
	public int doStartTag() throws JspException {
		super.doStartTag();

		aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(
				ServletConstants.ALIAS_DESCRIPTOR,
				pageContext.APPLICATION_SCOPE);

		serviceDef = (ComponentDef) getDynAttribute("service");
		bean = (InfoBodyBase) getDynAttribute("bean");

		showTitleBar = StringUtils.getBoolean(
				(String) getDynAttribute("showTitleBar"), true);
		showActions = StringUtils.getBoolean(
				(String) getDynAttribute("showActions"), true);



        templateSupport = serviceDef.getTemplateSupport();

		contextPath = TagUtils.getWebAppContextPath(pageContext);

		log.debug("Order Type MetaData Name: "
				+ serviceDef.getBundleDef().getMetaDataName());



      
        String orderType = getOrderType(serviceDef.getBundleDef()
				.getMetaDataName());
		String currentStatus = bean.getHeaderValue(STATUS_FIELD);
		String orderId = bean.getHeaderValue("OrderID");
		String supplier = "";
		String interfaceVer= bean.getHeaderValue("InterfaceVersion");
		if(!currentStatus.equalsIgnoreCase(SAVED_STATUS) && orderId.equals("")){
			
			supplier = bean.getHeaderValue(SUPPLIER);
		}
		log.debug("Order Type : " + orderType);
		log.debug("Supplier Type : " + supplier);
		log.debug("ReqType : " + serviceDef.getID());
		log.debug("InterfaceVersion : " + interfaceVer);

		XMLMessageGenerator xmlmsg;
		try {
			xmlmsg = new XMLMessageGenerator(ManagerServletConstants.BODY);
			xmlmsg.setValue(ManagerServletConstants.ORDER_TYPE, orderType);
			xmlmsg.setValue(ManagerServletConstants.SORTBY, "CreateDate");
			if(orderType.equals(ManagerServletConstants.ICP_PORTIN)){
				xmlmsg.setValue(ManagerServletConstants.INFO_SUPPLIER, bean
									.getHeaderValue("TradingPartnerName"));
			}else {
				xmlmsg.setValue(ManagerServletConstants.INFO_SUPPLIER, supplier);
				xmlmsg.setValue(ManagerServletConstants.INTERFACEVERSION,interfaceVer);
			}
			xmlmsg.setValue(ManagerServletConstants.REQUEST_TYPE, serviceDef
					.getID());

			log.debug("MessageData : " + xmlmsg.generate());
		} catch (Exception e) {
			log.error("", e);
			throw new JspException(e);
		}

		log.debug("TempSupport : " + templateSupport);
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

		StringBuffer output = new StringBuffer();
		StringBuffer jscript = new StringBuffer();

		String serviceType = serviceDef.getID();
		log.debug("Service Type : " + serviceType);
		if (showTitleBar){
			if(templateSupport){
				createTitleBar(output, jscript, showActions, xmlmsg);
			}else {
				createTitleBar(output, jscript, showActions);
			}
		}
			

		List summary = serviceDef.getSummaryFields();

		for (int i = 0; i < summary.size(); i++) {
			log.debug("Summary Fields : " + summary.get(i));
		}

		int cellWidth = 100 / summary.size();
		createSummaryFields(output, summary, bean, cellWidth);

		try {

			// set html code
			if (varExists())
				setVarAttribute(output.toString());
			else
				pageContext.getOut().println(output.toString());
		} catch (Exception e) {
			throw new JspException(e);
		}

		// set jscript code
		VariableSupportUtil.setVarObj(varJscript, jscript.toString(), scope,
				pageContext);

		return SKIP_BODY;
	}

	private String getOrderType(String metaDataName) {

		if (metaDataName.equals(ManagerServletConstants.LSR_NP_SVC)) {
			metaDataName = ManagerServletConstants.LSR_ORDER;
		} else if (metaDataName.equals(ManagerServletConstants.LSR_PREORDER_SVC)) {
				metaDataName = ManagerServletConstants.LSR_PRE_ORDER;
		} else if (metaDataName.equals(ManagerServletConstants.ICP_PORTIN_SVC)) {
			metaDataName = ManagerServletConstants.ICP_PORTIN;
		} else if (metaDataName.equals(ManagerServletConstants.SOA_PORTIN_SVC)) {
			metaDataName = ManagerServletConstants.SOA_PORTIN;
		}else if(metaDataName.equals(ManagerServletConstants.ICP_PORTIN_SOA_PORTIN_SVC)){
			if(serviceDef.getID().equals(ManagerServletConstants.ICP_PORTIN)){
				metaDataName = ManagerServletConstants.ICP_PORTIN;
			}else if(serviceDef.getID().equals(ManagerServletConstants.SOA_PORTIN)){
				metaDataName = ManagerServletConstants.SOA_PORTIN;
			}
		}else if(metaDataName.equals(ManagerServletConstants.LSR_NP_SOA_SVC)){
			if(serviceDef.getID().equals(ManagerServletConstants.LSR_NUMBER_PORT)){
				metaDataName = ManagerServletConstants.LSR_ORDER;
			}else if(serviceDef.getID().equals(ManagerServletConstants.SOA_PORTIN)){
				metaDataName = ManagerServletConstants.SOA_PORTIN;
			}
		}
		return metaDataName;
	}
	
	protected void createSummaryFields(StringBuffer output, List summaryFields,
			InfoBodyBase bean, int cellWidth) throws JspException {

		Iterator iter = summaryFields.iterator();

		output.append("<!-- order order summary header -->");
		output.append("<tr>");

		output.append("<td>");
		output
				.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
		output.append("<tr>");
		output.append("<td class=\"ServiceSummaryFirstColumn\"></td>");
		output.append("<td>");

		output
				.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"2\" width=\"100%\">");
		output.append("<tr>");

		// create header fields
		while (iter.hasNext()) {
			Field sField = (Field) iter.next();
			output
					.append("<th onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('");
			output.append(sField.getFullName()).append(
					"');\" class=\"ServiceSummary\" style=\"width:").append(
					cellWidth).append("%\">").append(
					TagUtils.escapeHtmlSpaces(sField.getDisplayName())).append(
					"</th>");
		}

		output.append("</tr></table></td>");
		output.append("<td class=\"ServiceSummaryFirstColumn\"></td>");
		output.append("</tr>");
		output.append("<!-- end order summary headers -->");

		// create field values

		output.append("<!-- start order summary values -->");
		output
				.append("<tr><td class=\"ServiceSummaryFirstColumn\" align=\"center\"></td>");

		output
				.append("<td><table cellpadding=\"0\" cellspacing=\"0\" border=\"1\" width=\"100%\">");
		output.append("<tr>");

		iter = summaryFields.iterator();

		while (iter.hasNext()) {
			Field sField = (Field) iter.next();

			String value = TagUtils.getSummaryFieldValue(pageContext, bean,
					sField);

			if (!StringUtils.hasValue(value)) {
				if (sField.getXMLPath().equals(STATUS_FIELD))
					value = "New";
				else
					value = "&nbsp;";
			} else
				value = aliasDescriptor.getAlias(pageContext.getRequest(),
						sField.getFullXMLPath(), value, true);

			log.debug("Alias Value : " + value);

			output.append("<td class=\"ServiceSummary\" style=\"width:")
					.append(cellWidth).append("%\">").append(value).append(
							"</td>");

			if (log.isDebugEnabled())
				log.debug("Adding value [" + value + "] for field ["
						+ sField.getID() + "]");
		}

		output.append("</tr></table></td>");
		output.append("</tr>");
		output.append("</table>");
		output.append("</td>");
		output.append("</tr>");
		output.append("<!-- end order summary values -->");

	}

	protected void createTitleBar(StringBuffer output, StringBuffer jscript,
			boolean showActions,
			XMLMessageGenerator xmlmsg) throws JspException {

		String bundleStatus = ((ServiceComponentBean) bean).getParentBag()
				.getHeaderValue(STATUS_FIELD);

		output.append("<!-- order action bar -->");
		String currentStatus = bean.getHeaderValue(STATUS_FIELD);
                log.debug("Current Status is : " + currentStatus);
		String orderId = bean.getHeaderValue("OrderID");

		String[] optionValues = null;

		String[] optionDisplayValues = null;

		optionValues = queryOptionList("get-template-names", xmlmsg);
		
		output.append("<tr>");
		output.append("<td>");
		output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
		output.append("<tr>");
		output.append("<td class=\"ServiceCaptionBar\">&nbsp;&nbsp;&nbsp;Order&nbsp;Summary</td>");
		output.append("<td class=\"ServiceCaptionBar\">&nbsp;</td>");
		output.append("<td class=\"ServiceCaptionBar\" style=\"text-align: center;\">");
				
		if(!currentStatus.equalsIgnoreCase(SAVED_STATUS) && !currentStatus.equalsIgnoreCase(INVALID_STATUS) && orderId.equals(""))
		{			
			output.append("Template :<select name=\"templateDropdown\" size=\"1\" onChange=\"templateSelected(this)\">");
			optionDisplayValues = optionValues;
			output.append("<option value=\"None\"/>");
			output.append("None");
			output.append("</option>").append(NL);
	
			if (optionValues != null) {
				for (int i = 0; i < optionValues.length; i++) {
					output.append("<option value=\"");
	
					output.append(optionValues[i]);
	
					output.append("\">");
	
					// Set the display name if one exists. Otherwise use the option
					// value
					// as the display name.
	
					String displayValue = optionValues[i];
	
					if ((optionDisplayValues != null)
							&& (optionDisplayValues.length > i)) {
						displayValue = optionDisplayValues[i];
					}
	
					output.append(displayValue);
	
					output.append("</option>").append(NL);
				}
			}
	
				output.append("</select></td>");		
		}
		
		output.append("<td class=\"ServiceCaptionBar\" style=\"text-align: right;\">");
		output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('");
		output.append(VIEW_ACTION).append("', '").append(bean.getId()).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Order Details')\">");
		output.append("<img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(contextPath).append("/images/ServiceActionButton.gif\">View</a>");

		if (showActions) {
			output.append("&nbsp&nbsp;");
			createActions(output, jscript, bundleStatus, bean);
		}

		output.append("&nbsp&nbsp;&nbsp;&nbsp;");
		output.append("</td>");
		output.append("</tr>");
		output.append("</table>");
		output.append("</td>");
		output.append("</tr>");
		output.append("<!-- end order action bar -->");
		
	}
	
	protected void createTitleBar(StringBuffer output, StringBuffer jscript, boolean showActions) throws JspException 
    {

        String       bundleStatus = ((ServiceComponentBean)bean).getParentBag().getHeaderValue(STATUS_FIELD);

        output.append("<!-- order action bar -->");
        
        output.append("<tr>");
        
        output.append("<td>");
        output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
        output.append("<tr>");
        output.append("<td class=\"ServiceCaptionBar\">&nbsp;&nbsp;&nbsp;Order&nbsp;Summary</td>");
        output.append("<td class=\"ServiceCaptionBar\">&nbsp;</td>");
        output.append("<td class=\"ServiceCaptionBar\" style=\"text-align: right;\">");

        output.append("<a class=\"ServiceActionButton\" href=\"javascript:performServiceAction('");
        output.append(VIEW_ACTION).append("', '").append(bean.getId()).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Order Details')\">");
        output.append("<img class=\"ServiceActionActionButton\" border=\"0\" src=\"").append(contextPath).append("/images/ServiceActionButton.gif\">View</a>");

        
        if (showActions) {
            output.append("&nbsp&nbsp;");
            createActions(output, jscript, bundleStatus, bean);
        }
        
        output.append("&nbsp&nbsp;&nbsp;&nbsp;");
        
        output.append("</td>");
        output.append("</tr>");
        output.append("</table>");
        output.append("</td>");
        output.append("</tr>");
        output.append("<!-- end order action bar -->");

    }
	protected String getJScriptProcessActionEventMethod() {
		return "processOrderSummaryActionEvent";
	}

	protected ActionDef getActionDef(String id) {
		return serviceDef.getActionDef(id);
	}

	protected void createActions(StringBuffer output, StringBuffer jscript,
			String bundleStatus, InfoBodyBase bean) throws JspException {

		List actions = new ArrayList();

		try {
			jscript.append("<script language=\"Javascript\">");
			jscript.append("function ").append(
					getJScriptProcessActionEventMethod()).append("(action) {");

			// do nothing case
			jscript.append("if (action == '') { }");

			// add the view worklist and admin worklist buttons
			// if the user has permission
			// and this is not a saved bundle
			
			Set actionSet = new HashSet();
			log.debug("Bundle Status: " + bundleStatus);
			if (StringUtils.hasValue(bundleStatus) && !bundleStatus.equals(SAVED_STATUS) && !bundleStatus.equals(INVALID_STATUS)) {
                actionSet.add(VIEW_WORKITEMS_ACTION);
				actionSet.add(VIEW_ADMIN_WORKITEMS_ACTION);
			}
                
                if(bean.getAllowableActions()!=null){
                    actionSet.addAll(bean.getAllowableActions()) ;
                }

			// build a jscript code to handle processing the selection of an
			// action menu item.
			// this method will have to be called from the jscript function
			// triggered from a menu selection.
			// Here is processing, A menu action is selected, and it calls a
			// jscript method
			// That jscript method is then used to call this generated jscript
			// method for processing the click event.
			//
		
				Iterator actionIter = actionSet.iterator();

				while (actionIter.hasNext()) {
					String actionName = (String) actionIter.next();

					if (ServletUtils.isAuthorized(pageContext.getSession(),
							actionName)) {

						ActionDef aDef = (ActionDef) getActionDef(actionName);

						if (aDef != null) {

							NVPair allowableAction = new NVPair(aDef
									.getDisplayName(), aDef.getActionName());

							if (log.isDebugEnabled())
								log.debug("Action Name ["
										+ allowableAction.name + "], value ["
										+ allowableAction.value + "]");

							createEventHandlerCode(aDef, jscript);

							actions.add(allowableAction);
						}

					}

				}

		

			jscript.append("}");
			jscript.append("</SCRIPT>");

		} catch (Exception e) {
			String errorMessage = "Failed to authorize the user via NF Security Service: "
					+ e.getMessage();

			log.error(errorMessage);

			throw new JspTagException(errorMessage);
		}

		if (actions.size() > 0)
			createActionMenu(output, jscript, actions);

	}

	private void createEventHandlerCode(ActionDef aDef, StringBuffer jscript) {

		String redirectPage = aDef.getRedirectPage();
		java.awt.Dimension newWin = aDef.getNewWindow();

		String actionName = aDef.getActionName();

		jscript.append("else if (action == '").append(actionName)
				.append("') {");
		jscript.append("performServiceAction('");

		jscript.append(actionName);
		jscript.append("', '");

		jscript.append(bean.getId());
		jscript.append("', '");

		jscript.append(serviceDef.getDisplayName());

		jscript.append("',");
		jscript.append(aDef.getEditRequired());

		if (StringUtils.hasValue(redirectPage)) {
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

		jscript.append("}");

	}

	protected void createActionMenu(StringBuffer output, StringBuffer jscript,
			List actions) {
		StringBuffer rootMenu = new StringBuffer(
				"<img class=\"ServiceActionActionButton\" border=\"0\" src=\"")
				.append(contextPath).append(
						"/images/ServiceActionButton.gif\"/>Actions");
		MenuLinkElement elem = new MenuLinkElement(rootMenu.toString(),
				"ServiceActionButton", "OrderSummary", (Collection) actions);

		output.append(elem.getHTML());

	}

	/**
	 * This method uses QueryEngine to query the database for the options list
	 * values.
	 *
	 * @param queryCriteria
	 *            The criteria portion of the RepositoryManager's query
	 *            category-criteria.
	 * @param message
	 *            XMLGenerator instance of the message data.
	 *
	 * @return String[] representation of the queried option list from the
	 *         database.
	 */
	protected String[] queryOptionList(String queryCriteria,
			XMLMessageGenerator message) {
		try {
			if (log.isDebugEnabled()) {
				log
						.debug("queryOptionList(): Querying db for option list values using:\n  queryCriteria: "
								+ queryCriteria
								+ "\n  message:\n"
								+ message.generate());
			}

			QueryEngine.QueryResults queryResult = QueryEngine.executeQuery(
					queryCriteria, message.getDocument());

			if (log.isDebugEnabled()) {
				log.debug("queryOptionList(): Query result obtained is:\n"
						+ queryResult.getResultsAsString());
			}

			if (queryResult != null) {
				XMLGenerator resultGenerator = new XMLPlainGenerator(
						queryResult.getResultsAsDOM());

				int optionCount = queryResult.getResultCount();

				if (optionCount > 0) {
					String[] optionList = new String[optionCount];

					for (int i = 0; i < optionCount; i++) {
						String optionValue = resultGenerator
								.getValue("DataContainer.Data(" + i + ").0");

						if (log.isDebugEnabled()) {
							log
									.debug("queryOptionList(): Resulting option value at index ("
											+ i + ") is: " + optionValue);
						}

						optionList[i] = optionValue;
					}

					return optionList;
				}
			}

			log
					.debug("queryOptionList(): Query result is empty.  Resorting to other options ...");
		} catch (Exception e) {
			log
					.error("queryOptionList(): Failed to obtain the option list from the database via QueryEngine:\n"
							+ e.getMessage());
		}

		return null;
	}


	/**
	 * returns the xml message data associated with this request or response.
	 *
	 * @return XMLGenerator used to access the xml message.
	 */
	public XMLGenerator getMessageData() {
		return messageData;
	}

	/**
	 * Redefinition of release() in VariableTagBase. This method is invoked
	 * after doEndTag(), allowing any state maintainance to be performed.
	 */
	public void release() {
		super.release();

		serviceDef = null;

		bean = null;

		showActions = true;

		contextPath = null;

		varJscript = null;

		aliasDescriptor = null;
	}

}

/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.query;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.svcmeta.ActionDef;
import com.nightfire.webgui.core.beans.XMLBean;
import com.nightfire.webgui.core.beans.SessionInfoBean;
import com.nightfire.webgui.gateway.svcmeta.ServiceDef;

import javax.servlet.jsp.*;
import javax.servlet.http.*;
import javax.servlet.ServletException;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.security.tpr.TradingPartnerRelationship;

import java.util.*;


/**
 * Creates the status search results based on the query response.
 *
 */
public class OrderHistoryTag extends VariableTagBase
{

    /**
     * The service definition object for which the history needs
     * to be build
     */
    public ServiceDef service;
    
    /**
     * Prefix and Suffix that need to be used in form name
     * for every row
     */
    public String formNamePrefix, formNameSuffix;

    public String containerPath;
    /**
     * hidden fields to be created.
     */
    public List hiddenFieldsListPerRow;

    /**
     * Custom value for a field indicating if field is Anchor and value of this
     * this field would be the Anchoring value
     */
     public static final String ANCHOR_CUSTOM_VALUE = "anchor";

     public static final String ACTIONTYPE_ORDER_LEVEL = "order-level";

    /**
     * Semi Colon character
     */
     public static final String SEMI_COLON_CHAR = ";";

    private String contextPath;

    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    private AliasDescriptor aliasDescriptor;


     /**
     * Object which hold the current xml message data with its values.
     * Used to obtain the values of fields and determine number
     * of repeating structures.
     *
     */
    private XMLGenerator message;

    private XMLBean requestBean, responseBean;


    /* Setters */
    public void setService(ServiceDef service) throws JspException
    {
        this.service = (ServiceDef) TagUtils.getDynamicValue("service", service, ServiceDef.class, this, pageContext);
    }

    public void setContainerPath(String containerPath) throws JspException 
    {
        this.containerPath = (String) TagUtils.getDynamicValue("containerPath", containerPath, String.class, this, pageContext);
    }

    public void setFormNamePrefix(String formNamePrefix) throws JspException
    {
        this.formNamePrefix = (String) TagUtils.getDynamicValue("formNamePrefix", formNamePrefix, String.class, this, pageContext);
    }

    public void setFormNameSuffix(String formNameSuffix) throws JspException
    {
        this.formNameSuffix = (String) TagUtils.getDynamicValue("formNameSuffix", formNameSuffix, String.class, this, pageContext);
    }

    public void setHiddenFieldsListPerRow(List hiddenFieldsListPerRow) throws JspException
    {
        this.hiddenFieldsListPerRow = (List) TagUtils.getDynamicValue("hiddenFieldsListPerRow", hiddenFieldsListPerRow, List.class, this, pageContext);
    }

    public void setResponseBean(XMLBean responseBean) throws JspException
    {
        this.responseBean = (XMLBean) TagUtils.getDynamicValue("responseBean", responseBean, XMLBean.class, this, pageContext);
    }

    public void setRequestBean(XMLBean requestBean) throws JspException
    {
        this.requestBean = (XMLBean) TagUtils.getDynamicValue("requestBean", requestBean, XMLBean.class, this, pageContext);
    }

    /**
     * Starting point.
     *
     * @return int
     * @throws JspException on error
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, PageContext.APPLICATION_SCOPE);

        StringBuffer buf = new StringBuffer();

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        contextPath = request.getContextPath();

        try
        {

            // Start of the table that will enclose the order history
            buf.append("<table class=\"List\" width=\"98%\" cellspacing=\"0\" cellpadding=\"0\">");
            buf.append("<col width=\"1\"/>");

            // build history column header using the service def object's "Transaction Summary Fields"
            buildOrderHistoryHeader(buf, getHeaderList ());

            //build history result rows of records
            buildOrderHistoryRows(buf);

            buf.append("</table>");

            if ( varExists() ) setVarAttribute(buf.toString() );
            else pageContext.getOut().println(buf.toString());

        }
        catch (Exception e)
        {
           String err= "OrderHistoryTag: Failed to create html : " + e.getMessage();
           log.error(err);
           log.error("",e);
           throw new JspTagException(err);
        }
        return SKIP_BODY;
    }

    /**
     * Returns the names of the headers of the columns
     * It uses the "Transaction Summary Fields" of the passed
     * service object.
     *  
     * @return list object
     */
    private List getHeaderList()
    {
        List headers = new LinkedList();
        final String VIEW = "View";
        final String ACTION = "Action";
        // first column header on order history page will always be "View"
        headers.add(VIEW);

        /**
            Action header would be also be displayed for users
            with reviewer role only. If there are no action
            available, text 'viewable only' would be displayed.

            Only display the action column if this is a
            transaction-level action type.
         */
        if (!service.getActionsType().equals(ACTIONTYPE_ORDER_LEVEL))
            headers.add(ACTION);

        // now add all the "Transaction Summary Fields" of the service as headers
        for (Object txField : service.getTransactionSummaryFields())
        {
            headers.add(((MessagePart)txField).getDisplayName());
        }

        return headers;
    }


    /**
     * creates hidden fields.
     * 
     * @param buf String buffer in which html appends
     * @param fields list of the fields for which hidden html fields need to be created
     * @param idx index number of the node in message
     */
     private void createHiddenFields(StringBuffer buf, List fields, int idx) 
     {
         if (fields != null && fields.size() > 0)
         {
             // the values of the hidden fields
             // must present in the response bean body source
             for (Object field : fields)
             {
                 createHiddenField (buf, ((MessagePart) field).getId(), extractNodeValue(message, containerPath, ((MessagePart) field).getXMLPath(),idx));
             }
         }
     }

    /**
     * creates hidden field.
     *
     * @param buf String buffer in which html appends
     * @param fieldName name of the field
     * @param fieldValue value of the field
     */
     private void createHiddenField(StringBuffer buf, String fieldName, String fieldValue)
     {
         // fieldValue can be empty string
         if (StringUtils.hasValue(fieldName) && fieldValue != null)
         {
             buf.append("<input type=\"hidden\" value=\"").append(fieldValue).append("\" name=\"").append(ServletConstants.NF_FIELD_PREFIX);
             buf.append(TagUtils.stripIdSuffix(fieldName)).append("\"/>");
         }
     }


    /**
     * Builds the order history header which displays
     * the name of each column.
     *
     * @param buf the buffer to append the html to.
     * @param fields The display fields to add to the results header.
     * @throws MessageException on error
     */
    private void buildOrderHistoryHeader(StringBuffer buf, List fields) throws MessageException
    {
        buf.append("<tr class=\"ListHeading\">");

        // Display column headings.
        // loop over all fields to be displayed and build the field results header
        for (Object field : fields) {
            buf.append("<th>").append(field).append("</th>");
        }
    }


    /**
     * Builds the rows of records.
     *
     * @param buf the html buffer to add html to
     * @throws MessageException on error
     * @throws FrameworkException on error
     * @throws ServletException on error
     * @throws JspTagException on error
     * @throws JspException on error
     */
    private void buildOrderHistoryRows(StringBuffer buf) throws FrameworkException, ServletException, JspException
    {
        // Iterate through all history items that came
        // back in the query result and process each.

        // Nodes Constants
        final String MESSAGETYPE = "MessageType";
        final String MESSAGETYPE_ACK = "ack";
        final String MESSAGETYPE_NACK = "negack";
        final String DIRECTION = "Direction";
        final String DIR_OUT = "OUT";
        final String STATUS = "Status";
        final String SERVICE = "Service";
        final String ACTIONTYPE = "actionType";

        // CSS Constants
        final String ROW_CSS_1 = "trClass";
        final String ROW_CSS_2 = "ListItemOddRow";
        final String CSS_ACTION = "class=\"Action\"";

        // PageContext Attributes Constants
        final String DISPLAY997SUPPORT = "display997Support";
        final String CURRENT_STATE = "CurrentState";
        final String LOCKING_SUPPORTED = "lockingSupported";
        final String OWN_LOCK = "ownLock";
        final String LOCK_OWNER = "lockOwner";
        final String SERVICE_MODIFY_PERMISSION = "serviceModifyPermission";
        final String SESSIONBEAN = "SessionBean";
        final String SUPPLIER = "Supplier";

        // Variable Constants
        final String VIEW_ONLY = "Viewable Only";
        final String UPDATE_REMARK = "update-remark";
        final String UPDATE_ESC = "update-esc";
        final String MORE = "More";
        final String MAX_RCV = "MaxReceived";
        final String ACTION = "Action";
        final String ORDER_HISTORY = "OrderHistory";
        final String NBSP = "&nbsp;";
        final String COLON = ":";

        String firstRowActionType = "";

        // this variable must already set in pageContext in order-history.jsp
        boolean display997Support = obtainBooleanValue (pageContext.getAttribute(DISPLAY997SUPPORT), false);

        boolean moreFlag;

        if (responseBean == null)
        {
            String err = "OrderHistoryTag: responseBean attribute is null.";
            log.error(err);
            throw new JspTagException(err);
        }

        message = ((XMLGenerator) responseBean.getBodyDataSource());
        int count = message.getChildCount(containerPath);

        for (int i = 0; i < count; i++)
        {
            moreFlag = Boolean.FALSE;
/*
            Check whether the current Record is a outbound 997 record
            or a regular record.  The columns to be displayed for outbound
            997 are different than those for regular rows.
            The following check is used to determine whether the record is
            outbound 997: The MessageType is 'ack' or 'negack' and the
            direction is 'OUT'
*/
            String txMessageType = extractNodeValue (message, containerPath, MESSAGETYPE, i);
            String txDirection = extractNodeValue(message, containerPath, DIRECTION, i);
            String txMessageStatus = extractNodeValue(message, containerPath, STATUS, i);
            String rowService = extractNodeValue(message, containerPath, SERVICE, i);

            boolean isOutbound997Rec = display997Support
                    && (txMessageType.equals(MESSAGETYPE_ACK) || txMessageType.equals(MESSAGETYPE_NACK))
                    && txDirection.equals(DIR_OUT); 

            String formName = formNamePrefix + i + formNameSuffix;

            buf.append("<form name=\"").append(formName).append("\">");
            buf.append("<tr class=\"").append(i%2 == 0 ? ROW_CSS_1 : ROW_CSS_2).append("\" onMouseOver=\"highLightRow(this)\" onMouseOut=\"dehighLightRow(this)\">");

            if (!isOutbound997Rec)
            {
                buf.append("<td>");
                buf.append("<a href=\"javascript:viewDetails('").append(formName).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Detail')\">");
                buf.append("<img style=\"vertical-align:-60%\" alt=\"View Detail\" border=\"0\" src=\"").append(contextPath).append("/images/ViewButton.gif\"></a>");
                buf.append("</td>");

                /*
                Removed the security check based on ModifyPermission for
                displaying Action dropdown.  Actions would be displayed based
                on individual action-controlling-permission.  In case action-
                controlling-permission is not defined, ModifyPermission would
                be used.

                Only display the action column if this is a
                transaction-level action type.
                */
                if (!service.getActionsType().equals(ACTIONTYPE_ORDER_LEVEL))
                {
                    buf.append("<td>");
                    String serviceState = "";
                    if (pageContext.getAttribute(CURRENT_STATE) != null)
                        serviceState = (String)pageContext.getAttribute(CURRENT_STATE);
                    
                    List actions = service.getActions(serviceState, txMessageType);

                    Iterator   iterator		    = actions.iterator();
                    List	   actionOptions    = new LinkedList();

                    boolean	   lockingSupported = obtainBooleanValue (pageContext.getAttribute(LOCKING_SUPPORTED), false);

                    boolean	   ownLock          = obtainBooleanValue (pageContext.getAttribute(OWN_LOCK), false);

                    String	   lockOwner        = "";
                    if (pageContext.getAttribute(LOCK_OWNER) != null)
                        lockOwner = (String)pageContext.getAttribute(LOCK_OWNER);

                    String serviceModifyPermission = "";
                    if (pageContext.getAttribute(SERVICE_MODIFY_PERMISSION) != null)                   
                        serviceModifyPermission = (String)pageContext.getAttribute(SERVICE_MODIFY_PERMISSION);

                    boolean hasServiceModifyPermission = ServletUtils.isAuthorized (pageContext.getSession(), serviceModifyPermission);

                    // Obtaining Customer Id from CustomerContext not producing desirable results, so taking from SessionBean
                    SessionInfoBean sessionInfoBean = (SessionInfoBean)pageContext.getSession().getAttribute(SESSIONBEAN);
                    String               customerId = sessionInfoBean.getCustomerId();
                    TradingPartnerRelationship  tpr = TradingPartnerRelationship.getInstance(customerId);

                    while (iterator.hasNext())
                    {
                        ActionDef actionDef          = (ActionDef)iterator.next();
                        boolean isValidTPRRelation    = true;
                        String serviceTypeConvert = actionDef.getServiceTypeToConvert();

                        if(StringUtils.hasValue(serviceTypeConvert))
                        {
                            Map tps = tpr.getTradingPartnersForService(serviceTypeConvert);
                            isValidTPRRelation = tps.containsKey(pageContext.getAttribute(SUPPLIER));
                        }

                        String controllingPermission = actionDef.getControllingPermission();
                        boolean hasControllingPermission = false;
                        if(controllingPermission != null)
                        {
                            hasControllingPermission = ServletUtils.isAuthorized(pageContext.getSession(), controllingPermission);
                        }

                        boolean disableWithoutLock = actionDef.getDisableWithoutLock();
                        boolean shouldAddAction =
                                !service.getServiceGroupDef().getUpdateRequestFieldSupport()
                                ||
                                (service.getServiceGroupDef().getUpdateRequestFieldSupport()
                                 &&
                                 (
                                    (!actionDef.getActionName().equals(UPDATE_REMARK)
                                            && !actionDef.getActionName().equals(UPDATE_ESC))
                                    || ((actionDef.getActionName().equals(UPDATE_REMARK)
                                            || actionDef.getActionName().equals(UPDATE_ESC) ) && i == 0 )
                                 )
                                );

                        if(isValidTPRRelation)
                        {
                            if(hasControllingPermission || (controllingPermission == null && hasServiceModifyPermission))
                            {
                                if (!lockingSupported || !disableWithoutLock || (disableWithoutLock && ownLock) || !StringUtils.hasValue(lockOwner))
                                {
                                    NVPair actionOption = new NVPair(actionDef.getDisplayName(), actionDef.getActionName());
                                    if(!actionDef.getDisplayName().equals(MORE))
                                    {
                                        if (shouldAddAction)
                                        {
                                             actionOptions.add(actionOption);
                                        }
                                    }
                                }
                            }
                        }
                    }//isValidTPRRelation

                    // clearing all actions for an "in-flight" response
                    if (rowService.equals(ServletConstants.INFLIGHT))
                        actionOptions.clear();

                    if (txMessageStatus.equals(MAX_RCV))
                    {
                        moreFlag = Boolean.TRUE;
                        buf.append("<a href=\"javascript:moreSvQuery('").append(formName).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('More')\">");
                        buf.append("<img style=\"vertical-align:-60%\" alt=\"More\" border=\"0\" src=\"").append(contextPath).append("/images/more.gif\"></a>");
                    }

                    if (!moreFlag)
                    {
                        if (actionOptions != null && actionOptions.size() > 0)
                        {
                            buf.append(createDropDown ("ActionDropDown", CSS_ACTION, "", "", actionOptions, false, ""));
                            buf.append(NBSP);
                            buf.append("<a href=\"javascript:performAction('").append(formName).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('Perform Action')\">");
                            buf.append("<img style=\"vertical-align:-20%\" alt=\"Perform Action\" border=\"0\" src=\"").append(contextPath).append("/images/ETCButton.gif\"></a>");
                        }
                        else
                            buf.append(VIEW_ONLY);
                    }
                    else {
                        buf.append(VIEW_ONLY);
                    }
                    buf.append("</td>");
                }

                /*
                    Iterate through all column data of each row and
                    display each one. ("Transaction Summary Fields" will be displayed)
                */

                List txSummaryFields = service.getTransactionSummaryFields();

                ArrayList list = service.getConvertToUTC_TZ_TSFs();
                ArrayList timezoneList = service.getConvert_TZ_TSFs();

                String serverTZ = service.getTimezone_To_Convert_TZ_TSFs();

                if (!StringUtils.hasValue(serverTZ))
                    serverTZ = "UTC";

                for (Object txSummaryField : txSummaryFields)
                {
                    MessagePart col = (MessagePart) txSummaryField;
                    String colName = col.getXMLPath();
                    String colValue = extractNodeValue(message, containerPath, colName, i);
                    if(list.contains(colName) || timezoneList.contains(colName))
                    {
                        if (StringUtils.hasValue(colValue))
                            colValue = TagUtils.convertTimeZone(pageContext, colValue, TagUtils.CONVERT_TO_CLIENT_TZ, serverTZ);
                    }

                    // Save the action type of the latest record
                    if (i == 0)
                    {
                        if (colName.equals(ACTION))
                            firstRowActionType = colValue;
                    }

                    buf.append("<td>");
                    // When there is no value an nonblock space should be outputted so that the table data cell gets rendered correctly
                    if (StringUtils.hasValue(colValue))
                    {
                        String aliasKey = ORDER_HISTORY + "." + service.getServiceGroupDef().getID() + "." + colName;
                        colValue = aliasDescriptor.getAlias(pageContext.getRequest(), aliasKey, colValue, true);
                        buf.append(colValue);
                    } else buf.append(NBSP);

                    buf.append("</td>");

                }

                /*
                    Setup the form input fields required to obtain
                    the detail of each history item when an action
                    is selected.
                */

                // create hidden fields
                createHiddenFields(buf, hiddenFieldsListPerRow, i);

                // Creating "Interface Version" hidden field
                createHiddenField(buf, ServletConstants.INTERFACE_VER, extractNodeValue(message, containerPath, ServletConstants.INTERFACE_VER, i));
            }
            else
            {
                List headers = getHeaderList();

                List ackTxSummaryFields = service.getAckTransactionSummaryFields();
                for (int j = 0; j < ackTxSummaryFields.size(); j++)
                {
                    MessagePart col = (MessagePart) ackTxSummaryFields.get(j);
                    String colName = col.getXMLPath();
                    String colValue = extractNodeValue(message, containerPath, colName, i);

                    buf.append("<td>");
                    if (!headers.get(j).equals(col.getDisplayName()))
                    {
                        buf.append(col.getDisplayName()).append(COLON);
                    }

                    // When there is no value an nonblock space should be outputted so that the table data cell gets rendered correctly
                    if (StringUtils.hasValue(colValue))
                    {
                        String aliasKey = ORDER_HISTORY + "." + service.getServiceGroupDef().getID() + "." + colName;
                        colValue = aliasDescriptor.getAlias(pageContext.getRequest(), aliasKey, colValue, true);
                        buf.append(colValue);
                    } else buf.append(NBSP);

                    buf.append("</td>");

                }
            }
            buf.append("</tr></form>");
        }
        pageContext.setAttribute(ACTIONTYPE, firstRowActionType);
    }

    /**
     * This method checks if the boolean object is stored in the page context
     * variable of it is of type string and need to derive the boolean
     * value and in case of exception returns the default value.
     *
     * @param pgCtxAttrib page context attribute
     * @param defaultVal value to be returned in case of exception
     * @return boolean value
     */
    private boolean obtainBooleanValue(Object pgCtxAttrib, boolean defaultVal)
    {
        if (pgCtxAttrib != null)
        {
            try
            {
                if (pgCtxAttrib instanceof Boolean)
                    return (Boolean) pgCtxAttrib;
                else if (pgCtxAttrib instanceof String)
                    return StringUtils.getBoolean((String) pgCtxAttrib, defaultVal);
            }
            catch (Exception e)
            {
                // do nothing if exception occurs and returns default value
            }
        }

        return defaultVal;
    }

    /**
     * In the JSP this control is created via CreateDropDownList tag file.
     * Here this function will create the drop down based
     * on below parameters.
     *
     * @param name name of the control
     * @param htmlAttributes attributes to apply
     * @param jsOnChangeHandler javascript function
     * @param jsOnClickHandler javascript function
     * @param data list that contains data to display
     * @param showEmpty show first option as empty if true
     * @param selectItem to select and by default
     * @return String as html
     */
    private String createDropDown(String name, String htmlAttributes, String jsOnChangeHandler, String jsOnClickHandler, List data, boolean showEmpty, String selectItem)
    {
        StringBuffer html = new StringBuffer("<select name=\"");

        html.append(name);

        html.append("\"");

        if (StringUtils.hasValue(htmlAttributes))
        {
            html.append(" ");

            html.append(htmlAttributes);
        }

        if (StringUtils.hasValue(jsOnChangeHandler))
        {
            html.append(" onChange=\"");

            html.append(jsOnChangeHandler);

            html.append("(this)");
        }

        if (StringUtils.hasValue(jsOnClickHandler))
        {
            html.append("\" onClick=\"");

            html.append(jsOnClickHandler);

            html.append("(this)");
        }

        html.append("\">");

        if (data == null)
        {
            log.debug("doStartTag(): The data is null, the drop-down will contain no options at this time.");
        }
        else
        {
           // add an empty option if show empty is true
            if(showEmpty)
                data.add(0, new NVPair("", ""));

            int size = data.size();

            for (int i = 0; i < size; i++)
            {
                NVPair item = (NVPair)data.get(i);

                html.append("<option value=\"");

                html.append(item.value);

                html.append("\"");

                // If an item to be selected is specified then watch out for it,
                // otherwise select the first item as a default.

                if (StringUtils.hasValue(selectItem))
                {
                    if (selectItem.equals(item.value))
                    {
                        html.append(" selected");
                    }
                }
                else
                {
                    if (i == 0)
                    {
                        html.append(" selected");
                    }
                }

                html.append(">");

                html.append(item.name);

                html.append("</option>");
            }
        }
        html.append("</select>");
        return html.toString();
    }

    /**
     * If the node exists in the message than extract that value else returns empty string
     *
     * @param message XMLGenerator object
     * @param containerPath path of the container
     * @param nodeName name of the node
     * @param idx index in the message
     * @return value of the node
     */
    private String extractNodeValue(XMLGenerator message, String containerPath, String nodeName, int idx)
    {
        if (log.isInfoEnabled())
            log.info ("OrderHistoryTag: Processing param nodeName as [" + nodeName + "] and idx [" + idx + "]..." );

        String path = containerPath + "." + idx + "." + nodeName; 
        if (message.exists(path))
        {
            try
            {
                String nodeVal = message.getValue(path);
                if (!StringUtils.hasValue(nodeVal))
                    nodeVal = "";
                if (log.isInfoEnabled())
                    log.info ("OrderHistoryTag: Returning value [" + nodeVal + "] Node [" + nodeName + "]. Returning empty value." );
                
                return nodeVal;
            }
            catch (MessageException e)
            {
                log.warn("OrderHistoryTag: Unable to retrieve the value for Node [" + nodeName + "]. Returning empty value." );
            }
        }
        else
        {
            log.warn("OrderHistoryTag: Node [" + nodeName + "] does not exists." );
        }

        return "";
    }

    public void release()
    {
        super.release();
        message = null;
        service = null;
        hiddenFieldsListPerRow = null;
        formNamePrefix = null;
        formNameSuffix = null;
        containerPath = null;
        requestBean = null;
        responseBean = null;
        contextPath = null;
    }
}
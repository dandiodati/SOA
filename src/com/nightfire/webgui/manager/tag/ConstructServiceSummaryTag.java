/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.http.HttpServletRequest;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.util.xml.ParsedXPath;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.meta.Field;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.framework.message.common.xml.XMLGenerator;
import  com.nightfire.webgui.manager.beans.ServiceComponentBean;
import com.nightfire.webgui.core.tag.TagUtils;


/**
 * ConstructServiceSummaryTag is responsible for outputing the summary header and
 * data fields in HTML column format, of a given service-component.  
 */

public class ConstructServiceSummaryTag extends ConstructServiceInfoTag
{   
    private static final String TOGGLE_HISTORY_DISPLAY_ACTION = "toggle-history-display";
    
    private static KeyTypeLookup parsedXPathLookup            = new KeyTypeLookup(); 
    
    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    private AliasDescriptor aliasDescriptor;


    // indicates if this service summary should be selected
    private boolean selected = false;
    

    /**
     * Setter method indicating if the current summary should be selected.
     *
     * @param bool A boolean value
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setSelected(String bool) throws JspException
    {

        String temp = (String)TagUtils.getDynamicValue("selected", bool, String.class, this, pageContext);

        selected = StringUtils.getBoolean(temp, false);
        
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
      
        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);

        Object[]     summaryFields = serviceDef.getSummaryFields().toArray();
        
        // if there are no detail fields defined then 
        // this component does not support history details
        boolean supportDetailFields =   serviceDef.getDetailFields().size() != 0;
        
		StringBuffer output        = new StringBuffer("");

		String headerStr = constructServiceSummaryHeader();

		output.append(headerStr);

        output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
        
        output.append("<tr>");
        
        output.append("<td class=\"ServiceSummaryFirstColumn\"></td>");
        
        output.append("<td>");
        
        output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"1\" width=\"100%\">");
        
        output.append("<tr>");
        
        output.append("<th class=\"ServiceSummary\">&nbsp;</th>");
        
        int columnWidth = 100 / summaryFields.length;
        
        for (int i = 0; i < summaryFields.length; i++)
        {
            Field  summaryField = (Field)summaryFields[i];
            
            String fieldLabel   = summaryField.getDisplayName();
                        
            output.append("<th onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('" + summaryField.getFullName() +"');\" class=\"ServiceSummary\"");
            
            if ((i + 1) < summaryFields.length)
            {
                output.append(" style=\"width:").append(columnWidth).append("%\"");   
            }
            
            output.append(">").append(fieldLabel).append("</th>");
        }
        
        output.append("</tr>");
        
        output.append("</table>");
        
        output.append("</td>");
        
        output.append("</tr>");
        
        output.append("<tr>");
        
        output.append("<td class=\"ServiceSummaryFirstColumn\" align=\"center\">");
        
        String orderStatus = serviceBean.getHeaderValue(STATUS_FIELD);
        
        if (supportDetailFields && StringUtils.hasValue(orderStatus) && !orderStatus.equals(SAVED_STATUS) && !orderStatus.equals(NEW_STATUS))
        {
            output.append("<a href=\"javascript:toggleServiceHistory('").append(serviceBean.getId()).append("')\" ");
            output.append("onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Service History')\">");
                    
            output.append("<img border=\"0\" class=\"ServiceHistoryToggleButton\" src=\"").append(((HttpServletRequest)pageContext.getRequest()).getContextPath()).append("/images/").append(getHistoryDisplayStatus()).append(".gif\">");
            
            output.append("</a>");
        }
        
        output.append("</td>");
        
        output.append("<td>");
        
        output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"1\" width=\"100%\">");
        
        output.append("<tr>");
        
        output.append("<td class=\"ServiceSummaryFirstColumn\" align=\"center\">");
        
        output.append("<input ");
        
        if(selected)
            output.append("checked=\"true\" ");
        
        output.append("type=\"checkbox\" name=\"").append(SERVICE_COMPONENT_HEADER_FIELD).append("(").append(getServiceBeanIndex(serviceBean.getId())).append(")").append("\" value=\"").append(serviceBean.getId()).append("\">");
        
        output.append("</td>");
        
        for (int i = 0; i < summaryFields.length; i++)
        {
            Field  summaryField      = (Field)summaryFields[i];
                        
            String summaryFieldValue = getSummaryFieldValue(summaryField);

            if ( !StringUtils.hasValue(summaryFieldValue) )
               summaryFieldValue = "&nbsp;";
               
            output.append("<td class=\"ServiceSummary\"");
            
            if ((i + 1) < summaryFields.length)
            {
                output.append(" style=\"width:").append(columnWidth).append("%\"");   
            }
           
            //If an alias is defined for the field - use it.
            summaryFieldValue = aliasDescriptor.getAlias(pageContext.getRequest(), summaryField.getFullXMLPath(), summaryFieldValue, true);            
            
            output.append(">").append(summaryFieldValue).append("</td>");
        }
        
        output.append("</tr>");
        
        output.append("</table>");
        
        output.append("</td>");
        
        output.append("</tr>");
        
        output.append("</table>");
        
        try
        {
            pageContext.getOut().print(output.toString());
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ConstructServiceSummaryTag.doStartTag(): Failed to write to JspWriter output-stream.\n" + e.getMessage();

            log.error(errorMessage);

            throw new JspTagException(errorMessage);
        }
        
        return SKIP_BODY;
    }
    
    /**
	 * Add custom header elements if required.
	 */
	protected String constructServiceSummaryHeader()
	{
		return "";
	}
	
	/**
     * Extracts the index from the given service component id.
     *
     * @param  serviceBeanId  Service bean id.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  Index of the given service component id.
     */
    private String getServiceBeanIndex(String serviceBeanId) throws JspException
    {
        int leftParenIndex  = serviceBeanId.indexOf('(');
        
        int rightParenIndex = serviceBeanId.indexOf(')');
        
        if ((leftParenIndex == -1) || (rightParenIndex == -1))
        {
            String errorMessage = "ERROR: ConstructServiceSummaryTag.getServiceBeanIndex(): Encountered invalid service-component bean id [" + serviceBeanId + "]";
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
        
        return serviceBeanId.substring(leftParenIndex + 1, rightParenIndex);
    }
    
    /**
     * Obtains the history-display status of the service-component bean.  If a request
     * exists which indicates the toggling of the history-display, then the status
     * will reflect it.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  History-display status of the service-component bean.
     */
    private String getHistoryDisplayStatus() throws JspException
    {
        String currentStatus = serviceBean.getHeaderValue(ServiceComponentBean.HISTORY_DISPLAY_STATUS_FIELD);
        
        if (!StringUtils.hasValue(currentStatus))
        {
            currentStatus = ServiceComponentBean.COLLAPSE_STATUS;
        }
        
        String action = pageContext.getRequest().getParameter(NF_FIELD_ACTION_TAG);
                        
        if (StringUtils.hasValue(action) && action.equals(TOGGLE_HISTORY_DISPLAY_ACTION))
        {
            String componentToToggle = pageContext.getRequest().getParameter(SERVICE_COMPONENT_HEADER_FIELD);
            
            if (componentToToggle.equals(serviceBean.getId()))
            {            
                if (currentStatus.equals(ServiceComponentBean.COLLAPSE_STATUS))
                {
                    currentStatus = ServiceComponentBean.EXPAND_STATUS;
                }
                else
                {
                    currentStatus = ServiceComponentBean.COLLAPSE_STATUS;
                }
            }
        }
        
        try
        {
            serviceBean.setHeaderValue(ServiceComponentBean.HISTORY_DISPLAY_STATUS_FIELD, currentStatus);
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ConstructServiceSummaryTag.getHistoryDisplayStatus(): Failed to set the header value [" + ServiceComponentBean.HISTORY_DISPLAY_STATUS_FIELD + ", " + currentStatus + "] on the service-component bean:\n" + e.getMessage();     
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
       
        return currentStatus;
    }
    
    /**
     * Obtains the value of the specified summary field from the service-component bean.
     *
     * @param  summaryField  Summary field to look up the value for.
     *
     * @return  Summary field value.
     */
    private String getSummaryFieldValue(Field summaryField)
    {
        String xPath             = summaryField.getCustomValue("XPath");
        
        String summaryFieldValue = "";
        
        if (!StringUtils.hasValue(xPath))
        {
            summaryFieldValue = serviceBean.getHeaderValue(summaryField.getXMLPath());
        }
        else
        {
            String      serviceType = serviceDef.getID();
            
            ParsedXPath parsedXPath = (ParsedXPath)parsedXPathLookup.get(serviceType, xPath);
            
            try
            {
                XMLGenerator gen = (XMLGenerator)serviceBean.getBodyDataSource();
                if (parsedXPath == null)
                {
                    synchronized (getClass())
                    {
                        parsedXPath = (ParsedXPath)parsedXPathLookup.get(serviceType, xPath);
                        
                        if (parsedXPath == null)
                        {
                            parsedXPath = new WebParsedXPath(xPath, gen.getGroup());
                                                       
                            parsedXPathLookup.put(serviceType, xPath, parsedXPath);
                        }
                    }
                }
                
                String[] candidateValues = parsedXPath.getValues(gen.getDocument());
                
                if (candidateValues.length > 0)
                {
                    summaryFieldValue = candidateValues[0];
                }

		if (!StringUtils.hasValue(summaryFieldValue))
                {
                  summaryFieldValue = serviceBean.getHeaderValue(summaryField.getXMLPath());
                }
            }
            catch (Exception e)
            {
                log.error("ConstructServiceSummaryTag.getSummaryFieldValue(): Failed to obtain the value for summary field [" + summaryField.getID() + "], for service component [" + serviceBean.getId() + "].  A blank will be returned instead:\n" + e.getMessage());
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("getSummaryFieldValue(): The value for summary field [" + summaryField.getID() + "], for service component [" + serviceBean.getId() + "] is [" + summaryFieldValue + "].");
        }
        
        return summaryFieldValue;
    }

    /**
     * Redefinition of release() in VariableTagBase.  This method is invoked after
     * doEndTag(), allowing any state maintenance to be performed.
     */
    public void release()
    {
        super.release();
        
        selected  = false;
       
    }
}

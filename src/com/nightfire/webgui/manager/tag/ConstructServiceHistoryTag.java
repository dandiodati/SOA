/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  java.util.*;

import  javax.servlet.jsp.*;
import  javax.servlet.http.*;

import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.constants.PlatformConstants;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.tag.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.core.meta.Field;
import  com.nightfire.webgui.manager.beans.ServiceComponentBean;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.generator.MessageGeneratorException;
import  com.nightfire.framework.message.MessageException; 

/**
 * ConstructServiceHistoryTag is responsible for outputing the history of a service
 * in an HTML table format.  
 */

public class ConstructServiceHistoryTag extends ConstructServiceInfoTag implements PlatformConstants
{   
    protected static final String SERVICE_HISTORY = "SERVICE_HISTORY";
    
    
    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    private AliasDescriptor aliasDescriptor;

    
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
      
        if (log.isDebugEnabled())
        {
            log.debug("doStartTag(): Starting tag processing ...");    
        }
        
        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);

        String historyDisplayStatus = serviceBean.getHeaderValue(ServiceComponentBean.HISTORY_DISPLAY_STATUS_FIELD);
        
        if (StringUtils.hasValue(historyDisplayStatus) && historyDisplayStatus.equals(ServiceComponentBean.EXPAND_STATUS))
        {
			XMLGenerator serviceHistory = getServiceHistory();
            
            Object[]     detailFields = getDetailFields();
            
            StringBuffer output       = new StringBuffer("<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");
            
            output.append("<tr>");
            
            output.append("<td class=\"ServiceHistoryFirstColumn\"></td>");
            
            output.append("<td>");
            
            output.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"1\" width=\"100%\">");
            
            output.append("<tr>");
                        
            for (int i = 0; i < detailFields.length; i++)
            {
                Field  detailField = (Field)detailFields[i];
                
                String fieldLabel  = TagUtils.escapeHtmlSpaces(detailField.getDisplayName());
                            
                output.append("<th onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('" + detailField.getFullName() +"');\" class=\"ServiceHistory\">").append(fieldLabel).append("</th>");
            }
            
            output.append("</tr>");
            
            if (serviceHistory != null)
            {
                try
                {
                    Node[] rows = serviceHistory.getChildren(DATA_CONTAINER_NODE);
                    
                    for (int i = 0; i < rows.length; i++)
                    {            
                        output.append("<tr>");
                        
                        for (int j = 0; j < detailFields.length; j++)
                        {
                            Field  field      = (Field)detailFields[j];
                            
                            String fieldName  = field.getXMLPath();
                            
                            String fieldValue = "&nbsp;";
                            
                            if (serviceHistory.exists(rows[i], fieldName))
                            {
                                //fieldValue = serviceHistory.getValue(rows[i], fieldName);
								fieldValue = getDetailFieldValue(serviceHistory, rows[i], field);
                            }
                            
                            if (log.isDebugEnabled())
                            {
                                log.debug("doStartTag(): [" + serviceBean.getId() + "]'s history line-item [" + i + "]'s field [" + fieldName + "] has value [" + fieldValue + "].");
                            }
                            
                            // If an alias is defined for this field then use it.
                            fieldValue = aliasDescriptor.getAlias(pageContext.getRequest(), field.getFullXMLPath(), fieldValue, true);  

                            displayDetailField(i,serviceHistory, rows[i],fieldValue,output);
                        }
                        
                        output.append("</tr>");
                    }
                }
                catch (Exception e)
                {
                    log.error("ConstructServiceHistoryTag.doStartTag(): Failed to parse the obtained service history for display:\n" + e.getMessage());
                }
            }
            
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
                String errorMessage = "ERROR: ConstructServiceHistoryTag.doStartTag(): Failed to write to JspWriter output-stream.\n" + e.getMessage();

                log.error(errorMessage);

                throw new JspTagException(errorMessage);
            }
        }
        
        return SKIP_BODY;
    }

	/**
	 * Get the detail history field value from the service history data.
	 *
	 * @param serHist - Service History xml generator
	 * @param n - Service History Data Node
	 * @param field - Detail field whose value is required
	 * 
	 * @return The detail history field value accessed from service history xml generator
	 */
	 protected String getDetailFieldValue(XMLGenerator serHist, Node n, Field field) throws JspTagException
	{
		try{
			String fieldName = field.getXMLPath();
			return serHist.getValue(n, fieldName);
		} catch(Exception e) {
                String errorMessage = "ERROR: ConstructServiceHistoryTag.getDetailFieldValue(): Failed to get value of detail field [" + field.getXMLPath() + "] .\n" + e.getMessage();

                log.error(errorMessage);

                throw new JspTagException(errorMessage);
		}
	}
    
	/**
	 * Display the history detail field value.
	 *
	 * @param historyIndex - Service History Index
	 * @param serHist - Service History xml generator
	 * @param n - Service History Data Node
	 * @param fieldValue - Value of Service History Detail Field
	 * @param output - Buffer for output
	 * 
	 */
	protected void displayDetailField(int historyIndex, XMLGenerator serHist, Node n,String fieldValue,StringBuffer output) throws MessageException
	{
		output.append("<td class=\"ServiceHistory\"><a class=\"ServiceHistory\" href=\"javascript:getServiceHistoryDetail('");
                            
		output.append(serviceBean.getId()).append("', '").append(historyIndex).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Detail')\">");

		output.append(fieldValue);

		output.append("</a></td>");
	}
	
	
	
	/**
    * Get the object array containing
	* detail fields to be displayed in history.
	*
	* @return  Object array containing detail fields.
	*/
	protected Object[] getDetailFields() {
		return serviceDef.getDetailFields().toArray();
	}


	/**
	 * Create the request header generator. Set the action to get service history data in the header.
	 *
	 * @return xml generator for the request header.
	 */
	protected XMLGenerator getRequestHeaderGenerator() throws MessageGeneratorException, MessageException
    {
        XMLGenerator requestHeaderGenerator = new XMLPlainGenerator(HEADER_NODE);
        requestHeaderGenerator.setValue(ACTION_NODE, GET_SPI_MESSAGE_HISTORY_ACTION);
        return requestHeaderGenerator;
	
	}

	
	/**
	 * Get the service history cache having default responses.
	 * 
	 * @return xml generator containing history data to be displayed
	 */
	protected XMLGenerator getServiceHistoryCache()
    {
		XMLGenerator serviceHistory = null;
		serviceHistory = (XMLGenerator)serviceBean.customData.get(SERVICE_HISTORY);
 		return serviceHistory;
	}

	
	/**
	 * Set the history data in cache for the first time to avoid reloading it everytime from DB.
	 * 
	 * @param  serviceHistory xml generator for service history data
	 */
	protected void setServiceHistoryCache(XMLGenerator serviceHistory)
    {
        if (serviceHistory != null)
		{
			serviceBean.customData.put(SERVICE_HISTORY, serviceHistory);
		}
	}


    /**
     * Sends a request to the processing layer and obtains the history of the service
     * under consideration.
     *
     * @return  An XMLGenerator object that represent the historical information.
     */
    private XMLGenerator getServiceHistory()
    {
        try
        {
			XMLGenerator serviceHistory = getServiceHistoryCache();
            
            if (serviceHistory == null)
            {
				//XMLGenerator requestHeaderGenerator = new XMLPlainGenerator(HEADER_NODE);

				XMLGenerator requestHeaderGenerator = null;
			
				XMLGenerator requestBodyGenerator   = new XMLPlainGenerator(BODY_NODE);
				
				Node         infoNode               = requestBodyGenerator.create(INFO_NODE);

				//requestHeaderGenerator.setValue(ACTION_NODE, GET_SPI_MESSAGE_HISTORY_ACTION);

				requestHeaderGenerator = getRequestHeaderGenerator();
				
				requestBodyGenerator.setValue(infoNode, BOID_FIELD, serviceBean.getHeaderValue(BOID_FIELD));
				
				requestBodyGenerator.setValue(infoNode, META_DATA_NAME_NODE, serviceBean.getHeaderValue(META_DATA_NAME_NODE));
			
				requestBodyGenerator.setValue(infoNode, SERVICE_TYPE_NODE, serviceBean.getServiceType());

				DataHolder response = TagUtils.sendRequest(requestHeaderGenerator, requestBodyGenerator, pageContext);
				
				if (log.isDebugEnabled())
				{
					log.debug("ConstructServiceHistory.getServiceHistory(): The processing layer returned the following response:\n");
					
					log.debug("ConstructServiceHistory.getServiceHistory(): Header:\n" + response.getHeaderStr());
					
					log.debug("ConstructServiceHistory.getServiceHistory(): Body:\n" + response.getBodyStr());
				}
				
				XMLGenerator responseHeaderParser = new XMLPlainGenerator(response.getHeaderStr());

				String       responseCode         = responseHeaderParser.getValue(ServletConstants.RESPONSE_CODE_NODE);
				
				if (responseCode.equals(SUCCESS))
				{
					serviceHistory = new XMLPlainGenerator(response.getBodyStr());
				}

				if (serviceHistory != null)
                {
                    //serviceBean.customData.put(SERVICE_HISTORY, serviceHistory);
					setServiceHistoryCache(serviceHistory);
                }
			}

			return serviceHistory;
	    }
        catch (Exception e)
        {
            log.error("ConstructServiceHistory.getServiceHistory(): Failed to obtain the service history:\n" + e.getMessage());
        }
        
        return null;
    }
}

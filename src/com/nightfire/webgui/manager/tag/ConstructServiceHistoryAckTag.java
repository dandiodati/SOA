/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  org.w3c.dom.*;
import  javax.servlet.jsp.*;
import  javax.servlet.http.*;
import  com.nightfire.webgui.core.meta.Field;
import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.manager.ManagerServletConstants; 
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.generator.MessageGeneratorException;
import  com.nightfire.framework.message.MessageException; 

/**
 * ConstructServiceHistoryAckTag is responsible for outputing the history of a service
 * in an HTML table format along with the outbound acknowledgements.  
 */

public class ConstructServiceHistoryAckTag extends ConstructServiceHistoryTag
{   

	private static final String OUTBOUND_ACK = "OUTBOUND_ACK";
	private static final String DIRECTION_NODE_NAME = "Direction";
	private static final String ACK = "ack";
	private static final String NACK = "nack";
    private boolean showOutboundAck = false ;

   	/**
     * Overriding parent method to perform any initialization tasks as required.
     */
	public void setup() 
    {
        super.setup();
        String ackDisplayStatus = serviceBean.getHeaderValue(ManagerServletConstants.SHOW_OUTBOUND_ACK_HEADER_FIELD);
        this.showOutboundAck  = StringUtils.hasValue(ackDisplayStatus) &&  
                                ackDisplayStatus.equalsIgnoreCase("true") ;
    }


	/**
	 * Get the detail history field value from the service history data. If the field is ack/nack and is outbound
	 * display it along with direction.
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
			String fieldValue = serHist.getValue(n, fieldName);
			if(fieldName.equalsIgnoreCase("TYPE") && (fieldValue.equals(ACK) || fieldValue.equals(NACK)) && serHist.exists(n, DIRECTION_NODE_NAME) && serHist.getValue(n, DIRECTION_NODE_NAME).equalsIgnoreCase("OUT"))
				return "OutBound " + fieldValue;
			else
				return fieldValue;
		} catch(Exception e) {
                String errorMessage = "ERROR: ConstructServiceHistoryTagAck.getDetailFieldValue(): Failed to get value of detail field [" + field.getXMLPath() + "] .\n" + e.getMessage();

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
	protected void displayDetailField(int historyIndex, XMLGenerator serHist, Node n, String fieldValue, StringBuffer output) throws MessageException
	{
		if(serHist.exists(n, "Direction") && serHist.getValue(n, "Direction").equalsIgnoreCase("OUT") && serHist.exists(n, "TYPE") && (serHist.getValue(n, "TYPE").equals(ACK) || serHist.getValue(n, "TYPE").equals(NACK))) {
			output.append("<td class=\"ServiceHistory\">");
			output.append(fieldValue);
			output.append("</td>");
		} else {
			output.append("<td class=\"ServiceHistory\"><a class=\"ServiceHistory\" href=\"javascript:getServiceHistoryDetail('");
			output.append(serviceBean.getId()).append("', '").append(historyIndex).append("')\" onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('View Detail')\">");
			output.append(fieldValue);
			output.append("</a></td>");
		}
	}

   /**
    * Checks if show outbound acknowledgement flag is true, and depending on it retrun the object array containing
	* detail fields to be displayed in history.
	*
	* @return  Object array containing detail fields.
	*/
	protected Object[] getDetailFields() 
	{
		if(showOutboundAck) {
			return serviceDef.getAckDetailFields().toArray();
		}            
		else {
			return serviceDef.getDetailFields().toArray();
		}
	}


	/**
	 * Create the request header generator. Set the action to get service history data, in the header depending on 
	 * the value of show outbound acknowledgement flag.
	 *
	 * @return xml generator for the request header.
	 */
	protected XMLGenerator getRequestHeaderGenerator() throws MessageGeneratorException, MessageException
	{ 
        XMLGenerator requestHeaderGenerator = new XMLPlainGenerator(HEADER_NODE);
        
        if(showOutboundAck) {
            requestHeaderGenerator.setValue(ACTION_NODE, GET_SPI_MESSAGE_HISTORY_ACK_ACTION);
        } else {
            requestHeaderGenerator.setValue(ACTION_NODE, GET_SPI_MESSAGE_HISTORY_ACTION);
        }
        
        return requestHeaderGenerator;
	}


	/**
	 * Get the service history cache having outbound acknowledgements along with default responses
	 * if show outbound acknowledgement flag is true
	 * otherwise return one without them.
	 *
	 * @return xml generator containing history data to be displayed
	 */
	protected XMLGenerator getServiceHistoryCache()
    {
		XMLGenerator serviceHistory = null;
       	
		if(showOutboundAck) {
			if (serviceBean.customData.get(OUTBOUND_ACK) != null && serviceBean.customData.get(OUTBOUND_ACK).equals("true"))
			{
				serviceHistory = (XMLGenerator)serviceBean.customData.get(SERVICE_HISTORY);
			}
 		} else {
			if (serviceBean.customData.get(OUTBOUND_ACK) == null || serviceBean.customData.get(OUTBOUND_ACK).equals("false"))
			{
				serviceHistory = (XMLGenerator)serviceBean.customData.get(SERVICE_HISTORY);
			}
 		}
		
		return serviceHistory;
	}


	/**
	 * Set the history data in cache for the first time to avoid reloading it everytime from DB.
	 * 
	 * @param  serviceHistory xml generator for service history data
	 */
	protected void setServiceHistoryCache(XMLGenerator serviceHistory)
    {
		if(showOutboundAck) {
			if (serviceHistory != null)
			{
				serviceBean.customData.put(SERVICE_HISTORY, serviceHistory);
				serviceBean.customData.put(OUTBOUND_ACK, "true");
			}
    	} else {
	        if (serviceHistory != null)
			{
				serviceBean.customData.put(SERVICE_HISTORY, serviceHistory);
				serviceBean.customData.put(OUTBOUND_ACK, "false");
			}
		}
	}

}

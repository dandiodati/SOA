/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import  javax.servlet.jsp.*;
import  javax.servlet.http.HttpServletRequest;

import  com.nightfire.webgui.manager.ManagerServletConstants;
import  com.nightfire.framework.util.*;

/**
 * ConstructServiceSummaryAckTag is responsible for outputing the summary header including 
 * show outbound acknowledgement checkbox, in HTML column format, of a given service-component.  
 */

public class ConstructServiceSummaryAckTag extends ConstructServiceSummaryTag 
{   
  
 	private static final String TOGGLE_SHOW_OUTBOUND_ACK_ACTION = "toggle-show-outbound-ack";

	/**
     * Overriding parent method to perform any initialization tasks as required.
     */
    public void setup() 
    {
        super.setup();

        String ackDisplayStatus = serviceBean.getHeaderValue(ManagerServletConstants.SHOW_OUTBOUND_ACK_HEADER_FIELD);
       
		if (!StringUtils.hasValue(ackDisplayStatus)) {
            ackDisplayStatus = "false";
        }
        
        
		String action = pageContext.getRequest().getParameter(NF_FIELD_ACTION_TAG);
                        
        if (StringUtils.hasValue(action) && action.equals(TOGGLE_SHOW_OUTBOUND_ACK_ACTION))
        {
            String componentToToggle = pageContext.getRequest().getParameter(SERVICE_COMPONENT_HEADER_FIELD);
            
            if (componentToToggle.equals(serviceBean.getId()))
            {            
                if (ackDisplayStatus.equalsIgnoreCase("true")) {
                    ackDisplayStatus = "false";
                } else {
                    ackDisplayStatus = "true";
                }
            }
        }

        try
        {
            serviceBean.setHeaderValue(ManagerServletConstants.SHOW_OUTBOUND_ACK_HEADER_FIELD, ackDisplayStatus);
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ConstructServiceHistoryAckTag.reset(): Failed to set the header value [" + ManagerServletConstants.SHOW_OUTBOUND_ACK_HEADER_FIELD + ", " + ackDisplayStatus + "] on the service-component bean:\n" + e.getMessage();     
            
            log.error(errorMessage);
            
        }
       
    }
   
   
    /**
     * Adding header field display. Here header field is Show outbound Ack.
     */
    protected String constructServiceSummaryHeader()
    {
       StringBuffer output = new StringBuffer("");
       
       // if there are no detail fields defined then this component does not support history details
       boolean supportAckDetailFields =   serviceDef.getAckDetailFields().size() != 0;
       
       String orderStatus = serviceBean.getHeaderValue(STATUS_FIELD);

       if (supportAckDetailFields && StringUtils.hasValue(orderStatus) && !orderStatus.equals(SAVED_STATUS) && !orderStatus.equals(NEW_STATUS))
       {
            output.append("<input type=\"checkbox\""); 
            output.append( "onClick=\"javascript:showAckInHistory('").append(serviceBean.getId()).append("')\"");
            output.append(" name=\"NFH_showAckInHistory\"");
           
            String ackDisplayStatus = serviceBean.getHeaderValue(ManagerServletConstants.SHOW_OUTBOUND_ACK_HEADER_FIELD);

            if(StringUtils.hasValue(ackDisplayStatus) && ackDisplayStatus.equalsIgnoreCase("true")) {
                output.append(" checked");
            }
           
            output.append(" >Show Outbound Acknowledgements");
        
       }
       return output.toString();
    }
}

/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.gateway.tag.util;

import java.util.*;
import java.net.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;


import com.nightfire.webgui.gateway.svcmeta.*;
import com.nightfire.webgui.gateway.*;
import com.nightfire.security.tpr.TradingPartnerRelationship;
/**
 * This tag creates a ListWrapper of all the TradingPartnerNames available for a given service.
 * This list will be accessible via the value specified by "var" in the jsp tag.
 */
public class ListTradingPartnersTag extends VariableTagBase
{

    private ServiceDef service = null;


    /**
     * Set the service object to get TradingPartnerNames from.
     *
     * @param obj The service object
     */
    public void setService ( Object obj ) throws JspException
    {
       this.service = (ServiceDef)TagUtils.getDynamicValue("service", obj,
                           ServiceDef.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     * This method obtains the list of trading partner names available for the service object passed in the tag,
     * and returns that information in a List form placed at the location defined by
     * the variable var and the scope specified.
     *
     * Each item in the list is a NVPair type where:
     *     name = display name of order type (example "Verizon East")
     *     value = key of the order type (example "VZE")
     *
     * @throws JspException on processing error.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        try
        {
             //Get the trading partner field from the service object first.
            Field tradingPartnerField = service.getAddlInfoField( ServletConstants.TRADINGPARTNER);
			String customerId = TagUtils.getCustomerId(pageContext);
             
			TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(customerId);

			Collection tpTradingPartnerCollection = tpr.getEnabledTradingPartners(service.getID());
			
			if(tpTradingPartnerCollection  == null) {
							throw new JspException(" TradingPartner collection is null in DB for service [" +
								service.getID() + "]." );
						}

			List tpTradingPartnerList = new ArrayList(tpTradingPartnerCollection);
			List tradingPartnerList = new ArrayList();


            if ( tradingPartnerField == null )
            {
            	log.debug("TradingPartner field missing in meta-data for service [" +
				service.getID() + "]." );
                Iterator tpNameItr = tpTradingPartnerList.iterator();
                
             	while(tpNameItr.hasNext()){
             		
             		String tpValue = (String)tpNameItr.next();
             		
             		log.debug("TradingPartner Name is : "+tpValue);
             		
					tradingPartnerList.add(new NVPair( tpValue, tpValue ) );
                        
					if ( log.isDebugEnabled() )
					{
						log.debug( "Adding TradingPartner [" + tpValue + "]:[" + tpValue +"] to list.");
					}
             		
             	}
            }
       		else{
				// Having got the TradingPartner field, get all the TradingPartner listed in it.
				  OptionSource src = tradingPartnerField.getDataType().getOptionSource();
				  if ( src != null )
				  {
					  String [] optValues = src.getOptionValues();
					  String [] displayValues = src.getDisplayValues();
	
					  String name = null;
					  String value = null;
					  for ( int Ix=0; Ix < optValues.length; Ix++ )
					  {
						  name = displayValues[Ix];
						  value = optValues[Ix];
	
						  if( tpTradingPartnerList.contains( value ) )
						  {
							  tradingPartnerList.add(new NVPair( name, value ) );
	            
							  if ( log.isDebugEnabled() )
							  {
								  log.debug( "Adding TradingPartner [" + name + "]:[" + value +"] to list.");
										  }
									  }
	 
								  }//for
	    
				  } 
				  else
				  {
					  throw new JspException( "At least one TradingPartner must be defined." );
				  }
            
       			
       		}
            

            if ( log.isDebugEnabled() )
            {
                log.debug( "Returning [" + tradingPartnerList.size() + "] TradingPartner." );
            }

            

        	//Now set the services on the location specified by var and scope variables.
            setVarAttribute( new ListWrapper(tradingPartnerList) );
        }
        catch( Exception e )
        {
            String err = "Failed to get TradingPartner: " + e.getMessage();
            log.error(err, e);
            throw new JspTagException(err);
        }

        return SKIP_BODY;
    }


    public void release()
    {
       super.release();
       service = null;
    }

}

/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.webgui.gateway.tag.util;

import java.util.*;

import javax.servlet.jsp.*;

import com.nightfire.framework.util.*;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.gateway.svcmeta.*;

import com.nightfire.security.tpr.TradingPartnerRelationship;

/**
 * This tag creates a ListWrapper of all the suppliers available for a given service.
 * This list will be accessible via the value specified by "var" in the jsp tag.
 */
public class ListSuppliersTag extends VariableTagBase
{

    private ServiceDef service = null;

    /**
     * Set the service object to get suppliers from.
     *
     * @param obj The service object
     * @throws JspException on error
     */
    public void setService ( Object obj ) throws JspException
    {
       this.service = (ServiceDef)TagUtils.getDynamicValue("service", obj,
                           ServiceDef.class, this, pageContext);
    }


    /**
     * Starts procesing of this tag.
     * This method obtains the list of suppliers available for the service object passed in the tag,
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

            String customerId = TagUtils.getCustomerId(pageContext);
             
            TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(customerId);
            
            Map tpMap = tpr.getTradingPartnersForService(service.getID());

            if(tpMap  == null) {
                throw new JspException("Trading partner Map is null in DB for service [" +
                    service.getID() + "]." );
            }
            else
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Trading partner Map from DB is of Size [" + tpMap.size () + "]");
                }
            }

            TreeMap tradingPartnerMap = new TreeMap (tpMap);
            List tpList = new ArrayList();
            AliasDescriptor ad = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, PageContext.APPLICATION_SCOPE );

            if ( tradingPartnerMap != null )
            {
                Iterator itar = tradingPartnerMap.keySet ().iterator ();
                String name, value;
                /*
                    This piece of code has been introduced to limit the branded customer see the supplier
                    of the domain using which he has logged in, if he is configured for more than one branded supplier.
                */
                 String wholesaleProvider = TagUtils.getWSPInSession (pageContext);

                while ( itar.hasNext () )
                {
                    String key = (String) itar.next ();
                    String keyVal = (String) tradingPartnerMap.get ( key );
                    /**
                     * Getting alias for key value
                     */

                    String defaultAlias = ad.getAlias (pageContext.getRequest(), "CreateOrder.Supplier", keyVal, true);
                    name = ad.getAlias (pageContext.getRequest(), "CreateOrder."+service.getServiceGroupDef().getID()+".Supplier", keyVal, false);
                    if (!StringUtils.hasValue(name))
                        name = defaultAlias;

                    value = key;

                    if (StringUtils.hasValue(wholesaleProvider) && value.equals(wholesaleProvider))
                    {
                        tpList.clear();
                        tpList.add(new NVPair( name, value ) );
                        break;
                    }

                    tpList.add(new NVPair( name, value ) );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Adding trading partner [" + name + "]:[" + value +"] to list.");
                    }

                }//for
            }
            else
            {
                throw new JspException( "At least one trading partner must be defined." );
            }
            
            if ( log.isDebugEnabled() )
            {
                log.debug( "Returning [" + tpList.size() + "] trading partners." );
            }

            //Now set the services on the location specified by var and scope variables.
            setVarAttribute( new ListWrapper(tpList) );
        }
        catch( Exception e )
        {
            String err = "Failed to get suppliers: " + e.getMessage();
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

/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.util;


import javax.servlet.jsp.*;

import com.nightfire.framework.constants.*;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.security.tpr.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.SessionInfoBean;
import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.VariableTagBase;
import java.io.IOException;




/**
 * 
 */
public class GetActiveTPVersionTag extends VariableTagBase
{

    private String service;

    private String supplier = null;




     /**
     * The service to use
     */
    public void setService(String service) throws JspException
    {
        this.service = (String)TagUtils.getDynamicValue("service", service, String.class, this, pageContext);
    }



    /**
     * Provides the supplier for this data.
     * @param supplier the supplier associated with this data.
     *
     */
    public void setSupplier(String supplier) throws JspException
    {
       this.supplier = (String) TagUtils.getDynamicValue("supplier", supplier, String.class, this, pageContext);
    }


    /**
     * Gets the message part with the specified id.
     * If id does not have a value then defaultId is used.
     * It then sets the message part as a variable(var).
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();

      if (!StringUtils.hasValue(supplier) ||
          !StringUtils.hasValue(service))
          throw new JspTagException("Invalid properties, supplier[" + supplier +"] or service[" + service +"] are null");
      

       String cid = CustomerContext.DEFAULT_CUSTOMER_ID;
        
       if( pageContext.getSession() != null) {
            
           SessionInfoBean sBean = (SessionInfoBean) pageContext.getSession().getAttribute(ServletConstants.SESSION_BEAN);
           cid = sBean.getCustomerId();
            
       }

       try {
           
           TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(cid);
           String activeVer = tpr.getMostRecentVersion(supplier, service);

           if ( log.isDebugEnabled())
               log.debug("Got active version [" + activeVer +"]");
           
           if (varExists() )
               setVarAttribute(activeVer);
           else
               pageContext.getOut().write(activeVer);
           
       }
       catch (TPRException e) {
            
           String errorMessage = "Failed to get trading partner information: " + e.getMessage();
            

           log.error("doStartTag(): " + errorMessage);

           throw new JspTagException(errorMessage);
       }       
       catch (IOException e) {
            
           String errorMessage = "Failed to write out result: " + e.getMessage();
            

           log.error("doStartTag(): " + errorMessage);

           throw new JspTagException(errorMessage);
       }

       

       return SKIP_BODY;
    }

    public void release()
    {
       super.release();


       service = null;

       supplier = null;

    }



}

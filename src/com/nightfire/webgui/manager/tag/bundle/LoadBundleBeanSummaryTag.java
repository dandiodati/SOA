/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.tag.bundle;


import java.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;

import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.tag.message.*;
import com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.manager.svcmeta.*;
import com.nightfire.webgui.manager.tag.*;
import com.nightfire.webgui.core.tag.VariableSupportUtil;
import javax.servlet.ServletException;




/**
 * This tag creates a bundle bean tag with ServiceComponentBeans that contain
 * header info only. Sets the dirty bit to true on all inner beans.
 *
 */
public class LoadBundleBeanSummaryTag extends BeanTagBase implements ManagerServletConstants
{


    /**
     * The xml to use
     * @param data The xml bundle data
     * @exception JspException if an error occurs
     */
    public void setData(Object data) throws JspException
    {
        setDynAttribute("data", data, Object.class);
    }



    public NFBean createBean() throws JspException
    {
       
        
         
        Object bundleData = (Object)getDynAttribute("data"); 


           
        BundleBeanBag bag = new BundleBeanBag(getBeanName());

        try {
            
	   
            if ( bundleData != null) {
                if ( bundleData instanceof XMLGenerator)
                    bag.decompose((XMLGenerator)bundleData);
                else if (bundleData instanceof String )
                    bag.decompose((String)bundleData);
                else if (bundleData instanceof Document)
                    bag.decompose((Document)bundleData);

            }  else {
                log.error("data attribute was null");
                throw new JspTagException("data attribute was null");
            }

            String bundleDefName = bag.getHeaderValue(META_DATA_NAME);
    
            if (log.isDebugEnabled())
                log.debug("Looking up bundle def with metadataname [" + bundleDefName +"]");
        
            HashMap   bundleDefs = (HashMap)pageContext.getServletContext().getAttribute(BUNDLE_DEFS);
        
            BundleDef bundleDef  = (BundleDef)bundleDefs.get(bundleDefName);
        
            if (bundleDef == null)
                {
                    String errorMessage = "No BundleDef object found for bundle data with metaDataName [" + bundleDefName +"].";
            
                    log.error(errorMessage);
            
                    throw new JspException(errorMessage);
                }


            bag.setHeaderValue(BUNDLE_DISPLAY_NAME, bundleDef.getDisplayName());

        }
        catch (JspTagException e) {
            throw e;
        }
        catch(Exception e2) {
            log.error("Failed to create bundle : " + e2.getMessage());
            throw new JspTagException("Failed to create bundle : "+ e2.getMessage());
        }

        Iterator iter = bag.getBodyAsMap().values().iterator();
        
        while (iter.hasNext()) {
            XMLBean bean = (XMLBean) iter.next();
            bean.setDirty(true);
        }
        
         
         return bag;
        
    }
    
}

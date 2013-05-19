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
 * This tag loads bundle data into a bundle bean bag.
 *
 */
public class LoadBundleBeanTag extends ContainerBeanTagBase implements ManagerServletConstants
{

    private BundleDef bundleDef;
    
    private Iterator scOrderIter = null;

    private String orderInfoVar = null;
    
     
      /**
     * Setter method for variable that will hold the current orderbeaninfo object.
     * Used to pass scbean information to inner tags.
     *
     * @param  orderIfo The variable name of the object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setVarOrderInfo(String orderInfo) throws JspException
    {
        orderInfoVar = orderInfo;
    }  

    /**
     * The xml to use
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
        
            bundleDef  = (BundleDef)bundleDefs.get(bundleDefName);
        
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

         
        // build a list of sc beans that need to be initialized by the inner tag
        scOrderIter = createOrderInfo(bag).iterator();
        
  
        

        // set the initial order info object before the first loop of the body.
        if (scOrderIter.hasNext()) {
            OrderBeanInfo entry = (OrderBeanInfo) scOrderIter.next();
            VariableSupportUtil.setVarObj(orderInfoVar, entry, scope, pageContext);
        }

        return bag;
        
    }
    

    public int doAfterBody() throws JspException
    {

                
         // for each OrderBeanInfo object we let the inner tag create the bean
          if (scOrderIter.hasNext()) {

             
            OrderBeanInfo entry = (OrderBeanInfo) scOrderIter.next();
            
            VariableSupportUtil.setVarObj(orderInfoVar, entry, scope, pageContext);
           
            return EVAL_BODY_AGAIN;
            
          } else {
              //finished created all the sc beans.
              // let parent classes set the bundle bean in the message cache.
              return SKIP_BODY;
          }
          
    } 


   
    
     
    public void addBean(String beanName, NFBean bean)
    {
        // we want use the id of the bean since it already exists
        // due to the previous decompose of the bundle bean bag. 
        // This is done so that the new order bean replaces the existing
        // one in the bean bag. If this is not done then
        // we get duplicate bean entries. 
        String id = ((InfoBodyBase)bean).getId();   
        ((BundleBeanBag)getBean()).addBean(id, (ServiceComponentBean)bean);
    }
    
       
    
    private List createOrderInfo(BundleBeanBag bag) throws JspException
    {
        Iterator iter = bag.getBodyAsMap().values().iterator();
        
        List orderInfoList = new ArrayList();
        
        while (iter.hasNext()) {
            ServiceComponentBean bean = (ServiceComponentBean) iter.next();
            ComponentDef comp = bundleDef.getComponent(bean.getServiceType());    
            orderInfoList.add(new OrderBeanInfo(bean, comp, comp.getId()));
    
        }
                

        return orderInfoList;
    }

    
    public void release() 
    {
        super.release();
        

        bundleDef = null;
    
        scOrderIter = null;

        orderInfoVar = null;
    }



  
    
}

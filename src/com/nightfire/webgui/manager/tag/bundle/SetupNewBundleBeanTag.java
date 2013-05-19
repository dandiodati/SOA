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
import com.nightfire.webgui.core.svcmeta.ActionDef;




/**
 * This tag creates a bundle bean tag with new ServiceComponentBeans.
 * It uses the predefined bundle xml to create the bean.
 *
 */
public class SetupNewBundleBeanTag extends ContainerBeanTagBase implements ManagerServletConstants
{

    protected BundleDef bundleDef;
    
    private XMLPlainGenerator predefinedBundles;

    private String predefinedBundleName;

    private Iterator scBeanIter = null;

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
     * Setter method for the bundle definition object.
     *
     * @param  bundleDef  Bundle definition object.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setBundleDef(Object bundleDef) throws JspException
    {
        setDynAttribute("bundleDef", bundleDef, BundleDef.class);
    }

    /**
     * Setter method for the selected predefined bundle name.
     *
     * @param  name The name of the predefined bundle.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setPredefinedBundleName(String name) throws JspException
    {
        setDynAttribute("predefinedBundleName", name, String.class);
    }

    /**
     * Setter method for the predefined bundle xml.
     *
     * @param  bundles An XMLPlainGenerator that contains the predefined bundle xml.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     */
    public void setPredefinedBundles(Object bundles) throws JspException
    {
        setDynAttribute("predefinedBundles", bundles, Object.class);
    }



    public NFBean createBean() throws JspException
    {
        BundleBeanBag bundleBag = null;
        
        bundleDef = (BundleDef) getDynAttribute("bundleDef"); 
        Object data = (Object)getDynAttribute("predefinedBundles"); 

        try {
            
            if ( data != null) {
                if (data instanceof XMLPlainGenerator) {
                    predefinedBundles = (XMLPlainGenerator) data;
                }
                else if (data instanceof Document) {
                    predefinedBundles = new XMLPlainGenerator((Document) data);
                }
                else if (data instanceof String) {
                    predefinedBundles = new XMLPlainGenerator((String)data);
                }
                else
                    throw new JspTagException("Invalid type [" + data.getClass() +"] passed in for predefinedBundles attribute. Must be of type String,Document, or XMLPlainGenerator.");
            }
        }
        catch (Exception e ) {
            throw new JspException(e.getMessage());
        }
        
        

        predefinedBundleName = (String)getDynAttribute("predefinedBundleName"); 
    
        
        if (bundleDef == null | predefinedBundles == null)
        {
            String errorMessage = "The bundle definition or predefined bundle object is null.";

            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
         
        // build a list of sc beans that need to be created by the inner classes
        scBeanIter = createSCBeans(predefinedBundleName).iterator();

        // set the initial order info object before the first loop of the body.
        if (scBeanIter.hasNext()) {
            OrderBeanInfo entry = (OrderBeanInfo) scBeanIter.next();
            VariableSupportUtil.setVarObj(orderInfoVar, entry, scope, pageContext);
        }
        
        
        // if the bag is null then this is the first loop
        // initialize the bundlebean bag and the list of sc beans to create
        try {  
            bundleBag = new BundleBeanBag(getBeanName());
            bundleBag.setHeaderValue(BUNDLE_DISPLAY_NAME, bundleDef.getDisplayName());
            bundleBag.setHeaderValue(META_DATA_NAME, bundleDef.getMetaDataName());
        }
        catch (ServletException e) {
            throw new JspTagException("Failed to create bundle bean: " + e.getMessage());
        }

        return bundleBag;
        
    }
    

    public int doAfterBody() throws JspException
    {

        // for each OrderBeanInfo object we let the inner tag create the bean
          if (scBeanIter.hasNext()) {

             
            OrderBeanInfo entry = (OrderBeanInfo) scBeanIter.next();

            
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

        // add the edit action as an allowable action
        ComponentDef comp = bundleDef.getComponent(beanName);
        ActionDef edit = comp.getActionDef(ManagerServletConstants.EDIT_ACTION);
        
        if (edit != null &&
            edit.getAllowedInEmptyState()) {
            Set actions = new HashSet();
            actions.add(ManagerServletConstants.EDIT_ACTION);
            ((InfoBodyBase)bean).setAllowableActions(actions);      
        }
                


        ((BundleBeanBag)getBean()).addBean(beanName, (ServiceComponentBean)bean);
    }
    
       
    
    protected List createSCBeans(String predefinedBundleName) throws JspException
    {
        
        List scbeans = new ArrayList();
        
        try {
            
            if (predefinedBundles.exists(predefinedBundleName))
            {
                String bundleDefName = predefinedBundles.getValue(predefinedBundleName + ".BundleDefinition");
                

                String  componentsPath = predefinedBundleName + ".ServiceComponents";
                
                Node[]  components     = predefinedBundles.getChildren(componentsPath);
                
                for (int i = 0; i < components.length; i++)
                {
                    String componentName = components[i].getNodeName();
                    
                    int    count         = 1;
                    String supplier = null;
                    String version = null;
                    
                    if ( predefinedBundles.exists(components[i],"Supplier"))
                        supplier = predefinedBundles.getValue(components[i], "Supplier");

                    if ( predefinedBundles.exists(components[i],"Version"))
                        version = predefinedBundles.getValue(components[i], "Version");

                    
                    try
                    {
                        count = Integer.parseInt(predefinedBundles.getValue(components[i], "Count"));    
                    }
                    catch (Exception e)
                    {
                        log.error("createSCBeans(): Encountered a non-integer component count in predefined bundle definition, at bundle and component [" + predefinedBundleName + ", " + componentName + "].  A 1 will be used instead.");
                    }
    
                    if (log.isDebugEnabled())
                    {
                        log.debug("createSCBeans(): Adding [" + count + "] new [" + componentName + "] service-component bean(s) to the new [" + predefinedBundleName + "] service-bundle bean ...");
                    }

                    for (int j = 0; j < count; j++)
                    {
                        ComponentDef comp = bundleDef.getComponent(componentName);
                        if (comp == null ) 
                            throw new JspTagException("Could not find service component def [" + componentName +"]");
                        
                        scbeans.add(new OrderBeanInfo(comp, supplier, version, componentName));
                    }       
                }
            }
            else
            {
                log.error("createSCBeans(): Failed to locate [" + predefinedBundleName + "] bundle in the predefined-bundle definition document.  An empty bundle will be created instead.");
            }

            
            return scbeans;
        }
        catch (Exception e)
        {
            String errorMessage = "createSCBeans(): Failed to create a [" + predefinedBundleName + "] bundle bean bag based on the predefined-bundle definition document:\n" + e.getMessage();
            
            log.error(errorMessage);
            
            throw new JspException(errorMessage);
        }
    }



 
    public void release() 
    {
        super.release();
        

        bundleDef = null;
    
        predefinedBundles = null;

        predefinedBundleName = null;

        scBeanIter = null;


        orderInfoVar = null;
    }



  
    
}

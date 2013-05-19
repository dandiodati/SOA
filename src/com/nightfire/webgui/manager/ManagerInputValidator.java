/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager;

import  java.lang.*;
import  java.util.*;
import  javax.servlet.*;
import  javax.servlet.http.HttpSession;

import com.nightfire.framework.constants.*;
import  com.nightfire.framework.message.MessageException;
import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.meta.*;
import  com.nightfire.webgui.core.resource.*;
import  com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.manager.ManagerServletConstants;
import com.nightfire.webgui.manager.beans.*;
import  org.w3c.dom.*;
import com.nightfire.framework.debug.*;
import com.nightfire.mgrcore.common.MgrCoreConstants;
import com.nightfire.webgui.core.tag.message.BodyTag;



/**
 * <p><strong>ManagerInputValidator</strong> is the Manager's implementation of 
 * InputValidator interface.  It is used by the GUI framework to validate each
 * request being sent to the manager.  The process currently consists of only
 * validation of required fields specified by the requests' associated meta file.</p>
 */

public class ManagerInputValidator implements InputValidator, ManagerServletConstants
{ 
    public static final String SKIP_VALIDATION_ACTIONS ="skip-validation-actions";
    public static final String VALIDATE_EDITABLE_BEANS ="validate-editable-beans";
    
    private DebugLogger log;
  
    private Set skipValidationActions;

    /*
     * Indicates if validation should occur on InfoBodyBase beans
     * that are not read only.
     * If a bean is readonly(indicated if getActions returns null) then skip doing validation on it.
     * Note this only applies if allowable actions are being set on to the
     * bean. To set this for older managers that do not yet support it,
     * will cause no GUI validation to occur.
     */
    private boolean validateEditableBeans = false;
    
    
 
    public void init(Properties servletInitParams, ServletContext context) throws ServletException
    { 
      	String webAppName = ServletUtils.getWebAppContextPath(context); 

      	log               = DebugLogger.getLogger(webAppName, ManagerInputValidator.class);
        
        // add in a set of all actions that validation should be skipped for.
        // There is a default set, but more can be added via a configuration
        // property
        skipValidationActions = new HashSet();
        
        skipValidationActions.add(MgrCoreConstants.QUERY_SERVICE_BUNDLE_DETAIL_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_SERVICE_BUNDLES_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_ORDERS_ACTION);
        skipValidationActions.add(MgrCoreConstants.SAVE_SERVICE_BUNDLE_ACTION);
        skipValidationActions.add(MgrCoreConstants.SAVE_ORDER_ACTION);
        skipValidationActions.add(MgrCoreConstants.COPY_SERVICE_BUNDLE_ACTION);
        skipValidationActions.add(MgrCoreConstants.ACQUIRE_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.RELEASE_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.COMPLETE_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_AVAILABLE_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_ACQUIRED_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_COMPLETED_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_WORKITEM_ACTION);
        skipValidationActions.add(MgrCoreConstants.ASSIGN_USERS_WORKITEMS_ACTION);
        skipValidationActions.add(MgrCoreConstants.UPDATE_ORDER_STATUS_ACTION);
        skipValidationActions.add(MgrCoreConstants.GET_SPI_MESSAGE_HISTORY_ACTION);
        skipValidationActions.add(MgrCoreConstants.GET_SPI_MESSAGE_DETAIL_ACTION);

        skipValidationActions.add(MgrCoreConstants.BO_CHANGED_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_SERVICE_BUNDLE_SUMMARY_ACTION);
        skipValidationActions.add(MgrCoreConstants.QUERY_FULL_ORDER_DETAIL_ACTION);
        

        String actions = servletInitParams.getProperty(SKIP_VALIDATION_ACTIONS);

        
        if (StringUtils.hasValue(actions) ) {
            
            StringTokenizer toker = new StringTokenizer(actions, ",");
            while (toker.hasMoreTokens() ) {
                String action = toker.nextToken().trim();
                skipValidationActions.add(action);
            }
        }

        String validateEditableBeansStr = servletInitParams.getProperty(VALIDATE_EDITABLE_BEANS);
        validateEditableBeans = StringUtils.getBoolean(validateEditableBeansStr, validateEditableBeans);
        
    }
        

    public DataHolder validate(DataHolder guiRequest, DataHolder outputRequest, HttpSession session) throws ServletException
    {
        log.debug("validate(): Performing GUI validation on the request ...");
        
        XMLGenerator headerGen        = new XMLPlainGenerator(guiRequest.getHeaderDom());
        
        XMLGenerator bodyGen          = new XMLPlainGenerator(guiRequest.getBodyDom());
        
        KeyTypeMap   messageDataCache = (KeyTypeMap)session.getAttribute(ServletConstants.MESSAGE_DATA_CACHE);
      
        try 
        {          
            String messageID = headerGen.getValue(ServletConstants.MESSAGE_ID);
              
            String action    = headerGen.getValue(PlatformConstants.ACTION_NODE);
            
        
            NFBean requestBean    = (NFBean)messageDataCache.get(messageID, ServletConstants.REQUEST_BEAN);
            // skip at a latter stage since we still want to validate
            if (skipValidationActions.contains(action) && !(requestBean instanceof InfoBodyBase))
            { 
                if (log.isDebugEnabled())
                {
                    log.debug("validate(): Skipping GUI validation since action is [" + action + "] ...");
                }
                
                return null;
            }    

            BundleBeanBag  bundleBeanBag    = (BundleBeanBag)messageDataCache.get(messageID, ManagerServletConstants.SERVICE_BUNDLE_BEAN_BAG);
                          
            XMLGenerator   validationErrors = null;

	        ServletContext servletContext   = session.getServletContext();
			//Added SAVE_SERVICE_BUNDLE_ACTION since the save at the bundle level was throwing a NullPointerException
              
            if (action.equals(VALIDATE_SERVICE_BUNDLE_ACTION) || action.equals(NEW_SERVICE_BUNDLE_ACTION) || action.equals(SAVE_SERVICE_BUNDLE_ACTION))
            {
                validationErrors = validateBundleBag(bundleBeanBag, servletContext, action);
            }
            else 
            {
                String               scid          = headerGen.getValue(ManagerServletConstants.SCID_FIELD);
                
    		    InfoBodyBase componentBean = (InfoBodyBase)bundleBeanBag.getBean(scid);
      
      		    String               serviceType   = componentBean.getServiceType();

		        String               bundleStatus  = bundleBeanBag.getHeaderValue(STATUS_FIELD);
        		
 		        if (StringUtils.hasValue(bundleStatus) && !bundleStatus.equals("Initial"))
		        {
               	    validationErrors = validateServiceComponent(guiRequest, bundleBeanBag, componentBean, serviceType, servletContext, action);
		        }
		        else
		        {
                    validationErrors = validateServiceComponent(bundleBeanBag, componentBean, serviceType, servletContext, action);
		        }
            }
           
            if (validationErrors != null) 
            {
                XMLGenerator header = new XMLPlainGenerator(ServletConstants.ERR_HEADER_DOC_NAME);
                
                header.setValue(ServletConstants.RESPONSE_CODE_NODE, ManagerServletConstants.FAILURE);
              
                return new DataHolder(header.getOutputDOM(), validationErrors.getOutputDOM());
            } 

            return null;
        } 
        catch (MessageException e)
        {
            String errorMessage ="Failed to perform GUI request validation:\n" + e.getMessage();
            
            log.error(errorMessage);
            
            throw new ServletException(errorMessage, e);
        }
    }
  
  /**
   * Create validation errors for a service component or modifier bean.
   *
   * @param bag The bundle bag which contains the bean to be validated.
   * @param componentBean The service component bean.
   * @param serviceType The type of ths service component.
   * @param context a <code>ServletContext</code> value
   * @return a <code>XMLGenerator</code> value
   * @exception MessageException if an error occurs
   * @exception ServletException if an error occurs
   */
    private XMLGenerator validateServiceComponent(BundleBeanBag bag, InfoBodyBase componentBean, String serviceType, ServletContext context, String action) throws MessageException, ServletException
  {


      
        if (log.isDebugEnabled())
        {
            log.debug("Performing biz rule validation on service component with id [" + componentBean.getId() + "] ...");    
        }

        // use parent componentbean to obtain index count if this is a modifier bean.
      if ( componentBean instanceof ModifierBean) 
          componentBean = ((ModifierBean)componentBean).getParent();
    
    	XMLGenerator errors = null;
    
    	String       xml    = getServiceComponentErrors(componentBean, context, action);
          
    	if (xml != null)
	{
      		errors               = new XMLPlainGenerator("Body");
    
      		List beans           = bag.getBeans(serviceType );
      
      		int  index           = beans.indexOf(componentBean);
      
      		Node serviceTypeNode = errors.create(serviceType);

  		errors.setAttribute(serviceTypeNode, "index", String.valueOf(index));

      		errors.setNodeValue(serviceTypeNode,xml);
    	}
    
    	return errors;
  }
  
    private XMLGenerator validateServiceComponent(DataHolder guiRequest, BundleBeanBag bundleBeanBag, InfoBodyBase origComponentBean, String serviceType, ServletContext context, String action) throws MessageException, ServletException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Performing GUI validation on service component with id [" + origComponentBean.getId() + "] ...");    
        }


        // use parent componentbean to obtain index count if this is a modifier bean.
        if ( origComponentBean instanceof ModifierBean) 
          origComponentBean = ((ModifierBean)origComponentBean).getParent();
    
                
	    ServiceComponentBean componentBean = new ServiceComponentBean(guiRequest.getHeaderStr(), guiRequest.getBodyStr());
        // copy over service type and actions to new bean
        componentBean.setServiceType(origComponentBean.getServiceType());
        componentBean.setAllowableActions(origComponentBean.getAllowableActions());
        
	    XMLGenerator         headerParser  = new XMLPlainGenerator(guiRequest.getHeaderStr());

	    String               metaPath      = headerParser.getValue("metaResource"); 

	    if (log.isDebugEnabled())
	    {
		    log.debug("validateServiceComponent(DataHolder, BundleBeanBag, ServiceComponentBean, String, ServletContext): Setting a new meta path in the newly created ServiceComponentBean instance: [" + metaPath + "] ...");
	    }

	    componentBean.setHeaderValue(ServletConstants.META_DATA_PATH, metaPath);

    	String       xmlErrors = getServiceComponentErrors(componentBean, context, action);

      	XMLGenerator errors    = null;
    
        if (xmlErrors != null)
	    {
      		List   beans           = bundleBeanBag.getBeans(serviceType);
		
      		int    index           = beans.indexOf(origComponentBean);

      		errors                 = new XMLPlainGenerator("Body");

      		Node   serviceTypeNode = errors.create(serviceType);
      
      		errors.setAttribute(serviceTypeNode, "index", String.valueOf(index));

      		errors.setNodeValue(serviceTypeNode, xmlErrors);

		    if (log.isDebugEnabled())
		    {
			    log.debug("validateServiceComponent(DataHolder, BundleBeanBag, ServiceComponentBean, String, ServletContext): The validation error set obtained is:\n" + errors.getOutput());
		    }
	    }
      
      	return errors;
    }

  /**
   * Goes through each service component bean and and finds all missing 
   * required fields( These are fields which are not within a optional
   * or repeating MessageContainer.
   *
   * @param serviceBundleBag The service bundle bag
   * @param context a <code>ServletContext</code> value
   * @return a <code>XMLGenerator</code> value which contains the xml errors
   * If no errors is found the null is returned.
   * @exception MessageException if an error occurs
   * @exception ServletException if an error occurs
   */
    private XMLGenerator validateBundleBag(BundleBeanBag serviceBundleBag, ServletContext context, String action) throws MessageException, ServletException
    {
        log.debug("validateBundleBag(): Performing GUI validation on the whole service bundle ...");    

      XMLGenerator errors = new XMLPlainGenerator("Body");
      boolean errorsFound = false;
      
      Iterator iter = serviceBundleBag.getBeanGroups().iterator();

      
      while (iter.hasNext() ) {
        String group = (String) iter.next();
        log.debug("Looking at bean group [" + group +"]");
        // the count of instances of this service component
        int count = 0;
         
        // the xml index to add the service component at
        // this is only incremented when there are errors
        int xmlIndex = 0; 
      
        Iterator beans = serviceBundleBag.getBeans(group).iterator();
        
        while (beans.hasNext() ) {
          
          ServiceComponentBean bean = (ServiceComponentBean) beans.next();
               
          String xml = getServiceComponentErrors(bean, context, action);
          
          if ( xml != null) {
            errorsFound = true;

            Node grpNode = errors.create(group +"(" + xmlIndex +")");
            errors.setAttribute(grpNode, "index",String.valueOf(count));
            errors.setNodeValue(grpNode, xml);
            // increment instance count if there was are xml errors
            xmlIndex++;
          }
          // on the next bean instance
          count++;
        }
       
      }
       
      if ( errorsFound )
       return errors;
      else 
        return null;
      
    }

  
  /**
   * Gets the errors for a single service component bean.
   *
   *
   * @param bean a <code>ServiceComponentBean</code> value
   * @param context a <code>ServletContext</code> value
   * @return a <code>String</code> value
   * @exception MessageException if an error occurs
   * @exception ServletException if an error occurs
   */
    private String getServiceComponentErrors(InfoBodyBase bean, ServletContext context, String action) throws MessageException, ServletException
    {

        Map rFields = null;
        
        // If we are not validating editable beans then always try to obtain requred
        // fields.
        // else if we are validating editable beans only , then only
        // validate when allowable actions are not null. Null indicates
        // that the data only contains read only data.
        if (!validateEditableBeans || bean.getAllowableActions() != null) {    
          String       metaResource = bean.getHeaderValue(ServletConstants.META_DATA_PATH);
         
          Message msg = (Message)ServletUtils.getLocalResource(context, metaResource, ServletConstants.META_DATA, true);
          
          rFields = msg.getRequiredFields();


          // if this is a action to be skipped then just add the minumum set of fields 
          // that must be entered by the user.
          if (skipValidationActions.contains(action)) {
              Iterator fIter = rFields.values().iterator();
              
              Map minReqFields = new HashMap();
              
              while (fIter.hasNext()) {
                  Field f = (Field)fIter.next();
                  String alwaysRequired = f.getCustomValue(BodyTag.ALWAYS_REQUIRED); 
                  if (StringUtils.getBoolean(alwaysRequired, false)) 
                      minReqFields.put(f.getId(), f);
              }
              // point the required fields to the minimum required fields.
              rFields = minReqFields;
              
              
              log.debug("There is " + rFields.size() +" minimum required field(s) for this component bean [" + bean.getId() +"]");    
          }
          else
              log.debug("There is " + rFields.size() +" required field(s) for this component bean [" + bean.getId() +"]");
        }
        
          return InputValidationUtils.validateReqFields((XMLGenerator)bean.getBodyDataSource() , rFields);
    }
  


  
  
}

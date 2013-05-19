/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager;

import  java.util.*;
import  java.net.*;

import  javax.servlet.*;
import  javax.servlet.http.*;

import  org.w3c.dom.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.common.xml.*;

import  com.nightfire.webgui.manager.beans.*;
import  com.nightfire.webgui.core.beans.*;
import  com.nightfire.webgui.core.resource.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.core.meta.*;
import  com.nightfire.webgui.core.*;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.debug.*;
import com.nightfire.spi.common.*;



/**
 * <p><strong>ManagerDataAdapter</strong> is the Manager's implementation of 
 * DataAdapter interface.  It is used by the GUI framework to transform the
 * request and response, to and from the Manager's processing layer.</p>
 */

public class ManagerDataAdapter implements DataAdapter, ManagerServletConstants, ServletConstants, PlatformConstants
{  
    private DebugLogger log;
  

    public void init(Properties servletInitParams, ServletContext context) throws ServletException
    {
      String webAppName = ServletUtils.getWebAppContextPath(context); 
      log = DebugLogger.getLogger(webAppName, ManagerDataAdapter.class);
    }
        
    /**
     * This method transforms the request-data Bean into a request object of type
     * which the processing layer is expecting.
     *
     * @param  requestData     Request data Bean.
     * @param  servletRequest  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  A request DataHolder which the processing layer is expecting.
     */
    public DataHolder transformRequest(NFBean requestDataBean, HttpServletRequest servletRequest) throws ServletException
    {
        HttpSession session     = servletRequest.getSession();
        
        String      action      = requestDataBean.getHeaderValue(ACTION_NODE);

        Map beanMetaPaths = new HashMap();
        
        
        try
        {
            long         totalTime         = Performance.startTiming(Debug.BENCHMARK);
            
            // Build the request header.
            
            XMLGenerator headerGenerator    = new XMLPlainGenerator(HEADER_NODE);
            
            String       responseCodePrefix = requestDataBean.getHeaderValue(RESPONSE_CODE_PREFIX_NODE);
            
            headerGenerator.setValue(ACTION_NODE, action);
            
			String		 appBizRules	  = requestDataBean.getHeaderValue(HeaderNodeNames.APPLY_BUSINESS_RULES_NODE);
			
			if(StringUtils.hasValue(appBizRules)){
				headerGenerator.setValue(HeaderNodeNames.APPLY_BUSINESS_RULES_NODE, appBizRules);
			}        
            
			// Build the request body.
            
            XMLGenerator bodyGenerator;

            KeyTypeMap   messageDataCache = (KeyTypeMap)session.getAttribute(ServletConstants.MESSAGE_DATA_CACHE);
            
            String       messageID        = requestDataBean.getHeaderValue(ServletConstants.NF_FIELD_HEADER_MESSAGE_ID_TAG);

            if (action.equals(NEW_SERVICE_BUNDLE_ACTION) || action.equals(SAVE_SERVICE_BUNDLE_ACTION) || action.equals(VALIDATE_SERVICE_BUNDLE_ACTION))
            {
                bodyGenerator = processServiceBundleRequest(requestDataBean, messageDataCache, messageID, session, beanMetaPaths);
            }
            else
            {
                bodyGenerator = processRequest(requestDataBean, messageDataCache, messageID, session, beanMetaPaths);
            }

            String requestHeader = headerGenerator.getOutput();

            // Filter out empty nodes from the request body.
            
            long        filterTime = Performance.startTiming(Debug.BENCHMARK);
            
            XPathFilter filter     = new XPathFilter();
            
            filter.setAttributeFilter(new String[] {"value", "type"});

             // match any nodes which have value attributes that are not empty
            // and match any nodes that have a type attribute and has children
            filter.addXPathPattern("//*[@value != '' or ( @type and descendant::*/@value != '' ) ]");

            
            filter.addXPathPattern("/*/*/*[not(@value)]");
            
            String      requestBody = filter.filter(bodyGenerator).getOutput();

            Performance.stopTiming(Debug.BENCHMARK, filterTime, "ManagerDataAdapter.transformRequest(): Time taken to filter out empty nodes.");

            Performance.stopTiming(Debug.BENCHMARK, totalTime, "ManagerDataAdapter.transformRequest(): Total request transformation time.");


		    if (log.isDebugEnabled())
		    {
		        log.debug("ManagerDataAdapter.transformRequest(): Generated the following request header:\n" + requestHeader);
        
		        log.debug("ManagerDataAdapter.transformRequest(): Generated the following request body:\n" + requestBody);
		    }

            return new DataHolder(requestHeader, requestBody);
        }
        catch (ServletException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ManagerDataAdapter.transformRequest(): Failed to generate the header and body request data:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }

    }
        
    /**
     * Processes the bundle request body.
     *
     * @param  requestDataBean   Data Bean containing the raw request header and body data.
     * @param  messageDataCache  Cache where all session's messages are kept.
     * @param  messageID         Message ID of the data set to be processed.
     * @param  HttpSession       HttpSession object.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     *
     * @return  An XMLGenerator object for the request body.
     */
    private XMLGenerator processServiceBundleRequest(NFBean requestDataBean, KeyTypeMap messageDataCache, String messageID, HttpSession session, Map beanMetaPaths) throws Exception
    {
        NFBeanBag serviceBundleBeanBag = (NFBeanBag)messageDataCache.get(messageID, ManagerServletConstants.SERVICE_BUNDLE_BEAN_BAG);

        if (serviceBundleBeanBag == null)
        {
            String errorMessage = "ERROR: ManagerDataAdapter.processServiceBundleRequest(): Service-bundle bean bag does not exist in the session cache.";
                
            log.error(errorMessage);
                
            throw new ServletException(errorMessage);
        }

        // Make a copy of the bundle bean bag so the orginal request remains intact.
        
        long          time                     = Performance.startTiming(Debug.BENCHMARK);

        BundleBeanBag serviceBundleBeanBagCopy = (BundleBeanBag)serviceBundleBeanBag.getFinalCopy();
        
        Performance.stopTiming(Debug.BENCHMARK, time, "ManagerDataAdapter.processServiceBundleRequest(): Time taken to copy the bundle bean bag.");

        // Copy over the meta data paths to each bean's custom data map so that
        // the value is not lost during transformation.  This gets used by the
        // biz rule mappings to find default mappings.
        
        maintainMetaDataPaths(serviceBundleBeanBagCopy, beanMetaPaths);

        // Transform the bundle bean bag copy.
        
        time = Performance.startTiming(Debug.BENCHMARK);
        
        serviceBundleBeanBagCopy.transform();
        
        Performance.stopTiming(Debug.BENCHMARK, time, "ManagerDataAdapter.processServiceBundleRequest(): Time taken the transformed the bundle bean bag.");

        // Use the transformed copy to add each bean as a mapping to a map of biz 
        // rule mappers.  This must be done after the transformation so the mapping 
        // uses the transformed bean with ids.
        
        time            = Performance.startTiming(Debug.BENCHMARK);
        
        Map bizMappings = loadBizRuleBundleMap(serviceBundleBeanBagCopy, session, beanMetaPaths);
        
        messageDataCache.put(messageID,this.BIZ_RULE_MAPPINGS, bizMappings);
        
        Performance.stopTiming(Debug.BENCHMARK, time, "ManagerDataAdapter.processServiceBundleRequest(): Time taken to create business rule mappings.");

        // Compose the bundle bean bag into a single xml and strip off ids.
        
        time            = Performance.startTiming(Debug.BENCHMARK);
        
        Document output = serviceBundleBeanBagCopy.compose();
        
        Performance.stopTiming(Debug.BENCHMARK, time, "ManagerDataAdapter.processServiceBundleRequest(): Time taken to compose the bundle bean bag into one single xml.");

        return new XMLPlainGenerator(output);
    }

    // addds the meta data path as a custom map value for each bean so that
    // it is not lost in transformation
    private void maintainMetaDataPaths(BundleBeanBag serviceBundleBag, Map beanMetaPaths)
    {
       Iterator beans = serviceBundleBag.getBodyAsMap().values().iterator();

       while (beans.hasNext() )  {
          InfoBodyBase bean = (InfoBodyBase) beans.next();
          maintainMetaDataPath(bean, beanMetaPaths);
       }
    }


    private void maintainMetaDataPath(InfoBodyBase bean, Map beanMetaPaths)
    {
        
      String metaPath = bean.getHeaderValue(ServletConstants.META_DATA_PATH);
      beanMetaPaths.put(bean.getId(), metaPath);
      
    }

    /**
     * Processes the non-bundle request body.
     *
     * @param  requestDataBean   Data Bean containing the raw request header and body data.
     * @param  messageDataCache  Cache where all session's messages are kept.
     * @param  messageID         Message ID of the data set to be processed.
     * @param  HttpSession       HttpSession object.
     *
     * @exception  Exception  Thrown when an error occurs during processing.
     *
     * @return  An XMLGenerator object for the request body.
     */
    private XMLGenerator processRequest(NFBean requestDataBean, KeyTypeMap messageDataCache, String messageID, HttpSession session, Map beanMetaPaths) throws Exception
    {
        XMLPlainGenerator result = null;

        // If this is a service component bean then call compose() on the bean and 
        // return its data, else just return its data.
        
        if (requestDataBean instanceof InfoBodyBase)  
        {
		    log.debug("ManagerDataAdapter.processRequest(): Processing a InfoBodyBase bean ...");
            
            // Copy over the meta data paths to each bean's custom data map so that
            // the value is not lost during transformation.  This gets used by the
            // biz rule mappings to find default mappings.
           
            maintainMetaDataPath((InfoBodyBase)requestDataBean,beanMetaPaths);
            
            
            
            // Transform the bean's data for sending.
               
            requestDataBean.transform();
            
            
            // Use the transformed bean to create the mapping.
           
            BizRuleMapper bizMapping    = loadBizRuleMap((InfoBodyBase)requestDataBean, session, beanMetaPaths);
            
            
            Map           singleMapping = new HashMap();
            
            singleMapping.put(((InfoBodyBase)requestDataBean).getId(), bizMapping);
            
            messageDataCache.put(messageID, BIZ_RULE_MAPPINGS, singleMapping);
            
            
            // Compose the service component bean into a single xml.
          
            Document resultDom = ((InfoBodyBase)requestDataBean).compose();

            result = new XMLPlainGenerator(resultDom);
            
            
        } 
        else 
        {
		    log.debug("ManagerDataAdapter.processRequest(): Processing a non-InfoBodyBase bean ...");
            
            requestDataBean.transform();
            
            result = (XMLPlainGenerator)requestDataBean.getBodyDataSource();
        }

        return result;
    }






    private Map loadBizRuleBundleMap(BundleBeanBag serviceBundleBag, HttpSession session,Map beanMetaPaths) throws FrameworkException, ServletException
    {
       HashMap map = new HashMap();

       Iterator iter = serviceBundleBag.getBodyAsMap().values().iterator();

       while (iter.hasNext() ) {

          InfoBodyBase bean = (InfoBodyBase) iter.next();
          BizRuleMapper mapper = loadBizRuleMap(bean, session, beanMetaPaths);

          map.put(bean.getId(), mapper);

       }

       return map;


    }



    private BizRuleMapper loadBizRuleMap(InfoBodyBase bizRuleBean, HttpSession session, Map beanMetaPaths) throws ServletException
    {


        String metaPath = (String) beanMetaPaths.get(bizRuleBean.getId());
        

        String fullMetaPath = ServletUtils.getLocalResourceFullPath(session, metaPath);
       
       
 
       // make sure the meta resource got loaded
       // if the messages are never edited then the meta file was not loaded
       // load it here
       if (!ServletUtils.isLocalResourceLoaded(session, metaPath) )
          ServletUtils.getLocalResource(session, metaPath, ServletConstants.META_DATA);

       
       Map mappings = (Map) session.getServletContext().getAttribute(ServletConstants.BIZ_RULE_DEFAULT_MAPPINGS);
       log.debug("Trying to obtain biz rule mapping [" + fullMetaPath +"]");
       BizRuleMapper defaultMapping = (BizRuleMapper) mappings.get(fullMetaPath);

        try {

          if (!defaultMapping.isFinalState() )
             defaultMapping.updateDefault(bizRuleBean.getBodyTransform());

          BizRuleMapper newMapper = defaultMapping.getNewInstance((XMLGenerator)bizRuleBean.getBodyDataSource() );

          return newMapper;


       } catch (FrameworkException e) {
          String err =  "ManagerDataAdapter: Failed to create biz rule mapper: " + e.getMessage();
          log.error(err );
          throw new ServletException(err);
       }


    }


    /**
     * This method transforms the response obtained from the back end into a
     * DataHolder object.
     *
     * @param  responseData  Response data object.
     * @param  session       HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  A DataHolder object, which basically composes of a response header 
     *          XML and a response body XML. 
     */
    public DataHolder transformResponse(DataHolder responseData, HttpSession session) throws ServletException
    {
        return responseData;
    }
}

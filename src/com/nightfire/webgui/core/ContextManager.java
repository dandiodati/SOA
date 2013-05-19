/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/ContextManager.java#1 $
 */

package com.nightfire.webgui.core;

import  java.io.*;
import  java.lang.*;
import  java.net.*;
import  java.util.*;
import  javax.servlet.*;
import  org.w3c.dom.*;


import  com.nightfire.framework.db.*;
import  com.nightfire.framework.message.MessageException;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.util.*;
import  com.nightfire.webgui.core.beans.*;
import  com.nightfire.framework.debug.*;
import  com.nightfire.webgui.core.meta.*;
import  com.nightfire.webgui.core.meta.help.*;
import  com.nightfire.webgui.core.resource.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.framework.webapp.BasicContextManager;


/**
 * <p><strong>ContextManager</strong> performs necessary initialization and
 * cleanup tasks during servlet-container startup and shutdown processes.</p>
 */
 
public class ContextManager extends BasicContextManager implements ServletContextListener
{    
  
    private static final String SVC_HANDLER_ACTIONS_CONTEXT_PARAM   = "SVC_HANDLER_ACTIONS";
    public static final String SVC_HANDLER_ACTIONS     = "SvcHandlerActions";


    /**
     * Implementation of ServletContextListener's contextInitialized().  This
     * gets called by the servlet container to notify that the web application
     * is ready to process requests.
     *
     * @param  event  ServletContextEvent object.
     */
    public void contextInitialized(ServletContextEvent event)
    {
      super.contextInitialized(event);
        
      servletContext = event.getServletContext();
      
 
     
 
      // create a default converter for intialization of the webapp before 
      // cid is available. Used when there is no session available
      WebAppURLConverter c = new WebAppURLConverter(servletContext);
      servletContext.setAttribute(ServletConstants.DEFAULT_RESOURCE_URL_CONVERTER, c);   
 

        
        // Sets up the resource-data cache in the servlet context.

        String  resourceDataRoot               = initParameters.getProperty(ServletConstants.RESOURCE_DATA_ROOT);

        String  resourceCheckTimeStr = initParameters.getProperty(ServletConstants.RESOURCE_RELOAD_TIME);

        long checkTimeSec = 0;
        
        try {
            checkTimeSec = Long.parseLong(resourceCheckTimeStr);
        }
        catch (Exception e) {
            checkTimeSec = 0;
        }

        //Read IDLE_CLEANUP_TIME if configured (defaults to 3 hr)
        String  idleCleanupTimeStr = initParameters.getProperty(ServletConstants.IDLE_CLEANUP_TIME);

        long idleCleanupTimeSec = 0;
        if (StringUtils.hasValue(idleCleanupTimeStr))
        {
            idleCleanupTimeSec = Long.parseLong(idleCleanupTimeStr);
        }

        String enc = initParameters.getProperty(ServletConstants.CHAR_ENCODING);


        
        log.info("contextInitialized(): Production mode set to [" + prodMode + "]");

        ResourceDataCache dataCache = new ResourceDataCache(resourceDataRoot, !prodMode, checkTimeSec, enc, idleCleanupTimeSec);
        log.info("contextInitialized(): Adding ResourceDataCache to servlet context.");
        servletContext.setAttribute(ServletConstants.RESOURCE_DATA_CACHE, dataCache);

        // set the factory class
        dataCache.setResourceTransformerFactory(new WebAppResourceFactory(servletContext));


        String  helpDir = initParameters.getProperty(ServletConstants.HELP_DIRECTORY);
        helpDir = servletContext.getRealPath(helpDir);

        log.info("Setting help directory [" + helpDir +"].");

        if ( StringUtils.hasValue(helpDir) ) {
           HelpListener hl = new HelpListener(helpDir);
           dataCache.addResourceChangeListener(hl, ServletConstants.META_DATA);

        }

        // create default map to holder default biz mappings
        Map bizMap =  Collections.synchronizedMap( new HashMap() );
        servletContext.setAttribute(ServletConstants.BIZ_RULE_DEFAULT_MAPPINGS, bizMap);

        // add a biz rule listener to create default biz mappings
        BizRuleTemplateListener btml = new BizRuleTemplateListener(servletContext);
        dataCache.addResourceChangeListener(btml, ServletConstants.META_DATA);


        //Initialize actions that map to service handler execution.
        initializeSvcHandlerActionLookup(servletContext);
        
    }
    
    /**
     * Sets up an application-wide lookup which allows servlet components to
     * determine whether to send request directly to the service handler or
     * via the regular UI Connector route.
     *
     * @param  servletContext  ServletContext object.
     */
    private void initializeSvcHandlerActionLookup(ServletContext servletContext) 
    {
    	String configFilePath = initParameters.getProperty(SVC_HANDLER_ACTIONS_CONTEXT_PARAM);

        if (!StringUtils.hasValue(configFilePath)) {
            log.info("initializeBundleDefLookup(): Context initialization parameter [" + SVC_HANDLER_ACTIONS_CONTEXT_PARAM + "] does not exist in web.xml.  All requested actions will go through UI Connector.");
        }
        else {
            List actionList   = new ArrayList();

            List roActionList = Collections.unmodifiableList(actionList);

            servletContext.setAttribute(SVC_HANDLER_ACTIONS, roActionList);

            try {
                URL resourceURL = servletContext.getResource(configFilePath);

                if (resourceURL == null) {
                    log.error("initializeSvcHandlerActionLookup(): The path name given by the context initialization parameter [" + SVC_HANDLER_ACTIONS_CONTEXT_PARAM + "] in web.xml does not map to a resource.");
                }
                else {
                    XMLGenerator parser = new XMLPlainGenerator(resourceURL.openStream());

                    if (log.isDebugEnabled()) {
                        log.debug("initializeSvcHandlerActionLookup(): The service-handler-action XML has the following content:\n" + parser.describe());
                    }

                    Node[] actionNodes = parser.getChildren("ActionContainer");

                    for (int i = 0; i < actionNodes.length; i++) {
                        String action = parser.getNodeValue(actionNodes[i]);

                        if (!actionList.contains(action)) {
                            if (log.isDebugEnabled()) {
                                log.debug("initializeSvcHandlerActionLookup(): Adding service-handler action [" + action + "] to the lookup ...");
                            }

                            actionList.add(action);
                        }
                    }
                }
            }
            catch (MalformedURLException  e) {
                log.error("initializeSvcHandlerActionLookup(): Context initialization parameter [" + SVC_HANDLER_ACTIONS_CONTEXT_PARAM + "] in web.xml and does not have a valid path name:\n" + e.getMessage());
            }
            catch (Exception e) {
                log.error("initializeSvcHandlerActionLookup(): Failed to create an XMLPlainGenerator from the given service-handler-action XML [" + configFilePath + "]:\n" + e.getMessage());
            }
        }
    }


    private class HelpListener implements ResourceChangeListener
    {

       private String helpDir;

       public HelpListener(String helpDir)
       {
          this.helpDir = helpDir;
       }
       /**
        * A convenience function which gets called during registration.  It normally
        * contains initialization code.
        */
        public void register()
        {
        }

       /**
        * This function gets called when a resource has changed.
        *
        * @param  event  A ResourceChangeEvent object which describes the event and
        *                the resource which has changed.
        */
       public void resourceChange(ResourceChangeEvent event)
       {

         // Get the meta resource name   
         String resourceName = event.getResourceName ();
         Message msg = (Message) event.getNewValue();
         HelpCreator creator = new HelpCreator();
         log.info("Trying to create help pages for resource [" + resourceName + "]");

         try {
                 File helpDirFile = null;
                 // get the supplier from tha meta resource name.
                 String supplier = ServletUtils.getResourceSupplier ( resourceName );
                 String service = ServletUtils.getResourceService ( resourceName );
                 //If supplier value exist in the resource name then append the supplier name in the help dir path.
                if ( StringUtils.hasValue(service) && StringUtils.hasValue(supplier))
                {
                    helpDirFile =  new File(helpDir + "/" +  msg.getID() + "/" + service + "-" + supplier);
                    creator.createHelp(msg,helpDirFile, helpDirFile.getParentFile().getParentFile());
                }
                else if ( StringUtils.hasValue(supplier) )
                {
                    helpDirFile =  new File(helpDir + "/" +  msg.getID() + "/" + supplier);
                    creator.createHelp(msg,helpDirFile, helpDirFile.getParentFile().getParentFile());
                }
                else
                {
                    helpDirFile =  new File(helpDir + "/" +  msg.getID());
                    creator.createHelp(msg,helpDirFile, helpDirFile.getParentFile());
                }
            }
           catch (ServletException e) {
           log.error("Failed to create help pages for resource: " + resourceName + ".  Could not obtain Supplier value from resource.");
           }
            catch (FrameworkException e) {
            log.error("Failed to create help pages for resource: " + resourceName);
         }
       }

       /**
        * A convenience function which gets called during deregistration.  It normally
        * contains cleanup code.
        */
       public void deregister()
       {
       }

    }

    /**
     * Handles loading biz rule template maps based on each meta file.
     * Provides a default set of biz rule error mappings.
     */
     private class BizRuleTemplateListener implements ResourceChangeListener
    {

        private ServletContext context;

        public BizRuleTemplateListener(ServletContext context)
        {
           this.context = context;
        }


       /**
        * A convenience function which gets called during registration.  It normally
        * contains initialization code.
        */
        public void register()
        {
        }

       /**
        * This function gets called when a resource has changed.
        *
        * @param  event  A ResourceChangeEvent object which describes the event and
        *                the resource which has changed.
        */
       public void resourceChange(ResourceChangeEvent event)
       {

         String msgName =  event.getResourceName();
         Message meta = (Message) event.getNewValue();


         Map  templateMap = (Map) context.getAttribute(ServletConstants.BIZ_RULE_DEFAULT_MAPPINGS);

         XMLPlainGenerator tData = null;
         try {
            SampleXMLGenerator gen = new SampleXMLGenerator(meta, SampleXMLGenerator.STYLE_TEST_XSL );
            gen.setRepeatCount(1);
            tData = new XMLPlainGenerator(gen.generate() );
            BizRuleMapper mapper = new BizRuleMapper(servletContext,tData);
           
            templateMap.put(msgName, mapper );
         } catch (Exception  e) {
            log.error("Could not add default biz rule mapping for resource [" + msgName+ "]: " + e.getMessage());
            return;
         }

         if( log.isDebugDataEnabled())
          log.debugData("Added default biz rule mapping  for resource [" + msgName+ "]\n " + tData.describe() );


       }


    
       /**
        * A convenience function which gets called during deregistration.  It normally
        * contains cleanup code.
        */
       public void deregister()
       {
       }

    }
}

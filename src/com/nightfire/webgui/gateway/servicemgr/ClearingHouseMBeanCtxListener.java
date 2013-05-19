package com.nightfire.webgui.gateway.servicemgr;

import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.nightfire.framework.jmx.cmn.JMXMonitoringManager;
import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.MultiJVMPropUtils;
import com.nightfire.webgui.core.ServletConstants;

/**
 * Context Listener to register/unregister ClearingHouse MBeans
 */
public class ClearingHouseMBeanCtxListener implements ServletContextListener {

    private static DebugLogger log = DebugLogger.getLogger("/servicemgr", ClearingHouseMBeanCtxListener.class);

    private static final String GWS_SERVER_NAME = "GWS_SERVER_NAME";

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (log.isDebugEnabled())
            log.debug("Registering Clearing House MBeans...");

        String gwsServerName = null;

        try {
        	ServletContext servletContext = servletContextEvent.getServletContext();
        	String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
           	Properties initParameters=null;
   			try {
   				initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
   			} catch (FrameworkException fe) {
   				log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
   				throw new FrameworkException(fe);
   			}            
            gwsServerName = initParameters.getProperty("GWS_SERVER_NAME");
            if (!StringUtils.hasValue(gwsServerName)) {
                throw new Exception("Clearinghouse Gateway Server Name " + GWS_SERVER_NAME + " not found in deployment descriptor web.xml.");
            }
            //Register mbeans
            JMXMonitoringManager.getInstance().registerMbeans(gwsServerName);

            if (log.isDebugEnabled())
                log.debug("Done registering...");
        }
        catch (Exception e) {
            log.warn("Could not register MBeans... " + e.getMessage(), e);
        }

    }

    public void contextDestroyed(ServletContextEvent event) {
        try {

            //unregister mbeans
            JMXMonitoringManager.getInstance().unregisterMbeans();
        }
        catch (Exception e) {
            log.warn("An Exception occured while unregistering MBeans.." + e.getMessage(), e);
        }
    }
}

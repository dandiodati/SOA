package com.nightfire.webgui.gateway.servicemgr;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.nightfire.comms.servicemgr.ServerManagerFactory;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.MultiJVMPropUtils;
import com.nightfire.webgui.core.ServletConstants;

public class JMSQueueContextListener implements ServletContextListener {

	public static final String GWS_SERVER_NAME = "GWS_SERVER_NAME";

	private static DebugLogger log = DebugLogger.getLogger("/servicemgr", JMSQueueContextListener.class);

	public void contextInitialized(ServletContextEvent servletContextEvent) {
		
		
		String gwsServerName = null;
		log.debug("Starting all servicemgr services.");
		try {

			log.debug("Initialising database connection pools..");
			DBConnectionPool.initializePoolConfigurations();

			ServletContext servletContext = servletContextEvent.getServletContext();
			String webAppName = (String) servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
			Properties initParameters = null;
			try 
			{
				initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
			} 
			catch (FrameworkException fe) {
				log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file", fe);
				throw new RuntimeException("Failed to get initParameters from Tomcat JVM Instance specific Properties file", fe);
			}

			gwsServerName = initParameters.getProperty(GWS_SERVER_NAME);

			ServerManagerFactory factory = ServerManagerFactory.getInstance();
			factory.initialize(gwsServerName);

			log.debug("Completed startup of servicemgr services.");
		}
		catch(ResourceException re) {
			log.error("Could not initialise database connection pools " + re.getMessage());
			throw new RuntimeException(re);
		}
		catch (Exception exp) {
			log.error("Could not start Server Managers " + exp.getMessage());
			throw new RuntimeException(exp);
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		log.debug("Shutting down all servicemgr services.");
		ServerManagerFactory.getInstance().stopAll();
		log.debug("Completed shutdown of servicemgr services... ");
	}
}
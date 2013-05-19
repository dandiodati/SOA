package com.nightfire.comms.servicemgr;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;

/**
 * Singleton factory implementation of ServerManagerFactory, this manages
 * various kinds of configured server managers.
 */
public class ServerManagerFactory {
	/**
	 * singleton instance of ServerManagerFactory
	 */
	private static ServerManagerFactory singleFactory;


	/**
	 * private constructor for ServerManagerFactory to avoid outer class
	 * instantiation.
	 */
	private ServerManagerFactory() {
	}

	// constants representing xml element names
	public final static String MGR_CONFIG_CATEGORY = "servicemgr";
	private String gwsServerName = null; // consult on the access restrictions
											// on this field.

	/**
	 * Map binding various server mangers to its type
	 */
	private Map<String, ServerManagerBase> serverMap = new HashMap<String, ServerManagerBase>();

	/**
	 * Handler that sends notifications about error/exceptions
	 */
	private ServerManagerNotificationHandler notificationHandler = new ServerManagerNotificationHandler();

	/**
	 * returns an instance of ServerManagerFactory.
	 * 
	 * @return ServerManagerFactory
	 */
	public static ServerManagerFactory getInstance() {
		if (singleFactory == null)
			singleFactory = new ServerManagerFactory();

		return singleFactory;
	}

	/**
	 * a factory method which returns ServerManager depending on the type.
	 * 
	 * @param type
	 *            ServerManager type needed.
	 * @return ServerManager
	 * @throws ProcessingException
	 */
	public ServerManagerBase getServerManager(String type)
			throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "getServerManager[type:" + type
					+ "]");

		String className = ServerTypeMappingRegistry.getServerClassNm(type);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "[classname:" + className + "]");

		if (className == null || className.trim().length() == 0)
			throw new ProcessingException("No class defined for " + type + " "
					+ className);

		Class serverClass;
		Method getInstanceMethod;
		ServerManagerBase returnServer;

		try {
			serverClass = Class.forName(className.trim());
			getInstanceMethod = serverClass.getMethod("getInstance",
					new Class[] { String.class });
			returnServer = (ServerManagerBase) getInstanceMethod.invoke(null,
					new Object[] { type });
		} catch (ClassNotFoundException e) {
			Debug.error("No such class found " + className);
			throw new ProcessingException(e);
		} catch (Exception e) {
			Debug.error("No getInstance method in " + type + " " + className);
			throw new ProcessingException(e);
		}

		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "exiting getServerManager");

		return returnServer;
	}

	/**
	 * A factory method which returns ServerManager depending on the type.
	 * Parametrized form, takes GWS Server Name along with the ServerManager
	 * type Returns ServerManagerBase Object.
	 * 
	 * @param type
	 * @param gwsServerName
	 * @return ServerManagerBase
	 * @throws ProcessingException
	 */

	public ServerManagerBase getServerManager(String type, String gwsServerName)
			throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "getServerManager[type:" + type
					+ "]");

		// String className = (String)typeMap.get(type.toUpperCase());
		String className = ServerTypeMappingRegistry.getServerClassNm(type);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "[classname:" + className + "]");

		if (className == null || className.trim().length() == 0)
			throw new ProcessingException("No class defined for " + type + " "
					+ className + " and GWSServer Named: " + gwsServerName
					+ " ");

		Class serverClass;
		Method getInstanceMethod;
		ServerManagerBase returnServer;

		try {
			serverClass = Class.forName(className.trim());
			getInstanceMethod = serverClass.getMethod("getInstance",
					new Class[] { String.class });

			returnServer = (ServerManagerBase) getInstanceMethod.invoke(null,
					new Object[] { type }, new Object[] { gwsServerName });
		} catch (ClassNotFoundException e) {
			Debug.error("No such class found " + className);
			throw new ProcessingException(e);
		} catch (Exception e) {
			Debug.error("No getInstance method in " + type + " " + className);
			throw new ProcessingException(e);
		}

		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG, "exiting getServerManager");

		return returnServer;
	}


	/**
	 * Reads the configuration property and initializes the server properties.
	 * Takes GWS Server Name as the parameter.
	 * 
	 * @param gwsServerName
	 * @throws ProcessingException
	 */

	public void initialize(String gwsServerName) throws ProcessingException {
		this.gwsServerName = gwsServerName;
		Set<String> allServerTypes = ServerTypeMappingRegistry
				.getAllServerTypes();
		for (String serverType : allServerTypes) {

			List<String> configCategory = getConfigCategory(serverType);

			if (configCategory != null && configCategory.size() > 0) {

				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS, "Starting server :"
							+ serverType);

				start(serverType, gwsServerName);
			}
		}
	}

	/**
	 * This starts a particular server manager and starts all associated
	 * service. Allows for inclusion of the gwsServerName into the
	 * initialization code.
	 * 
	 * @param serverType
	 * @param gwsServerName
	 * @throws ProcessingException
	 */
	public void start(String serverType, String gwsServerName)
			throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
			Debug.log(Debug.OBJECT_LIFECYCLE, "inside start[type:" + serverType
					+ "]...");

		if (serverMap.get(serverType) != null) {
			if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
				Debug.log(Debug.OBJECT_LIFECYCLE, "server[" + serverType
						+ "] is already running...");
			return;
		}

		ServerManagerBase server = getServerManager(serverType);

		// start the server and update its property in the configuration file.
		server.initialize(gwsServerName);
		server.startAll();
		serverMap.put(serverType, server);
		// updateXML(serverType,"true");
		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
			Debug.log(Debug.OBJECT_LIFECYCLE, "exiting start [type:"
					+ serverType + "]...");
	}

	/**
	 * stops the server manager identified by its type.
	 * 
	 * @param serverType
	 * @throws ProcessingException
	 */
	public void stop(String serverType) throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
			Debug.log(Debug.OBJECT_LIFECYCLE, "stop[type:" + serverType
					+ "]...");

		ServerManagerBase server = (ServerManagerBase) serverMap
				.get(serverType);
		server.stopAll();
		serverMap.remove(serverType);
		// updateXML(serverType,"false");

		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
			Debug.log(Debug.OBJECT_LIFECYCLE, "exiting stop[type:" + serverType
					+ "]...");
	}

	/**
	 * stops all the configured server managers.
	 */
	public void stopAll() {
		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
			Debug.log(Debug.OBJECT_LIFECYCLE, "stopAll ...");

		ArrayList<String> keys = new ArrayList<String>(serverMap.keySet());

		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			String serverType = (String) iterator.next();
			ServerManagerBase server = (ServerManagerBase) serverMap
					.get(serverType);
			Notification notification = null;
			try {
				server.stopAll();
				serverMap.remove(serverType);
				notification = new Notification("Stopped server :" + serverType);
			} catch (Exception e) {
				Debug.log(Debug.ALL_WARNINGS, "ERROR: could not stop[type:"
						+ serverType + "]->" + e.getMessage());
				notification = new Notification(
						"Error occured while Stopping server :" + serverType, e);
			}
			notificationHandler.handle(notification);

		}

		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
			Debug.log(Debug.OBJECT_LIFECYCLE, "exiting stopAll...");
	}


	/**
	 * gets all configuration category for the given server manager type.
	 * 
	 * @param type
	 * @return List of file names
	 */
	public List<String> getConfigCategory(String type)
			throws ProcessingException {
		return ServerTypeMappingRegistry.getConfigCategory(type);
	}

}

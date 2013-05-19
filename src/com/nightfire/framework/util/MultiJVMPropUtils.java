package com.nightfire.framework.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Tomcat JVM Instance specific property-related utility class.
 */
public class MultiJVMPropUtils {

	/* Name for accessing instance properties file from CATALINA_OPTS */
	public static final String INSTANCE_PROPERTIES_FILE = "INSTANCE_PROPERTIES_FILE";

	/* The initialisation parameters */
	private static Properties initParameters = new Properties();

	/* List containing webAppName and its properties, the properties placed are a combination of properties 
	  defined in web.xml and instance specific properties file */
	private static Map<String, Properties> webAppNameInitParamsMap = new HashMap<String, Properties>();

	/**
	 * load initialisation param from Tomcat JVM Instance specific Properties file.
	 * 
	 * @param initProperties
	 *            Existing properties
	 * @param servletContext
	 *            Object of ServletContext, only use for logging.
	 * @param webAppName
	 *            name of web application
	 * @throws ServletException
	 *             the servlet exception
	 */

	public static void loadInitParamsFromPropFile(Properties initProperties, ServletContext servletContext, String webAppName) throws FrameworkException {
		
		/* Merge properties from instance specific properties file to contextProperties */
		servletContext.log("MultiJVMPropUtils.getInitParameters: Obtaining contextProperties from instance specific properties file...");
		
		String instancePropertiesFilePath = System.getProperty(INSTANCE_PROPERTIES_FILE);
		if (StringUtils.hasValue(instancePropertiesFilePath)) 
		{
			servletContext.log("Instance specific  properties file path [" + instancePropertiesFilePath + "]");
		
			if (FileUtils.isFileExists(instancePropertiesFilePath)) 
			{
				try 
				{
					FileUtils.loadProperties(initProperties, instancePropertiesFilePath);
				}
				catch (FrameworkException fe) 
				{
					String errMsg = "Failed to load file properties for [" + webAppName + "].";
					servletContext.log(errMsg, fe);
					throw new FrameworkException(fe);
				}
				servletContext.log("MultiJVMPropUtils.getInitParameters: Obtained contextProperties from instance specific  properties file...: " 
						+ PropUtils.suppressPasswords(initProperties.toString()));
			} 
			else {
				servletContext.log("WARN: Instance specific  properties file path [" + instancePropertiesFilePath + "] NOT exists");
			}
		} 
		else {
			servletContext.log("WARN: No instance specific  properties file found ");
		}

		initParameters.putAll(initProperties);
	}

	
	/**
	 * Load properties from instance specific property file
	 * @param initProperties
	 * @throws FrameworkException
	 */
	public static void loadInitParamsFromPropFile(Properties initProperties) throws FrameworkException {
		
		/* Merge properties from instance specific properties file to context properties */
		Debug.log(Debug.NORMAL_STATUS,"MultiJVMPropUtils.getInitParameters: Obtaining contextProperties from instance specific properties file...");
		String instancePropertiesFilePath = System.getProperty(INSTANCE_PROPERTIES_FILE);
		
		if (StringUtils.hasValue(instancePropertiesFilePath)) 
		{
			Debug.log(Debug.NORMAL_STATUS,"Instance specific  properties file path [" + instancePropertiesFilePath + "]");
			if (FileUtils.isFileExists(instancePropertiesFilePath)) 
			{
				try 
				{
					FileUtils.loadProperties(initProperties, instancePropertiesFilePath);
				} 
				catch (FrameworkException fe) 
				{
					String errMsg = "Failed to load properties file from [" + instancePropertiesFilePath + "].";
					Debug.error(errMsg);
					Debug.logStackTrace(fe);
					throw new FrameworkException(fe);
				}
				Debug.log(Debug.NORMAL_STATUS,"MultiJVMPropUtils.getInitParameters: Obtained contextProperties from instance specific  properties file...: " + PropUtils.suppressPasswords(initProperties.toString()));
			} 
			else {
				Debug.warning("Instance specific  properties file path [" + instancePropertiesFilePath + "] NOT exists");
			}
		} else {
			Debug.log(Debug.NORMAL_STATUS," No instance specific  properties file found ");
		}
	}

	/**
	 * Gets the the object of Properties which contain initParameters, initialised either from ServletConstextListener or
	 * from provided servletContext + Tomcat JVM Instance specific Properties file
	 * 
	 * @param servletContext  the servlet context
	 * @param webAppName the name of web application
	 * @return the object of Properties
	 * @throws FrameworkException
	 */
	@SuppressWarnings("unchecked")
	public static Properties getInitParameters(ServletContext servletContext, String webAppName) throws FrameworkException {

		if (StringUtils.hasValue(webAppName) && webAppNameInitParamsMap.containsKey(webAppName)) {
			return webAppNameInitParamsMap.get(webAppName);
		} else {
			
			Properties initProperties = null;
			if (!StringUtils.hasValue(webAppName)) {
				/* getting context(identified by its web application name) specific init-parameter 
				 If more then one webAppNames are 'null' then their properties will be merged with key 'null' */
				initProperties = webAppNameInitParamsMap.get(webAppName);
				if (initProperties == null)
					initProperties = new Properties();
			} else {
				// create blank properties
				initProperties = new Properties();
			}

			/* merge servlet context init parameters from web.xml */
			servletContext.log("MultiJVMPropUtils.getInitParameters: Obtaining contextProperties from web.xml for webAppName=[" + webAppName + "]");
			
			Enumeration initParamNames = servletContext.getInitParameterNames();

			while (initParamNames.hasMoreElements()) {
				String initParamName = (String) initParamNames.nextElement();
				String initParamValue = servletContext.getInitParameter(initParamName);
				if (initParamValue != null) {
					initProperties.put(initParamName, initParamValue);
				}
			}

			/* merge properties from Tomcat JVM Instance specific Properties file */
			loadInitParamsFromPropFile(initProperties, servletContext, webAppName);
			
			/* adding webAppName to list so next time it can be checked, whether web.xml of this webAppName loaded in initParameter properties or not */
			webAppNameInitParamsMap.put(webAppName, initProperties);
			servletContext.log("MultiJVMPropUtils.getInitParameters: Obtaining contextProperties from web.xml for webAppName=[" 
					+ webAppName + "] initParamters=[" + initProperties + "]");
		}
		
		Properties prop = new Properties();
		if(webAppNameInitParamsMap.get(webAppName)!=null)
			prop.putAll(webAppNameInitParamsMap.get(webAppName));
		return prop;
	}

	
	/**
	 * use method #getInitParameters(servletContext, wepAppName)
	 */
	public static Properties getInitParameters() {
		
		Properties prop = new Properties();
		prop.putAll(initParameters);
		return prop;
	}

	public static String getParameter(String name) throws FrameworkException {
		return getParameter(name, false);
	}

	/**
	 * 
	 * @param name
	 * @param required
	 * @return
	 * @throws FrameworkException
	 */
	public static String getParameter(String name, boolean required) throws FrameworkException {
		String value = null;
		if (! initParameters.isEmpty()) {
			value = initParameters.getProperty(name);
		}
		else{
			// if initParameters is empty at this stage then it only initialize with only instance specific properties file.
			loadInitParamsFromPropFile(initParameters);			
		}
		if (required && (value == null)) {
			throw new FrameworkException("ERROR: Missing required item named [" + name + "] in instance specific propeties file.");
		}
		return value;
	}
}

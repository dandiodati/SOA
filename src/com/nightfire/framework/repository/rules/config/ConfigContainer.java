package com.nightfire.framework.repository.rules.config;

import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

import java.util.Map;

/**
 * Class containing configuration objects (of FlowIdentifierConfig class) for
 * multiple flow identifier configuration files.
 * <install-root>/repository/DEFAULT/flowIdentifier/<request>.xml <p/>
 * Container: key = Attribute's value of 'Request' node of Header xml value =
 * object of FlowIdentifierConfig
 */
public class ConfigContainer implements CachingObject {

	public ConfigContainer() {

		CacheManager.getRegistrar().register(this);
	}

	/**
	 * Method to get FlowIdentifierConfig from container based on request.
	 * 
	 * @param key
	 * @return
	 */
	public RequestConfig getRequestCfg(String category, String key)
			throws FrameworkException {

		if (configContainerMap.isEmpty()) {
			
			if (Debug.isLevelEnabled(Debug.XML_BASE)) {
				Debug.log(Debug.XML_BASE, "Configuration container is found empty. Container needs to be reloaded." );
			}
			
			// If containerObj is empty, load flow identifier config files and
			// populate containerObj

			ConfigFilesLoader loader = new ConfigFilesLoader();

			loader.load(category);

			configContainerMap = loader.getConfigContainer();

		}
		
		RequestConfig reqConfig = configContainerMap.get(key);
		
		if (Debug.isLevelEnabled(Debug.XML_BASE)) {
				Debug.log(Debug.XML_BASE, "Found request specific configuration. Request ["+key+"]," +
						" Configuration [ "+ reqConfig +"]" );
		}
		
		return reqConfig;

	}

	/**
	 * Method used to get container map.
	 */
	public Map<String, RequestConfig> getConfigContainerMap() {
		return configContainerMap;
	}

	/**
	 * Method used to set container.
	 * 
	 * @param configContainer
	 */
	public void setContainer(Map<String, RequestConfig> configContainer) {

		this.configContainerMap = configContainer;
	}

	/**
	 * Flush config container
	 * 
	 * @throws FrameworkException
	 */
	public void flushCache() throws FrameworkException {
		
		
		configContainerMap.clear();
	}

	private Map<String, RequestConfig> configContainerMap;
}

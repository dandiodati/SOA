package com.nightfire.framework.repository.rules.config;

import org.w3c.dom.Document;

import java.util.Map;
import java.util.HashMap;

import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.repository.RepositoryException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.MessageException;

/**
 * Class loads flow identifier configuration files (as DOM) placed under
 * repository. Location: <install-root>/repository/DEFAULT/<category>
 */
public class ConfigFilesLoader {

	/**
	 * Method loads all configuration file placed under given category
	 */

	public void load(String category) throws FrameworkException {

		cfgContainer = new HashMap<String, RequestConfig>();

		cfgList = null;
		
		if (Debug.isLevelEnabled(Debug.XML_BASE)) {
			Debug.log(Debug.XML_BASE, "Loading all configuration files from category : ["+ category+ "]...");
		}

		try {

			cfgList = RepositoryManager.getInstance().getAllMetaData(category,
					false);

		} catch (RepositoryException e) {
			
			Debug.log(Debug.ALL_ERRORS, "Failed repository manager to load meta data of given catagory ["
					+ category + "]" + e.getStackTrace());
			
			new FrameworkException(
					"Failed repository manager to load meta data of given catagory ["
							+ category + "]" + e.getStackTrace());
		}

		
		if (cfgList == null || cfgList.length == 0) {
			
			Debug.log(Debug.ALL_WARNINGS, "Category ["+ category + "] or no configuration file exist under this category" );
			
			throw new FrameworkException(
					"No configuration file found under given catagory ["
							+ category + "]");
		}
			
		// Get instance of parser
		ConfigParser cfgParser = ConfigParser.getInstance();

		// Parse configuration files and populate containerObj

		for (NVPair config : cfgList) {

			Document document = null;
			try {

				document = XMLLibraryPortabilityLayer
						.convertStringToDom((String) config.getValue());

			} catch (MessageException e) {
				
				Debug.log(Debug.ALL_ERRORS, "While loading configuration files, failed to convert String to DOM :"
						+ Debug.getStackTrace(e));
				
				new FrameworkException("Failed to convert String to DOM :"
						+ Debug.getStackTrace(e));
			}
			String fileName = config.getName().substring(
					config.getName().indexOf(".") + 1);

			if (Debug.isLevelEnabled(Debug.XML_BASE)) {
				Debug.log(Debug.XML_BASE, "Parsing configuration file ["+fileName+"].xml... ");
			}

			RequestConfig requestCfg = cfgParser.parse(document);
			
			if (Debug.isLevelEnabled(Debug.XML_BASE)) {
				Debug.log(Debug.XML_BASE, "File has been parsed successfully");
			}
			
			cfgContainer.put(fileName, requestCfg);
		}

		if (Debug.isLevelEnabled(Debug.XML_BASE)) {
			Debug.log(Debug.XML_BASE, "Populated config container ["+ cfgContainer + "]");
		}

		emptyFileList = false;

	}

	public Map<String, RequestConfig> getConfigContainer() {

		return this.cfgContainer;
	}

	/**
	 * Method to check whether configuration files are available
	 * 
	 * @return
	 */
	public boolean checkEmptyFileList() {

		return emptyFileList;
	}

	private Map<String, RequestConfig> cfgContainer;

	private NVPair[] cfgList;

	private static boolean emptyFileList = true;

	public static void main(String a[]) {

		/*
		 * try {
		 * DBInterface.initialize("jdbc:oracle:thin:@192.168.64.125:1521:ORCL132",
		 * "suninstall", "suninstall"); } catch (DatabaseException e) {
		 * e.printStackTrace(); }
		 * 
		 * ConfigFilesLoader loader = new ConfigFilesLoader(); try {
		 * loader.load(); } catch (FrameworkException e) { e.printStackTrace(); }
		 * System.out.println(loader.getConfigContainer());
		 */
	}
}

package com.nightfire.spi.neustar_soa.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class NancValueMapping implements CachingObject {

	/*
	 * Declaring private variables
	 */
	private static HashMap<String, String> map = new HashMap<String, String>();
	private static String className = "NancValueMapping";

	public void flushCache() throws FrameworkException {

		if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE)) {

			Debug.log(Debug.OBJECT_LIFECYCLE, className + " : Flushing disabled-rules cache ...");
		}

		synchronized (map) {
			map.clear();
		}

	}

	/*
	 * This boolen method is used to check whether Hash map is cached or not.
	 */
	public static boolean isNancMapCached() {
		if (map.size() > 0) {

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {

				Debug.log(Debug.MSG_STATUS, className + " : Hash map for NANC mapping is in cache");
			}

			return true;

		} else {

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {

				Debug.log(Debug.MSG_STATUS, className + " : Hash map for NANC mapping is NOT in cache");
			}

			return false;
		}
	}

	public static HashMap<String, String> getNancValueMapping() {
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, className + " : Returning Hash map...");
		}

		return map;
	}

	/*
	 * This method will load the Hash map by reading the 
	 * contents from the configuration property file
	 */
	public static HashMap<String, String> loadAndGetMappingToHashMap(File path) {

		synchronized (map) {
			String line = null;
			BufferedReader input = null;

			try {
				input = new BufferedReader(new FileReader(path));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, className + " : Starts loading Hash map from configuration property " +
							"file - [" + path.getAbsolutePath()+ "]");
				}
					

				while ((line = input.readLine()) != null) {

					if (line.startsWith("#") || line.equals("")) {

						continue;

					} else {
						/*
						 * Check for '=' operand is present in mapping string.
						 * If '=' is present then, 
						 * Check for mapping value, and it must be present.
						 * 
						 * Throws exception if mapping file is wrong.
						 */
						if (line.contains("=")) {

							if (line.charAt(line.length() - 1) != '=') {
								String[] lineArray = line.split("=");

								if (!((lineArray[0].equals("") || lineArray[0] == null) && (lineArray[1].equals("") || lineArray[1] == null))) {
									map.put(lineArray[0].trim(), lineArray[1].trim());
								}
							} else {
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
									
									Debug.log(Debug.MSG_STATUS, className + " : Mapping string " + line.toString()
											+ " does not contain any mapping value after '=' operand");
								}
									
							}
						} else {
							if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
								
								Debug.log(Debug.MSG_STATUS, className + " : Mapping string " + line.toString()
										+ " does not contain '=' operand, Invalid Mapping line in mapping configuration file.");
							}
							
						}
					}
				}
				Debug.log(Debug.MSG_STATUS, className + " : Hash map loading is done...");

			} catch (FileNotFoundException fnf) {

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, className + " : Could not find the file on given path [" + path.getAbsolutePath() + "]");
					Debug.log(Debug.MSG_STATUS, className + " : Hash map is NOT loaded...");
					Debug.log(Debug.MSG_STATUS, className + " : " + fnf.getMessage());
				}
					

			} catch (IOException ioe) {
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, className + " : Could not read the property file contents to load the Hash map");
					Debug.log(Debug.MSG_STATUS, className + " : Hash map is NOT loaded...");
					Debug.log(Debug.MSG_STATUS, className + " : " + ioe.getMessage());
				}
				

			}finally{
				if(input != null){
					try {
						input.close();
					} catch (IOException e) {
						
						if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
							Debug.log(Debug.ALL_ERRORS, className + " : Exception occures at the time to close the " +
									"stream..." +e.getMessage());
						}
					}
				}
			}
		}

		return map;

	}

	/*
	 * A single private instance of this class will be created to assist in cache flushing.
	 */
	private NancValueMapping() {

		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )            
			Debug.log(Debug.SYSTEM_CONFIG, className + " : Initializing...");
		
		try {
			CacheManager.getRegistrar().register(this);

		} catch (Exception e) {
			Debug.warning(e.toString());
		}
	}

	//	 Used exclusively for cache flushing.
	private static NancValueMapping flusher = new NancValueMapping();

}

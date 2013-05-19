package com.nightfire.comms.servicemgr;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.MultiJVMPropUtils;

/**
 * Class contains the methods to filter out SUPPORTED or UNSUPPORTED id's from XMLMessageParser.If property file
 * (<NFI root>/tomcat/instances/<instance>/<Property File Name>)
 * contains supported list of id's then these id's shall be contained into the resulting parser and remaining ids 
 * shall be removed.Similarly if property file contains list of unsupported id's then these id's shall be removed
 * from resulting parser.
 * 
 * Supported id list shall have the preference over the unsupported id list.
 *  
 * @author vishalb.gupta
 */
public class ServiceIdFilter {
	
	/**
	 * Constant to separate id's.
	 */
	private static final String SEP = ",";
	
	/**
	 * Map that holds root element to service element mapping.
	 */

	private static Map<String, String> rootElement2serviceElement = new HashMap<String, String>();

	static {

		rootElement2serviceElement.put(JMSQueueConsumerManager.JMS_CONSUMERS,
				ServiceMgrConsts.JMS_CONSUMER);

		rootElement2serviceElement.put(
				AsyncEmailServerManager.ASYNC_EMAIL_SERVERS,
				ServiceMgrConsts.ASYNC_EMAIL_SERVER);

		rootElement2serviceElement.put(
				ServiceMgrConsts.POLL_COMM_SERVER_CONFIG,
				ServiceMgrConsts.POLL_COMM_SERVER);
	}

	public ServiceIdFilter() {
	}

	/**
	 * Returns the list of supported service id's. It will return null if
	 * Properties are not initialized.
	 * 
	 * @return List
	 * @throws Exception
	 *             When unable to process the list
	 */
	public static List<String> getSupportedIdList() throws ProcessingException {

		return getList(ServiceMgrConsts.SUPPORTED_SERVICES);
	}

	/**
	 * Returns the list of unsupported service Id's. It will return null if
	 * Properties are not initialized.
	 * 
	 * @return List
	 * @throws Exception
	 *             When unable to process the list
	 */

	public static List<String> getUnSupportedIdList() throws ProcessingException {

		return getList(ServiceMgrConsts.UNSUPPORTED_SERVICES);
	}

	/**
	 * 
	 * Returns the list of service id's based upon service type passed. It will
	 * return null if Properties are not initialized.
	 * 
	 * @param type
	 *            String
	 * @return List
	 * @throws ProcessingException
	 *             When unable to process the list
	 */

	private static List<String> getList(String serviceType) throws ProcessingException {
		List<String> listOfIds = new ArrayList<String>();
		try {
			Properties prop = new Properties(); 
			MultiJVMPropUtils.loadInitParamsFromPropFile(prop);
			
			if (prop.isEmpty()) {
				
			  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS,
						"Could not get Initial properties");

				return null;
			}

			Object services = prop.get(serviceType);
			if (services != null && services instanceof String) {
				String servicesLst = (String) services;
				StringTokenizer st = new StringTokenizer(servicesLst, SEP);
				while (st.hasMoreTokens()) {
					listOfIds.add(st.nextToken().trim());

				}
			}
			
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Going to return the list of Ids ["
					+ listOfIds + "] for service type [" + serviceType + "]");
			
			return listOfIds;
		} catch (Exception exp) {
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.error("An error is occured while getting the list of Ids for ["+ serviceType + "]");
			
			Debug.logStackTrace(exp);
			throw  new ProcessingException(exp);
		}

	}

	/**
	 * Returns the filtered fileParser.If supported id's are configured then this
	 * method shall filter all the id's except the supported id's from the
	 * fileParse.If unsupported id's are configured then this method shall filter
	 * all the unsupported id's from fileParser.Otherwise returns unmodified
	 * fileParser.
	 * 
	 * @param fileParser
	 * @param serviceType
	 * @return type XMLMessageParser
	 * @throws ProcessingException
	 *             When error occurred during dom filtering.
	 */

	public XMLMessageParser getFilteredDOM(XMLMessageParser fileParser,
			String serviceType) throws ProcessingException {

		try {
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Going to fetch SUPPORTED SERVICES");
			
			List<String> supportedIdList = getList(ServiceMgrConsts.SUPPORTED_SERVICES);
			
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS,
					"SUPPORTED SERVICES are fetched with values ["
							+ supportedIdList + "]");
			List<String> unSupportedIdList = null;
			if (!(supportedIdList != null && supportedIdList.size() > 0)) {
				
				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS,
								"Since SUPPORTED SERVICES are not configured so going to fetch UNSUPPORTED SERVICES");
				unSupportedIdList = getList(ServiceMgrConsts.UNSUPPORTED_SERVICES);
				
				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS,
						"UNSUPPORTED SERVICES are fetched with values ["
								+ unSupportedIdList + "]");
				if ((unSupportedIdList != null && unSupportedIdList.size() > 0)) {
					return getFilteredDOM(fileParser, unSupportedIdList,
							serviceType, false);
				}
			} else {
				return getFilteredDOM(fileParser, supportedIdList, serviceType,
						true);
			}
		} catch (Exception exp) {
			Debug.error("An exception occurred while filtering the Dom");
			Debug.logStackTrace(exp);
			throw   new ProcessingException(exp);
		}
		return fileParser;

	}

	/**
	 * Returns The list of included id's.
	 * 
	 * @param fileParser
	 * @param listOfIds
	 * @param consumerType
	 * @param include
	 * @return List
	 * @throws Exception
	 *             When error occurred during the processing.
	 */

	private List<String> getIncludedIDsList(XMLMessageParser fileParser,
			List<String> listOfIds, String consumerType, boolean include)
			throws ProcessingException {
		List<String> nodeList = new ArrayList<String>();
		List<String> listOfIncludedIds = new ArrayList<String>();
		List<String> listOfUnIncludedIds = new ArrayList<String>();
		try {
			Document tempDoc = fileParser.getDocument();
			NodeList consumersList = tempDoc.getElementsByTagName(consumerType);
			int outerCount = consumersList.getLength();
			for (int i = 0; i < outerCount; i++) {
				Node child = consumersList.item(i);
				if (!(child instanceof Element))
					continue;
				NodeList nl = ((Element) child).getChildNodes();
				int size = nl.getLength();
				for (int j = 0; j < size; j++) {
					Node childInner = nl.item(j);
					if (!(childInner instanceof Element))
						continue;
					Element element = (Element) childInner;

					if (element == null || element.getAttributes() == null)
						continue;
					String childType = rootElement2serviceElement
							.get(consumerType);

					if (childType.equals(element.getNodeName())) {
						Node namedItem = element.getAttributes().getNamedItem(
								ServiceMgrConsts.ID_CONSTANT);
						String nodeValue = namedItem.getNodeValue();
						nodeList.add(nodeValue);
					}
				}

			}
			listOfUnIncludedIds.addAll(nodeList);
			for (String nodeId : nodeList) {
				for (String id : listOfIds) {
					if (id.equals(nodeId)) {
						if (include) {
							listOfIncludedIds.add(nodeId);

						} else {
							listOfUnIncludedIds.remove(nodeId);
							}
							break;
						
					}

				}
			}
		} catch (Exception exp) {			
			Debug.error("An exception occured while getting the list of included Ids");
			Debug.logStackTrace(exp);
			throw new ProcessingException(exp);

		}

		if (include)
		{
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS,
						"Going to return list of included id's "
								+ listOfIncludedIds + "]");
				
			return listOfIncludedIds;
		}
		if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS,
					"Going to return list of included id's "
							+ listOfUnIncludedIds + "]");
		return listOfUnIncludedIds;
	}

	/**
	 * Returns the filtered Dom.If include is true then supplied id list of
	 * supplied consumerType shall be included into filtered dom.But if include
	 * is false then supplied id's of supplied consumerType shall not be included
	 * into filtered dom.
	 * 
	 * @param fileParser
	 * @param listOfIds
	 * @param consumerType
	 * @param include
	 * @return type XMLMessageParser
	 * @throws ProcessingException;
	 *             When error occurred during the processing
	 */

	private XMLMessageParser getFilteredDOM(XMLMessageParser fileParser,
			List<String> listOfIds, String consumerType, boolean include)
			throws  ProcessingException {
		try {
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Fetching the list of included Ids");

			List<String> listOfIncludedIds = getIncludedIDsList(fileParser,
					listOfIds, consumerType, include);

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Got the list of included Ids[ "
					+ listOfIncludedIds + "]");

			Document tempDoc = fileParser.getDocument();
			
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Document that will be filtered  "
					+ XMLLibraryPortabilityLayer.convertDomToString(tempDoc));

			NodeList consumersList = tempDoc.getElementsByTagName(consumerType);
			int outerCount = consumersList.getLength();
			for (int i = 0; i < outerCount; i++) {
				Node child = consumersList.item(i);
				if (!(child instanceof Element))
					continue;
				NodeList nl = ((Element) child).getChildNodes();
				int size = nl.getLength();
				for (int j = 0; j < size; j++) {
					Node childInner = nl.item(j);
					if (!(childInner instanceof Element))
						continue;
					Element element = (Element) childInner;

					if (element == null || element.getAttributes() == null)
						continue;
					String childType = rootElement2serviceElement
							.get(consumerType);

					if (childType.equals(element.getNodeName())) {
						Node namedItem = element.getAttributes().getNamedItem(
								ServiceMgrConsts.ID_CONSTANT);
						String val = namedItem.getNodeValue();
						if (!listOfIncludedIds.contains(val)) {
							Node parentNode = childInner.getParentNode();
							parentNode.removeChild(childInner);

						}

					}

				}
			}
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Document after filtered Ids  "
					+ XMLLibraryPortabilityLayer.convertDomToString(tempDoc));
		} catch (Exception exp) {
			Debug.error("An exception occured while modifying the Dom");
			Debug.logStackTrace(exp);
			throw new ProcessingException(exp);
		}
		return fileParser;

	}

}

package com.nightfire.webgui.gateway.servicemgr;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.comms.servicemgr.ServerManagerBase;
import com.nightfire.comms.servicemgr.ServerManagerFactory;
import com.nightfire.comms.servicemgr.ServerTypeMappingRegistry;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.repository.NotFoundException;
import com.nightfire.framework.repository.RepositoryException;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.comms.servicemgr.ServiceMgrConsts;
import com.nightfire.comms.servicemgr.ServiceIdFilter;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.MultiJVMPropUtils;
import com.nightfire.framework.util.FrameworkException;

/**
 * Class to generate view for service management UI.It checks the
 * servicemgr/displayConfig category in repository for files containing display
 * configuration.
 */
public class GatewayViewHelper {

	public GatewayViewHelper() {
	}

	private static final String CFG_DIR_NM = "servicemgr" + File.separator
			+ "displayConfig";
	private static final String XML_FILE_SUFFIX = ".xml";

	/**
	 * Method to check whether display configurations are present in repository
	 * or not
	 * 
	 * @return <code>true</code> if display configuration files are there in
	 *         repository
	 */
	public boolean exists() {

		NVPair[] pairs = null;
		try {
			pairs = RepositoryManager.getInstance().listMetaData(CFG_DIR_NM,
					false, XML_FILE_SUFFIX);

			if (pairs != null && pairs.length > 0) {
				Debug.log(Debug.NORMAL_STATUS,
						"No xml file found in repository folder [" + CFG_DIR_NM
								+ "]");
				return true;
			}
			return false;
		} catch (Exception e) {
			Debug
					.error("An exception occured while listing xml files from repository folder ["
							+ CFG_DIR_NM + "]");
			Debug.logStackTrace(e);
			return false;
		}

	}

	List<Element> grpElemLst = new ArrayList<Element>();

	/**
	 * Generate gateway specific view on service management UI.
	 * 
	 * @param out
	 *            PrintWriter
	 * @throws ProcessingException
	 */
	public void generateView(PrintWriter out) throws ProcessingException {
		// parse all displayConfiguration
		NVPair[] pairs = null;
		try {
			pairs = RepositoryManager.getInstance().listMetaData(CFG_DIR_NM,
					false, XML_FILE_SUFFIX);

			if (pairs == null || pairs.length == 0) {
				Debug.log(Debug.NORMAL_STATUS,
						"No xml file found in repository folder [" + CFG_DIR_NM
								+ "]");
				return;
			}
		} catch (Exception e) {
			Debug
					.error("An exception occured while listing xml files from repository folder ["
							+ CFG_DIR_NM + "]");
			Debug.logStackTrace(e);
			throw new ProcessingException(e);
		}

		List<Group> grpLst = new ArrayList<Group>();
		try {
			for (int j = 0; j < pairs.length; j++) {
				Document document = getDocument(pairs[j].name);
				Element svcGroupElem = document.getDocumentElement();

				NodeList grpNodeLst = svcGroupElem
						.getElementsByTagName(ServiceMgrConsts.GROUP_TAG_CONSTANT);

				for (int i = 0; i < grpNodeLst.getLength(); i++) {
					Node node = grpNodeLst.item(i);

					if (node instanceof Element) {
						Element grpElem = (Element) node;
						grpElemLst.add(grpElem);

						String name = grpElem.getAttribute(NAME_ATTR);
						String displayNm = grpElem
								.getAttribute(DISPLAY_NAME_ATTR);
						Group grp = new Group(name, displayNm);

						parseService(grpElem, grp);
						grpLst.add(grp);
					}
				}
			}
		} catch (Exception exp) {
			Debug
					.error("An exception occured while listing xml files from repository folder ["
							+ CFG_DIR_NM + "]");
			Debug.logStackTrace(exp);
			throw new ProcessingException(exp);
		}

		display(out, grpLst);
	}

	/**
	 * 
	 * @return
	 * @throws ProcessingException
	 */
	private Element getAllServiceElem(List<Group> grpLst)
			throws ProcessingException {
		Document aggregatedDoc;
		List<String> listIds = new ArrayList<String>();
		for (Group grp : grpLst) {
			for (Service svc : grp.svcLst) {
				listIds.add(svc.getId());
			}
		}

		List<String> supportedIdList = ServiceIdFilter.getSupportedIdList();
		List<String> unSupportedIdList = null;
		List<String> finalList = new ArrayList<String>();
		if ((supportedIdList != null && supportedIdList.size() > 0)) {
			for (String temp : listIds) {
				if ((supportedIdList.contains(temp))) {
					finalList.add(temp);
				}
			}
		} else {
			unSupportedIdList = ServiceIdFilter.getUnSupportedIdList();
			if ((unSupportedIdList != null && unSupportedIdList.size() > 0)) {
				for (String id : listIds) {
					if (!(unSupportedIdList.contains(id))) {
						finalList.add(id);
					}
				}

			} else
				finalList.addAll(listIds);
		}

		try {
			aggregatedDoc = XMLLibraryPortabilityLayer.getNewDocument(
					SERVICE_GRP_ELEM, null);

			for (Element elem : grpElemLst) {
				Node node = aggregatedDoc.importNode(elem, true);
				aggregatedDoc.getDocumentElement().appendChild(node);
			}

			NodeList nodeList = aggregatedDoc
					.getElementsByTagName(ServiceMgrConsts.GROUP_TAG_CONSTANT);
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node temp = nodeList.item(i);
				NodeList tempInner = temp.getChildNodes();
				for (int j = 0; j < tempInner.getLength(); j++) {
					Node node = tempInner.item(j);
					if (!(node instanceof Element))
						continue;
					Element element = (Element) node;
					if (element == null || element.getAttributes() == null)
						continue;
					Node namedItem = element.getAttributes().getNamedItem(
							ServiceMgrConsts.ID_CONSTANT);
					String val = namedItem.getNodeValue();
					boolean check = finalList.contains(val);
					if (!check) {
						temp.removeChild(node);

					}

				}
			}
		} catch (MessageException e) {
			throw new ProcessingException(e);
		}

		return aggregatedDoc.getDocumentElement();
	}

	/**
	 * 
	 * @param grpElem
	 * @param grp
	 */
	private void parseService(Element grpElem, Group grp) {

		NodeList svcNodeLst = grpElem.getElementsByTagName(SERVICE_ELEM);

		for (int i = 0; i < svcNodeLst.getLength(); i++) {
			Node node = svcNodeLst.item(i);
			if (node instanceof Element) {
				Element svcElem = (Element) node;
				String id = svcElem.getAttribute(ServiceMgrConsts.ID_CONSTANT);
				String type = svcElem.getAttribute(TYPE_ATTR);
				type = ServerTypeMappingRegistry.mapPseudo2ActualType(type);

				String displayNm = svcElem.getAttribute(DISPLAY_NAME_ATTR);
				Service svc = new Service(id, type, displayNm);
				grp.add(svc);
			}
		}
	}

	/**
	 * Get the document for the passed in file name from repository.
	 * 
	 * @param fileNm
	 * @return
	 * @throws ProcessingException
	 */
	private Document getDocument(String fileNm) throws ProcessingException {
		try {
			Document dom = RepositoryManager.getInstance().getMetaDataAsDOM(
					CFG_DIR_NM, fileNm + ".xml");
			return dom;
		} catch (Exception e) {
			throw new ProcessingException(
					"Exception occured while reading configuration file "
							+ fileNm + ".xml from repository/DEFAULT/"
							+ CFG_DIR_NM + "\n" + e.getStackTrace());
		}
	}

	/**
	 * 
	 * @param out
	 * @param grpLst
	 * @throws ProcessingException
	 */
	private void display(PrintWriter out, List<Group> grpLst)
			throws ProcessingException {
		Document aggregatedDoc;
		try {
			aggregatedDoc = XMLLibraryPortabilityLayer.getNewDocument(
					"aggregatedDoc", null);
		} catch (MessageException e1) {
			throw new ProcessingException(e1);
		}

		try {
			String serverNm = MultiJVMPropUtils
					.getParameter(JMSQueueContextListener.GWS_SERVER_NAME);
			if (StringUtils.hasValue(serverNm)) {
				aggregatedDoc.getDocumentElement().setAttribute(JMSQueueContextListener.GWS_SERVER_NAME, serverNm);
			}
		} catch (FrameworkException ignore) {
		}

		Set<String> distinctSvcType = new HashSet<String>();
		for (Group grp : grpLst)
			for (Service svc : grp.svcLst)
				distinctSvcType.add(svc.getType());

		for (String type : distinctSvcType) {

			ServerManagerBase serverManager = ServerManagerFactory
					.getInstance().getServerManager(type);

			XMLMessageParser parser = serverManager.getParser();

			if (parser != null) {
				Element document = parser.getDocument().getDocumentElement();
				Node node = aggregatedDoc.importNode(document, true);
				aggregatedDoc.getDocumentElement().appendChild(node);

			} else {
				String xmlDescription, configMeta = null;
				List<String> configCategory = null;
				try {
					configCategory = ServerManagerFactory.getInstance()
							.getConfigCategory(type);
					for (String queryCriteria : configCategory) {
						xmlDescription = RepositoryManager
								.getInstance()
								.getMetaData(
										ServerManagerFactory.MGR_CONFIG_CATEGORY,
										queryCriteria);

						parser = new XMLMessageParser(xmlDescription);
						Element document = parser.getDocument()
								.getDocumentElement();
						Node node = aggregatedDoc.importNode(document, true);
						aggregatedDoc.getDocumentElement().appendChild(node);
					}
				} catch (NotFoundException e) {
					Debug.error("Cannot find in repository ->" + configCategory
							+ ":" + configMeta);
					throw new ProcessingException(e);
				} catch (RepositoryException e) {
					throw new ProcessingException(e);
				} catch (MessageParserException e) {
					Debug.error("Could not parse configuration "
							+ configCategory + " : " + configMeta);
					throw new ProcessingException(e);
				}
			}
		}

		Node svcGrpElem = aggregatedDoc.importNode(getAllServiceElem(grpLst),
				true);
		aggregatedDoc.getDocumentElement().appendChild(svcGrpElem);

		try {
			if (Debug.isLevelEnabled(Debug.XML_DATA))
				Debug.log(Debug.XML_DATA,
						" Aggregated Document for GatewayViewDisplay \n"
								+ XMLLibraryPortabilityLayer
										.convertDomToString(aggregatedDoc));
		} catch (Exception ignore) {
		}

		Source source = new DOMSource(aggregatedDoc);
		StreamResult result = new StreamResult(out);

		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tFactory
					.newTransformer(new StreamSource(
							ViewDispatcher.getRealPath()
									+ "//WEB-INF//DEFAULT//resources//xsl//gateway-services.xsl"));

			transformer.transform(source, result);
		} catch (Exception e) {
			Debug.error("Exception while transforming XML document : "
					+ e.getMessage());
			throw new ProcessingException(e);
		}

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Exiting GatewayViewHelper ...");
	}

	static class Group {
		String name, displayNm;
		List<Service> svcLst;

		Group(String name, String displayNm) {
			this.name = name;
			this.displayNm = displayNm;
			svcLst = new ArrayList<Service>();
		}

		String getName() {
			return name;
		}

		String getDisplayNm() {
			return displayNm;
		}

		void add(Service svc) {
			svcLst.add(svc);
		}

		public String toString() {
			return name + displayNm + svcLst.toString();
		}
	}

	static class Service {
		String id, type, displayNm;

		Service(String id, String type, String displayNm) {
			this.id = id;
			this.type = type;
			this.displayNm = displayNm;
		}

		String getDisplayNm() {
			return displayNm;
		}

		String getId() {
			return id;
		}

		String getType() {
			return type;
		}

		public String toString() {
			return id + type + displayNm;
		}

	}

	// private static final String GROUP_ELEM = "group";
	private static final String SERVICE_ELEM = "service";
	private static final String SERVICE_GRP_ELEM = "service_group";

	// private static final String ID_ATTR = "id";
	private static final String NAME_ATTR = "name";
	private static final String TYPE_ATTR = "type";
	private static final String DISPLAY_NAME_ATTR = "displayname";

}

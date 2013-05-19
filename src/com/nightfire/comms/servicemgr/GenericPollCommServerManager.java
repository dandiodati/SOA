package com.nightfire.comms.servicemgr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.comms.eventchannel.MultiCustomerEventConsumerServer;
import com.nightfire.comms.file.AsyncFileServer;
import com.nightfire.comms.ia.IAServer;
import com.nightfire.comms.jms.MultiCustomerJMSConsumerServer;
import com.nightfire.comms.jms.JMSQueueEventConsumerServer;
import com.nightfire.comms.timer.AsyncTimerServer;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.communications.ComServerBase;

/**
 * ServerManager class for managing poll communication servers from service
 * administration gui.It supports following communication servers : <li>
 * AsyncFileServer</li> <li>AsyncTimerServer</li> <li>FTPPoller</li> <li>
 * MultiCustomerEventConsumerServer</li> <li>MultiCustomerJMSConsumerServer</li>
 * <li>IAServer</li>
 * 
 * @author hpirosha
 */
public class GenericPollCommServerManager extends ServerManagerBase {

	private static GenericPollCommServerManager instance = null;

	private GenericPollCommServerManager(String type) {
		super(type);
	}

	/**
	 * Returns a single instance of GenericPollCommServerManager based upon the
	 * type passed. Valid types are file-server,timer-server and ftp-poller.
	 * 
	 * @param type
	 *            String {file-server,timer-server,ftp-poller}
	 * @return GenericPollCommServerManager
	 */
	public static synchronized GenericPollCommServerManager getInstance(
			String type) {
		if (instance == null)
			instance = new GenericPollCommServerManager(type);

		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.nightfire.comms.servicemgr.ServerManagerBase#add(java.util.Map)
	 */
	@Override
	public void add(Map parameters) throws ProcessingException {
		// TODO Auto-generated method stub

	}

	private static final String ELEM_POLL_COMM_SERVER = "poll-comm-server";

	/**
	 * Configuration constant to identify a id of PollCommServer.
	 */
	private static final String ATTR_COMM_SERVER_ID = "id";

	/**
	 * Configuration constant to identify the key of PollCommServer.
	 */
	private static final String ELEM_COMM_SERVER_KEY = "key";

	/**
	 * Configuration constant to identify the driver type of PollCommServer.
	 */
	private static final String ELEM_COMM_SERVER_TYPE = "type";

	/**
	 * Configuration Constant to identify status of PollCommServer.
	 */
	private static final String ATTR_COMM_SERVER_START = "start";

	/**
	 * Confguration constant for params defined for a PollCommServer
	 */
	private static final String ELEM_PARAM = "param";

	/**
	 * SSL Configuration constant for FTP Poller
	 */
	private static final String SSL_PARAM_NM = "use-ssl";

	/**
	 * Map for storing poll server configs.
	 */
	private Map<String, CommServerConf> commServerConfMap = new HashMap<String, CommServerConf>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.nightfire.comms.servicemgr.ServerManagerBase#initialize()
	 */
	@Override
	public void initialize() throws ProcessingException {

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "initializing poll comm server : "
					+ getType());

		// read configuration from repository
		// String categoryConfig =
		// ServerManagerFactory.getInstance().getConfigCategory(getType());
		String categoryConfig = ServerManagerFactory.MGR_CONFIG_CATEGORY;
		// String metaConfig =
		// ServerManagerFactory.getInstance().getConfigMeta(getType());

		List<String> configCategory = ServerManagerFactory.getInstance()
				.getConfigCategory(getType());
		XMLMessageParser fileParser = null;

		Document aggregatedDoc;
		try {
			aggregatedDoc = XMLLibraryPortabilityLayer.getNewDocument(
					getType(), null);
		} catch (MessageException me) {
			Debug.logStackTrace(me);
			throw new ProcessingException(me);
		}

		ServiceIdFilter idFilter = new ServiceIdFilter();
		for (String fileNm : configCategory) {

			String xmlDescription;
			try {
				xmlDescription = RepositoryManager.getInstance().getMetaData(
						categoryConfig, fileNm);
				fileParser = new XMLMessageParser(xmlDescription);
				fileParser = idFilter.getFilteredDOM(fileParser,
						ServiceMgrConsts.POLL_COMM_SERVER_CONFIG);
				Element document = fileParser.getDocument()
						.getDocumentElement();
				Node node = aggregatedDoc.importNode(document, true);
				aggregatedDoc.getDocumentElement().appendChild(node);

			} catch (Exception e) {
				Debug
						.error("Unable to load and parse configuration from repository "
								+ categoryConfig + " " + fileNm);
				throw new ProcessingException(e);
			}

			NodeList list = fileParser.getDocument().getElementsByTagName(
					ELEM_POLL_COMM_SERVER);
			for (int Ix = 0; Ix < list.getLength(); Ix++) {
				if (list.item(Ix) != null) {
					Element pollCommServerElement = (Element) list.item(Ix);

					String id, key, value, start, pollCommServerType;

					id = getConfigurationValue(pollCommServerElement,
							ConfigType.ATTRIBUTE, ATTR_COMM_SERVER_ID, null);
					key = getConfigurationValue(pollCommServerElement,
							ConfigType.ELEMENT, ELEM_COMM_SERVER_KEY, null);
					value = getConfigurationValue(pollCommServerElement,
							ConfigType.ELEMENT, ELEM_COMM_SERVER_TYPE, null);
					start = getConfigurationValue(pollCommServerElement,
							ConfigType.ATTRIBUTE, ATTR_COMM_SERVER_START,
							"true");
					pollCommServerType = getConfigurationValue(
							pollCommServerElement, ConfigType.ATTRIBUTE,
							"type", null);

					if (!StringUtils.hasValue(id) || !StringUtils.hasValue(key)
							|| !StringUtils.hasValue(value)) {
						Debug
								.log(
										Debug.XML_ERROR,
										"Could not configure Poll Comm Server since mandatory property are not configured");
						Debug.log(Debug.DB_ERROR, ELEM_POLL_COMM_SERVER + "="
								+ getType());
						Debug.log(Debug.DB_ERROR, ATTR_COMM_SERVER_ID + "="
								+ id);
						Debug.log(Debug.DB_ERROR, ELEM_COMM_SERVER_KEY + "="
								+ key);
						Debug.log(Debug.DB_ERROR, ELEM_COMM_SERVER_TYPE + "="
								+ value);

						// skip to initialize FTP Poller server if not
						// configured correctly.
						continue;
					}

					boolean startFlag = "false".equalsIgnoreCase(start) ? false
							: true;

					CommServerConf commServerConf = new CommServerConf(key,
							value, pollCommServerType);
					commServerConf.setStarted(startFlag);

					NodeList paramList = pollCommServerElement
							.getElementsByTagName(ELEM_PARAM);
					if (paramList != null) {
						for (int i = 0; i < paramList.getLength(); i++) {
							Element paramElement = (Element) paramList.item(i);

							String paramNm = getConfigurationValue(
									paramElement, ConfigType.ATTRIBUTE, "name",
									null);

							String paramVal = getConfigurationValue(
									paramElement, ConfigType.ATTRIBUTE,
									"value", null);

							commServerConf.setParamValue(paramNm, paramVal);

							if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
								Debug.log(Debug.OBJECT_LIFECYCLE,
										"Setting param [" + paramNm
												+ "] value [" + paramVal + "]");
						}
					}

					if (id == null)
						Debug.log(Debug.MSG_ERROR,
								"Could not initialize poll comm server with id ["
										+ id + "].");
					else
						commServerConfMap.put(id, commServerConf);

				}

			}
		}

		try {
			parser = new XMLMessageParser(aggregatedDoc);
		} catch (Exception e) {
			throw new ProcessingException(
					"Unable to create XMLMessageParser object "
							+ e.getMessage());
		}

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS,
					"done initializing poll comm servers of type [" + getType()
							+ "].. ");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.nightfire.comms.servicemgr.ServerManagerBase#remove(java.util.Map)
	 */
	@Override
	public void remove(Map parameters) {
		// TODO Auto-generated method stub

	}

	/**
	 * Start the Poll Comm Server identified by parameters.
	 * 
	 * @param parameters
	 *            identifies the Consumer to be started
	 * @throws ProcessingException
	 *             when unable to start the server.
	 */
	public void start(Map parameters) throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Starting Poll Comm Server ["
					+ getType() + "]: " + parameters);

		String id = (String) parameters.get(ATTR_COMM_SERVER_ID);

		if (id == null) {
			Debug.log(Debug.MSG_WARNING,
					"Could not start a Poll Comm Server with id [" + id + "].");
			return;
		}

		CommServerConf comServerConf = commServerConfMap.get(id);
		if (comServerConf == null) {
			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS,
						"Could not start a Poll Comm Server with id[" + id
								+ "] since Poll Comm Server "
								+ "was not already configured.");
		} else {
			ComServerBase commServer = comServerConf.getCommServer();
			if (commServer == null) {
				if (ServiceMgrConsts.FTP_POLLER.equals(comServerConf
						.getServerType())) {
					if ("true".equals(comServerConf.getParamVal(SSL_PARAM_NM))) {
						commServer = new com.nightfire.comms.ftp.ssl.FTPPoller(
								comServerConf.getDriverKey(), comServerConf
										.getDriverType());
					} else {
						commServer = new com.nightfire.comms.ftp.java.FTPPoller(
								comServerConf.getDriverKey(), comServerConf
										.getDriverType());
					}
				} else if (ServiceMgrConsts.ASYNC_TIMER_SERVER
						.equals(comServerConf.getServerType())) {
					commServer = new AsyncTimerServer(comServerConf
							.getDriverKey(), comServerConf.getDriverType());
				} else if (ServiceMgrConsts.FILE_SERVER.equals(comServerConf
						.getServerType())) {
					commServer = new AsyncFileServer(comServerConf
							.getDriverKey(), comServerConf.getDriverType());
				} else if (ServiceMgrConsts.SRM_EVENT_SERVER
						.equals(comServerConf.getServerType())) {
					commServer = new MultiCustomerEventConsumerServer(
							comServerConf.getDriverKey(), comServerConf
									.getDriverType());
				} else if (ServiceMgrConsts.SRM_CONFIGURED_QUEUES_JMS_SERVER
						.equals(comServerConf.getServerType())) {
					commServer = new JMSQueueEventConsumerServer(comServerConf
							.getDriverKey(), comServerConf.getDriverType());
				} else if (ServiceMgrConsts.SRM_JMS_SERVER.equals(comServerConf
						.getServerType())) {
					commServer = new MultiCustomerJMSConsumerServer(
							comServerConf.getDriverKey(), comServerConf
									.getDriverType());
				}

				else if (ServiceMgrConsts.IA_SERVER.equals(comServerConf
						.getServerType())) {
					try {
						commServer = new IAServer(comServerConf.getDriverKey(),
								comServerConf.getDriverType());
					} catch (FrameworkException e) {
						throw new ProcessingException(e);
					}
				}

				comServerConf.setCommServer(commServer);
				new Thread(commServer).start();
			} else {
				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS,
							"Poll Comm Server is already started [" + getType()
									+ "]");
			}
		}

		// update configuration with the latest status.
		updateXML(ELEM_POLL_COMM_SERVER, ATTR_COMM_SERVER_ID, id,
				ATTR_COMM_SERVER_START, "true");
	}

	/**
	 * Starts all the configured Poll Comm Servers
	 * 
	 * @throws ProcessingException
	 */
	public void startAll() throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS,
					"Starting All Comm Server of type = " + getType());

		Iterator iter = commServerConfMap.keySet().iterator();
		while (iter.hasNext()) {
			String id = (String) iter.next();
			CommServerConf conf = commServerConfMap.get(id);
			if (conf.isStarted()) {
				Notification notification = null;
				Map params = Collections.singletonMap(ATTR_COMM_SERVER_ID, id);
				try {
					start(params);
					notification = new Notification(
							"Started Comm Server of type = " + getType()
									+ " id : " + id);
				} catch (Exception e) {
					Debug.log(Debug.MSG_ERROR,
							"Could not start Comm Server of type = "
									+ getType() + " id : " + id);
					Debug.error(Debug.getStackTrace(e));
					notification = new Notification(
							"Error starting Comm Server of type = " + getType()
									+ " id : " + id, e);
				}
				notificationHandler.handle(notification);
			}

		}
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS,
					"done starting Comm Servers of type = " + getType());
	}

	/**
	 * Stops the Poll Comm Servers identified by parameters.
	 */
	public void stop(Map params) {
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Stopping Poll Comm Server ["
					+ getType() + "] " + params);

		String id = (String) params.get(ATTR_COMM_SERVER_ID);

		CommServerConf commServerConf = commServerConfMap.get(id);

		if (commServerConf == null) {
			Debug.log(Debug.MSG_WARNING,
					"No configured poll comm server found with ID[" + id + "]");
			return;
		}

		ComServerBase commServer = commServerConf.getCommServer();

		if (commServer == null) {
			return;
		} else {
			commServer.shutdown();
			commServerConf.setCommServer(null);
			commServerConf.setStarted(false);
		}

		/* update configuration with the latest status. */
		updateXML(ELEM_POLL_COMM_SERVER, ATTR_COMM_SERVER_ID, id,
				ATTR_COMM_SERVER_START, "false");

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "done stopping poll comm server ["
					+ getType() + "] " + id);
	}

	/**
	 * Stops all the running Poll Comm Servers.
	 * 
	 * @throws ProcessingException
	 */
	public void stopAll() throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Stopping all poll comm servers : "
					+ getType());

		Iterator iter = commServerConfMap.keySet().iterator();

		while (iter.hasNext()) {
			String id = (String) iter.next();
			Map map = Collections.singletonMap(ATTR_COMM_SERVER_ID, id);
			Notification notification = null;
			try {
				stop(map);
				notification = new Notification("Stopped poll comm server : "
						+ id);
			} catch (Exception e) {
				Debug.log(Debug.MSG_WARNING,
						"Exception while stopping poll comm server " + id + " "
								+ e.getMessage());
				Debug.log(Debug.MSG_WARNING, e.getMessage());
				notification = new Notification(
						"Exception occured while stopping Poll Comm Server: "
								+ id, e);
			}
			notificationHandler.handle(notification);
		}
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "done stopping all services .. ");
	}

	/**
	 * Place holder class to store the Poll Comm Server configuration.
	 */
	private class CommServerConf {
		// configuration attributes.
		private String driverKey;
		private String driverType;
		private String commServerType;

		private boolean started;

		private ComServerBase commServer;

		public ComServerBase getCommServer() {
			return commServer;
		}

		public void setCommServer(ComServerBase commServer) {
			this.commServer = commServer;
		}

		public CommServerConf(String driverKey, String driverType,
				String commServerType) {
			this.driverKey = driverKey;
			this.driverType = driverType;
			this.commServerType = commServerType;
		}

		// getters and setters for all the configuration parameters.
		public String getDriverKey() {
			return driverKey;
		}

		public void setDriverKey(String driverKey) {
			this.driverKey = driverKey;
		}

		public String getDriverType() {
			return driverType;
		}

		public void setDriverType(String driverType) {
			this.driverType = driverType;
		}

		public String getServerType() {
			return commServerType;
		}

		public boolean isStarted() {
			return started;
		}

		public void setStarted(boolean started) {
			this.started = started;
		}

		private Map<String, String> params = new HashMap<String, String>();

		public void setParamValue(String paramNm, String paramVal) {
			params.put(paramNm, paramVal);
		}

		public String getParamVal(String paramNm) {
			return params.get(paramNm);
		}
	}

}

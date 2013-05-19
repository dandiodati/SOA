package com.nightfire.comms.servicemgr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import com.nightfire.comms.jms.JMSConsumerHelper;
import com.nightfire.comms.jms.failover.MultipleKeyConsumer;
import com.nightfire.comms.jms.failover.MultipleKeyConsumerBase;
import com.nightfire.comms.jms.failover.TimerMultipleKeyConsumer;
import com.nightfire.comms.servicemgr.helper.MessageProcessorExecutor;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
//import com.nightfire.framework.db.DBUtils;
import com.nightfire.framework.jms.JMSException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

public class JMSQueueConsumerManager extends ServerManagerBase {
	/* singleton instance for JMSConsumerManager */
	private static JMSQueueConsumerManager single;

	/* configuration properties constants */
	public static final String JMS_CONSUMERS = "jms-consumers";

	public static final String JMS_CONSUMER_KEY = "key";

	public static final String JMS_CONSUMER_TYPE = "type";

	public static final String JMS_CONSUMER_METHOD = "method";

	public static final String JMS_QUEUE_NAME = "queue-name";

	public static final String MAX_POOL_SIZE = "maxpool-size";

	public static final String CORE_POOL_SIZE = "corepool-size";

	public static final String THREAD_POOL_REQUIRED = "threadpool-required";

	private static final String JMS_CONSUMER_START = "start";

	private static final String JMS_CONSUMER_ID = "id";

	private static final String JMS_RETRY_COUNT = "retry_count";

	private static final String JMS_MESSAGE_SELECTOR = "msg_selector";

	private static final String TIMER_INTERVAL = "timer-interval";

	private static final String ENABLE_TIMER = "enable-timer";

	private static final String CLIENT_ACK = "client-ack";

	private static final String IS_REPLICATED = "is_replicated";

	private static final String LEASE_EXTEND_BY = "lease-extend-by";

	private static final String LEASE_RENEW_INTERVAL = "lease-renew-interval";
	
	private static final String DB_POOL_KEY = "db-pool-key";

	private static final String SELECT_KEY_SQL = "SELECT a.key AS myKey, a.alternate_consumer altConsumerName, b.key AS altKey FROM consumer_lease_conf A, consumer_lease_conf B "
			+ "WHERE A.consumer = b.alternate_consumer AND a.consumer = ? ";

	/* Maps to store consumers and db instance associated */
	//private HashMap<String, JMSConsumerConf> consumersMap = new HashMap<String, JMSConsumerConf>();

	/* private constructor for singleton implementation */
	private JMSQueueConsumerManager(String type) {
		super(type);
	}

	private JMSQueueConsumerManager(String type, String gwsServerName) {
		super(type, gwsServerName);
	}

	/**
	 * returns a single instance of JMSConsumer.
	 * 
	 * @param type
	 * @return JMSConsumerManager
	 */
	public static JMSQueueConsumerManager getInstance(String type) {
		if (single == null)
			single = new JMSQueueConsumerManager(type);

		return single;
	}

	/**
	 * Parametrised method for instantiating JMSConsumer.
	 * 
	 * @param type
	 * @param gwsServerName
	 * @return
	 */
	public static JMSQueueConsumerManager getInstance(String type,
			String gwsServerName) {
		if (single == null)
			single = new JMSQueueConsumerManager(type, gwsServerName);

		return single;
	}

	/**
	 * Get map
	 */
	public Object getServerMetaCfg(String id) {
		return null;
	}

	/**
	 * NOT SUPPORTED
	 */
	public void add(Map parameters) throws ProcessingException {
		throw new UnsupportedOperationException("add method is not supported");
	}

	/**
	 * NOT SUPPORTED
	 */
	public void remove(Map parameters) {
		throw new UnsupportedOperationException("remove method is not supported");
	}

	/**
	 * start the JMSConsumer identified by parameters.
	 * 
	 * @param parameters
	 *            identifies the Consumer to be started
	 * @throws ProcessingException
	 *             when unable to register the consumer.
	 */
	public void start(Map parameters) throws ProcessingException {}

		/*if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Starting ... " + parameters);

		String id = (String) parameters.get(JMS_CONSUMER_ID);

		if (id == null) {
			Debug.log(Debug.MSG_WARNING,
					"Could not start a JMS Consumer Service with id [" + id
							+ "].");
			return;
		}

		JMSConsumerConf consumerConf = consumersMap.get(id);

		if (consumerConf == null) {
			Debug.log(Debug.MSG_WARNING,
					"Could not start a JMS Consumer serivce with id[" + id
							+ "] since consumer "
							+ "was not already configured.");
		} else if (consumerConf.getKeyConsumer() != null) {
			MultipleKeyConsumerBase keyConsumer = consumerConf.getKeyConsumer();
			try {
				keyConsumer.start();
			} catch (FrameworkException e) {
				/* update status with "false" value since an exception occurred
				   while starting 
				updateXML(ServiceMgrConsts.JMS_CONSUMER, JMS_CONSUMER_ID, id,
						JMS_CONSUMER_START, "false");

				Debug.log(Debug.MSG_ERROR, "Failed while registering consumer");
				throw new ProcessingException(e);
			}
		} else {
			/* attach a database connection to the consumer 
			Connection dbConn = consumerConf.getConnection();

			if (dbConn == null) {
				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(Debug.MSG_STATUS,
							"Getting a new database connection ...");
				try {
					dbConn = DBUtils.getConnection(consumerConf.getDbPoolKey());
				} catch (ResourceException re) {
					/* update status with "false" value since consumer count not
					   be started 
					updateXML(ServiceMgrConsts.JMS_CONSUMER, JMS_CONSUMER_ID,
							id, JMS_CONSUMER_START, "false");

					Debug.log(Debug.DB_ERROR,
							"Could not aquire database connection");
					Debug.log(Debug.DB_ERROR, "Cause : " + re.getMessage());
					throw new ProcessingException(re);
				}
				consumerConf.setConnection(dbConn);
			}

			MessageProcessorExecutor executor = consumerConf
					.getMessageProcessorExecutor();
			if (executor == null) {
				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(Debug.MSG_STATUS,
							"Getting a new message processor executor ...");
				executor = new MessageProcessorExecutor(consumerConf
						.getDriverKey(), consumerConf.getDriverValue(),
						getGwsServerName());
				/* set the retry count value for executor 
				executor.setRetryCount(consumerConf.getRetryCount());
				consumerConf.setMessageProcessorExecutor(executor);
			}

			JMSConsumerHelper consumer = consumerConf.getHelper(!consumerConf
					.isListener());

			if (consumer == null) {
				/* update status with "false" value since no consumer exists
				   with this id 
				updateXML(ServiceMgrConsts.JMS_CONSUMER, JMS_CONSUMER_ID, id,
						JMS_CONSUMER_START, "false");

				Debug.log(Debug.MSG_ERROR,
						"No JMSConsumerHelper found attached with id [" + id
								+ "].");
				throw new ProcessingException(
						"No JMSConsumerHelper found attached with id [" + id
								+ "].");
			}
			try {
				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug
							.log(Debug.MSG_STATUS,
									"Going to register the executor and db conn with consumer");
				consumer.register(executor, dbConn);
				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(Debug.MSG_STATUS,
							"JMS Queue Consumer with configuration ID[" + id
									+ "] has registered successfully");

			} catch (JMSException je) {
				/* update status with "false" value since an exception occurred
				   while starting 
				updateXML(ServiceMgrConsts.JMS_CONSUMER, JMS_CONSUMER_ID, id,
						JMS_CONSUMER_START, "false");

				Debug.log(Debug.MSG_ERROR, "Failed while registering consumer");
				throw new ProcessingException(je);
			}
		}
		/* update configuration with the latest status. 
		updateXML(ServiceMgrConsts.JMS_CONSUMER, JMS_CONSUMER_ID, id,
				JMS_CONSUMER_START, "true");
		if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Done starting ... " + parameters);
	}

	/**
	 * Initialise the consumer instance from the configuration defined in the
	 * repository.
	 * 
	 * @throws ProcessingException
	 */
	public void initialize() throws ProcessingException {
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "initializing consumers .. ");


		String categoryConfig = ServerManagerFactory.MGR_CONFIG_CATEGORY;

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
				fileParser = idFilter.getFilteredDOM(fileParser, JMS_CONSUMERS);
				Element document = fileParser.getDocument()
						.getDocumentElement();
				Node node = aggregatedDoc.importNode(document, true);
				aggregatedDoc.getDocumentElement().appendChild(node);

			} catch (Exception e) {
				Debug.error("Unable to load and parse file from  repository : "
						+ categoryConfig + " " + fileNm);
				throw new ProcessingException(e);
			}

			NodeList list = fileParser.getDocument().getElementsByTagName(
					ServiceMgrConsts.JMS_CONSUMER);
			for (int Ix = 0; Ix < list.getLength(); Ix++) {
				if (list.item(Ix) != null) {
					Element consumerElement = (Element) list.item(Ix);
					String id, key, value, start, queueName, threadPoolRequired, maxPoolStr, corePoolStr, method, retrycountStr;
					String msgSelector, enableTimer, timerInterval, ackMode, isReplicated, leaseExtendBy, leaseRenewInterval,dbPoolKey;

					id = getConfigurationValue(consumerElement,
							ConfigType.ATTRIBUTE, JMS_CONSUMER_ID, null);
					isReplicated = getConfigurationValue(consumerElement,
							ConfigType.ATTRIBUTE, IS_REPLICATED, "false");
					key = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, JMS_CONSUMER_KEY, null);
					value = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, JMS_CONSUMER_TYPE, null);
					start = getConfigurationValue(consumerElement,
							ConfigType.ATTRIBUTE, JMS_CONSUMER_START, "true");
					queueName = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, JMS_QUEUE_NAME, null);
					threadPoolRequired = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, THREAD_POOL_REQUIRED, "true");
					maxPoolStr = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, MAX_POOL_SIZE, "10");
					corePoolStr = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, CORE_POOL_SIZE, "10");
					method = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, JMS_CONSUMER_METHOD, "poll");
					retrycountStr = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, JMS_RETRY_COUNT, "-1");
					msgSelector = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, JMS_MESSAGE_SELECTOR, null);
					enableTimer = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, ENABLE_TIMER, null);
					timerInterval = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, TIMER_INTERVAL, null);
					ackMode = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, CLIENT_ACK, null);
					leaseExtendBy = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, LEASE_EXTEND_BY, null);
					leaseRenewInterval = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, LEASE_RENEW_INTERVAL, null);
					dbPoolKey = getConfigurationValue(consumerElement,
							ConfigType.ELEMENT, DB_POOL_KEY, null);

					if (!StringUtils.hasValue(id) || !StringUtils.hasValue(key)
							|| !StringUtils.hasValue(value)
							|| !StringUtils.hasValue(queueName)) {
						Debug
								.log(
										Debug.XML_ERROR,
										"Could not configure JMSQueueConsumer since mandatory property is not configured");
						Debug.log(Debug.DB_ERROR, JMS_CONSUMER_ID + "=" + id);
						Debug.log(Debug.DB_ERROR, JMS_CONSUMER_KEY + "=" + key);
						Debug.log(Debug.DB_ERROR, JMS_CONSUMER_TYPE + "="+ value);
						Debug.log(Debug.DB_ERROR, JMS_QUEUE_NAME + "="+ queueName);

						/* skip to initialise consumer if this consumer is not
						   configured correctly */
						continue;
					}

					boolean listener = "poll".equalsIgnoreCase(method) ? false	: true;
					boolean poolRequired = "true".equalsIgnoreCase(threadPoolRequired) ? true
							: false;
					boolean startFlag = "false".equalsIgnoreCase(start) ? false
							: true;
					boolean timerConsumer = "true".equals(enableTimer)
							&& timerInterval != null;
					boolean clientAckMode = "true".equalsIgnoreCase(ackMode) ? true
							: false;
					boolean isReplicatedMode = "yes"
							.equalsIgnoreCase(isReplicated) ? true : false;

					int corePoolSize = Integer.parseInt(corePoolStr);
					int maxPoolSize = Integer.parseInt(maxPoolStr);
					int retryCount = Integer.parseInt(retrycountStr);

					if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
						Debug.log(Debug.SYSTEM_CONFIG, id + " " + key + " "
								+ value + " " + start + " " + queueName + " "
								+ threadPoolRequired + " " + maxPoolStr + " "
								+ corePoolStr + " " + method + " " + retryCount
								+ " " + msgSelector + " " + enableTimer + " "
								+ timerInterval + " " + clientAckMode+" "+dbPoolKey);

					JMSConsumerHelper consumer = null;
					MultipleKeyConsumerBase keyConsumer = null;

					if (isReplicatedMode) {
						Connection dbConn = null;
						PreparedStatement pstm = null;
						ResultSet rs = null;
						try {
							String consumerNm = getGwsServerName() + "#" + id;
							dbConn = DBConnectionPool.getInstance()
									.acquireConnection();
							pstm = dbConn.prepareStatement(SELECT_KEY_SQL);
							pstm.setString(1, consumerNm);

							if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
								Debug.log(Debug.NORMAL_STATUS,
										"JMSQueueConsumerManager: Executing SQL: \n"
												+ SELECT_KEY_SQL);

							rs = pstm.executeQuery();

							if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
								Debug
										.log(Debug.NORMAL_STATUS,
												"JMSQueueConsumerManager: Finished Executing SQL Select statement...");

							if (rs.next()) {
								String myKey = rs.getString("myKey");
								String altKey = rs.getString("altKey");
								String altConsumerName = rs.getString("altConsumerName");

								MessageProcessorExecutor executor = new MessageProcessorExecutor(
										key, value, getGwsServerName());
								executor.setRetryCount(retryCount);

								if (!timerConsumer)
									keyConsumer = new MultipleKeyConsumer(
											myKey,
											altKey,
											consumerNm,
											altConsumerName,
											clientAckMode,
											queueName,
											executor,
											corePoolSize,
											maxPoolSize,
											Integer
													.parseInt(leaseRenewInterval),
											Integer.parseInt(leaseExtendBy),
											key, consumerNm);
								else
									keyConsumer = new TimerMultipleKeyConsumer(
											consumerNm,
											myKey,
											altConsumerName,
											altKey,
											queueName,
											clientAckMode,
											key,
											consumerNm,
											executor,
											corePoolSize,
											maxPoolSize,
											Integer
													.parseInt(leaseRenewInterval),
											Integer.parseInt(leaseExtendBy),
											Long.parseLong(timerInterval));

								if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
									Debug.log(Debug.NORMAL_STATUS,
											"initialized failover consumer :"
													+ keyConsumer.getClass()
															.getSimpleName()
													+ " key->" + myKey);
							}

							/*JMSConsumerConf consumerConf = new JMSConsumerConf();
							consumerConf.setKeyConsumer(keyConsumer);
							consumerConf.setStart(startFlag);
							consumerConf.setRetryCount(retryCount);
							consumerConf.setListener(true);
							consumersMap.put(id, consumerConf);
*/
							continue;
						} catch (Exception ex) {
							Debug.error("Failed to configure consumer: " + id);
							Debug.logStackTrace(ex);
							throw new ProcessingException(
									"Failed to configure consumer[id:" + id
											+ "]" + ex.getMessage());
						} finally {
								try {
								//	DBUtils.releaseConnection(dbConn);
								} catch (Exception ignore) { }

							//DBUtils.closeDBResources(pstm, rs);
						}
					} else if (timerConsumer)
						consumer = new JMSTimerConsumerHelper(queueName, null,
								msgSelector, listener, poolRequired,
								corePoolSize, maxPoolSize, Long
										.valueOf(timerInterval));
					else
						consumer = new JMSConsumerHelper(queueName, null,
								msgSelector, listener, poolRequired,
								corePoolSize, maxPoolSize);

					if (clientAckMode)
						consumer.setClientAckMode(true);

					if (getGwsServerName() != null)
						consumer.setConsumerNm(getGwsServerName() + "#" + id);
					else
						consumer.setConsumerNm(id);

					consumer.setGatewayNm(key);

					if (id == null)
						Debug.log(Debug.MSG_ERROR,
								"Could not initialize consumer with id [" + id
										+ "].");
					else {
						
					}
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

		Debug.log(Debug.NORMAL_STATUS, "done initializing consumers .. ");
	}

	/**
	 * starts all the configured JMSQueue
	 */
	public void startAll() throws ProcessingException {
		/*if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "starting All .. ");

		Iterator<String> iter = consumersMap.keySet().iterator();
		while (iter.hasNext()) {
			String id = iter.next();
			JMSConsumerConf conf = consumersMap.get(id);
			if (conf.isStart()) {
				Map params = Collections.singletonMap(JMS_CONSUMER_ID, id);
				Notification notification = null;
				try {
					start(params);
					notification = new Notification(
							"Started JMS Consumer Server:" + id);
				} catch (Exception e) {
					Debug.log(Debug.MSG_ERROR,
							"could not start JMS consumer id [" + id + "]");
					Debug.log(Debug.MSG_ERROR, e.getMessage());
					notification = new Notification(
							"Could not start JMS Consumer Server:" + id, e);
				}
				notificationHandler.handle(notification);
			}
		}

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "done starting All.. ");*/
	}

	public void stop(Map params) {
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			Debug.log(Debug.SYSTEM_CONFIG,
					"Stopping Consumer managers services.. " + params);

		String id = (String) params.get(JMS_CONSUMER_ID);
		//JMSConsumerConf consumerConf = consumersMap.get(id);
/*
		if (consumerConf == null) {
			Debug.log(Debug.MSG_WARNING, "No configured consumer found");
			return;
		}

		if (consumerConf.getKeyConsumer() != null) {
			consumerConf.getKeyConsumer().shutdown();
		} else {
			JMSConsumerHelper consumer = consumerConf.getHelper(false);

			if (consumer == null)
				return;
			else {
				consumer.disConnect();
			}

			/* release db connection */
			try {
				Debug.log(Debug.NORMAL_STATUS, "Releasing DB Connection.. ");
				if (DBInterface.isInitialized()) {
				//	DBUtils.releaseConnection(consumerConf.getDbPoolKey(), consumerConf.getConnection());
					Debug.log(Debug.NORMAL_STATUS,
							"Done Releasing DB Connection Successfully.. ");
				} else
					Debug.log(Debug.NORMAL_STATUS,
							"DB Connection pools have already been closed.");
			} catch (Exception ignore) {
				/* No operation on error */
			}
			//consumerConf.setConnection(null);
		}

	@Override
	public void stopAll() throws ProcessingException {
		// TODO Auto-generated method stub
		
	}
}
		// update configuration with the latest status.
		//updateXML(ServiceMgrConsts.JMS_CONSUMER, JMS_CONSUMER_ID, id,
		//		JMS_CONSUMER_START, "false");

		//if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
		//	Debug.log(Debug.NORMAL_STATUS, "done stoping... ");
	//}

	/**
	 * stops all the running JMS Queue Consumer services.
	 */
	//public void stopAll() throws ProcessingException {}
		/*
		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Stop all servicess .. ");

		Iterator<String> iter = consumersMap.keySet().iterator();

		while (iter.hasNext()) {
			String id =  iter.next();
			Map params = Collections.singletonMap(JMS_CONSUMER_ID, id);
			Notification notification = null;
			try {
				stop(params);
				notification = new Notification("Stopped JMS Consumer Server:"
						+ id);
			} catch (Exception e) {
				Debug.log(Debug.MSG_WARNING,
						"Exception while stoping JMS consumer "
								+ e.getMessage());
				Debug.log(Debug.MSG_WARNING, e.getMessage());
				notification = new Notification(
						"Exception occured while stopping JMS Consumer Server:"
								+ id, e);
			}
			notificationHandler.handle(notification);
		}

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "done stoping all services .. ");
	}*/
//}

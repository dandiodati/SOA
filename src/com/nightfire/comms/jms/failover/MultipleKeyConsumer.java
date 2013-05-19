package com.nightfire.comms.jms.failover;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ExceptionListener;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.jms.JMSException;
import com.nightfire.framework.jms.JMSPortabilityLayer;
import com.nightfire.framework.jms.JmsMsgStoreDAOFactory;
import com.nightfire.framework.jms.JmsMsgStoreDataObject;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

/**
 * Consumer that consumes messages for a given key and keeps a watch on an
 * alternate key. A watch is kept so as to support failover. This implementation
 * uses a lease mechanism to keep track of a consumer consuming messages. It
 * uses a database table for keeping the information related to key and it's
 * lease. A consumer needs to acquire it's key and keep renewing it's lease. In
 * case it goes down or is unable to serve then eventually its lease shall
 * expire. Another consumer that is keeping track of this key shall acquire (the
 * key) and start consuming the messages.
 * 
 * A thread pool is used to dispatch consumed message for processing.   
 * 
 * @author hpirosha
 * 
 */
public class MultipleKeyConsumer extends MultipleKeyConsumerBase {

	/**
	 * Use default database pool
	 * 
	 * @param key my key
	 * @param altKey key to keep a watch on
	 * @param myName name that shall identify this consumer
	 * @param altConsumerName name of the alternate consumer to keep a watch on 
	 * @param server JMSConsumerCallBack 
	 * @param corePoolSize core pool size to use for the thread pool
	 * @param maxPoolSize max pool size to use for the thread pool
	 * @param leaseRenewInterval lease will be renewed every leaseRenewInterval minutes
	 * @param extendLeaseBy lease will be extended by extendLeaseBy minutes
	 * @param daoKey persistentproperty key for message store DAO
	 * @param daoType persistentproperty propertytype for message store DAO
	 */
	public MultipleKeyConsumer(String key, String altKey, String myName, String altConsumerName,
			boolean useClientAck, String queueNm, JMSConsumerCallBack server,
			int corePoolSize, int maxPoolSize, int leaseRenewInterval,
			int extendLeaseBy, String daoKey, String daoType) {
		
		super(myName, key, altConsumerName, altKey, queueNm, useClientAck, daoKey, daoType, server, corePoolSize,
				maxPoolSize, leaseRenewInterval, extendLeaseBy, DBConnectionPool.getDefaultResourceName());
	}

	/**
	 * Use the passed in pool key to acquire database connections
	 * 
	 * @param key my key
	 * @param altKey altKey key to keep a watch on
	 * @param myName an id or name that shall identify this consumer
	 * @param altConsumerName name of the alternate consumer to keep a watch on	  
	 * @param dbPoolKey name of the database pool to be used
	 * @param server JMSConsumerCallBack 
	 * @param corePoolSize core pool size to use for the thread pool
	 * @param maxPoolSize max pool size to use for the thread pool
	 * @param leaseRenewInterval lease will be renewed every leaseRenewInterval minutes
	 * @param extendLeaseBy lease will be extended by extendLeaseBy minutes
	 * @param daoKey persistentproperty key for message store DAO
	 * @param daoType persistentproperty propertytype for message store DAO
	 */
	public MultipleKeyConsumer(String key, String altKey, String myName,String altConsumerName,
			boolean useClientAck, String queueNm, String dbPoolKey,
			JMSConsumerCallBack server, int corePoolSize, int maxPoolSize,
			int leaseRenewInterval, int extendLeaseBy, String daoKey, String daoType) {
		
		super(myName, key, altConsumerName, altKey, queueNm, useClientAck, daoKey, daoType, server, corePoolSize,
				maxPoolSize, leaseRenewInterval, extendLeaseBy, dbPoolKey);
	}
	
	private KeyLeaseTracker tracker = null; 
	private AcquireKeyOnStartup acqKey = null;

	/**
	 * Acquire own key and start
	 */
	public void start() throws FrameworkException {

		try {
			
			super.start();
			/* first acquire myKey */
			acqKey = new AcquireKeyOnStartup(myKey, myName);
			acqKey.acquire();
			
			/* its done, setting it to null */
			acqKey = null;

		} catch (KeyAcquisitionFailed ke) {
			Debug.error("Failed to acquire key " + ke.getMessage());

			throw new FrameworkException(ke);
		}

		if (useClientAck)
			dao = JmsMsgStoreDAOFactory.getInstance().getMsgStoreDAO(daoKey,
					daoType, queueNm);

		startConsumer(myKey);

		tracker = new KeyLeaseTracker(myKey, myName, this, altConsumerName, altKey, leaseRenewInterval,extendLeaseBy);
		tracker.start();

	}

	/**
	 * Initiate shutdown of consumer
	 */
	public void shutdown() {
		
		/* in case key acquiration has not finished */
		if(acqKey!=null)
			acqKey.forceShutdown();
		
		for(MessageConsumer consumer:consumers.values()) {
			consumer.stop();
            consumer.cleanUp();
            consumer = null;
		}
		
		consumers.clear();
		
		/* stop the lease tracker thread */
		tracker.shutdown();

		super.shutdown();
	}



	/**
	 * Returns <code>true</code> if both or any one of the consumer's is running
	 * @return boolean
	 */
	public boolean isAlive() {
		for(MessageConsumer consumer : consumers.values())
		{
			if(consumer.isRunning())
				return true;
		}
		return false;
	}
	

	Map<String, MessageConsumer> consumers = new HashMap<String, MessageConsumer>();
	/**
	 * Start a consumer
	 * @param key
	 * @throws FrameworkException
	 */
	protected void startConsumer(String key) throws FrameworkException
	{
		MessageConsumer consumer = consumers.get(key);
		if(consumer==null)
		{
			consumer = new MessageConsumer(this,key,dbPoolKey);
			consumer.initialize();
	        consumers.put(key, consumer);
	        return;
		}
		else if(consumer!=null && consumer.isRunning())
			return; /* consumer has already started */
		
        

	}
	
	/**
	 * Stop a consumer
	 * @param key
	 */
	protected void stopConsumer(String key)
	{
	
		MessageConsumer consumer = consumers.get(key);
		if(consumer==null)
			return; /* it has already been stopped */
		
		if (consumer.isRunning()) {
			consumer.stop();
			consumer.cleanUp();
		} else {
			consumer.cleanUp();
		}
		consumer = null;
			
	}
	
    protected JMSPortabilityLayer jpl = new JMSPortabilityLayer();

    /**
     * 
     * @author hpirosha
     *
     */
	private class MessageConsumer implements MessageListener, ExceptionListener {

		private MultipleKeyConsumer consumer;
		private String poolKey;
		private String key;
		private QueueConnection queueConn;
		private QueueSession queueSession;
		private QueueReceiver queueReceiver;
		private boolean isRunning = false;
		private Connection conn = null;
		
		/**
		 * 
		 * @param consumer
		 * @param key
		 * @param poolKey
		 */
		MessageConsumer(MultipleKeyConsumer consumer,String key, String poolKey) {
			this.consumer = consumer;
			this.key = key;
			this.poolKey = poolKey;
		}
		
		/**
		 * 
		 * @throws FrameworkException
		 */
		void initialize() throws FrameworkException {

			String currentPoolKey = DBConnectionPool.getThreadSpecificPoolKey();
			try {
				
				DBConnectionPool.setThreadSpecificPoolKey(poolKey);
				conn = DBConnectionPool.getInstance(poolKey)
						.acquireConnection();
				
				queueConn = jpl.createQueueConnection(conn);

				if (useClientAck)
					queueSession = jpl
							.createQueueSessionWithClientAck(queueConn);
				else
					queueSession = jpl.createQueueSession(queueConn);

				queueReceiver = jpl.createQueueReceiver(queueSession, jpl
						.getQueue(queueSession, queueNm), key);

				queueConn.start();

				queueReceiver.setMessageListener(this);

				isRunning = true;
			} catch (JMSException je) {
				throw new FrameworkException("A jms exception occured while starting the consumer :"+je.getMessage());
			}
			 catch (javax.jms.JMSException je) {
				throw new FrameworkException("A jms exception occured while starting the consumer :"+je.getMessage());
			}
			finally {
                /* reset the default key */
				DBConnectionPool.setThreadSpecificPoolKey(currentPoolKey);
			}
		}
	
		boolean isRunning() {
			return isRunning;
		}
		
		/**
		 * Callback method for de-queued message processing.
		 */
		public void onMessage(Message message) {

			String header = null;
			String body = null;

			try {

			header = jpl.getHeaderProperties(message);
			body = jpl.getTextMessage(message);

			if (useClientAck) {
				/* store the message first and send an ack */
					JmsMsgStoreDataObject dataObject = new JmsMsgStoreDataObject();
					dataObject.setJmsMessage(message);
					dataObject.setMsgHeader(header);
					dataObject.setConsumerNm(consumer.getName());

					boolean saved = consumer.getMsgStoreDAO().insert(
							dataObject);

					/*
					 * call recover; without acknowledging the message skip
					 * further processing
					 */

					if (!saved) {
						queueSession.recover();
						return;
					}

					message.acknowledge();

				} 
			}
			catch (javax.jms.JMSException e) {

				Debug.error("Unable to send an  acknowledgement.."
						+ Debug.getStackTrace(e));
				throw new RuntimeException(
						"Failed to send an acknowledement back..."
								+ e.getMessage());

			}
			catch (JMSException e) {

				Debug.error("Unable to send an  acknowledgement.."
						+ Debug.getStackTrace(e));
				throw new RuntimeException(
						"Failed to send an acknowledement back..."
								+ e.getMessage());

			}
			
			String msgId = UNAVAILABLE_CONST;
			try {
				msgId = message.getJMSMessageID();
			} catch (Exception ignore) { /* can't do anything */
			}

			consumer.process(header, body, msgId);

		}
		
		/**
		 * Method to stop jms message listener.
		 */
		void stop() {

			isRunning = false;

			try {
				if (Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
					Debug.log(Debug.STATE_LIFECYCLE,
							"stopping consumer for key[" + key + "]");

                if(queueReceiver!=null)
                    queueReceiver.close();

				if (queueSession != null)
					queueSession.close();

				if (queueConn != null)
					queueConn.close();

			} catch (Exception ignore) {
			}

		}

		/**
		 * Method to clean-up
		 */
		private void cleanUp() {
			/* do clean-up */
			String currentPoolKey = DBConnectionPool.getThreadSpecificPoolKey();
			try {
				if (StringUtils.hasValue(dbPoolKey)) {
					DBConnectionPool.setThreadSpecificPoolKey(dbPoolKey);
					DBConnectionPool.getInstance(dbPoolKey).releaseConnection(
							conn);
				} else
					DBConnectionPool.getInstance().releaseConnection(conn);
			} catch (ResourceException e) {
				Debug.warning("Exception occured while releasing connection :"
						+ e.getMessage());
			} finally {
				if (StringUtils.hasValue(dbPoolKey))
					DBConnectionPool.setThreadSpecificPoolKey(currentPoolKey);
			}

		}

		/**
		 * Method invoked by the JMS provider in case of an errorneous condition.
		 */
		public void onException(javax.jms.JMSException exp) {
			
			Debug.warning("An exception is thrown by JMS provider :"+exp.getMessage());
			Debug.warning("Stopping and re-starting again...");
			stop();
			
			cleanUp();
			
			/* try to re-initialize */
			try {
				initialize();
			} catch (FrameworkException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException("Failed to re-initialize after getting an exception from JMS provider ",e);
			}
			
		}
	}

}

package com.nightfire.comms.jms.failover;

import com.nightfire.framework.jms.AbstractJmsMsgStoreDAO;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public abstract class MultipleKeyConsumerBase implements KeyLeaseEventListener {
	
	protected String myName, altConsumerName, myKey, altKey , queueNm, daoKey, daoType, dbPoolKey;
	protected boolean useClientAck;
	protected AbstractJmsMsgStoreDAO dao = null;
	protected int leaseRenewInterval, extendLeaseBy;
	protected static String UNAVAILABLE_CONST = "UNAVAILABLE";
	protected JMSConsumerCallBack server;
	protected int corePoolSize, maxPoolSize;

	MultipleKeyConsumerBase(String myName, String myKey, String altConsumerName, String altKey, 
			String queueNm, boolean useClientAck, String daoKey,
			String daoType, JMSConsumerCallBack server, int corePoolSize,
			int maxPoolSize,int leaseRenewInterval, int extendLeaseBy,String dbPoolKey) {
		this.myName = myName;
		this.altConsumerName = altConsumerName;
		this.myKey = myKey;
		this.altKey = altKey;
		this.queueNm = queueNm;
		this.useClientAck = useClientAck;
		this.daoKey = daoKey;
		this.daoType = daoType;
		this.leaseRenewInterval = leaseRenewInterval;
		this.extendLeaseBy = extendLeaseBy;
		this.dbPoolKey = dbPoolKey;
        this.server = server;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
	}

	/**
	 * Method to handle events related to Key lease.
	 * @param event
	 */
	public void handleEvent(KeyLeaseEvent event) throws FrameworkException {

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			Debug.log(Debug.NORMAL_STATUS, "Consumer[" + myName
					+ "] : Recieved event ->" + event.eventType());

		switch (event.eventType()) {

		case LEASE_FOR_OWN_KEY_AVAILABLE: {
			/* if own key consumer not started, then start it */
			startConsumer(myKey);
			break;
		}

		case LEASE_FOR_ALTERNATE_KEY_AVAILABLE: {
			/* if alternate consumer not running then start it */
			startConsumer(altKey);
			break;
		}

		case RELEASE_ALTERNATE_KEY: {
			/* if alternate consumer is running then stop it */
			stopConsumer(altKey);
			break;
		}

		case OWN_KEY_ALREADY_ACQUIRED: {
			/* if own key consumer is not running then start it */
			stopConsumer(myKey);
			break;
		}

		}
	}

	/**
	 * Gets the status of the consumer
	 */
	public abstract boolean isAlive();

	/**
	 * Initializes the message dispatcher
	 * @throws FrameworkException
	 */
	public void start() throws FrameworkException
    {
         dispatcher = new MessageDispatcher(server, corePoolSize, maxPoolSize);
    }
	
	/**
	 * 
	 * @param key
	 * @throws FrameworkException
	 */
	protected abstract void startConsumer(String key) throws FrameworkException;

	/**
	 * 
	 * @param key
	 */
	protected abstract void stopConsumer(String key);

	private MessageDispatcher dispatcher = null;
	
	/**
	 * Process a consumed message.
	 * @param msg
	 */
	public void process(String header,String body, String msgId) {
		
		/* need to call the client ack before 
		 * submitting to the thread pool */
		dispatcher.dispatch(header, body, msgId);
	}

	public String getName() {
		return this.myName;
	}

	public AbstractJmsMsgStoreDAO getMsgStoreDAO() {
		return this.dao;
	}

	public void shutdown() {
		if (dispatcher != null) {
			dispatcher.shutdown();
			dispatcher = null;
		}
	}
}

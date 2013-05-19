package com.nightfire.comms.jms.failover;

import java.sql.Connection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.jms.JMSException;
import com.nightfire.framework.jms.JMSPortabilityLayer;
import com.nightfire.framework.jms.JmsMsgStoreDAOFactory;
import com.nightfire.framework.jms.JmsMsgStoreDataObject;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class TimerMultipleKeyConsumer extends MultipleKeyConsumerBase {

	private long timerInterval;
	/**
	 * Use default database pool
     *
	 * @param myName an id or name that shall identify this consumer
	 * @param key my key
	 * @param altKey key to keep a watch on
	 * @param daoKey persistentproperty key for message store DAO
	 * @param daoType persistentproperty propertytype for message store DAO
	 * @param server JMSConsumerCallBack 
	 * @param corePoolSize core pool size to use for the thread pool
	 * @param maxPoolSize max pool size to use for the thread pool
	 * @param leaseRenewInterval lease will be renewed every leaseRenewInterval minutes
	 * @param extendLeaseBy lease will be extended by extendLeaseBy minutes
	 * @param timerInterval 
	 */
	public TimerMultipleKeyConsumer(String myName, String key, String altConsumerName,String altKey, 
			String queueNm, boolean useClientAck,String daoKey, String daoType,JMSConsumerCallBack server,
			int corePoolSize, int maxPoolSize,
			int leaseRenewInterval, int extendLeaseBy,long timerInterval) {
		
		super(myName, key, altConsumerName,altKey, queueNm, useClientAck, daoKey, daoType, server, corePoolSize,
				maxPoolSize, leaseRenewInterval, extendLeaseBy, DBConnectionPool.getDefaultResourceName());
		this.timerInterval = timerInterval;

	}

	/**
	 * Use the passed in pool key to acquire database connections
	 * 
	 * @param myName an id or name that shall identify this consumer
	 * @param key my key
	 * @param altKey key to keep a watch on
	 * @param daoKey persistentproperty key for message store DAO
	 * @param daoType persistentproperty propertytype for message store DAO
	 * @param server JMSConsumerCallBack 
	 * @param corePoolSize core pool size to use for the thread pool
	 * @param maxPoolSize max pool size to use for the thread pool
	 * @param leaseRenewInterval lease will be renewed every leaseRenewInterval minutes
	 * @param extendLeaseBy lease will be extended by extendLeaseBy minutes
	 * @param dbPoolKey pool key to get database connection
	 * @param timerInterval
	 */
	public TimerMultipleKeyConsumer(String myName, String key, String altConsumerName,String altKey, 
			String queueNm, boolean useClientAck,String daoKey, String daoType,JMSConsumerCallBack server,
			int corePoolSize, int maxPoolSize,
			int leaseRenewInterval, int extendLeaseBy,String dbPoolKey,long timerInterval) {
		super(myName, key, altConsumerName, altKey, queueNm, useClientAck, daoKey, daoType, server, corePoolSize,
				maxPoolSize, leaseRenewInterval, extendLeaseBy, dbPoolKey);
	
		this.timerInterval = timerInterval;
	}

	
	public boolean isAlive() {
		for(MessageConsumer consumer : consumers.values())
		{
			if(consumer.isRunning())
				return true;
		}
		return false;
	}

	private KeyLeaseTracker tracker = null; 
	private AcquireKeyOnStartup acqKey = null;

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
		
		/* in case key acquisition has not finished */
		if(acqKey!=null)
			acqKey.forceShutdown();
		
		for(MessageConsumer consumer:consumers.values()) {
			consumer.stop();
		}
		
		consumers.clear();
		
		/* stop the lease tracker thread */
		tracker.shutdown();
		
		/* call the base class to do the cleanup */
		super.shutdown();
	}

	Map<String, MessageConsumer> consumers = new HashMap<String, MessageConsumer>();
	@Override
	protected void startConsumer(String key) throws FrameworkException {

		MessageConsumer consumer = consumers.get(key);
		if(consumer==null)
		{
			consumer = new MessageConsumer(this,key,timerInterval,dbPoolKey);
	        consumers.put(key, consumer);
		}
		else if(consumer!=null && consumer.isRunning())
			return; /* consumer has already started */

	}

	@Override
	protected void stopConsumer(String key) {
		MessageConsumer consumer = consumers.get(key);
		if(consumer==null)
			return; /* it has already been stopped */
		
		if (consumer.isRunning()) {
			consumer.stop();
		} 
		consumer = null;
		
	}

	private class MessageConsumer {
		
		private JMSPortabilityLayer jpl = null;
		private String msgSelector = null;
		private TimerMultipleKeyConsumer consumer;
		private Timer timer = null; 
		private String dbPoolKey = null;
		
		MessageConsumer(TimerMultipleKeyConsumer consumer, String msgSelector,long timerInterval,String dbPoolKey) {

			this.consumer = consumer;
			this.msgSelector = msgSelector;
			this.dbPoolKey = dbPoolKey;
			timer = new Timer(msgSelector);
	        timer.scheduleAtFixedRate( new TimerTask() {
	                    public void run()
	                    {
	                    	MessageConsumer.this.run();
	                    }
	                  },0,timerInterval*1000);


		}
		
	    public boolean isRunning() {
	    	return timer!=null ? true : false;
		}

		/**
	     * Invoked when consumer awakens. It creates a queue browser to get the count of messages
	     * available. Consumes them one by one and invokes callback class that does further processing. 
	     */
	    public void run()
	    {
	        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
	            Debug.log(Debug.STATE_LIFECYCLE,"Executing TimerMultipleKeyConsumer.run()....");

	        	QueueSession queueSession = null;
	        	QueueReceiver queueReceiver  = null;
	        	QueueConnection queueConn = null;
	        	Connection dbConn = null;
                String currentPoolKey = DBConnectionPool.getThreadSpecificPoolKey();

	            try
	            {

					DBConnectionPool.setThreadSpecificPoolKey(dbPoolKey);
					dbConn = DBConnectionPool.getInstance(dbPoolKey)
							.acquireConnection();
	                DBConnectionPool.setThreadSpecificPoolKey(currentPoolKey);


	                jpl = new JMSPortabilityLayer();
	                queueConn = jpl.createQueueConnection(dbConn);
	                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
	                    Debug.log(Debug.STATE_LIFECYCLE,"Created queue connection");

	                
	                if(useClientAck)
	                    queueSession = jpl.createQueueSessionWithClientAck(queueConn);
	                else
	                	queueSession = jpl.createQueueSession(queueConn);
	                
	                Queue queue = jpl.getQueue(queueSession, queueNm);

	                queueReceiver = jpl.createQueueReceiver(queueSession,queue,msgSelector);

	                queueConn.start();

	                /* get number of messages present in queue. */
	                QueueBrowser browser = null;
	                
	                if(msgSelector !=null)
	                    browser = queueSession.createBrowser(queue,msgSelector);
	                else
	                    browser = queueSession.createBrowser(queue);
	                
	                Enumeration enumeration = browser.getEnumeration();
	                long msgCount = 0;
	                while(enumeration.hasMoreElements())
	                {
	                    msgCount++;
	                    enumeration.nextElement();
	                }
	                browser.close();
	                
	                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
	                    Debug.log(Debug.STATE_LIFECYCLE,"TimerMultipleKeyConsumer : Total message count ="+msgCount);

	                String header=null, body=null;
	                // consume all messages that are present
	                for(; msgCount > 0; msgCount--)
	                {
	                    Message message = queueReceiver.receiveNoWait();
	                    
	                    if(message!=null)
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
	                

	                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
	                    Debug.log(Debug.STATE_LIFECYCLE,"Finished consumed all messages..going to sleep");
	            
	            }
	            catch(FrameworkException exp)
	            {
	                Debug.error("Exception occurred while creating QueueReciever.."+exp.toString());
	                throw new RuntimeException(exp);
	            }
	            catch(javax.jms.JMSException exp)
	            {
	                Debug.error("Exception occurred while creating consumer.."+exp.toString());
	                throw new RuntimeException(exp);
	            }
	            finally
	            {
	                // stop the consumer now.
	                try
	                {
	                    if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
	                        Debug.log(Debug.STATE_LIFECYCLE,"TimerMultipleKeyConsumer -> Finished current task.");

	                    if(queueSession!=null)
	                        queueSession.close();

	                    if(queueConn!=null)
	                       queueConn.close();
	                    
	                    if(queueReceiver!=null)
	                        queueReceiver.close();
	                }
	                catch(Exception ignore)
	                {
	                }

	                try
	                {
						DBConnectionPool.setThreadSpecificPoolKey(dbPoolKey);
						DBConnectionPool.getInstance(dbPoolKey).releaseConnection(dbConn);
	                } catch(Exception ignore) {}
	                finally
	                {
						DBConnectionPool.setThreadSpecificPoolKey(currentPoolKey);
	                }
	            }
	    }
	    
	    void stop()
	    {
	    	if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
	    		Debug.log(Debug.OBJECT_LIFECYCLE,"> Shutting TimerMultipleKeyConsumer for key ->"+msgSelector);
	        
	    	if(timer!=null)
	        {
	            timer.cancel();
	            timer = null;
	        }

	    }
	}
}

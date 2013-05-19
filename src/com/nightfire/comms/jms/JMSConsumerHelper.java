package com.nightfire.comms.jms;

import com.nightfire.framework.jms.AbstractJmsMsgStoreDAO;
import com.nightfire.framework.jms.JMSConstants;
import com.nightfire.framework.jms.JMSPortabilityLayer;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.jms.JMSException;
import com.nightfire.framework.jms.JmsMsgStoreDAOFactory;
import com.nightfire.framework.jms.JmsMsgStoreDataObject;
//import com.nightfire.framework.db.DBUtils;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

import javax.jms.ExceptionListener;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.Message;

import java.sql.Connection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Helper class which is registered by a CommServer on JMS queues for consumption of messages.
 * All the set methods needs to be called before calling the register method.
 */
public class JMSConsumerHelper extends Thread implements MessageListener , ExceptionListener
{
    /**
     * Name of JMS queue on which this listener registers.
     */
    protected String queueNm;

    /**
     * CustomerId is used as part of JMS Message selector.
     */
    protected String customerId;

	/**
	 * String that stores JMS Message selector
	 */
    protected String msgSelector;

    /**
     * Reference of the commServer that needs to be notified when a message is recieved.
     */
    protected JMSConsumerCallBack commServer;

    /**
     * Queue Session object
     */
    protected QueueSession queueSession = null;

    /**
     * Queue Connection object
     */
    protected QueueConnection queueConn = null;

    /**
     * JMSPortability class is required to create the session/connection to queue.
     */
    protected JMSPortabilityLayer jpl;

    /**
     * boolean flag to indicate whether a listener is to be created for message consumption
     * or a poll consumer.
     */
    protected boolean listener = false;

    /**
     * Thread Pool Executor.
     */
    protected ThreadPoolExecutor executor = null;

    /**
	 * Queue Receiver object used to receive messages from a queue.
	 */
    protected QueueReceiver queueReceiver = null;

    /**
	 * Queue to receive messages from.
	 */
    protected Queue queue =  null;

    /**
     * variables for thread pool executor service.
     */
    protected boolean threadPoolRequired = false;
    protected int corePoolSize;
    protected int maxPoolSize;
    protected Connection dbConn;
    /**
     * Default mode is auto acknowledgement.
     */
    protected boolean clientAckMode = false;
    
    /**
     * A name to be associated with this jms consumer. 
     */
    protected String consumerNm = null;
    
    /**
     * Name of gateway with whom this consumer is associated. 
     */
    protected String gatewayNm = null;
    
    /**
     * <code>true</code> if a new connection is acquired by this class.
     * A new connection from DEFAULT pool is acquired when the passed in 
     * connection is closed/invalid. 
     */
    protected boolean acquiredNew = false;
    
    /**
     * If the consumer reconnects to the queue then this flag is set to
     * <code>true</code>. 
     */
    protected boolean reConnect = false;
    
    /**
     * If set then this pool key would be used to obtain a new connection
     * during reConnect. 
     */
    protected String dbConnPoolKey = null;
    
    private static String UNAVAILABLE_CONST = "UNAVAILABLE";;
    
    public String getQueueNm() {
		return queueNm;
	}


	public void setQueueNm(String queueNm) {
		this.queueNm = queueNm;
	}


	public String getMsgSelector() {
		return msgSelector;
	}


	public void setMsgSelector(String msgSelector) {
		this.msgSelector = msgSelector;
	}


	public boolean isListener() {
		return listener;
	}


	public void setListener(boolean listener) {
		this.listener = listener;
	}


	public boolean isThreadPoolRequired() {
		return threadPoolRequired;
	}


	public void setThreadPoolRequired(boolean threadPoolRequired) {
		this.threadPoolRequired = threadPoolRequired;
	}


	public int getCorePoolSize() {
		return corePoolSize;
	}


	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}


	public int getMaxPoolSize() {
		return maxPoolSize;
	}


	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}


	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

    public boolean isClientAckMode() {
        return clientAckMode;
    }


    protected AbstractJmsMsgStoreDAO dao = null;
    public void setClientAckMode(boolean clientAckMode) {
        this.clientAckMode = clientAckMode;
    }

    public String getConsumerNm() {
        return consumerNm;
    }


    public void setConsumerNm(String consumerNm) {
        this.consumerNm = consumerNm;
    }


    public String getGatewayNm() {
        return gatewayNm;
    }


    public void setGatewayNm(String gatewayNm) {
        this.gatewayNm = gatewayNm;
    }

	/**
     * Constructor
     * @param queueNm Name of the queue
     * @param customerId id is used for logging messages recieved by this message listner.
     * @param msgSelector used to selectively consume messages from the queue.
     * @param listener  if <code>false</code> then queue is polled for messages.
     *                  if <code>true</code> then a message listener is registered on the queue.
     * @param threadPoolRequired use a thread pool to process dequed messages
     * @param corePoolSize core size of thread pool
     * @param maxPoolSize  max size of thread pool               
     */
    public JMSConsumerHelper(String queueNm, String customerId, String msgSelector,boolean listener,
                             boolean threadPoolRequired,int corePoolSize,int maxPoolSize)
	{
		this.queueNm = queueNm;
		this.customerId = customerId;
        this.msgSelector = msgSelector;
        this.listener = listener;
        this.threadPoolRequired = threadPoolRequired;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Constructor
     * @param queueNm Name of the queue
     * @param customerId id is used for logging messages recieved by this message listner.
     * @param msgSelector used to selectively consume messages from the queue.
     * @param listener  if <code>false</code> then queue is polled for messages.
     *                  if <code>true</code> then a message listener is registered on the queue.
     * @param threadPoolRequired use a thread pool to process dequed messages
     * @param corePoolSize core size of thread pool
     * @param maxPoolSize  max size of thread pool               
     * @param dbConnPoolKey pool id to be used to reConnect the consumer, in case of an exception.
     *                      This is required when consumer is started to dequeue messages from a 
     *                      queue which belongs to database other than the default.                   
     */
    public JMSConsumerHelper(String queueNm, String customerId, String msgSelector,boolean listener,
                             boolean threadPoolRequired,int corePoolSize,int maxPoolSize,String dbConnPoolKey)
    {
            this(queueNm, customerId, 
                 msgSelector,listener,
                 threadPoolRequired,corePoolSize,
                 maxPoolSize);
            
            this.dbConnPoolKey = dbConnPoolKey;
    }


	/**
	 * Method used for registering this listener on queue.
	 * @param commServer to whom messages would be passed in as they are recieved from the queue.
	 * @param dbConn database connection is required to create the JMS session.
	 * @throws JMSException if any exception occurs while registering this message listener on queue.
	 */
	public void register(JMSConsumerCallBack commServer,Connection dbConn) throws JMSException
	{
	    if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Registering JMSConsumerHelper....");

        try
		{
            this.dbConn = dbConn;

            jpl = new JMSPortabilityLayer();
            queueConn = jpl.createQueueConnection(dbConn);
    	    if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Created queue connection");

            if(clientAckMode)
                queueSession = jpl.createQueueSessionWithClientAck(queueConn);
            else
                queueSession = jpl.createQueueSession(queueConn);
            
    	    if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Created queue session with clientAckMode = "+clientAckMode);

            queue = jpl.getQueue(queueSession, queueNm);

            if(msgSelector!=null)
               queueReceiver = jpl.createQueueReceiver(queueSession,queue,msgSelector);
            else
               queueReceiver = jpl.createQueueReceiver(queueSession,queue);

            this.commServer = commServer;
            
            // explicitly set it to false; this is done since a reconnect would set it to true.
            // the onException method would shut down this consumer and would make disConnect to true.
            disConnect = false;
            queueConn.start();

            if(threadPoolRequired)
                executor = new ThreadPoolExecutor(  corePoolSize, maxPoolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue(maxPoolSize) );

            if(clientAckMode && dao==null)
                dao = JmsMsgStoreDAOFactory.getInstance().getMsgStoreDAO(getGatewayNm(),getConsumerNm(),getQueueNm());

            if(listener)
            {
                queueConn.setExceptionListener(this);
                queueReceiver.setMessageListener(this);
            }
            else
            {
                // If the consumer is trying to reConnect to queue then start method on thread
                // cannot be invoked ; this would throw an IllegalThreadStateException  
                if(!reConnect)
                    this.start();
                
            }

            this.setPriority(Thread.MAX_PRIORITY);
            
            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Sucessfully Registered JMSConsumerHelper with queue ["+queueNm+"]....");

		}
        catch(FrameworkException exp)
        {
            Debug.error("Exception occurred while creating ThreadPoolExecutorService.."+exp.toString());
            throw new JMSException(exp);
        }
        catch(javax.jms.JMSException exp)
        {
            Debug.error("Exception occurred while creating consumer.."+exp.toString());
            throw new JMSException(exp);
        }
	}

    private boolean disConnect = false;
	/**
     * Disconnect will immediately shutdown the consumer,
     * it will close the queue connection and session.
     */
    public void disConnect()
	{
        disConnect = true;
        shutdown();
    }

    private void shutdown()
    {
        try
        {
            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Disconnecting JMSConsumer...");
            
            if(queueSession!=null)
                queueSession.close();

            if(queueConn!=null)
               queueConn.close();
            
        }
        catch(Exception ignore)
        {
            //ignore.printStackTrace();
        }

        cleanUp();
	}

    protected void cleanUp()
    {
        if(threadPoolRequired && executor!=null)
            executor.shutdown();

        /* release the new connection that is acquired by this class */
        if(acquiredNew)
        {
            try 
            {
            	//DBUtils.releaseConnection(dbConnPoolKey, dbConn);
            } 
            catch (Exception e) {
                Debug.warning("Exception occured while releasing connection :"+e.getMessage());
            }
        }
    }

    /**
     * Callback Method declared in JMS MessageListener interface.
     */
    public void onMessage(Message msg)
    {
        try
        {
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
               Debug.log(Debug.MSG_BASE, "Listener ["+this.hashCode()+"] consuming message for customer ["+this.customerId+"]");

            processMessage(msg);
        }
        catch (Exception exp)
        {
            Debug.error("Exception occurred while consuming message.."+exp.toString());
            Debug.error(Debug.getStackTrace(exp));
        }
    }

    /**
     * Get the associated customerId for which this consumer is listening
     * @return String
     */
    public String getCustomerId()
    {
        return customerId;
    }

    public void run()
    {
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Inside run of thread "+this.getName());
         
        while(true && !disConnect)
         {
           try
             {
                // If an exception occurs while consuming a message then
                // it should be enqueued in the ExceptionQueue
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS,"Ready to recieve message from queue with messageSelector["+msgSelector+"]");

                 Message message = jpl.receiveWaitForEver(queueSession,queueReceiver);
                 processMessage(message);

             }
              catch(JMSException jmsExp)
              {
                 if(!disConnect)
                 {
                    Debug.error(StringUtils.getClassName(this)+
                      "An exception ocurred while consuming from queue["+queueNm+"]"+
                          jmsExp.toString());

                   onException(new javax.jms.JMSException(jmsExp.getMessage()));
                 }
               }
               catch(Exception exp)
               {
                 if(!disConnect)
                   Debug.error(StringUtils.getClassName(this)+
                       "Error ocurred while consuming from queue["+queueNm+"]"+
                           exp.toString()+"\n"+Debug.getStackTrace(exp));
               }
           }
       }


    /**
     * Called from run or onMessage method to do further processing.
     * @param message
     * @throws JMSException
     */
    private void processMessage(Message message) throws JMSException
    {
       String header = jpl.getHeaderProperties(message);
       String body = jpl.getTextMessage(message);

       if(clientAckMode)
       {
           /* store the message first and send an ack */
           try 
           {
               JmsMsgStoreDataObject dataObject = new JmsMsgStoreDataObject();
               dataObject.setJmsMessage(message);
               dataObject.setMsgHeader(header);
               
               boolean inserted = dao.insert(dataObject);
               
               if(!inserted)
               {
                   /* call recover; without acknowledging the message
                    skip further processing */
                   queueSession.recover();
                   return;
               }
               
               message.acknowledge();
               
           }
           catch (javax.jms.JMSException e) 
           {
              
               Debug.error("Unable to send an  acknowledgement.."+Debug.getStackTrace(e));
               throw new JMSException("Failed to send an acknowledement back..."+e.getMessage());
              
           }
        }

       String msgId = UNAVAILABLE_CONST;
       try
       {
           msgId = message.getJMSMessageID();
       }
       catch(Exception ignore) {}

       Processor processor = new Processor(header,body,msgId);
       if(threadPoolRequired)
       {
               boolean notProcessed = true;
               while(notProcessed)
               {
                   try
                   {
                       executor.execute(processor);
                       notProcessed = false;
                   }
                   catch(RejectedExecutionException ree)
                   {
                       /* if executor rejects this message wait for a
                          free thread in the pool. */ 
                       try
                       {
                           Thread.sleep(10);
                       }
                       catch (InterruptedException e)
                       {
                           Debug.log(Debug.ALL_ERRORS, "Interrupted sleeping thread..");
                       }
                   }
                   catch(RuntimeException re)
                   {
                       notProcessed = false;
                       Debug.log(Debug.ALL_ERRORS, "Runtime Exception while executing the request");
                       Debug.log(Debug.ALL_ERRORS, Debug.getStackTrace(re));
                       Debug.error("Skipping this request .. "+processor.toString());
                   }
               }
           }
           else
           {
               processor.start();
           }
    }

    /**
     * Utility class to execute the processing of dequeued message by ComServer
     * in a separate thread.
     */
    private class Processor extends Thread
    {
        private String header;
        private String body;
        private String jmsMsgId;

        public Processor(String header, String body, String jmsMsgId)
        {
            this.header = header;
            this.body = body;
            this.jmsMsgId = jmsMsgId;
        }
        
        public void run()
        {
            try
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS,"Started processing dequeued message in new thread...");
                
                setJMSMsgIdInCtx(jmsMsgId);
                setQueueNameInCtx();

                commServer.processMessage (header,body);
                
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS,"Done processing dequeued message.");
            }
            catch(Exception ex)
            {
                Debug.log(Debug.ALL_ERRORS, "ERROR: Processor.run() - Processing failed due to [" + ex.getMessage()+"]");
                // ex.printStackTrace();
                Debug.log(Debug.ALL_ERRORS,Debug.getStackTrace(ex));
            }
            finally
            {
                try 
                {
                    CustomerContext.getInstance().cleanup();
                } 
                catch (FrameworkException e) 
                {
                    Debug.warning("An exception occurred while cleaning up CustomerContext :" +Debug.getStackTrace(e));
                }
            }
        }
        @Override
        public String toString()
        {
            return "[header]"+header+"[body]"+body;
        }
    }

    private void setJMSMsgIdInCtx(String id) {
    	
        try
        {
            CustomerContext.getInstance().set(AbstractJmsMsgStoreDAO.PROP_JMSMESSAGEID, 
                    id);
        }
        catch(Exception ignore) {
        }
    }
    private void setQueueNameInCtx() {
        try
        {
            CustomerContext.getInstance().set(JMSConstants.QUEUE_NAME, 
            		this.queueNm);
        }
        catch(Exception ignore) {
        }
    }
    

    /**
     * Method call by JMS provider to notify about connection problems.
     * 
     *  @param javax.jms.JMSException exp
     */
    public void onException(javax.jms.JMSException exp)  
    {
          //  Debug.warning("An exception was thrown by JMS provider : "+exp.getMessage());
           // reConnect = true;
            /*if(DBUtils.validateConnection(this.dbConn))
            {
                Debug.log(Debug.NORMAL_STATUS, "Database connection is valid; reconnecting to queue..");
                try 
                {
                    // call shutdown first to clean up..
                    disConnect();
                    
                    register(this.commServer, this.dbConn);
                } 
                catch (JMSException e) 
                {
                    Debug.log(Debug.ALL_ERRORS, "An exception occured while registering consumer on queue :"+Debug.getStackTrace(e));
                    Debug.log(Debug.ALL_ERRORS, "Failed to register consumer on queue; calling shutdown");
                    disConnect();
                }
                
            }
            else
            {
                // get a new connection from DBConnectionPool
                Debug.log(Debug.NORMAL_STATUS, "Getting a new connection from DEFAULT/Thread Specific pool");
                Debug.log(Debug.NORMAL_STATUS, "sleeping for [30] seconds..");
                try 
                {
                    // sleep for 30 secs before reconnecting..
                    Thread.sleep(30*1000);
                } 
                catch (InterruptedException e1) 
                {
                    Debug.warning("Thread was Interrupted from sleep..");
                }
                
                try 
                {
                    
                    Debug.log(Debug.NORMAL_STATUS,"Calling shutdown for clean up....");
                    /* call shutdown first to clean up.. 
                    disConnect();

                    this.dbConn = DBUtils.getConnection(dbConnPoolKey);
                    acquiredNew = true;
                    
                    Debug.log(Debug.NORMAL_STATUS,"Registering again ....");
                    
                    register(this.commServer, this.dbConn);
                } 
                catch (JMSException e) 
                {
                    Debug.log(Debug.ALL_ERRORS, "An exception occured while registering consumer on queue :"+Debug.getStackTrace(e));
                    Debug.log(Debug.ALL_ERRORS, "Failed to register consumer on queue; calling shutdown");
                    disConnect();
                }
                catch (ResourceException e) 
                {
                    Debug.log(Debug.ALL_ERRORS, "Exception occured while getting a db connection "+Debug.getStackTrace(e));
                    Debug.log(Debug.ALL_ERRORS, "Failed to acquire a connection; calling shutdown");
                    disConnect();
                    return;
                }
            }
 */   }
    
    
    /**
     * If ThreadPoolExecutor is being used, this method would return
     * the current number of active threads that are executing. 
     * 
     * @return integer
     */
    public int getActiveThreadCount()
    {
        if(executor!=null)
            return executor.getActiveCount();
             
        throw new IllegalStateException("ThreadPoolExecutor is not being used !!");
    }
}

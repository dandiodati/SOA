package com.nightfire.comms.servicemgr;

import java.sql.Connection;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.Message;
import javax.jms.QueueBrowser;

import com.nightfire.comms.jms.JMSConsumerHelper;
import com.nightfire.framework.jms.JmsMsgStoreDAOFactory;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.jms.JMSException;
import com.nightfire.framework.jms.JMSPortabilityLayer;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;


/**
 * A JMS Consumer that sleeps for configured interval at fixed rate.
 * When it awakens; it polls the queue for number of messages available for consumption.
 * After taking a count of messages; it starts consumption and its processing. Thus it
 * only consumes messages that it has taken a count of it i.e while processing if any new messages
 * arrives it gets consumed when it awakens next.      
 * 
 * @author hpirosha
 */
public class JMSTimerConsumerHelper extends JMSConsumerHelper 
{
    
    private long timerInterval;
    private Timer timer;

    /**
     * 
     * @param queueNm
     * @param customerId
     * @param msgSelector
     * @param listener
     * @param threadPoolRequired
     * @param corePoolSize
     * @param maxPoolSize
     * @param timerInterval Interval in seconds for which the consumer sleeps.
     * @see com.nightfire.framework.jms.JMSPortabilityLayer.JMSConsumerHelper 
     */
    public JMSTimerConsumerHelper(String queueNm, String customerId, String msgSelector,boolean listener,
            boolean threadPoolRequired,int corePoolSize,int maxPoolSize,long timerInterval,String poolKey)
   {
    	super(queueNm, customerId, msgSelector,listener,threadPoolRequired,corePoolSize,maxPoolSize,poolKey);

        /* convert to milliseconds */
        this.timerInterval = timerInterval * 1000; 
        /* ignore the listener variable
           a listener cannot be started since that would register itself on queue and would be 
           called asynchronously. */ 
        
        /* this is set to true since JMSQueueConsumerManager would avoid creating 
           new consumer each time when start is invoked through servicemgr  */
        listener = true;

   }
    
    /**
     * 
     * @param queueNm
     * @param customerId
     * @param msgSelector
     * @param listener
     * @param threadPoolRequired
     * @param corePoolSize
     * @param maxPoolSize
     * @param timerInterval Interval in seconds for which the consumer sleeps.
     * @see com.nightfire.framework.jms.JMSPortabilityLayer.JMSConsumerHelper 
     */
    public JMSTimerConsumerHelper(String queueNm, String customerId, String msgSelector,boolean listener,
            boolean threadPoolRequired,int corePoolSize,int maxPoolSize,long timerInterval)
   {
    		this(queueNm, customerId, msgSelector,listener,threadPoolRequired,corePoolSize,maxPoolSize,timerInterval,null);
   }

    /**
     * @Override 
     */
    public void register(JMSConsumerCallBack commServer,Connection dbConn) throws JMSException
    {
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Registering JMSTimerConsumerHelper....");
        timer = new Timer(queueNm);
        timer.scheduleAtFixedRate( new TimerTask() {
                    public void run()
                    {
                        JMSTimerConsumerHelper.this.run();
                    }
                  },0,timerInterval);
        this.dbConn = dbConn;
        this.commServer = commServer;
        
        if(threadPoolRequired)
            executor = new ThreadPoolExecutor(  corePoolSize, maxPoolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue(maxPoolSize) );
        
    }    
    
    /**
     * Invoked when consumer awakens. It creates a queue browser to get the count of messages
     * available. Consumes them one by one and invokes callback class that does further processing. 
     */
    public void run()
    {
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Executing JMSTimerConsumerHelper.run()....");

            try
            {
                
                jpl = new JMSPortabilityLayer();
                queueConn = jpl.createQueueConnection(dbConn);
                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                    Debug.log(Debug.STATE_LIFECYCLE,"Created queue connection");

                if(isClientAckMode())
                {
                    queueSession = jpl.createQueueSessionWithClientAck(queueConn);
                    
                    if(dao==null)
                        dao = JmsMsgStoreDAOFactory.getInstance().getMsgStoreDAO(getGatewayNm(),getConsumerNm(),getQueueNm());
                }
                else
                    queueSession = jpl.createQueueSession(queueConn);
                
                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                    Debug.log(Debug.STATE_LIFECYCLE,"Created queue session with clientAckMode :"+isClientAckMode());

                queue = jpl.getQueue(queueSession, queueNm);

                if(msgSelector !=null)
                   queueReceiver = jpl.createQueueReceiver(queueSession,queue,msgSelector);
                else
                   queueReceiver = jpl.createQueueReceiver(queueSession,queue);

                queueConn.setExceptionListener(this);
                queueConn.start();


                // get number of messages present in queue.
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
                    Debug.log(Debug.STATE_LIFECYCLE,"Total message count ="+msgCount);

                // consume all messages that are present
                for(; msgCount > 0; msgCount--)
                {
                    Message message = queueReceiver.receiveNoWait();
                    
                    if(message!=null)
                        onMessage(message);
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
                        Debug.log(Debug.STATE_LIFECYCLE,"stopping JMSTimerConsumerHelper...");

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

            }
    }
    
    /**
     * Cancel the timer and cleanup.
     * @Override 
     */
    public void disConnect()
    {
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Disconnecting JMSTimerConsumerHelper[cancelling timer]...");

        if(timer!=null)
            timer.cancel();

        cleanUp();        
        
    }

}

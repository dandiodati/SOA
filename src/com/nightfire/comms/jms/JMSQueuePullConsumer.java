/**
* @author
**/
package com.nightfire.comms.jms;


import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.ComServerBase;
import com.nightfire.framework.jms.*;
import com.nightfire.framework.jms.JMSException;

import javax.jms.*;


/**
 * A generic message-processor for consuming messages from a configured JMS Queue.
 * Messages are pushed to this communication server, whenever there are messages in the Queue.
 */
public class JMSQueuePullConsumer extends ComServerBase implements JMSConsumerCallBack
{
    /**
     * Property indicating name of the queue to consume message(s) from.
     */
    public static final String QUEUE_NAME_PROP = "QUEUE_NAME";

    /**
     * Property indicating Message Selection Criteria for consuming messages.
     */
    public static final String MESSAGE_SELECTOR_PROP = "MESSAGE_SELECTOR";

    /* Logging class Name */
    private static final String LOGGING_CLASS_NAME = "JMSQueuePullConsumer";

      /**
       * Whether to enable Pull Consumer Thread Pooling for this server or not.
       *
       */
      private static String THREAD_POOL_REQUIRED_PROP =   "THREAD_POOL_REQUIRED";

     /**
       * Thread Pool will be on by default.
       *
       */
      private boolean threadPoolRequired = true;

      /**
       * If Thread Pool is on then MAX_POOL_SIZE and CORE_POOL_SIZE .
       * are required parameters for configuring Thread Pool.
       */
      private static String MAX_POOL_SIZE_PROP = "MAX_POOL_SIZE";

      private static String CORE_POOL_SIZE_PROP =   "CORE_POOL_SIZE";
      
      private static String USE_CLIENT_ACK_PROP = "USE_CLIENT_ACK";

      private int maxPoolSize = 10;

      private int corePoolSize = 5;

        /* Flag indicating whether the server is shutdown or running */
      private static boolean shutdownInProgress = false;

      /**
       * Thread Pool Executor.
       *
       */
      private ThreadPoolExecutorService executor = null;
      
      private AbstractJmsMsgStoreDAO dao = null;
      
      private boolean useClientAck = false;
      
     /**
     * Constructor
     *
     * @param   key   Property-key to use for locating initialization properties.
     * @param   type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException FrameworkException  Thrown if initialization fails.
     */
    public JMSQueuePullConsumer ( String key, String type ) throws ProcessingException, FrameworkException
    {
         // Call the base class method that creates comm server object and loads its properties.
        super ( key, type );

        Debug.log( Debug.OBJECT_LIFECYCLE, LOGGING_CLASS_NAME + ": Initializing the Asynchronous JMS Queue Pull Consumer Server ..." );

        StringBuffer errorBuffer = new StringBuffer();

        // Get configuration properties specific to this comm server.

        // Get the queue name from which the messages have to be consumed.
        String tmp;

        tmp = getPropertyValue(THREAD_POOL_REQUIRED_PROP);
        if ( StringUtils.hasValue(tmp) )
        {
            try
            {
                threadPoolRequired = StringUtils.getBoolean(tmp);
            }
            catch ( FrameworkException fe )
            {
                threadPoolRequired = false;
                Debug.log(Debug.ALL_WARNINGS, "Invalid value ["+tmp+"] for property ["
                                          + THREAD_POOL_REQUIRED_PROP + "], using default value ["
                                          + threadPoolRequired + "]" );
                errorBuffer.append("Invalid value [" +tmp+"] for property ["+ THREAD_POOL_REQUIRED_PROP+"].");

            }
        }

        if(threadPoolRequired)
        {
            //getting maximum pool size
            tmp = getPropertyValue(MAX_POOL_SIZE_PROP);

            if(StringUtils.hasValue(tmp))
            {
                try
                {
                    maxPoolSize   =  StringUtils.getInteger(tmp);

                }
                catch(FrameworkException fe)
                {
                    maxPoolSize = 10;
                    Debug.log(Debug.ALL_WARNINGS,"Invalid value [" +tmp+"] for property ["+ MAX_POOL_SIZE_PROP+"]. Using Default value 10");
                    errorBuffer.append("Invalid value [" +tmp+"] for property ["+ MAX_POOL_SIZE_PROP+"].");

                }
            }

            //getting core pool size
            tmp = getPropertyValue(CORE_POOL_SIZE_PROP);

            if(StringUtils.hasValue(tmp))
            {
                try
                {
                    corePoolSize   =  StringUtils.getInteger(tmp);
                }
                catch(FrameworkException fe)
                {
                    corePoolSize=5;
                    Debug.log(Debug.ALL_WARNINGS, "Invalid value [" +tmp+"] for property ["+ CORE_POOL_SIZE_PROP+"]. Using Default value 5");
                    errorBuffer.append("Invalid value [" +tmp+"] for property ["+ CORE_POOL_SIZE_PROP+"].");
                }
            }
            if(corePoolSize > maxPoolSize )
            {
                Debug.log(Debug.ALL_WARNINGS,  CORE_POOL_SIZE_PROP+" value should be less than "+MAX_POOL_SIZE_PROP+" value.");
                errorBuffer.append("Invalid values for Pool Size. "+CORE_POOL_SIZE_PROP+" value should be less than "+MAX_POOL_SIZE_PROP+" value.");
            }
        }
        queueName = getRequiredPropertyValue ( QUEUE_NAME_PROP, errorBuffer );

        Debug.log ( Debug.SYSTEM_CONFIG, LOGGING_CLASS_NAME + ": Configured Queue from which message(s) are to be consumed from is [" + queueName + "]." );

        // Get the message selector expression used to filter messages being consumed.
        msgSelector = getPropertyValue(MESSAGE_SELECTOR_PROP);

        Debug.log ( Debug.SYSTEM_CONFIG, LOGGING_CLASS_NAME + ": Configured Message Selection Criteria while consuming message(s) is  [" + msgSelector + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

       //initialize Thread Pool
        if(threadPoolRequired)
        {
            executor =  new ThreadPoolExecutorService(corePoolSize, maxPoolSize);
        }
        
        String useClientAckStr =  getPropertyValue(USE_CLIENT_ACK_PROP);
        
        if(StringUtils.hasValue(useClientAckStr))
        try
        {
            useClientAck = StringUtils.getBoolean(useClientAckStr);
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "Use client ack mode :"+useClientAck);
        }
        catch(FrameworkException fe)
        {
            Debug.error("Incorrect value configured for " +USE_CLIENT_ACK_PROP+" : "+useClientAckStr);
            throw new FrameworkException("Incorrect value configured for " +USE_CLIENT_ACK_PROP+" : "+useClientAckStr);
        }
        
        if(useClientAck)
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "client ack mode is true");
            
            dao = JmsMsgStoreDAOFactory.getInstance().getMsgStoreDAO(key, type, queueName);
        }
    }

   /**
    * Shuts-down the server object.
    */
    public void shutdown()
   {
        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log ( Debug.IO_STATUS, LOGGING_CLASS_NAME +
            ": Received a request to shutdown.  Notifying ShutdownHandler ..." );

        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log ( Debug.IO_STATUS, LOGGING_CLASS_NAME +
            ": Disconnecting from " + queueName );

            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+".shutdown(): Disconnecting from JMS Provider ... ");
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+".shutdown(): Setting the flag 'isServerShutDown' to [true]" );
            shutdownInProgress = true;

            try
            {
                // Release the Queue Session if not released earlier.
                if(queueSession != null)
                {
                   JMSSession.closeQueueSession ( jpl, queueSession );
                   queueSession = null;
                }

                // Release the Queue Connection if unreleased.
                if(queueConnection != null)
                {
                    JMSConnection.closeQueueConnection ( jpl, queueConnection );
                    queueConnection= null;
                }
            }catch(Exception e)
            {
                 Debug.logStackTrace(e);
            }

           try
           {
                if(threadPoolRequired)
                {
                    executor.shutdown();
                }
            }
            catch ( Exception e )
            {
                Debug.logStackTrace(e);
            }
   }

   /**
    * The run method creates a queue consumer to receive messages indefinitely
    * delivered to a configured queue, and process them individually.
    */
    public void run()
    {
        try {
            processRequests();
        }
        catch ( Exception e)
        {
            Debug.logStackTrace(e);
        }

    }

    /**
    * This method creates a queue consumer to synchronously receive messages
    * delivered to a configured queue; until the server is shutdown. This consumer
    * blocks itself indefinitely until a message arrives in the queue.Upon receiving
    * the request message it is forwarded for further processing. In case of any exception,
    * it tries to resume processing for once after sleeping for 120 seconds.
    */
    public void processRequests()
    {
        try {
            // Acquire an instance of JMS Portability Layer, for invoking vendor specific methods.
            jpl = new JMSPortabilityLayer();

            // Connect to JMS Provider.
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Connecting to JMS Provider ... " );

            // Acquire JMS connection.
            queueConnection = JMSConnection.acquireQueueConnection ( jpl );

            // Acquire JMS Session.
            if(useClientAck)
                queueSession =  jpl.createQueueSessionWithClientAck(queueConnection);
            else
                queueSession =  JMSSession.acquireQueueSession ( jpl, queueConnection );
            
            Debug.log( Debug.BENCHMARK, LOGGING_CLASS_NAME + ": Creating a pull consumer ...");

            //Start the JMS connection
            JMSConnection.startQueueConnection ( jpl, queueConnection );

            // Create an instance of an existing queue.
            Queue queue = jpl.getQueue ( queueSession, queueName );

            // Create a Queue Receiver object used to receive messages from queue.
            QueueReceiver queueReceiver;

            // If Message-Filter criteria is configured, use the criteria to create the Queue Consumer
            if ( StringUtils.hasValue ( msgSelector ) )
                queueReceiver = jpl.createQueueReceiver ( queueSession, queue, msgSelector);
            else
                queueReceiver = jpl.createQueueReceiver ( queueSession, queue);

            String header, body;
            // Wait and receive messages for ever.
             while (  !shutdownInProgress )
             {
                 Debug.log(Debug.MSG_STATUS,LOGGING_CLASS_NAME + ": Blocking to get a message from queue...");
                 Message msg = jpl.receiveWaitForEver (queueSession,queueReceiver);
                 Debug.log(Debug.MSG_STATUS,LOGGING_CLASS_NAME + ": Got a message from the queue...");
                 
                 header = jpl.getHeaderProperties(msg);
                 body = jpl.getTextMessage(msg);

                 String customerId = (String)CustomerContext.getInstance().getOtherItems().get(CustomerContext.CUSTOMER_ID_NODE);
                 if(useClientAck)
                 {
                     // store the message first and send an ack
                     try 
                     {
                         JmsMsgStoreDataObject dataObject = new JmsMsgStoreDataObject();
                           dataObject.setJmsMessage(msg);
                           dataObject.setMsgHeader(header);
                           dataObject.setCustomerId(customerId);

                           boolean inserted = dao.insert(dataObject);
//                         boolean inserted = dao.insert(customerId,msg);
                         
                         if(!inserted)
                         {
                             // call recover; without acknowledging the message
                             // skip further processing
                             queueSession.recover();
                             continue;
                         }
                         
                         msg.acknowledge();
                         
                     }
                     catch (javax.jms.JMSException e) 
                     {
                        
                         Debug.error("Unable to send an  acknowledgement.."+Debug.getStackTrace(e));
                        throw new JMSException("Failed to send an acknowledement back..."+e.getMessage());
                        
                     }

                 }
                 
                Processor processor = new Processor(header,body);
                if(threadPoolRequired)
                {
                    executor.execute(processor);
                }
                else
                {
                    Thread t = new Thread(processor);
                    t.start();
                }
             }
        }
        catch ( Exception re)
        {
            try
            {
                if(!shutdownInProgress)
                {
                    Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+" .registerConsumer(): Disconnecting from JMS Provider ... ");
                    
                    // Release the Queue Session if not released earlier.
                    if(queueSession != null)
                    {
                        JMSSession.closeQueueSession ( jpl, queueSession );
                        queueSession = null;
                    }

                    // Release the Queue Connection if unreleased.
                    if(queueConnection != null)
                    {
                        JMSConnection.closeQueueConnection ( jpl, queueConnection );
                        queueConnection= null;
                    }
                    Debug.logStackTrace(re);

                    if(repeatProcessing)
                    {
                       Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+" .registerConsumer(): Timer going to sleep for [120] seconds ... ");
                       Thread.sleep(120*1000);

                       // Resume processing only for once.
                       repeatProcessing = false;
                       resumeProcessing();
                    }

                }
            }
            catch ( Exception e )
            {
                Debug.warning ( e.toString() );
            }
        }
    }


    /**
     * This method is called by the pull Consumer whenever any message is consumed from a queue.
     *
     * @param message - header containing the message properties
     * @param message - the message
     */
    public void processMessage ( String header, String message )
    {
        try
        {
            // Process the consumed message.
            process ( header, message );

        } catch ( Exception e )
        {
            Debug.error("ERROR: " + LOGGING_CLASS_NAME + ": Failed to process the message consumed from the queue" + e.toString() );
        }
    }


    /**
     * This method is called by the JMSExceptionListener in order to resume processing in case
     * of an exception notified by Exception Listener, to which the consumer has registered.
     */
    public void resumeProcessing ()
    {
        try
        {
            // Reconnect the consumer to the JMS provider and resume processing.
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME + ": Reconnecting the consumer to the JMS provider and resuming the processing." );
            processRequests();

        } catch ( Exception e )
        {
            Debug.error("ERROR: " + LOGGING_CLASS_NAME + ": Failed to reconnect the consumer to the " +
                    "JMS provider and to resume processing." + e.toString());
        }
    }





    // Stores the name of the queue from which the messages have to be consumed.
    private String queueName = null;

    // Stores the message selector expression used to filter the messages
    // while consuming from a queue asynchronously.
    private String msgSelector = null;

    // Stores an instance of a JMS Queue Connection.
    private QueueConnection queueConnection = null;

    // Stores an instance of a JMS Queue Session.
    private QueueSession queueSession = null;

    // Stores an instance of JMS Portability Layer, for invoking vendor specific methods.
    private JMSPortabilityLayer jpl = null;

    // Flag indicating to resume processing in case of any exception.
    boolean repeatProcessing = true;

    private class Processor extends Thread
    {
        private String headerObj;
        private String messageObj;

        public Processor(String header, String body)
        {
            headerObj = header;
            messageObj = body;
        }
        public void run()
        {
            try
            {
                Debug.log(Debug.MSG_STATUS,LOGGING_CLASS_NAME + ": Started processing dequeued message in new thread...");
                processMessage (headerObj,messageObj);
                Debug.log(Debug.MSG_STATUS,LOGGING_CLASS_NAME + ": Done processing dequeued message.");
            }
            catch(Exception ex)
            {
                Debug.log(Debug.ALL_ERRORS, "ERROR: Processor.run() - Processing failed due to [" + ex.getMessage()+"]");
                ex.printStackTrace();
            }

        }
    }

}
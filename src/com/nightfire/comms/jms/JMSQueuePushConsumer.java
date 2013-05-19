/**
* @author
**/
package com.nightfire.comms.jms;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.ComServerBase;
import com.nightfire.framework.jms.*;

import javax.jms.QueueConnection;
import javax.jms.QueueSession;

/**
 * A generic message-processor for consuming messages from a configured JMS Queue.
 * Messages are pushed to this communication server, whenever there are messages in the Queue.
 */
public class JMSQueuePushConsumer extends ComServerBase implements JMSConsumerCallBack, JMSExceptionCallBack
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
    private static final String LOGGING_CLASS_NAME = "JMSQueuePushConsumer";

    private AbstractJmsMsgStoreDAO dao = null;
    
    private boolean useClientAck = false;

    private static String USE_CLIENT_ACK_PROP = "USE_CLIENT_ACK";

   /**
     * Constructor
     *
     * @param   key   Property-key to use for locating initialization properties.
     * @param   type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException FrameworkException  Thrown if initialization fails.
     */
    public JMSQueuePushConsumer ( String key, String type ) throws ProcessingException, FrameworkException
    {
         // Call the base class method that creates comm server object and loads its properties.
        super ( key, type );

        Debug.log( Debug.OBJECT_LIFECYCLE, LOGGING_CLASS_NAME + ": Initializing the Asynchronous JMS Queue Push Consumer Server ..." );

        StringBuffer errorBuffer = new StringBuffer();

        // Get configuration properties specific to this comm server.

        // Get the queue name from which the messages have to be consumed.
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

        try
        {
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+".shutdown(): Disconnecting from JMS Provider ... " );

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
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+": .shutdown(): Server is successfully ShutDown. Setting the flag 'isServerShutDown' to [true]" );

        }
        catch ( Exception e )
        {
            Debug.logStackTrace(e);
        }

   }

    /**
     * This method registers a queue consumer to receive any available messages
     * asynchronously from a specified queue.
     */
    public void run()
    {
        try {
            registerConsumer();
        }
        catch ( Exception e)
        {
            Debug.logStackTrace(e);
        }

    }

    /**
     * This method registers a message listener to listen for specific messages
     * (if a message selector is specified) on specific queue and wait for
     * pushConsumer to call back with message(s).
     */
    public void registerConsumer()
    {
        try {
            // Acquire an instance of JMS Portability Layer, for invoking vendor specific methods.
            jpl = new JMSPortabilityLayer();

            // Connect to JMS Provider.
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Connecting to JMS Provider ... " );

            // Acquire JMS connection.
            queueConnection = JMSConnection.acquireQueueConnection ( jpl );

            // Register an Exception Listener on JMS Connection, so that the consumer is notified in
            // case of any JMSException thrown on the connection object.
            JMSConnection.registerQueueExceptionListener( this, jpl, queueConnection);

            // Acquire JMS Session.
            if(useClientAck)
                queueSession =  jpl.createQueueSessionWithClientAck(queueConnection);
            else
                queueSession =  JMSSession.acquireQueueSession ( jpl, queueConnection );

            Debug.log( Debug.BENCHMARK, LOGGING_CLASS_NAME + ": Creating a push consumer ...");

            // Create JMS Push Consumer object
            JMSConsumer pushConsumer = null;
            if(useClientAck)
            {
                pushConsumer = new JMSConsumer ( jpl, queueSession, useClientAck );
                pushConsumer.setMsgStoreDAO(dao);
            }
            else
                pushConsumer = new JMSConsumer ( jpl, queueSession );
            
            // If Message-Filter criteria is configured, use the criteria to create the Queue Consumer
            if ( StringUtils.hasValue ( msgSelector ) )
                pushConsumer.createQueueConsumer ( queueName, msgSelector );
            else
                pushConsumer.createQueueConsumer ( queueName );

            //Start the JMS connection
            JMSConnection.startQueueConnection ( jpl, queueConnection );

            // Create and Set the message listener for this Consumer
            pushConsumer.registerQueueMessageListener ( this );

        }
        catch ( Exception re)
        {
            try
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
            }
            catch ( Exception e )
            {
                Debug.warning ( e.toString() );
            }
            Debug.logStackTrace(re);
        }

    }



    /**
     * This method is called by the push Consumer when any consumable messages are available.
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
            Debug.error("ERROR: " + LOGGING_CLASS_NAME + ": Failed to process the message consumed from the queue" + e.toString());
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
            // Release the Queue Session if not released earlier.
            JMSSession.closeQueueSession ( jpl, queueSession );

            // Release the Queue Connection if unreleased.
            JMSConnection.closeQueueConnection ( jpl, queueConnection );

            // Reconnect the consumer to the JMS provider and resume processing.
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME + ": Reconnecting the consumer to the JMS provider and resuming the processing." );
            registerConsumer();

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

}
package com.nightfire.comms.jms;

import com.nightfire.spi.common.communications.PollComServerBase;
import com.nightfire.framework.jms.*;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.common.ProcessingException;

import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.QueueBrowser;
import javax.jms.Queue;

import java.util.Enumeration;

/**
 * This class is intended to be used to poll messages from JMS queues periodically.
 * THe JMSQueuePollConsumer is polled periodically and after every period, the JMS queue
 * configured is checked for any messages ready to be dequeued. If there exists a message, then
 * that message is consumed and forwarded for further processing.
 */

public class JMSQueuePollConsumer extends PollComServerBase implements JMSConsumerCallBack
{
    /**
     * Property indicating name of the queue to consume message(s) from
     */
    public static final String QUEUE_NAME_PROP = "QUEUE_NAME";

    /**
     * Property indicating Message Selection Criteria for consuming messages.
     */
    public static final String MESSAGE_SELECTOR_PROP = "MESSAGE_SELECTOR";

     /* Logging class Name */
    private static final String LOGGING_CLASS_NAME = "JMSQueuePollConsumer";

    private boolean useClientAck = false;

    private static String USE_CLIENT_ACK_PROP = "USE_CLIENT_ACK";

    private AbstractJmsMsgStoreDAO dao = null;

   /**
     * Constructor
     *
     * @param   key   Property-key to use for locating initialization properties.
     * @param   type  Property-type to use for locating initialization properties.
     *
     * @exception  com.nightfire.common.ProcessingException FrameworkException  Thrown if initialization fails.
     */
    public JMSQueuePollConsumer ( String key, String type ) throws ProcessingException, FrameworkException
    {
        // Call the base class method that creates comm server object and loads its properties.
        super ( key, type );

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, LOGGING_CLASS_NAME + ": Initializing the Asynchronous JMS Queue Poll Consumer Server ..." );

        StringBuffer errorBuffer = new StringBuffer();

        // Get configuration properties specific to this comm server.

        // Get the queue name from which the messages have to be consumed.
        queueName = getRequiredPropertyValue ( QUEUE_NAME_PROP, errorBuffer );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log ( Debug.SYSTEM_CONFIG, LOGGING_CLASS_NAME + ": Configured Queue from which message(s) are to be consumed from is [" + queueName + "]." );

        // Get the message selector expression to be used to filter messages while consuming from queue.
        msgSelector = getPropertyValue ( MESSAGE_SELECTOR_PROP );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, LOGGING_CLASS_NAME + ": Configured Message Selection Criteria while consuming message(s) is  [" + msgSelector + "]." );

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

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException ( errMsg );
        }
    }

    /**
     * This method is invoked every time the specified timer expires, to receive
     * a message from a JMS Queue if one exists, and then forwards the message
     * received for further processing.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    protected void processRequests() throws ProcessingException {

        try {
            // Count of browsed messages
            int msgCount = 0;

            // Acquire an instance of JMS Portability Layer, for invoking vendor specific methods.
            jpl = new JMSPortabilityLayer();

            // Connect to JMS Provider.
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Connecting to JMS Provider ... " );


            // Acquire JMS connection.
            queueConnection = JMSConnection.acquireQueueConnection ( jpl );

            // Acquire JMS Session.
            if(useClientAck)
                queueSession = jpl.createQueueSessionWithClientAck(queueConnection);
            else
                queueSession = jpl.createQueueSession(queueConnection);

            if(Debug.isLevelEnabled(Debug.BENCHMARK))
                Debug.log(Debug.BENCHMARK, LOGGING_CLASS_NAME + ": Creating a poll consumer ...");

            // Create JMS Poll Consumer object
            JMSConsumer pollConsumer = null; 
            if(useClientAck)
            {
                pollConsumer =  new JMSConsumer ( jpl, queueSession ,useClientAck );
                pollConsumer.setMsgStoreDAO(dao);
            }
            else
                pollConsumer =  new JMSConsumer ( jpl, queueSession );

            // If Message-Filter criteria is configured, use the criteria to create the Queue Consumer.
            if ( StringUtils.hasValue ( msgSelector ) )
                pollConsumer.createQueueConsumer ( queueName, msgSelector );
            else
                pollConsumer.createQueueConsumer ( queueName );
            
            //Starting the JMS connection
            JMSConnection.startQueueConnection ( jpl, queueConnection );

            // Get the number of available messages at the time of polling attempt
            msgCount = getAvailableMessageCount(queueSession, queueName, msgSelector);

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_CLASS_NAME + ": Got [" + msgCount + "] messages available in this polling attempt from queue [" + queueName + "]");

            // Poll consumer would consume only those messages that are available at the time of polling attempt
            for(int ix = 0; ix < msgCount; ix++)
            {
                pollConsumer.receiveNoWait ( this );
            }

        }
        catch (Exception e)
        {
            Debug.logStackTrace(e);
        }
        finally
        {
            try
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Disconnecting from JMS Provider ... " );

                // Release the Queue Session if not released earlier.
                JMSSession.closeQueueSession ( jpl, queueSession );

                // Release the Queue Connection if unreleased.
                JMSConnection.closeQueueConnection ( jpl, queueConnection );

            }
            catch(Exception e)
            {
                Debug.logStackTrace(e);
            }
        }


    }


    /**
     *  This method is called by the poll Consumer whenever any message is consumed from a queue.
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
     * This method is called by the poll Consumer to get the count of available messages
     * Poll consumer would only poll messages available at the time of start of Queue Poll Consumer
     * This method uses message selctor properties as filter to get the message count
     *
     * @param qSession - Queue session
     * @param qName - Name of the Queue to browse
     * @param messageSelector - Message selector properties
     * @return int - Count of available messages
     */
    public int getAvailableMessageCount ( QueueSession qSession, String qName, String messageSelector ) throws ProcessingException
        {
        //Queue object created to be used by QueueBrowser
        Queue queue = null;

        //Enumeration to hold the browser messages
        Enumeration enumeration = null;

        //Stores an instance of QueueBrowser object
        QueueBrowser queueBrowser = null;

        int count = 0;

        try
        {
            queue = jpl.getQueue ( qSession, qName );

            if( StringUtils.hasValue ( messageSelector ) )
            queueBrowser = qSession.createBrowser(queue, messageSelector);
            else
                queueBrowser = qSession.createBrowser ( queue );

            enumeration = queueBrowser.getEnumeration();

            while( enumeration.hasMoreElements () )
            {
                count++;
                enumeration.nextElement();
            }

            return count;
        }
        catch(Exception e)
        {
            Debug.error ( "ERROR: " + LOGGING_CLASS_NAME + ": Failed to get the available messages count from the queue" + e.toString() );
            throw new ProcessingException(e);
        }
        finally
        {
            try
            {
                if(queueBrowser != null)
                queueBrowser.close();
            }
            catch(Exception e)
            {
                Debug.error("ERROR: " + LOGGING_CLASS_NAME + ": Failed to close queue browser" + e.toString());
            }
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
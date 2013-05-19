package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.*;


import com.nightfire.common.ProcessingException;

import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.FrameworkException;

import com.nightfire.mgrcore.queue.ConsumerBase;
import com.nightfire.mgrcore.queue.ConsumerPolicy;
import com.nightfire.mgrcore.queue.DefaultConsumerPolicy;
import com.nightfire.mgrcore.queue.MessageQueue;
import com.nightfire.mgrcore.queue.PushOperations;
import com.nightfire.mgrcore.queue.QueueException;
import com.nightfire.mgrcore.queue.QueueConstants;
import com.nightfire.mgrcore.queue.agents.QueueAgentBase;

/**
 * This processor pulls NPAC XML messages from the outbound queue.
 *
 */
public abstract class QueuePollerBase extends QueueAgentBase{

    /**
     * Property name for specifying an optional WHERE condition for use when
     * querying for queued messages.
     */
    private static final String WHERE_CONDITION_PROP = "WHERE_CONDITION";

    private ConsumerBase consumer;

    private ConsumerPolicy policy;

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type )
                             throws ProcessingException{

       super.initialize(key, type);
       
       ThreadMonitor.ThreadInfo tmti = null;
       try{
    	   
    	  tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
          // the query that goes to the NPAC queue is not meant to be
          // specific to any customer, so the ID in the customer context
          // is cleared.
          CustomerContext.getInstance().setNoCustomer();

          consumer = createConsumer();

          Map values = new HashMap();

          // get optional where condition
          String whereCondition = getPropertyValue( WHERE_CONDITION_PROP );

          // poll only for our particular message type and order by queue ID
          if (whereCondition == null) {

             consumer.setDequeueCriteria( getMessageType(),
                                          values,
                                          QueueConstants.ID_COL );


          }
          else {

             consumer.setDequeueCriteria( getMessageType(),
                                          values,
                                          QueueConstants.ID_COL,
                                          whereCondition);

          }

          policy = createPolicy();

       }
       catch(QueueException qex){

          throw new ProcessingException( qex.getMessage() );

       }
       catch(FrameworkException fex){

          // could not clear the customer ID
          throw new ProcessingException( fex.getMessage() );

       }
       finally
       {
    	   ThreadMonitor.stop(tmti);
       }

    }

    /**
     * Get the message type. This is used to locate the queue message type
     * definition in the repository.
     *
     * @return String
     */
    protected abstract String getMessageType();

    /**
     * Get the specific queue consumer instance to use.
     *
     * @throws QueueException
     * @return ConsumerBase
     */
    protected abstract ConsumerBase createConsumer()
                                    throws QueueException,
                                           ProcessingException;

    /**
     * This gets the consumer policy instance to use. This implementation
     * returns a new DefaultConsumerPolicy instance.
     *
     * @throws QueueException
     * @return ConsumerPolicy
     */
    protected ConsumerPolicy createPolicy()
                             throws QueueException{

       return new DefaultConsumerPolicy();

    }

    /**
     * Get the queue containing messages to process.
     *
     * @return  A MessageQueue object.
     *
     * @exception  QueueException  Thrown on errors.
     */
    public MessageQueue getQueue ( ) throws QueueException{

       return consumer.getQueue();


    }

    /**
     * Get the consumer that will be used to process queued messages.
     *
     * @return  A PushOperations object.
     *
     * @exception  QueueException  Thrown on errors.
     */
    public PushOperations getConsumer( ) throws QueueException{

       return consumer;

    }

    /**
     * Get the consumer policy used to determine how to handle the outcome of
     * queued message processing.  If not re-implemented in leaf class, the
     * default consumer policy will be used.
     *
     * @return  A ConsumerPolicy object.
     *
     * @exception  QueueException  Thrown on errors.
     */
    public ConsumerPolicy getPolicy ( ) throws QueueException
    {
        return policy;
    }


}

package com.nightfire.spi.neustar_soa.queue;

import com.nightfire.mgrcore.queue.*;
import com.nightfire.framework.util.Debug;

public class SOAConsumerPolicy extends DefaultConsumerPolicy {

    /**
     * This adds a special check for exceptions that occured because the
     * SOA Server is down. In this case, the error count will not be increased
     * so that the message will be resent when the SOA server comes back up.
     *
     * @param e Exception
     * @param queueMessage QueueMessage
     * @param queue MessageQueue
     * @throws QueueException
     */
    public void handleError ( Exception e,
                              QueueMessage queueMessage,
                              MessageQueue queue )
                              throws QueueException
    {

        if(e instanceof ConnectivityDownException){

           // the SOA server is down
           Debug.error(e.getMessage());

        }
        else{

           // There was something wrong with the message itself.
           // This will increase the message's error count.
           super.handleError(e, queueMessage, queue);

        }

    }

}

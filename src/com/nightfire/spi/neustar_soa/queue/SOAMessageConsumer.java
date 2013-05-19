////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import com.nightfire.spi.neustar_soa.utils.RoundRobinAsyncCorbaClient;

import com.nightfire.mgrcore.queue.ConsumerBase;
import com.nightfire.mgrcore.queue.ConsumerPolicy;
import com.nightfire.mgrcore.queue.PushOperations;
import com.nightfire.mgrcore.queue.QueueException;

public class SOAMessageConsumer extends ConsumerBase {

   /**
    * Used to deliver messages consumed from the queue.
    */
   private PushOperations deliveryService;

   /**
    *
    */
   private ConsumerPolicy policy;

   /**
    * Constructor.
    *
    * @param client RoundRobinAsyncCorbaClient the client used to deliver
    *               messages.
    */
   public SOAMessageConsumer(RoundRobinAsyncCorbaClient client) throws QueueException {

      super();

      deliveryService = new SOADeliveryService(client);
      policy = new SOAConsumerPolicy();

   }

   /**
    * Gets the SOA delivery service instance.
    */
   public PushOperations getDeliveryService( String messageType )
                                           throws QueueException{

      return deliveryService;

   }

    public ConsumerPolicy getConsumerPolicy( String messageType )
                                             throws QueueException{

       return policy;

    }


}

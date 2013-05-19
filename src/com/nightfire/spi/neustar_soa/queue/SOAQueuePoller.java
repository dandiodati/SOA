package com.nightfire.spi.neustar_soa.queue;

import java.util.*;

import com.nightfire.mgrcore.queue.DefaultConsumerPolicy;

import com.nightfire.spi.neustar_soa.utils.RoundRobinAsyncCorbaClient;

public class SOAQueuePoller implements Poller{

   private QueuePollerThread poller;

   public SOAQueuePoller(String whereCondition,
                         String spid,
                         Integer region,
                         int maxThreadCount,
                         long pollingInterval,
                         RoundRobinAsyncCorbaClient client)
                         throws Exception{

      SOAMessageConsumer consumer = new SOAMessageConsumer(client);

      Map values = new HashMap();

      if( spid != null ){
         values.put(SOAMessageType.SPID_COL, spid);
      }

      if( region != null ){
         values.put(SOAMessageType.REGION_COL, region);
      }

      poller = new QueuePollerThread(consumer,
                                     SOAMessageType.SOA_MESSAGE_TYPE,
                                     whereCondition,
                                     values,
                                     maxThreadCount,
                                     pollingInterval);

   }

   public void start(){

      poller.start();

   }

   public void shutdown(){

      poller.shutdown();

   }

}

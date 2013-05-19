package com.nightfire.spi.neustar_soa.queue;

import java.util.*;
import com.nightfire.mgrcore.queue.DefaultConsumerPolicy;
import com.nightfire.spi.neustar_soa.utils.RoundRobinAsyncCorbaClient;
import com.nightfire.framework.util.Debug;


public class SOARequestQueuePoller implements Poller{

   	private QueuePollerThread poller;

   	public SOARequestQueuePoller(String whereCondition,
						  		String spid,
						  		Integer region,
						  		int maxThreadCount,
						  		long pollingInterval,
						  		RoundRobinAsyncCorbaClient client)
						  		throws Exception{

	SOAMessageConsumer consumer = new SOAMessageConsumer(client);
	
	Map values = new HashMap();

	if( spid != null ){
		values.put(SOARequestMessageType.SPID_COL, spid);
	}

	poller = new QueuePollerThread(consumer,
								   SOARequestMessageType.SOA_REQUEST_MESSAGE_TYPE,
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
